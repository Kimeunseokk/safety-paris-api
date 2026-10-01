package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.MarkerDetailResponseDto;
import com.safetyparis.safetyparis_api.dto.MarkerListResponseDto;
import com.safetyparis.safetyparis_api.service.MarkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/markers")
@RequiredArgsConstructor
public class MarkerController {

    private final MarkerService markerService;

    @GetMapping("/{id}")
    public ResponseEntity<MarkerDetailResponseDto> markerDetail(@PathVariable Long id) {
        return ResponseEntity.ok(markerService.getMarkerDetail(id));
    }

    @GetMapping
    public ResponseEntity<List<MarkerListResponseDto>> markerList() {
        return ResponseEntity.ok(markerService.getMarkerList());
    }
}
