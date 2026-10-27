-- =====================================================================
-- V20261027_002__promotion_code_column.sql
-- bug-⑭（原「Promotion.promotionCode 指向不存在的列」）：补业务码列，不动接口契约
--
-- 背景：实体 Promotion.promotionCode 声明 @TableField("promotion_code")，但库里只有 promotion_id（主键）。
--   按码查询的分支永远 500（列不存在）；而 promotion_id 是主键、promotion_code 是**业务码**，两者本就该并存。
--   Lead 裁定：补列（只影响库、可回滚），不改契约（改 code→id 会连带前端与外部调用方）。
--
-- 不 backfill：不伪造编码（历史推广没有真实业务码）。
--   因此「按码查」在 code 为空时由应用侧返回 404「该推广未设置推广码」，而不是 500/空列表。
-- =====================================================================
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS promotion_code varchar(64);

-- 同名不同单：唯一部分索引，仅约束真正带码的推广（多个 NULL 不冲突）
CREATE UNIQUE INDEX IF NOT EXISTS uk_promotion_code
    ON promotion (promotion_code)
 WHERE promotion_code IS NOT NULL;

COMMENT ON COLUMN promotion.promotion_code IS '推广业务码（非主键；可为空，空表示未设置推广码）';