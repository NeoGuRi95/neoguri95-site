package com.neoguri95.site;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    static final int MAX_AUTHOR = 40;
    static final int MAX_BODY = 1000;

    private final JdbcClient jdbc;

    public CommentService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(long postId) {
        return jdbc.sql("""
                SELECT author, body, created_at FROM comments
                WHERE post_id = :postId ORDER BY id
                """)
                .param("postId", postId)
                .query((rs, row) -> Map.<String, Object>of("author", rs.getString("author"),
                        "body", rs.getString("body"),
                        "createdAt", rs.getTimestamp("created_at")))
                .list();
    }

    /** @return false when the post does not exist */
    public boolean add(long postId, String author, String body) {
        String name = author == null ? "" : author.trim();
        String text = body == null ? "" : body.trim();
        if (name.isEmpty() || name.length() > MAX_AUTHOR || text.isEmpty() || text.length() > MAX_BODY) {
            throw new IllegalArgumentException("이름과 댓글 내용을 확인해 주세요.");
        }
        return jdbc.sql("""
                INSERT INTO comments (post_id, author, body)
                SELECT :postId, :author, :body
                WHERE EXISTS (SELECT 1 FROM posts WHERE id = :postId)
                """)
                .param("postId", postId).param("author", name).param("body", text)
                .update() == 1;
    }
}
