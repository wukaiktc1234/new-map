package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 知识库文章更新 DTO
 * 所有字段可选，仅更新传入的字段
 */
@Schema(description = "知识库文章更新DTO")
public class KnowledgeArticleUpdateDTO {

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "分类（safety/service/manual/policy）")
    private String category;

    @Schema(description = "图标名称")
    private String icon;

    @Schema(description = "文章内容（Markdown 格式）")
    private String content;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "作者姓名")
    private String author;

    @Schema(description = "发布状态")
    private String publishStatus;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "测验题目列表")
    private List<QuizQuestionDTO> quiz;

    @Schema(description = "是否为重要内容")
    private Boolean isImportant;

    @Schema(description = "作者所属部门")
    private String authorDepartment;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<QuizQuestionDTO> getQuiz() {
        return quiz;
    }

    public void setQuiz(List<QuizQuestionDTO> quiz) {
        this.quiz = quiz;
    }

    public Boolean getIsImportant() {
        return isImportant;
    }

    public void setIsImportant(Boolean isImportant) {
        this.isImportant = isImportant;
    }

    public String getAuthorDepartment() {
        return authorDepartment;
    }

    public void setAuthorDepartment(String authorDepartment) {
        this.authorDepartment = authorDepartment;
    }
}
