package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.VoucherHeader;
import java.util.List;

/**
 * 凭证头服务接口
 * @author example
 * @since 2025-12-07
 */
public interface VoucherHeaderService extends IService<VoucherHeader> {
    
    /**
     * 根据状态获取凭证列表
     * @param status 状态
     * @return 凭证列表
     */
    List<VoucherHeader> getByStatus(String status);
    
    /**
     * 根据会计期间获取凭证列表
     * @param period 会计期间
     * @return 凭证列表
     */
    List<VoucherHeader> getByPeriod(String period);
    
    /**
     * 根据日期范围获取凭证列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 凭证列表
     */
    List<VoucherHeader> getByDateRange(String startDate, String endDate);
    
    /**
     * 生成凭证号
     * @return 凭证号
     */
    String generateVoucherNumber();
    
    /**
     * 提交凭证
     * @param id 凭证ID
     * @return 操作结果
     */
    boolean submitVoucher(Long id);
    
    /**
     * 审核凭证
     * @param id 凭证ID
     * @return 操作结果
     */
    boolean approveVoucher(Long id);
    
    /**
     * 过账凭证
     * @param id 凭证ID
     * @return 操作结果
     */
    boolean postVoucher(Long id);
    
    /**
     * 取消凭证
     * @param id 凭证ID
     * @return 操作结果
     */
    boolean cancelVoucher(Long id);

    /**
     * 创建红字冲销凭证
     *
     * <p>会计法合规：原凭证不得直接修改/删除，必须通过红字凭证冲销。
     * 红字凭证金额取原凭证金额的负数，并关联原凭证ID与冲销原因。</p>
     *
     * @param originalVoucherId 原凭证ID
     * @param reason            冲销原因（不可为空）
     * @return 生成的红字凭证
     */
    VoucherHeader createReverseVoucher(Long originalVoucherId, String reason);
}