START TRANSACTION;

-- 삭제 대상 food_id 목록(각 food_name 당 1개만 남기고 나머지)
CREATE TEMPORARY TABLE dup_delete_ids AS
SELECT fi.food_id
FROM food_items fi
JOIN (
  SELECT food_name, MIN(food_id) AS keep_id
  FROM food_items
  GROUP BY food_name
  HAVING COUNT(*) > 1
) k ON k.food_name = fi.food_name
WHERE fi.food_id <> k.keep_id;

-- (선택) 몇 개 삭제되는지 카운트
SELECT COUNT(*) AS ids_to_delete FROM dup_delete_ids;

-- 1) 자식 테이블 삭제
DELETE nfp
FROM nutrition_facts_primary nfp
JOIN dup_delete_ids d ON d.food_id = nfp.food_id;

DELETE nf
FROM nutrition_facts nf
JOIN dup_delete_ids d ON d.food_id = nf.food_id;

-- 2) 부모 테이블 삭제
DELETE fi
FROM food_items fi
JOIN dup_delete_ids d ON d.food_id = fi.food_id;

COMMIT;
-- 문제 생기면: ROLLBACK;


SELECT food_name, COUNT(*) AS cnt
FROM food_items
GROUP BY food_name
HAVING COUNT(*) > 1;

