package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.ReportStatus;
import lombok.Getter;

import java.time.LocalDateTime;

// 관리자 제보 검토용 응답 - 승인/거절 판단에 필요한 제보 내용 전체 + 작성자 정보
// report.getUser()가 LAZY 로딩이라 반드시 트랜잭션 안(서비스)에서 생성해야 함
@Getter
public class AdminResponseDto {
    private final Long reportId;
    private final ReportStatus status;
    private final String reporterNickname;
    private final String reporterEmail;
    private final Double latitude;
    private final Double longitude;
    private final String ageGroup;
    private final String gender;
    private final String clothing;
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

    public AdminResponseDto(Report report) {
        this.reportId = report.getId();
        this.status = report.getStatus();
        this.reporterNickname = report.getUser().getNickname();
        this.reporterEmail = report.getUser().getEmail();
        this.latitude = report.getLatitude();
        this.longitude = report.getLongitude();
        this.ageGroup = report.getAgeGroup();
        this.gender = report.getGender();
        this.clothing = report.getClothing();
        this.race = report.getRace();
        this.headCount = report.getHeadCount();
        this.height = report.getHeight();
        this.build = report.getBuild();
        this.hasBeard = report.getHasBeard();
        this.hasGlasses = report.getHasGlasses();
        this.locationDescription = report.getLocationDescription();
        this.stolenItems = report.getStolenItems();
        this.storyContent = report.getStoryContent();
        this.createdAt = report.getCreatedAt();
    }
}
