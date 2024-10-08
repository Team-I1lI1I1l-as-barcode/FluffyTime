package com.fluffytime.domain.user.dao;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PasswordChangeDao {

    private static final String PASSWORD_KEY_HEADER = "change_password:";
    private static final int PASSWORD_EXPIRY = 300;

    private final RedisTemplate<String, String> redisTemplate;

    // 비밀번호 변경 ttl 저장하는 메서드
    public void saveChangePasswordTtl(String email) {
        redisTemplate.opsForValue().set(
            PASSWORD_KEY_HEADER + email,
            email,
            Duration.ofSeconds(PASSWORD_EXPIRY)
        );
    }

    // key(email)이 존재하는지 확인하는 메서드
    public boolean hasKey(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PASSWORD_KEY_HEADER + email));
    }

    // 비밀번호 변경 ttl 제거하는 메서드
    public void removePasswordChangeTtl(String email) {
        redisTemplate.delete(PASSWORD_KEY_HEADER + email);
    }
}
