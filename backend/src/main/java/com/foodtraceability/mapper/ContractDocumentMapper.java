package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ContractDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 合同正文Mapper
 *
 * <p>注意：原生 SQL 中需手动添加 deleted = 0 条件，
 * 因为 @Select 注解不走 MyBatis-Plus 的 @TableLogic 自动逻辑删除。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Mapper
public interface ContractDocumentMapper extends BaseMapper<ContractDocument> {

    /**
     * 查询合同的所有版本（按版本号倒序）
     *
     * @param contractId 合同ID
     * @return 版本列表
     */
    @Select("SELECT * FROM contract_document WHERE contract_id = #{contractId} AND deleted = 0 ORDER BY version DESC")
    List<ContractDocument> selectByContractId(@Param("contractId") Long contractId);

    /**
     * 查询合同的当前版本
     *
     * @param contractId 合同ID
     * @return 当前版本正文，不存在返回 null
     */
    @Select("SELECT * FROM contract_document WHERE contract_id = #{contractId} AND is_current = 1 AND deleted = 0 LIMIT 1")
    ContractDocument selectCurrentByContractId(@Param("contractId") Long contractId);

    /**
     * 查询指定版本
     *
     * @param contractId 合同ID
     * @param version    版本号
     * @return 指定版本正文，不存在返回 null
     */
    @Select("SELECT * FROM contract_document WHERE contract_id = #{contractId} AND version = #{version} AND deleted = 0 LIMIT 1")
    ContractDocument selectByContractIdAndVersion(@Param("contractId") Long contractId, @Param("version") Integer version);

    /**
     * 统计版本数量
     *
     * @param contractId 合同ID
     * @return 版本数量
     */
    @Select("SELECT COUNT(*) FROM contract_document WHERE contract_id = #{contractId} AND deleted = 0")
    int countByContractId(@Param("contractId") Long contractId);
}
