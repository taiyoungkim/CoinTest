# CLAUDE.md

이 프로젝트는 Upbit 공개 API를 활용한 암호화폐 종목 리스트 + 실시간 호가창 Android 앱입니다.

## 프로젝트 개요

- **앱 이름**: CoinTest
- **패키지**: com.tydev.cointest
- **최소 SDK**: 26 (Android 8.0)
- **타겟 SDK**: 35
- **언어**: Kotlin
- **UI**: Jetpack Compose + Material3

## 아키텍처

MVI + Clean Architecture + 멀티모듈

### 모듈 구조
```
:app                    → :feature:list, :feature:orderbook, :data (DI 연결)
:feature:list           → :domain
:feature:orderbook      → :domain
:data                   → :domain, :core:network
:core:network           → (Ktor만)
:domain                 → (순수 Kotlin, Android 의존성 없음)
```

### 의존성 방향 (절대 위반 금지)
- feature → domain (O)
- feature → data (X)
- feature → core:network (X)
- data → domain (O)
- data → core:network (O)
- domain → 아무것도 의존하지 않음

## 기술 스택

| 영역 | 라이브러리 |
|------|-----------|
| Network (HTTP + WebSocket) | Ktor Client 3.x (OkHttp 엔진) |
| JSON | kotlinx.serialization |
| DI | Hilt |
| Navigation | Compose Navigation (Type-safe, Kotlin Serialization) |
| Async | Kotlin Coroutines + Flow |
| Lifecycle | lifecycle-runtime-compose (collectAsStateWithLifecycle) |

## 📚 도메인별 상세 규칙 (작업 시 반드시 해당 문서를 먼저 읽을 것)
AI, 너는 코드를 작성하거나 수정하기 전에 현재 작업하는 영역에 맞는 아래 문서를 반드시 먼저 참조해야 한다.

- **UI 구현 시**:`docs/UI_SPEC.md`
- **MVI 구현 시**:`docs/MVI_GUIDE.md`
- **Kotlin Coding Convention**:`docs/KOTLIN_CONVENTION.md`
- **Network Rule**:`docs/NETWORK_RULE.md`

## 코딩 컨벤션

### 에러 처리
- Repository는 Result<T> 반환 (REST API)
- WebSocket은 Flow + catch 연산자
- CancellationException은 반드시 재전파 (catch에서 무시 금지)

## 커밋 컨벤션

```
feat: 새로운 기능 추가
fix: 버그 수정
refactor: 코드 리팩토링
docs: 문서 추가/수정
chore: 빌드, 설정 등 기타
style: 코드 포맷팅 (기능 변경 없음)
test: 테스트 추가/수정
```

## 주의사항

### 절대 하지 말 것
- squash commit (커밋 히스토리 유지 필수)
- feature 모듈에서 data/network 모듈 직접 참조
- collectAsState() 사용 (collectAsStateWithLifecycle 사용)
- runCatching을 suspend 함수에서 사용
- WebSocket 응답을 Text 프레임만 가정 (Binary도 처리)
- 하드코딩된 색상값 (Material3 테마 컬러 사용)

### 반드시 할 것
- 모든 Serializable DTO에 @SerialName 명시
- Json { ignoreUnknownKeys = true } 설정
- WebSocket combine 후 .conflate() 적용
- LazyColumn items에 key 지정
- SideEffect는 repeatOnLifecycle(STARTED)로 수집
- ViewModel.onCleared()에서 WebSocket 정리
- CancellationException은 catch에서 재전파