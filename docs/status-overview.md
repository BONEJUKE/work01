# 프로젝트 진행 현황

## 저장소 소개
- Jetpack Compose로 구현된 일정/할 일/리마인더 통합 캘린더 앱입니다.
- Room 데이터베이스, WorkManager, AlarmManager를 묶어 리마인더를 신뢰성 있게 스케줄합니다.
- `docs` 폴더에는 설계 개요, UX 흐름, 남은 작업 정리가 포함되어 있습니다.

## 지금까지 구현된 것
- **Agenda 통합 뷰**: `AgendaAggregator`가 일·주·월 기간에 맞춰 일정과 할 일을 확장/정렬하고, UI는 탭·필터·상세 시트까지 제공합니다.
- **빠른 추가 플로우**: FAB에서 열리는 빠른 추가 시트로 새 할 일과 일정을 생성하고, `AgendaViewModel`이 저장·알림 예약·스낵바 안내를 처리합니다.
- **리마인더 오케스트레이션**: `ReminderOrchestrator`와 `ReminderNotificationWorker`가 알림 채널, 빠른 액션, 스누즈, 재부팅 복원을 지원합니다.
- **Room 기반 데이터**: `CalendarApplication`이 `RoomAppContainer`를 초기화해 DAO/Repository/Reminder를 앱 전역에 주입합니다.
- **테스트**: 반복 일정 전개, 알림 복구, ViewModel 상호작용을 다루는 단위 테스트와 Compose 계측 테스트가 구성되어 있습니다.

## 남은 주요 과제
- 상세 시트의 `편집` 버튼을 실제 편집 화면과 연결하고, 수정/삭제 후 상태 업데이트 흐름을 정비합니다.
- 리마인더 오프셋·스누즈 등 사용자 정의 입력을 UI와 도메인 계층에 추가합니다.
- Compose에 하드코딩된 텍스트를 문자열 리소스로 이전하고 다국어 번역을 준비합니다.
- Agenda 화면의 중복된 컴포저블을 정리하고 접근성 QA(스크린 리더·포커스 순서)를 수행합니다.
- `docs/sync-conflict-strategy.md`에 정의된 클라우드 동기화 정책을 실제 구현으로 옮깁니다.

## 추천 다음 단계
1. `docs/remaining-work.md`를 기준으로 편집/리마인더 UI 요구사항을 정리하고 내비게이션을 설계합니다.
2. 문자열 리소스화를 진행하면서 접근성 설명과 테스트를 함께 보완합니다.
3. 리마인더 입력값을 `ReminderOrchestrator`와 `ReminderNotificationWorker`에 연결해 사용자 정의 스케줄을 지원합니다.
4. 클라우드 동기화 서비스 도입 여부를 결정하고, Room과 원격 소스를 잇는 데이터 파이프라인을 준비합니다.
