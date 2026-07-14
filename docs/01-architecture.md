# 아키텍처 — 기술 스택 / 모듈 구조

## 기술 스택 / 환경

| 항목 | 값 |
|------|-----|
| Java | 21 |
| Spring Boot | 3.4.1 (+ Spring Cloud OpenFeign) |
| Build | Maven (멀티 모듈) |
| MQ | RabbitMQ 3.13-management (Docker, 5672/15672 guest/guest) |
| DB | PostgreSQL (외부, localhost:5438 / db=wcs / user=wcs) |
| ORM | MyBatis 3.x (Mapper 인터페이스 + XML) |
| 기타 | Lombok (@SuperBuilder) |

## 모듈 구조 (5개)

```
ifmq/                        ← parent pom
├── common/                  ← 공통 DTO만. Spring/MQ/DB 의존 없음
│   └── com.example.common.dto
│       ├── WcsMessageBase        (messageType, messageId, refMessageId, sequenceNo, timestamp)
│       ├── 입고: InboundCmdDto(+InboundCmdDetail), InboundCompleteDto, InboundCancelDto,
│       │        BcrReadDto, InboundTaskDto/AckDto, InboundStartDto, InboundDoneDto/AckDto, MappingRegisterDto
│       ├── 출고: OutboundCmdDto(+OutboundCmdItem), OutboundCmdAckDto,
│       │        OutboundTaskDto/AckDto, OutboundDoneDto/AckDto
│       └── 공용: StationStatusDto
│
├── wcs-app/                 ← WMS 연계 MQ 어댑터 (port 9001, DB=inf.if_msg_log)
│   ├── consumer/  InboundCmdConsumer · InboundCancelConsumer · OutboundCmdConsumer
│   ├── producer/  InboundCompleteProducer · OutboundCmdAckProducer · InboundCmdProducer(테스트용)
│   ├── controller/ InternalController(/internal/inbound-complete) · TestController(/test/*)
│   ├── client/    ShuttleWcsClient → shuttle-wcs 위임 (inbound-order, outbound-order, outbound-start)
│   ├── cli/       WcsCliRunner — 1. INBOUND_COMPLETE 발행 / 2. 출고 시작
│   └── service/db MsgLogService · IfMsgLog(Mapper)  ← if_msg_log 적재/멱등/상태전이
│
├── shuttle-wcs/             ← WCS 비즈니스 모듈 (port 9002, DB=biz 스키마)
│   ├── controller/
│   │   ├── InboundOrderController   POST /internal/inbound-order    (wcs-app 위임 수신)
│   │   ├── OutboundOrderController  POST /internal/outbound-order   (〃, ACK 반환)
│   │   ├── OutboundStartController  POST /internal/outbound-start   (출고 시작 트리거)
│   │   ├── MappingController        POST /api/mapping               (PRE03 매핑 등록)
│   │   ├── RcsInboundController     POST /rcs/station-status · /rcs/bcr-read · /rcs/inbound-start · /rcs/inbound-done
│   │   └── RcsOutboundController    POST /rcs/outbound-done
│   ├── service/  InboundOrderService · MappingService · RcsInboundService
│   │             OutboundOrderService · OutboundStartService · OutboundDoneService
│   │             RcsMsgLogService(wcs_shuttle_msg_log SEND/RECEIVE)
│   └── client/   RcsClient(→rcs-mock: inbound-task, outbound-task) · WcsAppClient(→wcs-app: inbound-complete)
│
├── rcs-mock/                ← RCS/설비ECS 시뮬레이터 (port 9003, DB 없음)
│   ├── cli/  RcsCliRunner — 1. STATION_STATUS(입고) / 2. BCR_READ / 3. INBOUND_DONE
│   │                        4. STATION_STATUS(출고) / 5. OUTBOUND_DONE / 6. INBOUND_START
│   ├── controller/ RcsTaskController — POST /rcs/inbound-task · /rcs/outbound-task (ACK 동기 회신)
│   └── controller/ TestController — POST /test/* (CLI 메뉴 1:1 대응 REST, 도커용)
│
└── wms-mock/                ← WMS 시뮬레이터 (port 9004, DB 없음)
    ├── cli/  WmsCliRunner — 1. INBOUND_CMD / 2. INBOUND_CANCEL / 3. OUTBOUND_CMD
    ├── producer/ InboundCmdPublisher · InboundCancelPublisher · OutboundCmdPublisher
    ├── consumer/ InboundCompleteConsumer · OutboundCmdAckConsumer (화면 출력만)
    └── controller/ TestController — POST /test/* (CLI 메뉴 1:1 대응 REST, 도커용)
```

`wcs-app`의 CLI 메뉴(출고 시작 등)도 `POST /test/outbound-start`로 동일하게 REST 호출 가능
(TestController에 통합, 기존 inbound-cmd·inbound-complete 엔드포인트와 함께).

**호출 방향**: `wms-mock ⇄(MQ)⇄ wcs-app ⇄(Feign)⇄ shuttle-wcs ⇄(Feign/REST)⇄ rcs-mock`

- WMS↔WCS 는 RabbitMQ 비동기, WCS↔RCS 는 REST(Feign) 동기.
- WMS↔WCS 송수신 전문은 WCS 입장에서 `inf.if_msg_log`, WCS↔RCS 전문은 shuttle-wcs 기준으로 `biz.wcs_shuttle_msg_log` 에 적재.
