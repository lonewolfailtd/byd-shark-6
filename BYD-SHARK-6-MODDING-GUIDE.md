# BYD Shark 6 Modding & Customization Guide

**Last Updated:** March 2026
**Firmware Versions Covered:** Up to 2503 and beyond
**Based on:** Community research from XDA Forums, YouTube guides (Trail Shark, BYD Buddy, SG BYD Gear), Facebook groups, GitHub repos, and owner experiences

---

## Table of Contents
1. [What You've Already Done](#what-youve-already-done)
2. [How to Enable ADB (Detailed Steps)](#how-to-enable-adb-detailed-steps)
3. [Safe Modifications (DO THIS)](#safe-modifications-do-this)
4. [Risky Modifications (CAUTION)](#risky-modifications-caution)
5. [Dangerous - Will Brick/Void Warranty (DON'T DO)](#dangerous---will-brickvoid-warranty-dont-do)
6. [Firmware Version Compatibility](#firmware-version-compatibility)
7. [What Others Have Done Successfully](#what-others-have-done-successfully)
8. [What Wrecked People's Head Units](#what-wrecked-peoples-head-units)
9. [Recovery Methods](#recovery-methods)
10. [Advanced ADB Commands](#advanced-adb-commands)
11. [Useful Resources](#useful-resources)

---

## What You've Already Done

You have successfully:
- Enabled Developer Options and USB Debugging
- Connected via ADB over WiFi (phone hotspot method)
- Installed the Downloader app via `adb install`

**Your current setup is working and safe.**

---

## How to Enable ADB (Detailed Steps)

### Method 1: Standard Developer Options (Pre-2407 Firmware)

**Step 1: Enable Developer Options**
1. On the head unit, go to **Car > System > Version**
2. Tap **"Factory Reset" text** (the text itself, NOT the button) **10 times rapidly**
3. A hidden menu appears
4. **Rotate the screen vertically** to reveal additional buttons
5. Press **"CONNECT USB TO ENABLE DEBUGGING MODE / REVOKE USB DEBUGGING ENABLE AUTHORIZATION"**

**Step 2: Connect via WiFi**
1. Turn on your **phone's hotspot**
2. Connect both **your laptop** and the **car head unit** to the same hotspot
3. On the car, go to **Settings > WiFi** > tap the connected network to find the car's IP address
4. On your laptop, run:
```bash
adb connect <car-ip-address>:5555
```
5. Verify with:
```bash
adb devices
```

### Method 2: IMEI-Based Bypass (Post-2407 Firmware)

BYD disabled developer tools in firmware 2407+. The community discovered this workaround:

1. Connect your **phone to the car via Bluetooth**
2. On the car's infotainment, dial a specific service number to access a **verification menu** showing your IMEI
3. Enter the IMEI on a specific verification website (time synchronization between car clock and website is critical)
4. The website generates a **one-time code** to re-enable ADB
5. Enter the code on the car to unlock developer tools
6. Proceed with standard ADB connection (Method 1, Step 2)

**Detailed guides:**
- `github.com/ahmada3mar/BYD` - Step-by-step with screenshots
- `github.com/murtaza9000-tech/automark-byd` - Includes APK packages

### Method 3: Unlocked PackageInstaller (No Laptop Needed After Setup)

Once you have ADB access, you can install a modified PackageInstaller that lets you install APKs directly from the car's own screen — from USB drives, web downloads, Telegram, file managers, etc.

1. Get the **PackageInstallerUnlocked.apk** (extracted from Chinese BYD Yuan Plus variant)
   - Found in: `github.com/MorghusDragon/BYD-Shark-Sideloading` Issue #1
2. Install via ADB: `adb install PackageInstallerUnlocked.apk`
3. After this, you can download and install APKs directly on the car without a laptop

**Why this matters:** After the one-time ADB setup, you never need your laptop again for app installs.

### Method 4: USB Sideloading (No ADB Required)

For firmware versions that still support it:
1. Format USB drive as **FAT32**
2. Create a folder called **`third party apps`**
3. Copy your APK files inside
4. Plug into the car's USB **data port**
5. Enter the password when prompted (see [Passwords Reference](#passwords-reference))

### Checking Your Firmware Version
On your head unit: **Settings > About > System Version**

---

## Safe Modifications (DO THIS)

### 1. App Installation via ADB
**Risk Level: LOW**
**Warranty Impact: Generally Safe**

Apps you can safely install:
- **Downloader** (you have this)
- **Aurora Store** - Google Play alternative
- **YouTube** / **YouTube ReVanced** - Ad-free YouTube
- **Spotify** (if not pre-installed)
- **Netflix**
- **Disney+**
- **Waze** / **Google Maps**
- **Firefox** browser
- **Poweramp** (music player)
- **App Manager** - For managing installed apps

**How to install:**
```bash
adb connect <car-ip>
adb install -r <app-name>.apk
```

### 2. Custom Launchers
**Risk Level: LOW**
**Warranty Impact: Safe (just an app)**

Recommended launchers for car use:
| Launcher | Pros | Cons |
|----------|------|------|
| **AGAMA Launcher** | Car-focused, large buttons, customizable colors | Paid |
| **Car Launcher Pro** | Designed for driving, big widgets | Some features paid |
| **CarWebGuru** | Popular, many themes | Can be overwhelming |
| **Nova Launcher** | Most customizable | May slow system, not car-optimized |

**Recommendation:** Start with AGAMA or Car Launcher Pro - designed for touchscreens while driving.

### 3. Wallpapers & Themes
**Risk Level: NONE**
**Warranty Impact: None**

- Change wallpaper via Settings or launcher
- Use wallpaper apps
- Custom icon packs (if launcher supports)

### 4. Blocking OTA Updates (Reversible)
**Risk Level: LOW**
**Warranty Impact: None (reversible)**

If you want to keep your current firmware and sideloading ability:
1. Install **App Manager**
2. Search for "OTA" in App Manager
3. Uninstall OTA-related apps
4. **Fully reversible** by factory reset

**Why do this:** Newer firmware (2503+) may block some sideloading methods.

---

## Risky Modifications (CAUTION)

### 1. GBOX / GBox for Google Services
**Risk Level: MEDIUM**
**What it does:** Provides Google Play Services compatibility

**Issues reported:**
- Older GBOX versions work better than newer ones
- Some apps crash or don't authenticate properly
- Netflix DRM issues
- MicroG services conflicts

**From BYD Buddy (July 2025):**
> "GBOX hiccups, ADB problems, Google services confusion... not always smooth sailing"

**Workaround:** Use older versions of GBOX from APKMirror.

### 2. Root Access (Magisk)
**Risk Level: HIGH**
**Warranty Impact: LIKELY VOIDS WARRANTY**

Process involves:
```bash
adb reboot bootloader
fastboot --disable-verity --disable-verification flash vbmeta vbmeta.img
```

**Only attempt if:**
- You have backup firmware ready
- You understand fastboot commands
- You're okay with potentially voiding warranty

### 3. Debloating System Apps
**Risk Level: MEDIUM-HIGH**

Using ADB to remove system apps:
```bash
pm uninstall --user 0 [package.name]
```

**Safe to remove (research first):**
- Unused BYD apps
- Bloatware

**NEVER remove:**
- Anything with "system" in name
- Anything with "framework"
- Launcher apps
- Settings apps
- Package manager

### 4. Installing from Unknown Sources
**Risk Level: MEDIUM**

**Safe sources:**
- APKMirror
- APKPure
- Aurora Store
- F-Droid
- GitHub releases

**Avoid:**
- Random websites
- "Cracked" APKs
- Apps from untrusted Telegram groups

---

## Dangerous - Will Brick/Void Warranty (DON'T DO)

### 1. Flashing Wrong Firmware
**WILL BRICK YOUR CAR**

**NEVER:**
- Flash firmware from a different BYD model (e.g., Atto 3 firmware on Shark 6)
- Flash firmware from a different region
- Flash firmware meant for different DiLink version
- Interrupt a firmware update mid-process

**From XDA Forums - User who bricked their head unit:**
> "It's been about a week and half and I haven't been able to get my radio back up... I'm about to throw this radio into the trash"

### 2. Removing Critical System Packages
**WILL SOFT-BRICK YOUR SYSTEM**

**NEVER uninstall:**
- `com.android.systemui`
- `com.android.settings`
- `com.android.launcher*`
- `com.byd.launcher*`
- `com.android.packageinstaller`
- Any package with "framework" in name
- Any package with "core" in name

### 3. Fastboot Commands Without Backup
**EXTREMELY DANGEROUS**

Commands like:
```bash
fastboot erase boot
fastboot flash boot <wrong-image>
fastboot --disable-verity (without proper vbmeta)
```

**Only have 5-10 seconds in fastboot before auto-reboot** - one wrong command can be unrecoverable.

### 4. Modifying /system Partition Without Root
Any attempt to modify system files without proper root access will fail and potentially corrupt the system.

---

## Firmware Version Compatibility

| Firmware | Sideloading Status | Notes |
|----------|-------------------|-------|
| **Pre-2307** | Full access | Easiest to mod |
| **2307** | Works | Last "easy" version |
| **2310** | Blocked USB sideloading | Removed developer tools, 2FA for installs |
| **2403** | Partially blocked | Vehicle location tracker added |
| **2407+** | Developer tools disabled | Requires IMEI-based bypass (see above) |
| **2503** | Blocked standard APK | Need workarounds (xapk, zip method) |
| **2412/2501+** | Most restricted | May need downgrade |

### 2503 Firmware Workaround
From YouTube comment (July 2025):
> "I'm actually on 2503 and BYD now blocks the install of the 3rd party apk. Xapk work though. And the workaround is to zip the apk and then explore the zip with App Manager and then it installs."

### Checking Your Firmware Version
On your head unit: Settings > About > System Version

---

## What Others Have Done Successfully

### YouTube Channel: Trail Shark (BYD Shark Specific)
**Video:** "How to Sideload Apps on the BYD Shark (Full Guide)" - August 2025

Successfully installed:
- Downloader app
- Chrome
- Navigation apps
- Entertainment apps

**Method:** Exact same as what you've done - ADB via WiFi

### YouTube Channel: BYD Buddy
**Issues covered:**
- GBOX installation
- Google Maps workarounds
- YouTube ReVanced vs BYD YouTube

**Key insight:** Older versions of apps often work better than newest versions.

### YouTube Channel: SG BYD Gear
**Video:** "BYD M6 - Guide on how to do Sideloading" - Works for all BYD models

**Confirmed working:**
- USB sideloading method with password
- ADB wireless method
- Aurora Store installation

### XDA Forums Success Stories
From user **insinc** (March 2023):
1. Enable USB ADB to sideload apps
2. Re-enable Wireless ADB via sideloaded package
3. Replace Package Manager with unrestricted version
4. Install apps via alternate Package Manager
5. Use Aurora Store for most apps
6. Use GBox (older version) for Google-dependent apps

### Facebook BYD Global Owners Club
**December 2024 Guide** for 2025:
- USB method still works with correct folder name
- Different countries need different folder prefixes
- Password varies by firmware version

---

## What Wrecked People's Head Units

### Case 1: Wrong Firmware Flash
**Platform:** XDA Forums
**What happened:** User tried to update but flashed wrong firmware
**Result:** Stuck on boot logo permanently
**Recovery:** Never recovered - radio replaced

### Case 2: Removing System Packages
**Platform:** Various forums
**What happened:** Used debloat script without understanding packages
**Result:** System unstable, apps crashing, eventually boot loop
**Recovery:** Factory reset (partial), firmware reflash (full)

### Case 3: Interrupted Firmware Update
**Platform:** BYD owner forums
**What happened:** Car was turned off during OTA update
**Result:** Corrupted system
**Recovery:** Engineering mode + USB firmware reflash

### Case 4: Bad Fastboot Commands
**Platform:** XDA Forums
**What happened:** Flashed wrong boot image during root attempt
**Result:** Complete brick
**Recovery:** Required professional repair

### Common Mistakes to Avoid
1. **Assuming all BYD models are the same** - They're not
2. **Using guides from different firmware versions** - Commands may not work
3. **Not backing up before major changes** - No recovery path
4. **Installing apps from untrusted sources** - Malware risk
5. **Modifying system without understanding** - Permanent damage

---

## Recovery Methods

### Method 1: Soft Reset
For minor issues:
1. Long-press power button on screen
2. Or: Settings > System > Reset

### Method 2: Engineering Mode Recovery
For boot loops or stuck systems:
1. Hold **steering wheel 'left' button** (volume roller area) + **center console volume wheel button**
2. Hold for ~10 seconds
3. Unit reboots into engineering mode
4. Can reflash firmware from USB

### Method 3: USB Firmware Reflash
If you can access engineering mode:
1. Download correct firmware from `wiki.defective.tech/BYD/Firmware`
2. Format USB to FAT32/exFAT
3. Create folder: `BYDUpdatePackage/msm8953_64/`
4. Copy `UpdateFull.zip` inside
5. Boot into engineering mode
6. Follow on-screen instructions
7. Wait 30-60 minutes - **DO NOT INTERRUPT**

### Method 4: Factory Reset
Nuclear option - loses all data:
- Settings > System > Factory Reset
- Or via engineering mode

---

## Advanced ADB Commands

### Display & UI Tweaks
```bash
# Change DPI (UI scaling) - experiment to find best value for your screen
adb shell wm density 400
adb shell wm density reset

# Change resolution
adb shell wm size 1080x2400
adb shell wm size reset

# Disable animations (makes system feel faster)
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Re-enable animations
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
```

### App Management
```bash
# List all installed packages
adb shell pm list packages

# List only third-party (sideloaded) apps
adb shell pm list packages -3

# Allow Aurora Store to install apps + storage access
adb shell appops set com.aurora.store REQUEST_INSTALL_PACKAGES allow
adb shell appops set com.aurora.store WRITE_EXTERNAL_STORAGE allow
adb shell appops set com.aurora.store MANAGE_EXTERNAL_STORAGE allow

# Force stop a misbehaving app
adb shell am force-stop com.package.name

# Clear app data (reset an app)
adb shell pm clear com.package.name

# Grant a specific permission
adb shell pm grant com.package.name android.permission.WRITE_EXTERNAL_STORAGE
```

### System Info & Diagnostics
```bash
# Take a screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png

# Record screen (max 3 minutes)
adb shell screenrecord /sdcard/video.mp4

# View real-time system logs (useful for debugging crashes)
adb logcat

# Dump log to file
adb logcat -d > byd-log.txt

# Check battery/system info
adb shell dumpsys battery

# See what app is currently in foreground
adb shell dumpsys activity activities | grep mResumedActivity
```

### File Transfer
```bash
# Copy file from PC to car
adb push local-file.apk /sdcard/

# Copy file from car to PC
adb pull /sdcard/some-file.txt ./

# Copy custom boot animation (BACKUP ORIGINAL FIRST)
adb pull /system/media/bootanimation.zip ./bootanimation-backup.zip
```

### Theme Overlays (DiLink)
```bash
# List available overlays
adb shell cmd overlay list

# Enable an overlay
adb shell cmd overlay enable <overlay-name>

# Disable an overlay
adb shell cmd overlay disable <overlay-name>
```

### Input Simulation (Remote Control)
```bash
# Simulate a screen tap at x,y coordinates
adb shell input tap 500 500

# Simulate a swipe
adb shell input swipe 100 500 900 500

# Type text
adb shell input text "hello"

# Simulate home button
adb shell input keyevent 3

# Simulate back button
adb shell input keyevent 4
```

---

## Useful Resources

### Websites
- **XDA Forums BYD Thread:** `xdaforums.com/t/byd-multimedia-install-apk.4541247/`
- **Defective Tech Wiki (Firmware):** `wiki.defective.tech/BYD/Firmware`
- **Defective Tech Wiki (Fastboot):** `wiki.defective.tech/BYD/Upgrading/Fastboot`
- **Just BYD Forum:** `forums.justbyd.com`
- **BYD Owners Forum:** `bydowners.com`
- **One Finite Planet (Guides):** `onefiniteplanet.org/webpapers/byd-atto-3-software-and-tips/`
- **BYD Sideloading Guide:** `byd.deskblur.com/us`
- **All Terrain BYD Notes:** `allterrain.app/byd/`
- **GBox Lab:** `gboxlab.com`

### YouTube Channels
- **Trail Shark** - BYD Shark specific guides
- **BYD Buddy** - General BYD tips and sideloading
- **SG BYD Gear** - Sideloading tutorials
- **AleTech** - Third party apps 2025
- **BYD CLUB ITALIA** - Sentry mode and advanced mods

### GitHub Repositories
- `github.com/MorghusDragon/BYD-Shark-Sideloading` - Shark-specific sideloading guide
- `github.com/ahmada3mar/BYD` - Tips, tricks, and post-2407 ADB bypass
- `github.com/murtaza9000-tech/automark-byd` - ADB bypass with APK packages included
- `github.com/MuntashirAkon/AppManager` - App Manager releases
- `github.com/BYDcar/BYDGlobalFactoryImages1` - Factory images and repair manuals
- `github.com/Niek/BYD-re` - BYD reverse engineering
- `github.com/loryanstrant/BYD-PID-list` - OBD2 PID listing for BYD vehicles

### Hardware Add-ons
- **Smart World App2Car Box** - Multimedia box that plugs into wired Android Auto USB port, enables direct app downloads without ADB. Compatible with Shark 6. Available at `smartworldcompany.com`
- **WiCAN Pro** - ESP32-based OBD2 adapter, integrates with Home Assistant for vehicle data monitoring. Available at `crowdsupply.com/meatpi-electronics/wican-pro`

### Facebook Groups
- BYD Global Owners Club
- BYD Owners Club Malaysia
- BYD Shark 6 Camping, Off-road, Setups/Mods
- Regional BYD groups

### Telegram
- Multiple BYD groups available
- Search "BYD" in Telegram

### Other References
- **Scribd:** ADB Debug DiLink 5 document - `scribd.com/document/914887883/ADB-Debug-Dilink-5`

---

## Passwords Reference

For USB sideloading (folder: "third party apps"):

| Firmware/Region | Password |
|-----------------|----------|
| Older firmware | `20211231` |
| v1.5+ (NZ/Australia) | `BYD6125F` |
| v1.6.1+ | `GHY0613byd` |

---

## Recommended Setup for BYD Shark 6

### Safe Starting Point (What to Install)
1. **Downloader** - Already installed
2. **Aurora Store** - For easy app downloads
3. **App Manager** - For managing apps and permissions
4. **Firefox** - Alternative browser
5. **YouTube ReVanced** - Ad-free YouTube
6. **Custom Launcher** (optional) - AGAMA or Car Launcher Pro

### What NOT to Install Yet
- Root tools (Magisk)
- System modifiers
- Anything that requires system partition access
- Untested GBOX versions

### Customization Path (Safest Order)
1. Install basic apps via ADB (current)
2. Install Aurora Store for easier future installs
3. Try a custom launcher
4. Customize wallpaper/themes
5. (Optional) Block OTA if you want to keep current setup

---

## Summary: Quick Reference

| Modification | Safety | Do It? |
|--------------|--------|--------|
| Install apps via ADB | SAFE | YES |
| Custom launcher | SAFE | YES |
| Wallpaper/themes | SAFE | YES |
| Aurora Store | SAFE | YES |
| Block OTA updates | LOW RISK | Optional |
| GBOX for Google Services | MEDIUM RISK | Carefully |
| Debloating bloatware | MEDIUM RISK | Research first |
| Root (Magisk) | HIGH RISK | Only if experienced |
| Firmware downgrade | HIGH RISK | Only if necessary |
| Flash firmware | DANGEROUS | Only for recovery |
| Remove system apps | DANGEROUS | NO |
| Fastboot commands | DANGEROUS | NO (unless expert) |

---

## Disclaimer

This guide is based on community research and owner experiences. Modifying your vehicle's infotainment system:
- May void your warranty
- Could cause system instability
- Is done at your own risk

The author is not responsible for any damage caused by following this guide. Always backup and research before making changes.

---

*Document created based on research from XDA Forums, YouTube (Trail Shark, BYD Buddy, SG BYD Gear, AleTech), Facebook groups, and BYD owner communities.*
