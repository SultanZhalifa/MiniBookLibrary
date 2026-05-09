# Screenshots

The README references the following filenames. Drop your captures here using these exact
names so the markdown renders without further editing.

| File | Screen | What to capture |
| ---- | ------ | --------------- |
| `01_onboarding.png` | Onboarding | The first onboarding slide — illustration + title + body + "Next" button |
| `02_login.png` | Login | The login form with the brand logo, hint text visible |
| `03_dashboard.png` | Dashboard | After signing in, with at least 5 books added so the stats card shows real numbers |
| `04_library.png` | Library list | The library tab with several books, search bar empty |
| `05_detail.png` | Book detail | A book in "Currently Reading" state, with progress bar visible and rating set |
| `06_edit.png` | Add / Edit | The Add Book form mid-typing, dropdowns visible |
| `07_settings.png` | Settings | The settings tab, with theme toggle row and data section visible |
| `08_dark_mode.png` | Dark mode | The Dashboard in dark mode (toggle Dark in Settings, then capture) |
| `09_pdf.png` | PDF preview | Open the exported PDF in a viewer, capture the first page |

## How to capture

### From an emulator (recommended for clean dimensions)

1. Run the app on an Android Studio emulator with Pixel 6 / Pixel 7 device profile.
2. Sign in, add 5–10 sample books with varied statuses + ratings + cover images so the
   screens look populated.
3. In the emulator window, click the camera icon (or `Ctrl + S` on Windows) to save a
   screenshot. Files land in `~/Desktop` by default.
4. Rename to the table above and copy here.

### From a real device

1. Build and install the debug APK: `./gradlew installDebug`
2. Use device-native screenshot (Power + Volume Down).
3. Pull via ADB: `adb pull /sdcard/Pictures/Screenshots/`

## Recommended size

- **Width:** 1080 px (Pixel-class density-1)
- **Height:** ~2340 px for full-screen captures, or trimmed to the relevant region
- **Format:** PNG

The README renders these in a 3-column table — ~360 px wide each. Don't worry about
exact pixel parity; just keep aspect ratios consistent so the table doesn't get jagged.
