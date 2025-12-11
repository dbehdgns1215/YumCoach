package com.ssafy.yumcoach.community.model.service;

import com.ssafy.yumcoach.community.model.Comment;
import com.ssafy.yumcoach.community.model.Post;
import com.ssafy.yumcoach.community.model.mapper.CommunityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {
    
    private final CommunityMapper communityMapper;
    
    @Override
    public List<Post> getAllPosts(int page, int size) {
        int offset = (page - 1) * size;
        return communityMapper.selectAllPosts(offset, size);
    }
    
    @Override
    public int getPostCount() {
        return communityMapper.selectPostCount();
    }
    
    @Override
    public Post getPostById(Integer id) {
        return communityMapper.selectPostById(id);
    }
    
    @Override
    @Transactional
    public void createPost(Post post) {
        communityMapper.insertPost(post);
        log.info("Post created with id: {}", post.getId());
    }
    
    @Override
    @Transactional
    public void updatePost(Post post) {
        communityMapper.updatePost(post);
        log.info("Post updated: {}", post.getId());
    }
    
    @Override
    @Transactional
    public void deletePost(Integer id, Integer deletedBy) {
        communityMapper.deletePost(id, deletedBy);
        log.info("Post deleted: {} by user: {}", id, deletedBy);
    }
    
    @Override
    public List<Comment> getCommentsByPostId(Integer postId) {
        return communityMapper.selectCommentsByPostId(postId);
    }
    
    @Override
    @Transactional
    public void createComment(Comment comment) {
        communityMapper.insertComment(comment);
        log.info("Comment created with id: {}", comment.getId());
    }
    
    @Override
    @Transactional
    public void deleteComment(Integer commentId) {
        communityMapper.deleteComment(commentId);
        log.info("Comment deleted: {}", commentId);
    }
}
