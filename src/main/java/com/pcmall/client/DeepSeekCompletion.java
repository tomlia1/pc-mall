package com.pcmall.client;

public record DeepSeekCompletion(String content, String toolCallId, String toolName, String toolArguments) {
    public static DeepSeekCompletion text(String content) { return new DeepSeekCompletion(content, null, null, null); }
    public static DeepSeekCompletion toolCall(String id, String name, String arguments) { return new DeepSeekCompletion(null, id, name, arguments); }
    public boolean hasToolCall() { return toolName != null; }
}
