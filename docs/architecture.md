# Architecture overview

The calendar application is structured using a clean architecture-inspired approach with three layers:

1. **Presentation (UI/ViewModel)** – Jetpack Compose screens backed by ViewModels that expose state flows.
2. **Domain** – Use cases and business logic governing scheduling, reminder calculation, and task completion.
3. **Data** – Repository implementations that coordinate Room database, AlarmManager integrations, and optional remote sync.

```
┌──────────────┐      ┌──────────────┐      ┌───────────────┐
│   Compose    │◀────▶│  ViewModel   │◀────▶│   Use Cases    │
└──────────────┘      └──────────────┘      └───────────────┘
                                           ▲            ▲
                                           │            │
                                   ┌─────────────┐ ┌──────────────┐
                                   │Repositories │ │ AlarmWorker  │
                                   └─────────────┘ └──────────────┘
                                           ▲            ▲
                                           │            │
                                   ┌──────────────┐ ┌──────────────┐
                                   │   Room DB    │ │ Notification │
                                   │              │ │  Scheduler   │
                                   └──────────────┘ └──────────────┘
```

## Domain layer components

- `Task` and `CalendarEvent` models capture common scheduling properties.
- `AgendaPeriod` sealed class encapsulates day/week/month ranges.
- `TaskRepository` and `EventRepository` interfaces expose flows of domain models.
- Use cases (e.g., `CreateTaskUseCase`, `GetAgendaUseCase`, `ScheduleReminderUseCase`) orchestrate business rules.

## Reminder scheduling

- A `ReminderScheduler` translates domain reminders into OS-level alarms.
- Reminders are persisted in Room with metadata for restoration after reboot.
- WorkManager handles long-running or network-dependent operations.

## UI considerations

- Compose screens subscribe to `StateFlow` data exposed by ViewModels.
- Weekly/monthly calendars share a core `CalendarGridState` to reuse selection/highlighting logic.
- Tasks leverage lazy column lists with swipe actions for quick completion toggles.

## Data persistence

- Room entities mirror domain models with converters for recurrence rules.
- DAO queries provide aggregated lists for day/week/month filtering.
- Repository implementations map DAO results to domain models and dispatch reminder scheduling side effects.

## Testing strategy

- Unit tests for use cases mock repositories to verify business logic.
- Instrumented tests interact with Room + AlarmManager using Robolectric or emulator.
- Snapshot tests ensure Compose calendar views render expected states.
