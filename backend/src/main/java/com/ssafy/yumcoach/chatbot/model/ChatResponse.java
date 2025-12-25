package com.ssafy.yumcoach.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 응답 DTO
 * 
 * 챗봇 서버에서 클라이언트로 반환하는 AI 응답을 담는 객체
 * 
 * @param reply AI가 생성한 응답 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private String detected_hashtag;
}