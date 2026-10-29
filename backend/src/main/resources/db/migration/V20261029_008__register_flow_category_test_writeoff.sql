-- V20261029_008__register_flow_category_test_writeoff.sql
--
-- 注册 fund_flows.flow_category = 8 (TEST_WRITEOFF) —— **代码侧类别**（常量在
-- common/finance/FundFlowCategory.java，注释标明「仅用于测试资金冲销，禁止业务使用」）。
--
-- 背景（2026-10-29 Owner 授权）：夹具与门禁脚本曾向真实账户（门店1现金账户 account_id=20）
-- 记入测试资金；冲销需要一个既不属于退款(2)、也不属于内部转账(5)、也不属于采购付款(6/7)的类别，
-- 否则冲销金额会被 A30/A31/A33（退款对拍）或 A14/A36（转账配对）误算。
--
-- 现状核查（2026-10-29 实测）：
--   ① 全库没有 flow_category 字典表（sys_dict / sys_dict_item 均无 flow/fund 类型），
--      类别唯一权威定义就是 Java 常量；因此本迁移**不新建表、不插字典行**。
--   ② 本迁移只做两项不变量断言：
--      (a) fund_flows.flow_category 存在且为整数类型；
--      (b) 若已有 flow_category=8 的行，**必须全部**是冲销署名 'system/test-writeoff'
--          （防止业务代码误用 8；若发现有其它署名 → 直接失败）。
--
-- 消亡条件：若未来引入 flow_category 字典表，本类别应在其后补一个迁移写字典行。

DO $$
DECLARE
    v_col_type text;
    v_total bigint;
    v_foreign bigint;
BEGIN
    SELECT data_type INTO v_col_type
      FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'fund_flows' AND column_name = 'flow_category';

    IF v_col_type IS NULL THEN
        RAISE EXCEPTION 'V008 断言失败：fund_flows.flow_category 不存在';
    END IF;

    IF v_col_type NOT IN ('integer', 'bigint', 'smallint') THEN
        RAISE EXCEPTION 'V008 断言失败：flow_category 类型为 %，期望整数', v_col_type;
    END IF;

    SELECT count(*) INTO v_total FROM fund_flows WHERE flow_category = 8;
    SELECT count(*) INTO v_foreign FROM fund_flows
     WHERE flow_category = 8 AND COALESCE(created_by, '') <> 'system/test-writeoff';

    IF v_foreign > 0 THEN
        RAISE EXCEPTION 'V008 断言失败：flow_category=8 存在 % 行非冲销署名（created_by <> system/test-writeoff），本类别禁止业务使用',
            v_foreign;
    END IF;

    RAISE NOTICE 'V008 OK：flow_category=8 (TEST_WRITEOFF) 已注册；当前 % 行使用，全部为冲销署名', v_total;
END $$;
