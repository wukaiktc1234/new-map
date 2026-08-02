package com.foodtraceability.controller.purchase;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.purchase.PurchaseSettlementCreateDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementQueryDTO;
import com.foodtraceability.dto.purchase.PurchaseSettlementUpdateDTO;
import com.foodtraceability.entity.PurchaseSettlement;
import com.foodtraceability.service.PurchaseSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 采购结算管理控制器
 * 提供采购结算的增删改查、确认结算、发票申请/收票等接口
 *
 * <p>状态编码（后端 INTEGER）：
 * <ul>
 *   <li>0 - 待结算（前端 'pending'）</li>
 *   <li>1 - 部分结算（前端 'partial'）</li>
 *   <li>2 - 财务审核中（前端 'finance_reviewing'）</li>
 *   <li>3 - 已完成（前端 'completed'）</li>
 *   <li>4 - 已逾期（前端 'overdue'）</li>
 * </ul>
 *
 * <p>发票状态：0未开票 1已开票 2已收票</p>
 * <p>金额单位：分（BIGINT），前端显示元</p>
 */
@Tag(name = "采购结算", description = "采购结算单管理接口")
@RestController
@RequestMapping("/v1/purchase/settlements")
public class PurchaseSettlementController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseSettlementController.class);

    private final PurchaseSettlementService purchaseSettlementService;

    public PurchaseSettlementController(PurchaseSettlementService purchaseSettlementService) {
        this.purchaseSettlementService = purchaseSettlementService;
    }

    /**
     * 分页查询采购结算列表
     * @param queryDTO 查询参数（含分页、状态、发票状态、日期范围、关键词等）
     * @return 分页结果
     */
    @Operation(summary = "分页查询采购结算列表",
            description = "支持按结算编号、订单编号、供应商名称、状态、发票状态、到期日期范围、关键词筛选")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<PurchaseSettlementDTO>> getList(@Valid PurchaseSettlementQueryDTO queryDTO) {
        try {
            Page<PurchaseSettlement> page = purchaseSettlementService.getSettlementPage(queryDTO);
            // 转换实体分页为 DTO 分页
            Page<PurchaseSettlementDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            List<PurchaseSettlementDTO> dtoList = page.getRecords().stream()
                    .map(PurchaseSettlementDTO::fromEntity)
                    .collect(Collectors.toList());
            dtoPage.setRecords(dtoList);
            return Result.success(dtoPage);
        } catch (Exception e) {
            log.error("分页查询采购结算列表失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询采购结算列表失败");
        }
    }

    /**
     * 获取采购结算详情
     * @param id 采购结算ID
     * @return 采购结算详情
     */
    @Operation(summary = "获取采购结算详情", description = "根据ID获取采购结算详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchaseSettlementDTO> getById(@PathVariable Long id) {
        try {
            PurchaseSettlement settlement = purchaseSettlementService.getSettlementDetail(id);
            return Result.success(PurchaseSettlementDTO.fromEntity(settlement));
        } catch (Exception e) {
            log.error("获取采购结算详情失败: id={}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取采购结算详情失败");
        }
    }

    /**
     * 创建采购结算
     * @param createDTO 采购结算创建数据
     * @return 创建结果
     */
    @Operation(summary = "创建采购结算", description = "新增一条采购结算记录，结算单编号由后端自动生成")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'finance_manager', 'purchaser')")
    public Result<PurchaseSettlementDTO> create(@Valid @RequestBody PurchaseSettlementCreateDTO createDTO) {
        try {
            PurchaseSettlement settlement = purchaseSettlementService.createSettlement(createDTO);
            return Result.success(PurchaseSettlementDTO.fromEntity(settlement));
        } catch (Exception e) {
            log.error("创建采购结算失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建采购结算失败");
        }
    }

    /**
     * 更新采购结算
     * @param id 采购结算ID
     * @param updateDTO 采购结算更新数据
     * @return 更新结果
     */
    @Operation(summary = "更新采购结算", description = "根据ID更新采购结算信息（仅待结算/部分结算状态可更新）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager', 'purchaser')")
    public Result<PurchaseSettlementDTO> update(@PathVariable Long id,
                                                  @Valid @RequestBody PurchaseSettlementUpdateDTO updateDTO) {
        try {
            PurchaseSettlement settlement = purchaseSettlementService.updateSettlement(id, updateDTO);
            return Result.success(PurchaseSettlementDTO.fromEntity(settlement));
        } catch (Exception e) {
            log.error("更新采购结算失败: id={}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新采购结算失败");
        }
    }

    /**
     * 删除采购结算
     * @param id 采购结算ID
     * @return 删除结果
     */
    @Operation(summary = "删除采购结算", description = "根据ID删除采购结算记录（逻辑删除，仅待结算状态可删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager')")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            purchaseSettlementService.deleteSettlement(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除采购结算失败: id={}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除采购结算失败");
        }
    }

    /**
     * 确认结算（付款/完成）
     * @param id 采购结算ID
     * @param action 操作类型：pay-付款，complete-完成
     * @param voucherNo 付款凭证号（pay 操作时必填）
     * @return 更新后的结算单
     */
    @Operation(summary = "确认结算", description = "对指定采购结算执行付款或完成操作")
    @PostMapping("/{id}/settle")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager')")
    public Result<PurchaseSettlementDTO> settle(@PathVariable Long id,
                                                  @Parameter(description = "操作类型：pay-付款，complete-完成") @RequestParam String action,
                                                  @Parameter(description = "付款凭证号（pay 操作时必填）") @RequestParam(required = false) String voucherNo) {
        try {
            PurchaseSettlement settlement = purchaseSettlementService.settle(id, action, voucherNo);
            return Result.success(PurchaseSettlementDTO.fromEntity(settlement));
        } catch (Exception e) {
            log.error("确认结算失败: id={}, action={}", id, action, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "确认结算失败");
        }
    }

    /**
     * 申请发票
     * @param id 采购结算ID
     * @return 更新后的结算单
     */
    @Operation(summary = "申请发票", description = "对指定采购结算发起发票申请（发票状态：未开票 → 已开票）")
    @PostMapping("/{id}/invoice/apply")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager')")
    public Result<PurchaseSettlementDTO> applyInvoice(@PathVariable Long id) {
        try {
            PurchaseSettlement settlement = purchaseSettlementService.applyInvoice(id);
            return Result.success(PurchaseSettlementDTO.fromEntity(settlement));
        } catch (Exception e) {
            log.error("申请发票失败: id={}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "申请发票失败");
        }
    }

    /**
     * 确认收票
     * @param id 采购结算ID
     * @param invoiceNo 发票号
     * @return 更新后的结算单
     */
    @Operation(summary = "确认收票", description = "确认收到发票并登记发票号（发票状态：已开票 → 已收票）")
    @PostMapping("/{id}/invoice/receive")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager')")
    public Result<PurchaseSettlementDTO> receiveInvoice(@PathVariable Long id,
                                                         @Parameter(description = "发票号") @RequestParam String invoiceNo) {
        try {
            PurchaseSettlement settlement = purchaseSettlementService.receiveInvoice(id, invoiceNo);
            return Result.success(PurchaseSettlementDTO.fromEntity(settlement));
        } catch (Exception e) {
            log.error("确认收票失败: id={}, invoiceNo={}", id, invoiceNo, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "确认收票失败");
        }
    }
}
