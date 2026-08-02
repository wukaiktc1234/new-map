package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.schedule.GenerateScheduleDTO;
import com.foodtraceability.dto.schedule.ScheduleEntryVO;

import java.util.List;
import java.util.Map;

/**
 * 排班生成引擎服务接口
 * 提供基于模板的自动排班生成、预览、进度查询等功能
 *
 * <p>注意：本接口当前为框架实现，复杂的排班算法（约束求解、班次平衡等）后续完善
 */
public interface ScheduleEngineService {

    /**
     * 生成排班（仅预览，不保存）
     * @param generateDTO 生成参数（方案ID、模板ID、是否覆盖等）
     * @return 生成的排班条目列表
     */
    List<ScheduleEntryVO> generateSchedule(GenerateScheduleDTO generateDTO);

    /**
     * 预览排班（基于方案和模板）
     * @param planId 方案ID
     * @param templateId 模板ID（可选，不传则使用方案已配置的模板）
     * @return 预览结果（含条目列表和统计信息）
     */
    Map<String, Object> previewSchedule(String planId, String templateId);

    /**
     * 获取生成进度
     * @param planId 方案ID
     * @return 进度信息（含进度百分比、状态等）
     */
    Map<String, Object> getGenerateProgress(String planId);

    /**
     * 获取生成历史
     * @param planId 方案ID
     * @return 历史记录列表
     */
    List<Map<String, Object>> getGenerateHistory(String planId);
}
