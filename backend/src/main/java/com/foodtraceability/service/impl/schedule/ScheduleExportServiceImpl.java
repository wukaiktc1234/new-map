package com.foodtraceability.service.impl.schedule;

import com.foodtraceability.dto.schedule.ConflictQueryDTO;
import com.foodtraceability.dto.schedule.ScheduleExportDTO;
import com.foodtraceability.entity.schedule.ScheduleConflict;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import com.foodtraceability.entity.schedule.SchedulePlan;
import com.foodtraceability.entity.schedule.SwapRequest;
import com.foodtraceability.mapper.schedule.ScheduleConflictMapper;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.mapper.schedule.SchedulePlanMapper;
import com.foodtraceability.mapper.schedule.SwapRequestMapper;
import com.foodtraceability.service.schedule.ScheduleExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 排班数据导出服务实现类
 * 实现排班数据、冲突、换班、考勤差异的导出功能
 *
 * <p>注意：当前为框架实现，使用简单的 CSV 格式输出
 * 完整的 Excel 导出（使用 Apache POI）后续完善
 */
@Service
public class ScheduleExportServiceImpl implements ScheduleExportService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleExportServiceImpl.class);

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 时间格式化器 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SchedulePlanMapper schedulePlanMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final ScheduleConflictMapper scheduleConflictMapper;
    private final SwapRequestMapper swapRequestMapper;

    public ScheduleExportServiceImpl(SchedulePlanMapper schedulePlanMapper,
                                     ScheduleEntryMapper scheduleEntryMapper,
                                     ScheduleConflictMapper scheduleConflictMapper,
                                     SwapRequestMapper swapRequestMapper) {
        this.schedulePlanMapper = schedulePlanMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
        this.scheduleConflictMapper = scheduleConflictMapper;
        this.swapRequestMapper = swapRequestMapper;
    }

    @Override
    public byte[] exportSchedule(ScheduleExportDTO exportDTO) {
        log.info("导出排班数据: planId={}, format={}, scope={}",
                exportDTO.getPlanId(), exportDTO.getFormat(), exportDTO.getScope());

        // 校验方案存在
        SchedulePlan plan = schedulePlanMapper.selectById(exportDTO.getPlanId());
        if (plan == null) {
            throw new RuntimeException("排班方案不存在：" + exportDTO.getPlanId());
        }

        // 查询排班条目
        List<ScheduleEntry> entries = scheduleEntryMapper.selectByPlanIds(List.of(exportDTO.getPlanId()));

        // 构建 CSV 内容
        StringBuilder csv = new StringBuilder();
        // 表头
        csv.append("员工ID,员工姓名,岗位,工作日期,班次类型,班次名称,开始时间,结束时间,来源,冲突等级\n");
        // 数据行
        for (ScheduleEntry entry : entries) {
            csv.append(safeCsv(entry.getEmployeeId()))
               .append(",").append(safeCsv(entry.getEmployeeName()))
               .append(",").append(safeCsv(entry.getPositionName()))
               .append(",").append(entry.getWorkDate() != null ? entry.getWorkDate().format(DATE_FORMATTER) : "")
               .append(",").append(safeCsv(entry.getShiftType()))
               .append(",").append(safeCsv(entry.getShiftName()))
               .append(",").append(entry.getStartTime() != null ? entry.getStartTime().format(TIME_FORMATTER) : "")
               .append(",").append(entry.getEndTime() != null ? entry.getEndTime().format(TIME_FORMATTER) : "")
               .append(",").append(safeCsv(entry.getSource()))
               .append(",").append(safeCsv(entry.getConflictLevel()))
               .append("\n");
        }

        // TODO: 实现 Excel 格式导出（使用 Apache POI）
        // 当前统一返回 CSV 格式（带 BOM 头，确保 Excel 正确识别中文）
        return addCsvBom(csv.toString());
    }

    @Override
    public byte[] exportConflicts(String planId, String format) {
        log.info("导出冲突数据: planId={}, format={}", planId, format);

        List<ScheduleConflict> conflicts = scheduleConflictMapper.selectByPlanId(planId, null);

        StringBuilder csv = new StringBuilder();
        csv.append("冲突ID,级别,类型,员工ID,员工姓名,冲突日期,班次类型,描述,修复建议,修复状态\n");
        for (ScheduleConflict conflict : conflicts) {
            csv.append(safeCsv(conflict.getConflictId()))
               .append(",").append(safeCsv(conflict.getLevel()))
               .append(",").append(safeCsv(conflict.getType()))
               .append(",").append(safeCsv(conflict.getEmployeeId()))
               .append(",").append(safeCsv(conflict.getEmployeeName()))
               .append(",").append(conflict.getConflictDate() != null ? conflict.getConflictDate().format(DATE_FORMATTER) : "")
               .append(",").append(safeCsv(conflict.getShiftType()))
               .append(",").append(safeCsv(conflict.getMessage()))
               .append(",").append(safeCsv(conflict.getSuggestion()))
               .append(",").append(safeCsv(conflict.getFixStatus()))
               .append("\n");
        }

        return addCsvBom(csv.toString());
    }

    @Override
    public byte[] exportSwapRequests(ConflictQueryDTO queryDTO, String format) {
        log.info("导出换班请求数据: planId={}, format={}", queryDTO.getPlanId(), format);

        // 复用 SwapRequestMapper 查询
        List<SwapRequest> list;
        if (queryDTO.getPlanId() != null) {
            list = swapRequestMapper.selectByPlanId(queryDTO.getPlanId());
        } else {
            // 无方案ID筛选时查询所有
            list = swapRequestMapper.selectList(null);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("请求ID,方案ID,发起人,目标员工,发起人日期,目标人日期,状态,原因,申请时间,审批人,审批时间\n");
        for (SwapRequest req : list) {
            csv.append(safeCsv(req.getRequestId()))
               .append(",").append(safeCsv(req.getPlanId()))
               .append(",").append(safeCsv(req.getInitiatorName()))
               .append(",").append(safeCsv(req.getTargetEmployeeName()))
               .append(",").append(req.getInitiatorWorkDate() != null ? req.getInitiatorWorkDate().format(DATE_FORMATTER) : "")
               .append(",").append(req.getTargetWorkDate() != null ? req.getTargetWorkDate().format(DATE_FORMATTER) : "")
               .append(",").append(safeCsv(req.getStatus()))
               .append(",").append(safeCsv(req.getReason()))
               .append(",").append(req.getCreateTime() != null ? req.getCreateTime().format(DATETIME_FORMATTER) : "")
               .append(",").append(safeCsv(req.getApproverName()))
               .append(",").append(req.getApproveTime() != null ? req.getApproveTime().format(DATETIME_FORMATTER) : "")
               .append("\n");
        }

        return addCsvBom(csv.toString());
    }

    @Override
    public byte[] exportAttendanceDiff(String planId, String format) {
        log.info("导出考勤差异报告: planId={}, format={}", planId, format);

        // TODO: 实现考勤差异报告导出
        // 当前返回仅含表头的空 CSV
        StringBuilder csv = new StringBuilder();
        csv.append("员工ID,员工姓名,日期,排班班次,排班开始,排班结束,实际签到,实际签退,差异类型,差异时长(分钟),备注\n");

        return addCsvBom(csv.toString());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 为 CSV 字符串添加 UTF-8 BOM 头
     * 确保 Excel 打开时正确识别中文编码
     * @param csvContent CSV 内容
     * @return 带BOM的字节数组
     */
    private byte[] addCsvBom(String csvContent) {
        String bom = "\uFEFF";
        return (bom + csvContent).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * CSV 字段安全处理
     * 去除字段中的逗号、换行符，避免破坏 CSV 格式
     * @param value 字段值
     * @return 安全的字段值
     */
    private String safeCsv(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace(",", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
