package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ElectronicSignature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 电子签名记录Mapper
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Mapper
public interface ElectronicSignatureMapper extends BaseMapper<ElectronicSignature> {

    @Select("SELECT * FROM electronic_signature WHERE contract_id = #{contractId} AND deleted = 0 ORDER BY create_time")
    List<ElectronicSignature> selectByContractId(@Param("contractId") Long contractId);

    @Select("SELECT * FROM electronic_signature WHERE contract_id = #{contractId} AND signer_type = #{signerType} AND deleted = 0")
    ElectronicSignature selectByContractIdAndSignerType(
        @Param("contractId") Long contractId, 
        @Param("signerType") String signerType
    );

    @Select("SELECT COUNT(*) FROM electronic_signature WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") String status);
}
