package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.repository.MarkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkerServiceTest {

    @InjectMocks
    private MarkerService markerService;

    @Mock
    private MarkerRepository markerRepository;

    @Test
    @DisplayName("마커 상세 조회 실패 - 존재하지 않는 id면 NoSuchElementException(404)을 던진다")
    void getMarkerDetail_notFound_throwsNoSuchElementException() {
        // given
        given(markerRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> markerService.getMarkerDetail(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("존재하지 않는 마커입니다");
    }

    @Test
    @DisplayName("반경 조회 사각형 계산 - 위도 60도에서 3km면 dLat = 3000/(6,370,986×π/180), 경도 폭은 cos(60°)=0.5라 2배")
    void getMarkersWithinRadius_boundingBox() {
        // 기대값은 서비스 상수를 쓰지 않고 독립적으로 계산 (서비스 상수가 111320 등으로 바뀌면 테스트가 잡도록)
        double dLat = 3000 / (6_370_986 * Math.PI / 180);
        double dLng = dLat * 2;

        markerService.getMarkersWithinRadius(60, 10, 3000);

        verify(markerRepository).findWithinRadius(eq(60.0), eq(10.0), eq(3000),
                doubleThat(v -> Math.abs(v - (60 - dLat)) < 1e-7),
                doubleThat(v -> Math.abs(v - (60 + dLat)) < 1e-7),
                doubleThat(v -> Math.abs(v - (10 - dLng)) < 1e-7),
                doubleThat(v -> Math.abs(v - (10 + dLng)) < 1e-7));
    }
}
