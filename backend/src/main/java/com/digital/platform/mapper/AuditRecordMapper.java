package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.digital.platform.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}
