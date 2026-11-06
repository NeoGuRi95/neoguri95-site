package com.neoguri95.site;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostContentTest {

    @Test
    void keepsEditorFormatting() {
        var content = PostContent.fromEditor("""
                <h2>제목</h2><p><strong>굵게</strong> <em>기울임</em> <u>밑줄</u> <s>취소</s> <code>x</code></p>\
                <ul><li><p>하나</p></li></ul><ol start="3"><li><p>셋</p></li></ol>\
                <blockquote><p>인용</p></blockquote><pre><code>  a\n  b</code></pre><hr>""", "{}");

        assertThat(content.html())
                .contains("<h2>제목</h2>", "<strong>굵게</strong>", "<u>밑줄</u>", "<s>취소</s>",
                        "<ol start=\"3\">", "<blockquote>", "<pre><code>  a\n  b</code></pre>", "<hr>");
        assertThat(content.text()).startsWith("제목 굵게 기울임");
    }

    @Test
    void removesScriptsAndDangerousAttributes() {
        var content = PostContent.fromEditor("""
                <p onclick="alert(1)">안녕<script>alert(1)</script></p>\
                <a href="javascript:alert(1)">나쁜 링크</a><img src="x" onerror="alert(1)">\
                <iframe src="https://evil.example"></iframe><p style="color:red">스타일</p>""", null);

        assertThat(content.html())
                .doesNotContain("script", "onclick", "onerror", "javascript:", "iframe", "style")
                .startsWith("<p>안녕</p><a rel=\"noopener noreferrer nofollow\">나쁜 링크</a>");
    }

    @Test
    void keepsSafeLinksAndUploadedImages() {
        var content = PostContent.fromEditor("""
                <p><a href="https://example.com" target="_blank">링크</a></p>\
                <img src="/uploads/a.png" alt="사진"><img src="data:image/png;base64,AAAA">""", null);

        assertThat(content.html())
                .contains("<a href=\"https://example.com\" rel=\"noopener noreferrer nofollow\">링크</a>")
                .contains("<img src=\"/uploads/a.png\" alt=\"사진\" loading=\"lazy\">")
                .doesNotContain("target", "data:");
    }

    @Test
    void dropsTrailingEmptyParagraphs() {
        assertThat(PostContent.fromEditor("<p>본문</p><p></p><p> </p>", null).html()).isEqualTo("<p>본문</p>");
    }

    @Test
    void allowsImageOnlyPostButRejectsEmptyBody() {
        assertThat(PostContent.fromEditor("<img src=\"/uploads/a.png\">", null).text()).isEmpty();
        assertThatThrownBy(() -> PostContent.fromEditor("<p>  </p><script>x</script>", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsPlainTextToEscapedParagraphs() {
        var content = PostContent.fromPlainText("첫 줄\n<b>둘째</b>\n\n새 문단");

        assertThat(content.html()).isEqualTo("<p>첫 줄<br>&lt;b&gt;둘째&lt;/b&gt;</p><p>새 문단</p>");
        assertThat(content.json()).isNull();
    }
}
