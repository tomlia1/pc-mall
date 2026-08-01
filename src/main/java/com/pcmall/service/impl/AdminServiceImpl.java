package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcmall.common.BusinessException;
import com.pcmall.config.JwtService;
import com.pcmall.dto.LoginRequest;
import com.pcmall.entity.AdminUser;
import com.pcmall.mapper.AdminUserMapper;
import com.pcmall.security.LoginAttemptGuard;
import com.pcmall.service.AdminService;
import com.pcmall.vo.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AdminServiceImpl implements AdminService {
    private final AdminUserMapper adminUserMapper; private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; private final LoginAttemptGuard loginAttemptGuard;
    public AdminServiceImpl(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder, JwtService jwtService, LoginAttemptGuard loginAttemptGuard) {
        this.adminUserMapper = adminUserMapper; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.loginAttemptGuard = loginAttemptGuard;
    }
    @Override public LoginResponse login(LoginRequest request, String clientIp) {
        loginAttemptGuard.check(clientIp);
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptGuard.failure(clientIp);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) throw new BusinessException(HttpStatus.FORBIDDEN, "管理员账户已停用");
        loginAttemptGuard.success(clientIp);
        user.setLastLoginIp(clientIp); user.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(user);
        return new LoginResponse(jwtService.createToken(user), jwtService.expirationSeconds());
    }
}
