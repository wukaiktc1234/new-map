package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.schedule.ScheduleTemplateCreateDTO;
import com.foodtraceability.dto.schedule.ScheduleTemplateUpdateDTO;
import com.foodtraceability.dto.schedule.ScheduleTemplateVO;

import java.util.List;

/**
 * 排班模板服务接口
 * 提供排班模板的增删改查及状态管理功能
 */
public interface ScheduleTemplateService {

    /**
     * 查询门店模板列表
     * @param storeId 门店ID
     * @return 模板列表(按创建时间倒序)
     */
    List<ScheduleTemplateVO> getTemplateList(Long storeId);

    /**
     * 获取模板详情
     * @param templateId 模板ID
     * @return 模板详情
     */
    ScheduleTemplateVO getTemplateById(Long templateId);

    /**
     * 创建模板
     * @param createDTO 创建请求DTO
     * @return 创建后的模板
     */
    ScheduleTemplateVO createTemplate(ScheduleTemplateCreateDTO createDTO);

    /**
     * 编辑模板
     * @param templateId 模板ID
     * @param updateDTO 更新请求DTO
     * @return 更新后的模板
     */
    ScheduleTemplateVO updateTemplate(Long templateId, ScheduleTemplateUpdateDTO updateDTO);

    /**
     * 删除模板(逻辑删除)
     * 需检查是否有关联的排班方案(useCount > 0时禁止删除)
     * @param templateId 模板ID
     */
    void deleteTemplate(Long templateId);

    /**
     * 复制模板
     * 新名称自动追加"副本"后缀
     * @param templateId 源模板ID
     * @return 复制后的新模板
     */
    ScheduleTemplateVO copyTemplate(Long templateId);

    /**
     * 设为默认模板
     * 每个门店仅一个默认模板，需先将该门店其他模板的isDefault设为false
     * @param templateId 模板ID
     * @return 设置后的模板
     */
    ScheduleTemplateVO setDefault(Long templateId);

    /**
     * 启用/停用切换
     * @param templateId 模板ID
     * @return 切换后的模板
     */
    ScheduleTemplateVO toggleStatus(Long templateId);
}
