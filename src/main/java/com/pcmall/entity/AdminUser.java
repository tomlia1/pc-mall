package com.pcmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("admin_user")
public class AdminUser {
    private Long id; private String username; private String passwordHash; private Integer status;
    private String lastLoginIp; private LocalDateTime lastLoginAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
    public String getLastLoginIp() { return lastLoginIp; } public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; } public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
