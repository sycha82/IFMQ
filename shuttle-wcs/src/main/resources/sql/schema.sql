-- ============================================================================
-- 4Way 셔틀 WCS 공통 도메인 스키마
-- 적용: psql -h localhost -p 5438 -U wcs -d wcs -f schema.sql
-- 스키마 biz 는 이미 생성되어 있다는 전제
--
-- 공통 정책:
-- · 감사 컬럼 : created_at · updated_at · created_by · updated_by 전 테이블 공통
-- · FK 미설정. 무결성은 Service 단일 트랜잭션(@Transactional)으로 보장
-- · CHECK : Y/N 류만 유지. enum 류는 코드 레벨 검증
-- ============================================================================


-- ① wcs_inbound_order_h — 입고 지시 헤더 (task 레벨 집계)
CREATE TABLE IF NOT EXISTS biz.wcs_inbound_order_h (
    task_id          VARCHAR(30)  NOT NULL,
    cmd_status       VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    recv_message_id  VARCHAR(50)  NOT NULL,
    last_message_id  VARCHAR(50)  NOT NULL,
    recv_count       SMALLINT     NOT NULL DEFAULT 1,
    received_at      TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at     TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
    created_by       VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by       VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_inbound_order_h PRIMARY KEY (task_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_inbound_order_h__status
    ON biz.wcs_inbound_order_h (cmd_status, received_at);

COMMENT ON TABLE  biz.wcs_inbound_order_h IS '입고 지시 헤더 — task 단위 메시지 추적 + 집계 결과 (WMS INBOUND_CMD)';
COMMENT ON COLUMN biz.wcs_inbound_order_h.task_id          IS 'WMS 입고 지시 task_id';
COMMENT ON COLUMN biz.wcs_inbound_order_h.cmd_status       IS 'RECEIVED · COMPLETED (task 내 활성 PalletId 전체 완료 여부 집계)';
COMMENT ON COLUMN biz.wcs_inbound_order_h.recv_message_id  IS '최초 수신 INBOUND_CMD message_id (if_msg_log 역추적)';
COMMENT ON COLUMN biz.wcs_inbound_order_h.last_message_id  IS '최종 수신 message_id (재수신 시 갱신)';
COMMENT ON COLUMN biz.wcs_inbound_order_h.recv_count       IS '재수신 누적 횟수';
COMMENT ON COLUMN biz.wcs_inbound_order_h.received_at      IS '최초 수신 시각';
COMMENT ON COLUMN biz.wcs_inbound_order_h.completed_at     IS 'task 내 활성(미취소) PalletId 전체 완료 시각';


-- ①-1 wcs_inbound_order_d — 입고 지시 상세 (PalletId 라인 + 라이프사이클)
CREATE TABLE IF NOT EXISTS biz.wcs_inbound_order_d (
    task_id       VARCHAR(30)  NOT NULL,
    pallet_id     VARCHAR(30)  NOT NULL,
    sku_code      VARCHAR(30)  NOT NULL,
    lot_id        VARCHAR(30)  NOT NULL DEFAULT 'N/A',
    line_no       SMALLINT     NULL,
    qty           INTEGER      NOT NULL,
    expire_date   DATE         NULL,
    mapped_at     TIMESTAMP    NULL,
    completed_at  TIMESTAMP    NULL,
    cancelled_at  TIMESTAMP    NULL,
    cancel_reason VARCHAR(200) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    created_by    VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by    VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_inbound_order_d PRIMARY KEY (task_id, pallet_id, sku_code, lot_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_inbound_order_d__pending_map
    ON biz.wcs_inbound_order_d (pallet_id)
    WHERE mapped_at IS NULL AND cancelled_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_wcs_inbound_order_d__pallet
    ON biz.wcs_inbound_order_d (pallet_id);

COMMENT ON TABLE  biz.wcs_inbound_order_d IS '입고 지시 상세 — INBOUND_CMD inboundDetail 원본 라인 + PalletId 라이프사이클';
COMMENT ON COLUMN biz.wcs_inbound_order_d.task_id       IS 'WMS 입고 지시 task_id (wcs_inbound_order_h 참조, FK 미설정)';
COMMENT ON COLUMN biz.wcs_inbound_order_d.pallet_id     IS 'WMS 발행 PalletId';
COMMENT ON COLUMN biz.wcs_inbound_order_d.sku_code      IS '품목 코드';
COMMENT ON COLUMN biz.wcs_inbound_order_d.lot_id        IS '로트 번호. lot 미관리 품목은 N/A (sentinel)';
COMMENT ON COLUMN biz.wcs_inbound_order_d.line_no       IS 'INBOUND_CMD inboundDetail 배열 내 순번 (표시·디버깅용)';
COMMENT ON COLUMN biz.wcs_inbound_order_d.qty           IS '요청 수량';
COMMENT ON COLUMN biz.wcs_inbound_order_d.expire_date   IS '유통기한. FEFO 품목만, 그 외 NULL';
COMMENT ON COLUMN biz.wcs_inbound_order_d.mapped_at     IS 'PalletId-EqpPalletId 매핑 등록 시각 (PRE 03)';
COMMENT ON COLUMN biz.wcs_inbound_order_d.completed_at  IS 'INBOUND_COMPLETE 발송 시각 (이 PalletId 라인 기준)';
COMMENT ON COLUMN biz.wcs_inbound_order_d.cancelled_at  IS '취소 시각 (PalletId 단위 취소 — 동일 팔렛 전 라인 동시 취소)';
COMMENT ON COLUMN biz.wcs_inbound_order_d.cancel_reason IS '취소 사유';


-- 입고 지시 집계 뷰
CREATE OR REPLACE VIEW biz.wcs_vw_inbound_order_summary AS
SELECT
    h.task_id,
    h.cmd_status,
    h.received_at,
    h.completed_at,
    COUNT(DISTINCT d.pallet_id) AS pallet_count,
    COUNT(DISTINCT d.pallet_id) FILTER (WHERE d.cancelled_at IS NOT NULL) AS cancel_count,
    COUNT(DISTINCT d.pallet_id) FILTER (
        WHERE d.cancelled_at IS NULL AND d.completed_at IS NOT NULL
    ) AS completed_pallet_count
FROM biz.wcs_inbound_order_h h
JOIN biz.wcs_inbound_order_d d ON d.task_id = h.task_id
GROUP BY h.task_id, h.cmd_status, h.received_at, h.completed_at;

COMMENT ON VIEW biz.wcs_vw_inbound_order_summary IS '입고 지시 task별 PalletId 집계 (pallet_count·cancel_count·completed_pallet_count)';


-- ② wcs_pallet_line_h — Pallet 적재 라인 (시계열, is_latest 플래그)
CREATE TABLE IF NOT EXISTS biz.wcs_pallet_line_h (
    pallet_id       VARCHAR(30)  NOT NULL,
    effective_from  TIMESTAMP    NOT NULL,
    sku_code        VARCHAR(30)  NOT NULL,
    lot_id          VARCHAR(30)  NOT NULL,
    qty             INTEGER      NOT NULL,
    expire_date     DATE         NULL,
    is_latest       CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (is_latest IN ('Y','N')),
    effective_to    TIMESTAMP    NULL,
    superseded_by   TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_pallet_line_h PRIMARY KEY (pallet_id, effective_from, sku_code, lot_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_wcs_pallet_line_h_latest
    ON biz.wcs_pallet_line_h (pallet_id, sku_code, lot_id)
    WHERE is_latest = 'Y';

CREATE OR REPLACE VIEW biz.wcs_vw_pallet_line_h AS
    SELECT
        pallet_id,
        sku_code,
        lot_id,
        qty,
        expire_date,
        effective_from,
        created_at,
        updated_at
    FROM biz.wcs_pallet_line_h
    WHERE is_latest = 'Y';


-- ③ wcs_eqp_pallet_m — EqpPallet 마스터 (물리 풀)
CREATE TABLE IF NOT EXISTS biz.wcs_eqp_pallet_m (
    eqp_pallet_id   VARCHAR(30)  NOT NULL,
    eqp_pallet_no   INTEGER      NULL,
    pallet_status   VARCHAR(20)  NOT NULL DEFAULT 'IN_USE',
    use_yn          CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y','N')),
    maint_reason    VARCHAR(200) NULL,
    registered_at   TIMESTAMP    NOT NULL,
    last_used_at    TIMESTAMP    NULL,
    memo            VARCHAR(300) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_eqp_pallet_m PRIMARY KEY (eqp_pallet_id)
);


-- ④ wcs_eqp_pallet_map — EqpPallet 매핑 (현재 상태)
CREATE TABLE IF NOT EXISTS biz.wcs_eqp_pallet_map (
    eqp_pallet_id   VARCHAR(30)  NOT NULL,
    task_id         VARCHAR(30)  NULL,
    pallet_id       VARCHAR(30)  NULL,
    map_status      VARCHAR(20)  NOT NULL DEFAULT 'EMPTY',
    location        VARCHAR(20)  NOT NULL DEFAULT 'IDLE',
    cycle_no        INTEGER      NOT NULL DEFAULT 0,
    mapped_at       TIMESTAMP    NULL,
    stored_at       TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_eqp_pallet_map PRIMARY KEY (eqp_pallet_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_eqp_pallet_map_pallet_id
    ON biz.wcs_eqp_pallet_map (pallet_id);
CREATE INDEX IF NOT EXISTS ix_wcs_eqp_pallet_map_status
    ON biz.wcs_eqp_pallet_map (map_status);


-- ⑤ wcs_eqp_pallet_map_h — 매핑 이력 (사이클링·상태 변경 보존)
CREATE TABLE IF NOT EXISTS biz.wcs_eqp_pallet_map_h (
    hst_seq         BIGSERIAL    NOT NULL,
    eqp_pallet_id   VARCHAR(30)  NOT NULL,
    cycle_no        INTEGER      NOT NULL,
    task_id         VARCHAR(30)  NULL,
    pallet_id       VARCHAR(30)  NULL,
    map_status      VARCHAR(20)  NOT NULL,
    location        VARCHAR(20)  NOT NULL,
    mapped_at       TIMESTAMP    NULL,
    stored_at       TIMESTAMP    NULL,
    event_type      VARCHAR(20)  NOT NULL,
    event_at        TIMESTAMP    NOT NULL,
    event_by        VARCHAR(30)  NOT NULL,
    note            VARCHAR(200) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_eqp_pallet_map_h PRIMARY KEY (hst_seq)
);

CREATE INDEX IF NOT EXISTS ix_wcs_eqp_pallet_map_h_eqp
    ON biz.wcs_eqp_pallet_map_h (eqp_pallet_id, cycle_no);
CREATE INDEX IF NOT EXISTS ix_wcs_eqp_pallet_map_h_pallet
    ON biz.wcs_eqp_pallet_map_h (pallet_id);


-- ⑥ wcs_station — 입고/출고 스테이션 상태 (RCS STATION_STATUS 수신 반영)
CREATE TABLE IF NOT EXISTS biz.wcs_station (
    station_id         VARCHAR(30)  NOT NULL,
    station_type       VARCHAR(20)  NOT NULL DEFAULT 'INBOUND',   -- INBOUND | OUTBOUND
    status             VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE | BUSY | DOWN
    cur_eqp_pallet_id  VARCHAR(30)  NULL,
    status_changed_at  TIMESTAMP    NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by         VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_station PRIMARY KEY (station_id)
);

COMMENT ON TABLE  biz.wcs_station IS '스테이션 상태 — RCS/설비ECS STATION_STATUS 수신분 반영 (스테이션 마스터 겸용, 최초 보고 시 자동 생성)';
COMMENT ON COLUMN biz.wcs_station.station_id        IS '스테이션 ID';
COMMENT ON COLUMN biz.wcs_station.station_type      IS 'INBOUND · OUTBOUND';
COMMENT ON COLUMN biz.wcs_station.status            IS 'AVAILABLE · BUSY · DOWN';
COMMENT ON COLUMN biz.wcs_station.cur_eqp_pallet_id IS 'BCR_READ로 스테이션에 진입한 현재 eqpPalletId';
COMMENT ON COLUMN biz.wcs_station.status_changed_at IS '최근 상태 변경 시각 (RCS 보고 기준)';


-- ⑦ wcs_shuttle_msg_log — RCS/설비ECS 연계 REST API 송수신 이력 (WCS 기준)
CREATE TABLE IF NOT EXISTS biz.wcs_shuttle_msg_log (
    log_id          BIGSERIAL    NOT NULL,
    direction       VARCHAR(10)  NOT NULL,   -- SEND | RECEIVE (Shuttle-WCS 기준)
    api_name        VARCHAR(30)  NOT NULL,   -- STATION_STATUS | BCR_READ | INBOUND_TASK | INBOUND_TASK_ACK | INBOUND_DONE | INBOUND_DONE_ACK
    message_id      VARCHAR(60)  NULL,
    ref_message_id  VARCHAR(60)  NULL,
    station_id      VARCHAR(30)  NULL,
    eqp_pallet_id   VARCHAR(30)  NULL,
    wcs_task_id     VARCHAR(60)  NULL,
    payload         JSONB        NOT NULL,
    result          VARCHAR(20)  NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_shuttle_msg_log PRIMARY KEY (log_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_shuttle_msg_log_eqp
    ON biz.wcs_shuttle_msg_log (eqp_pallet_id, created_at);
CREATE INDEX IF NOT EXISTS ix_wcs_shuttle_msg_log_task
    ON biz.wcs_shuttle_msg_log (wcs_task_id);

COMMENT ON TABLE  biz.wcs_shuttle_msg_log IS 'RCS/설비ECS REST 연계 송수신 이력 — Shuttle-WCS 기준 SEND(발신)/RECEIVE(수신)';
COMMENT ON COLUMN biz.wcs_shuttle_msg_log.direction      IS 'SEND(Shuttle-WCS→RCS) · RECEIVE(RCS→Shuttle-WCS)';
COMMENT ON COLUMN biz.wcs_shuttle_msg_log.api_name       IS 'API 식별자 (STATION_STATUS · BCR_READ · INBOUND_TASK · INBOUND_TASK_ACK ...)';
COMMENT ON COLUMN biz.wcs_shuttle_msg_log.wcs_task_id    IS 'Shuttle-WCS가 발급한 이동 task 식별자 (eqpPalletId-cycleNo)';
COMMENT ON COLUMN biz.wcs_shuttle_msg_log.payload        IS '요청/응답 DTO 원본 JSON';
COMMENT ON COLUMN biz.wcs_shuttle_msg_log.result         IS '응답류 로그의 result 필드 (ACCEPTED/REJECTED/OK 등), 요청류는 NULL';


-- ⑧ wcs_inventory — 재고 (INBOUND_DONE 시 (sku_code, location_id) 단위 수량 누적)
--    location_id 에 eqp_pallet_id 를 사용 (팔레트가 랙에 적재된 상태의 재고 위치)
--    pallet_id(WMS 운영 PalletId)·lot_id 는 표시·추적용 부가 정보 — PK 는 (sku_code, location_id) 그대로
CREATE TABLE IF NOT EXISTS biz.wcs_inventory (
    sku_code     VARCHAR(64)  NOT NULL,
    location_id  VARCHAR(64)  NOT NULL,
    pallet_id    VARCHAR(30)  NOT NULL,
    lot_id       VARCHAR(30)  NULL,
    quantity     INT8         NOT NULL DEFAULT 0,
    uom          VARCHAR(16)  NOT NULL DEFAULT 'EA',
    reserved_qty INT8         NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wcs_inventory_pkey PRIMARY KEY (sku_code, location_id),
    CONSTRAINT ck_biz_wcs_inventory_quantity_nonneg CHECK (quantity >= 0),
    CONSTRAINT ck_wcs_inventory_reserved CHECK (reserved_qty >= 0 AND reserved_qty <= quantity)
);

CREATE INDEX IF NOT EXISTS idx_biz_wcs_inventory_location ON biz.wcs_inventory (location_id);
CREATE INDEX IF NOT EXISTS idx_biz_wcs_inventory_sku      ON biz.wcs_inventory (sku_code);

COMMENT ON TABLE  biz.wcs_inventory IS '재고 — INBOUND_DONE 완료 시 (sku_code, location_id) 단위 수량 누적. location_id=eqp_pallet_id';
COMMENT ON COLUMN biz.wcs_inventory.sku_code     IS '품목 코드';
COMMENT ON COLUMN biz.wcs_inventory.location_id  IS '재고 위치 — 현재는 eqp_pallet_id (팔레트 단위 랙 재고)';
COMMENT ON COLUMN biz.wcs_inventory.quantity     IS 'on-hand 수량 (랙 물리 재고, 0 이상)';
COMMENT ON COLUMN biz.wcs_inventory.reserved_qty IS '예약 수량 — OUTBOUND_CMD 할당 시 팔렛 전체 예약. available = quantity - reserved_qty';
COMMENT ON COLUMN biz.wcs_inventory.uom          IS '수량 단위 (기본 EA)';
COMMENT ON COLUMN biz.wcs_inventory.pallet_id    IS 'WMS 운영 PalletId — eqp_pallet_id(location_id)에 매핑된 값. 재고 적재 시 항상 확정되므로 NOT NULL';
COMMENT ON COLUMN biz.wcs_inventory.lot_id       IS '로트 번호 (lot 미관리 품목은 NULL)';

-- 가용 재고 뷰 — available = on-hand - reserved
CREATE OR REPLACE VIEW biz.wcs_vw_available_inventory AS
    SELECT sku_code, location_id, pallet_id, lot_id,
           quantity, uom, reserved_qty,
           (quantity - reserved_qty) AS available_qty, updated_at
    FROM biz.wcs_inventory;

COMMENT ON VIEW biz.wcs_vw_available_inventory IS '가용 재고 — quantity(on-hand) - reserved_qty(예약)';


-- ⑨ wcs_outbound_order_h — 출고 지시 헤더 (task 레벨 집계) · 입고 헤더와 동일 구조
CREATE TABLE IF NOT EXISTS biz.wcs_outbound_order_h (
    task_id          VARCHAR(30)  NOT NULL,
    cmd_status       VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    recv_message_id  VARCHAR(50)  NOT NULL,
    last_message_id  VARCHAR(50)  NOT NULL,
    recv_count       SMALLINT     NOT NULL DEFAULT 1,
    received_at      TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at     TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
    created_by       VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by       VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_outbound_order_h PRIMARY KEY (task_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_outbound_order_h__status
    ON biz.wcs_outbound_order_h (cmd_status, received_at);

COMMENT ON TABLE  biz.wcs_outbound_order_h IS '출고 지시 헤더 — task 단위 메시지 추적 + 집계 결과 (WMS OUTBOUND_CMD)';
COMMENT ON COLUMN biz.wcs_outbound_order_h.task_id          IS 'WMS 출고 지시 task_id';
COMMENT ON COLUMN biz.wcs_outbound_order_h.cmd_status       IS 'RECEIVED · COMPLETED (task 내 활성 PalletId 전체 출고 완료 여부 집계)';
COMMENT ON COLUMN biz.wcs_outbound_order_h.recv_message_id  IS '최초 수신 OUTBOUND_CMD message_id (if_msg_log 역추적)';
COMMENT ON COLUMN biz.wcs_outbound_order_h.last_message_id  IS '최종 수신 message_id (재수신 시 갱신)';


-- ⑨-1 wcs_outbound_order_d — 출고 지시 상세 (PalletId 라인 + 라이프사이클) · 입고 상세와 동일 구조
CREATE TABLE IF NOT EXISTS biz.wcs_outbound_order_d (
    task_id       VARCHAR(30)  NOT NULL,
    pallet_id     VARCHAR(30)  NOT NULL,
    sku_code      VARCHAR(30)  NOT NULL,
    lot_id        VARCHAR(30)  NOT NULL DEFAULT 'N/A',
    line_no       SMALLINT     NULL,
    qty           INTEGER      NOT NULL,
    expire_date   DATE         NULL,
    mapped_at     TIMESTAMP    NULL,
    completed_at  TIMESTAMP    NULL,
    cancelled_at  TIMESTAMP    NULL,
    cancel_reason VARCHAR(200) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    created_by    VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by    VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_outbound_order_d PRIMARY KEY (task_id, pallet_id, sku_code, lot_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_outbound_order_d__pallet
    ON biz.wcs_outbound_order_d (pallet_id);

CREATE INDEX IF NOT EXISTS ix_wcs_outbound_order_d__pending_map
    ON biz.wcs_outbound_order_d (pallet_id)
    WHERE mapped_at IS NULL AND cancelled_at IS NULL;

COMMENT ON TABLE  biz.wcs_outbound_order_d IS '출고 지시 상세 — OUTBOUND_CMD items 원본 라인 + PalletId 라이프사이클 (팔렛트 전체 출고)';
COMMENT ON COLUMN biz.wcs_outbound_order_d.qty          IS '출고 요청 수량 (OUTBOUND_CMD pickQty)';
COMMENT ON COLUMN biz.wcs_outbound_order_d.completed_at IS 'OUTBOUND_COMPLETE 발송 시각 (이 PalletId 라인 기준)';
COMMENT ON COLUMN biz.wcs_outbound_order_d.cancelled_at IS '취소 시각 (PalletId 단위)';


-- ⑩ wcs_task_h — 셔틀 이동 작업(TASK) 이력 · 사용자 조회용
--    wcs_shuttle_msg_log 가 전문(payload) 원본 이력이라면, 이 테이블은 "작업" 단위 요약 이력.
--    wcs_task_id = 'TSK-' + wcs_task_seq (작업 전용 채번) → 입고/출고 TASK 가 각각 고유 ID를 갖는다.
--    팔렛/사이클 역추적은 이 테이블의 eqp_pallet_id · cycle_no · pallet_id 컬럼으로 조인 없이 가능.
CREATE SEQUENCE IF NOT EXISTS biz.wcs_task_seq START WITH 1 INCREMENT BY 1;

COMMENT ON SEQUENCE biz.wcs_task_seq IS '셔틀 작업(TASK) 채번 — wcs_task_id = TSK-{8자리 0패딩}';

CREATE TABLE IF NOT EXISTS biz.wcs_task_h (
    task_seq        BIGSERIAL    NOT NULL,
    wcs_task_id     VARCHAR(60)  NOT NULL,
    task_type       VARCHAR(20)  NOT NULL,   -- INBOUND | OUTBOUND
    task_status     VARCHAR(20)  NOT NULL DEFAULT 'DISPATCHING', -- DISPATCHING | DISPATCHED | REJECTED | STARTED | COMPLETED | FAILED
    eqp_pallet_id   VARCHAR(30)  NOT NULL,
    cycle_no        INTEGER      NOT NULL,
    pallet_id       VARCHAR(30)  NULL,
    order_task_id   VARCHAR(30)  NULL,
    station_id      VARCHAR(30)  NULL,
    shuttle_id      VARCHAR(30)  NULL,
    sku_code        VARCHAR(30)  NULL,
    lot_id          VARCHAR(30)  NULL,
    qty             INTEGER      NULL,
    task_message_id VARCHAR(60)  NULL,
    ack_message_id  VARCHAR(60)  NULL,
    ack_result      VARCHAR(20)  NULL,
    fail_reason     VARCHAR(200) NULL,
    dispatched_at   TIMESTAMP    NOT NULL,
    started_at      TIMESTAMP    NULL,
    completed_at    TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_task_h PRIMARY KEY (task_seq),
    CONSTRAINT uk_wcs_task_h__task UNIQUE (wcs_task_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_task_h__eqp
    ON biz.wcs_task_h (eqp_pallet_id, dispatched_at DESC);
CREATE INDEX IF NOT EXISTS ix_wcs_task_h__status
    ON biz.wcs_task_h (task_status, dispatched_at DESC);
CREATE INDEX IF NOT EXISTS ix_wcs_task_h__order
    ON biz.wcs_task_h (order_task_id);
CREATE INDEX IF NOT EXISTS ix_wcs_task_h__pallet
    ON biz.wcs_task_h (pallet_id);

COMMENT ON TABLE  biz.wcs_task_h IS '셔틀 이동 작업(TASK) 이력 — INBOUND_TASK/OUTBOUND_TASK 발행부터 START·DONE 까지의 작업 단위 요약 (사용자 조회용)';
COMMENT ON COLUMN biz.wcs_task_h.wcs_task_id     IS 'WCS 발급 작업 식별자 — TSK-{wcs_task_seq 8자리}. 작업 단위 전역 고유(입고/출고 각각 별도 채번)';
COMMENT ON COLUMN biz.wcs_task_h.task_type       IS 'INBOUND(입고 TASK) · OUTBOUND(출고 TASK)';
COMMENT ON COLUMN biz.wcs_task_h.task_status     IS 'DISPATCHING(발송 전 선기록) → DISPATCHED(ACK 수락) → STARTED(설비 착수) → COMPLETED / FAILED. ACK 거부는 REJECTED, 전송 실패는 FAILED(수신 여부 불명)';
COMMENT ON COLUMN biz.wcs_task_h.order_task_id   IS 'WMS 지시 task_id (wcs_inbound_order_h / wcs_outbound_order_h 역추적)';
COMMENT ON COLUMN biz.wcs_task_h.station_id      IS '입고=팔렛이 출발한 스테이션 · 출고=목적지 스테이션(destStation)';
COMMENT ON COLUMN biz.wcs_task_h.shuttle_id      IS 'TASK_ACK/START/DONE 에서 보고된 셔틀 ID';
COMMENT ON COLUMN biz.wcs_task_h.task_message_id IS 'TASK 발행 전문 message_id (wcs_shuttle_msg_log 역추적)';
COMMENT ON COLUMN biz.wcs_task_h.ack_message_id  IS 'TASK_ACK 전문 message_id';
COMMENT ON COLUMN biz.wcs_task_h.ack_result      IS 'TASK_ACK result (ACCEPTED 등)';
COMMENT ON COLUMN biz.wcs_task_h.fail_reason     IS 'DONE 실패 보고 사유';
COMMENT ON COLUMN biz.wcs_task_h.dispatched_at   IS 'TASK 발행 시각 (전문 발송 직전 선기록 시점)';
COMMENT ON COLUMN biz.wcs_task_h.started_at      IS 'INBOUND_START / OUTBOUND_START 수신 시각';
COMMENT ON COLUMN biz.wcs_task_h.completed_at    IS 'INBOUND_DONE / OUTBOUND_DONE 수신 시각 (실패 포함)';
