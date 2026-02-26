package com.ssafy.yumcoach.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.yumcoach.chatbot.model.ChatRequest;
import com.ssafy.yumcoach.chatbot.model.ChatResponse;
import com.ssafy.yumcoach.chatbot.model.PythonChatResponse;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ObjectMapper objectMapper;
    private final ReportService reportService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String PYTHON_URL = "http://localhost:8077/chat";

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        String rawMessage = req.getMessage();
        Integer userId = Integer.parseInt(req.getUser_id());

        /* ======================================================
         * 0️⃣ #일일리포트 + 날짜 없음 → 날짜 선택
         * ====================================================== */
        if (rawMessage.contains("#일일리포트")) {
            String message = rawMessage.replace("#일일리포트", "").trim();

            if (!isDateOnly(message)) {
                List<String> dates = findAvailableDailyDates(userId, 3);

                try {
                    String payload = objectMapper.writeValueAsString(
                            Map.of(
                                    "type", "date_request",
                                    "message", "분석할 날짜를 선택하세요.",
                                    "available_dates", dates
                            )
                    );

                    return ResponseEntity.ok(
                            new ChatResponse(payload, "#일일리포트")
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // 날짜가 있으면 리포트 조회
            LocalDate date = resolveDate(message);
            ReportDto report = reportService.getDailyReport(userId, date);

            if (report == null) {
                return ResponseEntity.ok(
                        new ChatResponse(
                                "⚠️ 오류\n해당 날짜의 리포트가 없습니다.",
                                "#일일리포트"
                        )
                );
            }

            Map<String, Object> reportMap = objectMapper.convertValue(report, Map.class);

            req.setReportType("DAILY");
            req.setReportDate(date.toString());
            req.setReport_data(reportMap);

            log.info("[CHAT] daily report loaded userId={}, date={}", userId, date);
        }

        /* ======================================================
         * 1️⃣ #주간리포트 + 범위 없음 → 주간 선택
         * ====================================================== */
        if (rawMessage.contains("#주간리포트")) {
            String message = rawMessage.replace("#주간리포트", "").trim();

            if (!isRangeOnly(message)) {
                List<String> ranges = findAvailableWeeklyRanges(3);

                try {
                    String payload = objectMapper.writeValueAsString(
                            Map.of(
                                    "type", "range_request",
                                    "message", "분석할 주를 선택하세요.",
                                    "available_ranges", ranges
                            )
                    );

                    return ResponseEntity.ok(
                            new ChatResponse(payload, "#주간리포트")
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // 범위가 있으면 주간 리포트 조회
            LocalDate[] range = resolveRange(message);
            LocalDate fromDate = range[0];
            LocalDate toDate = range[1];

            ReportDto weeklyReport = reportService.getWeeklyReport(userId, fromDate, toDate);

            if (weeklyReport == null) {
                return ResponseEntity.ok(
                        new ChatResponse(
                                "⚠️ 오류\n해당 기간의 데이터가 부족합니다.",
                                "#주간리포트"
                        )
                );
            }

            Map<String, Object> reportMap = objectMapper.convertValue(weeklyReport, Map.class);

            req.setReportType("WEEKLY");
            req.setReportDate(fromDate.toString());
            req.setReport_data(reportMap);

            log.info("[CHAT] weekly report loaded userId={}, range={} ~ {}", userId, fromDate, toDate);
        }

        /* ======================================================
         * 2️⃣ Python AI 호출
         * ====================================================== */
        HttpEntity<ChatRequest> entity = new HttpEntity<>(req, jsonHeaders());

        ResponseEntity<PythonChatResponse> pyRes =
                restTemplate.postForEntity(
                        PYTHON_URL,
                        entity,
                        PythonChatResponse.class
                );

        PythonChatResponse body = pyRes.getBody();

        if (body == null) {
            return ResponseEntity.ok(
                    new ChatResponse(
                            "⚠️ 오류\nAI 서버 응답이 없습니다.",
                            null
                    )
            );
        }

        return ResponseEntity.ok(
                new ChatResponse(
                        body.getReply(),
                        body.getDetected_hashtag()
                )
        );
    }

    /* ===============================
     * 날짜만 들어왔는지 검사 (MM.DD 또는 YYYY-MM-DD)
     * =============================== */
    private boolean isDateOnly(String message) {
        return message != null && (
                message.matches("\\d{2}\\.\\d{2}") ||
                        message.matches("\\d{4}-\\d{2}-\\d{2}")
        );
    }

    /* ===============================
     * 범위만 들어왔는지 검사 (MM.DD~MM.DD 또는 YYYY-MM-DD~YYYY-MM-DD)
     * =============================== */
    private boolean isRangeOnly(String message) {
        return message != null && (
                message.matches("\\d{2}\\.\\d{2}~\\d{2}\\.\\d{2}") ||
                        message.matches("\\d{4}-\\d{2}-\\d{2}~\\d{4}-\\d{2}-\\d{2}")
        );
    }

    /* ===============================
     * MM.DD / YYYY-MM-DD → LocalDate
     * =============================== */
    private LocalDate resolveDate(String message) {
        if (message.matches("\\d{2}\\.\\d{2}")) {
            int year = LocalDate.now().getYear();
            return LocalDate.parse(year + "-" + message.replace(".", "-"));
        }
        return LocalDate.parse(message);
    }

    /* ===============================
     * MM.DD~MM.DD / YYYY-MM-DD~YYYY-MM-DD → LocalDate[]
     * =============================== */
    private LocalDate[] resolveRange(String message) {
        String[] parts = message.split("~");
        LocalDate start = resolveDate(parts[0].trim());
        LocalDate end = resolveDate(parts[1].trim());
        return new LocalDate[]{start, end};
    }

    /* ===============================
     * JSON 헤더
     * =============================== */
    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /* ======================================================
     * 오늘 제외하고 존재하는 일일 리포트 날짜 찾기
     * ====================================================== */
    private List<String> findAvailableDailyDates(int userId, int limit) {
        List<String> result = new ArrayList<>();

        LocalDate cursor = LocalDate.now().minusDays(1); // 오늘 제외

        while (result.size() < limit) {
            ReportDto report = reportService.getDailyReport(userId, cursor);
            if (report != null) {
                result.add(cursor.format(DateTimeFormatter.ofPattern("MM.dd")));
            }
            cursor = cursor.minusDays(1);
        }

        return result;
    }

    /* ======================================================
     * 이번 주 제외하고 주간 범위 찾기 (지난 3주)
     * ====================================================== */
    private List<String> findAvailableWeeklyRanges(int limit) {
        List<String> result = new ArrayList<>();

        // 이번 주 월요일
        LocalDate thisMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 지난 주 월요일부터 시작
        LocalDate cursor = thisMonday.minusWeeks(1);

        for (int i = 0; i < limit; i++) {
            LocalDate start = cursor;
            LocalDate end = cursor.plusDays(6); // 일요일

            String range = String.format("%s~%s",
                    start.format(DateTimeFormatter.ofPattern("MM.dd")),
                    end.format(DateTimeFormatter.ofPattern("MM.dd"))
            );

            result.add(range);
            cursor = cursor.minusWeeks(1); // 그 전주로
        }

        return result;
    }
}