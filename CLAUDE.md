# IFMQ — WCS/WMS RabbitMQ 통신 모듈

WMS(상위 시스템)와 WCS(창고제어) 간 RabbitMQ Topic Exchange 기반 비동기 메시지 통신 샘플.
모든 송수신 메시지는 WCS 입장에서 PostgreSQL `inf.if_msg_log` 테이블에 적재된다.

> 이 문서는 **서브에이전트(Task 도구) 실습용**으로 작성되었다. 모듈 경계·작업 분배 가이드·
> 검증 명령이 명시되어 있어, 여러 서브에이전트에게 병렬로 일을 나눠주는 흐름을 연습할 수 있다.

---

## 1. 기술 스택 / 환경

| 항목 | 값 |
|------|-----|
| Java | 21 |
| Spring Boot | 3.4.1 |
| Build | Maven (멀티 모듈) |
| MQ | RabbitMQ 3.13-management (Docker) |
| DB | PostgreSQL (외부, 로컬 Docker) |
| ORM | MyBatis 3.0.3 (Mapper 인터페이스 + XML) |
| 기타 | Lombok (@SuperBuilder) |

---

## 2. 모듈 구조

```
ifmq/                                  ← parent pom (멀티 모듈)
├── common/                            ← 공통 DTO. Spring/MQ/DB 의존 없음
│   └── com.example.common.dto
│       ├── WcsMessageBase             ← 추상 베이스 (messageType, messageId, refMessageId, sequenceNo, timestamp)
│       ├── InboundCmdDto              ← WMS→WCS
│       └── InboundCompleteDto         ← WCS→WMS
│
├── wcs-app/                           ← WCS 메인 앱 (REST + CLI + DB), port 9001
│   └── com.example.wcsapp
│       ├── config/                    ← RabbitMQConfig, RabbitMQProperties
│       ├── consumer/InboundCmdConsumer    ← INBOUND_CMD 수신 + 상태전이 + DB
│       ├── producer/
│       │   ├── InboundCompleteProducer    ← INBOUND_COMPLETE 발행 + DB
│       │   └── InboundCmdProducer         ← 테스트 전용 발행기 (DB 로그 X)
│       ├── controller/TestController      ← POST /test/inbound-cmd, /test/inbound-complete
│       ├── cli/WcsCliRunner               ← CLI: INBOUND_COMPLETE 발행
│       ├── service/MsgLogService          ← 로그 적재/상태전이/멱등 처리
│       └── db/                            ← IfMsgLog, IfMsgLogMapper
│
└── wms-mock/                          ← WMS 시뮬레이터 (CLI 전용, DB 없음)
    └── com.example.wmsmock
        ├── config/                    ← WmsRabbitMQConfig, WmsRabbitMQProperties
        ├── producer/InboundCmdPublisher       ← INBOUND_CMD 발행
        ├── consumer/InboundCompleteConsumer   ← INBOUND_COMPLETE 수신 (화면 출력만)
        └── cli/WmsCliRunner                    ← CLI: INBOUND_CMD 발행
```

---

## 3. 빌드 & 실행

```bash
# 0. RabbitMQ 기동 (최초 1회)
docker compose up -d                 # AMQP 5672 / Management UI 15672 (guest/guest)

# 1. 전체 빌드 — 멀티 모듈이므로 반드시 루트에서. common 먼저 빌드되어야 wcs-app/wms-mock 컴파일됨
mvn install -DskipTests

# 2. WCS 앱 실행 (터미널 1)
cd wcs-app && mvn spring-boot:run     # http://localhost:9001

# 3. WMS Mock 실행 (터미널 2)
cd wms-mock && mvn spring-boot:run    # CLI 메뉴 표시
```

> ⚠️ `wcs-app`을 단독 빌드하면 `common`을 못 찾아 실패한다. 항상 루트에서 `mvn install` 후 실행.

---

## 4. 메시지 계약 (Contract) — 서브에이전트 공유 기준

라우팅은 **하나의 Topic Exchange `wcs.topic`** 위에서 라우팅 키로 구분된다.

| 흐름 | 라우팅 키 | 큐 | 메시지 타입 |
|------|-----------|-----|-------------|
| WMS → WCS | `wms.inbound.cmd` | `wcs.queue.inbound.cmd` | INBOUND_CMD |
| WCS → WMS | `wcs.inbound.complete` | `wms.queue.inbound.complete` | INBOUND_COMPLETE |

### DTO 필드 (common 모듈 = 변경 시 전 모듈 영향)

```
WcsMessageBase (공통)
  messageType   String         메시지 종류 식별자
  messageId     String         메시지 고유 ID (멱등 키의 일부)
  refMessageId  String?        연관 메시지 ID (nullable)
  sequenceNo    int
  timestamp     LocalDateTime

InboundCmdDto extends WcsMessageBase    // INBOUND_CMD
  taskId, palletId, itemCode, lotId  String
  qty           int
  expireDate    LocalDate?     @JsonInclude(NON_NULL)

InboundCompleteDto extends WcsMessageBase   // INBOUND_COMPLETE
  taskId, palletId, itemCode, lotId  String
  qty           int
  status        String         COMPLETED | FAILED
  message       String
```

---

## 5. DB 스키마

```sql
-- 접속: localhost:5438 / db=wcs / user=wcs (application.yml 기본값, 환경변수로 override)
CREATE TABLE inf.if_msg_log (
    log_id          BIGSERIAL    PRIMARY KEY,
    direction       VARCHAR(10)  NOT NULL,   -- INBOUND | OUTBOUND  (WCS 기준)
    message_type    VARCHAR(50)  NOT NULL,
    message_id      VARCHAR(60)  NOT NULL,
    ref_message_id  VARCHAR(60),
    sequence_no     INTEGER,
    msg_timestamp   TIMESTAMP    NOT NULL,
    routing_key     VARCHAR(100),
    queue_name      VARCHAR(100),
    payload         JSONB        NOT NULL,    -- DTO를 ObjectMapper로 직렬화한 문자열, ::jsonb 캐스팅
    status          VARCHAR(20)  NOT NULL,    -- RECEIVED | PROCESSING | COMPLETED | FAILED
    error_msg       TEXT,
    processed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 멱등 보장: (direction, message_id) UNIQUE  (제약명 ux_msg_id)
```

### 상태 전이
```
INBOUND  (Consumer 수신):  RECEIVED → PROCESSING → COMPLETED  (예외 시 FAILED)
OUTBOUND (Producer 발행):  발행 성공 시 COMPLETED insert / 실패 시 FAILED insert
```

---

## 6. 핵심 규칙 (코드 수정 시 반드시 지킬 것)

1. **로그는 WCS 입장만 기록한다.** INBOUND_CMD 수신 = `INBOUND`, INBOUND_COMPLETE 발행 = `OUTBOUND`.
   테스트용 `InboundCmdProducer`(WMS 흉내)와 `wms-mock` 전체는 **DB 로그를 남기지 않는다.**
2. **INBOUND은 멱등 처리한다.** `(direction, message_id)` 중복이면 `MsgLogService.insertInbound`가
   `null`을 반환하고 Consumer는 skip한다. (`IfMsgLogMapper.existsByDirectionAndMessageId`)
3. **DTO는 `common`에만 정의.** wcs-app/wms-mock에서 DTO를 복제하지 말 것.
4. **라우팅 키/Exchange/큐 이름은 application.yml에서 관리.** 하드코딩 금지.
5. 코드 스타일: Lombok `@SuperBuilder` + `@NoArgsConstructor`, 로그/주석은 한국어.

---

## 7. 서브에이전트 작업 분배 가이드

### 7-1. 모듈별 오너십 (병렬 작업 단위)

| 모듈 | 책임 경계 | 건드리면 안 되는 것 |
|------|-----------|---------------------|
| `common` | DTO 정의/필드 추가 | 비즈니스 로직, Spring 의존성 |
| `wcs-app` | 수신·발행·DB·상태전이 | wms-mock 코드 의존 |
| `wms-mock` | 메시지 시뮬레이션 | DB 적재, 실제 비즈니스 로직 |

### 7-2. 병렬 분배가 적합한 작업
- **신규 메시지 타입 추가**: `common`(DTO) → `wcs-app`(Consumer/Producer) → `wms-mock`(반대편) 3-way 분할
- **양방향 대칭 작업**: "송신측 에이전트 / 수신측 에이전트" 동시 진행
- **모듈별 테스트 작성**: 각 모듈에 독립 에이전트 할당

### 7-3. 단일 에이전트로 처리할 작업 (순차 필수)
- `common` DTO **필드 변경/삭제** → 전 모듈 컴파일 영향
- 라우팅 키·Exchange 변경 → 양쪽 config 동시 수정
- DB 컬럼/제약 변경 → 엔티티·Mapper·XML 동반 수정

---

## 8. 실습 예제 시나리오 — INBOUND_CANCEL 추가

> 입고 취소 메시지(WMS→WCS)를 추가하는 작업을 **3개 서브에이전트로 병렬 분배**해보는 예제.

**선행(단일):** `common`에 `InboundCancelDto`(taskId, palletId, reason) 추가 → 계약 확정.

선행 완료 후 아래를 병렬 분배:

| 에이전트 | 범위 | 산출물 |
|----------|------|--------|
| A (wcs-app 수신) | `wms.inbound.cancel` 라우팅 키/큐 추가, `InboundCancelConsumer` 작성, DB 멱등 적재 | config + consumer |
| B (wms-mock 발행) | `InboundCancelPublisher` + CLI 메뉴 항목 추가 | publisher + cli |
| C (테스트) | wcs-app Consumer 단위 테스트 (멱등성 포함) | test 코드 |

**충돌 포인트(문서화 목적):** A·B 모두 application.yml의 라우팅 키를 참조 → 키 이름은 선행 단계에서
이 표에 확정해 둬야 두 에이전트가 같은 값을 쓴다.

---

## 9. 검증 명령 (각 에이전트의 self-check)

```bash
mvn -q -pl common install              # 계약 변경 후 우선 빌드
mvn -q -pl wcs-app -am test            # wcs-app + 의존 모듈 테스트
mvn -q -pl wms-mock -am compile        # wms-mock 컴파일 확인
mvn -q install -DskipTests             # 전체 통합 빌드

# 런타임 동작 확인
curl -X POST http://localhost:9001/test/inbound-cmd       # INBOUND_CMD 발행→수신 1회전
docker exec rabbitmq rabbitmqctl purge_queue wcs.queue.inbound.cmd   # 큐 비우기
```

---

## 10. 제외 범위 (현 단계)
인증/보안 · Dead Letter Queue · 재시도 정책 · 앱 컨테이너화(docker-compose에 주석으로 골격만 존재).
