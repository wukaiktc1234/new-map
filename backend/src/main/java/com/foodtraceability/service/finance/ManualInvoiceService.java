package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.ManualInvoiceInputDTO;
import com.foodtraceability.dto.finance.ManualInvoiceQueryDTO;
import com.foodtraceability.dto.finance.ManualInvoiceStatsVO;
import com.foodtraceability.dto.finance.ManualInvoiceUpdateDTO;
import com.foodtraceability.dto.finance.ManualInvoiceVO;
import com.foodtraceability.entity.ElectronicVoucher;

/**
 * 手动输入发票服务接口
 *
 * <p>用于OCR识别失败或用户主动选择手动输入发票信息，支持手动发票的增删改查、验真与统计。</p>
 */
public interface ManualInvoiceService {

    /**
     * 从手动输入创建电子凭证
     *
     * @param input 手动输入信息
     * @return 创建的电子凭证
     */
    ElectronicVoucher createFromManualInput(ManualInvoiceInputDTO input);

    /**
     * 分页查询手动发票
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ManualInvoiceVO> queryPage(ManualInvoiceQueryDTO query);

    /**
     * 获取手动发票详情
     *
     * @param id 凭证ID
     * @return 手动发票VO
     */
    ManualInvoiceVO getDetail(Long id);

    /**
     * 更新手动发票
     *
     * @param id  凭证ID
     * @param dto 更新DTO
     * @return 是否更新成功
     */
    boolean update(Long id, ManualInvoiceUpdateDTO dto);

    /**
     * 删除手动发票（逻辑删除）
     *
     * @param id 凭证ID
     * @return 是否删除成功
     */
    boolean delete(Long id);

    /**
     * 发票验真
     *
     * @param id 凭证ID
     * @return 是否验真成功
     */
    boolean verify(Long id);

    /**
     * 统计手动发票
     *
     * @param startDate 起始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 统计信息
     */
    ManualInvoiceStatsVO getStats(String startDate, String endDate);
}
