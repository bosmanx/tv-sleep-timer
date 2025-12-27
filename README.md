# TV Sleep Timer for Android TV

A lightweight, non-intrusive sleep timer specifically designed for Android TV (optimized for Sharp/Leanback devices). 

This app allows you to set a sleep timer using the **RED button** on your remote control, working seamlessly on top of other apps like **Netflix**, **YouTube**, or **Disney+** without interrupting your viewing experience.

## 🚀 Key Features

- **One-Button Control**: Cycle through timer options using only the RED button on your remote.
- **Smart Toggling**: 
  - First press: Shows the current remaining time.
  - Subsequent presses (within 3s): Cycles the timer (60 min → 30 min → 15 min → 5 sec → OFF).
- **Background Operation**: Uses an Accessibility Service to detect the RED button press even when you are inside other apps.
- **Visual Feedback**: Non-intrusive Toast messages show the timer status and countdown alerts (at 5m, 1m, and 5s).
- **Persistent Recovery**: Automatically restores your standard system screen timeout after the TV wakes up (via Boot and Screen-On receivers).

## 🛠️ How It Works

Traditional Android "Power Off" commands often fail on consumer TVs due to security restrictions. This app uses a "Smart Timeout" strategy:
1. When the timer expires, the app sets the system `SCREEN_OFF_TIMEOUT` to 5 seconds.
2. The TV naturally enters sleep mode after 5 seconds of inactivity.
3. Upon waking, the app restores your original timeout (default 10 minutes).

## 📋 Setup & Installation

### 1. Enable Developer Mode on your TV
- Go to **Settings > Device Preferences > About**.
- Click on **Build** 7 times until "You are now a developer" appears.
- Go to **Developer options** and enable **USB debugging**.

### 2. Install the App
- Build the project in Android Studio and install via ADB.
- Or use the included `deploy_and_monitor.ps1` script (requires ADB in PATH).

### 3. Grant Permissions
- **Accessibility Service**: Open the app and click "OPEN ACCESSIBILITY SETTINGS". Enable "TV Sleep".
- **Write Settings**: Click "GRANT WRITE_SETTINGS PERMISSION" to allow the app to modify the screen timeout.
  - *Note for Sharp TVs*: If the UI button fails, use ADB:
    `adb shell appops set pl.haxen.tvsleep WRITE_SETTINGS allow`

## 🛡️ Privacy

- **No Data Collection**: This app does not collect, store, or transmit any data.
- **No Internet Access**: The app does not require internet permissions.
- **Open Source**: Built with transparency for the community.

## ⚖️ License
This project is provided "as is" for personal use.

---
*Developed by [Kamil Christ](https://haxen.pl)*
