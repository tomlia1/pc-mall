package com.pcmall.common;

public record Result<T>(int code, String message, T data) {
    public static <T> Result<T> success(T data) { return new Result<>(200, "success", data); }
}
