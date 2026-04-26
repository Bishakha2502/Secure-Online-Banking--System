package com.banking.application.service;

import com.banking.application.dto.EmailDetails;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // SIMPLE EMAIL (Credit/Debit/Transfer Alerts)
    @Override
    public void sendEmailAlert(EmailDetails emailDetails) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(senderEmail);
            mailMessage.setTo(emailDetails.getRecipient());
            mailMessage.setSubject(emailDetails.getSubject());
            mailMessage.setText(emailDetails.getMessageBody());

            javaMailSender.send(mailMessage);

            log.info("Email alert sent successfully to {}", emailDetails.getRecipient());

        } catch (MailException e) {
            log.error("Error sending email alert", e);
            throw new RuntimeException("Failed to send email alert", e);
        }
    }

    // EMAIL WITH ATTACHMENT
    @Override
    public void sendEmailWithAttachment(EmailDetails emailDetails) {

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            // UTF-8 support added (VERY IMPORTANT)
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(emailDetails.getRecipient());
            helper.setSubject(emailDetails.getSubject());
            helper.setText(emailDetails.getMessageBody(), false);

            // ATTACHMENT CHECK
            if (emailDetails.getAttachment() != null && !emailDetails.getAttachment().isEmpty()) {

                File fileObj = new File(emailDetails.getAttachment());

                if (!fileObj.exists()) {
                    throw new RuntimeException("Attachment file not found: " + emailDetails.getAttachment());
                }

                FileSystemResource file = new FileSystemResource(fileObj);

                helper.addAttachment(
                        Objects.requireNonNull(file.getFilename()),
                        file
                );
            }

            javaMailSender.send(mimeMessage);

            log.info("Email with attachment sent successfully to {}", emailDetails.getRecipient());

        } catch (MessagingException e) {
            log.error("Messaging error while sending email with attachment", e);
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }
}