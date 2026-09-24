package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.dto.order.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.event.OrderRefundEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.OrderNewService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.finance.CostRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

/**
 * 璁㈠崟鏈嶅姟瀹炵幇绫? * 绯荤粺鏈€鏍稿績鐨勪笟鍔￠€昏緫灞傦紝绠＄悊璁㈠崟瀹屾暣鐢熷懡鍛ㄦ湡
 *
 * 閲嶆瀯璇存槑锛? * - 宸茬Щ闄?RabbitMQ 渚濊禆锛圧abbitTemplate锛? * - 璁㈠崟鍒涘缓浜嬩欢鏀逛负鏃ュ織璁板綍
 * - 淇濈暀鎵€鏈変笟鍔￠€昏緫鍜屾暟鎹簱鎿嶄綔
 */
@Service
public class OrderNewServiceImpl implements OrderNewService {

    private static final Logger log = LoggerFactory.getLogger(OrderNewServiceImpl.class);
    private static final DateTimeFormatter ORDER_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderNewMapper orderNewMapper;
    private final OrderItemNewMapper orderItemNewMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;
    private final OrderRefundRecordNewMapper orderRefundRecordNewMapper;
    private final DiningTableNewMapper diningTableNewMapper;
    private final FoodNewMapper foodNewMapper;
    private final DishComboNewMapper dishComboNewMapper;
    private final DishRecipeNewMapper dishRecipeNewMapper;
    private final ComboIngredientMapper comboIngredientMapper;
    private final StoreInventoryService storeInventoryService;
    /** POS璁㈠崟Mapper锛堟煡璇?orders_legacy 琛級锛岀敤浜庣鐞嗙鏌ヨPOS鏀堕摱绔鍗?*/
    private final OrderMapper posOrderMapper;
    /** POS璁㈠崟椤筂apper锛堟煡璇?order_items_legacy 琛級 */
    private final OrderItemMapper posOrderItemMapper;
    /** 浜嬩欢鍙戝竷鍣細鐢ㄤ簬鍙戝竷璁㈠崟瀹屾垚浜嬩欢锛岃Е鍙戣储鍔℃敹鍏?鍑瘉绛夊紓姝ヨ仈鍔紙DF-008 淇锛?*/
    private final ApplicationEventPublisher applicationEventPublisher;
    /** 鎴愭湰璁板綍鏈嶅姟锛氱敤浜庢寔涔呭寲璁㈠崟鎴愭湰鍒?cost_record 琛紙DF-009 淇锛?*/
    private final CostRecordService costRecordService;
    /** 璧勯噾娴佹按鏈嶅姟锛團6锛氳鍗曟敮浠樿惤璧勯噾娴佹按锛?*/
    private final com.foodtraceability.service.finance.FundFlowService fundFlowService;
    /** 閾惰璐︽埛鏈嶅姟锛團6锛氬彇榛樿璐︽埛锛?*/
    private final com.foodtraceability.service.finance.BankAccountService bankAccountService;

    public OrderNewServiceImpl(
            OrderNewMapper orderNewMapper,
            OrderItemNewMapper orderItemNewMapper,
            OrderPaymentRecordNewMapper orderPaymentRecordNewMapper,
            OrderRefundRecordNewMapper orderRefundRecordNewMapper,
            DiningTableNewMapper diningTableNewMapper,
            FoodNewMapper foodNewMapper,
            DishComboNewMapper dishComboNewMapper,
            DishRecipeNewMapper dishRecipeNewMapper,
            ComboIngredientMapper comboIngredientMapper,
            StoreInventoryService storeInventoryService,
            OrderMapper posOrderMapper,
            OrderItemMapper posOrderItemMapper,
            ApplicationEventPublisher applicationEventPublisher,
            CostRecordService costRecordService,
            com.foodtraceability.service.finance.FundFlowService fundFlowService,
            com.foodtraceability.service.finance.BankAccountService bankAccountService) {
        this.orderNewMapper = orderNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.orderPaymentRecordNewMapper = orderPaymentRecordNewMapper;
        this.orderRefundRecordNewMapper = orderRefundRecordNewMapper;
        this.diningTableNewMapper = diningTableNewMapper;
        this.foodNewMapper = foodNewMapper;
        this.dishComboNewMapper = dishComboNewMapper;
        this.dishRecipeNewMapper = dishRecipeNewMapper;
        this.comboIngredientMapper = comboIngredientMapper;
        this.storeInventoryService = storeInventoryService;
        this.posOrderMapper = posOrderMapper;
        this.posOrderItemMapper = posOrderItemMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.costRecordService = costRecordService;
        this.fundFlowService = fundFlowService;
        this.bankAccountService = bankAccountService;
    }

    // ==================== 璁㈠崟鍒涘缓 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO createDTO) {
        log.info("寮€濮嬪垱寤鸿鍗? 绫诲瀷: {}", createDTO.getOrderType());

        // 1. 鏍￠獙鍫傞璁㈠崟鐨勬鍙扮姸鎬?        if (createDTO.getOrderType() != null && createDTO.getOrderType() == 1 && createDTO.getTableId() != null) {
            validateTableAvailable(createDTO.getTableId());
        }

        // 2. 鏍￠獙鑿滃搧/濂楅骞惰绠楅噾棰?        OrderAmountCalculation calculation = calculateOrderAmount(createDTO.getItems());

        // 3. 鐢熸垚璁㈠崟缂栧彿
        String orderCode = generateOrderCode();

        // 4. 鍒涘缓璁㈠崟涓昏〃
        OrderNew order = buildOrderEntity(createDTO, calculation, orderCode);
        orderNewMapper.insert(order);

        // 5. 鍒涘缓璁㈠崟鏄庣粏
        saveOrderItems(order.getOrderId(), createDTO.getItems());

        // 6. 鍫傞璁㈠崟閿佸畾妗屽彴
        if (createDTO.getOrderType() != null && createDTO.getOrderType() == 1 && createDTO.getTableId() != null) {
            diningTableNewMapper.lockTable(createDTO.getTableId(), order.getOrderId());
        }

        log.info("璁㈠崟鍒涘缓鎴愬姛, 缂栧彿: {}, ID: {}", orderCode, order.getOrderId());

        // 閲嶆瀯璇存槑锛氬凡绉婚櫎 RabbitMQ锛岃鍗曞垱寤轰簨浠舵敼涓烘棩蹇楄褰?        log.info("璁㈠崟鍒涘缓浜嬩欢锛歰rderId={}, orderCode={}, orderType={}, eventType=ORDER_CREATED",
                order.getOrderId(), orderCode, createDTO.getOrderType());

        return getOrderDetail(order.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createPosQuickOrder(PosQuickOrderDTO quickOrderDTO) {
        log.info("POS蹇€熶笅鍗? 绫诲瀷: {}", quickOrderDTO.getOrderType());

        // 灏嗗揩閫熶笅鍗曢」杞崲涓烘爣鍑嗘槑缁嗘牸寮?        // 娉ㄦ剰锛歱roductName/unitPrice 鐢?createOrder 鍐呴儴 validateAndPriceFood 缁熶竴鏍￠獙鍙栦环锛?        // 姝ゅ涓嶉鏌ワ紝閬垮厤閲嶅 DB 璁块棶 + 闃叉瀹㈡埛绔鏀逛环鏍?        List<OrderCreateDTO.OrderItemCreateDTO> items = quickOrderDTO.getItems().stream()
                .map(item -> {
                    OrderCreateDTO.OrderItemCreateDTO dto = new OrderCreateDTO.OrderItemCreateDTO();
                    dto.setProductType(item.getProductType());
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    dto.setSpecification(item.getSpecification());
                    dto.setRemark(item.getRemark());
                    return dto;
                })
                .collect(Collectors.toList());

        // 鏋勫缓瀹屾暣鍒涘缓DTO
        OrderCreateDTO createDTO = new OrderCreateDTO();
        createDTO.setOrderType(quickOrderDTO.getOrderType());
        createDTO.setStoreId(quickOrderDTO.getStoreId());
        createDTO.setTableId(quickOrderDTO.getTableId());
        createDTO.setDiningPeopleCount(quickOrderDTO.getDiningPeopleCount());
        createDTO.setCustomerPhone(quickOrderDTO.getCustomerPhone());
        createDTO.setItems(items);
        createDTO.setRemark(quickOrderDTO.getRemark());

        return createOrder(createDTO);
    }

    // ==================== 鏀粯澶勭悊 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(String orderId, OrderPayDTO payDTO) {
        log.info("寮€濮嬫敮浠樿鍗? orderId: {}", orderId);

        OrderNew order = getAndValidateOrder(orderId);

        // 鏍￠獙璁㈠崟鐘舵€侊細蹇呴』鏄緟纭鎴栧凡纭涓旀湭瀹屽叏鏀粯
        if (!isPayableStatus(order.getOrderStatus())) {
            throw new RuntimeException("褰撳墠璁㈠崟鐘舵€佷笉鍏佽鏀粯");
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            throw new RuntimeException("璁㈠崟宸叉敮浠樺畬鎴?);
        }

        // 璁＄畻鎬绘敮浠橀噾棰?        long totalPayAmount = payDTO.getPayments().stream()
                .mapToLong(p -> p.getAmount())
                .sum();

        // VULN-06 淇锛氭牎楠屾瘡绗旀敮浠橀噾棰濆繀椤讳负姝ｆ暟锛岄槻姝㈣礋鏁版敮浠樼粫杩囨€婚噾棰濇牎楠?        // 鏀诲嚮鍦烘櫙锛氭彁浜?+1000 鍜?-900 涓ょ瑪锛屾€诲拰 100 鍏冪粫杩囨牎楠岋紝浣嗛€犳垚鏁版嵁娣蜂贡
        for (OrderPayDTO.PaymentDetail payment : payDTO.getPayments()) {
            if (payment.getAmount() <= 0) {
                throw new RuntimeException("鍗曠瑪鏀粯閲戦蹇呴』澶т簬0");
            }
        }

        // 楠岃瘉鏀粯閲戦涓嶈兘瓒呰繃搴斾粯閲戦
        long remainingAmount = order.getFinalAmount() - (order.getPaidAmount() != null ? order.getPaidAmount() : 0L);
        if (totalPayAmount > remainingAmount) {
            throw new RuntimeException("鏀粯閲戦瓒呰繃搴斾粯閲戦");
        }

        // 淇濆瓨姣忕瑪鏀粯璁板綍
        for (OrderPayDTO.PaymentDetail payment : payDTO.getPayments()) {
            OrderPaymentRecordNew record = new OrderPaymentRecordNew();
            record.setOrderId(orderId);
            record.setPaymentMethod(payment.getPaymentMethod());
            record.setPaymentAmount(payment.getAmount());
            record.setTransactionNo(payment.getTransactionNo());
            record.setPaymentTime(LocalDateTime.now());
            record.setOperatorId(payDTO.getOperatorId());
            record.setRemark(payDTO.getRemark());
            orderPaymentRecordNewMapper.insert(record);
        }

        // 鏇存柊璁㈠崟鏀粯鐘舵€?        long newPaidAmount = (order.getPaidAmount() != null ? order.getPaidAmount() : 0L) + totalPayAmount;
        int newPaymentStatus = (newPaidAmount >= order.getFinalAmount()) ? 2 : 1; // 2宸叉敮浠?1閮ㄥ垎鏀粯

        orderNewMapper.updatePaymentStatus(orderId, newPaymentStatus, newPaidAmount);

        // 濡傛灉鍏ㄩ鏀粯锛屾洿鏂拌鍗曠姸鎬佷负宸茬‘璁わ紙鍒朵綔涓級
        if (newPaymentStatus == 2 && (order.getOrderStatus() == null || order.getOrderStatus() == 0)) {
            orderNewMapper.updateStatus(orderId, 1); // 寰呯‘璁?>宸茬‘璁?        }

        // F6: 璁㈠崟鏀粯钀借祫閲戞祦姘达紙鏀跺叆/閿€鍞敹娆撅級
        recordPaymentFundFlow(order, totalPayAmount);

        log.info("璁㈠崟鏀粯瀹屾垚, orderId: {}, 鏀粯閲戦: {}", orderId, totalPayAmount);
        return getOrderDetail(orderId);
    }

    /**
     * F6锛氳鍗曟敮浠樺悗鍒涘缓璧勯噾娴佹按锛堟敹鍏?閿€鍞敹娆撅級锛岃璧勯噾閾惧彲瑙佽鍗曟敹鍏ャ€?     * 鏃犲彲鐢ㄩ摱琛岃处鎴锋椂璺宠繃锛堣褰曟棩蹇楋級锛涘け璐ラ殧绂讳笉褰卞搷鏀粯涓绘祦绋嬨€?     */
    private void recordPaymentFundFlow(OrderNew order, long amount) {
        try {
            if (fundFlowService == null || amount <= 0) {
                return;
            }
            Long accountId = getDefaultBankAccountId();
            if (accountId == null) {
                log.info("F6 鏃犲彲鐢ㄩ摱琛岃处鎴凤紝璺宠繃璁㈠崟鏀粯璧勯噾娴佹按锛歰rderId={}", order.getOrderId());
                return;
            }
            com.foodtraceability.dto.finance.FundFlowCreateDTO dto =
                    new com.foodtraceability.dto.finance.FundFlowCreateDTO();
            dto.setAccountId(accountId);
            dto.setFlowDirection(1); // 鏀跺叆
            dto.setFlowCategory(1);  // 閿€鍞敹娆?            dto.setAmount(amount);
            dto.setCounterpartyName(order.getCustomerName() != null ? order.getCustomerName() : "鏁ｅ");
            dto.setBusinessDate(java.time.LocalDate.now());
            dto.setRemark("璁㈠崟鏀粯 - " + (order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId()));
            fundFlowService.create(dto);
            log.info("F6 璁㈠崟鏀粯璧勯噾娴佹按宸插垱寤猴細orderId={}, amount={}鍒?, order.getOrderId(), amount);
        } catch (Exception e) {
            log.error("F6 璁㈠崟鏀粯璧勯噾娴佹按鍒涘缓澶辫触锛歰rderId={}, 閿欒={}", order.getOrderId(), e.getMessage());
        }
    }

    /** 鍙栭粯璁ら摱琛岃处鎴凤紙绗竴涓惎鐢ㄧ殑锛夛紝鏃犲垯杩斿洖 null */
    private Long getDefaultBankAccountId() {
        try {
            java.util.List<com.foodtraceability.entity.finance.BankAccount> accounts = bankAccountService.list();
            for (com.foodtraceability.entity.finance.BankAccount account : accounts) {
                if (account.getStatus() == null || account.getStatus() == 1) {
                    return account.getAccountId();
                }
            }
        } catch (Exception e) {
            log.warn("F6 鏌ヨ榛樿閾惰璐︽埛澶辫触锛歿}", e.getMessage());
        }
        return null;
    }

    // ==================== 鍙栨秷璁㈠崟 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(String orderId, String cancelReason) {
        log.info("鍙栨秷璁㈠崟, orderId: {}, 鍘熷洜: {}", orderId, cancelReason);

        OrderNew order = getAndValidateOrder(orderId);

        // 鏍￠獙鏄惁鍙互鍙栨秷锛氬彧鏈夊緟纭鍜屽凡纭鐘舵€佺殑璁㈠崟鍙互鍙栨秷
        Integer status = order.getOrderStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("褰撳墠璁㈠崟鐘舵€佷笉鍏佽鍙栨秷");
        }

        // 濡傛灉宸叉敮浠橈紝闇€瑕侀€€娆鹃€昏緫
        if (order.getPaymentStatus() != null && order.getPaymentStatus() >= 1) {
            // TODO: 璋冪敤閫€娆炬湇鍔″鐞嗚嚜鍔ㄩ€€娆?            log.warn("璁㈠崟宸叉敮浠橈紝闇€瑕佸鐞嗛€€娆鹃€昏緫, orderId: {}", orderId);
        }

        // 鏇存柊璁㈠崟鐘舵€佷负宸插彇娑?        orderNewMapper.updateStatus(orderId, 3); // 3=宸插彇娑?
        // 瑙ｉ攣妗屽彴
        diningTableNewMapper.unlockTableByOrderId(orderId);

        log.info("璁㈠崟鍙栨秷鎴愬姛, orderId: {}", orderId);
        return getOrderDetail(orderId);
    }

    // ==================== 閫€娆惧鐞?====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.OrderRefundRecordVO applyRefund(String orderId, OrderRefundDTO refundDTO) {
        log.info("鐢宠閫€娆? orderId: {}, 绫诲瀷: {}", orderId, refundDTO.getRefundType());

        OrderNew order = getAndValidateOrder(orderId);

        // 鏍￠獙閫€娆剧被鍨嬪拰閲戦
        Long refundAmount = refundDTO.getRefundType() == 1 ? order.getFinalAmount() : refundDTO.getRefundAmount();

        // 鍒涘缓閫€娆捐褰?        OrderRefundRecordNew refundRecord = new OrderRefundRecordNew();
        refundRecord.setOrderId(orderId);
        refundRecord.setRefundType(refundDTO.getRefundType());
        refundRecord.setRefundAmount(refundAmount);
        refundRecord.setRefundReason(refundDTO.getRefundReason());
        refundRecord.setRefundMethod(refundDTO.getRefundMethod());
        refundRecord.setRefundStatus(0); // 0=寰呭鏍?        // DF-024 淇锛氫繚瀛橀€€娆炬槑缁咺D鍒楄〃锛圕SV鏍煎紡锛夛紝鐢ㄤ簬閮ㄥ垎閫€娆剧簿纭洖琛ュ簱瀛?        refundRecord.setPaymentChannelStatus(0); // 0=鏈彁浜?        refundRecord.setRefundItemIds(convertRefundItemIdsToCsv(refundDTO.getItemIds()));
        orderRefundRecordNewMapper.insert(refundRecord);

        // 濡傛灉鏄叏棰濋€€娆撅紝鐩存帴鏇存柊璁㈠崟鐘舵€?        if (refundDTO.getRefundType() == 1) {
            int newOrderStatus = 5; // 鍏ㄩ閫€娆?            orderNewMapper.addRefundAmount(orderId, refundAmount, newOrderStatus);
        } else {
            // 閮ㄥ垎閫€娆撅紝鏇存柊涓洪儴鍒嗛€€娆剧姸鎬?            orderNewMapper.addRefundAmount(orderId, refundAmount, 4); // 4=閮ㄥ垎閫€娆?        }

        log.info("閫€娆剧敵璇锋彁浜ゆ垚鍔? refundId: {}", refundRecord.getRefundId());
        return convertToRefundVO(refundRecord);
    }

    /**
     * 灏嗛€€娆炬槑缁咺D鍒楄〃杞崲涓篊SV瀛楃涓?     * @param itemIds 閫€娆炬槑缁咺D鍒楄〃
     * @return CSV瀛楃涓诧紙濡?"id1,id2,id3"锛夛紝绌哄垪琛ㄨ繑鍥?null
     */
    private String convertRefundItemIdsToCsv(java.util.List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }
        return String.join(",", itemIds);
    }

    /**
     * 灏咰SV瀛楃涓茶В鏋愪负閫€娆炬槑缁咺D鍒楄〃
     * @param csv 閫€娆炬槑缁咺D CSV瀛楃涓?     * @return 閫€娆炬槑缁咺D鍒楄〃锛岀┖CSV杩斿洖绌哄垪琛?     */
    private java.util.List<String> parseRefundItemIdsFromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long refundId, boolean approved, Long approveUserId) {
        log.info("瀹℃壒閫€娆? refundId: {}, 鍚屾剰: {}", refundId, approved);

        OrderRefundRecordNew refundRecord = orderRefundRecordNewMapper.selectById(refundId);
        if (refundRecord == null) {
            throw new RuntimeException("閫€娆捐褰曚笉瀛樺湪");
        }

        if (approved) {
            // DF-022 淇锛氬疄鐜?閫€娆捐褰曟寔涔呭寲"鏂规
            // 鐢变簬鏆傛湭鎺ュ叆鐪熷疄鏀粯娓犻亾 SDK锛屾湰娆￠噰鐢?閫€娆捐褰曟寔涔呭寲"鏂规锛?            //   1. 鏍囪閫€娆捐褰曚负"宸查€€娆?锛坮efund_status=3锛?            //   2. 鏍囪鏀粯娓犻亾鐘舵€佷负"寰呮笭閬撳鐞?锛坧ayment_channel_status=1锛?            //   3. 璁板綍娓犻亾鎻愪氦鏃堕棿
            //   4. 鍚庣画鎺ュ叆鐪熷疄鏀粯 SDK 鏃讹紝鍐嶅疄鐜板疄闄呴€€娆撅紙鏇存柊 payment_channel_status 涓烘垚鍔?澶辫触锛?            refundRecord.setApproveUserId(approveUserId);
            refundRecord.setRefundStatus(3); // 3=宸查€€娆?            refundRecord.setPaymentChannelStatus(1); // 1=寰呮笭閬撳鐞?            refundRecord.setPaymentChannelSubmitTime(LocalDateTime.now());
            refundRecord.setCompleteTime(LocalDateTime.now());

            // 閫€娆鹃€氳繃鏃跺洖琛ュ簱瀛橈紙浠呭綋璁㈠崟宸插畬鎴愪笖鏈夊叧鑱旈棬搴楁椂锛?            OrderNew order = orderNewMapper.selectById(refundRecord.getOrderId());
            if (order != null && order.getStoreId() != null
                    && order.getOrderStatus() != null && order.getOrderStatus() == 2) {
                // DF-023 淇锛氬簱瀛樺洖琛ュけ璐ュ繀椤绘姏鍑哄紓甯稿洖婊氫富浜嬪姟
                // 鍘熷疄鐜?try/catch 闈欓粯鍚炲紓甯革紝瀵艰嚧閫€娆惧凡瀹℃壒浣嗗簱瀛樻湭鍥炶ˉ鐨勬暟鎹笉涓€鑷?                // 鐜版敼涓猴細搴撳瓨鍥炶ˉ澶辫触鎶涘嚭寮傚父 鈫?涓讳簨鍔″洖婊?鈫?閫€娆捐褰曚笉鏇存柊涓?宸查€€娆?
                restoreInventoryForRefund(order, refundRecord);
            }

            // 鍙戝竷閫€娆句簨浠讹紙DF-022 淇锛夛細瑙﹀彂涓嬫父璐㈠姟鑱斿姩
            // 鐩戝惉鍣?OrderRefundEventListener 浼氬湪涓讳簨鍔℃彁浜ゅ悗锛圓FTER_COMMIT锛夊紓姝ワ細
            //   1. 鐢熸垚閫€娆炬敮鍑鸿储鍔℃祦姘达紙finance_record锛?            //   2. 锛堟湭鏉ワ級鐢熸垚绾㈠瓧鍑瘉鍏宠仈鍘熼攢鍞嚟璇?            publishOrderRefundEvent(order, refundRecord);
        } else {
            refundRecord.setRefundStatus(2); // 2=宸叉嫆缁?            refundRecord.setApproveUserId(approveUserId);
        }

        orderRefundRecordNewMapper.updateById(refundRecord);
    }

    /**
     * 鍙戝竷璁㈠崟閫€娆句簨浠?     * 瑙﹀彂涓嬫父璐㈠姟鑱斿姩锛氶€€娆炬敮鍑烘祦姘淬€佺孩瀛楀嚟璇佺瓑
     *
     * @param order 鍏宠仈璁㈠崟锛堝彲鑳戒负 null锛屽璁㈠崟宸茶鍒犻櫎锛?     * @param refundRecord 閫€娆捐褰?     */
    private void publishOrderRefundEvent(OrderNew order, OrderRefundRecordNew refundRecord) {
        try {
            OrderRefundEvent event = new OrderRefundEvent();
            event.setRefundId(refundRecord.getRefundId());
            event.setOrderId(refundRecord.getOrderId());
            event.setOrderNumber(order != null ? order.getOrderCode() : null);
            event.setStoreId(order != null ? order.getStoreId() : null);
            event.setRefundType(refundRecord.getRefundType());
            event.setRefundAmount(refundRecord.getRefundAmount());
            event.setRefundReason(refundRecord.getRefundReason());
            event.setRefundMethod(refundRecord.getRefundMethod());
            event.setApproveUserId(refundRecord.getApproveUserId());
            event.setRefundItemIds(parseRefundItemIdsFromCsv(refundRecord.getRefundItemIds()));
            event.setPaymentChannelStatus(refundRecord.getPaymentChannelStatus());
            event.setCompleteTime(refundRecord.getCompleteTime());

            applicationEventPublisher.publishEvent(event);
            log.info("宸插彂甯冭鍗曢€€娆句簨浠? refundId={}, orderId={}",
                    refundRecord.getRefundId(), refundRecord.getOrderId());
        } catch (Exception e) {
            // 浜嬩欢鍙戝竷澶辫触浠呰褰曟棩蹇楋紝涓嶅奖鍝嶄富浜嬪姟
            // 锛堜笌椤圭洰鐜版湁浜嬩欢鍙戝竷妯″紡淇濇寔涓€鑷达紝DF-038 寰呭悗缁紩鍏ヤ簨浠舵寔涔呭寲鏈哄埗鍚庣粺涓€瑙ｅ喅锛?            log.error("鍙戝竷璁㈠崟閫€娆句簨浠跺け璐? refundId={}, 閿欒={}",
                    refundRecord.getRefundId(), e.getMessage(), e);
        }
    }

    // ==================== 璁㈠崟鐘舵€佸彉鏇?====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(String orderId) {
        log.info("纭璁㈠崟, orderId: {}", orderId);
        OrderNew order = getAndValidateOrder(orderId);
        if (order.getOrderStatus() != null && order.getOrderStatus() != 0) {
            throw new RuntimeException("鍙湁寰呯‘璁ょ姸鎬佺殑璁㈠崟鎵嶈兘纭");
        }
        orderNewMapper.updateStatus(orderId, 1); // 1=宸茬‘璁?鍒朵綔涓?    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(String orderId) {
        log.info("瀹屾垚璁㈠崟, orderId: {}", orderId);
        OrderNew order = getAndValidateOrder(orderId);
        if (order.getOrderStatus() == null || (order.getOrderStatus() != 1 && order.getOrderStatus() != 2)) {
            throw new RuntimeException("褰撳墠璁㈠崟鐘舵€佹棤娉曟爣璁板畬鎴?);
        }

        // 鎵ｅ噺闂ㄥ簵搴撳瓨骞剁粨杞垚鏈紙浠呭綋璁㈠崟鏈夊叧鑱旈棬搴楁椂锛?        // DF-009 淇锛氬師 totalCost 浠?log 鍚庝涪寮冿紝鐜拌繑鍥炲苟鎸佷箙鍖栧埌 cost_record 琛?        long totalCost = 0L;
        if (order.getStoreId() != null) {
            totalCost = deductInventoryAndCalculateCost(order);
            // 鍚屾鎸佷箙鍖栬鍗曟垚鏈埌 cost_record 琛紙鍚屼簨鍔″己涓€鑷达紝纭繚鎴愭湰鏁版嵁涓嶄涪澶憋級
            persistOrderCost(order, totalCost);
        }

        orderNewMapper.updateStatus(orderId, 2); // 2=宸插畬鎴?        // 瑙ｉ攣妗屽彴
        diningTableNewMapper.unlockTableByOrderId(orderId);

        // DF-008 淇锛氬師 completeOrder 鏈彂甯?OrderCompletedEvent锛屽鑷磋鍗曞畬鎴愨啋璐㈠姟閾捐矾瀹屽叏鏂
        // 鐩戝惉鍣?OrderCompletedEventListener 浣跨敤 @TransactionalEventListener(AFTER_COMMIT) + @Async
        // 寮傚父闅旂锛屽彂甯冨け璐ヤ笉褰卞搷涓讳簨鍔?        publishOrderCompletedEvent(order, totalCost);
    }

    /**
     * 鎸佷箙鍖栬鍗曟垚鏈埌 cost_record 琛紙DF-009 淇锛?     * 鍚屾鍚屼簨鍔″啓鍏ワ紝纭繚鎴愭湰鏁版嵁涓嶄涪澶?     * @param order 璁㈠崟瀹炰綋
     * @param totalCost 璁㈠崟鎬绘垚鏈紙鍗曚綅锛氬垎锛?     */
    private void persistOrderCost(OrderNew order, long totalCost) {
        if (totalCost <= 0) {
            log.info("璁㈠崟鎴愭湰涓?锛岃烦杩囨寔涔呭寲, orderId: {}", order.getOrderId());
            return;
        }
        try {
            CostRecordCreateDTO dto = new CostRecordCreateDTO();
            dto.setCostType(1); // 1-椋熸潗鎴愭湰
            dto.setCostCenterId(order.getStoreId());
            dto.setAmount(totalCost); // 鍗曚綅锛氬垎
            dto.setPeriod(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            dto.setRemark("璁㈠崟鏉愭枡鎴愭湰 - " + order.getOrderCode());
            dto.setCalculationMethod(1); // 1-瀹為檯鍙戠敓
            costRecordService.create(dto);
            log.info("璁㈠崟鎴愭湰宸叉寔涔呭寲, orderId: {}, totalCost: {}鍒?, order.getOrderId(), totalCost);
        } catch (Exception e) {
            log.error("鎸佷箙鍖栬鍗曟垚鏈け璐? orderId: {}, error: {}", order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("鎸佷箙鍖栬鍗曟垚鏈け璐? " + e.getMessage(), e);
        }
    }

    /**
     * 鍙戝竷璁㈠崟瀹屾垚浜嬩欢锛圖F-008 淇锛?     * 瑙﹀彂 OrderCompletedEventListener 鑱斿姩鐢熸垚璐㈠姟鏀跺叆璁板綍绛?     * 寮傚父闅旂锛屽彂甯冨け璐ヤ笉褰卞搷涓讳簨鍔?     * @param order 璁㈠崟瀹炰綋
     * @param totalCost 璁㈠崟鎬绘垚鏈紙鍗曚綅锛氬垎锛?     */
    private void publishOrderCompletedEvent(OrderNew order, long totalCost) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent();
            event.setEventId("EVT" + System.currentTimeMillis());
            event.setOrderId(order.getOrderId());
            event.setOrderNumber(order.getOrderCode());
            event.setOrderType(order.getOrderType());
            event.setStoreId(order.getStoreId());
            event.setCompleteTime(LocalDateTime.now());
            event.setOrderTime(order.getCreateTime());

            // 閲戦锛氳鍗曡〃瀛樺垎锛圠ong锛夛紝浜嬩欢璇箟涓哄厓锛圔igDecimal锛?            Long finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : 0L;
            BigDecimal amountYuan = BigDecimal.valueOf(finalAmount)
                    .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
            event.setOrderAmount(amountYuan);
            event.setActualAmount(amountYuan);

            // 鎴愭湰锛歵otalCost 鏄垎锛坙ong锛夛紝浜嬩欢璇箟涓哄厓锛圔igDecimal锛?            // 娉ㄦ剰锛氫笉璁剧疆 materialCost 瀛楁锛岄伩鍏嶇洃鍚櫒閲嶅璁板綍鎴愭湰锛堟垚鏈凡鍦?persistOrderCost 涓悓姝ユ寔涔呭寲锛?            if (totalCost > 0) {
                BigDecimal costYuan = BigDecimal.valueOf(totalCost)
                        .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
                event.setTotalCost(costYuan);
            }

            applicationEventPublisher.publishEvent(event);
            log.info("宸插彂甯冭鍗曞畬鎴愪簨浠? orderId={}, totalCost={}鍒?, order.getOrderId(), totalCost);
        } catch (Exception e) {
            log.error("鍙戝竷璁㈠崟瀹屾垚浜嬩欢澶辫触: orderId={}, error={}", order.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 鎵ｅ噺闂ㄥ簵搴撳瓨骞惰绠楄鍗曟垚鏈?     * 鎸夎彍鍝丅OM閰嶆柟灞曞紑锛屾墸鍑忔瘡绉嶅師鏂欑殑搴撳瓨鏁伴噺锛屽悓鏃剁粨杞搴旂殑鎴愭湰
     * @return 璁㈠崟鎬绘垚鏈紙鍗曚綅锛氬垎锛?     */
    private long deductInventoryAndCalculateCost(OrderNew order) {
        Long storeId = order.getStoreId();
        List<OrderItemNew> orderItems = getOrderItems(order.getOrderId());

        // 鏀堕泦鎵€鏈夐渶瑕佹墸鍑忕殑鍘熸枡鍙婂叾鏁伴噺锛堟寜鍘熸枡ID姹囨€伙級
        Map<Long, BigDecimal> materialDeductionMap = new HashMap<>();
        Map<Long, String> materialNameMap = new HashMap<>();

        for (OrderItemNew item : orderItems) {
            // 璺宠繃宸查€€娆剧殑鑿滃搧
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                continue;
            }

            Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;

            if (item.getProductType() != null && item.getProductType() == 1) {
                // 鍗曞搧锛氭煡鎵鹃厤鏂?                if (item.getFoodId() != null) {
                    addFoodMaterialDeductions(
                            item.getFoodId(),
                            quantity,
                            materialDeductionMap,
                            materialNameMap);
                }
            } else if (item.getProductType() != null && item.getProductType() == 2) {
                // 濂楅锛氬睍寮€濂楅鍐呯殑鎵€鏈夎彍鍝侊紝鍐嶆寜鑿滃搧閰嶆柟鎵ｅ噺
                if (item.getFoodId() != null) {
                    List<ComboIngredient> comboIngredients = comboIngredientMapper.selectByComboId(item.getFoodId());
                    for (ComboIngredient ingredient : comboIngredients) {
                        if (ingredient.getFoodId() == null) continue;
                        BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                                ? ingredient.getQuantity() : BigDecimal.ONE;
                        // 濂楅涓彍鍝佺殑瀹為檯鏁伴噺 = 濂楅涓鑿滃搧鏁伴噺 脳 濂楅璐拱鏁伴噺
                        int actualFoodQty = foodQtyInCombo.multiply(new BigDecimal(quantity)).intValue();
                        if (actualFoodQty <= 0) continue;

                        try {
                            Long foodIdLong = Long.parseLong(ingredient.getFoodId());
                            addFoodMaterialDeductions(
                                    foodIdLong,
                                    actualFoodQty,
                                    materialDeductionMap,
                                    materialNameMap);
                        } catch (NumberFormatException e) {
                            log.warn("濂楅鑿滃搧ID鏍煎紡寮傚父, comboId: {}, foodId: {}", item.getFoodId(), ingredient.getFoodId());
                        }
                    }
                }
            }
        }

        // 鎵ц搴撳瓨鎵ｅ噺
        long totalCost = 0L;
        for (Map.Entry<Long, BigDecimal> entry : materialDeductionMap.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal qty = entry.getValue();
            String materialName = materialNameMap.get(materialId);

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            try {
                // 鎵ｅ噺搴撳瓨锛岃繑鍥炲嚭搴撴垚鏈紙鍒嗭級
                Long cost = storeInventoryService.decreaseStock(
                        String.valueOf(storeId),
                        materialId,
                        qty,
                        2,
                        "璁㈠崟閿€鍞嚭搴?- 璁㈠崟:" + order.getOrderCode());
                if (cost != null) {
                    totalCost += cost;
                }
                log.info("璁㈠崟鎵ｅ噺搴撳瓨, orderId: {}, materialId: {}, materialName: {}, qty: {}, cost: {}",
                        order.getOrderId(), materialId, materialName, qty, cost);
            } catch (Exception e) {
                log.error("璁㈠崟鎵ｅ噺搴撳瓨澶辫触, orderId: {}, materialId: {}, error: {}",
                        order.getOrderId(), materialId, e.getMessage());
                throw new RuntimeException("搴撳瓨鎵ｅ噺澶辫触锛? + materialName + " - " + e.getMessage(), e);
            }
        }

        log.info("璁㈠崟鎴愭湰璁＄畻瀹屾垚, orderId: {}, totalCost: {}鍒?, order.getOrderId(), totalCost);
        return totalCost;
    }

    /**
     * 鑾峰彇璁㈠崟鏄庣粏鍒楄〃
     */
    private List<OrderItemNew> getOrderItems(String orderId) {
        LambdaQueryWrapper<OrderItemNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItemNew::getOrderId, orderId);
        return orderItemNewMapper.selectList(wrapper);
    }

    /**
     * 鑾峰彇鑿滃搧閰嶆柟鍒楄〃
     */
    private List<DishRecipeNew> getDishRecipes(Long foodId) {
        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipeNew::getFoodId, foodId);
        return dishRecipeNewMapper.selectList(wrapper);
    }

    /**
     * 绱姞鏌愪釜鑿滃搧鐨勫師鏂欐墸鍑忛渶姹?     * @param foodId 鑿滃搧ID
     * @param quantity 鑿滃搧鏁伴噺
     * @param materialDeductionMap 鍘熸枡鎵ｅ噺姹囨€籑ap锛堢疮鍔狅級
     * @param materialNameMap 鍘熸枡鍚嶇ОMap
     */
    private void addFoodMaterialDeductions(
            Long foodId,
            int quantity,
            Map<Long, BigDecimal> materialDeductionMap,
            Map<Long, String> materialNameMap) {
        List<DishRecipeNew> recipes = getDishRecipes(foodId);
        for (DishRecipeNew recipe : recipes) {
            Long materialId = recipe.getMaterialId();
            if (materialId == null) continue;

            BigDecimal requiredQty = recipe.getRequiredQuantity() != null
                    ? recipe.getRequiredQuantity() : BigDecimal.ZERO;
            BigDecimal lossRate = recipe.getLossRate() != null
                    ? recipe.getLossRate() : BigDecimal.ZERO;

            // 瀹為檯鐢ㄩ噺 = 鏍囧噯鐢ㄩ噺 脳 鏁伴噺 脳 (1 + 鎹熻€楃巼)
            BigDecimal actualQty = requiredQty
                    .multiply(new BigDecimal(quantity))
                    .multiply(BigDecimal.ONE.add(lossRate.divide(new BigDecimal(100))));

            materialDeductionMap.merge(materialId, actualQty, BigDecimal::add);
            materialNameMap.put(materialId, recipe.getMaterialName());
        }
    }

    /**
     * 閫€娆炬椂鍥炶ˉ搴撳瓨
     * 鍏ㄩ閫€娆撅紙refundType=1锛夛細鍥炶ˉ鎵€鏈夋湭閫€娆捐彍鍝佺殑搴撳瓨
     * 閮ㄥ垎閫€娆撅紙refundType=2锛夛細浠呭洖琛ラ€€娆炬槑缁嗕腑鎸囧畾鑿滃搧鐨勫簱瀛橈紙DF-024 淇锛?     *
     * <p>DF-023 淇锛氬簱瀛樺洖琛ュけ璐ユ姏鍑哄紓甯革紝鐢辫皟鐢ㄦ柟鍐冲畾鏄惁鍥炴粴涓讳簨鍔°€?     * 鍘熷疄鐜?try/catch 闈欓粯鍚炲紓甯革紝瀵艰嚧閫€娆惧凡瀹℃壒浣嗗簱瀛樻湭鍥炶ˉ鐨勬暟鎹笉涓€鑷淬€?     * 鐜版敼涓猴細搴撳瓨鍥炶ˉ澶辫触 鈫?鎶涘嚭 RuntimeException 鈫?涓讳簨鍔″洖婊?鈫?閫€娆捐褰曚笉鏇存柊涓?宸查€€娆?銆?/p>
     *
     * @param order 璁㈠崟瀹炰綋
     * @param refundRecord 閫€娆捐褰曪紙鍖呭惈 refundType 涓?refundItemIds锛?     */
    private void restoreInventoryForRefund(OrderNew order, OrderRefundRecordNew refundRecord) {
        Long storeId = order.getStoreId();
        Integer refundType = refundRecord.getRefundType();

        if (refundType == null) {
            return;
        }

        // 鏀堕泦闇€瑕佸洖琛ョ殑鍘熸枡鍙婂叾鏁伴噺锛堟寜鍘熸枡ID姹囨€伙級
        Map<Long, BigDecimal> materialRestoreMap = new HashMap<>();
        Map<Long, String> materialNameMap = new HashMap<>();

        if (refundType == 1) {
            // 鍏ㄩ閫€娆撅細鍥炶ˉ鎵€鏈夋湭閫€娆捐彍鍝佺殑搴撳瓨
            collectMaterialsForAllOrderItems(order, materialRestoreMap, materialNameMap);
        } else if (refundType == 2) {
            // DF-024 淇锛氶儴鍒嗛€€娆?- 浠呭洖琛ラ€€娆炬槑缁嗕腑鎸囧畾鐨勮彍鍝佸簱瀛?            collectMaterialsForRefundItems(order, refundRecord, materialRestoreMap, materialNameMap);
        } else {
            log.warn("鏈煡閫€娆剧被鍨嬶紝璺宠繃搴撳瓨鍥炶ˉ: refundId={}, refundType={}",
                    refundRecord.getRefundId(), refundType);
            return;
        }

        // 鎵ц搴撳瓨鍥炶ˉ
        // DF-023 淇锛氬洖琛ュけ璐ユ姏鍑哄紓甯革紝鍥炴粴涓讳簨鍔?        for (Map.Entry<Long, BigDecimal> entry : materialRestoreMap.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal qty = entry.getValue();
            String materialName = materialNameMap.get(materialId);

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 鑾峰彇褰撳墠搴撳瓨璁板綍锛岀敤褰撳墠鍗曚綅鎴愭湰鍥炶ˉ锛堜繚鎸佸崟浣嶆垚鏈笉鍙橈級
            StoreInventory currentInventory = storeInventoryService.getByStoreAndMaterial(
                    String.valueOf(storeId), materialId);
            Long currentUnitCost = (currentInventory != null && currentInventory.getUnitCost() != null)
                    ? currentInventory.getUnitCost() : 0L;

            try {
                storeInventoryService.increaseStock(
                        String.valueOf(storeId),
                        materialId,
                        materialName,
                        qty,
                        null,
                        currentUnitCost,
                        1,
                        "璁㈠崟閫€娆惧洖琛ュ簱瀛?- 璁㈠崟:" + order.getOrderCode());
                log.info("閫€娆惧洖琛ュ簱瀛? orderId: {}, materialId: {}, materialName: {}, qty: {}, unitCost: {}鍒?,
                        order.getOrderId(), materialId, materialName, qty, currentUnitCost);
            } catch (Exception e) {
                // DF-023 淇锛氭姏鍑哄紓甯稿洖婊氫富浜嬪姟锛岄伩鍏嶉€€娆惧凡瀹℃壒浣嗗簱瀛樻湭鍥炶ˉ
                log.error("閫€娆惧洖琛ュ簱瀛樺け璐? orderId: {}, materialId: {}, error: {}",
                        order.getOrderId(), materialId, e.getMessage());
                throw new RuntimeException("閫€娆惧洖琛ュ簱瀛樺け璐ワ細" + materialName
                        + "锛坢aterialId=" + materialId + "锛? " + e.getMessage(), e);
            }
        }

        log.info("閫€娆惧洖琛ュ簱瀛樺畬鎴? orderId: {}, refundType: {}, materialCount: {}",
                order.getOrderId(), refundType, materialRestoreMap.size());
    }

    /**
     * 鏀堕泦鍏ㄩ閫€娆鹃渶瑕佸洖琛ョ殑鎵€鏈夊師鏂欙紙閬嶅巻璁㈠崟鍏ㄩ儴鏄庣粏锛?     * @param order 璁㈠崟瀹炰綋
     * @param materialRestoreMap 鍘熸枡鍥炶ˉ姹囨€籑ap锛堢疮鍔狅級
     * @param materialNameMap 鍘熸枡鍚嶇ОMap
     */
    private void collectMaterialsForAllOrderItems(OrderNew order,
                                                  Map<Long, BigDecimal> materialRestoreMap,
                                                  Map<Long, String> materialNameMap) {
        List<OrderItemNew> orderItems = getOrderItems(order.getOrderId());
        for (OrderItemNew item : orderItems) {
            // 璺宠繃鍘ㄦ埧鐘舵€佷负宸查€€娆撅紙4锛夌殑鑿滃搧
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                continue;
            }
            collectMaterialsFromOrderItem(item, materialRestoreMap, materialNameMap);
        }
    }

    /**
     * 鏀堕泦閮ㄥ垎閫€娆鹃渶瑕佸洖琛ョ殑鍘熸枡锛堜粎閬嶅巻閫€娆炬槑缁嗕腑鎸囧畾鐨勮彍鍝侊級
     * @param order 璁㈠崟瀹炰綋
     * @param refundRecord 閫€娆捐褰曪紙鍖呭惈 refundItemIds CSV锛?     * @param materialRestoreMap 鍘熸枡鍥炶ˉ姹囨€籑ap锛堢疮鍔狅級
     * @param materialNameMap 鍘熸枡鍚嶇ОMap
     */
    private void collectMaterialsForRefundItems(OrderNew order,
                                                OrderRefundRecordNew refundRecord,
                                                Map<Long, BigDecimal> materialRestoreMap,
                                                Map<Long, String> materialNameMap) {
        List<String> refundItemIds = parseRefundItemIdsFromCsv(refundRecord.getRefundItemIds());
        if (refundItemIds.isEmpty()) {
            log.warn("閮ㄥ垎閫€娆炬湭鎻愪緵閫€娆炬槑缁咺D鍒楄〃锛岃烦杩囧簱瀛樺洖琛? refundId={}, orderId={}",
                    refundRecord.getRefundId(), order.getOrderId());
            return;
        }

        List<OrderItemNew> orderItems = getOrderItems(order.getOrderId());
        // 鏋勫缓 itemId 鈫?OrderItemNew 绱㈠紩锛岄伩鍏?N+1 鏌ヨ
        Map<String, OrderItemNew> orderItemMap = new HashMap<>();
        for (OrderItemNew item : orderItems) {
            if (item.getItemId() != null) {
                orderItemMap.put(item.getItemId(), item);
            }
        }

        int matchedCount = 0;
        for (String itemId : refundItemIds) {
            OrderItemNew item = orderItemMap.get(itemId);
            if (item == null) {
                log.warn("閫€娆炬槑缁咺D鍦ㄨ鍗曚腑涓嶅瓨鍦紝璺宠繃: orderId={}, itemId={}",
                        order.getOrderId(), itemId);
                continue;
            }
            // 璺宠繃鍘ㄦ埧鐘舵€佷负宸查€€娆撅紙4锛夌殑鑿滃搧锛堥槻姝㈤噸澶嶅洖琛ワ級
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                log.info("鑿滃搧宸查€€娆撅紝璺宠繃搴撳瓨鍥炶ˉ: orderId={}, itemId={}",
                        order.getOrderId(), itemId);
                continue;
            }
            collectMaterialsFromOrderItem(item, materialRestoreMap, materialNameMap);
            matchedCount++;
        }

        log.info("閮ㄥ垎閫€娆惧尮閰嶆槑缁嗘暟: refundId={}, requested={}, matched={}",
                refundRecord.getRefundId(), refundItemIds.size(), matchedCount);
    }

    /**
     * 浠庡崟涓鍗曟槑缁嗕腑鏀堕泦闇€瑕佸洖琛ョ殑鍘熸枡
     * 鏀寔鍗曞搧锛坧roductType=1锛夊拰濂楅锛坧roductType=2锛変袱绉嶇被鍨?     * @param item 璁㈠崟鏄庣粏
     * @param materialRestoreMap 鍘熸枡鍥炶ˉ姹囨€籑ap锛堢疮鍔狅級
     * @param materialNameMap 鍘熸枡鍚嶇ОMap
     */
    private void collectMaterialsFromOrderItem(OrderItemNew item,
                                                Map<Long, BigDecimal> materialRestoreMap,
                                                Map<Long, String> materialNameMap) {
        Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;

        if (item.getProductType() != null && item.getProductType() == 1) {
            // 鍗曞搧
            if (item.getFoodId() != null) {
                addFoodMaterialDeductions(
                        item.getFoodId(),
                        quantity,
                        materialRestoreMap,
                        materialNameMap);
            }
        } else if (item.getProductType() != null && item.getProductType() == 2) {
            // 濂楅
            if (item.getFoodId() != null) {
                List<ComboIngredient> comboIngredients = comboIngredientMapper.selectByComboId(item.getFoodId());
                for (ComboIngredient ingredient : comboIngredients) {
                    if (ingredient.getFoodId() == null) continue;
                    BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                            ? ingredient.getQuantity() : BigDecimal.ONE;
                    int actualFoodQty = foodQtyInCombo.multiply(new BigDecimal(quantity)).intValue();
                    if (actualFoodQty <= 0) continue;

                    try {
                        Long foodIdLong = Long.parseLong(ingredient.getFoodId());
                        addFoodMaterialDeductions(
                                foodIdLong,
                                actualFoodQty,
                                materialRestoreMap,
                                materialNameMap);
                    } catch (NumberFormatException e) {
                        log.warn("濂楅鑿滃搧ID鏍煎紡寮傚父, comboId: {}, foodId: {}", item.getFoodId(), ingredient.getFoodId());
                    }
                }
            }
        }
    }

    // ==================== 鏌ヨ鏂规硶 ====================

    @Override
    public OrderVO getOrderDetail(String orderId) {
        OrderNew order = getAndValidateOrder(orderId);
        return convertToOrderVO(order);
    }

    @Override
    public Page<OrderVO> queryOrders(OrderQueryDTO queryDTO) {
        Page<OrderNew> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        QueryWrapper<OrderNew> wrapper = buildQueryWrapper(queryDTO);
        page = orderNewMapper.selectPage(page, wrapper);

        // 杞崲涓篤O鍒楄〃
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::convertToSimpleOrderVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 鍒嗛〉鏌ヨPOS缁堢璁㈠崟锛坥rders_legacy 琛級
     * 涓?queryOrders锛堟煡璇?orders 琛級浜掕ˉ锛岃绠＄悊绔兘鏌ョ湅POS鏀堕摱绔垱寤虹殑璁㈠崟銆?     * 鍐呴儴瀹屾垚璁㈠崟鐘舵€?鏀粯鏂瑰紡/閲戦鍗曚綅锛堝厓鈫掑垎锛夌殑鏄犲皠杞崲銆?     */
    @Override
    public Page<OrderVO> queryPosOrders(OrderQueryDTO queryDTO) {
        Page<com.foodtraceability.entity.Order> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        QueryWrapper<com.foodtraceability.entity.Order> wrapper = buildPosQueryWrapper(queryDTO);
        page = posOrderMapper.selectPage(page, wrapper);

        // 杞崲涓篤O鍒楄〃锛堝寘鍚姸鎬佺爜/鏀粯鏂瑰紡/閲戦鍗曚綅鏄犲皠锛?        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::convertPosOrderToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 鏍规嵁璁㈠崟缂栧彿鑾峰彇POS缁堢璁㈠崟璇︽儏锛堝惈鑿滃搧鏄庣粏锛?     * 鐢ㄤ簬绠＄悊绔鍗曚腑蹇冩煡鐪婸OS鏀堕摱绔鍗曠殑瀹屾暣鏄庣粏锛堟暟鎹拷婧級銆?     *
     * 瀹炵幇瑕佺偣锛?     * 1. 閫氳繃 orderNumber 鏌ヨ orders_legacy 琛ㄨ幏鍙栬鍗曚富淇℃伅
     * 2. 閫氳繃 orderId 鏌ヨ order_items_legacy 琛ㄨ幏鍙栬彍鍝佹槑缁嗗垪琛?     * 3. 璋冪敤 convertPosOrderToVO 瀹屾垚鐘舵€?鏀粯鏂瑰紡/閲戦鍗曚綅鐨勭粺涓€鏄犲皠
     * 4. 琛ュ厖 items 鍒楄〃锛圤rderItemVO 鏍煎紡锛岄噾棰濊浆鍒嗭級
     */
    @Override
    public OrderVO getPosOrderDetail(String orderNumber) {
        log.info("鏌ヨPOS璁㈠崟璇︽儏, orderNumber: {}", orderNumber);
        if (orderNumber == null || orderNumber.isEmpty()) {
            throw new RuntimeException("璁㈠崟缂栧彿涓嶈兘涓虹┖");
        }
        // 鎸夎鍗曠紪鍙锋煡璇?orders_legacy 琛?        com.foodtraceability.entity.Order posOrder = posOrderMapper.selectOne(
                new LambdaQueryWrapper<com.foodtraceability.entity.Order>()
                        .eq(com.foodtraceability.entity.Order::getOrderNumber, orderNumber));
        if (posOrder == null) {
            throw new RuntimeException("POS璁㈠崟涓嶅瓨鍦? " + orderNumber);
        }
        // 杞崲涓?OrderVO锛堝惈鐘舵€?鏀粯鏂瑰紡/閲戦鍗曚綅鏄犲皠锛屼絾涓嶅惈鏄庣粏锛?        OrderVO vo = convertPosOrderToVO(posOrder);

        // 鏌ヨ璁㈠崟鏄庣粏锛坥rder_items_legacy 琛級
        List<OrderItem> posItems = posOrderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, posOrder.getOrderId()));
        // 杞崲涓?OrderItemVO锛堥噾棰?鍏冣啋鍒嗭紝涓?OrderVO 鍗曚綅淇濇寔涓€鑷达級
        List<OrderVO.OrderItemVO> itemVos = posItems.stream()
                .map(this::convertPosItemToVO)
                .collect(Collectors.toList());
        vo.setItems(itemVos);

        log.info("鏌ヨPOS璁㈠崟璇︽儏鎴愬姛, orderNumber: {}, 鏄庣粏鏁? {}", orderNumber, itemVos.size());
        return vo;
    }

    /**
     * 灏哖OS璁㈠崟椤癸紙order_items_legacy锛夎浆鎹负绠＄悊绔?OrderItemVO
     * 閲戦鍗曚綅锛氬厓锛圔igDecimal锛夆啋 鍒嗭紙Long锛夛紝涓?OrderVO 淇濇寔涓€鑷?     */
    private OrderVO.OrderItemVO convertPosItemToVO(OrderItem item) {
        OrderVO.OrderItemVO vo = new OrderVO.OrderItemVO();
        vo.setItemId(item.getOrderItemId());
        // POS璁㈠崟椤规棤浜у搧绫诲瀷瀛楁锛岄粯璁や负鍗曞搧
        vo.setProductType(1);
        vo.setProductTypeName("鍗曞搧");
        vo.setProductName(item.getFoodName());
        vo.setSpecification(item.getSpecification());
        // 鍏冭浆鍒?        if (item.getUnitPrice() != null) {
            vo.setUnitPrice(item.getUnitPrice().multiply(new BigDecimal("100")).longValue());
        }
        vo.setQuantity(item.getQuantity());
        if (item.getSubtotalAmount() != null) {
            vo.setAmount(item.getSubtotalAmount().multiply(new BigDecimal("100")).longValue());
        }
        return vo;
    }

    /**
     * 鏋勫缓POS璁㈠崟锛坥rders_legacy锛夋煡璇㈡潯浠?     * 瀛楁鍚嶄娇鐢?orders_legacy 琛ㄧ殑瀹為檯鍒楀悕锛堝ぇ鍐欎笅鍒掔嚎锛?     */
    private QueryWrapper<com.foodtraceability.entity.Order> buildPosQueryWrapper(OrderQueryDTO queryDTO) {
        QueryWrapper<com.foodtraceability.entity.Order> wrapper = new QueryWrapper<>();
        if (queryDTO.getOrderCode() != null && !queryDTO.getOrderCode().isEmpty()) {
            wrapper.like("ORDER_NUMBER", queryDTO.getOrderCode());
        }
        if (queryDTO.getOrderType() != null) {
            wrapper.eq("ORDER_TYPE", queryDTO.getOrderType());
        }
        // 璁㈠崟鐘舵€佹槧灏勶細绠＄悊绔姸鎬佺爜 鈫?POS绔姸鎬佺爜
        if (queryDTO.getOrderStatus() != null) {
            List<Integer> posStatusList = mapAdminStatusToPosStatus(queryDTO.getOrderStatus());
            if (posStatusList.size() == 1) {
                wrapper.eq("ORDER_STATUS", posStatusList.get(0));
            } else {
                wrapper.in("ORDER_STATUS", posStatusList);
            }
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            wrapper.ge("CREATE_TIME", parseDateTime(queryDTO.getStartTime()));
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            wrapper.le("CREATE_TIME", parseDateTime(queryDTO.getEndTime()));
        }
        // 閲戦鏉′欢锛氱鐞嗙鏄垎锛宱rders_legacy 鏄厓锛坣umeric锛夛紝闇€瑕佽浆鎹?        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("ACTUAL_AMOUNT", new BigDecimal(queryDTO.getMinAmount()).movePointLeft(2));
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("ACTUAL_AMOUNT", new BigDecimal(queryDTO.getMaxAmount()).movePointLeft(2));
        }
        // 闂ㄥ簵ID杩囨护锛歰rders_legacy.STORE_ID 鏄?String 绫诲瀷锛堜笌 stores_new.store_id Long 鍏宠仈锛?        if (queryDTO.getStoreId() != null && !queryDTO.getStoreId().isEmpty()) {
            wrapper.eq("STORE_ID", queryDTO.getStoreId());
        }
        wrapper.orderByDesc("CREATE_TIME");
        return wrapper;
    }

    /**
     * 绠＄悊绔鍗曠姸鎬佺爜 鈫?POS绔鍗曠姸鎬佺爜鏄犲皠
     * 绠＄悊绔細0寰呯‘璁?1宸茬‘璁?2宸插畬鎴?3宸插彇娑?4閮ㄥ垎閫€娆?5鍏ㄩ閫€娆?6寰呰瘎浠?     * POS绔細0寰呮敮浠?-1鏀粯涓?1宸叉敮浠?2寰呴厤閫?3閰嶉€佷腑 4宸插畬鎴?5宸插彇娑?6閫€娆句腑 7宸查€€娆?     */
    private List<Integer> mapAdminStatusToPosStatus(Integer adminStatus) {
        List<Integer> posStatusList = new ArrayList<>();
        switch (adminStatus) {
            case 0: // 寰呯‘璁?鈫?POS寰呮敮浠?鏀粯涓?                posStatusList.add(0);
                posStatusList.add(-1);
                break;
            case 1: // 宸茬‘璁?鈫?POS宸叉敮浠?寰呴厤閫?閰嶉€佷腑
                posStatusList.add(1);
                posStatusList.add(2);
                posStatusList.add(3);
                break;
            case 2: // 宸插畬鎴?鈫?POS宸插畬鎴?                posStatusList.add(4);
                break;
            case 3: // 宸插彇娑?鈫?POS宸插彇娑?                posStatusList.add(5);
                break;
            case 4: // 閮ㄥ垎閫€娆?鈫?POS閫€娆句腑
                posStatusList.add(6);
                break;
            case 5: // 鍏ㄩ閫€娆?鈫?POS宸查€€娆?                posStatusList.add(7);
                break;
            case 6: // 寰呰瘎浠?鈫?POS宸插畬鎴愶紙绠＄悊绔嫭鏈夛紝鏄犲皠鍒癙OS宸插畬鎴愶級
                posStatusList.add(4);
                break;
            default:
                // 涓嶉檺鍒剁姸鎬?        }
        return posStatusList;
    }

    /**
     * 灏哖OS璁㈠崟瀹炰綋锛坥rders_legacy锛夎浆鎹负绠＄悊绔疧rderVO
     * 鍖呭惈锛氳鍗曠姸鎬?鏀粯鏂瑰紡/閲戦鍗曚綅锛堝厓鈫掑垎锛夌殑鏄犲皠
     */
    private OrderVO convertPosOrderToVO(com.foodtraceability.entity.Order posOrder) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(posOrder.getOrderId());
        vo.setOrderCode(posOrder.getOrderNumber());
        // 璁㈠崟绫诲瀷鏄犲皠锛歅OS绔?0鍫傞1澶栧崠2鑷彁 鈫?绠＄悊绔?1鍫傞2澶栧崠3鑷彁4鎵撳寘
        Integer adminOrderType = mapPosOrderTypeToAdmin(posOrder.getOrderType());
        vo.setOrderType(adminOrderType);
        vo.setOrderTypeName(getOrderTypeName(adminOrderType));
        vo.setOrderSource(posOrder.getOrderSource());
        vo.setCustomerName(posOrder.getContactName());
        vo.setCustomerPhone(posOrder.getContactPhone());
        vo.setRemark(posOrder.getRemarks());
        vo.setCancelReason(posOrder.getCancelReason());
        vo.setDeliveryAddress(posOrder.getDeliveryAddress());

        // 璁㈠崟鐘舵€佹槧灏勶細POS绔?鈫?绠＄悊绔?        Integer posStatus = posOrder.getOrderStatus();
        Integer adminStatus = mapPosStatusToAdminStatus(posStatus);
        vo.setOrderStatus(adminStatus);
        vo.setOrderStatusName(getOrderStatusName(adminStatus));

        // 鏀粯鐘舵€侊細POS宸叉敮浠?1)/寰呴厤閫?2)/閰嶉€佷腑(3)/宸插畬鎴?4) 鈫?宸叉敮浠?2)锛涘凡閫€娆?7)/閫€娆句腑(6) 鈫?宸查€€娆?3)锛涘叾浠?鈫?鏈敮浠?0)
        Integer paymentStatus = mapPosStatusToPaymentStatus(posStatus);
        vo.setPaymentStatus(paymentStatus);
        vo.setPaymentStatusName(getPaymentStatusName(paymentStatus));

        // 鏀粯鏂瑰紡鏄犲皠锛歅OS绔?鈫?绠＄悊绔?        // POS: 0寰俊 1鏀粯瀹?2鐜伴噾 3閾惰鍗?4浣欓
        // 绠＄悊绔? 1鐜伴噾 2寰俊 3鏀粯瀹?4閾惰鍗?5绉垎(浣欓) 6娣峰悎鏀粯
        Integer posPayMethod = posOrder.getPaymentMethod();
        Integer adminPayMethod = null;
        if (posPayMethod != null) {
            switch (posPayMethod) {
                case 0: adminPayMethod = 2; break; // 寰俊
                case 1: adminPayMethod = 3; break; // 鏀粯瀹?                case 2: adminPayMethod = 1; break; // 鐜伴噾
                case 3: adminPayMethod = 4; break; // 閾惰鍗?                case 4: adminPayMethod = 5; break; // 浣欓鈫掔Н鍒?                default: adminPayMethod = null;
            }
        }
        vo.setPaymentMethodName(adminPayMethod != null ? getPaymentMethodName(adminPayMethod) : "鏈敮浠?);

        // 閲戦杞崲锛歰rders_legacy 鏄厓锛圔igDecimal锛夛紝OrderVO 鏄垎锛圠ong锛?        BigDecimal actualAmount = posOrder.getActualAmount() != null ? posOrder.getActualAmount() : posOrder.getOrderAmount();
        if (actualAmount != null) {
            long fenAmount = actualAmount.multiply(new BigDecimal("100")).longValue();
            vo.setTotalAmount(fenAmount);
            vo.setFinalAmount(fenAmount);
            vo.setPaidAmount(paymentStatus != null && paymentStatus == 2 ? fenAmount : 0L);
        }
        if (posOrder.getDiscountAmount() != null) {
            vo.setDiscountAmount(posOrder.getDiscountAmount().multiply(new BigDecimal("100")).longValue());
        }
        if (posOrder.getRefundAmount() != null && posOrder.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            vo.setRefundAmount(posOrder.getRefundAmount().multiply(new BigDecimal("100")).longValue());
        }

        vo.setCreateTime(posOrder.getCreateTime());
        vo.setUpdateTime(posOrder.getUpdateTime());
        // 闂ㄥ簵淇℃伅锛歅OS绔鍗曞叧鑱旂殑闂ㄥ簵
        vo.setStoreId(posOrder.getStoreId());
        vo.setStoreName(posOrder.getStoreName() != null ? posOrder.getStoreName() : "涓績鏃楄埌搴?);
        return vo;
    }

    /**
     * POS绔鍗曠姸鎬佺爜 鈫?绠＄悊绔鍗曠姸鎬佺爜鏄犲皠
     */
    private Integer mapPosStatusToAdminStatus(Integer posStatus) {
        if (posStatus == null) return 0;
        switch (posStatus) {
            case 0: case -1: return 0; // 寰呮敮浠?鏀粯涓?鈫?寰呯‘璁?            case 1: case 2: case 3: return 1; // 宸叉敮浠?寰呴厤閫?閰嶉€佷腑 鈫?宸茬‘璁?            case 4: return 2; // 宸插畬鎴?鈫?宸插畬鎴?            case 5: return 3; // 宸插彇娑?鈫?宸插彇娑?            case 6: return 4; // 閫€娆句腑 鈫?閮ㄥ垎閫€娆?            case 7: return 5; // 宸查€€娆?鈫?鍏ㄩ閫€娆?            default: return 0;
        }
    }

    /**
     * POS绔鍗曠姸鎬佺爜 鈫?绠＄悊绔敮浠樼姸鎬佺爜鏄犲皠
     */
    private Integer mapPosStatusToPaymentStatus(Integer posStatus) {
        if (posStatus == null) return 0;
        switch (posStatus) {
            case 1: case 2: case 3: case 4: return 2; // 宸叉敮浠?寰呴厤閫?閰嶉€佷腑/宸插畬鎴?鈫?宸叉敮浠?            case 6: case 7: return 3; // 閫€娆句腑/宸查€€娆?鈫?宸查€€娆?            default: return 0; // 寰呮敮浠?鏀粯涓?宸插彇娑?鈫?鏈敮浠?        }
    }

    @Override
    public PageResult<OrderRefundListVO> queryRefunds(OrderRefundQueryDTO queryDTO) {
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 10;

        LambdaQueryWrapper<OrderRefundRecordNew> wrapper = new LambdaQueryWrapper<>();
        // 閫€娆剧姸鎬佽繃婊わ紙瀹炰綋鍙栧€硷細0寰呭鏍?1宸插悓鎰?2宸叉嫆缁?3宸查€€娆撅級
        if (queryDTO.getRefundStatus() != null) {
            wrapper.eq(OrderRefundRecordNew::getRefundStatus, queryDTO.getRefundStatus());
        }
        // 鐢宠鏃堕棿鑼冨洿杩囨护
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            wrapper.ge(OrderRefundRecordNew::getCreateTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            wrapper.le(OrderRefundRecordNew::getCreateTime, queryDTO.getEndTime());
        }
        // 璁㈠崟缂栧彿妯＄硦杩囨护锛氶€€娆捐褰曚粎瀛?orderId锛岄渶鍏堟煡璁㈠崟鑾峰彇 orderId 闆嗗悎
        if (queryDTO.getOrderCode() != null && !queryDTO.getOrderCode().isEmpty()) {
            QueryWrapper<OrderNew> orderWrapper = new QueryWrapper<>();
            orderWrapper.like("order_code", queryDTO.getOrderCode());
            List<OrderNew> matchedOrders = orderNewMapper.selectList(orderWrapper);
            if (matchedOrders.isEmpty()) {
                return new PageResult<>(0L, new ArrayList<>(), (long) page, (long) size);
            }
            List<String> orderIds = matchedOrders.stream()
                    .map(OrderNew::getOrderId)
                    .collect(Collectors.toList());
            wrapper.in(OrderRefundRecordNew::getOrderId, orderIds);
        }
        wrapper.orderByDesc(OrderRefundRecordNew::getCreateTime);

        Page<OrderRefundRecordNew> refundPage = new Page<>(page, size);
        refundPage = orderRefundRecordNewMapper.selectPage(refundPage, wrapper);

        // 鎵归噺鏌ヨ鍏宠仈璁㈠崟缂栧彿锛岄伩鍏?N+1 鏌ヨ
        List<String> orderIdList = refundPage.getRecords().stream()
                .map(OrderRefundRecordNew::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> orderCodeMap = new HashMap<>();
        if (!orderIdList.isEmpty()) {
            List<OrderNew> orders = orderNewMapper.selectList(
                    new QueryWrapper<OrderNew>().in("order_id", orderIdList));
            for (OrderNew o : orders) {
                orderCodeMap.put(o.getOrderId(), o.getOrderCode());
            }
        }

        List<OrderRefundListVO> voList = refundPage.getRecords().stream()
                .map(record -> convertToRefundListVO(record, orderCodeMap))
                .collect(Collectors.toList());

        return new PageResult<>(refundPage.getTotal(), voList, refundPage.getCurrent(), refundPage.getSize());
    }

    /**
     * 杞崲閫€娆捐褰曚负鍒楄〃VO
     * @param record 閫€娆捐褰曞疄浣?     * @param orderCodeMap 璁㈠崟ID鍒拌鍗曠紪鍙风殑鏄犲皠
     * @return 閫€娆惧垪琛╒O
     */
    private OrderRefundListVO convertToRefundListVO(OrderRefundRecordNew record, Map<String, String> orderCodeMap) {
        OrderRefundListVO vo = new OrderRefundListVO();
        vo.setRefundId(record.getRefundId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderCode(orderCodeMap.get(record.getOrderId()));
        vo.setRefundAmount(record.getRefundAmount());
        vo.setRefundReason(record.getRefundReason());
        vo.setRefundStatus(record.getRefundStatus());
        vo.setRefundStatusName(getRefundStatusName(record.getRefundStatus()));
        vo.setCreateTime(record.getCreateTime());
        vo.setCompleteTime(record.getCompleteTime());
        return vo;
    }

    @Override
    public TodayStatisticsVO getTodayStatistics() {
        TodayStatisticsVO stats = new TodayStatisticsVO();
        // 璁㈠崟缁熻鑱氬悎锛歰rders锛堢鐞嗙璁㈠崟锛?+ orders_legacy锛圥OS绔鍗曪紝涓昏鏁版嵁鏉ユ簮锛?        // POS绔骇鐢熺殑浜ゆ槗琛屼负鏄鐞嗙鑾峰彇璁㈠崟鏁版嵁鐨勪富瑕佹潵婧愶紝蹇呴』鍖呭惈鍦ㄥ唴
        long todayOrdersAdmin = orderNewMapper.countTodayOrders();
        long todayOrdersPos = posOrderMapper.countTodayPosOrders();
        stats.setTotalOrders(todayOrdersAdmin + todayOrdersPos);

        // 閿€鍞鑱氬悎锛堝崟浣嶏細鍒嗭級
        long todaySalesAdmin = orderNewMapper.sumTodaySales();
        long todaySalesPos = posOrderMapper.sumTodayPosSales();
        stats.setTotalSalesAmount(todaySalesAdmin + todaySalesPos);

        // 宸插畬鎴愯鍗曪紙orders.order_status=2 + orders_legacy.order_status=4锛?        long completedOrdersAdmin = countOrdersByStatus(2);
        long completedOrdersPos = posOrderMapper.countTodayPosCompletedOrders();
        stats.setCompletedOrders(completedOrdersAdmin + completedOrdersPos);

        // 宸插彇娑堣鍗曪紙orders.order_status=3 + orders_legacy.order_status=5锛?        long cancelledOrdersAdmin = countOrdersByStatus(3);
        long cancelledOrdersPos = posOrderMapper.countTodayPosCancelledOrders();
        stats.setCancelledOrders(cancelledOrdersAdmin + cancelledOrdersPos);
        return stats;
    }

    @Override
    public Map<String, Object> getShiftSummary(Long cashierUserId) {
        Map<String, Object> summary = new HashMap<>();
        QueryWrapper<OrderNew> wrapper = new QueryWrapper<>();
        wrapper.eq("cashier_user_id", cashierUserId);
        wrapper.ge("create_time", LocalDateTime.now().with(java.time.LocalTime.MIN));
        wrapper.eq("payment_status", 2); // 宸叉敮浠?        Long totalCount = orderNewMapper.selectCount(wrapper);
        summary.put("totalOrders", totalCount);
        // TODO: 琛ュ厖鏇村浜ゆ帴鐝粺璁′俊鎭?        return summary;
    }

    @Override
    public void updateItemKitchenStatus(String itemId, Integer kitchenStatus) {
        orderItemNewMapper.updateKitchenStatus(itemId, kitchenStatus);
    }

    @Override
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        // 璁㈠崟缁熻鑱氬悎锛歰rders锛堢鐞嗙璁㈠崟锛?+ orders_legacy锛圥OS绔鍗曪紝涓昏鏁版嵁鏉ユ簮锛?        // POS绔骇鐢熺殑浜ゆ槗琛屼负鏄鐞嗙鑾峰彇璁㈠崟鏁版嵁鐨勪富瑕佹潵婧愶紝蹇呴』鍖呭惈鍦ㄥ唴
        long totalOrdersAdmin = orderNewMapper.countAllOrders();
        long totalOrdersPos = posOrderMapper.countAllPosOrders();
        stats.put("totalOrders", totalOrdersAdmin + totalOrdersPos);

        long completedOrdersAdmin = orderNewMapper.countAllCompletedOrders();
        long completedOrdersPos = posOrderMapper.countAllPosCompletedOrders();
        stats.put("completedOrders", completedOrdersAdmin + completedOrdersPos);

        long todayOrdersAdmin = orderNewMapper.countTodayOrders();
        long todayOrdersPos = posOrderMapper.countTodayPosOrders();
        stats.put("todayOrders", todayOrdersAdmin + todayOrdersPos);

        long totalSalesAdmin = orderNewMapper.sumAllSales();
        long totalSalesPos = posOrderMapper.sumAllPosSales();
        stats.put("totalSales", totalSalesAdmin + totalSalesPos);

        long todaySalesAdmin = orderNewMapper.sumTodaySales();
        long todaySalesPos = posOrderMapper.sumTodayPosSales();
        stats.put("todaySales", todaySalesAdmin + todaySalesPos);
        return stats;
    }

    @Override
    public OrderRefundStatsVO getRefundStats() {
        OrderRefundStatsVO stats = new OrderRefundStatsVO();
        stats.setPendingCount(orderRefundRecordNewMapper.countPendingRefunds());
        stats.setRefundedAmount(orderRefundRecordNewMapper.sumRefundedAmount());

        // 閫€娆剧巼 = 閫€娆惧崟鏁?/ 鎬昏鍗曟暟 * 100
        long totalOrders = orderNewMapper.countAllOrders();
        long totalRefunds = orderRefundRecordNewMapper.countAllRefunds();
        double refundRate = totalOrders > 0
                ? (double) totalRefunds / totalOrders * 100.0
                : 0.0;
        stats.setRefundRate(Math.round(refundRate * 100.0) / 100.0);

        stats.setAvgProcessHours(orderRefundRecordNewMapper.avgRefundProcessHours());
        return stats;
    }

    @Override
    public List<com.foodtraceability.dto.order.DailyStatsVO> getDailyStats(String startDate, String endDate, String storeName) {
        // 榛樿鏌ヨ杩?澶╋紙鍚粖澶╋級
        LocalDate end = (endDate != null && !endDate.trim().isEmpty())
                ? LocalDate.parse(endDate)
                : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.trim().isEmpty())
                ? LocalDate.parse(startDate)
                : end.minusDays(6);

        // 鑱氬悎 orders 琛ㄧ殑姣忔棩瓒嬪娍
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        List<Map<String, Object>> adminTrend = orderNewMapper.getDailyTrend(startDateTime, endDateTime);

        // 鑱氬悎 orders_legacy 琛ㄧ殑姣忔棩瓒嬪娍
        List<Map<String, Object>> posTrend = posOrderMapper.getDailyTrendForPos(start, end);

        // 鍚堝苟涓よ〃鏁版嵁鍒板悓涓€ Map锛堟寜鏃ユ湡鍒嗙粍绱姞锛?        Map<String, long[]> mergedData = new TreeMap<>();
        if (adminTrend != null) {
            for (Map<String, Object> row : adminTrend) {
                String date = String.valueOf(row.get("date"));
                long orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).longValue() : 0L;
                long revenue = row.get("revenue") != null ? ((Number) row.get("revenue")).longValue() : 0L;
                long[] existing = mergedData.getOrDefault(date, new long[]{0L, 0L});
                existing[0] += orderCount;
                existing[1] += revenue;
                mergedData.put(date, existing);
            }
        }
        if (posTrend != null) {
            for (Map<String, Object> row : posTrend) {
                String date = String.valueOf(row.get("date"));
                long orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).longValue() : 0L;
                long revenue = row.get("revenue") != null ? ((Number) row.get("revenue")).longValue() : 0L;
                long[] existing = mergedData.getOrDefault(date, new long[]{0L, 0L});
                existing[0] += orderCount;
                existing[1] += revenue;
                mergedData.put(date, existing);
            }
        }

        // 鏋勫缓缁撴灉鍒楄〃
        List<com.foodtraceability.dto.order.DailyStatsVO> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : mergedData.entrySet()) {
            com.foodtraceability.dto.order.DailyStatsVO vo = new com.foodtraceability.dto.order.DailyStatsVO();
            vo.setDate(entry.getKey());
            vo.setStoreName(storeName != null && !storeName.isEmpty() ? storeName : "鍏ㄩ儴闂ㄥ簵");
            vo.setOrderCount(entry.getValue()[0]);
            vo.setRevenue(entry.getValue()[1]);
            // 鎴愭湰鏆傛湭瀹炵幇锛岃涓?锛涘埄娑?= 钀ヤ笟棰?- 鎴愭湰
            vo.setCost(0L);
            vo.setProfit(entry.getValue()[1]);
            // 鍒╂鼎鐜?= 鍒╂鼎 / 钀ヤ笟棰?* 100
            vo.setProfitRate(entry.getValue()[1] > 0 ? 100.0 : 0.0);
            result.add(vo);
        }
        return result;
    }

    @Override
    public Map<String, Object> getOrderTrends(String startDate, String endDate, String granularity) {
        // 榛樿鏌ヨ杩?澶╋紙鍚粖澶╋級
        LocalDate end = (endDate != null && !endDate.trim().isEmpty())
                ? LocalDate.parse(endDate)
                : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.trim().isEmpty())
                ? LocalDate.parse(startDate)
                : end.minusDays(6);

        // 涓婁竴鍛ㄦ湡鐢ㄤ簬鍚屾瘮鐜瘮璁＄畻
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevStart = start.minusDays(periodDays);
        LocalDate prevEnd = start.minusDays(1);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        LocalDateTime prevStartDateTime = prevStart.atStartOfDay();
        LocalDateTime prevEndDateTime = prevEnd.plusDays(1).atStartOfDay();

        // 1. 鎬昏缁熻锛氫娇鐢ㄥ叏閲忔暟鎹紝涓嶆寜鏃堕棿鑼冨洿杩囨护
        long totalOrders = orderNewMapper.countAllOrders();
        long totalSales = orderNewMapper.sumAllSales();
        long totalCustomers = orderNewMapper.countDistinctCustomers();
        double avgOrderValue = totalOrders > 0
                ? (double) totalSales / totalOrders
                : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOrders", totalOrders);
        summary.put("totalSales", totalSales);
        summary.put("averageOrderValue", Math.round(avgOrderValue * 100.0) / 100.0);
        summary.put("totalCustomers", totalCustomers);

        // 2. 鏃堕棿搴忓垪锛氭寜澶╄仛鍚堝綋鍓嶅懆鏈熸暟鎹?        List<Map<String, Object>> rawTrend = orderNewMapper.getDailyTrend(startDateTime, endDateTime);
        List<Map<String, Object>> timeSeries = new ArrayList<>();
        // 琛ラ綈鏃犳暟鎹棩鏈燂紝淇濊瘉鏃堕棿搴忓垪杩炵画
        Map<String, Map<String, Object>> trendMap = new LinkedHashMap<>();
        if (rawTrend != null) {
            for (Map<String, Object> row : rawTrend) {
                String dateKey = String.valueOf(row.get("date"));
                trendMap.put(dateKey, row);
            }
        }
        for (int i = 0; i < periodDays; i++) {
            LocalDate day = start.plusDays(i);
            String dateKey = day.toString();
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", dateKey);
            Map<String, Object> raw = trendMap.get(dateKey);
            long orderCount = raw == null ? 0L
                    : ((Number) raw.getOrDefault("order_count", 0)).longValue();
            long revenue = raw == null ? 0L
                    : ((Number) raw.getOrDefault("revenue", 0)).longValue();
            dayData.put("orderCount", orderCount);
            dayData.put("sales", revenue);
            timeSeries.add(dayData);
        }

        // 3. 璁㈠崟绫诲瀷鍒嗗竷锛氬爞椋?澶栧崠/鑷彁/鎵撳寘鍗犳瘮
        List<Map<String, Object>> typeRows = orderNewMapper.countByOrderType();
        Map<String, Object> orderTypeDistribution = new LinkedHashMap<>();
        long typeTotal = 0L;
        if (typeRows != null) {
            for (Map<String, Object> row : typeRows) {
                Integer orderType = row.get("order_type") == null
                        ? null
                        : ((Number) row.get("order_type")).intValue();
                long cnt = ((Number) row.getOrDefault("cnt", 0)).longValue();
                typeTotal += cnt;
                orderTypeDistribution.put(getOrderTypeName(orderType), cnt);
            }
        }
        // 杞崲涓虹櫨鍒嗘瘮鍗犳瘮
        Map<String, Object> orderTypeRatio = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : orderTypeDistribution.entrySet()) {
            long cnt = ((Number) e.getValue()).longValue();
            double ratio = typeTotal > 0 ? (double) cnt / typeTotal * 100.0 : 0.0;
            orderTypeRatio.put(e.getKey(), Math.round(ratio * 100.0) / 100.0);
        }

        // 4. 鍚屾瘮鐜瘮锛氬綋鍓嶅懆鏈?vs 涓婁竴鍛ㄦ湡
        List<Map<String, Object>> prevTrend = orderNewMapper.getDailyTrend(prevStartDateTime, prevEndDateTime);
        long prevOrderCount = 0L;
        long prevSales = 0L;
        if (prevTrend != null) {
            for (Map<String, Object> row : prevTrend) {
                prevOrderCount += ((Number) row.getOrDefault("order_count", 0)).longValue();
                prevSales += ((Number) row.getOrDefault("revenue", 0)).longValue();
            }
        }
        long curOrderCount = timeSeries.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue())
                .sum();
        long curSales = timeSeries.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("sales", 0)).longValue())
                .sum();
        double curAvg = curOrderCount > 0 ? (double) curSales / curOrderCount : 0.0;
        double prevAvg = prevOrderCount > 0 ? (double) prevSales / prevOrderCount : 0.0;

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("orderCountChange", calcChangeRate(curOrderCount, prevOrderCount));
        comparison.put("salesChange", calcChangeRate(curSales, prevSales));
        comparison.put("averageOrderValueChange", calcChangeRate(curAvg, prevAvg));

        // 5. 瓒嬪娍鍒嗘瀽锛氬熀浜庢椂闂村簭鍒楀垽鏂笂鍗?涓嬮檷/骞崇ǔ锛屾壘鍑哄嘲鍊兼棩
        Map<String, Object> trendAnalysis = new LinkedHashMap<>();
        if (timeSeries.isEmpty() || curOrderCount == 0) {
            trendAnalysis.put("trend", "stable");
            trendAnalysis.put("growthRate", 0.0);
            trendAnalysis.put("peakDay", null);
        } else {
            // 鍓嶅崐娈?vs 鍚庡崐娈靛垽鏂秼鍔挎柟鍚?            int half = timeSeries.size() / 2;
            long firstHalf = timeSeries.subList(0, Math.max(1, half)).stream()
                    .mapToLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue()).sum();
            long secondHalf = timeSeries.subList(half, timeSeries.size()).stream()
                    .mapToLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue()).sum();
            String trend;
            if (secondHalf > firstHalf * 1.1) {
                trend = "up";
            } else if (secondHalf < firstHalf * 0.9) {
                trend = "down";
            } else {
                trend = "stable";
            }
            trendAnalysis.put("trend", trend);
            trendAnalysis.put("growthRate", calcChangeRate(secondHalf, firstHalf));
            // 宄板€兼棩锛氳鍗曟暟鏈€澶氱殑鏃ユ湡
            String peakDay = timeSeries.stream()
                    .max(Comparator.comparingLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue()))
                    .map(m -> String.valueOf(m.get("date")))
                    .orElse(null);
            trendAnalysis.put("peakDay", peakDay);
        }
        // peakHour 鏆傛棤灏忔椂绾ф暟鎹紝淇濇寔 null
        trendAnalysis.put("peakHour", null);

        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("timeSeries", timeSeries);
        trends.put("summary", summary);
        trends.put("comparison", comparison);
        trends.put("trendAnalysis", trendAnalysis);
        trends.put("orderTypeDistribution", orderTypeRatio);
        return trends;
    }

    /**
     * 璁＄畻鍚屾瘮/鐜瘮鍙樺寲鐜?     * @param current 褰撳墠鍊?     * @param previous 涓婁竴鍛ㄦ湡鍊?     * @return 鍙樺寲鐜囩櫨鍒嗘瘮锛堜繚鐣欎袱浣嶅皬鏁帮級锛屼笂涓€鍛ㄦ湡涓?0 鏃惰繑鍥?0
     */
    private double calcChangeRate(double current, double previous) {
        if (previous == 0) {
            return 0.0;
        }
        return Math.round((current - previous) / previous * 100.0 * 100.0) / 100.0;
    }

    // ==================== 绉佹湁杈呭姪鏂规硶 ====================

    /**
     * 鑾峰彇骞舵牎楠岃鍗曟槸鍚﹀瓨鍦?     */
    private OrderNew getAndValidateOrder(String orderId) {
        OrderNew order = orderNewMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("璁㈠崟涓嶅瓨鍦紝ID: " + orderId);
        }
        return order;
    }

    /**
     * 鏍￠獙妗屽彴鏄惁鍙敤
     */
    private void validateTableAvailable(Long tableId) {
        DiningTableNew table = diningTableNewMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("妗屽彴涓嶅瓨鍦?);
        }
        if (table.getStatus() == null || table.getStatus() != 1) {
            throw new RuntimeException("妗屽彴褰撳墠涓嶅彲鐢紝鐘舵€? " + table.getStatus());
        }
    }

    /**
     * 鍒ゆ柇璁㈠崟鏄惁澶勪簬鍙敮浠樼姸鎬?     */
    private boolean isPayableStatus(Integer orderStatus) {
        return orderStatus != null && (orderStatus == 0 || orderStatus == 1);
    }

    /**
     * 缁熻鎸囧畾鐘舵€佺殑璁㈠崟鏁伴噺
     */
    private long countOrdersByStatus(Integer status) {
        QueryWrapper<OrderNew> wrapper = new QueryWrapper<>();
        wrapper.eq("order_status", status);
        return orderNewMapper.selectCount(wrapper);
    }

    /**
     * 璁＄畻璁㈠崟閲戦锛堝惈鍟嗗搧鏍￠獙锛?     * 瀹夊叏瑕佺偣锛?     * 1. 涓嶄俊浠诲鎴风浼犲叆鐨?unitPrice锛屼竴寰嬩互鏁版嵁搴?sale_price/combo_price 涓哄噯锛堥槻绡℃敼锛?     * 2. 鏍￠獙鍟嗗搧瀛樺湪鎬с€佸湪鍞姸鎬併€佸簱瀛橈紙闄愰噺鑿滃搧锛?     * 3. 鐢?DB 鍚嶇О瑕嗙洊瀹㈡埛绔?productName锛堢‘淇濆揩鐓у噯纭級
     */
    private OrderAmountCalculation calculateOrderAmount(List<OrderCreateDTO.OrderItemCreateDTO> items) {
        OrderAmountCalculation calc = new OrderAmountCalculation();
        long totalAmount = 0L;
        long discountAmount = 0L;

        for (OrderCreateDTO.OrderItemCreateDTO item : items) {
            // 鏍￠獙鍟嗗搧骞惰幏鍙栫湡瀹炰环鏍?            ValidatedFood food = validateAndPriceFood(item.getProductId(), item.getProductType(), item.getQuantity());
            // 鐢?DB 鐪熷疄鏁版嵁瑕嗙洊瀹㈡埛绔紶鍏ュ€硷紙闃茬鏀?+ 蹇収鍑嗙‘锛?            item.setUnitPrice(food.salePrice);
            item.setProductName(food.name);

            long itemAmount = food.salePrice * item.getQuantity();
            totalAmount += itemAmount;
            discountAmount += (item.getDiscountAmount() != null ? item.getDiscountAmount() : 0L);
        }

        calc.totalAmount = totalAmount;
        calc.discountAmount = discountAmount;
        calc.finalAmount = totalAmount - discountAmount;
        return calc;
    }

    /**
     * 鏍￠獙鍟嗗搧瀛樺湪鎬?鐘舵€?搴撳瓨锛岃繑鍥?DB 鐪熷疄鍚嶇О鍜屼环鏍?     * @param productId 鑿滃搧鎴栧椁怚D
     * @param productType 1鑿滃搧 2濂楅
     * @param quantity 璐拱鏁伴噺
     */
    private ValidatedFood validateAndPriceFood(Long productId, Integer productType, Integer quantity) {
        if (productType == null) {
            throw new RuntimeException("鍟嗗搧绫诲瀷涓嶈兘涓虹┖");
        }
        if (productId == null) {
            throw new RuntimeException("鍟嗗搧ID涓嶈兘涓虹┖");
        }
        if (productType == 1) {
            FoodNew food = foodNewMapper.selectById(productId);
            if (food == null) {
                throw new RuntimeException("鑿滃搧涓嶅瓨鍦? ID=" + productId);
            }
            if (food.getStatus() == null || food.getStatus() != 1) {
                throw new RuntimeException("鑿滃搧褰撳墠涓嶅彲璐拱锛堝凡鍋滃敭/鍞絼锛? " + food.getFoodName());
            }
            // 闄愰噺鑿滃搧搴撳瓨鏍￠獙锛坰tock > 0 琛ㄧず鍚敤搴撳瓨绠＄悊锛?            if (food.getStock() != null && food.getStock() > 0 && quantity > food.getStock()) {
                throw new RuntimeException("鑿滃搧搴撳瓨涓嶈冻: " + food.getFoodName() + " 鍓╀綑 " + food.getStock() + " 浠?);
            }
            return new ValidatedFood(food.getFoodName(), food.getSalePrice());
        } else if (productType == 2) {
            DishComboNew combo = dishComboNewMapper.selectById(productId);
            if (combo == null) {
                throw new RuntimeException("濂楅涓嶅瓨鍦? ID=" + productId);
            }
            if (combo.getStatus() == null || combo.getStatus() != 1) {
                throw new RuntimeException("濂楅褰撳墠涓嶅彲璐拱锛堝凡鍋滃敭/鍞絼锛? " + combo.getComboName());
            }
            return new ValidatedFood(combo.getComboName(), combo.getComboPrice());
        }
        throw new RuntimeException("鏈煡鍟嗗搧绫诲瀷: " + productType);
    }

    /**
     * 宸叉牎楠屽晢鍝侊紙鍚嶇О + 浠锋牸锛?     */
    private static class ValidatedFood {
        final String name;
        final Long salePrice;
        ValidatedFood(String name, Long salePrice) {
            this.name = name;
            this.salePrice = salePrice;
        }
    }

    /**
     * 鐢熸垚璁㈠崟缂栧彿
     * 鏍煎紡: ORD + 骞存湀鏃ユ椂鍒嗙 + 4浣嶅簭鍙?     */
    private String generateOrderCode() {
        String prefix = "ORD" + LocalDateTime.now().format(ORDER_CODE_FORMATTER);
        int sequence = orderNewMapper.getMaxTodaySequence(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    /**
     * 鏋勫缓璁㈠崟瀹炰綋
     */
    private OrderNew buildOrderEntity(OrderCreateDTO createDTO, OrderAmountCalculation calc, String orderCode) {
        OrderNew order = new OrderNew();
        order.setOrderCode(orderCode);
        order.setOrderType(createDTO.getOrderType());
        order.setOrderSource(createDTO.getOrderSource() != null ? createDTO.getOrderSource() : 1);
        order.setStoreId(createDTO.getStoreId());
        order.setCustomerId(createDTO.getCustomerId());
        order.setCustomerName(createDTO.getCustomerName());
        order.setCustomerPhone(createDTO.getCustomerPhone());
        order.setTableId(createDTO.getTableId());
        order.setDiningPeopleCount(createDTO.getDiningPeopleCount());
        order.setOrderStatus(0); // 寰呯‘璁?        order.setPaymentStatus(0); // 鏈敮浠?        order.setTotalAmount(calc.totalAmount);
        order.setDiscountAmount(calc.discountAmount);
        order.setDeliveryFee(0L);
        order.setPackagingFee(0L);
        order.setFinalAmount(calc.finalAmount);
        order.setPaidAmount(0L);
        order.setRefundAmount(0L);
        order.setDeliveryAddress(createDTO.getDeliveryAddress());
        order.setRemark(createDTO.getRemark());
        return order;
    }

    /**
     * 淇濆瓨璁㈠崟鏄庣粏
     */
    private void saveOrderItems(String orderId, List<OrderCreateDTO.OrderItemCreateDTO> items) {
        for (OrderCreateDTO.OrderItemCreateDTO item : items) {
            OrderItemNew orderItem = new OrderItemNew();
            orderItem.setOrderId(orderId);
            orderItem.setProductType(item.getProductType());
            if (item.getProductType() == 1) {
                orderItem.setFoodId(item.getProductId());
            } else {
                orderItem.setComboId(item.getProductId());
            }
            orderItem.setProductName(item.getProductName());
            orderItem.setSpecification(item.getSpecification());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setAmount(item.getUnitPrice() * item.getQuantity());
            orderItem.setDiscountAmount(item.getDiscountAmount() != null ? item.getDiscountAmount() : 0L);
            orderItem.setRemark(item.getRemark());
            orderItem.setKitchenStatus(0); // 寰呭埗浣?            orderItemNewMapper.insert(orderItem);
        }
    }

    /**
     * 鏋勫缓鏌ヨ鏉′欢鍖呰鍣?     */
    private QueryWrapper<OrderNew> buildQueryWrapper(OrderQueryDTO queryDTO) {
        QueryWrapper<OrderNew> wrapper = new QueryWrapper<>();

        if (queryDTO.getOrderCode() != null && !queryDTO.getOrderCode().isEmpty()) {
            wrapper.like("order_code", queryDTO.getOrderCode());
        }
        if (queryDTO.getOrderType() != null) {
            wrapper.eq("order_type", queryDTO.getOrderType());
        }
        if (queryDTO.getOrderStatus() != null) {
            wrapper.eq("order_status", queryDTO.getOrderStatus());
        }
        if (queryDTO.getPaymentStatus() != null) {
            wrapper.eq("payment_status", queryDTO.getPaymentStatus());
        }
        if (queryDTO.getCustomerId() != null) {
            wrapper.eq("customer_id", queryDTO.getCustomerId());
        }
        if (queryDTO.getTableId() != null) {
            wrapper.eq("table_id", queryDTO.getTableId());
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            wrapper.ge("create_time", queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            wrapper.le("create_time", queryDTO.getEndTime());
        }
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("final_amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("final_amount", queryDTO.getMaxAmount());
        }

        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    /**
     * 杞崲涓哄畬鏁磋鍗昖O锛堝惈鏄庣粏鍜屾敮浠樿褰曪級
     */
    private OrderVO convertToOrderVO(OrderNew order) {
        OrderVO vo = convertToSimpleOrderVO(order);

        // 鏌ヨ骞惰缃鍗曟槑缁?        List<OrderItemNew> items = orderItemNewMapper.selectByOrderId(order.getOrderId());
        vo.setItems(items.stream().map(this::convertToItemVO).collect(Collectors.toList()));

        // 鏌ヨ骞惰缃敮浠樿褰?        List<OrderPaymentRecordNew> payments = orderPaymentRecordNewMapper.selectByOrderId(order.getOrderId());
        vo.setPayments(payments.stream().map(this::convertToPaymentVO).collect(Collectors.toList()));

        // 鏌ヨ骞惰缃€€娆捐褰?        List<OrderRefundRecordNew> refunds = orderRefundRecordNewMapper.selectByOrderId(order.getOrderId());
        vo.setRefunds(refunds.stream().map(this::convertToRefundVO).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 杞崲涓虹畝鍗曡鍗昖O锛堜笉鍚槑缁嗭級
     */
    private OrderVO convertToSimpleOrderVO(OrderNew order) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderCode(order.getOrderCode());
        vo.setOrderType(order.getOrderType());
        vo.setOrderTypeName(getOrderTypeName(order.getOrderType()));
        vo.setOrderSource(order.getOrderSource());
        vo.setCustomerId(order.getCustomerId());
        vo.setCustomerName(order.getCustomerName());
        vo.setCustomerPhone(order.getCustomerPhone());
        vo.setTableId(order.getTableId());
        vo.setTableName(order.getTableName());
        vo.setDiningPeopleCount(order.getDiningPeopleCount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setPaymentStatusName(getPaymentStatusName(order.getPaymentStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPointsAmount(order.getPointsAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setPackagingFee(order.getPackagingFee());
        vo.setFinalAmount(order.getFinalAmount());
        vo.setPaidAmount(order.getPaidAmount());
        vo.setRefundAmount(order.getRefundAmount());
        vo.setPointsEarned(order.getPointsEarned());
        vo.setRemark(order.getRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setDeliveryAddress(order.getDeliveryAddress());
        vo.setExpectedTime(order.getExpectedTime());
        vo.setActualDeliveryTime(order.getActualDeliveryTime());
        vo.setCashierUserId(order.getCashierUserId());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    /**
     * 杞崲璁㈠崟鏄庣粏涓篤O
     */
    private OrderVO.OrderItemVO convertToItemVO(OrderItemNew item) {
        OrderVO.OrderItemVO vo = new OrderVO.OrderItemVO();
        vo.setItemId(item.getItemId());
        vo.setProductType(item.getProductType());
        vo.setProductTypeName(item.getProductType() == 1 ? "鍗曞搧" : "濂楅");
        vo.setFoodId(item.getFoodId());
        vo.setComboId(item.getComboId());
        vo.setProductName(item.getProductName());
        vo.setSpecification(item.getSpecification());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setAmount(item.getAmount());
        vo.setDiscountAmount(item.getDiscountAmount());
        vo.setRemark(item.getRemark());
        vo.setKitchenStatus(item.getKitchenStatus());
        vo.setKitchenStatusName(getKitchenStatusName(item.getKitchenStatus()));
        return vo;
    }

    /**
     * 杞崲鏀粯璁板綍涓篤O
     */
    private OrderVO.OrderPaymentRecordVO convertToPaymentVO(OrderPaymentRecordNew record) {
        OrderVO.OrderPaymentRecordVO vo = new OrderVO.OrderPaymentRecordVO();
        vo.setPaymentId(record.getPaymentId());
        vo.setPaymentMethod(record.getPaymentMethod());
        vo.setPaymentMethodName(getPaymentMethodName(record.getPaymentMethod()));
        vo.setPaymentAmount(record.getPaymentAmount());
        vo.setTransactionNo(record.getTransactionNo());
        vo.setPaymentTime(record.getPaymentTime());
        return vo;
    }

    /**
     * 杞崲閫€娆捐褰曚负VO
     */
    private OrderVO.OrderRefundRecordVO convertToRefundVO(OrderRefundRecordNew record) {
        OrderVO.OrderRefundRecordVO vo = new OrderVO.OrderRefundRecordVO();
        vo.setRefundId(record.getRefundId());
        vo.setRefundType(record.getRefundType());
        vo.setRefundTypeName(record.getRefundType() == 1 ? "鍏ㄩ閫€娆? : "閮ㄥ垎閫€娆?);
        vo.setRefundAmount(record.getRefundAmount());
        vo.setRefundReason(record.getRefundReason());
        vo.setRefundMethod(record.getRefundMethod());
        vo.setRefundStatus(record.getRefundStatus());
        vo.setRefundStatusName(getRefundStatusName(record.getRefundStatus()));
        vo.setCreateTime(record.getCreateTime());
        vo.setCompleteTime(record.getCompleteTime());
        return vo;
    }

    // ==================== 鐘舵€佸悕绉版槧灏?====================

    private String getOrderTypeName(Integer type) {
        if (type == null) return "鏈煡";
        switch (type) {
            case 1: return "鍫傞";
            case 2: return "澶栧崠";
            case 3: return "鑷彁";
            case 4: return "鎵撳寘";
            default: return "鏈煡";
        }
    }

    /**
     * POS绔鍗曠被鍨嬬爜 鈫?绠＄悊绔鍗曠被鍨嬬爜鏄犲皠
     * POS绔?orders_legacy.ORDER_TYPE: 0鍫傞 1澶栧崠 2鑷彁
     * 绠＄悊绔?orders.order_type: 1鍫傞 2澶栧崠 3鑷彁 4鎵撳寘
     */
    private Integer mapPosOrderTypeToAdmin(Integer posOrderType) {
        if (posOrderType == null) return null;
        switch (posOrderType) {
            case 0: return 1; // 鍫傞
            case 1: return 2; // 澶栧崠
            case 2: return 3; // 鑷彁
            default: return null;
        }
    }

    /**
     * 灏嗗瓧绗︿覆鏃ユ湡瑙ｆ瀽涓?LocalDateTime
     * 鏀寔鏍煎紡锛歽yyy-MM-dd HH:mm:ss 鍜?yyyy-MM-dd
     * PostgreSQL 鏃犳硶鐩存帴姣旇緝 TIMESTAMP 涓?VARCHAR锛岄渶瑕佹樉寮忚浆鎹负 LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String trimmed = dateStr.trim();
        try {
            if (trimmed.length() <= 10) {
                return LocalDate.parse(trimmed).atStartOfDay();
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.warn("瑙ｆ瀽鏃ユ湡瀛楃涓插け璐? {}, 閿欒: {}", dateStr, e.getMessage());
            return null;
        }
    }

    private String getOrderStatusName(Integer status) {
        if (status == null) return "鏈煡";
        switch (status) {
            case 0: return "寰呯‘璁?;
            case 1: return "宸茬‘璁?;
            case 2: return "宸插畬鎴?;
            case 3: return "宸插彇娑?;
            case 4: return "閮ㄥ垎閫€娆?;
            case 5: return "鍏ㄩ閫€娆?;
            case 6: return "寰呰瘎浠?;
            default: return "鏈煡";
        }
    }

    private String getPaymentStatusName(Integer status) {
        if (status == null) return "鏈煡";
        switch (status) {
            case 0: return "鏈敮浠?;
            case 1: return "閮ㄥ垎鏀粯";
            case 2: return "宸叉敮浠?;
            case 3: return "宸查€€娆?;
            default: return "鏈煡";
        }
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null) return "鏈煡";
        switch (method) {
            case 1: return "鐜伴噾";
            case 2: return "寰俊";
            case 3: return "鏀粯瀹?;
            case 4: return "閾惰鍗?;
            case 5: return "绉垎";
            case 6: return "娣峰悎鏀粯";
            default: return "鏈煡";
        }
    }

    private String getKitchenStatusName(Integer status) {
        if (status == null) return "鏈煡";
        switch (status) {
            case 0: return "寰呭埗浣?;
            case 1: return "鍒朵綔涓?;
            case 2: return "宸插畬鎴?;
            case 3: return "宸蹭笂鑿?;
            case 4: return "宸查€€娆?;
            default: return "鏈煡";
        }
    }

    private String getRefundStatusName(Integer status) {
        if (status == null) return "鏈煡";
        switch (status) {
            case 0: return "寰呭鏍?;
            case 1: return "宸插悓鎰?;
            case 2: return "宸叉嫆缁?;
            case 3: return "宸查€€娆?;
            default: return "鏈煡";
        }
    }

    /**
     * 璁㈠崟閲戦璁＄畻缁撴灉锛堝唴閮ㄧ被锛?     */
    private static class OrderAmountCalculation {
        long totalAmount;
        long discountAmount;
        long finalAmount;
    }
}
