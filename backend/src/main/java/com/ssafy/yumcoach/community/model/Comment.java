package com.ssafy.yumcoach.community.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private Integer id;
    private Integer postId;
    private Integer userId;
    private String content;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    
    // 조회용 추가 필드 (JOIN 결과)
    private String userName;  // 댓글 작성자 이름
}
