package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ExpiryWarningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 预警记录Mapper接口
 */
@Mapper
public interface ExpiryWarningRecordMapper extends BaseMapper<ExpiryWarningRecord> {

    /**
     * 根据规则ID查询预警记录
     * @param ruleId 规则ID
     * @return 预警记录列表
     */
    List<ExpiryWarningRecord> selectByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 根据目标类型和目标ID查询
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 预警记录列表
     */
    List<ExpiryWarningRecord> selectByTarget(
            @Param("targetType") Integer targetType,
            @Param("targetId") Long targetId);

    /**
     * 查询未处理的预警记录
     * @return 预警记录列表
     */
    List<ExpiryWarningRecord> selectUnhandled();

    /**
     * 按预警级别统计未处理记录数
     * @param warningLevel 预警级别
     * @return 记录数
     }
    Long countUnhandledByLevel(@Param("warningLevel") Integer warningLevel);

    /**
     * 查询指定日期范围内的预警记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预警记录列表
     */
    List<ExpiryWarningRecord> selectByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
