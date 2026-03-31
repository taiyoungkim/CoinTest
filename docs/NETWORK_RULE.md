# 코인 거래소 앱 네트워크

본 문서는 `core:network` 및 `data` 모듈에서 Upbit API 통신 및 데이터 처리를 구현할 때 준수해야 할 기술 표준입니다.

---

### 1. REST API (Ktor HttpClient)

* **엔진 설정**: OkHttp 엔진을 사용하며, `connectionPool`을 통해 리소스를 최적화한다.
* **타임아웃**: `connectTimeoutMillis = 10_000`, `requestTimeoutMillis = 15_000`을 기본으로 명시한다.
* **플러그인 필수 설정**:
    * `ContentNegotiation`: `Json { ignoreUnknownKeys = true; isLenient = true }` 설정 필수.
    * `Logging`: 개발 빌드(`DEBUG`)에서만 `LogLevel.ALL` 또는 `LogLevel.INFO`를 활성화한다.
    * `DefaultRequest`: `header(HttpHeaders.ContentType, ContentType.Application.Json)`를 기본 포함한다.

### 2. WebSocket (Real-time Stream)

* **기본 구조**: Ktor WebSocket과 `channelFlow`를 조합하여 메시지 스트림을 구성한다.
* **연결 유지**: `pingInterval = 20_000` (20초) 설정을 통해 서버와의 세션을 유지한다.
* **메시지 파싱 (Polymorphic JSON)**:
    1.  `Frame.Binary`와 `Frame.Text` 모두 대응 가능하도록 설계한다.
    2.  `Json.parseToJsonElement`로 선파싱 후, `type` 필드(예: ticker, orderbook)를 확인하여 개별 DTO로 디코딩한다.
* **부하 조절 (Backpressure)**: 실시간 데이터 폭주로 인한 UI 프리징을 방지하기 위해 반드시 `.conflate()` 연산자를 사용한다. (최신 데이터 위주 처리)
* **리소스 정리**: `awaitClose` 블록에서 `session.close()`를 호출하여 연결 누수를 방지하고, ViewModel의 `onCleared()`와 연동한다.

### 3. 에러 처리 및 상태 관리

* **네트워크 상태 감시**: API 호출 전 `ConnectivityManager`를 통해 기기의 네트워크 연결 여부를 우선 확인한다.
* **에러 분류 (Domain Layer)**:
    * `NetworkError.Connectivity`: 네트워크 미연결 (Offline)
    * `NetworkError.Server(code)`: 5xx 서버 에러
    * `NetworkError.Api(message)`: 4xx 비즈니스 에러 (Rate limit 포함)
    * `NetworkError.Unknown`: 파싱 실패 및 기타 정의되지 않은 에러
* **재연결 전략**: WebSocket 연결 실패 시 `Exponential Backoff`를 적용한다. (초기 1초, 최대 30초까지 점진적 증가)
* **라이프사이클**: `repeatOnLifecycle(STARTED)`와 연동하여 앱이 백그라운드일 때는 연결을 해제하거나 일시정지한다.

### 4. 데이터 매핑 및 성능 (Data ↔ Domain)

* **Threading**: 모든 DTO → Domain Model 매핑 로직은 `Dispatchers.Default`에서 수행하여 UI 스레드 부하를 방지한다.
* **DTO 모델**:
    * 모든 필드는 `val`을 사용하며 `@Serializable`을 붙인다.
    * API 명세와 변수명이 일치하더라도 명시적으로 `@SerialName`을 작성하여 가독성과 난독화에 대비한다.

### 5. 네이밍 및 컨벤션

* **인터페이스**: `CoinRepository` (interface) / `CoinRepositoryImpl` (class) 형식을 따른다.
* **API 명세**: Upbit API의 요청 제한(Rate Limit)을 준수하도록 호출 빈도를 제어한다. (초당 요청 수 초과 주의)
* **Boolean**: 변수명은 `is-`, `has-`, `should-` 접두사를 사용한다. (예: `isConnected`, `hasMoreData`)

---

### 6. Upbit API 명세

Base URL: `https://api.upbit.com`

#### 6.1 마켓 코드 목록 조회

| 항목 | 값 |
|------|----|
| **Endpoint** | `GET /v1/market/all` |
| **인증** | 불필요 |
| **Rate Limit** | 초당 최대 10회 (IP 단위, 마켓 그룹 공유) |

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `is_details` | Boolean | No | 유의종목/관리종목 상세 포함 여부. 기본값 false |

**Response (배열)** → `MarketDto`

| 필드 | 타입 | 설명 |
|------|------|------|
| `market` | String | 마켓 코드 (예: "KRW-BTC") |
| `korean_name` | String | 한글 종목명 (예: "비트코인") |
| `english_name` | String | 영문 종목명 (예: "Bitcoin") |
| `market_event.warning` | Boolean | 유의종목 지정 여부 (`is_details=true` 시 포함) |

> KRW 마켓만 사용 시: `market.startsWith("KRW-")` 필터 적용

#### 6.2 현재가(Ticker) 조회

| 항목 | 값 |
|------|----|
| **Endpoint** | `GET /v1/ticker` |
| **인증** | 불필요 |
| **Rate Limit** | 초당 최대 10회 (IP 단위, 현재가 그룹 공유) |

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `markets` | String | **필수** | 조회할 마켓 코드 목록. 쉼표(,) 구분. 예: `"KRW-BTC,KRW-ETH"` |

**Response (배열)** → `TickerDto`

| 필드 | 타입 | 설명 |
|------|------|------|
| `market` | String | 마켓 코드 |
| `trade_price` | Double | 현재가 (최근 체결가) |
| `prev_closing_price` | Double | 전일 종가 (UTC 00:00 기준) |
| `change` | String | 전일 대비 상태: `"RISE"` / `"FALL"` / `"EVEN"` |
| `change_price` | Double | 전일 종가 대비 변화액 (**절대값**) |
| `change_rate` | Double | 전일 종가 대비 변화율 (**절대값**) |
| `signed_change_price` | Double | 전일 종가 대비 변화액 (부호 포함) |
| `signed_change_rate` | Double | 전일 종가 대비 변화율 (부호 포함) |
| `high_price` | Double | 24h 최고가 |
| `low_price` | Double | 24h 최저가 |
| `trade_volume` | Double | 최근 체결량 |
| `acc_trade_price` | Double | 누적 거래대금 (UTC 00:00 기준) |
| `acc_trade_price_24h` | Double | 24시간 누적 거래대금 |
| `acc_trade_volume` | Double | 누적 거래량 (UTC 00:00 기준) |
| `acc_trade_volume_24h` | Double | 24시간 누적 거래량 |
| `trade_date_kst` | String | 최근 체결일 (KST, yyyyMMdd) |
| `trade_time_kst` | String | 최근 체결시각 (KST, HHmmss) |
| `timestamp` | Long | 현재가 반영 시각 (ms) |

> ⚠️ UI 표시 시 `change_rate` / `change_price` 대신 반드시 `signed_change_rate` / `signed_change_price` 사용.
> 색상 판별은 `change` 필드 기준: `"RISE"` → Red, `"FALL"` → Blue, `"EVEN"` → 기본색.

#### 6.3 데이터 조합 흐름 (coin-list)

```
1. GET /v1/market/all
   → 전체 마켓 목록 수신
   → "KRW-" 접두사로 필터링 → KRW 마켓 코드 목록 확보

2. GET /v1/ticker?markets={KRW 마켓 코드 전체, 쉼표 구분}
   → 현재가/변동률/거래대금 수신

3. market 코드 기준으로 두 응답을 조인
   → Coin 도메인 모델로 변환 (korean_name + trade_price + signed_change_rate + ...)

4. acc_trade_price_24h 내림차순 정렬 후 UI 반영
```

---

### ⚠️ 주의사항 (절대 금지)
1.  `runCatching` 내부에서 `suspend` 함수를 직접 호출하지 않는다. (CancellationException swallow 방지)
2.  Data 레이어에서 UI 관련 클래스(Context 등)를 직접 참조하지 않는다.
3.  WebSocket 응답을 처리할 때 에러가 발생해도 전체 스트림이 종료되지 않도록 `catch` 블록을 적절히 위치시킨다.