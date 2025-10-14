# 진행 상황 점검 보고서

## README 기준 현재 요약
- README에는 도메인 모델, 애그리게이터, 알림 오케스트레이션이 구현되었고 저장소/알림 계층은 목(mock) 상태이며 Compose UI 작업이 남아 있다고 적혀 있습니다.【F:README.md†L62-L74】
- 저장소 코드도 이를 확인해 줍니다. 도메인 엔터티와 알림 로직은 `app/src/main/kotlin/com/example/calendar/data` 및 `.../reminder` 아래에 정리되어 있고, `QuickStartAppContainer`는 여전히 인메모리 저장소와 `NoOpReminderScheduler`에 연결되어 있습니다.【F:app/src/main/kotlin/com/example/calendar/data/Task.kt†L14-L35】【F:app/src/main/kotlin/com/example/calendar/reminder/ReminderOrchestrator.kt†L10-L66】【F:app/src/main/kotlin/com/example/calendar/QuickStartAppContainer.kt†L31-L96】
- 다만 README의 설명과 달리, 현재 레포에는 `AgendaRoute`/`AgendaScreen`을 포함한 Compose UI 뼈대가 존재하므로 “UI 미구현”이라는 문구는 최신 상태가 아닙니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L353】

## `docs/next-steps.md` 체크리스트 진행도
1. **UI 뼈대 구성 (Step 1)** – 완료. `AgendaRoute`가 `AgendaViewModel`과 상태를 연동하고, 탭/기간 상태를 관리하며 `AgendaAggregator`에서 가져온 일/주/월 콘텐츠를 렌더링합니다.【F:docs/next-steps.md†L10-L18】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L210】【F:app/src/main/kotlin/com/example/calendar/scheduler/AgendaAggregator.kt†L16-L53】
2. **상호작용 추가 (Step 2)** – 부분 충족. 상세 정보 바텀시트에서 토글·삭제·편집 훅을 제공하고, 작업 행은 접근성 라벨을 포함한 스와이프 완료 제스처를 지원합니다.【F:docs/next-steps.md†L15-L18】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L287-L758】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1187-L1334】
3. **입력 및 저장 (Step 3)** – **미완료.** 스캐폴드에는 `onQuickAddClick` 슬롯이 노출되어 있지만 `AgendaRoute`에서 실제 핸들러를 전달하지 않고, 퀵 추가 시트를 구현한 코드도 없으며 참조된 `QuickAddType` 열거형이 존재하지 않아 FAB으로 항목을 추가할 수 없습니다.【F:docs/next-steps.md†L19-L21】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L287-L369】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L175-L210】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L356-L369】
4. **알림 연결 준비 (Step 4)** – 진행 전. 오케스트레이터는 여전히 `NoOpReminderScheduler`에 위임하고 있으며 런타임 권한 확인을 위한 주석이나 흐름도 준비되어 있지 않습니다.【F:docs/next-steps.md†L22-L24】【F:app/src/main/kotlin/com/example/calendar/QuickStartAppContainer.kt†L90-L96】
5. **품질 보강 (Step 5)** – 부분 진행. ViewModel/도메인 테스트(`AgendaViewModelTest`, `AgendaAggregatorTest`, `ReminderOrchestratorTest`)는 존재하지만 Compose 프리뷰나 UI 테스트는 아직 마련되지 않았습니다.【F:docs/next-steps.md†L25-L27】【F:app/src/test/kotlin/com/example/calendar/ui/AgendaViewModelTest.kt†L33-L153】

## 추가 관찰
- 현재 아젠다 화면에는 `SwipeableTaskRow`, `AgendaDetailSheet` 등 중복된 컴포저블이 함께 존재해 머지 충돌 잔재로 보이며, 본격적인 UI 작업에 앞서 정리가 필요합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L807-L1009】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1032-L1406】
- 퀵 추가 로직이 비어 있으므로, 실제 알람/저장소 계층을 통합하거나 UI 프리뷰·테스트를 도입하기 전에 Step 3 완성이 가장 우선입니다.
