package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.SupplierCreateDTO;
import com.foodtraceability.dto.SupplierQueryDTO;
import com.foodtraceability.dto.SupplierUpdateDTO;
import com.foodtraceability.dto.SupplierVO;
import com.foodtraceability.entity.Supplier;

import java.util.List;
import java.util.Map;

/**
 * 供应商服务接口
 * 提供供应商的CRUD、状态管理、资质管理等业务功能
 */
public interface SupplierService extends IService<Supplier> {

    /**
     * 分页查询供应商列表
     * @param queryDTO 查询条件
     * @return 分页结果（包含SupplierVO列表）
     */
    IPage<SupplierVO> getSupplierPage(SupplierQueryDTO queryDTO);

    /**
     * 获取供应商列表（不分页）
     * @param status 状态过滤（可选）
     * @return 供应商列表
     */
    List<Supplier> getSupplierList(Integer status);

    /**
     * 获取供应商详情
     * @param id 供应商主键ID
     * @return 供应商视图对象
     */
    SupplierVO getSupplierDetail(Long id);

    /**
     * 创建供应商
     * @param createDTO 创建数据
     * @return 创建后的供应商视图对象
     */
    SupplierVO createSupplier(SupplierCreateDTO createDTO);

    /**
     * 更新供应商信息
     * @param id 供应商主键ID
     * @param updateDTO 更新数据
     * @return 更新后的供应商视图对象
     */
    SupplierVO updateSupplier(Long id, SupplierUpdateDTO updateDTO);

    /**
     * 删除供应商（逻辑删除）
     * @param id 供应商主键ID
     */
    void deleteSupplier(Long id);

    /**
     * 批量删除供应商（逻辑删除）
     * @param ids 主键ID列表
     */
    void batchDeleteSuppliers(List<Long> ids);

    /**
     * 更新供应商状态
     * @param id 供应商主键ID
     * @param status 状态值（1合作中 0停用 2黑名单）
     * @return 更新后的供应商
     */
    Supplier updateSupplierStatus(Long id, Integer status);

    /**
     * 获取供应商统计信息
     * @return 统计数据Map
     */
    Map<String, Object> getStatistics();

    /**
     * 检查供应商编码是否已存在
     * @param supplierCode 供应商编码
     * @return 是否存在
     */
    boolean isSupplierCodeExists(String supplierCode);
}
