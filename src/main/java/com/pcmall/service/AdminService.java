package com.pcmall.service;
import com.pcmall.dto.LoginRequest;
import com.pcmall.vo.LoginResponse;
public interface AdminService { LoginResponse login(LoginRequest request, String clientIp); }
