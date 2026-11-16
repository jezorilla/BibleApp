$target = 'app\src\main\java\com\example\bibliya\MainActivity.kt'
$nl = [char]13 + [char]10
$bytes = [System.IO.File]::ReadAllBytes($target)
$bom = $false
if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) { $bom = $true }
$t = [System.Text.Encoding]::UTF8.GetString($bytes)

function CountOccurrences($haystack, $needle) {
    $c = 0; $start = 0
    while ($true) {
        $idx = $haystack.IndexOf($needle, $start, [StringComparison]::Ordinal)
        if ($idx -lt 0) { break }
        $c++; $start = $idx + $needle.Length
    }
    return $c
}

$edits = @(
    @{ old = ('                    BackHandler(' + $nl + '                        enabled = state.pickerOpen && nav.currentDestination?.route == "picker"' + $nl + '                    ) {' + $nl + '                        nav.popBackStack()' + $nl + '                    }'); new = ('                    BackHandler(' + $nl + '                        enabled = state.pickerOpen && nav.currentDestination?.route == "picker"' + $nl + '                    ) {' + $nl + '                        // Keep the nav back stack and pickerOpen in sync so the' + $nl + '                        // picker stays open while choosing book/chapter and closes on verse.' + $nl + '                        vm.closePicker()' + $nl + '                        nav.popBackStack()' + $nl + '                    }'); label = 'back handler' },
    @{ old = '                            ReaderRoot(state, vm) { nav.navigate("picker") }'; new = '                            ReaderRoot(state, vm, vm::openPicker)'; label = 'open picker wiring' },
    @{ old = ('private fun LoadingScreen() {' + $nl + '    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {' + $nl + '        Column(horizontalAlignment = Alignment.CenterHorizontally) {' + $nl + '            CircularProgressIndicator()' + $nl + '            Spacer(Modifier.height(16.dp))' + $nl + '            Text(stringResource(R.string.loading), fontSize = 18.sp)' + $nl + '        }' + $nl + '    }' + $nl + '}'); new = ('private fun LoadingScreen() {' + $nl + '    Box(' + $nl + '        Modifier' + $nl + '            .fillMaxSize()' + $nl + '            .background(MaterialTheme.colorScheme.background),' + $nl + '        contentAlignment = Alignment.Center,' + $nl + '    ) {' + $nl + '        Column(horizontalAlignment = Alignment.CenterHorizontally) {' + $nl + '            CircularProgressIndicator()' + $nl + '            Spacer(Modifier.height(16.dp))' + $nl + '            Text(' + $nl + '                stringResource(R.string.loading),' + $nl + '                fontSize = 18.sp,' + $nl + '                color = MaterialTheme.colorScheme.onBackground,' + $nl + '            )' + $nl + '        }' + $nl + '    }' + $nl + '}'); label = 'loading screen colors' }
)

foreach ($e in $edits) {
    $n = CountOccurrences $t $e.old
    if ($n -ne 1) { throw ($e.label + ': expected exactly 1 match, got ' + $n) }
    $t = $t.Replace($e.old, $e.new)
    Write-Host ("OK: " + $e.label)
}

if ($bom) { $enc = New-Object System.Text.UTF8Encoding $true } else { $enc = New-Object System.Text.UTF8Encoding $false }
[System.IO.File]::WriteAllText($target, $t, $enc)
Write-Host "Written: $target (BOM preserved: $bom)"