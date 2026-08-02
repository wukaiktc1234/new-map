package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 测验题目 DTO
 * 嵌套在 KnowledgeArticleCreateDTO/UpdateDTO 中传递
 */
@Schema(description = "测验题目")
public class QuizQuestionDTO {

    @Schema(description = "题目ID")
    private String id;

    @Schema(description = "题目内容")
    private String question;

    @Schema(description = "选项列表")
    private List<QuizOptionDTO> options;

    @Schema(description = "正确答案选项ID")
    private String correctAnswerId;

    @Schema(description = "难度（normal:普通题 required:必会题）")
    private String difficulty;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<QuizOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<QuizOptionDTO> options) {
        this.options = options;
    }

    public String getCorrectAnswerId() {
        return correctAnswerId;
    }

    public void setCorrectAnswerId(String correctAnswerId) {
        this.correctAnswerId = correctAnswerId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * 测验选项 DTO
     */
    @Schema(description = "测验选项")
    public static class QuizOptionDTO {

        @Schema(description = "选项ID（A/B/C/D）")
        private String id;

        @Schema(description = "选项文本")
        private String text;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
