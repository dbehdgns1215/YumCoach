package com.ssafy.yumcoach.report.controller;

import com.ssafy.yumcoach.report.model.CreateReportRequest;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.service.ReportService;
import com.ssafy.yumcoach.ai.OpenAiService;
import com.ssafy.yumcoach.ai.ReportAnalysisResult;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
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
    private final Environment env;

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