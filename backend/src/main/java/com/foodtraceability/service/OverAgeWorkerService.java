package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.entity.OverAgeWorker;

import java.util.List;

/**
 * 超龄劳动者 Service 接口
 */
public interface OverAgeWorkerService {

    /**
     * 分页查询超龄劳动者列表
     * @param page 页码
     * @param size 每页条数
     * @param keyword 关键字（员工姓名/员工ID/协议编号）
     * @param healthCheckResult 健康体检结果筛选
     * @return 分页结果
     */
    IPage<OverAgeWorker> getList(int page, int size, String keyword, String healthCheckResult);

    /**
     * 根据ID获取超龄劳动者详情
     * @param id 主键ID
     * @return 超龄劳动者信息
     */
    OverAgeWorker getById(Long id);

    /**
     * 创建超龄劳动者记录
     * @param entity 超龄劳动者信息
     * @return 创建后的记录
     */
    OverAgeWorker create(OverAgeWorker entity);

    /**
     * 更新超龄劳动者记录
     * @param id 主键ID
     * @param entity 更新信息
     * @return 更新后的记录
     */
    OverAgeWorker update(Long id, OverAgeWorker entity);

    /**
     * 删除超龄劳动者记录
     * @param id 主键ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 获取合规预警列表
     * 包含：协议即将到期、工伤保险即将到期、健康体检即将到期等
     * @return 合规预警列表
     */
    List<OverAgeWorker> getComplianceWarnings();
}
