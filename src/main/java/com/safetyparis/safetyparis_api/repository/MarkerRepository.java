package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.entity.Marker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkerRepository extends JpaRepository<Marker, Long> {
}
