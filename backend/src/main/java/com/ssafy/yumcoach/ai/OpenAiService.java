package com.ssafy.yumcoach.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.ReportInsightDto;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
import com.ssafy.yumcoach.ai.ReportAnalysisResult.Insight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ReportMapper reportMapper;
    private final String fastApiUrl;

    public OpenAiService(Environment env, ReportMapper reportMapper) {
        this.restTemplate = new RestTemplate();
        this.reportMapper = reportMapper;

        String propUrl = env.getProperty("report.ai.url");
        String envUrl = env.getProperty("REPORT_AI_URL", System.getenv("REPORT_AI_URL"));
        if (propUrl != null && !propUrl.isEmpty()) {
            this.fastApiUrl = propUrl;
        } else if (envUrl != null && !envUrl.isEmpty()) {
            this.fastApiUrl = envUrl;
        } else {
            this.fastApiUrl = "http://localhost:8000";
        }

        log.info("Report FastAPI URL 설정: {}", this.fastApiUrl);
    }

    public ReportAnalysisResult analyzeReport(ReportDto report) throws Exception {
        String reportJson = mapper.writeValueAsString(report);

        Map<String, Object> payload = new HashMap<>();
        payload.put("report", reportJson);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            log.info("=== FastAPI 호출 시작 ===");
            log.info("URL: {}/analyze-report", fastApiUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    fastApiUrl + "/analyze-report",
                    request,
                    String.class
            );

            String respText = response.getBody();
            if (respText == null || respText.isEmpty()) {
                throw new IllegalStateException("FastAPI로부터 빈 응답이 반환되었습니다.");
            }

            log.info("✅ FastAPI 응답 성공");
            log.debug("응답: {}", respText);

            // 3. 원본 응답 DB 저장
            try {
                if (report.getId() != null) {
                    reportMapper.updateReportAiResponse(report.getId(), respText);
                    try {
                        report.setAiResponse(respText);
                    } catch (Throwable t) {
                        // 무시
                    }
                }
            } catch (Exception ex) {
                log.warn("ai_response 저장 실패: {}", ex.getMessage());
            }

            // 4. 응답을 ReportAnalysisResult로 파싱
            ReportAnalysisResult result = null;
            boolean parsedToDto = false;
            try {
                result = mapper.readValue(respText, ReportAnalysisResult.class);
                parsedToDto = true;
            } catch (Exception ex) {
                log.warn("AI 응답을 DTO로 파싱하지 못함: {}", ex.getMessage());
            }

            // 5. DTO 파싱 성공 시 coachMessage, nextAction, insights 저장
            if (report.getId() != null && parsedToDto && result != null) {
                
                // 5-1. coachMessage를 insight로 저장
                try {
                    if (result.getCoachMessage() != null && !result.getCoachMessage().isEmpty()) {
                        reportMapper.insertReportInsight(report.getId(), "coach", "코치 메시지", result.getCoachMessage());
                        log.debug("coachMessage 저장 완료");
                    }
                } catch (Exception ex) {
                    log.warn("coachMessage 저장 실패: {}", ex.getMessage());
                }
                
                // 5-2. nextAction을 insight로 저장
                try {
                    if (result.getNextAction() != null && !result.getNextAction().isEmpty()) {
                        reportMapper.insertReportInsight(report.getId(), "action", "다음 행동", result.getNextAction());
                        log.debug("nextAction 저장 완료");
                    }
                } catch (Exception ex) {
                    log.warn("nextAction 저장 실패: {}", ex.getMessage());
                }
                
                // 5-3. insights 저장 (good, warn, keep)
                if (result.getInsights() != null) {
                    List<ReportInsightDto> createdInsights = new ArrayList<>();
                    for (Insight ins : result.getInsights()) {
                        try {
                            reportMapper.insertReportInsight(report.getId(), ins.getKind(), ins.getTitle(), ins.getBody());
                            createdInsights.add(new ReportInsightDto(null, report.getId(), ins.getKind(), ins.getTitle(), ins.getBody()));
                        } catch (Exception ex) {
                            log.warn("인사이트 삽입 실패: {}", ex.getMessage());
                        }
                    }
                    try {
                        report.setInsights(createdInsights);
                    } catch (Throwable t) {
                        // 무시
                    }
                }
            } else {
                // 6. 폴백: JsonNode로 파싱
                try {
                    JsonNode root = mapper.readTree(respText);
                    if (report.getId() != null) {
                        if (root.has("insights") && root.get("insights").isArray()) {
                            for (JsonNode n : root.get("insights")) {
                                String kind = n.has("kind") ? n.get("kind").asText() : "warn";
                                String title = n.has("title") ? n.get("title").asText() : (n.isTextual() ? n.asText() : n.toString());
                                String body = n.has("body") ? n.get("body").asText() : null;
                                try { 
                                    reportMapper.insertReportInsight(report.getId(), kind, title, body); 
                                } catch (Exception ex) { 
                                    log.warn("인사이트 삽입 실패: {}", ex.getMessage()); 
                                }
                            }
                        }

                        if (root.has("observations") && root.get("observations").isArray()) {
                            for (JsonNode n : root.get("observations")) {
                                String text = n.isTextual() ? n.asText() : n.toString();
                                try { 
                                    reportMapper.insertReportInsight(report.getId(), "warn", text, null); 
                                } catch (Exception ex) { 
                                    log.warn("인사이트 삽입 실패: {}", ex.getMessage()); 
                                }
                            }
                        }

                        if (root.has("recommendations") && root.get("recommendations").isArray()) {
                            for (JsonNode n : root.get("recommendations")) {
                                String text = n.isTextual() ? n.asText() : n.toString();
                                try { 
                                    reportMapper.insertReportInsight(report.getId(), "keep", text, null); 
                                } catch (Exception ex) { 
                                    log.warn("인사이트 삽입 실패: {}", ex.getMessage()); 
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("원본 JSON 폴백 파싱 중 오류: {}", ex.getMessage());
                }
            }

            if (result == null) result = new ReportAnalysisResult();
            return result;

        } catch (Exception e) {
            log.error("❌ FastAPI 요청 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}