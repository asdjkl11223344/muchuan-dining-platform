USE muchuan_platform;

UPDATE dish
SET image = CONCAT('/img/menu/dish-', id, '.svg')
WHERE id BETWEEN 46 AND 70;

UPDATE setmeal
SET image = CONCAT('/img/menu/setmeal-', id, '.svg')
WHERE id BETWEEN 32 AND 34;

UPDATE order_detail
SET image = CONCAT('/img/menu/dish-', dish_id, '.svg')
WHERE dish_id IS NOT NULL;

UPDATE order_detail
SET image = CONCAT('/img/menu/setmeal-', setmeal_id, '.svg')
WHERE setmeal_id IS NOT NULL;

UPDATE shopping_cart
SET image = CONCAT('/img/menu/dish-', dish_id, '.svg')
WHERE dish_id IS NOT NULL;

UPDATE shopping_cart
SET image = CONCAT('/img/menu/setmeal-', setmeal_id, '.svg')
WHERE setmeal_id IS NOT NULL;
