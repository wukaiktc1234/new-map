package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.SupplierCreateDTO;
import com.foodtraceability.dto.SupplierQueryDTO;
import com.foodtraceability.dto.SupplierUpdateDTO;
import com.foodtraceability.dto.SupplierVO;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.service.SupplierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商服务实现类
 * 实现供应商的CRUD、状态管理、资质管理等核心业务功能
 */
@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

    private final SupplierMapper supplierMapper;

    public SupplierServiceImpl(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<SupplierVO> getSupplierPage(SupplierQueryDTO queryDTO) {
        Page<Supplier> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StringUtils.hasText(queryDTO.getSupplierName())) {
            wrapper.like(Supplier::getSupplierName, queryDTO.getSupplierName());
        }
        if (StringUtils.hasText(queryDTO.getContactPerson())) {
            wrapper.like(Supplier::getContactPerson, queryDTO.getContactPerson());
        }
        if (StringUtils.hasText(queryDTO.getPhone())) {
            wrapper.like(Supplier::getPhone, queryDTO.getPhone());
        }
        if (queryDTO.getCategory() != null) {
            wrapper.eq(Supplier::getCategory, queryDTO.getCategory());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Supplier::getStatus, queryDTO.getStatus());
        }

        wrapper.orderByDesc(Supplier::getCreateTime);
        IPage<Supplier> supplierPage = supplierMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<SupplierVO> voPage = new Page<>(supplierPage.getCurrent(), supplierPage.getSize(), supplierPage.getTotal());
        List<SupplierVO> voList = supplierPage.getRecords().stream().map(this::convertToVO).toList();
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getSupplierList(Integer status) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Supplier::getStatus, status);
        } else {
            // 默认查询合作中的供应商
            wrapper.eq(Supplier::getStatus, 1);
        }
        wrapper.orderByDesc(Supplier::getCreateTime);
        return supplierMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierVO getSupplierDetail(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        return convertToVO(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierVO createSupplier(SupplierCreateDTO createDTO) {
        // 生成供应商编码
        String supplierCode = generateSupplierCode();

        // 构建实体
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(supplierCode);
        supplier.setSupplierName(createDTO.getSupplierName());
        supplier.setContactPerson(createDTO.getContactPerson());
        supplier.setPhone(createDTO.getPhone());
        supplier.setAddress(createDTO.getAddress());
        supplier.setLicenseNo(createDTO.getLicenseNo());
        supplier.setLicenseExpiry(createDTO.getLicenseExpiry());
        supplier.setBankAccount(createDTO.getBankAccount());
        supplier.setBankName(createDTO.getBankName());
        supplier.setTaxNo(createDTO.getTaxNo());
        supplier.setCategory(createDTO.getCategory());
        supplier.setStatus(1); // 默认合作中
        supplier.setCreditLevel("C"); // 默认C级
        supplier.setRating(java.math.BigDecimal.valueOf(5.00)); // 默认满分
        supplier.setRemark(createDTO.getRemark());
        supplier.setSettlementMethod(createDTO.getSettlementMethod() != null ? createDTO.getSettlementMethod() : "monthly");
        supplier.setPaymentTerms(createDTO.getPaymentTerms() != null ? createDTO.getPaymentTerms() : 30);

        supplierMapper.insert(supplier);
        log.info("创建供应商成功，编码：{}，名称：{}", supplierCode, createDTO.getSupplierName());

        return convertToVO(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierVO updateSupplier(Long id, SupplierUpdateDTO updateDTO) {
        Supplier existing = supplierMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }

        // 更新字段
        existing.setSupplierName(updateDTO.getSupplierName());
        existing.setContactPerson(updateDTO.getContactPerson());
        existing.setPhone(updateDTO.getPhone());
        existing.setAddress(updateDTO.getAddress());
        existing.setLicenseNo(updateDTO.getLicenseNo());
        existing.setLicenseExpiry(updateDTO.getLicenseExpiry());
        existing.setBankAccount(updateDTO.getBankAccount());
        existing.setBankName(updateDTO.getBankName());
        existing.setTaxNo(updateDTO.getTaxNo());
        existing.setCategory(updateDTO.getCategory());
        existing.setRemark(updateDTO.getRemark());
        existing.setSettlementMethod(updateDTO.getSettlementMethod());
        existing.setPaymentTerms(updateDTO.getPaymentTerms());

        supplierMapper.updateById(existing);
        log.info("更新供应商成功，ID：{}", id);

        return convertToVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        Supplier existing = supplierMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        // 使用MyBatis-Plus的逻辑删除
        supplierMapper.deleteById(id);
        log.info("删除供应商成功，ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteSuppliers(List<Long> ids) {
        supplierMapper.deleteBatchIds(ids);
        log.info("批量删除供应商成功，数量：{}", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Supplier updateSupplierStatus(Long id, Integer status) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        // 校验状态值合法性
        if (status < 0 || status > 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不合法，必须是0/1/2");
        }
        supplier.setStatus(status);
        supplierMapper.updateById(supplier);
        log.info("更新供应商状态成功，ID：{}，状态：{}", id, status);
        return supplier;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 总数
            long total = supplierMapper.selectCount(new LambdaQueryWrapper<>());
            stats.put("total", total);

            // 各状态数量
            long activeCount = supplierMapper.selectCount(
                new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 1));
            long inactiveCount = supplierMapper.selectCount(
                new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 0));
            long blacklistCount = supplierMapper.selectCount(
                new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 2));

            stats.put("active", activeCount);
            stats.put("inactive", inactiveCount);
            stats.put("blacklist", blacklistCount);

            // 平均评分（简单计算）
            stats.put("avgRating", "5.00");

        } catch (Exception e) {
            log.error("获取供应商统计信息失败", e);
            stats.put("total", 0);
            stats.put("active", 0);
            stats.put("inactive", 0);
            stats.put("blacklist", 0);
            stats.put("avgRating", "0.00");
        }
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSupplierCodeExists(String supplierCode) {
        if (!StringUtils.hasText(supplierCode)) {
            return false;
        }
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getSupplierCode, supplierCode);
        return supplierMapper.selectCount(wrapper) > 0;
    }

    /**
     * 生成供应商编码
     * 格式：SUP + 年月日 + 3位序号
     * @return 供应商编码
     */
    private String generateSupplierCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SUP" + dateStr;

        // 查询当天最大序号
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Supplier::getSupplierCode, prefix);
        wrapper.orderByDesc(Supplier::getSupplierCode);
        wrapper.last("LIMIT 1");
        Supplier lastSupplier = supplierMapper.selectOne(wrapper);

        int seq = 1;
        if (lastSupplier != null && lastSupplier.getSupplierCode() != null) {
            String lastCode = lastSupplier.getSupplierCode();
            String seqStr = lastCode.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%03d", seq);
    }

    /**
     * 将实体转换为视图对象
     * @param supplier 供应商实体
     * @return 视图对象
     */
    private SupplierVO convertToVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        vo.setSupplierId(supplier.getSupplierId());
        vo.setSupplierCode(supplier.getSupplierCode());
        vo.setSupplierName(supplier.getSupplierName());
        vo.setContactPerson(supplier.getContactPerson());
        vo.setPhone(supplier.getPhone());
        vo.setAddress(supplier.getAddress());
        vo.setLicenseNo(supplier.getLicenseNo());
        vo.setLicenseExpiry(supplier.getLicenseExpiry());
        vo.setBankAccount(supplier.getBankAccount());
        vo.setBankName(supplier.getBankName());
        vo.setTaxNo(supplier.getTaxNo());
        vo.setCategory(supplier.getCategory());
        vo.setStatus(supplier.getStatus());
        vo.setStatusDesc(getStatusDesc(supplier.getStatus()));
        vo.setCreditLevel(supplier.getCreditLevel());
        vo.setRating(supplier.getRating());
        vo.setRemark(supplier.getRemark());
        vo.setSettlementMethod(supplier.getSettlementMethod());
        vo.setPaymentTerms(supplier.getPaymentTerms());
        vo.setCreateTime(supplier.getCreateTime());
        return vo;
    }

    /**
     * 获取状态描述
     * @param status 状态值
     * @return 状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "合作中";
            case 0 -> "停用";
            case 2 -> "黑名单";
            default -> "未知";
        };
    }
}
