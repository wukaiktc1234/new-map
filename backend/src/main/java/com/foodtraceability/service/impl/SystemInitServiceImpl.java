package com.foodtraceability.service.impl;

import com.foodtraceability.dto.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.SystemInitService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统初始化服务实现类
 * 处理系统初始化向导的业务逻辑
 */
@Service
public class SystemInitServiceImpl implements SystemInitService {

    private static final Logger logger = LoggerFactory.getLogger(SystemInitServiceImpl.class);


    public SystemInitServiceImpl(SystemInitStatusMapper systemInitStatusMapper, EnterpriseConfigMapper enterpriseConfigMapper, PermissionTemplateMapper permissionTemplateMapper, RoleMapper roleMapper, PermissionMapper permissionMapper, RolePermissionMapper rolePermissionMapper, RoleStoreMapper roleStoreMapper, RoleDepartmentMapper roleDepartmentMapper, StoreMapper storeMapper, DepartmentMapper departmentMapper, PositionMapper positionMapper, ObjectMapper objectMapper, CompanyInitRecordMapper companyInitRecordMapper, WarehouseMapper warehouseMapper) {
        this.systemInitStatusMapper = systemInitStatusMapper;
        this.enterpriseConfigMapper = enterpriseConfigMapper;
        this.permissionTemplateMapper = permissionTemplateMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleStoreMapper = roleStoreMapper;
        this.roleDepartmentMapper = roleDepartmentMapper;
        this.storeMapper = storeMapper;
        this.departmentMapper = departmentMapper;
        this.positionMapper = positionMapper;
        this.objectMapper = objectMapper;
        this.companyInitRecordMapper = companyInitRecordMapper;
        this.warehouseMapper = warehouseMapper;
    }

    private final SystemInitStatusMapper systemInitStatusMapper;

    private final EnterpriseConfigMapper enterpriseConfigMapper;

    private final PermissionTemplateMapper permissionTemplateMapper;

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final RoleStoreMapper roleStoreMapper;

    private final RoleDepartmentMapper roleDepartmentMapper;

    private final StoreMapper storeMapper;

    private final DepartmentMapper departmentMapper;

    private final PositionMapper positionMapper;

    private final ObjectMapper objectMapper;

    private final CompanyInitRecordMapper companyInitRecordMapper;

    private final WarehouseMapper warehouseMapper;

    @Override
    public boolean isSystemInitialized() {
        return systemInitStatusMapper.isSystemInitialized();
    }

    @Override
    public SystemInitStatusDTO getInitStatus() {
        SystemInitStatus status = systemInitStatusMapper.selectLatest();
        if (status == null) {
            // 创建初始状态
            status = new SystemInitStatus();
            status.setStep("welcome");
            status.setIsCompleted(false);
            status.setCreatedAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            systemInitStatusMapper.insert(status);
        }

        SystemInitStatusDTO dto = new SystemInitStatusDTO();
        BeanUtils.copyProperties(status, dto);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemInitStatusDTO startInit() {
        // 检查是否已初始化
        if (isSystemInitialized()) {
            throw new RuntimeException("系统已完成初始化，无法重复初始化");
        }

        SystemInitStatus status = systemInitStatusMapper.selectLatest();
        if (status == null) {
            status = new SystemInitStatus();
            status.setStep("enterprise");
            status.setIsCompleted(false);
            status.setCreatedAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            systemInitStatusMapper.insert(status);
        } else {
            status.setStep("enterprise");
            status.setUpdatedAt(LocalDateTime.now());
            systemInitStatusMapper.updateById(status);
        }

        return getInitStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEnterpriseInfo(EnterpriseInfoDTO enterpriseInfo) {
        try {
            EnterpriseConfig config = enterpriseConfigMapper.selectLatest();
            if (config == null) {
                config = new EnterpriseConfig();
                config.setEnterpriseName(enterpriseInfo.getEnterpriseName());
                config.setEnterpriseType(enterpriseInfo.getEnterpriseType());
                config.setScale(enterpriseInfo.getScale());
                config.setCustomConfig(enterpriseInfo.getCustomConfig());
                config.setInitCompleted(false);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                enterpriseConfigMapper.insert(config);
            } else {
                config.setEnterpriseName(enterpriseInfo.getEnterpriseName());
                config.setEnterpriseType(enterpriseInfo.getEnterpriseType());
                config.setScale(enterpriseInfo.getScale());
                config.setCustomConfig(enterpriseInfo.getCustomConfig());
                config.setUpdatedAt(LocalDateTime.now());
                enterpriseConfigMapper.updateById(config);
            }

            // 更新初始化状态
            systemInitStatusMapper.updateStepStatus("organization", false);

            logger.info("保存企业信息成功: {}", enterpriseInfo.getEnterpriseName());
            return true;
        } catch (Exception e) {
            logger.error("保存企业信息失败", e);
            throw new RuntimeException("保存企业信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrganization(OrganizationInfoDTO organizationInfo) {
        try {
            // 保存门店
            if (organizationInfo.getStores() != null && !organizationInfo.getStores().isEmpty()) {
                for (OrganizationInfoDTO.StoreInfo storeInfo : organizationInfo.getStores()) {
                    Store store = new Store();
                    store.setStoreName(storeInfo.getName());
                    store.setStoreCode(storeInfo.getCode());
                    store.setAddress(storeInfo.getAddress());
                    store.setPhone(storeInfo.getContactPhone());
                    store.setManagerName(storeInfo.getContactPerson());
                    store.setRegion(storeInfo.getRegion());
                    store.setCreatedAt(LocalDateTime.now());
                    store.setUpdatedAt(LocalDateTime.now());
                    storeMapper.insert(store);
                }
            }

            // 保存部门
            if (organizationInfo.getDepartments() != null && !organizationInfo.getDepartments().isEmpty()) {
                for (OrganizationInfoDTO.DepartmentInfo deptInfo : organizationInfo.getDepartments()) {
                    Department dept = new Department();
                    dept.setDepartmentName(deptInfo.getName());
                    dept.setDepartmentCode(deptInfo.getCode());
                    if (deptInfo.getParentId() != null && !deptInfo.getParentId().isEmpty()) {
                        dept.setParentId(Long.parseLong(deptInfo.getParentId()));
                    }
                    dept.setLevel(1);
                    dept.setStatus(1);
                    dept.setCreatedAt(LocalDateTime.now());
                    dept.setUpdatedAt(LocalDateTime.now());
                    departmentMapper.insert(dept);
                }
            }

            // 保存职位
            if (organizationInfo.getPositions() != null && !organizationInfo.getPositions().isEmpty()) {
                for (OrganizationInfoDTO.PositionInfo posInfo : organizationInfo.getPositions()) {
                    Position position = new Position();
                    position.setPositionName(posInfo.getName());
                    position.setPositionCode(posInfo.getCode());
                    if (posInfo.getDepartmentId() != null && !posInfo.getDepartmentId().isEmpty()) {
                        position.setDepartmentId(Long.parseLong(posInfo.getDepartmentId()));
                    }
                    position.setStatus(1);
                    position.setCreatedAt(LocalDateTime.now());
                    position.setUpdatedAt(LocalDateTime.now());
                    positionMapper.insert(position);
                }
            }

            // 更新初始化状态
            systemInitStatusMapper.updateStepStatus("roles", false);

            logger.info("保存组织架构成功");
            return true;
        } catch (Exception e) {
            logger.error("保存组织架构失败", e);
            throw new RuntimeException("保存组织架构失败: " + e.getMessage());
        }
    }

    @Override
    public List<PermissionTemplateDTO> getPermissionTemplates() {
        List<PermissionTemplate> templates = permissionTemplateMapper.selectAllEnabled();
        return templates.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PermissionTemplateDTO> getRecommendedTemplates(String enterpriseType, String scale) {
        List<PermissionTemplate> templates = permissionTemplateMapper.selectByEnterpriseTypeAndScale(enterpriseType, scale);
        return templates.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyPermissionTemplate(Integer templateId) {
        try {
            PermissionTemplate template = permissionTemplateMapper.selectById(templateId);
            if (template == null) {
                throw new RuntimeException("权限模板不存在");
            }

            String roleConfig = template.getRoleConfig();
            if (roleConfig == null || roleConfig.isEmpty()) {
                throw new RuntimeException("权限模板配置为空");
            }

            // 解析角色配置
            List<RoleConfigDTO> roleConfigs = objectMapper.readValue(roleConfig, new TypeReference<List<RoleConfigDTO>>() {});

            // 应用角色配置
            configureRoles(roleConfigs);

            // 更新企业配置
            EnterpriseConfig config = enterpriseConfigMapper.selectLatest();
            if (config != null) {
                config.setInitTemplateId(templateId);
                config.setUpdatedAt(LocalDateTime.now());
                enterpriseConfigMapper.updateById(config);
            }

            logger.info("应用权限模板成功: templateId={}", templateId);
            return true;
        } catch (Exception e) {
            logger.error("应用权限模板失败: templateId={}", templateId, e);
            throw new RuntimeException("应用权限模板失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean configureRoles(List<RoleConfigDTO> roleConfigs) {
        try {
            for (RoleConfigDTO config : roleConfigs) {
                Role role;
                if (config.getRoleId() != null && !config.getRoleId().isEmpty()) {
                    // 更新现有角色
                    role = roleMapper.selectById(config.getRoleId());
                    if (role == null) {
                        throw new RuntimeException("角色不存在: " + config.getRoleId());
                    }
                    role.setRoleName(config.getName());
                    role.setRoleDescription(config.getDescription());
                    role.setDataScope(config.getDataScope());
                    role.setStatus(config.getEnabled() != null && config.getEnabled() ? "active" : "inactive");
                    role.setUpdatedTime(LocalDateTime.now());
                    roleMapper.updateById(role);
                } else {
                    // 创建新角色
                    role = new Role();
                    role.setRoleName(config.getName());
                    role.setRoleCode(config.getCode());
                    role.setRoleDescription(config.getDescription());
                    role.setDataScope(config.getDataScope());
                    role.setStatus(config.getEnabled() != null && config.getEnabled() ? "active" : "inactive");
                    role.setCreatedTime(LocalDateTime.now());
                    role.setUpdatedTime(LocalDateTime.now());
                    roleMapper.insert(role);
                }

                // 分配权限
                if (config.getPermissionIds() != null && !config.getPermissionIds().isEmpty()) {
                    // 删除原有权限
                    rolePermissionMapper.deleteByRoleId(role.getId());

                    // 添加新权限
                    List<RolePermission> rolePermissions = new ArrayList<>();
                    for (String permissionId : config.getPermissionIds()) {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(Long.valueOf(role.getId()));
                        rp.setPermissionId(Long.valueOf(permissionId));
                        rolePermissions.add(rp);
                    }
                    rolePermissionMapper.batchInsert(rolePermissions);
                }

                // 分配门店
                if ("stores".equals(config.getDataScope()) && config.getStoreIds() != null && !config.getStoreIds().isEmpty()) {
                    roleStoreMapper.deleteByRoleId(role.getId());

                    List<RoleStore> roleStores = new ArrayList<>();
                    for (String storeId : config.getStoreIds()) {
                        RoleStore rs = new RoleStore();
                        rs.setRoleId(role.getId());
                        rs.setStoreId(storeId);
                        rs.setCreatedAt(LocalDateTime.now());
                        roleStores.add(rs);
                    }
                    roleStoreMapper.batchInsert(roleStores);
                }

                // 分配部门
                if ("departments".equals(config.getDataScope()) && config.getDepartmentIds() != null && !config.getDepartmentIds().isEmpty()) {
                    roleDepartmentMapper.deleteByRoleId(role.getId());

                    List<RoleDepartment> roleDepartments = new ArrayList<>();
                    for (String departmentId : config.getDepartmentIds()) {
                        RoleDepartment rd = new RoleDepartment();
                        rd.setRoleId(role.getId());
                        rd.setDepartmentId(departmentId);
                        rd.setCreatedAt(LocalDateTime.now());
                        roleDepartments.add(rd);
                    }
                    roleDepartmentMapper.batchInsert(roleDepartments);
                }
            }

            logger.info("配置角色成功: count={}", roleConfigs.size());
            return true;
        } catch (Exception e) {
            logger.error("配置角色失败", e);
            throw new RuntimeException("配置角色失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeInit() {
        try {
            // 更新初始化状态
            SystemInitStatus status = systemInitStatusMapper.selectLatest();
            if (status != null) {
                status.setStep("completed");
                status.setIsCompleted(true);
                status.setCompletedAt(LocalDateTime.now());
                status.setUpdatedAt(LocalDateTime.now());
                systemInitStatusMapper.updateById(status);
            }

            // 更新企业配置
            EnterpriseConfig config = enterpriseConfigMapper.selectLatest();
            if (config != null) {
                config.setInitCompleted(true);
                config.setInitCompletedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                enterpriseConfigMapper.updateById(config);
            }

            logger.info("系统初始化完成");
            return true;
        } catch (Exception e) {
            logger.error("完成初始化失败", e);
            throw new RuntimeException("完成初始化失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean skipInit() {
        try {
            // 更新初始化状态
            SystemInitStatus status = systemInitStatusMapper.selectLatest();
            if (status != null) {
                status.setStep("completed");
                status.setIsCompleted(true);
                status.setCompletedAt(LocalDateTime.now());
                status.setUpdatedAt(LocalDateTime.now());
                systemInitStatusMapper.updateById(status);
            }

            // 更新企业配置
            EnterpriseConfig config = enterpriseConfigMapper.selectLatest();
            if (config != null) {
                config.setInitCompleted(true);
                config.setInitCompletedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                enterpriseConfigMapper.updateById(config);
            }

            logger.info("跳过初始化向导");
            return true;
        } catch (Exception e) {
            logger.error("跳过初始化失败", e);
            throw new RuntimeException("跳过初始化失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetInitStatus() {
        try {
            // 重置初始化状态
            SystemInitStatus status = systemInitStatusMapper.selectLatest();
            if (status != null) {
                status.setStep("welcome");
                status.setIsCompleted(false);
                status.setCompletedAt(null);
                status.setUpdatedAt(LocalDateTime.now());
                systemInitStatusMapper.updateById(status);
            }

            // 重置企业配置
            EnterpriseConfig config = enterpriseConfigMapper.selectLatest();
            if (config != null) {
                config.setInitCompleted(false);
                config.setInitCompletedAt(null);
                config.setUpdatedAt(LocalDateTime.now());
                enterpriseConfigMapper.updateById(config);
            }

            logger.info("重置初始化状态成功");
            return true;
        } catch (Exception e) {
            logger.error("重置初始化状态失败", e);
            throw new RuntimeException("重置初始化状态失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initializeCompany(CompanyInitDTO companyInitDTO) {
        logger.info("开始初始化公司信息: {}", companyInitDTO.getCompanyName());

        // 检查是否已初始化
        CompanyInitRecord latestRecord = companyInitRecordMapper.selectLatest();
        if (latestRecord != null && "completed".equals(latestRecord.getStatus())) {
            throw new RuntimeException("公司已完成初始化，不可重复操作");
        }

        // 保存公司初始化记录
        CompanyInitRecord record = new CompanyInitRecord();
        BeanUtils.copyProperties(companyInitDTO, record);
        record.setStatus("completed");
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        companyInitRecordMapper.insert(record);

        // 创建顶级部门
        Department topDept = createTopDepartment(companyInitDTO);
        logger.info("创建顶级部门成功: {}", topDept.getDepartmentName());

        // 创建默认门店部门
        Department storeDept = createDefaultStoreDepartment(topDept);
        logger.info("创建默认门店部门成功: {}", storeDept.getDepartmentName());

        // 创建默认仓库
        Warehouse defaultWarehouse = createDefaultWarehouse(companyInitDTO);
        logger.info("创建默认仓库成功: {}", defaultWarehouse.getWarehouseName());

        return true;
    }

    private Department createTopDepartment(CompanyInitDTO dto) {
        Department dept = new Department();
        dept.setDepartmentName(dto.getCompanyName());
        dept.setDepartmentCode("TOP_" + System.currentTimeMillis());
        dept.setParentId(null);
        dept.setLevel(1);
        dept.setStatus(1);
        dept.setType("company");
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(dept);
        return dept;
    }

    private Department createDefaultStoreDepartment(Department parentDept) {
        Department dept = new Department();
        dept.setDepartmentName("默认门店部门");
        dept.setDepartmentCode("STORE_" + System.currentTimeMillis());
        dept.setParentId(parentDept.getDepartmentId());
        dept.setLevel(2);
        dept.setStatus(1);
        dept.setType("store");
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(dept);
        return dept;
    }

    private Warehouse createDefaultWarehouse(CompanyInitDTO dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode("WH_" + System.currentTimeMillis());
        warehouse.setWarehouseName("默认主仓库");
        warehouse.setWarehouseType(1);
        warehouse.setAddress(dto.getAddress());
        warehouse.setStatus(1);
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    /**
     * 转换模板实体为DTO
     */
    private PermissionTemplateDTO convertToDTO(PermissionTemplate template) {
        PermissionTemplateDTO dto = new PermissionTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }
}
