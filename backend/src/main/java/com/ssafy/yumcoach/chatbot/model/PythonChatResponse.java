package com.ssafy.yumcoach.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonChatResponse {

    private String reply;
    private String detected_hashtag;
}
