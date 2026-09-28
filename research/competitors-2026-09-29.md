# Competitor check, 29 Sep 2026

## OverDrive (yash-srivastava/Overdrive-release)

- Braveheart v51.0 / v51.1 adds opt in BYD Shark 6 support. Camera order 8, 9, 5, 4 (same ids we found).
- Shark firmware locks the folder its daemons use, so a "Daemon storage" toggle moves them.
- Locked sentry relies on "DiLink 5 parked cloud keep alive": while the ute is off it polls BYD Cloud
  about once a minute to stop the head unit going into deep sleep. Needs a verified BYD Cloud login and
  internet, uses vehicle SIM data and extra parked battery. Their own words.
- Also runs shell uid daemons (Camera, Surveillance, ACC) that persist after the app closes.

Verdict against our rules: this is a power keep alive. It falls under Tane's "dont play around with power"
decision (PLAN.md, 18 Sep 2026). Not building it. Risk is the small 12 V battery and the known parked drain.

## Sharkware (sharkwarehub.com), scrape in research/sharkware-scrape/2026-09-29/

- New product SharkSight: flat dashcam, one or two cameras, three minute loops, protected clips, browser,
  auto start, GPS and time overlay, 25 GB cap. A$95.99 licence plus A$5 a month.
- Sentry: not shipped. Rob reports about 10 minutes in testing and refuses to modify BYD software.
- Shark Hub A$65.99: Extended Fuel gauge for long range tanks, home dashboard, configurable gauges, pet mode screenshot.
- Trigger hardware bundle A$414.99, sales paused until 6 Oct.

## Where we stand

We already have: flat view, four camera recording, events, powered sentry, auto record, fuel panel, quick panel.
Gaps worth closing: recordings browser with playback in app, time and GPS overlay on clips, storage cap setting,
extended tank calibration, pet mode, home dashboard.
