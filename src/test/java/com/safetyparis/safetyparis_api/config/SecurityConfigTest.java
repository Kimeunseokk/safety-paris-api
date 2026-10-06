package com.safetyparis.safetyparis_api.config;

import com.safetyparis.safetyparis_api.service.MarkerService;
import com.safetyparis.safetyparis_api.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig의 URL별 접근 규칙(공개 / 로그인 필요 / ADMIN 필요)을 실제 요청으로 확인.
// 서비스는 Mock으로 바꿔 DB 데이터와 상관없이 "막히는지/통과하는지"만 본다.
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;
    @MockBean
    private MarkerService markerService;

    private static final String REPORT_JSON = """
            {"latitude": 48.85, "longitude": 2.29, "locationDescription": "에펠탑", "storyContent": "소매치기"}
            """;

    // JwtAuthFilter가 등록하는 것과 같은 형태(principal = email 문자열, 권한 = ROLE_xxx)의 로그인 사용자
    private static RequestPostProcessor loginAs(String email, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    @DisplayName("공개 경로 - 마커 목록/상세는 로그인 없이 조회 가능")
    void publicPaths_noLogin_ok() throws Exception {
        mockMvc.perform(get("/api/markers")).andExpect(status().isOk());
        mockMvc.perform(get("/api/markers/1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 필요 경로 - 토큰 없이 제보 등록하면 401")
    void protectedPath_noLogin_401() throws Exception {
        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(REPORT_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("로그인하면 제보 등록 가능하고, 컨트롤러는 principal의 email을 받는다")
    void protectedPath_loggedIn_201_withEmail() throws Exception {
        mockMvc.perform(post("/api/reports").with(loginAs("reporter@test.com", "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(REPORT_JSON))
                .andExpect(status().isCreated());

        verify(reportService).createReport(eq("reporter@test.com"), any());
    }

    @Test
    @DisplayName("관리자 경로 - 토큰 없으면 401")
    void adminPath_noLogin_401() throws Exception {
        mockMvc.perform(get("/api/admin/reports")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자 경로 - USER 권한이면 403")
    void adminPath_user_403() throws Exception {
        mockMvc.perform(get("/api/admin/reports").with(loginAs("user@test.com", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("관리자 권한이 필요합니다."));
        mockMvc.perform(patch("/api/admin/reports/1/approve").with(loginAs("user@test.com", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 경로 - 세미콜론(;)으로 주소를 변형해도 USER는 승인 못 함 (직접 구현 필터 시절 우회되던 경로)")
    void adminPath_semicolonBypass_blocked() throws Exception {
        // 전환 전 필터는 startsWith("/api/admin/")로 비교해 이 주소를 관리자 API로 못 알아봤고,
        // Spring MVC는 ;x=1을 무시하고 관리자 컨트롤러로 보내서 일반 회원이 승인할 수 있었음
        mockMvc.perform(patch("/api/admin;x=1/reports/1/approve").with(loginAs("user@test.com", "USER")))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).approveReport(any());
    }

    @Test
    @DisplayName("관리자 경로 - ADMIN 권한이면 통과")
    void adminPath_admin_ok() throws Exception {
        mockMvc.perform(get("/api/admin/reports").with(loginAs("admin@test.com", "ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/reports/1/approve").with(loginAs("admin@test.com", "ADMIN")))
                .andExpect(status().isOk());
    }
}
