package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.FoodTrace;
import com.foodtraceability.entity.StageCount;
import com.foodtraceability.entity.OperationCount;
import com.foodtraceability.entity.StageCompletion;
import com.foodtraceability.entity.OperatorCount;
import com.foodtraceability.entity.LocationCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 食品溯源流程Mapper接口
 * 提供食品溯源流程相关的数据库操作
 */
@Repository
public interface FoodTraceMapper extends BaseMapper<FoodTrace> {

    /**
     * 根据食品ID查询完整的溯源流程
     *
     * @param foodId 食品ID
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE food_id = #{foodId} AND deleted = 0 ORDER BY operation_time ASC")
    List<FoodTrace> selectByFoodId(@Param("foodId") String foodId);

    /**
     * 根据批次号查询完整的溯源流程
     *
     * @param batchNumber 批次号
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE batch_number = #{batchNumber} AND deleted = 0 ORDER BY operation_time ASC")
    List<FoodTrace> selectByBatchNumber(@Param("batchNumber") String batchNumber);

    /**
     * 根据流程阶段查询
     *
     * @param processStage 流程阶段
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE process_stage = #{processStage} AND deleted = 0 ORDER BY operation_time DESC")
    List<FoodTrace> selectByProcessStage(@Param("processStage") Integer processStage);

    /**
     * 根据操作类型查询
     *
     * @param operationType 操作类型
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE operation_type = #{operationType} AND deleted = 0 ORDER BY operation_time DESC")
    List<FoodTrace> selectByOperationType(@Param("operationType") String operationType);

    /**
     * 查询指定时间范围内的溯源记录
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE operation_time BETWEEN #{startTime} AND #{endTime} AND deleted = 0 ORDER BY operation_time DESC")
    List<FoodTrace> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据责任单位查询
     *
     * @param responsibleCompany 责任单位
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE responsible_company = #{responsibleCompany} AND deleted = 0 ORDER BY operation_time DESC")
    List<FoodTrace> selectByResponsibleCompany(@Param("responsibleCompany") String responsibleCompany);

    /**
     * 查询质检不合格的溯源记录
     *
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE quality_result = 'fail' AND deleted = 0 ORDER BY operation_time DESC")
    List<FoodTrace> selectFailedQualityRecords();

    /**
     * 查询运输中的食品
     *
     * @return 溯源流程列表
     */
    @Select("SELECT * FROM food_trace WHERE operation_type = 'transport' AND transport_end_time IS NULL AND deleted = 0")
    List<FoodTrace> selectTransportingFoods();

    /**
     * 获取食品的当前状态（最新的流程记录）
     *
     * @param foodId 食品ID
     * @return 最新的溯源流程记录
     */
    @Select("SELECT * FROM food_trace WHERE food_id = #{foodId} AND deleted = 0 ORDER BY operation_time DESC LIMIT 1")
    FoodTrace selectCurrentStatus(@Param("foodId") String foodId);

    /**
     * 分页查询溯源流程列表（包含条件查询）
     *
     * @param page 分页对象
     * @param foodId 食品ID（可选）
     * @param batchNumber 批次号（可选）
     * @param processStage 流程阶段（可选）
     * @param operationType 操作类型（可选）
     * @param responsibleCompany 责任单位（可选）
     * @return 分页结果
     */
    IPage<FoodTrace> selectTracePage(Page<FoodTrace> page,
                                     @Param("foodId") String foodId,
                                     @Param("batchNumber") String batchNumber,
                                     @Param("processStage") Integer processStage,
                                     @Param("operationType") String operationType,
                                     @Param("responsibleCompany") String responsibleCompany);

    /**
     * 统计各流程阶段的记录数量
     *
     * @return 流程阶段统计结果
     */
    @Select("SELECT process_stage as stage, stage_name as stageName, COUNT(*) as count " +
            "FROM food_trace WHERE deleted = 0 GROUP BY process_stage, stage_name")
    List<StageCount> countByProcessStage();

    /**
     * 统计各阶段的记录数量（简写方法名）
     *
     * @return 阶段统计结果
     */
    @Select("SELECT process_stage as stage, COUNT(*) as count " +
            "FROM food_trace WHERE deleted = 0 GROUP BY process_stage")
    List<StageCount> countByStage();

    /**
     * 统计各操作类型的记录数量
     *
     * @return 操作类型统计结果
     */
    @Select("SELECT operation_type as operationType, COUNT(*) as count " +
            "FROM food_trace WHERE deleted = 0 GROUP BY operation_type")
    List<OperationCount> countByOperationType();

    /**
     * 查询指定食品的所有溯源阶段是否完整
     *
     * @param foodId 食品ID
     * @return 各阶段完成情况
     */
    @Select("SELECT process_stage as stage, COUNT(*) as count " +
            "FROM food_trace WHERE food_id = #{foodId} AND deleted = 0 " +
            "GROUP BY process_stage ORDER BY process_stage")
    List<StageCompletion> checkStageCompletion(@Param("foodId") String foodId);

    /**
     * 统计指定时间范围内各操作者的操作数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作者统计结果
     */
    @Select("SELECT operator, COUNT(*) as count " +
            "FROM food_trace WHERE operation_time BETWEEN #{startTime} AND #{endTime} AND deleted = 0 " +
            "GROUP BY operator ORDER BY count DESC")
    List<OperatorCount> countByOperator(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内各操作地点的操作数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作地点统计结果
     */
    @Select("SELECT operation_location as location, COUNT(*) as count " +
            "FROM food_trace WHERE operation_time BETWEEN #{startTime} AND #{endTime} AND deleted = 0 " +
            "GROUP BY operation_location ORDER BY count DESC")
    List<LocationCount> countByLocation(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}