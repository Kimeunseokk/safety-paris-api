package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.AdminResponseDto;
import com.safetyparis.safetyparis_api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final ReportService reportService;

    @GetMapping // 승인대기 목록
    public ResponseEntity<List<AdminResponseDto>> getPendingList() {
        return ResponseEntity.ok(reportService.getPendingList());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<AdminResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.approveReport(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<AdminResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.rejectReport(id));
    }
}
