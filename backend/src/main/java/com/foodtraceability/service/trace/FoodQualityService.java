package com.foodtraceability.service.trace;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.trace.QualityRecordCreateDTO;
import com.foodtraceability.dto.trace.QualityRecordVO;
import com.foodtraceability.dto.trace.QualityStandardCreateDTO;
import com.foodtraceability.dto.trace.QualityStandardQueryDTO;
import com.foodtraceability.dto.trace.QualityStandardUpdateDTO;
import com.foodtraceability.dto.trace.QualityStandardVO;
import com.foodtraceability.entity.FoodQualityRecord;

import java.util.Map;

/**
 * 质量追溯服务接口
 * 提供质量记录管理、质量异常处理、质量标准维护等功能
 */
public interface FoodQualityService extends IService<FoodQualityRecord> {

    // ==================== 质量记录 ====================

    /**
     * 分页查询质量记录
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<QualityRecordVO> queryRecordsPage(QualityStandardQueryDTO queryDTO);

    /**
     * 查询质量记录详情
     * @param recordId 质量记录ID
     * @return 质量记录详情
     */
    QualityRecordVO getRecordDetail(Long recordId);

    /**
     * 创建质量记录
     * @param dto 创建请求
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 创建后的质量记录
     */
    QualityRecordVO createRecord(QualityRecordCreateDTO dto, Long operatorId, String operatorName);

    /**
     * 删除质量记录（逻辑删除）
     * @param recordId 质量记录ID
     * @return 是否删除成功
     */
    boolean deleteRecord(Long recordId);

    /**
     * 质量异常列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<QualityRecordVO> queryAbnormalRecords(QualityStandardQueryDTO queryDTO);

    /**
     * 处理质量异常
     * @param recordId 质量记录ID
     * @param handlingResult 处理结果
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 是否处理成功
     */
    boolean handleAbnormal(Long recordId, String handlingResult, Long operatorId, String operatorName);

    /**
     * 质量统计
     * @return 统计结果
     */
    Map<String, Object> getStatistics();

    // ==================== 质量标准 ====================

    /**
     * 分页查询质量标准
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<QualityStandardVO> queryStandardsPage(QualityStandardQueryDTO queryDTO);

    /**
     * 查询质量标准详情
     * @param standardId 质量标准ID
     * @return 质量标准详情
     */
    QualityStandardVO getStandardDetail(Long standardId);

    /**
     * 创建质量标准
     * @param dto 创建请求
     * @return 创建后的质量标准
     */
    QualityStandardVO createStandard(QualityStandardCreateDTO dto);

    /**
     * 更新质量标准
     * @param dto 更新请求
     * @return 更新后的质量标准
     */
    QualityStandardVO updateStandard(QualityStandardUpdateDTO dto);

    /**
     * 删除质量标准（逻辑删除）
     * @param standardId 质量标准ID
     * @return 是否删除成功
     */
    boolean deleteStandard(Long standardId);
}
