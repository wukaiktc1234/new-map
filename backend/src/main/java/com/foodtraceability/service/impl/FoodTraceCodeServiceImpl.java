package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.FoodTraceCodeGenerateDTO;
import com.foodtraceability.entity.FoodTraceCode;
import com.foodtraceability.mapper.FoodTraceCodeMapper;
import com.foodtraceability.service.FoodTraceCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 食品追溯码服务实现类
 */
@Service
public class FoodTraceCodeServiceImpl extends ServiceImpl<FoodTraceCodeMapper, FoodTraceCode> implements FoodTraceCodeService {


    public FoodTraceCodeServiceImpl(FoodTraceCodeMapper foodTraceCodeMapper, ObjectMapper objectMapper) {
        this.foodTraceCodeMapper = foodTraceCodeMapper;
        this.objectMapper = objectMapper;
    }

    private final FoodTraceCodeMapper foodTraceCodeMapper;

    private final ObjectMapper objectMapper;

    private static final String PREFIX = "FTC";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public FoodTraceCode generate(FoodTraceCodeGenerateDTO dto) {
        FoodTraceCode code = new FoodTraceCode();
        
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String traceCodeId = PREFIX + dateStr + String.format("%04d", getNextSequence());
        String traceCode = traceCodeId + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        
        code.setTraceCodeId(traceCodeId);
        code.setTraceCode(traceCode);
        code.setOrderId(dto.getOrderId());
        code.setOrderNumber(dto.getOrderNumber());
        code.setOrderType(dto.getOrderType());
        code.setDishId(dto.getDishId());
        code.setDishName(dto.getDishName());
        code.setDishPrice(dto.getDishPrice());
        code.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        code.setKitchenOrderId(dto.getKitchenOrderId());
        code.setChefId(dto.getChefId());
        code.setChefName(dto.getChefName());
        code.setStoreId(dto.getStoreId());
        code.setStoreName(dto.getStoreName());
        code.setTableNumber(dto.getTableNumber());
        code.setMakeStatus("pending");
        code.setStatus("created");
        code.setPrintCount(0);
        code.setMaterialCost(dto.getMaterialCost());
        code.setLaborCost(dto.getLaborCost());
        
        if (dto.getMaterialCost() != null && dto.getLaborCost() != null) {
            code.setTotalCost(dto.getMaterialCost().add(dto.getLaborCost()));
        } else if (dto.getMaterialCost() != null) {
            code.setTotalCost(dto.getMaterialCost());
        }
        
        if (dto.getMaterialTraceCodes() != null && !dto.getMaterialTraceCodes().isEmpty()) {
            try {
                code.setMaterialTraceCodes(objectMapper.writeValueAsString(dto.getMaterialTraceCodes()));
            } catch (Exception e) {
                code.setMaterialTraceCodes(dto.getMaterialTraceCodes().toString());
            }
        }
        
        code.setRemark(dto.getRemark());
        code.setCreateTime(LocalDateTime.now());
        code.setUpdateTime(LocalDateTime.now());
        
        save(code);
        return code;
    }

    @Override
    public FoodTraceCode getByTraceCode(String traceCode) {
        return foodTraceCodeMapper.selectByTraceCode(traceCode);
    }

    @Override
    public List<FoodTraceCode> getByOrderId(String orderId) {
        return foodTraceCodeMapper.selectByOrderId(orderId);
    }

    @Override
    public List<FoodTraceCode> getByKitchenOrderId(Long kitchenOrderId) {
        return foodTraceCodeMapper.selectByKitchenOrderId(kitchenOrderId);
    }

    @Override
    @Transactional
    public boolean updateMakeStatus(String traceCodeId, String makeStatus) {
        FoodTraceCode code = getById(traceCodeId);
        if (code == null) {
            return false;
        }
        
        code.setMakeStatus(makeStatus);
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean startMake(String traceCodeId, Long chefId, String chefName) {
        FoodTraceCode code = getById(traceCodeId);
        if (code == null) {
            return false;
        }
        
        code.setMakeStatus("making");
        code.setMakeStartTime(LocalDateTime.now());
        code.setChefId(chefId);
        code.setChefName(chefName);
        code.setUpdateTime(LocalDateTime.now());
        code.setUpdateBy(chefName);
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean completeMake(String traceCodeId) {
        FoodTraceCode code = getById(traceCodeId);
        if (code == null) {
            return false;
        }
        
        code.setMakeStatus("completed");
        code.setMakeCompleteTime(LocalDateTime.now());
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean serve(String traceCode) {
        FoodTraceCode code = getByTraceCode(traceCode);
        if (code == null) {
            return false;
        }
        
        code.setMakeStatus("served");
        code.setStatus("served");
        code.setServeTime(LocalDateTime.now());
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean printLabel(String traceCodeId, Long printerId) {
        FoodTraceCode code = getById(traceCodeId);
        if (code == null) {
            return false;
        }
        
        code.setStatus("printed");
        code.setPrintTime(LocalDateTime.now());
        code.setPrintCount(code.getPrintCount() + 1);
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    public boolean batchPrint(List<String> traceCodeIds, Long printerId) {
        for (String traceCodeId : traceCodeIds) {
            printLabel(traceCodeId, printerId);
        }
        return true;
    }

    @Override
    public BigDecimal calculateOrderCost(String orderId) {
        List<FoodTraceCode> codes = getByOrderId(orderId);
        BigDecimal totalCost = BigDecimal.ZERO;
        for (FoodTraceCode code : codes) {
            if (code.getTotalCost() != null) {
                totalCost = totalCost.add(code.getTotalCost());
            }
        }
        return totalCost;
    }

    @Override
    public String generateQrCode(String traceCodeId) {
        FoodTraceCode code = getById(traceCodeId);
        if (code == null) {
            return null;
        }
        return "/api/food-trace/qr/" + code.getTraceCode();
    }

    @Override
    public Object getTraceInfo(String traceCode) {
        FoodTraceCode code = getByTraceCode(traceCode);
        if (code == null) {
            return null;
        }
        
        Map<String, Object> traceInfo = new HashMap<>();
        traceInfo.put("traceCode", code.getTraceCode());
        traceInfo.put("dishName", code.getDishName());
        traceInfo.put("storeName", code.getStoreName());
        traceInfo.put("makeTime", code.getMakeCompleteTime());
        traceInfo.put("chefName", code.getChefName());
        traceInfo.put("serveTime", code.getServeTime());
        
        if (code.getOrderType() != null) {
            String orderTypeStr = switch (code.getOrderType()) {
                case 0 -> "堂食";
                case 1 -> "外卖";
                case 2 -> "自提";
                default -> "未知";
            };
            traceInfo.put("orderType", orderTypeStr);
        }
        
        if (code.getTableNumber() != null) {
            traceInfo.put("tableNumber", code.getTableNumber());
        }
        
        return traceInfo;
    }

    @Override
    public int countByStatus(String status) {
        return foodTraceCodeMapper.countByStatus(status);
    }

    @Override
    @Transactional
    public List<FoodTraceCode> generateForOrder(String kitchenOrderId) {
        Long kid = null;
        try {
            kid = Long.parseLong(kitchenOrderId.replace("KO", ""));
        } catch (Exception e) {
            kid = Long.parseLong(kitchenOrderId);
        }
        
        List<FoodTraceCode> existingCodes = foodTraceCodeMapper.selectByKitchenOrderId(kid);
        if (existingCodes != null && !existingCodes.isEmpty()) {
            return existingCodes;
        }
        
        List<FoodTraceCode> codes = new java.util.ArrayList<>();
        return codes;
    }

    private int getNextSequence() {
        List<FoodTraceCode> codes = lambdaQuery()
                .likeRight(FoodTraceCode::getTraceCodeId, PREFIX + LocalDate.now().format(DATE_FORMAT))
                .orderByDesc(FoodTraceCode::getTraceCodeId)
                .last("LIMIT 1")
                .list();
        
        if (codes.isEmpty()) {
            return 1;
        }
        String lastId = codes.get(0).getTraceCodeId();
        String seqStr = lastId.substring(lastId.length() - 4);
        return Integer.parseInt(seqStr) + 1;
    }
}
