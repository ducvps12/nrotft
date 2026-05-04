-- ============================================
-- Fix encoding + Doi ten Ngoc Rong Vo Cuc -> Sieu Cap
-- Chay bang: mysql -u root -p --default-character-set=utf8mb4 nrotft < update_sieu_cap_names.sql
-- ============================================

-- Cap nhat ten + mo ta cho 7 vien Ngoc Rong Sieu Cap
UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 1 Sao',
    description = 'Ngoc Rong Sieu Cap 1 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2980;

UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 2 Sao',
    description = 'Ngoc Rong Sieu Cap 2 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2981;

UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 3 Sao',
    description = 'Ngoc Rong Sieu Cap 3 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2982;

UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 4 Sao',
    description = 'Ngoc Rong Sieu Cap 4 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2983;

UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 5 Sao',
    description = 'Ngoc Rong Sieu Cap 5 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2984;

UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 6 Sao',
    description = 'Ngoc Rong Sieu Cap 6 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2985;

UPDATE item_template SET 
    name = 'Ngoc Rong Sieu Cap 7 Sao',
    description = 'Ngoc Rong Sieu Cap 7 Sao. Thu thap du 7 vien Ngoc Rong Sieu Cap (1-7 sao) de trieu hoi Rong Than Sieu Cap!'
WHERE id = 2986;

-- Cap nhat icon_id: dung icon giong style item 1015 (Ngoc rong Sieu Cap, icon = 9650)
-- Dat tat ca 7 vien dung cung icon 9650 (icon vang premium)
UPDATE item_template SET icon_id = 9650 WHERE id IN (2980, 2981, 2982, 2983, 2984, 2985, 2986);
