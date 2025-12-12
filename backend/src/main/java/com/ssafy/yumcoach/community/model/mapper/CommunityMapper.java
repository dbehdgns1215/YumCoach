package com.ssafy.yumcoach.community.model.mapper;

import com.ssafy.yumcoach.community.model.Comment;
import com.ssafy.yumcoach.community.model.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityMapper {
    
    // ===== Post 관련 =====
    
    // 게시글 목록 조회 (페이지네이션)
    List<Post> selectAllPosts(
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    
    // 게시글 총 개수
    int selectPostCount();
    
    // 게시글 상세 조회 (작성자 이름 포함)
    Post selectPostById(@Param("id") Integer id);
    
    // 게시글 작성
    int insertPost(Post post);
    
    // 게시글 수정
    int updatePost(Post post);
    
    // 게시글 삭제 (소프트 삭제)
    int deletePost(
            @Param("id") Integer id,
            @Param("deletedBy") Integer deletedBy
    );
    
    
    // ===== Comment 관련 =====
    
    // 특정 게시글의 댓글 목록 조회
    List<Comment> selectCommentsByPostId(@Param("postId") Integer postId);
    
    // 댓글 작성
    int insertComment(Comment comment);
    
    // 댓글 삭제 (소프트 삭제)
    int deleteComment(@Param("id") Integer id);
}
