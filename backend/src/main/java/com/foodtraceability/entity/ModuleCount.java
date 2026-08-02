package com.foodtraceability.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模块统计实体类
 */
@Schema(description = "模块统计")
public class ModuleCount {
    
    @Schema(description = "模块名称", example = "食品管理")
    private String module;
    
    @Schema(description = "数量", example = "15")
    private Long count;

    public ModuleCount() {}

    public ModuleCount(String module, Long count) {
        this.module = module;
        this.count = count;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}