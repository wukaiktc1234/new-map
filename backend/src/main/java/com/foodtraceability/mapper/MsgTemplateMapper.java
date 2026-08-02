package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.MsgTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 消息模板Mapper接口
 * 提供模板的分页查询、按类型查询、统计等自定义查询方法
 */
@Mapper
public interface MsgTemplateMapper extends BaseMapper<MsgTemplate> {

    /**
     * 分页查询消息模板列表
     *
     * @param page 分页参数
     * @param templateCode 模板编码(模糊)
     * @param templateName 模板名称(模糊)
     * @param templateType 模板类型
     * @param status 状态
     * @param channel 发送渠道
     * @return 分页结果
     */
    IPage<MsgTemplate> selectTemplatePage(
        Page<MsgTemplate> page,
        @Param("templateCode") String templateCode,
        @Param("templateName") String templateName,
        @Param("templateType") Integer templateType,
        @Param("status") Integer status,
        @Param("channel") String channel
    );

    /**
     * 按模板类型统计数量
     *
     * @return 统计结果Map，key为templateType，value为数量
     */
    java.util.Map<String, Long> countByTemplateType();

    /**
     * 按渠道统计模板数量
     *
     * @return 统计结果Map，key为channel，value为数量
     */
    java.util.Map<String, Long> countByChannel();
}
