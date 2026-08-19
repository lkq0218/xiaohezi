package com.xiaohezi.api;

import org.slf4j.MDC;

public class ApiResponse<T> {
    public String code;
    public String message;
    public T data;
    public String traceId;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get(RequestTraceFilter.TRACE_ID);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
