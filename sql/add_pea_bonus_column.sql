-- Migration: Thêm cột lưu trữ dữ liệu bonus đậu thần
-- Chạy script này trên database trước khi deploy server mới

ALTER TABLE `player` ADD COLUMN `data_pea_bonus` TEXT NULL DEFAULT NULL AFTER `checkNhanQua`;
