# byd-shark-6

Lonewolf's BYD Shark 6 software project: an Android app suite for the ute's DiLink 5 head unit (climate, seats, lighting, gauges, off road, towing, sentry) plus our own physical control bar.

Start with RESEARCH-REPORT.md. Build plan in PLAN.md once approved.

## Layout

- RESEARCH-REPORT.md: the summary and decisions.
- research/agents/: six deep research reports with sources.
- research/sharkware-scrape/: what the competitor ships (pages and screenshots).
- research/open-dikey/: the open source reference implementation that already works on the Shark 6.
- tools/recon.ps1: read only dump of the head unit over ADB. Run this first when the ute is on WiFi.
- BYD-SHARK-6-CUSTOMIZATION-GUIDE.md, BYD-SHARK-6-MODDING-GUIDE.md, BYD-SHARK-6-TIPS-TRICKS.md: earlier owner guides.

## Enabling ADB on the ute

Car > System > Version, tap the Factory Reset text 10 times, rotate the screen to portrait, press CONNECT USB TO ENABLE DEBUGGING MODE, connect the ute to the same WiFi as the PC, then:

```bash
"C:/Users/Hodgs/Downloads/platform-tools-latest-windows/platform-tools/adb.exe" connect <ute-ip>:5555
```
