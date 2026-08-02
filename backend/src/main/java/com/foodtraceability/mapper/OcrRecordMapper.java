package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OcrRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * OCR识别记录Mapper接口
 * @author example
 * @since 2025-12-06
 */
@Mapper
public interface OcrRecordMapper extends BaseMapper<OcrRecord> {
}
