# Agent report 03: Shark 6 vehicle hardware, electrical architecture, cloud API, accessories

Researched 18 Sep 2026. Confirmed = BYD, handbook or reputable published source. Community = reverse engineered by owners or GitHub. Speculated = inferred from sibling BYD models, not verified on a Shark 6.

## 1. Spec sheet (AU/NZ)

### 1.1 Powertrain (confirmed: BYD AU spec sheet, Wikipedia)

| Item | Dynamic / Premium (1.5T) | Performance (2.0T, from about May 2026) |
|---|---|---|
| Engine | 1.5 L BYD476ZQF turbo I4, longitudinal, 135 kW / 260 Nm | 2.0 L BYD487ZQD turbo I4 |
| Front motor | PMSM 170 kW / 310 Nm | not separately published |
| Rear motor | PMSM 150 kW / 340 Nm | not separately published |
| System output | 321 kW / 650 Nm | 350 kW / 700 Nm |
| 0 to 100 km/h | 5.7 s | 5.5 s |
| Battery | 29.58 kWh Blade LFP, cell to chassis | same |
| EV range | 100 km NEDC; 85 km WLTP | about 80 km WLTP |
| Fuel tank | 60 L | 60 L |
| Combined consumption | 2.0 L/100 km (SOC 25 to 100%); 7.9 L/100 km (SOC under 25%); 212 Wh/km | 1.3 L/100 km WLTP |
| Braked / unbraked tow | 2,500 / 750 kg; tow ball 250 kg | 3,500 / 750 kg |
| GVM / kerb / payload | 3,500 / 2,710 / 790 kg (Premium) | GVM 3,500, tare 2,738, payload about 762 kg |
| Drivetrain | DMO longitudinal hybrid, dual motor AWD, no low range, no mechanical diff locks; electronic LSD / torque vectoring only | |

Sources: https://bydautomotive.com.au/brochures/BYD-SHARK-6-2024.pdf, https://en.wikipedia.org/wiki/BYD_Shark_6, https://www.carexpert.com.au/byd/shark/premium/features-and-specs, https://www.racv.com.au/royalauto/transport/electric-vehicles/2026-byd-shark-6-performance-cab-chassis-price-specs-release-date.html, https://www.carsguide.com.au/adventure/byd-shark-6-98898, https://bydautomotive.com.au/shark-6.

### 1.2 Charging and V2L (confirmed)

- AC: Type 2, 7 kW onboard, about 4.6 h 0 to 100%. Mode 2 portable at 2.2 kW.
- DC: CCS2, 55 kW peak, 30 to 80% in 20 to 30 min; real world often about 40 kW.
- V2L: total 6.6 kW. 3 x 230 V 10 A in the tub (driver side under a flap) plus 1 x 230 V 10 A in the rear cabin (2.2 kW), plus a charge port adaptor adding two more sockets. Tub panel says "MAX 6kW 230V". Two level overcurrent protection; second level needs a dealer reset (https://forums.whirlpool.net.au/archive/95pnrppl-2).
- V2L control path: Energy > Charging and Discharging > "Vehicle To Load (VTOL)" toggle, default 5 hour timer, engine auto starts at low SOC, floor 15% SOC (handbook p89 to 91).
- 12 V accessory socket: 1 x front, 120 W, ignition dependent. Wireless charger 50 W; USB C + USB A front, same again rear.

### 1.3 12 V system (confirmed plus community)

- 12 V battery under the rear seat; jump start only at under bonnet terminals (https://www.bydaccessories.store/blogs/news/byd-shark-6-12v-battery-jump-start).
- Small LFP 12 V unit (about 13.8 Ah) topped up from HV via DC/DC; when parked the car wakes roughly every 15 minutes to check 12 V voltage and recharge, the root of the "8% per day vampire drain" complaints (https://forums.whirlpool.net.au/archive/3rvvjk1n).
- Handbook confirms intelligent charging of the LV battery from HV and a dormancy recovery procedure.

Implication for Sentry: any app holding the head unit awake drains the traction pack through the DC/DC, not just the 12 V. Community figures are 1 to 2% HV per parked day when done well.

### 1.4 Dimensions, chassis, wheels (confirmed)

- 5,457 x 1,971 x 1,925 mm, wheelbase 3,260 mm, ground clearance 230 mm, wading 700 mm, approach/ramp/departure 31/17/19.3 degrees, turning radius 6.75 m, tray 1,200 L.
- Double wishbone front and rear, ventilated discs, body on frame.
- 18 x 8J alloys, Continental CrossContact AT 265/65 R18 114T, full size spare. Cold pressures 250 kPa unladen, 250/290 kPa loaded.
- TPMS is direct (sensor in wheel) and shows actual pressures.

### 1.5 ADAS sensor suite (confirmed from the handbook)

Front mmWave radars (plural) plus a single forward camera for ACC/ICC/AEB/LDW/ELKA/TSR, rear corner mmWave radars for BSD/RCTA/RCTB/DOW. Four panoramic cameras (grille, both mirrors, rear plate). Front and rear ultrasonic parking sensors (count not published; other BYDs use 12). DFM listed; inward facing camera not confirmed in the AU handbook, but the OverDrive Shark port enumerates five camera inputs (four exterior plus cabin). Atto 3 opendbc port identifies BYD's radar supplier as Veoneer on a private CAN FD bus; Shark supplier unconfirmed.

Full ADAS list: ACC, ICC, AEB, FCW, LDW, LDP, ELKA, TSR, ISLI, ISLC, FCTA/FCTB, RCTA/RCTB, RCW, BSD, DOW, TSM (trailer stability), AFL, CPD, DFM, HDC, HHC, AVH, CDP, VDC, W HUD, 360 camera. ANCAP 5 star (Jan 2025).

### 1.6 Displays

15.6 inch rotating touchscreen (Premium, Performance), 12.8 inch fixed (Dynamic cab chassis), 10.25 inch LCD cluster, W HUD, 12 speaker Dynaudio, wireless CarPlay/Android Auto (OTA added), "Hi BYD" voice assistant with customisable wake word.

## 2. Electrical / electronic architecture

### 2.1 Confirmed

- Handbook data privacy section: vehicle data "can be read out via the legally required OBD interface"; the telematics unit uploads location, energy consumption, speed, gear, power mode, ESC status, steering, battery and powertrain status to BYD's regional data centre; offline mode = System > Link > WLAN off (handbook p31 to 32).
- OBD2 16 pin port, driver side under the dash. Manual warns not to leave OBD devices plugged in during OTA.
- OTA touches infotainment, ADAS, BMS, engine control, driving modes and vehicle security, so at minimum the head unit, ADAS domain, BMS, ECM and VCU are flash capable via the T Box.
- Towing mode is triggered by the 7 pin trailer plug 15 s after connection or manually from the Vehicle menu; it locks drive mode to Normal, caps speed and disables 10 ADAS functions. EPB has a "Trailer Mode" under Service > Overhaul. The body controller monitors the trailer socket, so tow mode is a software state we can observe.

### 2.2 Known from sibling BYD platforms (community, likely to generalise, unverified on Shark)

- Head unit to vehicle link is SPI to an MCU, not Ethernet/CAN directly. On DiLink 3: App > BYDAutoManager > Binder > DiCarServer (UID 1000) > auto.default.so > /dev/spidev_ivi > MCU, the MCU bridges to body/powertrain CAN. Packet format [featureId:4][len:1][data], no CRC. Cluster frames can be injected with `am broadcast -a com.byd.cluster.spi --es normal 'FF,...'` without root (https://github.com/wheregoes/byd-dolphin-hacking).
- Cloud remote control path: phone app > HTTPS > BYD cloud > AES TCP to the car's cloudmanager (root) > setBuffer(1034, 0xAA000004, frame) > MCU. Device 1034 = BYDAUTO_DEVICE_YUN. Remote commands are dropped by the MCU unless a cloud registered GUID exists.
- CAN buses: opendbc's Atto 3 port documents a main vehicle bus with 11 bit CAN plus a Veoneer radar private CAN FD bus; decoded IDs: 0x11F steering, 0x122 wheel speeds, 0x133 stalks, 0x1E2 ADAS steering cmd, 0x1F0 wheelspeed, 0x1FC EPS torque, 0x242 drive state (doors, gear, seatbelt, throttle), 0x294 cluster, 0x316 LKAS HUD, 0x32D ACC HUD, 0x32E ACC cmd, 0x342 pedals, 0x3B0 cruise buttons, 0x418 BSD radar. UDS quirk: firmware ID at DID 0xF195 not 0xF188 (https://raw.githubusercontent.com/commaai/opendbc/master/opendbc/dbc/byd_atto3.dbc, https://github.com/commaai/opendbc/pull/3337). No Shark 6 DBC exists; DMO powertrain IDs will differ, wheel/steering/door/ADAS IDs may overlap.
- Gateway: BYD cars have a central gateway; the OBD port is a diagnostic CAN (UDS on 0x7E4 BMS / 0x7E7), not a raw feed. Since EU R155 firmware BYD restricted what third party OBD readers can pull; generic DTCs and some BMS PIDs work, cell level data is dealer only.

### 2.3 OBD PIDs community tools use on BYD (verified Atto 3/Dolphin, untested on Shark 6)

From https://github.com/loryanstrant/BYD-PID-list (init ATSH7E7;ATFCSH7E7;ATFCSD300000;ATFCSM1;ATSH7E4, BMS at 0x7E4, response 0x7EC):

| PID | Data | Formula |
|---|---|---|
| 22 1FFC | SOC % | ((B5 x 256) + B4) / 100 |
| 22 0032 | Pack temp C | B4 - 40 |
| 22 0008 | Pack voltage | B4 + B5 x 25 (unconfirmed) |
| 22 0009 | Pack current A | ((A + B x 256) - 5000) / 10 |
| 22 000B | Charge cycles | B4 + B5 x 256 |
| 22 0011 / 0012 | Total kWh charged / discharged | B4 + B5 x 256 |

OVMS Atto 3 module reads SOC, range, speed, BMS voltage/temp and charge state, no TPMS or remote control (https://docs.openvehicles.com/en/latest/components/vehicle_byd_atto3/docs/index.html). Car Scanner has profiles for Seagull, Dolphin, Atto 2, Seal and several PHEVs, none for Shark 6. Nobody has published a Shark 6 PID validation; the PHEV BMS is still a Blade LFP with a BYD BMS so 0x7E4 SOC/temp PIDs are the first thing to try, plus mode 01 PIDs for the ICE (RPM, coolant, fuel level 0x2F).

## 3. Which functions live on the screen vs physical controls (confirmed)

| Function | Where | Notes |
|---|---|---|
| Climate, fan, temp | Screen | API controllable (Sharkware, Open DiKey) |
| Heated/ventilated seats | Screen | API controllable |
| EV / HEV / MAX EV | Physical EV/HEV button (hold 3 s = MAX EV) | not a screen toggle (Overdrive claims EV/HEV via API on Seal) |
| ECO / Normal / Sport | Steering wheel scroll button | |
| Terrain: Mud / Sand / Snow / Mountain (+ Crawl on 2026 Premium/Performance) | Screen | Crawl caps 20 km/h |
| HDC, AVH, auto hold | Physical + screen | |
| V2L on/off + timer | Screen: Energy > Charging and Discharging | auto arms on adaptor plug in |
| SOC hold / charge target | Screen Energy menu | 15 to 70% hold settings |
| Tow mode | Auto via 7 pin plug, or Vehicle menu | locks Normal, kills 10 ADAS |
| EPB trailer mode | Service > Overhaul | |
| ADAS enable/disable | Settings > ADAS > Safety Assist | |
| TPMS pressures | Cluster + screen + app | |
| Ambient lighting | Screen | API controllable (Open DiKey) |
| Camp mode | Screen, OTA added 2025 | |
| 4WD lock / diff lock | Not present | AWD only, e LSD |
| Wireless CarPlay / AA | Screen; wireless only | |

Sources: https://www.bydaccessories.store/blogs/news/byd-shark-6-driving-modes-ev-hev-eco-normal-sport-explained, https://www.bydaccessories.store/blogs/news/byd-shark-6-infotainment-screen-the-complete-owners-guide, handbook.

## 4. BYD app, T Box and reverse engineered cloud API

### 4.1 Official app (confirmed)

BYD AUTO app (com.byd.bydautolink, AU): remote lock/unlock, remote A/C with temp/fan/mode, seat heat/vent, steering wheel heat, flash lights/horn, door/window/tyre status, SOC, range, charge status, location, health alerts, OTA trigger, NFC/Bluetooth digital key. Two years free in AU then subscription (https://www.byd.com/au/support/app). The T Box has its own 4G SIM (OverDrive uses it for internet).

### 4.2 Reverse engineered cloud API (community)

- Hosts: overseas https://dilinkappoversea-eu.byd.auto (used by the AU app), CN https://dilinksuperappserver-cn.byd.auto.
- Crypto: outer white box AES (Bangcle tables in the overseas APK), inner AES 128 CBC zero IV keyed by MD5(MD5(password).toUpperCase()) at login then MD5(contentToken); login /app/account/login returns token.encryToken.
- Endpoints: /vehicleInfo/vehicle/vehicleRealTimeRequest, /control/remoteControl, /vehicle/vehicleswitch/verifyControlPassword (uppercase MD5 of the operation PIN), /app/emqAuth/getEmqBrokerIp.
- MQTT: EMQ broker, MQTTv5 over TLS 8883, client id oversea_<md5(IMEI)>, topic /oversea/res/<userId>.
- APK 2.9.1 is the last version that hooks cleanly (https://github.com/Niek/BYD-re, https://github.com/AwangYes/BYD-re).
- pyBYD wraps this: lock/unlock, climate start/stop/schedule, seat and battery heating, find car, flash, close windows (https://github.com/jkaberg/pyBYD). hass-byd-vehicle is the Home Assistant integration (https://github.com/jkaberg/hass-byd-vehicle); warns frequent polling drains the 12 V. No Shark 6 PHEV field mapping posted yet but the API returns what the app shows, and the app shows fuel/range on PHEVs.
- Electro (paid, sideloaded) skips the cloud: reads data on the head unit and pushes to ABRP and MQTT/Home Assistant, records 4 cameras, sentry mode (https://forums.whirlpool.net.au/archive/31mmxlpv).

## 5. Owner discovered features, OTAs and bugs (2025 to 2026)

- Crawl Mode (max 20 km/h) debuted on the 2.0T Performance (May 2026) and MY26 Premium, OTA to about 24,000 existing Premiums later in the year; Dynamic misses out because of its 12.8 inch screen. Same OTA improved HDC and torque vectoring (https://www.chasingcars.com.au/news/car-technology/free-crawl-mode-upgrade-for-byd-shark-6-24000-australian-customers-set-to-benefit/).
- Performance adds Google Automotive Services on the head unit, 3.5 t towing, dedicated brake controller harness (https://thedriven.io/2026/06/01/new-byd-shark-6-variants-test-drive-3-5-tonne-towing-crawl-mode-and-much-more/, https://forgehaus.au/products/forgehaus-byd-shark-towkit-brake-controller-patch-harness).
- Camping mode (OTA 2025), customisable wake word, AVAS sound selection, LFP recalibration advice.
- Towing limitation backlash: tow mode locking Normal and disabling ADAS under review by BYD (https://www.drive.com.au/news/byd-shark-6-towing-limitations-under-review-following-customer-feedback/).
- Bugs: CarPlay reconnect failures, screen freezes, NFC key oddities, 12 V vampire drain up to 8%/day, V2L overcurrent lockout needing a dealer.
- No native sentry/dashcam; the 360 system is live only.
- Hidden menus: 10 tap Factory Reset dev menu; the dialler engineering code does nothing on Shark.

## 6. Aftermarket electronics and integration

- Sharkware + Shark Trigger: head unit APK plus BLE button/dial/display box with modified firmware.
- DiKey / Open DiKey: BLE or USB (ESP32 C3) centre console controller for climate and ambient light; fully open protocol (https://github.com/sp-hy/Open-DiKey).
- Trailer brake controllers: no factory controller; Forgehaus/ASAP plug and play harness taps the factory tow bar loom and gives blue/red wires for a Redarc Tow Pro; Premium and Performance need different harnesses.
- Dash cams: mirror console integrated 4K units hardwired to the fuse box; none feed the head unit; the 12 V is too small for long parking mode without a battery pack.
- CarPlay/AI boxes: Carlinkit TBox and App2Car plug into the factory USB and run Android 13 with a nano SIM.
- OBD dongles: WiCAN Pro (ESP32, WiFi/BLE, MQTT to Home Assistant), OBDLink MX+, Mani BLE for ABRP.
- Dual battery / DC DC: Maka Offroad and Oz Canopies document 100 to 200 Ah lithium auxiliaries because the factory 12 V is undersized.

## 7. Practical recommendations

1. Richest, cheapest path is on the head unit: wireless ADB, sideload an APK compiled against the BYD Auto API v1.0.5 (as Open DiKey does) and use the device typed services for climate, seats, lights, doors, TPMS, SOC, fuel and camera frames. Model the collector on byd-collector and byd-hass.
2. OBD path is read only and thin: try the 0x7E4 BMS UDS PIDs first, add mode 01 ICE PIDs, assume R155 filtering hides anything not on the diagnostic CAN. No Shark 6 DBC exists; raw CAN needs a tap behind the gateway and our own capture.
3. Remote/cloud: pyBYD + hass-byd-vehicle already give lock, climate, location and status for the AU app account; MQTT push topics are documented. PHEV field names need mapping on a real Shark 6.
4. Physical controls not reachable from software: EV/HEV/MAX EV button and the drive mode scroller are hard buttons (though the head unit API's mode calls may cover most cases); no diff locks or low range to command.
