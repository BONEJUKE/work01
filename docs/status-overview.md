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
- **Agenda 텍스트 일원화**: Agenda 화면 관련 문구와 접근성 설명을 `AgendaText` 헬퍼로 중앙 집중 관리해 향후 카피 변경 범위를 줄였습니다.

## 남은 주요 과제
- Agenda 화면의 중복된 컴포저블을 정리하고 스크린 리더 포커스 흐름을 단순화합니다.
- TalkBack·스와이프 액션 등 핵심 상호작용을 검증할 Compose UI 테스트를 확장합니다.
- 클라우드 연동 없이 오프라인 품질을 유지하기 위한 시간 동기화·백업 전략을 수립합니다.

## 추천 다음 단계
1. `docs/remaining-work.md`의 UI 구조 개선 작업을 우선 순위로 두고 중복 컴포저블을 통합합니다.
2. Agenda 주요 플로우에 대한 TalkBack·포커스·스와이프 테스트를 추가해 회귀를 방지합니다.
3. 오프라인 유지 전략(시간 편차 감지, 백업 내보내기)을 정의하고 문서화합니다.
