package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.entity.Marker;
import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.ReportStatus;
import com.safetyparis.safetyparis_api.entity.User;
import com.safetyparis.safetyparis_api.repository.MarkerRepository;
import com.safetyparis.safetyparis_api.repository.ReportRepository;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MarkerRepository markerRepository;

    private Report pendingReport() {
        User user = User.builder().email("reporter@test.com").password("encoded").nickname("제보자").build();
        return Report.builder()
                .user(user)
                .latitude(48.8584)
                .longitude(2.2945)
                .locationDescription("에펠탑 근처")
                .storyContent("지갑 소매치기")
                .build();
    }

    @Test
    @DisplayName("제보 승인 - 상태가 APPROVED로 바뀌고 Marker가 저장된다")
    void approveReport_changesStatusAndSavesMarker() {
        Report report = pendingReport();
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        reportService.approveReport(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
        verify(markerRepository).save(any(Marker.class));
    }

    @Test
    @DisplayName("제보 거절 - 상태가 REJECTED로 바뀌고 Marker는 저장되지 않는다")
    void rejectReport_changesStatusWithoutMarker() {
        Report report = pendingReport();
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        reportService.rejectReport(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        verify(markerRepository, never()).save(any(Marker.class));
    }

    @Test
    @DisplayName("이미 승인된 제보를 다시 승인하면 예외가 나고 Marker가 중복 저장되지 않는다")
    void approveReport_alreadyProcessed_throwsAndNoDuplicateMarker() {
        Report report = pendingReport();
        report.approve();
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.approveReport(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 제보입니다");

        verify(markerRepository, never()).save(any(Marker.class));
    }
}
