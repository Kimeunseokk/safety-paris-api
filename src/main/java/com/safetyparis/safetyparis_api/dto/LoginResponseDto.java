package com.safetyparis.safetyparis_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private UserResponseDto user;
    private String accessToken;
    private String refreshToken;
}
