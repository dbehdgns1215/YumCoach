package com.ssafy.yumcoach.payments.controller;

import com.ssafy.yumcoach.auth.principal.CustomUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.Map;

import com.ssafy.yumcoach.payments.model.PaymentConfirmRequest;
import com.ssafy.yumcoach.payments.service.PaymentService;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final String secretKey;
    private final PaymentService paymentService;

    public PaymentController(@Value("${toss.secret-key:}") String secretKey,
            PaymentService paymentService) {
        this.secretKey = secretKey;
        this.paymentService = paymentService;
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@AuthenticationPrincipal CustomUserPrincipal user,
            @RequestBody PaymentConfirmRequest request) {
        try {
            Integer userId = (user != null) ? user.getUserId() : request.getUserId();

            if (secretKey == null || secretKey.isBlank()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "TOSS_SECRET_KEY가 설정되지 않았습니다. 테스트 키를 환경변수에 설정한 뒤 다시 시도해주세요."));
            }

            String paymentKey = request.getPaymentKey();
            String orderId = request.getOrderId();
            Integer amount = request.getAmount();

            if (paymentKey == null || orderId == null || amount == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "paymentKey, orderId, amount는 필수입니다."));
            }

            // Toss Payments API 호출
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.tosspayments.com/v1/payments/confirm";

            // Authorization 헤더 설정 (시크릿 키를 Base64 인코딩)
            String auth = secretKey + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);

            Map<String, Object> body = Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // 결제 성공 시 DB에 저장
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> paymentData = response.getBody();
                if (userId == null) {
                    log.warn(
                            "Confirm without authenticated user; orderId={}, subscription not linked to user unless userId provided.",
                            orderId);
                }
                paymentService.recordPaymentAndSubscription(paymentData, userId, request.getPlanType());
                return ResponseEntity.ok(paymentData);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "결제 승인 실패"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}