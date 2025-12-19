package com.ssafy.yumcoach.chatbot.model;

import lombok.Data;

@Data
public class ChatResponse {
    private String reply;
    private Object raw;

    public ChatResponse() {}

    public ChatResponse(String reply, Object raw) {
        this.reply = reply;
        this.raw = raw;
    }
}
