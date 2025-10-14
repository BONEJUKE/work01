# 진행 상황 점검 보고서

## README 기준 현재 요약
- README에는 도메인 모델, 애그리게이터, 알림 오케스트레이션이 구현되었고 저장소/알림 계층은 목(mock) 상태이며 Compose UI 작업이 남아 있다고 적혀 있습니다.【F:README.md†L62-L102】
- 저장소 코드도 이를 확인해 줍니다. 도메인 엔터티와 알림 로직은 `app/src/main/kotlin/com/example/calendar/data` 및 `.../reminder` 아래에 정리되어 있고, `QuickStartAppContainer`는 여전히 인메모리 저장소와 `NoOpReminderScheduler`에 연결되어 있습니다.【F:app/src/main/kotlin/com/example/calendar/data/Task.kt†L14-L35】【F:app/src/main/kotlin/com/example/calendar/reminder/ReminderOrchestrator.kt†L10-L66】【F:app/src/main/kotlin/com/example/calendar/QuickStartAppContainer.kt†L31-L96】
- 다만 README의 설명과 달리, 현재 레포에는 `AgendaRoute`/`AgendaScreen`을 포함한 Compose UI 뼈대가 존재하므로 “UI 미구현”이라는 문구는 최신 상태가 아닙니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L335】

## `docs/next-steps.md` 체크리스트 진행도
1. **UI 뼈대 구성 (Step 1)** – 완료. `AgendaRoute`가 `AgendaViewModel`과 상태를 연동하고, 탭/기간 상태를 관리하며 `AgendaAggregator`에서 가져온 일/주/월 콘텐츠를 렌더링합니다.【F:docs/next-steps.md†L10-L18】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L210】【F:app/src/main/kotlin/com/example/calendar/scheduler/AgendaAggregator.kt†L1-L53】
2. **상호작용 추가 (Step 2)** – 부분 충족. 상세 정보 바텀시트에서 토글·삭제·편집 훅을 제공하고, 작업 행은 접근성 라벨을 포함한 스와이프 완료 제스처를 지원합니다.【F:docs/next-steps.md†L15-L18】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L212-L239】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L734-L909】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1017-L1093】
3. **입력 및 저장 (Step 3)** – **미완료.** 스캐폴드에는 `onQuickAddClick` 슬롯이 노출되어 있지만 `AgendaRoute`에서 실제 핸들러를 전달하지 않고, `QuickAddType` 열거형도 정의되지 않아 FAB으로 항목을 추가할 수 없으며 `SnackbarHostState` 등의 필수 파라미터가 비어 있어 현재 코드는 컴파일되지 않습니다.【F:docs/next-steps.md†L19-L21】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L175-L210】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L271-L351】
4. **알림 연결 준비 (Step 4)** – 진행 전. 오케스트레이터는 여전히 `NoOpReminderScheduler`에 위임하고 있으며 런타임 권한 확인을 위한 주석이나 흐름도 준비되어 있지 않습니다.【F:docs/next-steps.md†L22-L24】【F:app/src/main/kotlin/com/example/calendar/QuickStartAppContainer.kt†L90-L96】
5. **품질 보강 (Step 5)** – 부분 진행. ViewModel/도메인 테스트(`AgendaViewModelTest`, `AgendaAggregatorTest`, `ReminderOrchestratorTest`)는 존재하지만 Compose 프리뷰나 UI 테스트는 아직 마련되지 않았습니다.【F:docs/next-steps.md†L25-L27】【F:app/src/test/kotlin/com/example/calendar/ui/AgendaViewModelTest.kt†L33-L153】【F:app/src/test/kotlin/com/example/calendar/scheduler/AgendaAggregatorTest.kt†L1-L154】

## 추가 관찰
- `SwipeableTaskRow`, `AgendaDetailSheet` 등 주요 컴포저블이 단일 정의로 정리돼 머지 충돌 잔재는 해소되었습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L734-L1093】
- 다만 `AgendaScreen`에 필요한 `SnackbarHostState`와 퀵 추가 핸들러가 비어 있어 기본 화면도 열리지 않으므로, Step 3 구현과 함께 컴파일 상태부터 회복하는 작업이 시급합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L175-L351】
