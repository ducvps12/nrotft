import json, sys

# Option ID meanings
OPTION_NAMES = {
    5: "Giảm dame%",
    14: "Chí mạng%",
    30: "Không thể giao dịch",
    50: "Sức đánh%",
    73: "Khóa",
    77: "HP%",
    93: "Giáp%",
    94: "Giảm sát thương%",
    100: "Vĩnh viễn",
    101: "Tăng KN đệ tử%",
    103: "KI%",
    106: "Hạn sử dụng"
}

with open(r"C:\xampp\tmp\shop1_full.txt", "r", encoding="utf-8-sig") as f:
    raw = f.read().strip().replace("\\r\\n", "").replace("\\n", "").replace("\r\n", "").replace("\n", "")

data = json.loads(raw)

print("=" * 120)
print(f"{'Tab':>4} | {'ItemID':>6} | {'Gem':>8} | {'Gold':>10} | {'Options (Stats)':<60} | Notes")
print("=" * 120)

# Tab 0 = Cải trang, Tab 1 = Cửa hàng, Tab 2 = Hỗ trợ
tab_names = ["Cải trang", "Cửa hàng", "Hỗ trợ"]

for tab_idx, tab in enumerate(data):
    tab_name = tab_names[tab_idx] if tab_idx < len(tab_names) else f"Tab {tab_idx}"
    for item in tab:
        item_id = item[0]
        gem = item[9]
        gold = item[12] if len(item) > 12 else 0
        options = item[18] if len(item) > 18 else []
        
        # Format options
        opt_strs = []
        total_pct = 0
        for opt in options:
            opt_id = opt[0]
            opt_val = opt[1]
            name = OPTION_NAMES.get(opt_id, f"Opt{opt_id}")
            opt_strs.append(f"{name}={opt_val}")
            if opt_id in [50, 77, 103]:  # Main combat stats
                total_pct += opt_val
        
        opt_text = ", ".join(opt_strs) if opt_strs else "No options"
        
        # Determine cost type
        cost_str = ""
        if gem > 0:
            cost_str = f"{gem} gem"
        elif gold > 0:
            cost_str = f"{gold} gold"
        
        # Flag balance issues
        notes = ""
        if gem > 0:
            ratio = total_pct / gem * 1000 if gem > 0 else 0
            if total_pct > 0:
                notes = f"Stats/Gem={ratio:.1f}"
                if gem >= 5000 and total_pct < 40:
                    notes += " ⚠️ OVERPRICED"
                elif gem <= 1000 and total_pct > 40:
                    notes += " ⚠️ TOO CHEAP"
        
        print(f"{tab_name:>10} | {item_id:>6} | {gem:>8} | {gold:>10} | {opt_text:<60} | {notes}")

print("\n" + "=" * 120)
print("BALANCE ANALYSIS")
print("=" * 120)

# Collect gem-priced items with combat stats
gem_items = []
for tab_idx, tab in enumerate(data):
    for item in tab:
        item_id = item[0]
        gem = item[9]
        options = item[18] if len(item) > 18 else []
        
        sd = hp = ki = cm = giap = 0
        for opt in options:
            if opt[0] == 50: sd = opt[1]
            elif opt[0] == 77: hp = opt[1]
            elif opt[0] == 103: ki = opt[1]
            elif opt[0] == 14: cm = opt[1]
            elif opt[0] == 93: giap = opt[1]
        
        total = sd + hp + ki
        if gem > 0 and total > 0:
            gem_items.append({
                'id': item_id, 'gem': gem, 'tab': tab_idx,
                'sd': sd, 'hp': hp, 'ki': ki, 'cm': cm, 'giap': giap,
                'total': total, 'ratio': total / gem * 1000
            })

gem_items.sort(key=lambda x: x['gem'])

print(f"\n{'ItemID':>6} | {'Gem':>8} | {'SĐ%':>5} | {'HP%':>5} | {'KI%':>5} | {'CM%':>5} | {'Total':>6} | {'Ratio':>8}")
print("-" * 80)
for item in gem_items:
    flag = ""
    if item['gem'] >= 5000 and item['total'] < 40:
        flag = " ⚠️ WEAK FOR PRICE"
    elif item['gem'] <= 500 and item['total'] > 30:
        flag = " ⚠️ OP FOR PRICE"
    
    print(f"{item['id']:>6} | {item['gem']:>8} | {item['sd']:>5} | {item['hp']:>5} | {item['ki']:>5} | {item['cm']:>5} | {item['total']:>6} | {item['ratio']:>7.1f}{flag}")
