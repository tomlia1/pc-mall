package com.pcmall.service.impl;

import com.pcmall.common.BusinessException;
import com.pcmall.config.JwtService;
import com.pcmall.dto.LoginRequest;
import com.pcmall.entity.AdminUser;
import com.pcmall.mapper.AdminUserMapper;
import com.pcmall.security.LoginAttemptGuard;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminServiceImplTest {
    @Test
    void rejectsIncorrectRawPasswordAgainstBcryptHash() {
        AdminUser user = new AdminUser(); user.setUsername("admin"); user.setStatus(1); user.setPasswordHash(new BCryptPasswordEncoder().encode("correct-password"));
        AdminUserMapper mapper = mock(AdminUserMapper.class); when(mapper.selectOne(any())).thenReturn(user);
        AdminServiceImpl service = new AdminServiceImpl(mapper, new BCryptPasswordEncoder(), mock(JwtService.class), new LoginAttemptGuard());
        BusinessException exception = assertThrows(BusinessException.class, () -> service.login(new LoginRequest("admin", "wrong-password"), "127.0.0.1"));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void returnsTokenAfterCorrectRawPasswordAgainstBcryptHash() {
        AdminUser user = new AdminUser(); user.setId(7L); user.setUsername("admin"); user.setStatus(1); user.setPasswordHash(new BCryptPasswordEncoder().encode("correct-password"));
        AdminUserMapper mapper = mock(AdminUserMapper.class); when(mapper.selectOne(any())).thenReturn(user);
        JwtService jwtService = mock(JwtService.class); when(jwtService.createToken(user)).thenReturn("signed-token"); when(jwtService.expirationSeconds()).thenReturn(7200L);
        AdminServiceImpl service = new AdminServiceImpl(mapper, new BCryptPasswordEncoder(), jwtService, new LoginAttemptGuard());
        var response = service.login(new LoginRequest("admin", "correct-password"), "127.0.0.1");
        assertEquals("signed-token", response.token());
        assertEquals(7200L, response.expiresIn());
    }
}
