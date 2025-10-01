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
3. Sync Gradle and build the project. The Gradle wrapper JAR will be downloaded automatically on first sync.
4. Run the app on an emulator or physical device running Android 8.0 (API 26) or higher.

## Project structure

- `app/src/main/java/com/example/calendar` – Application entry point, alarm receiver, notification setup
- `app/src/main/java/com/example/calendar/data` – Room entities, DAO, and converters
- `app/src/main/java/com/example/calendar/domain` – Repository and alarm scheduling utilities
- `app/src/main/java/com/example/calendar/ui` – Compose UI screens and view model logic
- `app/src/main/java/com/example/calendar/ui/theme` – Compose Material theme definitions

## Permissions

The application requests the `POST_NOTIFICATIONS` runtime permission on Android 13+ and declares `SCHEDULE_EXACT_ALARM` for precise reminders.
