package com.safetyparis.safetyparis_api.dto;

import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.ReportStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponseDto {
    private final Long reportId;
    private final ReportStatus status;
    private final LocalDateTime createdAt;

    public ReportResponseDto(Report report) {
        this.reportId = report.getId();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
    }
}
