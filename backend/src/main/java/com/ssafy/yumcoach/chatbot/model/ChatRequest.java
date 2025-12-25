package com.ssafy.yumcoach.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    // 사용자가 입력한 원문 메시지 (ex: "#일간리포트 분석해줘")
    private String message;

    private String user_id;

    // 리포트 타입
    private String reportType; // DAILY | WEEKLY

    // DAILY
    private String reportDate; // yyyy-MM-dd

    // WEEKLY
    private String startDate;
    private String endDate;

    // 서버에서 채워짐 (프론트에서는 안 보내도 됨)
    private Map<String, Object> user_profile;
    private Map<String, Object> report_data;
}
