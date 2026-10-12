package com.microshop.notification.service;

import com.microshop.notification.entity.EmailStatus;
import com.microshop.notification.entity.Notification;
import com.microshop.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDispatcherTest {

    @Mock NotificationRepository repo;
    @Mock JavaMailSender mailSender;

    private EmailDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new EmailDispatcher(repo, mailSender, "no-reply@microshop.local", 3);
    }

    private Notification pending(int attempts) {
        return Notification.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).email("a@b.com")
                .type("T").title("Order #1 confirmed").message("Thanks").emailStatus(EmailStatus.PENDING)
                .emailAttempts(attempts).createdAt(Instant.now()).build();
    }

    @Test
    void sendsMailAndMarksItSent() {
        Notification n = pending(0);
        when(repo.findTop50ByEmailStatusOrderByCreatedAtAsc(EmailStatus.PENDING)).thenReturn(List.of(n));

        dispatcher.dispatch();

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("a@b.com");
        assertThat(mail.getValue().getFrom()).isEqualTo("no-reply@microshop.local");
        assertThat(mail.getValue().getSubject()).isEqualTo("Order #1 confirmed");
        assertThat(mail.getValue().getText()).contains("Thanks");
        assertThat(n.getEmailStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(n.getEmailAttempts()).isEqualTo(1);
    }

    @Test
    void failedSendStaysPendingForARetryUntilTheAttemptLimit() {
        Notification n = pending(0);
        when(repo.findTop50ByEmailStatusOrderByCreatedAtAsc(EmailStatus.PENDING)).thenReturn(List.of(n));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        dispatcher.dispatch();

        assertThat(n.getEmailStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(n.getEmailAttempts()).isEqualTo(1);
    }

    @Test
    void givesUpAfterTheMaximumNumberOfAttempts() {
        Notification n = pending(2);     // the next attempt is the 3rd = the limit
        when(repo.findTop50ByEmailStatusOrderByCreatedAtAsc(EmailStatus.PENDING)).thenReturn(List.of(n));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        dispatcher.dispatch();

        assertThat(n.getEmailStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(n.getEmailAttempts()).isEqualTo(3);
    }

    @Test
    void oneBadEmailDoesNotStopTheOthers() {
        Notification bad = pending(0);
        Notification good = pending(0);
        when(repo.findTop50ByEmailStatusOrderByCreatedAtAsc(EmailStatus.PENDING)).thenReturn(List.of(bad, good));
        doThrow(new MailSendException("rejected")).doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        dispatcher.dispatch();

        assertThat(bad.getEmailStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(good.getEmailStatus()).isEqualTo(EmailStatus.SENT);
    }
}
