package com.microshop.notification.service;

import com.microshop.notification.entity.EmailStatus;
import com.microshop.notification.entity.Notification;
import com.microshop.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sends queued emails. Decoupled from the Kafka handler on purpose: a slow or dead mail server can never block or
 * fail event processing, and failed sends are retried (up to max-attempts) instead of lost.
 * Delivery is at-least-once: a crash between "sent" and "commit" could repeat one email.
 */
@Slf4j
@Component
public class EmailDispatcher {

    private final NotificationRepository notifications;
    private final JavaMailSender mailSender;
    private final String from;
    private final int maxAttempts;

    public EmailDispatcher(NotificationRepository notifications, JavaMailSender mailSender,
                           @Value("${app.mail.from}") String from,
                           @Value("${app.mail.max-attempts:5}") int maxAttempts) {
        this.notifications = notifications;
        this.mailSender = mailSender;
        this.from = from;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.mail.poll-interval-ms:2000}")
    @Transactional
    public void dispatch() {
        List<Notification> pending = notifications.findTop50ByEmailStatusOrderByCreatedAtAsc(EmailStatus.PENDING);
        for (Notification n : pending) {
            n.setEmailAttempts(n.getEmailAttempts() + 1);
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(from);
                mail.setTo(n.getEmail());
                mail.setSubject(n.getTitle());
                mail.setText(NotificationTemplates.emailBody(n.getMessage()));
                mailSender.send(mail);
                n.setEmailStatus(EmailStatus.SENT);
            } catch (MailException e) {
                if (n.getEmailAttempts() >= maxAttempts) {
                    n.setEmailStatus(EmailStatus.FAILED);
                    log.error("Giving up on email for notification {} after {} attempts: {}",
                            n.getId(), n.getEmailAttempts(), e.getMessage());
                } else {
                    log.warn("Email for notification {} failed (attempt {}/{}), will retry: {}",
                            n.getId(), n.getEmailAttempts(), maxAttempts, e.getMessage());
                }
            }
        }
    }
}
