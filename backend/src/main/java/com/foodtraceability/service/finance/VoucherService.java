package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceVoucher;

import java.util.List;

/**
 * 记账凭证Service接口
 * 管理凭证的完整生命周期：创建 -> 审核 -> 过账
 */
public interface VoucherService extends IService<FinanceVoucher> {

    /**
     * 创建记账凭证（含借贷平衡校验）
     * @param dto 凭证创建DTO
     * @return 凭证VO
     */
    FinanceVoucherVO create(FinanceVoucherCreateDTO dto);

    /**
     * 更新凭证（仅暂存状态可修改）
     * @param dto 凭证更新DTO
     * @return 是否成功
     */
    boolean update(FinanceVoucherUpdateDTO dto);

    /**
     * 获取凭证详情（含分录明细）
     * @param voucherId 凭证ID
     * @return 凭证VO
     */
    FinanceVoucherVO getDetail(Long voucherId);

    /**
     * 分页查询凭证列表
     * @param query 查询条件
     * @return 分页数据
     */
    IPage<FinanceVoucherVO> getPage(FinanceVoucherQueryDTO query);

    /**
     * 审核凭证（暂存 -> 已审核）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean approve(Long voucherId);

    /**
     * 过账凭证（已审核 -> 已过账）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean post(Long voucherId);

    /**
     * 反审核凭证（已审核 -> 暂存）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean unapprove(Long voucherId);

    /**
     * 反过账凭证（已过账 -> 已审核）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean unpost(Long voucherId);

    /**
     * 作废凭证（暂存/已审核可作废）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean voidVoucher(Long voucherId);

    /**
     * 生成凭证号
     * @return 凭证号
     */
    String generateVoucherNo();

    /**
     * 校验借贷平衡
     * @param details 分录明细列表
     * @return 是否平衡
     */
    boolean checkBalance(List<FinanceVoucherCreateDTO.VoucherDetailItem> details);
}
