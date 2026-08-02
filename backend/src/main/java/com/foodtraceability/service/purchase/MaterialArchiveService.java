package com.foodtraceability.service.purchase;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.purchase.MaterialArchiveCreateDTO;
import com.foodtraceability.dto.purchase.MaterialArchiveQueryDTO;
import com.foodtraceability.dto.purchase.MaterialArchiveUpdateDTO;
import com.foodtraceability.dto.purchase.MaterialArchiveVO;

/**
 * 商品档案 Service 接口
 * 提供商品档案的 CRUD + 状态切换 + 启用列表查询
 */
public interface MaterialArchiveService {

    /**
     * 分页查询商品档案
     * @param queryDTO 查询条件
     * @return 分页结果（含分类名/供应商名/状态中文名）
     */
    PageResult<MaterialArchiveVO> getArchivePage(MaterialArchiveQueryDTO queryDTO);

    /**
     * 根据 ID 查询商品档案详情
     * @param materialId 商品ID
     * @return 商品档案 VO（含关联字段），不存在返回 null
     */
    MaterialArchiveVO getArchiveById(Long materialId);

    /**
     * 创建商品档案
     * @param createDTO 创建请求
     * @return 创建后的 VO（含生成的 materialId 和 materialCode）
     */
    MaterialArchiveVO createArchive(MaterialArchiveCreateDTO createDTO);

    /**
     * 更新商品档案（部分更新）
     * @param materialId 商品ID
     * @param updateDTO 更新请求
     * @return 更新后的 VO
     */
    MaterialArchiveVO updateArchive(Long materialId, MaterialArchiveUpdateDTO updateDTO);

    /**
     * 逻辑删除商品档案
     * @param materialId 商品ID
     */
    void deleteArchive(Long materialId);

    /**
     * 更新商品状态（启用/停用）
     * @param materialId 商品ID
     * @param status 状态：1启用 0停用
     */
    void updateStatus(Long materialId, Integer status);

    /**
     * 确保物料档案存在对应的库存初始记录
     * <p>创建物料档案时调用，若库存中无对应记录则生成初始库存（current_stock=0）。</p>
     *
     * @param material 物料档案实体
     */
    void ensureInventoryForMaterial(com.foodtraceability.entity.MaterialArchive material);
}
