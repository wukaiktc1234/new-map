package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.report.ExportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报表导出任务Mapper接口
 * 对应数据库表 export_tasks
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {
}
