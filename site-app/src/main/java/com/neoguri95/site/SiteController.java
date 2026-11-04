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
    private final String adminToken;

    public SiteController(JdbcClient jdbc, SubscriptionService subscriptions, PostService posts,
            @Value("${site.admin-token:}") String adminToken) {
        this.jdbc = jdbc;
        this.subscriptions = subscriptions;
        this.posts = posts;
        this.adminToken = adminToken;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("posts", jdbc.sql("SELECT id, title, published_at FROM posts ORDER BY id DESC")
                .query((rs, row) -> Map.of("id", rs.getLong("id"),
                        "title", rs.getString("title"),
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
        model.addAttribute("post", post);
        return "post";
    }

    @PostMapping("/subscriptions")
    public String subscribe(@RequestParam String email) {
        subscriptions.requestSubscription(email);
        return "redirect:/?subscription=requested";
    }

    @GetMapping("/subscriptions/confirm")
    public String confirmPage(@RequestParam UUID token, Model model) {
        model.addAttribute("token", token);
        return "confirm";
    }

    @PostMapping("/subscriptions/confirm")
    public String confirm(@RequestParam UUID token, Model model) {
        model.addAttribute("confirmed", subscriptions.confirm(token));
        return "subscription-result";
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
