# 📊 CuraTrack Data Flow Diagram & Database Connectivity

## 🏗️ System Architecture Overview

```mermaid
graph TD
    A[User Interface Layer] --> B[Repository Layer]
    B --> C[Room Database Layer]
    C --> D[SQLite Database]
    
    E[Notification System] --> F[AlarmManager]
    F --> G[BroadcastReceivers]
    
    B --> E
    G --> B
```

## 📱 Complete Data Flow Diagram

```mermaid
flowchart TD
    %% User Interface Layer
    UI1[Medicine List Screen]
    UI2[Add Schedule Screen]
    UI3[History Screen]
    UI4[Add Medicine Screen]
    
    %% Repository Layer
    REPO1[Medicine Repository]
    REPO2[Schedule Repository]
    
    %% Database Layer
    DAO1[Medicine DAO]
    DAO2[Schedule DAO]
    DB[(SQLite Database)]
    
    %% Notification System
    ALARM[AlarmManager]
    RECEIVER1[DoseAlarmReceiver]
    RECEIVER2[DoseActionReceiver]
    NOTIF[Notification System]
    
    %% Data Flow for Adding Medicine
    UI4 -->|Create Medicine| REPO1
    REPO1 -->|Insert Medicine| DAO1
    DAO1 -->|SQL INSERT| DB
    
    %% Data Flow for Adding Schedule
    UI2 -->|Get Medicine Info| REPO1
    REPO1 -->|Query Medicine| DAO1
    DAO1 -->|SQL SELECT| DB
    DB -->|Medicine Data| DAO1
    DAO1 -->|Medicine Entity| REPO1
    REPO1 -->|Medicine Info| UI2
    
    UI2 -->|Create Schedule| REPO2
    REPO2 -->|Insert Schedule| DAO2
    DAO2 -->|SQL INSERT| DB
    REPO2 -->|Generate Doses| DAO2
    DAO2 -->|SQL INSERT Doses| DB
    
    %% Data Flow for Schedule Alarms
    REPO2 -->|Schedule Alarm| ALARM
    ALARM -->|Trigger at Time| RECEIVER1
    RECEIVER1 -->|Show Notification| NOTIF
    RECEIVER1 -->|Create Actions| RECEIVER2
    
    %% Data Flow for History
    UI3 -->|Get Upcoming/Past| REPO2
    REPO2 -->|Query Doses with Medicine| DAO2
    DAO2 -->|SQL JOIN SELECT| DB
    DB -->|Dose + Medicine Data| DAO2
    DAO2 -->|DoseProjection List| REPO2
    REPO2 -->|Formatted Data| UI3
    
    %% User Actions from Notifications
    RECEIVER2 -->|Mark as Taken/Snooze| ALARM
    RECEIVER2 -->|Reschedule| ALARM
    
    %% Styling
    classDef uiClass fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef repoClass fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef dbClass fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef notifClass fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class UI1,UI2,UI3,UI4 uiClass
    class REPO1,REPO2 repoClass
    class DAO1,DAO2,DB dbClass
    class ALARM,RECEIVER1,RECEIVER2,NOTIF notifClass
```

## 🗄️ Database Schema & Connectivity

### **Database Tables Structure**

```sql
-- Users Table (handled by Auth system)
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Medicines Table
CREATE TABLE medicines (
    id TEXT PRIMARY KEY,                 -- UUID string
    userId INTEGER NOT NULL,             -- Foreign key to users
    name TEXT NOT NULL,                  -- e.g., "Paracetamol"
    dosage TEXT NOT NULL,                -- e.g., "500mg"
    form TEXT NOT NULL,                  -- e.g., "Tablet"
    instructions TEXT,                   -- e.g., "After meals"
    color TEXT,                          -- UI color tag
    icon TEXT,                           -- UI icon identifier
    active INTEGER DEFAULT 1,           -- Boolean: 1=active, 0=deleted
    created_at INTEGER NOT NULL,        -- Timestamp
    updated_at INTEGER NOT NULL,        -- Timestamp
    
    FOREIGN KEY (userId) REFERENCES users(id)
);

-- Schedules Table
CREATE TABLE schedules (
    id TEXT PRIMARY KEY,                 -- UUID string
    userId INTEGER NOT NULL,             -- Foreign key to users
    medicineId TEXT NOT NULL,            -- Foreign key to medicines
    startDateEpochMillis INTEGER NOT NULL,
    endDateEpochMillis INTEGER,          -- NULL = no end date
    timezoneId TEXT NOT NULL,            -- e.g., "America/New_York"
    frequencyType TEXT NOT NULL,        -- "TIMES_PER_DAY", "INTERVAL", "WEEKLY"
    timesJson TEXT,                      -- JSON: ["08:00","20:00"]
    intervalMinutes INTEGER,             -- For INTERVAL type
    daysOfWeekJson TEXT,                 -- JSON: ["MON","WED","FRI"]
    exactAlarm INTEGER DEFAULT 1,       -- Boolean: precise vs approximate
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    
    FOREIGN KEY (userId) REFERENCES users(id),
    FOREIGN KEY (medicineId) REFERENCES medicines(id) ON DELETE CASCADE
);

-- Doses Table (Generated from Schedules)
CREATE TABLE doses (
    id TEXT PRIMARY KEY,                 -- UUID string
    scheduleId TEXT NOT NULL,            -- Foreign key to schedules
    timeEpochMillis INTEGER NOT NULL,    -- When to take the dose
    generatedFrom TEXT NOT NULL,         -- "SCHEDULE" or "MANUAL"
    
    FOREIGN KEY (scheduleId) REFERENCES schedules(id) ON DELETE CASCADE
);

-- Indexes for Performance
CREATE INDEX idx_medicines_user ON medicines(userId);
CREATE INDEX idx_schedules_user ON schedules(userId);
CREATE INDEX idx_schedules_medicine ON schedules(medicineId);
CREATE INDEX idx_doses_schedule ON doses(scheduleId);
CREATE INDEX idx_doses_time ON doses(timeEpochMillis);
```

### **Room Database Configuration**

```kotlin
@Database(
    entities = [
        UserEntity::class,
        MedicineEntity::class,
        ScheduleEntity::class,
        DoseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CuraTrackDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    
    companion object {
        @Volatile
        private var INSTANCE: CuraTrackDatabase? = null
        
        fun getDatabase(context: Context): CuraTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CuraTrackDatabase::class.java,
                    "curatrack_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

## 🔄 Detailed Data Flow Scenarios

### **Scenario 1: Adding a New Medicine Schedule**

```mermaid
sequenceDiagram
    participant U as User
    participant AS as AddScheduleScreen
    participant MR as MedicineRepository
    participant SR as ScheduleRepository
    participant MD as MedicineDAO
    participant SD as ScheduleDAO
    participant DB as SQLite Database
    participant AM as AlarmManager
    
    U->>AS: Fill schedule form
    AS->>MR: getMedicine(medicineId, userId)
    MR->>MD: getMedicineById(id, userId)
    MD->>DB: SELECT * FROM medicines WHERE id=? AND userId=?
    DB-->>MD: Medicine data
    MD-->>MR: MedicineEntity
    MR-->>AS: Medicine details
    AS->>AS: Display medicine info
    
    U->>AS: Submit schedule
    AS->>SR: createOrUpdateSchedule(params)
    SR->>SD: upsertSchedule(scheduleEntity)
    SD->>DB: INSERT INTO schedules VALUES(...)
    SR->>SR: generateDoses(schedule, horizonDays)
    SR->>SD: upsertDoses(doseList)
    SD->>DB: INSERT INTO doses VALUES(...) [Multiple]
    
    SR->>AM: scheduleExactDose(context, time, medicineId)
    AM->>AM: Set system alarm
    
    SR-->>AS: Success
    AS-->>U: Navigate back + success
```

### **Scenario 2: Displaying History**

```mermaid
sequenceDiagram
    participant U as User
    participant HS as HistoryScreen
    participant SR as ScheduleRepository
    participant SD as ScheduleDAO
    participant DB as SQLite Database
    
    U->>HS: Open History tab
    HS->>SR: getUpcomingDoses(userId)
    SR->>SD: getUpcomingDoseProjections(userId, now)
    SD->>DB: Complex JOIN query
    Note over SD,DB: SELECT d.scheduleId, s.medicineId, d.timeEpochMillis,<br/>m.name, m.dosage, m.form<br/>FROM doses d<br/>JOIN schedules s ON d.scheduleId = s.id<br/>JOIN medicines m ON s.medicineId = m.id<br/>WHERE s.userId = ? AND d.timeEpochMillis >= ?
    DB-->>SD: Joined results
    SD-->>SR: Flow<List<DoseProjection>>
    SR-->>HS: Observable dose list
    HS->>HS: Display in UI cards
    
    U->>HS: Switch to Past tab
    HS->>SR: getPastDoses(userId)
    Note over SR: Similar flow with different time filter
```

### **Scenario 3: Notification Flow**

```mermaid
sequenceDiagram
    participant AM as AlarmManager
    participant DAR as DoseAlarmReceiver
    participant NS as NotificationSystem
    participant U as User
    participant DATR as DoseActionReceiver
    
    AM->>DAR: Broadcast at scheduled time
    DAR->>DAR: Extract medicine info
    DAR->>NS: Create rich notification
    NS->>NS: Show notification with actions
    NS-->>U: Display notification
    
    alt User taps "Taken"
        U->>DATR: Action: TAKEN
        DATR->>NS: Dismiss notification
        DATR->>DATR: Mark as completed (future: update DB)
    else User taps "Snooze"
        U->>DATR: Action: SNOOZE
        DATR->>NS: Dismiss notification
        DATR->>AM: Schedule new alarm (+10 min)
        AM->>AM: Set snooze alarm
    end
```

## 💾 Database Connectivity Details

### **Repository Pattern Implementation**

```kotlin
// Data flows through this pattern:
UI Layer → Repository → DAO → Room Database → SQLite

// Example: Medicine Repository
class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    // Flow-based reactive data
    fun getMedicines(userId: Long): Flow<List<MedicineEntity>> {
        return medicineDao.getMedicinesByUser(userId)
    }
    
    // Suspend functions for operations
    suspend fun addMedicine(userId: Long, name: String, ...): String {
        val medicine = MedicineEntity(...)
        medicineDao.insert(medicine)
        return medicine.id
    }
}
```

### **Key Database Relationships**

```
Users (1) ←→ (Many) Medicines
  ↓
Medicines (1) ←→ (Many) Schedules  
  ↓
Schedules (1) ←→ (Many) Doses
```

### **Query Examples**

```kotlin
// Get upcoming doses with medicine information
@Query("""
    SELECT d.scheduleId, s.medicineId, d.timeEpochMillis,
           m.name AS medicineName, m.dosage AS medicineDosage, m.form AS medicineForm
    FROM doses d 
    INNER JOIN schedules s ON d.scheduleId = s.id
    INNER JOIN medicines m ON s.medicineId = m.id
    WHERE s.userId = :userId AND d.timeEpochMillis >= :now
    ORDER BY d.timeEpochMillis ASC
""")
fun getUpcomingDoseProjections(userId: Long, now: Long): Flow<List<DoseProjection>>
```

## 🔧 Data Validation & Integrity

### **Entity Validations**
- **Medicine**: Name required, dosage format validation
- **Schedule**: Date range validation, frequency type constraints
- **Dose**: Future timestamps only, valid schedule reference

### **Cascade Operations**
- Delete Medicine → Delete all related Schedules → Delete all related Doses
- Delete Schedule → Delete all related Doses
- User deletion → Cascade to all user data

### **Transaction Management**
```kotlin
// Schedule creation is atomic
suspend fun createOrUpdateSchedule(...): String {
    return withTransaction {
        val scheduleId = scheduleDao.upsertSchedule(schedule)
        scheduleDao.clearDoses(scheduleId)
        if (doses.isNotEmpty()) {
            scheduleDao.upsertDoses(doses)
        }
        scheduleId
    }
}
```

## 📊 Performance Considerations

### **Database Optimization**
- **Indexes**: Strategic indexes on userId, medicineId, timeEpochMillis
- **Pagination**: History queries limited to reasonable ranges
- **Caching**: Repository pattern caches frequently accessed data
- **Background Processing**: Dose generation happens off main thread

### **Memory Management**
- **Flow-based**: Reactive streams prevent memory leaks
- **Lazy Loading**: Only load data when screens are active
- **Efficient Queries**: JOINs minimize database round trips

This comprehensive data flow system ensures reliable, performant medicine scheduling with proper data integrity and user experience! 🚀