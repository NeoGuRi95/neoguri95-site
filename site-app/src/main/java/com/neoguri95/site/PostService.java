package com.neoguri95.site;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final JdbcClient jdbc;
    private final SiteEventWriter events;

    public PostService(JdbcClient jdbc, SiteEventWriter events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional
    public long publish(String title, String body) {
        if (title == null || title.isBlank() || title.length() > 200
                || body == null || body.isBlank()) {
            throw new IllegalArgumentException("제목과 본문을 입력해 주세요.");
        }
        long id = jdbc.sql("INSERT INTO posts (title, body) VALUES (:title, :body) RETURNING id")
                .param("title", title.trim()).param("body", body.trim())
                .query(Long.class).single();
        events.write("Post", Long.toString(id), "PostPublished",
                Map.of("postId", id, "title", title.trim(), "path", "/posts/" + id));
        return id;
    }
}
