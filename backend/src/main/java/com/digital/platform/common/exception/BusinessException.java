package com.digital.platform.common.exception;

import com.digital.platform.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 所有"可预期"的错误（账号已存在、商品已下架等）都抛这个异常，
 * 由 {@link GlobalExceptionHandler} 统一转换成 Result 返回，
 * 业务代码里就不用到处写 if-else 拼装返回值了。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 条件成立时抛出异常，写校验代码更简洁：Assert.isTrue(...)
     */
    public static void throwIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BusinessException(resultCode);
        }
    }

    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }
}
