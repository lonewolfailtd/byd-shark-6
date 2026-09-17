Title: How to Install Sharkware | Shark Hub, Control, Transfer & Transfer+

URL Source: https://sharkwarehub.com/install.html

Markdown Content:
Installation guide

This page covers Shark Hub, Shark Control, Shark Transfer and Sharkware Transfer+. Hub and Control are installed on the Shark. Shark Transfer runs on your Mac or Windows computer and installs or updates Sharkware. Transfer+ also runs on your computer, but is for compatible Android APK files you choose yourself.

**Start here:** your computer and BYD Shark must be on the same Wi-Fi network, the Shark must stay awake, and ADB debugging must be enabled before installation.

Before you start

## Prepare the Shark first.

Do this before opening Shark Transfer or Transfer+. It is the same preparation for Mac and Windows.

### You need

A Mac or Windows computer, the original Sharkware download from your order, internet access, your licence details, and the Shark connected to the same Wi-Fi as the computer.

### Keep the vehicle ready

Keep the centre screen awake for the whole installation. Do not change Wi-Fi networks while the installer is searching for or communicating with the Shark.

Step 1

## Enable ADB debugging on the Shark.

The installer uses the Shark's Android debugging connection to identify the head unit and install the selected software.

1

### Open Version

On the Shark screen go to **Car → System → Version**.

2

### Open the hidden menu

Tap the **FACTORY RESET text** 10 times quickly. Do not press the Reset button.

3

### BYD Shark Premium: Rotate the centre screen

When the hidden menu opens, rotate the screen vertically.

**BYD Shark Performance:** if your centre screen does not rotate, keep the hidden developer menu open and use the Shark's **Multi-window / Split-screen** control. Put the developer menu on one half of the screen and another app, such as Maps or Media, on the other half. Resizing the developer menu forces it to redraw and reveals the hidden top debugging controls. Then continue to Step 4 below.

4

### Enable debugging

Press **CONNECT USB TO ENABLE DEBUGGING MODE**.

5

### Confirm ADB

Wait for the message Turn on ADB debugging!, then leave the Shark screen awake.

Step 2 · Shark Transfer

## Install Hub or Control from your computer.

Shark Hub and Shark Control use the same Shark Transfer installation process. The Sharkware licence you enter after installation determines which edition is active.

### Windows

Use the Windows ZIP supplied from your Sharkware order/download page.

**1.** Download the current Windows package from your original Sharkware order link.

**2.****Extract the entire ZIP.** Do not run the installer from inside the ZIP preview.

**3.** Open the extracted folder and double-click INSTALL_SHARKWARE_WINDOWS.bat.

**4.** Allow the Windows security/run prompt if asked, then leave the Command Prompt window open.

**5.** Shark Transfer starts its bundled ADB, searches the local network, verifies the target as BYD AUTO and installs/updates Sharkware.

**6.** If it asks for the Shark IP address, enter it only then. Otherwise let automatic discovery finish.

**7.** Wait for the final PASS message before closing the window.

**A normal successful run can show:**  
ADB_READY  
FILE_VERIFICATION=PASS  
ADB_SERVER=PASS  
BYD_AUTO_VERIFY=PASS  
SHARKWARE V5.88 INSTALLATION PASSED

### Mac

Use the Shark Transfer DMG supplied from your Sharkware order/download page.

**1.** Open the downloaded Shark Transfer **.dmg**.

**2.** Drag **Shark Transfer.app** into **Applications**. Do not run it directly from the DMG.

**3.** Open Shark Transfer from Applications.

**4.** On the first launch, macOS may show **Not Opened**. Click **Done** — do not choose Move to Bin.

**5.** Go to **System Settings → Privacy & Security**, find the Shark Transfer blocked message and choose **Open Anyway**.

**6.** Authenticate with your Mac password/Touch ID and confirm **Open**.

**7.** Follow Shark Transfer. It checks the network, locates BYD AUTO, verifies the Shark and installs/updates Sharkware.

**8.** Wait for the final successful installation message before closing it.

**Mac:** you should not need to type Terminal commands such as xattr, chmod or zsh for the normal customer installation flow.

Step 3 · Hub / Control

## Activate the Sharkware edition you purchased.

After Sharkware is installed on the head unit, open it on the Shark and use the licence supplied with your purchase.

1

### Open Sharkware

The first-run activation screen shows that Sharkware is installed and ready, together with an Activation ID for the vehicle.

2

### Enter your licence

Choose **ENTER CODE** and enter your Shark Hub or Shark Control licence code. The code field accepts the Sharkware Hub/Control licence supplied with your order.

3

### Activate

Press **ACTIVATE**. The licence is linked to that Shark and the correct Hub or Control edition becomes active.

4

### Existing installation?

Shark Transfer is designed to update the current Sharkware app without uninstalling it or clearing saved app data. **Do not uninstall your working Sharkware app before an update.**

5

### If an old Sharkware app/icon is still showing

Some customers may have an older legacy Sharkware/Hub/Control app left on the head unit after moving to the current unified Sharkware app. First open the **new Sharkware app**, confirm your licence is active and check that it works normally. If you can then see a separate old app/icon that is no longer used, you may uninstall **that old legacy app only**. Do not clear data or uninstall the current working Sharkware app. If you are unsure which icon is old, leave both installed and contact support before deleting anything.

Sharkware Transfer+

## Install a compatible APK you choose.

Transfer+ is separate from the Sharkware Hub/Control installer. It lets the owner select a compatible Android APK and install or update it on the Shark over ADB.

1

### Prepare the Shark

Put the computer and Shark on the same Wi-Fi, keep the Shark awake and enable ADB using the steps above.

2

### Open Transfer+

Use the Mac or Windows Transfer+ download supplied with your purchase and follow its guided interface.

3

### Select your APK

Choose the compatible Android .apk file that you want to install or update.

4

### Verify the Shark

Allow Transfer+ to connect over ADB and verify the target head unit before continuing with the install.

5

### Install and wait for completion

Leave the Shark awake and do not disconnect the network until Transfer+ reports the final result.

**Important:** Transfer+ does not make third-party APKs safe or compatible. Only install APKs from sources you trust and that you have reason to believe are suitable for the Shark's Android head unit.

Troubleshooting

## If the installer stops.

Most installation problems can be narrowed down from the last line shown by the installer.

### Nothing happens after the Windows header

Close it and confirm the **entire ZIP was extracted**. The BAT, APK files and platform-tools folder need to stay together. Do not run the BAT from inside the ZIP.

### The Shark cannot be found

Check the computer and Shark are on exactly the same Wi-Fi, ADB is still enabled, the Shark screen is awake, and neither device changed networks. Let the automatic scan finish.

### Windows blocks the installer

Only allow the downloaded Sharkware installer if it came from your original Sharkware order/download link. Do not use replacement BAT/APK files from third-party sources.

### Old app still visible?

If the current Sharkware app is installed and working but a separate older Sharkware/Hub/Control icon remains, verify the new app and licence first. You can then remove the unused legacy app. **Never guess which app to delete** — if you are unsure, leave it installed and send us a screenshot.

### Do not uninstall or clear data during a failed install

If installation stops, do not repeatedly uninstall the current Sharkware app or clear its data. Take a clear photo/screenshot of the entire installer window instead.

### Still stuck?

Send a full screenshot of the installer window and tell us whether you reached ADB_READY, ADB_SERVER=PASS or BYD_AUTO_VERIFY=PASS.

[Email Sharkware Support](mailto:support@sharkwarehub.com?subject=Sharkware%20Installation%20Support)
