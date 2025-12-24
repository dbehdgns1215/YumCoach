package com.ssafy.yumcoach.ai;

public record AiResult(
        String rawJson,
        ReportAnalysisResult parsed
) {}

