package com.foodtraceability.service.impl.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.AnomalyAlertQueryDTO;
import com.foodtraceability.dto.marketing.AnomalyAlertVO;
import com.foodtraceability.entity.marketing.AnomalyAlert;
import com.foodtraceability.mapper.marketing.AnomalyAlertMapper;
import com.foodtraceability.service.marketing.AnomalyAlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 异常交易告警服务实现
 * 风控触发异常时生成告警记录，支持处理和忽略操作
 *
 * 状态机：
 * - pending → handled（处理告警）
 * - pending → ignored（忽略告警）
 */
@Service
public class AnomalyAlertServiceImpl
        extends ServiceImpl<AnomalyAlertMapper, AnomalyAlert>
        implements AnomalyAlertService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<AnomalyAlertVO> getAlertPage(AnomalyAlertQueryDTO queryDTO) {
        LambdaQueryWrapper<AnomalyAlert> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(AnomalyAlert::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(AnomalyAlert::getAlertTime);

        Page<AnomalyAlert> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );
        Page<AnomalyAlert> resultPage = page(page, wrapper);

        List<AnomalyAlertVO> records = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(resultPage.getTotal(), records, resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(String alertId, String remark) {
        AnomalyAlert alert = getById(alertId);
        if (alert == null) {
            throw new RuntimeException("异常告警不存在：" + alertId);
        }
        if (!AnomalyAlert.STATUS_PENDING.equals(alert.getStatus())) {
            throw new RuntimeException("仅待处理状态的告警可处理，当前状态：" + alert.getStatus());
        }
        alert.setStatus(AnomalyAlert.STATUS_HANDLED);
        alert.setHandler("当前用户");
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(remark);
        updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignoreAlert(String alertId) {
        AnomalyAlert alert = getById(alertId);
        if (alert == null) {
            throw new RuntimeException("异常告警不存在：" + alertId);
        }
        if (!AnomalyAlert.STATUS_PENDING.equals(alert.getStatus())) {
            throw new RuntimeException("仅待处理状态的告警可忽略，当前状态：" + alert.getStatus());
        }
        alert.setStatus(AnomalyAlert.STATUS_IGNORED);
        alert.setHandler("当前用户");
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    // ==================== 辅助方法 ====================

    /** Entity → VO 转换 */
    private AnomalyAlertVO convertToVO(AnomalyAlert alert) {
        AnomalyAlertVO vo = new AnomalyAlertVO();
        vo.setAlertId(alert.getAlertId());
        vo.setMemberId(alert.getMemberId());
        vo.setMemberName(alert.getMemberName());
        vo.setAlertType(alert.getAlertType());
        vo.setDescription(alert.getDescription());
        vo.setTriggerAmount(formatFenToYuan(alert.getTriggerAmount()));
        vo.setAlertTime(alert.getAlertTime() != null ? alert.getAlertTime().format(DATE_FORMATTER) : null);
        vo.setStatus(alert.getStatus());
        vo.setHandler(alert.getHandler());
        vo.setHandleTime(alert.getHandleTime() != null ? alert.getHandleTime().format(DATE_FORMATTER) : null);
        vo.setHandleRemark(alert.getHandleRemark());
        return vo;
    }

    /** 分（Long）转元（字符串，保留两位小数） */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }
}
