package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 중복 체크
    boolean existsByEmail(String email);
    // db에서 암호화된 password을 가져오기 위해 findByEmail을 따로 구성
    Optional<User> findByEmail(String email);

}
