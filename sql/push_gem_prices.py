import json, sys
import subprocess

# Read updated data
with open(r"C:\xampp\tmp\shop1_updated.txt", "r", encoding="utf-8") as f:
    updated_data = f.read().strip()

# Escape single quotes for SQL
updated_data_sql = updated_data.replace("'", "\\'")

# Update shops 1, 2, 3 (all identical)
for shop_id in [1, 2, 3]:
    sql = f"UPDATE shop SET arrItemShop = '{updated_data_sql}' WHERE shopId = {shop_id};"
    
    # Write SQL to temp file
    sql_file = f"C:\\xampp\\tmp\\update_shop{shop_id}.sql"
    with open(sql_file, "w", encoding="utf-8") as f:
        f.write(sql)
    
    # Execute via mysql
    result = subprocess.run(
        [r"C:\xampp\mysql\bin\mysql.exe", "-u", "root", "-D", "nro_data"],
        input=sql, capture_output=True, text=True, encoding="utf-8"
    )
    
    if result.returncode == 0:
        print(f"Shop {shop_id}: Updated successfully!")
    else:
        print(f"Shop {shop_id}: ERROR - {result.stderr}")

print("\nDone! All 3 shops updated with x10 gem prices.")
print("Restart server or use admin panel 'Reload Shops' to apply.")
