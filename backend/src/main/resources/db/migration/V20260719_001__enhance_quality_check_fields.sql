-- 增强采购入库单质检字段
-- 贴合生产生活的质检信息：外观、气味、温度、湿度、抽检、不合格分类、处理意见、单据核查

ALTER TABLE purchase_stockins
    ADD COLUMN IF NOT EXISTS appearance_result VARCHAR(20),        -- 外观检查结果：normal(正常)/abnormal(异常)
    ADD COLUMN IF NOT EXISTS odor_result VARCHAR(20),              -- 气味检查结果：normal(正常)/abnormal(异常)
    ADD COLUMN IF NOT EXISTS temperature DECIMAL(5, 2),            -- 实测温度(℃)
    ADD COLUMN IF NOT EXISTS humidity DECIMAL(5, 2),               -- 实测湿度(%)
    ADD COLUMN IF NOT EXISTS sample_quantity DECIMAL(10, 3),       -- 抽检数量
    ADD COLUMN IF NOT EXISTS sample_rate DECIMAL(5, 2),            -- 抽检比例(%)
    ADD COLUMN IF NOT EXISTS unqualified_type VARCHAR(50),         -- 不合格类型
    ADD COLUMN IF NOT EXISTS disposal_opinion VARCHAR(50),         -- 处理意见：return(退货)/concession(让步接收)/scrap(报废)/sort(挑选使用)
    ADD COLUMN IF NOT EXISTS document_check VARCHAR(20);           -- 随货单据核查：complete(齐全)/incomplete(不齐全)/none(无)

COMMENT ON COLUMN purchase_stockins.appearance_result IS '外观检查结果';
COMMENT ON COLUMN purchase_stockins.odor_result IS '气味检查结果';
COMMENT ON COLUMN purchase_stockins.temperature IS '实测温度(℃)';
COMMENT ON COLUMN purchase_stockins.humidity IS '实测湿度(%)';
COMMENT ON COLUMN purchase_stockins.sample_quantity IS '抽检数量';
COMMENT ON COLUMN purchase_stockins.sample_rate IS '抽检比例(%)';
COMMENT ON COLUMN purchase_stockins.unqualified_type IS '不合格类型';
COMMENT ON COLUMN purchase_stockins.disposal_opinion IS '处理意见';
COMMENT ON COLUMN purchase_stockins.document_check IS '随货单据核查结果';
