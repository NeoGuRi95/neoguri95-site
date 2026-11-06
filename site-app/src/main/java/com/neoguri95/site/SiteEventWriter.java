package com.neoguri95.site;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class SiteEventWriter {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public void write(String subjectType, String subjectId, String eventType, Map<String, ?> data) {
        UUID eventId = UUID.randomUUID();
        var payload = new HashMap<String, Object>(data);
        payload.put("eventId", eventId.toString());
        payload.put("eventType", eventType);
        jdbc.sql("""
                INSERT INTO outbox (id, subject_type, subject_id, event_type, payload)
                VALUES (:id, :subjectType, :subjectId, :eventType, CAST(:payload AS jsonb))
                """)
                .param("id", eventId)
                .param("subjectType", subjectType)
                .param("subjectId", subjectId)
                .param("eventType", eventType)
                .param("payload", json.writeValueAsString(payload))
                .update();
    }
}
