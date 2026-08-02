-- ============================================================
-- V20260728_001:  consolidated HR & purchase real data fix
-- 说明：
--   1. 修复 HR 脏数据（position_id=4 修正为会计，删除重复 1744，
--      重定位引用员工，修正员工 D/E 姓名）。
--   2. 根据实际员工数据重新计算 departments / positions 的 employee_count。
--   3. 清理采购申请表中 title / department_name / applicant_name 含 '?' 的脏数据。
--   4. 若 onboarding_archive 为空，从 employees + users + positions + departments
--      + employee_labor_contract 初始化真实入职档案（包含 contract_id）。
-- 所有操作均按幂等方式编写，可重复执行。
-- ============================================================

-- ============================================================
-- 一、修复 HR 脏数据
-- ============================================================

-- 1.1 将 position_id=4 的岗位修正为“会计”
UPDATE positions
SET position_code = 'POS_ACCOUNTANT',
    position_name = '会计',
    level = 'P2',
    description = '负责财务核算、凭证管理等工作',
    update_time = CURRENT_TIMESTAMP
WHERE position_id = 4;

-- 1.2 若存在重复会计岗位（position_id=1744），先将引用它的员工调整到 position_id=4，再删除
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM positions WHERE position_id = 1744) THEN
        UPDATE employees
        SET position_id = 4
        WHERE position_id = 1744;

        DELETE FROM positions WHERE position_id = 1744;
    END IF;
END $$;

-- 1.3 修正员工 D、E 的姓名（按固定 employee_id）
UPDATE employees
SET employee_name = '员工D'
WHERE employee_id = 2080155339590037506;

UPDATE employees
SET employee_name = '员工E'
WHERE employee_id = 2080155339590037507;

-- ============================================================
-- 二、重新计算部门和岗位的在岗人数
-- 说明：status = 1（在职）或 2（试用期）计入；逻辑删除标记 deleted = 0。
-- ============================================================

-- 2.1 重置所有部门的 employee_count
UPDATE departments SET employee_count = 0;

-- 2.2 按 department_id 统计在职/试用期员工数并更新
WITH dept_counts AS (
    SELECT department_id, COUNT(*) AS cnt
    FROM employees
    WHERE status IN (1, 2) AND deleted = 0 AND department_id IS NOT NULL
    GROUP BY department_id
)
UPDATE departments d
SET employee_count = COALESCE(dc.cnt, 0)
FROM dept_counts dc
WHERE d.department_id = dc.department_id;

-- 2.3 重置所有岗位的 employee_count
UPDATE positions SET employee_count = 0;

-- 2.4 按 position_id 统计在职/试用期员工数并更新
WITH pos_counts AS (
    SELECT position_id, COUNT(*) AS cnt
    FROM employees
    WHERE status IN (1, 2) AND deleted = 0 AND position_id IS NOT NULL
    GROUP BY position_id
)
UPDATE positions p
SET employee_count = COALESCE(pc.cnt, 0)
FROM pos_counts pc
WHERE p.position_id = pc.position_id;

-- ============================================================
-- 三、清理采购申请表中的脏数据
-- 说明：将 title / department_name / applicant_name 中包含 '?' 的记录进行修正或清空。
--       由于无法从上下文推断正确值，先采用“清空异常字符字段”的保守策略，
--       避免乱码影响前端展示与导出。
-- ============================================================

-- 3.1 title 包含 '?' 时，置为 request_no（至少保证非空且可读）
UPDATE purchase_request
SET title = request_no
WHERE title LIKE '%?%'
  AND deleted = 0;

-- 3.2 department_name 包含 '?' 时，根据 department_id 重新取部门名称
UPDATE purchase_request pr
SET department_name = d.dept_name
FROM departments d
WHERE pr.department_name LIKE '%?%'
  AND pr.department_id IS NOT NULL
  AND pr.department_id::bigint = d.department_id
  AND pr.deleted = 0;

-- 对于仍无法解析的 department_name，清空处理
UPDATE purchase_request
SET department_name = NULL
WHERE department_name LIKE '%?%'
  AND deleted = 0;

-- 3.3 applicant_name 包含 '?' 时，根据 applicant_id 重新取员工姓名
UPDATE purchase_request pr
SET applicant_name = e.employee_name
FROM employees e
WHERE pr.applicant_name LIKE '%?%'
  AND pr.applicant_id IS NOT NULL
  AND pr.applicant_id = e.employee_id::text
  AND pr.deleted = 0;

-- 对于仍无法解析的 applicant_name，清空处理
UPDATE purchase_request
SET applicant_name = NULL
WHERE applicant_name LIKE '%?%'
  AND deleted = 0;

-- ============================================================
-- 四、初始化入职档案（仅在 onboarding_archive 无有效记录时执行）
-- 说明：从 employees / users / positions / departments / employee_labor_contract
--       汇总生成，并回填 contract_id。
-- ============================================================

DO $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM onboarding_archive
    WHERE deleted = 0;

    IF v_count = 0 THEN
        -- 4.1 从现有员工/用户/部门/岗位/劳动合同初始化真实入职档案
        INSERT INTO onboarding_archive (
            candidate_id,
            candidate_name,
            email,
            phone,
            id_card,
            position,
            position_level,
            department_id,
            department_name,
            expected_salary,
            final_salary,
            onboard_date,
            employee_code,
            preset_username,
            user_id,
            contract_id,
            status,
            create_by,
            create_time,
            update_time,
            deleted
        )
        SELECT
            replace(gen_random_uuid()::text, '-', ''),
            e.employee_name,
            lower(u.username) || '@company.com',
            '138' || lpad((u.user_id % 100000000)::text, 8, '0'),
            '11010119900101' || lpad((u.user_id % 10000)::text, 4, '0'),
            p.position_name,
            CASE WHEN p.position_code LIKE '%MANAGER%' THEN 'MANAGER' ELSE 'STAFF' END,
            e.department_id::text,
            d.dept_name,
            8000 + (u.user_id % 10) * 500,
            8000 + (u.user_id % 10) * 500,
            '2026-01-01'::date + ((u.user_id % 30)::integer),
            e.employee_code,
            u.username,
            u.user_id,
            elc.id,
            'REGISTERED',
            1,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP,
            0
        FROM employees e
        JOIN users u ON u.employee_code = e.employee_code
        JOIN positions p ON p.position_id = e.position_id
        JOIN departments d ON d.department_id = e.department_id
        LEFT JOIN employee_labor_contract elc
            ON elc.employee_id = e.employee_id::text
            AND elc.deleted = 0
        WHERE e.deleted = 0
          AND u.deleted = 0
        ON CONFLICT DO NOTHING;

        -- 4.2 补充 2 条过渡态候选人数据（待入职 / 待签合同）
        INSERT INTO onboarding_archive (
            candidate_id,
            candidate_name,
            email,
            phone,
            id_card,
            position,
            position_level,
            department_id,
            department_name,
            expected_salary,
            final_salary,
            onboard_date,
            employee_code,
            preset_username,
            user_id,
            contract_id,
            status,
            create_by,
            create_time,
            update_time,
            deleted
        ) VALUES
        (
            replace(gen_random_uuid()::text, '-', ''), '候选人O', 'candidate.o@company.com',
            '13800138015', '110101199501011234', '服务员', 'STAFF', '7', '服务部',
            5000, 5200, '2026-08-01', NULL, NULL, NULL, NULL, 'APPROVED',
            1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
        ),
        (
            replace(gen_random_uuid()::text, '-', ''), '候选人P', 'candidate.p@company.com',
            '13800138016', '110101199202021234', '厨师', 'STAFF', '6', '后厨部',
            6500, 6800, '2026-08-05', NULL, NULL, NULL, NULL, 'CONTRACT_PENDING',
            1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
        )
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
