# Build Instructions

## Compilation Errors Fixed

### Issue
The build was failing with "Unresolved reference 'database'" and "Unresolved reference 'AppDatabase'" errors.

### Root Cause
The import path was incorrect. We were importing:
```kotlin
import com.curatrack.app.core.database.AppDatabase  // WRONG
```

But the actual path is:
```kotlin
import com.curatrack.app.core.db.AppDatabase  // CORRECT
```

### Files Fixed
1. **DoseAlarmReceiver.kt** - Fixed import and nullable property access
2. **DoseActionReceiver.kt** - Fixed import

## How to Build

### IMPORTANT: Database Migration
⚠️ **You MUST uninstall the app before building** because we added a new `status` column to the database.

### Steps:

#### 1. Uninstall Current App
```bash
# Via ADB (if device connected)
adb uninstall com.curatrack.app

# OR manually on your device:
# Settings → Apps → CuraTrack → Uninstall
```

#### 2. Build in Android Studio
Since `JAVA_HOME` is not configured in your terminal, use Android Studio:

1. **Open Project**
   - Open Android Studio
   - Open the project folder: `C:\Users\Asus\MAD-project`

2. **Sync Gradle**
   - Click "Sync Project with Gradle Files" button (or File → Sync Project with Gradle Files)
   - Wait for sync to complete

3. **Clean Build**
   - Menu: **Build** → **Clean Project**
   - Wait for clean to finish

4. **Rebuild**
   - Menu: **Build** → **Rebuild Project**
   - Wait for build to complete (should succeed now)

5. **Run the App**
   - Click the green "Run" button or press Shift+F10
   - Select your device/emulator
   - App will install and run

## Verification Checklist

After the app runs successfully, verify:

### ✓ Time Picker (from previous fix)
- [ ] Schedule screen shows 12-hour time format with AM/PM
- [ ] Time picker has AM/PM selector
- [ ] Selected times display as "8:00 AM" format

### ✓ Notification Medicine Name
- [ ] Create a medicine schedule with a time very soon (e.g., 2 minutes from now)
- [ ] Wait for notification to appear
- [ ] Notification shows medicine name (e.g., "Aspirin") NOT an ID number
- [ ] Notification shows dosage info (e.g., "100mg • Tablet")

### ✓ Taken/Skip Status
- [ ] When notification appears, tap **✓ Taken** button
- [ ] Notification disappears
- [ ] Go to History → Past tab
- [ ] Verify dose shows green "✓ Taken" badge

- [ ] For next notification, tap **⊗ Skip** button
- [ ] Go to History → Past tab
- [ ] Verify dose shows orange "✗ Skipped" badge

- [ ] Let a notification time pass without tapping anything
- [ ] Go to History → Past tab
- [ ] Verify it shows red "⊗ Missed" badge

## If Build Still Fails

### Check Java Setup
Android Studio should handle Java automatically, but if it doesn't:

1. **File** → **Project Structure**
2. Check **SDK Location**:
   - Android SDK location should be set
   - JDK location should be set (usually bundled with Android Studio)

### Check Gradle Version
In `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
```
Should be version 8.0 or higher.

### Invalidate Caches
If you still get errors:
1. **File** → **Invalidate Caches...**
2. Select both options:
   - ☑ Invalidate and Restart
   - ☑ Clear downloaded shared indexes
3. Click **Invalidate and Restart**

### Check Dependencies
In `app/build.gradle.kts`, ensure you have:
```kotlin
dependencies {
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // ... other dependencies
}
```

## Expected Behavior After Fix

### Notifications
```
┌─────────────────────────────────────────┐
│ 💊 Medicine Reminder                    │
│ Time to take: Aspirin (100mg Tablet)   │
│                                         │
│ [✓ Taken]         [⊗ Skip]             │
└─────────────────────────────────────────┘
```

### History - Past Tab
```
Aspirin  [✓ Taken]
100mg • Tablet
Fri, Nov 2 2025 8:00 AM

Ibuprofen  [✗ Skipped]
200mg • Tablet
Thu, Nov 1 2025 8:00 PM

Vitamin D  [⊗ Missed]
50000 IU • Capsule
Thu, Nov 1 2025 9:00 AM
```

## Troubleshooting

### "Room cannot find implementation"
- Make sure KSP (Kotlin Symbol Processing) plugin is applied
- Check `build.gradle.kts` has: `id("com.google.devtools.ksp")`

### "Unresolved reference: scheduleDao"
- Rebuild project: **Build** → **Rebuild Project**
- Room generates DAO implementations at compile time

### App Crashes on Launch
- Make sure you uninstalled the old version
- Check Logcat for crash details
- Likely a database migration issue - uninstall and reinstall

## Success Indicators

Build is successful when:
1. ✅ No compilation errors in Android Studio
2. ✅ App installs on device/emulator
3. ✅ App launches without crashing
4. ✅ Can create medicine and schedule
5. ✅ Notifications show medicine name
6. ✅ History shows status badges
