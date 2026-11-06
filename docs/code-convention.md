# 코드 컨벤션

## 빈 의존성 주입

- 스프링 빈의 의존성은 `private final` 필드로 선언하고, Lombok `@RequiredArgsConstructor`로 생성자 주입을 받는다.
  - 생성자를 직접 작성하지 않는다. 필드 주입(`@Autowired` 필드)과 setter 주입은 쓰지 않는다.
- 설정값은 `final` 필드에 `@Value`를 붙인다. 루트 `lombok.config`가 `@Value`를 생성자 파라미터로 복사한다.

  ```java
  @Component
  @RequiredArgsConstructor
  public class SiteEventConsumer {

      private final JdbcClient jdbc;
      @Value("${site.base-url}")
      private final String baseUrl;
  }
  ```

- 예외: 주입받은 값을 가공하거나 초기화 작업이 필요한 경우에만 생성자를 직접 작성한다.
  - 예: `AdminToken`(토큰을 바이트 배열로 변환), `ImageStorage`(업로드 디렉터리 생성)
