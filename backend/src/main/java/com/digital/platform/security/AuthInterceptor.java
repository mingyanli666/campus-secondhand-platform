package com.digital.platform.security;

import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.config.PlatformProperties;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 登录与角色校验拦截器。
 * <p>
 * 执行顺序：
 * <ol>
 *   <li>跨域预检请求（OPTIONS）直接放行；</li>
 *   <li>标注 {@link PublicApi} 的接口放行（公开接口也会尝试解析 token，但不强制）；</li>
 *   <li>解析 Authorization 头中的 token，失败则 401；</li>
 *   <li>回数据库确认账号仍然存在且未被禁用，否则 401 / 2004；</li>
 *   <li>把用户写入 {@link UserContext}，供业务层读取；</li>
 *   <li>若接口标注 {@link RequireRole}，校验角色，不匹配则 403。</li>
 * </ol>
 * 不管成功失败，afterCompletion 都会清理 ThreadLocal。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final PlatformProperties properties;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 跨域预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 非 Controller 方法（静态资源等）放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. 公开接口放行
        boolean isPublic = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PublicApi.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PublicApi.class);
        if (isPublic) {
            // 公开接口也尝试解析 token，方便详情页判断"当前用户是否已收藏"
            LoginUser user = jwtUtil.parseToken(resolveToken(request));
            if (user != null) {
                UserContext.set(user);
            }
            return true;
        }

        // 3. 校验 token
        LoginUser loginUser = jwtUtil.parseToken(resolveToken(request));
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 4. 回数据库确认账号仍然可用。
        //    只校验签名是不够的：JWT 一旦签发就有效到过期为止，
        //    如果这期间管理员把账号禁用了，光靠签名根本拦不住 ——
        //    用户还能拿旧 token 继续发帖、下单，最长 72 小时，"禁用"形同虚设。
        //    代价是每个需登录的请求多一次主键查询，对本项目的量级完全可以接受；
        //    真要扛高并发，这里应该换成缓存（Caffeine / Redis）而不是砍掉校验。
        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Constants.STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        // 角色和昵称以数据库为准：token 可能是很久以前签发的，里面的信息会过期
        loginUser.setRole(user.getRole());
        loginUser.setNickname(user.getNickname());

        // 5. 写入上下文
        UserContext.set(loginUser);

        // 6. 校验角色
        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole != null) {
            boolean allowed = Arrays.stream(requireRole.value())
                    .anyMatch(role -> role.getCode().equals(loginUser.getRole()));
            if (!allowed) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理，否则 Tomcat 线程复用会导致用户信息串号
        UserContext.clear();
    }

    /**
     * 从请求头取出 token，兼容带/不带 "Bearer " 前缀两种写法。
     */
    private String resolveToken(HttpServletRequest request) {
        String value = request.getHeader(properties.getJwt().getHeader());
        if (value == null || value.isBlank()) {
            // 兜底再读一次默认头，兼容前端配置不一致的情况
            value = request.getHeader(Constants.TOKEN_HEADER);
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith(Constants.TOKEN_PREFIX)) {
            return value.substring(Constants.TOKEN_PREFIX.length()).trim();
        }
        return value.trim();
    }
}
