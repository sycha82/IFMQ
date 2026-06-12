-- ============================================================================
-- 4Way 셔틀 WCS 공통 도메인 스키마 (입고·출고·피킹존 공통 5종)
-- 적용: psql -h localhost -p 5438 -U wcs -d wcs -f schema.sql
-- 스키마 biz 는 이미 생성되어 있다는 전제
--
-- 공통 정책:
-- · 감사 컬럼 : created_at · updated_at · created_by · updated_by 전 테이블 공통
-- · FK 미설정. 무결성은 Service 단일 트랜잭션(@Transactional)으로 보장
-- · CHECK : Y/N 류만 유지. enum 류는 코드 레벨 검증
-- ============================================================================


-- ① wcs_inbound_order_h — 입고 지시 (WMS INBOUND_CMD 수신)
-- 1 task → N pallet 구조 허용 → 복합 PK (task_id, pallet_id)
CREATE TABLE IF NOT EXISTS biz.wcs_inbound_order_h (
    task_id          VARCHAR(30)  NOT NULL,
    pallet_id        VARCHAR(30)  NOT NULL,
    cmd_status       VARCHAR(20)  NOT NULL,    -- RECEIVED | MAPPED | COMPLETED | CANCELLED
    recv_message_id  VARCHAR(50)  NOT NULL,    -- 최초 수신 INBOUND_CMD message_id
    last_message_id  VARCHAR(50)  NOT NULL,    -- 최종 수신 message_id (재수신 시 갱신)
    recv_count       SMALLINT     NOT NULL DEFAULT 1,
    received_at      TIMESTAMP    NOT NULL,
    qty_updated_at   TIMESTAMP    NULL,
    mapped_at        TIMESTAMP    NULL,
    completed_at     TIMESTAMP    NULL,
    cancelled_at     TIMESTAMP    NULL,
    cancel_reason    VARCHAR(200) NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by       VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_inbound_order_h PRIMARY KEY (task_id, pallet_id)
);

CREATE INDEX IF NOT EXISTS ix_wcs_inbound_order_h_pallet_id
    ON biz.wcs_inbound_order_h (pallet_id);
CREATE INDEX IF NOT EXISTS ix_wcs_inbound_order_h_cmd_status
    ON biz.wcs_inbound_order_h (cmd_status);


-- ② wcs_pallet_line_h — Pallet 적재 라인 (시계열, is_latest 플래그)
-- 1 pallet 멀티 SKU 운영 대응 구조. 현재 운영은 1 pallet 1 SKU 기준
-- 현재 유효 라인 유일성 : Partial Unique Index WHERE is_latest='Y'
CREATE TABLE IF NOT EXISTS biz.wcs_pallet_line_h (
    pallet_id       VARCHAR(30)  NOT NULL,
    effective_from  TIMESTAMP    NOT NULL,
    sku_code        VARCHAR(30)  NOT NULL,
    lot_id          VARCHAR(30)  NOT NULL,    -- lot 미관리 품목은 'N/A' sentinel
    qty             INTEGER      NOT NULL,
    expire_date     DATE         NULL,        -- FEFO 출고 기준. 해당 없는 품목은 NULL
    is_latest       CHAR(1)      NOT NULL DEFAULT 'Y' CHECK (is_latest IN ('Y','N')),
    effective_to    TIMESTAMP    NULL,        -- NULL=현재 유효
    superseded_by   TIMESTAMP    NULL,        -- 후속 버전 effective_from
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    updated_by      VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_wcs_pallet_line_h PRIMARY KEY (pallet_id, effective_from, sku_code, lot_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_wcs_pallet_line_h_latest
    ON biz.wcs_pallet_line_h (pallet_id, sku_code, lot_id)
    WHERE is_latest = 'Y';

-- 운영 조회 뷰 : is_latest='Y' 자동 필터. 컬럼 명시 (SELECT * 금지 — 운영상
-- 베이스 테이블 컬럼 추가/순서변경 시 영향 격리)
DROP VIEW IF EXISTS biz.wcs_vw_pallet_line_h;
CREATE VIEW biz.wcs_vw_pallet_line_h AS
    SELECT
        pallet_id,
        effective_from,
        sku_code,
        lot_id,
        qty,
        expire_date,
        is_latest,
        effective_to,
        superseded_by,
        created_at,
        updated_at,
        created_by,
        updated_by
    FROM biz.wcs_pallet_line_h
    WHERE is_latest = 'Y';


-- ③ wcs_eqp_pallet_m — EqpPallet 마스터 (물리 풀)
-- 레코드 수 = 물리 팔렛 수량 고정 (사이클링 대상 풀)
CREATE TABLE IF NOT EXISTS biz.wcs_eqp_pallet_m (
    eqp_pallet_id   VARCHAR(30)  NOT NULL,
    eqp_pallet_no   INTEGER      NULL,           -- 정렬·표시용
    pallet_status   VARCHAR(20)  NOT NULL DEFAULT 'EMPTY',  -- EMPTY | IN_USE | MAINTENANCE
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
-- 사이클링 시 직전 매핑 정보는 wcs_eqp_pallet_map_h 로 이관 후 row 덮어쓰기
CREATE TABLE IF NOT EXISTS biz.wcs_eqp_pallet_map (
    eqp_pallet_id   VARCHAR(30)  NOT NULL,
    task_id         VARCHAR(30)  NULL,           -- 현재 매핑된 입고 task_id
    pallet_id       VARCHAR(30)  NULL,           -- 현재 매핑된 WMS PalletId
    map_status      VARCHAR(20)  NOT NULL DEFAULT 'EMPTY',  -- EMPTY | MAPPED | IN_PROGRESS | STORED | PICKING
    location        VARCHAR(20)  NOT NULL DEFAULT 'IDLE',   -- IDLE | STATION | IN_RACK | PICKING_ZONE
    cycle_no        INTEGER      NOT NULL DEFAULT 0,        -- 사이클링 시마다 +1
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
    event_type      VARCHAR(20)  NOT NULL,    -- MAPPED | STATUS_CHANGE | LOCATION_CHANGE | CYCLE_END
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
