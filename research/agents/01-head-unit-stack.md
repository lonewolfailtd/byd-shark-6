# Agent report 01: Shark 6 head unit hardware, OS stack and third party app surface

Researched 18 Sep 2026. Confirmed = at least one hands on source. Speculated = inferred and marked.

## 0. TL;DR: what we are targeting

| Item | Value | Status |
|---|---|---|
| Head unit generation | DiLink 100 / "DiLink 5.0", UI 5.0, global branch, chipset code 51 | Confirmed (byd.forum version table lists "Sea Lion 7 / Tang EV / Shark" on controller 51) |
| SoC | Qualcomm 8155 (SA8155P family) | Confirmed for chipset 51 via byd.forum and the BYD8155F USB install password; not yet confirmed by a Shark getprop |
| Android | Android 11, arm64 on the global 8155 units (Sharkware: "our BYD AUTO Android 11 Shark"; byd-trip-stats: "DiLink 5.0 / Android 11, arm64, e.g. Sealion 7") | Two independent hands on sources; generic "DiLink 5 = 12L/13" claims describe Chinese 778G/BYD9000 units |
| ADB device name | BYD AUTO (Sharkware prints BYD_AUTO_VERIFY=PASS) | Confirmed |
| Firmware string family | 51.1.4.YYMMDDx (e.g. 51.1.4.2602270, 51.1.4.2605140.1 for Sealion 7 global) | Confirmed for chipset 51; Shark strings not yet posted (one owner quoted 56.1.2.2608110.1, see report 02) |
| Vehicle API | android.hardware.bydauto.* (BYD AutoSDK v1.0.5), loaded at runtime from OEM apks com.byd.carsettings / com.byd.data.collect | Confirmed on Shark 6 (Open DiKey) and Sealion 7 (byd-trip-stats) |
| Google Play | Absent. BYD OverseaAppStore; Aurora/microG workarounds; GAS announced as an added "sub ecosystem" for Premium/Performance | Confirmed |

Note the conflict with report 04, where one repo (edgycoder-ph/byd-shark6-adb-unlock) calls the Shark a DiLink 3.0 / 13.1.33 / Android 10 unit. The just_byd Telegram firmware post Di3.0_13.1.33.2407240.1 also lists "Shark" among chipset 13 (SM6125) models. Most likely there are two Shark head units: the 15.6 inch rotating 8155 unit (AU/NZ Premium/Performance) and a 12.8 inch fixed 6125 unit (some LatAm trims, possibly the AU Dynamic cab chassis). tools/recon.ps1 settles this for our ute.

## 1. Hardware and OS

Which DiLink generation: https://byd.forum/dilink-versions.html DiLink 100 (internal 5.0) block: controller 51, CPU Qualcomm 8155, UI 5.0, branch 51.1.2, sub brand Overseas, models "Sea Lion 7 / Tang EV / Shark". Chinese DiLink 100 row is controller 23, Qualcomm 778G/782G, 12+256 GB, Android 12; DiLink 150 is BYD 9000 / Android 13. RAM/storage for controller 51 listed Unknown. bydhack.com groups chipsets 23, 51 and 56 under "DiLink 5.0, SDK 32, Android 12L" (https://bydhack.com/en/systems) but that is a generation level generalisation.

Android version on 8155 global units: Android 11 per Sharkware diagnostics page and byd-trip-stats docs/DILINK5.md. Contradicting generic claims: byd-dashcast "DiLink 5 (Android 13)" on a Seal, BYDMate "DiLink 5.0 (Android 12, API 32)" on a Leopard 3, BYD-ADB-Unlock reports the Atto 3 EVO BYD100F unit is Android 14 / API 34. "DiLink 5" spans several Android bases by chipset; chipset 51 is the Android 11 one. Treat Android 11 / API 30 as high confidence, not proven. Target SDK: byd-dashcast pins targetSdk 29; Open DiKey and byd-trip-stats build against minSdk 29.

Screen: AU Premium has a 15.6 inch rotating screen; the 2026 Dynamic cab chassis has a 12.8 inch fixed screen (Chasing Cars, Carlinkit AU, Wikipedia). BYD publishes no resolution. Sharkware's screenshots are 1920x1080. DiLink 3 12.8 inch unit is 1920x1080 at 240 dpi with a [0,84][1920,990] stable frame (wheregoes/byd-apps). Sharkware notes the Performance centre screen "does not rotate" in some cases and needs the split screen trick.

Reference getprop (DiLink 3 Dolphin, for shape only): ro.build.fingerprint=BYD-AUTO/DiLink3.0/DiLink3.0:10/QKQ1.210910.001/eng.build.20250725.152222:user/release-keys, ro.product.model=BYD AUTO, ro.product.brand=BYD-AUTO, ro.product.board=QCM6125, ro.board.platform=trinket, ro.boot.flash.locked=0. Expect the Shark's to read ro.product.model=BYD AUTO with a DiLink5.0 / BYD100F style device name and an sa8155/sm8150 platform. BYD wireless ADB props on DiLink 5/100F: persist.sys.adb.wiress.enable and sys.connect.adb.wiress (BYD's misspelling), from BydDevelopmentTools apk (https://github.com/DottoreTozzi/BYD-ADB-Unlock).

Google services: no GMS. Play installer refuses with "For your driving safety and system stability, please do not install third party apps". Owners use Aurora Store plus ReVanced/microG. BYD Australia announced Google Automotive Services for Premium and Performance as a separate sub ecosystem over the embedded SIM (https://www.carexpert.com.au/car-news/byd-shark-6-range-to-get-more-standard-tech).

## 2. Firmware history

Numbering fact: the 13.1.33.2412 / 2503 / 2506 builds in our older guides are the DiLink 3.0 (chipset 13, SM6125) branch used by Seal, Atto 3 and Sealion 6 (https://wiki.defective.tech/en/BYD/Firmware). They do not apply to the 8155 Shark unit, whose branch is 51.1.x:

| Build | Date | Notes | Source |
|---|---|---|---|
| 51.1.2 | table baseline | | https://byd.forum/dilink-versions.html |
| 51.1.4.2602270 (DX_UKE_SOP-user-20260227_191246_51_1_4_2602270_1_SECURE_ALL.zip, 6.2 GB) | May 2026 | Global firmware for 51 chipset, Sealion 07 Global | https://byd.forum/topic/42-byd-sealion-07/ ; https://modhub24.com/firmware/firmware_8eab9541 |
| 51.1.4.2605140.1 (6.3 GB) | Sep 2026 | Same, update content unknown | https://t.me/s/just_byd ; https://modhub24.com/firmware/firmware_a1f943cc |

BYD Australia labels Shark OTAs with marketing versions: V1.5, V2.0.0 (Sep 2025), V2.1.2, V2.2.1/V2.2.2, plus the 2026 Crawl Mode OTA. Nobody has mapped V numbers to 51.1.x strings. Version screen: System > Version > Vehicle Version.

Lockdown timeline across BYD:
- DiLink 3: from build 2310 no direct ADB without downgrade to 2307.
- After 2407: standard developer tools hidden; dialler code workaround (does not work on Shark).
- DiLink 5.0 China: ADB opened only remotely via QR code from a BYD side contact.
- Summer 2026 "2606" DiLink 5 builds: ADB port closes on every reboot (BYDMate, verified Song L Aug 2026).
- EU R155 updates lock OBD apps on Atto 3.
- Every OTA can reset developer flags and remove sideloaded apps.

For the AU Shark, the hidden menu method still worked as of Trail Shark (Aug 2025) and Sharkware 7.04 (Sep 2026). Sharkware is testing a browser install path as a hedge against ADB loss.

## 3. Enabling ADB on the Shark 6

Confirmed AU method:
1. Car > System > Version. Tap the "Factory Reset" text (not the button) 10 times quickly.
2. Hidden developer menu opens; top buttons are clipped in landscape. Rotate to portrait (Premium). On a non rotating unit, open split screen with another app so the menu redraws.
3. Press "CONNECT USB TO ENABLE DEBUGGING MODE / REVOKE USB DEBUGGING ENABLE AUTHORIZATION (STEBU/UKE)". Toast "Turn on ADB debugging !" means on; press again to disable.
4. Put the car on WiFi, read its IP, `adb connect <ip>:5555`.
Sources: https://github.com/MorghusDragon/BYD-Shark-Sideloading, https://www.sharkwarehub.com/install.html.

Fallbacks and context:
- USB folder installer: DiLink 3 uses folder "third party apps" with password 20211231 or BYD6125F. Chipset 51 (Sealion 7 global) uses folder "Third Party Apps 998" and password BYD8155F; "installation and updates for all apps only via flash drive" (https://byd.forum/topic/8-sealion-7-installing-apps/). Untested on an AU Shark. Worth trying as an ADB free install path.
- Dialler engineering menu *#91532547#* (pair phone, dial in car Phone app, IMEI code from https://ahmada3mar.github.io/BYD/): reported not working on Shark; newer DiLink 5 builds show a QR code only BYD can clear.
- Persisting ADB across reboots: `settings put global adb_enabled 1` plus persist.sys.adb.wiress.enable=true / sys.connect.adb.wiress=1, which needs WRITE_SECURE_SETTINGS granted via pm grant while ADB is up (BYD-ADB-Unlock; BYDMate).
- ADB from inside the head unit: adbd listens on 127.0.0.1:5555, so a sideloaded app can shell itself using the dadb library and grant its own permissions (Open DiKey AdbPermissionManager.kt).

## 4. System packages

No pm list packages dump from a Shark is published. Confirmed on DiLink 5 (Shark 6 / Sealion 7): com.byd.carsettings (CarSetting, carries the newer DiPilot SDK classes) and com.byd.data.collect (BydDataCollect at /system/app/BydDataCollect/BydDataCollect.apk) bundle the android.hardware.bydauto.* classes (Open DiKey and byd-trip-stats Dilink5SdkInjector.kt). BYDMate on DiLink 5 talks to a system Binder called autoservice under shell uid.

DiLink 3 package list (78 BYD packages, Dolphin) as a proxy: com.byd.car.server (DiCarServer, UID 1000, CAN/MCU bridge), com.byd.airconditioning, com.byd.carsettings, com.byd.carsettings.plugins, com.byd.providers.carsettings, com.byd.providers.carstatus, com.byd.cameramanager, com.byd.auto_camera, com.byd.bydcamera, com.byd.vrassistant, com.byd.wallpaperhome (launcher), com.byd.androidauto, com.byd.carplay.ui, com.byd.byddevelopmenttools, com.byd.clusterdebug, com.byd.otaupdate, com.byd.otgupdate, com.byd.cota, com.byd.upgradeserver, com.byd.overseaappstore, com.byd.aftermarketinstalltool, com.byd.auto.permission, com.byd.appstartmanagement, com.byd.customkey, com.byd.intelligententry, com.byd.trafficmonitor, com.byd.drivingrestriction, com.byd.browser, com.byd.CanDataCollect (https://github.com/wheregoes/byd-dolphin-hacking/blob/master/data/packages/byd-packages.txt).

## 5. The vehicle API

No evidence of AOSP android.car / VehicleHAL exposed to third party apps on any DiLink; every project uses BYD's SDK. DiLink 3 stack: App > BYDAutoManager > Binder > DiCarServer (UID 1000) > auto.default.so > /dev/spidev_ivi > MCU. DiLink 5 equivalent: the autoservice Binder.

Classes (BYD Auto API V1.0.5; PDF redistributed in Open DiKey assets/BYD API/API.V1.0.5.pdf). All are android.hardware.bydauto.<pkg>.BYDAuto<X>Device singletons via getInstance(Context) with AbsBYDAuto<X>Listener callbacks. Confirmed present on DiLink 5 (Open DiKey probe list): ac, statistic, sensor, light, setting, seat, tyre, speed, gearbox, engine, charging, energy, instrument, bodywork, vehiclehealth, collectdata, dipilot, adas, ota, pm2p5, plus catalogues BYDAutoFeatureIds (about 21k int constants) and BYDAutoEventValue. BYDAutoMotorDevice.getInstance() throws "Stub!" on DiLink 5.

How to link against it on DiLink 5: the classes are not in the framework android.jar; apps inject the installed OEM apk's classes*.dex into their own PathClassLoader.dexElements at runtime (appended, so our own classes win name collisions), which requires relaxing the hidden API policy: `settings put global hidden_api_policy 1` and `settings put global hidden_api_blacklist_exemptions 'Lcom/ts/,Ldalvik/system/'` (Lcom/ts/ is the OEM SDK namespace). The setting resets on reboot (byd-trip-stats Dilink5SdkInjector.kt, AdbPermissionManager.kt, docs/DILINK5.md; Open DiKey copies the pattern).

Permissions: android.permission.BYDAUTO_<SUBSYSTEM>_{GET|SET|COMMON}, 100 plus (full list https://github.com/wheregoes/byd-dolphin-hacking/blob/master/docs/bydauto-api.md). *_COMMON are runtime and pm grantable; *_GET/_SET are signature. Workaround used by Open DiKey, byd-dashvoice and byd-dolphin-hacking: wrap the Context in a BydPermissionContext returning PERMISSION_GRANTED for anything starting with android.permission.BYDAUTO_ and override getApplicationContext() too (DiPilot/ADAS getInstance stores the app context). Works for AC, door lock, bodywork, seat; fails where the check is server side (Panorama/360 camera, Instrument SET) (https://github.com/sp-hy/Open-DiKey/blob/main/app/src/main/java/com/sphy/airconcontroller/byd/BydPermissionContext.kt). byd-dashcast instead signs with the public AOSP platform test key, accepted on DiLink 3 as platform signature.

Methods confirmed in use on Shark 6 / DiLink 5 (Open DiKey, byd-trip-stats):
- Climate (BYDAutoAcDevice, device 1000): getAcStartState, getTemprature(zone) (1 driver, 2 passenger, 4 outside), getAcWindLevel (1..7), getAcControlMode, getAcCycleMode, getAcCompressorMode, getAcMaxCoolingState, getAcVentilationState, getAcTemperatureControlMode; setters setAcStartState, setAcTemperature(zone,temp,source,flag), setAcWindLevel(source,level), setAcControlMode, setAcCycleMode, setAcDefrostState(source,area,state), setElectricDefrostState, setAcVentilationState, setAcCompressorMode, setAcMaxCoolingState, setAcTemperatureControlMode. Temp 17..32 °C, source 0 UI 1 voice, feature id 0x1DE0000C = wind level.
- Seats: BYDAutoSettingDevice.getSeatHeatingState/setSeatHeatingState(area,level) and getSeatVentilatingState/setSeatVentilatingState, levels OFF=1 LOW=2 HIGH=3 (Open DiKey BydSeatController.kt).
- ADAS/DiPilot: getLaneAssistType/setLaneAssistType, getAiEmergencyBrakeState/setAiEmergencyBrakeState, DMS/fatigue switches, getSpeedAdjustModeState (Open DiKey BydAdasController.kt).
- Lighting/ambient: BYDAutoLightDevice and BYDAutoSensorDevice.getLightIntensity, Android TYPE_LIGHT fallback (Open DiKey AmbientLightProbe.kt). Open DiKey also controls ambient light colours and modes on the Shark 6.

Lower level channels seen on DiLink 3 only (likely absent on 8155, unverified): CAN injection via `am broadcast -a com.byd.cluster.spi --es normal '...'` through com.byd.clusterdebug, UDS stack in BydDevelopmentTools.apk, /dev/spidev_ivi, content providers content://com.byd.carStatusProvider/car_status (works from shell), com.byd.car.server.provider.CarServiceProvider.

## 6. Telemetry available to apps

Confirmed on DiLink 5 (Sealion 7 BEV; some PHEV fields exercised on Shark 6 by Open DiKey BydVehicleInfoController):
- SoC (statistic.getElecPercentageValue, event onElecPercentageChanged, integer only on D5), usable kWh (getEVRemainingBatteryPower), EV range, fuel % and fuel range (getFuelPercentageValue, getFuelDrivingRangeValue), combined range (getDrivingRangeAll), odometer, EV/HEV mileage, lifetime and trip consumption, SOH (vehiclehealth.getBatteryHealthStatus).
- Coolant temperature: statistic.getWaterTemperature (PHEV path in byd-trip-stats).
- Engine: engine.getEnginePower, getEngineSpeed, getOilLevel, getEngineCode; energy/operation mode energy.getEnergyMode/getOperationMode.
- Speed, accelerator/brake depth, gear (gearbox.getGear), park brake, steering angle (bodywork.getSteeringWheelValue), VIN (bodywork.getAutoVIN).
- Tyres: tyre.getTyrePressureValueByType(area) (raw scaled by the cluster unit, read fid 4208 INSTRUMENT_UNIT_PRESSURE: 1 bar 2 psi 3 kPa), getTyrePressureValue, getTyreTemperatureValue, getTyrePressureState, getTyreAirLeakState, getTyreSignalState, getTyreBatteryState; per wheel temperature only via AbsBYDAutoTyreListener.onTyreTemperatureValueChanged(wheel,value) (0 LF 1 RF 2 LR 3 RR), only while driving. Needs BYDAUTO_TYRE_COMMON.
- HV pack V/I and motor RPM only via AbsBYDAutoCollectDataListener events (onMotorMCUGeneratrixVolt/Current, onDriverMotorSpeed(front, rear)); getters are dead. 12 V via ota.getBatteryVoltage(0), ambient via ac.getTemprature(4), drive mode via instrument.getSportModeState, T-Box serial via ota.getTBoxSerialNumber.
Sources: https://github.com/angoikon/byd-trip-stats/blob/main/app/src/dilink5/java/com/byd/tripstats/sdk/Dilink5Client.kt; https://github.com/sp-hy/Open-DiKey.

Tilt/roll and tow: sensor.getSlope was "confirmed dead" on the Sealion 7 dev car; pitch/roll is most likely the head unit's own accelerometer (see report 04 for the SMI130 -iner trick). OverDrive claims live bus reads of incline, parking radar, tyre pressure, battery, charge gun, drive and EV mode, so an incline signal may exist in the feature id catalogue; dump it with BYDMate's "save fid catalog" or by enumerating BYDAutoFeatureIds.

## 7. Practical gaps and what to capture first

1. Nobody has posted a Shark getprop, pm list packages, or the 51.1.x string for an AU/NZ Shark. Grab them on day one (tools/recon.ps1).
2. Confirm which OEM apk carries the SDK on the Shark (com.byd.carsettings vs com.byd.data.collect) and whether hidden API exemptions survive the current AU build.
3. Background survival is the hard problem: DiLink force stops sideloaded packages on deep sleep (stopped=true), whitelists (persist.sys.acc.whitelist) do not help, and "open once after power on" is the supported model (https://github.com/sp-hy/Open-DiKey/blob/main/autostart.md). Disable "Disable background Apps" for our package in DiLink settings (BYDMate).
4. Browser downloads are policy blocked in com.byd.browser on DiLink 3, so ADB remains the realistic install path unless the USB "Third Party Apps 998" folder works on the Shark.
5. Watch the 51.1.4.26xx line: the "2606" reboot closes ADB behaviour seen on Chinese DiLink 5 units would break Sharkware style products overnight; ship a WRITE_SECURE_SETTINGS based ADB restorer with the app.

## Sources

- https://byd.forum/dilink-versions.html
- https://byd.forum/topic/42-byd-sealion-07/
- https://byd.forum/topic/8-sealion-7-installing-apps/
- https://bydhack.com/en/systems
- https://t.me/s/just_byd
- https://modhub24.com/firmware/firmware_8eab9541 and https://modhub24.com/firmware/firmware_a1f943cc
- https://www.sharkwarehub.com/ (install, diagnostics, screenshots, terms)
- https://github.com/MorghusDragon/BYD-Shark-Sideloading
- https://github.com/sp-hy/Open-DiKey
- https://github.com/angoikon/byd-trip-stats
- https://github.com/AndyShaman/BYDMate
- https://github.com/DottoreTozzi/BYD-ADB-Unlock
- https://github.com/wheregoes/byd-dolphin-hacking
- https://github.com/wheregoes/byd-apps
- https://github.com/Kiroha/byd-dashcast
- https://github.com/homeo26/byd-dashvoice
- https://github.com/GeyuongGongPark/BYDLauncher
- https://github.com/ahmada3mar/BYD
- https://overdrive-5lc.pages.dev/
- https://www.scribd.com/document/914887883/ADB-Debug-Dilink-5
- https://wiki.defective.tech/en/BYD/Firmware
- https://allterrain.app/byd/
- https://xdaforums.com/t/byd-multimedia-install-apk.4541247/
- https://au.carlinkitfactory.com/byd-shark-6-tbox-add-carplay-android-apps/
- https://www.bydaccessories.store/blogs/news/byd-shark-6-infotainment-screen-the-complete-owners-guide
- https://www.carexpert.com.au/car-news/byd-shark-6-range-to-get-more-standard-tech
- https://www.chasingcars.com.au/news/car-technology/free-crawl-mode-upgrade-for-byd-shark-6-24000-australian-customers-set-to-benefit/
- https://en.wikipedia.org/wiki/BYD_Shark_6
- https://www.youtube.com/watch?v=oxDPU7E-FZw (Trail Shark)
