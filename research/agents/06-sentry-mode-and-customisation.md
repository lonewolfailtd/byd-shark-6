# Agent report 06: Sentry mode on the Shark 6, and the customisation catalogue

Researched 18 Sep 2026. [C] confirmed by a cited source. [I] inferred.

## Part A: Sentry mode

### A1. Community implementations

#### OverDrive (open source, MIT; the most advanced and the only one with a live Shark 6 port)

- Repo https://github.com/yash-srivastava/Overdrive-release ; site https://www.overdrive.qd.je/ ; forum https://byd.forum/topic/38-overdrive-advanced-sentry-mode-for-byd-vehicles/ ; architecture wiki https://deepwiki.com/yash-srivastava/Overdrive-release
- Camera access [C]:
  - DiLink 3/4 (Android 10): reflection on the proprietary android.hardware.AVMCamera HAL class, output to a SurfaceTexture / GL_TEXTURE_EXTERNAL_OES. No Camera2. Seal/Atto camera ID 1 delivers a 5120x960 panoramic strip; rearranged into a 2x2 mosaic.
  - DiLink 5 (Snapdragon SA8155P, Android 11 Automotive as a guest VM over QNX): AVMCamera is gone. OverDrive originally LD_PRELOADs libhook_qcarcam.so into /vendor/bin/qcarcam_test, intercepts the DMA buffers and streams UYVY frames over a UNIX socket @dilink5_cam to its CameraDaemon; newer builds (v46.6+) use a standalone `fast_cam_capture --all --time 0` grabber. Raw sensors are 1920x1300 at 30 fps per camera, encoded at 1920x1080 (https://deepwiki.com/yash-srivastava/Overdrive-release/2.2-dilink-5-qcarcam-sidecar-architecture ; issue 273). It also binds the OEM AIDL service com.ts.avm.AvmAndroidService (startAvm/stopAvm/getAvmStatus) to keep the camera hardware powered.
- Motion detection [C]: GPU shader pixel diff (MotionPipelineV2, requiredBlocks sensitivity) then a TFLite YOLO classifier (person, vehicle, bike, animal). "Proximity Guard" triggers off the car's 8 ultrasonic parking radars with pre event buffer and 500 ms debounce.
- Keeping the unit awake when locked [C]: ACC state via BYDAutoBodyworkDevice.getPowerLevel() (0..3, 255 unavailable) with fallbacks polling sys.accanim.status and dumpsys power every 500 ms. An AccSentryDaemon running as UID 2000 (shell) registers an AbsBYDAutoPowerListener and sends periodic Power Hold heartbeats to BYDAutoPowerDevice so the MCU does not cut 12 V to the head unit; a StealthPanel forces backlight to minimum via BYDAutoSettingDevice and injects key events. On DiLink 5 it also holds WakeLocks/WifiLocks and forces WIFI_SLEEP_POLICY_NEVER (https://deepwiki.com/yash-srivastava/Overdrive-release/3.1-acc-and-power-state-monitoring). The keep alive mechanism needs shell privilege, which is why wireless ADB must remain on and daemons launch from the shell domain.
- Battery drain [C]: Song Plus EV owner measured about 2% overnight before v39, then roughly 1 to 2% per day after power fixes (issue 220). Modes: "On and Off" (parked monitoring) vs "On Only" (zero overnight drain), low power parked mode, safe zones and schedules.
- Storage [C]: internal by default, USB flash drive supported with fallback; H.264/H.265, 2 to 12 Mbps, 15 to 30 fps.
- Root not required. ADB required and must stay enabled; install needs the Disable Autostart toggle unchecked plus a hard reboot (hold Volume Down 5 s).
- Shark 6 status (Sep 2026) [C]: issue 258 (1 Sep 2026) and PR 260:
  - Shark SELinux (enforcing) denies create for u:r:shell:s0 on u:object_r:data_local:s0, so /data/local/tmp is not writable even by the ADB shell. Workaround: scratch moved to /storage/emulated/0/Android/data/com.overdrive.app/files/daemon.
  - LD_PRELOAD into vendor qcarcam_test does not work from app/tmp/SD paths on Shark; the port runs a standalone AIS capture binary from the APK via linker64 instead.
  - All 5 cameras work (four exterior plus cabin), and battery/fuel telemetry works. A Shark profile and SOH estimation (29.6 kWh) were added.
  - Unresolved: the ACC daemon "gets killed often, no workaround yet"; keeping the unit alive when the car is switched off is still open.
  - Owner firmware quoted: DiLink build 56.1.2.2608110.1; OTA 2.2.1. Premium: rotate the screen and tap factory reset text 10 times for the ADB menu; the dialler code does nothing on Shark; Performance cannot rotate the screen and a paid unlock service (baolab.au) was suggested.

#### BladeWatch (open source, MIT)
- https://github.com/andrewloable/BladeWatch ; DiLink v3 only. GPU mosaic pipeline from the panoramic camera, GPU motion detection per quadrant, optional TFLite YOLO11n gate. Two APKs signed with the same key over wireless ADB. No Shark 6 / DiLink 5 support.

#### Strike (open source)
- https://github.com/UnrealSalty/Strike ; uses AVMCamera, TFLite person/vehicle detection, keeps head unit and cameras awake when armed; about 1 to 2% HV per parked day on an Atto 2. DiLink 5 QCarCam path explicitly not implemented.

#### Kim Launcher (commercial)
- https://kimlauncher.pro/ ; DiLink v3 only; Pro USD 29/yr includes Sentry and remote access.

#### Electro (commercial, closed)
- https://forums.whirlpool.net.au/archive/31mmxlpv ; records all four cameras to USB/microSD, sentry when off, subscription since late 2025. Drain: Dolphin about 1%/day, Atto 3 about 10%/week. No confirmed Shark 6 report. Claims DiLink 5.0/5.1 support in "Build 49" but a Sealion 7 (chipset 51) owner says it still does not work there.

#### BYD Sentry (bydsentry.com) and sentry ev
- https://bydsentry.com/ freemium, VIP USD 59.99/yr; reported not working on Sealion 7.
- https://sentryev.com/en installs from USB folder "third party apps 55", WebRTC live view, local automations.

#### BYD CLUB ITALIA video
- https://www.youtube.com/watch?v=sNkWT7vJU1Q (Jul 2024). Two apps from a community programmer for BEVs (Atto 3, Seal); almost certainly early Electro beta and/or Di+ [I].

#### Chinese tools
- Di+ (迪加): https://www.wuleba.com/2387.html ; remote camera view, sentry with 15 to 20 s pre roll, custom steering wheel buttons, trip/charge logs. Setup: disable autostart restriction, whitelist in memory cleaner, enable auxiliary functions under Vehicle Settings > DiLink > Application Management. Measured 2.8 km range loss per 24 h in sentry on a Song PLUS DM-i. Exposes a local HTTP API at http://localhost:8988/api/getDiPars used by byd-hass.
- 智能守卫 BYD edition, 迪迪卫: sentry plus 360 driving record to SD card.
- Official Chinese market "哨兵模式" requires 360 cameras, a data SIM and sufficient charge.

#### Standalone DiLink 5 camera PoC (important for Shark)
- https://github.com/VitalyArt/byd-360-cam-for-51-chipset plus notes DILINK5_CAMERA_CAPTURE_DISCOVERY.md
- dlopen("/vendor/lib64/libais_client.so") from a normal app without root, qcarcam_query_inputs, open input IDs 0 to 24, five ION/DMA buffers via qcarcam_s_buffers, qcarcam_get_frame/release_frame. Input 0 = 1920x1300 at 30 fps, format 0x07080102 UYVY, stride 3840, 4,992,000 bytes per frame. Stop before opening the OEM AVM app [C].

### A2. What BYD provides natively on the Shark 6

- AU Shark 6: no built in dashcam, no Sentry; the bracket behind the mirror is empty; 360 view is live only (https://dashcamguys.com.au/blogs/guides/byd-shark-6-dashcam-guide) [C].
- BYD OEM dashcam accessory: front only, 1080p, footage on a memory card viewed on the centre screen, no app, no parking mode, switches off with the ute [C].
- Sealion 8 owners reported a working parked Sentry in Aug 2026; nothing equivalent for Shark 6 as of Sep 2026 [C].
- 12 V context: 13.8 Ah 12 V battery; the car wakes every 15 minutes when off to check it and tops it up from the traction pack [C]. Sentry drains the traction pack through the DC/DC [I].

### A3. Android APIs that matter

Camera enumeration [C]:
- DiLink 5: CameraManager rejects non system apps with "Permission failure: android.permission.SYSTEM_CAMERA". The OEM com.byd.avm renders on a hardware overlay plane below SurfaceFlinger, so screencap and MediaProjection return black.
- DiLink 3: cameras live in /system/framework/bmmcamera.jar (AVMCamera / NormalCamera), not on the boot classpath. Logical IDs: front, rear, pano_h, pano_l, rf, dms, face, cargo, apa, rvs. BYDAutoPanoramaDevice GET is enforced server side.
- Shark 6 is on the DiLink 5 / AIS path (Android 11, vendor qcarcam_test, SA8155P class) per the OverDrive port; five inputs enumerate [C]. The only working route on the Shark is the AIS/QCarCam userspace client, not Camera2 and not MediaProjection [I, strongly supported].

Power states [C]: BYDAutoBodyworkDevice.getPowerLevel(), sys.accanim.status, dumpsys power, BYDAutoPowerDevice power hold heartbeats, AbsBYDAutoPowerListener. On Shark the ACC daemon is still being killed; whether SELinux, the MCU sleep timer, or the "2606+" ADB closure seen on DiLink 5 (BYDMate README: recover via WRITE_SECURE_SETTINGS) is not known [I].

Android 11 restrictions [C]: foreground services started from background cannot get camera/mic (exemptions: boot receivers, SYSTEM_ALERT_WINDOW holders). MediaProjection needs a consent dialog per session; moot on DiLink 5 because the AVM is on a hardware plane. Shizuku must be restarted after every reboot without root; every BYD project instead launches its own shell UID daemon from wireless ADB (OverDrive), does one shot app_process collection (byd-collector), or talks to autoservice Binder from the ADB shell (BYDMate). Permission model: BYDAUTO_*_COMMON runtime and pm grantable; _GET/_SET signature. On DiLink 5 users hit "Permission Deny!", consistent with server side enforcement, meaning shell privilege or platform signing is required (https://github.com/wheregoes/byd-apps/issues/3). Shark 6 will behave like DiLink 5 here [I]. (Counterpoint: Open DiKey reports the ContextWrapper works for AC, seat, bodywork and ambient light on the Shark 6; only camera and Instrument SET fail. Test both.)

## Part B: Customisation catalogue (software only)

Privilege key: P = plain APK plus ADB granted runtime permissions; S = needs a shell (UID 2000) helper launched over ADB; R = root (no BYD project uses root).

| Area | Demonstrated by | Mechanism | Priv | Shark 6 status |
|---|---|---|---|---|
| Custom launcher | Kinex https://kinex.lexwah.com/ ; Kim https://kimlauncher.pro/ ; BYDLauncher https://github.com/GeyuongGongPark/BYDLauncher ; DiLauncher | HOME intent; vehicle data via bydauto reflection | P (DiLink 3), likely S on DiLink 5 | Kinex lists Shark as early DiLink 5 development [C] |
| Wallpaper | native via USB image | built in | none | [C] |
| Boot animation | generic Android needs root | /system/media/bootanimation.zip | R | not feasible |
| Screen off / black screen | OverDrive StealthPanel via BYDAutoSettingDevice backlight | SDK backlight control | S | [C] on DiLink 3/5 |
| Split screen | BYDMate 1/3 + 2/3 via freeform_support system setting; native DiLink split | settings put then reboot | S (WRITE_SECURE_SETTINGS via ADB) | [C] |
| Floating widgets / overlays | BYDMate widget (SOC, range, 12 V); OverDrive floating | SYSTEM_ALERT_WINDOW plus byd_float_app_list | P | [C] |
| Automation | BYDMate WHEN then THEN rule engine (17 actions); DiLauncher conditional tasks JSON; Di+ steering button macros (auto open windows) | SDK setters (setAllWindowState, BYDAutoAcDevice.start/setAcTemperature) | P on DiLink 3, S on DiLink 5 | Tasker/MacroDroid: no BYD report |
| Custom AVAS / lock chime | wheregoes Door Sound + Engine Sound | Custom audio cannot reach the exterior AVAS speaker; only the MCU tones and built in engine presets can be selected; custom files play on interior speakers only | P | [C] on Dolphin; assume same on Shark |
| Voice assistant | byd-dashvoice (Vosk offline, BYDAutoAcDevice); BYDMate agent (32 tools); native custom wake word ("Let's call you X") and Settings > Assistant > Default digital assistant app | AudioSource.DEFAULT (MIC returned silence on that unit) | P | [C]; replacing "Hi BYD" hotword itself only via default assistant setting |
| CarPlay / Android Auto | Kinex embeds AA/CarPlay panes | | P | [C] |
| YouTube while driving | ReVanced + microG + Aurora on Shark; app compatibility list https://bydhack.com/en/apps | plain sideload | P | [C] |
| Speed camera overlay | Waze compatible; Radarbot style overlays need SYSTEM_ALERT_WINDOW | | P | [I] |
| HUD mirroring | BYDMate pushes manoeuvre icons to the factory projector via SOME/IP (Leopard 3) | direct SOME/IP channel | S | Shark has a W HUD; untested |
| Per driver profiles | none found | | | no evidence |
| Ambient light control | Open DiKey controls colours and modes on Shark 6 via BYDAutoLightDevice | SDK | P | [C] |
| Seat/mirror memory | seat heat/vent via BYDAutoSettingDevice; no mirror memory API found | SDK | P/S | partial [C] |
| Remote control from phone (HTTP/MQTT/HA) | OverDrive web app on :8080 + Cloudflare/Zrok/Tailscale + MQTT discovery (127 fields); DiLauncher MQTT (https://github.com/freddycs/byd_car_mqtt); byd-collector DiLink 5 to MQTT/Influx; byd-hass via Termux + Di+ localhost API | head unit 4G/WiFi outbound tunnel | S for control on DiLink 5 | [C] |
| Trip logging / exports | BYD Trip Stats https://github.com/angoikon/byd-trip-stats (trip detection, energydata SQLite BMS, CSV/JSON/HTML, Telegram, ABRP; PHEV fuel supported; DiLink 5 build needs Android 11+) | SDK + SQLite | P/S | "Shark 6 would require firmware verification" [C] |
| Charging / fuel logs | Trip Stats, Di+ | | P/S | [C] |
| Off road inclinometer | Sharkware; generic sensor apps | head unit accelerometer (SMI130 -iner instance) | P | [C] via Sharkware |
| Tow mode helpers | none found | | | no evidence |
| Camping / V2L monitoring | BYDLauncher camping mode AC/battery monitor; wheregoes Cabin pet mode | BYDAutoAcDevice, getBatteryCapacity() | P | V2L signal not identified |
| Engine on/off tricks | BYDAutoEnergyDevice.getPowerGenerationState/Value() read only | | | read only |
| HV SOC / 12 V display | Statistic getElecPercentageValue/getFuelPercentageValue, Bodywork getBatteryVoltageLevel; BYDMate reads BMS SoH from energydata DB as shell | SDK / SQLite | P (DiLink 3), S (DiLink 5) | Shark battery/fuel confirmed readable by OverDrive port [C] |

## Bottom line for our build

1. Camera route on Shark 6 [C]: userspace libais_client.so (QCarCam) as in VitalyArt's PoC and OverDrive PR 260. Five inputs, 1920x1300 UYVY 30 fps each. Camera2 and MediaProjection are dead ends.
2. Motion/trigger [C]: GPU frame diff plus TFLite works; radar based Proximity Guard is cheaper on power; radar reads on DiLink 5 may need shell level SDK access.
3. Stay alive when locked: the hardest unsolved part on Shark. OverDrive's UID 2000 AccSentryDaemon power hold approach is the reference, but on Shark it is being killed and /data/local/tmp is SELinux blocked, so scratch and binaries must live under the app's external files dir and start via linker64.
4. Budget: about 1 to 2% HV per parked day when it works. Offer "On Only" (drive recording only) and safe zone modes from day one.
5. Privileges: no root anywhere; plan on a persistent ADB launched shell helper because DiLink 5 may enforce BYDAUTO permissions server side for some subsystems.
