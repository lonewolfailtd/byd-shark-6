#!/bin/bash
# Lonewolf Shark installer for Mac. Double click this file. It finds the ute on your wifi, installs the app
# and sets the permissions it needs. Nothing on the ute is modified apart from installing the app.
cd "$(dirname "$0")"
ADB="./platform-tools/adb"
APK=$(ls *.apk 2>/dev/null | head -1)
PKG="nz.lonewolf.shark"
say() { printf "\n\033[36m%s\033[0m\n" "$1"; }
fail() { printf "\n\033[31m%s\033[0m\nPress Enter to close.\n" "$1"; read -r; exit 1; }

clear
printf "  \033[36mLONEWOLF SHARK\033[0m\n  Installer for the BYD Shark 6\n\n"
cat <<'EOT'
  Before you start:
   1. Turn your phone hotspot on and connect BOTH this computer and the ute to it.
      (Home wifi often blocks this. The hotspot works every time.)
   2. In the ute: Car > System > Version. Tap the words 'Factory Reset' ten times.
      Turn the screen to portrait and tap CONNECT USB TO ENABLE DEBUGGING MODE.
   3. Keep the ute switched on until this finishes.

  Press Enter when the ute shows debugging is on.
EOT
read -r
[ -x "$ADB" ] || chmod +x "$ADB" 2>/dev/null
[ -x "$ADB" ] || fail "The platform-tools folder is missing. Unzip the whole download, not just this file."
[ -n "$APK" ] || fail "No app file (.apk) found next to this installer."
xattr -dr com.apple.quarantine ./platform-tools 2>/dev/null

say "Looking for the ute on your network..."
"$ADB" kill-server >/dev/null 2>&1; "$ADB" start-server >/dev/null 2>&1
FOUND=""
for base in $(ifconfig | awk '/inet / && $2 !~ /^127/ {print $2}' | awk -F. '{print $1"."$2"."$3}' | sort -u); do
  for i in $(seq 1 254); do
    ( nc -z -G 1 "$base.$i" 5555 >/dev/null 2>&1 && echo "$base.$i" ) &
  done
  wait
done > /tmp/shark-hits.txt
for ip in $(cat /tmp/shark-hits.txt); do
  if "$ADB" connect "$ip:5555" 2>&1 | grep -q connected; then
    if "$ADB" -s "$ip:5555" shell getprop ro.product.model 2>/dev/null | grep -qi byd; then FOUND="$ip"; break; fi
    "$ADB" disconnect "$ip:5555" >/dev/null 2>&1
  fi
done
if [ -z "$FOUND" ]; then
  printf "\n  Could not find the ute. Type the IP address shown on the ute's debugging screen (or leave blank to give up): "
  read -r typed
  [ -n "$typed" ] || fail "Nothing installed. Check both devices are on the hotspot and debugging is on."
  "$ADB" connect "$typed:5555" 2>&1 | grep -q connected || fail "Could not connect to $typed."
  FOUND="$typed"
fi
DEV="$FOUND:5555"
say "Found the ute at $FOUND. Installing..."
"$ADB" -s "$DEV" install -r -g "$APK" 2>&1 | grep -q Success || fail "Install failed."

say "Setting permissions..."
for g in WRITE_SECURE_SETTINGS READ_LOGS SYSTEM_ALERT_WINDOW ACCESS_FINE_LOCATION READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do "$ADB" -s "$DEV" shell pm grant $PKG android.permission.$g >/dev/null 2>&1; done
for g in ADAS_COMMON ADAS_GET ADAS_SET AC_COMMON AC_GET AC_SET SETTING_COMMON SETTING_GET SETTING_SET LIGHT_COMMON LIGHT_GET LIGHT_SET BODYWORK_COMMON BODYWORK_GET SENSOR_COMMON SENSOR_GET TYRE_COMMON TYRE_GET STATISTIC_COMMON STATISTIC_GET SPEED_COMMON SPEED_GET GEARBOX_COMMON GEARBOX_GET ENGINE_COMMON ENGINE_GET CHARGING_COMMON CHARGING_GET ENERGY_COMMON ENERGY_GET INSTRUMENT_COMMON INSTRUMENT_GET VEHICLEHEALTH_COMMON VEHICLEHEALTH_GET OTA_COMMON OTA_GET COLLECTDATA_COMMON COLLECTDATA_GET; do
  "$ADB" -s "$DEV" shell pm grant $PKG android.permission.BYDAUTO_$g >/dev/null 2>&1
done
"$ADB" -s "$DEV" shell settings put global hidden_api_policy 1 >/dev/null 2>&1
"$ADB" -s "$DEV" shell appops set $PKG SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1
"$ADB" -s "$DEV" shell settings put system pointer_location 0 >/dev/null 2>&1
"$ADB" -s "$DEV" shell settings put system show_touches 0 >/dev/null 2>&1

say "Opening the app on the ute..."
"$ADB" -s "$DEV" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
printf "\n\033[32m  Done. Lonewolf Shark is on the ute.\033[0m\n"
echo "  Debugging switches itself off when the ute restarts. That is normal and the app keeps working."
echo "  To update later, run this installer again."
printf "\n  Press Enter to close.\n"; read -r
