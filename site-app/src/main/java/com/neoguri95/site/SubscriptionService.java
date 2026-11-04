package com.neoguri95.site;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private final JdbcClient jdbc;
    private final SiteEventWriter events;

    public SubscriptionService(JdbcClient jdbc, SiteEventWriter events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional
    public boolean requestSubscription(String address) {
        String email = address == null ? "" : address.trim().toLowerCase();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 254) {
            throw new IllegalArgumentException("올바른 이메일 주소를 입력해 주세요.");
        }
        UUID token = UUID.randomUUID();
        Optional<Long> id = jdbc.sql("""
                INSERT INTO subscriptions (email, confirmation_token, status)
                VALUES (:email, :token, 'PENDING')
                ON CONFLICT (email) DO UPDATE
                  SET confirmation_token = EXCLUDED.confirmation_token,
                      status = 'PENDING', created_at = now()
                WHERE subscriptions.status <> 'ACTIVE'
                RETURNING id
                """)
                .param("email", email).param("token", token)
                .query(Long.class).optional();
        if (id.isEmpty()) return false;
        events.write("Subscription", id.get().toString(), "SubscriptionRequested",
                Map.of("email", email, "token", token.toString()));
        return true;
    }

    @Transactional
    public boolean confirm(UUID token) {
        Optional<Subscription> subscription = jdbc.sql("""
                UPDATE subscriptions SET status = 'ACTIVE', confirmed_at = now()
                WHERE confirmation_token = :token AND status = 'PENDING'
                  AND created_at > now() - interval '24 hours'
                RETURNING id, email
                """)
                .param("token", token)
                .query((rs, row) -> new Subscription(rs.getLong("id"), rs.getString("email")))
                .optional();
        subscription.ifPresent(value -> events.write("Subscription", Long.toString(value.id()),
                "SubscriptionConfirmed", Map.of("email", value.email(), "token", token.toString())));
        return subscription.isPresent();
    }

    @Transactional
    public boolean unsubscribe(UUID token) {
        Optional<Subscription> subscription = jdbc.sql("""
                UPDATE subscriptions SET status = 'UNSUBSCRIBED'
                WHERE confirmation_token = :token AND status = 'ACTIVE'
                RETURNING id, email
                """)
                .param("token", token)
                .query((rs, row) -> new Subscription(rs.getLong("id"), rs.getString("email")))
                .optional();
        subscription.ifPresent(value -> events.write("Subscription", Long.toString(value.id()),
                "SubscriptionUnsubscribed", Map.of("email", value.email())));
        return subscription.isPresent();
    }

    private record Subscription(long id, String email) {}
}
