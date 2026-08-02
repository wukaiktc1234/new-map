-- ============================================================
-- 迁移脚本：为 ai_model_configs 表补充前端已采集的模型参数字段
-- 说明：前端 AI 模型配置页已采集服务商/模型版本/默认模型/温度/token/上下文长度/重试次数，但后端表未持久化
-- ============================================================

ALTER TABLE ai_model_configs
    ADD COLUMN IF NOT EXISTS provider VARCHAR(64),
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(128),
    ADD COLUMN IF NOT EXISTS default_model VARCHAR(128),
    ADD COLUMN IF NOT EXISTS temperature DECIMAL(3,1) DEFAULT 0.7,
    ADD COLUMN IF NOT EXISTS max_tokens INTEGER DEFAULT 2048,
    ADD COLUMN IF NOT EXISTS context_length INTEGER DEFAULT 4096,
    ADD COLUMN IF NOT EXISTS max_retries INTEGER DEFAULT 3;

COMMENT ON COLUMN ai_model_configs.provider IS '服务商标识（openai/azure/anthropic/qwen/deepseek 等）';
COMMENT ON COLUMN ai_model_configs.model_version IS '模型版本（仅 api 类型，如 gpt-4）';
COMMENT ON COLUMN ai_model_configs.default_model IS '默认模型名称（仅 api 类型）';
COMMENT ON COLUMN ai_model_configs.temperature IS '温度参数（仅 api 类型，默认 0.7）';
COMMENT ON COLUMN ai_model_configs.max_tokens IS '最大 token 数（仅 api 类型，默认 2048）';
COMMENT ON COLUMN ai_model_configs.context_length IS '上下文长度（仅 api 类型，默认 4096）';
COMMENT ON COLUMN ai_model_configs.max_retries IS '重试次数（默认 3）';

CREATE INDEX IF NOT EXISTS idx_ai_model_configs_provider ON ai_model_configs(provider);
