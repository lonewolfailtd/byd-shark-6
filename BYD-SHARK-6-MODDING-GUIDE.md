# BYD Shark 6 Modding & Customization Guide

**Last Updated:** December 2024
**Firmware Versions Covered:** Up to 2503 and beyond
**Based on:** Community research from XDA Forums, YouTube guides (Trail Shark, BYD Buddy, SG BYD Gear), Facebook groups, and owner experiences

---

## Table of Contents
1. [What You've Already Done](#what-youve-already-done)
2. [Safe Modifications (DO THIS)](#safe-modifications-do-this)
3. [Risky Modifications (CAUTION)](#risky-modifications-caution)
4. [Dangerous - Will Brick/Void Warranty (DON'T DO)](#dangerous---will-brickvoid-warranty-dont-do)
5. [Firmware Version Compatibility](#firmware-version-compatibility)
6. [What Others Have Done Successfully](#what-others-have-done-successfully)
7. [What Wrecked People's Head Units](#what-wrecked-peoples-head-units)
8. [Recovery Methods](#recovery-methods)
9. [Useful Resources](#useful-resources)

---

## What You've Already Done

You have successfully:
- Enabled Developer Options and USB Debugging
- Connected via ADB over WiFi (phone hotspot method)
- Installed the Downloader app via `adb install`

**Your current setup is working and safe.**

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

## Useful Resources

### Websites
- **XDA Forums BYD Thread:** `xdaforums.com/t/byd-multimedia-install-apk.4541247/`
- **Defective Tech Wiki (Firmware):** `wiki.defective.tech/BYD/Firmware`
- **Just BYD Forum:** `forums.justbyd.com`
- **One Finite Planet (Guides):** `onefiniteplanet.org/webpapers/byd-atto-3-software-and-tips/`

### YouTube Channels
- **Trail Shark** - BYD Shark specific guides
- **BYD Buddy** - General BYD tips and sideloading
- **SG BYD Gear** - Sideloading tutorials
- **AleTech** - Third party apps 2025

### GitHub
- `github.com/ahmada3mar/BYD` - Tips and tricks
- `github.com/MuntashirAkon/AppManager` - App Manager releases

### Facebook Groups
- BYD Global Owners Club
- BYD Owners Club Malaysia
- Regional BYD groups

### Telegram
- Multiple BYD groups available
- Search "BYD" in Telegram

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
