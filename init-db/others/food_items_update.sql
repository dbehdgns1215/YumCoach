USE yumcoach_db;

SELECT *
FROM food_items
ORDER BY food_id
LIMIT 50000 OFFSET 50000;


SELECT * FROM yumcoach_db.food_items;

SET SQL_SAFE_UPDATES = 0;

UPDATE food_items
SET food_name = TRIM(LEADING '로이즈 ' FROM food_name)
WHERE food_id IS NOT NULL
  AND food_name LIKE '로이즈 %';

SET SQL_SAFE_UPDATES = 1;


SET SQL_SAFE_UPDATES = 0;

UPDATE food_items
SET food_name = TRIM(LEADING '샌드위치 ' FROM food_name)
WHERE food_id IS NOT NULL
  AND food_name LIKE '샌드위치 %'
  AND food_id LIKE 'D202%';

SET SQL_SAFE_UPDATES = 1;


UPDATE food_items
SET food_name = REGEXP_REPLACE(
    food_name,
    '^잇츠온',
    '<잇츠온> '
)
WHERE food_name REGEXP '^잇츠온'
  AND food_name NOT REGEXP '^<잇츠온>';


SELECT
    food_id,
    food_name AS 'before',
    TRIM(LEADING '버거 ' FROM food_name) AS after
FROM food_items
WHERE food_name LIKE '버거 %';


SELECT
    food_id,
    food_name AS 'before',
    TRIM(LEADING '샌드위치 ' FROM food_name) AS after
FROM food_items
WHERE food_name LIKE '샌드위치 %' AND food_id LIKE 'D202%';
