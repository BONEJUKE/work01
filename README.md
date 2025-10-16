# Work01 Calendar App (워크01 캘린더 앱)

워크01 캘린더 앱은 일정, 리마인더, 할 일 관리를 하나의 Compose 기반 경험으로 통합하는 Kotlin 우선 Android 프로젝트입니다. (An Android calendar planner that unifies schedules, reminders, and to-dos.)

## 프로젝트 하이라이트
- **Compose 우선 아젠다 셸** – `CalendarApp`과 `AgendaRoute`가 탭·리스트·바텀시트를 포함한 전체 Agenda UI 골격을 제공합니다. (Compose-first agenda shell ready for navigation wiring.)
- **리마인더 오케스트레이션** – `ReminderOrchestrator`와 `AndroidReminderScheduler`가 AlarmManager·WorkManager 기반 알림을 예약합니다. (AlarmManager/WorkManager-backed reminders.)
- **빠른 추가 플로우** – Floating Action Button으로 일정/할 일을 최소 입력으로 즉시 등록할 수 있습니다. (Quick add FAB flow for tasks and events.)

## 주요 기능
- 일정, 할 일, 리마인더 도메인 모델과 리포지토리가 구축되어 있습니다.
- `AgendaAggregator`가 Day/Week/Month 뷰를 위한 데이터를 집계하고 `AgendaViewModel`이 상태를 노출합니다.
- 스와이프 완료, 상세 시트, 상태 토글 등 상호작용이 Compose로 구현되어 있습니다.
- Compose Preview와 기본 Compose UI 테스트가 추가되어 주요 뷰 회귀를 빠르게 검증할 수 있습니다.

## 저장소 구조
- `app/src/main/kotlin/` – 도메인 모델, 애그리게이터, 리포지토리, 알림 오케스트레이션, Compose UI.
- `app/src/main/res/` – Material 3 테마 및 기본 리소스.
- `docs/` – 요구사항, 아키텍처, UX 플로우, 진행 상황 문서.

## 시작 방법
1. 이 레포지토리를 Android Studio에서 열거나 `app/src/main/kotlin`을 기존 프로젝트로 복사합니다.
2. 레포지토리에 포함된 Gradle 래퍼(`./gradlew`)로 빌드·테스트를 실행합니다. 일부 코드 호스팅에서 바이너리 업로드가 제한되는
   관계로 래퍼 JAR은 `gradle/wrapper/gradle-wrapper.jar.base64`에 Base64 텍스트로 보관되며, `./gradlew`와 `gradlew.bat`가 최초 실행
   시 자동으로 복원합니다.
3. Room 또는 다른 영속성 계층과 AlarmManager/WorkManager 권한 플로우를 실제 앱 환경에 맞게 연결합니다.
4. `docs/ux-flows.md`를 참고해 내비게이션 및 추가 화면을 구성합니다.

## 품질 및 테스트 현황
- `AgendaScreen`의 로딩·빈 상태·오류 UI에 접근성 안내 및 라이브 리전을 추가했습니다.
- 기본 Compose UI 계측 테스트(`AgendaScreenTest`)가 FAB/리스트 표시 여부를 검증합니다.
- Aggregator, Reminder, ViewModel 단위 테스트가 포함되어 있으며, 도메인 회귀 보강 테스트는 추가 작업으로 남아 있습니다.
- GitHub Actions 워크플로(`.github/workflows/ci.yml`)가 래퍼 검증과 테스트 실행을 자동화합니다.

## 현재 상태 진단
### 완료됨
- Compose Agenda 화면이 일·주·월 탭, 상세 바텀 시트, 스와이프 제스처, 빠른 추가, 알림 권한 안내까지 포함해 구현되었습니다.
- `AgendaViewModel`이 애그리게이터·리포지토리와 연결되어 일정/할 일 토글·삭제·빠른 추가 시 리마인더 스케줄링까지 처리합니다.
- `ReminderOrchestrator`와 `AndroidReminderScheduler`가 AlarmManager·WorkManager·워커/리시버 연동으로 알림 예약 파이프라인을 구성했습니다.
- Room 기반 `RoomAppContainer`가 `CalendarDatabase`를 빌드해 `RoomTaskRepository`와 `RoomEventRepository`를 주입하므로 앱 재시작 후에도 데이터가 유지됩니다.
- 단위/계측 테스트로 애그리게이터, 뷰모델, Compose Agenda 화면 핵심 시나리오를 검증합니다.
- Gradle 래퍼와 GitHub Actions CI 워크플로가 포함되어 일관된 빌드 환경을 제공합니다.

### 부분 완료
- 리마인더 예약은 가능하지만 Task/Event의 `reminders` 정보가 인메모리 샘플과 함께만 유지되어 앱 재시작 시 복원 로직이 부족합니다.

### 미구현
- Room DB를 실제 앱 컨테이너에 연결해 프로세스/앱 재시작 간에도 일정·할 일을 보존해야 합니다.
- 예약된 리마인더 메타데이터를 저장·복구하고 기기 재부팅 이후 알람을 재스케줄링하는 흐름이 필요합니다.
- WorkManager/AlarmManager 연동을 계측 또는 통합 테스트로 검증하는 자동화가 아직 없습니다.

### 남은 과제
1. 리마인더 저장/복원 전략을 추가해 `ReminderOrchestrator` 스케줄이 기기 재부팅이나 앱 재시작 후에도 유지되도록 합니다.
2. 알림 권한 안내·빠른 추가 플로우에 대한 계측/통합 테스트를 보강해 회귀를 방지합니다.

## 우선순위 로드맵
| 우선순위 | 작업 | 상태 | 메모 |
| --- | --- | --- | --- |
| P0 | Compose 일정 화면 세부 다듬기 | ✅ | 탭·빈 상태·리스트 상호작용 및 접근성 폴리시까지 정비 |
| P0 | 일정·할 일 상세 시트 | ✅ | 토글·삭제·수정 흐름 연결 완료 |
| P0 | 새 항목 빠른 추가 | ✅ | FAB 시트로 Task/Event 빠른 등록 지원 |
| P1 | 알림 연동 준비 | ✅ | AndroidReminderScheduler ↔ AlarmManager·WorkManager, 권한 안내 연결 |
| P1 | 테스트·프리뷰 추가 | ✅ | Agenda Compose Preview + UI 스모크 테스트 확보 |
| P2 | 도메인/뷰모델 회귀 테스트 확대 | 🚧 | 주·월 집계 검증용 단위 테스트가 필요 |

## 문서 모음
- `docs/requirements.md` – 제품 요구사항과 페르소나.
- `docs/architecture.md` – 아키텍처 결정과 모듈 책임.
- `docs/ux-flows.md` – Compose 내비게이션과 사용자 플로우.
- `docs/status-overview.md`, `docs/progress-audit.md` – 과거 진행 상황 기록.
- `docs/next-steps.md` – 세부 체크리스트 및 향후 계획.

## 권한
Android 13 이상에서 `POST_NOTIFICATIONS` 런타임 권한을 요청하며, 정확한 알림을 위해 `SCHEDULE_EXACT_ALARM` 권한을 선언합니다. (Requires `POST_NOTIFICATIONS` runtime permission on Android 13+ and declares `SCHEDULE_EXACT_ALARM`.)
