# 💰 bill-mate

CLAUDE CODE를 활용한 바이브 코딩 방식으로 개발한 Slack 기반 구독 관리 봇입니다.<br>
여러 서비스에 흩어져 있는 구독 정보를 일일이 확인해야 하는 번거로움을 줄이고자 시작했으며, 
<Br>Slack을 중심으로 구독 상태 조회부터 만료 알림, 해지 유도까지 한 번에 관리할 수 있도록 만들었습니다.

개발 과정에서는 CLAUDE CODE를 활용해 빠르게 기능을 구현하고, 반복적인 작업은 줄이면서 전체 구조와 비즈니스 로직 설계에 더 집중했습니다.<br>


## 주요 기능

- **구독 관리** — 카테고리별 구독 등록 / 조회 / 삭제 
- **결제일 알림** — D-7, D-3, D-1, 당일 자동 DM 알림
- **월간 리포트** — 카테고리별 지출 요약 + 파이차트 
- **결제 이력** — 구독별 과거 결제 기록 조회
- **공유 구독** — 구독 멤버 추가 및 분담금 관리
- **멀티 워크스페이스** — OAuth 기반 여러 Slack 워크스페이스 동시 지원

## Slack 인터랙션 플로우

### 구독 추가 (대화형 상태 머신)

```
사용자 DM 전송
    │
    ▼
[📋 구독 목록] ──▶ [➕ 구독 추가] 클릭
                        │
                        ▼
                  [카테고리 선택]
                  📺 동영상 │ 🎵 음악 │ 🤖 AI │ ✏️ 직접 입력
                        │
                        ▼
                  서비스명 텍스트 입력
                        │
                        ▼
                  [💳 금액 선택]
                  프리셋 버튼 또는 직접 입력
                        │
                        ▼
                  [📅 결제일 설정]
                  드롭다운 (1~31일)
                        │
                        ▼
                  ✅ 등록 완료 → 구독 목록
```

### 구독 관리

```
[📋 구독 목록]
    │
    ├── [관리 ▼] 드롭다운
    │       ├── 📜 결제 이력 → 과거 결제 기록
    │       ├── 🔔 알림 설정 → 알림 추가/삭제
    │       └── 🗑️ 삭제     → 삭제 확인 → 목록
    │
    ├── [➕ 구독 추가] → 카테고리 선택 플로우
    └── [📊 리포트]   → 월간 지출 리포트 발송
```

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.5.0 |
| Database | MySQL 8, Spring Data JPA / Hibernate |
| Slack | Slack Bolt SDK 1.44.2 (Bolt Jakarta Servlet) |
| Chart | QuickChart.io REST API |
| Infra | Docker Compose, Gradle |
| Test | JUnit 5, Mockito, AssertJ, H2 (in-memory) |

## 시작하기

### 사전 준비

- Java 21
- MySQL 8 (port `3555`, database `billmate`, user `root`, password `password`)
- [ngrok](https://ngrok.com) (Slack이 로컬 서버에 요청을 보낼 수 있도록 터널링 필요)

### 1. Slack App 생성 및 크레덴셜 확인

[api.slack.com/apps](https://api.slack.com/apps) 에서 **Create New App → From scratch** 로 앱을 생성합니다.

생성 후 **Basic Information → App Credentials** 에서 아래 값을 복사해 둡니다.

- **Signing Secret** → `SLACK_SIGNING_SECRET`
- **Client ID** → `SLACK_CLIENT_ID`
- **Client Secret** → `SLACK_CLIENT_SECRET`

### 2. 환경변수 설정

```bash
export SLACK_SIGNING_SECRET=your_signing_secret
export SLACK_CLIENT_ID=your_client_id
export SLACK_CLIENT_SECRET=your_client_secret
```

### 3. ngrok 실행

[ngrok 대시보드](https://dashboard.ngrok.com/get-started/your-authtoken)에서 Authtoken을 발급받아 등록합니다.

```bash
ngrok config add-authtoken your_authtoken
```

터널을 엽니다.

```bash
ngrok http 8080
```

`https://xxxx-xxxx.ngrok-free.app` 형태의 URL이 생성됩니다. 이 URL을 아래 Slack App 설정에 사용합니다.

> **주의:** ngrok을 재시작하면 URL이 바뀝니다. URL이 바뀔 때마다 아래 Slack App의 모든 URL 설정을 다시 업데이트해야 합니다.

### 4. 앱 실행

```bash
./gradlew bootRun
```

> **주의:** Slack은 URL 등록 시 실제로 검증 요청을 보냅니다. **ngrok과 앱이 모두 실행 중인 상태**에서 아래 Slack App URL 설정을 진행하세요.

### 5. Slack App 설정

아래 항목들을 순서대로 설정합니다. `https://xxxx-xxxx.ngrok-free.app` 부분은 실제 ngrok URL로 교체하세요.

**① OAuth & Permissions**

*Redirect URLs* 에 추가:
```
https://xxxx-xxxx.ngrok-free.app/slack/oauth/callback
```

*Bot Token Scopes* 에 아래 권한 추가:

| Scope | 용도 |
|-------|------|
| `chat:write` | 메시지 전송 |
| `im:read` | DM 채널 조회 |
| `im:write` | DM 채널 열기 |
| `im:history` | DM 메시지 읽기 |
| `commands` | 슬래시 커맨드 |



**② Event Subscriptions**

*Enable Events* 를 ON으로 변경 후 Request URL 입력:
```
https://xxxx-xxxx.ngrok-free.app/slack/events
```

*Subscribe to bot events* 에 추가:
- `message.im` — 사용자 DM 수신



**③ Interactivity & Shortcuts**

*Interactivity* 를 ON으로 변경 후 Request URL 입력:
```
https://xxxx-xxxx.ngrok-free.app/slack/events
```

> 버튼 클릭 등 Block Actions도 이 URL로 전달됩니다.



**④ Slash Commands**

*Create New Command* 로 `/billmate` 커맨드 추가:

| 항목 | 값 |
|------|----|
| Command | `/billmate` |
| Request URL | `https://xxxx-xxxx.ngrok-free.app/slack/command` |
| Short Description | 구독 관리 봇 |



**⑤ App Home**

*Messages Tab* 섹션에서 **Allow users to send Slash commands and messages from the messages tab** 를 ON으로 변경합니다.

> 이 설정이 없으면 사용자가 봇에게 DM을 보낼 수 없어 대화형 인터페이스가 동작하지 않습니다.


**⑥ Manage Distribution**

*Manage Distribution* 페이지에서 **Activate Public Distribution** 을 활성화합니다.

> OAuth 기반 설치 플로우(`/slack/install`)가 동작하려면 반드시 필요합니다.


### 6. 워크스페이스 설치

브라우저에서 아래 URL에 접속해 Slack 워크스페이스에 앱을 설치합니다.

```
https://xxxx-xxxx.ngrok-free.app/slack/install
```

설치 완료 후 Slack에서 봇을 검색해 DM을 시작하면 구독 목록 화면이 표시됩니다.



## 아키텍처
![아키텍처](/docs/image/architecture.png)



## Demo
[Demo 확인하기](https://youtu.be/LvoNzLt9rJ8)
