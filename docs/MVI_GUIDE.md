# 코인 거래소 앱 MVI 패턴 개발 스펙

본 문서는 Compose 기반 Android 프로젝트에서 MVI 패턴을 구현할 때 반드시 지켜야 할 **절대 준수 규칙**과 **표준 템플릿 코드**를 정의합니다.

---

## 📌 1. MVI 컴포넌트 핵심 제약 조건

### 1.1 UiState (상태)
- **기본 형태**: 반드시 `data class`로 작성하며, 모든 프로퍼티는 불변(Immutable, `val`)으로 선언한다.
- **컬렉션**: `MutableList` 대신 `List`, `ImmutableList` 등을 사용한다.
- **초기값**: 모든 프로퍼티에 기본값(Default value)을 명시적으로 제공해야 한다.
- **예외 (상태 모순 방지)**: 화면의 상태가 논리적으로 완전히 분리되어야 하는 경우(예: Loading, Success, Error), 상태 모순을 막기 위해 예외적으로 `sealed interface`를 최상위 상태로 허용한다.

### 1.2 Intent (사용자 액션 / 이벤트)
- **기본 형태**: 반드시 `sealed interface`로 작성하며, 이름은 `[화면이름]Intent` 형식으로 지정한다.
- **파라미터 유무**:
    - 파라미터가 없는 액션: `data object`
    - 파라미터가 있는 액션: `data class`
- **동시성 제어**: API 통신이나 화면 이동을 트리거하는 Intent는 중복 실행 방지를 위해 Job Cancellation, `debounce` 등을 통해 제어해야 한다.

### 1.3 SideEffect (단발성 이벤트)
- **기본 형태**: 반드시 `sealed interface`로 작성하며, 이름은 `[화면이름]SideEffect`로 지정한다.
- **전달 방식**: ViewModel 내부에서 `Channel<[화면이름]SideEffect>(Channel.BUFFERED)`로 구현하여 백그라운드에서의 이벤트 유실을 방지한다.
- **노출 방식**: 외부(UI)에는 `receiveAsFlow()`를 통해 Flow 형태로 노출한다.

### 1.4 ViewModel 상태 업데이트 (State Mutation)
- ❌ **절대 금지**: `_uiState.value = _uiState.value.copy(...)` 방식은 스레드 경합(Race condition)을 유발하므로 절대 사용하지 않는다.
- ✅ **필수 사용**: 반드시 `kotlinx.coroutines.flow.update`를 활용하여 `_uiState.update { it.copy(...) }` 패턴을 사용한다.

### 1.5 Compose UI 데이터 수집 및 상태 호이스팅 (UDF)
- **상태 수집**: 상태(State)는 반드시 `collectAsStateWithLifecycle()`을 사용하여 수집한다.
- **이벤트 수집**: SideEffect는 수집 보일러플레이트를 줄이고 생명주기를 안전하게 타기 위해 공통 유틸리티인 `ObserveAsEvents`를 사용한다.
- **상태 호이스팅**: 최상위 Screen 컴포저블에서만 ViewModel을 주입받고, 하위 컴포저블에는 절대 ViewModel을 전달하지 않는다. (`UiState`와 `(Intent) -> Unit` 람다만 전달)