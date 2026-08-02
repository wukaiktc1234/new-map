package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.DictCreateDTO;
import com.foodtraceability.dto.DictUpdateDTO;
import com.foodtraceability.dto.DictItemCreateDTO;
import com.foodtraceability.dto.DictItemUpdateDTO;
import com.foodtraceability.entity.SysDict;
import com.foodtraceability.entity.SysDictItem;

import java.util.List;
import java.util.Map;

/**
 * 字典管理Service接口
 * 提供字典类型和字典项的完整CRUD操作
 */
public interface SysDictService {

    // ==================== 字典类型操作 ====================

    /**
     * 创建字典类型
     * @param dto 创建请求
     * @param userId 当前用户ID
     * @param username 当前用户名
     * @return 创建的字典实体
     */
    SysDict createDict(DictCreateDTO dto, String userId, String username);

    /**
     * 更新字典类型
     * @param dictId 字典ID
     * @param dto 更新请求
     * @param userId 当前用户ID
     * @param username 当前用户名
     * @return 是否成功
     */
    boolean updateDict(Long dictId, DictUpdateDTO dto, String userId, String username);

    /**
     * 删除字典类型（逻辑删除）
     * @param dictId 字典ID
     * @return 是否成功
     */
    boolean deleteDict(Long dictId);

    /**
     * 根据ID获取字典类型详情
     * @param dictId 字典ID
     * @return 字典实体
     */
    SysDict getDictById(Long dictId);

    /**
     * 根据编码获取字典类型
     * @param dictCode 字典编码
     * @return 字典实体
     */
    SysDict getDictByCode(String dictCode);

    /**
     * 分页查询字典类型列表
     * @param page 分页参数
     * @param dictName 字典名称（模糊查询）
     * @param dictCode 字典编码（模糊查询）
     * @param dictGroup 字典分组
     * @param status 状态
     * @return 分页结果
     */
    IPage<SysDict> getDictPage(Page<SysDict> page, String dictName, String dictCode,
                               String dictGroup, Integer status);

    /**
     * 获取所有字典分组列表
     * @return 分组列表
     */
    List<String> getAllGroups();

    /**
     * 启用字典类型
     * @param dictId 字典ID
     * @return 是否成功
     */
    boolean enableDict(Long dictId);

    /**
     * 禁用字典类型
     * @param dictId 字典ID
     * @return 是否成功
     */
    boolean disableDict(Long dictId);

    // ==================== 字典项操作 ====================

    /**
     * 创建字典项
     * @param dto 创建请求
     * @param userId 当前用户ID
     * @param username 当前用户名
     * @return 创建的字典项实体
     */
    SysDictItem createDictItem(DictItemCreateDTO dto, String userId, String username);

    /**
     * 更新字典项
     * @param itemId 字典项ID
     * @param dto 更新请求
     * @param userId 当前用户ID
     * @param username 当前用户名
     * @return 是否成功
     */
    boolean updateDictItem(Long itemId, DictItemUpdateDTO dto, String userId, String username);

    /**
     * 删除字典项（逻辑删除）
     * @param itemId 字典项ID
     * @return 是否成功
     */
    boolean deleteDictItem(Long itemId);

    /**
     * 根据ID获取字典项详情
     * @param itemId 字典项ID
     * @return 字典项实体
     */
    SysDictItem getDictItemById(Long itemId);

    /**
     * 分页查询字典项列表
     * @param page 分页参数
     * @param dictId 字典类型ID
     * @param itemLabel 字典项标签（模糊查询）
     * @param status 状态
     * @return 分页结果
     */
    IPage<SysDictItem> getDictItemPage(Page<SysDictItem> page, Long dictId,
                                       String itemLabel, Integer status);

    /**
     * 根据字典ID获取所有启用的字典项
     * @param dictId 字典类型ID
     * @return 字典项列表
     */
    List<SysDictItem> getEnabledItemsByDictId(Long dictId);

    /**
     * 根据字典编码获取所有启用的字典项（用于前端组件）
     * @param dictCode 字典编码
     * @return 字典项列表
     */
    List<SysDictItem> getEnabledItemsByDictCode(String dictCode);

    // ==================== 缓存操作 ====================

    /**
     * 清除指定字典的缓存
     * @param dictId 字典ID
     */
    void clearDictCache(Long dictId);

    /**
     * 清除指定字典编码的缓存
     * @param dictCode 字典编码
     */
    void clearDictCacheByCode(String dictCode);

    /**
     * 清除所有字典缓存
     */
    void clearAllDictCache();

    // ==================== 统计信息 ====================

    /**
     * 获取字典统计信息
     * @return 统计数据Map
     */
    Map<String, Object> getDictStats();
}
