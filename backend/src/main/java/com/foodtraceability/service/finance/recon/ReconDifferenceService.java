package com.foodtraceability.service.finance.recon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.recon.ReconDifferenceQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconDifferenceVO;
import com.foodtraceability.dto.finance.recon.ReconResolveDTO;

/**
 * 差异池服务（P1-B：R8 / R9 / R10）。
 *
 * <p><b>红线</b>：本服务对 fund_flows / bank_accounts <b>只读</b>；WRITE_OFF 只记录决议、
 * REVERSE 只把差异置为 PENDING_P2_REVERSE，均不生成任何流水（TD-28 留 P2）。</p>
 */
public interface ReconDifferenceService {

    /** R8 差异池列表（分页 + status/type/runId/assigneeId 过滤） */
    IPage<ReconDifferenceVO> listDifferences(ReconDifferenceQueryDTO query);

    /** R9 认领：OPEN → ASSIGNED（assignee_id + assigned_at）；已被他人认领时拒绝 */
    ReconDifferenceVO claim(Long differenceId);

    /**
     * R10 处理：ASSIGNED → CLOSED（WRITE_OFF）/ EXEMPT（EXEMPT）/ PENDING_P2_REVERSE（REVERSE）。
     *
     * <p>remark 必填；整段在一个事务内，失败回滚到认领后的状态（不会出现 CLOSED 但未落库）。</p>
     */
    ReconDifferenceVO resolve(Long differenceId, ReconResolveDTO dto);
}
