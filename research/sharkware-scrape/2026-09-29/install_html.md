Title: How to Install Sharkware | Shark Hub, Control, Transfer & Transfer+

URL Source: https://www.sharkwarehub.com/install.html

Markdown Content:
## Connect your computer before installing

**Installing SharkSight?**[SharkSight manuals and installers](https://www.sharkwarehub.com/index.html#sharksight-install). Mac and Windows manuals are free to download. Your active SHK-S licence is required for the installer.

Choose the detailed Mac or Windows guide below. Both include the new Shark hotspot route, same-Wi-Fi setup, ADB ON, installation and troubleshooting for BYD Shark Premium. Then acknowledge the checklist on the downloads page to reveal the Sharkware Windows and Mac buttons.

[Mac guide (PDF)](https://www.sharkwarehub.com/Sharkware-Mac-Connection-and-Installation-Guide.pdf)[Windows guide (PDF)](https://www.sharkwarehub.com/Sharkware-Windows-Connection-and-Installation-Guide.pdf)[Continue to Hub / Control downloads](https://www.sharkwarehub.com/updates-v578.html)
Installation guide

This page covers Shark Hub, Shark Control, Shark Transfer and Sharkware Transfer+. Hub and Control are installed on the Shark. Shark Transfer runs on your Mac or Windows computer and installs or updates Sharkware. Transfer+ also runs on your computer, but is for compatible Android APK files you choose yourself.

**Start here:** your computer and BYD Shark Premium must be on the same Wi-Fi network, or your computer must be connected to the Shark hotspot; the Shark must stay awake, and ADB debugging must be enabled before installation.

Before you start

## Prepare the Shark first.

Do this before opening Shark Transfer or Transfer+. It is the same preparation for Mac and Windows.

### You need

A Mac or Windows computer, the original Sharkware download from your order, internet access, your licence details, and either the same Wi-Fi on both devices or the computer connected to the Shark hotspot.

### Keep the vehicle ready

Keep the centre screen awake for the whole installation. Do not change Wi-Fi networks while the installer is searching for or communicating with the Shark.

New connection option

## Connect your computer to the Shark hotspot.

Use this if your home Wi-Fi will not connect to the Shark, or you prefer a direct hotspot connection.

1

### Download first

Download the current official installer and your Mac or Windows guide while your computer has internet. Extract the full package. Some installers also need internet to prepare Android tools.

2

### Turn on the Shark hotspot

Open hotspot settings on the Shark. Turn it on and note the hotspot network name and password shown there.

3

### Join it from your computer

**Mac:** open the Wi-Fi menu or System Settings > Wi-Fi.  
**Windows:** open Wi-Fi from the taskbar or Settings > Network & internet > Wi-Fi.  
Select the Shark hotspot name, enter its password and confirm the computer is connected.

4

### Keep that connection

Connected without internet can be normal. Keep the Shark screen awake and ADB ON. Do not let the computer switch back to home Wi-Fi during installation. If the installer needs to download tools, prepare them with working internet before returning to the hotspot.

5

### Confirm the correct Shark IP if asked

The address can change when switching from home Wi-Fi to hotspot. Do not use the computer's own IP or copy an example. If the Shark hotspot details do not show its address, ask support to confirm it.

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
INSTALLATION PASSED (version wording varies)

### Mac

Use the current official package. If it is a ZIP, extract the whole folder and open its included INSTALL_SHARKWARE_MAC_V5.89.command file. If it is a DMG containing Shark Transfer.app, follow the DMG steps below. The detailed Mac PDF covers both file types.

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

If a separate older Sharkware icon remains, leave both apps installed and contact support with a screenshot. Do not uninstall either app or clear data while troubleshooting.

Sharkware Transfer+

## Install a compatible APK you choose.

Transfer+ is separate from the Sharkware Hub/Control installer. It lets the owner select a compatible Android APK and install or update it on the Shark over ADB.

1

### Prepare the Shark

Use the same Wi-Fi on both devices or connect the computer to the Shark hotspot, keep the Shark awake and enable ADB using the steps above.

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

Check both devices are on the same Wi-Fi or the computer is connected to the Shark hotspot, ADB is still enabled, the Shark screen is awake, and neither device changed networks. Let the automatic scan finish.

### Windows blocks the installer

Only allow the downloaded Sharkware installer if it came from your original Sharkware order/download link. Do not use replacement BAT/APK files from third-party sources.

### Old app still visible?

Leave both apps installed and send support a screenshot so the packages can be identified. Do not guess which app to delete or clear its data.

### Do not uninstall or clear data during a failed install

If installation stops, do not repeatedly uninstall the current Sharkware app or clear its data. Take a clear photo/screenshot of the entire installer window instead.

### Still stuck?

Send a full screenshot of the installer window and tell us whether you reached ADB_READY, ADB_SERVER=PASS or BYD_AUTO_VERIFY=PASS.

[Email Sharkware Support](mailto:support@sharkwarehub.com?subject=Sharkware%20Installation%20Support)
