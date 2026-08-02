package com.foodtraceability.controller.purchase;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.ProcurementTraceInfoVO;
import com.foodtraceability.dto.ProcurementTraceQueryDTO;
import com.foodtraceability.service.ProcurementTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购溯源控制器
 * 提供采购全链路追溯查询接口
 */
@Tag(name = "采购溯源", description = "采购全链路追溯查询接口")
@RestController
@RequestMapping("/v1/purchase/trace")
public class ProcurementTraceController {

    private static final Logger log = LoggerFactory.getLogger(ProcurementTraceController.class);

    private final ProcurementTraceService procurementTraceService;

    public ProcurementTraceController(ProcurementTraceService procurementTraceService) {
        this.procurementTraceService = procurementTraceService;
    }

    /**
     * 根据物料ID查询采购溯源信息
     */
    @Operation(summary = "根据物料ID查询采购溯源信息")
    @GetMapping("/material/{materialId}")
    @PreAuthorize("hasAuthority('purchase:trace:view') or hasAuthority('*')")
    public Result<List<ProcurementTraceInfoVO>> getTraceByMaterialId(
            @Parameter(description = "物料ID") @PathVariable Long materialId) {
        try {
            List<ProcurementTraceInfoVO> result = procurementTraceService.getTraceInfoByMaterialId(materialId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("根据物料ID查询采购溯源信息失败，materialId={}", materialId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 根据采购订单编号查询采购溯源信息
     */
    @Operation(summary = "根据采购订单编号查询采购溯源信息")
    @GetMapping("/order/{orderNo}")
    @PreAuthorize("hasAuthority('purchase:trace:view') or hasAuthority('*')")
    public Result<ProcurementTraceInfoVO> getTraceByOrderNo(
            @Parameter(description = "采购订单编号") @PathVariable String orderNo) {
        try {
            ProcurementTraceInfoVO result = procurementTraceService.getTraceInfoByOrderNo(orderNo);
            if (result == null) {
                return Result.error("未找到采购溯源信息");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("根据订单编号查询采购溯源信息失败，orderNo={}", orderNo, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 根据采购申请编号查询采购溯源信息
     */
    @Operation(summary = "根据采购申请编号查询采购溯源信息")
    @GetMapping("/request/{requestNo}")
    @PreAuthorize("hasAuthority('purchase:trace:view') or hasAuthority('*')")
    public Result<List<ProcurementTraceInfoVO>> getTraceByRequestNo(
            @Parameter(description = "采购申请编号") @PathVariable String requestNo) {
        try {
            List<ProcurementTraceInfoVO> result = procurementTraceService.getTraceInfoByRequestNo(requestNo);
            return Result.success(result);
        } catch (Exception e) {
            log.error("根据申请编号查询采购溯源信息失败，requestNo={}", requestNo, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 根据入库单号查询采购溯源信息
     */
    @Operation(summary = "根据入库单号查询采购溯源信息")
    @GetMapping("/stockin/{stockinCode}")
    @PreAuthorize("hasAuthority('purchase:trace:view') or hasAuthority('*')")
    public Result<ProcurementTraceInfoVO> getTraceByStockinCode(
            @Parameter(description = "入库单号") @PathVariable String stockinCode) {
        try {
            ProcurementTraceInfoVO result = procurementTraceService.getTraceInfoByStockinCode(stockinCode);
            if (result == null) {
                return Result.error("未找到采购溯源信息");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("根据入库单号查询采购溯源信息失败，stockinCode={}", stockinCode, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 根据批次号查询采购溯源信息
     */
    @Operation(summary = "根据批次号查询采购溯源信息")
    @GetMapping("/batch/{batchNo}")
    @PreAuthorize("hasAuthority('purchase:trace:view') or hasAuthority('*')")
    public Result<List<ProcurementTraceInfoVO>> getTraceByBatchNo(
            @Parameter(description = "批次号") @PathVariable String batchNo) {
        try {
            List<ProcurementTraceInfoVO> result = procurementTraceService.getTraceInfoByBatchNo(batchNo);
            return Result.success(result);
        } catch (Exception e) {
            log.error("根据批次号查询采购溯源信息失败，batchNo={}", batchNo, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 综合查询采购溯源信息
     */
    @Operation(summary = "综合查询采购溯源信息")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('purchase:trace:view') or hasAuthority('*')")
    public Result<List<ProcurementTraceInfoVO>> searchTrace(
            @Parameter(description = "物料ID") @RequestParam(required = false) Long materialId,
            @Parameter(description = "采购订单编号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "采购申请编号") @RequestParam(required = false) String requestNo,
            @Parameter(description = "入库单号") @RequestParam(required = false) String stockinCode,
            @Parameter(description = "批次号") @RequestParam(required = false) String batchNo) {
        try {
            ProcurementTraceQueryDTO queryDTO = new ProcurementTraceQueryDTO();
            queryDTO.setMaterialId(materialId);
            queryDTO.setOrderNo(orderNo);
            queryDTO.setRequestNo(requestNo);
            queryDTO.setStockinCode(stockinCode);
            queryDTO.setBatchNo(batchNo);
            List<ProcurementTraceInfoVO> result = procurementTraceService.searchTraceInfo(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("综合查询采购溯源信息失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }
}
