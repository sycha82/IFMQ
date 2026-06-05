package com.example.wcsapp.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface IfMsgLogMapper {

    void insert(IfMsgLog log);

    void updateStatus(@Param("logId") Long logId,
                      @Param("status") String status,
                      @Param("errorMsg") String errorMsg,
                      @Param("processedAt") LocalDateTime processedAt);

    boolean existsByDirectionAndMessageId(@Param("direction") String direction,
                                          @Param("messageId") String messageId);
}
