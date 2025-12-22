package com.ssafy.yumcoach.report.controller;

import com.ssafy.yumcoach.report.model.CreateReportRequest;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.service.ReportService;
import com.ssafy.yumcoach.ai.OpenAiService;
import com.ssafy.yumcoach.ai.ReportAnalysisResult;
import com.ssafy.yumcoach.auth.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    /**
     * 리포트 관련 REST API 컨트롤러
     *
     * - 인증: `Authorization: Bearer ...` 또는 `accessToken` 쿠키에서 토큰을 추출합니다.
     * - 엔드포인트
     *   - POST /api/reports/daily : 사용자 일별 리포트 생성
     *   - GET  /api/reports/daily : 일별 리포트 조회
     *   - POST /api/reports/weekly: 사용자 주간 리포트 생성
     *   - GET  /api/reports/weekly: 주간 리포트 조회
     *   - GET  /api/reports/{id} : 리포트 상세 조회
     */

    private final ReportService reportService;
    private final JwtUtil jwtUtil;
    private final OpenAiService openAiService;

    @PostMapping("/daily")
    public ResponseEntity<?> createDaily(HttpServletRequest request, @RequestBody CreateReportRequest body) {
        /**
         * 사용자 요청으로 일별 리포트 생성
         * - 입력: JSON { date: "yyyy-MM-dd" } (date 없으면 전일)
         * - 반환: 201 + 생성된 ReportDto
         * - 한도 초과 시: 429 반환
         */
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>(); err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            LocalDate date = body.getDate() != null ? LocalDate.parse(body.getDate()) : LocalDate.now().minusDays(1);
            ReportDto dto = reportService.createDailyReport(userId, date);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            if (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().contains("LIMIT_EXCEEDED")) {
                Map<String,String> err = new HashMap<>(); err.put("error","생성 한도를 초과했습니다.");
                return ResponseEntity.status(429).body(err);
            }
            log.error("createDaily error", e);
            Map<String,String> err = new HashMap<>(); err.put("error","리포트 생성 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/daily")
    public ResponseEntity<?> getDaily(HttpServletRequest request, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        /**
         * 일별 리포트 조회
         * - 쿼리파라미터 `date` (ISO) 사용, 없으면 전일
         */
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>(); err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            LocalDate target = date != null ? date : LocalDate.now().minusDays(1);
            ReportDto dto = reportService.getDailyReport(userId, target);
            if (dto == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("getDaily error", e);
            Map<String,String> err = new HashMap<>(); err.put("error","리포트 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/weekly")
    public ResponseEntity<?> createWeekly(HttpServletRequest request, @RequestBody CreateReportRequest body) {
        /**
         * 주간 리포트 생성
         * - 입력: { fromDate: "yyyy-MM-dd", toDate: "yyyy-MM-dd" } (없으면 최근 주월요일-일요일)
         * - 반환: 201 + 생성된 ReportDto
         */
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>(); err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            LocalDate from = body.getFromDate() != null ? LocalDate.parse(body.getFromDate()) : LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate to = body.getToDate() != null ? LocalDate.parse(body.getToDate()) : from.plusDays(6);
            ReportDto dto = reportService.createWeeklyReport(userId, from, to);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            if (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().contains("LIMIT_EXCEEDED")) {
                Map<String,String> err = new HashMap<>(); err.put("error","생성 한도를 초과했습니다.");
                return ResponseEntity.status(429).body(err);
            }
            log.error("createWeekly error", e);
            Map<String,String> err = new HashMap<>(); err.put("error","주간 리포트 생성 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<?> getWeekly(HttpServletRequest request, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        /**
         * 주간 리포트 조회
         * - 쿼리 `fromDate`로 주간 시작을 지정 (없으면 직전주)
         */
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>(); err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate to = from.plusDays(6);
            ReportDto dto = reportService.getWeeklyReport(userId, from, to);
            if (dto == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("getWeekly error", e);
            Map<String,String> err = new HashMap<>(); err.put("error","주간 리포트 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(HttpServletRequest request, @PathVariable int id) {
        /**
         * 리포트 ID로 상세 조회
         * - 요청자의 소유 여부를 확인하여 결과를 반환합니다.
         */
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>(); err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            ReportDto dto = reportService.getReportById(userId, id);
            if (dto == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("getReportById error", e);
            Map<String,String> err = new HashMap<>(); err.put("error","리포트 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeReport(HttpServletRequest request, @PathVariable int id) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>(); err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            ReportDto dto = reportService.getReportById(userId, id);
            if (dto == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

            ReportAnalysisResult analysis = openAiService.analyzeReport(dto);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("analyzeReport error", e);
            Map<String,String> err = new HashMap<>(); err.put("error","AI 분석 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    /**
     * Cookie에서 토큰 추출 헬퍼 메소드
     */
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 요청에서 토큰 추출: Authorization 헤더(Bearer) 우선, 없으면 accessToken 쿠키 사용
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return getTokenFromCookie(request, "accessToken");
    }
}
