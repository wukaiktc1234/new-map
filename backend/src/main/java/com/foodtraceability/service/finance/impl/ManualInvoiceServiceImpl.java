package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.finance.ManualInvoiceInputDTO;
import com.foodtraceability.dto.finance.ManualInvoiceQueryDTO;
import com.foodtraceability.dto.finance.ManualInvoiceStatsVO;
import com.foodtraceability.dto.finance.ManualInvoiceUpdateDTO;
import com.foodtraceability.dto.finance.ManualInvoiceVO;
import com.foodtraceability.entity.ElectronicInvoice;
import com.foodtraceability.entity.ElectronicVoucher;
import com.foodtraceability.entity.enums.VoucherStatus;
import com.foodtraceability.mapper.ElectronicInvoiceMapper;
import com.foodtraceability.mapper.ElectronicVoucherMapper;
import com.foodtraceability.service.finance.ManualInvoiceService;
import com.foodtraceability.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 手动输入发票服务实现
 *
 * 用于OCR识别失败或用户主动选择手动输入发票信息
 */
@Service
public class ManualInvoiceServiceImpl implements ManualInvoiceService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ManualInvoiceServiceImpl.class);

    /** 手动输入来源标识 */
    private static final String SOURCE_FILE_TYPE_MANUAL = "MANUAL";

    /** 验真状态：未验真 */
    private static final int VERIFY_STATUS_PENDING = 0;
    /** 验真状态：验真通过 */
    private static final int VERIFY_STATUS_VERIFIED = 1;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ElectronicVoucherMapper voucherMapper;
    private final ElectronicInvoiceMapper invoiceMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElectronicVoucher createFromManualInput(ManualInvoiceInputDTO input) {
        log.info("[手动输入] 开始创建电子凭证, 发票号码: {}", input.getInvoiceNo());
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        checkDuplicate(input.getInvoiceNo(), tenantId);
        ElectronicVoucher voucher = new ElectronicVoucher();
        voucher.setTenantId(tenantId);
        voucher.setVoucherType(mapInvoiceType(input.getInvoiceType()));
        voucher.setVoucherNo(input.getInvoiceNo());
        voucher.setSourceFileType(SOURCE_FILE_TYPE_MANUAL);
        voucher.setSourceFilePath("手动输入_" + input.getInvoiceNo());
        voucher.setStatus(VoucherStatus.PENDING.getCode());
        voucher.setSignatureStatus(0);
        voucher.setVerifyStatus(0);
        voucher.setIssueDate(input.getIssueDate());
        voucher.setCreatedBy(userId);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedBy(userId);
        voucher.setUpdatedAt(LocalDateTime.now());
        voucher.setDeleted(0);
        voucherMapper.insert(voucher);
        log.info("[手动输入] 电子凭证创建成功, ID: {}", voucher.getId());
        ElectronicInvoice invoice = createInvoiceEntity(input, voucher, tenantId, userId);
        invoiceMapper.insert(invoice);
        log.info("[手动输入] 发票信息创建成功, ID: {}", invoice.getId());
        log.info("[手动输入] 完成, 凭证ID: {}, 发票ID: {}", voucher.getId(), invoice.getId());
        return voucher;
    }

    @Override
    public IPage<ManualInvoiceVO> queryPage(ManualInvoiceQueryDTO query) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        int current = query.getCurrent() != null ? query.getCurrent() : 1;
        int size = query.getSize() != null ? query.getSize() : 10;
        Page<ElectronicVoucher> page = new Page<>(current, size);

        LambdaQueryWrapper<ElectronicVoucher> wrapper = buildQueryWrapper(query, tenantId);
        wrapper.orderByDesc(ElectronicVoucher::getCreatedAt);

        IPage<ElectronicVoucher> entityPage = voucherMapper.selectPage(page, wrapper);

        Page<ManualInvoiceVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<ManualInvoiceVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<ManualInvoiceVO> result = (IPage<ManualInvoiceVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public ManualInvoiceVO getDetail(Long id) {
        ElectronicVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "手动发票不存在");
        }
        return convertToVO(voucher);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, ManualInvoiceUpdateDTO dto) {
        log.info("[手动输入] 更新发票, 凭证ID: {}", id);
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        ElectronicVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "手动发票不存在");
        }

        // 查找关联的发票明细
        LambdaQueryWrapper<ElectronicInvoice> invoiceWrapper = new LambdaQueryWrapper<>();
        invoiceWrapper.eq(ElectronicInvoice::getVoucherId, id)
                .eq(ElectronicInvoice::getTenantId, tenantId);
        ElectronicInvoice invoice = invoiceMapper.selectOne(invoiceWrapper);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发票明细不存在");
        }

        // 更新发票明细字段
        if (dto.getInvoiceCode() != null) {
            invoice.setInvoiceCode(dto.getInvoiceCode());
        }
        if (dto.getInvoiceNo() != null) {
            invoice.setInvoiceNo(dto.getInvoiceNo());
            // 同步更新凭证编号
            voucher.setVoucherNo(dto.getInvoiceNo());
        }
        if (dto.getTotalAmount() != null) {
            BigDecimal totalYuan = fenToYuan(dto.getTotalAmount());
            invoice.setTotalAmount(totalYuan);
            voucher.setTotalAmount(totalYuan);
        }
        if (dto.getAmountWithoutTax() != null) {
            invoice.setAmountWithoutTax(fenToYuan(dto.getAmountWithoutTax()));
        }
        if (dto.getTaxAmount() != null) {
            invoice.setTaxAmount(fenToYuan(dto.getTaxAmount()));
        }
        if (dto.getRemark() != null) {
            invoice.setRemark(dto.getRemark());
        }
        invoice.setUpdatedBy(userId);
        invoice.setUpdatedAt(LocalDateTime.now());

        voucher.setUpdatedBy(userId);
        voucher.setUpdatedAt(LocalDateTime.now());

        int invoiceRows = invoiceMapper.updateById(invoice);
        int voucherRows = voucherMapper.updateById(voucher);
        log.info("[手动输入] 更新完成, 凭证ID: {}, 发票行数: {}, 凭证行数: {}", id, invoiceRows, voucherRows);
        return invoiceRows > 0 && voucherRows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        log.info("[手动输入] 删除发票, 凭证ID: {}", id);
        Long tenantId = SecurityUtils.getCurrentTenantId();

        ElectronicVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "手动发票不存在");
        }

        // 逻辑删除凭证（@TableLogic 自动处理）
        int voucherRows = voucherMapper.deleteById(id);

        // 逻辑删除关联的发票明细
        LambdaQueryWrapper<ElectronicInvoice> invoiceWrapper = new LambdaQueryWrapper<>();
        invoiceWrapper.eq(ElectronicInvoice::getVoucherId, id)
                .eq(ElectronicInvoice::getTenantId, tenantId);
        invoiceMapper.delete(invoiceWrapper);

        log.info("[手动输入] 删除完成, 凭证ID: {}", id);
        return voucherRows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verify(Long id) {
        log.info("[手动输入] 发票验真, 凭证ID: {}", id);
        Long userId = SecurityUtils.getCurrentUserId();

        ElectronicVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "手动发票不存在");
        }

        // 简化实现：直接标记为验真通过
        voucher.setVerifyStatus(VERIFY_STATUS_VERIFIED);
        voucher.setVerifyTime(LocalDateTime.now());
        voucher.setVerifyMessage("手动验真通过");
        voucher.setUpdatedBy(userId);
        voucher.setUpdatedAt(LocalDateTime.now());

        int rows = voucherMapper.updateById(voucher);
        log.info("[手动输入] 验真完成, 凭证ID: {}, 结果: {}", id, rows > 0);
        return rows > 0;
    }

    @Override
    public ManualInvoiceStatsVO getStats(String startDate, String endDate) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        ManualInvoiceStatsVO stats = new ManualInvoiceStatsVO();

        // 总数（带日期范围过滤）
        LambdaQueryWrapper<ElectronicVoucher> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ElectronicVoucher::getSourceFileType, SOURCE_FILE_TYPE_MANUAL)
                .eq(ElectronicVoucher::getTenantId, tenantId);
        applyDateRange(totalWrapper, startDate, endDate);
        stats.setTotal(voucherMapper.selectCount(totalWrapper));

        // 已验真数
        LambdaQueryWrapper<ElectronicVoucher> verifiedWrapper = new LambdaQueryWrapper<>();
        verifiedWrapper.eq(ElectronicVoucher::getSourceFileType, SOURCE_FILE_TYPE_MANUAL)
                .eq(ElectronicVoucher::getTenantId, tenantId)
                .eq(ElectronicVoucher::getVerifyStatus, VERIFY_STATUS_VERIFIED);
        applyDateRange(verifiedWrapper, startDate, endDate);
        stats.setVerified(voucherMapper.selectCount(verifiedWrapper));

        // 待验真数
        LambdaQueryWrapper<ElectronicVoucher> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(ElectronicVoucher::getSourceFileType, SOURCE_FILE_TYPE_MANUAL)
                .eq(ElectronicVoucher::getTenantId, tenantId)
                .eq(ElectronicVoucher::getVerifyStatus, VERIFY_STATUS_PENDING);
        applyDateRange(pendingWrapper, startDate, endDate);
        stats.setPending(voucherMapper.selectCount(pendingWrapper));

        return stats;
    }

    /**
     * 检查重复发票
     */
    private void checkDuplicate(String invoiceNo, Long tenantId) {
        LambdaQueryWrapper<ElectronicInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectronicInvoice::getInvoiceNo, invoiceNo).eq(ElectronicInvoice::getTenantId, tenantId).eq(ElectronicInvoice::getDeleted, 0);
        ElectronicInvoice existing = invoiceMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发票号码已存在，不能重复录入。已存在的凭证ID: " + existing.getVoucherId());
        }
    }

    /**
     * 创建发票实体
     */
    private ElectronicInvoice createInvoiceEntity(ManualInvoiceInputDTO input, ElectronicVoucher voucher, Long tenantId, Long userId) {
        ElectronicInvoice invoice = new ElectronicInvoice();
        invoice.setTenantId(tenantId);
        invoice.setVoucherId(voucher.getId());
        invoice.setInvoiceType(input.getInvoiceType());
        invoice.setInvoiceCode(input.getInvoiceCode());
        invoice.setInvoiceNo(input.getInvoiceNo());
        invoice.setIssueDate(input.getIssueDate());
        invoice.setCheckCode(input.getCheckCode());
        invoice.setTotalAmount(input.getTotalAmount());
        invoice.setAmountWithoutTax(input.getAmountWithoutTax());
        invoice.setTaxAmount(input.getTaxAmount());
        if (input.getBuyer() != null) {
            invoice.setBuyerName(input.getBuyer().getName());
            invoice.setBuyerTaxNo(input.getBuyer().getTaxNo());
            invoice.setBuyerAddress(input.getBuyer().getAddressPhone());
            invoice.setBuyerBank(input.getBuyer().getBankAccount());
        }
        if (input.getSeller() != null) {
            invoice.setSellerName(input.getSeller().getName());
            invoice.setSellerTaxNo(input.getSeller().getTaxNo());
            invoice.setSellerAddress(input.getSeller().getAddressPhone());
            invoice.setSellerBank(input.getSeller().getBankAccount());
        }
        invoice.setRemark(input.getRemark());
        invoice.setCreatedBy(userId);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setDeleted(0);
        log.info("[手动输入] 完成, 凭证ID: {}, 发票ID: {}", voucher.getId(), invoice.getId());
        return invoice;
    }

    /**
     * 映射发票类型
     */
    private String mapInvoiceType(String inputType) {
        if (inputType == null) {
            return "invoice";
        }
        switch (inputType.toLowerCase()) {
        case "vat_special":
            return "vat_special";
        case "vat_general":
            return "vat_general";
        case "full_electronic":
        case "invoice":
        default:
            return "invoice";
        }
    }

    /**
     * 构建分页查询条件
     */
    private LambdaQueryWrapper<ElectronicVoucher> buildQueryWrapper(ManualInvoiceQueryDTO query, Long tenantId) {
        LambdaQueryWrapper<ElectronicVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectronicVoucher::getSourceFileType, SOURCE_FILE_TYPE_MANUAL)
                .eq(ElectronicVoucher::getTenantId, tenantId);
        if (query.getInvoiceNo() != null && !query.getInvoiceNo().isBlank()) {
            wrapper.like(ElectronicVoucher::getVoucherNo, query.getInvoiceNo());
        }
        if (query.getInvoiceType() != null && !query.getInvoiceType().isBlank()) {
            wrapper.eq(ElectronicVoucher::getVoucherType, mapInvoiceType(query.getInvoiceType()));
        }
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate());
        return wrapper;
    }

    /**
     * 应用日期范围过滤到 issueDate 字段
     *
     * @param wrapper   查询构造器
     * @param startDate 起始日期（yyyy-MM-dd），可为空
     * @param endDate   结束日期（yyyy-MM-dd），可为空
     */
    private void applyDateRange(LambdaQueryWrapper<ElectronicVoucher> wrapper, String startDate, String endDate) {
        if (startDate != null && !startDate.isBlank()) {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            wrapper.ge(ElectronicVoucher::getIssueDate, start);
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            wrapper.le(ElectronicVoucher::getIssueDate, end);
        }
    }

    /**
     * 分（Long）转元（BigDecimal）
     *
     * @param fen 金额（分）
     * @return 金额（元）
     */
    private BigDecimal fenToYuan(Long fen) {
        return BigDecimal.valueOf(fen).movePointLeft(2);
    }

    /**
     * 实体转VO（含关联发票明细）
     */
    private ManualInvoiceVO convertToVO(ElectronicVoucher voucher) {
        ManualInvoiceVO vo = new ManualInvoiceVO();
        vo.setId(voucher.getId());
        vo.setVoucherNo(voucher.getVoucherNo());
        vo.setVoucherType(voucher.getVoucherType());
        vo.setIssueDate(voucher.getIssueDate());
        vo.setTotalAmount(voucher.getTotalAmount());
        vo.setStatus(voucher.getStatus());
        vo.setVerifyStatus(voucher.getVerifyStatus());
        vo.setVerifyTime(voucher.getVerifyTime());
        vo.setVerifyMessage(voucher.getVerifyMessage());
        vo.setCreatedAt(voucher.getCreatedAt());
        vo.setUpdatedAt(voucher.getUpdatedAt());

        // 查询关联发票明细补充字段
        LambdaQueryWrapper<ElectronicInvoice> invoiceWrapper = new LambdaQueryWrapper<>();
        invoiceWrapper.eq(ElectronicInvoice::getVoucherId, voucher.getId())
                .eq(ElectronicInvoice::getTenantId, voucher.getTenantId());
        ElectronicInvoice invoice = invoiceMapper.selectOne(invoiceWrapper);
        if (invoice != null) {
            vo.setInvoiceType(invoice.getInvoiceType());
            vo.setInvoiceCode(invoice.getInvoiceCode());
            vo.setInvoiceNo(invoice.getInvoiceNo());
            vo.setAmountWithoutTax(invoice.getAmountWithoutTax());
            vo.setTaxAmount(invoice.getTaxAmount());
            vo.setBuyerName(invoice.getBuyerName());
            vo.setSellerName(invoice.getSellerName());
            vo.setRemark(invoice.getRemark());
        }
        return vo;
    }

    public ManualInvoiceServiceImpl(final ElectronicVoucherMapper voucherMapper, final ElectronicInvoiceMapper invoiceMapper, final ObjectMapper objectMapper) {
        this.voucherMapper = voucherMapper;
        this.invoiceMapper = invoiceMapper;
        this.objectMapper = objectMapper;
    }
}
