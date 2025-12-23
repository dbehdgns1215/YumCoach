package com.ssafy.yumcoach.community.controller;

import com.ssafy.yumcoach.auth.principal.CustomUserPrincipal;
import com.ssafy.yumcoach.auth.util.JwtUtil;
import com.ssafy.yumcoach.community.model.Comment;
import com.ssafy.yumcoach.community.model.Post;
import com.ssafy.yumcoach.community.model.service.CommunityService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {
    
    private final CommunityService communityService;
    private final JwtUtil jwtUtil;
    
    /**
     * 게시글 목록 조회 (페이지네이션)
     * GET /api/community?page=1&size=10
     */
    @GetMapping
    public ResponseEntity<?> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            List<Post> posts = communityService.getAllPosts(page, size);
            int totalCount = communityService.getPostCount();
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("posts", posts);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            response.put("totalCount", totalCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Get posts error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 목록 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게시글 상세 조회
     * GET /api/community/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable Integer id) {
        try {
            Post post = communityService.getPostById(id);
            if (post == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "게시글을 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Get post error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게시글 작성
     * POST /api/community
     */
    @PostMapping
    public ResponseEntity<?> createPost(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestBody Post post
    ) {
        try {
            Integer userId = user.getUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증이 필요합니다."));
            }
            
            post.setUserId(userId);
            communityService.createPost(post);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "게시글이 작성되었습니다.", "id", post.getId()));
        } catch (Exception e) {
            log.error("Create post error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 작성 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게시글 수정
     * PUT /api/community/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(
            HttpServletRequest request,
            @PathVariable Integer id,
            @RequestBody Post post
    ) {
        try {
            Integer userId = getUserIdFromToken(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증이 필요합니다."));
            }
            
            // 기존 게시글 조회
            Post existingPost = communityService.getPostById(id);
            if (existingPost == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "게시글을 찾을 수 없습니다."));
            }
            
            // 작성자 본인 확인
            if (!existingPost.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "게시글 수정 권한이 없습니다."));
            }
            
            post.setId(id);
            communityService.updatePost(post);
            
            return ResponseEntity.ok(Map.of("message", "게시글이 수정되었습니다."));
        } catch (Exception e) {
            log.error("Update post error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 수정 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게시글 삭제
     * DELETE /api/community/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(
            HttpServletRequest request,
            @PathVariable Integer id
    ) {
        try {
            Integer userId = getUserIdFromToken(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증이 필요합니다."));
            }
            
            // 기존 게시글 조회
            Post existingPost = communityService.getPostById(id);
            if (existingPost == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "게시글을 찾을 수 없습니다."));
            }
            
            // 작성자 본인 확인
            if (!existingPost.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "게시글 삭제 권한이 없습니다."));
            }
            
            communityService.deletePost(id, userId);
            
            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("Delete post error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 삭제 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 댓글 목록 조회
     * GET /api/community/{id}/comments
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Integer id) {
        try {
            List<Comment> comments = communityService.getCommentsByPostId(id);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("Get comments error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "댓글 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 댓글 작성
     * POST /api/community/{id}/comments
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> createComment(
            HttpServletRequest request,
            @PathVariable Integer id,
            @RequestBody Comment comment
    ) {
        try {
            Integer userId = getUserIdFromToken(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증이 필요합니다."));
            }
            
            comment.setPostId(id);
            comment.setUserId(userId);
            communityService.createComment(comment);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "댓글이 작성되었습니다.", "id", comment.getId()));
        } catch (Exception e) {
            log.error("Create comment error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "댓글 작성 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 댓글 삭제
     * DELETE /api/community/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            HttpServletRequest request,
            @PathVariable Integer commentId
    ) {
        try {
            Integer userId = getUserIdFromToken(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증이 필요합니다."));
            }
            
            // TODO: 댓글 작성자 확인 로직 추가 (필요시)
            
            communityService.deleteComment(commentId);
            
            return ResponseEntity.ok(Map.of("message", "댓글이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("Delete comment error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "댓글 삭제 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * Cookie에서 토큰 추출 후 사용자 ID 반환
     */
    private Integer getUserIdFromToken(HttpServletRequest request) {
        try {
            String token = getTokenFromHeaderOrCookie(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getUserId(token);
        } catch (Exception e) {
            log.error("Get userId from token error", e);
            return null;
        }
    }
    
    /**
     * Cookie에서 토큰 추출
     */
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Authorization 헤더(Bearer) 우선, 없으면 accessToken 쿠키 사용
     */
    private String getTokenFromHeaderOrCookie(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return getTokenFromCookie(request, "accessToken");
    }
}
