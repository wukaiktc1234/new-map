package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.*;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.*;
import com.foodtraceability.service.finance.AccountingPeriodService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 会计期间Service实现
 * 管理会计期间的生命周期，包括结账、反结账、试算平衡、损益结转等核心财务流程
 */
@Service
public class AccountingPeriodServiceImpl extends ServiceImpl<AccountingPeriodMapper, AccountingPeriod>
        implements AccountingPeriodService {

    private final FinanceVoucherMapper voucherMapper;
    private final FinanceVoucherDetailMapper voucherDetailMapper;
    private final AccountingSubjectMapper subjectMapper;

    public AccountingPeriodServiceImpl(FinanceVoucherMapper voucherMapper,
                                       FinanceVoucherDetailMapper voucherDetailMapper,
                                       AccountingSubjectMapper subjectMapper) {
        this.voucherMapper = voucherMapper;
        this.voucherDetailMapper = voucherDetailMapper;
        this.subjectMapper = subjectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountingPeriodVO create(AccountingPeriodCreateDTO dto) {
        // 校验期间编码唯一性
        LambdaQueryWrapper<AccountingPeriod> codeCheck = new LambdaQueryWrapper<>();
        codeCheck.eq(AccountingPeriod::getPeriodCode, dto.getPeriodCode());
        if (baseMapper.selectCount(codeCheck) > 0) {
            throw new BusinessException("期间编码已存在: " + dto.getPeriodCode());
        }

        // 校验日期范围
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }

        AccountingPeriod entity = new AccountingPeriod();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1); // 默认开放
        entity.setIsClosed(false);
        entity.setTrialBalancePassed(false);

        baseMapper.insert(entity);
        return convertToVO(entity);
    }

    @Override
    public AccountingPeriodVO getDetail(Long periodId) {
        AccountingPeriod entity = baseMapper.selectById(periodId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<AccountingPeriodVO> getPage(int current, int size, Integer periodType, Integer status) {
        Page<AccountingPeriod> page = new Page<>(current, size);
        LambdaQueryWrapper<AccountingPeriod> wrapper = new LambdaQueryWrapper<>();

        if (periodType != null) {
            wrapper.eq(AccountingPeriod::getPeriodType, periodType);
        }
        if (status != null) {
            wrapper.eq(AccountingPeriod::getStatus, status);
        }
        wrapper.orderByDesc(AccountingPeriod::getStartDate);

        IPage<AccountingPeriod> entityPage = baseMapper.selectPage(page, wrapper);

        Page<AccountingPeriodVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<AccountingPeriodVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<AccountingPeriodVO> result = (IPage<AccountingPeriodVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closePeriod(Long periodId, Long operatorId) {
        AccountingPeriod period = baseMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException("会计期间不存在");
        }
        if (period.getStatus() != null && period.getStatus() == 2) {
            throw new BusinessException("该期间已结账");
        }

        // 检查结账条件
        ClosingChecklistVO checklist = getClosingChecklist(periodId);
        if (!Boolean.TRUE.equals(checklist.getCanClose())) {
            throw new BusinessException("结账条件不满足，请检查结账清单");
        }

        period.setStatus(2); // 已结账
        period.setIsClosed(true);
        period.setCloseTime(LocalDateTime.now());
        period.setCloseUserId(operatorId);
        return baseMapper.updateById(period) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reopenPeriod(Long periodId, Long operatorId) {
        AccountingPeriod period = baseMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException("会计期间不存在");
        }
        if (period.getStatus() == null || period.getStatus() != 2) {
            throw new BusinessException("期间未结账，无需反结账");
        }

        // 检查后续期间是否已结账（已封账不可反结账）
        LambdaQueryWrapper<AccountingPeriod> laterCheck = new LambdaQueryWrapper<>();
        laterCheck.gt(AccountingPeriod::getStartDate, period.getStartDate())
                  .eq(AccountingPeriod::getStatus, 2);
        if (baseMapper.selectCount(laterCheck) > 0) {
            throw new BusinessException("后续期间已结账（已封账），不可反结账");
        }

        period.setStatus(1); // 重新开放
        period.setIsClosed(false);
        period.setCloseTime(null);
        period.setCloseUserId(null);
        return baseMapper.updateById(period) > 0;
    }

    @Override
    public TrialBalanceResultVO trialBalance(Long periodId) {
        AccountingPeriod period = baseMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException("会计期间不存在");
        }

        // 查询该期间所有已过账凭证（status=2）
        List<FinanceVoucher> postedVouchers = getPostedVouchersInPeriod(period);

        // 收集所有凭证ID
        List<Long> voucherIds = postedVouchers.stream()
                .map(FinanceVoucher::getVoucherId)
                .collect(Collectors.toList());

        TrialBalanceResultVO result = new TrialBalanceResultVO();
        long totalDebit = 0L;
        long totalCredit = 0L;

        if (voucherIds.isEmpty()) {
            result.setIsBalanced(true);
            result.setTotalDebit(0L);
            result.setTotalCredit(0L);
            result.setDifference(0L);
            result.setDetails(Collections.emptyList());
            return result;
        }

        // 查询所有分录明细
        LambdaQueryWrapper<FinanceVoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(FinanceVoucherDetail::getVoucherId, voucherIds);
        List<FinanceVoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        // 按科目汇总借方/贷方
        Map<Long, long[]> subjectBalanceMap = new HashMap<>(); // subjectId -> [debitSum, creditSum]
        for (FinanceVoucherDetail detail : details) {
            Long subjectId = detail.getSubjectId();
            long[] balance = subjectBalanceMap.computeIfAbsent(subjectId, k -> new long[2]);
            balance[0] += detail.getDebitAmount() != null ? detail.getDebitAmount() : 0L;
            balance[1] += detail.getCreditAmount() != null ? detail.getCreditAmount() : 0L;
            totalDebit += detail.getDebitAmount() != null ? detail.getDebitAmount() : 0L;
            totalCredit += detail.getCreditAmount() != null ? detail.getCreditAmount() : 0L;
        }

        // 查询科目信息
        Map<Long, AccountingSubject> subjectMap = new HashMap<>();
        if (!subjectBalanceMap.isEmpty()) {
            List<AccountingSubject> subjects = subjectMapper.selectBatchIds(subjectBalanceMap.keySet());
            for (AccountingSubject s : subjects) {
                subjectMap.put(s.getSubjectId(), s);
            }
        }

        // 构建明细列表
        List<SubjectBalanceItem> items = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : subjectBalanceMap.entrySet()) {
            AccountingSubject subject = subjectMap.get(entry.getKey());
            String code = subject != null ? subject.getSubjectCode() : "";
            String name = subject != null ? subject.getSubjectName() : "未知科目";
            long debit = entry.getValue()[0];
            long credit = entry.getValue()[1];
            items.add(new SubjectBalanceItem(code, name, debit, credit));
        }
        // 按科目编码排序
        items.sort(Comparator.comparing(SubjectBalanceItem::getSubjectCode));

        result.setTotalDebit(totalDebit);
        result.setTotalCredit(totalCredit);
        result.setDifference(totalDebit - totalCredit);
        result.setIsBalanced(totalDebit == totalCredit);
        result.setDetails(items);

        // 更新期间的试算平衡标志
        if (!Boolean.TRUE.equals(period.getTrialBalancePassed()) && totalDebit == totalCredit) {
            period.setTrialBalancePassed(true);
            baseMapper.updateById(period);
        }

        return result;
    }

    @Override
    public ClosingChecklistVO getClosingChecklist(Long periodId) {
        AccountingPeriod period = baseMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException("会计期间不存在");
        }

        ClosingChecklistVO checklist = new ClosingChecklistVO();
        checklist.setPeriodId(periodId);
        checklist.setPeriodName(period.getPeriodName());

        List<ClosingChecklistVO.ChecklistItem> items = new ArrayList<>();

        // 1. 检查是否有未审核/未过账凭证（status=0或1）
        List<FinanceVoucher> allVouchers = getVouchersInPeriod(period);
        int unauditedCount = 0;
        for (FinanceVoucher v : allVouchers) {
            if (v.getVoucherStatus() != null && v.getVoucherStatus() < 2) {
                unauditedCount++;
            }
        }
        boolean hasUnaudited = unauditedCount > 0;
        checklist.setHasUnauditedVouchers(hasUnaudited);
        checklist.setUnauditedCount(unauditedCount);
        items.add(new ClosingChecklistVO.ChecklistItem(
                "凭证审核与过账", !hasUnaudited,
                hasUnaudited ? "存在" + unauditedCount + "张未过账凭证" : "所有凭证已过账"));

        // 2. 检查试算平衡
        TrialBalanceResultVO trialResult = trialBalance(periodId);
        boolean trialPassed = Boolean.TRUE.equals(trialResult.getIsBalanced());
        checklist.setTrialBalancePassed(trialPassed);
        items.add(new ClosingChecklistVO.ChecklistItem(
                "试算平衡", trialPassed,
                trialPassed ? "借贷平衡" : "借贷不平衡，差额：" + trialResult.getDifference() + "分"));

        // 3. 检查损益是否已结转
        boolean profitTransferred = checkProfitTransferred(period);
        checklist.setProfitTransferred(profitTransferred);
        items.add(new ClosingChecklistVO.ChecklistItem(
                "损益结转", profitTransferred,
                profitTransferred ? "损益已结转" : "损益尚未结转"));

        // 综合判断是否可以结账
        boolean canClose = !hasUnaudited && trialPassed && profitTransferred;
        checklist.setCanClose(canClose);
        checklist.setItems(items);

        return checklist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean profitTransfer(Long periodId, Long operatorId) {
        AccountingPeriod period = baseMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException("会计期间不存在");
        }

        // 检查是否已结转
        if (checkProfitTransferred(period)) {
            throw new BusinessException("该期间损益已结转，不可重复操作");
        }

        // 查询本年利润科目（3103）
        AccountingSubject profitSubject = getSubjectByCode("3103");
        if (profitSubject == null) {
            throw new BusinessException("本年利润科目(3103)不存在，无法进行损益结转");
        }

        // 查询所有损益类科目（subjectType=5），叶子节点且启用
        LambdaQueryWrapper<AccountingSubject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(AccountingSubject::getSubjectType, 5)
                      .eq(AccountingSubject::getIsLeaf, true)
                      .eq(AccountingSubject::getStatus, 1);
        List<AccountingSubject> profitLossSubjects = subjectMapper.selectList(subjectWrapper);

        if (profitLossSubjects.isEmpty()) {
            throw new BusinessException("没有可结转的损益类科目");
        }

        // 查询该期间所有已过账凭证
        List<FinanceVoucher> postedVouchers = getPostedVouchersInPeriod(period);
        List<Long> voucherIds = postedVouchers.stream()
                .map(FinanceVoucher::getVoucherId)
                .collect(Collectors.toList());

        // 查询所有分录明细
        Map<Long, long[]> subjectBalanceMap = new HashMap<>(); // subjectId -> [debitSum, creditSum]
        if (!voucherIds.isEmpty()) {
            LambdaQueryWrapper<FinanceVoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(FinanceVoucherDetail::getVoucherId, voucherIds);
            List<FinanceVoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);
            for (FinanceVoucherDetail detail : details) {
                Long subjectId = detail.getSubjectId();
                long[] balance = subjectBalanceMap.computeIfAbsent(subjectId, k -> new long[2]);
                balance[0] += detail.getDebitAmount() != null ? detail.getDebitAmount() : 0L;
                balance[1] += detail.getCreditAmount() != null ? detail.getCreditAmount() : 0L;
            }
        }

        // 计算每个损益类科目的净余额并生成结转分录
        List<FinanceVoucherDetail> transferDetails = new ArrayList<>();
        long totalDebitToProfit = 0L; // 借记本年利润（费用类结转）
        long totalCreditToProfit = 0L; // 贷记本年利润（收入类结转）
        int sortOrder = 1;

        for (AccountingSubject subject : profitLossSubjects) {
            long[] balance = subjectBalanceMap.get(subject.getSubjectId());
            if (balance == null) {
                continue;
            }
            long netBalance = balance[0] - balance[1]; // 借方 - 贷方
            if (netBalance == 0L) {
                continue;
            }

            if (netBalance > 0) {
                // 借方余额（费用类）：贷记该科目以清零，借记本年利润
                FinanceVoucherDetail creditDetail = new FinanceVoucherDetail();
                creditDetail.setSummary("损益结转-" + subject.getSubjectName());
                creditDetail.setSubjectId(subject.getSubjectId());
                creditDetail.setDebitAmount(0L);
                creditDetail.setCreditAmount(netBalance);
                creditDetail.setSortOrder(sortOrder++);
                transferDetails.add(creditDetail);
                totalDebitToProfit += netBalance;
            } else {
                // 贷方余额（收入类）：借记该科目以清零，贷记本年利润
                long absNet = Math.abs(netBalance);
                FinanceVoucherDetail debitDetail = new FinanceVoucherDetail();
                debitDetail.setSummary("损益结转-" + subject.getSubjectName());
                debitDetail.setSubjectId(subject.getSubjectId());
                debitDetail.setDebitAmount(absNet);
                debitDetail.setCreditAmount(0L);
                debitDetail.setSortOrder(sortOrder++);
                transferDetails.add(debitDetail);
                totalCreditToProfit += absNet;
            }
        }

        // 添加本年利润的结转分录
        if (totalDebitToProfit > 0) {
            FinanceVoucherDetail profitDebit = new FinanceVoucherDetail();
            profitDebit.setSummary("损益结转-本年利润（费用）");
            profitDebit.setSubjectId(profitSubject.getSubjectId());
            profitDebit.setDebitAmount(totalDebitToProfit);
            profitDebit.setCreditAmount(0L);
            profitDebit.setSortOrder(sortOrder++);
            transferDetails.add(profitDebit);
        }
        if (totalCreditToProfit > 0) {
            FinanceVoucherDetail profitCredit = new FinanceVoucherDetail();
            profitCredit.setSummary("损益结转-本年利润（收入）");
            profitCredit.setSubjectId(profitSubject.getSubjectId());
            profitCredit.setDebitAmount(0L);
            profitCredit.setCreditAmount(totalCreditToProfit);
            profitCredit.setSortOrder(sortOrder++);
            transferDetails.add(profitCredit);
        }

        if (transferDetails.isEmpty()) {
            throw new BusinessException("本期损益类科目无余额，无需结转");
        }

        // 创建结转凭证
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setVoucherNo(generateVoucherNo());
        voucher.setVoucherDate(period.getEndDate());
        voucher.setVoucherType(1); // 手工凭证
        voucher.setVoucherStatus(2); // 直接过账
        voucher.setAttachmentCount(0);
        voucher.setReferenceNo(period.getPeriodCode());
        voucher.setRemark("损益结转-" + period.getPeriodName());
        voucher.setCreateUserId(operatorId);
        voucher.setTotalDebit(totalDebitToProfit + totalCreditToProfit);
        voucher.setTotalCredit(totalDebitToProfit + totalCreditToProfit);
        voucherMapper.insert(voucher);

        // 插入分录明细
        for (FinanceVoucherDetail detail : transferDetails) {
            detail.setVoucherId(voucher.getVoucherId());
            voucherDetailMapper.insert(detail);
        }

        return true;
    }

    @Override
    public AccountingPeriod getCurrentPeriod() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<AccountingPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(AccountingPeriod::getStartDate, today)
               .ge(AccountingPeriod::getEndDate, today)
               .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取期间内所有凭证
     */
    private List<FinanceVoucher> getVouchersInPeriod(AccountingPeriod period) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(FinanceVoucher::getVoucherDate, period.getStartDate())
               .le(FinanceVoucher::getVoucherDate, period.getEndDate())
               .ne(FinanceVoucher::getVoucherStatus, 3); // 排除已作废
        return voucherMapper.selectList(wrapper);
    }

    /**
     * 获取期间内所有已过账凭证（status=2）
     */
    private List<FinanceVoucher> getPostedVouchersInPeriod(AccountingPeriod period) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceVoucher::getVoucherStatus, 2)
               .ge(FinanceVoucher::getVoucherDate, period.getStartDate())
               .le(FinanceVoucher::getVoucherDate, period.getEndDate());
        return voucherMapper.selectList(wrapper);
    }

    /**
     * 检查损益是否已结转
     * 通过查询期间内是否存在备注为"损益结转"的凭证判断
     */
    private boolean checkProfitTransferred(AccountingPeriod period) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FinanceVoucher::getRemark, "损益结转")
               .ge(FinanceVoucher::getVoucherDate, period.getStartDate())
               .le(FinanceVoucher::getVoucherDate, period.getEndDate());
        return voucherMapper.selectCount(wrapper) > 0;
    }

    /**
     * 根据科目编码查询科目
     */
    private AccountingSubject getSubjectByCode(String code) {
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingSubject::getSubjectCode, code).last("LIMIT 1");
        return subjectMapper.selectOne(wrapper);
    }

    /**
     * 生成凭证号：PZ + yyyyMMdd + 4位序号
     */
    private String generateVoucherNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FinanceVoucher::getVoucherNo, "PZ" + datePart)
               .orderByDesc(FinanceVoucher::getVoucherId)
               .last("LIMIT 1");
        FinanceVoucher lastVoucher = voucherMapper.selectOne(wrapper);

        int seq = 1;
        if (lastVoucher != null && lastVoucher.getVoucherNo() != null) {
            String lastNo = lastVoucher.getVoucherNo();
            String seqStr = lastNo.substring(lastNo.length() - 4);
            seq = Integer.parseInt(seqStr) + 1;
        }
        return String.format("PZ%s%04d", datePart, seq);
    }

    /**
     * 实体转VO
     */
    private AccountingPeriodVO convertToVO(AccountingPeriod entity) {
        AccountingPeriodVO vo = new AccountingPeriodVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
