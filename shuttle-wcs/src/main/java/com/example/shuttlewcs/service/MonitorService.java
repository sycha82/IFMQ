package com.example.shuttlewcs.service;

import com.example.shuttlewcs.db.IfMsgLogView;
import com.example.shuttlewcs.db.IfMsgLogViewMapper;
import com.example.shuttlewcs.db.WcsAvailableInventory;
import com.example.shuttlewcs.db.WcsRcsMsgLog;
import com.example.shuttlewcs.db.WcsRcsMsgLogMapper;
import com.example.shuttlewcs.db.WcsEqpPalletMap;
import com.example.shuttlewcs.db.WcsEqpPalletMapMapper;
import com.example.shuttlewcs.db.WcsInboundOrderH;
import com.example.shuttlewcs.db.WcsInboundOrderHMapper;
import com.example.shuttlewcs.db.WcsInventoryMapper;
import com.example.shuttlewcs.db.WcsOutboundOrderH;
import com.example.shuttlewcs.db.WcsOutboundOrderHMapper;
import com.example.shuttlewcs.db.WcsStation;
import com.example.shuttlewcs.db.WcsStationMapper;
import com.example.shuttlewcs.db.WcsTaskH;
import com.example.shuttlewcs.db.WcsTaskHMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모니터링 스냅샷 — 대시보드가 1회 호출로 전체 현황을 받도록 묶어서 제공한다.
 * (화면 새로고침마다 5개 API를 병렬 호출하지 않게 하기 위함)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final WcsTaskHMapper taskMapper;
    private final WcsStationMapper stationMapper;
    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsInventoryMapper inventoryMapper;
    private final WcsInboundOrderHMapper inboundOrderHMapper;
    private final WcsOutboundOrderHMapper outboundOrderHMapper;
    private final WcsRcsMsgLogMapper rcsMsgLogMapper;
    private final IfMsgLogViewMapper ifMsgLogViewMapper;

    public Snapshot snapshot(int taskLimit, int orderLimit, int msgLimit) {
        return Snapshot.builder()
                .serverTime(LocalDateTime.now())
                .tasks(taskMapper.findRecent(null, null, null, taskLimit))
                .stations(stationMapper.findAll())
                .pallets(eqpPalletMapMapper.findAll())
                .inventory(inventoryMapper.findAllAvailable())
                .inboundOrders(inboundOrderHMapper.findRecent(orderLimit))
                .outboundOrders(outboundOrderHMapper.findRecent(orderLimit))
                .rcsMessages(rcsMsgLogMapper.findRecent(msgLimit))
                .wmsMessages(readWmsMessages(msgLimit))
                .build();
    }

    /**
     * WMS 구간 전문(inf.if_msg_log) 조회 — 적재 주체는 wcs-app 이고 여기서는 표시용으로 읽기만 한다.
     * wcs-app 이 아직 기동되지 않아 테이블/스키마가 없을 수도 있으므로, 실패해도 대시보드 전체가
     * 죽지 않도록 빈 목록으로 처리한다.
     */
    private List<IfMsgLogView> readWmsMessages(int limit) {
        try {
            return ifMsgLogViewMapper.findRecent(limit);
        } catch (Exception e) {
            log.warn("[MONITOR] WMS 전문 이력 조회 실패(빈 목록으로 대체) | {}", e.getMessage());
            return List.of();
        }
    }

    @Getter
    @Builder
    public static class Snapshot {
        private LocalDateTime serverTime;
        private List<WcsTaskH> tasks;
        private List<WcsStation> stations;
        private List<WcsEqpPalletMap> pallets;
        private List<WcsAvailableInventory> inventory;
        private List<WcsInboundOrderH> inboundOrders;
        private List<WcsOutboundOrderH> outboundOrders;
        private List<WcsRcsMsgLog> rcsMessages;   // WCS↔RCS (REST)
        private List<IfMsgLogView> wmsMessages;   // WMS↔WCS (MQ)
    }
}
