package com.pcmall.config;

import com.pcmall.entity.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {
    private final JwtProperties properties;
    private SecretKey signingKey;
    public JwtService(JwtProperties properties) { this.properties = properties; }
    @PostConstruct
    void initialize() {
        if (!StringUtils.hasText(properties.getSecretBase64())) throw new IllegalStateException("JWT_SECRET_BASE64 must be configured");
        byte[] key = Decoders.BASE64.decode(properties.getSecretBase64());
        if (key.length < 32) throw new IllegalStateException("JWT secret must be at least 32 bytes after Base64 decoding");
        signingKey = Keys.hmacShaKeyFor(key);
    }
    public String createToken(AdminUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(user.getId())).claim("username", user.getUsername())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.getExpirationSeconds())))
                .signWith(signingKey).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload(); }
    public long expirationSeconds() { return properties.getExpirationSeconds(); }
}
