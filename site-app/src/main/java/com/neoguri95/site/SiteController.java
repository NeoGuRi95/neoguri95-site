package com.neoguri95.site;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SiteController {

    private final JdbcClient jdbc;
    private final SubscriptionService subscriptions;
    private final PostService posts;
    private final CommentService comments;
    private final String adminToken;

    public SiteController(JdbcClient jdbc, SubscriptionService subscriptions, PostService posts,
            CommentService comments, @Value("${site.admin-token:}") String adminToken) {
        this.jdbc = jdbc;
        this.subscriptions = subscriptions;
        this.posts = posts;
        this.comments = comments;
        this.adminToken = adminToken;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("posts", jdbc.sql("""
                SELECT id, title, LEFT(REGEXP_REPLACE(body, '\\s+', ' ', 'g'), 120) AS excerpt, published_at
                FROM posts ORDER BY id DESC""")
                .query((rs, row) -> Map.of("id", rs.getLong("id"),
                        "title", rs.getString("title"),
                        "excerpt", rs.getString("excerpt"),
                        "publishedAt", rs.getTimestamp("published_at")))
                .list());
        return "home";
    }

    @GetMapping("/posts/{id}")
    public String post(@PathVariable long id, Model model) {
        var post = jdbc.sql("SELECT title, body, published_at FROM posts WHERE id = :id")
                .param("id", id)
                .query((rs, row) -> Map.of("title", rs.getString("title"),
                        "body", rs.getString("body"),
                        "publishedAt", rs.getTimestamp("published_at")))
                .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("postId", id);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments.list(id));
        return "post";
    }

    @PostMapping("/posts/{id}/comments")
    public String comment(@PathVariable long id, @RequestParam String author, @RequestParam String body) {
        if (!comments.add(id, author, body)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/posts/" + id + "#comments";
    }

    @PostMapping("/subscriptions")
    public String subscribe(@RequestParam String email) {
        subscriptions.requestSubscription(email);
        return "redirect:/?subscription=requested";
    }

    @GetMapping("/subscriptions/confirm")
    public String confirmPage(@RequestParam UUID token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("email", subscriptions.findPendingEmail(token).orElse(null));
        return "confirm";
    }

    @PostMapping("/subscriptions/confirm")
    public String confirm(@RequestParam UUID token, Model model) {
        model.addAttribute("confirmed", subscriptions.confirm(token));
        return "subscription-result";
    }

    @GetMapping("/subscriptions/unsubscribe-request")
    public String unsubscribeRequestPage() {
        return "unsubscribe-request";
    }

    @PostMapping("/subscriptions/unsubscribe-request")
    public String unsubscribeRequest(@RequestParam String email) {
        subscriptions.requestUnsubscribe(email);
        return "redirect:/subscriptions/unsubscribe-request?sent";
    }

    @GetMapping("/subscriptions/unsubscribe")
    public String unsubscribePage(@RequestParam UUID token, Model model) {
        model.addAttribute("token", token);
        return "unsubscribe";
    }

    @PostMapping("/subscriptions/unsubscribe")
    public String unsubscribe(@RequestParam UUID token, Model model) {
        model.addAttribute("unsubscribed", subscriptions.unsubscribe(token));
        return "unsubscribe-result";
    }

    @GetMapping("/write")
    public String writePage() {
        return "write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title, @RequestParam String body,
            @RequestParam(required = false) String token, Model model) {
        if (adminToken.isBlank() || !adminToken.equals(token)) {
            return writeError(model, title, body, "관리자 토큰이 올바르지 않아요.");
        }
        try {
            return "redirect:/posts/" + posts.publish(title, body);
        } catch (IllegalArgumentException e) {
            return writeError(model, title, body, e.getMessage());
        }
    }

    private String writeError(Model model, String title, String body, String error) {
        model.addAttribute("title", title);
        model.addAttribute("body", body);
        model.addAttribute("error", error);
        return "write";
    }

    @PostMapping("/admin/posts")
    public String publish(@RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody NewPost request) {
        if (adminToken.isBlank() || !adminToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return "redirect:/posts/" + posts.publish(request.title(), request.body());
    }

    public record NewPost(String title, String body) {}
}
