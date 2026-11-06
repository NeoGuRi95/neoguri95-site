# 프로젝트 규칙

## Gradle 모듈 이름

- 실행 가능한 Spring Boot 애플리케이션 모듈은 `<역할>-app` 형식으로 이름을 짓는다.
  - 현재 모듈은 `site-app`, `email-app`이다.
- 모듈 이름은 배포 단위의 역할을 나타낸다. 변경 가능한 구현 방식은 모듈 이름에 넣지 않는다.
  - OCP: 개방-폐쇄 원칙 준수
- Java 패키지는 `com.neoguri95.<역할>` 형식으로 구성한다.
  - 예시: `com.neoguri95.site`, `com.neoguri95.email`.
- `settings.gradle`, 실행 명령, 문서의 모듈 참조는 실제 디렉터리 이름과 일치시킨다.

## Pull Request

- 브랜치, PR 제목, 본문 작성 규칙은 [PR 작성 규칙](docs/pull-request.md)을 따른다. PR을 만들기 전에 이 문서를 읽는다.
