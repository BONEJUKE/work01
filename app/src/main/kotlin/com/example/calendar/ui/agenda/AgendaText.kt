package com.example.calendar.ui.agenda

object AgendaText {
    object Common {
        const val titleLabel = "제목"
        const val notesOptionalLabel = "메모 (선택)"
        const val locationOptionalLabel = "위치 (선택)"
        const val cancel = "취소"
        const val save = "저장"
        const val saving = "저장 중..."
        const val saveFailed = "저장에 실패했습니다."
        const val titleRequired = "제목을 입력해 주세요."
        const val edit = "편집"
        const val delete = "삭제"
        const val close = "닫기"
        const val reminderToggleLabel = "리마인더 사용"
        const val allowSnooze = "스누즈 허용"
        const val reminderIntro = "마감 전에 알림을 받아요."
        const val reminderEventIntro = "시작 전에 알림을 받아요."
        const val reminderSnoozeDescription = "알림에서 다시 알리기를 사용할 수 있어요."
        const val reminderEventSnoozeWarning = "이벤트 리마인더는 스누즈를 지원하지 않아요."
        const val reminderMinutesLabel = "알림 시점 (분)"
        fun reminderMinutesSummary(minutesText: String) = "마감 ${minutesText}분 전에 알림이 울립니다."
        fun reminderEventMinutesSummary(minutesText: String) = "시작 ${minutesText}분 전에 알림이 울립니다."
        const val reminderMinutesInvalid = "리마인더 시점은 1분 이상 입력해 주세요."
    }

    object TaskEdit {
        const val title = "할 일 편집"
        const val dueDateLabel = "마감 날짜 (YYYY-MM-DD)"
        const val dueDateHint = "비워 두면 날짜 없이 저장됩니다."
        const val dueTimeLabel = "마감 시간 (HH:mm)"
        const val dueTimeHint = "날짜 또는 시간을 비워 두면 마감 시간이 제거됩니다."
        const val dueDateRequired = "마감 날짜를 YYYY-MM-DD 형식으로 입력해 주세요."
        const val dueDateFormatError = "마감 날짜는 YYYY-MM-DD 형식이어야 해요."
        const val dueTimeRequired = "마감 시간을 HH:mm 형식으로 입력해 주세요."
        const val dueTimeFormatError = "마감 시간은 HH:mm 형식이어야 해요."
    }

    object EventEdit {
        const val title = "일정 편집"
        const val dateLabel = "날짜 (YYYY-MM-DD)"
        const val startTimeLabel = "시작 시간 (HH:mm)"
        const val endTimeLabel = "종료 시간 (HH:mm)"
        const val dateFormatError = "날짜는 YYYY-MM-DD 형식이어야 해요."
        const val startTimeFormatError = "시작 시간은 HH:mm 형식이어야 해요."
        const val endTimeFormatError = "종료 시간은 HH:mm 형식이어야 해요."
        const val endTimeBeforeStart = "종료 시간은 시작 시간 이후여야 합니다."
    }

    object Agenda {
        const val noEvents = "표시할 일정이 없어요. 필터를 확인해 보세요."
        const val noTasks = "표시할 할 일이 없어요. 필터를 확인해 보세요."
        const val eventsSectionTitle = "일정"
        const val tasksSectionTitle = "할 일"
        const val summaryDailyTitle = "일일 요약"
        const val summaryWeeklyTitle = "주간 요약"
        const val summaryMonthlyTitle = "월간 요약"
        const val showRecurring = "반복 일정 표시"
        const val markAsComplete = "완료로 표시"
        const val markAsIncomplete = "다시 미완료로 표시"
        const val conflictBanner = "다른 일정과 시간이 겹쳐요"
        fun conflictDescription(title: String, hasConflict: Boolean): String {
            return buildString {
                append(title)
                append(" 일정")
                if (hasConflict) {
                    append(", 다른 일정과 시간이 겹칩니다")
                }
            }
        }
        fun totalEvents(count: Int) = "전체 일정 ${count}건"
        fun progressSummary(pending: Int, completed: Int) = "진행 중 ${pending}건 · 완료 ${completed}건"
        fun visibleSummary(eventCount: Int, taskCount: Int) = "목록에 표시 중: 일정 ${eventCount}건 · 할 일 ${taskCount}건"
        fun overdueSummary(count: Int) = "마감 지남 ${count}건"
        fun conflictSummary(count: Int) = "시간이 겹치는 일정 ${count}건"
        fun hiddenRecurring(count: Int) = "반복 일정 ${count}건"
        fun hiddenCompletedTask(count: Int) = "완료된 할 일 ${count}건"
        fun hiddenPendingTask(count: Int) = "진행 중인 할 일 ${count}건"
        fun hiddenItems(details: String) = "필터로 숨겨진 항목: ${details}."
        fun summaryIntro(title: String, period: String) = "${title}. ${period} 일정 요약."
        fun summaryTotals(total: Int, pending: Int, completed: Int) =
            "전체 일정 ${total}건, 진행 중인 할 일 ${pending}건, 완료된 할 일 ${completed}건입니다."
        fun summaryOverdueDetail(count: Int) = "마감이 지난 할 일 ${count}건이 있어요."
        fun summaryConflictDetail(count: Int) = "시간이 겹치는 일정 ${count}건이 있어요."
        const val filterAllTasks = "할 일: 전체"
        const val filterHideCompleted = "할 일: 완료 숨김"
        const val filterCompletedOnly = "할 일: 완료만"
        const val filterAllTasksDescription = "모든 할 일을 표시합니다"
        const val filterHideCompletedDescription = "완료된 할 일을 숨깁니다"
        const val filterCompletedOnlyDescription = "완료된 할 일만 표시합니다"
        const val loading = "일정을 불러오는 중입니다"
        const val loadFailed = "일정을 불러오는 중 문제가 발생했어요"
        const val emptyTitle = "아직 일정이 없어요"
        const val emptyMessage = "빠른 추가 버튼으로 오늘의 첫 일정이나 할 일을 만들어 보세요."
        const val emptyAnnouncement = "아직 일정이 없어요. 빠른 추가 버튼으로 오늘의 첫 일정이나 할 일을 만들어 보세요."
        const val emptyAction = "빠른 추가 열기"
        const val fabDescription = "새 일정 또는 할 일 추가"
        const val agendaTabDaily = "일간"
        const val agendaTabWeekly = "주간"
        const val agendaTabMonthly = "월간"
        const val statusPending = "대기 중"
        const val statusInProgress = "진행 중"
        const val statusCompleted = "완료"
        const val statusCompletedDesc = "완료됨"
        const val statusPendingDesc = "미완료"
        fun dueLabel(detail: String) = "마감 ${detail}"
        fun statusLabel(displayName: String) = "상태: ${displayName}"
        fun accessibleTask(statusText: String, title: String) = "${statusText} 할 일: ${title}"
        const val toggleToPending = "대기 중으로 표시"
    }

    object QuickAdd {
        const val title = "빠른 추가"
        fun periodWithDate(period: String, date: String) = "${period} · ${date}"
        const val taskTab = "할 일"
        const val eventTab = "일정"
        const val dueTimeHint = "비워 두면 시간 없이 저장됩니다."
        const val reminderTitleMissing = "시간 형식은 HH:mm 이어야 해요."
        const val reminderMinutesMissing = "마감 시간을 설정해야 리마인더를 사용할 수 있어요."
        const val eventTimeCheck = "시간을 다시 확인해 주세요."
        const val eventStartFormatError = "시작 시간을 HH:mm 형식으로 입력하세요."
        const val eventEndFormatError = "종료 시간을 HH:mm 형식으로 입력하세요."
    }

    object Period {
        const val day = "하루"
        const val week = "한 주"
        const val month = "한 달"
    }

    object Accessibility {
        const val selected = "선택됨"
        const val notSelected = "선택되지 않음"
        const val previousPeriod = "이전 기간으로 이동"
        const val nextPeriod = "다음 기간으로 이동"
        const val dailyTab = "일간 아젠다 탭"
        const val weeklyTab = "주간 아젠다 탭"
        const val monthlyTab = "월간 아젠다 탭"
    }

    object Recurrence {
        const val daily = "매일"
        const val weekly = "매주"
        const val monthly = "매월"
        const val yearly = "매년"
        const val dayUnit = "일"
        const val weekUnit = "주"
        const val monthUnit = "달"
        const val yearUnit = "년"
        fun intervalLabel(interval: Int, unit: String) = "매 ${interval}${unit}"
    }

    object QuickAddResult {
        const val taskSuccess = "할 일을 추가했어요."
        const val eventSuccess = "일정을 추가했어요."
    }
}
