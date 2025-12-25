package com.ssafy.yumcoach.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonChatResponse {

    private Reply reply;
    private String detected_hashtag;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reply {
        private String response;
    }
}
