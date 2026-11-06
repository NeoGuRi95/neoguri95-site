package com.neoguri95.site;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

/**
 * 글 본문. 에디터가 보낸 HTML은 신뢰하지 않고 허용 목록으로 정제한 뒤 저장한다.
 *
 * @param text 목록 발췌와 검색에 쓰는 평문
 * @param html 화면에 그대로 출력하는 정제된 HTML
 * @param json 에디터(Tiptap) 원본 문서. 평문으로 발행한 글은 null
 */
public record PostContent(String text, String html, String json) {

    static final int MAX_HTML_LENGTH = 1_000_000;

    // 상대 경로(/uploads/...)를 허용 프로토콜 검사에 통과시키기 위한 기준 주소
    private static final String BASE_URI = "https://neoguri95.com/";

    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "h2", "h3", "strong", "em", "u", "s", "code", "pre",
                    "blockquote", "ul", "ol", "li", "hr", "a", "img")
            .addAttributes("a", "href")
            .addAttributes("ol", "start")
            .addAttributes("img", "src", "alt", "title")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow")
            .preserveRelativeLinks(true);

    public static PostContent fromEditor(String html, String json) {
        if (html != null && html.length() > MAX_HTML_LENGTH) {
            throw new IllegalArgumentException("본문이 너무 길어요.");
        }
        // 정제 결과를 들여쓰기하면 공백이 바뀌므로 prettyPrint를 끈다.
        var compact = new Document.OutputSettings().prettyPrint(false);
        Document doc = Jsoup.parseBodyFragment(
                Jsoup.clean(html == null ? "" : html, BASE_URI, SAFELIST, compact), BASE_URI);
        doc.outputSettings().prettyPrint(false);
        doc.select("img").attr("loading", "lazy");
        // 에디터가 끝에 자동으로 붙이는 빈 문단을 지운다.
        var last = doc.body().lastElementChild();
        while (last != null && last.nameIs("p") && last.children().isEmpty() && last.text().isBlank()) {
            last.remove();
            last = doc.body().lastElementChild();
        }
        boolean hasImage = !doc.select("img").isEmpty();
        String text = doc.body().text().trim();
        if (text.isEmpty() && !hasImage) {
            throw new IllegalArgumentException("제목과 본문을 입력해 주세요.");
        }
        return new PostContent(text, doc.body().html(), json);
    }

    public static PostContent fromPlainText(String body) {
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("제목과 본문을 입력해 주세요.");
        }
        var html = new StringBuilder();
        for (String paragraph : text.split("\\R\\s*\\R")) {
            html.append("<p>")
                    .append(String.join("<br>", paragraph.strip().lines().map(Entities::escape).toList()))
                    .append("</p>");
        }
        return new PostContent(text, html.toString(), null);
    }
}
