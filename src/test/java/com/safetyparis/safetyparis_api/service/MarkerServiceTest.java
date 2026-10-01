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
import static org.mockito.BDDMockito.given;

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
}
