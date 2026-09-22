package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.UserLoginRequestDto;
import com.safetyparis.safetyparis_api.dto.UserSignUpRequestDto;
import com.safetyparis.safetyparis_api.dto.UserResponseDto;
import com.safetyparis.safetyparis_api.entity.User;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional // 회원가입 기능
    public UserResponseDto signUpUser(UserSignUpRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다. email=" + requestDto.getEmail());
        }
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        User savedUser = userRepository.save(requestDto.toEntity(encodedPassword));
        return new UserResponseDto(savedUser);
    }

    @Transactional
    public UserResponseDto login(UserLoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다. 입력한 정보를 다시 확인해 주세요."));
        if(!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다. 입력한 정보를 다시 확인해 주세요.");
        }
        return new UserResponseDto(user);
    }

    public UserResponseDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + id));
        return new UserResponseDto(user);
    }
}
