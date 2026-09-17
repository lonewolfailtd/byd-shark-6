# Agent report 05: DIY BLE control panel hardware (our replacement for Shark Trigger)

Researched 18 Sep 2026.

## 1. What Shark Trigger most likely is

Sharkware's site describes Trigger as "the compatible Bluetooth switch, dial-display and ambient-light hardware for Shark Control" (AUD 316.99 hardware only, AUD 371.99 with Control). Two telling statements:

- "Shark Trigger's internal electronics and BLE implementation have been specifically modified for compatibility. Generic or look-alike button units are not compatible."
- Trigger only works with the Premium head unit, not Performance.

That is effectively an admission it is a re-flashed generic unit from the Tesla aftermarket "under screen button bar" family. (Agent 03 later found the more specific answer: the BLE name "DiKey" is a real BYD centre console controller product, with an open source reimplementation at https://github.com/sp-hy/Open-DiKey. See report 03.)

The generic family (same Shenzhen ODM lineage: slim bar under the centre screen, 10 backlit buttons plus one or two rotary knobs with a 128x128 round LCD, RGB strip, harness):

| Product | Price | Buttons / knobs | Knob display | Link | Notes |
|---|---|---|---|---|---|
| Yeslak Gen 3.0 Multifunction Control Knob | USD 188.99 | 10 + 1 knob | 128x128 round LCD | https://www.yeslak.com/products/multifunction-control-knob-buttons-for-tesla-model-3-y | Gen 3 went wired because Gen 2 BLE suffered interference in car parks. Gen 2 is the BLE variant Trigger resembles. 12 V, 200 mA. |
| Hansshow Center Console Physical Control Button | USD 150 to 200 | 10 + 1 LCD knob | yes | https://www.hansshow.com/products/center-console-physical-control-button-multiple-functions-for-tesla-model-y-3 | BLE bar to a CAN box in the harness |
| THEVRSE Magic LCD Knob | USD 150 to 190 | buttons + LCD knob | yes | https://www.amazon.com/dp/B0D2KKWCSB | Same ODM shell |
| SmarTesla under screen buttons | USD 130 to 180 | 10 + LCD knob | yes | https://smartesla.us/products/under-screen-physical-buttons-with-lcd-knob-rgb-lighting-for-tesla-model-y-3 | Installer channel |
| Generic AliExpress | USD 40 to 90 | 10 | some | https://www.aliexpress.com/item/1005007518930669.html | Unbranded source |
| Tesstudio Bluetooth Buttons | USD 60 to 80 | 5 | no | https://www.tesstudio.com/products/tesla-model-3-y-physical-bluetooth-commander-buttons | BLE to phone app |
| Ctrl-Bar | USD 150 | 2 knobs + 4 buttons | small displays | https://insideevs.com/news/632969/tesla-model-3-y-phisical-buttons-bar/ | Closest two knob layout to Trigger |
| Enhance S3XY Buttons / Knob | USD 99 to 199 | 4 / 1 knob | OLED | https://www.enhauto.com/pages/knob | Encrypted BLE to CAN dongle. Best in class UX reference (200+ actions, macros) |

## 2. DIY hardware design

MCU: ESP32-S3.
- Two GC9A01 240x240 round displays with LVGL want dual core (BLE on core 0, UI on core 1) and PSRAM. C3 and C6 are single core.
- S3 has native USB OTG (USB HID or CDC fallback), 14 capacitive touch pins, 4 RMT channels (addressable LEDs), 4 PCNT units (hardware quadrature decode for encoders).

Boards:
- Main: Seeed XIAO ESP32-S3 (about USD 7.50, USB-C, 8 MB flash, 8 MB PSRAM) or ESP32-S3-DevKitC-1 N16R8 (about USD 10).
- Knob option A (fastest prototype): M5Stack Dial v1.1 (USD 34.90, ESP32-S3 + EC11 encoder + 1.28 inch GC9A01 touch + buzzer). Two Dials as knobs plus a XIAO for buttons and LEDs. Downside: three BLE links, 45 mm knobs.
- Knob option B (recommended for the product): one XIAO ESP32-S3 driving two 1.28 inch GC9A01 modules (USD 5 to 8 each) on shared SPI with separate CS. Espressif esp_lvgl_adapter has a dual GC9A01 example. LovyanGFX supports multiple panels.
- Waveshare ESP32-S3-Touch-LCD-1.28 (about USD 20): S3 + GC9A01 + touch + IMU in one puck. The IMU is useful for a hardware pitch and roll source.

Inputs:
- 10 tactile switches (Kailh Choc low profile or 6x6 mm) under 3D printed backlit keycaps, 4x3 matrix or MCP23017 I2C expander. Debounce in firmware.
- 2x EC11 rotary encoders with push (USD 0.50). Read via PCNT using ESP32Encoder (madhephaestus) or RotaryEncoderPCNT (vickash).

LEDs:
- Light bars: SK6812 RGBW 60 per metre or WS2812B, 5 V, driven by RMT (FastLED or Espressif led_strip). 74AHCT125 level shifter for 3.3 to 5 V data. Per button: SK6812 mini 3535 under each keycap on the same chain. Cap brightness to stay inside a 2 A USB budget.

Power: car USB 5 V into the XIAO 5 V pin; LEDs straight off 5 V with a 1000 uF bulk cap and 330 ohm data resistor. Shark 6 USB ports switch off with the car; still implement deep sleep after 10 minutes with no central.

Software stack:
- BLE: NimBLE-Arduino 2.x under PlatformIO. NimBLEOta (h2zero) or BLEOTA (gb88) for firmware update over BLE. ArduinoOTA over WiFi as a bench fallback.
- Graphics: LovyanGFX panel driver plus LVGL 9 for the knob UI.
- Haptics: DRV2605L + LRA coin motor (about USD 6) on I2C.
- Touch strip: S3 touch pads with copper tape, or an MPR121 breakout.

## 3. Protocol design

Custom GATT (primary). 128 bit base UUID, e.g. a1b2xxxx-0000-4c6f-6e65-776f6c660000.

| Char | Suffix | Props | Payload |
|---|---|---|---|
| Input events | A101 | Notify | [type u8][id u8][value i16][seq u8]; type 0 button down, 1 up, 2 long, 3 encoder delta, 4 encoder push, 5 touch |
| Display text | A102 | Write | [knob u8][line u8][len u8][utf8...] |
| Display gauge | A103 | Write | [knob u8][mode u8][value i16][min i16][max i16] |
| LED zones | A104 | Write no rsp | [zone u8][r g b w][effect u8][speed u8]; zones 0 left bar, 1 centre, 2 right, 3 all buttons, 10 to 19 per button |
| Config | A105 | Read/Write | brightness, night mode, haptic, device name |
| Heartbeat | A106 | Notify + Write | ESP notifies [uptime u32][vbus mV u16] every 2 s; app acks; 3 missed acks shows reconnecting and dims LEDs |
| OTA | NimBLEOta service | | standard |

MTU 247, connection interval 15 to 30 ms. LE Secure Connections bonding, Just Works. Manufacturer data in the advert (0xFFFF + "LW01") for filtered scanning.

Alternative: adopt the Open DiKey wire protocol (service FF10, chars FF11/FF12, frames AA 55 | len | cmd | payload | sum) so our hardware is also compatible with that open source app. See report 03.

BLE HID fallback (works without our app): a second HID service via HijelHID_BLEKeyboard or ESP32-NimBLE-Keyboard. Map buttons to Consumer Control usages Android honours natively (Play/Pause, Vol, Next/Prev, Home, Back, App Switch), encoders to Vol. Test bonding order; HID plus custom on Android 11 can make the OS grab the connection first.

Android 11 client (API 30, sideloaded, ADB grants):

    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>

BLUETOOTH_SCAN and BLUETOOTH_CONNECT are Android 12 only. On 11 scanning needs ACCESS_FINE_LOCATION, and from a background started service ACCESS_BACKGROUND_LOCATION. Grant with:

    adb shell pm grant <pkg> android.permission.ACCESS_FINE_LOCATION
    adb shell pm grant <pkg> android.permission.ACCESS_BACKGROUND_LOCATION
    adb shell appops set <pkg> RUN_IN_BACKGROUND allow
    adb shell dumpsys deviceidle whitelist +<pkg>
    adb shell settings put secure location_mode 3

Reconnect strategy:
1. Foreground service with foregroundServiceType="connectedDevice", started on BOOT_COMPLETED.
2. First connect via filtered scan (service UUID), connectGatt(ctx, false, cb, TRANSPORT_LE), bond, cache MAC.
3. After that skip scanning: adapter.getRemoteDevice(mac).connectGatt(ctx, true, cb, TRANSPORT_LE). autoConnect=true reconnects whenever the bar powers up, no scan needed. Always close() the old GATT on disconnect; back off on status 133.
4. If not reconnected 30 s after boot, run a 10 s filtered scan.

Quirks: no Google Play Services (irrelevant for BLE), possible OEM battery killer (whitelist), BT chip shared with phone HFP/A2DP so keep the connection interval at 15 ms or more.

## 4. Better than Trigger

- OBD/CAN telemetry into the same app. Cheapest solid path: MeatPi WiCAN Pro (USD 89, ESP32-S3, BLE/WiFi/USB, ELM327 emulation, open firmware) https://www.crowdsupply.com/meatpi-electronics/wican-pro and https://github.com/meatpiHQ/wican-fw. DIY: a second XIAO ESP32-S3 + SN65HVD230 (USD 2) on TWAI with https://github.com/muki01/OBD2_CAN_Bus_Library or https://github.com/Kostovite/TWAI_ISO-TP, plus an ELM327 over BLE personality from https://github.com/terrafirma2021/ESP32-S3-BLE-ELM327 so Torque and Car Scanner also work. BYD PIDs: https://github.com/loryanstrant/BYD-PID-list covers Atto 3 and Dolphin (header 7E7/7E4, SOC 221FFC, pack temp 220032, V and I 220008/220009). No Shark 6 list exists; publishing one is a differentiator.
- Haptic encoders (DRV2605L) and a capacitive touch strip for fan and temperature swipes.
- USB-C and a USB fallback: the head unit is a USB host, so an S3 can enumerate as a USB HID keyboard (zero permissions, TinyUSB) or CDC-ACM serial read via usb-serial-for-android with the USB permission granted once via ADB. AOA is the wrong direction. Newer BYD firmware has locked ADB and OTG is reportedly flaky (https://github.com/MorghusDragon/BYD-Shark-Sideloading), so USB is optional and BLE is primary.
- Enclosure: 3D print (PETG or ASA, matte black) to the dash top tray. Start from the "BYD Shark 6 Centre Console Top Organiser Tray" on MakerWorld and the Yeggi "byd shark" models for geometry.
- Software moat: knob screens mirrored in the app, macros, night dimming tied to headlights, tilt page from the IMU, OTA from inside the app.

## 5. Open source to reuse

| Project | Reuse |
|---|---|
| https://github.com/sp-hy/Open-DiKey | Shark 6 verified BYD climate and ambient control plus BLE controller protocol. Start here. |
| https://github.com/afpineda/OpenSourceSimWheelESP32 | Mature ESP32 BLE button box: matrices, encoders, NimBLE HID |
| https://github.com/jamiehs/ble-button-box | Simple encoder and button BLE box |
| argaar/bledeck | BLE macro pad with per key RGB, encoder and OLED |
| https://github.com/ljgonzalez1/esp32-s3-rotary-lcd-examples | S3 + round LCD + encoder + BLE/USB HID examples |
| https://github.com/UsefulElectronics/esp32s3-gc9a01-lvgl | PCNT encoder + GC9A01 + LVGL knob UI |
| https://github.com/JshStadler/esp32-secure-ble-key | MIT Kotlin BLE app with cached MAC reconnect and HMAC auth |
| https://github.com/fritsjan/BLE-android-example-esp32 | Minimal Kotlin coroutine GATT client |
| https://github.com/h2zero/NimBLEOta and https://github.com/gb88/BLEOTA | BLE OTA |
| https://github.com/meatpiHQ/wican-fw | OBD BLE/ELM327 firmware |
| https://github.com/fr3ts0n/AndrOBD and https://github.com/barnhill/AndroidOBD | Android ELM327 parsing |

## Bill of materials (prototype, one unit)

| Item | Qty | USD | AUD | NZD |
|---|---|---|---|---|
| Seeed XIAO ESP32-S3 | 1 | 7.50 | 11 | 12.50 |
| 1.28 inch GC9A01 240x240 round IPS | 2 | 12 | 18 | 20 |
| EC11 encoder with push | 2 | 1 | 1.50 | 1.70 |
| Kailh Choc or 6x6 tactiles | 10 | 4 | 6 | 7 |
| SK6812 RGBW strip 60/m (1 m) | 1 | 8 | 12 | 13 |
| SK6812 mini 3535 | 12 | 3 | 4.50 | 5 |
| 74AHCT125 | 1 | 1 | 1.50 | 1.70 |
| MCP23017 | 1 | 1.50 | 2.30 | 2.50 |
| DRV2605L + LRA | 2 | 12 | 18 | 20 |
| MPR121 (optional) | 1 | 3 | 4.50 | 5 |
| USB-C breakout, caps, resistors, wire | 1 | 5 | 7.50 | 8 |
| 3D printed enclosure and keycaps | 1 | 6 | 9 | 10 |
| Control bar subtotal | | about 64 | about 96 | about 106 |
| WiCAN Pro OBD dongle (or XIAO S3 + SN65HVD230 about USD 10) | 1 | 89 | 135 | 148 |
| M5Stack Dial v1.1 (alt knob for rapid prototype) | 2 | 70 | 105 | 116 |

At volume (10 plus) a custom JLCPCB board with the ESP32-S3-WROOM-1 module lands the bar at roughly USD 35 to 45 in parts against Trigger's AUD 317 retail.

## Key conclusions

1. Trigger is a rebadged BLE button bar. "DiKey" is the BLE name of a real BYD console controller product that Open DiKey has already reverse engineered.
2. Build on ESP32-S3, NimBLE-Arduino 2.x, LovyanGFX + LVGL, PCNT encoders, SK6812 via RMT, NimBLEOta.
3. Ship a custom GATT service (or the Open DiKey protocol) plus a BLE HID consumer control profile so basic functions work with no app.
4. On the Android 11 head unit: FINE + BACKGROUND location via ADB, a connectedDevice foreground service, cached MAC with autoConnect=true, scan only as fallback.
5. Differentiators Trigger cannot match: live CAN/OBD data in the same app, haptics, touch strip, an enclosure moulded to the Shark 6 dash.
