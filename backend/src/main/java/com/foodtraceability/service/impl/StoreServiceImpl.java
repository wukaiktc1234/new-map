package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Position;
import com.foodtraceability.entity.Store;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.mapper.PositionMapper;
import com.foodtraceability.mapper.StoreMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);


    public StoreServiceImpl(StoreMapper storeMapper, DepartmentMapper departmentMapper, PositionMapper positionMapper, DepartmentService departmentService, PositionService positionService, EmployeeService employeeService) {
        this.storeMapper = storeMapper;
        this.departmentMapper = departmentMapper;
        this.positionMapper = positionMapper;
        this.departmentService = departmentService;
        this.positionService = positionService;
        this.employeeService = employeeService;
    }

    private final StoreMapper storeMapper;

    private final DepartmentMapper departmentMapper;

    private final PositionMapper positionMapper;

    private final DepartmentService departmentService;

    private final PositionService positionService;

    private final EmployeeService employeeService;

    @Override
    public List<Store> getActiveStores() {
        try {
            logger.info("开始查询活跃门店列表");
            LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Store::getStatus, "active");
            List<Store> stores = storeMapper.selectList(wrapper);
            logger.info("查询到 {} 个活跃门店", stores != null ? stores.size() : 0);
            return stores;
        } catch (Exception e) {
            logger.error("查询活跃门店列表失败", e);
            throw new RuntimeException("查询活跃门店列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Store getStoreById(String id) {
        try {
            logger.info("开始查询门店信息，门店ID: {}", id);
            Store store = storeMapper.selectById(id);
            logger.info("查询门店信息成功，门店名称: {}", store != null ? store.getStoreName() : null);
            return store;
        } catch (Exception e) {
            logger.error("查询门店信息失败，门店ID: {}", id, e);
            throw new RuntimeException("查询门店信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Store createStore(Store store) {
        try {
            logger.info("开始创建门店，门店名称: {}", store.getStoreName());
            storeMapper.insert(store);
            logger.info("创建门店成功，门店ID: {}", store.getStoreId());
            return store;
        } catch (Exception e) {
            logger.error("创建门店失败", e);
            throw new RuntimeException("创建门店失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStoreStatus(String storeId, String status) {
        try {
            logger.info("开始更新门店状态，门店ID: {}, 状态: {}", storeId, status);

            Store store = storeMapper.selectById(storeId);
            if (store == null) {
                throw new RuntimeException("门店不存在");
            }

            store.setStatus(status);
            storeMapper.updateById(store);
            logger.info("门店状态更新成功");

            logger.info("门店状态更新完成");
        } catch (Exception e) {
            logger.error("更新门店状态失败，门店ID: {}", storeId, e);
            throw new RuntimeException("更新门店状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStore(String storeId) {
        try {
            logger.info("开始删除门店，门店ID: {}", storeId);

            Store store = storeMapper.selectById(storeId);
            if (store == null) {
                throw new RuntimeException("门店不存在");
            }

            storeMapper.deleteById(storeId);
            logger.info("门店删除成功");

            logger.info("门店删除完成");
        } catch (Exception e) {
            logger.error("删除门店失败，门店ID: {}", storeId, e);
            throw new RuntimeException("删除门店失败: " + e.getMessage(), e);
        }
    }
}
