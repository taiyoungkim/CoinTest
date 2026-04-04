# CoinTest

Upbit 공개 API를 활용한 암호화폐 종목 리스트 + 실시간 호가창 Android 앱

---

## 빌드 및 실행 방법

### 환경 요구사항

- JDK 17
- Android SDK 36
- Android Studio Meerkat 이상 권장

### 빌드 및 실행

```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 연결된 기기에 설치 및 실행
./gradlew installDebug
```

---

## 아키텍처

### MVI + Clean Architecture 선택 근거

**MVI (Model-View-Intent)**
- 단방향 데이터 흐름(UDF)으로 상태 변화 추적이 용이
- WebSocket 같은 실시간 스트림 이벤트를 Intent로 명확하게 표현 가능
- 단일 불변 State 객체로 UI 일관성 보장

**Clean Architecture**
- 도메인 레이어 격리로 비즈니스 로직을 Android 프레임워크에 독립적으로 테스트 가능
- Repository 인터페이스를 도메인에 정의하여 데이터 소스 교체에 유연
- 계층별 역할 분리로 책임 범위 명확

**멀티모듈**
- 모듈 간 의존성 방향을 컴파일 타임에 강제
- Gradle 병렬 빌드 및 빌드 캐시 활용 가능
- 기능 단위 독립적 개발 가능

### 모듈 구조

```
:app
├── :feature:list
│   └── :domain
├── :feature:orderbook
│   └── :domain
├── :data
│   ├── :domain
│   └── :core:network
├── :core:ui
└── :domain
```

### 의존성 방향

```
:app → :feature:list, :feature:orderbook, :data (DI 연결)
:feature:list → :domain
:feature:orderbook → :domain
:data → :domain, :core:network
:core:network → (독립)
:domain → (순수 Kotlin, Android 의존성 없음)
:core:ui → (Compose 유틸리티)
```

- `feature` 모듈은 `data`, `core:network`를 직접 참조하지 않음
- `domain` 모듈은 어떤 모듈에도 의존하지 않는 순수 Kotlin 모듈

---

## 거래소 선택 근거

**Upbit**을 선택한 이유는 다음과 같습니다.

- 국내 최대 거래량의 원화 마켓 거래소로 실사용 데이터 품질이 높음
- REST API와 WebSocket API 문서가 충실하게 정리되어 있음
- 종목 시세 및 호가 조회에 별도 인증(API Key) 불필요
- 한글 종목명(`korean_name`) 필드를 기본 제공하여 한국 사용자 친화적 UI 구현 가능

---

## 주요 라이브러리 선택 근거

| 라이브러리 | 버전 | 선택 근거 |
|-----------|------|----------|
| Ktor Client (OkHttp 엔진) | 3.1.3 | HTTP REST와 WebSocket을 단일 클라이언트로 처리. Retrofit은 WebSocket을 지원하지 않아 별도 OkHttp WebSocket 인스턴스 관리가 필요했고, Ktor는 이를 통합하여 연결 관리를 단순화 |
| kotlinx.serialization | 1.8.1 | Kotlin Multiplatform 호환, Ktor와 네이티브 통합, reflection 없는 컴파일 타임 직렬화 처리로 ProGuard 설정 부담 없음 |
| Hilt | 2.56.1 | Dagger 기반 컴파일 타임 의존성 주입으로 런타임 오류 조기 발견. Android 생명주기(`ViewModel`, `Activity`, `Fragment`) 통합 지원, 멀티모듈 환경에서의 `@InstallIn` 기반 컴포넌트 분리 |
| Compose Navigation | 2.9.0 | Type-safe 라우팅(Kotlin Serialization 기반)으로 화면 이동 시 런타임 타입 오류 방지. Fragment 없는 단일 Activity 구조에 적합 |
| Kotlin Coroutines + Flow | 1.10.2 | 비동기 스트림 처리와 생명주기 통합(`collectAsStateWithLifecycle`)을 통한 메모리 누수 방지 |

---

## 주요 기능

### 종목 리스트

- Upbit KRW 마켓의 전체 종목 조회
- 현재가, 전일 대비 등락률, 24시간 거래대금 표시
- 24시간 거래대금 기준 내림차순 정렬
- 종목 탭 시 호가창으로 이동

### 호가창

- 선택한 종목의 실시간 매수/매도 호가 10단계 표시
- WebSocket으로 ticker(현재가) + orderbook(호가) 동시 구독
- 현재가 기준 매수/매도 비율 시각화

### 실시간 갱신

- WebSocket 연결을 통해 호가 및 현재가 실시간 수신
- Exponential backoff(1초~30초) 기반 자동 재연결 전략 적용
- `Flow.conflate()`로 UI 갱신 누적 방지

---

## 설계 판단 기록

모호한 요구사항에 대해 다음과 같이 판단했습니다.

| 항목 | 판단 | 근거 |
|------|------|------|
| 종목 리스트 기본 정렬 기준 | 24시간 거래대금 내림차순 | 가장 활발하게 거래되는 종목이 사용자 관심도와 상관관계가 높음 |
| 표시 마켓 범위 | KRW 마켓만 표시 | BTC/USDT 마켓은 원화 기준 사용자에게 혼란을 줄 수 있어 원화 마켓으로 한정 |
| 호가창 WebSocket 구독 방식 | ticker + orderbook 동시 구독 | 현재가와 호가를 별개의 API로 요청하면 동기화 문제가 발생할 수 있어 단일 WebSocket 세션에서 동시 구독 처리 |
| WebSocket 재연결 전략 | Exponential backoff (최소 1초, 최대 30초) | 즉시 재연결은 서버 부하 가중 및 배터리 소모 문제가 있고, 고정 주기 재연결보다 backoff가 네트워크 복구 상황에 더 적합 |
| 피처 모듈 분리 단위 | `:feature:list`와 `:feature:orderbook`을 독립 모듈로 분리하고, 네비게이션은 `:app`에 위임 | 호가창은 푸시 알림, 위젯, 딥링크 등 리스트를 거치지 않는 직접 진입이 필요할 수 있어 독립성 확보가 필수적. 또한 호가창은 WebSocket + 복잡한 UI를 포함하므로 합칠 경우 리스트의 단순 UI 수정에도 불필요한 재빌드가 발생. 피처 모듈은 `SideEffect`만 방출하고 `:app`이 라우터 역할을 수행하여 모듈 간 순환 참조를 원천 차단 |
