package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface WcsInboundOrderHMapper {

    WcsInboundOrderH findByPk(@Param("taskId") String taskId,
                              @Param("palletId") String palletId);

    // PRE03 매핑 등록 검증용 — PalletId 로 RECEIVED 상태 row 조회
    WcsInboundOrderH findReceivedByPalletId(@Param("palletId") String palletId);

    int countAll();

    void insert(WcsInboundOrderH order);

    // 재수신 시 last_message_id 갱신 + recv_count + 1
    void updateOnResubmit(@Param("taskId") String taskId,
                          @Param("palletId") String palletId,
                          @Param("lastMessageId") String lastMessageId,
                          @Param("qtyUpdatedAt") LocalDateTime qtyUpdatedAt);

    void updateStatus(@Param("taskId") String taskId,
                      @Param("palletId") String palletId,
                      @Param("cmdStatus") String cmdStatus,
                      @Param("mappedAt") LocalDateTime mappedAt,
                      @Param("completedAt") LocalDateTime completedAt,
                      @Param("cancelledAt") LocalDateTime cancelledAt,
                      @Param("cancelReason") String cancelReason);
}
