package com.safetyparis.safetyparis_api.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인한 회원만 제보 가능 - 작성자 필수
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // 승인 시 Marker로 그대로 옮겨지는 필드 - Marker와 동일하게 인상착의는 전부 선택 입력(nullable)
    private String ageGroup;

    private String gender;

    private String clothing;

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
    public Report(User user, Double latitude, Double longitude, String ageGroup, String gender, String clothing,
                  String race, Integer headCount, String height, String build, Boolean hasBeard, Boolean hasGlasses,
                  String locationDescription, String stolenItems, String storyContent) {
        this.user = user;
        this.status = ReportStatus.PENDING;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.clothing = clothing;
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

    public void approve(){
        validatePending();// 상태(PENDING -> APPROVED)
        this.status = ReportStatus.APPROVED;
    }

    public void reject(){ // t상태(PENDING -> REJECTED)
        validatePending();
        this.status = ReportStatus.REJECTED;
    }

    private void validatePending(){
        if(this.status != ReportStatus.PENDING){
            throw new IllegalArgumentException("이미 처리된 제보입니다.");
        }
    }
}
