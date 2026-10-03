package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 제보 등록 요청. user(작성자)는 토큰에서, status는 엔티티에서 PENDING으로 정하므로 받지 않음
// 문자열은 DB 기본 컬럼 길이(255)를 넘으면 저장 시 500이 나므로 @Size로 먼저 400 처리
@Getter
@NoArgsConstructor
public class ReportCreateRequestDto {

    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    // 인상착의 - 기억 못 할 수 있어 전부 선택 입력
    @Size(max = 255)
    private String ageGroup;

    @Size(max = 255)
    private String gender;

    @Size(max = 255)
    private String clothing;

    @Size(max = 255)
    private String race;

    @Positive(message = "인원은 1명 이상이어야 합니다")
    private Integer headCount;

    @Size(max = 255)
    private String height;

    @Size(max = 255)
    private String build;

    private Boolean hasBeard;

    private Boolean hasGlasses;

    @NotBlank(message = "위치 설명은 필수입니다")
    @Size(max = 255)
    private String locationDescription;

    @Size(max = 255)
    private String stolenItems;

    @NotBlank(message = "제보 내용은 필수입니다")
    @Size(max = 255)
    private String storyContent;

    public Report toEntity(User user) {
        return Report.builder()
                .user(user)
                .latitude(latitude)
                .longitude(longitude)
                .ageGroup(ageGroup)
                .gender(gender)
                .clothing(clothing)
                .race(race)
                .headCount(headCount)
                .height(height)
                .build(build)
                .hasBeard(hasBeard)
                .hasGlasses(hasGlasses)
                .locationDescription(locationDescription)
                .stolenItems(stolenItems)
                .storyContent(storyContent)
                .build();
    }
}
