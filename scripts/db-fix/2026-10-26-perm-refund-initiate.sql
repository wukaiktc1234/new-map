\set ON_ERROR_STOP on
SET client_encoding='UTF8';
BEGIN;
INSERT INTO permissions (permission_id_str, permission_name, permission_code, resource_type, description, status, create_time, deleted)
SELECT 'order:refund:initiate','订单-发起退款','order:refund:initiate','API',
       '发起退款（收银员可发起；制作中/已出餐需店长审批）— NEW-19',1,now(),0
 WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_id_str='order:refund:initiate');
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id_str,'order:refund:initiate',0,now() FROM roles r
 WHERE r.role_id_str IN ('ROLE_CASHIER','ROLE_STORE_MANAGER','ROLE_FINANCE_DIRECTOR') AND r.deleted=0
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.permission_id='order:refund:initiate' AND rp.role_id=r.role_id_str AND rp.deleted=0);
SELECT (SELECT count(*) FROM role_permissions WHERE permission_id='order:refund:initiate' AND deleted=0) AS bound_roles,
       (SELECT string_agg(role_id,',' ORDER BY role_id) FROM role_permissions WHERE permission_id='order:refund:initiate' AND deleted=0) AS roles;
COMMIT;