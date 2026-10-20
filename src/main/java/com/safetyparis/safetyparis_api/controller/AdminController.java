package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.AdminResponseDto;
import com.safetyparis.safetyparis_api.dto.UserResponseDto;
import com.safetyparis.safetyparis_api.service.ReportService;
import com.safetyparis.safetyparis_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 관리자 API 모음 - /api/admin/** 는 SecurityConfig에서 ADMIN만 접근 가능
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ReportService reportService;
    private final UserService userService;

    @GetMapping("/reports") // 승인대기 목록
    public ResponseEntity<List<AdminResponseDto>> getPendingList() {
        return ResponseEntity.ok(reportService.getPendingList());
    }

    @PatchMapping("/reports/{id}/approve")
    public ResponseEntity<AdminResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.approveReport(id));
    }

    @PatchMapping("/reports/{id}/reject")
    public ResponseEntity<AdminResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.rejectReport(id));
    }

    // 응답에 이메일이 포함되므로 관리자만 조회 가능 - 로그인만 확인하면 id를 바꿔가며 전체 회원 이메일이 노출됨(IDOR)
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
}
