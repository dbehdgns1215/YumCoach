package com.ssafy.yumcoach.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.ReportInsightDto;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
import com.ssafy.yumcoach.ai.ReportAnalysisResult.Insight;
import com.ssafy.yumcoach.user.model.service.UserService;
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

    private final RestTemplate restTemplate = new RestTemplate();
        private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // allow unknown properties in AI response so parsing doesn't fail on extra fields
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final UserService userService;
    private final String fastApiUrl;

    public OpenAiService(Environment env, UserService userService) {
        this.userService = userService;

        String propUrl = env.getProperty("report.ai.url");
        String envUrl = System.getenv("REPORT_AI_URL");
        this.fastApiUrl = propUrl != null ? propUrl :
                (envUrl != null ? envUrl : "http://localhost:8000");
    }

    public AiResult analyze(ReportDto report) throws Exception {

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> reportMap = mapper.convertValue(report, Map.class);

        if (report.getUserId() != null) {
            Map<String, Object> user = new HashMap<>();
            var u = userService.findById(report.getUserId());
            var uh = userService.findUserHealthByUserId(report.getUserId());

            if (u != null) {
                user.put("name", u.getName());
                user.put("age", u.getAge());
            }
            if (uh != null) {
                user.put("height", uh.getHeight());
                user.put("weight", uh.getWeight());
                user.put("activity_level", uh.getActivityLevel());
            }
            reportMap.put("user", user);
        }

        payload.put("report", reportMap);

        // Diagnostic: log whether activeChallenges were attached and its size
        try {
            Object ac = reportMap.get("activeChallenges");
            if (ac == null) {
                log.debug("[OpenAiService] reportMap.activeChallenges is null for reportId={}", report.getId());
            } else if (ac instanceof java.util.List) {
                log.debug("[OpenAiService] reportMap.activeChallenges present, size={} for reportId={}", ((java.util.List) ac).size(), report.getId());
            } else {
                log.debug("[OpenAiService] reportMap.activeChallenges present but not list: {} for reportId={}", ac.getClass().getSimpleName(), report.getId());
            }
        } catch (Exception e) {
            log.warn("[OpenAiService] failed to inspect activeChallenges: {}", e.toString());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                fastApiUrl + "/analyze-report",
                new HttpEntity<>(payload, headers),
                String.class
        );

        String raw = response.getBody();
        log.debug("[OpenAiService] raw AI response length={}", raw == null ? 0 : raw.length());
        if (raw != null && raw.length() > 1000) {
            log.debug("[OpenAiService] raw AI response (truncated): {}", raw.substring(0, 1000));
        } else {
            log.debug("[OpenAiService] raw AI response: {}", raw);
        }

        ReportAnalysisResult parsed = null;
        try {
            if (raw != null && !raw.isBlank()) {
                parsed = mapper.readValue(raw, ReportAnalysisResult.class);
                log.debug("[OpenAiService] parsed AI result: heroTitle={}, score={}, insights={}",
                        parsed.getHeroTitle(), parsed.getScore(), parsed.getInsights() == null ? 0 : parsed.getInsights().size());
            }
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패, raw만 저장합니다. error={}", e.toString());
            log.debug("AI raw content: {}", raw);
        }

        return new AiResult(raw, parsed);
    }
}
