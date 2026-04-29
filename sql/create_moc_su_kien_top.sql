-- Tạo bảng moc_su_kien_top (cùng cấu trúc với moc_suc_manh_top)
-- Fix lỗi: Table 'nrotft.moc_su_kien_top' doesn't exist

CREATE TABLE IF NOT EXISTS `moc_su_kien_top` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `info` text NOT NULL,
  `detail` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Dữ liệu mẫu cho Top 1-10 Sự Kiện
-- Top 1: 300 thỏi vàng + 2 giày (khóa) + 12 mỗi loại ngọc rồng
INSERT INTO `moc_su_kien_top` (`id`, `info`, `detail`) VALUES
(1, 'TopSuKien', '[{"temp_id":457,"quantity":300,"options":[]},{"temp_id":1703,"quantity":2,"options":[{"param":1,"id":30}]},{"temp_id":14,"quantity":12,"options":[]},{"temp_id":15,"quantity":12,"options":[]},{"temp_id":16,"quantity":12,"options":[]}]'),
(2, 'TopSuKien', '[{"temp_id":457,"quantity":200,"options":[]},{"temp_id":1703,"quantity":1,"options":[{"param":1,"id":30}]},{"temp_id":14,"quantity":10,"options":[]},{"temp_id":15,"quantity":10,"options":[]},{"temp_id":16,"quantity":10,"options":[]}]'),
(3, 'TopSuKien', '[{"temp_id":457,"quantity":150,"options":[]},{"temp_id":1703,"quantity":1,"options":[{"param":1,"id":30}]},{"temp_id":14,"quantity":8,"options":[]},{"temp_id":15,"quantity":8,"options":[]},{"temp_id":16,"quantity":8,"options":[]}]'),
(4, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":1703,"quantity":1,"options":[{"param":1,"id":30}]},{"temp_id":14,"quantity":8,"options":[]},{"temp_id":15,"quantity":8,"options":[]},{"temp_id":16,"quantity":8,"options":[]}]'),
(5, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]},{"temp_id":16,"quantity":5,"options":[]}]'),
(6, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]},{"temp_id":16,"quantity":5,"options":[]}]'),
(7, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]},{"temp_id":16,"quantity":5,"options":[]}]'),
(8, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]},{"temp_id":16,"quantity":5,"options":[]}]'),
(9, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]},{"temp_id":16,"quantity":5,"options":[]}]'),
(10, 'TopSuKien', '[{"temp_id":457,"quantity":100,"options":[]},{"temp_id":14,"quantity":5,"options":[]},{"temp_id":15,"quantity":5,"options":[]},{"temp_id":16,"quantity":5,"options":[]}]');
