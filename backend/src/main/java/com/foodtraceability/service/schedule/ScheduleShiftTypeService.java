package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.schedule.ScheduleShiftTypeCreateDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeReorderDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeUpdateDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeVO;

import java.util.List;

/**
 * 班次类型配置服务接口
 * 提供班次类型的增删改查及状态管理功能
 */
public interface ScheduleShiftTypeService {

    /**
     * 查询门店启用的班次类型列表
     * @param storeId 门店ID
     * @return 启用的班次类型列表(按排序)
     */
    List<ScheduleShiftTypeVO> getActiveShiftTypes(Long storeId);

    /**
     * 查询门店所有班次类型(含停用)
     * @param storeId 门店ID
     * @return 所有班次类型列表(按排序)
     */
    List<ScheduleShiftTypeVO> getAllShiftTypes(Long storeId);

    /**
     * 获取班次类型详情
     * @param shiftTypeId 班次类型ID
     * @return 班次类型详情
     */
    ScheduleShiftTypeVO getShiftTypeById(Long shiftTypeId);

    /**
     * 创建班次类型
     * @param createDTO 创建请求DTO
     * @return 创建后的班次类型
     */
    ScheduleShiftTypeVO createShiftType(ScheduleShiftTypeCreateDTO createDTO);

    /**
     * 编辑班次类型
     * @param shiftTypeId 班次类型ID
     * @param updateDTO 更新请求DTO
     * @return 更新后的班次类型
     */
    ScheduleShiftTypeVO updateShiftType(Long shiftTypeId, ScheduleShiftTypeUpdateDTO updateDTO);

    /**
     * 切换班次类型状态(启用/停用)
     * @param shiftTypeId 班次类型ID
     * @return 更新后的班次类型
     */
    ScheduleShiftTypeVO toggleStatus(Long shiftTypeId);

    /**
     * 批量调整班次排序
     * @param reorderDTO 排序调整请求DTO
     */
    void reorderShiftTypes(ScheduleShiftTypeReorderDTO reorderDTO);
}
