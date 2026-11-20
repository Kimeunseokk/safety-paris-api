package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.MarkerDetailResponseDto;
import com.safetyparis.safetyparis_api.dto.MarkerListResponseDto;
import com.safetyparis.safetyparis_api.service.MarkerService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
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

    // 사용자 위치 기준 반경 내 마커 조회 (전체 목록 조회를 대체)
    // 파라미터 검증은 Spring 6.1 기본 메서드 검증이 처리 → 실패 시 GlobalExceptionHandler가 400 JSON으로 응답
    // (클래스에 @Validated를 붙이면 AOP 방식 ConstraintViolationException으로 바뀌어 500 처리되므로 붙이지 않음)
    @GetMapping
    public ResponseEntity<List<MarkerListResponseDto>> markerList(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue = "3000") @Max(5000) int radius) {
        return ResponseEntity.ok(markerService.getMarkersWithinRadius(lat, lng, radius));
    }
}
