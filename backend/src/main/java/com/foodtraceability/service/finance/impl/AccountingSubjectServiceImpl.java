package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import com.foodtraceability.service.finance.AccountingSubjectService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会计科目Service实现
 */
@Service
public class AccountingSubjectServiceImpl extends ServiceImpl<AccountingSubjectMapper, AccountingSubject>
        implements AccountingSubjectService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountingSubject create(AccountingSubjectCreateDTO dto) {
        // 校验科目编码唯一性
        LambdaQueryWrapper<AccountingSubject> codeCheck = new LambdaQueryWrapper<>();
        codeCheck.eq(AccountingSubject::getSubjectCode, dto.getSubjectCode());
        if (baseMapper.selectCount(codeCheck) > 0) {
            throw new BusinessException("科目编码已存在: " + dto.getSubjectCode());
        }

        AccountingSubject entity = new AccountingSubject();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1); // 默认启用
        entity.setIsLeaf(dto.getParentId() != null); // 有父级则为叶子节点

        baseMapper.insert(entity);

        // 如果有父级，更新父级的isLeaf为false
        if (dto.getParentId() != null) {
            AccountingSubject parent = baseMapper.selectById(dto.getParentId());
            if (parent != null && Boolean.TRUE.equals(parent.getIsLeaf())) {
                parent.setIsLeaf(false);
                baseMapper.updateById(parent);
            }
        }

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(AccountingSubjectUpdateDTO dto) {
        AccountingSubject existing = baseMapper.selectById(dto.getSubjectId());
        if (existing == null) {
            throw new BusinessException("科目不存在");
        }

        AccountingSubject updateEntity = new AccountingSubject();
        updateEntity.setSubjectId(dto.getSubjectId());

        if (dto.getSubjectName() != null) {
            updateEntity.setSubjectName(dto.getSubjectName());
        }
        if (dto.getSubjectType() != null) {
            updateEntity.setSubjectType(dto.getSubjectType());
        }
        if (dto.getDirection() != null) {
            updateEntity.setDirection(dto.getDirection());
        }
        if (dto.getStatus() != null) {
            updateEntity.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    public List<AccountingSubjectVO> getSubjectTree() {
        // 查询所有启用的科目
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingSubject::getStatus, 1)
               .orderByAsc(AccountingSubject::getSubjectCode);
        List<AccountingSubject> allSubjects = baseMapper.selectList(wrapper);

        // 构建树形结构
        Map<Long, List<AccountingSubject>> childrenMap = allSubjects.stream()
                .filter(s -> s.getParentId() != null)
                .collect(Collectors.groupingBy(AccountingSubject::getParentId));

        List<AccountingSubjectVO> tree = new ArrayList<>();
        for (AccountingSubject subject : allSubjects) {
            if (subject.getParentId() == null) { // 一级科目
                AccountingSubjectVO vo = convertToVO(subject);
                buildChildren(vo, childrenMap);
                tree.add(vo);
            }
        }
        return tree;
    }

    @Override
    public AccountingSubjectVO getDetail(Long subjectId) {
        AccountingSubject entity = baseMapper.selectById(subjectId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<AccountingSubjectVO> getPage(AccountingSubjectQueryDTO query) {
        Page<AccountingSubject> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();

        if (query.getSubjectCode() != null && !query.getSubjectCode().isBlank()) {
            wrapper.like(AccountingSubject::getSubjectCode, query.getSubjectCode());
        }
        if (query.getSubjectName() != null && !query.getSubjectName().isBlank()) {
            wrapper.like(AccountingSubject::getSubjectName, query.getSubjectName());
        }
        if (query.getSubjectType() != null) {
            wrapper.eq(AccountingSubject::getSubjectType, query.getSubjectType());
        }
        if (query.getParentId() != null) {
            wrapper.eq(AccountingSubject::getParentId, query.getParentId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AccountingSubject::getStatus, query.getStatus());
        }
        if (Boolean.TRUE.equals(query.getLeafOnly())) {
            wrapper.eq(AccountingSubject::getIsLeaf, true);
        }
        wrapper.orderByAsc(AccountingSubject::getSubjectCode);

        IPage<AccountingSubject> entityPage = baseMapper.selectPage(page, wrapper);

        Page<AccountingSubjectVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<AccountingSubjectVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<AccountingSubjectVO> result = (IPage<AccountingSubjectVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleStatus(Long subjectId) {
        AccountingSubject entity = baseMapper.selectById(subjectId);
        if (entity == null) {
            throw new BusinessException("科目不存在");
        }

        Integer newStatus = entity.getStatus() == 1 ? 0 : 1;
        entity.setStatus(newStatus);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    public AccountingSubject getByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("科目编码不能为空");
        }
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingSubject::getSubjectCode, code).last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public List<AccountingSubject> getLeafSubjects() {
        return baseMapper.selectLeafSubjects();
    }

    /**
     * 递归构建子科目树
     */
    private void buildChildren(AccountingSubjectVO parent,
                                Map<Long, List<AccountingSubject>> childrenMap) {
        List<AccountingSubject> children = childrenMap.get(parent.getSubjectId());
        if (children == null || children.isEmpty()) {
            return;
        }
        List<AccountingSubjectVO> childVOs = new ArrayList<>();
        for (AccountingSubject child : children) {
            AccountingSubjectVO childVO = convertToVO(child);
            buildChildren(childVO, childrenMap);
            childVOs.add(childVO);
        }
        parent.setChildren(childVOs);
    }

    private AccountingSubjectVO convertToVO(AccountingSubject entity) {
        AccountingSubjectVO vo = new AccountingSubjectVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
