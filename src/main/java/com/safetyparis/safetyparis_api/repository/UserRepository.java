package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
