package com.safetyparis.safetyparis_api.entity;

// 제보 처리 상태. 흐름: PENDING → (관리자 검토) → APPROVED 또는 REJECTED
public enum ReportStatus {
    PENDING,   // 승인 대기 - 제보 등록 시 기본값. 관리자 검토 전이라 지도에 노출되지 않음
    APPROVED,  // 승인 - 관리자가 사실로 판단해 Marker로 전환됨 (지도에 노출)
    REJECTED;  // 거절 - 장난성/허위 제보 등으로 관리자가 걸러냄 (지도에 노출되지 않음)
}
