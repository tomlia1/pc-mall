package com.pcmall.util;

import jakarta.servlet.http.HttpServletRequest;
public final class ClientIpResolver {
    private ClientIpResolver() { }
    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
    }
}
