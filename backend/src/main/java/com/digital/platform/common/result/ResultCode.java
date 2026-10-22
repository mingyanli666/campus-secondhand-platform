package com.digital.platform.common.result;

import lombok.Getter;

/**
 * 统一响应状态码。
 * <p>
 * 约定：200 成功；4xx 客户端错误；5xx 服务端错误；2xxx 业务错误（便于前端区分提示文案）。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 业务错误：用户
    USERNAME_EXISTS(2001, "该账号已被注册"),
    USERNAME_OR_PASSWORD_ERROR(2002, "账号或密码错误"),
    USER_NOT_FOUND(2003, "用户不存在"),
    USER_DISABLED(2004, "账号已被禁用，请联系管理员"),
    OLD_PASSWORD_ERROR(2005, "原密码不正确"),
    CANNOT_DISABLE_SELF(2006, "不能禁用自己的账号"),

    // 业务错误：商品
    PRODUCT_NOT_FOUND(3001, "商品不存在或已被删除"),
    PRODUCT_STATUS_ILLEGAL(3002, "当前商品状态不允许该操作"),
    NOT_PRODUCT_OWNER(3003, "只能操作自己发布的商品"),

    // 业务错误：分类
    CATEGORY_NOT_FOUND(3010, "分类不存在"),
    CATEGORY_NAME_EXISTS(3011, "分类名称已存在"),
    CATEGORY_HAS_PRODUCT(3012, "该分类下还有商品，无法删除"),

    // 业务错误：收藏
    ALREADY_FAVORITE(4001, "已经收藏过了"),
    NOT_FAVORITE(4002, "尚未收藏该商品"),
    FAVORITE_SELF(4003, "不能收藏自己发布的商品"),

    // 业务错误：订单
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_STATUS_ILLEGAL(5002, "当前订单状态不允许该操作"),
    CANNOT_BUY_SELF(5003, "不能购买自己发布的商品"),
    PRODUCT_NOT_ON_SALE(5004, "商品已下架或已售出"),
    NOT_ORDER_PARTICIPANT(5005, "你不是该订单的买卖双方，无权操作"),
    PRODUCT_HAS_ACTIVE_ORDER(5006, "该商品已有进行中的订单，请等待卖家处理"),

    // 业务错误：文件
    FILE_EMPTY(6001, "上传文件不能为空"),
    FILE_TYPE_NOT_ALLOWED(6002, "不支持的图片格式"),
    FILE_TOO_LARGE(6003, "图片大小超出限制"),
    FILE_UPLOAD_ERROR(6004, "文件上传失败，请重试"),

    // 业务错误：留言
    COMMENT_NOT_FOUND(7001, "留言不存在或已被删除"),
    CANNOT_COMMENT_SELF(7002, "不能给自己的商品留言"),
    COMMENT_PRODUCT_MISMATCH(7003, "回复的留言不属于该商品"),
    NOT_COMMENT_OWNER(7004, "只能删除自己的留言"),

    // 业务错误：举报
    REPORT_NOT_FOUND(8001, "举报记录不存在"),
    CANNOT_REPORT_SELF(8002, "不能举报自己发布的商品"),
    ALREADY_REPORTED(8003, "你已经举报过该商品，平台正在处理中"),
    REPORT_ALREADY_HANDLED(8004, "该举报已经处理过了");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
