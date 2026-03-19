# BYD Shark 6 - Complete Customization & Apps Guide

**Last Updated:** March 2026
**Your Setup:** ADB via WiFi working, Downloader app installed

---

## Table of Contents
1. [Launchers (Change Your Home Screen)](#1-launchers-change-your-home-screen)
2. [Wallpapers & Backgrounds](#2-wallpapers--backgrounds)
3. [Icon Packs & Themes](#3-icon-packs--themes)
4. [Widgets](#4-widgets)
5. [Streaming & Entertainment Apps](#5-streaming--entertainment-apps)
6. [Navigation Apps](#6-navigation-apps)
7. [Music & Audio Apps](#7-music--audio-apps)
8. [Utility Apps](#8-utility-apps)
9. [Security & Dashcam Apps](#9-security--dashcam-apps)
10. [OBD2 & Vehicle Data Apps](#10-obd2--vehicle-data-apps)
11. [What Other BYD Owners Have Done](#11-what-other-byd-owners-have-done)
12. [APK Download Sources](#12-apk-download-sources)
13. [Installation Commands](#13-installation-commands)

---

## 1. Launchers (Change Your Home Screen)

### Top Recommended Launchers for Car Use

#### **AGAMA Car Launcher** ⭐ HIGHLY RECOMMENDED
- **Best for:** Clean, customizable car-focused interface
- **Features:**
  - Large buttons designed for driving
  - Car manufacturer logos (including custom)
  - Infinite color customization
  - Speedometer widget
  - Auto-switching widgets (changes based on what car is doing)
  - Album art display when music changes
- **Price:** Paid (worth it)
- **APK Source:** APKMirror or Google Play
- **Install:** `adb install agama-launcher.apk`

#### **CarWebGuru Launcher** ⭐ MOST CUSTOMIZABLE
- **Best for:** Maximum customization, tons of themes
- **Features:**
  - 100+ free and premium themes
  - Large speedometer options
  - GPS tracker built-in
  - Music player with visualization
  - Support for system widgets
  - Background image customization
  - Geographic track recording
- **Price:** Free with paid themes
- **APK Source:** Google Play, APKPure
- **Install:** `adb install carwebguru.apk`

#### **Car Launcher Pro**
- **Best for:** Simple, driving-focused layout
- **Features:**
  - Big widget areas
  - Quick access buttons
  - Music controls
  - Designed for touch while driving
- **Price:** Paid
- **APK Source:** APKMirror

#### **VIVID Launcher**
- **Best for:** Modern look, vertical screen support
- **Features:**
  - Highly customizable
  - Works on vertical screens
  - Modern UI design
- **Price:** Paid
- **Note:** May not be on Play Store anymore, check APKMirror

#### **Nova Launcher**
- **Best for:** If you want phone-like customization
- **Features:**
  - Most customizable Android launcher
  - Icon packs support
  - Gestures
  - "OK Google" support
- **Cons:** Not designed for car use, may slow down head unit
- **Price:** Free/Paid Pro
- **APK Source:** Google Play, APKMirror

#### **DuDu Launcher** (Mentioned by BYD owner)
- **Best for:** All controls on one screen
- **Features:**
  - One-screen access to all apps and controls
  - Simple setup
- **Source:** Search APKPure

### Launcher Comparison Table

| Launcher | Car-Optimized | Themes | Widgets | Price | Ease of Use |
|----------|---------------|--------|---------|-------|-------------|
| AGAMA | Yes | Many | Yes | Paid | Easy |
| CarWebGuru | Yes | 100+ | Yes | Free/Paid | Medium |
| Car Launcher Pro | Yes | Some | Yes | Paid | Easy |
| VIVID | Yes | Many | Yes | Paid | Medium |
| Nova | No | Many | Yes | Free/Paid | Complex |

---

## 2. Wallpapers & Backgrounds

### Built-in BYD Options
- New OTA 2.0 (firmware 2506) includes "Liquid Horizon" animated wallpaper
- Standard BYD wallpapers in Settings

### Custom Wallpapers

#### **Electric Seal Custom Wallpapers**
- Creator makes BYD-specific wallpaper packs
- "Ambient Abstract Wallpaper Set" (Liquid Horizon + 9 more)
- Available at: `electricseal.gumroad.com`

#### **Wallpaper Apps to Install**
1. **Wallpaper Engine** - Animated wallpapers
2. **Zedge** - Massive wallpaper library
3. **Backdrops** - Clean, minimal wallpapers
4. **Walli** - Artist wallpapers

#### **DIY Custom Wallpapers**
- Resolution for BYD screens: Usually 1920x1080 or 1920x1280
- Use any image editing app
- Transfer via USB or download via browser

### Screensavers
- **Kustom Live Wallpaper (KLWP)** - Create animated wallpapers
- **Muzei** - Automatic wallpaper rotation
- Clock screensavers (various)

---

## 3. Icon Packs & Themes

### Icon Packs (Work with Nova, AGAMA, etc.)
1. **Whicons** - White minimal icons
2. **Flight** - Flat colorful icons
3. **Delta** - Triangle/geometric icons
4. **Lines** - Line-art icons
5. **Viral** - Colorful vibrant icons
6. **Minima** - Super minimal icons
7. **CandyCons** - Colorful material icons

### How to Apply Icon Packs
1. Install icon pack APK
2. Open your launcher settings
3. Find "Icon Pack" or "Icons" option
4. Select installed pack

### CarWebGuru Themes
- Access via CarWebGuru app
- Hundreds of car-themed designs
- Many free options
- Premium themes available

---

## 4. Widgets

### Best Widgets for Car Screens

#### **Speedometer Widgets**
- **DigiHUD Speedometer** - Large digital speed display
- **Speedometer GPS** - Uses GPS for speed
- **AGAMA built-in** - Already included
- **CarWebGuru built-in** - Multiple styles

#### **Music Widgets**
- **Media Widget** - Controls any music app
- **Music Widget** - Album art + controls
- **Built-in launcher widgets** - Most launchers have these

#### **Clock Widgets**
- **Digital Clock Widget** - Large, readable
- **Chronus** - Weather + clock combo
- **KWGT** - Custom widget maker

#### **Weather Widgets**
- **1Weather** - Clean design
- **Weather Timeline** - Detailed forecasts
- **Geometric Weather** - Modern look

#### **Navigation Widgets**
- **Google Maps Widget** - Quick directions
- **Waze Widget** - Traffic alerts

#### **OBD2/Vehicle Widgets**
- **Torque** - Real-time car data
- **Car Scanner** - Vehicle diagnostics
- **RealDash** - Full dashboard replacement

### Widget Apps
- **KWGT (Kustom Widget)** - Create custom widgets
- **Widgetsmith** - Simple custom widgets
- **HD Widgets** - Clock, weather, battery

---

## 5. Streaming & Entertainment Apps

### Video Streaming

#### **YouTube** ⭐ MUST HAVE
- **Options:**
  - Official YouTube app
  - **YouTube ReVanced** (ad-free, background play) - RECOMMENDED
  - **YouTube Vanced** (older, still works)
- **Install:** `adb install youtube-revanced.apk`
- **Note:** ReVanced needs MicroG for login

#### **Netflix**
- Works on BYD head units
- May need older version for DRM compatibility
- **Tip:** Try Netflix 7.x or 8.x versions from APKMirror if latest doesn't work
- Some users report error -1033, try older versions

#### **Disney+**
- Generally works
- May have DRM issues on some units

#### **Spotify** (Built-in on some BYDs)
- If not pre-installed, sideload it
- Works well on head units

#### **Amazon Prime Video**
- Sideloadable
- May have casting/quality limitations

#### **Plex**
- Great for personal media library
- Stream from home server

#### **VLC Player**
- Play any video format
- Local file playback

### TV/Live Streaming
- **Twitch** - Live streaming
- **Pluto TV** - Free TV
- **Tubi** - Free movies/shows

---

## 6. Navigation Apps

### **Google Maps**
- Works via GBOX or direct install
- May need older versions for full functionality
- **Note:** Requires Google services (use GBOX or MicroG)

### **Waze** ⭐ RECOMMENDED
- Community-based traffic
- Police/hazard alerts
- Works without Google services
- **Install:** `adb install waze.apk`

### **HERE WeGo**
- Offline maps
- No Google services needed
- Great for areas with poor signal

### **Sygic**
- Premium offline navigation
- Truck/caravan modes
- Speed camera alerts

### **A Better Route Planner (ABRP)**
- EV-specific navigation
- Charging stop planning
- Battery estimation
- **MUST HAVE for EV road trips**

### **TomTom GO Navigation**
- Offline capability
- Speed cameras
- Premium quality

### **OsmAnd**
- Open source
- Fully offline
- Highly customizable

---

## 7. Music & Audio Apps

### Music Players

#### **Poweramp** ⭐ BEST SOUND
- Best audio quality
- Powerful equalizer
- All format support
- **Note:** License validation may need workaround (manual activation)
- **Install:** `adb install poweramp.apk`

#### **Musicolet**
- Lightweight
- No internet required
- Free

#### **BlackPlayer**
- Clean interface
- Good equalizer

#### **Pulsar**
- Material design
- Tag editor

### Streaming Music
- **Spotify** (pre-installed or sideload)
- **YouTube Music** (needs Google services)
- **Tidal** - High quality audio
- **Deezer**
- **SoundCloud**

### Podcasts
- **Pocket Casts** - Best podcast app
- **Google Podcasts**
- **Spotify** (has podcasts built-in)

### Audiobooks
- **Audible** - May need Google services
- **Smart AudioBook Player** - For local files
- **Libby** - Library audiobooks

---

## 8. Utility Apps

### Essential Utilities

#### **Aurora Store** ⭐ MUST HAVE
- Google Play alternative
- No Google account needed
- Install apps easily
- **Install:** `adb install aurora-store.apk`

#### **App Manager** ⭐ MUST HAVE
- Manage installed apps
- Grant permissions
- Uninstall system apps safely
- **Source:** GitHub releases

#### **Firefox Browser**
- Alternative to built-in browser
- Download APKs directly
- Privacy focused

#### **File Manager**
- **Solid Explorer** - Best file manager
- **Total Commander** - Powerful
- **FX File Explorer** - Simple

#### **Downloader** (You have this!)
- Download APKs via URL
- Fire TV style

### Connectivity
- **Bluetooth Auto Connect** - Fix BT issues
- **WiFi Analyzer** - Check signal strength

### System Tools
- **CPU-Z** - System information
- **AIDA64** - Hardware info
- **3C Toolbox** - All-in-one system tool

---

## 9. Security & Dashcam Apps

### **Sentry Mode** ⭐ POPULAR MOD

BYD owners have created apps to enable Tesla-like Sentry Mode using the built-in 360-degree cameras!

#### **How it works:**
- Uses existing 360 cameras
- Records when motion detected
- Alerts via screen
- Saves footage to storage

#### **Requirements:**
- Two apps from BYD community developers
- ADB access
- USB storage for recordings
- Battery usage warning (keeps car awake)

#### **Installation:**
- Watch: "How to install and set up the Sentry Mode on all the BYDs!" by BYD CLUB ITALIA (1hr tutorial)
- Requires multiple APKs and configuration

#### **Notes:**
- May have Chinese voice notifications
- Battery drain consideration
- Community-developed, not official BYD

### Dashcam Apps
- **AutoBoy Dash Cam** - Use phone as dashcam
- **CamOnRoad** - Dashcam with cloud backup
- **Smart Dash Cam** - Simple recording

### Official BYD Dashcam
- BYD sells official dashcam accessory
- Integrates with head unit
- Recommended for reliability

---

## 10. OBD2 & Vehicle Data Apps

### **Torque Pro/Lite**
- Real-time vehicle data
- Custom gauges
- Fault code reading
- Track logging

### **Car Scanner**
- OBD2 diagnostics
- Live data
- Clean interface

### **RealDash** ⭐ FULL DASHBOARD REPLACEMENT
- Custom digital dashboards
- OBD2 integration
- Import custom gauge designs
- Can replace entire launcher

### **OBD Fusion**
- Cross-platform
- Data logging

### Requirements:
- OBD2 Bluetooth adapter
- May have limited functionality with BYD's proprietary systems

---

## 11. What Other BYD Owners Have Done

### Common Setups

#### **Setup 1: Entertainment Focus**
- AGAMA Launcher
- YouTube ReVanced
- Netflix
- Spotify
- Waze

#### **Setup 2: Utility Focus**
- CarWebGuru Launcher
- Aurora Store
- Google Maps (via GBOX)
- Torque Pro
- ABRP

#### **Setup 3: Maximum Customization**
- Nova Launcher
- KWGT widgets
- Custom icon pack
- Custom wallpaper
- Multiple music apps

#### **Setup 4: Tesla-like Experience**
- Sentry Mode apps
- RealDash dashboard
- ABRP navigation
- YouTube for in-car entertainment

### Popular Combinations from Forums

**From XDA Forums user:**
1. Unrestricted Package Manager
2. App Manager
3. Aurora Store
4. Wireless ADB Switch
5. Firefox
6. GBOX (older version) for Google apps

**From BYD Malaysia group:**
1. Sideloaded apps via USB
2. Netflix for passengers
3. YouTube
4. Alternative navigation

**From Trail Shark (Shark 6 specific):**
1. Downloader app
2. Chrome browser
3. Navigation apps
4. Entertainment apps

---

## 12. APK Download Sources

### Trusted Sources

| Source | URL | Best For |
|--------|-----|----------|
| **APKMirror** | apkmirror.com | Most apps, multiple versions |
| **APKPure** | apkpure.com | Wide selection |
| **Aurora Store** | (install as app) | Easy browsing |
| **F-Droid** | f-droid.org | Open source apps |
| **GitHub** | github.com | Developer apps like App Manager |
| **Uptodown** | uptodown.com | Alternative source |

### Apps with Direct APK Links

- **Aurora Store:** `auroraoss.com`
- **App Manager:** `github.com/MuntashirAkon/AppManager/releases`
- **YouTube ReVanced:** `revanced.app` or GitHub
- **MicroG:** `microg.org`

### Avoid
- Random APK sites
- "Cracked" or "modded" APKs from unknown sources
- Telegram groups with suspicious files

---

## 13. Installation Commands

### Basic Installation
```bash
# Connect to car
adb connect <car-ip-address>

# Verify connection
adb devices

# Install app
adb install -r <app-name>.apk
```

### Common Apps Installation
```bash
# Aurora Store
adb install aurora-store.apk

# YouTube ReVanced
adb install youtube-revanced.apk

# AGAMA Launcher
adb install agama-launcher.apk

# Waze
adb install waze.apk

# Firefox
adb install firefox.apk
```

### Useful ADB Commands
```bash
# List installed packages
adb shell pm list packages

# List only sideloaded (third-party) apps
adb shell pm list packages -3

# Uninstall app
adb uninstall com.package.name

# Force stop app
adb shell am force-stop com.package.name

# Clear app data
adb shell pm clear com.package.name

# Grant permission
adb shell pm grant com.package.name android.permission.WRITE_EXTERNAL_STORAGE

# Allow Aurora Store to install apps + storage access
adb shell appops set com.aurora.store REQUEST_INSTALL_PACKAGES allow
adb shell appops set com.aurora.store WRITE_EXTERNAL_STORAGE allow
adb shell appops set com.aurora.store MANAGE_EXTERNAL_STORAGE allow

# Take a screenshot of the head unit
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png

# Check what's running in foreground
adb shell dumpsys activity activities | grep mResumedActivity
```

### Firmware 2503+ Workaround
If standard `adb install` is blocked:
1. Rename `.apk` to `.zip` (e.g., `aurora-store.apk` -> `aurora-store.zip`)
2. Transfer to car: `adb push aurora-store.zip /sdcard/`
3. Open **App Manager** on the car
4. Navigate to the zip file and install from there
5. Alternatively, use `.xapk` format files which still work

### Using Downloader App (Easier Method)
1. Open Downloader on car
2. Enter URL of APK
3. Download and install directly
4. No laptop needed!

---

## Quick Start Recommendations

### If You Want to Customize Looks:
1. Install **AGAMA Launcher** or **CarWebGuru**
2. Browse themes/customize colors
3. Add widgets (clock, music, weather)
4. Set custom wallpaper

### If You Want Entertainment:
1. Install **Aurora Store**
2. Get **YouTube ReVanced** (ad-free)
3. Install **Netflix** (try older version if issues)
4. Add **Spotify** if not pre-installed

### If You Want Better Navigation:
1. Install **Waze** (easy, no Google needed)
2. Or set up **GBOX** + **Google Maps**
3. Install **ABRP** for EV trip planning

### If You Want Full Customization:
1. Install **Nova Launcher**
2. Get **KWGT** for custom widgets
3. Install icon pack of choice
4. Create personalized home screens

---

## Useful YouTube Channels

- **Trail Shark** - BYD Shark specific tutorials
- **BYD Buddy** - General BYD tips and app guides
- **SG BYD Gear** - Sideloading tutorials
- **Saab Unleashed** - Android head unit launchers (general)
- **BYD CLUB ITALIA** - Sentry mode and advanced mods
- **Electric Seal** - BYD UI customization and wallpapers

---

## Notes

- Always test apps before relying on them
- Keep backup of stock setup (know how to reset)
- Some apps may not work with latest firmware
- Join BYD owner groups for latest tips and APK versions
- When in doubt, stick with well-known apps

---

*This guide is based on community research from XDA Forums, YouTube channels, Facebook groups, and BYD owner experiences. Always proceed with caution when modifying your vehicle's systems.*
