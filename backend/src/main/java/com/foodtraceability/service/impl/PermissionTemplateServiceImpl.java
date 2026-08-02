package com.foodtraceability.service.impl;

import com.foodtraceability.dto.PermissionTemplateDTO;
import com.foodtraceability.entity.PermissionTemplate;
import com.foodtraceability.mapper.PermissionTemplateMapper;
import com.foodtraceability.service.PermissionTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限模板服务实现
 * 对应权限中心 4 种模式：集中式单店/标准连锁/大型连锁/自定义
 */
@Service
public class PermissionTemplateServiceImpl implements PermissionTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PermissionTemplateServiceImpl.class);

    private final PermissionTemplateMapper permissionTemplateMapper;

    public PermissionTemplateServiceImpl(PermissionTemplateMapper permissionTemplateMapper) {
        this.permissionTemplateMapper = permissionTemplateMapper;
    }

    @Override
    public List<PermissionTemplateDTO> getAllEnabledTemplates() {
        List<PermissionTemplate> templates = permissionTemplateMapper.selectAllEnabled();
        return templates.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<PermissionTemplateDTO> getSystemTemplates() {
        List<PermissionTemplate> templates = permissionTemplateMapper.selectSystemTemplates();
        return templates.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public PermissionTemplateDTO getTemplateByCode(String code) {
        PermissionTemplate template = permissionTemplateMapper.selectByCode(code);
        return template != null ? toDTO(template) : null;
    }

    @Override
    public List<PermissionTemplateDTO> getTemplatesByEnterpriseTypeAndScale(String enterpriseType, String scaleRange) {
        List<PermissionTemplate> templates = permissionTemplateMapper.selectByEnterpriseTypeAndScale(enterpriseType, scaleRange);
        return templates.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public PermissionTemplate getTemplateEntityByCode(String code) {
        return permissionTemplateMapper.selectByCode(code);
    }

    @Override
    public PermissionTemplateDTO createTemplate(PermissionTemplateDTO dto) {
        PermissionTemplate entity = toEntity(dto);
        entity.setIsSystem(false);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        permissionTemplateMapper.insert(entity);
        log.info("创建自定义权限模板: code={}, name={}", entity.getCode(), entity.getName());
        return toDTO(entity);
    }

    @Override
    public PermissionTemplateDTO updateTemplate(Integer id, PermissionTemplateDTO dto) {
        PermissionTemplate existing = permissionTemplateMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在: id=" + id);
        }
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new IllegalStateException("系统模板不允许修改: " + existing.getCode());
        }
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getRoleConfig() != null) existing.setRoleConfig(dto.getRoleConfig());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        permissionTemplateMapper.updateById(existing);
        log.info("更新权限模板: id={}, code={}", id, existing.getCode());
        return toDTO(existing);
    }

    @Override
    public void deleteTemplate(Integer id) {
        PermissionTemplate existing = permissionTemplateMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在: id=" + id);
        }
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new IllegalStateException("系统模板不允许删除: " + existing.getCode());
        }
        permissionTemplateMapper.deleteById(id);
        log.info("删除权限模板: id={}, code={}", id, existing.getCode());
    }

    // ==================== 转换方法 ====================

    private PermissionTemplateDTO toDTO(PermissionTemplate entity) {
        PermissionTemplateDTO dto = new PermissionTemplateDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setEnterpriseType(entity.getEnterpriseType());
        dto.setScaleRange(entity.getScaleRange());
        dto.setRoleConfig(entity.getRoleConfig());
        dto.setIsSystem(entity.getIsSystem());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private PermissionTemplate toEntity(PermissionTemplateDTO dto) {
        PermissionTemplate entity = new PermissionTemplate();
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setDescription(dto.getDescription());
        entity.setEnterpriseType(dto.getEnterpriseType() != null ? dto.getEnterpriseType() : "restaurant");
        entity.setScaleRange(dto.getScaleRange() != null ? dto.getScaleRange() : "all");
        entity.setRoleConfig(dto.getRoleConfig());
        return entity;
    }
}
