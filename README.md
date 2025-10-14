codex/add-calendar-app-with-scheduler-and-alarm
# Work01 Calendar App (워크01 캘린더 앱)

A product specification and Kotlin domain layer prototype for an Android calendar application. The app focuses on three primary capabilities:

1. **Scheduler** – create and manage events or tasks on specific dates and times.
2. **Alarm reminders** – configure notifications to remind users ahead of upcoming items.
3. **Task lists** – view actionable to-do lists for each day, week, or month with completion tracking.

워크01 캘린더 앱은 안드로이드 캘린더 애플리케이션을 위한 제품 사양과 코틀린 도메인 레이어 프로토타입을 제공합니다. 주요 기능은 다음과 같습니다.

1. **스케줄러** – 특정 날짜와 시간에 맞춘 이벤트와 작업을 생성하고 관리합니다.
2. **알람 리마인더** – 예정된 항목을 미리 알릴 수 있도록 알림을 구성합니다.
3. **작업 목록** – 일/주/월 단위로 실행 가능한 할 일 목록과 완료 현황을 제공합니다.

The repository contains documentation for requirements, architecture decisions, and Kotlin domain logic that can be adapted into a full Android project.

이 저장소에는 전체 안드로이드 프로젝트로 확장할 수 있는 요구사항, 아키텍처 의사결정, 코틀린 도메인 로직 문서가 포함되어 있습니다.

## Repository layout (저장소 구성)

- `docs/` – requirements, architecture, and UX planning notes.
- `app/src/main/kotlin/` – Kotlin domain-layer prototypes (data models, use cases, and view models).

- `docs/` – 요구사항, 아키텍처, UX 기획 노트
- `app/src/main/kotlin/` – 데이터 모델, 유스케이스, 뷰모델로 구성된 코틀린 도메인 레이어 프로토타입

## Getting started (시작하기)

These files are intended as a foundation for bootstrapping a full Android Studio project:

이 파일들은 전체 안드로이드 스튜디오 프로젝트를 부트스트랩하기 위한 기반으로 활용할 수 있습니다.

1. Copy the `app/src/main/kotlin` package into your Android project.
2. Wire the domain classes to persistence (Room, Realm, etc.) and notification APIs.
3. Implement UI layers (Jetpack Compose or XML) guided by the UX flows in the documentation.

1. `app/src/main/kotlin` 패키지를 기존 안드로이드 프로젝트에 복사합니다.
2. 도메인 클래스를 영속성 계층(Room, Realm 등) 및 알림 API와 연결합니다.
3. 문서에 수록된 UX 흐름을 기반으로 Jetpack Compose 또는 XML UI 레이어를 구현합니다.

### Build tooling (빌드 도구)

- The prototype does **not** ship with a Gradle wrapper. Generate one from a local Gradle installation with `gradle wrapper --gradle-version 8.5` (or the version your Android Studio install expects) before running `./gradlew` commands.
- Alternatively, open the project with Android Studio and let it create/update the wrapper during the first Gradle sync.

- 현재 프로토타입에는 Gradle 래퍼(`gradlew`)가 포함되어 있지 않습니다. `gradle wrapper --gradle-version 8.5`(또는 Android Studio가 요구하는 버전)를 로컬 Gradle 설치에서 실행해 래퍼를 생성한 뒤 `./gradlew` 명령을 사용하세요.
- 또는 Android Studio에서 프로젝트를 열면 첫 Gradle 동기화 시 래퍼를 자동으로 생성/업데이트합니다.

## Next steps (다음 단계)

- Integrate the domain logic with Android frameworks (ViewModel, WorkManager, AlarmManager).
- Persist data using Room or another database solution.
- Build Compose-based calendar and task list UI components.
- Add instrumentation/unit tests for scheduling logic and reminder calculations.

- 도메인 로직을 ViewModel, WorkManager, AlarmManager 등 안드로이드 프레임워크와 통합합니다.
- Room 등 데이터베이스 솔루션을 사용해 데이터를 영구 저장합니다.
- Compose 기반의 캘린더 및 작업 목록 UI 컴포넌트를 구축합니다.
- 스케줄링 로직과 리마인더 계산을 검증하는 계측/단위 테스트를 추가합니다.

## Current progress (현재 진행 상황)

- ✅ Domain models, aggregators, and reminder orchestration logic are implemented in Kotlin and covered by documentation in `docs/`.
- ✅ Agenda view model wiring is in place, including agenda reloading and error handling when switching between day/week/month periods.
- ⚙️ Storage and notification layers are mocked for now; they must be connected to real Room databases and Android alarm APIs inside a full application project.
- 🚧 Jetpack Compose UI screens, navigation, and user interactions are still pending and should be developed next following the UX plans in the documentation.
- 🧪 Automated tests and Gradle tasks cannot run until the wrapper (see above) is generated and Android project scaffolding is completed.

- ✅ 코틀린으로 구현된 도메인 모델, 어그리게이터, 리마인더 오케스트레이션 로직이 `docs/` 문서와 함께 준비되어 있습니다.
- ✅ 일/주/월 기간 전환 시 아젠다를 다시 불러오고 오류를 처리하는 Agenda 뷰모델 연결이 완료되었습니다.
- ⚙️ 저장소와 알림 계층은 현재 목(mock) 상태이며, 실제 Room 데이터베이스와 안드로이드 알람 API에 연결해야 합니다.
- 🚧 Jetpack Compose UI 화면, 내비게이션, 사용자 상호작용은 아직 구현되지 않았으며 문서의 UX 계획에 따라 가장 먼저 개발해야 합니다.
- 🧪 Gradle 래퍼를 생성하고 안드로이드 프로젝트 골격을 갖추기 전까지는 자동화 테스트와 Gradle 작업을 실행할 수 없습니다.

# work01
# Calendar Planner (캘린더 플래너)

An Android calendar planner application featuring daily, weekly, and monthly task organization, reminders, and recurring task alarms. The project is built with Kotlin, Jetpack Compose, and Room.

워크01 캘린더 플래너는 일간, 주간, 월간 작업 정리와 리마인더, 반복 알람 기능을 제공하는 안드로이드 애플리케이션입니다. 프로젝트는 Kotlin, Jetpack Compose, Room을 기반으로 합니다.

## Features (주요 기능)

- 📅 Date, week, and month scoped task lists with completion tracking
- 📝 Scheduler for creating detailed tasks with description, reminder lead time, and repeat cadence
- 🔔 Exact alarms with notification channel support for Android 13+ permission handling
- ♻️ Automatic rescheduling for recurring tasks (daily, weekly, monthly)

- 📅 일/주/월 범위의 작업 목록과 완료 추적 기능
- 📝 설명, 리마인더 리드타임, 반복 주기를 포함한 상세 작업 생성 스케줄러
- 🔔 Android 13+ 권한 처리를 고려한 알림 채널 기반 정시 알람
- ♻️ 반복 작업(일간, 주간, 월간)을 위한 자동 재예약 기능

## Getting started (시작하기)

1. Ensure you have Android Studio Giraffe (or newer) with the latest Android SDK (API 34) installed.
2. Clone the repository and open it in Android Studio.
3. Sync Gradle and build the project.
4. Run the app on an emulator or physical device running Android 8.0 (API 26) or higher.

1. Android Studio Giraffe 이상 버전과 최신 Android SDK(API 34)가 설치되어 있는지 확인합니다.
2. 저장소를 클론한 후 Android Studio에서 엽니다.
3. Gradle 동기화 후 프로젝트를 빌드합니다.
4. Android 8.0(API 26) 이상 버전이 실행되는 에뮬레이터 또는 실제 기기에서 앱을 실행합니다.

## Project structure (프로젝트 구조)

- `app/src/main/java/com/example/calendar` – Application entry point, alarm receiver, notification setup
- `app/src/main/java/com/example/calendar/data` – Room entities, DAO, and converters
- `app/src/main/java/com/example/calendar/domain` – Repository and alarm scheduling utilities
- `app/src/main/java/com/example/calendar/ui` – Compose UI screens and view model logic
- `app/src/main/java/com/example/calendar/ui/theme` – Compose Material theme definitions

- `app/src/main/java/com/example/calendar` – 앱 진입점, 알람 리시버, 알림 설정
- `app/src/main/java/com/example/calendar/data` – Room 엔터티, DAO, 컨버터
- `app/src/main/java/com/example/calendar/domain` – 리포지토리 및 알람 스케줄링 유틸리티
- `app/src/main/java/com/example/calendar/ui` – Compose UI 화면 및 뷰모델 로직
- `app/src/main/java/com/example/calendar/ui/theme` – Compose Material 테마 정의

## Permissions (권한)

The application requests the `POST_NOTIFICATIONS` runtime permission on Android 13+ and declares `SCHEDULE_EXACT_ALARM` for precise reminders.

이 애플리케이션은 정확한 리마인더 제공을 위해 Android 13+에서 `POST_NOTIFICATIONS` 런타임 권한을 요청하고 `SCHEDULE_EXACT_ALARM` 권한을 선언합니다.

main
