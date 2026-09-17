# BYD Shark 6 software: master research report

Compiled 18 September 2026 from six parallel research passes (full reports in research/agents/), a scrape of sharkwarehub.com (research/sharkware-scrape/) and the Open DiKey source (research/open-dikey/). This document is the summary and the decisions. The agent reports carry the sources.

## 1. The one paragraph answer

Yes, it is possible, and the community has already proven every piece we need on this exact vehicle. The Shark 6 Premium/Performance head unit is a BYD DiLink 5.0 unit (chipset 51, Qualcomm 8155, Android 11, arm64, device name "BYD AUTO"). A sideloaded Android app can read and control the vehicle through BYD's own in car SDK (android.hardware.bydauto.*) loaded at runtime from the OEM apps, exactly as Open DiKey does today on the Shark 6 for climate, seats and ambient lighting. Sharkware is a closed source app doing the same thing plus a rebranded BLE button bar. Sentry mode is achievable through the Qualcomm AIS camera client, which OverDrive has already running on a Shark 6 with all five cameras, though keeping the unit awake when the ute is locked is still unsolved by anyone. The two things nobody controls are ADB access (BYD can close it with any OTA) and autostart after deep sleep.

## 2. What Sharkware actually is (research/agents/02)

- Three APKs pushed over wireless ADB by a wrapped `adb connect` + `adb install` installer ("Shark Transfer"): the visible app plus two hidden helpers.
- Climate, heated and ventilated seats, seat memory, app shortcuts, pitch/roll page, gauges page (12 V, HV %, ranges, fuel, speed, odometer, RPM, tyres, outside temp, drive mode), towing page (ranges, battery, tyre pressures; the trailer size buttons are illustration only), ambient lighting zones and button backlights, physical key mapping.
- "Shark Trigger" (AUD 317) is the commercial "DiKey" accessory sold on Alibaba as "BYD Shark 6 DiKey Vehicle Intelligent Keys" (vendor app 迪铠智联, com.easytech.link, JieLi BLE chip). Its full BLE protocol is documented in research/open-dikey/PROTOCOL.md. Sharkware's "custom BLE chip" claim is marketing.
- Tested by them on Android 11, OTA V2.2.x. Their gauges page showed every value as Unavailable in their own screenshots.
- Priced AUD 48.99 (Hub), 72.99 (Control), 34.99 (Transfer+), 317 to 372 (Trigger bundles).

## 3. The head unit we are targeting (research/agents/01 and 04)

| Item | Value |
|---|---|
| Generation | DiLink 5.0 global, chipset 51 |
| SoC | Qualcomm SA8155P class |
| OS | Android 11 (API 30), arm64, Automotive as a QNX guest VM |
| Screen | 15.6 inch rotating (Premium/Performance), 1920x1080 in Sharkware screenshots; 12.8 inch fixed on Dynamic |
| ADB name | BYD AUTO |
| Firmware family | 51.1.x (owner facing OTA names V2.0.0 to V2.2.2) |
| Google services | none (GAS being added as a separate sub ecosystem) |
| SELinux | enforcing; /data/local/tmp not writable even by shell |

One repo claims the Shark is a DiLink 3.0 / Android 10 / 13.1.33 unit. That is almost certainly the 12.8 inch unit in some trims. The recon script settles it for our ute.

Enable ADB: Car > System > Version, tap the Factory Reset text 10 times, rotate the screen to portrait (or split screen on a fixed screen), press CONNECT USB TO ENABLE DEBUGGING MODE, then `adb connect <ip>:5555` over WiFi. The dialler engineering code does not work on the Shark. A USB folder install path ("Third Party Apps 998", password BYD8155F) works on the sibling Sealion 7 and is untested on the Shark.

## 4. How an app talks to the ute (research/agents/04, research/open-dikey/)

- SDK: android.hardware.bydauto.<subsystem>.BYDAuto<X>Device singletons, about 20 subsystems, plus BYDAutoFeatureIds (about 21,000 constants). Not on the boot classpath on DiLink 5; inject the dex of com.byd.carsettings and com.byd.data.collect into our classloader at runtime (Dilink5SdkInjector.kt) after `settings put global hidden_api_policy 1`.
- Permissions: BYDAUTO_<X>_COMMON are runtime and can be `pm grant`ed over ADB (or by the app itself through the loopback ADB at 127.0.0.1:5555 using dadb). _GET/_SET are signature level; a ContextWrapper that returns GRANTED for BYDAUTO_* (BydPermissionContext.kt) is enough for AC, seats, bodywork and lights on the Shark 6. Camera (Panorama) and Instrument SET are enforced server side and need a shell UID helper (BYDMate/OverDrive pattern) instead.
- Confirmed on Shark 6 by Open DiKey: climate on/off, temperature per zone, fan, mode, recirc, auto, defog, rear/mirror heat, sync, max cool, ambient bar colours and modes, seat heat and vent levels, ambient light sensor, ADAS toggles.
- Confirmed on the same generation by byd-trip-stats: SOC, usable kWh, EV/fuel/combined range, odometer, trip consumption, SOH, coolant temp, engine RPM and power, speed, pedals, gear, steering angle, VIN, tyre pressures and temps, 12 V, outside temp, drive mode, HV pack V/I via listener events.
- Pitch and roll: head unit Bosch SMI130 IMU; use the `-iner` sensor instances, not the default stubs.
- Autostart after deep sleep is blocked (package is force stopped, stopped=true). Every project, Sharkware included, lives with "open once after power on" or a fragile shell daemon. See research/open-dikey/autostart.md.

## 5. Sentry mode (research/agents/06)

- Native: nothing. No dashcam, no sentry, 360 view is live only.
- Working route on the Shark: dlopen /vendor/lib64/libais_client.so (QCarCam) from a normal app, five inputs at 1920x1300 UYVY 30 fps. Camera2 and MediaProjection return nothing. OverDrive's Shark port (Sep 2026) has all five cameras and telemetry working.
- Motion: GPU frame diff plus TFLite YOLO; or cheaper, trigger from the parking radars.
- Unsolved: staying alive when locked. OverDrive's shell UID power hold daemon is killed on the Shark. Budget when it works is about 1 to 2% HV per parked day through the DC/DC from the 13.8 Ah 12 V.
- Plan: ship drive recording plus "on only" sentry first (armed while the ute is on or in camp mode), and treat locked sentry as a research track.

## 6. Hardware: decision is touch only

A physical button bar is not needed. Everything Sharkware Hub does runs on the touchscreen, and the app gets a floating quick panel for eyes free climate and seat changes instead. If that ever changes: DiKey is a real Shark 6 accessory on Alibaba with a fully documented protocol (research/open-dikey/PROTOCOL.md), and research/agents/05 has the ESP32-S3 design, parts list and the note that some DiLink 5 units cannot scan BLE, so a USB-C link would come first.

## 7. Vehicle facts that matter for features (research/agents/03)

- 29.58 kWh Blade LFP, 60 L tank, V2L 6.6 kW (3 tub outlets plus 1 cabin), 12 V is a 13.8 Ah LFP topped up from HV every 15 minutes when parked.
- Tow mode is auto triggered by the 7 pin plug and disables 10 ADAS functions; it is a readable state.
- EV/HEV and ECO/Sport are hard buttons; terrain modes, Crawl, HDC, V2L, SOC hold, camp mode are screen settings.
- Cloud API for the BYD phone app is reverse engineered (pyBYD, hass-byd-vehicle): lock, climate, location, status. Optional for remote features without keeping the head unit awake.

## 8. Risks

- BYD closes ADB on a future OTA (already happened on Chinese DiLink 5 "2606" builds where the port closes on every reboot). Mitigation: WRITE_SECURE_SETTINGS based restorer, USB folder install fallback, browser install test.
- Autostart is not solvable for a normal APK today. Design every feature around "open once per drive" and make the launcher tile and floating icon frictionless.
- Server side permission enforcement varies per subsystem and firmware. Probe on the real ute before promising any control.
- Warranty: installing apps does not void it, but BYD can wipe them with an OTA.

## 8a. Limp mode: what causes it and why our approach avoids it

No public report links sideloaded apps or BYD SDK calls to limp mode on any BYD. The documented Shark 6 limp mode cases are powertrain protection: reduced power at about 70 km/h when heavily loaded on long climbs or in heat (Whirlpool thread page 3), and the V2L two stage overcurrent lockout where the second stage needs a dealer reset. Both are VCU decisions from physical conditions, not the head unit.

Software actions that genuinely can hurt the vehicle, and that we will not do:

- Blind writes to unknown feature IDs (BYDAutoFeatureIds has about 21,000). Powertrain, Instrument SET and ADAS writes could set warning states. We only call the same climate, seat, light and read only methods BYD's own UI uses, verified by Open DiKey and byd-trip-stats on this generation.
- OBD/UDS writes: clearing DTCs while driving, adaptations, security access attempts. Our OBD path is read only mode 01 and mode 22.
- CAN injection (the DiLink 3 cluster SPI broadcast trick). Not used.
- Firmware flashing, downgrades, removing system packages, or leaving an OBD dongle plugged in during an OTA (the handbook warns this can corrupt the update).
- Sentry keep alive draining the 12 V. A flat 12 V produces a no start and dormancy recovery, not limp mode, but it is the one real ownership risk of parked sentry, so it ships with SOC floors and "on only" by default.

Rule for the build: reads are free, control writes are limited to the allow list in section 4, and every write is verified by read back.

## 9. What to do first

1. Ute on home WiFi, enable ADB, run `tools/recon.ps1 -Ip <ute ip>`. That gives the real Android version, chipset, BYD package list, SDK apk, camera and sensor inventory.
2. Sideload wheregoes byd-probe.apk and Open DiKey to confirm which BYDAUTO calls work on our firmware.
3. Then build. The plan is in PLAN.md.

## Repo map

- research/agents/01 head unit stack, 02 Sharkware teardown, 03 vehicle hardware and electrical, 04 BYD AutoSDK and permissions, 05 DIY control panel hardware, 06 sentry mode and customisation catalogue.
- research/sharkware-scrape/ every public sharkwarehub.com page plus the eight app screenshots and Trigger photo.
- research/open-dikey/ README, DiKey BLE protocol, autostart findings, and the four Kotlin files that do SDK injection, permission bypass, ADB self grant and climate control.
- tools/recon.ps1 read only device dump script.
- BYD-SHARK-6-*.md the earlier owner guides (note their firmware numbers are DiLink 3 numbers and do not apply to the 8155 unit).
