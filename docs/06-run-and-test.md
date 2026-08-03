# 빌드 · 실행 · 테스트

## 로컬 mvn 실행 (CLI 메뉴 조작)

```bash
docker compose up -d rabbitmq           # RabbitMQ만 (최초 1회). Postgres는 별도 로컬 Docker(5438)
psql -h localhost -p 5438 -U wcs -d wcs -f shuttle-wcs/src/main/resources/sql/schema.sql

mvn install -DskipTests                 # 반드시 루트에서 (common 선행 빌드 필요)

# 터미널 4개
cd wcs-app     && mvn spring-boot:run   # 9001 + CLI
cd shuttle-wcs && mvn spring-boot:run   # 9002
cd rcs-mock    && mvn spring-boot:run   # 9003 + CLI
cd wms-mock    && mvn spring-boot:run   # 9004 + CLI
```
base-url은 각 application.yml에서 환경변수로 override (`SHUTTLE_WCS_BASE_URL`, `RCS_MOCK_BASE_URL`, `WCS_APP_BASE_URL`).

## 전체 도커 실행 (앱 4종 컨테이너화, CLI 대신 REST)

앱 4개는 도커로, Postgres는 기존 로컬 Docker(5438) 그대로 사용(`docker-compose.yml`에 미포함).
컨테이너 CLI는 표준입력이 없어 조작 불가 — **모든 CLI 메뉴는 `TestController`의 `POST /test/*`로 1:1 대응**.

```bash
docker compose up -d --build            # rabbitmq + wcs-app + shuttle-wcs + rcs-mock + wms-mock

# 입고~출고 전체 시나리오 (기본값 사용, body 생략 가능)
curl -X POST http://localhost:9004/test/inbound-cmd
curl -X POST http://localhost:9003/test/station-status-in
curl -X POST http://localhost:9003/test/bcr-read
curl -X POST http://localhost:9003/test/inbound-start    # 착수 → 스테이션 해제
curl -X POST http://localhost:9003/test/inbound-done
curl -X POST http://localhost:9004/test/outbound-cmd
curl -X POST http://localhost:9003/test/station-status-out
curl -X POST http://localhost:9001/test/user-outbound-request  # 작업자 트리거 → OUTBOUND_TASK 발행
curl -X POST http://localhost:9003/test/outbound-start    # 설비 착수 → OUTBOUNDING 전이 (RCS)
curl -X POST http://localhost:9003/test/outbound-done

# 작업(TASK) 이력 조회 — 전문 payload 없이 작업 진행 상태 확인
curl "http://localhost:9002/api/tasks?limit=20"
curl "http://localhost:9002/api/tasks?taskType=OUTBOUND&taskStatus=STARTED"
curl "http://localhost:9002/api/tasks/EP0001-1"   # 입고·출고 각 1건(같은 wcsTaskId 공유)

docker compose logs -f wcs-app          # 개별 서비스 로그
docker compose down                     # 종료 (RabbitMQ 볼륨 유지)
```
- 매핑 등록(PRE03)은 `/test`에 없음 — 실제 API 직접: `POST :9002/api/mapping {"eqpPalletId":"...","palletId":"..."}`.
- Linux Docker Engine은 `host.docker.internal` 미지원이라 `extra_hosts: host-gateway`를 wcs-app·shuttle-wcs에 매핑(맥/윈도우는 자동).
- 코드 변경 후 `docker compose up -d --build`로 재빌드.
- `Dockerfile` 루트 1개, 멀티스테이지 + `--target`으로 4개 이미지(공유 `build` 스테이지 캐시 공유).
- wcs-app·rcs-mock·wms-mock은 CommandLineRunner(CLI)가 기동 시 stdin을 읽으므로 compose에 `stdin_open: true`+`tty: true` 필요(없으면 EOF 예외로 컨테이너 즉시 종료).

## Postman으로 "실제 인터페이스처럼" 테스트

- **RCS→WCS (REST)**: shuttle-wcs를 직접 호출 = 실제 그 자체. 예 `POST localhost:9002/rcs/inbound-done` (전체 JSON body).
- **WMS→WCS (MQ)**: HTTP 엔드포인트가 아니라 큐 발행. Postman으로 하려면 **RabbitMQ Management API**(실제 버스에 직접 발행):
  ```
  POST http://localhost:15672/api/exchanges/%2F/wcs.topic/publish
  Authorization: Basic  guest / guest
  Content-Type: application/json
  ```
  Body (봉투 형식 — 실제 메시지는 payload에 이스케이프 문자열로):
  ```json
  {
    "properties": { "content_type": "application/json" },
    "routing_key": "wms.inbound.cmd",
    "payload_encoding": "string",
    "payload": "{\"messageType\":\"INBOUND_CMD\", ... }"
  }
  ```
  - 응답 `{"routed": true}` = 큐 도달. `false` = 매칭 큐 없음(주로 wcs-app 미기동 → 큐/바인딩 미생성).
  - 출고는 `routing_key`를 `wms.outbound.cmd`로.
  - 이스케이프가 번거로우면 관리 UI(`:15672` → Exchanges → `wcs.topic` → Publish message) 웹폼 사용.
- `/test/*`(wms-mock 등)는 시뮬레이터 편의 경로 — raw JSON을 봉투/이스케이프 없이 그대로 보낼 수 있으나 실제 버스는 아님.

## 검증 명령

```bash
mvn -q -pl common install              # 계약(DTO) 변경 후 우선 빌드
mvn -q -pl shuttle-wcs -am install -DskipTests
mvn -q install -DskipTests             # 전체 통합 빌드

docker exec rabbitmq rabbitmqctl purge_queue wcs.queue.outbound.cmd   # 큐 비우기
```

주요 확인 쿼리:
```sql
SELECT * FROM inf.if_msg_log ORDER BY log_id DESC LIMIT 10;
SELECT * FROM biz.wcs_shuttle_msg_log ORDER BY log_id DESC LIMIT 10;

-- 작업(TASK) 이력 — 전문 payload 없이 작업 단위로 확인
SELECT wcs_task_id, task_type, task_status, pallet_id, order_task_id, station_id,
       shuttle_id, qty, dispatched_at, started_at, completed_at
  FROM biz.wcs_task_h ORDER BY dispatched_at DESC LIMIT 20;
SELECT * FROM biz.wcs_vw_available_inventory;
SELECT eqp_pallet_id, pallet_id, map_status, location, cycle_no FROM biz.wcs_eqp_pallet_map;
SELECT task_id, cmd_status FROM biz.wcs_outbound_order_h;
```
