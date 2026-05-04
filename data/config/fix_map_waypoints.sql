-- Thêm waypoint cho map 176 (Cung Trăng) -> map 178 (Vùng Đất Huyền Thoại)
UPDATE map_template SET waypoints = '[["Vung Dat Huyen Thoai",1150,250,1200,360,0,0,178,100,312]]' WHERE id = 176;

-- Thêm waypoint cho map 178 (Vùng Đất Huyền Thoại) -> map 176 (Cung Trăng)
UPDATE map_template SET waypoints = '[["Cung Trang",0,250,50,360,0,0,176,289,312]]' WHERE id = 178;
