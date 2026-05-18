package com.ior.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendVerificationCode(String to, String code, String type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("IOR <" + from + ">");
            helper.setTo(to);
            helper.setSubject(getSubject(type, code));
            helper.setText(loadTemplate(type, code), true); // true = HTML 模式

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /** 加载 HTML 模板并替换占位符 */
    private String loadTemplate(String type, String code) throws IOException {
        String templateName = switch (type) {
            case "register"        -> "register";
            case "login"           -> "login";
            case "change_email"    -> "change_email";
            case "reset_password"  -> "reset_password";
            default                -> "error";
        };

        ClassPathResource resource = new ClassPathResource("templates/mail/" + templateName + ".html");
        String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // 替换占位符
        return template.replace("{code}", code);
    }

    private String getSubject(String type, String code) {
        String scene = switch (type) {
            case "register"        -> "注册";
            case "login"           -> "登录";
            case "change_email"    -> "修改邮箱";
            case "reset_password"  -> "找回密码";
            default                -> "验证";
        };
        return " IOR " + scene + " 验证码 :"  +  code ;
    }
}