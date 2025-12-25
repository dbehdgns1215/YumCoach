package com.ssafy.yumcoach.chatbot.controller;

import com.ssafy.yumcoach.chatbot.model.ChatRequest;
import com.ssafy.yumcoach.chatbot.model.ChatResponse;
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
            ResponseEntity<ChatResponse> response =
                    restTemplate.postForEntity(PYTHON_API_URL, entity, ChatResponse.class);

            return ResponseEntity.ok(response.getBody());

        } catch (HttpStatusCodeException ex) {
            // FastAPI가 내려준 에러 JSON을 그대로 전달
            String body = ex.getResponseBodyAsString();
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse(
                            "챗봇 서비스 오류: " + e.getMessage(),
                            null
                    ));
        }
    }
}