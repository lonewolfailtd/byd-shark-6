# BYD Shark 6 head unit recon. READ ONLY. Dumps device facts we need before writing any app.
# Usage:  powershell -ExecutionPolicy Bypass -File tools\recon.ps1 -Ip 192.168.1.xx
# Prereq: ADB enabled on the ute (Car > System > Version > tap FACTORY RESET text 10x > rotate screen > CONNECT USB TO ENABLE DEBUGGING MODE)
#         and the ute on the same WiFi as this PC.
param(
  [Parameter(Mandatory=$true)][string]$Ip,
  [string]$Adb = "$env:USERPROFILE\Downloads\platform-tools-latest-windows\platform-tools\adb.exe",
  [string]$Out = ""
)

$ErrorActionPreference = "Continue"
if (-not $Out) { $Out = Join-Path (Split-Path -Parent $PSCommandPath) "..\research\device-dump" }
New-Item -ItemType Directory -Force $Out | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$Out = Join-Path $Out $stamp
New-Item -ItemType Directory -Force $Out | Out-Null

& $Adb connect "$Ip`:5555"
& $Adb devices -l

function Dump($name, $cmd) {
  Write-Host "== $name"
  & $Adb shell $cmd 2>&1 | Out-File -Encoding utf8 (Join-Path $Out "$name.txt")
}

Dump "getprop"                 "getprop"
Dump "build-fingerprint"       "getprop ro.build.fingerprint; getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.product.model; getprop ro.hardware; getprop ro.board.platform; getprop ro.build.display.id; getprop ro.build.version.incremental"
Dump "packages-all"            "pm list packages -f"
Dump "packages-byd"            "pm list packages -f | grep -i byd"
Dump "packages-system"         "pm list packages -s"
Dump "packages-3rd"            "pm list packages -3"
Dump "services"                "service list"
Dump "services-byd"            "service list | grep -i -E 'byd|auto|car|vehicle|can|hvac|seat|light'"
Dump "features"                "pm list features"
Dump "permissions-byd"         "pm list permissions -g -f | grep -i -B2 -A6 byd"
Dump "libraries"               "pm list libraries"
Dump "framework-jars"          "ls -la /system/framework /system/etc/permissions /system/etc/sysconfig 2>/dev/null; cat /system/etc/permissions/*.xml 2>/dev/null | grep -i -E 'byd|auto|car'"
Dump "cameras"                 "dumpsys media.camera | head -400"
Dump "sensors"                 "dumpsys sensorservice | head -200"
Dump "bluetooth"               "dumpsys bluetooth_manager | head -300"
Dump "display"                 "wm size; wm density; dumpsys display | grep -i -E 'mDisplayInfo|rotation|DisplayDeviceInfo' | head -20"
Dump "power"                   "dumpsys power | head -150"
Dump "usb"                     "dumpsys usb | head -100"
Dump "net"                     "ip addr; ip route; getprop | grep -i -E 'dhcp|wifi'"
Dump "car-service"             "dumpsys car_service 2>&1 | head -300"
Dump "activity-services"       "dumpsys activity services | grep -i -E 'byd|auto' | head -200"
Dump "launcher"                "cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME"
Dump "kernel"                  "uname -a; cat /proc/version; cat /proc/cpuinfo | head -40; cat /proc/meminfo | head -5; df -h"
Dump "props-vehicle"           "getprop | grep -i -E 'byd|vehicle|car|vin|dilink|version|ota'"
Dump "logcat-sample"           "logcat -d -t 2000"

# Pull the BYD framework jars/APKs so we can decompile the AutoSDK offline (read only copy).
$pull = Join-Path $Out "pulled"
New-Item -ItemType Directory -Force $pull | Out-Null
foreach ($p in @("/system/framework/bydauto.jar","/system/framework/BYDAutoSDK.jar","/system/framework/com.byd.auto.jar","/system/etc/permissions")) {
  & $Adb pull $p $pull 2>&1 | Out-Null
}
& $Adb shell "pm list packages -f | grep -i byd" | ForEach-Object {
  if ($_ -match 'package:(.+\.apk)=') { & $Adb pull $matches[1] $pull 2>&1 | Out-Null }
}

Write-Host "`nDone. Output in $Out"
