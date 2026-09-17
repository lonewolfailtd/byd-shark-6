# Shark 6 app suite: build plan

## Context

Tane wants Sharkware style features on his BYD Shark 6 Premium (climate and seat control, seat memory, gauges, off road pitch/roll, towing info, ambient lighting, app shortcuts) plus Sentry mode, built by us, personal use first and sellable later. Research (C:\Users\Hodgs\byd-shark-6\RESEARCH-REPORT.md and research/agents/) shows the head unit is a DiLink 5.0 unit (Qualcomm 8155, Android 11, arm64, "BYD AUTO") and that Open DiKey (research/open-dikey/) already does SDK injection, permission self grant and climate control on this exact vehicle. No physical button hardware is needed; it is an optional last phase.

Repo: C:\Users\Hodgs\byd-shark-6 (github lonewolfailtd/byd-shark-6). Research files are already written there and uncommitted; the first implementation step commits them.

Working name for the app: Lonewolf Shark (package nz.lonewolf.shark). Rename later.

## Constraints that shape everything

- Only ADB over WiFi installs apps. Every install and every runtime permission grant goes through adb. The app can also self grant through the loopback ADB (127.0.0.1:5555) with dadb once authorised, as Open DiKey does.
- BYD SDK classes are not on the boot classpath. Inject com.byd.carsettings and com.byd.data.collect dex at runtime (copy the pattern in research/open-dikey/Dilink5SdkInjector.kt) after `settings put global hidden_api_policy 1`.
- BYDAUTO_*_COMMON permissions are pm grantable; _GET/_SET are bypassed by a ContextWrapper (research/open-dikey/BydPermissionContext.kt). Camera and Instrument SET are server side enforced: camera goes through libais_client.so, Instrument SET is not used.
- No autostart after deep sleep. Design is "open once per drive" with a fast launcher tile; a foreground service keeps state while the ute is on.
- Control writes limited to an allow list (climate, seat heat/vent, ambient light, defog) that BYD's own UI uses. Reads unlimited. Every write verified by read back. No OBD writes, no unknown feature IDs.
- No dashes in UI copy, NZ spelling.

## Phase 0: ground truth on the real ute (1 session, ute on WiFi)

1. Commit the research to the repo.
2. Enable ADB on the ute (Car > System > Version, tap Factory Reset text 10x, rotate to portrait, CONNECT USB TO ENABLE DEBUGGING MODE).
3. Run `tools/recon.ps1 -Ip <ute ip>` (already written, read only). Output lands in research/device-dump/<stamp>/. Confirms Android version, chipset, BYD package names, which apk carries the SDK, camera inputs, sensor list, BT stack.
4. Sideload two known good probes: byd-probe.apk from wheregoes/byd-apps (enumerates every BYDAuto*Device method and reports permission results) and the Open DiKey release APK (proves climate control works on our firmware). Record results in research/device-dump/probe-results.md.
5. Decide from the dump: if the unit is DiLink 5 as expected, proceed. If it turns out DiLink 3 (Android 10), swap the injector for direct framework.jar linking (simpler) and the rest of the plan stands.

## Phase 1: app skeleton and vehicle bridge

New Android project at C:\Users\Hodgs\byd-shark-6\app (Kotlin, minSdk 29, targetSdk 30, arm64 only, Jetpack Compose for UI, no Google dependencies). Toolchain: install Android Studio or command line SDK + JDK 17 (none present on the PC today; adb exists at Downloads/platform-tools-latest-windows).

Modules:
- `core/adb`: AdbSelfGrant using dadb, ported from research/open-dikey/AdbPermissionManager.kt. Grants BYDAUTO_*_COMMON, SYSTEM_ALERT_WINDOW, hidden_api_policy, byd_float_app_list entry, deviceidle whitelist.
- `core/byd`: Dilink5SdkInjector + BydPermissionContext (port from open-dikey), then typed wrappers: ClimateBridge (start/stop, temp per zone, fan, mode, recirc, auto, defog, sync, max cool, from BydAcController.kt), SeatBridge (BYDAutoSettingDevice seat heat/vent levels OFF 1 LOW 2 HIGH 3), LightBridge (ambient bars colour and mode), TelemetryBridge (Statistic, Speed, Engine, Tyre, Bodywork, Energy, Instrument, VehicleHealth, OTA 12 V; listener based where getters are dead, per byd-trip-stats Dilink5Client.kt). Every bridge exposes a StateFlow and a `probe()` that reports which calls work on this firmware.
- `core/imu`: pitch/roll from the SMI130 `-iner` accelerometer and gyro (pick by name, not getDefaultSensor), complementary filter, user "level here" calibration, screen rotation aware.
- `service/VehicleService`: foreground service holding the bridges, polling telemetry at 1 Hz, re applying seat memory a few seconds after start.
- Installer: `tools/install.ps1` (adb connect, install -r, then the grant list). Later becomes the customer installer.

Deliverable: a Diagnostics screen that lists every bridge call and its live value or error. This is the gate for everything after.

## Phase 2: the four Sharkware pages, done better

- Climate: driver and passenger temp, fan arc, vent modes, auto, A/C, recirc, dual, rear defog; presets (Boost, Winter, Summer, two custom modes) stored as full climate snapshots; seat heat and vent I/II per side with "restore on start" toggle.
- Gauges: 12 V, HV %, EV range, fuel %, fuel range, combined range, odometer, trip, speed, engine RPM, coolant temp, outside temp, tyre pressures and temps, drive mode, SOH. Show "not on this firmware" per gauge from the probe instead of a blanket Unavailable.
- Off road: pitch and roll dials with sensitivity, peak hold, and a wading depth reference; shortcuts to BYD's terrain and crawl screens via intents found in the package dump.
- Towing: tow mode state read from the vehicle (not an illustration), ranges, battery, tyres, plus a trailer profile (name, weight) and a pre departure checklist.
- Ambient lighting: bar colours and modes with day/night profiles from the light sensor.
- Shell: bottom tab bar, floating icon (SYSTEM_ALERT_WINDOW), theme follows drive mode, app shortcut slots, settings.

### Profiles (first class, Phase 2)

A profile is a named snapshot of everything the app controls, stored in a local Room database and applied in one tap or automatically:
- Climate: on/off, driver and passenger temp, fan, vent mode, auto, A/C, recirc, dual, rear defog.
- Seats: heat and vent level per side, and "restore a few seconds after start" on or off.
- Ambient lighting: colours and mode, day and night variants.
- App shortcuts, gauge layout and order, theme, quick panel contents.
- Presets Boost, Winter, Summer are just built in profiles; Tane, partner, work, camping, towing are user profiles.
- Apply rules: default profile on start; optional rules like "outside temp under 8 apply Winter", "tow mode detected apply Towing", "time after 20:00 apply night lighting". Rules run in VehicleService while the ute is on.
- Export and import profiles as JSON (needed for reinstalls after an OTA wipe, and for selling later).

### Recommended extras nobody else ships (add to Phase 2 or 3 as marked)

Owner pain points from the research, all doable with reads plus the allow listed writes:
- Battery coach (P2): LFP recalibration reminders (weekly 100% charge, 10 to 100% every 3 months), SOC hold explainer, charge session log with kWh and cost at the NZ rate you set.
- 12 V and vampire drain monitor (P2): log 12 V voltage and HV SOC at every start, chart drain per parked day, warn early. The 13.8 Ah 12 V and 8% per day drain are the two most common Shark 6 complaints.
- Camp and V2L monitor (P2): live SOC, estimated draw from SOC delta, hours remaining at current draw, user SOC floor alert, reminder that the engine will auto start below 15%.
- Tow mode helper (P2): detect tow mode from the vehicle, show the 10 ADAS functions it disables, tyre pressure targets loaded (290 kPa rear) vs unloaded (250), trailer profiles with weight and ball weight against the 250 kg / 2,500 kg limits, pre departure checklist.
- Tyre pressure targets and alerts (P2): per profile targets, colour coded against live TPMS, temperature drift warning on long tows.
- Trip logbook (P2): automatic trips from gear and odometer, EV vs fuel km, litres and kWh, CSV export in the IRD mileage logbook format for the business ute claim. Hook into the existing Telegram vehicle service tracker at C:\Users\Hodgs\vehicle-bot later for service reminders.
- Fuel and EV economy coach (P2): last 50 km, per trip, per profile, compared with the 2.0 and 7.9 L/100 km spec figures.
- Off road extras (P2): GPS breadcrumb track recording with pitch/roll peaks, wading depth reference (700 mm), max angle log per trip.
- Night mode and screen off (P2): one tap dim and black screen for camping, brightness tied to the light sensor.
- Remote telemetry (P3): publish telemetry over the ute's 4G to MQTT for Home Assistant and a phone view (location, SOC, fuel, 12 V) without the BYD cloud. Read only, so it is safe.
- Dashcam clip sharing (P3): export event clips to a phone over the ute's WiFi hotspot with a QR code.
- Probe and decide later (needs Phase 0 results): windows crack open on hot days (setBodyWindowCtrlState, body interlock may refuse), SOC target shortcut (setSOCTarget exists in SettingDevice), custom voice commands through the default assistant slot (byd-dashvoice pattern).

Reuse: seat and climate call sequences from research/open-dikey/BydAcController.kt; telemetry names from research/agents/01-head-unit-stack.md section 6.

## Phase 3: drive recorder and Sentry

- Camera: native helper (C++ via NDK) that dlopen's /vendor/lib64/libais_client.so, queries inputs, streams the five 1920x1300 UYVY feeds (research/agents/06, VitalyArt PoC and OverDrive PR 260). Stop capture before the BYD AVM app opens.
- Encoder: MediaCodec H.265, 1080p per camera, rolling 3 minute segments to USB drive with internal fallback.
- Drive recorder first (ute on): always records, event marker button, clip export.
- Sentry "on only" second: armed while the ute is powered (camp mode, waiting in car parks): GPU frame diff on the mosaic, TFLite person/vehicle gate, pre event buffer, alert on screen.
- Sentry when locked is a research track, not a promise: try OverDrive's shell UID power hold daemon on our firmware, measure daily HV drain, enforce a SOC floor and safe zones. If it cannot be kept alive, the product ships without it and says so.

## Phase 4: polish and sellability

- Signed release builds, in app update check from GitHub releases, licence key tied to VIN (bodywork.getAutoVIN), customer installer (PowerShell and a Mac script), Stripe checkout, one page site. Log it in Mission Control.

## Physical control bar: not building one

Decision: touch only. Instead, Phase 2 includes a floating quick panel (temp, fan, seat heat, one tap presets) reachable from any screen, and large touch targets sized for use while driving. The hardware research stays in research/agents/05 and research/open-dikey/PROTOCOL.md if that ever changes.

## Verification

- Phase 0: research/device-dump contains getprop, package list, probe results; Open DiKey changes the cabin temperature on the real ute.
- Phase 1: Diagnostics screen shows green for the climate, seat, light and telemetry calls; `adb shell dumpsys activity services` shows VehicleService alive after 30 minutes with the screen off.
- Phase 2: change temperature and fan from the app and see the BYD bottom bar mirror it; seat heat restores after a key cycle; pitch dial reads 0 on flat ground after calibration and matches a phone inclinometer on a slope.
- Phase 3: five camera clips written to USB with correct timestamps; a person walking past the parked ute produces an event clip.
- Tane tests on the ute himself; I do not drive the browser or hardware.

## First three concrete steps once approved

1. `git add -A && git commit` the research in C:\Users\Hodgs\byd-shark-6 and push.
2. Install the Android command line tools and JDK 17, create the app project skeleton with the injector, permission context and self grant ported from open-dikey.
3. Wait for the ute to be on WiFi, run recon.ps1 and the probes, and fill in research/device-dump.
