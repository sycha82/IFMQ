package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WcsEqpPalletMapHMapper {

    int countAll();

    List<WcsEqpPalletMapH> findByEqpPalletId(@Param("eqpPalletId") String eqpPalletId);

    void insert(WcsEqpPalletMapH history);
}
