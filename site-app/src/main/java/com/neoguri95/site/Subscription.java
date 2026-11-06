package com.neoguri95.site;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    public enum Status { PENDING, ACTIVE, UNSUBSCRIBED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String email;

    @Column(name = "confirmation_token", nullable = false, unique = true)
    private UUID confirmationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected Subscription() {
    }

    public Subscription(String email, UUID confirmationToken) {
        this.email = email;
        this.confirmationToken = confirmationToken;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void confirm() {
        this.status = Status.ACTIVE;
        this.confirmedAt = Instant.now();
    }

    public void unsubscribe() {
        this.status = Status.UNSUBSCRIBED;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public UUID getConfirmationToken() { return confirmationToken; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
