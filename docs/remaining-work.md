# 남은 주요 작업 정리

다음 표는 현재 코드베이스 기준으로 남아 있는 핵심 과제를 요약한 것입니다.

| 영역 | 미완료 항목 | 영향 | 다음 조치 |
| --- | --- | --- | --- |
| UI 구조 | Agenda 화면에 유사한 카드/리스트 컴포저블이 중복 선언됨 | 유지보수성·스크린 리더 탐색성 저하 | 공통 컴포넌트를 정리하고 의미 있는 heading/role 속성을 재검토 |
| 접근성/QA | 포커스 이동·스와이프 액션 등을 다루는 UI 테스트가 부족함 | 회귀 검출이 어려워 접근성 품질 저하 위험 | Compose UI 테스트를 확장해 스크린 리더·포커스 시나리오를 포함 |
| 오프라인 운영 | 클라우드 동기화를 도입하지 않는 대신 시간 동기화·백업 전략이 미정 | 장기 오프라인 사용 시 데이터 일관성 검증이 어려움 | 기기 시간 편차 감지, 내보내기/가져오기 등 오프라인 보조 시나리오 정의 |

## 완료된 항목

### 리마인더 구성 UI 추가 (완료)
- 빠른 추가 시트에서 리마인더 스위치와 분 단위 입력, 스누즈 토글을 제공해 사용자 지정 알림 구성을 받을 수 있습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L1558-L1717】
- 수집한 설정을 `AgendaViewModel`이 검증 후 `Reminder` 목록으로 저장·스케줄링합니다.【F:app/src/main/kotlin/com/example/calendar/ui/AgendaViewModel.kt†L163-L211】

### 편집 플로우 보완 (완료)
- 상세 시트에서 `편집`을 누르면 상위 `CalendarApp`이 전용 편집 모달 시트를 열어 기존 일정·할 일을 수정할 수 있습니다.【F:app/src/main/kotlin/com/example/calendar/ui/CalendarApp.kt†L20-L121】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/EditSheets.kt†L1-L170】
- `AgendaRoute`는 편집 버튼을 누를 때 상세 시트를 닫고 콜백을 호출해 상위 내비게이션과 자연스럽게 연동합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L264-L310】

### Agenda 텍스트 관리 일원화 (완료)
- Agenda 화면 전반에 사용되는 문구와 접근성 설명을 `AgendaText` 헬퍼로 모아 일관되게 재사용합니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaText.kt†L1-L158】
- 편집 시트·퀵 추가 흐름 등 기존 컴포저블에서 하드코딩 문자열을 제거해 유지보수 포인트를 줄였습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/EditSheets.kt†L1-L170】【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L108-L377】

## 세부 항목

### 1. Agenda UI 구조 개선
- `AgendaScreen`에는 리스트/요약 컴포저블이 유사한 형태로 중복 선언되어 있어 유지보수가 어렵습니다.【F:app/src/main/kotlin/com/example/calendar/ui/agenda/AgendaScreen.kt†L807-L1406】
- 기존 Compose UI 테스트는 기본 렌더링만 검증하므로, 접근성 포커스·스크린 리더 플로우를 확인하는 시나리오가 추가로 필요합니다.【F:app/src/androidTest/kotlin/com/example/calendar/ui/agenda/AgendaScreenTest.kt†L19-L70】

### 2. 접근성·QA 보강
- TalkBack 포커스 순서, 스와이프 제스처 안내 등 주요 시나리오를 자동화 테스트로 검증하는 체계가 없습니다.【F:app/src/androidTest/kotlin/com/example/calendar/ui/agenda/AgendaScreenTest.kt†L19-L70】
- 테스트 부재로 인해 `AgendaText`에서 문구를 변경할 때 스크린 리더 아나운스가 깨질 위험이 있습니다.

### 3. 오프라인 품질 전략 수립
- 앱은 오프라인 우선으로 운영되므로 클라우드 동기화 대신 기기 시간 차이, 백업 내보내기 등 보조 시나리오가 필요합니다.【F:docs/requirements.md†L26-L35】
- 장기 미접속 사용자를 위한 데이터 무결성 체크리스트와 사용자 안내가 아직 문서화되지 않았습니다.
