package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.PlanItemRequest;
import com.foodtraceability.entity.PlanItem;
import com.foodtraceability.mapper.PlanItemMapper;
import com.foodtraceability.service.PlanItemService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 计划项Service实现类
 * 
 * @author demo
 * @since 1.0.0
 */
@Service
public class PlanItemServiceImpl extends ServiceImpl<PlanItemMapper, PlanItem> implements PlanItemService {

    @Override
    public PlanItem createPlanItem(PlanItemRequest request) {
        PlanItem planItem = new PlanItem();
        planItem.setTitle(request.getTitle());
        planItem.setDescription(request.getDescription());
        planItem.setType(request.getType());
        planItem.setPriority(request.getPriority());
        planItem.setStatus(request.getStatus());
        planItem.setAssignee(request.getAssignee());
        planItem.setStartTime(request.getStartTime());
        planItem.setEndTime(request.getEndTime());
        planItem.setRemark(request.getRemark());
        planItem.setCreateTime(LocalDateTime.now());
        planItem.setUpdateTime(LocalDateTime.now());
        planItem.setDeleted(0);

        // 如果状态是已完成，设置完成时间
        if ("done".equals(request.getStatus())) {
            planItem.setCompleteTime(LocalDateTime.now());
        }

        this.save(planItem);
        return planItem;
    }

    @Override
    public PlanItem updatePlanItem(Long id, PlanItemRequest request) {
        PlanItem planItem = this.getById(id);
        if (planItem == null) {
            throw new RuntimeException("计划项不存在");
        }

        planItem.setTitle(request.getTitle());
        planItem.setDescription(request.getDescription());
        planItem.setType(request.getType());
        planItem.setPriority(request.getPriority());
        
        // 如果状态从非完成变为完成，设置完成时间
        if ("done".equals(request.getStatus()) && !"done".equals(planItem.getStatus())) {
            planItem.setCompleteTime(LocalDateTime.now());
        }
        // 如果状态从完成变为非完成，清空完成时间
        else if (!"done".equals(request.getStatus()) && "done".equals(planItem.getStatus())) {
            planItem.setCompleteTime(null);
        }
        
        planItem.setStatus(request.getStatus());
        planItem.setAssignee(request.getAssignee());
        planItem.setStartTime(request.getStartTime());
        planItem.setEndTime(request.getEndTime());
        planItem.setRemark(request.getRemark());
        planItem.setUpdateTime(LocalDateTime.now());

        this.updateById(planItem);
        return planItem;
    }

    @Override
    public boolean deletePlanItem(Long id) {
        return this.removeById(id);
    }

    @Override
    public PlanItem getPlanItemById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<PlanItem> listPlanItems(Map<String, Object> params) {
        LambdaQueryWrapper<PlanItem> queryWrapper = new LambdaQueryWrapper<>();
        
        // 类型过滤
        if (params.containsKey("type")) {
            queryWrapper.eq(PlanItem::getType, params.get("type"));
        }
        
        // 状态过滤
        if (params.containsKey("status")) {
            queryWrapper.eq(PlanItem::getStatus, params.get("status"));
        }
        
        // 优先级过滤
        if (params.containsKey("priority")) {
            queryWrapper.eq(PlanItem::getPriority, params.get("priority"));
        }
        
        // 负责人过滤
        if (params.containsKey("assignee")) {
            queryWrapper.eq(PlanItem::getAssignee, params.get("assignee"));
        }
        
        // 标题搜索
        if (params.containsKey("keyword")) {
            queryWrapper.like(PlanItem::getTitle, params.get("keyword"))
                    .or().like(PlanItem::getDescription, params.get("keyword"));
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc(PlanItem::getCreateTime);
        
        return this.list(queryWrapper);
    }

    @Override
    public Map<String, Long> countPlanItems(Map<String, Object> params) {
        Map<String, Long> result = new HashMap<>();
        
        // 统计总数量
        LambdaQueryWrapper<PlanItem> totalWrapper = new LambdaQueryWrapper<>();
        addCommonFilters(totalWrapper, params);
        result.put("total", this.count(totalWrapper));
        
        // 统计未开始数量
        LambdaQueryWrapper<PlanItem> todoWrapper = new LambdaQueryWrapper<>();
        addCommonFilters(todoWrapper, params);
        todoWrapper.eq(PlanItem::getStatus, "todo");
        result.put("todo", this.count(todoWrapper));
        
        // 统计进行中数量
        LambdaQueryWrapper<PlanItem> doingWrapper = new LambdaQueryWrapper<>();
        addCommonFilters(doingWrapper, params);
        doingWrapper.eq(PlanItem::getStatus, "doing");
        result.put("doing", this.count(doingWrapper));
        
        // 统计已完成数量
        LambdaQueryWrapper<PlanItem> doneWrapper = new LambdaQueryWrapper<>();
        addCommonFilters(doneWrapper, params);
        doneWrapper.eq(PlanItem::getStatus, "done");
        result.put("done", this.count(doneWrapper));
        
        return result;
    }

    @Override
    public int batchUpdateStatus(List<Long> ids, String status) {
        PlanItem planItem = new PlanItem();
        planItem.setStatus(status);
        planItem.setUpdateTime(LocalDateTime.now());
        
        // 如果状态是已完成，设置完成时间
        if ("done".equals(status)) {
            planItem.setCompleteTime(LocalDateTime.now());
        }
        // 如果状态不是已完成，清空完成时间
        else {
            planItem.setCompleteTime(null);
        }
        
        LambdaQueryWrapper<PlanItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(PlanItem::getId, ids);
        
        return this.update(planItem, queryWrapper) ? ids.size() : 0;
    }
    
    /**
     * 添加通用过滤条件
     * 
     * @param queryWrapper 查询包装器
     * @param params 查询参数
     */
    private void addCommonFilters(LambdaQueryWrapper<PlanItem> queryWrapper, Map<String, Object> params) {
        // 类型过滤
        if (params.containsKey("type")) {
            queryWrapper.eq(PlanItem::getType, params.get("type"));
        }
        
        // 优先级过滤
        if (params.containsKey("priority")) {
            queryWrapper.eq(PlanItem::getPriority, params.get("priority"));
        }
        
        // 负责人过滤
        if (params.containsKey("assignee")) {
            queryWrapper.eq(PlanItem::getAssignee, params.get("assignee"));
        }
    }
}
