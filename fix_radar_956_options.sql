-- Cập nhật options cho Mảnh Đội trưởng Vàng (956)
-- Level 1: Sức đánh +3%, HP +3%
-- Level 2: Sức đánh +5%, HP +5%
UPDATE radar SET options = '[{"id":50,"param":3,"activeCard":1},{"id":77,"param":3,"activeCard":1},{"id":50,"param":5,"activeCard":2},{"id":77,"param":5,"activeCard":2}]' WHERE id = 956;
