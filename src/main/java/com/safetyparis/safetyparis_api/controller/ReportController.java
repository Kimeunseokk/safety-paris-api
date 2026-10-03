package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.ReportCreateRequestDto;
import com.safetyparis.safetyparis_api.dto.ReportResponseDto;
import com.safetyparis.safetyparis_api.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    // email은 JwtAuthFilter가 토큰에서 꺼내 request attribute로 넣어준 값
    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(@RequestAttribute("email") String email,
                                                          @RequestBody @Valid ReportCreateRequestDto reportCreateRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(email, reportCreateRequestDto));
    }
}
