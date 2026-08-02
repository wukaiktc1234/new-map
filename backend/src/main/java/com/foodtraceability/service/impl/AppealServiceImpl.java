package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.Appeal;
import com.foodtraceability.entity.AppealAttachment;
import com.foodtraceability.entity.AppealProcessLog;
import com.foodtraceability.mapper.AppealAttachmentMapper;
import com.foodtraceability.mapper.AppealMapper;
import com.foodtraceability.mapper.AppealProcessLogMapper;
import com.foodtraceability.service.AppealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 申诉业务服务实现类
 *
 * 已实现申诉的创建、查询、审核、撤销等基础CRUD与流程编排
 * 待补充：权限校验、附件联动查询、流程日志增强等扩展逻辑
 */
@Service
public class AppealServiceImpl implements AppealService {

    private final AppealMapper appealMapper;
    private final AppealAttachmentMapper attachmentMapper;
    private final AppealProcessLogMapper processLogMapper;

    /**
     * 构造函数注入依赖
     */
    public AppealServiceImpl(AppealMapper appealMapper,
                              AppealAttachmentMapper attachmentMapper,
                              AppealProcessLogMapper processLogMapper) {
        this.appealMapper = appealMapper;
        this.attachmentMapper = attachmentMapper;
        this.processLogMapper = processLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppealDetailVO createAppeal(AppealCreateDTO dto, String userId) {
        // 1. 构建申诉实体
        Appeal appeal = new Appeal();
        appeal.setType(dto.getType());
        appeal.setAnonymousFlag(dto.getAnonymousFlag());
        appeal.setEmployeeId(userId);
        appeal.setTargetDecisionId(dto.getTargetDecisionId());
        appeal.setStatus("pending");
        appeal.setTitle(dto.getTitle());
        appeal.setDescription(dto.getDescription());
        appeal.setExpectedResult(dto.getExpectedResult());

        // 2. 保存申诉
        appealMapper.insert(appeal);

        // 3. 记录提交日志
        logAction(appeal.getAppealId(), "submit", userId, "提交申诉申请");

        // 4. 返回详情（Mock转换）
        return convertToDetailVO(appeal);
    }

    @Override
    public PageResult<AppealVO> getMyAppeals(String userId, String status, int current, int size) {
        Page<Appeal> page = new Page<>(current, size);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getEmployeeId, userId)
                .orderByDesc(Appeal::getCreateTime);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Appeal::getStatus, status);
        }

        Page<Appeal> result = appealMapper.selectPage(page, wrapper);

        PageResult<AppealVO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent((long) current);
        pageResult.setSize((long) size);
        return pageResult;
    }

    @Override
    public AppealDetailVO getAppealDetail(String appealId, String userId) {
        Appeal appeal = appealMapper.selectDetailById(appealId);
        if (appeal == null) {
            throw new RuntimeException("申诉不存在");
        }
        // TODO: 权限校验 - 本人或审核人可查看

        return convertToDetailVO(appeal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawAppeal(String appealId, String userId) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new RuntimeException("申诉不存在");
        }
        if (!"pending".equals(appeal.getStatus()) && !"processing".equals(appeal.getStatus())) {
            throw new RuntimeException("当前状态不允许撤回");
        }

        // 更新状态为已撤回
        appeal.setStatus("withdrawn");
        appeal.setUpdateTime(LocalDateTime.now());
        appealMapper.updateById(appeal);

        // 记录日志
        logAction(appealId, "withdraw", userId, "申请人撤回申诉");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processAppeal(String appealId, AppealProcessDTO dto, String operatorId) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new RuntimeException("申诉不存在");
        }

        // 根据动作更新状态
        switch (dto.getAction()) {
            case "accept":
                appeal.setStatus("processing");
                break;
            case "resolve":
                appeal.setStatus("resolved");
                break;
            case "close":
                appeal.setStatus("closed");
                break;
            case "reject":
                appeal.setStatus("pending"); // 驳回后回到待处理，可重新提交
                break;
            default:
                throw new RuntimeException("不支持的操作类型: " + dto.getAction());
        }

        appeal.setUpdateTime(LocalDateTime.now());
        appealMapper.updateById(appeal);

        // 记录处理日志
        logAction(appealId, dto.getAction(), operatorId, dto.getComment());
    }

    // ========== 私有辅助方法 ==========

    /** 记录处理日志 */
    private void logAction(String appealId, String action, String operatorId, String comment) {
        AppealProcessLog log = new AppealProcessLog();
        log.setAppealId(appealId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        // TODO: 根据operatorId查询姓名
        log.setOperatorName(operatorId); // 临时使用ID作为名称
        log.setComment(comment);
        processLogMapper.insert(log);
    }

    /** 转换为列表VO */
    private AppealVO convertToVO(Appeal appeal) {
        AppealVO vo = new AppealVO();
        vo.setAppealId(appeal.getAppealId());
        vo.setType(appeal.getType());
        vo.setAnonymousFlag(appeal.getAnonymousFlag());
        vo.setStatus(appeal.getStatus());
        vo.setTitle(appeal.getTitle());
        vo.setCreateTime(appeal.getCreateTime());
        // TODO: 查询附件数量
        vo.setAttachmentCount(0);
        return vo;
    }

    /** 转换为详情VO */
    private AppealDetailVO convertToDetailVO(Appeal appeal) {
        AppealDetailVO vo = new AppealDetailVO();
        // 复制基础字段
        vo.setAppealId(appeal.getAppealId());
        vo.setType(appeal.getType());
        vo.setAnonymousFlag(appeal.getAnonymousFlag());
        vo.setStatus(appeal.getStatus());
        vo.setTitle(appeal.getTitle());
        vo.setCreateTime(appeal.getCreateTime());
        vo.setDescription(appeal.getDescription());
        vo.setTargetDecisionId(appeal.getTargetDecisionId());
        vo.setExpectedResult(appeal.getExpectedResult());
        // TODO: 查询附件和处理日志
        vo.setAttachments(java.util.Collections.emptyList());
        vo.setProcessLogs(java.util.Collections.emptyList());
        vo.setAttachmentCount(0);
        return vo;
    }
}
