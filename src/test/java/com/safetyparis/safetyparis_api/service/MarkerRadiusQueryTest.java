package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.MarkerListResponseDto;
import com.safetyparis.safetyparis_api.entity.Marker;
import com.safetyparis.safetyparis_api.repository.MarkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 반경 조회 통합 테스트 - 실제 MySQL의 BETWEEN + ST_Distance_Sphere로 확인 (Mock으로는 SQL을 검증할 수 없음)
// @Transactional: 테스트가 끝나면 넣은 마커를 롤백해 DB에 남기지 않음
@SpringBootTest
@Transactional
class MarkerRadiusQueryTest {

    // 기준점: 파리 시청
    private static final double LAT = 48.8566;
    private static final double LNG = 2.3522;
    // 서비스 상수를 쓰지 않고 독립적으로 계산 (서비스 상수가 바뀌면 테스트가 잡도록)
    private static final double M_PER_DEG = 6_370_986 * Math.PI / 180;

    @Autowired MarkerService markerService;
    @Autowired MarkerRepository markerRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long north(double meters) {
        return save(LAT + meters / M_PER_DEG, LNG);
    }

    private Long east(double meters) {
        return save(LAT, LNG + meters / (M_PER_DEG * Math.cos(Math.toRadians(LAT))));
    }

    private Long save(double lat, double lng) {
        return markerRepository.saveAndFlush(Marker.builder().latitude(lat).longitude(lng)
                .locationDescription("반경 테스트").storyContent("반경 테스트").build()).getId();
    }

    private List<Long> findIds(int radius) {
        return markerService.getMarkersWithinRadius(LAT, LNG, radius).stream()
                .map(MarkerListResponseDto::getId).toList();
    }

    @Test
    @DisplayName("3km 조회 - 1km, 2.9km 마커만 나오고 3.1km, 10km 마커는 제외")
    void withinRadius_includesOnlyInside() {
        Long m1km = north(1_000), m2_9km = north(2_900), m3_1km = north(3_100), m10km = north(10_000);

        assertThat(findIds(3_000)).contains(m1km, m2_9km).doesNotContain(m3_1km, m10km);
    }

    @Test
    @DisplayName("3km 조회 - 사각형 대각선 모서리(원 밖, 사각형 안) 마커는 2단계 거리 계산에서 제외")
    void withinRadius_excludesBoxCorner() {
        // 북동쪽으로 각 2.5km → 실제 거리 약 3.54km: BETWEEN 사각형은 통과하지만 원 밖
        Long corner = save(LAT + 2_500 / M_PER_DEG, LNG + 2_500 / (M_PER_DEG * Math.cos(Math.toRadians(LAT))));

        assertThat(findIds(3_000)).doesNotContain(corner);
    }

    @Test
    @DisplayName("3km 조회 - 정북·정동 2,998m 경계 마커도 포함 (사각형과 거리 계산의 지구 반지름 기준 일치 검증)")
    void withinRadius_includesNearBoundary() {
        // 111320m/도로 사각형을 만들면 반변이 약 2,996.6m라 아래 두 마커가 1단계에서 빠짐 - 이 버그를 잡는 테스트
        // 정동은 경도 방향 cos(위도) 계산까지 검증
        Long north = north(2_998), east = east(2_998);

        // 전제 확인: 두 마커가 MySQL 기준으로 실제 3km 원 안에 있음
        for (Long id : List.of(north, east)) {
            Double distance = jdbcTemplate.queryForObject(
                    "SELECT ST_Distance_Sphere(POINT(longitude, latitude), POINT(?, ?)) FROM markers WHERE id = ?",
                    Double.class, LNG, LAT, id);
            assertThat(distance).isBetween(2_990.0, 3_000.0);
        }

        assertThat(findIds(3_000)).contains(north, east);
    }
}
