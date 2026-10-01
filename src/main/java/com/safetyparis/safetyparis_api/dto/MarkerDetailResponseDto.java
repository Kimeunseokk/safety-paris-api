package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.Marker;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MarkerDetailResponseDto {
    private final Long id;
    private final Double latitude;
    private final Double longitude;
    private final String ageGroup;
    private final String gender;
    private final String race;
    private final Integer headCount;
    private final String height;
    private final String build;
    private final Boolean hasBeard;
    private final Boolean hasGlasses;
    private final String locationDescription;
    private final String stolenItems;
    private final String storyContent;
    private final LocalDateTime createdAt;

    public MarkerDetailResponseDto(Marker marker) {
        this.id = marker.getId();
        this.latitude = marker.getLatitude();
        this.longitude = marker.getLongitude();
        this.ageGroup = marker.getAgeGroup();
        this.gender = marker.getGender();
        this.race = marker.getRace();
        this.headCount = marker.getHeadCount();
        this.height = marker.getHeight();
        this.build = marker.getBuild();
        this.hasBeard = marker.getHasBeard();
        this.hasGlasses = marker.getHasGlasses();
        this.locationDescription = marker.getLocationDescription();
        this.stolenItems = marker.getStolenItems();
        this.storyContent = marker.getStoryContent();
        this.createdAt = marker.getCreatedAt();
    }
}
