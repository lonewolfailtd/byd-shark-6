# Buyer installer

What goes in the download zip:

- `Install Lonewolf Shark.ps1` (Windows) and `install-lonewolf-shark.command` (Mac)
- `lonewolf-shark.apk`, the signed release build
- `platform-tools/` from Google's Android SDK platform tools for that operating system

Build the zip with `tools/pack-installer.ps1` once a release build exists.

The buyer only has to do three things: put the laptop and the ute on a phone hotspot, switch on debugging
in the ute (Car > System > Version, tap Factory Reset ten times, portrait, CONNECT USB TO ENABLE DEBUGGING MODE)
and double click the installer. The script finds the ute by scanning the hotspot for the debugging port,
checks it is a BYD, installs, grants permissions and opens the app. It also switches off the pointer
trails and touch circles people turn on by accident in developer options.

Not built yet: licence keys, payments, in app updates and the sales page.
