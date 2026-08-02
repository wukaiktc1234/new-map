package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.ElectronicVoucherQueryDTO;
import com.foodtraceability.dto.ElectronicVoucherVO;
import com.foodtraceability.entity.ElectronicVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 电子凭证Mapper接口
 */
@Mapper
public interface ElectronicVoucherMapper extends BaseMapper<ElectronicVoucher> {
    
    /**
     * 分页查询电子凭证
     */
    IPage<ElectronicVoucherVO> selectVoucherPage(
            Page<ElectronicVoucherVO> page,
            @Param("query") ElectronicVoucherQueryDTO query,
            @Param("tenantId") Long tenantId);
    
    /**
     * 根据文件哈希查询
     */
    ElectronicVoucher selectByFileHash(@Param("fileHash") String fileHash, @Param("tenantId") Long tenantId);
    
    /**
     * 根据凭证编号查询
     */
    ElectronicVoucher selectByVoucherNo(@Param("voucherNo") String voucherNo, @Param("tenantId") Long tenantId);
    
    /**
     * 查找相似凭证（用于关键信息去重）
     * 根据金额和日期查找可能的重复凭证
     */
    List<ElectronicVoucher> findSimilarVouchers(
            @Param("tenantId") Long tenantId,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("issueDate") LocalDate issueDate);
}
