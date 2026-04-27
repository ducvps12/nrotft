
-- Economy hotfix for NROTFT
-- Purpose: disable overpowered public giftcodes that inject too much gold/ruby/items.
-- Run on nrotft database.

UPDATE giftcode
SET active = 0
WHERE code IN (
  'MTDLIVE', 'OPEN2026', 'OPENBETA2026', 'mtv', 'free', 'denbu', 'dbv',
  'codeloantinso1sz', 'fancung', 'coduyen123', 'dnc', 'item', 'ngocrong'
);

-- Keep only small member-card codes active if needed.
SELECT id, code, count_left, type, active, expired, LEFT(detail, 200) AS detail
FROM giftcode
ORDER BY id DESC;
