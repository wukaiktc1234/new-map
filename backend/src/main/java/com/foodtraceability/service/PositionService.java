package com.foodtraceability.service;

import com.foodtraceability.entity.Position;
import java.util.List;
import java.util.Map;

/**
 * 职位服务接口
 */
public interface PositionService {

    /**
     * 获取职位列表（分页，支持部门、状态、关键词筛选）
     */
    Map<String, Object> getPositions(int current, int size, Long departmentId, String status, String keyword);

    /**
     * 获取职位列表（分页，无筛选）
     */
    default Map<String, Object> getPositions(int current, int size) {
        return getPositions(current, size, null, null, null);
    }

    /**
     * 获取所有职位
     */
    List<Position> getAllPositions();

    /**
     * 根据ID获取职位
     */
    Position getPositionById(Long id);

    /**
     * 创建职位
     */
    Position createPosition(Position position);

    /**
     * 更新职位
     */
    Position updatePosition(Long id, Position position);

    /**
     * 删除职位
     */
    void deletePosition(Long id);

    /**
     * 按编码前缀清理无员工关联的职位（物理删除）
     * @param prefix 编码前缀
     * @return 清理数量
     */
    int cleanupPositionsByPrefix(String prefix);

    /**
     * 更新职位状态
     */
    void updatePositionStatus(Long id, Boolean status);

    /**
     * 更新职位状态（级联更新关联的员工）
     */
    void updatePositionStatusWithCascade(Long id, Boolean status);

    /**
     * 根据部门获取职位列表
     */
    List<Position> getPositionsByDepartment(Long departmentId);

    /**
     * 增加职位员工数量
     */
    void incrementEmployeeCount(Long positionId);

    /**
     * 减少职位员工数量
     */
    void decrementEmployeeCount(Long positionId);

    /**
     * 清空所有职位数据
     */
    void clearAllPositions();

    /**
     * 根据职位名称获取职位类型编码
     */
    String getPositionCodePrefix(String positionName);

    /**
     * 根据部门ID获取部门编码
     */
    String getDepartmentCode(String departmentId);

    /**
     * 获取下一个职位编码序号
     */
    Integer getNextPositionSeq(String codePrefix);

    /**
     * 生成职位编码
     */
    String generatePositionCode(String departmentId, String positionName);

    /**
     * 批量更新职位编码
     */
    Map<String, Object> batchUpdatePositionCodes();
}
