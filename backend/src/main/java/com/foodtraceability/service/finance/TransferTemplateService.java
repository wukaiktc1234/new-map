package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.TransferTemplate;

/**
 * 结转模板Service接口
 * 管理期末结转业务模板，包括损益结转、定期计提等自动化结转规则
 */
public interface TransferTemplateService extends IService<TransferTemplate> {

    /**
     * 创建结转模板
     * @param dto 创建DTO
     * @return 模板VO
     */
    TransferTemplateVO create(TransferTemplateCreateDTO dto);

    /**
     * 更新结转模板
     * @param templateId 模板ID
     * @param dto 更新DTO
     * @return 是否成功
     */
    boolean update(Long templateId, TransferTemplateUpdateDTO dto);

    /**
     * 删除结转模板（逻辑删除）
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean delete(Long templateId);

    /**
     * 获取模板详情
     * @param templateId 模板ID
     * @return 模板VO
     */
    TransferTemplateVO getDetail(Long templateId);

    /**
     * 分页查询结转模板
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<TransferTemplateVO> getPage(TransferTemplateQueryDTO query);

    /**
     * 启用/停用模板
     * @param templateId 模板ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long templateId, Boolean enabled);
}
