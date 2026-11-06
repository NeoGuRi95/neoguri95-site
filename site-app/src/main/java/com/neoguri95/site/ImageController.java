package com.neoguri95.site;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageController {

    private final AdminToken adminToken;
    private final ImageStorage images;

    public ImageController(AdminToken adminToken, ImageStorage images) {
        this.adminToken = adminToken;
        this.images = images;
    }

    @PostMapping("/admin/images")
    public ResponseEntity<Map<String, String>> upload(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam MultipartFile file) throws IOException {
        if (!adminToken.matches(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "관리자 토큰이 올바르지 않아요."));
        }
        try {
            return ResponseEntity.ok(Map.of("url", images.store(file)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
