package com.neoguri95.site;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ImageStorageTest {

    @Test
    void detectsImageBySignature() {
        assertThat(ImageStorage.detectExtension(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A})).isEqualTo("png");
        assertThat(ImageStorage.detectExtension(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("jpg");
        assertThat(ImageStorage.detectExtension("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEqualTo("gif");
        assertThat(ImageStorage.detectExtension("RIFF\0\0\0\0WEBP".getBytes(StandardCharsets.US_ASCII))).isEqualTo("webp");
    }

    @Test
    void rejectsSvgAndOtherFiles() {
        assertThat(ImageStorage.detectExtension("<svg xmlns=".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageStorage.detectExtension("RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageStorage.detectExtension(new byte[0])).isNull();
    }
}
