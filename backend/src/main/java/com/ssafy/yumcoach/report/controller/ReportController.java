package com.ssafy.yumcoach.report.controller;

import com.ssafy.yumcoach.report.model.CreateReportRequest;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.service.ReportService;
import com.ssafy.yumcoach.ai.OpenAiService;
import com.ssafy.yumcoach.ai.ReportAnalysisResult;
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
    private final OpenAiService openAiService;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final Environment env;

    /**
     * 배치/관리 엔드포인트: 모든 유저에 대해 일별/주별 생성 횟수를 계산하여
     * `user_generation_count` 테이블에 동기화합니다.
     *
     * 사용법 (Postman / curl 예시):
     * - curl:
     *   curl -X POST "http://localhost:8282/api/reports/admin/sync-generation-counts" -H "Authorization: Bearer <TOKEN>"
     *
     * 권한: 관리자 전용(토큰의 유저 role이 ADMIN 이어야 합니다).
     * 이 엔드포인트는 배치에서 주기적으로 호출하거나 수동 동기화용으로 사용합니다.
     *
     * 반환값: { "updated": <number of users updated>, "errors": <number of failures> }
     */
    @PostMapping("/admin/sync-generation-counts")
    public ResponseEntity<?> syncGenerationCounts(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int adminId = jwtUtil.getUserId(token);
            User admin = userMapper.findById(adminId);
            if (admin == null || admin.getRole() == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
                Map<String,String> err = new HashMap<>();
                err.put("error","관리자 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
            }

            java.util.List<Integer> userIds = userMapper.findAllUserIds();
            int updated = 0; int errors = 0;
            ZoneId zone = ZoneId.of("Asia/Seoul");
            LocalDate today = LocalDate.now(zone);
            LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            java.time.LocalDateTime dayStart = today.atStartOfDay();
            java.time.LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
            java.time.LocalDateTime weekStartDt = weekStart.atStartOfDay();
            java.time.LocalDateTime weekEndDt = weekEnd.plusDays(1).atStartOfDay();

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

            Map<String,Integer> resp = new HashMap<>();
            resp.put("updated", updated);
            resp.put("errors", errors);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("syncGenerationCounts error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","동기화 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/daily")
    public ResponseEntity<?> createDaily(HttpServletRequest request, @RequestBody CreateReportRequest body) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);

            ZoneId zone = ZoneId.of("Asia/Seoul");
            String bodyDate = body == null ? null : body.getDate();
            LocalDate date = bodyDate != null ? LocalDate.parse(bodyDate) : LocalDate.now(zone);

            log.info("createDaily - userId={} date={}", userId, date);

            ReportDto dto = reportService.createDailyReport(userId, date);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("LIMIT_EXCEEDED")) {
                    Map<String,String> err = new HashMap<>();
                    err.put("error","생성 한도를 초과했습니다.");
                    return ResponseEntity.status(429).body(err);
                }
                if (msg.contains("NO_MEALS")) {
                    Map<String,String> err = new HashMap<>();
                    err.put("error","해당 날짜에 기록된 식사 데이터가 없습니다.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
                }
            }
            log.error("createDaily error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","리포트 생성 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/daily")
    public ResponseEntity<?> getDaily(HttpServletRequest request,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            LocalDate target = date != null ? date : LocalDate.now().minusDays(1);

            ReportDto dto = reportService.getDailyReport(userId, target);
            if (dto == null) {
                Map<String,String> err = new HashMap<>();
                err.put("error","리포트를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
            }
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("getDaily error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","리포트 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/weekly")
    public ResponseEntity<?> createWeekly(HttpServletRequest request, @RequestBody CreateReportRequest body) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);

            ZoneId zone = ZoneId.of("Asia/Seoul");
            LocalDate from = body.getFromDate() != null ? LocalDate.parse(body.getFromDate())
                    : LocalDate.now(zone).with(java.time.DayOfWeek.MONDAY);
            LocalDate to = body.getToDate() != null ? LocalDate.parse(body.getToDate())
                    : LocalDate.now(zone);

            log.info("createWeekly - userId={} from={} to={}", userId, from, to);

            ReportDto dto = reportService.createWeeklyReport(userId, from, to);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("LIMIT_EXCEEDED")) {
                    Map<String,String> err = new HashMap<>();
                    err.put("error","생성 한도를 초과했습니다.");
                    return ResponseEntity.status(429).body(err);
                }
                if (msg.contains("NO_MEALS")) {
                    Map<String,String> err = new HashMap<>();
                    err.put("error","해당 주간에 기록된 식사 데이터가 없습니다.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
                }
            }
            log.error("createWeekly error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","주간 리포트 생성 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<?> getWeekly(HttpServletRequest request,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);
            LocalDate from = fromDate != null ? fromDate
                    : LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate to = from.plusDays(6);

            ReportDto dto = reportService.getWeeklyReport(userId, from, to);
            if (dto == null) {
                Map<String,String> err = new HashMap<>();
                err.put("error","리포트를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
            }
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("getWeekly error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","주간 리포트 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(HttpServletRequest request, @PathVariable int id) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);

            ReportDto dto = reportService.getReportById(userId, id);
            if (dto == null) {
                Map<String,String> err = new HashMap<>();
                err.put("error","리포트를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
            }
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("getReportById error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","리포트 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/quota")
    public ResponseEntity<?> getQuota(HttpServletRequest request,
                                      @RequestParam(required = false, defaultValue = "DAILY") String type,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);

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

            // Determine limit by user role (fall back to application properties)
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
                // fallback to application properties when user lookup fails
                try {
                    String key = "report.limit." + type.toLowerCase();
                    limit = Integer.parseInt(env.getProperty(key, "1"));
                } catch (Exception ex2) {
                    limit = 1;
                }
            }

            int remaining = Math.max(0, limit - used);

            Map<String,Object> resp = new HashMap<>();
            resp.put("used", used);
            resp.put("limit", limit);
            resp.put("remaining", remaining);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("getQuota error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","잔여 생성 횟수 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeReport(HttpServletRequest request, @PathVariable int id) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                Map<String,String> err = new HashMap<>();
                err.put("error","인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
            }
            int userId = jwtUtil.getUserId(token);

            ReportDto dto = reportService.getReportById(userId, id);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            ReportAnalysisResult analysis = openAiService.analyzeReport(dto);
            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("analyzeReport error", e);
            Map<String,String> err = new HashMap<>();
            err.put("error","AI 분석 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

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

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return getTokenFromCookie(request, "accessToken");
    }
}