package com.ssafy.yumcoach.auth.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailAuthMailServiceImpl implements EmailAuthMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    private static final SecureRandom random = new SecureRandom();

    @Override
    public String sendAuthCode(String email) {
        String code = generate6DigitCode();
        sendMail(email, code);
        return code;
    }

    private void sendMail(String to, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("회원 가입을 위한 이메일 인증");
            helper.setText(
                    """
                    아래의 인증번호를 입력하여 이메일 인증을 완료해주세요.<br><br>
                    <b>인증번호: %s</b>
                    """.formatted(code),
                    true
            );
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("이메일 인증 메일 전송 실패", e);
        }
    }

    private String generate6DigitCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}