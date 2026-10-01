package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.MarkerDetailResponseDto;
import com.safetyparis.safetyparis_api.dto.MarkerListResponseDto;
import com.safetyparis.safetyparis_api.entity.Marker;
import com.safetyparis.safetyparis_api.repository.MarkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkerService {
    private final MarkerRepository markerRepository;

    public MarkerDetailResponseDto getMarkerDetail(Long id) {
        Marker marker = markerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 마커입니다. id=" + id));
        return new  MarkerDetailResponseDto(marker);
    }

    public List<MarkerListResponseDto> getMarkerList(){
        return markerRepository.findAll().stream()
                .map(MarkerListResponseDto::new)
                .toList();
    }
}
