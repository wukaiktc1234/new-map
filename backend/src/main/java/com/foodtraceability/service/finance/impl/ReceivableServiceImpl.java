package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import com.foodtraceability.service.finance.ReceivableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 应收账款Service实现类
 */
@Service
public class ReceivableServiceImpl extends ServiceImpl<ReceivableMapper, Receivable>
        implements ReceivableService {

    private static final Logger log = LoggerFactory.getLogger(ReceivableServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceivableVO create(ReceivableCreateDTO dto) {
        log.info("创建应收账款，客户：{}，金额：{}", dto.getCustomerName(), dto.getOriginalAmount());

        Receivable receivable = new Receivable();
        receivable.setReceivableNo(generateReceivableNo());
        receivable.setCustomerType(dto.getCustomerType() != null ? Integer.parseInt(dto.getCustomerType()) : 0);
        receivable.setCustomerId(dto.getCustomerId());
        receivable.setCustomerName(dto.getCustomerName());
        receivable.setOrderId(dto.getOrderId());
        receivable.setOriginalAmount(dto.getOriginalAmount() != null ? dto.getOriginalAmount().longValue() : 0L);
        receivable.setReceivedAmount(0L);
        receivable.setBalanceAmount(dto.getOriginalAmount() != null ? dto.getOriginalAmount().longValue() : 0L);
        receivable.setStatus(1); // 正常
        receivable.setDueDate(dto.getDueDate());

        this.save(receivable);

        return getDetail(receivable.getReceivableId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ReceivableUpdateDTO dto) {
        Receivable existing = this.getById(dto.getReceivableId());
        if (existing == null) {
            throw new BusinessException("应收账款不存在");
        }
        if (dto.getCustomerName() != null) {
            existing.setCustomerName(dto.getCustomerName());
        }
        if (dto.getOriginalAmount() != null) {
            long newOriginal = dto.getOriginalAmount().longValue();
            existing.setOriginalAmount(newOriginal);
            existing.setBalanceAmount(newOriginal - existing.getReceivedAmount());
        }
        if (dto.getRemark() != null) {
            existing.setRemark(dto.getRemark());
        }
        return this.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReceivable(Long receivableId) {
        log.info("删除应收账款，ID：{}", receivableId);

        Receivable receivable = this.getById(receivableId);
        if (receivable == null) {
            throw new BusinessException("应收账款不存在");
        }
        /* 已核销的应收账款不允许删除 */
        if (receivable.getStatus() != null && receivable.getStatus() == 3) {
            throw new BusinessException("已核销的应收账款不允许删除");
        }
        return this.removeById(receivableId);
    }

    @Override
    public ReceivableVO getDetail(Long receivableId) {
        Receivable r = this.getById(receivableId);
        if (r == null) {
            throw new BusinessException("应收账款不存在");
        }
        return convertToVO(r);
    }

    @Override
    public IPage<ReceivableVO> getPage(ReceivableQueryDTO query) {
        // 默认分页参数
        long current = 1L;
        long size = 10L;
        Page<Receivable> page = new Page<>(current, size);
        IPage<Receivable> result = baseMapper.selectReceivablePage(page,
                query.getCustomerName(), query.getStatus(),
                query.getStartDueDate(), query.getEndDueDate());

        Page<ReceivableVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        java.util.List<ReceivableVO> list = new java.util.ArrayList<>();
        for (Receivable r : result.getRecords()) {
            list.add(convertToVO(r));
        }
        voPage.setRecords(list);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPayment(Long receivableId, Long amount) {
        log.info("确认收款，应收ID：{}，金额：{}", receivableId, amount);

        Receivable r = this.getById(receivableId);
        if (r == null) {
            throw new BusinessException("应收账款不存在");
        }

        if (amount > r.getBalanceAmount()) {
            throw new BusinessException("收款金额不能大于余额");
        }

        long newReceived = r.getReceivedAmount() + amount;
        long newBalance = r.getBalanceAmount() - amount;

        r.setReceivedAmount(newReceived);
        r.setBalanceAmount(newBalance);
        r.setLastPaymentDate(LocalDate.now());

        // 判断是否全部收回（状态：1未收 / 2部分收 / 3已核销 / 4逾期）
        if (newBalance <= 0) {
            r.setStatus(3); // 已核销
        } else {
            r.setStatus(2); // 部分收
        }

        return this.updateById(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean writeOff(Long receivableId) {
        Receivable r = this.getById(receivableId);
        if (r == null) {
            throw new BusinessException("应收账款不存在");
        }
        r.setStatus(3); // 已核销
        return this.updateById(r);
    }

    /**
     * 为销售订单创建应收账款（T-039 联动）
     *
     * <p>幂等性：通过确定性的 receivableNo（"AR" + 0填充 orderId）实现。
     * 同一订单重复调用返回已有记录，不产生重复数据。</p>
     *
     * <p>注：数据库暂停期间（ADR-006）不使用 source_type/source_id 字段，
     * 待数据库恢复后可迁移至标准的 source 字段幂等方案。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceivableVO createForOrder(Long orderId, Long customerId, String customerName,
                                       Long amount, Date orderDate) {
        log.info("为销售订单创建应收账款，订单ID：{}，客户：{}，金额：{}分",
                orderId, customerName, amount);

        // 1. 幂等性检查：通过确定性 receivableNo 判断是否已存在
        String receivableNo = generateOrderReceivableNo(orderId);
        Receivable existing = this.lambdaQuery()
                .eq(Receivable::getReceivableNo, receivableNo)
                .one();
        if (existing != null) {
            log.info("应收账款已存在（幂等跳过），订单ID：{}，应收编号：{}", orderId, receivableNo);
            return convertToVO(existing);
        }

        // 2. 计算到期日（订单日 + 30天）
        LocalDate dueDate;
        if (orderDate != null) {
            dueDate = orderDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(30);
        } else {
            dueDate = LocalDate.now().plusDays(30);
        }

        // 3. 构建并保存应收账款
        Receivable receivable = new Receivable();
        receivable.setReceivableNo(receivableNo);
        receivable.setCustomerType(1); // 普通客户
        receivable.setCustomerId(customerId);
        receivable.setCustomerName(customerName != null ? customerName : "客户" + customerId);
        receivable.setOrderId(orderId);
        receivable.setOriginalAmount(amount);
        receivable.setReceivedAmount(0L);
        receivable.setBalanceAmount(amount);
        receivable.setOverdueDays(0);
        receivable.setStatus(1); // 正常
        receivable.setDueDate(dueDate);
        receivable.setRemark("销售订单出库自动生成");

        this.save(receivable);
        log.info("销售订单应收账款创建成功，订单ID：{}，应收编号：{}", orderId, receivableNo);

        return convertToVO(receivable);
    }

    /**
     * 为核心订单（OrderNew，字符串订单号）创建应收账款（F4）
     * 按订单编号（order_code）幂等；应收编号 = "AR" + orderNo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceivableVO createForOrderByNo(String orderNo, Long customerId, String customerName,
                                           Long amount, Date orderDate) {
        log.info("为核心订单创建应收账款，订单号：{}，客户：{}，金额：{}分",
                orderNo, customerName, amount);

        // 1. 幂等性检查：通过确定性 receivableNo（"AR"+orderNo）判断是否已存在
        String receivableNo = "AR" + (orderNo != null ? orderNo : String.valueOf(System.currentTimeMillis()));
        Receivable existing = this.lambdaQuery()
                .eq(Receivable::getReceivableNo, receivableNo)
                .one();
        if (existing != null) {
            log.info("应收账款已存在（幂等跳过），订单号：{}，应收编号：{}", orderNo, receivableNo);
            return convertToVO(existing);
        }

        // 2. 计算到期日（订单日 + 30天）
        LocalDate dueDate;
        if (orderDate != null) {
            dueDate = orderDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(30);
        } else {
            dueDate = LocalDate.now().plusDays(30);
        }

        // 3. 构建并保存应收账款
        Receivable receivable = new Receivable();
        receivable.setReceivableNo(receivableNo);
        receivable.setCustomerType(1); // 普通客户
        receivable.setCustomerId(customerId);
        receivable.setCustomerName(customerName != null ? customerName : "散客");
        receivable.setOrderId(null);
        receivable.setOriginalAmount(amount);
        receivable.setReceivedAmount(0L);
        receivable.setBalanceAmount(amount);
        receivable.setOverdueDays(0);
        receivable.setStatus(1); // 未收
        receivable.setDueDate(dueDate);
        receivable.setRemark("订单未付清自动生成应收，订单号：" + orderNo);

        this.save(receivable);
        log.info("核心订单应收账款创建成功，订单号：{}，应收编号：{}", orderNo, receivableNo);

        return convertToVO(receivable);
    }

    /**
     * 生成销售订单应收编号（确定性，幂等键）
     * 格式：AR + 0填充至10位的 orderId
     * 示例：orderId=123 → "AR0000000123"
     */
    private String generateOrderReceivableNo(Long orderId) {
        return "AR" + String.format("%010d", orderId);    }

    private String generateReceivableNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Receivable>();
        wrapper.likeRight(Receivable::getReceivableNo, "YS" + datePart)
               .orderByDesc(Receivable::getReceivableId).last("LIMIT 1");
        Receivable last = this.getOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getReceivableNo() != null) {
            seq = Integer.parseInt(last.getReceivableNo().substring(last.getReceivableNo().length() - 4)) + 1;
        }
        return String.format("YS%s%04d", datePart, seq);
    }

    private ReceivableVO convertToVO(Receivable r) {
        ReceivableVO vo = new ReceivableVO();
        vo.setReceivableId(r.getReceivableId());
        vo.setReceivableNo(r.getReceivableNo());
        vo.setCustomerType(r.getCustomerType());
        vo.setCustomerId(r.getCustomerId());
        vo.setCustomerName(r.getCustomerName());
        vo.setOrderId(r.getOrderId());
        vo.setOriginalAmount(r.getOriginalAmount());
        vo.setOriginalAmountDisplay(formatAmount(r.getOriginalAmount()));
        vo.setReceivedAmount(r.getReceivedAmount());
        vo.setReceivedAmountDisplay(formatAmount(r.getReceivedAmount()));
        vo.setBalanceAmount(r.getBalanceAmount());
        vo.setBalanceAmountDisplay(formatAmount(r.getBalanceAmount()));
        vo.setOverdueDays(r.getOverdueDays());

        // 自动逾期联动：未核销且已过到期日 → 显示为逾期(4)（与应付方案一致）
        Integer status = r.getStatus();
        if (status != null && status != 3
                && r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now())
                && r.getBalanceAmount() != null && r.getBalanceAmount() > 0) {
            status = 4;
        }
        vo.setStatus(status);
        vo.setStatusName(getStatusName(status));
        vo.setDueDate(r.getDueDate());
        vo.setLastPaymentDate(r.getLastPaymentDate());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        // 状态编码统一：1未收 / 2部分收 / 3已核销 / 4逾期（与前端/应付方案一致）
        switch (status) { case 1: return "未收"; case 2: return "部分收"; case 3: return "已核销"; case 4: return "逾期"; default: return "未知"; }
    }

    private String formatAmount(Long amount) {
        if (amount == null) return "0.00";
        return String.valueOf(amount / 100.0);
    }
}
