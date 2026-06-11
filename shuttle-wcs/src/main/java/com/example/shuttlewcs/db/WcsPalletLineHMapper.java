package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WcsPalletLineHMapper {

    // 현재 유효 라인 조회 (뷰 wcs_vw_pallet_line_h 경유)
    List<WcsPalletLineH> findLatestByPalletId(@Param("palletId") String palletId);

    int countAll();

    void insert(WcsPalletLineH line);

    // 기존 latest 라인 무효화 — effective_to, superseded_by 채우고 is_latest='N'
    void supersedeLatest(@Param("palletId") String palletId,
                         @Param("skuCode") String skuCode,
                         @Param("lotId") String lotId,
                         @Param("effectiveTo") LocalDateTime effectiveTo,
                         @Param("supersededBy") LocalDateTime supersededBy);
}
