package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.finance.AccountMappingRuleCreateDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleQueryDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleUpdateDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleVO;
import com.foodtraceability.dto.finance.AutoVoucherQueryDTO;
import com.foodtraceability.dto.finance.AutoVoucherVO;
import com.foodtraceability.dto.finance.TestMappingResultVO;
import com.foodtraceability.dto.finance.VoucherStatsVO;
import com.foodtraceability.entity.AccountingRule;
import com.foodtraceability.entity.AutoVoucherLog;
import com.foodtraceability.entity.finance.FinanceVoucher;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.AccountingRuleMapper;
import com.foodtraceability.mapper.AutoVoucherLogMapper;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
import com.foodtraceability.service.finance.AutoVoucherManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动凭证管理Service实现
 *
 * <p>F-018：自动凭证管理类服务实现，提供科目映射规则管理和凭证生命周期管理。
 * 与事件型的 {@link com.foodtraceability.service.finance.AutoVoucherService} 互补。</p>
 *
 * <p>简化实现策略：
 * <ul>
 *   <li>映射规则使用 AccountingRule 实体，部分前端字段（借/贷科目编码、税率、成本中心）
 *       暂未持久化，待后续扩展表结构</li>
 *   <li>凭证管理使用 FinanceVoucher 实体，分录明细待接入</li>
 *   <li>导出和重新生成为简化实现，标注 TODO 待后续完善</li>
 * </ul>
 * </p>
 */
@Service
public class AutoVoucherManageServiceImpl implements AutoVoucherManageService {

    private static final Logger log = LoggerFactory.getLogger(AutoVoucherManageServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** AccountingRule 状态：启用 */
    private static final String RULE_STATUS_ACTIVE = "ACTIVE";
    /** AccountingRule 状态：停用 */
    private static final String RULE_STATUS_INACTIVE = "INACTIVE";

    /** FinanceVoucher 凭证状态：暂存 */
    private static final int VOUCHER_STATUS_PENDING = 0;
    /** FinanceVoucher 凭证状态：已审核 */
    private static final int VOUCHER_STATUS_REVIEWED = 1;
    /** FinanceVoucher 凭证状态：已过账 */
    private static final int VOUCHER_STATUS_POSTED = 2;
    /** FinanceVoucher 凭证状态：已作废 */
    private static final int VOUCHER_STATUS_VOIDED = 3;

    private final AccountingRuleMapper accountingRuleMapper;
    private final FinanceVoucherMapper financeVoucherMapper;
    private final AutoVoucherLogMapper autoVoucherLogMapper;

    public AutoVoucherManageServiceImpl(AccountingRuleMapper accountingRuleMapper,
                                        FinanceVoucherMapper financeVoucherMapper,
                                        AutoVoucherLogMapper autoVoucherLogMapper) {
        this.accountingRuleMapper = accountingRuleMapper;
        this.financeVoucherMapper = financeVoucherMapper;
        this.autoVoucherLogMapper = autoVoucherLogMapper;
    }

    // ==================== 映射规则管理 ====================

    @Override
    public IPage<AccountMappingRuleVO> getMappingRulesPage(AccountMappingRuleQueryDTO query) {
        int current = query.getPageNum() != null ? query.getPageNum() : 1;
        int size = query.getPageSize() != null ? query.getPageSize() : 10;
        Page<AccountingRule> page = new Page<>(current, size);

        LambdaQueryWrapper<AccountingRule> wrapper = new LambdaQueryWrapper<>();
        if (query.getEventType() != null && !query.getEventType().isBlank()) {
            wrapper.eq(AccountingRule::getEventType, query.getEventType());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(AccountingRule::getRuleName, query.getKeyword());
        }
        wrapper.orderByDesc(AccountingRule::getPriority);

        IPage<AccountingRule> entityPage = accountingRuleMapper.selectPage(page, wrapper);

        Page<AccountMappingRuleVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<AccountMappingRuleVO> voList = entityPage.getRecords().stream()
                .map(this::convertRuleToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<AccountMappingRuleVO> result = (IPage<AccountMappingRuleVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountMappingRuleVO createMappingRule(AccountMappingRuleCreateDTO dto) {
        log.info("创建科目映射规则，规则名称：{}，事件类型：{}", dto.getRuleName(), dto.getEventType());

        AccountingRule entity = new AccountingRule();
        entity.setRuleName(dto.getRuleName());
        entity.setEventType(dto.getEventType());
        entity.setEventDescription(getEventTypeName(dto.getEventType()));
        entity.setRuleCode("RULE_" + System.currentTimeMillis());
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        entity.setStatus(RULE_STATUS_ACTIVE);
        entity.setAutoExecute(true);
        // TODO: 借/贷科目编码、税率、成本中心、摘要模板需要扩展表结构后持久化

        accountingRuleMapper.insert(entity);
        log.info("科目映射规则创建成功，ID：{}", entity.getId());
        return convertRuleToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountMappingRuleVO updateMappingRule(Long ruleId, AccountMappingRuleUpdateDTO dto) {
        log.info("更新科目映射规则，ID：{}", ruleId);

        AccountingRule entity = accountingRuleMapper.selectById(ruleId);
        if (entity == null) {
            throw new BusinessException("映射规则不存在");
        }

        if (dto.getRuleName() != null) {
            entity.setRuleName(dto.getRuleName());
        }
        if (dto.getEventType() != null) {
            entity.setEventType(dto.getEventType());
            entity.setEventDescription(getEventTypeName(dto.getEventType()));
        }
        if (dto.getPriority() != null) {
            entity.setPriority(dto.getPriority());
        }
        // TODO: 借/贷科目编码、税率、成本中心、摘要模板需要扩展表结构后更新

        accountingRuleMapper.updateById(entity);
        return convertRuleToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMappingRule(Long ruleId) {
        log.info("删除科目映射规则，ID：{}", ruleId);
        AccountingRule entity = accountingRuleMapper.selectById(ruleId);
        if (entity == null) {
            throw new BusinessException("映射规则不存在");
        }
        // AccountingRule 无 @TableLogic，使用物理删除（配置类数据）
        return accountingRuleMapper.deleteById(ruleId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleMappingRule(Long ruleId, Boolean enabled) {
        log.info("切换映射规则状态，ID：{}，启用：{}", ruleId, enabled);

        AccountingRule entity = accountingRuleMapper.selectById(ruleId);
        if (entity == null) {
            throw new BusinessException("映射规则不存在");
        }

        entity.setStatus(Boolean.TRUE.equals(enabled) ? RULE_STATUS_ACTIVE : RULE_STATUS_INACTIVE);
        return accountingRuleMapper.updateById(entity) > 0;
    }

    @Override
    public TestMappingResultVO testMappingRule(String eventType, Long amount) {
        log.info("测试规则匹配，事件类型：{}，金额：{}", eventType, amount);

        TestMappingResultVO result = new TestMappingResultVO();

        // 查找匹配的启用规则（按优先级降序取第一条）
        LambdaQueryWrapper<AccountingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingRule::getEventType, eventType)
                .eq(AccountingRule::getStatus, RULE_STATUS_ACTIVE)
                .orderByDesc(AccountingRule::getPriority)
                .last("LIMIT 1");
        AccountingRule rule = accountingRuleMapper.selectOne(wrapper);

        if (rule == null) {
            result.setMatched(false);
            result.setMessage("未找到匹配的启用规则，事件类型：" + eventType);
            return result;
        }

        result.setMatched(true);
        result.setRule(convertRuleToVO(rule));
        result.setSimulatedEntries(buildSimulatedEntries(amount));
        result.setMessage("匹配成功，已生成模拟分录");
        // TODO: 模拟分录应基于规则中的借/贷科目编码生成，待表结构扩展后完善
        return result;
    }

    // ==================== 凭证管理 ====================

    @Override
    public IPage<AutoVoucherVO> getVouchersPage(AutoVoucherQueryDTO query) {
        int current = query.getPageNum() != null ? query.getPageNum() : 1;
        int size = query.getPageSize() != null ? query.getPageSize() : 10;
        Page<FinanceVoucher> page = new Page<>(current, size);

        LambdaQueryWrapper<FinanceVoucher> wrapper = buildVoucherQueryWrapper(query);
        wrapper.orderByDesc(FinanceVoucher::getVoucherDate);

        IPage<FinanceVoucher> entityPage = financeVoucherMapper.selectPage(page, wrapper);

        Page<AutoVoucherVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<AutoVoucherVO> voList = entityPage.getRecords().stream()
                .map(this::convertVoucherToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<AutoVoucherVO> result = (IPage<AutoVoucherVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public AutoVoucherVO getVoucherDetail(Long voucherId) {
        FinanceVoucher entity = financeVoucherMapper.selectById(voucherId);
        if (entity == null) {
            throw new BusinessException("凭证不存在");
        }
        AutoVoucherVO vo = convertVoucherToVO(entity);
        // TODO: 查询凭证分录明细并填充 entries 字段，待接入 FinanceVoucherDetail 查询
        vo.setEntries(new ArrayList<>());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewVoucher(Long voucherId) {
        log.info("审核凭证，ID：{}", voucherId);
        FinanceVoucher entity = getVoucherOrThrow(voucherId);

        if (entity.getVoucherStatus() != null && entity.getVoucherStatus() != VOUCHER_STATUS_PENDING) {
            throw new BusinessException("仅暂存状态的凭证可以审核");
        }

        entity.setVoucherStatus(VOUCHER_STATUS_REVIEWED);
        entity.setApproveTime(LocalDateTime.now());
        // TODO: 接入用户上下文设置审核人ID
        return financeVoucherMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean postVoucher(Long voucherId) {
        log.info("过账凭证，ID：{}", voucherId);
        FinanceVoucher entity = getVoucherOrThrow(voucherId);

        if (entity.getVoucherStatus() == null || entity.getVoucherStatus() != VOUCHER_STATUS_REVIEWED) {
            throw new BusinessException("仅已审核状态的凭证可以过账");
        }

        entity.setVoucherStatus(VOUCHER_STATUS_POSTED);
        // TODO: 接入用户上下文设置过账人ID和过账时间
        return financeVoucherMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidVoucher(Long voucherId, String reason) {
        log.info("作废凭证，ID：{}，原因：{}", voucherId, reason);
        FinanceVoucher entity = getVoucherOrThrow(voucherId);

        if (entity.getVoucherStatus() != null && entity.getVoucherStatus() == VOUCHER_STATUS_POSTED) {
            throw new BusinessException("已过账凭证不能直接作废，请使用红字冲销");
        }

        entity.setVoucherStatus(VOUCHER_STATUS_VOIDED);
        if (reason != null && !reason.isBlank()) {
            entity.setRemark("作废原因：" + reason);
        }
        return financeVoucherMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchPostVouchers(List<Long> voucherIds) {
        log.info("批量过账凭证，数量：{}", voucherIds != null ? voucherIds.size() : 0);
        if (voucherIds == null || voucherIds.isEmpty()) {
            throw new BusinessException("凭证ID列表不能为空");
        }

        int successCount = 0;
        for (Long voucherId : voucherIds) {
            try {
                if (postVoucher(voucherId)) {
                    successCount++;
                }
            } catch (BusinessException e) {
                log.warn("批量过账跳过凭证 {}：{}", voucherId, e.getMessage());
            }
        }
        log.info("批量过账完成，成功：{}，总计：{}", successCount, voucherIds.size());
        return successCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutoVoucherVO regenerateVoucher(Long sourceEventId) {
        log.info("重新生成凭证，来源事件ID：{}", sourceEventId);

        // 查找该来源事件已有的凭证
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceVoucher::getSourceId, sourceEventId)
                .orderByDesc(FinanceVoucher::getVoucherId)
                .last("LIMIT 1");
        FinanceVoucher existing = financeVoucherMapper.selectOne(wrapper);

        if (existing == null) {
            throw new BusinessException("未找到来源事件对应的凭证，无法重新生成");
        }

        // TODO: 完整的重新生成逻辑应包含：
        // 1. 作废原凭证
        // 2. 根据 sourceType 调用 AutoVoucherService 的对应方法重新生成
        // 当前简化实现：返回原凭证信息
        return convertVoucherToVO(existing);
    }

    @Override
    public VoucherStatsVO getVoucherStats(String startDate, String endDate) {
        VoucherStatsVO stats = new VoucherStatsVO();

        // 总数（带日期范围过滤）
        LambdaQueryWrapper<FinanceVoucher> totalWrapper = new LambdaQueryWrapper<>();
        applyVoucherDateRange(totalWrapper, startDate, endDate);
        stats.setTotalVouchers(financeVoucherMapper.selectCount(totalWrapper));

        // 待审核数
        LambdaQueryWrapper<FinanceVoucher> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(FinanceVoucher::getVoucherStatus, VOUCHER_STATUS_PENDING);
        applyVoucherDateRange(pendingWrapper, startDate, endDate);
        stats.setPendingReview(financeVoucherMapper.selectCount(pendingWrapper));

        // 今日已过账数
        LambdaQueryWrapper<FinanceVoucher> postedTodayWrapper = new LambdaQueryWrapper<>();
        postedTodayWrapper.eq(FinanceVoucher::getVoucherStatus, VOUCHER_STATUS_POSTED)
                .eq(FinanceVoucher::getVoucherDate, LocalDate.now());
        stats.setPostedToday(financeVoucherMapper.selectCount(postedTodayWrapper));

        // 本月凭证数
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LambdaQueryWrapper<FinanceVoucher> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(FinanceVoucher::getVoucherDate, monthStart);
        stats.setThisMonthCount(financeVoucherMapper.selectCount(monthWrapper));

        // 生成失败数（从日志表统计）
        LambdaQueryWrapper<AutoVoucherLog> errorWrapper = new LambdaQueryWrapper<>();
        errorWrapper.eq(AutoVoucherLog::getStatus, "FAILED");
        stats.setErrorCount(autoVoucherLogMapper.selectCount(errorWrapper));

        // 热门事件类型统计（按来源类型分组取Top5）
        stats.setTopEventTypes(buildTopEventTypes(startDate, endDate));

        return stats;
    }

    @Override
    public byte[] exportVouchers(AutoVoucherQueryDTO query) {
        // TODO: 实现完整的 Excel 导出逻辑，使用 EasyExcel 或 POI 生成 xlsx 文件
        // 当前返回空字节数组作为占位
        log.info("导出凭证数据（简化实现），查询条件：{}", query);
        return new byte[0];
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建凭证查询条件
     */
    private LambdaQueryWrapper<FinanceVoucher> buildVoucherQueryWrapper(AutoVoucherQueryDTO query) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        applyVoucherDateRange(wrapper, query.getStartDate(), query.getEndDate());
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            Integer statusValue = mapStatusToInteger(query.getStatus());
            if (statusValue != null) {
                wrapper.eq(FinanceVoucher::getVoucherStatus, statusValue);
            }
        }
        if (query.getEventType() != null && !query.getEventType().isBlank()) {
            Integer sourceType = mapEventTypeToSourceType(query.getEventType());
            if (sourceType != null) {
                wrapper.eq(FinanceVoucher::getSourceType, sourceType);
            }
        }
        return wrapper;
    }

    /**
     * 应用凭证日期范围过滤
     */
    private void applyVoucherDateRange(LambdaQueryWrapper<FinanceVoucher> wrapper, String startDate, String endDate) {
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(FinanceVoucher::getVoucherDate, LocalDate.parse(startDate, DATE_FORMATTER));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(FinanceVoucher::getVoucherDate, LocalDate.parse(endDate, DATE_FORMATTER));
        }
    }

    /**
     * 获取凭证或抛出异常
     */
    private FinanceVoucher getVoucherOrThrow(Long voucherId) {
        FinanceVoucher entity = financeVoucherMapper.selectById(voucherId);
        if (entity == null) {
            throw new BusinessException("凭证不存在");
        }
        return entity;
    }

    /**
     * AccountingRule 实体转 VO
     */
    private AccountMappingRuleVO convertRuleToVO(AccountingRule entity) {
        AccountMappingRuleVO vo = new AccountMappingRuleVO();
        vo.setRuleId(entity.getId());
        vo.setRuleName(entity.getRuleName());
        vo.setEventType(entity.getEventType());
        vo.setEventTypeName(entity.getEventDescription() != null ? entity.getEventDescription() : getEventTypeName(entity.getEventType()));
        vo.setEnabled(RULE_STATUS_ACTIVE.equals(entity.getStatus()));
        vo.setPriority(entity.getPriority());
        // TODO: 以下字段需扩展表结构后填充
        vo.setDebitAccountCode(null);
        vo.setDebitAccountName(null);
        vo.setCreditAccountCode(null);
        vo.setCreditAccountName(null);
        vo.setTaxRate(null);
        vo.setCostCenter(null);
        vo.setDescriptionTemplate(entity.getDescription());
        return vo;
    }

    /**
     * FinanceVoucher 实体转 VO
     */
    private AutoVoucherVO convertVoucherToVO(FinanceVoucher entity) {
        AutoVoucherVO vo = new AutoVoucherVO();
        vo.setVoucherId(entity.getVoucherId());
        vo.setVoucherNo(entity.getVoucherNo());
        vo.setVoucherDate(entity.getVoucherDate());
        vo.setTotalDebit(entity.getTotalDebit());
        vo.setTotalCredit(entity.getTotalCredit());
        vo.setSourceEventId(entity.getSourceId());
        vo.setSourceEvent(getSourceEventName(entity.getSourceType()));
        vo.setStatus(mapStatusToString(entity.getVoucherStatus()));
        vo.setReviewedAt(entity.getApproveTime());
        vo.setReviewedBy(entity.getApproveUserId() != null ? String.valueOf(entity.getApproveUserId()) : null);
        vo.setCreatedAt(entity.getCreateTime());
        // TODO: postedBy/postedAt 需扩展实体字段后填充
        return vo;
    }

    /**
     * 构建模拟分录列表
     */
    private List<AutoVoucherVO.VoucherEntryVO> buildSimulatedEntries(Long amount) {
        List<AutoVoucherVO.VoucherEntryVO> entries = new ArrayList<>();
        long safeAmount = amount != null ? amount : 0L;

        // 借方分录
        AutoVoucherVO.VoucherEntryVO debitEntry = new AutoVoucherVO.VoucherEntryVO();
        debitEntry.setEntryId(1L);
        debitEntry.setAccountCode("");
        debitEntry.setAccountName("借方科目（待配置）");
        debitEntry.setDebitAmount(safeAmount);
        debitEntry.setCreditAmount(0L);
        debitEntry.setDescription("模拟借方分录");
        entries.add(debitEntry);

        // 贷方分录
        AutoVoucherVO.VoucherEntryVO creditEntry = new AutoVoucherVO.VoucherEntryVO();
        creditEntry.setEntryId(2L);
        creditEntry.setAccountCode("");
        creditEntry.setAccountName("贷方科目（待配置）");
        creditEntry.setDebitAmount(0L);
        creditEntry.setCreditAmount(safeAmount);
        creditEntry.setDescription("模拟贷方分录");
        entries.add(creditEntry);

        return entries;
    }

    /**
     * 构建热门事件类型统计
     */
    private List<VoucherStatsVO.EventTypeStat> buildTopEventTypes(String startDate, String endDate) {
        // TODO: 使用 MyBatis-Plus 分组查询统计各来源类型的凭证数量，当前返回基本统计
        List<VoucherStatsVO.EventTypeStat> stats = new ArrayList<>();

        // 统计采购入库凭证数
        stats.add(new VoucherStatsVO.EventTypeStat("purchase_in", "采购入库",
                countBySourceType(2, startDate, endDate)));
        // 统计销售出库凭证数
        stats.add(new VoucherStatsVO.EventTypeStat("sale_out", "销售出库",
                countBySourceType(3, startDate, endDate)));
        // 统计折旧凭证数
        stats.add(new VoucherStatsVO.EventTypeStat("depreciation", "折旧计提",
                countBySourceType(8, startDate, endDate)));

        return stats;
    }

    /**
     * 按来源类型统计凭证数（带日期范围）
     */
    private Long countBySourceType(Integer sourceType, String startDate, String endDate) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceVoucher::getSourceType, sourceType);
        applyVoucherDateRange(wrapper, startDate, endDate);
        return financeVoucherMapper.selectCount(wrapper);
    }

    /**
     * 事件类型 → 事件名称映射
     */
    private String getEventTypeName(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case "purchase_in" -> "采购入库";
            case "sale_out" -> "销售出库";
            case "inventory_check" -> "盘点差异";
            case "expense" -> "费用报销";
            case "adjustment" -> "账务调整";
            case "depreciation" -> "折旧计提";
            default -> eventType;
        };
    }

    /**
     * 来源类型 → 来源事件名称映射
     */
    private String getSourceEventName(Integer sourceType) {
        if (sourceType == null) {
            return null;
        }
        return switch (sourceType) {
            case 1 -> "手动创建";
            case 2 -> "采购入库";
            case 3 -> "销售出库";
            case 4 -> "收款";
            case 5 -> "付款";
            case 6 -> "费用报销";
            case 7 -> "成本结转";
            case 8 -> "折旧计提";
            case 9 -> "门店日结";
            case 10 -> "发票报销";
            case 11 -> "工资计提";
            default -> "其他";
        };
    }

    /**
     * 凭证状态：整数 → 字符串（pending/reviewed/posted/voided）
     */
    private String mapStatusToString(Integer voucherStatus) {
        if (voucherStatus == null) {
            return null;
        }
        return switch (voucherStatus) {
            case VOUCHER_STATUS_PENDING -> "pending";
            case VOUCHER_STATUS_REVIEWED -> "reviewed";
            case VOUCHER_STATUS_POSTED -> "posted";
            case VOUCHER_STATUS_VOIDED -> "voided";
            default -> "pending";
        };
    }

    /**
     * 凭证状态：字符串 → 整数
     */
    private Integer mapStatusToInteger(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status) {
            case "pending" -> VOUCHER_STATUS_PENDING;
            case "reviewed" -> VOUCHER_STATUS_REVIEWED;
            case "posted" -> VOUCHER_STATUS_POSTED;
            case "voided" -> VOUCHER_STATUS_VOIDED;
            default -> null;
        };
    }

    /**
     * 前端事件类型 → 后端来源类型映射
     */
    private Integer mapEventTypeToSourceType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        return switch (eventType) {
            case "purchase_in" -> 2;
            case "sale_out" -> 3;
            case "expense" -> 10;
            case "depreciation" -> 8;
            case "adjustment" -> 1;
            default -> null;
        };
    }
}
