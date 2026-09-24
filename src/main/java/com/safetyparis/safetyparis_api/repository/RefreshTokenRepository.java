package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
