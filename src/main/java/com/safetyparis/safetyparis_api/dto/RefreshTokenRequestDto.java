package com.safetyparis.safetyparis_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {
    private String token; // refeshtoken이 들어갈 그릇
}
