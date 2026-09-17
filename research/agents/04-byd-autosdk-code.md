# Agent report 04: the BYD in car SDK (android.hardware.bydauto), permissions, tilt and roll

Researched 18 Sep 2026.

## Executive summary

- The SDK is real and well documented by the community. It is the `android.hardware.bydauto.*` class family baked into `framework.jar` on every DiLink head unit: singleton `BYDAutoXxxDevice.getInstance(Context)`, generic `set/get(deviceType, featureId, value)` on `AbsBYDAutoDevice`, and `BYDAutoDeviceManager.getInt/setInt`. Full stub sources for 20 plus device classes are on GitHub (confirmed).
- Whether a normal sideloaded APK can call it depends on the DiLink generation. On DiLink 3.0 (Android 10, Qualcomm 665/QCM6125) the signature level checks are client side and are defeated by a ContextWrapper; no platform key, no root, no system app, no whitelist (confirmed on Dolphin and Seal). On DiLink 5.0 (Android 11/12L, SA8155P/778G, QNX hypervisor) the check moved server side, so apps route calls through a shell UID helper spawned over on device ADB (confirmed on Leopard 3, Sea Lion 07, Sealion 7).
- One source (edgycoder-ph/byd-shark6-adb-unlock) states the Shark 6 is a DiLink 3.0 unit on firmware branch 13.1.33 (Android 10, Qualcomm 665), the same branch as Seal/Han/Tang. Sharkware and an OverDrive issue say Android 11 and a build string 56.1.2.2608110.1. CONFLICT: resolve with `tools/recon.ps1` on the real ute.
- No one has published a decompile of Sharkware. Its wording ("stock control pathways", "helper update checks", "signing material") is consistent with the BYDAUTO API plus permission bypass (inferred).
- Tilt and roll: the head unit has a physical Bosch SMI130 accelerometer and gyro pair reachable through Android SensorManager if you pick the `-iner` instance rather than the stub default. A CAN "road slope" signal also exists. (Confirmed on DiLink 3 Seal, Shark 6 inferred.)

## 1. The API surface (confirmed)

### 1.1 Complete stub source: GeyuongGongPark/BYD-Health-Monitor

https://github.com/GeyuongGongPark/BYD-Health-Monitor (module `bydauto-stub/`, compileOnly). Classes under `android/hardware/bydauto/`: `AbsBYDAutoDevice`, `BYDAutoConstants`, `BYDAutoDeviceManager`, plus device and listener pairs for ac, audio, bodywork, charging, doorlock, energy, engine, gearbox, instrument, light, multimedia (+MediaInfo, MediaControlParam), panorama, pm2p5, radar, safetybelt, sensor, setting, speed, statistic, time, tyre. Base interfaces in `android/hardware/`: `IBYDAutoDevice`, `IBYDAutoEvent`, `IBYDAutoListener`.

Key signatures:

```java
// android.hardware.bydauto.BYDAutoDeviceManager
static BYDAutoDeviceManager getInstance(Context);
int setInt(int device, int event, int value); int getInt(int device, int event);
int setDouble(...); double getDouble(...); int[] getIntArray(...); byte[] getBuffer(...);

// android.hardware.bydauto.AbsBYDAutoDevice (all devices extend this)
int set(int device, int event, int value); int get(int device, int event);
boolean postEvent(int devType, int evtType, int val, Object data);
void registerListener(IBYDAutoListener l);

// BYDAutoAcDevice
int start(int source); int stop(int source);
int setAcTemperature(int type, int value, int tempSource, int unit);
int setAcWindLevel(int, int); int setAcWindMode(int, int); int setAcCycleMode(int, int);
int setAcDefrostState(int, int, int); int getTemprature(int area);

// BYDAutoBodyworkDevice
int getWindowState(int area); int getDoorState(int area); int getWindowOpenPercent(int area);
double getSteeringWheelValue(int type); int getPowerLevel(); String getAutoVIN();

// BYDAutoLightDevice: getLightStatus(int type) with LIGHT_SIDE/LOW_BEAM/HIGH_BEAM/TURN/FOG
// BYDAutoSettingDevice: hasFeature("driver_seat_heating"|"driver_seat_ventilating"|"four_wheel_drive"...),
//   setSOCTarget, setEnergyFeedback, setSteerAssis, setLockOff, setOverspeedLock, setBackDoorOpenedHeight
// BYDAutoSpeedDevice: getCurrentSpeed(), getAccelerateDeepness(), getBrakeDeepness()
// BYDAutoSensorDevice: getLightIntensity()   // NOT an IMU, only ambient light
```

Device type IDs (BYDAutoConstants): AC 1000, BODYWORK 1001, AUDIO 1002, LIGHT 1004, ENERGY 1006, INSTRUMENT 1007, PM2P5 1008, CHARGING 1009, GEARBOX 1011, ENGINE 1012, SPEED 1013, STATISTIC 1014, TYRE 1016, LOCATION 1017, SETTING 1023, TIME 1024, RADAR 1025, PANORAMA 1031, DOOR_LOCK 1041, SAFETY_BELT 1042, SENSOR 1043, WIPER 1046, REAR_VIEW_MIRROR 1047. Result codes: SUCCESS 0, FAILED 0x80000020, BUSY 0x80000021, TIMEOUT 0x80000022, INVALID_VALUE 0x80000023. The real jar can be pulled from a car and dropped in as `libs/bydauto-openapi.jar`.

### 1.2 Live verified API reference: wheregoes/byd-apps and wheregoes/byd-dolphin-hacking

- https://github.com/wheregoes/byd-apps/blob/main/research/byd-auto-api-reference.md (reverse engineered from framework.jar, AirConditioning.apk, live probing; Dolphin 2024, DiLink 3.0, Android 10, firmware 13.1.32.2507250.1).
- https://github.com/wheregoes/byd-dolphin-hacking (bydauto-api.md, ac-climate-control.md, light-control.md, sideloading-guide.md, rooting-guide.md, decompiled DiCarServer, CarSetting, ClusterDebug).

Verified working on car: `start(0)/stop(0)`, `setAcTemperature(1, celsius, 1, 1)`, `set(1000, 0x1DE0000C, level)` for fan (the named setAcWindLevel was broken), `setAcWindMode(mode,1)`, `setAcCycleMode`, `setAcControlMode`, `hasFeature("ACRemoteControl")=1`. DRL auto toggle `setInt(1004, 0x43100046, 1|2)` works. Turn signal, hazard, fog and headlight flash all return LIGHT_COMMAND_FAILED (MCU locked). Door lock status returns INVALID for main doors on Dolphin; child lock readable; no setDoorLockStatus(). Panorama GET is enforced server side even on DiLink 3. `IBYDAutoPanoService` AIDL: getValue(int), setValue(int,int), getBuffer(int), setBuffer(int,byte[]), registerUser/unregisterUser. Native side: libbydauto.so, android.gui.BYDAutoServer; DiCarServer is com.byd.car.server running as UID 1000.

Ships `apps/common/libs/byd_sdk.dex`, stubs, a `byd-probe.apk` that enumerates every BYDAuto*Device method via reflection, and an AVD harness with a fake vehicle. Dolphin docs confirm most BYDAUTO permissions have protectionLevel=normal on that firmware while AC_GET/SET are signature level, so protection level varies per subsystem and firmware.

### 1.3 Official (2018 era) documentation on CSDN

- https://blog.csdn.net/ccagy/article/details/105005081 (253 interface list, Tang/Qin) and https://blog.csdn.net/ccagy/article/details/104545809 (HelloWorld manifest with android.permission.BYDAUTO_BODYWORK_COMMON, "COMMON must be requested at runtime").
- https://blog.csdn.net/shangxianyue5670/article/details/84567212 (series 1 to 18 covering Energy, Charging, Setting devices etc).

### 1.4 Other packaged SDKs

- https://byd.forum/topic/61-byd-sdk-for-developers/ links sdk_v1.0.5.zip on modhub24. Unverified binary, probably a repack of the framework classes.
- yash-srivastava/Overdrive-release bundles app/libs/classes.jar and a stub AbsBYDAutoEnergyListener.java.

## 2. Permission model per generation (confirmed)

Three tiers per subsystem: `android.permission.BYDAUTO_<SUB>_COMMON` (runtime, gates getInstance()), `_GET` and `_SET` (signature|privileged, enforced inside each getter and setter). Overdrive's manifest lists 60 plus including BYDAUTO_ADAS_*, BYDAUTO_BMS_*, BYDAUTO_DOOR_LOCK_*, BYDAUTO_LIGHT_*, BYDAUTO_MOTOR_GET, BYDACQUISITION_SEND_*.

DiLink 3.0 / 4.0 (Android 10: Dolphin, Seal, Atto 3, Han, Tang, possibly Shark 6): the _GET/_SET check is done in process against the Context you pass to getInstance(). `BydPermissionContext` (a ContextWrapper overriding checkCallingOrSelfPermission, enforceCallingOrSelfPermission, checkPermission, enforcePermission to return GRANTED for anything starting with android.permission.BYDAUTO_) is enough. No platform signature, no system app, no root, no package whitelist (https://github.com/wheregoes/byd-apps/issues/3, https://github.com/homeo26/byd-dashvoice). Shizuku and LSPatch do not help and are not needed.

DiLink 5.0 / 5.1 (Android 11/12L/14: Sealion 7, Sea Lion 07, Leopard 3, 2025 refreshes): server side enforcement against the real UID. Two projects that work there go through shell UID 2000:
- BYDMate (https://github.com/AndyShaman/BYDMate): a hand rolled ADB client connects to 127.0.0.1:5555, the user accepts the debugging dialog once, reads go via `service call autoservice 5 i32 <dev> i32 <fid>` (tx 5 getInt, 7 getFloat, 6 setInt) and writes via an app_process daemon spawned under shell UID with the app's own APK as CLASSPATH, registering a bydmate_helper binder. Validated windows, AC, locks (doors_lock=2, doors_unlock=1), sunroof, interior and ambient light, mirror heat, seat heat and vent on Leopard 3.
- Overdrive (https://github.com/yash-srivastava/Overdrive-release): daemons run as shell, PermissionGranter does pm grant for every BYDAUTO permission, BydDeviceHelper spoofs the package name to com.android.shell via reflection on ContextImpl, and Dilink5SdkInjector loads the real bydauto classes at runtime from the installed com.byd.data.collect APK. It calls setSeatHeatingState(int,int), setSeatVentilatingState(int,int), voiceCtlMoonRoof(int), and switches drive and energy modes (ECO/Sport/Normal/Snow, EV/HEV) on the DiLink 3 Seal.

Nobody has found an android.car / CarPropertyManager / VHAL path used by third party apps on any BYD unit. DiLink 5 is Android Automotive inside a QNX 7.1 VM, so android.car classes exist there, but BYD's own apps use autoservice/DiCarServer.

## 3. The Shark 6 specifically

Platform: https://github.com/edgycoder-ph/byd-shark6-adb-unlock states "DiLink 3.0 (Qualcomm 665, Android 10, firmware branch 13.1.33)", tested on 13.1.33.2602030.1 (Feb 2026), branch shared with Seal, Han and Tang. The Defective Tech wiki firmware table (https://wiki.defective.tech/en/BYD/Firmware) lists 13.1.33 OTAs 2307 through 2602 including an AU 2401 set. Sharkware says "Android 11" and a Shark owner posted 56.1.2.2608110.1. Unresolved until we run recon on the real unit.

Either way the Shark 6 is the generation on which Overdrive verified climate, windows, seats, drive modes, DRL and charge limit (Seal) and byd-apps verified AC via the ContextWrapper bypass (Dolphin). Nobody has published a Shark 6 specific byd-probe run.

Getting ADB on: pre 2407 firmware, tap Factory Reset text 10 times under Car > System > Version, rotate portrait, toggle debugging (https://github.com/MorghusDragon/BYD-Shark-Sideloading). Post 2407 the toggle was removed from that screen; the edgycoder repo reaches `com.byd.byddevelopmenttools/VerificationActivity` (exported) via a sideloaded Activity Launcher accepted by AftermarketInstallTool's whitelist, then generates the IMEI+time AES password with tools/byd_verification_pwgen.py. Overdrive documents the Bluetooth dialler code *#91532547#* route (reported not to work on Shark). Sharkware's Shark Transfer wraps adb connect + adb install.

Per domain expectations for a Shark 6 app (inferred from Seal and Dolphin):
- Climate: works (start/stop, temp, fan via generic set(1000, 0x1DE0000C, n), mode, recirculation, defrost).
- Seats heat and vent: setSeatHeatingState(area, level) / setSeatVentilatingState used by Overdrive on the Seal; gate with BYDAutoSettingDevice.hasFeature("driver_seat_heating"). Run byd-probe to find the owning class on the Shark.
- Windows: BYDAutoBodyworkDevice.setBodyWindowCtrlState(area, state) / setAllWindowState. A body interlock refused windows on a DiLink 2.1H unit (getWindowPermitState=0); Overdrive reports windows working on the Seal. Expect to need READY.
- Doors: lock/unlock feature IDs known on DiLink 5 (0x41A00008 bitmask candidates on Dolphin). Unverified on 13.1.33.
- Lights: DRL and config type settings work; turn signals, hazards, fog, flash are MCU locked.
- Drive modes: BYDAutoEnergyDevice (getEnergyMode/getOperationMode); Overdrive exposes ECO/Sport/Normal/Snow and EV/HEV switching.
- 360 camera: blocked for third party apps on DiLink 3 (bmmcamera.jar off boot classpath); Overdrive works around it natively.

2025 refresh / DiLink 100: DiLink 100F manual describes an Android 14 unit; bydhack lists DiLink 5.1 as Android 14 SDK 34. No public confirmation of third party vehicle control on Android 14 BYD units. If a later Shark gets that head unit, assume the DiLink 5 shell UID helper approach at best.

## 4. Tilt and roll

Confirmed (DiLink 3 Seal, Overdrive roadsense/detect/ImuSource.kt): the head unit exposes two physical accelerometers and two gyros, Bosch SMI130. `smi130-accel` and `smi130-gyro` are stubs (frozen values, Z=0); `smi130-accel-iner` and `smi130-gyro-iner` are the real inertial sensors. SensorManager.getDefaultSensor() returns the stub, so enumerate getSensorList(TYPE_ACCELEROMETER) and pick the `-iner` instance by name (BYD's own DIP app does the same). Fusion sensors (TYPE_GRAVITY, rotation vector) are derived from the stub and are poisoned. Overdrive does its own gravity frame alignment and self calibrates mounting geometry.

Also confirmed: Overdrive publishes slope_deg ("Road Slope") and lists "incline" among live signals read off the BYD bus, so a CAN derived longitudinal grade exists via the BYDAUTO API on DiLink 3 (feature ID not published; find with byd-probe).

Shark 6 stock UI: the AU handbook has no pitch/roll display text and Sharkware advertises tilt and roll as something it adds, so stock does not show angles (inferred). BYDAutoSensorDevice only exposes ambient light.

Recommendation: read the -iner accelerometer plus gyro, complementary filter, user "level here" calibration in prefs, optional cross check against the bus incline signal for pitch. The rotating screen means axis mapping must account for screen orientation.

## 5. Other repos worth knowing

- https://github.com/homeo26/byd-dashvoice (DiLink 2.1H/4.0, MT6765; documents the inert airconditioning system service decoy vs the real BYDAutoAcDevice, microphone quirks, autostart block).
- https://github.com/sunlixWhyNotAvailable/byd-collector (DiLink 5.0, read only, 23,083 signature catalogue, on device ADB).
- https://github.com/AmaroPSJunior/Byd-dilink (Brazilian control/discovery app with HALReflectionTransport, BinderTransport, PermissionDiscovery).
- https://github.com/Niek/BYD-re (cloud/MQTT crypto of the phone app).
- https://github.com/ahmada3mar/BYD, https://github.com/DeBondor/byd-unlocking-guide (13.1.33 downgrade unlock), https://bydhack.com/en/systems (generation table).
- https://github.com/kangrio/BYD-Hub (BYDChargeControl, Aurora Store automation).

## 6. Bottom line for the build

1. Get ADB on the Shark 6 and run byd-probe.apk from wheregoes to dump the exact class and method surface and permission behaviour. That single run settles every "inferred" above.
2. Build a plain APK (targetSdk 33 or lower, minSdk 29 or lower, arm64) with a BydPermissionContext, request all BYDAUTO_*_COMMON at runtime, call BYDAutoAcDevice / BYDAutoSettingDevice / BYDAutoBodyworkDevice / BYDAutoEnergyDevice by reflection, verify every write by read back (setInt returns 0 even when the MCU ignores it).
3. Ship a shell UID helper path (BYDMate/Overdrive pattern) as the fallback so the product survives a DiLink 5 style enforcement change or a future Android 14 head unit.
4. Note the autostart filter on DiLink 3 (no third party cold start) and that BYD OTAs can remove ADB toggles again.

## Sources

- https://github.com/GeyuongGongPark/BYD-Health-Monitor
- https://github.com/wheregoes/byd-apps and https://github.com/wheregoes/byd-apps/issues/3
- https://github.com/wheregoes/byd-dolphin-hacking and issues/4
- https://github.com/homeo26/byd-dashvoice
- https://github.com/AndyShaman/BYDMate
- https://github.com/yash-srivastava/Overdrive-release and https://overdrive-5lc.pages.dev/
- https://github.com/edgycoder-ph/byd-shark6-adb-unlock
- https://github.com/MorghusDragon/BYD-Shark-Sideloading
- https://github.com/sunlixWhyNotAvailable/byd-collector
- https://github.com/i99dash/dilink5-sim
- https://github.com/AmaroPSJunior/Byd-dilink
- https://github.com/DeBondor/byd-unlocking-guide
- https://github.com/ahmada3mar/BYD
- https://github.com/Niek/BYD-re
- https://blog.csdn.net/ccagy/article/details/105005081
- https://blog.csdn.net/ccagy/article/details/104545809
- https://blog.csdn.net/shangxianyue5670/article/details/84567212
- https://byd.forum/topic/61-byd-sdk-for-developers/
- https://bydhack.com/en/systems
- https://wiki.defective.tech/en/BYD/Firmware
- https://xdaforums.com/t/byd-multimedia-install-apk.4541247/
- https://manuals.plus/byd/dilink-100f-smart-cockpit-manual
- https://bydautomotive.com.au/brochures/BYD-SHARK-6-Owners-Handbook-2024.pdf
