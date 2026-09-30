package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.entity.HelpLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestBody;


public interface HelpLocationRepository extends JpaRepository<HelpLocation, Long> {
}
