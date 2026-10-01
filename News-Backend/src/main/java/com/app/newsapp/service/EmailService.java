package com.app.newsapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

//    @Autowired
//    private JavaMailSender mailSender;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        sendSecureMail(toEmail,
                "The Daily Chronicle - Verify Your Account",
                "Verify Your Account",
                "Thank you for registering. Use the following security code to complete your verification pipeline.",
                otp);
    }

    @Async
    public void sendForgotPasswordOtp(String toEmail, String otp) {
        sendSecureMail(toEmail,
                "The Daily Chronicle - Password Reset Request",
                "Password Reset Request",
                "We received a request to reset your password. Use the secure verification code below to set a new password.",
                otp);
    }


    private void sendSecureMail(String toEmail, String subject, String headerTitle, String messageText, String otp) {
        try {
            String htmlContent = "<div style='font-family: Arial, sans-serif; border: 1px solid #e2e8f0; padding: 25px; border-radius: 8px; max-width: 480px; margin: 0 auto; color: #1e293b; background-color: #ffffff;'>"
                    + "<h2 style='color: #2563eb; margin-top: 0; font-size: 20px;'>" + headerTitle + "</h2>"
                    + "<p style='color: #475569; font-size: 14px; line-height: 1.6;'>" + messageText + "</p>"
                    + "<div style='background: #f8fafc; border: 1px dashed #cbd5e1; padding: 15px; border-radius: 6px; text-align: center; margin: 20px 0;'>"
                    + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #0f172a;'>" + otp + "</span>"
                    + "</div>"
                    + "<p style='font-size: 12px; color: #64748b;'>This code is strictly valid for <strong>3 minutes</strong>. If you did not request this operation, you can safely ignore this email.</p>"
                    + "<hr style='border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;'>"
                    + "<p style='font-size: 11px; color: #94a3b8; text-align: center;'>The Daily Chronicle Engine • Render Dev</p>"
                    + "</div>";

            // Brevo API ka expected JSON body banaya
            Map<String, Object> sender = new HashMap<>();
            sender.put("name", "The Daily Chronicle News Team");
            sender.put("email", senderEmail);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", toEmail);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", List.of(recipient));
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("Accept", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(BREVO_API_URL, request, String.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to transmit security email pipeline: " + e.getMessage(), e);
        }
    }
}
