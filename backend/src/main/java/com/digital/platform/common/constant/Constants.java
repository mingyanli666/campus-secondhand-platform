package com.digital.platform.common.constant;

/**
 * 全局常量。魔法值集中管理，避免散落在各处的字符串/数字。
 */
public final class Constants {

    private Constants() {
    }

    /** 请求头中携带 token 的字段名 */
    public static final String TOKEN_HEADER = "Authorization";

    /** token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 会话中存放当前登录用户 */
    public static final String SESSION_USER = "currentUser";

    /** 默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /** 默认每页条数 */
    public static final long DEFAULT_PAGE_SIZE = 12L;

    /** 每页最大条数，防止前端传个 100000 把库拉爆 */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 正常状态 */
    public static final Integer STATUS_ENABLED = 1;

    /** 禁用/停用状态 */
    public static final Integer STATUS_DISABLED = 0;

    /** 未读 */
    public static final Integer UNREAD = 0;

    /** 已读 */
    public static final Integer READ = 1;

    /** 默认页码字符串，用于查询参数兜底 */
    public static final String ASC = "asc";

    /** 降序 */
    public static final String DESC = "desc";
}
