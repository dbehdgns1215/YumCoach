package com.ssafy.yumcoach.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {
    private String title;
    private String content;
    private String category;  // 프론트엔드에서 사용 (경험, 식단, 팁)
}
