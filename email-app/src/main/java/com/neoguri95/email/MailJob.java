package com.neoguri95.email;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mail_jobs", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "recipient"}))
public class MailJob {

    public enum Kind { CONFIRMATION, POST, UNSUBSCRIBE }

    public enum Status { PENDING, SENT, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, columnDefinition = "text")
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private Kind kind;

    @Column(nullable = false, columnDefinition = "text")
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected MailJob() {
    }

    public MailJob(UUID eventId, String recipient, Kind kind, String subject, String body) {
        this.eventId = eventId;
        this.recipient = recipient;
        this.kind = kind;
        this.subject = subject;
        this.body = body;
        this.status = Status.PENDING;
        this.nextAttemptAt = Instant.now();
    }

    public void markSent() {
        this.status = Status.SENT;
        this.sentAt = Instant.now();
    }

    public void retryAt(Instant nextAttemptAt) {
        this.attempts++;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void cancel() {
        this.status = Status.CANCELED;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getRecipient() { return recipient; }
    public Kind getKind() { return kind; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getSentAt() { return sentAt; }
}
