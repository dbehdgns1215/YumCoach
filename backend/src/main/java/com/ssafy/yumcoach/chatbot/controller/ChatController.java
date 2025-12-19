package com.ssafy.yumcoach.chatbot.controller;

import com.ssafy.yumcoach.chatbot.model.ChatRequest;
import com.ssafy.yumcoach.chatbot.model.ChatResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RestTemplate restTemplate = new RestTemplate();

    // Python FastAPI 서버 URL
    private static final String PYTHON_API_URL = "http://localhost:8001/chat";
    
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Python FastAPI로 요청 전달
            HttpEntity<ChatRequest> entity = new HttpEntity<>(req, headers);
            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                PYTHON_API_URL,
                entity,
                ChatResponse.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChatResponse(null, "챗봇 서비스 오류: " + e.getMessage()));
        }
    }
}