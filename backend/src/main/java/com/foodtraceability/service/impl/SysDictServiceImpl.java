package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.DictCreateDTO;
import com.foodtraceability.dto.DictItemCreateDTO;
import com.foodtraceability.dto.DictItemUpdateDTO;
import com.foodtraceability.dto.DictUpdateDTO;
import com.foodtraceability.entity.SysDict;
import com.foodtraceability.entity.SysDictItem;
import com.foodtraceability.mapper.SysDictItemMapper;
import com.foodtraceability.mapper.SysDictMapper;
import com.foodtraceability.service.SysDictService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典管理Service实现类
 * 实现字典类型和字典项的完整业务逻辑
 */
@Service
public class SysDictServiceImpl implements SysDictService {

    private static final Logger logger = LoggerFactory.getLogger(SysDictServiceImpl.class);

    private final SysDictMapper sysDictMapper;
    private final SysDictItemMapper sysDictItemMapper;

    public SysDictServiceImpl(SysDictMapper sysDictMapper, SysDictItemMapper sysDictItemMapper) {
        this.sysDictMapper = sysDictMapper;
        this.sysDictItemMapper = sysDictItemMapper;
    }

    // ==================== 字典类型操作实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDict createDict(DictCreateDTO dto, String userId, String username) {
        // 检查字典编码是否已存在
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDict::getDictCode, dto.getDictCode());
        Long count = sysDictMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(400, "字典编码已存在: " + dto.getDictCode());
        }

        // 创建字典实体
        SysDict dict = new SysDict();
        dict.setDictName(dto.getDictName());
        dict.setDictCode(dto.getDictCode());
        dict.setDictGroup(dto.getDictGroup() != null ? dto.getDictGroup() : "default");
        dict.setDescription(dto.getDescription());
        dict.setStatus(1); // 默认启用
        dict.setIsSystem(0); // 默认非系统内置
        dict.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dict.setDeleted(0);

        // 设置审计字段
        dict.setCreateUserId(userId);
        dict.setCreateUsername(username);
        dict.setUpdateUserId(userId);
        dict.setUpdateUsername(username);
        dict.setCreateTime(LocalDateTime.now());
        dict.setUpdateTime(LocalDateTime.now());

        sysDictMapper.insert(dict);
        logger.info("创建字典成功: code={}, name={}", dto.getDictCode(), dto.getDictName());

        return dict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDict(Long dictId, DictUpdateDTO dto, String userId, String username) {
        SysDict dict = sysDictMapper.selectById(dictId);
        if (dict == null || dict.getDeleted() == 1) {
            throw new BusinessException(400, "字典不存在或已被删除");
        }

        // 检查是否为系统内置字典
        if (dict.getIsSystem() == 1 && dto.getDictGroup() != null) {
            logger.warn("尝试修改系统内置字典的分类: dictId={}", dictId);
            // 系统内置字典不允许修改某些字段
        }

        // 更新字段
        dict.setDictName(dto.getDictName());
        if (dto.getDictGroup() != null) {
            dict.setDictGroup(dto.getDictGroup());
        }
        if (dto.getDescription() != null) {
            dict.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            dict.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            dict.setSortOrder(dto.getSortOrder());
        }
        dict.setUpdateUserId(userId);
        dict.setUpdateUsername(username);
        dict.setUpdateTime(LocalDateTime.now());

        int rows = sysDictMapper.updateById(dict);
        if (rows > 0) {
            clearDictCache(dictId); // 清除缓存
            logger.info("更新字典成功: dictId={}", dictId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDict(Long dictId) {
        SysDict dict = sysDictMapper.selectById(dictId);
        if (dict == null || dict.getDeleted() == 1) {
            throw new BusinessException(400, "字典不存在或已被删除");
        }

        // 检查是否为系统内置字典（禁止删除）
        if (dict.getIsSystem() == 1) {
            throw new BusinessException(400, "系统内置字典不允许删除");
        }

        // 检查是否有关联的字典项
        LambdaQueryWrapper<SysDictItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(SysDictItem::getDictId, dictId);
        Long itemCount = sysDictItemMapper.selectCount(itemQuery);
        if (itemCount > 0) {
            // 先删除关联的字典项（逻辑删除）
            sysDictItemMapper.delete(itemQuery);
        }

        // 逻辑删除字典
        int rows = sysDictMapper.deleteById(dictId);
        if (rows > 0) {
            clearDictCache(dictId);
            logger.info("删除字典成功: dictId={}, itemCount={}", dictId, itemCount);
            return true;
        }
        return false;
    }

    @Override
    @Cacheable(value = "dict", key = "'basic:' + #dictId")
    public SysDict getDictById(Long dictId) {
        return sysDictMapper.selectById(dictId);
    }

    @Override
    public SysDict getDictByCode(String dictCode) {
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDict::getDictCode, dictCode);
        return sysDictMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<SysDict> getDictPage(Page<SysDict> page, String dictName, String dictCode,
                                       String dictGroup, Integer status) {
        return sysDictMapper.selectDictPage(page, dictName, dictCode, dictGroup, status);
    }

    @Override
    public List<String> getAllGroups() {
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(SysDict::getDictGroup)
               .eq(SysDict::getDeleted, 0)
               .groupBy(SysDict::getDictGroup)
               .orderByAsc(SysDict::getDictGroup);

        List<SysDict> dicts = sysDictMapper.selectList(queryWrapper);
        return dicts.stream().map(SysDict::getDictGroup).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableDict(Long dictId) {
        SysDict dict = sysDictMapper.selectById(dictId);
        if (dict == null || dict.getDeleted() == 1) {
            throw new BusinessException(400, "字典不存在或已被删除");
        }

        dict.setStatus(1);
        dict.setUpdateTime(LocalDateTime.now());
        int rows = sysDictMapper.updateById(dict);
        if (rows > 0) {
            clearDictCache(dictId);
            logger.info("启用字典成功: dictId={}", dictId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableDict(Long dictId) {
        SysDict dict = sysDictMapper.selectById(dictId);
        if (dict == null || dict.getDeleted() == 1) {
            throw new BusinessException(400, "字典不存在或已被删除");
        }

        // 禁用字典时同时禁用所有字典项
        LambdaQueryWrapper<SysDictItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(SysDictItem::getDictId, dictId).eq(SysDictItem::getStatus, 1);
        List<SysDictItem> items = sysDictItemMapper.selectList(itemQuery);
        for (SysDictItem item : items) {
            item.setStatus(0);
            item.setUpdateTime(LocalDateTime.now());
            sysDictItemMapper.updateById(item);
        }

        dict.setStatus(0);
        dict.setUpdateTime(LocalDateTime.now());
        int rows = sysDictMapper.updateById(dict);
        if (rows > 0) {
            clearDictCache(dictId);
            logger.info("禁用字典成功: dictId={}, disabledItems={}", dictId, items.size());
            return true;
        }
        return false;
    }

    // ==================== 字典项操作实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictItem createDictItem(DictItemCreateDTO dto, String userId, String username) {
        // 检查字典是否存在且启用
        SysDict dict = sysDictMapper.selectById(dto.getDictId());
        if (dict == null || dict.getDeleted() == 1) {
            throw new BusinessException(400, "关联的字典不存在或已被删除");
        }

        // 检查同一字典下是否已存在相同的值
        LambdaQueryWrapper<SysDictItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDictItem::getDictId, dto.getDictId())
                   .eq(SysDictItem::getItemValue, dto.getItemValue());
        Long count = sysDictItemMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(400, "字典项值已存在: " + dto.getItemValue());
        }

        // 如果设置为默认项，先清除同字典下的其他默认项
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefaultFlag(dto.getDictId());
        }

        // 创建字典项实体
        SysDictItem item = new SysDictItem();
        item.setDictId(dto.getDictId());
        item.setItemLabel(dto.getItemLabel());
        item.setItemValue(dto.getItemValue());
        item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        item.setCssClass(dto.getCssClass());
        item.setListClass(dto.getListClass());
        item.setColorType(dto.getColorType());
        item.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);
        item.setStatus(1); // 默认启用
        item.setRemark(dto.getRemark());
        item.setDeleted(0);

        // 设置审计字段
        item.setCreateUserId(userId);
        item.setCreateUsername(username);
        item.setUpdateUserId(userId);
        item.setUpdateUsername(username);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());

        sysDictItemMapper.insert(item);
        clearDictCache(dto.getDictId()); // 清除字典缓存

        logger.info("创建字典项成功: dictId={}, value={}, label={}",
                   dto.getDictId(), dto.getItemValue(), dto.getItemLabel());

        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictItem(Long itemId, DictItemUpdateDTO dto, String userId, String username) {
        SysDictItem item = sysDictItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(400, "字典项不存在或已被删除");
        }

        // 更新字段
        item.setItemLabel(dto.getItemLabel());
        item.setItemValue(dto.getItemValue());
        if (dto.getSortOrder() != null) {
            item.setSortOrder(dto.getSortOrder());
        }
        if (dto.getCssClass() != null) {
            item.setCssClass(dto.getCssClass());
        }
        if (dto.getListClass() != null) {
            item.setListClass(dto.getListClass());
        }
        if (dto.getColorType() != null) {
            item.setColorType(dto.getColorType());
        }
        if (dto.getIsDefault() != null) {
            // 如果设置为默认项，先清除同字典下的其他默认项
            if (dto.getIsDefault() == 1) {
                clearDefaultFlag(item.getDictId());
            }
            item.setIsDefault(dto.getIsDefault());
        }
        if (dto.getStatus() != null) {
            item.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            item.setRemark(dto.getRemark());
        }
        item.setUpdateUserId(userId);
        item.setUpdateUsername(username);
        item.setUpdateTime(LocalDateTime.now());

        int rows = sysDictItemMapper.updateById(item);
        if (rows > 0) {
            clearDictCache(item.getDictId()); // 清除字典缓存
            logger.info("更新字典项成功: itemId={}", itemId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictItem(Long itemId) {
        SysDictItem item = sysDictItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(400, "字典项不存在或已被删除");
        }

        int rows = sysDictItemMapper.deleteById(itemId);
        if (rows > 0) {
            clearDictCache(item.getDictId());
            logger.info("删除字典项成功: itemId={}, dictId={}", itemId, item.getDictId());
            return true;
        }
        return false;
    }

    @Override
    public SysDictItem getDictItemById(Long itemId) {
        return sysDictItemMapper.selectById(itemId);
    }

    @Override
    public IPage<SysDictItem> getDictItemPage(Page<SysDictItem> page, Long dictId,
                                             String itemLabel, Integer status) {
        return sysDictItemMapper.selectItemPage(page, dictId, itemLabel, status);
    }

    @Override
    @Cacheable(value = "dict", key = "'items:' + #dictId")
    public List<SysDictItem> getEnabledItemsByDictId(Long dictId) {
        return sysDictItemMapper.selectEnabledItemsByDictId(dictId);
    }

    @Override
    @Cacheable(value = "dict", key = "'itemsByCode:' + #dictCode")
    public List<SysDictItem> getEnabledItemsByDictCode(String dictCode) {
        return sysDictItemMapper.selectEnabledItemsByDictCode(dictCode);
    }

    // ==================== 缓存操作实现 ====================

    @Override
    @CacheEvict(value = "dict", allEntries = false)
    public void clearDictCache(Long dictId) {
        // 通过Spring Cache注解自动清除缓存
        logger.debug("清除字典缓存: dictId={}", dictId);
    }

    @Override
    public void clearDictCacheByCode(String dictCode) {
        SysDict dict = getDictByCode(dictCode);
        if (dict != null) {
            clearDictCache(dict.getDictId());
        }
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public void clearAllDictCache() {
        logger.info("清除所有字典缓存");
    }

    // ==================== 统计信息实现 ====================

    @Override
    public Map<String, Object> getDictStats() {
        Map<String, Object> stats = new HashMap<>();

        // 字典总数
        LambdaQueryWrapper<SysDict> dictQuery = new LambdaQueryWrapper<>();
        dictQuery.eq(SysDict::getDeleted, 0);
        Long totalDicts = sysDictMapper.selectCount(dictQuery);
        stats.put("totalDicts", totalDicts);

        // 启用的字典数
        LambdaQueryWrapper<SysDict> enabledDictQuery = new LambdaQueryWrapper<>();
        enabledDictQuery.eq(SysDict::getDeleted, 0).eq(SysDict::getStatus, 1);
        Long enabledDicts = sysDictMapper.selectCount(enabledDictQuery);
        stats.put("enabledDicts", enabledDicts);

        // 字典项总数
        LambdaQueryWrapper<SysDictItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(SysDictItem::getDeleted, 0);
        Long totalItems = sysDictItemMapper.selectCount(itemQuery);
        stats.put("totalItems", totalItems);

        // 分组数量
        List<String> groups = getAllGroups();
        stats.put("groupCount", groups.size());

        return stats;
    }

    /**
     * 清除指定字典下的所有默认标记（批量更新优化版）
     * 使用单次SQL批量更新替代循环逐条更新，性能从O(N)提升至O(1)
     * @param dictId 字典ID
     */
    private void clearDefaultFlag(Long dictId) {
        // 构建更新实体
        SysDictItem updateEntity = new SysDictItem();
        updateEntity.setIsDefault(0);
        updateEntity.setUpdateTime(LocalDateTime.now());

        // 构建查询条件：指定字典下所有默认项
        LambdaQueryWrapper<SysDictItem> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(SysDictItem::getDictId, dictId)
                   .eq(SysDictItem::getIsDefault, 1);

        // 单次批量更新（性能优化：1次SQL vs N次SQL）
        sysDictItemMapper.update(updateEntity, updateWrapper);
    }
}
