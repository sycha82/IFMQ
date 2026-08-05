package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WcsRcsMsgLogMapper {

    int countAll();

    // 모니터링 조회 — 최근 RCS 연계 전문
    List<WcsRcsMsgLog> findRecent(@Param("limit") int limit);

    void insert(WcsRcsMsgLog log);
}
