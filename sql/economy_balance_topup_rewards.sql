-- Balanced top-up milestone rewards for NROTFT
-- Applies to nrotft.moc_nap and nrotft.moc_nap_top
-- Item IDs: 457 = Thỏi vàng, 861 = Hồng ngọc

-- Mốc nạp tích lũy thresholds are defined in Archivement.GIADOLACHIADOI:
-- 20k, 40k, 60k, 100k, 140k, 200k, 400k, 800k, 1.2m, 1.6m, 2m, 2.6m, 4m, 6m
-- Rewards are intentionally conservative to avoid server inflation.

UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":1,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":500,"options":[]}]' WHERE id=1;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":2,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":800,"options":[]}]' WHERE id=2;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":3,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":1200,"options":[]}]' WHERE id=3;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":5,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":2000,"options":[]}]' WHERE id=4;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":7,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":3000,"options":[]}]' WHERE id=5;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":10,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":5000,"options":[]}]' WHERE id=6;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":15,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":8000,"options":[]}]' WHERE id=7;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":25,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":12000,"options":[]}]' WHERE id=8;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":35,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":16000,"options":[]}]' WHERE id=9;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":45,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":20000,"options":[]}]' WHERE id=10;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":55,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":25000,"options":[]}]' WHERE id=11;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":70,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":32000,"options":[]}]' WHERE id=12;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":100,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":50000,"options":[]}]' WHERE id=13;
UPDATE moc_nap SET detail='[{"temp_id":457,"quantity":150,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":80000,"options":[]}]' WHERE id=14;

-- Đua top nạp: keep prestige items but drastically reduce tradeable currency injection.
UPDATE moc_nap_top SET detail='[{"temp_id":457,"quantity":80,"options":[]},{"temp_id":1704,"quantity":1,"options":[]},{"temp_id":1632,"quantity":1,"options":[{"id":50,"param":25},{"id":77,"param":25},{"id":103,"param":25},{"id":5,"param":10}]}]' WHERE id=1;
UPDATE moc_nap_top SET detail='[{"temp_id":457,"quantity":60,"options":[]},{"temp_id":1704,"quantity":1,"options":[]},{"temp_id":1632,"quantity":1,"options":[{"id":50,"param":22},{"id":77,"param":22},{"id":103,"param":22},{"id":5,"param":8}]}]' WHERE id=2;
UPDATE moc_nap_top SET detail='[{"temp_id":457,"quantity":40,"options":[]},{"temp_id":1703,"quantity":1,"options":[]},{"temp_id":1632,"quantity":1,"options":[{"id":50,"param":18},{"id":77,"param":18},{"id":103,"param":18},{"id":5,"param":6}]}]' WHERE id=3;
UPDATE moc_nap_top SET detail='[{"temp_id":457,"quantity":25,"options":[]},{"temp_id":1703,"quantity":1,"options":[]}]' WHERE id=4;
UPDATE moc_nap_top SET detail='[{"temp_id":457,"quantity":15,"options":[]}]' WHERE id BETWEEN 5 AND 10;

SELECT 'moc_nap' AS tbl, id, detail FROM moc_nap ORDER BY id;
SELECT 'moc_nap_top' AS tbl, id, detail FROM moc_nap_top ORDER BY id;
