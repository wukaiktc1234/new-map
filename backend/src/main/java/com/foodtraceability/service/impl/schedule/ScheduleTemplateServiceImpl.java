package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.dataservice.schedule.ScheduleTemplateDataService;
import com.foodtraceability.dto.schedule.ScheduleTemplateCreateDTO;
import com.foodtraceability.dto.schedule.ScheduleTemplateUpdateDTO;
import com.foodtraceability.dto.schedule.ScheduleTemplateVO;
import com.foodtraceability.entity.schedule.ScheduleTemplate;
import com.foodtraceability.mapper.schedule.ScheduleTemplateMapper;
import com.foodtraceability.service.schedule.ScheduleTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 排班模板服务实现类
 * 实现排班模板的业务逻辑处理
 */
@Service
public class ScheduleTemplateServiceImpl implements ScheduleTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleTemplateServiceImpl.class);

    /** 副本后缀 */
    private static final String COPY_SUFFIX = " 副本";

    private final ScheduleTemplateMapper scheduleTemplateMapper;
    private final ScheduleTemplateDataService scheduleTemplateDataService;

    public ScheduleTemplateServiceImpl(
            ScheduleTemplateMapper scheduleTemplateMapper,
            ScheduleTemplateDataService scheduleTemplateDataService) {
        this.scheduleTemplateMapper = scheduleTemplateMapper;
        this.scheduleTemplateDataService = scheduleTemplateDataService;
    }

    @Override
    public List<ScheduleTemplateVO> getTemplateList(Long storeId) {
        log.debug("查询门店模板列表: storeId={}", storeId);
        return scheduleTemplateDataService.getTemplatesByStore(storeId);
    }

    @Override
    public ScheduleTemplateVO getTemplateById(Long templateId) {
        log.debug("查询模板详情: templateId={}", templateId);
        return scheduleTemplateDataService.getTemplateBasicInfo(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleTemplateVO createTemplate(ScheduleTemplateCreateDTO createDTO) {
        log.info("创建排班模板: templateName={}, storeId={}",
                createDTO.getTemplateName(), createDTO.getStoreId());

        // 构建实体
        ScheduleTemplate entity = new ScheduleTemplate();
        entity.setTemplateName(createDTO.getTemplateName());
        entity.setDescription(createDTO.getDescription());
        entity.setStoreId(createDTO.getStoreId());
        entity.setDemandMatrix(createDTO.getDemandMatrix());
        entity.setEnabledRuleIds(createDTO.getEnabledRuleIds());
        entity.setIsDefault(Boolean.FALSE);
        entity.setStatus(ScheduleTemplate.STATUS_ACTIVE);
        entity.setUseCount(0);

        // 插入数据库
        scheduleTemplateMapper.insert(entity);

        // 清除缓存
        scheduleTemplateDataService.clearStoreTemplateCache(createDTO.getStoreId());

        log.info("排班模板创建成功: templateId={}, templateName={}",
                entity.getTemplateId(), entity.getTemplateName());
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleTemplateVO updateTemplate(Long templateId, ScheduleTemplateUpdateDTO updateDTO) {
        log.info("更新排班模板: templateId={}", templateId);

        // 查询现有记录
        ScheduleTemplate existingEntity = scheduleTemplateMapper.selectById(templateId);
        if (existingEntity == null) {
            throw new RuntimeException("排班模板不存在");
        }

        // 更新非空字段
        if (updateDTO.getTemplateName() != null) {
            existingEntity.setTemplateName(updateDTO.getTemplateName());
        }
        if (updateDTO.getDescription() != null) {
            existingEntity.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getDemandMatrix() != null) {
            existingEntity.setDemandMatrix(updateDTO.getDemandMatrix());
        }
        if (updateDTO.getEnabledRuleIds() != null) {
            existingEntity.setEnabledRuleIds(updateDTO.getEnabledRuleIds());
        }

        // 更新数据库
        scheduleTemplateMapper.updateById(existingEntity);

        // 清除缓存
        scheduleTemplateDataService.clearTemplateCache(String.valueOf(templateId));
        scheduleTemplateDataService.clearStoreTemplateCache(existingEntity.getStoreId());

        log.info("排班模板更新成功: templateId={}", templateId);
        return convertToVO(existingEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        log.info("删除排班模板: templateId={}", templateId);

        // 查询现有记录
        ScheduleTemplate existingEntity = scheduleTemplateMapper.selectById(templateId);
        if (existingEntity == null) {
            throw new RuntimeException("排班模板不存在");
        }

        // 检查是否有关联的排班方案（useCount > 0时禁止删除）
        Integer useCount = existingEntity.getUseCount();
        if (useCount != null && useCount > 0) {
            throw new RuntimeException("该模板已被使用" + useCount + "次，无法删除。请先解除关联的排班方案。");
        }

        // 逻辑删除
        scheduleTemplateMapper.deleteById(templateId);

        // 清除缓存
        scheduleTemplateDataService.clearTemplateCache(String.valueOf(templateId));
        scheduleTemplateDataService.clearStoreTemplateCache(existingEntity.getStoreId());

        log.info("排班模板删除成功: templateId={}, templateName={}",
                templateId, existingEntity.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleTemplateVO copyTemplate(Long templateId) {
        log.info("复制排班模板: templateId={}", templateId);

        // 查询源模板
        ScheduleTemplate sourceEntity = scheduleTemplateMapper.selectById(templateId);
        if (sourceEntity == null) {
            throw new RuntimeException("源排班模板不存在");
        }

        // 构建新实体（名称追加"副本"后缀）
        ScheduleTemplate newEntity = new ScheduleTemplate();
        newEntity.setTemplateName(sourceEntity.getTemplateName() + COPY_SUFFIX);
        newEntity.setDescription(sourceEntity.getDescription());
        newEntity.setStoreId(sourceEntity.getStoreId());
        newEntity.setDemandMatrix(sourceEntity.getDemandMatrix());
        newEntity.setEnabledRuleIds(sourceEntity.getEnabledRuleIds());
        newEntity.setIsDefault(Boolean.FALSE); // 副本不设为默认
        newEntity.setStatus(ScheduleTemplate.STATUS_ACTIVE);
        newEntity.setUseCount(0); // 使用次数重置

        // 插入数据库
        scheduleTemplateMapper.insert(newEntity);

        // 清除缓存
        scheduleTemplateDataService.clearStoreTemplateCache(sourceEntity.getStoreId());

        log.info("排班模板复制成功: sourceId={}, newId={}, newName={}",
                templateId, newEntity.getTemplateId(), newEntity.getTemplateName());
        return convertToVO(newEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleTemplateVO setDefault(Long templateId) {
        log.info("设置默认模板: templateId={}", templateId);

        // 查询目标模板
        ScheduleTemplate targetEntity = scheduleTemplateMapper.selectById(templateId);
        if (targetEntity == null) {
            throw new RuntimeException("排班模板不存在");
        }

        Long storeId = targetEntity.getStoreId();

        // 先将该门店所有其他模板的isDefault设为false
        LambdaUpdateWrapper<ScheduleTemplate> clearDefaultWrapper = new LambdaUpdateWrapper<>();
        clearDefaultWrapper.eq(ScheduleTemplate::getStoreId, storeId)
                .eq(ScheduleTemplate::getIsDefault, true)
                .set(ScheduleTemplate::getIsDefault, false);
        scheduleTemplateMapper.update(null, clearDefaultWrapper);

        // 将目标模板设为默认
        targetEntity.setIsDefault(true);
        scheduleTemplateMapper.updateById(targetEntity);

        // 清除缓存
        scheduleTemplateDataService.clearStoreTemplateCache(storeId);

        log.info("默认模板设置成功: templateId={}, storeId={}", templateId, storeId);
        return convertToVO(targetEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleTemplateVO toggleStatus(Long templateId) {
        log.info("切换模板状态: templateId={}", templateId);

        // 查询现有记录
        ScheduleTemplate existingEntity = scheduleTemplateMapper.selectById(templateId);
        if (existingEntity == null) {
            throw new RuntimeException("排班模板不存在");
        }

        // 切换状态
        String newStatus = ScheduleTemplate.STATUS_ACTIVE.equals(existingEntity.getStatus())
                ? ScheduleTemplate.STATUS_INACTIVE
                : ScheduleTemplate.STATUS_ACTIVE;
        existingEntity.setStatus(newStatus);

        // 更新数据库
        scheduleTemplateMapper.updateById(existingEntity);

        // 清除缓存
        scheduleTemplateDataService.clearTemplateCache(String.valueOf(templateId));
        scheduleTemplateDataService.clearStoreTemplateCache(existingEntity.getStoreId());

        log.info("模板状态切换成功: templateId={}, newStatus={}", templateId, newStatus);
        return convertToVO(existingEntity);
    }

    /**
     * 将实体转换为视图对象
     * @param entity 排班模板实体
     * @return 视图对象
     */
    private ScheduleTemplateVO convertToVO(ScheduleTemplate entity) {
        if (entity == null) {
            return null;
        }

        ScheduleTemplateVO vo = new ScheduleTemplateVO();
        vo.setTemplateId(entity.getTemplateId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setDescription(entity.getDescription());
        vo.setStoreId(entity.getStoreId());
        vo.setDemandMatrix(entity.getDemandMatrix());
        vo.setEnabledRuleIds(entity.getEnabledRuleIds());
        vo.setIsDefault(entity.getIsDefault());
        vo.setStatus(entity.getStatus());
        vo.setUseCount(entity.getUseCount());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        return vo;
    }
}
