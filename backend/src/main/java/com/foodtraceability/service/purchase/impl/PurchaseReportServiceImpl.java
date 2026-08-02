package com.foodtraceability.service.purchase.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;
import com.foodtraceability.entity.MaterialArchive;
import com.foodtraceability.entity.MaterialCategory;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.MaterialCategoryMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseStockinMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.service.purchase.PurchaseReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采购报表服务实现
 *
 * <p>核心实现策略：采用 Java 侧聚合（stream + groupingBy），避免 H2/PostgreSQL 方言差异。
 * 数据规模通常较小（单企业采购订单数 < 10000），Java 侧聚合性能可接受。</p>
 *
 * <p>数据来源：
 * <ul>
 *   <li>purchase_orders：订单基础数据（订单数、总金额、付款状态、订单日期、供应商）</li>
 *   <li>purchase_order_items：明细金额、数量、关联物料</li>
 *   <li>suppliers：供应商名称查找</li>
 *   <li>material_archives：物料→分类映射</li>
 *   <li>material_categories：分类名称查找</li>
 *   <li>purchase_stockins：准时交付率、质检合格率（基于入库单）</li>
 * </ul>
 * </p>
 *
 * <p>金额处理：所有金额字段以"分"为单位（Long）返回，前端 DataConverter 转换为"元"。</p>
 */
@Service
public class PurchaseReportServiceImpl implements PurchaseReportService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseReportServiceImpl.class);

    /** 付款状态：2已支付 */
    private static final int PAYMENT_STATUS_PAID = 2;
    /** 订单状态：0草稿 1待审核（视为待处理） */
    private static final int ORDER_STATUS_DRAFT = 0;
    private static final int ORDER_STATUS_PENDING = 1;
    /** 质检结果：1合格 */
    private static final int QC_RESULT_QUALIFIED = 1;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialArchiveMapper materialArchiveMapper;
    private final MaterialCategoryMapper materialCategoryMapper;
    private final PurchaseStockinMapper purchaseStockinMapper;

    public PurchaseReportServiceImpl(PurchaseOrderMapper purchaseOrderMapper,
                                      PurchaseOrderItemMapper purchaseOrderItemMapper,
                                      SupplierMapper supplierMapper,
                                      MaterialArchiveMapper materialArchiveMapper,
                                      MaterialCategoryMapper materialCategoryMapper,
                                      PurchaseStockinMapper purchaseStockinMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.supplierMapper = supplierMapper;
        this.materialArchiveMapper = materialArchiveMapper;
        this.materialCategoryMapper = materialCategoryMapper;
        this.purchaseStockinMapper = purchaseStockinMapper;
    }

    // ==================== 公共方法 ====================

    @Override
    public PurchaseReportSummaryVO getSummary(String startDate, String endDate,
                                               String supplierId, String category) {
        log.debug("获取报表汇总：startDate={}, endDate={}, supplierId={}, category={}",
                startDate, endDate, supplierId, category);

        // 1. 查询符合条件的采购订单（含分类过滤）
        List<PurchaseOrder> orders = queryOrders(startDate, endDate, supplierId, category);
        PurchaseReportSummaryVO vo = new PurchaseReportSummaryVO();

        if (orders.isEmpty()) {
            vo.setTotalPurchaseAmount(0L);
            vo.setTotalOrderCount(0);
            vo.setTotalSettledAmount(0L);
            vo.setTotalUnsettledAmount(0L);
            vo.setAvgOrderAmount(0L);
            vo.setSupplierCount(0);
            vo.setPendingOrders(0);
            vo.setOnTimeDeliveryRate(BigDecimal.ZERO);
            vo.setQualifiedRate(BigDecimal.ZERO);
            return vo;
        }

        // 2. 聚合订单指标
        long totalPurchaseAmount = orders.stream()
                .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0L)
                .sum();
        long totalSettledAmount = orders.stream()
                .filter(o -> o.getPaymentStatus() != null && o.getPaymentStatus() == PAYMENT_STATUS_PAID)
                .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0L)
                .sum();
        long totalUnsettledAmount = totalPurchaseAmount - totalSettledAmount;
        int totalOrderCount = orders.size();
        long avgOrderAmount = totalOrderCount > 0 ? totalPurchaseAmount / totalOrderCount : 0L;

        // 3. 供应商数（去重）
        long supplierCount = orders.stream()
                .map(PurchaseOrder::getSupplierId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        // 4. 待处理订单数（草稿/待审核）
        int pendingOrders = (int) orders.stream()
                .filter(o -> o.getOrderStatus() != null
                        && (o.getOrderStatus() == ORDER_STATUS_DRAFT || o.getOrderStatus() == ORDER_STATUS_PENDING))
                .count();

        // 5. 准时交付率 / 质检合格率（基于入库单）
        BigDecimal[] rates = computeStockinRates(startDate, endDate, supplierId);
        BigDecimal onTimeRate = rates[0];
        BigDecimal qualifiedRate = rates[1];

        vo.setTotalPurchaseAmount(totalPurchaseAmount);
        vo.setTotalOrderCount(totalOrderCount);
        vo.setTotalSettledAmount(totalSettledAmount);
        vo.setTotalUnsettledAmount(totalUnsettledAmount);
        vo.setAvgOrderAmount(avgOrderAmount);
        vo.setSupplierCount((int) supplierCount);
        vo.setPendingOrders(pendingOrders);
        vo.setOnTimeDeliveryRate(onTimeRate);
        vo.setQualifiedRate(qualifiedRate);
        return vo;
    }

    @Override
    public List<PurchaseReportMonthlyItemVO> getMonthlyTrend(String startDate, String endDate,
                                                              String supplierId, String category) {
        log.debug("获取月度趋势：startDate={}, endDate={}, supplierId={}, category={}",
                startDate, endDate, supplierId, category);

        List<PurchaseOrder> orders = queryOrders(startDate, endDate, supplierId, category);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 按月份分组聚合
        Map<String, List<PurchaseOrder>> groupedByMonth = orders.stream()
                .filter(o -> o.getOrderDate() != null)
                .collect(Collectors.groupingBy(o -> o.getOrderDate().format(MONTH_FMT),
                        java.util.TreeMap::new, Collectors.toList()));

        List<PurchaseReportMonthlyItemVO> result = new ArrayList<>(groupedByMonth.size());
        for (Map.Entry<String, List<PurchaseOrder>> entry : groupedByMonth.entrySet()) {
            List<PurchaseOrder> monthOrders = entry.getValue();
            PurchaseReportMonthlyItemVO vo = new PurchaseReportMonthlyItemVO();
            vo.setMonth(entry.getKey());
            vo.setOrderCount(monthOrders.size());
            vo.setPurchaseAmount(monthOrders.stream()
                    .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0L)
                    .sum());
            vo.setSettledAmount(monthOrders.stream()
                    .filter(o -> o.getPaymentStatus() != null && o.getPaymentStatus() == PAYMENT_STATUS_PAID)
                    .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0L)
                    .sum());
            vo.setSupplierCount((int) monthOrders.stream()
                    .map(PurchaseOrder::getSupplierId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<PurchaseReportSupplierItemVO> getSupplierReport(String startDate, String endDate,
                                                                 String supplierId, String category) {
        log.debug("获取供应商维度：startDate={}, endDate={}, supplierId={}, category={}",
                startDate, endDate, supplierId, category);

        List<PurchaseOrder> orders = queryOrders(startDate, endDate, supplierId, category);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 按供应商分组聚合
        Map<Long, List<PurchaseOrder>> groupedBySupplier = orders.stream()
                .filter(o -> o.getSupplierId() != null)
                .collect(Collectors.groupingBy(PurchaseOrder::getSupplierId));

        // 一次性查询所有供应商名称
        Set<Long> supplierIds = new HashSet<>(groupedBySupplier.keySet());
        Map<Long, String> supplierNameMap = lookupSupplierNames(supplierIds);

        // 同时查询每个供应商的入库单聚合（用于准时率/合格率）
        Map<Long, List<PurchaseStockin>> stockinBySupplier = queryStockinsGroupedBySupplier(startDate, endDate, supplierId);

        List<PurchaseReportSupplierItemVO> result = new ArrayList<>(groupedBySupplier.size());
        for (Map.Entry<Long, List<PurchaseOrder>> entry : groupedBySupplier.entrySet()) {
            Long sid = entry.getKey();
            List<PurchaseOrder> supplierOrders = entry.getValue();
            PurchaseReportSupplierItemVO vo = new PurchaseReportSupplierItemVO();
            vo.setSupplierId(sid);
            vo.setSupplierName(supplierNameMap.getOrDefault(sid, "未知供应商"));
            vo.setOrderCount(supplierOrders.size());
            vo.setTotalAmount(supplierOrders.stream()
                    .mapToLong(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0L)
                    .sum());

            // 计算该供应商的准时率/合格率
            List<PurchaseStockin> supplierStockins = stockinBySupplier.getOrDefault(sid, Collections.emptyList());
            BigDecimal[] rates = computeRatesForSupplier(supplierOrders, supplierStockins);
            vo.setOnTimeRate(rates[0]);
            vo.setQualifiedRate(rates[1]);
            result.add(vo);
        }

        // 按采购总额降序排序
        result.sort((a, b) -> Long.compare(
                b.getTotalAmount() != null ? b.getTotalAmount() : 0L,
                a.getTotalAmount() != null ? a.getTotalAmount() : 0L));
        return result;
    }

    @Override
    public List<PurchaseReportCategoryItemVO> getCategoryReport(String startDate, String endDate,
                                                                 String supplierId, String category) {
        log.debug("获取分类维度：startDate={}, endDate={}, supplierId={}, category={}",
                startDate, endDate, supplierId, category);

        List<PurchaseOrder> orders = queryOrders(startDate, endDate, supplierId, category);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询这些订单的所有明细
        Set<Long> orderIds = orders.stream()
                .map(PurchaseOrder::getOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(PurchaseOrderItem::getOrderId, orderIds);
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(itemWrapper);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询物料→分类映射
        Set<Long> materialIds = items.stream()
                .map(PurchaseOrderItem::getMaterialId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> materialToCategory = lookupMaterialCategoryMap(materialIds);

        // 查询分类名称
        Set<Long> categoryIds = new HashSet<>(materialToCategory.values());
        Map<Long, String> categoryNameMap = lookupCategoryNames(categoryIds);

        // 按 categoryId 聚合金额与数量，同时按分类去重统计订单数
        Map<Long, CategoryAggregator> aggregatorMap = new HashMap<>();
        Map<Long, Set<Long>> orderIdsPerCategoryMap = new HashMap<>();
        for (PurchaseOrderItem item : items) {
            Long materialId = item.getMaterialId();
            if (materialId == null) {
                continue;
            }
            Long categoryId = materialToCategory.get(materialId);
            if (categoryId == null) {
                continue;
            }
            CategoryAggregator agg = aggregatorMap.computeIfAbsent(categoryId, k -> new CategoryAggregator());
            long itemAmount = item.getAmount() != null ? item.getAmount() : 0L;
            BigDecimal itemQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            agg.totalAmount += itemAmount;
            agg.totalQuantity = agg.totalQuantity.add(itemQty);
            if (item.getOrderId() != null) {
                orderIdsPerCategoryMap.computeIfAbsent(categoryId, k -> new HashSet<>())
                        .add(item.getOrderId());
            }
        }

        List<PurchaseReportCategoryItemVO> result = new ArrayList<>(aggregatorMap.size());
        for (Map.Entry<Long, CategoryAggregator> entry : aggregatorMap.entrySet()) {
            Long categoryId = entry.getKey();
            CategoryAggregator agg = entry.getValue();
            PurchaseReportCategoryItemVO vo = new PurchaseReportCategoryItemVO();
            vo.setCategoryName(categoryNameMap.getOrDefault(categoryId, "未分类"));
            vo.setTotalAmount(agg.totalAmount);
            vo.setTotalQuantity(agg.totalQuantity);
            Set<Long> catOrderIds = orderIdsPerCategoryMap.get(categoryId);
            vo.setOrderCount(catOrderIds != null ? catOrderIds.size() : 0);
            result.add(vo);
        }

        // 按采购总额降序排序
        result.sort((a, b) -> Long.compare(
                b.getTotalAmount() != null ? b.getTotalAmount() : 0L,
                a.getTotalAmount() != null ? a.getTotalAmount() : 0L));
        return result;
    }

    @Override
    public String exportReportAsCsv(String startDate, String endDate,
                                     String supplierId, String category) {
        log.debug("导出报表 CSV：startDate={}, endDate={}, supplierId={}, category={}",
                startDate, endDate, supplierId, category);

        // 复用月度趋势数据作为 CSV 主体内容
        List<PurchaseReportMonthlyItemVO> monthlyData = getMonthlyTrend(startDate, endDate, supplierId, category);
        PurchaseReportSummaryVO summary = getSummary(startDate, endDate, supplierId, category);

        StringBuilder sb = new StringBuilder();
        // BOM 头确保 Excel 正确识别 UTF-8
        sb.append('\uFEFF');
        sb.append("采购报表\n");
        sb.append("开始日期,").append(startDate != null ? startDate : "全部").append("\n");
        sb.append("结束日期,").append(endDate != null ? endDate : "全部").append("\n");
        sb.append("供应商ID,").append(supplierId != null ? supplierId : "全部").append("\n");
        sb.append("物资分类,").append(category != null ? category : "全部").append("\n");
        sb.append("\n");
        sb.append("汇总指标\n");
        sb.append("总采购金额(分),").append(summary.getTotalPurchaseAmount()).append("\n");
        sb.append("订单总数,").append(summary.getTotalOrderCount()).append("\n");
        sb.append("已结算金额(分),").append(summary.getTotalSettledAmount()).append("\n");
        sb.append("未结算金额(分),").append(summary.getTotalUnsettledAmount()).append("\n");
        sb.append("平均订单金额(分),").append(summary.getAvgOrderAmount()).append("\n");
        sb.append("供应商数,").append(summary.getSupplierCount()).append("\n");
        sb.append("待处理订单数,").append(summary.getPendingOrders()).append("\n");
        sb.append("准时交付率(%),").append(summary.getOnTimeDeliveryRate()).append("\n");
        sb.append("质检合格率(%),").append(summary.getQualifiedRate()).append("\n");
        sb.append("\n");
        sb.append("月度趋势\n");
        sb.append("月份,采购金额(分),订单数,已结算金额(分),供应商数\n");
        for (PurchaseReportMonthlyItemVO m : monthlyData) {
            sb.append(m.getMonth()).append(',')
                    .append(m.getPurchaseAmount()).append(',')
                    .append(m.getOrderCount()).append(',')
                    .append(m.getSettledAmount()).append(',')
                    .append(m.getSupplierCount()).append('\n');
        }
        return sb.toString();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查询符合条件的采购订单列表
     * 应用日期范围、供应商ID、物资分类过滤
     */
    private List<PurchaseOrder> queryOrders(String startDate, String endDate,
                                            String supplierId, String category) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseOrder::getOrderDate, LocalDate.parse(endDate));
        }
        if (StringUtils.hasText(supplierId)) {
            try {
                wrapper.eq(PurchaseOrder::getSupplierId, Long.parseLong(supplierId));
            } catch (NumberFormatException e) {
                log.warn("supplierId 格式非法：{}", supplierId);
            }
        }
        // category 过滤：需要先查询该分类下的物料ID集合，再过滤订单明细
        if (StringUtils.hasText(category)) {
            Set<Long> materialIds = lookupMaterialIdsByCategoryName(category);
            if (materialIds.isEmpty()) {
                // 该分类下无物料，直接返回空
                return Collections.emptyList();
            }
            // 查询包含这些物料的订单ID集合
            LambdaQueryWrapper<PurchaseOrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.select(PurchaseOrderItem::getOrderId)
                    .in(PurchaseOrderItem::getMaterialId, materialIds);
            List<PurchaseOrderItem> matchedItems = purchaseOrderItemMapper.selectList(itemWrapper);
            Set<Long> matchedOrderIds = matchedItems.stream()
                    .map(PurchaseOrderItem::getOrderId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (matchedOrderIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(PurchaseOrder::getOrderId, matchedOrderIds);
        }
        return purchaseOrderMapper.selectList(wrapper);
    }

    /** 根据分类名称查询该分类下的所有物料ID */
    private Set<Long> lookupMaterialIdsByCategoryName(String categoryName) {
        LambdaQueryWrapper<MaterialCategory> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.eq(MaterialCategory::getCategoryName, categoryName);
        List<MaterialCategory> cats = materialCategoryMapper.selectList(catWrapper);
        if (cats.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> categoryIds = cats.stream()
                .map(MaterialCategory::getCategoryId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<MaterialArchive> matWrapper = new LambdaQueryWrapper<>();
        matWrapper.select(MaterialArchive::getMaterialId)
                .in(MaterialArchive::getCategoryId, categoryIds);
        List<MaterialArchive> mats = materialArchiveMapper.selectList(matWrapper);
        return mats.stream()
                .map(MaterialArchive::getMaterialId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 查询供应商ID→名称映射 */
    private Map<Long, String> lookupSupplierNames(Set<Long> supplierIds) {
        if (supplierIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Supplier::getSupplierId, supplierIds);
        List<Supplier> suppliers = supplierMapper.selectList(wrapper);
        return suppliers.stream()
                .collect(Collectors.toMap(Supplier::getSupplierId,
                        s -> s.getSupplierName() != null ? s.getSupplierName() : "未知供应商",
                        (a, b) -> a));
    }

    /** 查询物料ID→分类ID映射 */
    private Map<Long, Long> lookupMaterialCategoryMap(Set<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MaterialArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MaterialArchive::getMaterialId, materialIds);
        List<MaterialArchive> mats = materialArchiveMapper.selectList(wrapper);
        return mats.stream()
                .filter(m -> m.getCategoryId() != null)
                .collect(Collectors.toMap(MaterialArchive::getMaterialId,
                        MaterialArchive::getCategoryId,
                        (a, b) -> a));
    }

    /** 查询分类ID→名称映射 */
    private Map<Long, String> lookupCategoryNames(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MaterialCategory::getCategoryId, categoryIds);
        List<MaterialCategory> cats = materialCategoryMapper.selectList(wrapper);
        return cats.stream()
                .collect(Collectors.toMap(MaterialCategory::getCategoryId,
                        c -> c.getCategoryName() != null ? c.getCategoryName() : "未分类",
                        (a, b) -> a));
    }

    /**
     * 计算指定条件下的准时交付率与质检合格率
     * @return [0]=准时交付率，[1]=质检合格率（均为百分比 BigDecimal，0~100）
     */
    private BigDecimal[] computeStockinRates(String startDate, String endDate, String supplierId) {
        LambdaQueryWrapper<PurchaseStockin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseStockin::getStockinDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseStockin::getStockinDate, LocalDate.parse(endDate));
        }
        if (StringUtils.hasText(supplierId)) {
            try {
                wrapper.eq(PurchaseStockin::getSupplierId, Long.parseLong(supplierId));
            } catch (NumberFormatException e) {
                log.warn("supplierId 格式非法：{}", supplierId);
            }
        }
        List<PurchaseStockin> stockins = purchaseStockinMapper.selectList(wrapper);
        if (stockins.isEmpty()) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }

        // 准时交付率：stockin_date <= 关联订单 expected_date 的比例
        // 需要查询关联订单的 expected_date
        Set<Long> orderIds = stockins.stream()
                .map(PurchaseStockin::getOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, LocalDate> orderExpectedDateMap = Collections.emptyMap();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<PurchaseOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.in(PurchaseOrder::getOrderId, orderIds);
            List<PurchaseOrder> relatedOrders = purchaseOrderMapper.selectList(orderWrapper);
            orderExpectedDateMap = relatedOrders.stream()
                    .filter(o -> o.getExpectedDate() != null)
                    .collect(Collectors.toMap(PurchaseOrder::getOrderId,
                            PurchaseOrder::getExpectedDate,
                            (a, b) -> a));
        }

        long onTimeCount = 0;
        long validStockinCount = 0;
        for (PurchaseStockin s : stockins) {
            if (s.getOrderId() == null || s.getStockinDate() == null) {
                continue;
            }
            LocalDate expected = orderExpectedDateMap.get(s.getOrderId());
            if (expected == null) {
                continue;
            }
            validStockinCount++;
            if (!s.getStockinDate().isAfter(expected)) {
                onTimeCount++;
            }
        }
        BigDecimal onTimeRate = validStockinCount > 0
                ? BigDecimal.valueOf(onTimeCount * 100.0 / validStockinCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 质检合格率：quality_check_result == 1 的比例
        long totalQc = stockins.stream()
                .filter(s -> s.getQualityCheckResult() != null)
                .count();
        long qualifiedCount = stockins.stream()
                .filter(s -> s.getQualityCheckResult() != null && s.getQualityCheckResult() == QC_RESULT_QUALIFIED)
                .count();
        BigDecimal qualifiedRate = totalQc > 0
                ? BigDecimal.valueOf(qualifiedCount * 100.0 / totalQc).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new BigDecimal[]{onTimeRate, qualifiedRate};
    }

    /** 查询入库单并按供应商分组 */
    private Map<Long, List<PurchaseStockin>> queryStockinsGroupedBySupplier(String startDate, String endDate, String supplierId) {
        LambdaQueryWrapper<PurchaseStockin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseStockin::getStockinDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseStockin::getStockinDate, LocalDate.parse(endDate));
        }
        if (StringUtils.hasText(supplierId)) {
            try {
                wrapper.eq(PurchaseStockin::getSupplierId, Long.parseLong(supplierId));
            } catch (NumberFormatException e) {
                log.warn("supplierId 格式非法：{}", supplierId);
            }
        }
        List<PurchaseStockin> stockins = purchaseStockinMapper.selectList(wrapper);
        return stockins.stream()
                .filter(s -> s.getSupplierId() != null)
                .collect(Collectors.groupingBy(PurchaseStockin::getSupplierId));
    }

    /**
     * 计算单个供应商的准时交付率与合格率
     * @return [0]=准时率，[1]=合格率
     */
    private BigDecimal[] computeRatesForSupplier(List<PurchaseOrder> supplierOrders,
                                                   List<PurchaseStockin> supplierStockins) {
        // 准时率：使用订单的 expected_date 与入库单的 stockin_date 比较
        Map<Long, LocalDate> orderExpectedMap = supplierOrders.stream()
                .filter(o -> o.getOrderId() != null && o.getExpectedDate() != null)
                .collect(Collectors.toMap(PurchaseOrder::getOrderId,
                        PurchaseOrder::getExpectedDate,
                        (a, b) -> a));

        long onTimeCount = 0;
        long validCount = 0;
        for (PurchaseStockin s : supplierStockins) {
            if (s.getOrderId() == null || s.getStockinDate() == null) {
                continue;
            }
            LocalDate expected = orderExpectedMap.get(s.getOrderId());
            if (expected == null) {
                continue;
            }
            validCount++;
            if (!s.getStockinDate().isAfter(expected)) {
                onTimeCount++;
            }
        }
        BigDecimal onTimeRate = validCount > 0
                ? BigDecimal.valueOf(onTimeCount * 100.0 / validCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalQc = supplierStockins.stream()
                .filter(s -> s.getQualityCheckResult() != null)
                .count();
        long qualified = supplierStockins.stream()
                .filter(s -> s.getQualityCheckResult() != null && s.getQualityCheckResult() == QC_RESULT_QUALIFIED)
                .count();
        BigDecimal qualifiedRate = totalQc > 0
                ? BigDecimal.valueOf(qualified * 100.0 / totalQc).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new BigDecimal[]{onTimeRate, qualifiedRate};
    }

    /** 分类聚合临时容器（订单数在外部通过 orderIdsPerCategoryMap 去重统计） */
    private static class CategoryAggregator {
        long totalAmount = 0L;
        BigDecimal totalQuantity = BigDecimal.ZERO;
    }
}
