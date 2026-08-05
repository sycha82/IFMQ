package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// inf.if_msg_log 조회 전용 매퍼 (모니터링 화면). 적재는 wcs-app 담당 — 여기서 쓰기 금지.
@Mapper
public interface IfMsgLogViewMapper {

    List<IfMsgLogView> findRecent(@Param("limit") int limit);
}
