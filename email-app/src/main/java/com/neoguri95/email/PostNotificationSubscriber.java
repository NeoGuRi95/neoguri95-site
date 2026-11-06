package com.neoguri95.email;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** site-app의 구독 완료·해지 이벤트로 갱신하는 구독자 투영. */
@Entity
@Table(name = "post_notification_subscribers")
public class PostNotificationSubscriber {

    @Id
    @Column(columnDefinition = "text")
    private String email;

    @Column(nullable = false)
    private UUID token;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    protected PostNotificationSubscriber() {
    }

    public PostNotificationSubscriber(String email, UUID token) {
        this.email = email;
        this.token = token;
        this.confirmedAt = Instant.now();
    }

    public void reconfirm(UUID token) {
        this.token = token;
        this.confirmedAt = Instant.now();
    }

    public String getEmail() { return email; }
    public UUID getToken() { return token; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
