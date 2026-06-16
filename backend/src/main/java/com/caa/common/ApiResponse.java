package com.caa.common;

/**
 * Unified API response envelope.
 * All controllers return ApiResponse<T> to ensure consistent structure.
 *
 * @param success whether the request succeeded
 * @param code    application error code (null on success)
 * @param message human-readable message
 * @param data    response payload (null on error)
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    /** 200 OK with payload. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, "OK", data);
    }

    /** 200 OK with no payload (e.g. DELETE). */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, "OK", null);
    }

    /** Error response using an ErrorCode enum constant. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.code(), errorCode.message(), null);
    }

    /** Error response with a custom message override. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.code(), message, null);
    }
}
