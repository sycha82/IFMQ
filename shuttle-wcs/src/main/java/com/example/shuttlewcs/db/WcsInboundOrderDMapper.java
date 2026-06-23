package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WcsInboundOrderDMapper {

    List<WcsInboundOrderD> findByTaskId(@Param("taskId") String taskId);

    WcsInboundOrderD findPendingByPalletId(@Param("palletId") String palletId);

    int countAll();

    void insert(WcsInboundOrderD detail);
}
