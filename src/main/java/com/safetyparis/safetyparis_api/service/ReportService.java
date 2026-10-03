package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.ReportCreateRequestDto;
import com.safetyparis.safetyparis_api.dto.ReportResponseDto;
import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.User;
import com.safetyparis.safetyparis_api.repository.ReportRepository;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReportResponseDto createReport(String email,ReportCreateRequestDto reportCreateRequestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 사용자입니다"));
        Report report = reportRepository.save(reportCreateRequestDto.toEntity(user));
        return new ReportResponseDto(report);
    }
}
