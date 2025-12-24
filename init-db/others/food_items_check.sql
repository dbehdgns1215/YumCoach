SELECT
    food_id,
    food_name AS 'before',
    CONCAT('<잇츠온> ', SUBSTRING(food_name, CHAR_LENGTH('잇츠온') + 1)) AS after
FROM food_items
WHERE food_name LIKE '잇츠온%'
  AND food_name NOT LIKE '<잇츠온>%';
  -- AND food_id LIKE 'P123-20%';
  
  SELECT fi.food_id, fi.food_name
FROM food_items fi
JOIN (
  SELECT food_name, MIN(food_id) AS keep_id
  FROM food_items
  GROUP BY food_name
  HAVING COUNT(*) > 1
) k ON k.food_name = fi.food_name
WHERE fi.food_id <> k.keep_id
ORDER BY fi.food_name, fi.food_id;

