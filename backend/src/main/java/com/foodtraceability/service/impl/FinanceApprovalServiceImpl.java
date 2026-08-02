package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.finance.FinanceApprovalVO;
import com.foodtraceability.entity.finance.FinanceApproval;
import com.foodtraceability.entity.finance.InvoiceReimbursement;
import com.foodtraceability.mapper.FinanceApprovalMapper;
import com.foodtraceability.mapper.finance.InvoiceReimbursementMapper;
import com.foodtraceability.service.FinanceApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务审批Service实现
 *
 * <p>历史状态：原为 no-op STUB，Sprint 3.2 P1 F-033 跨层联动阶段补齐真实持久化
 * 与关联查询能力。</p>
 *
 * <p>注意：本类仍标记 @Deprecated（接口层废弃），但 createApproval/queryPageWithReimbursement
 * 已具备真实实现，供 InvoiceReimbursementServiceImpl 联动调用。</p>
 */
@Deprecated
@Service("legacyFinanceApprovalServiceImpl")
public class FinanceApprovalServiceImpl implements FinanceApprovalService {
    private static final Logger log = LoggerFactory.getLogger(FinanceApprovalServiceImpl.class);

    /** 发票报销审批类型常量（与 InvoiceReimbursementServiceImpl 保持一致） */
    private static final String BUSINESS_TYPE_INVOICE_REIMBURSEMENT = "INVOICE_REIMBURSEMENT";

    private final FinanceApprovalMapper financeApprovalMapper;
    private final InvoiceReimbursementMapper invoiceReimbursementMapper;

    public FinanceApprovalServiceImpl(FinanceApprovalMapper financeApprovalMapper,
                                      InvoiceReimbursementMapper invoiceReimbursementMapper) {
        this.financeApprovalMapper = financeApprovalMapper;
        this.invoiceReimbursementMapper = invoiceReimbursementMapper;
    }

    /**
     * 创建财务审批记录（F-033 真实持久化）
     *
     * @param a 审批实体（businessType/businessId/amount/status 等字段已由调用方填充）
     * @return true-插入成功 false-插入失败
     */
    @Override
    public boolean createApproval(FinanceApproval a) {
        if (a == null) {
            log.warn("[FinanceApproval] createApproval 入参为 null");
            return false;
        }
        try {
            int rows = financeApprovalMapper.insert(a);
            if (rows > 0) {
                log.info("[FinanceApproval] 审批记录创建成功，businessType={}，businessId={}",
                        a.getBusinessType(), a.getBusinessId());
                return true;
            }
            log.warn("[FinanceApproval] 审批记录插入返回 0 行，businessType={}，businessId={}",
                    a.getBusinessType(), a.getBusinessId());
            return false;
        } catch (Exception e) {
            log.error("[FinanceApproval] 审批记录创建失败，businessType={}，businessId={}，错误：{}",
                    a.getBusinessType(), a.getBusinessId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateApprovalStatus(Long id, String s, Long aid, String c) {
        log.warn("[STUB] updateApprovalStatus no-op");
        return false;
    }

    @Override
    public List<FinanceApproval> getByBusinessIdAndType(Long bt, String t) {
        return Collections.emptyList();
    }

    @Override
    public List<FinanceApproval> getByApplicantId(Long id) {
        return Collections.emptyList();
    }

    @Override
    public List<FinanceApproval> getPendingApprovalsByApproverId(Long id) {
        return Collections.emptyList();
    }

    @Override
    public FinanceApproval getApprovalDetail(Long id) {
        return null;
    }

    /**
     * 分页查询财务审批列表，关联展示发票报销信息（F-033 跨层联动）
     *
     * <p>实现策略（批量查询避免 N+1）：</p>
     * <ol>
     *   <li>分页查询 FinanceApproval（可按 businessType 过滤）</li>
     *   <li>收集当前页中 businessType=INVOICE_REIMBURSEMENT 的 businessId 列表</li>
     *   <li>一次性批量查询 InvoiceReimbursement（selectBatchIds）</li>
     *   <li>构建 Map&lt;reimbursementId, InvoiceReimbursement&gt;，组装 VO 关联字段</li>
     * </ol>
     *
     * @param current      当前页码（从 1 开始）
     * @param size         每页条数
     * @param businessType 业务类型过滤（可空，查询全部类型）
     * @return 分页结果，每条记录为 FinanceApprovalVO（含报销关联字段）
     */
    @Override
    public IPage<FinanceApprovalVO> queryPageWithReimbursement(int current, int size, String businessType) {
        Page<FinanceApproval> page = new Page<>(current, size);
        LambdaQueryWrapper<FinanceApproval> wrapper = new LambdaQueryWrapper<>();
        if (businessType != null && !businessType.isBlank()) {
            wrapper.eq(FinanceApproval::getBusinessType, businessType);
        }
        wrapper.orderByDesc(FinanceApproval::getCreateTime);

        IPage<FinanceApproval> entityPage = financeApprovalMapper.selectPage(page, wrapper);

        // 批量关联查询：收集 INVOICE_REIMBURSEMENT 类型的 businessId
        List<Long> reimbursementIds = entityPage.getRecords().stream()
                .filter(a -> BUSINESS_TYPE_INVOICE_REIMBURSEMENT.equals(a.getBusinessType()))
                .map(FinanceApproval::getBusinessId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询 InvoiceReimbursement，构建 Map（避免 N+1）
        Map<Long, InvoiceReimbursement> reimbursementMap;
        if (reimbursementIds.isEmpty()) {
            reimbursementMap = Collections.emptyMap();
        } else {
            List<InvoiceReimbursement> reimbursements = invoiceReimbursementMapper.selectBatchIds(reimbursementIds);
            reimbursementMap = reimbursements.stream()
                    .collect(Collectors.toMap(
                            InvoiceReimbursement::getReimbursementId,
                            r -> r,
                            (a, b) -> a));
        }

        // 组装 VO
        List<FinanceApprovalVO> voRecords = new ArrayList<>();
        for (FinanceApproval entity : entityPage.getRecords()) {
            FinanceApprovalVO vo = convertToVO(entity);
            // 填充报销关联字段
            if (BUSINESS_TYPE_INVOICE_REIMBURSEMENT.equals(entity.getBusinessType())
                    && entity.getBusinessId() != null) {
                InvoiceReimbursement reimbursement = reimbursementMap.get(entity.getBusinessId());
                if (reimbursement != null) {
                    vo.setReimbursementNo(reimbursement.getReimbursementNo());
                    vo.setTotalAmount(reimbursement.getTotalAmount());
                    vo.setDepartmentName(reimbursement.getDepartmentName());
                }
            }
            voRecords.add(vo);
        }

        Page<FinanceApprovalVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    /** FinanceApproval 实体转 VO */
    private FinanceApprovalVO convertToVO(FinanceApproval entity) {
        FinanceApprovalVO vo = new FinanceApprovalVO();
        vo.setApprovalId(entity.getApprovalId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setApplicantId(entity.getApplicantId());
        vo.setApplicantName(entity.getApplicantName());
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(entity.getApproverName());
        vo.setApprovalComment(entity.getApprovalComment());
        vo.setApplyTime(entity.getApplyTime());
        vo.setApproveTime(entity.getApproveTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
