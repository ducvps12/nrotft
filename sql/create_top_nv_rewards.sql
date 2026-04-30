-- Tạo bảng moc_nhiem_vu_top (phần thưởng Top Nhiệm Vụ)
CREATE TABLE IF NOT EXISTS `moc_nhiem_vu_top` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `info` varchar(255) NOT NULL DEFAULT 'TopNhiemVu',
  `detail` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Thêm dữ liệu phần thưởng Top 1-10 NV
INSERT INTO `moc_nhiem_vu_top` (`id`, `info`, `detail`) VALUES
(1, 'TopNhiemVu', '[{"temp_id":457,"quantity":200,"options":[]},{"temp_id":1703,"quantity":1,"options":[{"param":1,"id":30}]},{"temp_id":14,"quantity":10,"options":[]},{"temp_id":15,"quantity":10,"options":[]},{"temp_id":16,"quantity":10,"options":[]}]'),
(2, 'TopNhiemVu', '[{"temp_id":457,"quantity":150,"options":[]},{"temp_id":1703,"quantity":1,"options":[{"param":1,"id":30}]},{"temp_id":14,"quantity":8,"options":[]},{"temp_id":15,"quantity":8,"options":[]}]'),
(3, 'TopNhiemVu', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":6,"options":[]},{"temp_id":15,"quantity":6,"options":[]},{"temp_id":16,"quantity":6,"options":[]}]'),
(4, 'TopNhiemVu', '[{"temp_id":457,"quantity":80,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]}]'),
(5, 'TopNhiemVu', '[{"temp_id":457,"quantity":60,"options":[]},{"temp_id":14,"quantity":4,"options":[]},{"temp_id":15,"quantity":4,"options":[]}]'),
(6, 'TopNhiemVu', '[{"temp_id":457,"quantity":50,"options":[]},{"temp_id":14,"quantity":3,"options":[]},{"temp_id":15,"quantity":3,"options":[]}]'),
(7, 'TopNhiemVu', '[{"temp_id":457,"quantity":50,"options":[]},{"temp_id":14,"quantity":3,"options":[]},{"temp_id":15,"quantity":3,"options":[]}]'),
(8, 'TopNhiemVu', '[{"temp_id":457,"quantity":40,"options":[]},{"temp_id":14,"quantity":2,"options":[]},{"temp_id":15,"quantity":2,"options":[]}]'),
(9, 'TopNhiemVu', '[{"temp_id":457,"quantity":40,"options":[]},{"temp_id":14,"quantity":2,"options":[]},{"temp_id":15,"quantity":2,"options":[]}]'),
(10, 'TopNhiemVu', '[{"temp_id":457,"quantity":30,"options":[]},{"temp_id":14,"quantity":2,"options":[]},{"temp_id":15,"quantity":2,"options":[]}]');

-- Thêm cột tracking claim vào bảng player (ĐÃ CHẠY)
-- ALTER TABLE `player` ADD COLUMN `lastClaimTopSM` bigint(20) NOT NULL DEFAULT 0;
-- ALTER TABLE `player` ADD COLUMN `lastClaimTopNV` bigint(20) NOT NULL DEFAULT 0;
-- ALTER TABLE `player` ADD COLUMN `totalManhVoBought` int(11) NOT NULL DEFAULT 0;
