-- 初始化真实入职档案数据，与员工/用户/部门打通
-- 状态说明：REGISTERED=已注册入职，CONTRACT_SIGNED=合同已签，APPROVED=审批通过

INSERT INTO onboarding_archive (
    candidate_id, candidate_name, email, phone, id_card,
    position, position_level, department_id, department_name,
    expected_salary, final_salary, onboard_date,
    employee_code, preset_username, user_id, status,
    create_by, create_time, update_time, deleted
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
    'REGISTERED',
    1,
    NOW(),
    NOW(),
    0
FROM employees e
JOIN users u ON u.employee_code = e.employee_code
JOIN positions p ON p.position_id = e.position_id
JOIN departments d ON d.department_id = e.department_id
WHERE e.deleted = 0 AND u.deleted = 0
ON CONFLICT DO NOTHING;

-- 同时补充2条待入职/待签合同的真实候选人数据，模拟招聘到入职的过渡期
INSERT INTO onboarding_archive (
    candidate_id, candidate_name, email, phone, id_card,
    position, position_level, department_id, department_name,
    expected_salary, final_salary, onboard_date,
    employee_code, preset_username, user_id, status,
    create_by, create_time, update_time, deleted
) VALUES
(
    replace(gen_random_uuid()::text, '-', ''), '候选人O', 'candidate.o@company.com',
    '13800138015', '110101199501011234', '服务员', 'STAFF', '7', '服务部',
    5000, 5200, '2026-08-01', NULL, NULL, NULL, 'APPROVED',
    1, NOW(), NOW(), 0
),
(
    replace(gen_random_uuid()::text, '-', ''), '候选人P', 'candidate.p@company.com',
    '13800138016', '110101199202021234', '厨师', 'STAFF', '6', '后厨部',
    6500, 6800, '2026-08-05', NULL, NULL, NULL, 'CONTRACT_PENDING',
    1, NOW(), NOW(), 0
)
ON CONFLICT DO NOTHING;

-- 验证
SELECT status, COUNT(*) FROM onboarding_archive WHERE deleted = 0 GROUP BY status ORDER BY status;
SELECT candidate_name, department_name, position, status, employee_code, preset_username FROM onboarding_archive WHERE deleted = 0 ORDER BY create_time LIMIT 10;
