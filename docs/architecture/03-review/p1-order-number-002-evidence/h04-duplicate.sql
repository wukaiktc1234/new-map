-- H-04: attempt duplicate order_number (UNIQUE should reject)
INSERT INTO orders (order_id, order_code, order_number, order_type, store_id, order_status, payment_status, total_amount, discount_amount, delivery_fee, packaging_fee, final_amount, paid_amount, refund_amount, create_time, update_time)
SELECT 'H04-DUP-TEST', 'ORD202609231655140001', 'ORD202609231655140001', 3, 1, 0, 0, 80, 0, 0, 0, 80, 0, 0, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM orders WHERE order_id='H04-DUP-TEST');
