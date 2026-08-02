package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.service.SupplierService;
import com.foodtraceability.service.finance.PayableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 应付账款Service实现类
 */
@Service
public class PayableServiceImpl extends ServiceImpl<PayableMapper, Payable>
        implements PayableService {

    private static final Logger log = LoggerFactory.getLogger(PayableServiceImpl.class);

    private final SupplierService supplierService;

    public PayableServiceImpl(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayableVO create(PayableCreateDTO dto) {
        log.info("创建应付账款，供应商：{}，金额：{}", dto.getSupplierName(), dto.getOriginalAmount());

        Payable payable = new Payable();
        payable.setPayableNo(generatePayableNo());
        payable.setSupplierId(dto.getSupplierId());
        payable.setSupplierName(dto.getSupplierName());
        payable.setPurchaseOrderId(dto.getPurchaseOrderId());
        payable.setOriginalAmount(dto.getOriginalAmount());
        payable.setPaidAmount(0L);
        payable.setBalanceAmount(dto.getOriginalAmount());
        payable.setDueDate(dto.getDueDate());
        payable.setPaymentTerm(dto.getPaymentTerm());
        payable.setStatus(1); // 待付

        this.save(payable);

        return getDetail(payable.getPayableId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(PayableUpdateDTO dto) {
        log.info("更新应付账款，ID：{}，供应商：{}", dto.getPayableId(), dto.getSupplierName());

        Payable payable = this.getById(dto.getPayableId());
        if (payable == null) {
            throw new BusinessException("应付账款不存在");
        }

        /* 更新允许修改的字段 */
        if (dto.getSupplierName() != null) {
            payable.setSupplierName(dto.getSupplierName());
        }
        if (dto.getDueDate() != null) {
            payable.setDueDate(dto.getDueDate());
        }
        if (dto.getPaymentTerm() != null) {
            payable.setPaymentTerm(dto.getPaymentTerm());
        }
        /* 注意：Payable实体没有remark字段，已移除remark的更新逻辑 */

        return this.updateById(payable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePayable(Long payableId) {
        log.info("删除应付账款，ID：{}", payableId);

        Payable payable = this.getById(payableId);
        if (payable == null) {
            throw new BusinessException("应付账款不存在");
        }
        /* 已付清的应付账款不允许删除 */
        if (payable.getStatus() != null && payable.getStatus() == 3) {
            throw new BusinessException("已付清的应付账款不允许删除");
        }
        return this.removeById(payableId);
    }

    @Override
    public PayableVO getDetail(Long payableId) {
        Payable p = this.getById(payableId);
        if (p == null) {
            throw new BusinessException("应付账款不存在");
        }
        return convertToVO(p);
    }

    @Override
    public IPage<PayableVO> getPage(PayableQueryDTO query) {
        Page<Payable> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<Payable> result = baseMapper.selectPayablePage(page,
                query.getSupplierName(), query.getStatus(),
                query.getStartDate(), query.getEndDate());

        Page<PayableVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        java.util.List<PayableVO> list = new java.util.ArrayList<>();
        for (Payable p : result.getRecords()) {
            list.add(convertToVO(p));
        }
        voPage.setRecords(list);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPayment(Long payableId, Long amount) {
        log.info("确认付款，应付ID：{}，金额：{}", payableId, amount);

        Payable p = this.getById(payableId);
        if (p == null) {
            throw new BusinessException("应付账款不存在");
        }

        if (amount > p.getBalanceAmount()) {
            throw new BusinessException("付款金额不能大于余额");
        }

        long newPaid = p.getPaidAmount() + amount;
        long newBalance = p.getBalanceAmount() - amount;

        p.setPaidAmount(newPaid);
        p.setBalanceAmount(newBalance);

        // 判断是否全部付清
        if (newBalance <= 0) {
            p.setStatus(3); // 已付清
        } else {
            p.setStatus(2); // 部分支付
        }

        return this.updateById(p);
    }

    /**
     * 为采购入库单创建应付账款（T-038 联动）
     *
     * <p>幂等性：通过确定性的 payableNo（"AP" + 0填充 stockinId）实现。
     * 同一入库单重复调用返回已有记录，不产生重复数据。</p>
     *
     * <p>注：数据库暂停期间（ADR-006）不使用 source_type/source_id 字段，
     * 待数据库恢复后可迁移至标准的 source 字段幂等方案。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayableVO createForStockin(Long stockinId, String stockinNo, Long supplierId, String supplierName,
                                      Long orderId, String orderNo, Long amount, LocalDate stockinDate) {
        log.info("为采购入库单创建应付账款，入库单ID：{}，供应商：{}，金额：{}分",
                stockinId, supplierName, amount);

        // 1. 幂等性检查：通过确定性 payableNo 判断是否已存在
        String payableNo = generateStockinPayableNo(stockinId);
        Payable existing = this.lambdaQuery()
                .eq(Payable::getPayableNo, payableNo)
                .one();
        if (existing != null) {
            log.info("应付账款已存在（幂等跳过），入库单ID：{}，应付编号：{}", stockinId, payableNo);
            return convertToVO(existing);
        }

        // 2. 根据供应商账期计算到期日
        int paymentTerms = 30;
        if (supplierId != null) {
            Supplier supplier = supplierService.getById(supplierId);
            if (supplier != null && supplier.getPaymentTerms() != null) {
                paymentTerms = supplier.getPaymentTerms();
            }
        }
        LocalDate baseDate = stockinDate != null ? stockinDate : LocalDate.now();

        // 3. 构建并保存应付账款
        Payable payable = new Payable();
        payable.setPayableNo(payableNo);
        payable.setSupplierId(supplierId);
        payable.setSupplierName(supplierName != null ? supplierName : "供应商" + supplierId);
        payable.setPurchaseOrderId(orderId);
        payable.setStockinId(stockinId);
        payable.setStockinNo(stockinNo);
        payable.setOrderNo(orderNo);
        payable.setOriginalAmount(amount);
        payable.setPaidAmount(0L);
        payable.setBalanceAmount(amount);
        payable.setDueDate(baseDate.plusDays(paymentTerms));
        payable.setPaymentTerm(paymentTerms);
        payable.setStatus(1); // 待付

        this.save(payable);
        log.info("采购入库应付账款创建成功，入库单ID：{}，应付编号：{}", stockinId, payableNo);

        return convertToVO(payable);
    }

    /**
     * 创建红字应付单（采购退货）
     *
     * <p>退货金额为负，通过 related_invoice_id 关联原正向应付单，
     * invoice_type 标记为 red，支持供应商应付余额实时扣减。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayableVO createRedPayableForReturn(Long returnId, Long originalPayableId,
                                               Long supplierId, String supplierName,
                                               Long orderId, String orderNo,
                                               Long stockinId, String stockinNo,
                                               Long amount, LocalDate returnDate,
                                               Integer initialStatus) {
        log.info("创建红字应付单，退货单ID：{}，原应付ID：{}，金额：{}分", returnId, originalPayableId, amount);

        if (amount == null || amount <= 0) {
            throw new BusinessException("退货金额必须大于0");
        }

        int paymentTerms = 30;
        if (supplierId != null) {
            Supplier supplier = supplierService.getById(supplierId);
            if (supplier != null && supplier.getPaymentTerms() != null) {
                paymentTerms = supplier.getPaymentTerms();
            }
        }
        LocalDate baseDate = returnDate != null ? returnDate : LocalDate.now();

        Payable payable = new Payable();
        payable.setPayableNo(generateRedPayableNo());
        payable.setSupplierId(supplierId);
        payable.setSupplierName(supplierName != null ? supplierName : "供应商" + supplierId);
        payable.setPurchaseOrderId(orderId);
        payable.setStockinId(stockinId);
        payable.setStockinNo(stockinNo);
        payable.setOrderNo(orderNo);
        payable.setOriginalAmount(-amount);
        payable.setPaidAmount(0L);
        payable.setBalanceAmount(-amount);
        payable.setDueDate(baseDate.plusDays(paymentTerms));
        payable.setPaymentTerm(paymentTerms);
        payable.setStatus(initialStatus != null ? initialStatus : 1);
        payable.setRelatedInvoiceId(originalPayableId);
        payable.setInvoiceType("red");

        this.save(payable);
        log.info("红字应付单创建成功，退货单ID：{}，应付编号：{}，原应付ID：{}",
                returnId, payable.getPayableNo(), originalPayableId);

        return convertToVO(payable);
    }

    /**
     * 生成红字应付编号
     * 格式：YR + 年月日 + 4位序号
     */
    private String generateRedPayableNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payable>();
        wrapper.likeRight(Payable::getPayableNo, "YR" + datePart)
               .orderByDesc(Payable::getPayableId).last("LIMIT 1");
        Payable last = this.getOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getPayableNo() != null) {
            seq = Integer.parseInt(last.getPayableNo().substring(last.getPayableNo().length() - 4)) + 1;
        }
        return String.format("YR%s%04d", datePart, seq);
    }

    /**
     * 生成采购入库应付编号（确定性，幂等键）
     * 格式：AP + 0填充至10位的 stockinId
     * 示例：stockinId=123 → "AP0000000123"
     */
    private String generateStockinPayableNo(Long stockinId) {
        return "AP" + String.format("%010d", stockinId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayableVO createForReceiptConfirmation(Long confirmationId, String confirmationNo, Long supplierId,
                                                  String supplierName, Long orderId, String orderNo,
                                                  Long amount, LocalDate confirmDate) {
        log.info("为收货确认单创建应付账款，确认单ID：{}，供应商：{}，金额：{}分",
                confirmationId, supplierName, amount);

        String payableNo = generateConfirmationPayableNo(confirmationId);
        Payable existing = this.lambdaQuery()
                .eq(Payable::getPayableNo, payableNo)
                .one();
        if (existing != null) {
            log.info("应付账款已存在（幂等跳过），确认单ID：{}，应付编号：{}", confirmationId, payableNo);
            return convertToVO(existing);
        }

        int paymentTerms = 30;
        if (supplierId != null) {
            Supplier supplier = supplierService.getById(supplierId);
            if (supplier != null && supplier.getPaymentTerms() != null) {
                paymentTerms = supplier.getPaymentTerms();
            }
        }
        LocalDate baseDate = confirmDate != null ? confirmDate : LocalDate.now();

        Payable payable = new Payable();
        payable.setPayableNo(payableNo);
        payable.setSupplierId(supplierId);
        payable.setSupplierName(supplierName != null ? supplierName : "供应商" + supplierId);
        payable.setPurchaseOrderId(orderId);
        payable.setStockinId(confirmationId);
        payable.setStockinNo(confirmationNo);
        payable.setOrderNo(orderNo);
        payable.setOriginalAmount(amount);
        payable.setPaidAmount(0L);
        payable.setBalanceAmount(amount);
        payable.setDueDate(baseDate.plusDays(paymentTerms));
        payable.setPaymentTerm(paymentTerms);
        payable.setStatus(1);

        this.save(payable);
        log.info("收货确认应付账款创建成功，确认单ID：{}，应付编号：{}", confirmationId, payableNo);

        return convertToVO(payable);
    }

    /**
     * 生成收货确认单应付编号（确定性，幂等键）
     * 格式：AP + 0填充至10位的 confirmationId
     */
    private String generateConfirmationPayableNo(Long confirmationId) {
        return "AP" + String.format("%010d", confirmationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidPayableByStockinId(Long stockinId) {
        log.info("根据入库单ID作废应付账款，入库单ID：{}", stockinId);
        if (stockinId == null) {
            return true;
        }

        Payable payable = this.lambdaQuery()
                .eq(Payable::getStockinId, stockinId)
                .eq(Payable::getDeleted, 0)
                .one();
        if (payable == null) {
            log.info("入库单ID：{} 无关联应付账款，跳过作废", stockinId);
            return true;
        }

        // 仅允许作废待付（1）或逾期（4）的应付账款；已部分/全部付款会留下资金流水，禁止直接作废
        Integer status = payable.getStatus();
        if (status == null || (status != 1 && status != 4)) {
            throw new BusinessException("该入库单关联的应付账款已发生付款，请先作废付款单后再作废入库单");
        }

        boolean removed = this.removeById(payable.getPayableId());
        if (removed) {
            log.info("应付账款已作废，payableId：{}，payableNo：{}，入库单ID：{}",
                    payable.getPayableId(), payable.getPayableNo(), stockinId);
        }
        return removed;
    }

    private String generatePayableNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payable>();
        wrapper.likeRight(Payable::getPayableNo, "YF" + datePart)
               .orderByDesc(Payable::getPayableId).last("LIMIT 1");
        Payable last = this.getOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getPayableNo() != null) {
            seq = Integer.parseInt(last.getPayableNo().substring(last.getPayableNo().length() - 4)) + 1;
        }
        return String.format("YF%s%04d", datePart, seq);
    }

    private PayableVO convertToVO(Payable p) {
        PayableVO vo = new PayableVO();
        vo.setPayableId(p.getPayableId());
        vo.setPayableNo(p.getPayableNo());
        vo.setSupplierId(p.getSupplierId());
        vo.setSupplierName(p.getSupplierName());
        vo.setPurchaseOrderId(p.getPurchaseOrderId());
        vo.setStockinId(p.getStockinId());
        vo.setStockinNo(p.getStockinNo());
        vo.setOrderNo(p.getOrderNo());
        vo.setOriginalAmount(p.getOriginalAmount());
        vo.setOriginalAmountDisplay(formatAmount(p.getOriginalAmount()));
        vo.setPaidAmount(p.getPaidAmount());
        vo.setPaidAmountDisplay(formatAmount(p.getPaidAmount()));
        vo.setBalanceAmount(p.getBalanceAmount());
        vo.setBalanceAmountDisplay(formatAmount(p.getBalanceAmount()));
        vo.setDueDate(p.getDueDate());
        vo.setPaymentTerm(p.getPaymentTerm());
        vo.setStatus(p.getStatus());
        vo.setStatusName(getStatusName(p.getStatus()));
        vo.setRelatedInvoiceId(p.getRelatedInvoiceId());
        vo.setInvoiceType(p.getInvoiceType());
        vo.setCreateTime(p.getCreateTime());

        // 状态自动联动：未付清且已到期自动标记为逾期
        if (vo.getStatus() != null && vo.getStatus() != 3 && vo.getDueDate() != null) {
            if (vo.getDueDate().isBefore(LocalDate.now())) {
                vo.setStatus(4);
                vo.setStatusName("逾期");
            }
        }
        return vo;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) { case 1: return "待付"; case 2: return "部分支付"; case 3: return "已付清"; case 4: return "逾期"; default: return "未知"; }
    }

    private String formatAmount(Long amount) {
        if (amount == null) return "0.00";
        return String.valueOf(amount / 100.0);
    }
}
