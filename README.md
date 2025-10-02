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

## Next steps

- Integrate the domain logic with Android frameworks (ViewModel, WorkManager, AlarmManager).
- Persist data using Room or another database solution.
- Build Compose-based calendar and task list UI components.
- Add instrumentation/unit tests for scheduling logic and reminder calculations.
