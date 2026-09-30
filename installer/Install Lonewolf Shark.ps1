# Lonewolf Shark installer for Windows.
# Right click this file and choose "Run with PowerShell". It finds the ute on your wifi, installs the app
# and sets the permissions it needs. Nothing on the ute is modified apart from installing the app.

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb = Join-Path $here "platform-tools\adb.exe"
$apk = Get-ChildItem $here -Filter "*.apk" | Select-Object -First 1
$pkg = "nz.lonewolf.shark"

function Say($t) { Write-Host ""; Write-Host $t -ForegroundColor Cyan }
function Fail($t) { Write-Host ""; Write-Host $t -ForegroundColor Red; Write-Host "Press Enter to close."; Read-Host | Out-Null; exit 1 }

Clear-Host
Write-Host "  LONEWOLF SHARK" -ForegroundColor Cyan
Write-Host "  Installer for the BYD Shark 6"
Write-Host ""
Write-Host "  Before you start:"
Write-Host "   1. Turn your phone hotspot on and connect BOTH this computer and the ute to it."
Write-Host "      (Home wifi often blocks this. The hotspot works every time.)"
Write-Host "   2. In the ute: Car > System > Version. Tap the words 'Factory Reset' ten times."
Write-Host "      Turn the screen to portrait and tap CONNECT USB TO ENABLE DEBUGGING MODE."
Write-Host "   3. Keep the ute switched on until this finishes."
Write-Host ""
Write-Host "  Press Enter when the ute shows debugging is on."
Read-Host | Out-Null

if (-not (Test-Path $adb)) { Fail "The platform-tools folder is missing. Unzip the whole download, not just this file." }
if (-not $apk) { Fail "No app file (.apk) found next to this installer." }

Say "Looking for the ute on your network..."
& $adb kill-server 2>$null | Out-Null
& $adb start-server 2>$null | Out-Null
$ips = @()
Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.*" } | ForEach-Object {
    $base = ($_.IPAddress -split "\.")[0..2] -join "."
    $ips += (1..254 | ForEach-Object { "$base.$_" })
}
$found = $null
$jobs = foreach ($ip in $ips) {
    Start-ThreadJob -ThrottleLimit 64 -ScriptBlock {
        param($ip)
        $c = New-Object Net.Sockets.TcpClient
        try { $r = $c.BeginConnect($ip, 5555, $null, $null); if ($r.AsyncWaitHandle.WaitOne(400) -and $c.Connected) { $ip } } catch {} finally { $c.Close() }
    } -ArgumentList $ip
}
$hits = $jobs | Wait-Job -Timeout 60 | Receive-Job
$jobs | Remove-Job -Force
foreach ($ip in $hits) {
    $r = & $adb connect "$ip`:5555" 2>&1
    if ($r -match "connected") {
        $model = & $adb -s "$ip`:5555" shell getprop ro.product.model 2>$null
        if ($model -match "BYD") { $found = $ip; break }
        & $adb disconnect "$ip`:5555" | Out-Null
    }
}
if (-not $found) {
    Write-Host ""
    Write-Host "  Could not find the ute. Type the IP address shown on the ute's debugging screen"
    Write-Host "  (or leave blank to give up):"
    $typed = Read-Host
    if (-not $typed) { Fail "Nothing installed. Check both devices are on the hotspot and debugging is on." }
    $r = & $adb connect "$typed`:5555" 2>&1
    if ($r -notmatch "connected") { Fail "Could not connect to $typed. $r" }
    $found = $typed
}
$dev = "$found`:5555"
Say "Found the ute at $found. Installing..."
$r = & $adb -s $dev install -r -g $apk.FullName 2>&1
if ($r -notmatch "Success") { Fail "Install failed: $r" }

Say "Setting permissions..."
$grants = @("WRITE_SECURE_SETTINGS", "READ_LOGS", "SYSTEM_ALERT_WINDOW", "ACCESS_FINE_LOCATION", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE")
$byd = @("ADAS_COMMON","ADAS_GET","ADAS_SET","AC_COMMON","AC_GET","AC_SET","SETTING_COMMON","SETTING_GET","SETTING_SET","LIGHT_COMMON","LIGHT_GET","LIGHT_SET",
    "BODYWORK_COMMON","BODYWORK_GET","SENSOR_COMMON","SENSOR_GET","TYRE_COMMON","TYRE_GET","STATISTIC_COMMON","STATISTIC_GET","SPEED_COMMON","SPEED_GET",
    "GEARBOX_COMMON","GEARBOX_GET","ENGINE_COMMON","ENGINE_GET","CHARGING_COMMON","CHARGING_GET","ENERGY_COMMON","ENERGY_GET","INSTRUMENT_COMMON","INSTRUMENT_GET",
    "VEHICLEHEALTH_COMMON","VEHICLEHEALTH_GET","OTA_COMMON","OTA_GET","COLLECTDATA_COMMON","COLLECTDATA_GET")
foreach ($g in $grants) { & $adb -s $dev shell pm grant $pkg android.permission.$g 2>$null | Out-Null }
foreach ($g in $byd) { & $adb -s $dev shell pm grant $pkg android.permission.BYDAUTO_$g 2>$null | Out-Null }
& $adb -s $dev shell settings put global hidden_api_policy 1 | Out-Null
& $adb -s $dev shell appops set $pkg SYSTEM_ALERT_WINDOW allow 2>$null | Out-Null
# Tidy up the developer options people turn on by accident.
& $adb -s $dev shell settings put system pointer_location 0 2>$null | Out-Null
& $adb -s $dev shell settings put system show_touches 0 2>$null | Out-Null

Say "Opening the app on the ute..."
& $adb -s $dev shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 2>$null | Out-Null

Write-Host ""
Write-Host "  Done. Lonewolf Shark is on the ute." -ForegroundColor Green
Write-Host "  Debugging switches itself off when the ute restarts. That is normal and the app keeps working."
Write-Host "  To update later, run this installer again."
Write-Host ""
Write-Host "  Press Enter to close."
Read-Host | Out-Null
