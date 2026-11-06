package com.neoguri95.site;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** 본문 이미지를 로컬 디렉터리에 저장한다. 파일 이름은 추측할 수 없게 UUID로 짓는다. */
@Component
public class ImageStorage {

    public static final String URL_PREFIX = "/uploads/";

    private final Path root;

    public ImageStorage(@Value("${site.upload-dir:data/uploads}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path root() {
        return root;
    }

    public String store(MultipartFile file) throws IOException {
        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = detectExtension(in.readNBytes(12));
        }
        if (extension == null) {
            throw new IllegalArgumentException("PNG, JPG, GIF, WEBP 이미지만 올릴 수 있어요.");
        }
        String name = UUID.randomUUID() + "." + extension;
        file.transferTo(root.resolve(name));
        return URL_PREFIX + name;
    }

    // 확장자나 Content-Type 대신 파일 시그니처로 형식을 판별한다. SVG는 스크립트를 품을 수 있어 받지 않는다.
    static String detectExtension(byte[] head) {
        if (startsWith(head, 0x89, 'P', 'N', 'G')) return "png";
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) return "jpg";
        if (startsWith(head, 'G', 'I', 'F', '8')) return "gif";
        if (startsWith(head, 'R', 'I', 'F', 'F') && head.length >= 12
                && Arrays.equals(Arrays.copyOfRange(head, 8, 12), new byte[] {'W', 'E', 'B', 'P'})) return "webp";
        return null;
    }

    private static boolean startsWith(byte[] head, int... signature) {
        if (head.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((head[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }
}
