package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.HelpLocation;
import com.safetyparis.safetyparis_api.entity.HelpLocationType;
import lombok.Getter;

@Getter
public class HelpLocationResponseDto {
    private final Long id;
    private final String name;
    private final Double latitude;
    private final Double longitude;
    private final String phoneNumber;
    private final HelpLocationType type;

    public HelpLocationResponseDto(HelpLocation helpLocation) {
        this.id = helpLocation.getId();
        this.name = helpLocation.getName();
        this.latitude = helpLocation.getLatitude();
        this.longitude = helpLocation.getLongitude();
        this.phoneNumber = helpLocation.getPhoneNumber();
        this.type = helpLocation.getType();
    }
}
