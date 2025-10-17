# 남은 주요 작업 정리

다음 표는 현재 코드베이스 기준으로 남아 있는 핵심 과제를 요약한 것입니다.

| 영역 | 미완료 항목 | 영향 | 다음 조치 |
| --- | --- | --- | --- |
| 현지화 | Compose 내부에 한국어 문자열이 하드코딩되어 있음 | 다국어/접근성 대응 불가 | 문자열을 `strings.xml`로 추출하고 번역 키를 준비 |
| UI 구조 | Agenda 화면에 유사한 카드/리스트 컴포저블이 중복 선언됨 | 유지보수성·스크린 리더 탐색성 저하 | 공통 컴포넌트를 정리하고 의미 있는 heading/role 속성을 재검토 |
| 동기화 | `docs/sync-conflict-strategy.md`에 정의된 클라우드 동기화 정책 미구현 | 멀티 디바이스/오프라인 사용 시 데이터 손실 우려 | 서버/동기화 계층을 도입하고 충돌 해결 정책을 실제 코드로 이전 |

## 완료된 항목

### 리마인더 구성 UI 추가 (완료)
- 빠른 추가 시트에서 리마인더 스위치와 분 단위 입력, 스누즈 토글을 제공해 사용자 지정 알림 구성을 받을 수 있습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1558-L1717】
- 수집한 설정을 `AgendaViewModel`이 검증 후 `Reminder` 목록으로 저장·스케줄링합니다.【F:app/src/main/kotlin/com/example/calendar/ui/AgendaViewModel.kt†L163-L211】

### 편집 플로우 보완 (완료)
- 상세 시트에서 `편집`을 누르면 상위 `CalendarApp`이 전용 편집 모달 시트를 열어 기존 일정·할 일을 수정할 수 있습니다.【F:app/src/main/kotlin/com/example/calendar/ui/CalendarApp.kt†L20-L121】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/EditSheets.kt†L1-L170】
- `AgendaRoute`는 편집 버튼을 누를 때 상세 시트를 닫고 콜백을 호출해 상위 내비게이션과 자연스럽게 연동합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L264-L310】

## 세부 항목

### 1. 현지화 리소스 정비
- 대다수 Compose 텍스트가 하드코딩되어 있고, `strings.xml`에는 앱 이름과 알림 관련 문자열만 존재합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L707-L1165】【F:app/src/main/res/values/strings.xml†L1-L8】
- 문자열을 리소스로 추출하고, 접근성 설명(`contentDescription`, `stateDescription`)도 Locale 대응이 가능하도록 개선해야 합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L914-L1100】

### 2. Agenda UI 구조 개선
- `AgendaScreen`에는 리스트/요약 컴포저블이 유사한 형태로 중복 선언되어 있어 유지보수가 어렵습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L807-L1406】
- 기존 Compose UI 테스트는 기본 렌더링만 검증하므로, 접근성 포커스·스크린 리더 플로우를 확인하는 시나리오가 추가로 필요합니다.【F:app/src/androidTest/kotlin/com/example/calendar/ui/agenda/AgendaScreenTest.kt†L19-L70】

### 3. 클라우드 동기화 구현
- 클라우드 동기화 및 충돌 해결 전략은 문서로만 존재하며 실제 코드에서는 동작하지 않습니다.【F:docs/sync-conflict-strategy.md†L1-L33】
- Room과 원격 소스를 연결하는 동기화 서비스, 충돌 히스토리 저장, 사용자 알림 흐름을 구현해야 합니다.
