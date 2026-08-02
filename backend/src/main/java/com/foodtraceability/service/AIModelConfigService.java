package com.foodtraceability.service;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.aimodel.AIModelConfigCreateDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigQueryDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigUpdateDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigVO;

import java.util.Map;

/**
 * AI 模型配置服务接口
 * 提供智能补货建议 AI 模型接入的管理能力（支持 local/api 两种类型）
 */
public interface AIModelConfigService {

    /**
     * 分页查询 AI 模型配置列表
     * @param query 查询条件（分页参数 + 过滤条件）
     * @return 分页结果
     */
    PageResult<AIModelConfigVO> getList(AIModelConfigQueryDTO query);

    /**
     * 根据ID查询 AI 模型配置详情
     * @param id 主键ID
     * @return 视图对象
     */
    AIModelConfigVO getById(Integer id);

    /**
     * 根据业务编码查询 AI 模型配置
     * @param modelCode 业务编码
     * @return 视图对象
     */
    AIModelConfigVO getByCode(String modelCode);

    /**
     * 创建 AI 模型配置
     * @param dto 创建参数
     * @return 创建后的视图对象
     */
    AIModelConfigVO create(AIModelConfigCreateDTO dto);

    /**
     * 更新 AI 模型配置（支持部分更新）
     * @param id 主键ID
     * @param dto 更新参数
     * @return 更新后的视图对象
     */
    AIModelConfigVO update(Integer id, AIModelConfigUpdateDTO dto);

    /**
     * 删除 AI 模型配置（逻辑删除）
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean delete(Integer id);

    /**
     * 更新 AI 模型状态
     * @param id 主键ID
     * @param status 状态（active/inactive）
     * @return 是否更新成功
     */
    boolean updateStatus(Integer id, String status);

    /**
     * 触发模型同步（更新 lastSyncTime）
     * @param id 主键ID
     * @return 是否同步成功
     */
    boolean syncModel(Integer id);

    /**
     * 获取统计信息（总数/启用/本地/API）
     * @return 统计信息
     */
    Map<String, Object> getStatistics();

    /**
     * 测试 AI 模型连接是否可用
     * <p>对于 api 类型：发送 HTTP GET 请求到 endpoint，根据响应状态码判断可用性，并记录响应耗时；
     * 对于 local 类型：若 modelPath 为空或为"内置规则引擎"则视为可用，否则检查路径是否存在。
     * @param id 主键ID
     * @return 测试结果（success/message/responseTimeMs/httpStatus/modelType）
     */
    Map<String, Object> testConnection(Integer id);
}
