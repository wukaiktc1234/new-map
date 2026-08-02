package com.foodtraceability.service.trace;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.trace.InspectionCreateDTO;
import com.foodtraceability.dto.trace.InspectionQueryDTO;
import com.foodtraceability.dto.trace.InspectionStatisticsVO;
import com.foodtraceability.dto.trace.InspectionUpdateDTO;
import com.foodtraceability.dto.trace.InspectionVO;
import com.foodtraceability.entity.FoodInspection;

import java.util.List;

/**
 * 检验记录服务接口
 * 提供入库检验、加工检验、成品检验等全流程检验记录的管理功能
 */
public interface FoodInspectionService extends IService<FoodInspection> {

    /**
     * 分页查询检验记录
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<InspectionVO> queryPage(InspectionQueryDTO queryDTO);

    /**
     * 根据ID查询检验记录详情
     * @param inspectionId 检验记录ID
     * @return 检验记录详情
     */
    InspectionVO getDetailById(Long inspectionId);

    /**
     * 创建检验记录
     * @param dto 创建请求
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 创建后的检验记录
     */
    InspectionVO create(InspectionCreateDTO dto, Long operatorId, String operatorName);

    /**
     * 更新检验记录
     * @param dto 更新请求
     * @return 更新后的检验记录
     */
    InspectionVO update(InspectionUpdateDTO dto);

    /**
     * 删除检验记录（逻辑删除）
     * @param inspectionId 检验记录ID
     * @return 是否删除成功
     */
    boolean delete(Long inspectionId);

    /**
     * 按批次号查询检验记录
     * @param batchNo 批次号
     * @return 检验记录列表
     */
    List<InspectionVO> queryByBatchNo(String batchNo);

    /**
     * 按供应商查询检验记录
     * @param supplierId 供应商ID
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<InspectionVO> queryBySupplier(Long supplierId, InspectionQueryDTO queryDTO);

    /**
     * 上传检验报告
     * @param inspectionId 检验记录ID
     * @param reportUrl 报告URL
     * @return 是否上传成功
     */
    boolean uploadReport(Long inspectionId, String reportUrl);

    /**
     * 检验统计
     * @param queryDTO 查询条件
     * @return 统计结果
     */
    InspectionStatisticsVO getStatistics(InspectionQueryDTO queryDTO);
}
