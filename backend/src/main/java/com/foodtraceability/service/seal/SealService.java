package com.foodtraceability.service.seal;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.entity.seal.Seal;
import com.foodtraceability.entity.seal.SealUsageLog;

import java.util.List;
import java.util.Map;

/**
 * 电子签章服务接口
 * 提供印章的增删改查、状态管理、授权查询及使用记录管理功能
 *
 * <p>说明：
 * 印章的 authorizedUsers / authorizedScenes 在数据库中以 JSON 数组字符串存储，
 * 对外交互时由实现类负责 List 与 JSON 字符串之间的序列化/反序列化，
 * 因此印章相关方法使用 Map 承载（数组字段以 List 形式出现），使用记录无 JSON 字段直接使用实体。
 */
public interface SealService extends IService<Seal> {

    /**
     * 分页查询印章列表
     * @param page 页码(从1开始)
     * @param size 每页条数
     * @param sealType 印章类型筛选(可空)
     * @param status 状态筛选(可空)
     * @param keyword 关键词(匹配印章名称或保管人,可空)
     * @return 分页结果(每条记录为Map,含数组形式的授权字段)
     */
    PageResult<Map<String, Object>> getSealList(Integer page, Integer size,
                                                String sealType, String status, String keyword);

    /**
     * 根据ID获取印章详情
     * @param id 印章ID
     * @return 印章详情(含数组形式的授权字段),不存在返回null
     */
    Map<String, Object> getSealById(String id);

    /**
     * 新增印章
     * @param data 印章数据(authorizedUsers/authorizedScenes 为 List)
     * @return 新增后的印章详情
     */
    Map<String, Object> createSeal(Map<String, Object> data);

    /**
     * 更新印章
     * @param id 印章ID
     * @param data 印章数据(部分字段更新)
     * @return 更新后的印章详情
     */
    Map<String, Object> updateSeal(String id, Map<String, Object> data);

    /**
     * 作废印章(逻辑层面:状态改为 revoked,记录仍可见)
     * @param id 印章ID
     * @return true-作废成功
     */
    boolean revokeSeal(String id);

    /**
     * 切换印章状态(启用/停用)
     * @param id 印章ID
     * @param status 目标状态(active/inactive)
     * @return 更新后的印章详情
     */
    Map<String, Object> updateSealStatus(String id, String status);

    /**
     * 获取当前用户已授权的印章(用于签署时选择)
     * 筛选条件:status=active 且 scene 在 authorizedScenes 中 且 userId 在 authorizedUsers 中
     * @param scene 使用场景
     * @param userId 用户ID
     * @return 授权印章列表
     */
    List<Map<String, Object>> getAuthorizedSeals(String scene, String userId);

    /**
     * 分页查询印章使用记录
     * @param page 页码(从1开始)
     * @param size 每页条数
     * @param sealId 印章ID筛选(可空)
     * @param businessType 业务类型筛选(可空)
     * @return 分页结果(按操作时间倒序)
     */
    PageResult<SealUsageLog> getUsageLogs(Integer page, Integer size,
                                          String sealId, String businessType);

    /**
     * 记录印章使用(签署时调用)
     * @param data 使用记录数据(sealId, businessType, businessId, businessNo, operator, remark)
     * @param ipAddress 操作人IP地址
     * @return 新增的使用记录
     */
    SealUsageLog recordUsage(Map<String, Object> data, String ipAddress);
}
