# 워크01 캘린더 앱

## 1. 주요 기능
- **통합 일정 관리**: 일정, 할 일, 리마인더를 하나의 Compose 기반 Agenda 화면에서 조회·편집합니다.
- **빠른 추가 플로우**: FAB를 통해 최소 입력만으로 새 이벤트나 할 일을 바로 생성합니다.
- **알림 오케스트레이션**: `ReminderOrchestrator`가 AlarmManager와 WorkManager를 활용해 알림을 예약하고 재부팅 이후에도 복원합니다.
- **Room 기반 데이터 보존**: `RoomAppContainer`가 Room 데이터베이스와 리포지토리를 초기화해 프로세스가 재시작돼도 데이터를 유지합니다.

## 2. 현재 진행도
- Compose Agenda UI(일·주·월 탭, 상세 바텀 시트, 스와이프 액션)가 구현돼 기본 사용 시나리오를 처리합니다.
- `AgendaViewModel`이 일정/할 일 토글, 삭제, 빠른 추가, 알림 예약을 연결해 핵심 비즈니스 로직을 담당합니다.
- Aggregator·Reminder·ViewModel 단위 테스트와 핵심 Compose UI 테스트로 주요 회귀를 감시하고 있습니다.
- GitHub Actions CI가 Gradle 래퍼 검증과 테스트 실행을 자동화해 빌드 품질을 유지합니다.

## 3. 남은 과제
- 반복 일정 인스턴스 확장, 완료/반복 필터, 일정 충돌 강조 등 Agenda 심화 기능 보강.
- 알림 빠른 액션, 권한 안내 재노출 정책, 실제 단말 기반 WorkManager/AlarmManager 통합 테스트 확보.
- 문자열 리소스화 및 다국어 지원, 통합/E2E 테스트로 품질과 접근성 수준 향상.

## 4. 앱 테스트 방법
1. 프로젝트 루트에서 `./gradlew test`로 단위 테스트를 실행합니다.
2. Android Studio의 `Run > Run` 또는 `./gradlew connectedAndroidTest`로 Compose UI 및 계측 테스트를 수행합니다.
3. 알림 동작을 검증하려면 POST_NOTIFICATIONS 권한이 부여된 실제 기기나 에뮬레이터에서 앱을 설치해 워크플로우를 수동 점검합니다.
