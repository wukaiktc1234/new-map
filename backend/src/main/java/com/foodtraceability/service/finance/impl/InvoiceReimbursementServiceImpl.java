package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.finance.BudgetVO;
import com.foodtraceability.dto.finance.InvoiceReimbursementApproveDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementPayDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementStatsVO;
import com.foodtraceability.dto.finance.InvoiceReimbursementUpdateDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementVO;
import com.foodtraceability.dto.finance.ReimbursementCreateDTO;
import com.foodtraceability.dto.finance.ReimbursementItemDTO;
import com.foodtraceability.dto.finance.ReimbursementQueryDTO;
import com.foodtraceability.entity.finance.FinanceApproval;
import com.foodtraceability.entity.finance.InvoiceReimbursement;
import com.foodtraceability.entity.finance.InvoiceReimbursementApprovalRecord;
import com.foodtraceability.entity.finance.InvoiceReimbursementItem;
import com.foodtraceability.event.InvoiceReimbursementApprovedEvent;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.InvoiceReimbursementApprovalRecordMapper;
import com.foodtraceability.mapper.finance.InvoiceReimbursementItemMapper;
import com.foodtraceability.mapper.finance.InvoiceReimbursementMapper;
import com.foodtraceability.service.FinanceApprovalService;
import com.foodtraceability.service.finance.InvoiceReimbursementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 发票报销Service实现
 *
 * <p>Sprint 3.1 P0 F-001 / T-015：报销单 CRUD + 审批 + 付款 + 状态机管理。</p>
 *
 * <p>状态机（5 状态，spec/后端/前端三方统一）：</p>
 * <ul>
 *   <li>0(草稿) → 1(已审批)：approve(approved=true)</li>
 *   <li>0(草稿) → 4(已拒绝)：approve(approved=false)</li>
 *   <li>0(草稿) → 3(已取消)：cancel()</li>
 *   <li>1(已审批) → 2(已付款)：pay()</li>
 *   <li>1(已审批) → 3(已取消)：cancel()</li>
 *   <li>3(已取消) / 4(已拒绝) 为终态</li>
 * </ul>
 *
 * <p>所有写操作加 {@code @Transactional(rollbackFor = Exception.class)}；
 * 构造函数注入三个 Mapper；金额字段 Long（分），无 BigDecimal。</p>
 */
@Service
public class InvoiceReimbursementServiceImpl implements InvoiceReimbursementService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReimbursementServiceImpl.class);

    /** 状态常量（与 spec/前端 Converter 三方统一） */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_CANCELLED = 3;
    private static final int STATUS_REJECTED = 4;

    /** 付款状态常量 */
    private static final int PAYMENT_UNPAID = 0;
    private static final int PAYMENT_PAID = 1;

    /** 审批动作常量 */
    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_PAY = "pay";

    /** 期间格式（yyyy-MM，用于统计周期与预算展示） */
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 发票报销审批类型常量（用于关联 FinanceApproval） */
    private static final String APPROVAL_BUSINESS_TYPE_INVOICE_REIMBURSEMENT = "INVOICE_REIMBURSEMENT";

    private final InvoiceReimbursementMapper reimbursementMapper;
    private final InvoiceReimbursementItemMapper itemMapper;
    private final InvoiceReimbursementApprovalRecordMapper approvalRecordMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final FinanceApprovalService financeApprovalService;

    public InvoiceReimbursementServiceImpl(InvoiceReimbursementMapper reimbursementMapper,
                                           InvoiceReimbursementItemMapper itemMapper,
                                           InvoiceReimbursementApprovalRecordMapper approvalRecordMapper,
                                           ApplicationEventPublisher eventPublisher,
                                           FinanceApprovalService financeApprovalService) {
        this.reimbursementMapper = reimbursementMapper;
        this.itemMapper = itemMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.eventPublisher = eventPublisher;
        this.financeApprovalService = financeApprovalService;
    }

    // ============================================================
    // 1. create()
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceReimbursementVO create(ReimbursementCreateDTO dto) {
        log.info("创建报销申请，申请人：{}，金额：{}", dto.getApplicantName(), dto.getTotalAmount());

        // 1. 业务校验
        validateCreateDTO(dto);

        // 2. 构造主表实体
        InvoiceReimbursement entity = new InvoiceReimbursement();
        entity.setReimbursementNo(generateReimbursementNo());
        entity.setApplicantId(dto.getApplicantId());
        entity.setApplicantName(dto.getApplicantName());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setDepartmentName(dto.getDepartmentName());
        entity.setReimbursementType(dto.getReimbursementType());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setApprovedAmount(0L);
        entity.setStatus(STATUS_DRAFT);
        entity.setPaymentStatus(PAYMENT_UNPAID);
        entity.setApplyDate(dto.getApplyDate());
        entity.setRemark(dto.getRemark());

        reimbursementMapper.insert(entity);

        // 3. 保存明细
        List<InvoiceReimbursementItem> items = new ArrayList<>();
        for (ReimbursementItemDTO itemDTO : dto.getItems()) {
            InvoiceReimbursementItem item = new InvoiceReimbursementItem();
            item.setReimbursementId(entity.getReimbursementId());
            item.setRelatedInvoiceId(itemDTO.getRelatedInvoiceId());
            item.setVoucherId(itemDTO.getVoucherId());
            item.setVoucherNo(itemDTO.getVoucherNo());
            item.setVoucherType(itemDTO.getVoucherType());
            item.setAmount(itemDTO.getAmount());
            item.setApprovedAmount(0L);
            item.setExpenseType(itemDTO.getExpenseType());
            item.setExpenseDescription(itemDTO.getExpenseDescription());
            items.add(item);
            itemMapper.insert(item);
        }

        // 4. 写入 submit 审批记录
        saveApprovalRecord(entity.getReimbursementId(), dto.getApplicantId(), dto.getApplicantName(),
                ACTION_SUBMIT, "提交报销申请");

        log.info("报销申请创建成功，单号：{}", entity.getReimbursementNo());
        return getById(entity.getReimbursementId());
    }

    // ============================================================
    // 2. queryPage()
    // ============================================================
    @Override
    public IPage<InvoiceReimbursementVO> queryPage(ReimbursementQueryDTO query) {
        Page<InvoiceReimbursement> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<InvoiceReimbursement> wrapper = new LambdaQueryWrapper<>();

        if (query.getReimbursementNo() != null && !query.getReimbursementNo().isBlank()) {
            wrapper.like(InvoiceReimbursement::getReimbursementNo, query.getReimbursementNo());
        }
        if (query.getApplicantName() != null && !query.getApplicantName().isBlank()) {
            wrapper.like(InvoiceReimbursement::getApplicantName, query.getApplicantName());
        }
        if (query.getApplicantId() != null) {
            wrapper.eq(InvoiceReimbursement::getApplicantId, query.getApplicantId());
        }
        if (query.getDepartmentId() != null) {
            wrapper.eq(InvoiceReimbursement::getDepartmentId, query.getDepartmentId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(InvoiceReimbursement::getStatus, query.getStatus());
        }
        if (query.getPaymentStatus() != null) {
            wrapper.eq(InvoiceReimbursement::getPaymentStatus, query.getPaymentStatus());
        }
        if (query.getReimbursementType() != null && !query.getReimbursementType().isBlank()) {
            wrapper.eq(InvoiceReimbursement::getReimbursementType, query.getReimbursementType());
        }
        if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
            wrapper.ge(InvoiceReimbursement::getApplyDate, LocalDate.parse(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
            wrapper.le(InvoiceReimbursement::getApplyDate, LocalDate.parse(query.getEndDate()));
        }
        wrapper.orderByDesc(InvoiceReimbursement::getCreateTime);

        IPage<InvoiceReimbursement> entityPage = reimbursementMapper.selectPage(page, wrapper);

        Page<InvoiceReimbursementVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<InvoiceReimbursementVO> records = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    // ============================================================
    // 3. getById()
    // ============================================================
    @Override
    public InvoiceReimbursementVO getById(Long reimbursementId) {
        InvoiceReimbursement entity = reimbursementMapper.selectById(reimbursementId);
        if (entity == null) {
            throw new BusinessException("报销单不存在：" + reimbursementId);
        }

        InvoiceReimbursementVO vo = convertToVO(entity);

        // 查询明细
        LambdaQueryWrapper<InvoiceReimbursementItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InvoiceReimbursementItem::getReimbursementId, reimbursementId)
                   .orderByAsc(InvoiceReimbursementItem::getItemId);
        List<InvoiceReimbursementItem> items = itemMapper.selectList(itemWrapper);
        List<InvoiceReimbursementVO.InvoiceReimbursementItemVO> itemVOs = items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        // 查询审批记录
        LambdaQueryWrapper<InvoiceReimbursementApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(InvoiceReimbursementApprovalRecord::getReimbursementId, reimbursementId)
                     .orderByAsc(InvoiceReimbursementApprovalRecord::getOperateTime);
        List<InvoiceReimbursementApprovalRecord> records = approvalRecordMapper.selectList(recordWrapper);
        List<InvoiceReimbursementVO.InvoiceReimbursementApprovalRecordVO> recordVOs = records.stream()
                .map(this::convertRecordToVO)
                .collect(Collectors.toList());
        vo.setApprovalRecords(recordVOs);

        return vo;
    }

    // ============================================================
    // 4. update()
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceReimbursementVO update(InvoiceReimbursementUpdateDTO dto) {
        log.info("更新报销申请，ID：{}", dto.getReimbursementId());

        InvoiceReimbursement existing = reimbursementMapper.selectById(dto.getReimbursementId());
        if (existing == null) {
            throw new BusinessException("报销单不存在：" + dto.getReimbursementId());
        }
        if (existing.getStatus() != null && existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException("只有草稿状态的报销单可以修改");
        }

        // 更新主表字段
        existing.setApplicantId(dto.getApplicantId() != null ? dto.getApplicantId() : existing.getApplicantId());
        existing.setApplicantName(dto.getApplicantName() != null ? dto.getApplicantName() : existing.getApplicantName());
        existing.setDepartmentId(dto.getDepartmentId() != null ? dto.getDepartmentId() : existing.getDepartmentId());
        existing.setDepartmentName(dto.getDepartmentName() != null ? dto.getDepartmentName() : existing.getDepartmentName());
        existing.setReimbursementType(dto.getReimbursementType());
        existing.setTotalAmount(dto.getTotalAmount());
        existing.setApplyDate(dto.getApplyDate());
        existing.setRemark(dto.getRemark());
        reimbursementMapper.updateById(existing);

        // 重新生成明细：先删除旧明细，再插入新明细
        LambdaQueryWrapper<InvoiceReimbursementItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(InvoiceReimbursementItem::getReimbursementId, dto.getReimbursementId());
        itemMapper.delete(deleteWrapper);

        for (ReimbursementItemDTO itemDTO : dto.getItems()) {
            InvoiceReimbursementItem item = new InvoiceReimbursementItem();
            item.setReimbursementId(dto.getReimbursementId());
            item.setRelatedInvoiceId(itemDTO.getRelatedInvoiceId());
            item.setVoucherId(itemDTO.getVoucherId());
            item.setVoucherNo(itemDTO.getVoucherNo());
            item.setVoucherType(itemDTO.getVoucherType());
            item.setAmount(itemDTO.getAmount());
            item.setApprovedAmount(0L);
            item.setExpenseType(itemDTO.getExpenseType());
            item.setExpenseDescription(itemDTO.getExpenseDescription());
            itemMapper.insert(item);
        }

        return getById(dto.getReimbursementId());
    }

    // ============================================================
    // 5. approve()
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceReimbursementVO approve(Long reimbursementId, Long approverId, String approverName,
                                          InvoiceReimbursementApproveDTO dto) {
        log.info("审批报销申请，ID：{}，approved：{}", reimbursementId, dto.getApproved());

        InvoiceReimbursement existing = reimbursementMapper.selectById(reimbursementId);
        if (existing == null) {
            throw new BusinessException("报销单不存在：" + reimbursementId);
        }
        if (existing.getStatus() == null || existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException("只有草稿状态的报销单可以审批");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 审批通过
            if (dto.getApprovedAmount() == null || dto.getApprovedAmount() <= 0L) {
                throw new BusinessException("审批通过金额必须大于0");
            }
            existing.setStatus(STATUS_APPROVED);
            existing.setApprovedAmount(dto.getApprovedAmount());
            existing.setApproverId(approverId);
            existing.setApproverName(approverName);
            existing.setApproveDate(LocalDate.now());
            reimbursementMapper.updateById(existing);

            saveApprovalRecord(reimbursementId, approverId, approverName,
                    ACTION_APPROVE, dto.getRemark());

            // T-042: 审批通过后发布 InvoiceReimbursementApprovedEvent 联动凭证生成 + 成本归集
            // try-catch 隔离：发布失败不影响审批主流程（ADR-004 事件隔离策略）
            publishApprovedEvent(existing, dto.getApprovedAmount(), approverId);

            // F-033: 审批通过后联动创建 FinanceApproval 审批记录（跨层联动）
            // try-catch 隔离：审批记录创建失败不影响审批主流程，仅记录日志
            createFinanceApprovalRecord(existing, dto.getApprovedAmount(), approverId, approverName, dto.getRemark());
        } else {
            // 审批拒绝 → 已拒绝(4)（与"取消(3)"区分；rejected 为终态，不可再审批/付款）
            existing.setStatus(STATUS_REJECTED);
            existing.setApproverId(approverId);
            existing.setApproverName(approverName);
            existing.setApproveDate(LocalDate.now());
            existing.setRejectReason(dto.getRemark());
            reimbursementMapper.updateById(existing);

            saveApprovalRecord(reimbursementId, approverId, approverName,
                    ACTION_REJECT, dto.getRemark());
        }

        return getById(reimbursementId);
    }

    // ============================================================
    // 6. cancel()
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceReimbursementVO cancel(Long reimbursementId, String cancelReason) {
        log.info("取消报销申请，ID：{}", reimbursementId);

        InvoiceReimbursement existing = reimbursementMapper.selectById(reimbursementId);
        if (existing == null) {
            throw new BusinessException("报销单不存在：" + reimbursementId);
        }
        // 状态机：草稿(0) / 已审批(1) → 已取消(3)；已付款(2)/已取消(3)/已拒绝(4) 为终态不可取消
        Integer currentStatus = existing.getStatus();
        if (currentStatus == null
                || (currentStatus != STATUS_DRAFT && currentStatus != STATUS_APPROVED)) {
            throw new BusinessException("只有草稿或已审批状态的报销单可以取消");
        }

        existing.setStatus(STATUS_CANCELLED);
        existing.setRejectReason(cancelReason);
        reimbursementMapper.updateById(existing);

        saveApprovalRecord(reimbursementId, existing.getApplicantId(), existing.getApplicantName(),
                ACTION_CANCEL, cancelReason);

        return getById(reimbursementId);
    }

    // ============================================================
    // 7. pay()
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceReimbursementVO pay(Long reimbursementId, InvoiceReimbursementPayDTO dto) {
        log.info("标记报销单已付款，ID：{}", reimbursementId);

        InvoiceReimbursement existing = reimbursementMapper.selectById(reimbursementId);
        if (existing == null) {
            throw new BusinessException("报销单不存在：" + reimbursementId);
        }
        if (existing.getStatus() == null || existing.getStatus() != STATUS_APPROVED) {
            throw new BusinessException("只有已审批状态的报销单可以付款");
        }
        // 重复付款保护（兼容旧数据：历史双状态时期 paymentStatus=1 即已付款）
        if (existing.getStatus() == STATUS_PAID
                || (existing.getPaymentStatus() != null && existing.getPaymentStatus() == PAYMENT_PAID)) {
            throw new BusinessException("报销单已付款，不能重复付款");
        }

        // F2：单一状态源——不再双写 payment_status，status=2(已付款) 即唯一表达
        existing.setStatus(STATUS_PAID);
        existing.setPaymentDate(dto.getPaymentDate());
        existing.setPaymentVoucherNo(dto.getPaymentVoucherNo());
        reimbursementMapper.updateById(existing);

        saveApprovalRecord(reimbursementId, null, null,
                ACTION_PAY, "付款凭证号：" + dto.getPaymentVoucherNo());

        return getById(reimbursementId);
    }

    // ============================================================
    // 8. getStats() — F-005 报销统计真实聚合
    //
    // 返回 InvoiceReimbursementStatsVO（报销专用统计 VO）：
    //   - 数量字段：按状态分类统计报销单数量（pending/approved/paid/cancelled/rejected/total）
    //   - 金额字段：totalAmount（有效单据）/ approvedAmount（已审批+已付款）/ paidAmount（已付款）
    //   - period：统计周期（yyyy-MM），由 query.startDate 推断，默认当月
    // ============================================================
    @Override
    public InvoiceReimbursementStatsVO getStats(ReimbursementQueryDTO query) {
        YearMonth periodMonth = resolveStatsPeriod(query);
        log.info("报销统计，周期：{}", periodMonth);

        LocalDate start = periodMonth.atDay(1);
        LocalDate end = periodMonth.atEndOfMonth();

        // 拉取周期内所有报销单
        LambdaQueryWrapper<InvoiceReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InvoiceReimbursement::getApplyDate, start)
               .le(InvoiceReimbursement::getApplyDate, end);
        if (query != null) {
            if (query.getDepartmentId() != null) {
                wrapper.eq(InvoiceReimbursement::getDepartmentId, query.getDepartmentId());
            }
            if (query.getReimbursementType() != null && !query.getReimbursementType().isBlank()) {
                wrapper.eq(InvoiceReimbursement::getReimbursementType, query.getReimbursementType());
            }
        }
        List<InvoiceReimbursement> list = reimbursementMapper.selectList(wrapper);

        // 按状态分组计数
        long pending = 0L;      // 0-草稿
        long approved = 0L;     // 1-已审批
        long paid = 0L;         // 2-已付款
        long cancelled = 0L;    // 3-已取消
        long rejected = 0L;     // 4-已拒绝
        long totalAmountSum = 0L;     // 有效单据 totalAmount 之和（不含取消/拒绝）
        long approvedAmountSum = 0L;  // 已审批+已付款 approvedAmount 之和
        long paidAmountSum = 0L;      // 已付款 approvedAmount 之和

        for (InvoiceReimbursement r : list) {
            Integer status = r.getStatus();
            if (status == null) continue;
            switch (status) {
                case STATUS_DRAFT:
                    pending++;
                    break;
                case STATUS_APPROVED:
                    approved++;
                    break;
                case STATUS_PAID:
                    paid++;
                    break;
                case STATUS_CANCELLED:
                    cancelled++;
                    break;
                case STATUS_REJECTED:
                    rejected++;
                    break;
                default:
                    break;
            }
            // 金额聚合：取消/拒绝不计入 totalAmount
            if (status != STATUS_CANCELLED && status != STATUS_REJECTED) {
                totalAmountSum += r.getTotalAmount() != null ? r.getTotalAmount() : 0L;
            }
            if (status == STATUS_APPROVED || status == STATUS_PAID) {
                approvedAmountSum += r.getApprovedAmount() != null ? r.getApprovedAmount() : 0L;
            }
            if (status == STATUS_PAID) {
                paidAmountSum += r.getApprovedAmount() != null ? r.getApprovedAmount() : 0L;
            }
        }

        InvoiceReimbursementStatsVO vo = new InvoiceReimbursementStatsVO();
        vo.setTotalCount((long) list.size());
        vo.setPendingCount(pending);
        vo.setApprovedCount(approved);
        vo.setPaidCount(paid);
        vo.setCancelledCount(cancelled);
        vo.setRejectedCount(rejected);
        vo.setTotalAmount(totalAmountSum);
        vo.setApprovedAmount(approvedAmountSum);
        vo.setPaidAmount(paidAmountSum);
        vo.setPeriod(periodMonth.format(PERIOD_FORMATTER));
        return vo;
    }

    // ============================================================
    // 9. getBudgetInfo() — F-006 报销预算执行情况
    //
    // 实现策略：基于报销单聚合生成简化预算视图（无独立 BudgetService 调用）：
    //   - 按 reimbursementType 分组
    //   - budgetAmount = 该类型年度有效报销总额 × 1.2（估算预算上限）
    //   - actualAmount = 该类型年度已审批金额之和（APPROVED + PAID）
    //   - variance = budgetAmount - actualAmount
    //   - executionRate = actualAmount / budgetAmount × 100（保留 2 位小数）
    // ============================================================
    @Override
    public List<BudgetVO> getBudgetInfo(Long departmentId, Integer budgetYear) {
        int year = budgetYear != null ? budgetYear : LocalDate.now().getYear();
        log.info("查询报销预算信息，部门ID：{}，年度：{}", departmentId, year);

        LambdaQueryWrapper<InvoiceReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(InvoiceReimbursement::getApplyDate, LocalDate.of(year, 1, 1))
               .le(InvoiceReimbursement::getApplyDate, LocalDate.of(year, 12, 31));
        if (departmentId != null) {
            wrapper.eq(InvoiceReimbursement::getDepartmentId, departmentId);
        }
        List<InvoiceReimbursement> list = reimbursementMapper.selectList(wrapper);

        // 按 reimbursementType 分组：有效报销总额（不含取消/拒绝）
        Map<String, Long> totalByType = list.stream()
                .filter(r -> r.getReimbursementType() != null)
                .filter(r -> isValidStatus(r.getStatus()))
                .collect(Collectors.groupingBy(
                        InvoiceReimbursement::getReimbursementType,
                        Collectors.summingLong(r ->
                                r.getTotalAmount() != null ? r.getTotalAmount() : 0L)));

        // 按 reimbursementType 分组：已审批金额之和（APPROVED + PAID）
        Map<String, Long> approvedByType = list.stream()
                .filter(r -> r.getReimbursementType() != null)
                .filter(r -> r.getStatus() != null
                        && (r.getStatus() == STATUS_APPROVED || r.getStatus() == STATUS_PAID))
                .collect(Collectors.groupingBy(
                        InvoiceReimbursement::getReimbursementType,
                        Collectors.summingLong(r ->
                                r.getApprovedAmount() != null ? r.getApprovedAmount() : 0L)));

        List<BudgetVO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : totalByType.entrySet()) {
            String type = entry.getKey();
            long total = entry.getValue();
            long approved = approvedByType.getOrDefault(type, 0L);
            // 估算预算：年度有效报销总额 × 1.2（无独立预算表时的简化策略）
            long budgetAmount = (long) Math.ceil(total * 1.2);
            long variance = budgetAmount - approved;
            BigDecimal executionRate = budgetAmount == 0L ? null
                    : BigDecimal.valueOf(approved * 100.0 / budgetAmount)
                            .setScale(2, RoundingMode.HALF_UP);

            BudgetVO vo = new BudgetVO();
            vo.setBudgetYear(year);
            vo.setBudgetType(2); // 2-支出预算
            vo.setBudgetTypeName("报销预算");
            vo.setCategoryName(type);
            vo.setBudgetAmount(budgetAmount);
            vo.setBudgetAmountDisplay(formatAmountDisplay(budgetAmount));
            vo.setActualAmount(approved);
            vo.setActualAmountDisplay(formatAmountDisplay(approved));
            vo.setVariance(variance);
            vo.setVarianceDisplay(formatAmountDisplay(variance));
            vo.setExecutionRate(executionRate);
            vo.setResponsibleDeptId(departmentId);
            vo.setRemark("基于报销单聚合的预算估算（无独立预算数据）");
            result.add(vo);
        }
        return result;
    }

    // ============================================================
    // 报销单号生成（protected 便于测试 spy）
    // ============================================================
    protected String generateReimbursementNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<InvoiceReimbursement> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(InvoiceReimbursement::getReimbursementNo, "RE" + datePart)
               .orderByDesc(InvoiceReimbursement::getReimbursementId)
               .last("LIMIT 1");
        InvoiceReimbursement last = reimbursementMapper.selectOne(wrapper, false);

        int seq = 1;
        if (last != null && last.getReimbursementNo() != null) {
            String lastNo = last.getReimbursementNo();
            String seqStr = lastNo.substring(lastNo.length() - 4);
            seq = Integer.parseInt(seqStr) + 1;
        }
        return String.format("RE%s%04d", datePart, seq);
    }

    // ============================================================
    // 私有辅助方法
    // ============================================================

    /**
     * 解析报销统计周期：优先 startDate 推断，其次默认当月
     *
     * @param query 查询DTO（含 startDate/endDate）
     * @return 统计周期所在年月
     */
    private YearMonth resolveStatsPeriod(ReimbursementQueryDTO query) {
        if (query != null && query.getStartDate() != null && !query.getStartDate().isBlank()) {
            try {
                return YearMonth.from(LocalDate.parse(query.getStartDate()));
            } catch (Exception e) {
                log.warn("startDate 格式错误：{}，回退到当月", query.getStartDate());
            }
        }
        return YearMonth.now();
    }

    /** 判断状态是否为有效状态（非取消、非拒绝） */
    private boolean isValidStatus(Integer status) {
        return status != null
                && status != STATUS_CANCELLED
                && status != STATUS_REJECTED;
    }

    /**
     * 金额（分）格式化为元展示字符串（保留 2 位小数）
     */
    private String formatAmountDisplay(Long amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount / 100.0);
    }

    /** 创建DTO业务校验 */
    private void validateCreateDTO(ReimbursementCreateDTO dto) {
        if (dto.getTotalAmount() == null || dto.getTotalAmount() <= 0L) {
            throw new BusinessException("报销金额必须大于0");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("报销明细不能为空");
        }
    }

    /** 保存审批记录 */
    private void saveApprovalRecord(Long reimbursementId, Long operatorId, String operatorName,
                                    String action, String remark) {
        InvoiceReimbursementApprovalRecord record = new InvoiceReimbursementApprovalRecord();
        record.setReimbursementId(reimbursementId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(action);
        record.setRemark(remark);
        record.setOperateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }

    /**
     * 发布发票报销审批通过事件（T-042）
     *
     * <p>try-catch 隔离：事件发布失败不影响审批主流程，仅记录错误日志。
     * 监听器使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 确保
     * 审批事务提交后才消费事件。</p>
     *
     * @param entity          报销单实体（含 reimbursementId、reimbursementNo、applicantId、departmentId 等字段）
     * @param approvedAmount  审批通过金额（分）
     * @param approverId      审批人ID
     */
    private void publishApprovedEvent(InvoiceReimbursement entity, Long approvedAmount, Long approverId) {
        try {
            InvoiceReimbursementApprovedEvent event = new InvoiceReimbursementApprovedEvent(
                    this,
                    entity.getReimbursementId(),
                    entity.getReimbursementNo(),
                    entity.getApplicantId(),
                    entity.getDepartmentId(),
                    entity.getDepartmentName(),
                    entity.getReimbursementType(),
                    approvedAmount,
                    approverId,
                    LocalDateTime.now()
            );
            eventPublisher.publishEvent(event);
            log.info("发票报销审批通过事件已发布，报销单ID：{}，单号：{}",
                    entity.getReimbursementId(), entity.getReimbursementNo());
        } catch (Exception e) {
            log.error("发布发票报销审批通过事件失败，报销单ID：{}，错误：{}",
                    entity.getReimbursementId(), e.getMessage(), e);
            // 异常隔离：不重抛，避免影响审批主流程
        }
    }

    /**
     * 联动创建财务审批记录（F-033 跨层联动）
     *
     * <p>发票报销审批通过后，自动在 FinanceApproval 中创建一条审批记录，
     * businessType=INVOICE_REIMBURSEMENT，businessId=reimbursementId，
     * 便于财务审批列表统一展示。</p>
     *
     * <p>try-catch 隔离：审批记录创建失败不影响审批主流程，仅记录错误日志。
     * 金额转换：报销金额 Long（分）→ BigDecimal（元），与 FinanceApproval 实体字段类型对齐。</p>
     *
     * @param entity          报销单实体（含 reimbursementId、reimbursementNo、applicantId 等字段）
     * @param approvedAmount  审批通过金额（分）
     * @param approverId      审批人ID
     * @param approverName    审批人姓名
     * @param remark          审批备注
     */
    private void createFinanceApprovalRecord(InvoiceReimbursement entity, Long approvedAmount,
                                             Long approverId, String approverName, String remark) {
        try {
            FinanceApproval approval = new FinanceApproval();
            approval.setBusinessType(APPROVAL_BUSINESS_TYPE_INVOICE_REIMBURSEMENT);
            approval.setBusinessId(entity.getReimbursementId());
            // 金额转换：分 → 元（FinanceApproval.amount 为 BigDecimal 类型）
            approval.setAmount(BigDecimal.valueOf(approvedAmount).movePointLeft(2));
            approval.setStatus("APPROVED");
            approval.setTitle("发票报销审批-" + entity.getReimbursementNo());
            approval.setContent(remark);
            approval.setApplicantId(entity.getApplicantId());
            approval.setApplicantName(entity.getApplicantName());
            approval.setApproverId(approverId);
            approval.setApproverName(approverName);
            approval.setApprovalComment(remark);
            approval.setApplyTime(new Date());
            approval.setApproveTime(new Date());

            boolean created = financeApprovalService.createApproval(approval);
            if (created) {
                log.info("财务审批记录已联动创建，报销单ID：{}，单号：{}",
                        entity.getReimbursementId(), entity.getReimbursementNo());
            } else {
                log.warn("财务审批记录联动创建返回 false，报销单ID：{}", entity.getReimbursementId());
            }
        } catch (Exception e) {
            log.error("联动创建财务审批记录失败，报销单ID：{}，错误：{}",
                    entity.getReimbursementId(), e.getMessage(), e);
            // 异常隔离：不重抛，避免影响审批主流程
        }
    }

    /** 主实体转VO */
    private InvoiceReimbursementVO convertToVO(InvoiceReimbursement entity) {
        InvoiceReimbursementVO vo = new InvoiceReimbursementVO();
        vo.setReimbursementId(entity.getReimbursementId());
        vo.setReimbursementNo(entity.getReimbursementNo());
        vo.setApplicantId(entity.getApplicantId());
        vo.setApplicantName(entity.getApplicantName());
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setDepartmentName(entity.getDepartmentName());
        vo.setReimbursementType(entity.getReimbursementType());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setApprovedAmount(entity.getApprovedAmount());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusName(entity.getStatus()));
        vo.setApplyDate(entity.getApplyDate());
        vo.setApproveDate(entity.getApproveDate());
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(entity.getApproverName());
        vo.setRejectReason(entity.getRejectReason());
        vo.setRemark(entity.getRemark());
        vo.setPaymentStatus(entity.getPaymentStatus());
        vo.setPaymentStatusName(getPaymentStatusName(entity.getPaymentStatus()));
        vo.setPaymentDate(entity.getPaymentDate());
        vo.setPaymentVoucherNo(entity.getPaymentVoucherNo());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /** 明细分录转VO */
    private InvoiceReimbursementVO.InvoiceReimbursementItemVO convertItemToVO(InvoiceReimbursementItem item) {
        InvoiceReimbursementVO.InvoiceReimbursementItemVO vo = new InvoiceReimbursementVO.InvoiceReimbursementItemVO();
        vo.setItemId(item.getItemId());
        vo.setReimbursementId(item.getReimbursementId());
        vo.setRelatedInvoiceId(item.getRelatedInvoiceId());
        vo.setVoucherId(item.getVoucherId());
        vo.setVoucherNo(item.getVoucherNo());
        vo.setVoucherType(item.getVoucherType());
        vo.setAmount(item.getAmount());
        vo.setApprovedAmount(item.getApprovedAmount());
        vo.setExpenseType(item.getExpenseType());
        vo.setExpenseDescription(item.getExpenseDescription());
        return vo;
    }

    /** 审批记录转VO */
    private InvoiceReimbursementVO.InvoiceReimbursementApprovalRecordVO convertRecordToVO(
            InvoiceReimbursementApprovalRecord record) {
        InvoiceReimbursementVO.InvoiceReimbursementApprovalRecordVO vo =
                new InvoiceReimbursementVO.InvoiceReimbursementApprovalRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setReimbursementId(record.getReimbursementId());
        vo.setOperatorId(record.getOperatorId());
        vo.setOperatorName(record.getOperatorName());
        vo.setAction(record.getAction());
        vo.setRemark(record.getRemark());
        vo.setOperateTime(record.getOperateTime());
        return vo;
    }

    /** 获取单据状态名称（5 状态：0草稿/1已审批/2已付款/3已取消/4已拒绝） */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已审批";
            case 2: return "已付款";
            case 3: return "已取消";
            case 4: return "已拒绝";
            default: return "未知";
        }
    }

    /** 获取付款状态名称 */
    private String getPaymentStatusName(Integer paymentStatus) {
        if (paymentStatus == null) return "未知";
        switch (paymentStatus) {
            case 0: return "未付款";
            case 1: return "已付款";
            default: return "未知";
        }
    }
}
