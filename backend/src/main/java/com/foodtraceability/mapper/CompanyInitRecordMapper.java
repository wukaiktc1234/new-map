package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CompanyInitRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 公司初始化记录 Mapper 接口
 */
@Mapper
public interface CompanyInitRecordMapper extends BaseMapper<CompanyInitRecord> {

    /**
     * 查询最新的初始化记录
     * @return 公司初始化记录
     */
    @Select("SELECT * FROM company_init_record WHERE deleted = 0 ORDER BY record_id DESC LIMIT 1")
    CompanyInitRecord selectLatest();
}
