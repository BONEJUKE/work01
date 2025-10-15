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
2. 필요 시 `gradle wrapper --gradle-version 8.5` 명령으로 Gradle 래퍼를 생성합니다.
3. Room 또는 다른 영속성 계층과 AlarmManager/WorkManager 권한 플로우를 실제 앱 환경에 맞게 연결합니다.
4. `docs/ux-flows.md`를 참고해 내비게이션 및 추가 화면을 구성합니다.

## 품질 및 테스트 현황
- `AgendaScreen`에 대표 상태(로딩, 빈 목록, 데이터 로드)를 시각화하는 Compose Preview가 추가되었습니다.
- 기본 Compose UI 계측 테스트(`AgendaScreenTest`)가 FAB/리스트 표시 여부를 검증합니다.
- Aggregator, Reminder, ViewModel 단위 테스트가 포함되어 있으며, 도메인 회귀 보강 테스트는 추가 작업으로 남아 있습니다.

## 현재 상태 진단
### 완료됨
- 도메인·데이터 레이어 및 Room 연동, 리마인더 오케스트레이션, 빠른 추가 플로우.
- Compose Agenda 셸과 상호작용(스와이프 완료, 상세 시트, 상태 토글).
- Compose Preview와 기본 UI 테스트를 통해 화면 회귀 점검의 출발점을 마련했습니다.

### 부분 완료
- 접근성 및 세부 UI 폴리시(특히 빈 상태·탭 포커스 경험) 추가 다듬기.
- ViewModel/도메인 추가 단위 테스트로 일·주·월 집계 정확성 보강 필요.

### 미구현
- Gradle 래퍼 및 CI 파이프라인.
- Room 기반 영속 데이터 저장 및 알림 영속화 확장 작업.

## 우선순위 로드맵
| 우선순위 | 작업 | 상태 | 메모 |
| --- | --- | --- | --- |
| P0 | Compose 일정 화면 세부 다듬기 | ✅ | 탭·빈 상태·리스트 상호작용 골격 완성, 추가 UX 폴리시 진행 중 |
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
