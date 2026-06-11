-- 4Way 셔틀 입고 도메인 스키마
-- 적용: psql -h localhost -p 5438 -U wcs -d wcs -f schema.sql
-- 스키마 biz 는 이미 생성되어 있다는 전제

-- EqpPallet 마스터 : 설비 전용 팔렛트. 물리 수량만큼 마스터 정의 후 사이클링
CREATE TABLE IF NOT EXISTS biz.eqp_pallet (
    eqp_pallet_id   VARCHAR(40)  PRIMARY KEY,
    status          VARCHAR(20)  NOT NULL,   -- EMPTY | IN_USE | MAINTENANCE
    location        VARCHAR(20)  NOT NULL,   -- IN_RACK | PICKING_ZONE | STATION | IDLE
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- INBOUND_CMD 적재 (PENDING 단계)
-- PalletId 기준. PRE03 매핑 등록 전까지는 EqpPalletId 없음.
-- 같은 PalletId 재수신 시 수량 갱신 (WMS 라벨 재사용 케이스)
CREATE TABLE IF NOT EXISTS biz.inbound_cmd (
    pallet_id       VARCHAR(60)  PRIMARY KEY,
    task_id         VARCHAR(60)  NOT NULL,    -- WMS 발행 taskId
    sku_code        VARCHAR(40)  NOT NULL,
    lot_id          VARCHAR(40)  NOT NULL,
    qty             INTEGER      NOT NULL,
    expire_date     DATE,                     -- 해당 없는 품목은 NULL
    status          VARCHAR(20)  NOT NULL,    -- PENDING | MAPPED (매핑 등록 시 전환)
    received_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- EqpPalletId ↔ PalletId 매핑 테이블
-- 레코드 수 = EqpPallet 물리 수량 고정. 새 입고 시 EqpPalletId 기준 덮어쓰기
-- PRE03 매핑 등록부터 STORED 까지 라이프사이클 관리
CREATE TABLE IF NOT EXISTS biz.inbound_mapping (
    eqp_pallet_id   VARCHAR(40)  PRIMARY KEY,
    pallet_id       VARCHAR(60)  NOT NULL,
    sku_code        VARCHAR(40)  NOT NULL,
    lot_id          VARCHAR(40)  NOT NULL,
    qty             INTEGER      NOT NULL,
    expire_date     DATE,
    status          VARCHAR(20)  NOT NULL,    -- MAPPED | IN_PROGRESS | STORED
    wcs_task_id     VARCHAR(60),              -- BCR_READ 시점 발급
    mapped_at       TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_inbound_mapping_pallet_id
    ON biz.inbound_mapping (pallet_id);
