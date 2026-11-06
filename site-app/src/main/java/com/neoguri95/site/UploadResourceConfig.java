package com.neoguri95.site;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UploadResourceConfig implements WebMvcConfigurer {

    private final ImageStorage images;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드 파일 이름은 UUID라 내용이 바뀌지 않으므로 오래 캐시한다.
        registry.addResourceHandler(ImageStorage.URL_PREFIX + "**")
                .addResourceLocations(images.root().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
