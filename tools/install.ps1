# Install or update Lonewolf Shark on the ute over WiFi ADB and grant it what it needs.
# Usage: powershell -ExecutionPolicy Bypass -File tools\install.ps1 -Ip 192.168.1.xx [-Apk path\to\app-debug.apk]
param(
  [Parameter(Mandatory=$true)][string]$Ip,
  [string]$Apk = "",
  [string]$Adb = "$env:USERPROFILE\Downloads\platform-tools-latest-windows\platform-tools\adb.exe",
  [string]$Pkg = "nz.lonewolf.shark"
)
$ErrorActionPreference = "Continue"
if (-not $Apk) { $Apk = Join-Path (Split-Path -Parent $PSCommandPath) "..\app\app\build\outputs\apk\debug\app-debug.apk" }
if (-not (Test-Path $Apk)) { Write-Error "APK not found at $Apk. Build first: cd app; .\gradlew.bat assembleDebug"; exit 1 }

& $Adb connect "$Ip`:5555" | Out-Null
$model = (& $Adb -s "$Ip`:5555" shell getprop ro.product.model 2>$null).Trim()
if ($model -ne "BYD AUTO") { Write-Error "Target reports '$model', expected 'BYD AUTO'. Not installing."; exit 1 }
Write-Host "Target verified: $model"

& $Adb -s "$Ip`:5555" install -r -g $Apk
if ($LASTEXITCODE -ne 0) { Write-Error "install failed"; exit 1 }

$grants = @(
  "pm grant $Pkg android.permission.WRITE_SECURE_SETTINGS",
  "pm grant $Pkg android.permission.READ_LOGS",
  "pm grant $Pkg android.permission.SYSTEM_ALERT_WINDOW",
  "pm grant $Pkg android.permission.ACCESS_FINE_LOCATION",
  "settings put global hidden_api_policy 1",
  "settings put global hidden_api_blacklist_exemptions 'Lcom/ts/,Ldalvik/system/'",
  "appops set $Pkg SYSTEM_ALERT_WINDOW allow",
  "appops set $Pkg START_ACTIVITIES_FROM_BACKGROUND allow",
  "appops set $Pkg RUN_IN_BACKGROUND allow",
  "appops set $Pkg RUN_ANY_IN_BACKGROUND allow",
  "appops set $Pkg WAKE_LOCK allow",
  "dumpsys deviceidle whitelist +$Pkg"
)
$byd = "AC_COMMON","AC_GET","AC_SET","SETTING_COMMON","SETTING_GET","SETTING_SET","LIGHT_COMMON","LIGHT_GET","LIGHT_SET",
  "BODYWORK_COMMON","BODYWORK_GET","SENSOR_COMMON","SENSOR_GET","TYRE_COMMON","TYRE_GET","STATISTIC_COMMON","STATISTIC_GET",
  "SPEED_COMMON","SPEED_GET","GEARBOX_COMMON","GEARBOX_GET","ENGINE_COMMON","ENGINE_GET","CHARGING_COMMON","CHARGING_GET",
  "ENERGY_COMMON","ENERGY_GET","INSTRUMENT_COMMON","INSTRUMENT_GET","VEHICLEHEALTH_COMMON","VEHICLEHEALTH_GET",
  "OTA_COMMON","OTA_GET","COLLECTDATA_COMMON","COLLECTDATA_GET"
foreach ($p in $byd) { $grants += "pm grant $Pkg android.permission.BYDAUTO_$p" }

foreach ($g in $grants) {
  $out = (& $Adb -s "$Ip`:5555" shell $g 2>&1 | Out-String).Trim()
  if ($out) { Write-Host ("  {0} -> {1}" -f $g, $out.Split("`n")[0]) }
}

# Add to the BYD floating app list so the floating icon is allowed over other apps.
$list = (& $Adb -s "$Ip`:5555" shell settings get global byd_float_app_list 2>$null).Trim()
if ($list -notlike "*$Pkg*") {
  $new = if ($list -and $list -ne "null") { "$list,$Pkg" } else { $Pkg }
  & $Adb -s "$Ip`:5555" shell "settings put global byd_float_app_list '$new'" | Out-Null
}

& $Adb -s "$Ip`:5555" shell monkey -p $Pkg -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "Installed and launched $Pkg. Open the app and press Start vehicle link."
