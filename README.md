# 워크01 캘린더 앱

워크01 캘린더는 Jetpack Compose로 구현된 일정·할 일 통합 캘린더 실험 프로젝트입니다. Room 기반 저장소와 리마인더 오케스트레이션을 갖추고 있어, 단일 Agenda 화면에서 일/주/월 일정 확인부터 빠른 추가, 알림 처리까지 한 번에 수행할 수 있습니다.

## 구현된 기능 요약

### 1. Agenda 경험
- `AgendaAggregator`가 기간별(일/주/월) 일정과 할 일을 통합해 반복 일정 전개, 예외 처리, 겹침 탐지를 수행합니다.【F:app/src/main/kotlin/com/example/calendar/scheduler/AgendaAggregator.kt†L16-L140】
- Compose 기반 `AgendaRoute`/`AgendaScreen`이 탭 전환, 기간 이동, 상세 바텀 시트, 스와이프 삭제, 완료 상태/반복 항목 필터를 제공해 주요 상호작용을 모두 지원합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L82-L1180】
- FAB에서 열리는 빠른 추가 시트가 최소 입력으로 새 할 일·일정을 생성하고, 성공/실패 스낵바와 상세 보기 연결까지 처리합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L232-L377】【F:app/src/main/kotlin/com/example/calendar/ui/AgendaViewModel.kt†L57-L152】

### 2. 리마인더 시스템
- `RoomAppContainer`가 AlarmManager·WorkManager·SharedPreferences를 묶어 `ReminderOrchestrator`를 구성하고, 앱 전체에서 동일한 스케줄러 인스턴스를 사용합니다.【F:app/src/main/kotlin/com/example/calendar/RoomAppContainer.kt†L17-L75】
- `ReminderNotificationWorker`와 `ReminderActionReceiver`가 완료/스누즈 빠른 액션, 딥링크, 스누즈 재예약을 처리해 알림 상호작용을 완성합니다.【F:app/src/main/kotlin/com/example/calendar/reminder/ReminderNotificationWorker.kt†L21-L129】【F:app/src/main/kotlin/com/example/calendar/reminder/ReminderActionReceiver.kt†L14-L88】
- 기기 재부팅 시 `ReminderBootReceiver`가 저장된 리마인더를 복구해 알림 신뢰성을 유지합니다.【F:app/src/main/kotlin/com/example/calendar/reminder/ReminderBootReceiver.kt†L9-L71】

### 3. 데이터 계층 및 앱 구성
- `CalendarApplication`이 `RoomAppContainer`를 초기화해 Room DAO 기반 저장소와 리마인더 오케스트레이터를 주입합니다.【F:app/src/main/kotlin/com/example/calendar/CalendarApplication.kt†L9-L20】【F:app/src/main/kotlin/com/example/calendar/RoomAppContainer.kt†L28-L75】
- Room 엔티티/DAO가 일정·할 일·리마인더 데이터를 보존하고 스트림으로 제공해 UI가 즉시 갱신됩니다.【F:app/src/main/kotlin/com/example/calendar/data/CalendarEvent.kt†L8-L54】【F:app/src/main/kotlin/com/example/calendar/data/Task.kt†L8-L86】【F:app/src/main/kotlin/com/example/calendar/data/CalendarDatabase.kt†L8-L32】
- 메인 진입점은 `MainActivity`의 Compose `CalendarApp`으로, 알림 권한 카드와 Agenda 화면을 결합합니다.【F:app/src/main/kotlin/com/example/calendar/MainActivity.kt†L7-L25】【F:app/src/main/kotlin/com/example/calendar/ui/CalendarApp.kt†L18-L88】

### 4. 테스트 커버리지
- 도메인/리마인더 단위 테스트가 반복 일정 전개, 충돌 탐지, 알림 재예약을 검증합니다.【F:app/src/test/kotlin/com/example/calendar/scheduler/AgendaAggregatorTest.kt†L17-L124】【F:app/src/test/kotlin/com/example/calendar/reminder/ReminderReschedulerTest.kt†L13-L119】
- `AgendaViewModelTest`가 빠른 추가·필터링·삭제 로직을 확인합니다.【F:app/src/test/kotlin/com/example/calendar/ui/AgendaViewModelTest.kt†L33-L207】
- Compose 계측 테스트가 빠른 추가 흐름과 알림 권한 카드 UI를 검증합니다.【F:app/src/androidTest/kotlin/com/example/calendar/ui/QuickAddFlowTest.kt†L8-L55】【F:app/src/androidTest/kotlin/com/example/calendar/ui/CalendarAppNotificationTest.kt†L8-L46】

## 남은 과제
1. **편집 화면/내비게이션 연결** – 상세 시트의 `편집` 버튼이 `onTaskEdit`/`onEventEdit` 콜백만 호출하며 기본 구현이 없어 실제 편집 화면으로 연결되지 않습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1404-L1477】
2. **리마인더 구성 UI** – 빠른 추가 시트가 제목/시간만 입력받고 리마인더 오프셋·스누즈 설정을 노출하지 않아, 요구사항에 있는 사용자 지정 리마인더 구성이 불가합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1479-L1662】
3. **현지화 리소스 확장** – 대부분의 UI 문구가 Compose 내부에 하드코딩되어 있어 문자열 리소스화와 다국어 지원이 필요합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L707-L1165】【F:app/src/main/res/values/strings.xml†L1-L8】
4. **동기화·백엔드 연동** – `docs/sync-conflict-strategy.md`에 정의된 클라우드 동기화 정책은 설계만 존재하고 구현되지 않았습니다.【F:docs/sync-conflict-strategy.md†L1-L33】
5. **접근성/시각 구조 정리** – Agenda 화면에 중복된 카드/리스트 컴포저블이 남아 있어 구조 단순화와 스크린 리더 QA가 필요합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L807-L1406】

## 실행 및 테스트 방법
1. **의존성 설치**: Android Studio에서 Gradle 동기화를 수행하거나 CLI에서 `./gradlew tasks`로 래퍼 동작을 확인합니다.
2. **단위 테스트**: `./gradlew test`를 실행해 도메인·리마인더 로직을 검증합니다.
3. **계측 테스트**: 에뮬레이터/디바이스에서 `./gradlew connectedAndroidTest`를 실행해 Compose UI 흐름을 확인합니다.
4. **알림 검증**: 알림 권한을 허용한 실제 기기에서 빠른 추가 후 리마인더가 스케줄되고 액션이 동작하는지 수동 테스트합니다.

## 참고 문서
- 전체 설계 개요: [`docs/architecture.md`](docs/architecture.md)
- 남은 작업 세부 목록: [`docs/remaining-work.md`](docs/remaining-work.md)
- UX 흐름 및 화면 정의: [`docs/ux-flows.md`](docs/ux-flows.md)
