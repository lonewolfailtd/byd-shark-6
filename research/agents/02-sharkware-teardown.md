# Agent report 02: how Sharkware works and how apps drive the Shark 6 head unit

Researched 18 Sep 2026. [C] confirmed by a cited page or Sharkware's own screenshots. [I] inferred.

## 1. Sharkware: what is public

### 1.1 Product line and pricing [C] (https://www.sharkwarehub.com/)

| Product | AUD | What it is |
|---|---|---|
| Shark Hub | 48.99 | Touchscreen app: climate, heated/ventilated seats, seat comfort memory on startup, app shortcuts, Off Road page (tilt/roll). Includes Shark Transfer. |
| Shark Control | 72.99 | Hub plus Gauges and Towing pages, button/dial assignments with display feedback, lighting zones with night brightness limits, automatic Trigger connection, floating icon, themes, "in app app and helper update checks". |
| Shark Transfer | included | Mac/Windows installer and updater. |
| Shark Transfer+ | 34.99 | Same tool but sideloads any APK. |
| Shark Trigger | 316.99 hardware; 336.99 Hub upgrade bundle; 371.99 Control bundle | BLE button bar. Orders paused until 23 September. Premium only; Performance "testing ongoing". |

Site: "Shark Trigger uses the custom BLE chip... Generic or look alike button units are not compatible." "Not affiliated with BYD." "Future BYD updates may affect or restrict functionality."

### 1.2 Installation mechanism: ADB over WiFi [C] (https://www.sharkwarehub.com/install.html)

- Computer and Shark on the same WiFi, Shark awake, ADB enabled.
- Enabling ADB: Car > System > Version, tap the FACTORY RESET text 10 times, Premium: rotate the screen vertically to expose hidden buttons, press CONNECT USB TO ENABLE DEBUGGING MODE, wait for "Turn on ADB debugging!". Performance (fixed screen): use Multi window / Split screen so the developer menu is resized and "forces it to redraw and reveals the hidden top debugging controls". Same trick as https://github.com/MorghusDragon/BYD-Shark-Sideloading.
- Shark Transfer is a .bat plus bundled platform-tools plus APKs on Windows, a .app on Mac. It "starts its bundled ADB, searches the local network, verifies the target as BYD AUTO and installs/updates". Log lines: ADB_READY, FILE_VERIFICATION=PASS, ADB_SERVER=PASS, BYD_AUTO_VERIFY=PASS, SHARKWARE V5.88 INSTALLATION PASSED. So: `adb connect <ip>` plus `adb install -r`, wrapped.
- Licensing: first run shows an "Activation ID for the vehicle"; enter a licence code; "The licence is linked to that Shark". Updates install over the top. There was a "legacy" app and now a "unified Sharkware app" (UNIFIED v7.56 on the downloads page).

### 1.3 Helper services and the Trigger connection [C] (diagnostics.html, diagnostic-guide.html)

- The diagnostic tool "replaces older compatible versions of Sharkware and its two hidden helpers, and saves the selected address". Three packages: visible app plus two hidden helper APKs. "CONNECTED means the helper reports that the selected controller is ready." [I] one helper is the BLE bridge service that owns the GATT connection; the other is a background/keep alive or vehicle API service.
- The Trigger advertises over BLE as "DiKey". Users scan with BLE Hero on a phone, note the MAC; the tool stores it on the head unit so the helper connects by address. Phone Bluetooth must be off afterwards (single connection box).
- "Tested with our BYD AUTO Android 11 Shark and controller." Trigger is USB powered.
- A 15 Sept 2026 browser install test APK is posted: they are testing whether an APK downloaded in the Shark's browser shows an install prompt, i.e. an ADB free path. "Installation on the Shark is not yet confirmed."

### 1.4 What each page shows (7.04 screenshots, 13 to 14 Sept 2026) [C]

Screenshots saved in research/sharkware-scrape/screenshots/.

- Climate: per seat HEAT I/II and COOL I/II for passenger and driver, SYNC WITH AIR, SEATS ON, driver and passenger temp with +/- (23 and LO), fan arc 0 to 7 with OFF, five vent mode buttons, AUTO / A/C / REAR / DUAL / RECIRC, presets BOOST, FAN CYCLE, CYCLE, WINTER, SUMMER, MY MODE 1/2, a row of app shortcut icons, status pills TRIGGER CONNECTED and VEHICLE CONNECTED. The stock BYD bottom bar remains visible and mirrors the app's changes (23°C, Lo, fan OFF), proving real HVAC control.
- Off Road: PITCH and ROLL dials (1.2° / 0.9°), SENSITIVITY: SMOOTH, WADING: OFF, RAGE MODE shortcut. [I] pitch/roll from the head unit accelerometer with smoothing; Wading and Rage Mode are shortcuts into BYD's own screens.
- Gauges (Control only): 12V BATTERY (10 to 16 V), HV BATTERY, RANGE, FUEL REMAINING, FUEL LEVEL, VEHICLE SPEED (0 to 240), TOTAL MILEAGE, TRIP DISTANCE, EV RANGE, ENGINE RPM · COLLECTION (0 to 8000), TYRE PRESSURE, OUTSIDE TEMP, FUEL · LAST 50 KM, DRIVE MODE, plus a NOTICE SOMETHING NOT RIGHT? button. Every reading showed Unavailable in their own capture.
- Towing (Control only): TRAILER MODE Small/Medium/Large captioned "Trailer mode off · illustration" (a visual, not a vehicle mode switch), then live COMBINED RANGE 240 km, FUEL RANGE 204 km, EV RANGE 36 km, BATTERY 54.0%, tyres FL/FR/RL/RR in psi (37.9/37.9/37.9/38.4). The same data the Gauges page could not read was live here.
- Physical Keys: 10 buttons each with separate UP and DOWN actions, two knobs (left display 23°C, right 17°C) with Rotate / Press / Long press (e.g. Rotate: Driver Temperature, Press: Climate · Power, Long press: Open Sharkware).
- Ambient Lighting: 5 zones (LEFT BAR, CENTRE BAR, OUTSIDE BAR, SIDE LIGHTS, ALL), RGB wheel with numeric R/G/B, brightness, separate BUTTON BACKLIGHTS for all ten buttons, EFFECTS, "Brightness when headlights are on · Bars 4% · Buttons 57%".
- Settings: floating icon toggle, "Edition: Shark Control · Vehicle: Premium", theme "Auto follows the vehicle drive mode" (reads drive mode live), home app shortcut slots.

### 1.5 Firmware, reboot, car on/off

- Sharkware says tested with V2.2.2 and the previous OTA [C].
- A Shark 6 Premium owner on 2.2.1 confirms the factory reset tap trick and wireless ADB still work (https://github.com/yash-srivastava/Overdrive-release/issues/258). Another Shark owner reports DiLink 56.1.2.2608110.1, hidden menu reachable but TCP 5555 stayed closed, USB APK install shows "For driving safety do not install third party apps", and the dialler code *#91532547#* does nothing on the Shark.
- "Seat comfort memory on startup" and TRIGGER CONNECTED imply the app runs at every ignition. [I] BYD firmware blocks third party autostart, so the hidden helpers are the workaround. Car must be on (ACC) for install and Trigger use.

### 1.6 Community coverage

- Facebook groups: BYD Shark Owners Club Australia (https://www.facebook.com/groups/bydsharkaustralia/), The Shark Hub (https://www.facebook.com/groups/sharkhub/). Content not fetchable without login.
- YouTube: no Sharkware specific videos surfaced. Sideloading: https://www.youtube.com/watch?v=9kwHxrDEpPQ and Trail Shark https://www.youtube.com/watch?v=oxDPU7E-FZw.
- Whirlpool "BYD Shark 6 PHEV ute" (https://forums.whirlpool.net.au/thread/95pnrppl): no Sharkware mentions in pages 1 to 8.

## 2. Shark Trigger hardware

[C] Product photo shows a black bar with 10 rocker buttons, two round knob displays ("18°C" and "12 VOL"), 5 RGB light bar segments (Zones 1 to 5). BLE name DiKey. USB powered.

[I] This is the Tesla Model 3/Y "physical button docking station" family from Shenzhen: Tesstudio Magic Controller 3.0 (10 programmable buttons plus knob, https://www.tesstudio.com/products/magic-rotating-button-docking-station-tesla-model-3-y-highland-juniper), Tesstudio Bluetooth Commander, TESMAG Smart Physical Button Controller with Rotary Climate Knob (https://www.teslaacessories.com/products/smart-physical-button-controller-with-rotary-climate-knob-for-tesla-model-3-y). In the Tesla world those pair over BLE to a phone app (Ctrl-Bar works that way).

[I] Why "only our custom BLE chip works": retail units run undocumented vendor GATT firmware often bonded to the vendor's app. Sharkware most plausibly commissioned a fixed documented GATT service (writable chars for the two knob displays, 5 RGB zones, 10 backlights; notify char for button up/down, knob rotate/press/long press). See report 03: DiKey is a real BYD console controller, and https://github.com/sp-hy/Open-DiKey has the protocol (service FF10, chars FF11/FF12, frames AA 55 | len | cmd | payload | sum).

## 3. How apps read and command the vehicle: the BYD AutoSDK

### 3.1 API surface [C]

- Namespace android.hardware.bydauto.*, singletons via XxxDevice.getInstance(Context), in framework.jar. Backed by libbydauto.so and native android.gui.BYDAutoServer talking to the vehicle MCU/CAN. Sources: https://github.com/homeo26/byd-dashvoice, https://github.com/wheregoes/byd-apps.
- Official SDK: BYD open platform https://oip.byd.com/ requires registration and real name ID; SDK is Android 7.1.2 based with a modified android.jar. 253 interface list: https://blog.csdn.net/ccagy/article/details/105005081. Per class series: https://blog.csdn.net/shangxianyue5670/article/details/84548323 (Bodywork), .../84567212 (Energy), .../84635653 (Charging), .../84749936 (Settings).
- Classes: BYDAutoBodyworkDevice (getAutoVIN, getDoorState, getWindowState, getBatteryVoltageLevel, getPowerLevel, getSteeringWheelValue, getFuelElecLowPower, getAlarmState, setBodyWindowCtrlState, setMoonRoofState), BYDAutoStatisticDevice (getElecPercentageValue, getFuelPercentageValue, getElecDrivingRangeValue, getFuelDrivingRangeValue, getTotalMileageValue, getLastFuelConPHMValue, getLastElecConPHMValue, getTotalFuelConValue, getDrivingTimeValue, getKeyBatteryLevel, getEVMileageValue), BYDAutoSpeedDevice, BYDAutoEnergyDevice (getEnergyMode, getOperationMode, getRoadSurfaceMode, getPowerGenerationValue), BYDAutoGearboxDevice, BYDAutoEngineDevice (RPM, fuel), BYDAutoSensorDevice, BYDAutoRadarDevice, BYDAutoTyreDevice (getTyreAirLeakState, pressures), BYDAutoPanoramaDevice, BYDAutoAcDevice, BYDAutoChargingDevice, BYDAutoSettingDevice, BYDAutoDoorLockDevice, BYDAutoMqttDevice. Every device has registerListener(AbsBYDAutoXxxListener) for push updates.
- Gauge page mapping [I]: 12V = Bodywork.getBatteryVoltageLevel; HV %, EV range, fuel %, fuel range, total mileage, fuel last N km = Statistic; speed = Speed.getCurrentSpeed; RPM = Engine; tyres = Tyre; outside temp = Ac.getTemprature(4); drive mode = Energy.getEnergyMode/getOperationMode.

### 3.2 AC and seats, concretely [C]

From byd-apps (Dolphin, DiLink 3.0) live tests:
- BYDAutoAcDevice.start(0)/stop(0) work; setAcTemperature(1, tempC, 1, 1) works; setAcWindMode(mode,1), setAcCycleMode(mode, 0|1), setAcControlMode(mode,1) work; setAcWindLevel is broken and fan is set via set(1000, 0x1DE0000C, level).
- Constants: fan 0 to 7, temp 17 to 33 °C, zones 1 main / 2 deputy / 3 rear / 4 outside, wind modes FACE=1, FOOT=5, DEFROST=0, cycle OUTLOOP=1 / INLOOP=0.
- On Di 3.0 (Sealion 6 DM-i) auto mode requires writing the pair AC_CTRL_MODE_SET 0x1DE00018 + AC_CTRL_SOURCE_SET 0x1DE00015 via set(int,int[],int[]) (https://github.com/yash-srivastava/Overdrive-release/pull/286).
- Seats: BYDAutoSettingDevice.setSeatVentilatingState(seatId, state) / setSeatHeatingState(seatId, state), 1 based seat IDs, state 1 off, 2 level 1, 3 level 2; heating and ventilation mutually exclusive in the HAL (https://github.com/dangkhoi/byd-kachi). Almost certainly the exact call behind Sharkware HEAT I/II and COOL I/II; "seat comfort memory" is re issuing the saved state a few seconds after startup.
- Trap: getSystemService("airconditioning") returns an inert AirConditioningManager; only the bydauto classes reach the MCU.

### 3.3 Permissions [C]

- Three tiers per device: BYDAUTO_<DEV>_COMMON (dangerous, runtime), _GET and _SET (signature|privileged, checked inside the framework class in the calling process). Community projects use a ContextWrapper (BydPermissionContext) whose checkPermission returns GRANTED for BYDAUTO_*; the device object then passes its own client side checks. Shizuku and LSPatch do not help.
- Exceptions: Panorama GET is enforced server side; camera needs bmmcamera.jar which is off the boot classpath. Window control on some cars is refused by a body interlock (getWindowPermitState=0).
- [I] Sharkware's Unavailable gauges alongside a working Towing page suggest different devices; some GET calls may be server side enforced or return 65535, rendered as Unavailable.

### 3.4 Head unit generations [C]

- DiLink 3.0 global (Atto 3, Dolphin, Seal): Android 10, QCM6125, bydauto reflection works, USB "Third Party Apps 55" folder with password BYD6125F or wireless ADB.
- Chinese Di2.1H / 4.0: Android 9, MT6765.
- DiLink 5.0: Android 12, Snapdragon 778G, "Fission" isolation, Magisk root blocked; byd-apps incompatible; OverDrive switched to native sidecar injection; BH4GMI cluster mirror relies on ADB loopback via sys.connect.adb.wiress=1 and shell uid 2000 (https://github.com/BH4GMI/byd-dashboard).
- Shark 6: Android 11 (Sharkware), build 56.1.2.2608110.1, SELinux enforcing, /data/local/tmp not writable even by shell, Qualcomm camera stack (qcarcam_test, AIS via linker64), OverDrive daemons killed frequently, but battery/fuel and all 5 cameras work (issue 258). Owner facing OTA names V2.1.x/V2.2.x.

### 3.5 Surviving reboot / autostart [C]

- dashvoice: firmware patches ActivityManager with a vendor self start filter that refuses to start a third party process not already running; app must be opened once per boot.
- OverDrive: needs the "Disable Autostart" toggle unchecked, uses a daemon plus watchdog, needs wireless ADB always on; on Shark 6 SELinux blocks those script drops.
- Kachi: self grants notification listener and SYSTEM_ALERT_WINDOW, appends packages to the global byd_float_app_list setting, all via an on device dadb loopback ADB client (no laptop), and self updates through the same loopback.
- [I] Sharkware's two hidden helpers plus floating icon = same recipe: one helper is a foreground service registered for boot/ACC broadcasts; the floating icon needs SYSTEM_ALERT_WINDOW plus the byd_float_app_list entry, both grantable from Shark Transfer while ADB is connected (appops set, settings put global byd_float_app_list, cmd notification allow_listener).

## 4. Other community apps [C]

- OverDrive (sentry/dashcam, DiLink 3/4/5, Shark 6 port in progress): reads gear, drive mode, speed, SOC/SOH, range, cabin temp, AC, doors/windows/locks, 12V, charging power, tyre pressures, radar; controls AC, windows, seat heating/cooling. https://github.com/yash-srivastava/Overdrive-release, https://www.overdrive.qd.je/
- byd-apps (door sounds, engine sound via AVAS 0xAA000104, pet mode, byd-probe enumerator, byd_sdk.dex stubs). https://github.com/wheregoes/byd-apps
- byd-dashvoice (offline voice climate). https://github.com/homeo26/byd-dashvoice
- byd-kachi / ClusterNav 2.0 (nav on cluster via feature 0x4C10E015, HUD flag 0x38B00030, seat comfort, PM2.5). https://github.com/dangkhoi/byd-kachi
- BYDLauncher (Korean launcher; window %, eco coach; DiLink 3 and 5). https://github.com/GeyuongGongPark/BYDLauncher
- byd-dashboard (DiLink 5 second screen mirror). https://github.com/BH4GMI/byd-dashboard
- Cloud route: hass-byd-vehicle / byd-hass. https://github.com/jkaberg/hass-byd-vehicle, https://github.com/jkaberg/byd-hass
- Sideloading guides: https://github.com/MorghusDragon/BYD-Shark-Sideloading, https://github.com/ahmada3mar/BYD, and this repo.

## 5. OBD / CAN [C unless marked]

- Standard 16 pin OBD2 port under the driver's dash; generic ELM327 reads engine DTCs and standard live data, no BMS data; OBDLink MX+ with Torque/ABRP gets partial extended data (https://www.bydaccessories.store/blogs/news/what-obd2-scanner-work-with-byd).
- Known BYD PIDs (Atto 3 / Dolphin, header 7E7, init ATSH7E7;ATFCSH7E7;ATFCSD300000;ATFCSM1): SOC 221FFC = ((B5*256)+B4)/100, battery temp 220032 = B4-40, total charged kWh 220011, discharged 220012, cycles 22000B, pack voltage 220008, current 220009; repo archived Feb 2026 with no Shark 6 / DM-O PIDs (https://github.com/loryanstrant/BYD-PID-list). WiCAN Pro to Home Assistant is the usual telemetry route.
- Apps never see a raw CAN socket; the path is framework.jar bydauto class > binder > BYDAutoServer > MCU > CAN. [I] The bydauto route is richer and free; OBD is only worth it for BMS cell level data the SDK does not expose.

## 6. Build recipe implied [I]

1. Plain APK, minSdk 29/30, no platform signature. Obtain devices by reflection through a ContextWrapper that fakes BYDAUTO_* grants; request *_COMMON runtime permissions; fall back to generic set(devType, featureId, value) when named setters no op.
2. Climate: BYDAutoAcDevice as in 3.2; seats: BYDAutoSettingDevice seat methods; data: Statistic/Speed/Engine/Tyre/Bodywork/Energy getters plus listeners; pitch/roll: head unit accelerometer.
3. Installer: bundle platform-tools, adb connect, adb install -r, then grant overlay/notification/float list over the same session; a foreground service to hold BLE and re apply seat memory on ACC.
4. Buttons: any BLE peripheral we control; GATT notify for inputs, write chars for displays and RGB; connect by stored MAC from a helper service.
5. Shark 6 specifics: Android 11, SELinux enforcing, /data/local/tmp unwritable, no dialler code, enable ADB via factory reset tap plus rotate (Premium) or split screen redraw (Performance); test on V2.2.x.
