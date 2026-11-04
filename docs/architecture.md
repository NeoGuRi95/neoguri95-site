# 개발 메모

## 구성

| 구성 | 역할 |
|---|---|
| `site-app` | Thymeleaf 화면, 글 발행, 구독 신청·확정, outbox 기록 |
| PostgreSQL `neoguri95_site` | 글·구독·outbox 원본 데이터 |
| Debezium Connect | PostgreSQL `site_outbox_pub`의 INSERT를 CDC로 읽어 Kafka에 발행 |
| Kafka `site-events` | 구독 및 글 발행 이벤트 전달 |
| `email-app` / `neoguri95_email` | 구독자 투영, 이메일 발송 작업, 중복 처리 기록 |
| Mailpit | 로컬 SMTP와 메일 확인 화면 |

`site-app`의 `SubscriptionService`는 구독 신청·확정·해지를, `PostService`는 글 발행을 처리한다. 두 서비스는 각자의 DB 변경과 outbox 이벤트 기록을 한 트랜잭션으로 처리한다. Outbox의 `subject_type`은 `Subscription` 또는 `Post`이며 `subject_id`에는 해당 레코드 ID를 기록한다. Debezium은 두 종류의 이벤트를 `site-events` 토픽으로 전달한다. 이 토픽은 단일 파티션으로 운영해 구독 상태 변경과 글 발행의 순서를 유지한다. `email-app`은 구독 완료·해지 이벤트로 자체 구독자 목록을 갱신하고, 글 발행 이벤트에서 발송 작업을 생성한다. 작업자는 SMTP 발송 실패 시 재시도한다.

## 로컬 Compose 흐름

```mermaid
flowchart LR
    subgraph Host[호스트에서 Gradle로 실행]
        Site[site-app]
        Email[email-app]
    end
    subgraph Compose[compose.local.yaml]
        Bootstrap[flyway-bootstrap]
        PG[(PostgreSQL)]
        TopicInit[kafka-init]
        Kafka[(Kafka)]
        Connect[Debezium Connect]
        ConnectorInit[connector-init]
        Mailpit[Mailpit]
    end
    Bootstrap -->|DB 생성| PG
    Site -->|글·구독·outbox 저장| PG
    PG -->|논리 복제 WAL| Connect
    ConnectorInit -->|커넥터 등록| Connect
    Connect -->|site-events 발행| Kafka
    TopicInit -->|토픽 생성| Kafka
    Kafka -->|이벤트 소비| Email
    Email -->|구독자·메일 작업 저장| PG
    Email -->|SMTP 발송| Mailpit
```

`flyway-bootstrap`, `kafka-init`, `connector-init`은 초기화 후 종료하는 일회성 작업이다. `site-app`과 `email-app`은 Compose 서비스가 아니며 호스트에서 `bootRun`으로 실행한다. 앱별 Flyway 마이그레이션은 각 앱이 시작할 때 실행된다.

## 로컬 실행

```bash
docker compose -f compose.local.yaml stop connect
docker compose -f compose.local.yaml up -d --wait postgres kafka mailpit
docker compose -f compose.local.yaml run --rm flyway-bootstrap
docker compose -f compose.local.yaml run --rm kafka-init
SPRING_PROFILES_ACTIVE=local ./gradlew :site-app:bootRun
```

별도 터미널에서:

```bash
docker compose -f compose.local.yaml up -d connect
docker compose -f compose.local.yaml run --rm connector-init
SPRING_PROFILES_ACTIVE=local ./gradlew :email-app:bootRun
```

홈은 `http://localhost:8080`, 로컬 이메일은 `http://localhost:8025`에서 확인한다. 글 발행 예시:

```bash
curl -i -X POST http://localhost:8080/admin/posts \
  -H 'X-Admin-Token: local-admin-token' -H 'Content-Type: application/json' \
  -d '{"title":"첫 글","body":"안녕하세요."}'
```
