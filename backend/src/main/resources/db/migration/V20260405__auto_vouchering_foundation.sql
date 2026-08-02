-- ========================================
-- 自动记账规则引擎基础建设 - 数据库迁移
-- 版本：v1.0
-- 日期：2026-04-05
-- 内容：
--   1. 扩展会计科目表 - 餐饮业二级科目初始化
--   2. 创建自动记账规则引擎表（4张核心表）
--   3. 扩展凭证表字段（支持自动记账追溯）
--   4. 插入18条初始业务规则及分录模板
-- ========================================

-- ============================================================================
-- 第一部分：扩展会计科目表 — 餐饮业二级科目初始化
-- ============================================================================

-- 创建会计科目表（V7.0.0已禁用，此处补充建表）
CREATE TABLE IF NOT EXISTS accounting_subject (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code               VARCHAR(20) NOT NULL UNIQUE,
    name               VARCHAR(100) NOT NULL,
    category           VARCHAR(20) NOT NULL,
    type               VARCHAR(20) NOT NULL,
    parent_id          BIGINT,
    balance_direction  VARCHAR(4) NOT NULL DEFAULT '借',
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description        TEXT,
    create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE accounting_subject IS '会计科目表';
COMMENT ON COLUMN accounting_subject.code IS '科目编码';
COMMENT ON COLUMN accounting_subject.name IS '科目名称';
COMMENT ON COLUMN accounting_subject.category IS '科目类别：资产/负债/权益/成本/损益';
COMMENT ON COLUMN accounting_subject.type IS '科目级别：一级科目/二级科目';
COMMENT ON COLUMN accounting_subject.parent_id IS '父科目ID';
COMMENT ON COLUMN accounting_subject.balance_direction IS '余额方向：借/贷';
COMMENT ON COLUMN accounting_subject.status IS '状态：ACTIVE/INACTIVE';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_accounting_subject_code ON accounting_subject(code);
CREATE INDEX IF NOT EXISTS idx_accounting_subject_parent ON accounting_subject(parent_id);

-- ---------------------------------------------------------------
-- 1.1 新增一级科目（原表缺少的重要餐饮业科目）
-- ---------------------------------------------------------------

INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('1012', '其他货币资金', '资产', '一级科目', NULL, '借', 'ACTIVE', '除现金和银行存款以外的其他各种货币资金（微信、支付宝、美团收款等）'),
('1411', '低值易耗品', '资产', '一级科目', NULL, '借', 'ACTIVE', '不能作为固定资产核算的各种用具物品（餐具厨具、清洁用品等）'),
('1801', '长期待摊费用', '资产', '一级科目', NULL, '借', 'ACTIVE', '已经发生但应由本期和以后各期负担的分摊期限在1年以上的各项费用');

-- ---------------------------------------------------------------
-- 1.2 资产类二级科目
-- ---------------------------------------------------------------

-- 1002 银行存款 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('100201', '银行存款-基本户', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1002'), '借', 'ACTIVE', '企业基本结算账户'),
('100202', '银行存款-一般户', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1002'), '借', 'ACTIVE', '企业一般结算账户');

-- 1122 应收账款 → 二级（按平台/客户）
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('112201', '应收账款-美团', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1122'), '借', 'ACTIVE', '美团平台应收款项'),
('112202', '应收账款-饿了么', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1122'), '借', 'ACTIVE', '饿了么平台应收款项'),
('112203', '应收账款-抖音团购', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1122'), '借', 'ACTIVE', '抖音团购平台应收款项'),
('112299', '应收账款-其他客户', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1122'), '借', 'ACTIVE', '其他客户应收款项');

-- 1221 其他应收款 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('122101', '其他应收款-员工借款', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1221'), '借', 'ACTIVE', '员工因公借款'),
('122102', '其他应收款-押金保证金', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1221'), '借', 'ACTIVE', '各类押金及保证金');

-- 1403 原材料 → 二级（餐饮核心！）
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('140301', '原材料-肉类', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1403'), '借', 'ACTIVE', '猪牛羊鸡鸭鱼等肉类食材'),
('140302', '原材料-蔬菜', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1403'), '借', 'ACTIVE', '时令蔬菜、根茎叶菜等'),
('140303', '原材料-调料', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1403'), '借', 'ACTIVE', '酱油醋盐糖辣椒等调味品'),
('140304', '原材料-粮油', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1403'), '借', 'ACTIVE', '米面油杂粮等主食原料'),
('140305', '原材料-酒水饮料', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1403'), '借', 'ACTIVE', '啤酒白酒饮料果汁等酒水饮料'),
('140306', '原材料-其他', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1403'), '借', 'ACTIVE', '一次性用品、包装等其他材料');

-- 1405 库存商品 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('140501', '库存商品-预包装食品', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1405'), '借', 'ACTIVE', '预包装成品食品'),
('140502', '库存商品-半成品', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1405'), '借', 'ACTIVE', '厨房加工的半成品');

-- 1601 固定资产 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('160101', '固定资产-厨房设备', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1601'), '借', 'ACTIVE', '炉灶、烤箱、蒸柜等厨房专用设备'),
('160102', '固定资产-电子设备', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1601'), '借', 'ACTIVE', 'POS机、收银系统、电脑等电子设备'),
('160103', '固定资产-家具', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1601'), '借', 'ACTIVE', '桌椅、空调、冰柜等家具设备');

-- 1602 累计折旧 → 对应分类的二级折旧
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('160201', '累计折旧-厨房设备', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1602'), '贷', 'ACTIVE', '厨房设备累计折旧'),
('160202', '累计折旧-电子设备', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1602'), '贷', 'ACTIVE', '电子设备累计折旧'),
('160203', '累计折旧-家具', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1602'), '贷', 'ACTIVE', '家具设备累计折旧');

-- 1012 其他货币资金 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('101201', '其他货币资金-微信', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1012'), '借', 'ACTIVE', '微信支付账户余额'),
('101202', '其他货币资金-支付宝', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1012'), '借', 'ACTIVE', '支付宝账户余额'),
('101203', '其他货币资金-美团收款', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1012'), '借', 'ACTIVE', '美团收款账户余额');

-- 1411 低值易耗品 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('141101', '低值易耗品-餐具厨具', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1411'), '借', 'ACTIVE', '碗筷盘勺锅铲等餐具厨具'),
('141102', '低值易耗品-清洁用品', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1411'), '借', 'ACTIVE', '洗涤剂、消毒液等清洁用品'),
('141103', '低值易耗品-办公耗材', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1411'), '借', 'ACTIVE', '打印纸、笔等办公用品');

-- 1801 长期待摊费用 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('180101', '长期待摊费用-装修费', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1801'), '借', 'ACTIVE', '店铺装修费分期摊销'),
('180102', '长期待摊费用-租金', '资产', '二级科目', (SELECT id FROM accounting_subject WHERE code = '1801'), '借', 'ACTIVE', '预付租金分期摊销');

-- ---------------------------------------------------------------
-- 1.3 负债类二级科目
-- ---------------------------------------------------------------

-- 2202 应付账款 → 按供应商分类
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('220201', '应付账款-生鲜供应商', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2202'), '贷', 'ACTIVE', '生鲜食材供应商应付款'),
('220202', '应付账款-酒水供应商', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2202'), '贷', 'ACTIVE', '酒水饮料供应商应付款'),
('220203', '应付账款-粮油供应商', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2202'), '贷', 'ACTIVE', '粮油调料供应商应付款'),
('220204', '应付账款-调料供应商', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2202'), '贷', 'ACTIVE', '调味品供应商应付款'),
('220205', '应付账款-设备维修商', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2202'), '贷', 'ACTIVE', '设备维修服务商应付款'),
('220299', '应付账款-其他供应商', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2202'), '贷', 'ACTIVE', '其他供应商应付款');

-- 2211 应付职工薪酬 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('221101', '应付职工薪酬-工资', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2211'), '贷', 'ACTIVE', '应付员工基本工资'),
('221102', '应付职工薪酬-社保企业部分', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2211'), '贷', 'ACTIVE', '企业承担的社会保险费'),
('221103', '应付职工薪酬-公积金企业部分', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2211'), '贷', 'ACTIVE', '企业承担的住房公积金'),
('221104', '应付职工薪酬-奖金津贴', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2211'), '贷', 'ACTIVE', '奖金、加班费等');

-- 2221 应交税费 → 二级（非常重要！含数电票相关）
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('222101', '应交税费-应交增值税-销项税额', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '销售产生的增值税销项税额'),
('222102', '应交税费-应交增值税-进项税额', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '借', 'ACTIVE', '采购产生的增值税进项税额（借方余额）'),
('222103', '应交税费-应交增值税-转出未交增值税', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '转出未交增值税'),
('222104', '应交税费-应交城建税', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '城市维护建设税'),
('222105', '应交税费-应交教育费附加', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '教育费附加'),
('222106', '应交税费-应交地方教育附加', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '地方教育附加'),
('222107', '应交税费-应交个人所得税', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '代扣代缴个人所得税'),
('222108', '应交税费-应交企业所得税', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '企业所得税'),
('222109', '应交税费-应交印花税', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2221'), '贷', 'ACTIVE', '印花税');

-- 2241 其他应付款 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('224101', '其他应付款-代扣社保个人部分', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2241'), '贷', 'ACTIVE', '代扣员工个人社保部分'),
('224102', '其他应付款-代扣公积金个人部分', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2241'), '贷', 'ACTIVE', '代扣员工个人公积金部分'),
('224103', '其他应付款-暂收款项', '负债', '二级科目', (SELECT id FROM accounting_subject WHERE code = '2241'), '贷', 'ACTIVE', '各类暂收款项');

-- ---------------------------------------------------------------
-- 1.4 所有者权益类二级科目
-- ---------------------------------------------------------------

-- 3104 利润分配 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('310401', '利润分配-提取法定盈余公积', '所有者权益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '3104'), '借', 'ACTIVE', '提取法定盈余公积'),
('310402', '利润分配-应付利润/分红', '所有者权益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '3104'), '借', 'ACTIVE', '向投资者分配利润或分红'),
('310403', '利润分配-未分配利润', '所有者权益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '3104'), '贷', 'ACTIVE', '历年累积未分配利润');

-- ---------------------------------------------------------------
-- 1.5 成本类二级科目
-- ---------------------------------------------------------------

-- 4001 生产成本 → 改为更贴合餐饮的命名和二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('400101', '生产成本-直接材料成本', '成本', '二级科目', (SELECT id FROM accounting_subject WHERE code = '4001'), '借', 'ACTIVE', '直接用于菜品制作的食材成本'),
('400102', '生产成本-直接人工成本', '成本', '二级科目', (SELECT id FROM accounting_subject WHERE code = '4001'), '借', 'ACTIVE', '厨师等直接生产人员人工成本'),
('400103', '生产成本-制造费用-分摊', '成本', '二级科目', (SELECT id FROM accounting_subject WHERE code = '4001'), '借', 'ACTIVE', '分摊至产品的制造费用');

-- 4101 制造费用 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('410101', '制造费用-厨房能耗', '成本', '二级科目', (SELECT id FROM accounting_subject WHERE code = '4101'), '借', 'ACTIVE', '厨房水电气燃气等能耗费用'),
('410102', '制造费用-厨房折旧', '成本', '二级科目', (SELECT id FROM accounting_subject WHERE code = '4101'), '借', 'ACTIVE', '厨房设备折旧费用'),
('410103', '制造费用-厨房低耗品摊销', '成本', '二级科目', (SELECT id FROM accounting_subject WHERE code = '4101'), '借', 'ACTIVE', '厨房低值易耗品摊销');

-- ---------------------------------------------------------------
-- 1.6 损益类（收入侧）二级科目
-- ---------------------------------------------------------------

-- 5001 主营业务收入 → 二级（餐饮核心收入分类）
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('500101', '主营业务收入-堂食收入', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '店内堂食销售收入'),
('500102', '主营业务收入-外卖收入-美团', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '美团外卖平台销售收入'),
('500103', '主营业务收入-外卖收入-饿了么', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '饿了么外卖平台销售收入'),
('500104', '主营业务收入-外卖收入-抖音', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '抖音团购外卖销售收入'),
('500105', '主营业务收入-团购核销', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '团购券核销收入'),
('500106', '主营业务收入-包间宴请', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '包间宴请服务收入'),
('500107', '主营业务收入-酒水销售', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '酒水饮料单独销售收入'),
('500199', '主营业务收入-其他收入', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5001'), '贷', 'ACTIVE', '其他主营业务收入');

-- 5301 营业外收入 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('530101', '营业外收入-政府补助', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5301'), '贷', 'ACTIVE', '政府补助收入'),
('530102', '营业外收入-罚没收入', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5301'), '贷', 'ACTIVE', '罚没款项收入'),
('530103', '营业外收入-其他', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '5301'), '贷', 'ACTIVE', '其他营业外收入');

-- ---------------------------------------------------------------
-- 1.7 损益类（成本/费用侧）二级科目
-- ---------------------------------------------------------------

-- 6001 主营业务成本 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('600101', '主营业务成本-食材成本', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6001'), '借', 'ACTIVE', '销售对应的食材成本'),
('600102', '主营业务成本-酒水成本', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6001'), '借', 'ACTIVE', '销售对应的酒水成本'),
('600103', '主营业务成本-包装成本', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6001'), '借', 'ACTIVE', '餐盒包装材料成本'),
('600104', '主营业务成本-外卖包材成本', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6001'), '借', 'ACTIVE', '外卖专用包装材料成本');

-- 6601 销售费用 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('660101', '销售费用-平台佣金-美团', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6601'), '借', 'ACTIVE', '美团平台佣金扣点'),
('660102', '销售费用-平台佣金-饿了么', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6601'), '借', 'ACTIVE', '饿了么平台佣金扣点'),
('660103', '销售费用-推广营销费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6601'), '借', 'ACTIVE', '线上推广、广告投放费用'),
('660104', '销售费用-配送费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6601'), '借', 'ACTIVE', '外卖配送相关费用');

-- 6602 管理费用 → 二级（餐饮业高频使用）
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('660201', '管理费用-房租', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '店铺租赁费用'),
('660202', '管理费用-水电燃气费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '水电燃气费用'),
('660203', '管理费用-员工薪资-管理人员', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '管理人员薪资费用'),
('660204', '管理费用-办公费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '日常办公费用'),
('660205', '管理费用-通讯费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '电话网络通讯费用'),
('660206', '管理费用-清洁卫生费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '清洁卫生消杀费用'),
('660207', '管理费用-维修保养费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '设备维修保养费用'),
('660208', '管理费用-保险费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '财产保险、责任保险等'),
('660209', '管理费用-其他', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6602'), '借', 'ACTIVE', '其他管理费用');

-- 6603 财务费用 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('660301', '财务费用-利息支出', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6603'), '借', 'ACTIVE', '银行贷款利息支出'),
('660302', '财务费用-银行手续费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6603'), '借', 'ACTIVE', '银行转账汇划手续费'),
('660303', '财务费用-汇兑损失', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6603'), '借', 'ACTIVE', '外币兑换汇兑损失'),
('660304', '财务费用-现金提现手续费', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6603'), '借', 'ACTIVE', '支付宝/微信提现手续费');

-- 6711 营业外支出 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('671101', '营业外支出-捐赠支出', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6711'), '借', 'ACTIVE', '对外捐赠支出'),
('671102', '营业外支出-罚款滞纳金', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6711'), '借', 'ACTIVE', '行政罚款及滞纳金'),
('671103', '营业外支出-非常损失', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6711'), '借', 'ACTIVE', '自然灾害等非常损失'),
('671104', '营业外支出-其他', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6711'), '借', 'ACTIVE', '其他营业外支出');

-- 6801 所得税费用 → 二级
INSERT INTO accounting_subject (code, name, category, type, parent_id, balance_direction, status, description) VALUES
('680101', '所得税费用-当期所得税', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6801'), '借', 'ACTIVE', '当期应交所得税费用'),
('680102', '所得税费用-递延所得税', '损益', '二级科目', (SELECT id FROM accounting_subject WHERE code = '6801'), '借', 'ACTIVE', '递延所得税费用');


-- ============================================================================
-- 第二部分：创建自动记账规则引擎表
-- ============================================================================

-- ---------------------------------------------------------------
-- 表1: accounting_rule（会计分录规则表）
-- 核心规则配置表，定义每种业务场景触发的记账行为
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounting_rule (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_code VARCHAR(20) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description VARCHAR(200),
    template_id BIGINT,
    priority INTEGER DEFAULT 100,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    auto_execute SMALLINT DEFAULT 1,
    quality_threshold INTEGER DEFAULT 80,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ar_event_type ON accounting_rule(event_type);
CREATE INDEX IF NOT EXISTS idx_ar_status ON accounting_rule(status);
COMMENT ON TABLE accounting_rule IS '会计分录规则表';

-- ---------------------------------------------------------------
-- 表2: entry_template（分录模板表）
-- 每条规则对应多条分录模板行，定义借贷方向和金额计算方式
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entry_template (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    rule_id BIGINT,
    line_no INTEGER NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount_expression VARCHAR(200),
    summary_template VARCHAR(200) NOT NULL,
    is_required SMALLINT DEFAULT 1,
    condition_expression VARCHAR(200),
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entry_template_rule FOREIGN KEY (rule_id) REFERENCES accounting_rule(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_et_rule_id ON entry_template(rule_id);
CREATE INDEX IF NOT EXISTS idx_et_subject_code ON entry_template(subject_code);
COMMENT ON TABLE entry_template IS '会计分录模板表';
COMMENT ON COLUMN entry_template.id IS '模板ID';
COMMENT ON COLUMN entry_template.template_name IS '模板名称';
COMMENT ON COLUMN entry_template.rule_id IS '关联规则ID';
COMMENT ON COLUMN entry_template.line_no IS '分录行号（同一模板内排序）';
COMMENT ON COLUMN entry_template.subject_code IS '科目编码（对应accounting_subject.code）';
COMMENT ON COLUMN entry_template.direction IS '方向：DEBIT（借方）或CREDIT（贷方）';
COMMENT ON COLUMN entry_template.amount_expression IS '金额表达式（如#{amount}、#{amount}*0.06等）';
COMMENT ON COLUMN entry_template.summary_template IS '摘要模板（支持变量替换，如[自动]POS销售-#{orderNo}）';
COMMENT ON COLUMN entry_template.is_required IS '是否必须生成（0=可选/1=必填）';
COMMENT ON COLUMN entry_template.condition_expression IS '条件表达式（满足条件才生成分录行）';
COMMENT ON COLUMN entry_template.sort_order IS '排序号';

-- ---------------------------------------------------------------
-- 表3: auto_voucher_log（自动记账日志表=审计轨迹）
-- 记录每次自动记账的完整过程，用于审计追溯和问题排查
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auto_voucher_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    source_business_id VARCHAR(50) NOT NULL,
    source_business_no VARCHAR(50),
    rule_id BIGINT,
    voucher_id BIGINT,
    voucher_no VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    quality_score INTEGER,
    error_message TEXT,
    process_time_ms INTEGER,
    operator VARCHAR(50) DEFAULT 'SYSTEM',
    request_payload JSONB,
    response_snapshot JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_avl_event_type ON auto_voucher_log(event_type);
CREATE INDEX IF NOT EXISTS idx_avl_source_id ON auto_voucher_log(source_business_id);
CREATE INDEX IF NOT EXISTS idx_avl_voucher_id ON auto_voucher_log(voucher_id);
CREATE INDEX IF NOT EXISTS idx_avl_status ON auto_voucher_log(status);
CREATE INDEX IF NOT EXISTS idx_avl_create_time ON auto_voucher_log(create_time);
COMMENT ON TABLE auto_voucher_log IS '自动记账日志表';
COMMENT ON COLUMN auto_voucher_log.id IS '日志ID';
COMMENT ON COLUMN auto_voucher_log.event_type IS '事件类型';
COMMENT ON COLUMN auto_voucher_log.source_business_id IS '源业务单据ID';
COMMENT ON COLUMN auto_voucher_log.source_business_no IS '源业务单据编号';
COMMENT ON COLUMN auto_voucher_log.rule_id IS '匹配的规则ID';
COMMENT ON COLUMN auto_voucher_log.voucher_id IS '生成的凭证ID';
COMMENT ON COLUMN auto_voucher_log.voucher_no IS '生成的凭证编号';
COMMENT ON COLUMN auto_voucher_log.status IS '处理状态：SUCCESS/FAILED/PENDING_REVIEW/SKIPPED/DUPLICATE';
COMMENT ON COLUMN auto_voucher_log.quality_score IS '质量评分（0-100）';
COMMENT ON COLUMN auto_voucher_log.error_message IS '错误信息';
COMMENT ON COLUMN auto_voucher_log.process_time_ms IS '处理耗时（毫秒）';
COMMENT ON COLUMN auto_voucher_log.operator IS '操作人';
COMMENT ON COLUMN auto_voucher_log.request_payload IS '原始请求内容JSON';
COMMENT ON COLUMN auto_voucher_log.response_snapshot IS '生成结果快照JSON';

-- ---------------------------------------------------------------
-- 表4: voucher_idempotent（幂等控制表，防止重复入账）
-- 通过业务唯一键确保同一条业务单据不会重复生成凭证
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS voucher_idempotent (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_key VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    source_business_id VARCHAR(50) NOT NULL,
    voucher_id BIGINT,
    first_generate_time TIMESTAMP NOT NULL,
    last_generate_time TIMESTAMP,
    attempt_count INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_vi_business_key ON voucher_idempotent(business_key);
COMMENT ON TABLE voucher_idempotent IS '凭证幂等控制表';
COMMENT ON COLUMN voucher_idempotent.id IS '记录ID';
COMMENT ON COLUMN voucher_idempotent.business_key IS '业务唯一键（事件类型+源单据ID组合）';
COMMENT ON COLUMN voucher_idempotent.event_type IS '事件类型';
COMMENT ON COLUMN voucher_idempotent.source_business_id IS '源业务单据ID';
COMMENT ON COLUMN voucher_idempotent.voucher_id IS '已生成的凭证ID';
COMMENT ON COLUMN voucher_idempotent.first_generate_time IS '首次生成时间';
COMMENT ON COLUMN voucher_idempotent.last_generate_time IS '最后尝试时间';
COMMENT ON COLUMN voucher_idempotent.attempt_count IS '尝试次数';


-- ============================================================================
-- 第三部分：扩展凭证表字段
-- 支持自动记账来源追溯和质量审核
-- ============================================================================

-- 创建voucher_header表（V7.0.0已禁用，此处补充建表）
CREATE TABLE IF NOT EXISTS voucher_header (
    voucher_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_no           VARCHAR(50) NOT NULL UNIQUE,
    voucher_date         DATE NOT NULL,
    voucher_type         VARCHAR(20) DEFAULT 'MANUAL',
    status               VARCHAR(20) DEFAULT 'DRAFT',
    total_debit          BIGINT DEFAULT 0,
    total_credit         BIGINT DEFAULT 0,
    remark               TEXT,
    create_user_id       BIGINT,
    create_username      VARCHAR(50),
    create_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE voucher_header IS '记账凭证主表';
COMMENT ON COLUMN voucher_header.voucher_no IS '凭证编号';
COMMENT ON COLUMN voucher_header.voucher_type IS '凭证类型';
COMMENT ON COLUMN voucher_header.status IS '状态：DRAFT/PENDING/APPROVED/POSTED/VOIDED';

-- ---------------------------------------------------------------
-- 3.1 扩展 voucher_header 表
-- 新增自动记账相关字段
-- ---------------------------------------------------------------
ALTER TABLE voucher_header
ADD COLUMN IF NOT EXISTS source_type VARCHAR(30) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS source_business_id VARCHAR(50) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS source_business_no VARCHAR(50) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS auto_generated SMALLINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS quality_score INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS review_status VARCHAR(20) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS reviewer_id BIGINT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS review_time TIMESTAMP DEFAULT NULL,
ADD COLUMN IF NOT EXISTS review_remark VARCHAR(500) DEFAULT NULL;

COMMENT ON COLUMN voucher_header.source_type IS '来源类型：MANUAL/AUTO_POS/AUTO_PURCHASE/AUTO_SETTLE/AUTO_PERIOD_END';
COMMENT ON COLUMN voucher_header.source_business_id IS '来源业务单据ID';
COMMENT ON COLUMN voucher_header.source_business_no IS '来源业务单据编号';
COMMENT ON COLUMN voucher_header.auto_generated IS '是否自动生成（0=手工/1=自动）';
COMMENT ON COLUMN voucher_header.quality_score IS '质量评分（NULL=未评分）';
COMMENT ON COLUMN voucher_header.review_status IS '审核状态：PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN voucher_header.reviewer_id IS '审核人ID';
COMMENT ON COLUMN voucher_header.review_time IS '审核时间';
COMMENT ON COLUMN voucher_header.review_remark IS '审核备注';

-- 为新增字段添加索引
CREATE INDEX IF NOT EXISTS idx_vh_source ON voucher_header(source_type, source_business_id);
CREATE INDEX IF NOT EXISTS idx_vh_auto ON voucher_header(auto_generated);

-- 创建voucher_line表（V7.0.0已禁用，此处补充建表）
CREATE TABLE IF NOT EXISTS voucher_line (
    line_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_id           BIGINT NOT NULL,
    line_no              INTEGER NOT NULL,
    subject_code         VARCHAR(20),
    subject_name         VARCHAR(100),
    direction            VARCHAR(4) NOT NULL DEFAULT '借',
    amount               BIGINT NOT NULL DEFAULT 0,
    summary              VARCHAR(500),
    create_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vl_voucher FOREIGN KEY (voucher_id) REFERENCES voucher_header(voucher_id)
);

COMMENT ON TABLE voucher_line IS '记账凭证明细行';
COMMENT ON COLUMN voucher_line.voucher_id IS '关联凭证ID';
COMMENT ON COLUMN voucher_line.line_no IS '行号';
COMMENT ON COLUMN voucher_line.subject_code IS '科目编码';
COMMENT ON COLUMN voucher_line.direction IS '方向：借/贷';
COMMENT ON COLUMN voucher_line.amount IS '金额（分）';

-- ---------------------------------------------------------------
-- 3.2 扩展 voucher_line 表
-- 新增辅助核算和溯源字段
-- ---------------------------------------------------------------
ALTER TABLE voucher_line
ADD COLUMN IF NOT EXISTS source_document_no VARCHAR(50) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS auxiliary_vendor VARCHAR(50) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS auxiliary_customer VARCHAR(50) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS auxiliary_account VARCHAR(50) DEFAULT NULL;

COMMENT ON COLUMN voucher_line.source_document_no IS '源单据编号（用于前端显示追溯）';
COMMENT ON COLUMN voucher_line.auxiliary_vendor IS '辅助核算-供应商名称';
COMMENT ON COLUMN voucher_line.auxiliary_customer IS '辅助核算-客户名称';
COMMENT ON COLUMN voucher_line.auxiliary_account IS '辅助核算-结算账户';

-- 为新增字段添加索引
CREATE INDEX IF NOT EXISTS idx_vl_source_doc ON voucher_line(source_document_no);


-- ============================================================================
-- 第四部分：插入18条初始规则数据及分录模板
-- 覆盖个体餐饮企业主要业务场景
-- ============================================================================

-- ---------------------------------------------------------------
-- 规则E01: POS堂食销售 - 现金/扫码收款
-- 触发条件：POS系统完成一笔堂食订单并收到现金/微信/支付宝
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E01', 'POS堂食销售-现金/扫码收款', 'POS_SALE_CASH', 'POS系统堂食订单完成，通过现金、微信、支付宝收款', 10, 'ACTIVE', 1, 90, '堂食销售最常见场景，收银台收款后自动生成收入凭证');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('堂食销售-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E01'), 1, '101201', 'DEBIT', '#{amount}', '[自动]堂食收款-微信-#{orderNo}', 1, 1),
('堂食销售-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E01'), 2, '500101', 'CREDIT', '#{amount}', '[自动]堂食收入-#{orderNo}', 1, 2);

-- ---------------------------------------------------------------
-- 规则E02: 美团外卖销售
-- 触发条件：美团外卖订单完成，货款进入美团账户
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E02', '美团外卖销售', 'MEITUAN_SALE', '美团外卖订单完成，货款暂存美团账户', 11, 'ACTIVE', 1, 85, '美团外卖订单完成后，先挂应收美团，后续结算时再处理');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('美团外卖-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E02'), 1, '112201', 'DEBIT', '#{amount}', '[自动]美团外卖应收-#{orderNo}', 1, 1),
('美团外卖-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E02'), 2, '500102', 'CREDIT', '#{amount}', '[自动]美团外卖收入-#{orderNo}', 1, 2);

-- ---------------------------------------------------------------
-- 规则E03: 饿了么外卖销售
-- 触发条件：饿了么外卖订单完成
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E03', '饿了么外卖销售', 'ELEME_SALE', '饿了么外卖订单完成，货款暂存饿了么账户', 12, 'ACTIVE', 1, 85, '饿了么外卖订单处理逻辑与美团类似');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('饿了么外卖-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E03'), 1, '112202', 'DEBIT', '#{amount}', '[自动]饿了么外卖应收-#{orderNo}', 1, 1),
('饿了么外卖-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E03'), 2, '500103', 'CREDIT', '#{amount}', '[自动]饿了么外卖收入-#{orderNo}', 1, 2);

-- ---------------------------------------------------------------
-- 规则E04: 抖音团购核销
-- 触发条件：顾客到店使用抖音团购券消费
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E04', '抖音团购核销', 'DOUYIN_REDEEM', '顾客到店核销抖音团购券', 13, 'ACTIVE', 1, 85, '团购券核销时确认收入');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('团购核销-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E04'), 1, '112203', 'DEBIT', '#{amount}', '[自动]抖音团购应收-#{couponNo}', 1, 1),
('团购核销-收入确认', (SELECT id FROM accounting_rule WHERE rule_code = 'E04'), 2, '500105', 'CREDIT', '#{amount}', '[自动]团购核销收入-#{couponNo}', 1, 2);

-- ---------------------------------------------------------------
-- 规则E05: 生鲜食材采购入库
-- 触发条件：采购生鲜食材入库验收完成
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E05', '生鲜食材采购入库', 'PURCHASE_FRESH', '采购生鲜食材（肉/菜）入库验收完成', 20, 'ACTIVE', 1, 80, '生鲜采购是餐饮最高频采购场景，含进项税处理');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('生鲜采购-入库', (SELECT id FROM accounting_rule WHERE rule_code = 'E05'), 1, '140301', 'DEBIT', '#{materialCost}', '[自动]生鲜入库-肉类-#{purchaseNo}', 1, 1),
('生鲜采购-入库', (SELECT id FROM accounting_rule WHERE rule_code = 'E05'), 2, '140302', 'DEBIT', '#{vegetableCost}', '[自动]生鲜入库-蔬菜-#{purchaseNo}', 1, 2),
('生鲜采购-进项税', (SELECT id FROM accounting_rule WHERE rule_code = 'E05'), 3, '222102', 'DEBIT', '#{taxAmount}', '[自动]进项税额-#{purchaseNo}', 1, 3),
('生鲜采购-应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E05'), 4, '220201', 'CREDIT', '#{totalAmount}', '[自动]应付生鲜供应商-#{supplierName}-#{purchaseNo}', 1, 4);

-- ---------------------------------------------------------------
-- 规则E06: 调料粮油采购入库
-- 触发条件：采购调料、粮油等非生鲜物资入库
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E06', '调料粮油采购入库', 'PURCHASE_SEASONING', '采购调料、粮油等非生鲜物资入库', 21, 'ACTIVE', 1, 80, '调料粮油采购通常为定期批量采购');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('调料采购-入库', (SELECT id FROM accounting_rule WHERE rule_code = 'E06'), 1, '140303', 'DEBIT', '#{seasoningCost}', '[自动]调料入库-#{purchaseNo}', 1, 1),
('调料采购-入库', (SELECT id FROM accounting_rule WHERE rule_code = 'E06'), 2, '140304', 'DEBIT', '#{grainOilCost}', '[自动]粮油入库-#{purchaseNo}', 1, 2),
('调料采购-进项税', (SELECT id FROM accounting_rule WHERE rule_code = 'E06'), 3, '222102', 'DEBIT', '#{taxAmount}', '[自动]进项税额-#{purchaseNo}', 1, 3),
('调料采购-应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E06'), 4, '220204', 'CREDIT', '#{totalAmount}', '[自动]应付调料供应商-#{supplierName}-#{purchaseNo}', 1, 4);

-- ---------------------------------------------------------------
-- 规则E07: 酒水饮料采购入库
-- 触发条件：采购酒水饮料入库
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E07', '酒水饮料采购入库', 'PURCHASE_BEVERAGE', '采购酒水饮料入库验收完成', 22, 'ACTIVE', 1, 80, '酒水饮料采购通常有独立供应商');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('酒水采购-入库', (SELECT id FROM accounting_rule WHERE rule_code = 'E07'), 1, '140305', 'DEBIT', '#{amount}', '[自动]酒水饮料入库-#{purchaseNo}', 1, 1),
('酒水采购-进项税', (SELECT id FROM accounting_rule WHERE rule_code = 'E07'), 2, '222102', 'DEBIT', '#{taxAmount}', '[自动]进项税额-#{purchaseNo}', 1, 2),
('酒水采购-应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E07'), 3, '220202', 'CREDIT', '#{totalAmount}', '[自动]应付酒水供应商-#{supplierName}-#{purchaseNo}', 1, 3);

-- ---------------------------------------------------------------
-- 规则E08: 支付供应商货款
-- 触发条件：通过银行转账/微信/支付宝支付供应商货款
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E08', '支付供应商货款', 'PAYMENT_SUPPLIER', '通过银行/微信/支付宝支付供应商货款', 30, 'ACTIVE', 1, 85, '供应商付款后冲减应付账款');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('付供应商款-冲应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E08'), 1, '220201', 'DEBIT', '#{amount}', '[自动]支付供应商货款-#{supplierName}-#{paymentNo}', 1, 1),
('付供应商款-银行', (SELECT id FROM accounting_rule WHERE rule_code = 'E08'), 2, '100201', 'CREDIT', '#{amount}', '[自动]银行付款-#{paymentNo}', 1, 2);

-- ---------------------------------------------------------------
-- 规则E09: 支付员工工资
-- 触发条件：发放员工工资（通过银行代发）
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E09', '支付员工工资', 'PAYMENT_SALARY', '通过银行代发员工工资', 31, 'ACTIVE', 1, 85, '工资发放涉及多个科目：工资、社保、个税代扣');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('发工资-冲应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E09'), 1, '221101', 'DEBIT', '#{netSalary}', '[自动]发放工资-#{period}', 1, 1),
('发工资-代扣社保', (SELECT id FROM accounting_rule WHERE rule_code = 'E09'), 2, '224101', 'DEBIT', '#{socialPersonal}', '[自动]代扣社保个人部分-#{period}', 1, 2),
('发工资-代扣公积金', (SELECT id FROM accounting_rule WHERE rule_code = 'E09'), 3, '224102', 'DEBIT', '#{fundPersonal}', '[自动]代扣公积金个人部分-#{period}', 1, 3),
('发工资-代扣个税', (SELECT id FROM accounting_rule WHERE rule_code = 'E09'), 4, '222107', 'DEBIT', '#{personalTax}', '[自动]代扣个人所得税-#{period}', 1, 4),
('发工资-银行', (SELECT id FROM accounting_rule WHERE rule_code = 'E09'), 5, '100201', 'CREDIT', '#{netSalary}', '[自动]银行代发工资-#{period}', 1, 5);

-- ---------------------------------------------------------------
-- 规则E10: 计提本月工资及社保公积金
-- 触发条件：月末计提当月工资及企业承担的社保公积金
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E10', '计提本月工资及社保公积金', 'ACCRUE_SALARY', '月末计提当月工资及企业承担的社保公积金', 40, 'ACTIVE', 1, 75, '月末结账前必须完成的计提操作，区分管理人员和生产人员');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('计提工资-管理', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 1, '660203', 'DEBIT', '#{adminSalary}', '[自动]计提管理人员工资-#{period}', 1, 1),
('计提工资-生产', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 2, '400102', 'DEBIT', '#{kitchenSalary}', '[自动]计提厨房人员工资-#{period}', 1, 2),
('计提工资-应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 3, '221101', 'CREDIT', '#{totalSalary}', '[自动]应付职工薪酬-工资-#{period}', 1, 3),
('计提社保-管理', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 4, '660203', 'DEBIT', '#{adminSocial}', '[自动]计提管理人员社保-#{period}', 1, 4),
('计提社保-生产', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 5, '400102', 'DEBIT', '#{kitchenSocial}', '[自动]计提厨房人员社保-#{period}', 1, 5),
('计提社保-应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 6, '221102', 'CREDIT', '#{totalSocial}', '[自动]应付职工薪酬-社保企业部分-#{period}', 1, 6),
('计提公积金-管理', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 7, '660203', 'DEBIT', '#{adminFund}', '[自动]计提管理人员公积金-#{period}', 1, 7),
('计提公积金-生产', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 8, '400102', 'DEBIT', '#{kitchenFund}', '[自动]计提厨房人员公积金-#{period}', 1, 8),
('计提公积金-应付', (SELECT id FROM accounting_rule WHERE rule_code = 'E10'), 9, '221103', 'CREDIT', '#{totalFund}', '[自动]应付职工薪酬-公积金企业部分-#{period}', 1, 9);

-- ---------------------------------------------------------------
-- 规则E11: 美团/饿了么平台结算到账
-- 触发条件：外卖平台将货款结算至商家银行卡
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E11', '美团/饿了么平台结算到账', 'PLATFORM_SETTLE', '外卖平台结算货款至商家银行账户', 32, 'ACTIVE', 1, 85, '平台结算时扣除佣金后实际到账金额');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('平台结算-银行入账', (SELECT id FROM accounting_rule WHERE rule_code = 'E11'), 1, '100201', 'DEBIT', '#{settleAmount}', '[自动]平台结算入账-#{platform}-#{settleDate}', 1, 1),
('平台结算-佣金', (SELECT id FROM accounting_rule WHERE rule_code = 'E11'), 2, '660101', 'DEBIT', '#{commission}', '[自动]平台佣金-#{platform}-#{settleDate}', 1, 2),
('平台结算-冲应收', (SELECT id FROM accounting_rule WHERE rule_code = 'E11'), 3, '112201', 'CREDIT', '#{originalAmount}', '[自动]冲减应收-#{platform}-#{settleDate}', 1, 3);

-- ---------------------------------------------------------------
-- 规则E12: 销售成本结转（按日/月）
-- 触发条件：每日/月末根据销售出库记录结转销售成本
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E12', '销售成本结转', 'COST_TRANSFER', '根据销售出库记录结转主营业务成本', 41, 'ACTIVE', 1, 70, '成本结转是餐饮核算核心，需要准确匹配销售与消耗');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('成本结转-食材', (SELECT id FROM accounting_rule WHERE rule_code = 'E12'), 1, '600101', 'DEBIT', '#{foodCost}', '[自动]结转食材成本-#{dateRange}', 1, 1),
('成本结转-酒水', (SELECT id FROM accounting_rule WHERE rule_code = 'E12'), 2, '600102', 'DEBIT', '#{beverageCost}', '[自动]结转酒水成本-#{dateRange}', 1, 2),
('成本结转-包装', (SELECT id FROM accounting_rule WHERE rule_code = 'E12'), 3, '600103', 'DEBIT', '#{packageCost}', '[自动]结转包装成本-#{dateRange}', 1, 3),
('成本结转-原材料', (SELECT id FROM accounting_rule WHERE rule_code = 'E12'), 4, '140301', 'CREDIT', '#{meatCost}', '[自动]冲减原材料-肉类-#{dateRange}', 1, 4),
('成本结转-原材料', (SELECT id FROM accounting_rule WHERE rule_code = 'E12'), 5, '140302', 'CREDIT', '#{vegetableCost}', '[自动]冲减原材料-蔬菜-#{dateRange}', 1, 5),
('成本结转-原材料', (SELECT id FROM accounting_rule WHERE rule_code = 'E12'), 6, '140305', 'CREDIT', '#{beverageMaterialCost}', '[自动]冲减原材料-酒水饮料-#{dateRange}', 1, 6);

-- ---------------------------------------------------------------
-- 规则E13: 支付房租
-- 触发条件：支付店铺租金（可按月/季/年）
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E13', '支付房租', 'PAYMENT_RENT', '支付店铺租金（预付或当期）', 33, 'ACTIVE', 1, 85, '房租是餐饮最大固定支出之一，注意区分预付和当期');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('付房租-费用化', (SELECT id FROM accounting_rule WHERE rule_code = 'E13'), 1, '660201', 'DEBIT', '#{currentRent}', '[自动]支付房租-#{period}', 1, 1),
('付房租-预付', (SELECT id FROM accounting_rule WHERE rule_code = 'E13'), 2, '180102', 'DEBIT', '#{prepaidRent}', '[自动]预付房租-#{period}', 1, 2),
('付房租-银行', (SELECT id FROM accounting_rule WHERE rule_code = 'E13'), 3, '100201', 'CREDIT', '#{totalAmount}', '[自动]银行付房租-#{period}', 1, 3);

-- ---------------------------------------------------------------
-- 规则E14: 支付水电燃气费
-- 触发条件：支付水费、电费、燃气费
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E14', '支付水电燃气费', 'PAYMENT_UTILITY', '支付水费、电费、燃气费', 34, 'ACTIVE', 1, 85, '水电燃气费通常按月缴纳，区分厨房和管理区域');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('付水电-厨房', (SELECT id FROM accounting_rule WHERE rule_code = 'E14'), 1, '410101', 'DEBIT', '#{kitchenUtility}', '[自动]支付厨房水电燃气-#{period}', 1, 1),
('付水电-管理', (SELECT id FROM accounting_rule WHERE rule_code = 'E14'), 2, '660202', 'DEBIT', '#{officeUtility}', '[自动]支付管理区水电燃气-#{period}', 1, 2),
('付水电-银行', (SELECT id FROM accounting_rule WHERE rule_code = 'E14'), 3, '100201', 'CREDIT', '#{totalAmount}', '[自动]银行付水电燃气-#{period}', 1, 3);

-- ---------------------------------------------------------------
-- 规则E15: 收取定金/预收款
-- 触发条件：收取包间宴请定金或预收款
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E15', '收取定金/预收款', 'RECEIVE_DEPOSIT', '收取包间宴请定金或预收款', 35, 'ACTIVE', 1, 85, '预收款需在合同负债中体现，消费时转入收入');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('收定金-入账', (SELECT id FROM accounting_rule WHERE rule_code = 'E15'), 1, '101201', 'DEBIT', '#{amount}', '[自动]收取定金-微信-#{customerName}', 1, 1),
('收定金-合同负债', (SELECT id FROM accounting_rule WHERE rule_code = 'E15'), 2, '224103', 'CREDIT', '#{amount}', '[自动]预收定金-#{customerName}', 1, 2);

-- ---------------------------------------------------------------
-- 规则E16: 固定资产购置
-- 触发条件：购买厨房设备、电子设备、家具等固定资产
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E16', '固定资产购置', 'ASSET_PURCHASE', '购买厨房设备、电子设备、家具等固定资产', 25, 'ACTIVE', 0, 70, '固定资产购置涉及金额较大，建议人工审核确认');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('购固定资产-原值', (SELECT id FROM accounting_rule WHERE rule_code = 'E16'), 1, '160101', 'DEBIT', '#{assetValue}', '[自动]购入厨房设备-#{assetName}', 1, 1),
('购固定资产-进项税', (SELECT id FROM accounting_rule WHERE rule_code = 'E16'), 2, '222102', 'DEBIT', '#{taxAmount}', '[自动]固定资产进项税-#{assetName}', 1, 2),
('购固定资产-付款', (SELECT id FROM accounting_rule WHERE rule_code = 'E16'), 3, '100201', 'CREDIT', '#{totalAmount}', '[自动]银行付设备款-#{assetName}', 1, 3);

-- ---------------------------------------------------------------
-- 规则E17: 月末计提折旧
-- 触发条件：月末对固定资产计提折旧
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E17', '月末计提折旧', 'MONTHLY_DEPRECIATION', '月末对固定资产计提折旧', 42, 'ACTIVE', 1, 75, '按资产类别分别计提折旧，区分厨房设备、电子设备、家具');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('折旧-厨房设备', (SELECT id FROM accounting_rule WHERE rule_code = 'E17'), 1, '410102', 'DEBIT', '#{kitchenDepreciation}', '[自动]计提厨房设备折旧-#{period}', 1, 1),
('折旧-电子设备', (SELECT id FROM accounting_rule WHERE rule_code = 'E17'), 2, '660209', 'DEBIT', '#{electronicsDepreciation}', '[自动]计提电子设备折旧-#{period}', 1, 2),
('折旧-家具', (SELECT id FROM accounting_rule WHERE rule_code = 'E17'), 3, '660209', 'DEBIT', '#{furnitureDepreciation}', '[自动]计提家具折旧-#{period}', 1, 3),
('折旧-累计折旧', (SELECT id FROM accounting_rule WHERE rule_code = 'E17'), 4, '160201', 'CREDIT', '#{totalDepreciation}', '[自动]累计折旧-厨房设备-#{period}', 1, 4);

-- ---------------------------------------------------------------
-- 规则E18: 月末税费计提
-- 触发条件：月末计提各项税费（增值税、附加税、所得税等）
-- ---------------------------------------------------------------
INSERT INTO accounting_rule (rule_code, rule_name, event_type, event_description, priority, status, auto_execute, quality_threshold, description) VALUES
('E18', '月末税费计提', 'MONTHLY_TAX_ACCRUAL', '月末计提增值税、城建税、教育费附加、所得税等', 43, 'ACTIVE', 1, 70, '月末结账前必须完成的税务计提，确保账务与纳税申报一致');

INSERT INTO entry_template (template_name, rule_id, line_no, subject_code, direction, amount_expression, summary_template, is_required, sort_order) VALUES
('税费计提-转出未交', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 1, '222103', 'DEBIT', '#{unpaidVat}', '[自动]转出未交增值税-#{period}', 1, 1),
('税费计提-销项', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 2, '222101', 'CREDIT', '#{outputVat}', '[自动]销项税额-#{period}', 1, 2),
('税费计提-进项', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 3, '222102', 'CREDIT', '#{inputVat}', '[自动]进项税额-#{period}', 1, 3),
('税费计提-城建税', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 4, '660209', 'DEBIT', '#{cityTax}', '[自动]计提城建税-#{period}', 1, 4),
('税费计提-城建税', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 5, '222104', 'CREDIT', '#{cityTax}', '[自动]应交城建税-#{period}', 1, 5),
('税费计提-教育费附加', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 6, '660209', 'DEBIT', '#{educationTax}', '[自动]计提教育费附加-#{period}', 1, 6),
('税费计提-教育费附加', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 7, '222105', 'CREDIT', '#{educationTax}', '[自动]应交教育费附加-#{period}', 1, 7),
('税费计提-地方教育附加', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 8, '660209', 'DEBIT', '#{localEducationTax}', '[自动]计提地方教育附加-#{period}', 1, 8),
('税费计提-地方教育附加', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 9, '222106', 'CREDIT', '#{localEducationTax}', '[自动]应交地方教育附加-#{period}', 1, 9),
('税费计提-所得税', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 10, '680101', 'DEBIT', '#{incomeTax}', '[自动]计提企业所得税-#{period}', 1, 10),
('税费计提-所得税', (SELECT id FROM accounting_rule WHERE rule_code = 'E18'), 11, '222108', 'CREDIT', '#{incomeTax}', '[自动]应交企业所得税-#{period}', 1, 11);


-- ========================================
-- 迁移完成统计：
-- 一级科目新增：3个（1012其他货币资金、1411低值易耗品、1801长期待摊费用）
-- 二级科目新增：约95个（覆盖资产/负债/权益/成本/损益全部类别）
-- 新建表：4张（accounting_rule、entry_template、auto_voucher_log、voucher_idempotent）
-- 凭证表扩展：voucher_header增加9个字段，voucher_line增加4个字段
-- 初始规则：18条（E01-E18），配套分录模板约65行
-- ========================================
