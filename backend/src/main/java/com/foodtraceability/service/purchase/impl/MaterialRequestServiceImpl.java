package com.foodtraceability.service.purchase.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.purchase.MaterialRequestCreateDTO;
import com.foodtraceability.dto.purchase.MaterialRequestDTO;
import com.foodtraceability.dto.purchase.MaterialRequestItemDTO;
import com.foodtraceability.dto.purchase.MaterialRequestQueryDTO;
import com.foodtraceability.dto.purchase.MaterialRequestUpdateDTO;
import com.foodtraceability.entity.MaterialRequest;
import com.foodtraceability.entity.MaterialRequestItem;
import com.foodtraceability.entity.PurchaseRequest;
import com.foodtraceability.entity.PurchaseRequestItem;
import com.foodtraceability.mapper.MaterialRequestItemMapper;
import com.foodtraceability.mapper.MaterialRequestMapper;
import com.foodtraceability.mapper.PurchaseRequestItemMapper;
import com.foodtraceability.mapper.PurchaseRequestMapper;
import com.foodtraceability.service.purchase.MaterialRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 物资需求提报服务实现
 *
 * <p>核心业务：
 * <ul>
 *   <li>分页查询：支持按提报单号/状态/门店/申请人/日期范围/关键词过滤</li>
 *   <li>创建：自动生成 requestNo（MR+yyyyMMdd+4位序号）+ 计算总金额 + 批量插入明细</li>
 *   <li>更新：仅草稿状态可更新，覆盖式更新明细（先删后插）</li>
 *   <li>删除：仅草稿状态可删除（逻辑删除主表+明细）</li>
 *   <li>审批流：submit(0→1) / approve(1→2) / reject(1→3)</li>
 *   <li>转采购申请：convert(2→4)，创建 PurchaseRequest + PurchaseRequestItem，记录 convertedRequestNo</li>
 * </ul>
 * </p>
 *
 * <p>金额转换：本表以"分"为单位（Long），转采购申请时需转为"元"（BigDecimal，除以100）。</p>
 */
@Service
public class MaterialRequestServiceImpl extends ServiceImpl<MaterialRequestMapper, MaterialRequest> implements MaterialRequestService {

    private static final Logger log = LoggerFactory.getLogger(MaterialRequestServiceImpl.class);

    /** 状态编码常量 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_CONVERTED = 4;

    private final MaterialRequestMapper requestMapper;
    private final MaterialRequestItemMapper itemMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final PurchaseRequestItemMapper purchaseRequestItemMapper;

    public MaterialRequestServiceImpl(MaterialRequestMapper requestMapper,
                                       MaterialRequestItemMapper itemMapper,
                                       PurchaseRequestMapper purchaseRequestMapper,
                                       PurchaseRequestItemMapper purchaseRequestItemMapper) {
        this.requestMapper = requestMapper;
        this.itemMapper = itemMapper;
        this.purchaseRequestMapper = purchaseRequestMapper;
        this.purchaseRequestItemMapper = purchaseRequestItemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<MaterialRequestDTO> getPage(MaterialRequestQueryDTO queryDTO) {
        Page<MaterialRequest> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<MaterialRequest> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StringUtils.hasText(queryDTO.getRequestNo())) {
            wrapper.like(MaterialRequest::getRequestNo, queryDTO.getRequestNo());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(MaterialRequest::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getStoreName())) {
            wrapper.like(MaterialRequest::getStoreName, queryDTO.getStoreName());
        }
        if (queryDTO.getApplicantId() != null) {
            wrapper.eq(MaterialRequest::getApplicantId, queryDTO.getApplicantId());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(MaterialRequest::getCreateTime, queryDTO.getStartDate().atStartOfDay());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(MaterialRequest::getCreateTime, queryDTO.getEndDate().plusDays(1).atStartOfDay());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(MaterialRequest::getRequestNo, queryDTO.getKeyword())
                    .or().like(MaterialRequest::getTitle, queryDTO.getKeyword()));
        }

        wrapper.orderByDesc(MaterialRequest::getCreateTime);
        IPage<MaterialRequest> requestPage = requestMapper.selectPage(page, wrapper);

        // 转换为 DTO（列表不含明细，items=null）
        Page<MaterialRequestDTO> dtoPage = new Page<>(requestPage.getCurrent(), requestPage.getSize(), requestPage.getTotal());
        List<MaterialRequestDTO> dtoList = requestPage.getRecords().stream()
                .map(this::convertToDTOWithoutItems)
                .toList();
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialRequestDTO getById(Long id) {
        MaterialRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        MaterialRequestDTO dto = convertToDTOWithoutItems(request);

        // 查询明细
        List<MaterialRequestItem> items = itemMapper.selectByRequestId(id);
        dto.setItems(items.stream().map(this::convertItemToVO).toList());
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialRequestDTO create(MaterialRequestCreateDTO createDTO) {
        // 生成提报单号
        String requestNo = generateRequestNo();

        // 计算总金额（分）
        long totalAmountFen = 0L;
        for (MaterialRequestItemDTO item : createDTO.getItems()) {
            BigDecimal itemTotal = item.getQuantity().multiply(BigDecimal.valueOf(item.getEstimatedPrice()));
            totalAmountFen += itemTotal.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }

        // 构建主表实体
        MaterialRequest request = new MaterialRequest();
        request.setRequestNo(requestNo);
        request.setTitle(createDTO.getTitle());
        request.setStoreName(createDTO.getStoreName());
        request.setApplicantId(createDTO.getApplicantId());
        request.setApplicantName(createDTO.getApplicantName());
        request.setExpectedDate(createDTO.getExpectedDate());
        request.setStatus(STATUS_DRAFT);
        request.setTotalAmount(totalAmountFen);
        request.setRemark(createDTO.getRemark());
        request.setConvertedRequestNo(null);

        requestMapper.insert(request);
        log.info("创建物资需求提报成功，requestNo={}, itemCount={}, totalAmount={}分",
                requestNo, createDTO.getItems().size(), totalAmountFen);

        // 批量插入明细
        List<MaterialRequestItem> itemEntities = new ArrayList<>(createDTO.getItems().size());
        for (MaterialRequestItemDTO itemDTO : createDTO.getItems()) {
            MaterialRequestItem item = convertDTOToItem(itemDTO);
            item.setRequestId(request.getRequestId());
            itemEntities.add(item);
        }
        for (MaterialRequestItem item : itemEntities) {
            itemMapper.insert(item);
        }

        // 返回 DTO（含明细）
        MaterialRequestDTO dto = convertToDTOWithoutItems(request);
        dto.setItems(itemEntities.stream().map(this::convertItemToVO).toList());
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialRequestDTO update(Long id, MaterialRequestUpdateDTO updateDTO) {
        MaterialRequest existing = requestMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_CANNOT_MODIFY);
        }

        // 重新计算总金额
        long totalAmountFen = 0L;
        for (MaterialRequestItemDTO item : updateDTO.getItems()) {
            BigDecimal itemTotal = item.getQuantity().multiply(BigDecimal.valueOf(item.getEstimatedPrice()));
            totalAmountFen += itemTotal.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }

        // 更新主表
        existing.setTitle(updateDTO.getTitle());
        existing.setStoreName(updateDTO.getStoreName());
        existing.setExpectedDate(updateDTO.getExpectedDate());
        existing.setRemark(updateDTO.getRemark());
        existing.setTotalAmount(totalAmountFen);
        requestMapper.updateById(existing);

        // 覆盖式更新明细：先逻辑删除旧明细，再插入新明细
        LambdaQueryWrapper<MaterialRequestItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(MaterialRequestItem::getRequestId, id);
        itemMapper.delete(deleteWrapper);

        for (MaterialRequestItemDTO itemDTO : updateDTO.getItems()) {
            MaterialRequestItem item = convertDTOToItem(itemDTO);
            item.setRequestId(id);
            itemMapper.insert(item);
        }

        log.info("更新物资需求提报成功，id={}, itemCount={}", id, updateDTO.getItems().size());

        // 返回最新 DTO
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialRequest existing = requestMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_CANNOT_DELETE);
        }

        // 逻辑删除明细
        LambdaQueryWrapper<MaterialRequestItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MaterialRequestItem::getRequestId, id);
        itemMapper.delete(itemWrapper);

        // 逻辑删除主表
        requestMapper.deleteById(id);
        log.info("删除物资需求提报成功，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        MaterialRequest existing = requestMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_CANNOT_SUBMIT);
        }
        existing.setStatus(STATUS_PENDING);
        requestMapper.updateById(existing);
        log.info("物资需求提报提交审核成功，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        MaterialRequest existing = requestMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        if (existing.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_CANNOT_APPROVE);
        }
        existing.setStatus(STATUS_APPROVED);
        requestMapper.updateById(existing);
        log.info("物资需求提报审核通过，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        MaterialRequest existing = requestMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        if (existing.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_CANNOT_APPROVE);
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回原因不能为空");
        }
        existing.setStatus(STATUS_REJECTED);
        // 驳回原因记录到 remark 末尾（提报表无独立 reject_reason 字段）
        String originRemark = existing.getRemark();
        String rejectMark = "[驳回原因：" + reason + "]";
        existing.setRemark(StringUtils.hasText(originRemark) ? originRemark + " " + rejectMark : rejectMark);
        requestMapper.updateById(existing);
        log.info("物资需求提报审核驳回，id={}, reason={}", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> convertToPurchaseRequest(Long id) {
        // 校验提报存在且状态为已审核
        MaterialRequest existing = requestMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_NOT_FOUND);
        }
        if (existing.getStatus() != STATUS_APPROVED) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_CANNOT_CONVERT);
        }

        // 查询提报明细
        List<MaterialRequestItem> items = itemMapper.selectByRequestId(id);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.MATERIAL_REQUEST_ITEMS_EMPTY);
        }

        // 创建采购申请主表（PurchaseRequest 使用 String UUID 主键 + BigDecimal 元金额）
        String purchaseRequestId = UUID.randomUUID().toString().replace("-", "");
        String purchaseRequestNo = generatePurchaseRequestNo();

        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setRequestId(purchaseRequestId);
        purchaseRequest.setRequestNo(purchaseRequestNo);
        purchaseRequest.setTitle(existing.getTitle());
        purchaseRequest.setRequestType("routine");
        purchaseRequest.setDepartmentId(null);
        purchaseRequest.setDepartmentName(existing.getStoreName());
        // 申请人ID：Long → String（PurchaseRequest.applicantId 为 String）
        purchaseRequest.setApplicantId(existing.getApplicantId() != null ? String.valueOf(existing.getApplicantId()) : null);
        purchaseRequest.setApplicantName(existing.getApplicantName());
        // 总金额：分（Long）→ 分（Long），单位一致直接传递
        purchaseRequest.setTotalAmount(existing.getTotalAmount());
        purchaseRequest.setStatus("draft");
        purchaseRequest.setPriority("normal");
        purchaseRequest.setExpectedDate(existing.getExpectedDate());
        purchaseRequest.setDescription("由物资需求提报[" + existing.getRequestNo() + "]转换生成");
        purchaseRequest.setCreateTime(LocalDateTime.now());
        purchaseRequest.setUpdateTime(LocalDateTime.now());

        purchaseRequestMapper.insert(purchaseRequest);
        log.info("物资需求提报转采购申请成功，materialRequestId={}, purchaseRequestNo={}",
                id, purchaseRequestNo);

        // 创建采购申请明细（PurchaseRequestItem 使用 String UUID 主键 + BigDecimal 元金额）
        for (MaterialRequestItem srcItem : items) {
            PurchaseRequestItem destItem = new PurchaseRequestItem();
            destItem.setItemId(UUID.randomUUID().toString().replace("-", ""));
            destItem.setRequestId(purchaseRequestId);
            // materialId( Long) → foodId(String)
            destItem.setFoodId(srcItem.getMaterialId() != null ? String.valueOf(srcItem.getMaterialId()) : null);
            destItem.setFoodName(srcItem.getMaterialName());
            destItem.setSpecification(srcItem.getSpecification());
            destItem.setQuantity(srcItem.getQuantity());
            destItem.setUnit(srcItem.getUnit());
            // 预估单价：分（Long）→ 元（BigDecimal）
            destItem.setEstimatedPrice(BigDecimal.valueOf(srcItem.getEstimatedPrice())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            // 小计金额：分（Long）→ 元（BigDecimal）
            BigDecimal subtotalYuan = BigDecimal.valueOf(srcItem.getEstimatedPrice())
                    .multiply(srcItem.getQuantity())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            destItem.setSubtotalAmount(subtotalYuan);
            destItem.setRemark(srcItem.getRemark());
            destItem.setCreateTime(LocalDateTime.now());
            destItem.setUpdateTime(LocalDateTime.now());
            purchaseRequestItemMapper.insert(destItem);
        }

        // 更新提报状态为"已转采购申请"并记录转换后的采购申请单号
        existing.setStatus(STATUS_CONVERTED);
        existing.setConvertedRequestNo(purchaseRequestNo);
        requestMapper.updateById(existing);

        // 返回转换结果
        Map<String, Object> result = new HashMap<>();
        result.put("materialRequestId", id);
        result.put("materialRequestNo", existing.getRequestNo());
        result.put("purchaseRequestId", purchaseRequestId);
        result.put("purchaseRequestNo", purchaseRequestNo);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("draft", countByStatus(STATUS_DRAFT));
        stats.put("pending", countByStatus(STATUS_PENDING));
        stats.put("approved", countByStatus(STATUS_APPROVED));
        stats.put("rejected", countByStatus(STATUS_REJECTED));
        stats.put("converted", countByStatus(STATUS_CONVERTED));
        stats.put("total", requestMapper.selectCount(null));
        return stats;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 按状态统计数量
     */
    private Long countByStatus(int status) {
        LambdaQueryWrapper<MaterialRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialRequest::getStatus, status);
        return requestMapper.selectCount(wrapper);
    }

    /**
     * 生成提报单号：MR + yyyyMMdd + 4位序号
     */
    private String generateRequestNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "MR" + dateStr;

        LambdaQueryWrapper<MaterialRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(MaterialRequest::getRequestNo, prefix);
        wrapper.orderByDesc(MaterialRequest::getRequestNo);
        wrapper.last("LIMIT 1");
        MaterialRequest last = requestMapper.selectOne(wrapper);

        int seq = 1;
        if (last != null && last.getRequestNo() != null) {
            String lastNo = last.getRequestNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * 生成采购申请单号：PR + yyyyMMdd + 4位序号
     * <p>注意：PurchaseRequest 主表也使用此方法生成单号，这里独立实现避免循环依赖。</p>
     */
    private String generatePurchaseRequestNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PR" + dateStr;

        LambdaQueryWrapper<PurchaseRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseRequest::getRequestNo, prefix);
        wrapper.orderByDesc(PurchaseRequest::getRequestNo);
        wrapper.last("LIMIT 1");
        PurchaseRequest last = purchaseRequestMapper.selectOne(wrapper);

        int seq = 1;
        if (last != null && last.getRequestNo() != null) {
            String lastNo = last.getRequestNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /**
     * Entity → DTO（不含明细）
     */
    private MaterialRequestDTO convertToDTOWithoutItems(MaterialRequest request) {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setRequestId(request.getRequestId());
        dto.setRequestNo(request.getRequestNo());
        dto.setTitle(request.getTitle());
        dto.setStoreName(request.getStoreName());
        dto.setApplicantId(request.getApplicantId());
        dto.setApplicantName(request.getApplicantName());
        dto.setExpectedDate(request.getExpectedDate());
        dto.setStatus(request.getStatus());
        dto.setTotalAmount(request.getTotalAmount());
        dto.setConvertedRequestNo(request.getConvertedRequestNo());
        dto.setRemark(request.getRemark());
        dto.setCreateTime(request.getCreateTime());
        dto.setUpdateTime(request.getUpdateTime());
        return dto;
    }

    /**
     * Item Entity → Item VO
     */
    private MaterialRequestDTO.MaterialRequestItemVO convertItemToVO(MaterialRequestItem item) {
        MaterialRequestDTO.MaterialRequestItemVO vo = new MaterialRequestDTO.MaterialRequestItemVO();
        vo.setItemId(item.getItemId());
        vo.setRequestId(item.getRequestId());
        vo.setMaterialId(item.getMaterialId());
        vo.setMaterialName(item.getMaterialName());
        vo.setSpecification(item.getSpecification());
        vo.setQuantity(item.getQuantity());
        vo.setUnit(item.getUnit());
        vo.setEstimatedPrice(item.getEstimatedPrice());
        vo.setSubtotalAmount(item.getSubtotalAmount());
        vo.setRemark(item.getRemark());
        return vo;
    }

    /**
     * Item DTO → Item Entity（未设置 requestId，由调用方设置）
     */
    private MaterialRequestItem convertDTOToItem(MaterialRequestItemDTO dto) {
        MaterialRequestItem item = new MaterialRequestItem();
        item.setMaterialId(dto.getMaterialId());
        item.setMaterialName(dto.getMaterialName());
        item.setSpecification(dto.getSpecification());
        item.setQuantity(dto.getQuantity());
        item.setUnit(dto.getUnit());
        item.setEstimatedPrice(dto.getEstimatedPrice());
        // 小计金额 = quantity × estimatedPrice（分）
        BigDecimal subtotal = dto.getQuantity().multiply(BigDecimal.valueOf(dto.getEstimatedPrice()));
        item.setSubtotalAmount(subtotal.setScale(0, RoundingMode.HALF_UP).longValueExact());
        item.setRemark(dto.getRemark());
        return item;
    }
}
