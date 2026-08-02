package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.entity.finance.FinanceVoucher;
import com.foodtraceability.entity.finance.FinanceVoucherDetail;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import com.foodtraceability.mapper.finance.FinanceVoucherDetailMapper;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
import com.foodtraceability.service.finance.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 记账凭证Service实现类
 * 核心业务逻辑：借贷平衡校验、凭证状态流转管理
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<FinanceVoucherMapper, FinanceVoucher>
        implements VoucherService {

    private static final Logger log = LoggerFactory.getLogger(VoucherServiceImpl.class);

    private final FinanceVoucherDetailMapper detailMapper;
    private final AccountingSubjectMapper subjectMapper;

    public VoucherServiceImpl(FinanceVoucherMapper voucherMapper,
                              FinanceVoucherDetailMapper detailMapper,
                              AccountingSubjectMapper subjectMapper) {
        this.detailMapper = detailMapper;
        this.subjectMapper = subjectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO create(FinanceVoucherCreateDTO dto) {
        log.info("创建记账凭证，日期：{}，类型：{}", dto.getVoucherDate(), dto.getVoucherType());

        // 1. 校验分录明细不为空
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new BusinessException("分录明细不能为空");
        }

        // 2. 校验借贷平衡
        if (!checkBalance(dto.getDetails())) {
            throw new BusinessException("借贷不平衡，借方合计必须等于贷方合计");
        }

        // 3. 校验每个科目的有效性
        for (FinanceVoucherCreateDTO.VoucherDetailItem item : dto.getDetails()) {
            AccountingSubject subject = subjectMapper.selectById(item.getSubjectId());
            if (subject == null) {
                throw new BusinessException("会计科目不存在：" + item.getSubjectId());
            }
            if (subject.getStatus() == null || subject.getStatus() != 1) {
                throw new BusinessException("会计科目已停用：" + subject.getSubjectName());
            }
        }

        // 4. 创建凭证头
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setVoucherNo(generateVoucherNo());
        voucher.setVoucherDate(dto.getVoucherDate());
        voucher.setVoucherType(dto.getVoucherType());
        voucher.setVoucherStatus(0); // 暂存状态
        voucher.setAttachmentCount(dto.getAttachmentCount() != null ? dto.getAttachmentCount() : 0);
        voucher.setReferenceNo(dto.getReferenceNo());
        voucher.setSourceType(dto.getSourceType());
        voucher.setSourceId(dto.getSourceId());
        voucher.setRemark(dto.getRemark());

        // 计算借贷合计
        AtomicLong totalDebit = new AtomicLong(0);
        AtomicLong totalCredit = new AtomicLong(0);
        dto.getDetails().forEach(item -> {
            totalDebit.addAndGet(item.getDebitAmount() != null ? item.getDebitAmount() : 0L);
            totalCredit.addAndGet(item.getCreditAmount() != null ? item.getCreditAmount() : 0L);
        });
        voucher.setTotalDebit(totalDebit.get());
        voucher.setTotalCredit(totalCredit.get());

        this.save(voucher);

        // 5. 创建分录明细
        List<FinanceVoucherDetail> details = new ArrayList<>();
        int sortOrder = 1;
        for (FinanceVoucherCreateDTO.VoucherDetailItem item : dto.getDetails()) {
            FinanceVoucherDetail detail = new FinanceVoucherDetail();
            detail.setVoucherId(voucher.getVoucherId());
            detail.setSummary(item.getSummary());
            detail.setSubjectId(item.getSubjectId());
            detail.setDebitAmount(item.getDebitAmount() != null ? item.getDebitAmount() : 0L);
            detail.setCreditAmount(item.getCreditAmount() != null ? item.getCreditAmount() : 0L);
            detail.setAuxiliaryItem(item.getAuxiliaryItem());
            detail.setSortOrder(sortOrder++);
            details.add(detail);
        }
        // 循环插入明细
        for (FinanceVoucherDetail detail : details) {
            detailMapper.insert(detail);
        }

        log.info("记账凭证创建成功，凭证号：{}", voucher.getVoucherNo());

        return getDetail(voucher.getVoucherId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(FinanceVoucherUpdateDTO dto) {
        log.info("更新记账凭证，ID：{}", dto.getVoucherId());

        FinanceVoucher voucher = this.getById(dto.getVoucherId());
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        // 仅暂存状态可修改
        if (voucher.getVoucherStatus() != null && voucher.getVoucherStatus() != 0) {
            throw new BusinessException("只有暂存状态的凭证可以修改");
        }

        // 更新凭证头信息
        if (dto.getVoucherDate() != null) {
            voucher.setVoucherDate(dto.getVoucherDate());
        }
        if (dto.getVoucherType() != null) {
            voucher.setVoucherType(dto.getVoucherType());
        }
        if (dto.getAttachmentCount() != null) {
            voucher.setAttachmentCount(dto.getAttachmentCount());
        }
        if (dto.getReferenceNo() != null) {
            voucher.setReferenceNo(dto.getReferenceNo());
        }
        if (dto.getRemark() != null) {
            voucher.setRemark(dto.getRemark());
        }

        // 如果有新的分录明细，重新校验并保存
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            // 校验借贷平衡
            List<FinanceVoucherCreateDTO.VoucherDetailItem> items = new ArrayList<>();
            dto.getDetails().forEach(d -> {
                FinanceVoucherCreateDTO.VoucherDetailItem item = new FinanceVoucherCreateDTO.VoucherDetailItem();
                item.setSummary(d.getSummary());
                item.setSubjectId(d.getSubjectId());
                item.setDebitAmount(d.getDebitAmount());
                item.setCreditAmount(d.getCreditAmount());
                item.setAuxiliaryItem(d.getAuxiliaryItem());
                items.add(item);
            });
            if (!checkBalance(items)) {
                throw new BusinessException("借贷不平衡，借方合计必须等于贷方合计");
            }

            // 删除旧明细
            detailMapper.deleteByVoucherId(voucher.getVoucherId());

            // 计算新的借贷合计
            AtomicLong totalDebit = new AtomicLong(0);
            AtomicLong totalCredit = new AtomicLong(0);
            dto.getDetails().forEach(d -> {
                totalDebit.addAndGet(d.getDebitAmount() != null ? d.getDebitAmount() : 0L);
                totalCredit.addAndGet(d.getCreditAmount() != null ? d.getCreditAmount() : 0L);
            });
            voucher.setTotalDebit(totalDebit.get());
            voucher.setTotalCredit(totalCredit.get());

            // 插入新明细
            List<FinanceVoucherDetail> details = new ArrayList<>();
            int sortOrder = 1;
            for (FinanceVoucherUpdateDTO.VoucherDetailItem d : dto.getDetails()) {
                FinanceVoucherDetail detail = new FinanceVoucherDetail();
                detail.setVoucherId(voucher.getVoucherId());
                detail.setSummary(d.getSummary());
                detail.setSubjectId(d.getSubjectId());
                detail.setDebitAmount(d.getDebitAmount() != null ? d.getDebitAmount() : 0L);
                detail.setCreditAmount(d.getCreditAmount() != null ? d.getCreditAmount() : 0L);
                detail.setAuxiliaryItem(d.getAuxiliaryItem());
                detail.setSortOrder(sortOrder++);
                details.add(detail);
            }
            // 循环插入新明细
            for (FinanceVoucherDetail detail : details) {
                detailMapper.insert(detail);
            }
        }

        return this.updateById(voucher);
    }

    @Override
    public FinanceVoucherVO getDetail(Long voucherId) {
        FinanceVoucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        FinanceVoucherVO vo = convertToVO(voucher);

        // 查询分录明细
        List<FinanceVoucherDetail> details = detailMapper.selectByVoucherId(voucherId);
        List<FinanceVoucherDetailVO> detailVOs = new ArrayList<>();
        for (FinanceVoucherDetail detail : details) {
            FinanceVoucherDetailVO detailVO = convertDetailToVO(detail);
            detailVOs.add(detailVO);
        }
        vo.setDetails(detailVOs);

        return vo;
    }

    @Override
    public IPage<FinanceVoucherVO> getPage(FinanceVoucherQueryDTO query) {
        Page<FinanceVoucher> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<FinanceVoucher> voucherPage = baseMapper.selectVoucherPage(page,
                query.getVoucherNo(), query.getStartDate(), query.getEndDate(),
                query.getVoucherType(), query.getVoucherStatus());

        // 转换为VO
        Page<FinanceVoucherVO> voPage = new Page<>(voucherPage.getCurrent(), voucherPage.getSize(), voucherPage.getTotal());
        List<FinanceVoucherVO> records = new ArrayList<>();
        for (FinanceVoucher voucher : voucherPage.getRecords()) {
            records.add(convertToVO(voucher));
        }
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long voucherId) {
        log.info("审核凭证，ID：{}", voucherId);

        FinanceVoucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        if (voucher.getVoucherStatus() == null || voucher.getVoucherStatus() != 0) {
            throw new BusinessException("只有暂存状态的凭证可以审核");
        }

        voucher.setVoucherStatus(1); // 已审核
        voucher.setApproveTime(java.time.LocalDateTime.now());
        return this.updateById(voucher);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean post(Long voucherId) {
        log.info("过账凭证，ID：{}", voucherId);

        // 1. 校验凭证存在
        FinanceVoucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        // 2. 校验凭证状态为已审核(1)
        if (voucher.getVoucherStatus() == null || voucher.getVoucherStatus() != 1) {
            throw new BusinessException("只有已审核状态的凭证可以过账");
        }

        // 3. 查询凭证分录明细
        List<FinanceVoucherDetail> details = detailMapper.selectByVoucherId(voucherId);
        if (details == null || details.isEmpty()) {
            throw new BusinessException("凭证分录明细为空，无法过账");
        }

        // 4. 按借贷方向更新科目余额（ADR-005）
        for (FinanceVoucherDetail detail : details) {
            applySubjectBalanceUpdate(detail, 1);
        }

        // 5. 更新凭证状态为已过账(2)
        voucher.setVoucherStatus(2);
        return this.updateById(voucher);
    }

    /**
     * 应用单条分录对应的科目余额更新（ADR-005 规则，乐观锁）
     *
     * <p>4 种规则：
     * - 借方分录 + 借方科目(direction=1) → balance += debit
     * - 借方分录 + 贷方科目(direction=2) → balance -= debit
     * - 贷方分录 + 借方科目(direction=1) → balance -= credit
     * - 贷方分录 + 贷方科目(direction=2) → balance += credit</p>
     *
     * <p>简化为：借方科目 delta = debit - credit；贷方科目 delta = credit - debit。
     * sign=1 正向（过账），sign=-1 反向（反过账）。</p>
     *
     * @param detail 凭证分录明细
     * @param sign 符号：1=正向(过账)，-1=反向(反过账)
     * @throws BusinessException 科目不存在、方向为空、乐观锁冲突时抛出
     */
    private void applySubjectBalanceUpdate(FinanceVoucherDetail detail, int sign) {
        AccountingSubject subject = subjectMapper.selectById(detail.getSubjectId());
        if (subject == null) {
            throw new BusinessException("科目不存在：" + detail.getSubjectId());
        }
        Long delta = calculateBalanceDelta(detail, subject);
        if (delta == null || delta == 0L) {
            // 借贷均为0，跳过更新
            return;
        }
        Long actualDelta = sign == 1 ? delta : -delta;
        Integer version = subject.getVersion();
        int rows = subjectMapper.updateBalanceWithOptimisticLock(
                subject.getSubjectId(), actualDelta, version);
        if (rows == 0) {
            throw new BusinessException("科目余额更新失败（乐观锁冲突）："
                    + subject.getSubjectCode());
        }
    }

    /**
     * 计算科目余额变化量（ADR-005 规则）
     *
     * @param detail 凭证分录明细
     * @param subject 会计科目
     * @return 余额变化量（正/负/零）；科目方向为空时抛 BusinessException
     */
    private Long calculateBalanceDelta(FinanceVoucherDetail detail, AccountingSubject subject) {
        long debit = detail.getDebitAmount() != null ? detail.getDebitAmount() : 0L;
        long credit = detail.getCreditAmount() != null ? detail.getCreditAmount() : 0L;
        Integer direction = subject.getDirection();
        if (direction == null) {
            throw new BusinessException("科目余额方向为空：" + subject.getSubjectCode());
        }
        if (direction == 1) {
            // 借方科目：借方分录 +debit，贷方分录 -credit
            return debit - credit;
        } else {
            // 贷方科目：借方分录 -debit，贷方分录 +credit
            return credit - debit;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unapprove(Long voucherId) {
        log.info("反审核凭证，ID：{}", voucherId);

        FinanceVoucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        if (voucher.getVoucherStatus() == null || voucher.getVoucherStatus() != 1) {
            throw new BusinessException("只有已审核状态的凭证可以反审核");
        }

        voucher.setVoucherStatus(0); // 暂存
        voucher.setApproveTime(null);
        return this.updateById(voucher);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpost(Long voucherId) {
        log.info("反过账凭证，ID：{}", voucherId);

        // 1. 校验凭证存在
        FinanceVoucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        // 2. 校验凭证状态为已过账(2)
        if (voucher.getVoucherStatus() == null || voucher.getVoucherStatus() != 2) {
            throw new BusinessException("只有已过账状态的凭证可以反过账");
        }

        // 3. 查询凭证分录明细
        List<FinanceVoucherDetail> details = detailMapper.selectByVoucherId(voucherId);
        if (details == null || details.isEmpty()) {
            throw new BusinessException("凭证分录明细为空，无法反过账");
        }

        // 4. 反向更新科目余额（ADR-005，sign=-1）
        for (FinanceVoucherDetail detail : details) {
            applySubjectBalanceUpdate(detail, -1);
        }

        // 5. 更新凭证状态为已审核(1)
        voucher.setVoucherStatus(1);
        return this.updateById(voucher);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidVoucher(Long voucherId) {
        log.info("作废凭证，ID：{}", voucherId);

        FinanceVoucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }

        if (voucher.getVoucherStatus() != null && voucher.getVoucherStatus() == 2) {
            throw new BusinessException("已过账的凭证不能直接作废，请使用红字冲销");
        }

        if (voucher.getVoucherStatus() != null && (voucher.getVoucherStatus() == 0 || voucher.getVoucherStatus() == 1)) {
            voucher.setVoucherStatus(3); // 已作废
            return this.updateById(voucher);
        }

        throw new BusinessException("当前状态不允许作废");
    }

    @Override
    public String generateVoucherNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大序号
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FinanceVoucher::getVoucherNo, "PZ" + datePart)
               .orderByDesc(FinanceVoucher::getVoucherId)
               .last("LIMIT 1");
        FinanceVoucher lastVoucher = this.getOne(wrapper, false);

        int seq = 1;
        if (lastVoucher != null && lastVoucher.getVoucherNo() != null) {
            String lastNo = lastVoucher.getVoucherNo();
            String seqStr = lastNo.substring(lastNo.length() - 4);
            seq = Integer.parseInt(seqStr) + 1;
        }

        return String.format("PZ%s%04d", datePart, seq);
    }

    @Override
    public boolean checkBalance(List<FinanceVoucherCreateDTO.VoucherDetailItem> details) {
        if (details == null || details.isEmpty()) {
            return false;
        }

        long totalDebit = 0;
        long totalCredit = 0;

        for (FinanceVoucherCreateDTO.VoucherDetailItem item : details) {
            totalDebit += item.getDebitAmount() != null ? item.getDebitAmount() : 0L;
            totalCredit += item.getCreditAmount() != null ? item.getCreditAmount() : 0L;
        }

        return totalDebit == totalCredit;
    }

    /**
     * 将实体转换为VO
     */
    private FinanceVoucherVO convertToVO(FinanceVoucher voucher) {
        FinanceVoucherVO vo = new FinanceVoucherVO();
        vo.setVoucherId(voucher.getVoucherId());
        vo.setVoucherNo(voucher.getVoucherNo());
        vo.setVoucherDate(voucher.getVoucherDate());
        vo.setVoucherType(voucher.getVoucherType());
        vo.setVoucherTypeName(getVoucherTypeName(voucher.getVoucherType()));
        vo.setVoucherStatus(voucher.getVoucherStatus());
        vo.setVoucherStatusName(getVoucherStatusName(voucher.getVoucherStatus()));
        vo.setTotalDebit(voucher.getTotalDebit());
        vo.setTotalCredit(voucher.getTotalCredit());
        vo.setTotalDebitDisplay(formatAmount(voucher.getTotalDebit()));
        vo.setTotalCreditDisplay(formatAmount(voucher.getTotalCredit()));
        vo.setAttachmentCount(voucher.getAttachmentCount());
        vo.setReferenceNo(voucher.getReferenceNo());
        vo.setSourceType(voucher.getSourceType());
        vo.setSourceId(voucher.getSourceId());
        vo.setRemark(voucher.getRemark());
        vo.setCreateTime(voucher.getCreateTime());
        vo.setUpdateTime(voucher.getUpdateTime());
        return vo;
    }

    /**
     * 将明细实体转换为VO
     */
    private FinanceVoucherDetailVO convertDetailToVO(FinanceVoucherDetail detail) {
        FinanceVoucherDetailVO vo = new FinanceVoucherDetailVO();
        vo.setDetailId(detail.getDetailId());
        vo.setSummary(detail.getSummary());
        vo.setSubjectId(detail.getSubjectId());

        // 查询科目名称
        if (detail.getSubjectId() != null) {
            AccountingSubject subject = subjectMapper.selectById(detail.getSubjectId());
            if (subject != null) {
                vo.setSubjectCode(subject.getSubjectCode());
                vo.setSubjectName(subject.getSubjectName());
            }
        }

        vo.setDebitAmount(detail.getDebitAmount());
        vo.setCreditAmount(detail.getCreditAmount());
        vo.setDebitAmountDisplay(formatAmount(detail.getDebitAmount()));
        vo.setCreditAmountDisplay(formatAmount(detail.getCreditAmount()));
        vo.setAuxiliaryItem(detail.getAuxiliaryItem());
        vo.setSortOrder(detail.getSortOrder());
        return vo;
    }

    /** 获取凭证类型名称 */
    private String getVoucherTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "手工凭证";
            case 2: return "采购入库";
            case 3: return "销售出库";
            case 4: return "费用";
            case 5: return "付款";
            case 6: return "收款";
            case 7: return "转账";
            default: return "未知";
        }
    }

    /** 获取凭证状态名称 */
    private String getVoucherStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "暂存";
            case 1: return "已审核";
            case 2: return "已过账";
            case 3: return "已作废";
            default: return "未知";
        }
    }

    /** 格式化金额（分转元显示） */
    private String formatAmount(Long amount) {
        if (amount == null) return "0.00";
        return String.valueOf(amount / 100.0);
    }
}
