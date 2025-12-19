package com.ssafy.yumcoach.chatbot.model;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;

    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }
}
