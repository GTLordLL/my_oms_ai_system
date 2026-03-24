package com.erp.oms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Map;

public class JwtUtils {

    // 生产环境建议放在 application.yml 中
    private static final String SECRET = "your_super_secret_key_make_it_very_long_for_security_2026";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    // 将过期时间设置为当前时间 + 100年（以毫秒计算）
    private static final long EXPIRE = System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000);

    /**
     * 生成 Token
     */
    public static String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token（后续拦截器会用到）
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}