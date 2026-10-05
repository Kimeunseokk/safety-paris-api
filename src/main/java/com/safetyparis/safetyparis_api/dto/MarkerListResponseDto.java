package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.Marker;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

// 마커 목록은 MarkerService.getMarkerList()에서 Redis에 캐싱됨 - 기본 캐시 저장 방식(Java 직렬화)이라 Serializable 필요
@Getter
public class MarkerListResponseDto implements Serializable {
    private final Long id;
    private final Double latitude;
    private final Double longitude;
    private final LocalDateTime createdAt;

    public MarkerListResponseDto(Marker marker) {
        this.id = marker.getId();
        this.latitude = marker.getLatitude();
        this.longitude = marker.getLongitude();
        this.createdAt = marker.getCreatedAt();
    }
}
