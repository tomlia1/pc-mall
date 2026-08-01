package com.pcmall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secretBase64;
    private long expirationSeconds = 7200;
    public String getSecretBase64() { return secretBase64; }
    public void setSecretBase64(String secretBase64) { this.secretBase64 = secretBase64; }
    public long getExpirationSeconds() { return expirationSeconds; }
    public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
}
