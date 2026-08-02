package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.StandardCostCard;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.StandardCostCardMapper;
import com.foodtraceability.service.finance.StandardCostCardService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标准成本卡Service实现
 */
@Service
public class StandardCostCardServiceImpl extends ServiceImpl<StandardCostCardMapper, StandardCostCard>
        implements StandardCostCardService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StandardCostCardVO create(StandardCostCardCreateDTO dto) {
        // 校验同一dishId不重复（未删除的）
        LambdaQueryWrapper<StandardCostCard> check = new LambdaQueryWrapper<>();
        check.eq(StandardCostCard::getDishId, dto.getDishId());
        if (baseMapper.selectCount(check) > 0) {
            throw new BusinessException("该菜品已存在标准成本卡");
        }

        StandardCostCard entity = new StandardCostCard();
        BeanUtils.copyProperties(dto, entity);
        entity.setWarningStatus(0); // 默认正常
        entity.setLastUpdateDate(LocalDate.now()); // 设置最后更新日期为当前日期
        baseMapper.insert(entity);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long cardId, StandardCostCardUpdateDTO dto) {
        StandardCostCard existing = baseMapper.selectById(cardId);
        if (existing == null) {
            throw new BusinessException("标准成本卡不存在");
        }

        // 校验dishId唯一性（排除自身）
        if (dto.getDishId() != null && !dto.getDishId().equals(existing.getDishId())) {
            LambdaQueryWrapper<StandardCostCard> check = new LambdaQueryWrapper<>();
            check.eq(StandardCostCard::getDishId, dto.getDishId())
                 .ne(StandardCostCard::getCardId, cardId);
            if (baseMapper.selectCount(check) > 0) {
                throw new BusinessException("该菜品已存在标准成本卡");
            }
        }

        StandardCostCard updateEntity = new StandardCostCard();
        updateEntity.setCardId(cardId);

        if (dto.getDishId() != null) {
            updateEntity.setDishId(dto.getDishId());
        }
        if (dto.getDishName() != null) {
            updateEntity.setDishName(dto.getDishName());
        }
        if (dto.getStandardCost() != null) {
            updateEntity.setStandardCost(dto.getStandardCost());
        }
        if (dto.getLossCoefficient() != null) {
            updateEntity.setLossCoefficient(dto.getLossCoefficient());
        }
        if (dto.getWarningStatus() != null) {
            updateEntity.setWarningStatus(dto.getWarningStatus());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }
        // 更新时同步lastUpdateDate
        updateEntity.setLastUpdateDate(LocalDate.now());

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long cardId) {
        StandardCostCard existing = baseMapper.selectById(cardId);
        if (existing == null) {
            throw new BusinessException("标准成本卡不存在");
        }
        return baseMapper.deleteById(cardId) > 0;
    }

    @Override
    public StandardCostCardVO getDetail(Long cardId) {
        StandardCostCard entity = baseMapper.selectById(cardId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<StandardCostCardVO> getPage(StandardCostCardQueryDTO query) {
        Page<StandardCostCard> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<StandardCostCard> wrapper = new LambdaQueryWrapper<>();

        if (query.getDishName() != null && !query.getDishName().isBlank()) {
            wrapper.like(StandardCostCard::getDishName, query.getDishName());
        }
        if (query.getWarningStatus() != null) {
            wrapper.eq(StandardCostCard::getWarningStatus, query.getWarningStatus());
        }
        wrapper.orderByDesc(StandardCostCard::getCardId);

        IPage<StandardCostCard> entityPage = baseMapper.selectPage(page, wrapper);

        Page<StandardCostCardVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<StandardCostCardVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<StandardCostCardVO> result = (IPage<StandardCostCardVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWarningStatus(Long cardId, Integer warningStatus) {
        StandardCostCard existing = baseMapper.selectById(cardId);
        if (existing == null) {
            throw new BusinessException("标准成本卡不存在");
        }
        StandardCostCard updateEntity = new StandardCostCard();
        updateEntity.setCardId(cardId);
        updateEntity.setWarningStatus(warningStatus);
        updateEntity.setLastUpdateDate(LocalDate.now());
        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    public StandardCostCardVO getByDishId(Long dishId) {
        LambdaQueryWrapper<StandardCostCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StandardCostCard::getDishId, dishId)
               .last("LIMIT 1");
        StandardCostCard entity = baseMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    /**
     * 实体转VO
     */
    private StandardCostCardVO convertToVO(StandardCostCard entity) {
        StandardCostCardVO vo = new StandardCostCardVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
