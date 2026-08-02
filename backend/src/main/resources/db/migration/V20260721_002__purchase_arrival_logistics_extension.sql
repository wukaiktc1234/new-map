-- ============================================================
-- V20260721_002: 扩展采购到货单物流信息字段
--
-- 变更内容：
--   1. 增加物流公司、运输方式、车牌号、车辆类型、司机信息、运费字段
-- ============================================================

ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(100);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS transport_mode VARCHAR(20);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS vehicle_plate_no VARCHAR(20);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS vehicle_type VARCHAR(30);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS driver_name VARCHAR(50);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS driver_phone VARCHAR(30);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS freight_amount BIGINT DEFAULT 0;

COMMENT ON COLUMN purchase_arrivals.logistics_company IS '物流公司名称';
COMMENT ON COLUMN purchase_arrivals.transport_mode IS '运输方式：land-陆运 / air-空运 / sea-海运 / express-快递 / self-自提';
COMMENT ON COLUMN purchase_arrivals.vehicle_plate_no IS '运输车辆车牌号';
COMMENT ON COLUMN purchase_arrivals.vehicle_type IS '车辆类型：van-厢式货车 / refrigerated-冷藏车 / flatbed-平板车 / minivan-面包车 / other-其他';
COMMENT ON COLUMN purchase_arrivals.driver_name IS '司机姓名';
COMMENT ON COLUMN purchase_arrivals.driver_phone IS '司机联系电话';
COMMENT ON COLUMN purchase_arrivals.freight_amount IS '运费金额（分）';
