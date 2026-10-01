package com.safetyparis.safetyparis_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "markers")
@Getter
@NoArgsConstructor
public class Marker extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // 아래부터는 설계서 기준 인상착의 관련 필드 - 사용자가 기억 못 할 수 있어 전부 선택 입력(nullable)
    private String ageGroup;

    private String gender;

    private String race;

    private Integer headCount;

    private String height;

    private String build;

    private Boolean hasBeard;

    private Boolean hasGlasses;

    @Column(nullable = false)
    private String locationDescription;

    private String stolenItems;

    @Column(nullable = false)
    private String storyContent;

    @Builder
    public Marker(Double latitude, Double longitude, String ageGroup, String gender, String race,
                  Integer headCount, String height, String build, Boolean hasBeard, Boolean hasGlasses,
                  String locationDescription, String stolenItems, String storyContent) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.race = race;
        this.headCount = headCount;
        this.height = height;
        this.build = build;
        this.hasBeard = hasBeard;
        this.hasGlasses = hasGlasses;
        this.locationDescription = locationDescription;
        this.stolenItems = stolenItems;
        this.storyContent = storyContent;
    }
}
