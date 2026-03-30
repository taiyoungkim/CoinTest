# 코인 거래소 앱 Kotlin Coding Conventions

본 문서는 Compose 기반 Android 프로젝트에서 Kotlin Convention을 정의합니다.

---

## 기본 원칙

- `data class`는 불변(`val`)으로만 구성
- `sealed interface` 선호 (`sealed class` 대신)
- 확장 함수 적극 활용
- `runCatching` 대신 `try-catch` 사용 (코루틴 컨텍스트에서 `CancellationException` 삼킴 방지)

## 포매팅

- 일반 함수 파라미터가 3개 이상이면 줄바꿈
- Composable 함수는 포매터(ktlint/detekt)에 위임

## 코루틴 / Flow

- `StateFlow` 수집 시 `collectAsStateWithLifecycle()` 사용
- 일회성 이벤트는 `Channel` + `receiveAsFlow()` 사용 (`SharedFlow(replay=0)` 이벤트 유실 방지)
- `CoroutineScope` 직접 생성 금지 — `viewModelScope` 등 구조화된 스코프 사용

## Jetpack Compose

- Composable 함수의 첫 번째 optional 파라미터는 `Modifier`
- State hoisting 원칙 준수: Composable은 상태를 소유하지 않고 파라미터로 수신
- `remember` 안에서 무거운 연산 금지 — `derivedStateOf` 또는 ViewModel로 분리

## 아키텍처 / 모듈

- `:domain` 모듈은 Android 의존성 없이 순수 Kotlin 유지
- UseCase는 `operator fun invoke()` 패턴 사용
- DTO → Entity 변환은 `:data`에서만, Entity → UiModel 변환은 `:feature` 또는 `:core:ui`에서만

## 네이밍

- Interface / 구현체: `UserRepository` (interface) / `UserRepositoryImpl` (구현체)
- Boolean 변수는 `is-`, `has-`, `should-` 접두사 사용
