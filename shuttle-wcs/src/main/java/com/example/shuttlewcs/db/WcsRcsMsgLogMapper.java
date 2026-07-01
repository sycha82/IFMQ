package com.example.shuttlewcs.db;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WcsRcsMsgLogMapper {

    int countAll();

    void insert(WcsRcsMsgLog log);
}
