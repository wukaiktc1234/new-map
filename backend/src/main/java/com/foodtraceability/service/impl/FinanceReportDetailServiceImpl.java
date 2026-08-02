package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.FinanceReportItemDTO;
import com.foodtraceability.entity.finance.FinanceReportDetail;
import com.foodtraceability.mapper.FinanceReportDetailMapper;
import com.foodtraceability.service.FinanceReportDetailService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceReportDetailServiceImpl extends ServiceImpl<FinanceReportDetailMapper, FinanceReportDetail> implements FinanceReportDetailService {

    @Override
    public List<FinanceReportItemDTO> getReportDetails(Long reportId) {
        List<FinanceReportDetail> details = baseMapper.selectByReportId(reportId);
        return buildReportItemTree(details);
    }

    @Override
    public List<FinanceReportItemDTO> getReportDetailsByType(Long reportId, String itemType) {
        List<FinanceReportDetail> details = baseMapper.selectByReportIdAndType(reportId, itemType);
        return details.stream().map(detail -> {
            FinanceReportItemDTO dto = new FinanceReportItemDTO();
            BeanUtils.copyProperties(detail, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReportDetails(Long reportId, List<FinanceReportItemDTO> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        
        deleteReportDetails(reportId);
        
        saveReportItems(reportId, details, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReportDetails(Long reportId) {
        baseMapper.deleteByReportId(reportId);
    }

    private List<FinanceReportItemDTO> buildReportItemTree(List<FinanceReportDetail> details) {
        Map<Long, List<FinanceReportDetail>> childrenMap = new HashMap<>();
        List<FinanceReportItemDTO> rootItems = new ArrayList<>();
        
        for (FinanceReportDetail detail : details) {
            childrenMap.computeIfAbsent(detail.getId(), k -> new ArrayList<>());
        }
        
        for (FinanceReportDetail detail : details) {
            FinanceReportItemDTO dto = new FinanceReportItemDTO();
            BeanUtils.copyProperties(detail, dto);
            dto.setChildren(new ArrayList<>());
            
            if (detail.getParentId() == null || detail.getParentId() == 0) {
                rootItems.add(dto);
            } else {
                List<FinanceReportDetail> siblings = childrenMap.get(detail.getParentId());
                if (siblings != null) {
                    siblings.add(detail);
                }
            }
        }
        
        return rootItems;
    }

    private void saveReportItems(Long reportId, List<FinanceReportItemDTO> items, Long parentId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < items.size(); i++) {
            FinanceReportItemDTO dto = items.get(i);
            FinanceReportDetail detail = new FinanceReportDetail();
            BeanUtils.copyProperties(dto, detail);
            detail.setReportId(reportId);
            detail.setParentId(parentId);
            detail.setSort(i + 1);
            baseMapper.insert(detail);
            
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                saveReportItems(reportId, dto.getChildren(), detail.getId());
            }
        }
    }
}
