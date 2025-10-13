package com.example.calendar

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.InMemoryEventRepository
import com.example.calendar.data.InMemoryTaskRepository
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import com.example.calendar.data.TaskStatus
import com.example.calendar.reminder.NoOpReminderScheduler
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.scheduler.AgendaAggregator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Minimal container that wires the agenda UI to in-memory data so the app shows
 * meaningful content as soon as it launches. The goal is to get a runnable
 * build quickly without waiting for Room, WorkManager, or alarm integrations.
 */
class QuickStartAppContainer : AppContainer {

    private val today: LocalDate = LocalDate.now()
    private val weekStart: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val monthPeriod = AgendaPeriod.Month(today.year, today.monthValue)

    private val taskRepositoryImpl: InMemoryTaskRepository by lazy {
        InMemoryTaskRepository(
            tasks = listOf(
                Task(
                    title = "오늘 핵심 정리",
                    description = "앱에서 바로 확인하고 체크하세요.",
                    status = TaskStatus.InProgress,
                    dueAt = LocalDateTime.of(today, LocalTime.of(10, 0)),
                    period = AgendaPeriod.Day(today)
                ),
                Task(
                    title = "주간 목표 검토",
                    description = "주요 일정과 업무 상태 공유",
                    status = TaskStatus.Pending,
                    dueAt = LocalDateTime.of(weekStart.plusDays(2), LocalTime.of(14, 0)),
                    period = AgendaPeriod.Week(weekStart)
                ),
                Task(
                    title = "월간 리뷰 초안",
                    description = "핵심 지표와 회고 작성",
                    status = TaskStatus.Pending,
                    dueAt = LocalDateTime.of(monthPeriod.firstDay.plusDays(10), LocalTime.of(16, 0)),
                    period = monthPeriod
                )
            )
        )
    }

    private val eventRepositoryImpl: InMemoryEventRepository by lazy {
        InMemoryEventRepository(
            events = listOf(
                CalendarEvent(
                    title = "팀 스탠드업",
                    description = "10분 안에 진행 상황 공유",
                    start = LocalDateTime.of(today, LocalTime.of(9, 30)),
                    end = LocalDateTime.of(today, LocalTime.of(9, 45)),
                    location = "회의실 A"
                ),
                CalendarEvent(
                    title = "디자인 싱크",
                    start = LocalDateTime.of(weekStart.plusDays(3), LocalTime.of(15, 0)),
                    end = LocalDateTime.of(weekStart.plusDays(3), LocalTime.of(16, 0)),
                    location = "온라인"
                ),
                CalendarEvent(
                    title = "분기 리뷰",
                    description = "이번 달 목표와 데이터를 정리",
                    start = LocalDateTime.of(monthPeriod.firstDay.plusDays(12), LocalTime.of(11, 0)),
                    end = LocalDateTime.of(monthPeriod.firstDay.plusDays(12), LocalTime.of(12, 0)),
                    location = "대회의실"
                )
            )
        )
    }

    override val taskRepository: TaskRepository = taskRepositoryImpl

    override val eventRepository: EventRepository = eventRepositoryImpl

    override val reminderOrchestrator: ReminderOrchestrator by lazy {
        ReminderOrchestrator(NoOpReminderScheduler())
    }

    override val agendaAggregator: AgendaAggregator by lazy {
        AgendaAggregator(taskRepository, eventRepository)
    }
}

