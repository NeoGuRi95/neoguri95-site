package com.neoguri95.site;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminToken {

    private final byte[] expected;

    public AdminToken(@Value("${site.admin-token:}") String token) {
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    public boolean matches(String token) {
        return expected.length > 0 && token != null
                && MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8));
    }
}
