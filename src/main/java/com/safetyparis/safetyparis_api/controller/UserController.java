package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.UserLoginRequestDto;
import com.safetyparis.safetyparis_api.dto.UserSignUpRequestDto;
import com.safetyparis.safetyparis_api.dto.UserResponseDto;
import com.safetyparis.safetyparis_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDto> signupUser(@RequestBody @Valid UserSignUpRequestDto requestDto) {
        return ResponseEntity.ok(userService.signUpUser(requestDto));
    }
    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> loginUser(@RequestBody @Valid UserLoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(userService.login(loginRequestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
}
