# 워크01 캘린더 앱

워크01 캘린더 앱은 일정, 리마인더, 할 일 관리를 하나의 Compose 기반 경험으로 통합하는 Kotlin 우선 Android 프로젝트입니다.

## 프로젝트 하이라이트
- **Compose 우선 아젠다 셸** – `CalendarApp`과 `AgendaRoute`가 탭·리스트·바텀시트를 포함한 전체 Agenda UI 골격을 제공합니다.
- **리마인더 오케스트레이션** – `ReminderOrchestrator`, `AndroidReminderScheduler`, `SharedPreferencesReminderStore`가 AlarmManager·WorkManager 기반 알림을 예약하고 재부팅 후에도 복원합니다.
- **빠른 추가 플로우** – Floating Action Button으로 일정/할 일을 최소 입력으로 즉시 등록할 수 있습니다.
- **Room 기반 컨테이너** – `RoomAppContainer`가 `CalendarDatabase`와 Room 리포지토리를 빌드하고, `ReminderStoreSynchronizer`로 SharedPreferences 리마인더 저장소와 동기화합니다.

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
3. 기본 `RoomAppContainer`가 Room DB와 알림 스케줄러/SharedPreferences 저장소를 빌드하므로, 필요 시 앱 환경에 맞춰 마이그레이션 전략이나 백업 정책만 조정하면 됩니다.
4. `docs/ux-flows.md`를 참고해 내비게이션 및 추가 화면을 구성합니다.

## 품질 및 테스트 현황
- `AgendaScreen`의 로딩·빈 상태·오류 UI에 접근성 안내 및 라이브 리전을 추가했습니다.
- 기본 Compose UI 계측 테스트(`AgendaScreenTest`)가 FAB/리스트 표시 여부를 검증합니다.
- Aggregator, Reminder, ViewModel 단위 테스트가 주·월 집계와 리마인더 재동기화 시나리오까지 커버하도록 확장되었습니다.
- 새로운 계측 테스트(`CalendarAppNotificationTest`, `QuickAddFlowTest`)가 알림 권한 안내 카드와 빠른 추가 플로우 회귀를 막습니다.
- GitHub Actions 워크플로(`.github/workflows/ci.yml`)가 래퍼 검증과 테스트 실행을 자동화합니다.

## 현재 상태 진단
### 진행 상황 요약
| 영역 | 진행률 |
| --- | --- |
| 기능 구현 | <progress value="85" max="100"></progress> 85% |
| 단위·통합 테스트 | <progress value="65" max="100"></progress> 65% |
| 실기기 검증 및 배포 준비 | <progress value="40" max="100"></progress> 40% |

### 완료됨
- Compose Agenda 화면이 일·주·월 탭, 상세 바텀 시트, 스와이프 제스처, 빠른 추가, 알림 권한 안내까지 포함해 구현되었습니다.
- `AgendaViewModel`이 애그리게이터·리포지토리와 연결되어 일정/할 일 토글·삭제·빠른 추가 시 리마인더 스케줄링까지 처리합니다.
- `ReminderOrchestrator`와 `AndroidReminderScheduler`가 AlarmManager·WorkManager·워커/리시버 연동으로 알림 예약 파이프라인을 구성했습니다.
- `SharedPreferencesReminderStore`를 통해 예약된 리마인더를 저장·복원해 앱 재시작이나 기기 재부팅 이후에도 스케줄이 유지됩니다.
- `RoomAppContainer`가 Room 데이터베이스 및 리포지토리를 앱 컨테이너에 연결해 프로세스 재시작 후에도 데이터가 유지됩니다.
- `ReminderStoreSynchronizer`가 SharedPreferences 리마인더 저장소를 Room 데이터와 자동 동기화해 다중 기기/계정 시나리오를 대비합니다.
- 단위/계측 테스트로 애그리게이터, 뷰모델, Compose Agenda 화면 핵심 시나리오를 검증합니다.
- 주·월 뷰 회귀 단위 테스트와 리마인더 재동기화 회귀 테스트를 추가해 재부팅·기간 변경 시나리오를 모의 검증합니다.
- Gradle 래퍼와 GitHub Actions CI 워크플로가 포함되어 일관된 빌드 환경을 제공합니다.

### 부분 완료
- 다중 기기/백그라운드 동기화를 대비한 리마인더 재동기화 흐름은 구현됐지만, 실제 단말의 재부팅·네트워크 전환 시나리오 검증이 필요합니다.

### 미구현
- WorkManager/AlarmManager 기반 알림이 실제 기기/에뮬레이터에서 예상대로 동작하는지에 대한 통합 테스트.
- 클라우드 동기화나 외부 캘린더 가져오기 같은 대량 데이터 시나리오에 대한 성능/안정성 검증.

### 남은 과제
1. 실제 단말에서 알림 권한/스케줄링 플로우를 검증하는 E2E 테스트 시나리오를 작성합니다.

## 우선순위 로드맵
| 우선순위 | 작업 | 상태 | 메모 |
| --- | --- | --- | --- |
| P0 | Compose 일정 화면 세부 다듬기 | ✅ | 탭·빈 상태·리스트 상호작용 및 접근성 폴리시까지 정비 |
| P0 | 일정·할 일 상세 시트 | ✅ | 토글·삭제·수정 흐름 연결 완료 |
| P0 | 새 항목 빠른 추가 | ✅ | FAB 시트로 Task/Event 빠른 등록 지원 |
| P1 | 알림 연동 준비 | ✅ | AndroidReminderScheduler ↔ AlarmManager·WorkManager, 권한 안내 연결 |
| P1 | 테스트·프리뷰 추가 | ✅ | Agenda Compose Preview + UI 스모크 테스트 확보 |
| P2 | 도메인/뷰모델 회귀 테스트 확대 | 🚧 | 주·월 집계 단위 테스트 확보, 실제 단말 E2E 보강 예정 |

## 문서 모음
- `docs/requirements.md` – 제품 요구사항과 페르소나.
- `docs/architecture.md` – 아키텍처 결정과 모듈 책임.
- `docs/ux-flows.md` – Compose 내비게이션과 사용자 플로우.
- `docs/status-overview.md`, `docs/progress-audit.md` – 과거 진행 상황 기록.
- `docs/next-steps.md` – 세부 체크리스트 및 향후 계획.
- `docs/sync-conflict-strategy.md` – 서버/외부 캘린더 동기화 충돌 해결 정책과 테스트 전략.

## 권한
Android 13 이상에서 `POST_NOTIFICATIONS` 런타임 권한을 요청하며, 정확한 알림을 위해 `SCHEDULE_EXACT_ALARM` 권한을 선언합니다.
