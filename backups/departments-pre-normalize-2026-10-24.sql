--
-- PostgreSQL database dump
--

\restrict W9koY5r1XoMj35jVPtqb7u3VzFzf0HCUsbErpGi5cYzA1m7bkPcTkgiXlRN9fBo

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: departments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.departments (department_id, dept_name, dept_code, parent_id, level, status, description, sort_order, employee_count, base_salary, deleted, create_time, update_time, created_by, updated_by, store_id, type, manager_name) FROM stdin;
4	运营部	DEPT_OPERATIONS	576	2	1	负责门店运营、销售管理、运营数据分析等工作	0	5	\N	0	2026-07-23 12:57:33.794661	2026-07-23 12:57:33.794661	\N	\N	\N	department	\N
5	仓储部	DEPT_WAREHOUSE	576	2	1	负责仓库管理、库存盘点、出入库等仓储管理工作	0	2	\N	0	2026-07-23 12:57:33.794949	2026-07-23 12:57:33.794949	\N	\N	\N	department	\N
3	采购部	DEPT_PURCHASE	576	2	1	负责采购计划、供应商管理、采购执行等采购管理工作	0	2	\N	0	2026-07-23 12:57:33.79438	\N	\N	\N	\N	department	采购部经理
2	财务部	DEPT_FINANCE	576	2	1	负责财务核算、资金管理、应付账款等财务管理工作	0	3	\N	0	2026-07-23 12:57:33.794103	\N	\N	\N	\N	department	财务部经理
1	人事部	DEPT_HR	576	2	1	负责人事招聘、员工档案、考勤薪资等人事管理工作	1	2	\N	0	2026-07-23 12:57:33.793008	\N	\N	\N	\N	department	人事部经理
2425	测试门店C	STORE_1	4	3	1	\N	0	0	\N	0	\N	\N	\N	\N	\N	store	\N
3002	S9C???020636	S9C_PUR_020636	576	2	1	S9c E2E	0	0	\N	0	\N	\N	\N	\N	\N	department	\N
3003	S9C???020744	S9C_PUR_020744	576	2	1	S9c E2E	0	0	\N	0	\N	\N	\N	\N	\N	department	\N
584	仓库A（主仓库）	WH_A	5	3	1	北京市朝阳区仓储路1号	0	0	\N	0	2026-07-25 17:42:13.638361	\N	\N	\N	\N	warehouse	\N
585	仓库B（分仓库）	WH_B	5	3	1	北京市海淀区仓储路2号	0	0	\N	0	2026-07-25 17:42:13.638591	\N	\N	\N	\N	warehouse	\N
624	测试门店A	STORE_A	4	3	1	北京市朝阳区测试路1号，联系电话：010-88880001	0	0	\N	0	2026-07-25 18:19:50.47051	\N	\N	\N	\N	store	\N
625	测试门店B	STORE_B	4	3	1	北京市海淀区测试路2号，联系电话：010-88880002	0	0	\N	0	2026-07-25 18:19:50.470891	\N	\N	\N	\N	store	\N
576	测试公司	COMPANY_ROOT	0	1	1	食品溯源系统测试公司，组织架构根节点	0	0	\N	0	2026-07-25 17:42:13.633731	2026-07-25 17:42:13.633731	\N	\N	\N	company	\N
1729	Test Purchase Dept 154027	TEST_PUR_154027	576	2	1	Test department	0	0	\N	1	\N	\N	\N	\N	\N	department	\N
1730	Test Purchase Dept 154237	TEST_PUR_154237	576	2	1	Test department	0	0	\N	1	\N	\N	\N	\N	\N	department	\N
1745	测试门店A001	STORE	0	1	1	\N	0	0	\N	1	\N	\N	\N	\N	\N	store	\N
1723	默认门店部门	STORE_1785306863510	1722	2	0	\N	0	0	\N	1	2026-07-29 14:34:23.51002	2026-07-29 14:34:23.51002	\N	\N	\N	store	\N
1722	测试公司001	TOP_1785306863500	\N	1	0	\N	0	0	\N	1	2026-07-29 14:34:23.500148	2026-07-29 14:34:23.500148	\N	\N	\N	company	\N
7	服务部	DEPT_SERVICE	576	2	1	负责前厅服务、顾客接待等服务工作	0	0	\N	1	2026-07-23 12:57:33.795643	2026-07-23 12:57:33.795643	\N	\N	\N	department	\N
6	后厨部	DEPT_KITCHEN	576	2	1	updated	0	0	\N	0	2026-07-23 12:57:33.795284	\N	\N	\N	\N	department	后厨负责人
1822	测试采购部	PUR_1	0	1	1	测试	0	0	\N	1	\N	\N	\N	\N	\N	department	张三
1823	测试采购部	PUR_2	0	1	1	测试	0	0	\N	1	\N	\N	\N	\N	\N	department	张三
1824	测试连锁部	QA_1	0	1	1	\N	0	0	\N	0	\N	\N	\N	\N	\N	department	\N
706	API?????-???	UNIQUE_DEPT_001	576	2	0	???	0	0	\N	1	\N	\N	\N	\N	\N	department	????
561	测试公司A	QA	576	2	1	\N	0	0	\N	1	\N	\N	\N	\N	\N	company	\N
664	API-Test-Dept	DEPT	576	2	0	Created by API test	0	0	\N	1	\N	\N	\N	\N	\N	department	Test-Manager
877	CRUDValidationDept-Updated	CRUD191856	576	2	1	Updated description	0	0	\N	1	\N	\N	\N	\N	\N	department	TestManager-Updated
864	APITestDept-Updated	CRUD191515	576	2	1	Updated description	0	0	\N	1	\N	\N	\N	\N	\N	department	TestManager-Updated
915	API验证子部门	CHILD193104	914	4	1	用于删除拦截验证	0	0	\N	1	\N	\N	\N	\N	\N	team	子部门经理
914	API验证部门-已更新	CRUD193104	624	3	1	已更新描述	0	0	\N	1	\N	\N	\N	\N	\N	department	验证经理-已更新
941	API验证子部门	CHILD210319	940	4	1	用于删除拦截验证	0	0	\N	1	\N	\N	\N	\N	\N	team	子部门经理
940	API验证部门-已更新	CRUD210319	624	3	1	已更新描述	0	0	\N	1	\N	\N	\N	\N	\N	department	验证经理-已更新
1744	测试采购部UI	PUR	0	1	1	\N	0	0	\N	1	\N	\N	\N	\N	\N	department	\N
1743	E2E Dept 160258715	DEP-160258715	0	1	1	\N	0	0	\N	1	\N	\N	\N	\N	\N	department	\N
1724	Test Purchase Dept	TEST_PUR_001	576	2	1	Test department	0	0	\N	1	\N	\N	\N	\N	\N	department	\N
3004	S9C???023003	S9C_PUR_023003	576	2	1	S9c E2E	0	0	\N	0	\N	\N	\N	\N	\N	department	\N
3005	S9C???023059	S9C_PUR_023059	576	2	1	S9c E2E	0	0	\N	0	\N	\N	\N	\N	\N	department	\N
3006	S9C???023148	S9C_PUR_023148	576	2	1	S9c E2E	0	0	\N	0	\N	\N	\N	\N	\N	department	\N
\.


--
-- Name: departments_department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.departments_department_id_seq', 3006, true);


--
-- PostgreSQL database dump complete
--

\unrestrict W9koY5r1XoMj35jVPtqb7u3VzFzf0HCUsbErpGi5cYzA1m7bkPcTkgiXlRN9fBo

