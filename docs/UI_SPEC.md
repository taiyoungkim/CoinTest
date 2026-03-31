# 코인 거래소 앱 UI/UX 및 개발 스펙 문서 (Jetpack Compose 기반)

## 0. 공통 규칙 및 상태 처리 (Common Rules & State)

### 0.1. Compose 개발 공통 컨벤션 (필수)
* **Stateless Composable 원칙:** 모든 Screen 컴포저블은 자체적으로 상태를 가지지 않으며(Stateless), 상태(`UiState`)와 사용자 액션 이벤트 콜백(`onEvent`)을 ViewModel로부터 인자로 주입받아 구현합니다.
* **Modifier 규칙:** 모든 커스텀 컴포저블 함수의 첫 번째 선택적(Optional) 파라미터는 반드시 `Modifier`로 지정하여 외부에서 레이아웃을 조정할 수 있도록 합니다.
* **Preview 렌더링 최적화:** 모든 Preview 함수는 `@Preview` 어노테이션과 함께 반드시 `private` 접근 제한자를 사용하여, 불필요한 퍼블릭 클래스 생성을 방지하고 컴파일 타임을 최적화합니다.
* **Lifecycle Aware 수집:** ViewModel의 StateFlow/SharedFlow를 UI에서 수집할 때는 반드시 `collectAsStateWithLifecycle()`을 사용합니다. (`collectAsState` 금지, 백그라운드 전환 시 불필요한 웹소켓 및 UI 리소스 낭비 방지)

### 0.2. 공통 UI 컴포넌트
* **Loading State:** 데이터를 불러오는 중일 때 화면 중앙에 Material3 `CircularProgressIndicator` 표시.
* **Error State:** 네트워크 에러 또는 데이터 파싱 실패 시, 에러 메시지 텍스트와 함께 중앙에 `재시도(Retry) 버튼` 배치. 단발성 에러(예: WebSocket 일시 끊김)의 경우 `Snackbar`를 통해 SideEffect로 알림 처리.
* **디자인 시스템:** Material3 테마 기반. 색상은 하드코딩을 지양하고 Theme 컬러를 우선하되, 주식/코인 도메인 특화 색상(상승 Red, 하락 Blue)은 예외적으로 지정하여 사용.

---

## 1. 종목 리스트 화면 (`CoinListScreen`)

### 1.1. 화면 구성 (UI Layout)
* **Top App Bar:** 화면 타이틀 ("거래소")
* **List Header:** 정렬 기준을 나타내는 헤더 행 (종목명, 현재가, 전일대비)
* **List Area (`LazyColumn`):** 코인 종목 리스트 출력 영역

### 1.2. 리스트 아이템 디자인 (`CoinListItem`)
* **좌측 (Left):** 종목명 (한글명 및 영문 심볼, 예: 비트코인 / BTC)
    * 한글명(`korean_name`)과 마켓 코드에서 추출한 심볼(예: "KRW-BTC" → "BTC")을 함께 표시.
* **중앙 (Center):** 현재가 (`trade_price`, 천 단위 콤마 포맷팅 적용)
* **우측 (Right):** 24h 변동률 및 변동폭
    * 변동률: `signed_change_rate` × 100 (%) — 부호 포함. ⚠️ `change_rate`는 절대값이므로 사용 금지.
    * 변동폭: `signed_change_price` — 부호 포함. ⚠️ `change_price`는 절대값이므로 사용 금지.
    * **색상 규칙:** `change` 필드 기준 — `"RISE"` → `Color.Red`, `"FALL"` → `Color.Blue`, `"EVEN"` → 테마 기본 텍스트 색상.

### 1.2.1. 기본 정렬 기준
* **기본값:** 24시간 누적 거래대금(`acc_trade_price_24h`) 내림차순.

### 1.3. 데이터 갱신 및 상호작용
* **초기 로드:** 화면 진입 시 REST API를 1회 호출하여 리스트 구성.
* **새로고침 UX (Pull-to-Refresh):** Material3의 `PullToRefreshContainer`를 적용하여 리스트 최상단에서 아래로 당기면 REST API 재호출.
* **클릭 이벤트:** 아이템 클릭 시 해당 종목의 `market` 코드(예: "KRW-BTC")를 네비게이션 인자로 전달하며 **[2. 호가창 상세 화면]**으로 이동.
* **렌더링 최적화 (필수):** `LazyColumn`의 `items` 블록에 반드시 `key = { coin.symbol }`을 지정하여, 리스트 순서가 바뀌거나 업데이트될 때 전체 재구성이 일어나는 것을 방지합니다.

---

## 2. 호가창 상세 화면 (`OrderBookScreen`)

### 2.1. 화면 구성 (UI Layout)
* **Top App Bar:** 선택한 코인의 종목명(예: "비트코인 KRW-BTC") 및 뒤로가기 버튼
* **Header Info:** 현재가, 24h 고가/저가, 거래대금 등 요약 정보 표시
* **Order Book Area (`LazyColumn`):** 호가 데이터 및 현재가를 스크롤 가능하게 통합 출력하는 영역

### 2.2. 호가창 리스트 구조 (Order Book Structure)
`LazyColumn` 내에서 상단부터 하단으로 다음 순서대로 아이템을 배치합니다. (위아래 최대 15호가, 총 30호가 기준). **이곳의 `items` 블록 역시 반드시 `key = { order.price }`를 지정하여 호가 갱신 시의 스크롤 튐 현상과 렌더링 부하를 막습니다.**

1.  **매도 호가 영역 (`AskPriceItem`)**
    * **위치:** 리스트 상단부 (가격 내림차순 배치)
    * **UI:** 텍스트 색상 `Color.Blue`, 배경에 수량 비례 Bar Chart (`Color.Blue.copy(alpha = 0.1f)`) 적용.
2.  **현재가 영역 (`CurrentPriceItem`)**
    * **위치:** 매도 호가와 매수 호가의 정중앙
    * **UI:** 직전 체결가(현재가)를 굵은 폰트로 표시.
    * **색상 규칙:** 전일 종가 대비 (현재가 > 전일 종가: `Color.Red`, 현재가 < 전일 종가: `Color.Blue`, 동일: 테마 기본 색상).
3.  **매수 호가 영역 (`BidPriceItem`)**
    * **위치:** 리스트 하단부 (가격 내림차순 배치)
    * **UI:** 텍스트 색상 `Color.Red`, 배경에 수량 비례 Bar Chart (`Color.Red.copy(alpha = 0.1f)`) 적용.
4.  **총 잔량 영역 (`OrderBookTotalItem`)**
    * **위치:** 호가창 리스트 최하단
    * **UI:** `total_ask_size`와 `total_bid_size`를 좌우로 나란히 표시.

### 2.3. 호가 아이템 상세 디자인 및 성능 최적화
* **가격/수량 필드:** 가격은 천 단위 콤마, 잔량은 소수점 포맷팅 적용.
* **Depth 시각화 (Bar Chart):** `Modifier.drawBehind`를 활용하여 캔버스 단에서 배경색 너비(`해당 호가 잔량 / 최대 잔량`)를 그립니다.
* **Recomposition 격리:** 최대 잔량이 변할 때마다 30개의 호가 아이템이 전부 리렌더링되지 않도록, 비율 계산 로직은 ViewModel에서 수행하여 넘겨주거나, 컴포저블 내부에서 `derivedStateOf`를 사용하여 상태 변화 인식을 격리합니다. 각 호가 Row 컴포저블의 파라미터는 불변(Immutable) 상태로 유지합니다.

### 2.4. 데이터 실시간 갱신 (Real-time Update)
* **동작 방식:** 진입 시 WebSocket을 연결하여 호가 스냅샷 및 업데이트 데이터를 스트리밍으로 수신합니다.
* **최적화 (Throttling):** Flow 수집 시 `sample()` 또는 `conflate()`를 활용하여 UI 업데이트 빈도를 조절(예: 100ms~200ms 단위)하여, 초당 수십 번의 데이터 유입으로 인한 UI 스레드 블로킹(버벅임)을 원천 차단합니다.