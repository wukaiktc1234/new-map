package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.store.StoreCreateDTO;
import com.foodtraceability.dto.store.StoreUpdateDTO;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.StoreNew;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.service.StoreNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * 门店服务实现类（增强版）
 */
@Service
public class StoreNewServiceImpl extends ServiceImpl<StoreNewMapper, StoreNew>
        implements StoreNewService {

    private static final Logger log = LoggerFactory.getLogger(StoreNewServiceImpl.class);

    private final StoreNewMapper storeNewMapper;
    private final EmployeeMapper employeeMapper;

    public StoreNewServiceImpl(StoreNewMapper storeNewMapper, EmployeeMapper employeeMapper) {
        this.storeNewMapper = storeNewMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Page<StoreNew> getPageList(Page<StoreNew> page, String storeName,
                                     Integer storeType, Integer status) {
        LambdaQueryWrapper<StoreNew> wrapper = new LambdaQueryWrapper<>();
        
        if (storeName != null && !storeName.isEmpty()) {
            wrapper.like(StoreNew::getStoreName, storeName);
        }
        if (storeType != null) {
            wrapper.eq(StoreNew::getStoreType, storeType);
        }
        if (status != null) {
            wrapper.eq(StoreNew::getStatus, status);
        }
        
        wrapper.orderByDesc(StoreNew::getCreateTime);
        return storeNewMapper.selectPage(page, wrapper);
    }

    @Override
    public StoreNew getByStoreCode(String storeCode) {
        return storeNewMapper.selectByStoreCode(storeCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreNew createStore(StoreCreateDTO createDTO) {
        // 校验门店编码唯一性（应用层强校验，配合DB唯一索引兜底）
        if (!isCodeAvailable(createDTO.getStoreCode(), null)) {
            throw new BusinessException(400, "门店编码已存在: " + createDTO.getStoreCode());
        }

        // 校验店长存在性（若指定了 managerId）
        validateManagerExists(createDTO.getManagerId());

        StoreNew store = new StoreNew();

        // 生成或使用传入的门店编码
        store.setStoreCode(createDTO.getStoreCode());
        store.setStoreName(createDTO.getStoreName());
        store.setStoreType(createDTO.getStoreType());
        store.setAddress(createDTO.getAddress());
        store.setPhone(createDTO.getPhone());

        // 解析营业时间
        if (createDTO.getBusinessHoursStart() != null) {
            store.setBusinessHoursStart(LocalTime.parse(createDTO.getBusinessHoursStart()));
        }
        if (createDTO.getBusinessHoursEnd() != null) {
            store.setBusinessHoursEnd(LocalTime.parse(createDTO.getBusinessHoursEnd()));
        }

        store.setAreaSize(createDTO.getAreaSize());
        store.setArea(createDTO.getArea());
        store.setManagerId(createDTO.getManagerId());
        store.setDepartmentId(createDTO.getDepartmentId());
        store.setOrgId(createDTO.getOrgId());
        store.setOpenDate(createDTO.getOpenDate());
        store.setStatus(createDTO.getStatus() != null ? createDTO.getStatus() : 1);
        store.setLicenseNo(createDTO.getLicenseNo());
        store.setLicenseExpiry(createDTO.getLicenseExpiry());
        store.setConfigJson(createDTO.getConfigJson());
        store.setRemark(createDTO.getRemark());

        storeNewMapper.insert(store);

        log.info("创建门店成功: 编码={}, 名称={}", store.getStoreCode(), store.getStoreName());
        return store;
    }

    /**
     * 校验店长（员工）是否存在。
     * 弱化校验：managerId 为空跳过；指定但员工不存在时仅记录警告日志，不阻断门店创建。
     * 业务场景：新店开业时店长员工可能尚未录入系统，门店档案创建不应被阻塞。
     * @param managerId 店长员工ID，为空则跳过校验
     */
    private void validateManagerExists(String managerId) {
        if (managerId == null || managerId.isEmpty()) {
            return;
        }
        Employee employee = employeeMapper.selectById(managerId);
        if (employee == null) {
            // 弱化校验：仅记录警告，不抛异常，避免阻塞门店档案创建
            log.warn("指定的店长员工不存在: managerId={}，门店仍将创建，后续请补充员工档案", managerId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long storeId, Integer status) {
        StoreNew store = storeNewMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("门店不存在: " + storeId);
        }

        int oldStatus = store.getStatus();
        store.setStatus(status);
        storeNewMapper.updateById(store);

        log.info("门店状态变更: ID={}, {} -> {}", storeId, oldStatus, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreNew updateStore(Long storeId, StoreUpdateDTO updateDTO) {
        StoreNew store = storeNewMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException(400, "门店不存在: " + storeId);
        }

        // 校验门店编码唯一性（store_code 不可变，排除自身后不应存在相同编码）
        if (!isCodeAvailable(store.getStoreCode(), storeId)) {
            throw new BusinessException(400, "门店编码已存在: " + store.getStoreCode());
        }

        // 仅更新非 null 字段
        if (updateDTO.getStoreName() != null) {
            store.setStoreName(updateDTO.getStoreName());
        }
        if (updateDTO.getStoreType() != null) {
            store.setStoreType(updateDTO.getStoreType());
        }
        if (updateDTO.getAddress() != null) {
            store.setAddress(updateDTO.getAddress());
        }
        if (updateDTO.getPhone() != null) {
            store.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getBusinessHoursStart() != null) {
            store.setBusinessHoursStart(LocalTime.parse(updateDTO.getBusinessHoursStart()));
        }
        if (updateDTO.getBusinessHoursEnd() != null) {
            store.setBusinessHoursEnd(LocalTime.parse(updateDTO.getBusinessHoursEnd()));
        }
        if (updateDTO.getAreaSize() != null) {
            store.setAreaSize(updateDTO.getAreaSize());
        }
        if (updateDTO.getArea() != null) {
            store.setArea(updateDTO.getArea());
        }
        if (updateDTO.getManagerId() != null) {
            // 校验新店长存在性
            validateManagerExists(updateDTO.getManagerId());
            store.setManagerId(updateDTO.getManagerId());
        }
        if (updateDTO.getDepartmentId() != null) {
            store.setDepartmentId(updateDTO.getDepartmentId());
        }
        if (updateDTO.getOrgId() != null) {
            store.setOrgId(updateDTO.getOrgId());
        }
        if (updateDTO.getOpenDate() != null) {
            store.setOpenDate(updateDTO.getOpenDate());
        }
        if (updateDTO.getStatus() != null) {
            store.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getLicenseNo() != null) {
            store.setLicenseNo(updateDTO.getLicenseNo());
        }
        if (updateDTO.getLicenseExpiry() != null) {
            store.setLicenseExpiry(updateDTO.getLicenseExpiry());
        }
        if (updateDTO.getConfigJson() != null) {
            store.setConfigJson(updateDTO.getConfigJson());
        }
        if (updateDTO.getRemark() != null) {
            store.setRemark(updateDTO.getRemark());
        }

        storeNewMapper.updateById(store);
        log.info("更新门店成功: ID={}, 编码={}", storeId, store.getStoreCode());
        return store;
    }

    @Override
    public List<StoreNew> getActiveStores() {
        return storeNewMapper.selectActiveStores();
    }

    @Override
    public boolean isCodeAvailable(String storeCode, Long excludeId) {
        int count = storeNewMapper.countByCode(storeCode, excludeId);
        return count == 0;
    }
}
