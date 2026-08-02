package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.schedule.ConflictCheckResultVO;
import com.foodtraceability.dto.schedule.ConflictDetailVO;
import com.foodtraceability.dto.schedule.ConflictQueryDTO;

import java.util.List;

/**
 * 排班冲突检测服务接口
 * 提供冲突检查、查询、自动修复、忽略等功能
 *
 * <p>冲突级别：
 * <ul>
 *   <li>error: 严重冲突（必须修复才能发布）</li>
 *   <li>warning: 警告（建议修复）</li>
 *   <li>info: 提示信息</li>
 * </ul>
 */
public interface ScheduleConflictService {

    /**
     * 检查方案的排班冲突
     * 业务逻辑：扫描方案下所有排班条目，识别重复班次、连续工作超限、休息违规等问题
     * @param planId 方案ID
     * @return 冲突检查结果（含汇总、详情列表、是否可发布）
     */
    ConflictCheckResultVO checkConflicts(String planId);

    /**
     * 查询方案的冲突列表（支持按级别筛选）
     * @param queryDTO 查询条件（含方案ID、级别筛选）
     * @return 冲突详情列表
     */
    List<ConflictDetailVO> getConflictList(ConflictQueryDTO queryDTO);

    /**
     * 根据冲突ID获取冲突详情
     * @param id 冲突ID
     * @return 冲突详情，不存在返回null
     */
    ConflictDetailVO getConflictById(String id);

    /**
     * 自动修复冲突
     * 业务规则：仅 autoFixAvailable=true 且 fix_status=pending 的冲突可修复
     * @param id 冲突ID
     * @return 修复后的冲突详情
     */
    ConflictDetailVO autoFixConflict(String id);

    /**
     * 忽略冲突
     * 业务规则：将 fix_status 标记为 ignored
     * @param id 冲突ID
     * @return 忽略后的冲突详情
     */
    ConflictDetailVO ignoreConflict(String id);
}
