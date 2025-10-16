# 남은 주요 작업 재정리

다음 표는 현재 미해결 상태인 핵심 과제를 요약합니다.

| 영역 | 미완료 항목 | 영향 | 즉시 조치 |
| --- | --- | --- | --- |
| 기능 | 반복 일정 전개, 필터링, 충돌 강조 | 반복/필터 요구사항을 충족하지 못해 사용자 신뢰 저하 | 집계 단계에서 인스턴스 확장, 필터 상태 모델링 |
| 알림 | 빠른 액션 부재, 재부팅 복원 미지원 | 알림 상호작용·신뢰성 저하 | Notification 액션 추가, BOOT 처리 도입 |
| UX/권한 | 권한 카드 재노출 부재 | 권한 획득 전환율 하락 | 거절 후 재노출 로직과 타이밍 정의 |
| 품질 | 현지화 미흡, 통합 테스트 부재 | 출시 품질·접근성 미달 | 문자열 리소스화, E2E/통합 시나리오 설계 |

## 제품 요구사항 미충족 항목

### 반복 일정 인스턴스 확장 미구현
- **문제**: `AgendaAggregator`는 저장소에서 받은 일정 컬렉션을 그대로 합쳐 노출하며, 반복 규칙을 풀어 개별 인스턴스로 변환하지 않습니다.【F:app/src/main/kotlin/com/example/calendar/scheduler/AgendaAggregator.kt†L16-L64】
- **영향**: 반복 일정이 UI에 단 한 번만 등장해 요구사항의 반복 일정 지원 항목을 충족하지 못합니다.【F:docs/requirements.md†L3-L22】
- **다음 조치**: 반복 규칙 파서를 추가하고, 선택된 기간에 맞춰 인스턴스를 생성하도록 `AgendaAggregator`와 저장소 계층을 확장합니다.

### 완료/반복 필터 UI 미구현
- **문제**: `AgendaScreen`은 완료 여부나 반복 유형을 필터링할 상태·컨트롤을 제공하지 않습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L791-L841】
- **영향**: 사용자 요구사항의 필터 기능을 제공하지 못해 긴 목록에서 작업을 추적하기 어렵습니다.【F:docs/requirements.md†L8-L13】
- **다음 조치**: 뷰모델에 필터 상태를 도입하고 토글/드롭다운 UI를 구성해 목록 쿼리를 조건부로 갱신합니다.

### 이벤트 충돌 강조 미구현
- **문제**: 집계 모델에 일정 겹침 여부를 계산하거나 전달하는 필드가 없습니다.【F:app/src/main/kotlin/com/example/calendar/scheduler/AgendaAggregator.kt†L16-L64】
- **영향**: 충돌 하이라이트 요구사항이 미충족되어 사용자가 겹치는 일정을 인지하지 못합니다.【F:docs/requirements.md†L20-L22】
- **다음 조치**: 스냅샷 생성 시 겹치는 시간대를 계산해 UI에 색상/배지 정보를 전달합니다.

### 알림 빠른 액션 미구현
- **문제**: `ReminderNotificationWorker`는 제목과 본문만 포함한 기본 알림을 발행하며, 스누즈·완료 같은 액션 버튼을 추가하지 않습니다.【F:app/src/main/kotlin/com/example/calendar/reminder/ReminderNotificationWorker.kt†L33-L78】
- **영향**: 알림 요구사항의 빠른 액션 제공이 누락돼 사용자가 앱 진입 없이 작업을 처리할 수 없습니다.【F:docs/requirements.md†L14-L18】
- **다음 조치**: `PendingIntent` 기반 액션을 추가하고, 스누즈·완료 처리를 위한 워커/리시버를 연결합니다.

## 플랫폼 통합 및 UX 개선 필요

### 재부팅 복원 미지원
- **문제**: `AndroidManifest.xml`에 `BOOT_COMPLETED` 브로드캐스트를 수신하는 컴포넌트가 정의돼 있지 않습니다.【F:app/src/main/AndroidManifest.xml†L1-L27】
- **영향**: 기기 재부팅 후 예약된 알림이 복원되지 않아 신뢰성이 떨어집니다.【F:docs/requirements.md†L14-L18】
- **다음 조치**: 부팅 수신 리시버 또는 재등록 워커를 추가하고 `ReminderOrchestrator`와 연동합니다.

### 권한 안내 재노출 부재
- **문제**: `CalendarApp`은 `shouldShowPrompt`가 `false`로 평가되면 `dismissed` 플래그를 재설정하지 않아, 한 번 닫힌 권한 안내 카드가 영구적으로 숨겨집니다.【F:app/src/main/kotlin/com/example/calendar/ui/CalendarApp.kt†L55-L76】
- **영향**: 사용자 권한 전환율이 떨어지고, 재설치 없이 알림 기능을 사용할 수 없습니다.
- **다음 조치**: 거절 후 일정 시간/세션 경과 시 재노출하는 정책을 도입하고, 권한 상태 옵저버를 통해 UI를 업데이트합니다.

## 품질 및 현지화 TODO

### 문자열 리소스 정비
- **문제**: 문자열 리소스 파일이 앱 이름만 포함하고 대부분의 UI 문구가 하드코딩돼 있습니다.【F:app/src/main/res/values/strings.xml†L1-L4】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L807-L900】
- **영향**: 현지화 및 접근성 요구사항을 충족하지 못합니다.【F:docs/requirements.md†L28-L30】
- **다음 조치**: 주요 Compose 텍스트를 문자열 리소스로 추출하고 번역 키를 준비합니다.

### 통합/E2E 테스트 부재
- **문제**: 알림과 일정 흐름을 통합 검증하는 테스트 케이스가 정의돼 있지 않습니다.【F:app/src/androidTest/kotlin/com/example/calendar/ui/agenda/AgendaScreenTest.kt†L1-L59】
- **영향**: 비즈니스 크리티컬 시나리오(알림 예약·발송)가 회귀 테스트 없이 배포됩니다.【F:docs/status-overview.md†L20-L28】
- **다음 조치**: WorkManager·AlarmManager 플로우를 계측 테스트로 검증하고, UI 테스트에 알림 권한 흐름을 포함합니다.
