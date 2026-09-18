# Agent report 07: sentry with the ute off, on the Shark 6

Researched 18 Sep 2026. Decision after this report: stays off the table (see PLAN.md).

## Verdict

- Nobody has publicly shown sentry recording working on a Shark 6 after it is locked and asleep. The only Shark 6 specific evidence (OverDrive issue 258, 1 Sep 2026) says cameras and vehicle data work but the daemon that keeps the head unit awake "gets killed often, no workaround yet". Still open mid September 2026. Everything else is Seal, Atto, Dolphin, Atto 2 experience or marketing copy.
- The mechanism is a request to the body computer: a helper running as the ADB shell user sends a periodic "power hold" heartbeat to BYDAutoPowerDevice so the MCU does not cut 12 V to the head unit after ACC off. It asks the car to stay half awake indefinitely.
- Cost where it works: 1 to 2 percent of traction battery per parked day, paid through the 12 V top up cycle. No Shark 6 numbers exist.
- Honest advice for "do not play with power": do not run car off sentry on the Shark 6. Use "on only" modes. For parked protection use a hardwired dashcam with a low voltage cut off, or wait for BYD, which shipped native Sentry on the Sealion 8 in AU by OTA (Aug 2026).

## Who claims what

- OverDrive (https://github.com/yash-srivastava/Overdrive-release, https://www.overdrive.qd.je/): claims 24/7 surveillance on Seal (Global) and Sealion 7; Shark 6 not on the supported list. Issue 258: five cameras and telemetry work on Shark 6, /data/local/tmp blocked by SELinux, LD_PRELOAD fails, ACC daemon killed often. PR 260 (open) adds a Shark camera profile, does not fix the daemon. PR 274 (Sep 2026, open) adds keep awake hardening for DiLink 5 and fixes a codec crash that caused "6 tombstone cascade reboot loops"; nothing says the Shark daemon kill is solved. Issue 31 (May 2026): a Brazilian Shark owner lost remote access when the car switched off, unresolved.
- Electro (https://forums.whirlpool.net.au/archive/31mmxlpv): works with the car off on Seal, Dolphin, Atto 3. On Shark 6 one user reports it keeps needing USB debugging re enabled; no successful Shark 6 report. A Shark 6 Facebook thread has no working example; a commenter says the Shark's systems shut down with the vehicle.
- Sharkware: no sentry claim at all.
- BYD Sentry (https://bydsentry.com): claims cameras active when off; no model list, no drain data.
- sentryev.com: beta, no model list. Kim Launcher: DiLink 3 only. Kinex: launcher only. BladeWatch: DiLink 3 only. Strike (https://github.com/UnrealSalty/Strike): Atto 2 only, 1 to 2 percent per day, warns a power cut can corrupt the clip being written. Di+: China only.

## Mechanism (OverDrive wiki, https://deepwiki.com/yash-srivastava/Overdrive-release/3.1-acc-and-power-state-monitoring)

1. AccSentryDaemon runs as UID 2000 (shell), so wireless ADB must stay on. ACC state via AbsBYDAutoBodyworkListener, fallback polling sys.accanim.status and dumpsys power every 500 ms.
2. On ACC off it sends periodic power hold heartbeats to BYDAutoPowerDevice, watching MCU status codes (Sleeping, Active, ACC Off, Deep Sleep). Full wake locks, WIFI_SLEEP_POLICY_NEVER, StealthPanel backlight suppression. No SOC floor, no voltage floor, no time cap, no auto stop in the public docs.
3. State while working: ACC off, locked, body MCU awake enough to feed 12 V, head unit and cameras fully running.
4. Shark 6: the daemon is killed; inferred that DiLink 5 on the Shark reaps shell processes or ignores the hold.

## Risks

- 12 V flat: no Shark 6 report tied to a sentry app, but generic Shark 6 flat 12 V reports exist and are blamed on the 13.8 Ah battery (Dash Cam Guys, Facebook). A hold adds a continuous 10 to 20 W head unit load.
- HV top up cycling: the Shark wakes every 15 minutes to check and charge the 12 V; a failing 12 V produced 8 percent per day drain for one owner (https://forums.whirlpool.net.au/archive/3rvvjk1n). Electro users saw 4 to 7 top ups a day with loads, one replaced the 12 V early.
- SOC floor: on Atto 3 below about 15 percent HV the car stops topping the 12 V; a week of sentry at low charge can strand you.
- Head unit stability: reboot loops documented on DiLink 5 (PR 274), a drain event when parked.
- Warranty: permanent ADB plus SELinux workarounds; easy for a dealer to decline a 12 V or head unit claim.
- Native alternative: Sealion 8 has BYD Sentry by OTA (Aug 2026, https://dashcamguys.com.au/blogs/guides/byd-dash-cam-australia-sentry-mode-guide). Shark 6 has none yet; "coming soon" is rumour only.

## Measured cost

- Dolphin (LFP 12 V), Electro: about 1 percent per day. Atto 3 (lead acid 12 V): about 10 percent a week. Atto 2, Strike: 1 to 2 percent a day. Shark 6: no numbers.

## Recommendation

1. Do not run car off sentry on the Shark 6 now.
2. Drive recording and sentry only while the ute is on (what Lonewolf Shark already does).
3. For parked protection a hardwired dashcam with impact only parking mode and a hard low voltage cut off, or its own battery pack.
4. Revisit only if OverDrive merges a "Shark 6 daemon stable" fix with a SOC floor and time cap, and a Shark owner posts overnight 12 V and HV numbers. Or if BYD ships native Sentry to the Shark 6.
