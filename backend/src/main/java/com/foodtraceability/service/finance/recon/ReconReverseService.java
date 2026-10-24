package com.foodtraceability.service.finance.recon;

import com.foodtraceability.dto.finance.recon.ReverseExecutionVO;

/**
 * 差异 REVERSE 实际执行（P2-B）。
 *
 * <p><b>五项红线</b>：①只增反向流水、原流水置 status=3，禁止物理删除与改历史金额；
 * ②幂等键 [reconReverseOf=<原flow_no>] 写在 remark，重跑返回 400 且不新增；
 * ③余额只走乐观锁 UPDATE（balance = balance + delta, version = version + 1 WHERE version = expected）；
 * ④balance_after 由余额原子更新后回读写入，不手填；⑤全部在单事务内，任一失败整体回滚。</p>
 */
public interface ReconReverseService {

    /** 执行冲正：差异必须为 status=PENDING_P2_REV 且 resolution=REVERSE */
    ReverseExecutionVO executeReverse(Long differenceId);
}
