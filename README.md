codex/add-calendar-app-with-scheduler-and-alarm
# Work01 Calendar App (워크01 캘린더 앱)

A Kotlin-first Android calendar planner that unifies scheduling, reminders, and to-do tracking. The repository ships with a fully documented domain layer prototype, Compose agenda scaffolding, and guidance for finishing the production-ready experience.

코틀린 기반으로 일정, 알림, 할 일 추적을 통합한 안드로이드 캘린더 플래너입니다. 이 저장소에는 문서화된 도메인 레이어 프로토타입과 Compose 아젠다 스캐폴드, 출시를 위한 마무리 가이드가 포함되어 있습니다.

## Core capabilities (주요 기능)
- **Scheduler & task lists** – Create and review day/week/month plans with completion tracking.
  **스케줄러와 작업 목록** – 일·주·월 단위 계획을 생성하고 완료 상태를 추적합니다.
- **Reminder orchestration** – Configure alarms, lead times, and recurring cadence from the same workflow.
  **알림 오케스트레이션** – 동일한 플로우에서 알람, 리드타임, 반복 주기를 설정합니다.
- **Compose-first UI entry point** – `CalendarApp` boots a Compose shell that is ready to wire to navigation and storage.
  **Compose 중심 UI 진입점** – `CalendarApp`이 내비게이션과 저장소를 연결하기 쉬운 Compose 셸을 제공합니다.

## Repository layout (저장소 구성)
- `app/src/main/kotlin/` – Domain models, aggregators, repositories, reminder orchestration, and Compose agenda screens.
  `app/src/main/kotlin/` – 도메인 모델, 애그리게이터, 리포지토리, 알림 오케스트레이션, Compose 아젠다 화면.
- `docs/` – Requirements, architecture decisions, UX journey maps, progress audits, and roadmap.
  `docs/` – 요구사항, 아키텍처 결정, UX 여정 지도, 진행 상황 점검, 로드맵 문서.

## Getting started (시작하기)
These files are intended as a foundation for a full Android Studio project.

이 레포지토리는 안드로이드 스튜디오 프로젝트를 부트스트랩하기 위한 기반입니다.

1. Copy `app/src/main/kotlin` into your project or open this repository directly in Android Studio.
   `app/src/main/kotlin` 패키지를 프로젝트에 복사하거나 이 레포를 Android Studio에서 직접 엽니다.
2. Wire domain classes to persistence (Room, etc.) and Android alarm APIs.
   도메인 클래스를 영속성 계층(Room 등)과 안드로이드 알람 API에 연결합니다.
3. Implement Compose navigation and surfaces guided by `docs/ux-flows.md`.
   `docs/ux-flows.md`를 참고하여 Compose 내비게이션과 화면을 구현합니다.

### Build tooling (빌드 도구)
- The prototype does **not** ship with a Gradle wrapper; generate one via `gradle wrapper --gradle-version 8.5` (or your Studio version).
  현재 Gradle 래퍼가 없으므로 `gradle wrapper --gradle-version 8.5`(또는 사용 중인 버전)으로 생성하세요.
- Android Studio can also create/update the wrapper on first sync.
  Android Studio 첫 동기화 시 래퍼를 자동으로 생성하거나 업데이트할 수 있습니다.

## Current status diagnosis (현 상태 진단)
### What is complete (완료됨)
- **Domain & data** – `Task`, `CalendarEvent`, `Reminder` models, repositories, and Room wiring are implemented.
  **도메인 & 데이터** – `Task`, `CalendarEvent`, `Reminder` 모델과 리포지토리, Room 구성이 구현되어 있습니다.
- **Agenda aggregation** – `AgendaAggregator` groups schedules for day/week/month views and feeds `AgendaViewModel` state.
  **아젠다 집계** – `AgendaAggregator`가 일·주·월 데이터를 묶어 `AgendaViewModel`에 제공합니다.
- **Compose shell** – `CalendarApp`/`AgendaRoute` render agenda tabs, list scaffolds, swipe-to-complete rows, and detail sheets backed by the view model.
  **Compose 셸** – `CalendarApp`과 `AgendaRoute`가 탭, 리스트 스캐폴드, 스와이프 완료 행, 상세 시트를 뷰모델과 연동합니다.
- **Unit tests** – Aggregator, reminder orchestration, and `AgendaViewModel` tests validate core scheduling logic.
  **단위 테스트** – 애그리게이터, 알림 오케스트레이터, `AgendaViewModel` 테스트로 핵심 스케줄링 로직을 검증합니다.

### Partially done (부분 완료)
- **Interactive UI polish** – Agenda detail sheet toggles and swipe actions are present, but require QA and accessibility review.
  **상호작용 다듬기** – 아젠다 상세 시트와 스와이프 제스처가 구현되어 있으나 QA/접근성 점검이 필요합니다.
- **Quality tooling** – ViewModel/unit coverage exists, yet Compose previews and UI tests are still missing.
  **품질 도구** – ViewModel/도메인 테스트는 있으나 Compose 프리뷰와 UI 테스트는 없습니다.

### Not yet implemented (미구현)
- **Quick add flow** – The FAB surface is exposed but no quick-add sheet or handlers are wired, preventing in-app creation.
  **빠른 추가 플로우** – FAB은 존재하지만 시트/핸들러가 없어 앱 내 생성이 불가합니다.
- **Real reminder scheduling** – `ReminderOrchestrator` still targets a `NoOpReminderScheduler`; Android alarm integration and permission prompts remain.
  **실제 알림 스케줄링** – `ReminderOrchestrator`가 여전히 `NoOpReminderScheduler`에 연결되어 있어 알람 통합과 권한 흐름이 남아 있습니다.
- **Gradle wrapper & CI** – The project cannot run automated builds until the Gradle wrapper and CI tasks are configured.
  **Gradle 래퍼 & CI** – Gradle 래퍼와 CI 작업을 구성해야 자동 빌드가 가능합니다.

## Priority roadmap (남은 핵심 과제)
| Priority (우선순위) | Task (작업) | Status (상태) | Notes (메모) |
| --- | --- | --- | --- |
| P0 | Compose agenda layout polish (tabs, empty states, list accessibility) / Compose 일정 화면 세부 다듬기 | ✅ Skeleton in place, needs UX polish. / 뼈대 완료, UX 다듬기 필요 |
| P0 | Agenda detail bottom sheet / 일정·할 일 상세 시트 | ✅ Opens with toggle/delete hooks; finalize flows. / 열림 및 토글/삭제 훅 존재, 플로우 마무리 필요 |
| P0 | Quick add FAB workflow / 새 항목 빠른 추가 | 🚧 Missing handlers, implement quick-add sheet. / 핸들러 미구현, 빠른 추가 시트 작성 |
| P1 | Reminder orchestration hand-off / 알림 연동 준비 | 🚧 Wire real scheduler & permissions. / 실제 스케줄러 및 권한 연동 |
| P1 | Testing & previews / 테스트·프리뷰 추가 | 🚧 Add Compose previews + UI tests. / Compose 프리뷰·UI 테스트 추가 |

### Additional backlog (추가 백로그)
- Connect WorkManager/AlarmManager for recurring reminders and exact alarms.
  WorkManager/AlarmManager를 연결해 반복/정시 알람을 구현합니다.
- Fill out Android resources (strings, themes, navigation graph) and internationalization.
  문자열, 테마, 내비게이션 그래프 및 다국어 리소스를 채워 넣습니다.
- Investigate backup/sync strategies once local persistence is stable.
  로컬 저장이 안정화되면 백업/동기화 전략을 검토합니다.

## Documentation index (문서 안내)
- `docs/requirements.md` – Product requirements and personas.
  `docs/requirements.md` – 제품 요구사항과 페르소나.
- `docs/architecture.md` – Architectural decisions and module responsibilities.
  `docs/architecture.md` – 아키텍처 결정과 모듈 책임.
- `docs/ux-flows.md` – Compose navigation, user flows, and interaction notes.
  `docs/ux-flows.md` – Compose 내비게이션과 사용자 플로우 노트.
- `docs/status-overview.md` / `docs/progress-audit.md` – Historical audits retained for traceability; this README now reflects the unified status.
  `docs/status-overview.md` / `docs/progress-audit.md` – 기록 보존용 과거 점검 문서이며, 최신 현황은 README에 통합되었습니다.
- `docs/next-steps.md` – Detailed checklist supporting the roadmap above.
  `docs/next-steps.md` – 위 로드맵을 뒷받침하는 세부 체크리스트.

## Permissions (권한)
The application requests the `POST_NOTIFICATIONS` runtime permission on Android 13+ and declares `SCHEDULE_EXACT_ALARM` for precise reminders.

이 애플리케이션은 정확한 리마인더 제공을 위해 Android 13+에서 `POST_NOTIFICATIONS` 런타임 권한을 요청하고 `SCHEDULE_EXACT_ALARM` 권한을 선언합니다.
