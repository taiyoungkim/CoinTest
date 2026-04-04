# AI 활용 개발 회고

## 사용 도구

- **Claude Code** (Claude Sonnet 4.6 / Opus 4.6)
- IDE 통합 CLI 방식으로 코드 검토 및 보조 작업에 활용

---

## 작업 전략

### 사전 컨벤션 문서화

코드 작성에 앞서 프로젝트 전반의 규칙과 제약 조건을 문서로 먼저 정리했다.

- **`CLAUDE.md`**: 모듈 간 의존성 방향, 금지 사항(`collectAsState()` 사용 금지, `runCatching`을 suspend 함수에서 사용 금지, `CancellationException` 재전파 필수 등), 기술 스택과 아키텍처 원칙을 명시
- **`docs/MVI_GUIDE.md`**: MVI 패턴의 State/Intent/SideEffect 설계 규칙
- **`docs/NETWORK_RULE.md`**: WebSocket 재연결 전략, Binary 프레임 처리, 에러 핸들링 규칙
- **`docs/KOTLIN_CONVENTION.md`**: 코루틴 예외 처리, 네이밍 규칙
- **`docs/UI_SPEC.md`**: Compose UI 구현 시 색상, 레이아웃, 컴포넌트 규칙

이 문서들은 AI에게 맥락을 주입하는 용도이기도 하지만, 먼저 본인이 프로젝트의 기술적 방향을 정리하고 기준을 세우는 과정이었다. 규칙이 문서로 존재하면 AI가 맥락을 잊더라도 재주입이 가능하고, 코드 리뷰 시 판단 기준이 명확해진다.

### 문제 분해 방식

사전과제 요구사항을 분석한 뒤 구현 순서를 bottom-up으로 잡았다.

1. 도메인 모델 정의 (`Coin`, `OrderBook`, `OrderBookUnit`)
2. 네트워크 레이어 (Ktor HTTP / WebSocket, DTO)
3. 데이터 레이어 (Repository 구현체, 매핑)
4. Presentation 레이어 (ViewModel, MVI Contract)
5. UI 레이어 (Compose Screen)
6. 네비게이션 연결 및 통합

도메인이 확정되지 않으면 상위 레이어 코드가 계속 바뀌기 때문에 도메인부터 먼저 확정했다. 이 순서는 AI 활용 여부와 관계없이 의존성 방향을 따른 자연스러운 구현 순서이며, 결과적으로 AI에게 작업을 요청할 때도 이전 레이어가 확정된 상태이므로 맥락 손실이 적었다.

### 전체 작업 흐름

```
요구사항 분석 (직접)
 → 컨벤션 문서 작성: CLAUDE.md, docs/ (직접)
 → 아키텍처 결정, 모듈 구조 설계, build-logic 구성 (직접)
 → 코인 리스트 기능 구현 (도메인 → 데이터 → ViewModel → UI)
 → 호가창 기능 구현 (도메인 → WebSocket/DTO → 데이터 → ViewModel → UI → 네비게이션)
 → 문서 작성 (직접)
```

---

## 구체적 AI 상호작용 사례

### 사례 1: WebSocket Binary 프레임 처리 누락

- **프롬프트**: "Ktor WebSocket으로 Upbit 호가 데이터를 구독하는 서비스 클래스를 작성해줘. ticker와 orderbook을 동시에 구독하고 Flow로 반환해야 해."
- **AI 출력**: `for (frame in incoming)` 루프 안에서 `Frame.Text`만 처리하는 코드를 생성했다. Upbit WebSocket은 실제로 Binary 프레임으로 응답을 내려보내는 경우가 있는데 이 케이스가 빠져 있었다.
- **판단**: 수정
- **이유**: Upbit 공식 문서를 확인하면 WebSocket 응답이 Text 또는 Binary 프레임 모두 가능하다고 명시되어 있다. Text만 처리하면 Binary로 수신되는 메시지가 무시되어 호가가 갱신되지 않는 증상이 발생한다.
- **수정 내용**: `Frame.Binary -> frame.readBytes().decodeToString()` 분기를 추가하여 두 타입 모두 처리하도록 수정했다.

---

### 사례 2: DTO `@SerialName` 누락

- **프롬프트**: "Upbit orderbook WebSocket 응답 JSON을 파싱할 DTO 클래스를 작성해줘. `orderbook_units` 배열을 포함해야 해."
- **AI 출력**: `@Serializable` 어노테이션과 필드 목록은 올바르게 생성했으나, 일부 필드에 `@SerialName`이 없었다. 예를 들어 `totalAskSize`는 JSON 키가 `total_ask_size`이지만 `@SerialName` 없이 그냥 `val totalAskSize: Double`로 생성했다.
- **판단**: 수정
- **이유**: kotlinx.serialization의 기본 네이밍 전략은 Kotlin 필드명을 그대로 JSON 키로 사용한다. snake_case 키를 camelCase 필드명으로 받으려면 반드시 `@SerialName`을 명시해야 한다. 누락된 상태로 빌드하면 런타임에서 파싱 에러가 발생한다.
- **수정 내용**: 모든 필드에 `@SerialName`을 명시적으로 추가했다. 이후 이 규칙을 프로젝트 컨벤션으로 문서화했다.

---

### 사례 3: WebSocket 재연결 전략의 무한 즉시 재시도

- **프롬프트**: "WebSocket 연결이 끊겼을 때 자동으로 재연결하는 로직을 추가해줘."
- **AI 출력**: `flow.retry(Long.MAX_VALUE) { true }` 형태로 즉시 재시도하는 코드를 생성했다. 딜레이가 없었다.
- **판단**: 수정
- **이유**: 딜레이 없이 무한 재시도하면 서버가 일시적으로 내려간 상황에서 클라이언트가 초당 수십 번 연결을 시도하게 된다. 이는 불필요한 배터리·네트워크 소모를 일으키고, 서버 복구를 방해할 수 있다.
- **수정 내용**: Exponential backoff 방식으로 변경했다. 초기 1초부터 시작해서 연결 실패마다 지연을 두 배로 늘리고 최대 30초로 제한하는 방식으로 직접 구현했다. 연결 성공 시 `retryDelay`를 초기값으로 리셋하는 로직도 함께 추가했다.

---

### 사례 4: Compose UI에서 `collectAsState()` 사용

- **프롬프트**: "OrderBookViewModel의 uiState를 Compose에서 구독하는 코드를 작성해줘."
- **AI 출력**: `val uiState by viewModel.uiState.collectAsState()`를 사용하는 코드를 생성했다.
- **판단**: 수정
- **이유**: `collectAsState()`는 `Lifecycle`을 인식하지 못해 앱이 백그라운드로 진입해도 계속 Flow를 수집한다. `collectAsStateWithLifecycle()`을 사용하면 `Lifecycle.State.STARTED` 이상일 때만 수집하여 불필요한 리소스 소모와 메모리 누수를 방지할 수 있다. 이 프로젝트에서는 이를 금지 규칙으로 명시했다.
- **수정 내용**: `collectAsStateWithLifecycle()`로 교체했다. `lifecycle-runtime-compose` 의존성이 이미 포함되어 있어 추가 작업 없이 교체 가능했다.

---

### 사례 5: Hilt 멀티모듈 DI 설정

- **프롬프트**: "`:data` 모듈에서 `CoinRepository` 인터페이스를 `CoinRepositoryImpl`로 바인딩하는 Hilt 모듈 코드를 작성해줘. `:domain` 모듈에 인터페이스가 있고 `:data` 모듈에 구현체가 있어."
- **AI 출력**: `@Module`, `@InstallIn(SingletonComponent::class)`, `@Binds`, `@Singleton` 어노테이션을 올바르게 사용한 코드를 생성했다. 모듈 간 의존성 방향도 지켜졌다.
- **판단**: 수락
- **이유**: 생성된 코드가 Hilt 멀티모듈 표준 패턴과 일치했고, 의존성 방향 규칙(`data → domain`)을 위반하지 않았다. 별도 수정 없이 그대로 사용했다.
- **수정 내용**: 없음

---

## AI가 만든 실수

### 1. WebSocket Binary 프레임 미처리

실제 기기에서 테스트했을 때 호가창이 갱신되지 않는 현상이 발생했다. 로그를 확인하니 `incoming` 채널에서 프레임을 수신하고 있었으나 `Frame.Text`가 아닌 경우 조용히 skip되고 있었다. Upbit WebSocket 응답이 Binary 프레임으로 내려올 수 있다는 점을 AI가 고려하지 않은 것이다.

### 2. `collectAsState()` 사용

Lifecycle-aware하지 않은 `collectAsState()`를 기본으로 사용했다. 코드 리뷰 시점에 직접 발견했다. 이 경우는 앱을 실행해도 바로 드러나지 않는 문제라 주의가 필요했다.

### 3. `CancellationException` 재전파 누락

초안 코드에서 `catch` 블록이 모든 `Throwable`을 잡은 뒤 재전파 없이 상태 업데이트만 하는 패턴이 있었다. `CancellationException`을 catch에서 재전파하지 않으면 코루틴 취소 메커니즘이 정상 동작하지 않는다. 코드 검토 과정에서 발견하여 `kotlinx.coroutines.CancellationException`일 경우 `throw`하도록 수정했다.
---

## 아키텍처 결정

### MVI + Clean Architecture + 멀티모듈 채택

사전과제에서 아키텍처는 자유였다. AI는 처음에 단순한 MVVM + 단일 모듈 구조를 제안했다. 이를 거부하고 MVI + 멀티모듈로 직접 설계했다.

**이유**: WebSocket 기반 실시간 데이터는 단방향 데이터 흐름(MVI)과 궁합이 좋다. 실시간으로 들어오는 이벤트를 `Intent`로 표현하고 `State`를 단일 불변 객체로 유지하면 UI 일관성을 보장하기 쉽다. 또한 멀티모듈 구조는 의존성 방향을 컴파일 타임에 강제할 수 있어 레이어 위반을 사전에 차단한다.

### `feature` 모듈의 `data`/`network` 직접 참조 금지

AI가 편의상 `feature:orderbook`에서 직접 `UpbitWebSocketService`를 참조하는 코드를 생성한 적이 있었다. 이를 거부하고 항상 도메인 레이어의 `OrderBookRepository` 인터페이스를 통해 접근하도록 강제했다.

**이유**: feature 모듈이 data/network 모듈을 직접 참조하면 계층 분리가 무너지고, 테스트 시 실제 네트워크 구현체를 교체하기 어려워진다. 인터페이스를 통한 접근만 허용해야 Repository 패턴의 이점을 온전히 누릴 수 있다.

### WebSocket `combine` 후 `.conflate()` 적용

ticker와 orderbook 두 스트림을 `combine`으로 합친 뒤 `.conflate()`를 적용했다. AI는 처음에 `conflate()` 없이 제안했다.

**이유**: WebSocket은 UI 렌더링 속도보다 훨씬 빠르게 메시지를 보낼 수 있다. `conflate()`를 적용하지 않으면 처리하지 못한 데이터가 쌓이며 메모리와 지연이 증가한다. 가장 최신 상태만 UI에 반영하면 충분하기 때문에 `conflate()`가 적절하다.

---

## 솔직한 평가

### AI가 효과적이었던 부분

- **보일러플레이트 검증**: Hilt 모듈 설정, `build.gradle.kts` 의존성 구성처럼 형식이 정해진 코드는 AI에게 생성을 맡기고 검토하는 방식이 효율적이었다.
- **패턴 일관성 확인**: 여러 화면에 걸쳐 MVI Contract 구조(`UiState`, `Intent`, `SideEffect`)가 일관적으로 작성되었는지 AI를 통해 교차 검증할 수 있었다.
- **컨벤션 준수 감시**: `CLAUDE.md`에 명시한 규칙을 기반으로 AI가 코드 리뷰 시 위반 사항을 잡아내는 보조 역할을 했다.

### AI의 한계를 느낀 부분

- **플랫폼 특성 파악 부족**: Upbit WebSocket이 Binary 프레임을 사용한다는 실제 API 동작 방식은 AI가 알지 못했다. 공식 문서를 직접 확인해야 했다.
- **Android 생명주기 이해 부족**: `collectAsState()` vs `collectAsStateWithLifecycle()` 같은 Android 플랫폼 베스트 프랙티스를 놓치는 경우가 있었다. AI 출력을 그대로 믿지 않고 항상 검토해야 한다.
- **맥락 유지의 한계**: 작업이 길어지면 AI가 앞서 설정한 규칙(예: 의존성 방향 제한)을 잊고 이전 패턴으로 돌아가는 경우가 있었다. `CLAUDE.md`와 `docs/` 문서를 사전에 작성해둔 덕분에 재주입으로 대응할 수 있었지만, 세션이 길어질수록 수동 개입이 필요했다.

### 다음에 다르게 할 점

- **레이어별 검증 체크리스트 도입**: 이번 프로젝트에서는 컨벤션 문서(`CLAUDE.md`, `docs/`)를 사전에 작성하여 AI의 맥락 손실과 규칙 위반을 줄일 수 있었다. 다음에는 각 레이어 구현 완료 시점에 의존성 방향, 에러 처리, 생명주기 처리를 자동으로 확인하는 체크리스트를 추가하여 검증을 더 체계화하고 싶다.
- **공식 문서 대조 습관 유지**: 특히 플랫폼별 특성(Upbit API 동작, Android 생명주기)은 AI가 정확하게 파악하지 못하는 영역이다. AI는 코드 형식은 잘 맞추지만 실제 런타임 동작까지 보장하지 않으므로, 공식 문서와 대조하는 과정을 생략하지 않아야 한다.
