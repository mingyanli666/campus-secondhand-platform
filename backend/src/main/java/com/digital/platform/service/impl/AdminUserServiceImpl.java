package com.digital.platform.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.AdminUserQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.service.AdminUserService;
import com.digital.platform.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /**
     * 生成随机密码时使用的字符集。
     * 刻意去掉了 0 / O / 1 / l / I 这些容易看错的字符 ——
     * 管理员要把这个密码念给用户或抄下来，混淆字符会带来真实的沟通成本。
     */
    private static final String PASSWORD_CHARS =
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final UserMapper userMapper;

    @Override
    public PageResult<AdminUserVO> page(AdminUserQuery query) {
        if (StrUtil.isNotBlank(query.getKeyword())) {
            query.setKeyword(query.getKeyword().trim());
        }
        Page<AdminUserVO> page = query.toPage();
        IPage<AdminUserVO> result = userMapper.selectAdminUserPage(page, query);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long operatorId, Long userId, Integer status) {
        requireUser(userId);

        // 防呆：不能把自己禁用掉。
        // 这类"自我锁死"的操作在后台系统里一定要拦，否则管理员一个误操作
        // 就再也登不进来了，只能去数据库里改。
        if (Constants.STATUS_DISABLED.equals(status) && userId.equals(operatorId)) {
            throw new BusinessException(ResultCode.CANNOT_DISABLE_SELF);
        }

        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);

        log.info("管理员 {} 将用户 {} 状态改为 {}", operatorId, userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long userId) {
        requireUser(userId);

        // 随机生成而不是重置成固定的 "123456"。
        // 固定默认密码是真实存在的安全隐患：所有人都知道，
        // 谁都能拿它去试任何一个被重置过的账号。
        String newPassword = RandomUtil.randomString(PASSWORD_CHARS, 8);

        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userMapper.updateById(update);

        // 日志里不打印密码明文，只记录"谁重置了谁的密码"
        log.info("管理员重置了用户 {} 的密码", userId);

        // 明文通过返回值交给前端展示一次，不入库
        return newPassword;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }
}
