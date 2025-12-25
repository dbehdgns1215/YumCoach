package com.ssafy.yumcoach.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.yumcoach.chatbot.model.ChatRequest;
import com.ssafy.yumcoach.chatbot.model.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Python FastAPI 챗봇 서버의 엔드포인트 URL
     */
    private static final String PYTHON_API_URL = "http://localhost:8077/chat";
    
    /**
     * 챗봇 메시지 전송 및 응답 수신
     * 
     * 사용자의 메시지를 Python FastAPI 서버로 전달하고,
     * AI가 생성한 응답을 클라이언트에게 반환합니다.
     *
     * @param req 사용자 메시지가 포함된 요청 객체
     * @return AI 챗봇의 응답을 포함한 ResponseEntity<ChatResponse>
     * @throws Exception FastAPI 서버 통신 실패 시 500 에러 반환
     * 
     * POST /api/chat
     * Content-Type: application/json
     * 
     * Request Body:
     * {
     *   "message": "안녕하세요"
     * }
     * 
     * Response:
     * {
     *   "reply": "안녕하세요! 무엇을 도와드릴까요?",
     *   "raw": null
     * }
     */
    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(PYTHON_API_URL, entity, String.class);

            String rawJson = response.getBody();
            JsonNode root = objectMapper.readTree(rawJson);

            String text = extractText(root);
            String hashtag = extractHashtag(root);

            return ResponseEntity.ok(new ChatResponse(text, hashtag));

        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse("챗봇 서비스 오류: " + e.getMessage(), null));
        }
    }


    private String extractText(JsonNode root) {
        // 케이스 1: { "response": "..." }
        if (root.hasNonNull("response") && root.get("response").isTextual()) {
            return root.get("response").asText();
        }

        // 케이스 2: { "reply": "..." }
        if (root.hasNonNull("reply") && root.get("reply").isTextual()) {
            return root.get("reply").asText();
        }

        // 케이스 3: { "reply": { "response": "..." } }
        if (root.hasNonNull("reply") && root.get("reply").isObject()) {
            JsonNode r = root.get("reply").get("response");
            if (r != null && r.isTextual()) {
                return r.asText();
            }
        }

        return "응답 형식이 올바르지 않아요.";
    }

    private String extractHashtag(JsonNode root) {
        if (root.hasNonNull("detected_hashtag") && root.get("detected_hashtag").isTextual()) {
            return root.get("detected_hashtag").asText();
        }
        return null;
    }

}