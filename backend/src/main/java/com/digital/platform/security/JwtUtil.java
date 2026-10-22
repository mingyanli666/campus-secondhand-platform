package com.digital.platform.security;

import com.digital.platform.config.PlatformProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析 token。
 * <p>
 * 为什么用 JWT 而不是 Session？前后端分离 + 多端场景下，服务端无状态更易横向扩展，
 * 前端只要带着 token 就能访问，也不存在跨域 Cookie 的麻烦。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_NICKNAME = "nickname";
    private static final String CLAIM_ROLE = "role";

    private final PlatformProperties properties;

    private SecretKey key() {
        byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 签发 token。
     */
    public String generateToken(Long userId, String username, String nickname, String role) {
        long expireMillis = properties.getJwt().getExpireHours() * 60L * 60L * 1000L;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_NICKNAME, nickname)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key())
                .compact();
    }

    /**
     * 解析 token，失败返回 null（过期、篡改、格式错误都算失败）。
     * 不抛异常是为了让拦截器逻辑更直白。
     */
    public LoginUser parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(claims.getSubject()));
            user.setUsername(claims.get(CLAIM_USERNAME, String.class));
            user.setNickname(claims.get(CLAIM_NICKNAME, String.class));
            user.setRole(claims.get(CLAIM_ROLE, String.class));
            return user;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
