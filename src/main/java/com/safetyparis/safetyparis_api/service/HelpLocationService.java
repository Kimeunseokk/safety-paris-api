package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.HelpLocationResponseDto;
import com.safetyparis.safetyparis_api.repository.HelpLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HelpLocationService {
    private final HelpLocationRepository helpLocationRepository;

    public List<HelpLocationResponseDto> getAllHelpLocations() {
        return helpLocationRepository.findAll().stream()
                .map(HelpLocationResponseDto::new)
                .toList();
    }
}
