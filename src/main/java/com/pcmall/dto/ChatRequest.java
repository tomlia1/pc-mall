package com.pcmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$") String sessionId,
        @NotBlank @Size(max = 500) String message,
        Long contextProductId) {
    public ChatRequest(String sessionId, String message) {
        this(sessionId, message, null);
    }
}
