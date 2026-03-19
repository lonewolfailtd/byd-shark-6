#!/bin/bash
# =============================================================
# BYD Shark 6 - ADB Connection & App Management Tool
# =============================================================
# Run this script on your laptop when connected to the same
# WiFi/hotspot as your car's head unit.
#
# Prerequisites:
#   - ADB installed (Android Platform Tools)
#   - Developer Options + USB Debugging enabled on the car
#   - Both laptop and car on the same network (phone hotspot works)
#
# Usage:
#   chmod +x byd-adb-tool.sh
#   ./byd-adb-tool.sh
# =============================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Default ADB port for BYD
ADB_PORT=5555

# APK download directory
APK_DIR="./apks"

print_banner() {
    echo -e "${CYAN}"
    echo "╔═══════════════════════════════════════════════╗"
    echo "║        BYD Shark 6 - ADB Tool                ║"
    echo "║        Connection & App Manager               ║"
    echo "╚═══════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_menu() {
    echo -e "${BOLD}═══════════════════════════════════════════════${NC}"
    echo -e "${GREEN} 1)${NC} Connect to car"
    echo -e "${GREEN} 2)${NC} Check connection status"
    echo -e "${GREEN} 3)${NC} Install an APK"
    echo -e "${GREEN} 4)${NC} Install all APKs in ./apks/ folder"
    echo -e "${GREEN} 5)${NC} List installed third-party apps"
    echo -e "${GREEN} 6)${NC} List ALL installed packages"
    echo -e "${GREEN} 7)${NC} Uninstall an app"
    echo -e "${GREEN} 8)${NC} Take screenshot of head unit"
    echo -e "${GREEN} 9)${NC} Record head unit screen"
    echo -e "${GREEN}10)${NC} Push file to car"
    echo -e "${GREEN}11)${NC} Pull file from car"
    echo -e "${GREEN}12)${NC} Disable animations (speed boost)"
    echo -e "${GREEN}13)${NC} Re-enable animations"
    echo -e "${GREEN}14)${NC} Allow Aurora Store installs"
    echo -e "${GREEN}15)${NC} View system logs (logcat)"
    echo -e "${GREEN}16)${NC} Show system info"
    echo -e "${GREEN}17)${NC} Reboot head unit"
    echo -e "${GREEN}18)${NC} Disconnect"
    echo -e "${RED} 0)${NC} Exit"
    echo -e "${BOLD}═══════════════════════════════════════════════${NC}"
}

check_adb() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}ERROR: ADB is not installed or not in PATH.${NC}"
        echo "Install Android Platform Tools first:"
        echo "  - Download from: https://developer.android.com/tools/releases/platform-tools"
        echo "  - Or: winget install Google.PlatformTools (Windows)"
        echo "  - Or: brew install android-platform-tools (macOS)"
        echo "  - Or: sudo apt install adb (Linux)"
        exit 1
    fi
}

connect_car() {
    echo -e "${YELLOW}Enter the car's IP address (found in Settings > WiFi > connected network):${NC}"
    read -r CAR_IP

    if [ -z "$CAR_IP" ]; then
        echo -e "${RED}No IP address entered.${NC}"
        return 1
    fi

    echo -e "${BLUE}Connecting to ${CAR_IP}:${ADB_PORT}...${NC}"
    result=$(adb connect "${CAR_IP}:${ADB_PORT}" 2>&1)
    echo "$result"

    if echo "$result" | grep -q "connected"; then
        echo -e "${GREEN}Successfully connected to BYD Shark 6!${NC}"
        # Save IP for future use in this session
        export BYD_IP="$CAR_IP"
    else
        echo -e "${RED}Connection failed. Check:${NC}"
        echo "  1. Both devices are on the same WiFi/hotspot"
        echo "  2. Developer Options and USB Debugging are enabled on the car"
        echo "  3. The IP address is correct"
        echo "  4. No firewall is blocking port ${ADB_PORT}"
    fi
}

check_status() {
    echo -e "${BLUE}Connected devices:${NC}"
    adb devices
    echo ""
    device_count=$(adb devices | grep -c "device$")
    if [ "$device_count" -gt 0 ]; then
        echo -e "${GREEN}${device_count} device(s) connected.${NC}"
    else
        echo -e "${YELLOW}No devices connected. Use option 1 to connect.${NC}"
    fi
}

install_apk() {
    echo -e "${YELLOW}Enter the path to the APK file (or drag & drop):${NC}"
    read -r APK_PATH

    # Remove quotes if present (from drag & drop)
    APK_PATH="${APK_PATH//\"/}"
    APK_PATH="${APK_PATH//\'/}"

    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}File not found: ${APK_PATH}${NC}"
        return 1
    fi

    echo -e "${BLUE}Installing $(basename "$APK_PATH")...${NC}"
    adb install -r "$APK_PATH"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully installed!${NC}"
    else
        echo -e "${RED}Installation failed.${NC}"
        echo -e "${YELLOW}If on firmware 2503+, try the ZIP workaround:${NC}"
        echo "  1. Rename .apk to .zip"
        echo "  2. Use option 10 to push the zip to the car"
        echo "  3. Open App Manager on the car and install from there"
    fi
}

install_all_apks() {
    if [ ! -d "$APK_DIR" ]; then
        echo -e "${YELLOW}Creating ./apks/ directory...${NC}"
        mkdir -p "$APK_DIR"
        echo -e "${YELLOW}Place your APK files in ./apks/ and run this option again.${NC}"
        return
    fi

    apk_count=$(find "$APK_DIR" -name "*.apk" 2>/dev/null | wc -l)
    if [ "$apk_count" -eq 0 ]; then
        echo -e "${YELLOW}No APK files found in ./apks/${NC}"
        echo "Place your APK files there and try again."
        return
    fi

    echo -e "${BLUE}Found ${apk_count} APK(s) to install:${NC}"
    for apk in "$APK_DIR"/*.apk; do
        echo "  - $(basename "$apk")"
    done

    echo -e "${YELLOW}Install all? (y/n):${NC}"
    read -r confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "Cancelled."
        return
    fi

    success=0
    failed=0
    for apk in "$APK_DIR"/*.apk; do
        echo -e "${BLUE}Installing $(basename "$apk")...${NC}"
        if adb install -r "$apk" 2>&1 | grep -q "Success"; then
            echo -e "${GREEN}  OK${NC}"
            ((success++))
        else
            echo -e "${RED}  FAILED${NC}"
            ((failed++))
        fi
    done

    echo -e "\n${GREEN}Installed: ${success}${NC} | ${RED}Failed: ${failed}${NC}"
}

list_third_party() {
    echo -e "${BLUE}Third-party (sideloaded) apps:${NC}"
    echo "──────────────────────────────"
    adb shell pm list packages -3 | sed 's/package://' | sort
    echo "──────────────────────────────"
    count=$(adb shell pm list packages -3 | wc -l)
    echo -e "${CYAN}Total: ${count} third-party apps${NC}"
}

list_all_packages() {
    echo -e "${BLUE}All installed packages:${NC}"
    echo -e "${YELLOW}(This may be a long list)${NC}"
    adb shell pm list packages | sed 's/package://' | sort
    echo "──────────────────────────────"
    count=$(adb shell pm list packages | wc -l)
    echo -e "${CYAN}Total: ${count} packages${NC}"
}

uninstall_app() {
    echo -e "${YELLOW}Enter the package name to uninstall (e.g., com.example.app):${NC}"
    echo -e "${CYAN}Tip: Use option 5 or 6 to find package names first.${NC}"
    read -r PACKAGE

    if [ -z "$PACKAGE" ]; then
        echo -e "${RED}No package name entered.${NC}"
        return 1
    fi

    echo -e "${YELLOW}Uninstall method:${NC}"
    echo "  1) Full uninstall (third-party apps)"
    echo "  2) Uninstall for current user only (safer for system apps)"
    read -r method

    case $method in
        1)
            echo -e "${BLUE}Uninstalling ${PACKAGE}...${NC}"
            adb uninstall "$PACKAGE"
            ;;
        2)
            echo -e "${BLUE}Uninstalling ${PACKAGE} for current user...${NC}"
            adb shell pm uninstall -k --user 0 "$PACKAGE"
            ;;
        *)
            echo "Invalid option."
            ;;
    esac
}

take_screenshot() {
    timestamp=$(date +%Y%m%d_%H%M%S)
    filename="byd_screenshot_${timestamp}.png"

    echo -e "${BLUE}Taking screenshot...${NC}"
    adb shell screencap -p /sdcard/byd_screen.png
    adb pull /sdcard/byd_screen.png "./${filename}"
    adb shell rm /sdcard/byd_screen.png

    if [ -f "./${filename}" ]; then
        echo -e "${GREEN}Screenshot saved: ./${filename}${NC}"
    else
        echo -e "${RED}Failed to capture screenshot.${NC}"
    fi
}

record_screen() {
    echo -e "${YELLOW}Recording duration in seconds (max 180):${NC}"
    read -r duration

    if [ -z "$duration" ] || [ "$duration" -gt 180 ]; then
        duration=30
        echo -e "${YELLOW}Using default: 30 seconds${NC}"
    fi

    timestamp=$(date +%Y%m%d_%H%M%S)
    filename="byd_recording_${timestamp}.mp4"

    echo -e "${BLUE}Recording for ${duration} seconds... Press Ctrl+C to stop early.${NC}"
    adb shell screenrecord --time-limit "$duration" /sdcard/byd_recording.mp4
    echo -e "${BLUE}Pulling recording...${NC}"
    adb pull /sdcard/byd_recording.mp4 "./${filename}"
    adb shell rm /sdcard/byd_recording.mp4

    if [ -f "./${filename}" ]; then
        echo -e "${GREEN}Recording saved: ./${filename}${NC}"
    else
        echo -e "${RED}Failed to capture recording.${NC}"
    fi
}

push_file() {
    echo -e "${YELLOW}Enter local file path:${NC}"
    read -r LOCAL_PATH
    LOCAL_PATH="${LOCAL_PATH//\"/}"

    echo -e "${YELLOW}Enter destination on car (default: /sdcard/):${NC}"
    read -r REMOTE_PATH
    REMOTE_PATH="${REMOTE_PATH:-/sdcard/}"

    echo -e "${BLUE}Pushing $(basename "$LOCAL_PATH") to ${REMOTE_PATH}...${NC}"
    adb push "$LOCAL_PATH" "$REMOTE_PATH"
}

pull_file() {
    echo -e "${YELLOW}Enter file path on car (e.g., /sdcard/somefile.txt):${NC}"
    read -r REMOTE_PATH

    echo -e "${BLUE}Pulling ${REMOTE_PATH}...${NC}"
    adb pull "$REMOTE_PATH" "./"
}

disable_animations() {
    echo -e "${BLUE}Disabling animations for speed boost...${NC}"
    adb shell settings put global window_animation_scale 0
    adb shell settings put global transition_animation_scale 0
    adb shell settings put global animator_duration_scale 0
    echo -e "${GREEN}Animations disabled. The head unit should feel snappier.${NC}"
}

enable_animations() {
    echo -e "${BLUE}Re-enabling animations...${NC}"
    adb shell settings put global window_animation_scale 1
    adb shell settings put global transition_animation_scale 1
    adb shell settings put global animator_duration_scale 1
    echo -e "${GREEN}Animations restored to default.${NC}"
}

allow_aurora_installs() {
    echo -e "${BLUE}Granting Aurora Store full permissions...${NC}"
    adb shell appops set com.aurora.store REQUEST_INSTALL_PACKAGES allow 2>&1
    adb shell appops set com.aurora.store WRITE_EXTERNAL_STORAGE allow 2>&1
    adb shell appops set com.aurora.store MANAGE_EXTERNAL_STORAGE allow 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Aurora Store can now install apps and access storage!${NC}"
    else
        echo -e "${RED}Failed. Is Aurora Store installed?${NC}"
    fi
}

view_logs() {
    echo -e "${BLUE}Streaming system logs (Ctrl+C to stop)...${NC}"
    echo -e "${YELLOW}Tip: Pipe to a file with: adb logcat > log.txt${NC}"
    adb logcat
}

show_system_info() {
    echo -e "${BLUE}BYD Shark 6 Head Unit - System Info${NC}"
    echo "══════════════════════════════════════"

    echo -e "\n${CYAN}Android Version:${NC}"
    adb shell getprop ro.build.version.release 2>/dev/null

    echo -e "\n${CYAN}Build/Firmware:${NC}"
    adb shell getprop ro.build.display.id 2>/dev/null

    echo -e "\n${CYAN}Device Model:${NC}"
    adb shell getprop ro.product.model 2>/dev/null

    echo -e "\n${CYAN}Manufacturer:${NC}"
    adb shell getprop ro.product.manufacturer 2>/dev/null

    echo -e "\n${CYAN}CPU/Chipset:${NC}"
    adb shell getprop ro.board.platform 2>/dev/null

    echo -e "\n${CYAN}Screen Density (DPI):${NC}"
    adb shell wm density 2>/dev/null

    echo -e "\n${CYAN}Screen Resolution:${NC}"
    adb shell wm size 2>/dev/null

    echo -e "\n${CYAN}Available Storage:${NC}"
    adb shell df -h /sdcard 2>/dev/null

    echo -e "\n${CYAN}IP Address:${NC}"
    adb shell ip addr show wlan0 2>/dev/null | grep "inet " | awk '{print $2}'
}

reboot_unit() {
    echo -e "${RED}WARNING: This will reboot the car's head unit.${NC}"
    echo -e "${YELLOW}Are you sure? (y/n):${NC}"
    read -r confirm

    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        echo -e "${BLUE}Rebooting head unit...${NC}"
        adb reboot
        echo -e "${YELLOW}Head unit is rebooting. You will need to reconnect after.${NC}"
    else
        echo "Cancelled."
    fi
}

disconnect_car() {
    echo -e "${BLUE}Disconnecting...${NC}"
    adb disconnect
    echo -e "${GREEN}Disconnected from all devices.${NC}"
}

# ─── Main ───────────────────────────────────────────────────

check_adb
print_banner

while true; do
    echo ""
    print_menu
    echo -e "${YELLOW}Choose an option:${NC}"
    read -r choice

    case $choice in
        1)  connect_car ;;
        2)  check_status ;;
        3)  install_apk ;;
        4)  install_all_apks ;;
        5)  list_third_party ;;
        6)  list_all_packages ;;
        7)  uninstall_app ;;
        8)  take_screenshot ;;
        9)  record_screen ;;
        10) push_file ;;
        11) pull_file ;;
        12) disable_animations ;;
        13) enable_animations ;;
        14) allow_aurora_installs ;;
        15) view_logs ;;
        16) show_system_info ;;
        17) reboot_unit ;;
        18) disconnect_car ;;
        0)  echo -e "${GREEN}Goodbye!${NC}"; exit 0 ;;
        *)  echo -e "${RED}Invalid option.${NC}" ;;
    esac
done
