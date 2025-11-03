# 🗄️ CuraTrack Database Entity Relationship Diagram

## 📊 Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    USERS ||--o{ MEDICINES : owns
    USERS ||--o{ SCHEDULES : creates
    MEDICINES ||--o{ SCHEDULES : "scheduled for"
    SCHEDULES ||--o{ DOSES : generates
    
    USERS {
        bigint id PK "Auto-increment primary key"
        string email UK "Unique email address"
        string password_hash "Hashed password"
        bigint created_at "Timestamp"
        bigint updated_at "Timestamp"
    }
    
    MEDICINES {
        string id PK "UUID primary key"
        bigint userId FK "Foreign key to users"
        string name "Medicine name (e.g., Paracetamol)"
        string dosage "Dosage amount (e.g., 500mg)"
        string form "Medicine form (e.g., Tablet)"
        string instructions "Optional instructions"
        string color "UI color tag"
        string icon "UI icon identifier"
        boolean active "Active status (soft delete)"
        bigint created_at "Timestamp"
        bigint updated_at "Timestamp"
    }
    
    SCHEDULES {
        string id PK "UUID primary key"
        bigint userId FK "Foreign key to users"
        string medicineId FK "Foreign key to medicines"
        bigint startDateEpochMillis "Schedule start time"
        bigint endDateEpochMillis "Schedule end time (nullable)"
        string timezoneId "Timezone identifier"
        string frequencyType "TIMES_PER_DAY|INTERVAL|WEEKLY"
        string timesJson "JSON array of times"
        int intervalMinutes "Interval in minutes (for INTERVAL type)"
        string daysOfWeekJson "JSON array of weekdays"
        boolean exactAlarm "Exact vs approximate timing"
        bigint created_at "Timestamp"
        bigint updated_at "Timestamp"
    }
    
    DOSES {
        string id PK "UUID primary key"
        string scheduleId FK "Foreign key to schedules"
        bigint timeEpochMillis "When to take the dose"
        string generatedFrom "SCHEDULE or MANUAL"
    }
```

## 🔗 Database Connectivity Architecture

```mermaid
graph TB
    subgraph "Application Layer"
        UI1[AddScheduleScreen]
        UI2[HistoryScreen]
        UI3[MedicineListScreen]
    end
    
    subgraph "Business Logic Layer"
        REPO1[MedicineRepository]
        REPO2[ScheduleRepository]
        REPO3[AuthRepository]
    end
    
    subgraph "Data Access Layer"
        DAO1[MedicineDao]
        DAO2[ScheduleDao]
        DAO3[UserDao]
    end
    
    subgraph "Database Layer"
        ROOM[Room Database]
        SQLITE[(SQLite Database)]
    end
    
    subgraph "System Integration"
        ALARM[AlarmManager]
        NOTIF[NotificationManager]
    end
    
    %% UI to Repository connections
    UI1 --> REPO1
    UI1 --> REPO2
    UI2 --> REPO2
    UI3 --> REPO1
    
    %% Repository to DAO connections
    REPO1 --> DAO1
    REPO2 --> DAO2
    REPO3 --> DAO3
    
    %% DAO to Database connections
    DAO1 --> ROOM
    DAO2 --> ROOM
    DAO3 --> ROOM
    ROOM --> SQLITE
    
    %% System integrations
    REPO2 --> ALARM
    ALARM --> NOTIF
    
    %% Styling
    classDef uiLayer fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef businessLayer fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef dataLayer fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef dbLayer fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef systemLayer fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    
    class UI1,UI2,UI3 uiLayer
    class REPO1,REPO2,REPO3 businessLayer
    class DAO1,DAO2,DAO3 dataLayer
    class ROOM,SQLITE dbLayer
    class ALARM,NOTIF systemLayer
```

## 🔧 Detailed Connection Specifications

### **Room Database Configuration**
```kotlin
// Database connection configuration
@Database(
    entities = [UserEntity::class, MedicineEntity::class, ScheduleEntity::class, DoseEntity::class],
    version = 1
)
abstract class CuraTrackDatabase : RoomDatabase() {
    
    // Connection pool management
    companion object {
        @Volatile
        private var INSTANCE: CuraTrackDatabase? = null
        
        fun getDatabase(context: Context): CuraTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CuraTrackDatabase::class.java,
                    "curatrack_database"
                )
                .addMigrations(/* future migrations */)
                .fallbackToDestructiveMigration() // Development only
                .build()
            }
        }
    }
}
```

### **Repository Connection Pattern**
```kotlin
// Dependency Injection for database connections
@Singleton
class MedicineRepository @Inject constructor(
    private val medicineDao: MedicineDao
) {
    // Reactive data streams
    fun getMedicines(userId: Long): Flow<List<MedicineEntity>> = 
        medicineDao.getMedicinesByUser(userId)
    
    // Transactional operations
    suspend fun addMedicine(medicine: MedicineEntity) = 
        medicineDao.insert(medicine)
}
```

## 📋 Data Flow Mapping

### **Create Schedule Flow**
```
AddScheduleScreen
    ↓ (user input)
ScheduleRepository.createOrUpdateSchedule()
    ↓ (validate & transform)
ScheduleDao.upsertSchedule()
    ↓ (Room ORM)
SQLite INSERT INTO schedules
    ↓ (generate doses)
ScheduleDao.upsertDoses()
    ↓ (Room ORM)
SQLite INSERT INTO doses (multiple)
    ↓ (schedule alarms)
AlarmManager.setExact()
```

### **History Display Flow**
```
HistoryScreen.onAppear
    ↓ (request data)
ScheduleRepository.getUpcomingDoses()
    ↓ (reactive query)
ScheduleDao.getUpcomingDoseProjections()
    ↓ (complex JOIN)
SQLite SELECT with JOINs
    ↓ (stream results)
Flow<List<DoseProjection>>
    ↓ (UI binding)
LazyColumn with medicine details
```

## 🎯 Connection Optimization Strategies

### **Database Performance**
1. **Connection Pooling**: Room manages SQLite connections efficiently
2. **Query Optimization**: Strategic indexes on frequently queried columns
3. **Lazy Loading**: Data loaded only when UI components need it
4. **Caching**: Repository layer caches frequently accessed entities

### **Memory Management**
1. **Flow-based Reactive**: Prevents memory leaks with automatic cleanup
2. **Pagination**: Large datasets loaded in chunks
3. **Background Threading**: Database operations off main UI thread
4. **Transaction Management**: Atomic operations for data consistency

### **Real-time Updates**
```kotlin
// Reactive data binding
@Composable
fun HistoryScreen(repo: ScheduleRepository) {
    // Automatically updates when database changes
    val upcomingDoses by repo.getUpcomingDoses(userId)
        .collectAsState(initial = emptyList())
    
    LazyColumn {
        items(upcomingDoses) { dose ->
            DoseCard(dose) // UI updates automatically
        }
    }
}
```

## 🔒 Data Security & Integrity

### **Security Measures**
- **SQL Injection Prevention**: Parameterized queries through Room
- **User Isolation**: All queries filtered by userId
- **Password Security**: Bcrypt hashing for user passwords
- **Local Storage**: SQLite database stored in app's private directory

### **Data Integrity**
- **Foreign Key Constraints**: Maintain referential integrity
- **Cascade Deletes**: Proper cleanup when parent entities deleted
- **Transaction Atomicity**: All-or-nothing operations
- **Validation**: Entity validation before database operations

### **Backup & Migration**
```kotlin
// Database migration strategy
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE medicines ADD COLUMN category TEXT")
    }
}

// Backup capabilities
Room.databaseBuilder(context, CuraTrackDatabase::class.java, "curatrack_db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

This comprehensive database connectivity system ensures reliable, scalable, and secure medicine scheduling data management! 🚀