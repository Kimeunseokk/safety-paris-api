package com.safetyparis.safetyparis_api.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "help_locations")
@Getter
@NoArgsConstructor
public class HelpLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HelpLocationType type;

    @Builder
    public HelpLocation(String name, Double latitude, Double longitude, String phoneNumber, HelpLocationType type) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.type = type;
    }
}
