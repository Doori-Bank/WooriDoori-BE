package com.app.wooridooribe.service.mailService;

import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceIImpl implements MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    
    private static final String REDIS_KEY_PREFIX = "email:verification:";
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 3;

    /**
     * 회원가입 인증번호 발송
     * @param email 수신자 이메일
     * @return 인증번호 (6자리)
     */
    @Override
    public String sendVerificationCode(String email) {
        try {
            // 1. 6자리 인증번호 생성
            Random random = new Random();
            String verificationCode = String.format("%06d", random.nextInt(1000000));
            
            // 2. Redis에 저장 (3분 후 자동 만료)
            String redisKey = REDIS_KEY_PREFIX + email;
            redisTemplate.opsForValue().set(
                    redisKey, 
                    verificationCode, 
                    VERIFICATION_CODE_EXPIRE_MINUTES, 
                    TimeUnit.MINUTES
            );
            log.info("인증번호 Redis 저장 완료: {} -> {}", email, verificationCode);
            
            // 3. 이메일 발송
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom("ricky0130@naver.com");
            helper.setTo(email);
            helper.setSubject("🎉 우리두리 회원가입 인증번호입니다");
            helper.setText(createHtmlContent(verificationCode), true);  // true = HTML
            
            mailSender.send(mimeMessage);
            log.info("인증 이메일 발송 완료: {}", email);
            
            return verificationCode;
            
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", email, e);
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
    }
    
    /**
     * 인증번호 검증
     * @param email 이메일
     * @param code 사용자가 입력한 인증번호
     * @return 일치 여부
     */
    @Override
    public boolean verifyCode(String email, String code) {
        String redisKey = REDIS_KEY_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (savedCode == null) {
            log.error("인증번호 만료 또는 존재하지 않음: {}", email);
            throw new CustomException(ErrorCode.TIME_OUT);
        }
        
        if (!savedCode.equals(code)) {
            log.error("인증번호 불일치: {} (입력: {}, 저장: {})", email, code, savedCode);
            throw new CustomException(ErrorCode.AUTH_FAIL);
        }
        
        // 검증 성공 시 Redis에서 삭제
        redisTemplate.delete(redisKey);
        log.info("인증 성공: {}", email);
        
        return true;
    }
    
    /**
     * 임시 비밀번호 발송
     * @param email 수신자 이메일
     * @param name 회원 이름
     * @return 임시 비밀번호 (10자리)
     */
    @Override
    public String sendTemporaryPassword(String email, String name) {
        try {
            // 1. 10자리 임시 비밀번호 생성 (대소문자 + 숫자 + 특수문자)
            String tempPassword = generateTemporaryPassword();
            
            // 2. 이메일 발송
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom("ricky0130@naver.com");
            helper.setTo(email);
            helper.setSubject("🔐 우리두리 임시 비밀번호 안내");
            helper.setText(createPasswordResetHtmlContent(name, tempPassword), true);  // true = HTML
            
            mailSender.send(mimeMessage);
            log.info("임시 비밀번호 이메일 발송 완료: {}", email);
            
            return tempPassword;
            
        } catch (MessagingException e) {
            log.error("임시 비밀번호 이메일 발송 실패: {}", email, e);
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
    }
    
    /**
     * 임시 비밀번호 생성 (10자리: 대문자 + 소문자 + 숫자 + 특수문자)
     */
    private String generateTemporaryPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String special = "!@#$%^&*";
        String allChars = upperCase + lowerCase + numbers + special;
        
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        // 각 종류별로 최소 1개씩 포함
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(special.charAt(random.nextInt(special.length())));
        
        // 나머지 6자리는 랜덤
        for (int i = 0; i < 6; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // 문자 순서 섞기
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    /**
     * HTML 이메일 템플릿 생성 (인증번호용)
     */
    private String createHtmlContent(String verificationCode) {
        return createEmailTemplate(
            "👋 회원가입을 환영합니다! 👋",
            "안녕하세요! 🎉<br>우리두리 회원가입을 위한 인증번호를 보내드립니다.<br>아래 인증번호를 입력하여 가입을 완료해주세요.",
            "인증번호",
            verificationCode,
            "📋 인증번호를 클릭하여 복사해주세요",
            new String[]{
                "인증번호는 <strong style=\"color: #667eea;\">3분간 유효</strong>합니다.",
                "인증번호를 타인에게 공유하지 마세요.",
                "본인이 요청하지 않은 경우, 이 메일을 무시해주세요."
            }
        );
    }
    
    /**
     * HTML 이메일 템플릿 생성 (임시 비밀번호용)
     */
    private String createPasswordResetHtmlContent(String name, String tempPassword) {
        return createEmailTemplate(
            "🔐 임시 비밀번호가 발급되었습니다",
            String.format("안녕하세요, <strong>%s</strong>님! 👋<br>비밀번호 재설정 요청에 따라 임시 비밀번호를 발급해드립니다.<br>아래 임시 비밀번호로 로그인 후 <strong style=\"color: #e53e3e;\">반드시 비밀번호를 변경</strong>해주세요.", name),
            "임시 비밀번호",
            tempPassword,
            "🔑 임시 비밀번호를 클릭하여 복사해주세요",
            new String[]{
                "로그인 후 <strong style=\"color: #e53e3e;\">즉시 비밀번호를 변경</strong>해주세요.",
                "임시 비밀번호를 타인에게 공유하지 마세요.",
                "본인이 요청하지 않은 경우, 즉시 고객센터로 문의해주세요."
            }
        );
    }
    
    /**
     * 재사용 가능한 이메일 템플릿 생성
     */
    private String createEmailTemplate(String title, String description, String codeLabel, String code, String copyGuide, String[] warnings) {
        StringBuilder warningsList = new StringBuilder();
        for (String warning : warnings) {
            warningsList.append(String.format("<li>%s</li>", warning));
        }
        
        return String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f5f7fa;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f7fa; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <!-- 메인 컨테이너 -->
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); overflow: hidden;">
                                    
                                    <!-- 헤더 -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700;">
                                                우리두리
                                            </h1>
                                            <p style="margin: 10px 0 0 0; color: #e0e7ff; font-size: 14px;">
                                                함께하는 소비, 즐거운 절약 💰
                                            </p>
                                        </td>
                                    </tr>
                                    
                                    <!-- 본문 -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="margin: 0 0 20px 0; color: #1a202c; font-size: 22px; font-weight: 600; text-align: center;">
                                                %s
                                            </h2>
                                            <p style="margin: 0 0 30px 0; color: #4a5568; font-size: 15px; line-height: 1.6; text-align: center;">
                                                %s
                                            </p>
                                            
                                            <!-- 코드/비밀번호 박스 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="padding: 30px 0;">
                                                        <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 16px; padding: 35px 50px; display: inline-block; box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);">
                                                            <p style="margin: 0 0 12px 0; color: #e0e7ff; font-size: 14px; font-weight: 500;">
                                                                %s
                                                            </p>
                                                            <p style="margin: 0; color: #ffffff; font-size: 32px; font-weight: 700; letter-spacing: 4px; text-shadow: 0 2px 4px rgba(0,0,0,0.1); cursor: text; user-select: all; -webkit-user-select: all; -moz-user-select: all; -ms-user-select: all;">
                                                                %s
                                                            </p>
                                                        </div>
                                                        
                                                        <!-- 복사 안내 -->
                                                        <div style="margin-top: 15px; color: #718096; font-size: 13px;">
                                                            %s
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <!-- 안내 사항 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top: 30px; background-color: #f7fafc; border-radius: 12px; padding: 25px; border-left: 4px solid #667eea;">
                                                <tr>
                                                    <td>
                                                        <p style="margin: 0 0 15px 0; color: #2d3748; font-size: 14px; font-weight: 600;">
                                                            ⚠️ 유의사항
                                                        </p>
                                                        <ul style="margin: 0; padding-left: 20px; color: #718096; font-size: 13px; line-height: 2;">
                                                            %s
                                                        </ul>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    
                                    <!-- 푸터 -->
                                    <tr>
                                        <td style="background-color: #f7fafc; padding: 30px; text-align: center; border-top: 1px solid #e2e8f0;">
                                            <p style="margin: 0 0 10px 0; color: #a0aec0; font-size: 12px;">
                                                © 2025 Woori-Doori 
                                            </p>
                                            <p style="margin: 0; color: #cbd5e0; font-size: 11px;">
                                                이 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요. 📧
                                            </p>
                                        </td>
                                    </tr>
                                    
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, title, description, codeLabel, code, copyGuide, warningsList.toString());
    }

}
