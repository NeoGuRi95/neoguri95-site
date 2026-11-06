-- body는 목록 발췌와 검색에 쓰는 평문으로 유지한다.
-- body_html은 서버에서 정제한 표시용 HTML, body_json은 에디터(Tiptap) 원본 문서다.
ALTER TABLE posts
    ADD COLUMN body_html text,
    ADD COLUMN body_json jsonb;
