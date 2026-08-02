package com.foodtraceability.config;

import java.util.*;

public class PromptAssemblyConfig {

    public static final String SYSTEM_PROMPT_DYNAMIC_BOUNDARY = "<DYNAMIC_BOUNDARY/>";

    public enum SectionType {
        STATIC,
        DYNAMIC
    }

    public static class PromptSection {
        private final String id;
        private final String content;
        private final SectionType type;
        private final List<String> dependencies;

        public PromptSection(String id, String content, SectionType type, List<String> dependencies) {
            this.id = id;
            this.content = content;
            this.type = type;
            this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public SectionType getType() { return type; }
        public List<String> getDependencies() { return dependencies; }
    }

    public static class PromptBuilder {
        private final Map<String, PromptSection> staticSections = new LinkedHashMap<>();
        private final Map<String, PromptSection> dynamicSections = new LinkedHashMap<>();
        private final Map<String, Object> runtimeContext = new HashMap<>();

        public PromptBuilder addStaticSection(String id, String content) {
            staticSections.put(id, new PromptSection(id, content, SectionType.STATIC, null));
            return this;
        }

        public PromptBuilder addDynamicSection(String id, String content, List<String> dependencies) {
            dynamicSections.put(id, new PromptSection(id, content, SectionType.DYNAMIC, dependencies));
            return this;
        }

        public PromptBuilder setRuntimeContext(String key, Object value) {
            runtimeContext.put(key, value);
            return this;
        }

        public String build() {
            StringBuilder prompt = new StringBuilder();

            for (PromptSection section : staticSections.values()) {
                prompt.append(section.getContent()).append("\n\n");
            }

            prompt.append(SYSTEM_PROMPT_DYNAMIC_BOUNDARY).append("\n\n");

            for (PromptSection section : dynamicSections.values()) {
                prompt.append(section.getContent()).append("\n\n");
            }

            return prompt.toString();
        }

        public Map<String, Object> getSectionMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            staticSections.forEach((id, section) -> {
                Map<String, Object> sectionInfo = new HashMap<>();
                sectionInfo.put("type", "STATIC");
                sectionInfo.put("cacheable", true);
                sectionInfo.put("dependencies", Collections.emptyList());
                map.put(id, sectionInfo);
            });
            dynamicSections.forEach((id, section) -> {
                Map<String, Object> sectionInfo = new HashMap<>();
                sectionInfo.put("type", "DYNAMIC");
                sectionInfo.put("cacheable", false);
                sectionInfo.put("dependencies", section.getDependencies());
                map.put(id, sectionInfo);
            });
            return map;
        }
    }
}
