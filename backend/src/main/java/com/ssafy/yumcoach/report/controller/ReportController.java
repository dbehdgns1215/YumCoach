package com.ssafy.yumcoach.report.controller;

import com.ssafy.yumcoach.report.model.CreateReportRequest;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.service.ReportService;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.mapper.UserMapper;
import com.ssafy.yumcoach.auth.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final JwtUtil jwtUtil;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final Environment env;

    /**
     * 🔥 헬퍼: userId 추출 (토큰에서만)
     */
    private Integer extractUserId(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getUserId(token);
        } catch (Exception e) {
            log.warn("[ReportController] extractUserId failed", e);
            return null;
        }
    }

    /**
     * 🔥 배치/관리 엔드포인트: 생성 횟수 동기화 (관리자 전용)
     */
    @PostMapping("/admin/sync-generation-counts")
    public ResponseEntity<?> syncGenerationCounts(HttpServletRequest request) {
        Integer adminId = extractUserId(request);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        User admin = userMapper.findById(adminId);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            java.util.List<Integer> userIds = userMapper.findAllUserIds();
            int updated = 0;
            int errors = 0;
            ZoneId zone = ZoneId.of("Asia/Seoul");
            LocalDate today = LocalDate.now(zone);
            LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime dayStart = today.atStartOfDay();
            LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
            LocalDateTime weekStartDt = weekStart.atStartOfDay();
            LocalDateTime weekEndDt = weekEnd.plusDays(1).atStartOfDay();

            for (Integer uid : userIds) {
                try {
                    int dailyUsed = reportMapper.countGenerationLogsInPeriod(uid, "DAILY", dayStart, dayEnd, "USER");
                    int weeklyUsed = reportMapper.countGenerationLogsInPeriod(uid, "WEEKLY", weekStartDt, weekEndDt, "USER");
                    reportMapper.upsertUserGenerationCount(uid, today, dailyUsed, weekStart, weeklyUsed);
                    updated++;
                } catch (Exception ex) {
                    log.warn("sync failed for user {}: {}", uid, ex.getMessage());
                    errors++;
                }
            }

            return ResponseEntity.ok(Map.of("updated", updated, "errors", errors));

        } catch (Exception e) {
            log.error("syncGenerationCounts error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "동기화 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일간 리포트 생성
     */
    @PostMapping("/daily")
    public ResponseEntity<?> createDaily(HttpServletRequest request, @RequestBody CreateReportRequest body) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            ZoneId zone = ZoneId.of("Asia/Seoul");
            String bodyDate = body == null ? null : body.getDate();
            LocalDate date = bodyDate != null ? LocalDate.parse(bodyDate) : LocalDate.now(zone);

            log.info("createDaily - userId={} date={}", userId, date);

            ReportDto dto = reportService.createDailyReport(userId, date);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IllegalStateException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();

            if (msg.contains("LIMIT_EXCEEDED")) {
                logGenerationAttempt(userId, "DAILY", body, "LIMIT_EXCEEDED", msg);
                return ResponseEntity.status(429)
                        .body(Map.of("error", "생성 한도를 초과했습니다."));
            }

            if (msg.contains("NO_MEALS")) {
                logGenerationAttempt(userId, "DAILY", body, "NO_DATA", msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "해당 날짜에 기록된 식사 데이터가 없습니다."));
            }

            throw e;

        } catch (Exception e) {
            log.error("createDaily error", e);
            logGenerationAttempt(userId, "DAILY", body, "FAILED", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "리포트 생성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일간 리포트 조회
     */
    @GetMapping("/daily")
    public ResponseEntity<?> getDaily(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            LocalDate target = date != null ? date : LocalDate.now().minusDays(1);
            ReportDto dto = reportService.getDailyReport(userId, target);

            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "리포트를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("getDaily error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "리포트 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 주간 리포트 생성
     */
    @PostMapping("/weekly")
    public ResponseEntity<?> createWeekly(HttpServletRequest request, @RequestBody CreateReportRequest body) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            ZoneId zone = ZoneId.of("Asia/Seoul");
            LocalDate from = body.getFromDate() != null ? LocalDate.parse(body.getFromDate())
                    : LocalDate.now(zone).with(java.time.DayOfWeek.MONDAY);
            LocalDate to = body.getToDate() != null ? LocalDate.parse(body.getToDate())
                    : LocalDate.now(zone);

            log.info("createWeekly - userId={} from={} to={}", userId, from, to);

            ReportDto dto = reportService.createWeeklyReport(userId, from, to);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IllegalStateException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            LocalDate from = body.getFromDate() != null ? LocalDate.parse(body.getFromDate()) : null;
            LocalDate to = body.getToDate() != null ? LocalDate.parse(body.getToDate()) : null;

            if (msg.contains("LIMIT_EXCEEDED")) {
                logWeeklyAttempt(userId, from, to, "LIMIT_EXCEEDED", msg);
                return ResponseEntity.status(429)
                        .body(Map.of("error", "생성 한도를 초과했습니다."));
            }

            if (msg.contains("NO_MEALS")) {
                logWeeklyAttempt(userId, from, to, "NO_DATA", msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "해당 주간에 기록된 식사 데이터가 없습니다."));
            }

            throw e;

        } catch (Exception e) {
            log.error("createWeekly error", e);
            LocalDate from = body.getFromDate() != null ? LocalDate.parse(body.getFromDate()) : null;
            LocalDate to = body.getToDate() != null ? LocalDate.parse(body.getToDate()) : null;
            logWeeklyAttempt(userId, from, to, "FAILED", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "주간 리포트 생성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 주간 리포트 조회
     */
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeekly(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate
    ) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            LocalDate from = fromDate != null ? fromDate
                    : LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate to = from.plusDays(6);

            ReportDto dto = reportService.getWeeklyReport(userId, from, to);

            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "리포트를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("getWeekly error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "주간 리포트 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * ID로 리포트 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(HttpServletRequest request, @PathVariable int id) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            ReportDto dto = reportService.getReportById(userId, id);

            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "리포트를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("getReportById error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "리포트 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 생성 할당량 조회
     */
    @GetMapping("/quota")
    public ResponseEntity<?> getQuota(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "DAILY") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate
    ) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            ZoneId zone = ZoneId.of("Asia/Seoul");
            LocalDate startDate;
            LocalDate endDate;

            if ("WEEKLY".equalsIgnoreCase(type)) {
                LocalDate from = fromDate != null ? fromDate : LocalDate.now(zone).with(java.time.DayOfWeek.MONDAY);
                LocalDate to = from.plusDays(6);
                startDate = from;
                endDate = to;
            } else {
                LocalDate target = date != null ? date : LocalDate.now(zone);
                startDate = target;
                endDate = target;
            }

            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();

            int used = reportMapper.countGenerationLogsInPeriod(userId, type.toUpperCase(), start, end, "USER");

            // 사용자 role에 따른 limit 결정
            int limit = 0;
            try {
                User user = userMapper.findById(userId);
                String role = user != null && user.getRole() != null ? user.getRole().toUpperCase() : "";

                if ("ADMIN".equals(role)) {
                    limit = 1000;
                } else if ("ADVANCED".equals(role) || "AD".equals(role)) {
                    limit = "DAILY".equalsIgnoreCase(type) ? 2 : 10;
                } else {
                    limit = "DAILY".equalsIgnoreCase(type) ? 1 : 5;
                }
            } catch (Exception ex) {
                // fallback
                limit = 1;
            }

            int remaining = Math.max(0, limit - used);

            return ResponseEntity.ok(Map.of(
                    "used", used,
                    "limit", limit,
                    "remaining", remaining
            ));

        } catch (Exception e) {
            log.error("getQuota error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "잔여 생성 횟수 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 리포트 분석 (AI)
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeReport(HttpServletRequest request, @PathVariable int id) {
        Integer userId = extractUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ReportDto dto = reportService.getReportById(userId, id);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            reportService.analyzeReport(id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("analyzeReport error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 일간 리포트 생성 시도 로그
     */
    private void logGenerationAttempt(Integer userId, String type, CreateReportRequest body, String result, String details) {
        try {
            LocalDate date = body != null && body.getDate() != null
                    ? LocalDate.parse(body.getDate())
                    : LocalDate.now();
            reportMapper.insertGenerationLog(userId, type, date, null, null, "USER", result, null, details);
        } catch (Exception ex) {
            log.warn("Failed to log generation attempt", ex);
        }
    }

    /**
     * 주간 리포트 생성 시도 로그
     */
    private void logWeeklyAttempt(Integer userId, LocalDate from, LocalDate to, String result, String details) {
        try {
            reportMapper.insertGenerationLog(userId, "WEEKLY", null, from, to, "USER", result, null, details);
        } catch (Exception ex) {
            log.warn("Failed to log weekly attempt", ex);
        }
    }

    /**
     * 토큰 추출 (Authorization 헤더 우선, 쿠키 fallback)
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return getTokenFromCookie(request, "accessToken");
    }

    /**
     * 쿠키에서 토큰 추출
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
}