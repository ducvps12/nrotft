-- Thêm Mảnh Đội trưởng Vàng vào bảng radar để player có thể sử dụng thẻ
-- max = 5 (ghép đủ 5 mảnh để lên cấp)
-- Options: HP+5%, Sức đánh+5% khi card active (activeCard=2 = level 2)
INSERT INTO radar (id, iconId, `rank`, `max`, type, mob_id, body, name, info, options, aura_id)
VALUES (
    956,
    8935,
    1,
    5,
    0,
    1,
    '[{"head":-1,"body":-1,"leg":-1,"bag":-1}]',
    'Mảnh Đội trưởng Vàng',
    'Sưu tầm đủ mảnh để nhận buff sức mạnh',
    '[{"id":50,"param":5,"activeCard":2},{"id":77,"param":5,"activeCard":2}]',
    -1
);
