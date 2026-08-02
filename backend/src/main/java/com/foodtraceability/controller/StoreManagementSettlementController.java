package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.SettlementQueryDTO;
import com.foodtraceability.dto.store.operation.DailySettlementCreateDTO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementShiftVO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.User;
import com.foodtraceability.utils.SecurityUtils;
import com.foodtraceability.service.DailySettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 日结对账管理控制器
 * 提供每日营收核对与确认的RESTful API
 * 支持基于角色的权限控制，敏感字段（成本/利润/毛利率）仅对总部角色可见
 */
@RestController
@RequestMapping("/v1/store-management/settlements")
@Tag(name = "日结对账管理", description = "每日营收核对与确认")
public class StoreManagementSettlementController {

    private final DailySettlementService dailySettlementService;

    /**
     * 构造函数注入
     *
     * @param dailySettlementService 日结对账服务
     */
    public StoreManagementSettlementController(DailySettlementService dailySettlementService) {
        this.dailySettlementService = dailySettlementService;
    }

    /**
     * 分页查询日结对账列表
     * 敏感字段根据用户角色自动过滤
     */
    @GetMapping
    @Operation(summary = "获取日结列表", description = "分页查询日结对账记录（含权限过滤）")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'REGIONAL_MANAGER', 'FINANCE_DIRECTOR', 'ADMIN', 'admin')")
    public Result<IPage<DailySettlementVO>> getSettlementList(SettlementQueryDTO query,
                                                               Principal principal) {
        User currentUser = SecurityUtils.getCurrentUser();
        IPage<DailySettlementVO> settlementPage = dailySettlementService.getSettlementList(query, currentUser);
        return Result.success(settlementPage);
    }

    /**
     * 获取日结对账详情
     * 包含完整的结算信息和班次明细列表
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取日结详情", description = "获取日结对账完整信息（含班次明细和权限过滤）")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'REGIONAL_MANAGER', 'FINANCE_DIRECTOR', 'ADMIN', 'admin')")
    public Result<DailySettlementVO> getSettlementDetail(@PathVariable String id, Principal principal) {
        User currentUser = SecurityUtils.getCurrentUser();
        DailySettlementVO settlement = dailySettlementService.getSettlementWithPermission(id, currentUser);
        return Result.success(settlement);
    }

    /**
     * 获取班次明细列表
     * 查询指定日结记录下的所有班次数据
     */
    @GetMapping("/{id}/shifts")
    @Operation(summary = "获取班次明细", description = "获取指定日结的班次营业数据列表")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'REGIONAL_MANAGER', 'FINANCE_DIRECTOR', 'ADMIN', 'admin')")
    public Result<List<DailySettlementShiftVO>> getShiftDetail(@PathVariable String id) {
        List<DailySettlementShiftVO> shifts = dailySettlementService.getShiftsBySettlementId(id);
        return Result.success(shifts);
    }

    /**
     * 创建日结草稿
     */
    @PostMapping
    @Operation(summary = "创建日结草稿", description = "创建新的日结记录（初始状态为草稿）")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CASHIER', 'ADMIN', 'admin')")
    public Result<DailySettlementVO> createSettlement(@Valid @RequestBody DailySettlementCreateDTO dto,
                                                       Principal principal) {
        DailySettlementVO settlement = dailySettlementService.createSettlement(dto, principal.getName());
        return Result.success(settlement);
    }

    /**
     * 确认对账
     * 将结算状态更新为approved
     */
    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认对账", description = "将日结记录标记为已确认")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'FINANCE_DIRECTOR', 'ADMIN', 'admin')")
    public Result<Void> confirmSettlement(@PathVariable String id, Principal principal) {
        dailySettlementService.confirmSettlement(id, principal.getName());
        return Result.success();
    }

    /**
     * 上报异常问题
     * 将结算状态更新为abnormal，并通知区域经理
     */
    @PostMapping("/{id}/report-issue")
    @Operation(summary = "上报异常", description = "报告日结数据异常问题")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CASHIER', 'ADMIN', 'admin')")
    public Result<Void> reportIssue(@PathVariable String id,
                                     @RequestBody @Valid Object dto,
                                     Principal principal) {
        dailySettlementService.reportIssue(id, dto, principal.getName());
        return Result.success();
    }

    /**
     * 导出日结对账数据为Excel文件
     * 支持按门店ID、日期范围筛选，导出包含支付方式明细的完整对账数据
     *
     * <h2>导出字段说明</h2>
     * <ul>
     *   <li>基础信息：结算日期、门店、状态、订单数、总营收</li>
     *   <li>支付方式明细：现金/微信/支付宝/会员余额的金额和笔数</li>
     *   <li>其他统计：优惠金额、退款金额、作废金额</li>
     * </ul>
     *
     * @param storeId   门店ID（可选）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate   结束日期（可选，格式：yyyy-MM-dd）
     * @param response  HTTP响应（用于输出Excel文件流）
     */
    @GetMapping("/export")
    @Operation(summary = "导出日结对账Excel", description = "导出指定日期范围的日结对账数据为Excel文件")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'REGIONAL_MANAGER', 'FINANCE_DIRECTOR', 'ADMIN', 'admin')")
    public void exportSettlements(@RequestParam(required = false) String storeId,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   HttpServletResponse response,
                                   Principal principal) {
        try {
            // 1. 构建查询条件
            SettlementQueryDTO query = new SettlementQueryDTO();
            query.setStoreId(storeId);
            if (startDate != null && !startDate.isEmpty()) {
                query.setStartDate(LocalDate.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                query.setEndDate(LocalDate.parse(endDate));
            }
            // 导出时设置较大的分页大小
            query.setCurrent(1);
            query.setSize(10000);

            // 2. 查询数据
            User currentUser = SecurityUtils.getCurrentUser();
            IPage<DailySettlementVO> settlementPage = dailySettlementService.getSettlementList(query, currentUser);

            // 3. 设置响应头
            String fileName = "日结对账_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now()) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));

            // 4. 生成并写入Excel文件
            exportToExcel(settlementPage.getRecords(), response);

        } catch (Exception e) {
            log.error("导出日结对账Excel失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 将日结数据写入Excel响应流
     *
     * @param settlements 日结记录列表
     * @param response    HTTP响应对象
     */
    private void exportToExcel(List<DailySettlementVO> settlements, HttpServletResponse response) throws Exception {
        // 使用Apache POI生成Excel（此处提供核心逻辑框架）
        // 实际项目中建议提取到独立的Excel导出工具类或Service

        /*
         * Excel表头结构：
         * | 结算日期 | 门店ID | 状态 | 订单数 | 总营收(元) | 现金金额 | 现金笔数 |
         * | 微信金额 | 微信笔数 | 支付宝金额 | 支付宝笔数 | 会员余额金额 | 会员余额笔数 |
         * | 优惠金额(元) | 退款金额(元) | 退款笔数 | 作废金额(元) | 作废笔数 | 差异金额(元) | 备注 |
         */

        // TODO: 集成Apache POI实现完整的Excel生成逻辑
        // 建议使用EasyExcel或POI的SXSSF模式处理大数据量导出
        // 当前为占位实现：输出CSV格式
        StringBuilder csvContent = new StringBuilder();
        // 写入BOM头以支持中文
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        response.getOutputStream().write(bom);

        // 表头
        csvContent.append("结算日期,门店ID,状态,订单数,总营收(元),");
        csvContent.append("现金金额(分),现金笔数,");
        csvContent.append("微信金额(分),微信笔数,");
        csvContent.append("支付宝金额(分),支付宝笔数,");
        csvContent.append("会员余额金额(分),会员余额笔数,");
        csvContent.append("优惠金额(分),退款金额(分),退款笔数,");
        csvContent.append("作废金额(分),作废笔数,差异金额(分),备注\n");

        // 数据行
        for (DailySettlementVO vo : settlements) {
            csvContent.append(vo.getSettlementDate()).append(",");
            csvContent.append(vo.getStoreId()).append(",");
            csvContent.append(vo.getStatus()).append(",");
            csvContent.append(vo.getOrderCount()).append(",");
            csvContent.append(vo.getTotalRevenue()).append(",");

            // 支付方式明细
            if (vo.getPaymentBreakdown() != null) {
                appendPaymentDetail(csvContent, vo.getPaymentBreakdown(), "cash");
                appendPaymentDetail(csvContent, vo.getPaymentBreakdown(), "wechat");
                appendPaymentDetail(csvContent, vo.getPaymentBreakdown(), "alipay");
                appendPaymentDetail(csvContent, vo.getPaymentBreakdown(), "memberBalance");
            } else {
                csvContent.append("0,0,0,0,0,0,0,0,");
            }

            // 其他统计字段
            csvContent.append(vo.getTotalDiscountAmountDisplay() != null ? vo.getTotalDiscountAmountDisplay() : "0").append(",");
            csvContent.append(vo.getRefundAmountDisplay() != null ? vo.getRefundAmountDisplay() : "0").append(",");
            // 退款笔数和作废数据需要从VO扩展或Entity获取，这里暂时留空
            csvContent.append(",");
            csvContent.append(vo.getCancelledAmountDisplay() != null ? vo.getCancelledAmountDisplay() : "0").append(",");
            csvContent.append(",");
            csvContent.append(vo.getDifferenceAmount() != null ? vo.getDifferenceAmount() : "0").append(",");
            csvContent.append(vo.getRemark() != null ? vo.getRemark() : "").append("\n");
        }

        // 写入响应流
        response.getOutputStream().write(csvContent.toString().getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    /**
     * 追加单个支付方式的明细到CSV内容
     *
     * @param csv              CSV内容构建器
     * @param paymentBreakdown 支付方式明细Map
     * @param key              支付方式key（cash/wechat/alipay/memberBalance）
     */
    private void appendPaymentDetail(StringBuilder csv, Map<String, DailySettlementVO.PaymentMethodDetail> paymentBreakdown, String key) {
        DailySettlementVO.PaymentMethodDetail detail = paymentBreakdown.get(key);
        if (detail != null) {
            csv.append(detail.getAmount() != null ? detail.getAmount() : "0").append(",");
            csv.append(detail.getCount() != null ? detail.getCount() : "0").append(",");
        } else {
            csv.append("0,0,");
        }
    }

    /** 日志记录器 */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreManagementSettlementController.class);
}
