package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer code;
    private Long timestamp;

    public GlobalResponse(boolean success, String message, T data, Integer code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }


    public static <T> GlobalResponse<T> ok() {
        return new GlobalResponse<>(true, "OK", null, 200, System.currentTimeMillis());
    }

    public static <T> GlobalResponse<T> ok(T data) {
        return new GlobalResponse<>(true, "OK", data, 200, System.currentTimeMillis());
    }

    public static <T> GlobalResponse<T> ok(String message, T data) {
        return new GlobalResponse<>(true, message, data, 200, System.currentTimeMillis());
    }

    public static <T> GlobalResponse<T> created(String message, T data) {
        return new GlobalResponse<>(true, message, data, 201, System.currentTimeMillis());
    }

    public static <T> GlobalResponse<T> error(String message, int code) {
        return new GlobalResponse<>(false, message, null, code, System.currentTimeMillis());
    }

    public static <T> GlobalResponse<T> badRequest(String message) {
        return error(message, 400);
    }

    public static <T> GlobalResponse<T> unauthorized(String message) {
        return error(message, 401);
    }

    public static <T> GlobalResponse<T> forbidden(String message) {
        return error(message, 403);
    }

    public static <T> GlobalResponse<T> notFound(String message) {
        return error(message, 404);
    }
}
