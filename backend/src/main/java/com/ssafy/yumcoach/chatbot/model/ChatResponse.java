package com.ssafy.yumcoach.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    // AI가 준 response 그대로
    private String response;

    // 서버에서 판단한 해시태그 (#일간리포트, #주간리포트)
    private String detected_hashtag;
}
