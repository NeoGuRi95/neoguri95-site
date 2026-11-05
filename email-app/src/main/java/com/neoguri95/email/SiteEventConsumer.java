package com.neoguri95.email;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SiteEventConsumer {

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final String baseUrl;

    public SiteEventConsumer(JdbcClient jdbc, JsonMapper json,
            @Value("${site.base-url}") String baseUrl) {
        this.jdbc = jdbc;
        this.json = json;
        this.baseUrl = baseUrl;
    }

    @KafkaListener(topics = "site-events")
    @Transactional
    public void onEvent(String raw) {
        JsonNode event = json.readTree(raw);
        UUID eventId = UUID.fromString(event.path("eventId").asText());
        int inserted = jdbc.sql("INSERT INTO processed_events (event_id) VALUES (:id) ON CONFLICT DO NOTHING")
                .param("id", eventId).update();
        if (inserted == 0) return;

        switch (event.path("eventType").asText()) {
            case "SubscriptionRequested" -> {
                String email = event.path("email").asText();
                String url = baseUrl + "/subscriptions/confirm?token=" + event.path("token").asText();
                enqueue(eventId, email, "CONFIRMATION", "neoguri95 새 글 알림 구독 확인",
                        "새 글 알림 구독을 완료하려면 아래 링크를 열어주세요.\n\n" + url);
            }
            case "UnsubscribeRequested" -> {
                String email = event.path("email").asText();
                String url = baseUrl + "/subscriptions/unsubscribe?token=" + event.path("token").asText();
                enqueue(eventId, email, "UNSUBSCRIBE", "neoguri95 새 글 알림 구독 해지",
                        "새 글 알림 구독을 해지하려면 아래 링크를 열어주세요.\n"
                                + "직접 요청하지 않았다면 이 메일은 무시해도 됩니다.\n\n" + url);
            }
            case "SubscriptionConfirmed" -> jdbc.sql("""
                    INSERT INTO post_notification_subscribers (email, token) VALUES (:email, :token)
                    ON CONFLICT (email) DO UPDATE SET token = EXCLUDED.token, confirmed_at = now()
                    """).param("email", event.path("email").asText())
                    .param("token", UUID.fromString(event.path("token").asText())).update();
            case "SubscriptionUnsubscribed" -> {
                String email = event.path("email").asText();
                jdbc.sql("DELETE FROM post_notification_subscribers WHERE email = :email")
                        .param("email", email).update();
                jdbc.sql("UPDATE mail_jobs SET status = 'CANCELED' WHERE recipient = :email AND kind = 'POST' AND status = 'PENDING'")
                        .param("email", email).update();
            }
            case "PostPublished" -> jdbc.sql("""
                    INSERT INTO mail_jobs (event_id, recipient, kind, subject, body)
                    SELECT :eventId, email, 'POST', :subject,
                      :body || E'\n\n구독 해지: ' || :baseUrl || '/subscriptions/unsubscribe?token=' || token::text
                    FROM post_notification_subscribers
                    ON CONFLICT (event_id, recipient) DO NOTHING
                    """)
                    .param("eventId", eventId)
                    .param("subject", "새 글: " + event.path("title").asText())
                    .param("body", "새 글이 올라왔습니다.\n\n" + event.path("title").asText()
                            + "\n" + baseUrl + event.path("path").asText())
                    .param("baseUrl", baseUrl)
                    .update();
            default -> throw new IllegalArgumentException("지원하지 않는 이벤트: " + event.path("eventType").asText());
        }
    }

    private void enqueue(UUID eventId, String recipient, String kind, String subject, String body) {
        jdbc.sql("""
                INSERT INTO mail_jobs (event_id, recipient, kind, subject, body)
                VALUES (:eventId, :recipient, :kind, :subject, :body)
                ON CONFLICT (event_id, recipient) DO NOTHING
                """).param("eventId", eventId).param("recipient", recipient).param("kind", kind)
                .param("subject", subject).param("body", body).update();
    }
}
