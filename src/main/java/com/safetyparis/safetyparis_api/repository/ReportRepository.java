package com.safetyparis.safetyparis_api.repository;


import com.safetyparis.safetyparis_api.entity.Report;
import com.safetyparis.safetyparis_api.entity.ReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = "user")
    List<Report> findByStatus(ReportStatus status);
}
