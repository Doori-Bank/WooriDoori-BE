## SSE 백그라운드 동작 개선 완료

### ✅ **개선 사항**

1. **하트비트(Heartbeat) 추가**
   - 30초마다 `ping` 이벤트 전송
   - 백그라운드에서도 연결 유지
   - 브라우저가 연결을 끊지 않도록 유지

2. **자동 재연결 로직** (프론트엔드에서 구현 필요)

### 📱 **백그라운드 동작 특성**

#### ✅ **동작하는 경우:**
- **데스크톱 브라우저**: 탭이 백그라운드에 있어도 연결 유지 (하트비트 덕분)
- **모바일 브라우저 (포그라운드)**: 정상 동작
- **PWA (Progressive Web App)**: Service Worker와 함께 사용 시 더 안정적

#### ⚠️ **제한사항:**
- **모바일 앱 백그라운드**: iOS/Android는 앱이 백그라운드로 가면 네트워크 연결을 제한할 수 있음
- **배터리 절약 모드**: 일부 브라우저는 배터리 절약 모드에서 연결을 끊을 수 있음

### 💻 **프론트엔드 재연결 로직 예시**

```javascript
class SSEClient {
  constructor(url, token) {
    this.url = url;
    this.token = token;
    this.eventSource = null;
    this.reconnectDelay = 1000; // 1초부터 시작
    this.maxReconnectDelay = 30000; // 최대 30초
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = Infinity; // 무한 재시도
  }

  connect() {
    this.eventSource = new EventSource(this.url, {
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    this.eventSource.addEventListener('connect', (event) => {
      console.log('SSE 연결 성공:', event.data);
      this.reconnectDelay = 1000; // 재연결 성공 시 딜레이 초기화
      this.reconnectAttempts = 0;
    });

    this.eventSource.addEventListener('ping', (event) => {
      // 하트비트 수신 (백그라운드 연결 확인)
      console.log('하트비트 수신:', event.data);
    });

    this.eventSource.addEventListener('scoreUpdated', (event) => {
      const data = JSON.parse(event.data);
      console.log('점수 업데이트:', data);
      // UI 업데이트
    });

    this.eventSource.onerror = (error) => {
      console.error('SSE 에러:', error);
      this.eventSource.close();
      
      // 재연결 시도
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++;
        const delay = Math.min(
          this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
          this.maxReconnectDelay
        );
        
        console.log(`${delay}ms 후 재연결 시도... (${this.reconnectAttempts}회)`);
        
        setTimeout(() => {
          this.connect();
        }, delay);
      }
    };
  }

  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}

// 사용 예시
const sseClient = new SSEClient(
  'http://localhost:8080/sse/connect',
  'YOUR_JWT_TOKEN'
);

sseClient.connect();

// 페이지가 보일 때 재연결 (백그라운드에서 돌아왔을 때)
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && !sseClient.eventSource) {
    // 페이지가 다시 보이면 재연결
    sseClient.connect();
  }
});
```

### 🔧 **추가 개선 방안**

1. **Service Worker 사용** (PWA)
   - 백그라운드에서도 더 안정적으로 동작
   - 오프라인 지원 가능

2. **Web Push API** (모바일 백그라운드)
   - 앱이 완전히 종료되어도 알림 수신 가능
   - 더 나은 사용자 경험

3. **폴링(Polling) 폴백**
   - SSE 실패 시 자동으로 폴링으로 전환
   - 안정성 향상

### 📊 **현재 구현 상태**

✅ 하트비트로 백그라운드 연결 유지  
✅ 자동 재연결 로직 (프론트엔드 구현 필요)  
✅ 에러 처리 및 타임아웃 관리  

**결론**: SSE는 백그라운드에서도 동작하지만, 하트비트와 재연결 로직을 추가하면 더 안정적으로 동작합니다!

