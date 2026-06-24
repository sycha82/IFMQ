package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsInboundOrderDMapper {

    List<WcsInboundOrderD> findByTaskId(@Param("taskId") String taskId);

    WcsInboundOrderD findPendingByPalletId(@Param("palletId") String palletId);

    List<WcsInboundOrderD> findAllPendingByPalletId(@Param("palletId") String palletId);

    int countAll();

    void insert(WcsInboundOrderD detail);

    void updateMappedAt(@Param("palletId") String palletId,
                        @Param("mappedAt") LocalDateTime mappedAt);
}
