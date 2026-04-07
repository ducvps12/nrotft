$folder = "C:\Users\Administrator\Pictures\ThoiDaiNgocRong"
$oldStr = "103.78.0.30"
$newStr = "nrobest.com"

$oldBytes = [System.Text.Encoding]::ASCII.GetBytes($oldStr)
$newBytes = [System.Text.Encoding]::ASCII.GetBytes($newStr)

$files = Get-ChildItem -Path $folder -Recurse -File -Include "globalgamemanagers", "*.dat", "*.dll", "*.assets"

foreach ($file in $files) {
    if ($file.Length -gt 200000000) { continue } # skip very large files (e.g > 200MB) just in case
    
    try {
        [byte[]]$bytes = [System.IO.File]::ReadAllBytes($file.FullName)
        $modified = $false
        
        for ($i = 0; $i -le ($bytes.Length - $oldBytes.Length); $i++) {
            $match = $true
            # Check ASCII
            for ($j = 0; $j -lt $oldBytes.Length; $j++) {
                if ($bytes[$i + $j] -ne $oldBytes[$j]) {
                    $match = $false
                    break
                }
            }
            if ($match) {
                Write-Host "Found ASCII '$oldStr' at offset $i in $($file.FullName)"
                for ($k = 0; $k -lt $newBytes.Length; $k++) {
                    $bytes[$i + $k] = $newBytes[$k]
                }
                $modified = $true
                $i += $oldBytes.Length - 1
            }
            
            # Also could do UTF-16LE check (each char followed by 0x00), but standard Unity Il2cpp strings are UTF-8.
        }
        
        if ($modified) {
            Write-Host "Saving modified file $($file.FullName)"
            [System.IO.File]::WriteAllBytes($file.FullName, $bytes)
        }
    } catch {
        Write-Host "Failed to process $($file.FullName): $_"
    }
}
Write-Host "Patching script finished."
