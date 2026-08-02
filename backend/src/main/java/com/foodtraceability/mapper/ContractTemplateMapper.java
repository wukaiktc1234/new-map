package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ContractTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 合同模板Mapper
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Mapper
public interface ContractTemplateMapper extends BaseMapper<ContractTemplate> {

    @Select("SELECT COUNT(*) FROM contract_template WHERE contract_type = #{contractType} AND deleted = 0")
    int countByContractType(@Param("contractType") String contractType);

    @Select("SELECT COUNT(*) FROM contract_template WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") String status);
}
