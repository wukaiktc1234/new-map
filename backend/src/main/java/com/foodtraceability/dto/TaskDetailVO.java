package com.foodtraceability.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务详情视图对象
 * 继承列表字段，增加详情特有字段
 */
public class TaskDetailVO extends TaskVO {

    /** 任务详细描述 */
    private String description;

    /** 发布人ID */
    private String publisherId;

    /** 发布人姓名 */
    private String publisherName;

    /** 接收人ID列表 */
    private List<String> assigneeIds;

    /** 接收人姓名列表 */
    private List<String> assigneeNames;

    /** 工作流阶段配置 */
    private String stagesConfig;

    /** 子任务列表 */
    private List<SubTaskVO> subTasks;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisherId() { return publisherId; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public List<String> getAssigneeIds() { return assigneeIds; }
    public void setAssigneeIds(List<String> assigneeIds) { this.assigneeIds = assigneeIds; }

    public List<String> getAssigneeNames() { return assigneeNames; }
    public void setAssigneeNames(List<String> assigneeNames) { this.assigneeNames = assigneeNames; }

    public String getStagesConfig() { return stagesConfig; }
    public void setStagesConfig(String stagesConfig) { this.stagesConfig = stagesConfig; }

    public List<SubTaskVO> getSubTasks() { return subTasks; }
    public void setSubTasks(List<SubTaskVO> subTasks) { this.subTasks = subTasks; }
}
