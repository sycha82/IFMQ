package com.example.shuttlewcs.service;

import com.example.shuttlewcs.db.*;
import com.example.shuttlewcs.dto.MappingRequest;
import com.example.shuttlewcs.exception.MappingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingService {

    private final WcsEqpPalletMMapper eqpPalletMMapper;
    private final WcsEqpPalletMapMapper eqpPalletMapMapper;
    private final WcsEqpPalletMapHMapper eqpPalletMapHMapper;
    private final WcsInboundOrderDMapper orderDMapper;
    private final WcsPalletLineHMapper palletLineHMapper;

    @Transactional
    public void applyMapping(MappingRequest req) {
        String eqpPalletId = req.getEqpPalletId();
        String palletId = req.getPalletId();
        LocalDateTime now = LocalDateTime.now();

        // 1. EqpPallet 마스터 존재·사용가능 검증
        WcsEqpPalletM master = eqpPalletMMapper.findById(eqpPalletId);
        if (master == null) {
            throw new MappingException("존재하지 않는 설비파레트 | eqpPalletId=" + eqpPalletId);
        }
        if (!"Y".equals(master.getUseYn())) {
            throw new MappingException("사용불가 설비파레트 | eqpPalletId=" + eqpPalletId
                    + " useYn=" + master.getUseYn());
        }

        // 2. EqpPallet 현재 매핑 상태 검증 (EMPTY만 매핑 가능)
        WcsEqpPalletMap currentMap = eqpPalletMapMapper.findById(eqpPalletId);
        if (currentMap == null) {
            throw new MappingException("매핑 레코드 미존재 | eqpPalletId=" + eqpPalletId);
        }
        if (!"EMPTY".equals(currentMap.getMapStatus())) {
            throw new MappingException("이미 사용중인 설비파레트 | eqpPalletId=" + eqpPalletId
                    + " mapStatus=" + currentMap.getMapStatus());
        }

        // 3. 입고 지시에 해당 palletId가 미매핑 상태로 존재하는지 검증
        List<WcsInboundOrderD> pendingLines = orderDMapper.findAllPendingByPalletId(palletId);
        if (pendingLines.isEmpty()) {
            throw new MappingException("매핑 가능한 입고 지시 없음 | palletId=" + palletId);
        }

        String taskId = pendingLines.get(0).getTaskId();

        // 4. wcs_eqp_pallet_map 매핑 적용 (MAPPED, STATION, cycle_no+1)
        eqpPalletMapMapper.applyMapping(eqpPalletId, taskId, palletId, now);

        // 5. wcs_eqp_pallet_map_h 이력 적재
        int newCycleNo = (currentMap.getCycleNo() != null ? currentMap.getCycleNo() : 0) + 1;
        eqpPalletMapHMapper.insert(WcsEqpPalletMapH.builder()
                .eqpPalletId(eqpPalletId)
                .cycleNo(newCycleNo)
                .taskId(taskId)
                .palletId(palletId)
                .mapStatus("MAPPED")
                .location("STATION")
                .mappedAt(now)
                .eventType("MAPPED")
                .eventAt(now)
                .eventBy("KIOSK")
                .note("PRE03 매핑 등록")
                .build());

        // 6. wcs_inbound_order_d mapped_at 갱신
        orderDMapper.updateMappedAt(palletId, now);

        // 7. wcs_pallet_line_h 적재 라인 생성 (입고 지시 기준)
        for (WcsInboundOrderD line : pendingLines) {
            palletLineHMapper.insert(WcsPalletLineH.builder()
                    .palletId(palletId)
                    .effectiveFrom(now)
                    .skuCode(line.getSkuCode())
                    .lotId(line.getLotId())
                    .qty(line.getQty())
                    .expireDate(line.getExpireDate())
                    .isLatest("Y")
                    .build());
        }

        // 8. wcs_eqp_pallet_m 상태 갱신
        eqpPalletMMapper.updateStatus(eqpPalletId, "IN_USE", null);
        eqpPalletMMapper.updateLastUsedAt(eqpPalletId, now);

        log.info("[MAPPING] PRE03 매핑 완료 | eqpPalletId={} palletId={} taskId={} lines={}",
                eqpPalletId, palletId, taskId, pendingLines.size());
    }
}
