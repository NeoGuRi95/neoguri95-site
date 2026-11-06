package com.neoguri95.site;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Debezium이 CDC로 읽는 outbox 행. INSERT만 하고 수정하지 않는다. */
@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "subject_type", nullable = false, updatable = false, columnDefinition = "text")
    private String subjectType;

    @Column(name = "subject_id", nullable = false, updatable = false, columnDefinition = "text")
    private String subjectId;

    @Column(name = "event_type", nullable = false, updatable = false, columnDefinition = "text")
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String subjectType, String subjectId, String eventType, String payload) {
        this.id = id;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectId() { return subjectId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
