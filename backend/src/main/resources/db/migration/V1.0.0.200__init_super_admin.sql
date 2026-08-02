-- 餐饮管理系统 - 数据库初始化脚本（PostgreSQL兼容）
-- 创建超级管理员和邀请码相关表结构
-- 执行时机：系统首次部署时

-- ============================================
-- 1. 创建超级管理员角色（如果不存在）
-- ============================================
INSERT INTO roles (role_id, role_id_str, role_code, role_name, description, status, created_at, updated_at)
OVERRIDING SYSTEM VALUE
VALUES (1, 'ROLE_SUPER_ADMIN', 'SUPER_ADMIN', '超级管理员', '系统超级管理员，拥有所有权限，可以创建其他管理员和店长', 'active', NOW(), NOW())
ON CONFLICT (role_id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    updated_at = NOW();

-- ============================================
-- 2. 创建默认超级管理员用户
-- 初始密码：Admin@123（首次登录必须修改）
-- ============================================
INSERT INTO users (
    user_id,
    username,
    password,
    name,
    email,
    phone,
    status,
    is_locked,
    password_error_count,
    need_change_password,
    last_password_change_time,
    created_at,
    updated_at
) OVERRIDING SYSTEM VALUE VALUES (
    1,
    'admin',
    -- BCrypt加密后的 'Admin@123'，强度12轮
    '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW',
    '系统管理员',
    'admin@company.com',
    '13800000000',
    '1',  -- 启用状态
    0,  -- 未锁定
    0,  -- 密码错误次数为0
    1,  -- 需要修改密码（首次登录强制修改）
    NOW(),  -- 上次密码修改时间
    NOW(),
    NOW()
) ON CONFLICT (user_id) DO UPDATE SET
    name = EXCLUDED.name,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    need_change_password = 1,  -- 重置为需要修改密码
    updated_at = NOW();

-- ============================================
-- 3. 关联超级管理员角色
-- ============================================
INSERT INTO user_roles (id, user_id, role_id, created_at)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 1, NOW())
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    role_id = EXCLUDED.role_id;

-- ============================================
-- 4. 创建邀请码表
-- ============================================
CREATE TABLE IF NOT EXISTS invitation_codes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    inviter_id BIGINT NOT NULL,
    invitee_email VARCHAR(100),
    invitee_phone VARCHAR(20),
    role_id VARCHAR(50),
    department_id VARCHAR(50),
    store_id VARCHAR(50),
    status SMALLINT DEFAULT 1,
    expire_time TIMESTAMP NOT NULL,
    used_time TIMESTAMP,
    used_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invitation_inviter FOREIGN KEY (inviter_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_used_by FOREIGN KEY (used_by) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_invitation_codes_code         ON invitation_codes (code);
CREATE INDEX IF NOT EXISTS idx_invitation_codes_inviter_id   ON invitation_codes (inviter_id);
CREATE INDEX IF NOT EXISTS idx_invitation_codes_status       ON invitation_codes (status);
CREATE INDEX IF NOT EXISTS idx_invitation_codes_expire_time  ON invitation_codes (expire_time);

COMMENT ON TABLE  invitation_codes IS '邀请码表';
COMMENT ON COLUMN invitation_codes.id IS '邀请码ID';
COMMENT ON COLUMN invitation_codes.code IS '邀请码（加密随机字符串）';
COMMENT ON COLUMN invitation_codes.inviter_id IS '邀请人ID（创建者）';
COMMENT ON COLUMN invitation_codes.invitee_email IS '被邀请人邮箱（可选，预留）';
COMMENT ON COLUMN invitation_codes.invitee_phone IS '被邀请人手机号（可选，预留）';
COMMENT ON COLUMN invitation_codes.role_id IS '预设角色ID';
COMMENT ON COLUMN invitation_codes.department_id IS '预设部门ID';
COMMENT ON COLUMN invitation_codes.store_id IS '预设门店ID';
COMMENT ON COLUMN invitation_codes.status IS '状态：0-已使用，1-未使用，2-已过期';
COMMENT ON COLUMN invitation_codes.expire_time IS '过期时间';
COMMENT ON COLUMN invitation_codes.used_time IS '使用时间';
COMMENT ON COLUMN invitation_codes.used_by IS '使用者用户ID';
COMMENT ON COLUMN invitation_codes.created_at IS '创建时间';
COMMENT ON COLUMN invitation_codes.updated_at IS '更新时间';

-- ============================================
-- 5. 创建密码历史表（用于检查不能重复使用最近5次密码）
-- ============================================
CREATE TABLE IF NOT EXISTS password_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_history_user_id    ON password_history (user_id);
CREATE INDEX IF NOT EXISTS idx_password_history_created_at ON password_history (created_at);

COMMENT ON TABLE  password_history IS '密码历史表';
COMMENT ON COLUMN password_history.id IS 'ID';
COMMENT ON COLUMN password_history.user_id IS '用户ID';
COMMENT ON COLUMN password_history.password_hash IS '密码哈希（BCrypt）';
COMMENT ON COLUMN password_history.created_at IS '创建时间';

-- ============================================
-- 6. 记录初始化日志
-- ============================================
INSERT INTO login_log (
    user_id,
    username,
    login_time,
    login_ip,
    login_status,
    login_msg
) VALUES (
    '1',
    'admin',
    NOW(),
    '127.0.0.1',
    'success',
    '系统初始化完成，超级管理员账号已创建'
);

-- ============================================
-- 初始化完成提示
-- ============================================
-- 默认账号信息：
-- 用户名：admin
-- 初始密码：Admin@123
-- 首次登录必须修改密码
-- ============================================
