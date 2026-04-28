-- =====================================================
-- SCRIPT KIỂM TRA BÙA X2 ĐỆ TỬ
-- Kiểm tra xem bùa x2 đệ có hoạt động không
-- =====================================================

-- 1. Kiểm tra cấu trúc data_item_time
SELECT 
    '=== CẤU TRÚC DATA_ITEM_TIME ===' as info,
    p.id,
    p.name,
    a.username,
    p.data_item_time
FROM player p
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
LIMIT 5;

-- 2. Kiểm tra người chơi có bùa x2 đệ tử
SELECT 
    '=== NGƯỜI CHƠI CÓ BÙA X2 ĐỆ TỬ ===' as info,
    p.id,
    p.name,
    a.username,
    a.ip_address,
    CASE 
        WHEN p.data_item_time LIKE '%"25"%' THEN 'CÓ BÙA X2 ĐỆ TỬ'
        ELSE 'KHÔNG CÓ'
    END as bua_x2_de_status,
    p.data_item_time
FROM player p
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
  AND p.data_item_time IS NOT NULL
  AND p.data_item_time != '[]'
ORDER BY p.id DESC
LIMIT 20;

-- 3. Thống kê bùa đang sử dụng
SELECT 
    '=== THỐNG KÊ BÙA ĐANG SỬ DỤNG ===' as info,
    COUNT(*) as total_players,
    SUM(CASE WHEN data_item_time LIKE '%"0"%' THEN 1 ELSE 0 END) as bua_bo_huyet,
    SUM(CASE WHEN data_item_time LIKE '%"2"%' THEN 1 ELSE 0 END) as bua_bo_khi,
    SUM(CASE WHEN data_item_time LIKE '%"4"%' THEN 1 ELSE 0 END) as bua_giap_xen,
    SUM(CASE WHEN data_item_time LIKE '%"6"%' THEN 1 ELSE 0 END) as bua_cuong_no,
    SUM(CASE WHEN data_item_time LIKE '%"8"%' THEN 1 ELSE 0 END) as bua_an_danh,
    SUM(CASE WHEN data_item_time LIKE '%"10"%' THEN 1 ELSE 0 END) as bua_mo_gioi_han,
    SUM(CASE WHEN data_item_time LIKE '%"23"%' THEN 1 ELSE 0 END) as bua_co_bon_la,
    SUM(CASE WHEN data_item_time LIKE '%"25"%' THEN 1 ELSE 0 END) as bua_x2_de_tu
FROM player p
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
  AND p.data_item_time IS NOT NULL
  AND p.data_item_time != '[]';

-- 4. Kiểm tra logic bùa x2 đệ trong code
-- Index 25 trong data_item_time = timeUseBuax2DeTu
-- Nếu > 0 thì bùa đang hoạt động

SELECT 
    '=== CHI TIẾT BÙA X2 ĐỆ TỬ ===' as info,
    p.id,
    p.name,
    a.username,
    JSON_EXTRACT(p.data_item_time, '$[25]') as time_bua_x2_de,
    CASE 
        WHEN JSON_EXTRACT(p.data_item_time, '$[25]') > 0 THEN 'ĐANG HOẠT ĐỘNG'
        ELSE 'KHÔNG HOẠT ĐỘNG'
    END as status,
    FROM_UNIXTIME(UNIX_TIMESTAMP() - JSON_EXTRACT(p.data_item_time, '$[25]')/1000) as thoi_gian_con_lai
FROM player p
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
  AND p.data_item_time IS NOT NULL
  AND JSON_EXTRACT(p.data_item_time, '$[25]') IS NOT NULL
ORDER BY JSON_EXTRACT(p.data_item_time, '$[25]') DESC
LIMIT 20;

-- 5. Kiểm tra pet có nhận buff x2 không
SELECT 
    '=== KIỂM TRA PET NHẬN BUFF X2 ===' as info,
    p.id as player_id,
    p.name as player_name,
    pet.id as pet_id,
    pet.name as pet_name,
    pet.power as pet_power,
    JSON_EXTRACT(p.data_item_time, '$[25]') as bua_x2_de_time,
    CASE 
        WHEN JSON_EXTRACT(p.data_item_time, '$[25]') > 0 THEN 'MASTER CÓ BÙA X2'
        ELSE 'MASTER KHÔNG CÓ BÙA'
    END as master_bua_status
FROM player p
INNER JOIN player pet ON pet.account_id = p.account_id AND pet.is_pet = 1
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
  AND p.data_item_time IS NOT NULL
ORDER BY p.id DESC
LIMIT 10;

-- 6. Tìm người chơi có bùa x2 đệ nhưng pet không lên exp
-- (Nếu có thì bùa không hoạt động)
SELECT 
    '=== NGƯỜI CHƠI CÓ BÙA NHƯNG PET KHÔNG LÊN EXP ===' as warning,
    p.id,
    p.name,
    JSON_EXTRACT(p.data_item_time, '$[25]') as bua_x2_time,
    pet.name as pet_name,
    pet.power as pet_power,
    'Cần kiểm tra trong game' as note
FROM player p
INNER JOIN player pet ON pet.account_id = p.account_id AND pet.is_pet = 1
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
  AND JSON_EXTRACT(p.data_item_time, '$[25]') > 0
ORDER BY p.id DESC
LIMIT 10;

-- 7. Hướng dẫn test trong game
SELECT 
    '=== HƯỚNG DẪN TEST TRONG GAME ===' as guide,
    '1. Tạo tài khoản test' as step_1,
    '2. Mua bùa x2 đệ tử từ shop' as step_2,
    '3. Sử dụng bùa' as step_3,
    '4. Đánh quái với đệ tử' as step_4,
    '5. Kiểm tra exp đệ tử có x2 không' as step_5,
    '6. Nếu không x2 => Bug cần fix' as step_6;

-- 8. Kiểm tra item bùa x2 đệ trong database
SELECT 
    '=== THÔNG TIN ITEM BÙA X2 ĐỆ ===' as info,
    id,
    name,
    description,
    type,
    gender,
    level
FROM item_template
WHERE name LIKE '%đệ%' 
   OR name LIKE '%x2%'
   OR id IN (SELECT DISTINCT JSON_EXTRACT(value, '$.id') 
             FROM JSON_TABLE(
                 (SELECT items_bag FROM player LIMIT 1),
                 '$[*]' COLUMNS(value JSON PATH '$')
             ) as jt)
LIMIT 20;
