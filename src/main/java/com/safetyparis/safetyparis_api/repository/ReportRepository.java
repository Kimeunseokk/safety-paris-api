package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.dto.ReportResponseDto;
import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(ReportStatus status);
    Optional<Report> findById(Long id);
}
