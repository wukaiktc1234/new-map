package com.foodtraceability.service.finance.ops;

import com.foodtraceability.dto.finance.recon.FixtureReverseRequest;
import com.foodtraceability.dto.finance.recon.FixtureReverseVO;

/**
 * 财务运维动作（**仅用于测试夹具的余额抹平**，业务上不存在该场景）。
 *
 * <p>硬约束：只接受 remark 含 [testFixture 且 status=3 的流水；余额只走
 * {@link com.foodtraceability.common.finance.AtomicBalanceUpdater#addBalance}；
 * 反向流水只增不删；整批在一个事务里。</p>
 */
public interface FinanceOpsService {

    FixtureReverseVO reverseFixtures(FixtureReverseRequest req);
}
