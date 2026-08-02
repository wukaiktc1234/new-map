package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DiningTable;
import com.foodtraceability.mapper.DiningTableMapper;
import com.foodtraceability.service.DiningTableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable> implements DiningTableService {
    
    @Override
    public List<DiningTable> getAllTables() {
        return list();
    }
    
    @Override
    public List<DiningTable> getTablesByStatus(String status) {
        return baseMapper.findByStatus(status);
    }
    
    @Override
    public List<DiningTable> getTablesByArea(String area) {
        return baseMapper.findByArea(area);
    }
    
    @Override
    public List<String> getAllAreas() {
        return baseMapper.findAllAreas();
    }
    
    @Override
    public DiningTable getByTableNumber(String tableNumber) {
        return baseMapper.findByTableNumber(tableNumber);
    }
    
    @Override
    @Transactional
    public DiningTable createTable(DiningTable table) {
        table.setStatus("available");
        table.setCreatedAt(LocalDateTime.now());
        table.setUpdatedAt(LocalDateTime.now());
        save(table);
        table.setQrCode(generateQrCode(table.getId()));
        updateById(table);
        return table;
    }
    
    @Override
    @Transactional
    public DiningTable updateTable(Long id, DiningTable table) {
        DiningTable existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("桌台不存在");
        }
        existing.setTableName(table.getTableName());
        existing.setCapacity(table.getCapacity());
        existing.setArea(table.getArea());
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        return existing;
    }
    
    @Override
    @Transactional
    public void deleteTable(Long id) {
        DiningTable table = getById(id);
        if (table != null && "occupied".equals(table.getStatus())) {
            throw new RuntimeException("桌台正在使用中，无法删除");
        }
        removeById(id);
    }
    
    @Override
    @Transactional
    public DiningTable occupyTable(Long id, Long orderId, Integer guestCount) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new RuntimeException("桌台不存在");
        }
        if ("occupied".equals(table.getStatus())) {
            throw new RuntimeException("桌台已被占用");
        }
        table.setStatus("occupied");
        table.setCurrentOrderId(orderId);
        table.setGuestCount(guestCount);
        table.setSeatedAt(LocalDateTime.now());
        table.setUpdatedAt(LocalDateTime.now());
        updateById(table);
        return table;
    }
    
    @Override
    @Transactional
    public DiningTable releaseTable(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new RuntimeException("桌台不存在");
        }
        table.setStatus("available");
        table.setCurrentOrderId(null);
        table.setGuestCount(null);
        table.setSeatedAt(null);
        table.setUpdatedAt(LocalDateTime.now());
        updateById(table);
        return table;
    }
    
    @Override
    @Transactional
    public DiningTable transferTable(Long fromId, Long toId) {
        DiningTable fromTable = getById(fromId);
        DiningTable toTable = getById(toId);
        
        if (fromTable == null || toTable == null) {
            throw new RuntimeException("桌台不存在");
        }
        
        if (!"occupied".equals(fromTable.getStatus())) {
            throw new RuntimeException("源桌台未被占用");
        }
        
        if ("occupied".equals(toTable.getStatus())) {
            throw new RuntimeException("目标桌台已被占用");
        }
        
        toTable.setStatus("occupied");
        toTable.setCurrentOrderId(fromTable.getCurrentOrderId());
        toTable.setGuestCount(fromTable.getGuestCount());
        toTable.setSeatedAt(fromTable.getSeatedAt());
        toTable.setUpdatedAt(LocalDateTime.now());
        
        fromTable.setStatus("available");
        fromTable.setCurrentOrderId(null);
        fromTable.setGuestCount(null);
        fromTable.setSeatedAt(null);
        fromTable.setUpdatedAt(LocalDateTime.now());
        
        updateById(fromTable);
        updateById(toTable);
        
        return toTable;
    }
    
    @Override
    public String generateQrCode(Long id) {
        return "TABLE_" + id + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
