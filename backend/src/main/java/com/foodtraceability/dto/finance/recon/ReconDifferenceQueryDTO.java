package com.foodtraceability.dto.finance.recon;

import java.time.LocalDateTime;

/**
 * R8 差异池查询条件（status / type / runId / assigneeId + 可选时间窗 + 分页）。
 *
 * <p><b>from / to 是可选的时间窗</b>（按 {@code recon_differences.create_time} 过滤）：
 * <ul>
 *   <li>不传 → 与历史行为**逐字一致**的 SQL（不加任何时间条件）；</li>
 *   <li>只传 from → {@code create_time >= from}；只传 to → {@code create_time < to}；</li>
 *   <li>两者都传 → **半开区间 [from, to)**，避免「同日边界」被两个区间重复计入。</li>
 * </ul>
 * 前端可直接用 ISO-8601（{@code 2026-10-01T00:00:00}）传参；格式非法时由 Spring 转换失败 → 400。</p>
 */
public class ReconDifferenceQueryDTO {

    /** OPEN / ASSIGNED / RESOLVING / CLOSED / EXEMPT / PENDING_P2_REVERSE */
    private String status;
    /** AMOUNT_DIFF / TIME_DIFF / ONE_SIDED_INTERNAL / ONE_SIDED_EXTERNAL / DUPLICATE / CHANNEL_FEE / AMBIGUOUS */
    private String type;
    private Long runId;
    private Long assigneeId;
    /** 可选：create_time >= from（不传即原行为） */
    private LocalDateTime from;
    /** 可选：create_time < to（半开区间上界；不传即原行为） */
    private LocalDateTime to;
    private Long current = 1L;
    private Long size = 20L;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public LocalDateTime getFrom() { return from; }
    public void setFrom(LocalDateTime from) { this.from = from; }

    public LocalDateTime getTo() { return to; }
    public void setTo(LocalDateTime to) { this.to = to; }

    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
