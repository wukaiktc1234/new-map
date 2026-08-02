package com.foodtraceability.service.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PrintTaskCreateDTO;
import com.foodtraceability.dto.PrintTaskQueryDTO;
import com.foodtraceability.dto.PrintTaskVO;
import com.foodtraceability.entity.PrintTask;

import java.util.List;

/**
 * 打印任务服务接口
 * 提供打印任务的创建、状态查询、失败重试、批量打印等功能
 */
public interface PrintTaskService {

    /**
     * 创建打印任务（支持异步）
     * @param dto 创建请求DTO
     * @return 创建结果
     */
    Result<PrintTaskVO> createPrintTask(PrintTaskCreateDTO dto);

    /**
     * 批量创建打印任务（厨房单场景）
     * @param dtoList 任务列表
     * @return 创建结果
     */
    Result<List<PrintTaskVO>> batchCreatePrintTasks(List<PrintTaskCreateDTO> dtoList);

    /**
     * 根据ID查询任务
     * @param taskId 任务ID
     * @return 任务信息
     */
    Result<PrintTaskVO> getTaskById(Long taskId);

    /**
     * 分页查询任务列表
     * @param page 分页参数
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Result<IPage<PrintTaskVO>> getTaskPage(Page<?> page, PrintTaskQueryDTO queryDto);

    /**
     * 查询设备的待打印任务
     * @param deviceId 设备ID
     * @return 任务列表
     */
    Result<List<PrintTaskVO>> getPendingTasksByDevice(Long deviceId);

    /**
     * 更新任务状态为打印中
     * @param taskId 任务ID
     * @return 更新结果
     */
    Result<Void> updateTaskToPrinting(Long taskId);

    /**
     * 更新任务状态为已完成
     * @param taskId 任务ID
     * @return 更新结果
     */
    Result<Void> updateTaskToCompleted(Long taskId);

    /**
     * 更新任务状态为失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     * @return 更新结果
     */
    Result<Void> updateTaskToFailed(Long taskId, String errorMessage);

    /**
     * 重试失败的打印任务
     * @param taskId 任务ID
     * @return 重试结果
     */
    Result<Void> retryFailedTask(Long taskId);

    /**
     * 根据ID逻辑删除打印任务
     * @param taskId 任务ID
     * @return 删除结果
     */
    Result<Void> removeById(Long taskId);

    /**
     * 根据ID更新打印任务（通用更新方法）
     * @param task 打印任务实体（需包含taskId）
     * @return 更新结果
     */
    Result<PrintTaskVO> updateById(PrintTask task);
}
