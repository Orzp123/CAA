package com.caa.school.exception;

import com.caa.common.ErrorCode;

/**
 * 学校管理业务异常，携带 ErrorCode 供全局异常处理器映射。
 */
public class SchoolException extends RuntimeException {

    private final ErrorCode errorCode;

    public SchoolException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public SchoolException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
