package com.ssafy.yumcoach.community.model.service;

import com.ssafy.yumcoach.community.model.Comment;
import com.ssafy.yumcoach.community.model.Post;

import java.util.List;

public interface CommunityService {
    
    // 게시글 목록 조회
    List<Post> getAllPosts(int page, int size);
    
    // 게시글 총 개수
    int getPostCount();
    
    // 게시글 상세 조회
    Post getPostById(Integer id);
    
    // 게시글 작성
    void createPost(Post post);
    
    // 게시글 수정
    void updatePost(Post post);
    
    // 게시글 삭제
    void deletePost(Integer id, Integer deletedBy);
    
    // 댓글 목록 조회
    List<Comment> getCommentsByPostId(Integer postId);
    
    // 댓글 작성
    void createComment(Comment comment);
    
    // 댓글 삭제
    void deleteComment(Integer commentId);
}
