package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PrintFormatTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 打印格式模板Mapper接口
 */
@Mapper
public interface PrintFormatTemplateMapper extends BaseMapper<PrintFormatTemplate> {
    
    /**
     * 根据模板类型和设备类型查询模板列表
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 模板列表
     */
    List<PrintFormatTemplate> selectByTypeAndDevice(
            @Param("templateType") String templateType,
            @Param("deviceType") String deviceType,
            @Param("storeId") Long storeId);
    
    /**
     * 查询默认模板
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 默认模板
     */
    PrintFormatTemplate selectDefaultTemplate(
            @Param("templateType") String templateType,
            @Param("deviceType") String deviceType,
            @Param("storeId") Long storeId);
    
    /**
     * 更新默认模板状态
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 更新结果
     */
    int updateDefaultTemplateStatus(
            @Param("templateType") String templateType,
            @Param("deviceType") String deviceType,
            @Param("storeId") Long storeId);
}
