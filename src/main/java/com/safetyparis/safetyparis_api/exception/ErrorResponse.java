package com.safetyparis.safetyparis_api.exception;

// 모든 에러 응답의 공통 형식 {"message": "..."} - 프론트는 항상 res.json().message로 꺼내면 됨
// (SecurityConfig의 401/403 응답도 같은 형식)
public record ErrorResponse(String message) {
}
