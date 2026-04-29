import json, sys, re

# Read shop data
with open(r"C:\xampp\tmp\shop1_raw.txt", "r", encoding="utf-8-sig") as f:
    raw = f.read().strip()

# Replace \r\n and \n in the raw data string (escaped in MySQL output)
raw = raw.replace("\\r\\n", "").replace("\\n", "").replace("\r\n", "").replace("\n", "")

try:
    data = json.loads(raw)
except json.JSONDecodeError as e:
    print(f"JSON parse error: {e}")
    print(f"Data preview (first 500 chars): {raw[:500]}")
    sys.exit(1)

changes = []
for tab_idx, tab in enumerate(data):
    for item in tab:
        # item format: [itemId, tabId, typeSell, quantity, isNew, ?, ?, ?, ?, gem_cost, ?, ?, gold_cost, ...]
        item_id = item[0]
        tab_id = item[1]
        gem_cost = item[9]
        
        if gem_cost > 0:
            old_price = gem_cost
            new_price = gem_cost * 10
            item[9] = new_price
            changes.append(f"Item {item_id} (tab {tab_id}): {old_price} -> {new_price} gem")

# Output changes
for c in changes:
    print(c)

# Write updated data
result = json.dumps(data, ensure_ascii=False)

with open(r"C:\xampp\tmp\shop1_updated.txt", "w", encoding="utf-8") as f:
    f.write(result)

print(f"\nTotal items updated: {len(changes)}")
print("Updated data saved to C:\\xampp\\tmp\\shop1_updated.txt")
