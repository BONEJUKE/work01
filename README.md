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

## 남은 작업 및 다음 단계

### 이번 작업 계획
| 우선순위 | 작업 | 목표 | 상태 |
| --- | --- | --- | --- |
| P0 | 재부팅 후 리마인더 자동 복원 | BOOT_COMPLETED 시 SharedPreferences에 저장된 리마인더를 AlarmManager/WorkManager에 재등록해 알림 신뢰성을 확보 | ✅ 완료 |
| P1 | 권한 안내 재노출 정책 정비 | 사용자가 권한을 거부한 뒤에도 적절한 시점에 안내 카드를 다시 노출해 전환율 개선 | 대기 |
| P1 | 알림 빠른 액션 추가 | 스누즈/완료 액션을 추가해 앱 진입 없이 작업 처리 지원 | 대기 |

### 미해결 핵심 과제 요약
다음 표는 현재 미해결 상태인 핵심 과제를 요약합니다.

| 영역 | 미완료 항목 | 영향 | 즉시 조치 |
| --- | --- | --- | --- |
| 기능 | 반복 일정 전개, 필터링, 충돌 강조 | 반복/필터 요구사항을 충족하지 못해 사용자 신뢰 저하 | 집계 단계에서 인스턴스 확장, 필터 상태 모델링 |
| 알림 | 빠른 액션 부재 | 알림 상호작용·신뢰성 저하 | Notification 액션 추가, BOOT 처리 도입 |
| UX/권한 | 권한 카드 재노출 부재 | 권한 획득 전환율 하락 | 거절 후 재노출 로직과 타이밍 정의 |
| 품질 | 현지화 미흡, 통합 테스트 부재 | 출시 품질·접근성 미달 | 문자열 리소스화, E2E/통합 시나리오 설계 |

### 제품 요구사항 미충족 항목

#### 반복 일정 인스턴스 확장 미구현
- **문제**: `AgendaAggregator`는 저장소에서 받은 일정 컬렉션을 그대로 합쳐 노출하며, 반복 규칙을 풀어 개별 인스턴스로 변환하지 않습니다.
- **영향**: 반복 일정이 UI에 단 한 번만 등장해 요구사항의 반복 일정 지원 항목을 충족하지 못합니다.
- **다음 조치**: 반복 규칙 파서를 추가하고, 선택된 기간에 맞춰 인스턴스를 생성하도록 `AgendaAggregator`와 저장소 계층을 확장합니다.

#### 완료/반복 필터 UI 미구현
- **문제**: `AgendaScreen`은 완료 여부나 반복 유형을 필터링할 상태·컨트롤을 제공하지 않습니다.
- **영향**: 사용자 요구사항의 필터 기능을 제공하지 못해 긴 목록에서 작업을 추적하기 어렵습니다.
- **다음 조치**: 뷰모델에 필터 상태를 도입하고 토글/드롭다운 UI를 구성해 목록 쿼리를 조건부로 갱신합니다.

#### 이벤트 충돌 강조 미구현
- **문제**: 집계 모델에 일정 겹침 여부를 계산하거나 전달하는 필드가 없습니다.
- **영향**: 충돌 하이라이트 요구사항이 미충족되어 사용자가 겹치는 일정을 인지하지 못합니다.
- **다음 조치**: 스냅샷 생성 시 겹치는 시간대를 계산해 UI에 색상/배지 정보를 전달합니다.

#### 알림 빠른 액션 미구현
- **문제**: `ReminderNotificationWorker`는 제목과 본문만 포함한 기본 알림을 발행하며, 스누즈·완료 같은 액션 버튼을 추가하지 않습니다.
- **영향**: 알림 요구사항의 빠른 액션 제공이 누락돼 사용자가 앱 진입 없이 작업을 처리할 수 없습니다.
- **다음 조치**: `PendingIntent` 기반 액션을 추가하고, 스누즈·완료 처리를 위한 워커/리시버를 연결합니다.

### 플랫폼 통합 및 UX 개선 필요

#### 재부팅 복원 흐름 보완 (완료)
- **문제**: `AndroidManifest.xml`에 `BOOT_COMPLETED` 브로드캐스트를 수신하는 컴포넌트가 없어 기기 재부팅 후 예약된 알림이 복원되지 않았습니다.
- **조치**: `ReminderBootReceiver`가 `BOOT_COMPLETED` 및 `LOCKED_BOOT_COMPLETED` 이벤트를 수신해 `ReminderOrchestrator`로 SharedPreferences에 남아 있는 스케줄을 즉시 재등록하도록 구성했습니다.
- **결과**: 재부팅 이후에도 리마인더가 자동으로 복원돼 알림 신뢰성이 개선됐습니다.

#### 권한 안내 재노출 부재
- **문제**: `CalendarApp`은 `shouldShowPrompt`가 `false`로 평가되면 `dismissed` 플래그를 재설정하지 않아, 한 번 닫힌 권한 안내 카드가 영구적으로 숨겨집니다.
- **영향**: 사용자 권한 전환율이 떨어지고, 재설치 없이 알림 기능을 사용할 수 없습니다.
- **다음 조치**: 거절 후 일정 시간/세션 경과 시 재노출하는 정책을 도입하고, 권한 상태 옵저버를 통해 UI를 업데이트합니다.

### 품질 및 현지화 TODO

#### 문자열 리소스 정비
- **문제**: 문자열 리소스 파일이 앱 이름만 포함하고 대부분의 UI 문구가 하드코딩돼 있습니다.
- **영향**: 현지화 및 접근성 요구사항을 충족하지 못합니다.
- **다음 조치**: 주요 Compose 텍스트를 문자열 리소스로 추출하고 번역 키를 준비합니다.

#### 통합/E2E 테스트 부재
- **문제**: 알림과 일정 흐름을 통합 검증하는 테스트 케이스가 정의돼 있지 않습니다.
- **영향**: 비즈니스 크리티컬 시나리오(알림 예약·발송)가 회귀 테스트 없이 배포됩니다.
- **다음 조치**: WorkManager·AlarmManager 플로우를 계측 테스트로 검증하고, UI 테스트에 알림 권한 흐름을 포함합니다.

### 실행 체크리스트(완료 현황)
프로젝트를 세팅하면서 수행한 핵심 작업을 체크리스트로 정리했습니다.

1. **UI 뼈대 구성**
   - `CalendarApp`과 `RoomAppContainer`를 연결해 Room 데이터가 기본값으로 로드되도록 구성했습니다.
   - Day/Week/Month Compose 화면과 `AgendaViewModel` 상태 구독을 구현했습니다.
   - 일정 카드와 할 일 행에 상태/시간 정보를 표시했습니다.
2. **상호작용 추가**
   - 리스트 항목을 탭하면 상세 바텀 시트가 열리고, 상태 토글 버튼과 닫기 버튼을 제공합니다.
   - 바텀 시트에서 삭제/수정 작업을 연결하고, 스와이프 제스처로 빠른 완료를 지원합니다.
   - Week/Month 뷰에서 날짜를 선택하면 Day 뷰 포커스를 연동합니다.
3. **입력 및 저장**
   - 빠른 추가 FAB를 통해 최소 입력값만으로 이벤트/할 일을 생성합니다.
   - 저장 성공/실패를 스낵바와 인라인 검증으로 안내합니다.
4. **알림 연결 준비**
   - `ReminderOrchestrator`에 `AndroidReminderScheduler`를 연결해 AlarmManager/WorkManager와 연동합니다.
   - 알림 권한 확인/요청 흐름을 Compose에서 안내합니다.
5. **품질 보강**
   - Compose Preview와 UI 테스트를 추가해 회귀를 막습니다.
   - 알림 권한 안내 및 빠른 추가 계측 테스트로 핵심 플로우 회귀를 보강했습니다.
   - ViewModel/도메인 단위 테스트로 Day/Week/Month 집계를 검증합니다.
   - `AgendaAggregatorTest`, `AgendaViewModelTest`, `ReminderOrchestratorTest`로 주·월 집계와 재동기화 흐름을 회귀 방지합니다.

### 이후 확장 아이디어
- 재부팅·동기화 시나리오에서 리마인더가 예상대로 재스케줄되는지 통합 테스트를 추가합니다.
- 실제 단말 기준 알림 권한 흐름과 스케줄링을 검증하는 E2E 시나리오를 구체화합니다.
- 홈 화면 위젯과 다크 모드 대응을 준비합니다.

### 협업 메모
- UI 작업자는 `ui/` 패키지를 중심으로 작업하고, 저장소/알림 변경이 필요할 경우 먼저 인터페이스를 점검한 뒤 최소 변경으로 진행하세요.
- PR 작성 시 반드시 실행 영상 또는 스크린샷을 첨부해 이해관계자에게 진행 상황을 빠르게 공유합니다.
- 단위/통합 테스트와 문서 업데이트 상태는 README의 "남은 작업 및 다음 단계" 섹션에 즉시 반영합니다.

## 문서 모음
- `docs/requirements.md` – 제품 요구사항과 페르소나.
- `docs/architecture.md` – 아키텍처 결정과 모듈 책임.
- `docs/ux-flows.md` – Compose 내비게이션과 사용자 플로우.
- `docs/status-overview.md`, `docs/progress-audit.md` – 과거 진행 상황 기록.
- `docs/sync-conflict-strategy.md` – 서버/외부 캘린더 동기화 충돌 해결 정책과 테스트 전략.

## 권한
Android 13 이상에서 `POST_NOTIFICATIONS` 런타임 권한을 요청하며, 정확한 알림을 위해 `SCHEDULE_EXACT_ALARM` 권한을 선언합니다.
