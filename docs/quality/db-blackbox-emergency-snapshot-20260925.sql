-- PROVISIONAL_LOCAL_UNVERIFIED
-- 本快照从本地活体 DB 导出，未与生产核对
-- 用途：应急线索，非真相源；生产比对后作废重生成
-- 来源：P0-SCHEMA-SINGLE-SOURCE-001 收口溯源 KL-082（2026-09-25）
-- 重要口径修正：KL-082 的 47 张黑箱表中，仅以下 4 张存在于本地活体 DB（本文件含其 DDL）；
--   其余 43 张连本地 DB 都不存在——schema 只可能存在于生产 DB，重建风险比首判更高，生产核对为唯一解；
-- 表清单（存在于本地，已导出 DDL）：
--   app_device_registrations inventory_warning_rules position_code_rules receipt_print_log
-- 本地 DB 不存在的 43 张（生产核对重点清单）：
--   budget category_operation_log cost_allocation cost_allocation_detail cost_allocation_rule coupon data_link_event department_permissions hardware_config_version hardware_operation_log inventory_category inventory_transfer_detail inventory_unit inventory_warning_records invitation_expense_record invitation_reminder_log material_usage_record ocr_record operation_logs operation_type order_material_requirement over_age_worker plan_item print_format_template receivable registration_code_log role_departments role_stores salary_adjustment salary_record salary_rule self_purchase self_purchase_item supplier_evaluations sys_devices sys_permissions sys_print_templates sys_roles sys_stores trace_code_log trace_stage user_sessions v_inventory_summary

--
-- PostgreSQL database dump
--

\restrict wNg18jBjBOuH3NVGw7iFbqxRPpuTGGdWhXbevahYi1QzAmqCuQmF7f7vgfVytby

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_device_registrations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_device_registrations (
    registration_id bigint NOT NULL,
    device_id character varying(64),
    device_name character varying(100),
    activation_code character varying(16) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    bound_username character varying(64),
    code_expire_at timestamp without time zone,
    activated_at timestamp without time zone,
    last_seen_at timestamp without time zone,
    create_time timestamp without time zone DEFAULT now(),
    deleted smallint DEFAULT 0
);


ALTER TABLE public.app_device_registrations OWNER TO postgres;

--
-- Name: app_device_registrations_registration_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.app_device_registrations_registration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.app_device_registrations_registration_id_seq OWNER TO postgres;

--
-- Name: app_device_registrations_registration_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.app_device_registrations_registration_id_seq OWNED BY public.app_device_registrations.registration_id;


--
-- Name: inventory_warning_rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.inventory_warning_rules (
    rule_id bigint NOT NULL,
    rule_name character varying(100),
    warehouse_id bigint,
    warning_type integer,
    condition_field character varying(100),
    condition_operator character varying(20),
    threshold_value numeric(18,4),
    notify_method integer,
    is_enabled boolean,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.inventory_warning_rules OWNER TO postgres;

--
-- Name: inventory_warning_rules_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.inventory_warning_rules ALTER COLUMN rule_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.inventory_warning_rules_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: position_code_rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.position_code_rules (
    id bigint NOT NULL,
    keyword character varying(100) NOT NULL,
    code character varying(20) NOT NULL,
    format_template character varying(100),
    sort_order integer DEFAULT 0,
    created_by character varying(50),
    updated_by character varying(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted smallint DEFAULT 0,
    version integer DEFAULT 0
);


ALTER TABLE public.position_code_rules OWNER TO postgres;

--
-- Name: position_code_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.position_code_rules ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.position_code_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: receipt_print_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.receipt_print_log (
    print_id bigint NOT NULL,
    confirmation_id bigint NOT NULL,
    operator_user_id bigint,
    operator_name character varying(64),
    print_time timestamp without time zone DEFAULT now() NOT NULL,
    source_ip character varying(45)
);


ALTER TABLE public.receipt_print_log OWNER TO postgres;

--
-- Name: receipt_print_log_print_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.receipt_print_log_print_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.receipt_print_log_print_id_seq OWNER TO postgres;

--
-- Name: receipt_print_log_print_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.receipt_print_log_print_id_seq OWNED BY public.receipt_print_log.print_id;


--
-- Name: app_device_registrations registration_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_device_registrations ALTER COLUMN registration_id SET DEFAULT nextval('public.app_device_registrations_registration_id_seq'::regclass);


--
-- Name: receipt_print_log print_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.receipt_print_log ALTER COLUMN print_id SET DEFAULT nextval('public.receipt_print_log_print_id_seq'::regclass);


--
-- Name: app_device_registrations app_device_registrations_activation_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_device_registrations
    ADD CONSTRAINT app_device_registrations_activation_code_key UNIQUE (activation_code);


--
-- Name: app_device_registrations app_device_registrations_device_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_device_registrations
    ADD CONSTRAINT app_device_registrations_device_id_key UNIQUE (device_id);


--
-- Name: app_device_registrations app_device_registrations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_device_registrations
    ADD CONSTRAINT app_device_registrations_pkey PRIMARY KEY (registration_id);


--
-- Name: inventory_warning_rules inventory_warning_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventory_warning_rules
    ADD CONSTRAINT inventory_warning_rules_pkey PRIMARY KEY (rule_id);


--
-- Name: position_code_rules position_code_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.position_code_rules
    ADD CONSTRAINT position_code_rules_pkey PRIMARY KEY (id);


--
-- Name: receipt_print_log receipt_print_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.receipt_print_log
    ADD CONSTRAINT receipt_print_log_pkey PRIMARY KEY (print_id);


--
-- Name: idx_device_reg_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_device_reg_status ON public.app_device_registrations USING btree (status);


--
-- Name: idx_inventory_warning_rules_enabled; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_inventory_warning_rules_enabled ON public.inventory_warning_rules USING btree (is_enabled);


--
-- Name: idx_inventory_warning_rules_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_inventory_warning_rules_type ON public.inventory_warning_rules USING btree (warning_type);


--
-- Name: idx_inventory_warning_rules_warehouse; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_inventory_warning_rules_warehouse ON public.inventory_warning_rules USING btree (warehouse_id);


--
-- Name: idx_position_code_rules_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_position_code_rules_code ON public.position_code_rules USING btree (code);


--
-- Name: idx_position_code_rules_keyword; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_position_code_rules_keyword ON public.position_code_rules USING btree (keyword);


--
-- Name: idx_print_log_conf; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_print_log_conf ON public.receipt_print_log USING btree (confirmation_id, print_time);


--
-- PostgreSQL database dump complete
--

\unrestrict wNg18jBjBOuH3NVGw7iFbqxRPpuTGGdWhXbevahYi1QzAmqCuQmF7f7vgfVytby

