# Shark 6 lighting API (from BYD CarSettings 56.1.2.2608110.1, decompiled with jadx)

Source: research/decompiled/BydCarSettings (not committed, regenerate with tools/jadx). Presenters under b/b/g/n/i/ ("AtmLampColorsPresenter", "AtmLampBrightnessPresenter", "SingleSwitchPresenter"), wrappers in b/b/g/n/q/m.java (Setting device) and b/b/g/n/q/i.java (Light device).

Convention for every switch in these apps: 1 = on, 2 = off (`z ? 1 : 2`).

## Ambient (interior) light, "IAL"

| Action | Call | Notes |
|---|---|---|
| Zone id | SettingDevice.getIALArea() | BYD's UI always reads the area from the vehicle, never hard codes it |
| Read colour | SettingDevice.getIALColor(area) | slider value; min and max come from the settings item config |
| Set colour | SettingDevice.setIALColor(area, value, 0) | third arg 0 in the UI |
| Read brightness | SettingDevice.getIALBrightness(area) | |
| Set brightness | SettingDevice.setIALBrightness(area, level, 0) | |
| On/off | LightDevice.setAmbientMulticolorState(area, 1/2) | read getAmbientMulticolorState(area) == 1 |
| Mode | LightDevice.setAmbientMulticolorMode(area, mode) | read getAmbientMulticolorMode(area) |
| Music mode | SettingDevice.setAmbientMusicModeState(area, 1/2) | read getAmbientMusicModeState(area) |
| Support flags | SettingDevice.getAmbientLightSupport(), getAmbientLightColorSupport(), LightDevice.getAmbientColorsSupport() | |

setAmbientState(1) on the Light device returns -1 on the Shark 6 (verified 18 Sep 2026). Do not use.

## Exterior and convenience lights (CarLightsPresenter, b/b/g/n/h/e.java)

| Action | Call |
|---|---|
| AFS (adaptive front light) | LightDevice.setAFSSwitchSate(1/2) |
| Daytime running lights | LightDevice.setDayTimeLightState(1/2) |
| Front fog | LightDevice.setFrontFogLightSwitchState(1/2) |
| Rear fog | LightDevice.setRearFogLightSwitchState(1/2) |
| Cargo (tub) light | LightDevice.setCargoLightSwitchState(1/2), read getCargoLightSwitchState() |
| Headlight control mode | LightDevice.setHeadlightControlMode(mode) |
| Smart welcome light | SettingDevice.setSmartWelcomeLightState(1/2) |
| Leave home light delay | SettingDevice.setLeftHomeLightDelayValue(v) |
| Back home light delay | SettingDevice.setBackHomeLightDelayValue(v) |

Also seen in the Setting device: getCreepModeState / creep speed (Crawl mode), wading pattern state and speed tips, delay power off. Useful for the Off Road page.

## Seat position (checked 18 Sep 2026): not available on the Shark 6

BYD CarSettings only enables screen seat movement when getMainDriverSeatAdjustmentConfigurationByGCtrl or ...ByLeftBodyCtrl == 3. On this ute both read 0, getPassengerSeatElectricAdjustmentConfigurationByECU is 0, and every getDriverSeat*Position() returns 255 (invalid). turnSeatHorization/Height/backrest/Cushion(1, dir) are accepted but the body controller ignores them. The seat motors only answer the physical switches. SeatScreen.kt is kept in the tree but not in the tab bar.
