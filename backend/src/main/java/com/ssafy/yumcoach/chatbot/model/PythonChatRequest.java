package com.ssafy.yumcoach.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonChatRequest {

    private String message;
    private String user_id;

    private Map<String, Object> user_profile;
    private Map<String, Object> report_data;
}
