package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.AccountMappingRuleCreateDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleQueryDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleUpdateDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleVO;
import com.foodtraceability.dto.finance.AutoVoucherQueryDTO;
import com.foodtraceability.dto.finance.AutoVoucherVO;
import com.foodtraceability.dto.finance.TestMappingResultVO;
import com.foodtraceability.dto.finance.VoucherStatsVO;

import java.util.List;

/**
 * 自动凭证管理Service接口
 *
 * <p>F-018：自动凭证管理类服务，提供科目映射规则管理和凭证生命周期管理。
 * 与事件型的 {@link AutoVoucherService} 互补，后者负责凭证自动生成。</p>
 *
 * <p>本接口覆盖前端15个API端点：
 * <ul>
 *   <li>映射规则管理（6个）：分页查询/创建/更新/删除/启用禁用/测试匹配</li>
 *   <li>凭证管理（9个）：分页查询/详情/审核/过账/作废/批量过账/重新生成/统计/导出</li>
 * </ul>
 * </p>
 */
public interface AutoVoucherManageService {

    // ==================== 映射规则管理 ====================

    /**
     * 分页查询科目映射规则
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AccountMappingRuleVO> getMappingRulesPage(AccountMappingRuleQueryDTO query);

    /**
     * 创建科目映射规则
     * @param dto 创建DTO
     * @return 创建后的规则VO
     */
    AccountMappingRuleVO createMappingRule(AccountMappingRuleCreateDTO dto);

    /**
     * 更新科目映射规则
     * @param ruleId 规则ID
     * @param dto 更新DTO
     * @return 更新后的规则VO
     */
    AccountMappingRuleVO updateMappingRule(Long ruleId, AccountMappingRuleUpdateDTO dto);

    /**
     * 删除科目映射规则（逻辑删除）
     * @param ruleId 规则ID
     * @return 是否成功
     */
    boolean deleteMappingRule(Long ruleId);

    /**
     * 启用/禁用映射规则
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean toggleMappingRule(Long ruleId, Boolean enabled);

    /**
     * 测试规则匹配，返回模拟生成的分录
     * @param eventType 事件类型
     * @param amount 金额（分）
     * @return 测试匹配结果
     */
    TestMappingResultVO testMappingRule(String eventType, Long amount);

    // ==================== 凭证管理 ====================

    /**
     * 分页查询自动凭证列表
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<AutoVoucherVO> getVouchersPage(AutoVoucherQueryDTO query);

    /**
     * 获取凭证详情（含分录明细）
     * @param voucherId 凭证ID
     * @return 凭证VO
     */
    AutoVoucherVO getVoucherDetail(Long voucherId);

    /**
     * 审核凭证（pending → reviewed）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean reviewVoucher(Long voucherId);

    /**
     * 过账凭证（reviewed → posted）
     * @param voucherId 凭证ID
     * @return 是否成功
     */
    boolean postVoucher(Long voucherId);

    /**
     * 作废凭证
     * @param voucherId 凭证ID
     * @param reason 作废原因
     * @return 是否成功
     */
    boolean voidVoucher(Long voucherId, String reason);

    /**
     * 批量过账
     * @param voucherIds 凭证ID列表
     * @return 是否成功
     */
    boolean batchPostVouchers(List<Long> voucherIds);

    /**
     * 根据来源事件ID重新生成凭证
     * @param sourceEventId 来源事件ID
     * @return 重新生成的凭证VO
     */
    AutoVoucherVO regenerateVoucher(Long sourceEventId);

    /**
     * 获取凭证统计信息
     * @param startDate 起始日期（yyyy-MM-dd），可为空
     * @param endDate 结束日期（yyyy-MM-dd），可为空
     * @return 统计信息
     */
    VoucherStatsVO getVoucherStats(String startDate, String endDate);

    /**
     * 导出凭证数据
     * @param query 查询条件
     * @return 导出文件字节数组
     */
    byte[] exportVouchers(AutoVoucherQueryDTO query);
}
