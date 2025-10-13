codex/add-calendar-app-with-scheduler-and-alarm
# Work01 Calendar App

A product specification and Kotlin domain layer prototype for an Android calendar application. The app focuses on three primary capabilities:

1. **Scheduler** – create and manage events or tasks on specific dates and times.
2. **Alarm reminders** – configure notifications to remind users ahead of upcoming items.
3. **Task lists** – view actionable to-do lists for each day, week, or month with completion tracking.

The repository contains documentation for requirements, architecture decisions, and Kotlin domain logic that can be adapted into a full Android project.

## Repository layout

- `docs/` – requirements, architecture, and UX planning notes.
- `app/src/main/kotlin/` – Kotlin domain-layer prototypes (data models, use cases, and view models).

## Getting started

These files are intended as a foundation for bootstrapping a full Android Studio project:

1. Copy the `app/src/main/kotlin` package into your Android project.
2. Wire the domain classes to persistence (Room, Realm, etc.) and notification APIs.
3. Implement UI layers (Jetpack Compose or XML) guided by the UX flows in the documentation.

## Domain logic tests

Run the Kotlin domain unit tests with the system Gradle installation:

```bash
gradle test --console=plain --no-daemon
```

The repository omits the Gradle wrapper binaries to avoid committing unsupported binary artifacts, so invoking `gradle` directly ensures CI can execute the same verification task.

## Next steps

- Integrate the domain logic with Android frameworks (ViewModel, WorkManager, AlarmManager).
- Persist data using Room or another database solution.
- Build Compose-based calendar and task list UI components.
- Add instrumentation/unit tests for scheduling logic and reminder calculations.

# work01
# Calendar Planner

An Android calendar planner application featuring daily, weekly, and monthly task organization, reminders, and recurring task alarms. The project is built with Kotlin, Jetpack Compose, and Room.

## Features

- 📅 Date, week, and month scoped task lists with completion tracking
- 📝 Scheduler for creating detailed tasks with description, reminder lead time, and repeat cadence
- 🔔 Exact alarms with notification channel support for Android 13+ permission handling
- ♻️ Automatic rescheduling for recurring tasks (daily, weekly, monthly)

## Getting started

1. Ensure you have Android Studio Giraffe (or newer) with the latest Android SDK (API 34) installed.
2. Clone the repository and open it in Android Studio.
3. Sync Gradle and build the project.
4. Run the app on an emulator or physical device running Android 8.0 (API 26) or higher.

## Project structure

- `app/src/main/java/com/example/calendar` – Application entry point, alarm receiver, notification setup
- `app/src/main/java/com/example/calendar/data` – Room entities, DAO, and converters
- `app/src/main/java/com/example/calendar/domain` – Repository and alarm scheduling utilities
- `app/src/main/java/com/example/calendar/ui` – Compose UI screens and view model logic
- `app/src/main/java/com/example/calendar/ui/theme` – Compose Material theme definitions

## Permissions

The application requests the `POST_NOTIFICATIONS` runtime permission on Android 13+ and declares `SCHEDULE_EXACT_ALARM` for precise reminders.
main
