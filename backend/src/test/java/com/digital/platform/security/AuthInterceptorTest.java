package com.digital.platform.security;

import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.config.PlatformProperties;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 鉴权拦截器的单元测试。
 * <p>
 * 为什么这三个场景最关键？
 * <ol>
 *   <li><b>没有 token 必须拦住</b> —— 这是所有权限校验的前提</li>
 *   <li><b>被禁用的用户拿着旧 token 必须拦住</b> —— JWT 是无状态的，
 *       服务器不知道签发之后发生了什么，这是本项目真实踩过的坑</li>
 *   <li><b>普通用户访问管理员接口必须拦住</b> —— 越权操作的最后一道门</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("鉴权拦截器")
class AuthInterceptorTest {

    private static final long USER_ID = 1L;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    private AuthInterceptor interceptor;

    /** 用真实的配置对象而不是 Mock，避免为 header 这类默认值写一堆 stub */
    private final PlatformProperties properties = new PlatformProperties();

    /** 测试用的假 Controller，用来给拦截器提供 HandlerMethod */
    static class DummyController {
        public void normalApi() {
        }

        @RequireRole(RoleType.ADMIN)
        public void adminApi() {
        }
    }

    private final DummyController dummy = new DummyController();

    @BeforeEach
    void setUp() {
        interceptor = new AuthInterceptor(jwtUtil, properties, userMapper);
    }

    /**
     * 拦截器会把用户写进 ThreadLocal，preHandle 抛异常时不会走到 afterCompletion。
     * 如果不清理，用户信息会串到下一个测试里 —— 这正是生产环境"线程复用串号"的同款问题。
     */
    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("未携带 token 时返回 401")
    void preHandle_rejects_when_token_missing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerOf("normalApi");

        assertBizCode(ResultCode.UNAUTHORIZED,
                () -> interceptor.preHandle(request, response, handler));
    }

    @Test
    @DisplayName("账号被禁用后，手里的旧 token 立即失效（不能等 72 小时过期）")
    void preHandle_rejects_when_user_is_disabled() throws Exception {
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerOf("normalApi");

        when(jwtUtil.parseToken("valid-token")).thenReturn(loginUser(RoleType.USER));
        // 数据库里这个账号已经被管理员禁用了
        when(userMapper.selectById(USER_ID)).thenReturn(user(Constants.STATUS_DISABLED, RoleType.USER));

        assertBizCode(ResultCode.USER_DISABLED,
                () -> interceptor.preHandle(request, response, handler));
    }

    @Test
    @DisplayName("普通用户访问管理员接口时返回 403")
    void preHandle_rejects_when_role_not_allowed() throws Exception {
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerOf("adminApi");

        when(jwtUtil.parseToken("valid-token")).thenReturn(loginUser(RoleType.USER));
        when(userMapper.selectById(USER_ID)).thenReturn(user(Constants.STATUS_ENABLED, RoleType.USER));

        assertBizCode(ResultCode.FORBIDDEN,
                () -> interceptor.preHandle(request, response, handler));
    }

    // ============================================================
    //  辅助方法
    // ============================================================

    private HandlerMethod handlerOf(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(dummy, DummyController.class.getMethod(methodName));
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }

    private LoginUser loginUser(RoleType role) {
        LoginUser u = new LoginUser();
        u.setUserId(USER_ID);
        u.setUsername("test");
        u.setRole(role.getCode());
        return u;
    }

    private User user(Integer status, RoleType role) {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername("test");
        u.setNickname("测试用户");
        u.setStatus(status);
        u.setRole(role.getCode());
        return u;
    }

    private void assertBizCode(ResultCode expected, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertThat(ex.getCode()).isEqualTo(expected.getCode());
    }
}
