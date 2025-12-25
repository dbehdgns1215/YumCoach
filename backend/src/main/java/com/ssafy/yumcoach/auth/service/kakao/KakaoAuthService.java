package com.ssafy.yumcoach.auth.service.kakao;

import com.ssafy.yumcoach.auth.KakaoUserResponse;
import com.ssafy.yumcoach.auth.model.AuthResponse;
import com.ssafy.yumcoach.auth.model.KakaoTokenResponse;
import com.ssafy.yumcoach.auth.model.UserDto;
import com.ssafy.yumcoach.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final JwtUtil jwtUtil;

    public AuthResponse login(String code, String redirectUri) {

        // 1️⃣ 인가 코드 → 카카오 Access Token
        KakaoTokenResponse token = requestToken(code, redirectUri);
        if (token == null || token.getAccessToken() == null) {
            throw new IllegalArgumentException("카카오 토큰 요청 실패");
        }

        // 2️⃣ Access Token → 카카오 사용자 정보
        KakaoUserResponse kakaoUser = requestUser(token.getAccessToken());
        if (kakaoUser == null || kakaoUser.getId() == null) {
            throw new IllegalArgumentException("카카오 사용자 정보 조회 실패");
        }

        // 3️⃣ kakaoAccount 필드 확인 (필수 동의항목 미동의 시)
        if (kakaoUser.getKakaoAccount() == null) {
            throw new IllegalArgumentException("카카오 계정 정보 동의 필요: 프로필 정보 동의가 필요합니다.");
        }

        KakaoUserResponse.KakaoAccount kakaoAccount = kakaoUser.getKakaoAccount();

        // 프로필(닉네임) 필수 확인
        if (kakaoAccount.getProfile() == null || kakaoAccount.getProfile().getNickname() == null) {
            throw new IllegalArgumentException("카카오 계정 정보 불완전: 프로필(닉네임) 정보가 필요합니다.");
        }

        // 이메일은 선택사항 (개발 단계에서 권한 없을 수 있음)
        String email = kakaoAccount.getEmail();
        if (email == null) {
            // 이메일이 없으면 카카오 ID 기반 이메일 생성
            email = "kakao_" + kakaoUser.getId() + "@kakao.local";
            log.warn("[카카오 로그인] 사용자 이메일 없음, 임시 이메일로 대체: {}", email);
        }

        // 4️⃣ JWT 발급 (임시: 내부 userId를 확보하지 못하므로 0 사용, subject에 이메일)
        // 추후 카카오 계정을 로컬 유저와 매핑하면 실제 userId로 교체
        String jwt = jwtUtil.createAccessToken(0, email);

        UserDto user = new UserDto(
                kakaoUser.getId(),
                email,
                kakaoAccount.getProfile().getNickname());

        log.info("[카카오 로그인 성공] kakaoId={}, nickname={}", kakaoUser.getId(), kakaoAccount.getProfile().getNickname());
        return new AuthResponse(jwt, user);
    }

    private KakaoTokenResponse requestToken(String code, String redirectUri) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        log.debug("[카카오 토큰 요청] clientId={}, redirectUri={}, tokenUri={}", clientId, redirectUri, tokenUri);

        HttpEntity<?> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(tokenUri, request,
                    KakaoTokenResponse.class);
            log.debug("[카카오 토큰 응답] statusCode={}, body={}", response.getStatusCode(), response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("[카카오 토큰 요청 실패] 에러: {}, 메시지: {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private KakaoUserResponse requestUser(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                KakaoUserResponse.class);

        return response.getBody();
    }
}
