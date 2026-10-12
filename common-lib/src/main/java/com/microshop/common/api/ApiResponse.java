package com.microshop.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/** Uniform envelope for every REST response (success and error). */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final Integer status;          // only set on errors
    private final String message;
    private final T data;
    private final List<FieldErrorDetail> errors;
    private final String path;             // only set on errors
    private final Instant timestamp = Instant.now();

    private ApiResponse(boolean success, Integer status, String message, T data,
                        List<FieldErrorDetail> errors, String path) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.path = path;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, null, data, null, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, null, message, data, null, null);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message, null, null, null);
    }

    public static ApiResponse<Void> error(int status, String message, String path, List<FieldErrorDetail> errors) {
        return new ApiResponse<>(false, status, message, null,
                errors == null || errors.isEmpty() ? null : errors, path);
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
