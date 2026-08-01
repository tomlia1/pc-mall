package com.pcmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.pcmall.mapper")
public class PcMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(PcMallApplication.class, args);
    }
}
