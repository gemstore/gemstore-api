package com.gemstore.backend.services.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("GemStore Email Verification");
        message.setText(
                "Your verification OTP is: " + otp +
                        "\n\nThis OTP expires in 5 minutes."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String resetUrl) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset Your GemStore Password");
        message.setText(
                "You recently requested to reset your password for your GemStore account.\n\n" +
                        "Click the link below to set a new password:\n" +
                        resetUrl + "\n\n" +
                        "This link is valid for 15 minutes. If you did not request a password reset, please ignore this email."
        );

        mailSender.send(message);
    }
}