$target = 'app\src\main\java\com\example\bibliya\MainActivity.kt'
$nl = [char]13 + [char]10
$bytes = [System.IO.File]::ReadAllBytes($target)
$bom = $false
if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) { $bom = $true }
$t = [System.Text.Encoding]::UTF8.GetString($bytes)

function ReplaceOnce($t, $old, $new, $label) {
    $c = 0; $start = 0
    while ($true) { $idx = $t.IndexOf($old, $start, [StringComparison]::Ordinal); if ($idx -lt 0) { break }; $c++; $start = $idx + $old.Length }
    if ($c -ne 1) { throw ($label + ': expected 1 match, got ' + $c) }
    return $t.Replace($old, $new)
}

$t = ReplaceOnce $t 'import androidx.compose.foundation.layout.Box' ('import androidx.compose.foundation.background' + $nl + 'import androidx.compose.foundation.layout.Box') 'import background'
Write-Host 'OK: import background'
$t = ReplaceOnce $t 'import androidx.compose.material3.CircularProgressIndicator' ('import androidx.compose.material3.CircularProgressIndicator' + $nl + 'import androidx.compose.material3.MaterialTheme') 'import MaterialTheme'
Write-Host 'OK: import MaterialTheme'

if ($bom) { $enc = New-Object System.Text.UTF8Encoding $true } else { $enc = New-Object System.Text.UTF8Encoding $false }
[System.IO.File]::WriteAllText($target, $t, $enc)
Write-Host 'Written'