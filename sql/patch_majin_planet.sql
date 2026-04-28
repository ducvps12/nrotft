-- =====================================================
-- PATCH: Majin Planet Maps (Hệ Majin - Planet ID 3)
-- Maps 198-206 for the Majin race
-- Reuses tilesets/backgrounds from existing planets
-- =====================================================

-- Nhà Majin (Home) - Map 198 - Reuse Nhà Gôhan tileset
-- type=1 (indoor), planet_id=3 (Majin)
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(198, 'Nhà Majin', 50, 50, '[1,3,0,1,0]', 1, 3, 0, 1, 0, '[["Hoang Mạc Majin",456,312,528,336,1,1,199,332,432],["Sân Vườn Majin",720,312,744,336,0,0,205,110,336]]', '[]', '[[4,348,336],[3,84,336],[0,228,336],[50,700,336]]', 0, '[{"value":"15","key":"beff"},{"value":"53.2.439.344","key":"eff"}]', '[]');

-- Hoang Mạc Majin (Starter Map 1) - Map 199 - Reuse Xayda tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(199, 'Hoang Mạc Majin', 30, 12, '[0,3,0,9,8]', 0, 3, 0, 9, 8, '[["Vùng Đất Bùn",1224,384,1248,408,0,0,200,60,408],["Nhà Majin",504,384,576,408,1,1,198,475,336],["Vách Núi Majin",0,384,24,408,0,0,204,1380,432]]', '[["0,1,100,708,408"],["0,1,100,804,408"],["0,1,100,900,408"],["0,1,100,996,408"]]', '[[9,396,408],[6,252,408],[77,624,408]]', 0, '[{"value":"6","key":"beff"}]', '[]');

-- Vùng Đất Bùn (Starter Map 2) - Map 200 - Reuse Namec tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(200, 'Vùng Đất Bùn', 30, 12, '[0,3,4,5,0]', 0, 3, 4, 5, 0, '[["Hoang Mạc Majin",0,384,24,408,0,0,199,1188,408],["Thung Lũng Kẹo",1224,384,1248,408,0,0,201,60,408]]', '[["3,2,200,276,408"],["3,2,200,444,360"],["3,2,200,612,360"],["3,2,200,900,312"],["3,2,200,756,312"]]', '[[6,996,264]]', 0, '[]', '[]');

-- Thung Lũng Kẹo (Mid Map) - Map 201 - Reuse Earth tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(201, 'Thung Lũng Kẹo', 30, 12, '[0,3,0,1,0]', 0, 3, 0, 1, 0, '[["Vùng Đất Bùn",0,384,24,408,0,0,200,1188,408],["Rừng Ma Majin",1224,240,1248,264,0,0,202,60,288],["Trạm Tàu Vũ Trụ Majin",192,264,264,288,1,0,206,406,336]]', '[["6,3,500,564,312"],["6,3,500,804,336"],["3,2,200,972,288"],["3,2,200,684,384"]]', '[[6,396,288]]', 0, '[]', '[]');

-- Rừng Ma Majin (Mid Map) - Map 202 - Reuse tileset 2 (Rừng)
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(202, 'Rừng Ma Majin', 12, 12, '[0,3,0,2,1]', 0, 3, 0, 2, 1, '[["Thung Lũng Kẹo",0,264,24,288,0,0,201,1188,264],["Hầm Ngục Majin",1560,288,1584,312,0,0,203,60,408]]', '[["5,3,500,660,288"],["5,3,500,1068,288"],["8,4,600,372,312"],["8,4,600,852,336"],["8,4,600,1212,216"],["11,5,1000,516,144"],["11,5,1000,1044,144"]]', '[[6,180,408]]', 0, '[]', '[]');

-- Hầm Ngục Majin (High Map) - Map 203 - Reuse tileset 7 (Namec cave)
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(203, 'Hầm Ngục Majin', 12, 12, '[0,3,0,7,6]', 0, 3, 0, 7, 6, '[["Rừng Ma Majin",0,384,24,408,0,0,202,1524,312]]', '[["14,7,3000,852,288"],["14,7,3000,1020,288"],["14,7,3000,396,264"],["14,7,3000,1236,360"],["14,7,3000,1404,360"]]', '[[6,228,240]]', 0, '[]', '[]');

-- Vách Núi Majin (Outskirt/Boss area) - Map 204 - Reuse Xayda tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(204, 'Vách Núi Majin', 12, 12, '[0,3,0,9,8]', 0, 3, 0, 9, 8, '[["Hoang Mạc Majin",1416,408,1440,432,0,0,199,60,408],["Trạm tàu Hủy Diệt",35,258,40,288,0,0,207,895,336]]', '[]', '[[21,588,408],[23,1015,408],[75,429,432]]', 0, '[]', '[]');

-- Sân Vườn Majin (Garden) - Map 205 - Reuse Garden tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(205, 'Khu Vực Luyện Tập', 12, 12, '[1,3,0,9,8]', 0, 3, 0, 9, 8, '[["Nhà Majin",0,312,24,336,0,0,198,702,336]]', '[["110,0,2000000000,543,336"],["110,0,2000000000,446,336"],["110,0,2000000000,349,336"],["110,0,2000000000,646,336"]]', '[]', 0, '[]', '[]');

-- Trạm Tàu Vũ Trụ Majin (Space Station) - Map 206 - Reuse station tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(206, 'Trạm tàu vũ trụ', 12, 12, '[0,3,0,9,8]', 0, 3, 0, 9, 8, '[["Thung Lũng Kẹo",360,312,432,336,1,0,201,231,288]]', '[]', '[[12,228,336],[6,516,336],[16,510,336],[29,84,336]]', 0, '[]', '[]');

-- Trạm tàu Hủy Diệt Majin - Map 207 - Reuse trạm tàu hủy diệt tileset
INSERT INTO `map_template` (`id`, `NAME`, `zones`, `max_player`, `data`, `type`, `planet_id`, `bg_type`, `tile_id`, `bg_id`, `waypoints`, `mobs`, `npcs`, `is_map_double`, `effect`, `eff_event`) VALUES
(207, 'Trạm tàu Hủy Diệt Majin', 12, 12, '[1,0,0,4,3]', 0, 3, 0, 4, 3, '[["Vách Núi Majin",936,312,960,336,0,0,204,77,288]]', '[]', '[]', 0, '[]', '[]');

-- =====================================================
-- MAP ID SUMMARY:
-- 198 = Nhà Majin (Home)         → ConstMap.NHA_MAJIN
-- 199 = Hoang Mạc Majin          → ConstMap.HOANG_MAC_MAJIN  
-- 200 = Vùng Đất Bùn             → ConstMap.VUNG_DAT_BUN
-- 201 = Thung Lũng Kẹo           → ConstMap.THUNG_LUNG_KEO
-- 202 = Rừng Ma Majin            → ConstMap.RUNG_MA_MAJIN
-- 203 = Hầm Ngục Majin           → ConstMap.HAM_NGUC_MAJIN
-- 204 = Vách Núi Majin           → (outskirt/boss)
-- 205 = Khu Vực Luyện Tập Majin  → (training area)
-- 206 = Trạm Tàu Vũ Trụ Majin   → ConstMap.TRAM_TAU_MAJIN
-- 207 = Trạm tàu Hủy Diệt Majin → (destroyer station)
-- =====================================================
