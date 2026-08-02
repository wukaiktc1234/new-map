package com.foodtraceability.dataservice.impl.schedule;

import com.foodtraceability.dataservice.schedule.ScheduleTemplateDataService;
import com.foodtraceability.dto.schedule.ScheduleTemplateVO;
import com.foodtraceability.entity.schedule.ScheduleTemplate;
import com.foodtraceability.mapper.schedule.ScheduleTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排班模板数据服务实现类
 * 直接查询 PostgreSQL 数据库获取数据
 */
@Service
public class ScheduleTemplateDataServiceImpl implements ScheduleTemplateDataService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleTemplateDataServiceImpl.class);

    private final ScheduleTemplateMapper scheduleTemplateMapper;

    public ScheduleTemplateDataServiceImpl(ScheduleTemplateMapper scheduleTemplateMapper) {
        this.scheduleTemplateMapper = scheduleTemplateMapper;
    }

    @Override
    public Map<String, ScheduleTemplateVO> batchGetTemplateBasicInfo(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ScheduleTemplate> entities = scheduleTemplateMapper.selectBatchIds(templateIds);
        return entities.stream()
                .filter(entity -> entity != null)
                .collect(Collectors.toMap(
                        entity -> String.valueOf(entity.getTemplateId()),
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public ScheduleTemplateVO getTemplateBasicInfo(Long templateId) {
        if (templateId == null) {
            return null;
        }

        ScheduleTemplate entity = scheduleTemplateMapper.selectById(templateId);
        return convertToVO(entity);
    }

    @Override
    public List<ScheduleTemplateVO> getTemplatesByStore(Long storeId) {
        if (storeId == null) {
            return Collections.emptyList();
        }

        List<ScheduleTemplate> entities = scheduleTemplateMapper.selectByStoreId(storeId);
        return entities.stream()
                .map(this::convertToVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public ScheduleTemplateVO getDefaultTemplateByStore(Long storeId) {
        if (storeId == null) {
            return null;
        }

        ScheduleTemplate entity = scheduleTemplateMapper.selectDefaultByStoreId(storeId);
        return convertToVO(entity);
    }

    @Override
    public void clearTemplateCache(String templateId) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearTemplateBatchCache(List<Long> templateIds) {
        // 缓存已移除，空实现保留接口契约
    }

    @Override
    public void clearStoreTemplateCache(Long storeId) {
        // 缓存已移除，空实现保留接口契约
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
