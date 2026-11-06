package com.neoguri95.site;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "body_html", columnDefinition = "text")
    private String bodyHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "body_json", columnDefinition = "jsonb")
    private String bodyJson;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    protected Post() {
    }

    public Post(String title, String body) {
        this.title = title;
        this.body = body;
        this.publishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getBodyHtml() { return bodyHtml; }
    public String getBodyJson() { return bodyJson; }
    public Instant getPublishedAt() { return publishedAt; }
}
