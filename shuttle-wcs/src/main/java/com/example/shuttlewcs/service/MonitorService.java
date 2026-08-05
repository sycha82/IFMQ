package com.example.shuttlewcs.service;

import com.example.shuttlewcs.db.WcsAvailableInventory;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모니터링 스냅샷 — 대시보드가 1회 호출로 전체 현황을 받도록 묶어서 제공한다.
 * (화면 새로고침마다 5개 API를 병렬 호출하지 않게 하기 위함)
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final WcsTaskHMapper taskMapper;
    private final WcsStationMapper stationMapper;
    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsInventoryMapper inventoryMapper;
    private final WcsInboundOrderHMapper inboundOrderHMapper;
    private final WcsOutboundOrderHMapper outboundOrderHMapper;

    public Snapshot snapshot(int taskLimit, int orderLimit) {
        return Snapshot.builder()
                .serverTime(LocalDateTime.now())
                .tasks(taskMapper.findRecent(null, null, null, taskLimit))
                .stations(stationMapper.findAll())
                .pallets(eqpPalletMapMapper.findAll())
                .inventory(inventoryMapper.findAllAvailable())
                .inboundOrders(inboundOrderHMapper.findRecent(orderLimit))
                .outboundOrders(outboundOrderHMapper.findRecent(orderLimit))
                .build();
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
    }
}
