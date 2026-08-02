package com.foodtraceability.service.impl.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RechargeFinanceLogQueryDTO;
import com.foodtraceability.dto.marketing.RechargeFinanceLogVO;
import com.foodtraceability.entity.marketing.RechargeFinanceLog;
import com.foodtraceability.mapper.marketing.RechargeFinanceLogMapper;
import com.foodtraceability.service.marketing.RechargeFinanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 储值财务流水服务实现
 * 预留财务系统对接接口，记录每笔本金/赠送变动
 *
 * 流水类型：recharge-充值 consume-消费 refund-退款 bonus_expire-赠送过期 bonus_grant-赠送发放
 */
@Service
public class RechargeFinanceServiceImpl
        extends ServiceImpl<RechargeFinanceLogMapper, RechargeFinanceLog>
        implements RechargeFinanceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<RechargeFinanceLogVO> getFinanceLogPage(RechargeFinanceLogQueryDTO queryDTO) {
        LambdaQueryWrapper<RechargeFinanceLog> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getFinanceType() != null && !queryDTO.getFinanceType().isEmpty()) {
            wrapper.eq(RechargeFinanceLog::getFinanceType, queryDTO.getFinanceType());
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            LocalDateTime startTime = parseDateTime(queryDTO.getStartTime());
            if (startTime != null) {
                wrapper.ge(RechargeFinanceLog::getCreateTime, startTime);
            }
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            LocalDateTime endTime = parseDateTime(queryDTO.getEndTime());
            if (endTime != null) {
                wrapper.le(RechargeFinanceLog::getCreateTime, endTime);
            }
        }
        wrapper.orderByDesc(RechargeFinanceLog::getCreateTime);

        Page<RechargeFinanceLog> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );
        Page<RechargeFinanceLog> resultPage = page(page, wrapper);

        List<RechargeFinanceLogVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(resultPage.getTotal(), records, resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public void exportFinanceLogs(RechargeFinanceLogQueryDTO queryDTO) {
        // 导出占位实现：实际可接入异步导出任务
    }

    @Override
    public RechargeFinanceLog recordFinanceLog(RechargeFinanceLog log) {
        // 写入财务流水记录，建立会员管理→财务中心跨模块流转
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }
        save(log);
        return log;
    }

    // ==================== 辅助方法 ====================

    /** 解析日期字符串为 LocalDateTime */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /** Entity → VO 转换（金额分→元字符串，保留正负号） */
    private RechargeFinanceLogVO convertToVO(RechargeFinanceLog log) {
        RechargeFinanceLogVO vo = new RechargeFinanceLogVO();
        vo.setLogId(log.getLogId());
        vo.setRecordNo(log.getRecordNo());
        vo.setMemberId(log.getMemberId());
        vo.setMemberName(log.getMemberName());
        vo.setFinanceType(log.getFinanceType());
        vo.setPrincipalChange(formatSignedFenToYuan(log.getPrincipalChange()));
        vo.setBonusChange(formatSignedFenToYuan(log.getBonusChange()));
        vo.setRevenueChange(formatSignedFenToYuan(log.getRevenueChange()));
        vo.setBalanceAfter(formatFenToYuan(log.getBalanceAfter()));
        vo.setPrincipalAfter(formatFenToYuan(log.getPrincipalAfter()));
        vo.setBonusAfter(formatFenToYuan(log.getBonusAfter()));
        vo.setRemark(log.getRemark());
        vo.setCreatedAt(log.getCreateTime() != null ? log.getCreateTime().format(DATE_FORMATTER) : null);
        return vo;
    }

    /** 分（Long）转元（字符串，保留两位小数） */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }

    /** 分（Long，可能为负）转元（字符串，带正负号） */
    private String formatSignedFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        if (fen >= 0) {
            return "+" + String.format("%.2f", fen / 100.0);
        } else {
            return String.format("%.2f", fen / 100.0);
        }
    }
}
