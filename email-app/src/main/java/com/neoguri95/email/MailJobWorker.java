package com.neoguri95.email;

import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MailJobWorker {

    private static final Logger log = LoggerFactory.getLogger(MailJobWorker.class);

    private final JdbcClient jdbc;
    private final JavaMailSender mailSender;
    private final String from;

    public MailJobWorker(JdbcClient jdbc, JavaMailSender mailSender,
            @Value("${email.from}") String from) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
        this.from = from;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void sendNext() {
        jdbc.sql("""
                SELECT id, recipient, subject, body, attempts FROM mail_jobs
                WHERE status = 'PENDING' AND next_attempt_at <= now()
                ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED
                """)
                .query((ResultSet rs, int row) -> new Job(rs.getLong("id"),
                        rs.getString("recipient"), rs.getString("subject"),
                        rs.getString("body"), rs.getInt("attempts")))
                .optional().ifPresent(this::send);
    }

    private void send(Job job) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(job.recipient());
            message.setSubject(job.subject());
            message.setText(job.body());
            mailSender.send(message);
            jdbc.sql("UPDATE mail_jobs SET status = 'SENT', sent_at = now(), attempts = attempts + 1 WHERE id = :id")
                    .param("id", job.id()).update();
        } catch (Exception ex) {
            log.warn("메일 발송 실패: 작업 {}, 수신자 {}", job.id(), job.recipient(), ex);
            int delaySeconds = Math.min(3600, (int) Math.pow(2, Math.min(job.attempts() + 1, 11)));
            jdbc.sql("""
                    UPDATE mail_jobs SET attempts = attempts + 1,
                      next_attempt_at = now() + (:delay * interval '1 second')
                    WHERE id = :id
                    """).param("delay", delaySeconds).param("id", job.id()).update();
        }
    }

    private record Job(long id, String recipient, String subject, String body, int attempts) {}
}
