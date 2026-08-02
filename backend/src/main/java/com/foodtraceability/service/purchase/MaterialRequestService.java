package com.foodtraceability.service.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.purchase.MaterialRequestCreateDTO;
import com.foodtraceability.dto.purchase.MaterialRequestDTO;
import com.foodtraceability.dto.purchase.MaterialRequestQueryDTO;
import com.foodtraceability.dto.purchase.MaterialRequestUpdateDTO;

import java.util.Map;

/**
 * 物资需求提报服务接口
 *
 * <p>状态流转：
 * <ul>
 *   <li>draft(0) → pending(1)：submit（提交审核）</li>
 *   <li>pending(1) → approved(2)：approve（审核通过）</li>
 *   <li>pending(1) → rejected(3)：reject（审核驳回）</li>
 *   <li>approved(2) → converted(4)：convertToPurchaseRequest（转采购申请）</li>
 * </ul>
 * </p>
 *
 * <p>编号生成：MR + yyyyMMdd + 4位序号</p>
 */
public interface MaterialRequestService {

    /**
     * 分页查询物资需求提报列表（不含明细）
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<MaterialRequestDTO> getPage(MaterialRequestQueryDTO queryDTO);

    /**
     * 查询物资需求提报详情（含明细列表）
     * @param id 提报ID
     * @return 提报详情
     */
    MaterialRequestDTO getById(Long id);

    /**
     * 创建物资需求提报（状态=0 草稿）
     * @param createDTO 创建参数
     * @return 创建后的提报
     */
    MaterialRequestDTO create(MaterialRequestCreateDTO createDTO);

    /**
     * 更新物资需求提报（仅 status=0 草稿状态下可更新）
     * @param id 提报ID
     * @param updateDTO 更新参数
     * @return 更新后的提报
     */
    MaterialRequestDTO update(Long id, MaterialRequestUpdateDTO updateDTO);

    /**
     * 删除物资需求提报（仅 status=0 草稿状态下可删除，逻辑删除）
     * @param id 提报ID
     */
    void delete(Long id);

    /**
     * 提交审核（status: 0 → 1）
     * @param id 提报ID
     */
    void submit(Long id);

    /**
     * 审核通过（status: 1 → 2）
     * @param id 提报ID
     */
    void approve(Long id);

    /**
     * 审核驳回（status: 1 → 3）
     * @param id 提报ID
     * @param reason 驳回原因
     */
    void reject(Long id, String reason);

    /**
     * 转为采购申请（status: 2 → 4），创建 PurchaseRequest 记录
     * @param id 物资需求提报ID
     * @return 转换结果，含生成的采购申请单号
     */
    Map<String, Object> convertToPurchaseRequest(Long id);

    /**
     * 统计各状态数量
     * @return 统计数据
     */
    Map<String, Long> getStatistics();
}
