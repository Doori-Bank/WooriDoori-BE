# SSE 알림 전송 가이드

## 서버에서 SSE 이벤트로 알림 전송하는 방법

### 1. SseService 주입받기

```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final SseService sseService;
    
    // ... 서비스 로직
}
```

### 2. 특정 사용자에게 알림 전송

```java
// 단순 문자열 전송
sseService.sendToUser(memberId, "notification", "새로운 알림이 있습니다!");

// 객체 전송 (JSON으로 자동 변환됨)
Map<String, Object> data = new HashMap<>();
data.put("title", "목표 달성!");
data.put("message", "축하합니다!");
data.put("type", "achievement");

sseService.sendToUser(memberId, "notification", data);
```

### 3. 모든 사용자에게 브로드캐스트

```java
sseService.broadcast("announcement", "시스템 점검이 예정되어 있습니다.");
```

### 4. 실제 사용 예시 (GoalServiceImpl 참고)

```java
// 점수 계산 후 알림 전송
GoalScoreResponseDto result = calculateScore(memberId);

try {
    sseService.sendToUser(memberId, "scoreUpdated", result);
    log.info("SSE 점수 업데이트 알림 전송 - memberId: {}", memberId);
} catch (Exception e) {
    log.warn("SSE 알림 전송 실패 (무시) - memberId: {}", memberId, e);
    // SSE 실패해도 메인 로직은 계속 진행
}
```

## 이벤트 타입 예시

- `notification`: 일반 알림
- `scoreUpdated`: 점수 업데이트
- `goalAchieved`: 목표 달성
- `announcement`: 공지사항
- `ping`: 하트비트 (자동 전송)

## 프론트엔드에서 받는 방법

프론트엔드의 `useSSEConnection` 훅에서 이벤트를 받을 수 있습니다:

```typescript
useSSEConnection({
  handlers: {
    onCustomEvent: (eventName, data) => {
      if (eventName === 'notification') {
        // 알림 처리
        console.log('알림:', data);
      } else if (eventName === 'scoreUpdated') {
        // 점수 업데이트 처리
        console.log('점수 업데이트:', data);
      }
    },
  },
});
```

