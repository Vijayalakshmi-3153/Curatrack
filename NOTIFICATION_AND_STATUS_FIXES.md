# Notification and History Status Fixes

## Issues Fixed

### 1. Notification Shows Medicine Name Instead of ID

**Problem:** Notifications were displaying the medicine ID (a number/UUID) instead of the actual medicine name.

**Solution:**
- Updated `DoseAlarmReceiver` to query the database and fetch the medicine name before showing the notification
- Added coroutine support to fetch medicine details asynchronously
- Display medicine name with dosage and form information in notification

**Changes:**
```kotlin
// DoseAlarmReceiver now fetches medicine data from database
val database = AppDatabase.getDatabase(context)
val dose = database.scheduleDao().getDoseByMedicineAndTime(medId, doseTime)
val medicineName = dose?.medicineName ?: medId
```

**Files Modified:**
- `app/src/main/java/com/curatrack/app/core/notifications/DoseAlarmReceiver.kt`
- `app/src/main/java/com/curatrack/app/core/notifications/AlarmScheduler.kt`

### 2. History Shows Taken/Skipped Status

**Problem:** Past doses in the history screen didn't show whether they were taken, skipped, or missed.

**Solution:**
- Added `status` field to `DoseEntity` with values: PENDING, TAKEN, SKIPPED
- Updated all database queries to include and track status
- Created visual status badges (green for Taken, orange for Skipped, red for Missed)
- Changed notification button from "Snooze" to "Skip" to mark doses as skipped

**Database Schema Changes:**

1. **DoseEntity:**
   ```kotlin
   data class DoseEntity(
       @PrimaryKey val id: String,
       val scheduleId: String,
       val timeEpochMillis: Long,
       val generatedFrom: String,
       val status: String = "PENDING" // PENDING, TAKEN, SKIPPED
   )
   ```

2. **DoseProjection:**
   ```kotlin
   data class DoseProjection(
       val scheduleId: String,
       val medicineId: String,
       val timeEpochMillis: Long,
       val medicineName: String?,
       val medicineDosage: String?,
       val medicineForm: String?,
       val status: String = "PENDING"
   )
   ```

**New DAO Methods:**
```kotlin
@Query("UPDATE doses SET status = :status WHERE scheduleId = :scheduleId AND timeEpochMillis = :time")
suspend fun updateDoseStatus(scheduleId: String, time: Long, status: String)

@Query("SELECT ... WHERE s.medicineId = :medicineId AND d.timeEpochMillis = :time LIMIT 1")
suspend fun getDoseByMedicineAndTime(medicineId: String, time: Long): DoseProjection?
```

**Notification Actions:**
- ✓ Taken - Marks dose as TAKEN in database
- ⊗ Skip - Marks dose as SKIPPED in database

**Status Badges in History:**
- ✓ Taken (Green badge) - User confirmed they took the medicine
- ✗ Skipped (Orange badge) - User marked it as skipped
- ⊗ Missed (Red badge) - Past dose with PENDING status (not taken or skipped)

**Files Modified:**
- `app/src/main/java/com/curatrack/app/schedule/data/DoseEntity.kt`
- `app/src/main/java/com/curatrack/app/schedule/data/DoseProjection.kt`
- `app/src/main/java/com/curatrack/app/schedule/data/ScheduleDao.kt`
- `app/src/main/java/com/curatrack/app/core/notifications/DoseActionReceiver.kt`
- `app/src/main/java/com/curatrack/app/schedule/ui/HistoryScreen.kt`

## Database Migration Required

⚠️ **IMPORTANT:** Since we added a new `status` column to the `doses` table, you'll need to handle database migration.

### Option 1: Clear App Data (Development)
The easiest way during development:
1. Uninstall the app completely
2. Reinstall and run the new version
3. All data will be recreated with the new schema

### Option 2: Increment Database Version (Production)
For production, add a migration in `AppDatabase.kt`:

```kotlin
@Database(
    entities = [/* ... */],
    version = 2, // Increment version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE doses ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "curatrack_database"
            )
            .addMigrations(MIGRATION_1_2)
            .build()
        }
    }
}
```

## How to Build and Test

1. Open the project in **Android Studio**
2. Sync Gradle dependencies
3. **Clear app data** or uninstall the app (to handle database schema change)
4. Clean and rebuild:
   - **Build** → **Clean Project**
   - **Build** → **Rebuild Project**
5. Run the app

## Testing the Fixes

### Test Medicine Name in Notification:
1. Create a schedule for a medicine
2. Wait for the notification to appear (or set a time very soon)
3. Verify the notification shows the medicine name (e.g., "Aspirin") not an ID
4. Verify it shows dosage and form (e.g., "100mg • Tablet")

### Test Taken/Skip Actions:
1. When a notification appears, tap **✓ Taken**
2. Go to History → Past tab
3. Verify the dose shows a green "✓ Taken" badge
4. For the next notification, tap **⊗ Skip**
5. Verify it shows an orange "✗ Skipped" badge in history

### Test Missed Status:
1. Let a notification expire without tapping Taken or Skip
2. Wait for the time to pass
3. Go to History → Past tab
4. Verify past untaken doses show a red "⊗ Missed" badge

## UI Examples

### Notification:
```
💊 Medicine Reminder
Time to take: Aspirin (100mg Tablet)

[✓ Taken]  [⊗ Skip]
```

### History - Past Tab:
```
┌─────────────────────────────────────┐
│ Aspirin  [✓ Taken]                  │
│ 100mg • Tablet                      │
│ Fri, Nov 2 2025 8:00 AM            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Ibuprofen  [✗ Skipped]              │
│ 200mg • Tablet                      │
│ Thu, Nov 1 2025 8:00 PM            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Vitamin D  [⊗ Missed]               │
│ 50000 IU • Capsule                  │
│ Thu, Nov 1 2025 9:00 AM            │
└─────────────────────────────────────┘
```

## Benefits

1. **Better User Experience:** Users see actual medicine names in notifications
2. **Medication Adherence Tracking:** Users and caregivers can see which doses were taken, skipped, or missed
3. **Medical Records:** Complete history of medication compliance
4. **Actionable Insights:** Identify patterns of missed or skipped medications
