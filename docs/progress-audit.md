# 진행 상황 점검 보고서

## README 기준 현재 요약
- README는 도메인 계층, 아젠다 UI, 빠른 추가, 그리고 알림 오케스트레이션이 연결되었음을 명시하고 있습니다.【F:README.md†L42-L89】
- 런타임 구현에서도 `QuickStartAppContainer`가 `AndroidReminderScheduler`를 주입해 AlarmManager/WorkManager와 연동하고, `CalendarApp`이 알림 권한 카드를 노출합니다.【F:app/src/main/kotlin/com/example/calendar/QuickStartAppContainer.kt†L31-L109】【F:app/src/main/kotlin/com/example/calendar/ui/CalendarApp.kt†L15-L167】
- `AgendaRoute`/`AgendaScreen`은 상세 시트, 퀵 추가 시트, 스낵바 안내까지 포함한 인터랙션을 제공하고 있습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L324】

## `docs/next-steps.md` 체크리스트 진행도
1. **UI 뼈대 구성 (Step 1)** – 완료. `AgendaRoute`가 `AgendaViewModel`과 상태를 연동하고, 탭/기간 상태를 관리하며 `AgendaAggregator`에서 가져온 일/주/월 콘텐츠를 렌더링합니다.【F:docs/next-steps.md†L10-L18】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L210】【F:app/src/main/kotlin/com/example/calendar/scheduler/AgendaAggregator.kt†L16-L53】
2. **상호작용 추가 (Step 2)** – 세부 폴리시와 접근성 QA는 남아 있지만 상세 시트, 스와이프 액션, 스낵바 메시지가 작동합니다.【F:docs/next-steps.md†L15-L18】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L287-L758】
3. **입력 및 저장 (Step 3)** – 완료. FAB가 퀵 추가 시트를 열고, 입력 검증/성공 메시지를 처리하며 저장 시 리마인더를 예약합니다.【F:docs/next-steps.md†L19-L21】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L232-L324】【F:app/src/main/kotlin/com/example/calendar/ui/AgendaViewModel.kt†L57-L152】
4. **알림 연결 준비 (Step 4)** – 완료. 실제 스케줄러가 주입되었고, Compose에서 알림 권한 요청 카드를 노출합니다.【F:docs/next-steps.md†L22-L24】【F:app/src/main/kotlin/com/example/calendar/QuickStartAppContainer.kt†L90-L109】【F:app/src/main/kotlin/com/example/calendar/ui/CalendarApp.kt†L42-L160】
5. **품질 보강 (Step 5)** – 부분 진행. ViewModel/도메인 테스트(`AgendaViewModelTest`, `AgendaAggregatorTest`, `ReminderOrchestratorTest`)는 존재하지만 Compose 프리뷰나 UI 테스트는 아직 마련되지 않았습니다.【F:docs/next-steps.md†L25-L27】【F:app/src/test/kotlin/com/example/calendar/ui/AgendaViewModelTest.kt†L33-L153】

## 추가 관찰
- 현재 아젠다 화면에는 `SwipeableTaskRow`, `AgendaDetailSheet` 등 중복된 컴포저블이 함께 존재해 머지 충돌 잔재로 보이며, 본격적인 UI 작업에 앞서 정리가 필요합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L807-L1009】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1032-L1406】
- 알림 권한 카드를 한 번 닫으면 다시 보여주지 않으므로, 재노출 조건을 기획과 함께 정의해야 합니다.
