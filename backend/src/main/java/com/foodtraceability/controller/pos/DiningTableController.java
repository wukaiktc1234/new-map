package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.DiningTable;
import com.foodtraceability.service.DiningTableService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/pos/tables")
public class DiningTableController {
    

    public DiningTableController(DiningTableService diningTableService) {
        this.diningTableService = diningTableService;
    }

    private final DiningTableService diningTableService;
    
    @GetMapping
    public Result<List<DiningTable>> getAllTables() {
        return Result.success(diningTableService.getAllTables());
    }
    
    @GetMapping("/status/{status}")
    public Result<List<DiningTable>> getTablesByStatus(@PathVariable String status) {
        return Result.success(diningTableService.getTablesByStatus(status));
    }
    
    @GetMapping("/area/{area}")
    public Result<List<DiningTable>> getTablesByArea(@PathVariable String area) {
        return Result.success(diningTableService.getTablesByArea(area));
    }
    
    @GetMapping("/areas")
    public Result<List<String>> getAllAreas() {
        return Result.success(diningTableService.getAllAreas());
    }
    
    @GetMapping("/{id}")
    public Result<DiningTable> getTableById(@PathVariable Long id) {
        DiningTable table = diningTableService.getById(id);
        if (table == null) {
            return Result.error("桌台不存在");
        }
        return Result.success(table);
    }
    
    @GetMapping("/number/{tableNumber}")
    public Result<DiningTable> getTableByNumber(@PathVariable String tableNumber) {
        DiningTable table = diningTableService.getByTableNumber(tableNumber);
        if (table == null) {
            return Result.error("桌台不存在");
        }
        return Result.success(table);
    }
    
    @GetMapping("/qrcode/{qrCode}")
    public Result<DiningTable> getTableByQrCode(@PathVariable String qrCode) {
        List<DiningTable> tables = diningTableService.lambdaQuery()
            .eq(DiningTable::getQrCode, qrCode)
            .list();
        if (tables.isEmpty()) {
            return Result.error("无效的桌台二维码");
        }
        return Result.success(tables.get(0));
    }
    
    @PostMapping
    public Result<DiningTable> createTable(@RequestBody DiningTable table) {
        try {
            return Result.success(diningTableService.createTable(table));
        } catch (Exception e) {
            return Result.error("创建桌台失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public Result<DiningTable> updateTable(@PathVariable Long id, @RequestBody DiningTable table) {
        try {
            return Result.success(diningTableService.updateTable(id, table));
        } catch (Exception e) {
            return Result.error("更新桌台失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteTable(@PathVariable Long id) {
        try {
            diningTableService.deleteTable(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除桌台失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/occupy")
    public Result<DiningTable> occupyTable(
            @PathVariable Long id,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer guestCount) {
        try {
            return Result.success(diningTableService.occupyTable(id, orderId, guestCount));
        } catch (Exception e) {
            return Result.error("占用桌台失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/release")
    public Result<DiningTable> releaseTable(@PathVariable Long id) {
        try {
            return Result.success(diningTableService.releaseTable(id));
        } catch (Exception e) {
            return Result.error("释放桌台失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/transfer")
    public Result<DiningTable> transferTable(
            @RequestParam Long fromId,
            @RequestParam Long toId) {
        try {
            return Result.success(diningTableService.transferTable(fromId, toId));
        } catch (Exception e) {
            return Result.error("换台失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/regenerate-qrcode")
    public Result<DiningTable> regenerateQrCode(@PathVariable Long id) {
        try {
            DiningTable table = diningTableService.getById(id);
            if (table == null) {
                return Result.error("桌台不存在");
            }
            table.setQrCode(diningTableService.generateQrCode(id));
            diningTableService.updateById(table);
            return Result.success(table);
        } catch (Exception e) {
            return Result.error("重新生成二维码失败: " + e.getMessage());
        }
    }
}
