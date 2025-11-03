# 🚀 Quick Guide: View Your CuraTrack Database

## 🎯 **Fastest Method - Android Studio Database Inspector**

### **Step-by-Step for Your Windows Setup:**

1. **🏃‍♂️ Run Your App**
   ```bash
   # In your project directory
   cd C:\Users\Asus\MAD-project
   
   # Make sure an emulator is running or device connected
   # Build and run your app
   .\gradlew.bat installDebug
   ```

2. **📱 Use Your App**
   - Open CuraTrack app
   - Add a medicine (e.g., "Paracetamol 500mg Tablet")
   - Create a schedule for it
   - This puts data in your database

3. **👀 Open Database Inspector**
   - In Android Studio: **View → Tool Windows → App Inspection**
   - OR click **Database Inspector** tab at bottom
   - Select process: `com.curatrack.app`

4. **🔍 Explore Your Data**
   - Expand `curatrack.db`
   - Click on `medicines` table → See your Paracetamol entry
   - Click on `schedules` table → See your schedule rules
   - Click on `doses` table → See generated dose times

---

## 💻 **Command Line Method (Windows PowerShell)**

### **For Your Windows Environment:**

```powershell
# Open PowerShell and navigate to platform-tools
cd "C:\Users\Asus\AppData\Local\Android\Sdk\platform-tools"

# Check connected devices
.\adb devices

# Connect to your app's database
.\adb shell
cd /data/data/com.curatrack.app/databases/
sqlite3 curatrack.db

# View your data
.tables
SELECT * FROM medicines;
SELECT * FROM schedules;
SELECT COUNT(*) FROM doses;
.exit
```

---

## 📊 **What You'll See in Your Database**

### **After Adding Medicine & Schedule:**

**medicines table:**
```
id                  | name         | dosage | form   | userId
abc-123-def-456     | Paracetamol  | 500mg  | Tablet | 1
```

**schedules table:**
```
id                  | medicineId      | frequencyType  | timesJson
xyz-789-uvw-012     | abc-123-def-456 | TIMES_PER_DAY | ["08:00","20:00"]
```

**doses table:**
```
id                  | scheduleId      | timeEpochMillis | generatedFrom
dose-001           | xyz-789-uvw-012 | 1728886800000   | SCHEDULE
dose-002           | xyz-789-uvw-012 | 1728930000000   | SCHEDULE
```

---

## 🛠️ **Troubleshooting**

### **If Database Inspector Doesn't Show Data:**
1. Make sure app is running on device/emulator
2. Try adding more data in the app
3. Refresh the Database Inspector

### **If ADB Not Found:**
```powershell
# Add Android SDK to your PATH
$env:PATH += ";C:\Users\Asus\AppData\Local\Android\Sdk\platform-tools"
adb devices
```

### **If Permission Denied:**
- Use emulator instead of physical device
- Enable USB debugging on physical device

---

## 🎉 **Quick Test**

1. **Add Medicine**: "Aspirin 100mg Tablet"
2. **Add Schedule**: Daily at 9:00 AM and 9:00 PM
3. **Open Database Inspector**
4. **Check medicines table** → Should see Aspirin entry
5. **Check schedules table** → Should see TIMES_PER_DAY with ["09:00","21:00"]
6. **Check doses table** → Should see multiple dose entries for upcoming days

**You'll see your app data stored in SQLite in real-time!** 🚀