-- ============================================================
-- 托盘 5 状态机改造（idle → bound → making → ready → served）
-- 1. 扩展 tray 表：增加状态机/防抖/摄像头/YOLO 字段
-- 2. 扩展 kitchen_order 表：增加 bound/making/ready/served 时间戳
-- 3. 新建 tray_scan_record 表（托盘扫码全量记录 + 防抖判断）
-- ============================================================

-- ---------- 1. 扩展 tray 表 ----------
ALTER TABLE tray
    ADD COLUMN IF NOT EXISTS last_state_change_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_scan_device_id BIGINT,
    ADD COLUMN IF NOT EXISTS last_scan_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS camera_snapshot_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS camera_snapshot_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS yolo_verified SMALLINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS yolo_verify_time TIMESTAMP;

COMMENT ON COLUMN tray.status IS '5状态机：idle-空闲, bound-POS已绑定, making-后厨制作中, ready-后厨已出餐, served-已取餐, cleaning-清洁中, damaged-损坏';
COMMENT ON COLUMN tray.last_state_change_time IS '最近状态变更时间（用于防抖判断）';
COMMENT ON COLUMN tray.last_scan_device_id IS '最近扫码设备ID';
COMMENT ON COLUMN tray.last_scan_time IS '最近扫码时间（用于防抖判断）';
COMMENT ON COLUMN tray.camera_snapshot_url IS '最近摄像头拍照URL';
COMMENT ON COLUMN tray.camera_snapshot_time IS '最近摄像头拍照时间';
COMMENT ON COLUMN tray.yolo_verified IS 'YOLO识别结果：0-未识别, 1-有餐, 2-无餐';
COMMENT ON COLUMN tray.yolo_verify_time IS 'YOLO识别时间';

-- 兼容旧状态值：将 in_use 数据迁移到 bound
UPDATE tray SET status = 'bound' WHERE status = 'in_use';

-- ---------- 2. 扩展 kitchen_order 表 ----------
ALTER TABLE kitchen_order
    ADD COLUMN IF NOT EXISTS bound_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS making_start_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ready_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS served_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_camera_snapshot_url VARCHAR(500);

COMMENT ON COLUMN kitchen_order.bound_time IS '托盘绑定时间（POS绑定阶段）';
COMMENT ON COLUMN kitchen_order.making_start_time IS '后厨开始制作时间（后厨一次扫码）';
COMMENT ON COLUMN kitchen_order.ready_time IS '后厨出餐时间（后厨二次扫码）';
COMMENT ON COLUMN kitchen_order.served_time IS '顾客取餐时间（取餐口确认）';
COMMENT ON COLUMN kitchen_order.last_camera_snapshot_url IS '最新摄像头拍照URL';

-- 将旧状态 cooking 迁移到 making（保持向后兼容）
UPDATE kitchen_order SET status = 'making' WHERE status = 'cooking';
UPDATE kitchen_order SET status = 'ready' WHERE status = 'completed';

-- ---------- 3. 新建 tray_scan_record 表 ----------
CREATE TABLE IF NOT EXISTS tray_scan_record (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tray_id                BIGINT       NOT NULL,
    tray_code              VARCHAR(50) NOT NULL,
    kitchen_order_id       VARCHAR(50),
    scan_device_id         BIGINT,
    scan_device_code       VARCHAR(50),
    scan_type              VARCHAR(20) NOT NULL,
    from_status            VARCHAR(20),
    to_status              VARCHAR(20),
    scan_time              TIMESTAMP    NOT NULL,
    prev_scan_time         TIMESTAMP,
    is_debounced          SMALLINT     DEFAULT 0,
    camera_snapshot_url    VARCHAR(500),
    yolo_result            VARCHAR(100),
    yolo_confidence        DECIMAL(5,4),
    operator_id            BIGINT,
    operator_name          VARCHAR(50),
    store_id               BIGINT,
    remark                 VARCHAR(500),
    create_time            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted                SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tray_scan_record_tray_id        ON tray_scan_record (tray_id);
CREATE INDEX IF NOT EXISTS idx_tray_scan_record_scan_time      ON tray_scan_record (scan_time);
CREATE INDEX IF NOT EXISTS idx_tray_scan_record_kitchen_order  ON tray_scan_record (kitchen_order_id);
CREATE INDEX IF NOT EXISTS idx_tray_scan_record_scan_type      ON tray_scan_record (scan_type);
CREATE INDEX IF NOT EXISTS idx_tray_scan_record_store          ON tray_scan_record (store_id);

COMMENT ON TABLE tray_scan_record IS '托盘扫码记录表（每次扫码全量留痕）';
COMMENT ON COLUMN tray_scan_record.scan_type IS '扫码类型：KITCHEN_IN-后厨一次扫码(开始制作), KITCHEN_OUT-后厨二次扫码(出餐), SERVE-取餐口确认';
COMMENT ON COLUMN tray_scan_record.from_status IS '扫码前托盘状态';
COMMENT ON COLUMN tray_scan_record.to_status IS '扫码后托盘状态';
COMMENT ON COLUMN tray_scan_record.prev_scan_time IS '上一次同类型扫码时间（用于防抖判断）';
COMMENT ON COLUMN tray_scan_record.is_debounced IS '本次扫码是否被防抖忽略：0-正常处理, 1-被防抖忽略';
COMMENT ON COLUMN tray_scan_record.camera_snapshot_url IS '摄像头拍照URL（出餐时联动）';
COMMENT ON COLUMN tray_scan_record.yolo_result IS 'YOLO识别结果（预留）：has_food-有餐, no_food-无餐, unknown-不确定';
COMMENT ON COLUMN tray_scan_record.yolo_confidence IS 'YOLO识别置信度（0-1）';
