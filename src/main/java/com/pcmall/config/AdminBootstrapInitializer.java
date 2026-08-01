package com.pcmall.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcmall.entity.AdminUser;
import com.pcmall.mapper.AdminUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class AdminBootstrapInitializer {
    @Bean
    ApplicationRunner bootstrapAdmin(AdminUserMapper mapper, PasswordEncoder passwordEncoder,
            @Value("${ADMIN_INITIAL_USERNAME:admin}") String username,
            @Value("${ADMIN_INITIAL_PASSWORD:}") String password) {
        return args -> {
            if (!StringUtils.hasText(password) || mapper.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username)) != null) return;
            AdminUser admin = new AdminUser(); admin.setUsername(username); admin.setPasswordHash(passwordEncoder.encode(password)); admin.setStatus(1);
            mapper.insert(admin);
        };
    }
}
