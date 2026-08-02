package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DiningTable;
import java.util.List;

public interface DiningTableService extends IService<DiningTable> {
    
    List<DiningTable> getAllTables();
    
    List<DiningTable> getTablesByStatus(String status);
    
    List<DiningTable> getTablesByArea(String area);
    
    List<String> getAllAreas();
    
    DiningTable getByTableNumber(String tableNumber);
    
    DiningTable createTable(DiningTable table);
    
    DiningTable updateTable(Long id, DiningTable table);
    
    void deleteTable(Long id);
    
    DiningTable occupyTable(Long id, Long orderId, Integer guestCount);
    
    DiningTable releaseTable(Long id);
    
    DiningTable transferTable(Long fromId, Long toId);
    
    String generateQrCode(Long id);
}
