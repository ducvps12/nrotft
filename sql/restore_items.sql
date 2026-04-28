-- =====================================================
-- KHÔI PHỤC ĐỒ CHO 2 TÀI KHOẢN
-- meomeow (account 75) và nhimoon (account 98)
-- =====================================================

-- Khôi phục đồ mẫu (starter items) cho 2 nhân vật
UPDATE player 
SET items_bag = '[
    "[194,1,\\"[\\\\\\\"[30,1]\\\\\\\"]\\",1777355969103]",
    "[19,50,\\"[\\\\\\\"[73,0]\\\\\\\"]\\",1777356734494]",
    "[18,20,\\"[\\\\\\\"[73,0]\\\\\\\"]\\",1777356678919]",
    "[20,30,\\"[\\\\\\\"[73,0]\\\\\\\"]\\",1777358272625]",
    "[-1,0,\\"[]\\",1777370322944]",
    "[-1,0,\\"[]\\",1777370033044]"
]'
WHERE account_id IN (75, 98);

-- Kết quả
SELECT 
    '=== KHÔI PHỤC ĐỒ HOÀN TẤT ===' as result,
    id,
    name,
    account_id,
    LENGTH(items_bag) as items_bag_length
FROM player 
WHERE account_id IN (75, 98);
