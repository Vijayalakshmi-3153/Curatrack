# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Curatrack is an Android medication tracking application built with Kotlin and Jetpack Compose. It helps users manage their medications, set schedules, receive reminders, and scan prescriptions using ML Kit text recognition.

## Development Commands

### Building the Project
```powershell
# Build debug APK
.\gradlew assembleDebug

# Build release APK
.\gradlew assembleRelease

# Clean and rebuild
.\gradlew clean
.\gradlew build
```

### Running Tests
```powershell
# Run all unit tests
.\gradlew test

# Run instrumented tests
.\gradlew connectedAndroidTest

# Run specific test class
.\gradlew test --tests "com.curatrack.app.auth.data.AuthRepositoryTest"
```

### Development Tools
```powershell
# Install on connected device/emulator
.\gradlew installDebug

# Launch with specific device
.\gradlew installDebug -Padb.device.id=DEVICE_ID

# Generate test coverage report
.\gradlew testDebugUnitTestCoverage
```

### Code Quality
```powershell
# Run lint checks
.\gradlew lint

# Generate lint report
.\gradlew lintDebug
```

## Architecture Overview

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Dagger Hilt
- **Database**: Room (SQLite)
- **Navigation**: Navigation Compose
- **Background Work**: WorkManager + AlarmManager
- **ML**: ML Kit Text Recognition for prescription scanning

### Core Modules

#### Authentication (`auth/`)
- User registration and login with password hashing
- Session management via DataStore preferences
- Repository: `AuthRepository`, DAO: `UserDao`, Entity: `UserEntity`

#### Medicine Management (`medicine/`)
- CRUD operations for user medications
- Medicine metadata: name, dosage, form, instructions, UI styling
- Repository: `MedicineRepository`, DAO: `MedicineDao`, Entity: `MedicineEntity`

#### Scheduling (`schedule/`)
- Flexible medication schedules (times per day, intervals, weekly)
- Dose tracking and history
- Repository: `ScheduleRepository`, DAO: `ScheduleDao`
- Entities: `ScheduleEntity`, `DoseEntity`

#### Notifications (`core/notifications/`)
- Exact alarm scheduling for medication reminders
- Push notifications via `DoseAlarmReceiver`
- Background work with `ReminderWorker`

#### Prescription Scanning (`prescription/`)
- Camera integration for prescription capture
- ML Kit text recognition for automated data extraction

### Database Schema
The app uses Room with 4 main entities:
- `UserEntity`: User accounts and authentication
- `MedicineEntity`: Medicine catalog per user
- `ScheduleEntity`: Medication schedules with flexible timing
- `DoseEntity`: Individual dose records and completion tracking

### Navigation Structure
- Authentication flow: Login → Register
- Main flow: Medicine List → Add/Edit Medicine → Add Schedule → History
- Prescription scanning is accessible from medicine list

### Dependency Injection
Hilt modules are organized by feature:
- `AppModule`: Application-wide dependencies
- `AuthModule`: Authentication services
- `MedicineModule`: Medicine management
- `ScheduleModule`: Scheduling and notifications

## Key Development Patterns

### Entity ID Generation
Medicine and Schedule entities use String UUIDs, not auto-increment integers. Generate IDs using `java.util.UUID.randomUUID().toString()`.

### Time Handling
- All timestamps are stored as epoch milliseconds
- Schedules include timezone information for accurate cross-timezone functionality
- Schedule times stored as JSON arrays of LocalTime strings

### Repository Pattern
All data access goes through repositories that abstract Room DAOs. Repositories are injected via Hilt and expose suspend functions for coroutine compatibility.

### Navigation Arguments
Routes with parameters use string templates: `"add_edit_medicine/{medicineId}"`. Extract arguments from `NavBackStackEntry.arguments?.getString()`.

### Notification Permissions
The app requires `SCHEDULE_EXACT_ALARM` and `POST_NOTIFICATIONS` permissions for medication reminders. Ensure alarm scheduling respects Android 14+ exact alarm restrictions.

## Development Notes

### Testing Database Operations
When writing tests for DAOs, use an in-memory Room database. All database operations are suspend functions requiring coroutine test runners.

### Working with Compose UI
- All screens are Composables that take repository dependencies as parameters
- State management uses `collectAsState()` for Flow observation
- Navigation callbacks are passed down from parent composables

### Background Work Integration
- `AlarmManager` handles exact timing for critical medication reminders
- `WorkManager` handles less time-critical background tasks
- Coordinate between both systems to avoid duplicate notifications

### ML Kit Integration
Prescription scanning uses on-device ML Kit text recognition. No network requests are made for OCR processing.