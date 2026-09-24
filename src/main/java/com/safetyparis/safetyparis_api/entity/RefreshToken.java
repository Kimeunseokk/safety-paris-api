package com.safetyparis.safetyparis_api.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@AllArgsConstructor
@RedisHash("refreshToken")
public class RefreshToken {

    @Id
    private String email;

    private String token;

    @TimeToLive // 초 단위 - 이 시간이 지나면 Redis가 자동으로 삭제해줌
    private Long expiration;
}
