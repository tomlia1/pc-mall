package com.pcmall.config;

import com.pcmall.entity.AdminUser;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test
    void signsAndParsesAnAdminTokenWithConfiguredExpiry() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretBase64(Base64.getEncoder().encodeToString(new byte[32]));
        properties.setExpirationSeconds(7200);
        JwtService jwtService = new JwtService(properties);
        jwtService.initialize();
        AdminUser user = new AdminUser(); user.setId(9L); user.setUsername("admin");
        String token = jwtService.createToken(user);
        assertEquals("9", jwtService.parse(token).getSubject());
        assertEquals("admin", jwtService.parse(token).get("username", String.class));
    }
}
