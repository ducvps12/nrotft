import json, sys

# Option ID meanings (from game source)
COMBAT_STATS = {50, 77, 103}  # SĐ%, HP%, KI%
SUB_COMBAT = {14, 93, 94, 5}  # CM%, Giáp%, Giảm ST%, Giảm dame%
NON_COMBAT = {3, 4, 8, 9, 19, 24, 25, 26, 29, 30, 38, 73, 100, 106, 114, 145, 146, 150, 153, 162}

with open(r"C:\xampp\tmp\shop1_full.txt", "r", encoding="utf-8-sig") as f:
    raw = f.read().strip().replace("\\r\\n", "").replace("\\n", "").replace("\r\n", "").replace("\n", "")

data = json.loads(raw)

# ====================================================================
# REBALANCE: Stats MUST scale with gem price
# 
# Tiers:
#   500 gem    → SĐ 5%
#   2000 gem   → HP 8%  
#   3000 gem   → SĐ 10% (or SĐ 8% + CM 3%)
#   5000 gem   → SĐ 12% + HP 8%
#   10000 gem  → SĐ 15% + HP 10% + KI 10%
#   15000 gem  → SĐ 19% + HP 14% + KI 14%
#   20000 gem  → SĐ 22% + HP 18% + KI 18%
#   30000 gem  → SĐ 25% + HP 20% + KI 20%
#   50000 gem  → SĐ 30% + HP 25% + KI 25%
#   100000 gem → SĐ 35% + HP 30% + KI 30% + CM 5%
# ====================================================================

def get_new_stats(gem_price, had_cm=False):
    """Get balanced stats for a given gem price tier"""
    if gem_price <= 500:
        return {50: 5}
    elif gem_price <= 1500:
        return {50: 8}
    elif gem_price <= 2500:
        return {77: 8}
    elif gem_price <= 3500:
        if had_cm:
            return {50: 8, 14: 3, 94: 5}
        return {50: 10}
    elif gem_price <= 4500:
        return {50: 11, 14: 2}
    elif gem_price <= 7000:
        return {50: 12, 77: 8}
    elif gem_price <= 10500:
        return {50: 15, 77: 10, 103: 10}
    elif gem_price <= 11500:
        return {50: 16, 77: 11, 103: 11}
    elif gem_price <= 13500:
        return {50: 18, 77: 13, 103: 13}
    elif gem_price <= 16500:
        return {50: 19, 77: 14, 103: 14}
    elif gem_price <= 19000:
        return {50: 20, 77: 16, 103: 16}
    elif gem_price <= 25000:
        return {50: 22, 77: 18, 103: 18}
    elif gem_price <= 32000:
        return {50: 25, 77: 20, 103: 20}
    elif gem_price <= 40000:
        return {50: 27, 77: 22, 103: 22}
    elif gem_price <= 60000:
        return {50: 30, 77: 25, 103: 25}
    else:  # 100k+
        return {50: 35, 77: 30, 103: 30, 14: 5}


changes = []

for tab_idx, tab in enumerate(data):
    if tab_idx != 0:  # Only rebalance Tab 0 = Cải trang
        continue
        
    for item in tab:
        item_id = item[0]
        gem = item[9]
        options = item[18] if len(item) > 18 else []
        
        if gem <= 0:
            continue
        
        # Categorize existing options
        has_main_combat = False
        had_cm = False
        special_options = []  # Non-combat options to preserve
        
        for opt in options:
            opt_id = opt[0]
            opt_val = opt[1]
            
            if opt_id in COMBAT_STATS:
                has_main_combat = True
            elif opt_id == 14:  # CM
                had_cm = True
                has_main_combat = True
            elif opt_id in SUB_COMBAT:
                has_main_combat = True
            elif opt_id == 106:
                pass  # Remove expire, don't preserve
            else:
                special_options.append(opt)
        
        if not has_main_combat:
            # Item doesn't have combat stats (e.g., item 524/525 with speed options)
            # Remove expire option only if present
            new_options = [opt for opt in options if opt[0] != 106]
            if len(new_options) != len(options):
                item[18] = new_options
                changes.append(f"  Item {item_id} ({gem} gem): Removed expire only (non-combat item)")
            continue
        
        # Get balanced stats
        new_stats = get_new_stats(gem, had_cm)
        
        # Build new option list: new combat stats + preserved special options
        new_options = []
        for opt_id, opt_val in new_stats.items():
            new_options.append([opt_id, opt_val])
        for opt in special_options:
            new_options.append(opt)
        
        # Record change
        old_combat = {o[0]: o[1] for o in options if o[0] in COMBAT_STATS or o[0] in SUB_COMBAT or o[0] == 14}
        old_total = sum(v for k, v in old_combat.items() if k in COMBAT_STATS)
        new_total = sum(v for k, v in new_stats.items() if k in COMBAT_STATS)
        
        old_str = ", ".join([f"{k}={v}" for k, v in old_combat.items()])
        new_str = ", ".join([f"{k}={v}" for k, v in new_stats.items()])
        
        direction = "↑ BUFF" if new_total > old_total else ("↓ NERF" if new_total < old_total else "= SAME")
        
        changes.append(f"Item {item_id:>4} | {gem:>6} gem | [{old_total:>3}→{new_total:>3}] {direction} | OLD: {old_str} → NEW: {new_str}")
        
        # Apply
        item[18] = new_options

# Also remove expire from all remaining
for tab_idx, tab in enumerate(data):
    for item in tab:
        options = item[18] if len(item) > 18 else []
        new_options = [opt for opt in options if opt[0] != 106]
        if len(new_options) != len(options):
            item[18] = new_options

# Print changes
print("=" * 130)
print("REBALANCE RESULTS")
print("=" * 130)
for c in changes:
    print(c)

# Save
result = json.dumps(data, ensure_ascii=False)
with open(r"C:\xampp\tmp\shop1_rebalanced.txt", "w", encoding="utf-8") as f:
    f.write(result)

# Generate SQL
escaped = result.replace("'", "\\'")
sql = f"UPDATE shop SET arrItemShop = '{escaped}' WHERE shopId = 1;"
with open(r"C:\xampp\tmp\update_shop_balance.sql", "w", encoding="utf-8") as f:
    f.write(sql)

print(f"\nTotal changes: {len(changes)}")
print("SQL saved to C:\\xampp\\tmp\\update_shop_balance.sql")
print("\nKey changes summary:")
print("  500 gem items:   SĐ 5% (was 12-15%)")
print("  3000 gem items:  SĐ 10% (was 12-21%)")
print("  10000 gem items: SĐ 15% + HP 10% + KI 10% (was SĐ 10%)")
print("  20000 gem items: SĐ 22% + HP 18% + KI 18% (standardized)")
print("  50000 gem items: SĐ 30% + HP 25% + KI 25% (was SĐ 20%)")
print("  100K gem items:  SĐ 35% + HP 30% + KI 30% + CM 5% (was SĐ 23%)")
