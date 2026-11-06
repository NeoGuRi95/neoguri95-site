package com.neoguri95.site;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class PostService {

    private final JdbcClient jdbc;
    private final SiteEventWriter events;
    private final JsonMapper json;

    @Transactional
    public long publish(String title, PostContent content) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("제목과 본문을 입력해 주세요.");
        }
        long id = jdbc.sql("""
                INSERT INTO posts (title, body, body_html, body_json)
                VALUES (:title, :body, :bodyHtml, CAST(:bodyJson AS jsonb))
                RETURNING id""")
                .param("title", title.trim())
                .param("body", content.text())
                .param("bodyHtml", content.html())
                .param("bodyJson", validJsonOrNull(content.json()))
                .query(Long.class).single();
        events.write("Post", Long.toString(id), "PostPublished",
                Map.of("postId", id, "title", title.trim(), "path", "/posts/" + id));
        return id;
    }

    // 에디터 원본은 다시 편집할 때만 쓰므로, 깨진 값이면 버리고 정제된 HTML만 저장한다.
    private String validJsonOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            json.readTree(value);
            return value;
        } catch (JacksonException e) {
            return null;
        }
    }
}
