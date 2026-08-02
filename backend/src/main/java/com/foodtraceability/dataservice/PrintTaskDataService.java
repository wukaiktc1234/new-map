package com.foodtraceability.dataservice;

import com.foodtraceability.dto.PrintTaskVO;
import java.util.List;
import java.util.Map;

/**
 * 打印任务数据服务接口
 */
public interface PrintTaskDataService {

    /**
     * 批量获取打印任务基本信息
     * @param taskIds 任务ID列表
     * @return 任务ID到基本信息的映射
     */
    Map<Long, PrintTaskVO> batchGetPrintTaskBasicInfo(List<Long> taskIds);

    /**
     * 获取单个打印任务基本信息
     * @param taskId 任务ID
     * @return 打印任务基本信息
     */
    PrintTaskVO getPrintTaskBasicInfo(Long taskId);

    /**
     * 清除指定任务的缓存
     * @param taskId 任务ID
     */
    void clearPrintTaskCache(Long taskId);
}
