SET client_encoding='UTF8';
\echo '=== before ==='
SELECT food_id, food_name, stock FROM food ORDER BY food_id;
UPDATE food SET stock = 500 WHERE stock < 100;
\echo '=== after ==='
SELECT food_id, food_name, stock FROM food ORDER BY food_id;