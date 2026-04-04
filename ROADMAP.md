# ROADMAP

## 미구현 항목

### 테스트 코드
현재 각 모듈에 테스트 디렉토리 및 placeholder만 존재하며 실제 테스트 케이스가 작성되어 있지 않다.
상세 계획은 아래 **테스트 전략** 섹션 참고.

## 테스트 전략

### 테스트 피라미드

```
          [ UI Tests ]          ← Compose Testing (P1)
       [ Integration Tests ]    ← Ktor MockEngine (P1)
    [ Unit Tests (VM / Data / Domain) ]  ← JUnit5 + Turbine (P0)
```

단위 테스트 비중을 높게 유지하고, UI 테스트는 핵심 사용자 시나리오에 한정한다.

### 레이어별 테스트 계획

| 레이어 | 대상 | 도구 | 우선순위 |
|--------|------|------|----------|
| Domain | 모델 매핑 로직 (`toDomain()` 등) | JUnit5 | P0 |
| Data | `CoinRepositoryImpl`, `OrderBookRepositoryImpl` (REST + WebSocket) | JUnit5 + Turbine | P0 |
| Presentation | ViewModel 상태 전이 | JUnit5 + Turbine | P0 |
| UI | `CoinListScreen`, `OrderBookScreen` 렌더링 | Compose Testing | P1 |
| Integration | WebSocket 연결·재연결 시나리오 | Ktor MockEngine | P1 |

### Domain 단위 테스트

- **대상**: DTO → 도메인 모델 매핑 함수
- **검증 항목**
  - 정상 DTO 입력 시 도메인 모델 필드가 올바르게 변환되는가
  - `null` 또는 누락 필드 처리가 기대대로 동작하는가

### Data 단위 테스트

- **대상**: `CoinRepositoryImpl.getCoins()`, `OrderBookRepositoryImpl.getOrderBook()`
- **Ktor MockEngine**으로 HTTP 응답 주입
- **검증 항목**
  - 200 응답 → `Result.success`에 올바른 도메인 모델 포함
  - 4xx / 5xx 응답 → `Result.failure`에 적절한 예외 포함
  - WebSocket 스트림에서 메시지 수신 시 Flow 방출 여부 (Turbine `awaitItem()` 활용)

### Presentation (ViewModel) 단위 테스트

- **대상**: `CoinListViewModel`, `OrderBookViewModel`
- **시나리오**
  1. `LoadCoins` Intent 처리 → `isLoading = true` → 성공 시 `coins` 업데이트, `isLoading = false`
  2. 네트워크 에러 발생 → `errorMessage` 설정, `ShowError` SideEffect 발행
  3. WebSocket 스트림 수신 → `orderBook` 상태 갱신
- **도구**: JUnit5 + `kotlinx-coroutines-test` + Turbine
- **설정**: `StandardTestDispatcher` 사용, `Dispatchers.setMain()` / `resetMain()` 적용

### UI 테스트

- **대상**: `CoinListScreen`, `OrderBookScreen`
- **검증 항목**
  - 로딩 인디케이터 표시 여부
  - 코인 목록 아이템 렌더링 및 클릭 시 네비게이션 이동
  - 에러 상태 메시지 노출
- **도구**: `androidx.compose.ui.test`, Hilt Test Rule

### Integration 테스트

- **대상**: WebSocket 연결 → 데이터 수신 → 화면 갱신 전체 흐름
- **방법**: `Ktor MockEngine`으로 WebSocket 서버 모킹
- **시나리오**
  - 정상 연결 후 호가 데이터 수신 → ViewModel 상태 반영 확인
  - 연결 끊김 후 재시도 로직 동작 확인

---

## 개선 계획

### 성능 최적화

| 항목 | 내용 |
|------|------|
| WebSocket 갱신 주기 조절 | `conflate()`가 적용되어 있으나, 1초 단위 샘플링(`sample(1000)`)을 추가해 불필요한 리컴포지션 감소 |
| LazyColumn 안정화 | `@Stable` / `@Immutable` 어노테이션 적용으로 Compose 스킵 최적화 |
| 이미지 캐싱 | 코인 아이콘 로딩 시 Coil 도입 및 메모리/디스크 캐싱 설정 |
| Baseline Profiles | 앱 첫 실행 속도 개선을 위한 Baseline Profile 생성 |

### UX 개선

| 항목 | 내용 |
|------|------|
| Pull-to-Refresh | 코인 목록 화면에 SwipeRefresh 추가 |
| 에러 처리 개선 | 에러 발생 시 Snackbar + 재시도 버튼 제공 |
| Dark/Light 테마 | 시스템 테마 연동 및 테마 전환 지원 (현재 Material3 기본값) |

### 코드 품질

| 항목 | 내용 |
|------|------|
| Detekt 도입 | 정적 분석 규칙 설정 및 CI 연동 |
| 모듈별 API 가시성 제한 | `internal` 키워드 적용으로 모듈 경계 강화 |