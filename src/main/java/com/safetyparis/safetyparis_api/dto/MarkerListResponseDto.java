package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.Marker;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MarkerListResponseDto {
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
