package com.safetyparis.safetyparis_api.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.NoSuchElementException;

// ResponseEntityExceptionHandler를 상속해 Spring MVC 기본 예외(404 없는 주소, 405, 415, 경로 변수 타입 오류 등)도
// Spring Boot 기본 형식 대신 ErrorResponse로 응답
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    // 마지막 안전망 - 예상 못 한 예외(DB 제약 위반, Redis 연결 실패 등)는 원인을 로그로만 남기고 내부 정보는 응답에 넣지 않음
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        logger.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("서버 오류가 발생했습니다."));
    }

    // @Valid 검증 실패 - DTO에 적은 메시지("이메일은 필수입니다")를 그대로 전달
    // ponytail: 여러 필드가 동시에 틀려도 첫 번째 메시지만 보냄, 필드별로 보여줄 화면이 생기면 목록으로 확장
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return handleExceptionInternal(ex, new ErrorResponse(message), headers, status, request);
    }

    // JSON 문법 오류, 타입 불일치 등 요청 본문을 읽지 못한 경우 - 내부 파싱 메시지는 노출하지 않음
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorResponse("요청 형식이 올바르지 않습니다."), headers, status, request);
    }

    // 부모 클래스가 처리하는 모든 예외가 거치는 곳 - 상태 코드는 그대로 두고 본문만 ErrorResponse로 교체
    // 메시지는 고정 문구 (예외 메시지에는 요청 경로, 클래스명 같은 내부 정보가 들어 있음)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (!(body instanceof ErrorResponse)) {
            if (statusCode.is5xxServerError()) {
                logger.error("Spring MVC 처리 중 서버 오류", ex);
            }
            body = new ErrorResponse(switch (statusCode.value()) {
                case 400 -> "잘못된 요청입니다.";
                case 404 -> "요청한 주소를 찾을 수 없습니다.";
                case 405 -> "지원하지 않는 HTTP 메서드입니다.";
                case 406 -> "지원하지 않는 응답 형식입니다.";
                case 415 -> "지원하지 않는 Content-Type입니다.";
                default -> statusCode.is5xxServerError() ? "서버 오류가 발생했습니다." : "요청을 처리할 수 없습니다.";
            });
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
