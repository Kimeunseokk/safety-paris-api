package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.AdminResponseDto;
import com.safetyparis.safetyparis_api.dto.ReportCreateRequestDto;
import com.safetyparis.safetyparis_api.dto.ReportResponseDto;
import com.safetyparis.safetyparis_api.entity.Marker;
import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.ReportStatus;
import com.safetyparis.safetyparis_api.entity.User;
import com.safetyparis.safetyparis_api.repository.MarkerRepository;
import com.safetyparis.safetyparis_api.repository.ReportRepository;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MarkerRepository markerRepository;

    @Transactional
    public ReportResponseDto createReport(String email,ReportCreateRequestDto reportCreateRequestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 사용자입니다"));
        Report report = reportRepository.save(reportCreateRequestDto.toEntity(user));
        return new ReportResponseDto(report);
    }

    // 처리 진행중 목록 조회
    public List<AdminResponseDto> getPendingList() {
        return reportRepository.findByStatus(ReportStatus.PENDING).stream()
                .map(AdminResponseDto::new)
                .toList();
    }

    // 승인하기 - 상태 변경 후 Report 값을 복사한 Marker 저장(지도 노출)
    // approve()가 먼저 PENDING 검사를 하므로 이미 처리된 제보는 Marker가 중복 생성되지 않음
    @Transactional
    @CacheEvict(value = "markers" , allEntries = true)
    public AdminResponseDto approveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 제보입니다. id=" + reportId));
        report.approve();
        markerRepository.save(Marker.from(report));
        return new AdminResponseDto(report);
    }

    // 승인 거절하기 - 상태만 변경 (Marker 생성 없음)
    @Transactional
    public AdminResponseDto rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 제보입니다. id=" + reportId));
        report.reject();
        return new AdminResponseDto(report);
    }
}
