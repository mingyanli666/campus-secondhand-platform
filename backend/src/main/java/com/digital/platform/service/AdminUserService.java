package com.digital.platform.service;

import com.digital.platform.common.request.AdminUserQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.vo.AdminUserVO;

public interface AdminUserService {

    /** 分页查询用户，支持关键词、角色、状态筛选 */
    PageResult<AdminUserVO> page(AdminUserQuery query);

    /**
     * 启用 / 禁用用户。
     *
     * @param operatorId 操作人（当前管理员）ID，用于防止"把自己禁用掉"
     */
    void updateStatus(Long operatorId, Long userId, Integer status);

    /**
     * 重置用户密码。
     *
     * @return 重置后的新密码（明文），由前端展示给管理员转告用户
     */
    String resetPassword(Long userId);
}
