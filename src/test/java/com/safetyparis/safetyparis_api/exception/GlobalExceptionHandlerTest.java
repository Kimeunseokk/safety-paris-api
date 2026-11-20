package com.safetyparis.safetyparis_api.exception;

import com.safetyparis.safetyparis_api.controller.MarkerController;
import com.safetyparis.safetyparis_api.controller.UserController;
import com.safetyparis.safetyparis_api.service.MarkerService;
import com.safetyparis.safetyparis_api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 모든 예외가 같은 JSON 형식 {"message": ...}으로 나가는지 확인.
// 스프링 전체를 띄우지 않고 컨트롤러 + GlobalExceptionHandler만 연결해 DB/Redis 없이 실행
class GlobalExceptionHandlerTest {

    private UserService userService;
    private MarkerService markerService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        markerService = mock(MarkerService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService), new MarkerController(markerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("IllegalArgumentException - 400 + JSON 메시지 (이전엔 text/plain 문자열)")
    void illegalArgument_400_json() throws Exception {
        given(userService.login(any())).willThrow(new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"a@test.com\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("NoSuchElementException - 404 + JSON 메시지 (이전엔 text/plain 문자열)")
    void noSuchElement_404_json() throws Exception {
        given(markerService.getMarkerDetail(999L)).willThrow(new NoSuchElementException("존재하지 않는 마커입니다. id=999"));

        mockMvc.perform(get("/api/markers/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("존재하지 않는 마커입니다. id=999"));
    }

    @Test
    @DisplayName("@Valid 검증 실패 - DTO에 적은 메시지가 그대로 전달됨 (이전엔 메시지 없이 \"Bad Request\"만)")
    void validationFailure_400_dtoMessage() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"\", \"password\": \"password123\", \"nickname\": \"닉네임\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일은 필수입니다"));
    }

    @Test
    @DisplayName("깨진 JSON - 400 + 고정 메시지 (내부 파싱 에러는 노출하지 않음)")
    void malformedJson_400_fixedMessage() throws Exception {
        mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("경로 변수 타입 오류 - 400 + JSON 메시지 (이전엔 Spring 기본 형식)")
    void pathVariableTypeMismatch_400_json() throws Exception {
        mockMvc.perform(get("/api/markers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("반경 조회 파라미터 오류(lat 누락, 범위 밖, radius 0·음수·5001, 숫자 아님) - 400 + JSON 메시지")
    void markerRadiusParams_invalid_400_json() throws Exception {
        String[][] invalidParams = {
                {"lng", "2.35"},                                  // lat 누락
                {"lat", "91", "lng", "2.35"},                     // 위도 범위 밖
                {"lat", "48.85", "lng", "-181"},                  // 경도 범위 밖
                {"lat", "48.85", "lng", "-180"},                  // 경도 -180 (ST_Distance_Sphere 허용 범위 (-180, 180] 밖)
                {"lat", "48.85", "lng", "2.35", "radius", "5001"}, // 최대 반경 초과
                {"lat", "48.85", "lng", "2.35", "radius", "0"},    // 반경 0
                {"lat", "48.85", "lng", "2.35", "radius", "-1"},   // 음수 반경
                {"lat", "abc", "lng", "2.35"},                    // 숫자 아님
        };
        for (String[] params : invalidParams) {
            var request = get("/api/markers");
            for (int i = 0; i < params.length; i += 2) {
                request.param(params[i], params[i + 1]);
            }
            mockMvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
        }

        // 경도 180은 ST_Distance_Sphere 허용 범위라 통과
        mockMvc.perform(get("/api/markers").param("lat", "48.85").param("lng", "180"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("없는 주소 - 404 + JSON 메시지 (이전엔 Spring 기본 형식)")
    void unknownPath_404_json() throws Exception {
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }
}
