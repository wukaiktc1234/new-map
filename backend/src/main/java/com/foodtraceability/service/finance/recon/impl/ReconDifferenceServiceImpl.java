package com.foodtraceability.service.finance.recon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.finance.recon.ChannelFeeSummaryVO;
import com.foodtraceability.dto.finance.recon.ReconDifferenceQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconDifferenceVO;
import com.foodtraceability.dto.finance.recon.ReconResolveDTO;
import com.foodtraceability.entity.finance.recon.ReconDifference;
import com.foodtraceability.mapper.finance.recon.ReconDifferenceMapper;
import com.foodtraceability.service.finance.recon.ReconDictionary;
import com.foodtraceability.service.finance.recon.ReconDifferenceService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 差异池服务实现（P1-B：R8 / R9 / R10）。
 *
 * <p><b>红线（P1 范围锁）</b>：本类只读写 recon_differences；对 fund_flows / bank_accounts <b>零写入</b>：
 * WRITE_OFF 只记录决议（status=CLOSED + resolution_remark），REVERSE 只把状态置为
 * {@link ReconDictionary#DIFF_PENDING_P2_REVERSE}（PENDING_P2_REVERSE，等 P2 执行反向流水，TD-28），
 * 两者都<b>不会</b>生成任何流水、不冲正、不改余额。</p>
 *
 * <p><b>状态机</b>：OPEN --claim--> ASSIGNED --resolve--> CLOSED(WRITE_OFF) / EXEMPT(EXEMPT) /
 * PENDING_P2_REVERSE(REVERSE)。resolve 与 claim 各自是一个事务：resolve 失败整体回滚，
 * 差异停留在「认领后的状态」，不会出现 CLOSED 但未落库的中间态。</p>
 */
@Service
public class ReconDifferenceServiceImpl implements ReconDifferenceService {

    private static final Logger log = LoggerFactory.getLogger(ReconDifferenceServiceImpl.class);

    /** 允许 resolve 的入口状态（已 OPEN 未认领也允许直接处理；已终结的拒绝） */
    private static final Set<String> RESOLVABLE_STATUS =
            Set.of(ReconDictionary.DIFF_OPEN, ReconDictionary.DIFF_ASSIGNED, ReconDictionary.DIFF_RESOLVING);

    private final ReconDifferenceMapper reconDifferenceMapper;

    public ReconDifferenceServiceImpl(ReconDifferenceMapper reconDifferenceMapper) {
        this.reconDifferenceMapper = reconDifferenceMapper;
    }

    @Override
    public IPage<ReconDifferenceVO> listDifferences(ReconDifferenceQueryDTO query) {
        ReconDifferenceQueryDTO condition = query == null ? new ReconDifferenceQueryDTO() : query;
        LambdaQueryWrapper<ReconDifference> wrapper = new LambdaQueryWrapper<>();
        if (condition.getStatus() != null && !condition.getStatus().isBlank()) {
            wrapper.eq(ReconDifference::getStatus, condition.getStatus().trim());
        }
        if (condition.getType() != null && !condition.getType().isBlank()) {
            wrapper.eq(ReconDifference::getDifferenceType, condition.getType().trim());
        }
        if (condition.getRunId() != null) {
            wrapper.eq(ReconDifference::getRunId, condition.getRunId());
        }
        if (condition.getAssigneeId() != null) {
            wrapper.eq(ReconDifference::getAssigneeId, condition.getAssigneeId());
        }
        // 可选时间窗（按 create_time）：from 含端点、to 不含端点 —— 半开区间 [from, to)，
        // 避免相邻两天在同一时刻被重复统计；两者都不传时不追加任何条件，SQL 与历史逐字一致。
        if (condition.getFrom() != null) {
            wrapper.ge(ReconDifference::getCreateTime, condition.getFrom());
        }
        if (condition.getTo() != null) {
            wrapper.lt(ReconDifference::getCreateTime, condition.getTo());
        }
        wrapper.orderByDesc(ReconDifference::getDifferenceId);

        Page<ReconDifference> page = new Page<>(normalizeCurrent(condition.getCurrent()), normalizeSize(condition.getSize()));
        return reconDifferenceMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconDifferenceVO claim(Long differenceId) {
        ReconDifference diff = requireDifference(differenceId);

        if (ReconDictionary.DIFF_CLOSED.equals(diff.getStatus())
                || ReconDictionary.DIFF_EXEMPT.equals(diff.getStatus())
                || ReconDictionary.DIFF_PENDING_P2_REVERSE.equals(diff.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "差异已处理完毕，不可认领：id=" + differenceId + ", status=" + diff.getStatus());
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (ReconDictionary.DIFF_ASSIGNED.equals(diff.getStatus())
                && diff.getAssigneeId() != null
                && currentUserId != null
                && !diff.getAssigneeId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "差异已被他人认领：id=" + differenceId + ", assigneeId=" + diff.getAssigneeId());
        }

        LocalDateTime now = LocalDateTime.now();
        diff.setStatus(ReconDictionary.DIFF_ASSIGNED);
        diff.setAssigneeId(currentUserId);
        diff.setAssignedAt(now);
        diff.setUpdateTime(now);
        reconDifferenceMapper.updateById(diff);
        log.info("[METRIC] recon_difference_claimed id={} assigneeId={}", differenceId, currentUserId);
        return toVO(diff);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconDifferenceVO resolve(Long differenceId, ReconResolveDTO dto) {
        if (dto == null || dto.getResolution() == null || dto.getResolution().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "resolution 不能为空");
        }
        if (dto.getRemark() == null || dto.getRemark().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理说明（remark）必填");
        }
        String resolution = dto.getResolution().trim();
        if (!ReconDictionary.isValidResolution(resolution)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的决议：" + resolution + "（仅 WRITE_OFF / EXEMPT / REVERSE）");
        }

        ReconDifference diff = requireDifference(differenceId);
        if (!RESOLVABLE_STATUS.contains(diff.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前状态不允许处理：id=" + differenceId + ", status=" + diff.getStatus());
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (ReconDictionary.DIFF_ASSIGNED.equals(diff.getStatus())
                && diff.getAssigneeId() != null
                && currentUserId != null
                && !diff.getAssigneeId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "差异已他人认领，不可代为处理：id=" + differenceId + ", assigneeId=" + diff.getAssigneeId());
        }

        String targetStatus = switch (resolution) {
            case ReconDictionary.RES_WRITE_OFF -> ReconDictionary.DIFF_CLOSED;
            case ReconDictionary.RES_EXEMPT -> ReconDictionary.DIFF_EXEMPT;
            case ReconDictionary.RES_REVERSE -> ReconDictionary.DIFF_PENDING_P2_REVERSE;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的决议：" + resolution);
        };

        LocalDateTime now = LocalDateTime.now();
        diff.setStatus(targetStatus);
        diff.setResolution(resolution);
        diff.setResolutionRemark(dto.getRemark().trim());
        diff.setResolutionBy(currentUserId);
        diff.setResolutionTime(now);
        diff.setUpdateTime(now);
        // REVERSE 仅登记决议，reverse_flow_no 留空：反向流水由 P2 执行（TD-28），本阶段不写 fund_flows
        if (!ReconDictionary.RES_REVERSE.equals(resolution)) {
            diff.setReverseFlowNo(null);
        }
        reconDifferenceMapper.updateById(diff);
        log.info("[METRIC] recon_difference_resolved id={} resolution={} status={} operator={}",
                differenceId, resolution, targetStatus, currentUserId);
        return toVO(diff);
    }

    private ReconDifference requireDifference(Long differenceId) {
        if (differenceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "differenceId 不能为空");
        }
        ReconDifference diff = reconDifferenceMapper.selectById(differenceId);
        if (diff == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "差异记录不存在：id=" + differenceId);
        }
        return diff;
    }

    private ReconDifferenceVO toVO(ReconDifference diff) {
        ReconDifferenceVO vo = new ReconDifferenceVO();
        vo.setDifferenceId(diff.getDifferenceId());
        vo.setRunId(diff.getRunId());
        vo.setDifferenceType(diff.getDifferenceType());
        vo.setInternalRef(diff.getInternalRef());
        vo.setExternalRef(diff.getExternalRef());
        vo.setAmountInternal(diff.getAmountInternal());
        vo.setAmountExternal(diff.getAmountExternal());
        vo.setDiffAmount(diff.getDiffAmount());
        vo.setStatus(diff.getStatus());
        vo.setResolution(diff.getResolution());
        vo.setResolutionRemark(diff.getResolutionRemark());
        vo.setReverseFlowNo(diff.getReverseFlowNo());
        vo.setAssigneeId(diff.getAssigneeId());
        vo.setAssignedAt(diff.getAssignedAt());
        vo.setResolutionBy(diff.getResolutionBy());
        vo.setResolutionTime(diff.getResolutionTime());
        vo.setReopenOf(diff.getReopenOf());
        vo.setCreateTime(diff.getCreateTime());
        vo.setUpdateTime(diff.getUpdateTime());
        return vo;
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 200L);
    }

    @Override
    public ChannelFeeSummaryVO channelFeeSummary(Long runId, LocalDateTime from, LocalDateTime to) {
        // 只读聚合：一条 SQL 完成分组；三个条件都不传时与「无 <if> 版本」逐字相同
        List<ChannelFeeSummaryVO.Row> rows = reconDifferenceMapper.channelFeeSummary(runId, from, to);
        ChannelFeeSummaryVO vo = new ChannelFeeSummaryVO();
        vo.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        ChannelFeeSummaryVO.Row total = new ChannelFeeSummaryVO.Row();
        total.sourceType = "ALL";
        long cnt = 0L, fee = 0L, diff = 0L;
        for (ChannelFeeSummaryVO.Row r : vo.rows) {
            cnt += r.diffCount == null ? 0L : r.diffCount;
            fee += r.feeTotalFen == null ? 0L : r.feeTotalFen;
            diff += r.diffAmountFen == null ? 0L : r.diffAmountFen;
        }
        total.diffCount = cnt;
        total.feeTotalFen = fee;
        total.diffAmountFen = diff;
        vo.total = total;
        log.info("[CHANNEL_FEE_SUMMARY] runId={} from={} to={} groups={} rows={} diffFen={}",
                runId, from, to, vo.rows.size(), cnt, diff);
        return vo;
    }
}
