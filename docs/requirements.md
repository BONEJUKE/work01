# Functional requirements

## Core calendar & scheduling
- Users can create, edit, and delete calendar items that contain a title, optional description, start/end time, and optional location.
- Calendar items may be one-off events or repeating tasks (daily, weekly, monthly, or custom recurrence rules).
- Events appear on the calendar in daily, weekly, and monthly views.

## Task management
- Each calendar day may contain a to-do list with actionable tasks.
- Tasks can be marked as `Pending`, `InProgress`, or `Completed`.
- Tasks inherit the date context of their parent (day/week/month) but can also include explicit deadlines.
- Users can filter lists by completion state and recurrence type.

## Alarm & reminder system
- Users can attach one or more reminders to events and tasks.
- Reminders support offsets (e.g., 10 minutes before, 1 hour before, 1 day before).
- Reminder delivery uses Android's `AlarmManager` or `WorkManager` for reliability across device reboot.
- When a reminder triggers, the user receives a notification with quick actions (snooze, mark done, open details).

## Agenda aggregation
- The app aggregates all scheduled items into a consolidated agenda for the selected period (day/week/month).
- Agenda screens surface overdue items and highlight conflicts between events.

## Data persistence & sync
- Local storage uses Room with Flow/LiveData streams for UI updates.
- The app is offline-first and does not plan to integrate cloud sync; limited time drift checks ensure reminders stay reliable.

## Accessibility & copy guidelines
- All interactions support TalkBack and dynamic font sizing.
- Agenda copy is centralized so that labels and accessibility descriptions stay consistent in the single supported locale.

# Non-functional requirements
- Follow Material 3 guidelines for visual design.
- Offline-first behavior with background sync when connectivity returns.
- Unit and instrumentation tests cover scheduling, reminder calculations, and database queries.
- Modular architecture to support feature scaling.
