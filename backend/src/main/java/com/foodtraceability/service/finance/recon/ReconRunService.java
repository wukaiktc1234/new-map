package com.foodtraceability.service.finance.recon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.recon.ReconItemQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconItemVO;
import com.foodtraceability.dto.finance.recon.ReconMatchDTO;
import com.foodtraceability.dto.finance.recon.ReconRunCreateDTO;
import com.foodtraceability.dto.finance.recon.ReconRunCreateResultVO;
import com.foodtraceability.dto.finance.recon.ReconRunQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconRunVO;

/**
 * 对账跑批与勾稽主体服务（P1-B：R1 / R2 / R3 / R5 / R6 / R7）。
 */
public interface ReconRunService {

    /**
     * R1 发起跑批（幂等）。
     *
     * <p>事务边界：run 行在独立事务（REQUIRES_NEW）先落地为 RUNNING；items / differences 在
     * 单个事务（{@code ReconBatchExecutor#execute}）里写入并同步把 run 置 FINALIZED；
     * 中途任何异常 → 该事务整体回滚（不留半套 items/differences）→ 独立事务把 run 置 FAILED。</p>
     */
    ReconRunCreateResultVO run(ReconRunCreateDTO dto);

    /** R2 跑批列表（分页） */
    IPage<ReconRunVO> listRuns(ReconRunQueryDTO query);

    /** R3 跑批详情 */
    ReconRunVO getRun(Long runId);

    /** R5 勾稽主体列表（分页 + runId/side/subjectStatus/keyword 过滤） */
    IPage<ReconItemVO> listItems(ReconItemQueryDTO query);

    /**
     * R6 人工勾稽（1:1 由服务端生成 group key；1:N/N:1 可显式传 groupKey）。
     *
     * <p>校验：item 必须存在且为 INTERNAL 侧；externalRef 对应的 EXTERNAL 主体必须存在且属于同一 run；
     * 两侧当前状态必须是 UNMATCHED 或 MATCHED（已自动/人工勾稽的不允许重复勾稽）。</p>
     */
    ReconItemVO matchItem(Long itemId, ReconMatchDTO dto);

    /** R7 解绑：仅 MANUAL_MATCHED → UNMATCHED，并清空同一 match_group 下所有主体的匹配信息 */
    ReconItemVO unmatchItem(Long itemId);
}
