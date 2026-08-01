package com.pcmall.service;

import com.pcmall.dto.ChatRequest;
import com.pcmall.vo.ChatResult;

public interface ChatService {
    ChatResult chat(ChatRequest request);
}
