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
public class Post {
    private Integer id;
    private String title;
    private String content;
    private String category;  // 카테고리: 경험, 식단, 팁
    private Boolean isNotice;
    private Boolean isDeleted;
    private Integer userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer deletedBy;
    
    // 조회용 추가 필드 (JOIN 결과)
    private String userName;  // 작성자 이름
    private Integer commentCount;  // 댓글 수
    private Integer likeCount;  // 좋아요 수 (추후 추가 시)
}
