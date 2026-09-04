# HomeSweetHome 단체 In-App Notification Fan-out on Read 최소 설계

- 상태: Draft v3.0
- 대상 시스템: HomeSweetHome Notification
- 대상 규모: MAU 3,000,000
- 채널: In-App only
- 작성일: 2026-08-27

## 1. 목적

현재 개인·다중 사용자 알림은 대상 사용자마다 user_notification을 생성한다.

이 문서의 목적은 **전체 사용자 또는 연속된 사용자 ID 범위에 보내는 단체 알림만 fan-out on read로 변경**하는 것이다.

~~~text
기존 단체 알림
대상 사용자 조회
→ 사용자 수만큼 user_notification INSERT
→ 사용자별 Redis 메시지 발행

변경 후 단체 알림
broadcast_notification 1건 INSERT
→ 사용자가 알림함을 조회할 때 대상 여부 판정
~~~

기존 개인 알림과 소규모 임의 사용자 목록 알림은 변경하지 않는다.

## 2. 범위

### 2.1 구현 범위

- 전체 사용자 단체 알림
- 하나의 연속된 사용자 ID 범위 단체 알림
- 단체 알림 즉시 발행
- 개인 알림과 단체 알림의 통합 조회
- 단체 알림 사용자별 읽음·삭제
- 기존 개인 알림 경로 유지
- 기존 임의 사용자 목록 발송 경로의 건수 상한 유지

### 2.2 구현하지 않는 것

- READY, PUBLISHED, CANCELED 상태 모델
- 생성과 발행의 분리
- 예약 발행
- 재시도와 전송 보장
- Idempotency-Key와 request hash
- Transactional Outbox
- 단체 알림 전용 Kafka Topic
- Redis/SSE 실시간 갱신
- Caffeine 또는 별도 단체 알림 Cache
- Opaque Cursor와 pagination API 재설계
- 단체 알림 취소와 만료
- 등급·지역 등 조건형 Segment
- 불연속 사용자 ID 목록 Snapshot
- 사용자별 전달 성공 이력
- 별도 마이크로서비스

위 기능은 실제 요구나 성능 문제가 확인될 때 추가한다.

## 3. 알림 유형과 진입 경로

### 3.1 개인 알림

한 사용자에게 보내는 알림은 기존 경로를 그대로 사용한다.

~~~text
개인 이벤트
→ user_notification 생성
→ 사용자별 Redis 메시지 발행
→ 기존 알림함 조회
~~~

### 3.2 소규모 임의 사용자 목록

TemplateNotificationEvent.userIds와 CustomNotificationEvent.userIds처럼 임의의 사용자 ID 목록을 받는 기존 경로도 유지한다.

이 목록을 최솟값~최댓값 범위로 변환하면 목록에 없던 사용자까지 알림을 받으므로 단체 알림 경로로 자동 변환하지 않는다.

기존 다중 사용자 경로에는 현재의 허용 건수 상한을 유지한다.

### 3.3 단체 알림

다음 두 대상만 새 fan-out on read 경로를 사용한다.

~~~text
ALL             단체 알림 생성 시점까지 가입한 전체 사용자
USER_ID_RANGE   단체 알림 생성 시점까지 가입한 사용자 중 하나의 연속 ID 범위
~~~

단체 알림은 명시적인 전용 메서드나 API로만 생성한다.

~~~text
sendTemplateBroadcast(audience, notification)
sendCustomBroadcast(audience, notification)
~~~

기존 userIds 목록의 크기를 보고 단체 알림 여부를 추론하지 않는다.

## 4. 대상 판정

단체 알림은 생성과 동시에 발행된다. DB가 기록한 broadcast_notification.created_at을 발행 시각이자 대상 기준 시각으로 사용한다.

~~~text
user.created_at <= broadcast_notification.created_at
AND (
    audience_type = ALL
    OR target_min_user_id <= userId <= target_max_user_id
)
~~~

규칙:

- ALL은 target_min_user_id와 target_max_user_id를 저장하지 않는다.
- USER_ID_RANGE는 요청한 최소·최대 ID를 그대로 저장한다.
- 현재 MAX(user_id) 조회와 상한 제한은 하지 않는다.
- 생성 시 대상 사용자 수를 세거나 사용자를 조회하지 않는다.
- 미래 가입자는 user.created_at 조건으로 제외한다.
- 가입 transaction의 commit 순서가 아니라 DB가 기록한 users.created_at을 기준으로 한다.

users.created_at은 다음 조건을 만족해야 한다.

- DB가 UTC DATETIME(6)으로 생성
- 애플리케이션에서 값을 지정하거나 수정하지 않음
- 모든 MySQL connection time zone은 UTC

~~~sql
ALTER TABLE users
    MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
~~~

마이그레이션은 UTC session에서 실행하고 기존 데이터 변환 결과를 확인한다.

## 5. 데이터베이스

### 5.1 broadcast_notification

기존 user_notification과 같은 메시지 표현을 재사용한다.

- 템플릿 알림: notification_template_id와 context_data
- 커스텀 알림: notification_template_id = NULL, context_data에 제목·내용·URL 포함

~~~sql
CREATE TABLE broadcast_notification (
    broadcast_notification_id BIGINT NOT NULL AUTO_INCREMENT,
    notification_template_id BIGINT NULL,
    audience_type VARCHAR(20) NOT NULL,
    target_min_user_id BIGINT NULL,
    target_max_user_id BIGINT NULL,
    context_data JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (broadcast_notification_id),
    KEY idx_broadcast_notification_feed
        (created_at DESC, broadcast_notification_id DESC),
    CONSTRAINT fk_broadcast_notification_template
        FOREIGN KEY (notification_template_id)
        REFERENCES notification_template (notification_template_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_broadcast_notification_audience
        CHECK (
            (
                audience_type = 'ALL'
                AND target_min_user_id IS NULL
                AND target_max_user_id IS NULL
            )
            OR
            (
                audience_type = 'USER_ID_RANGE'
                AND target_min_user_id >= 1
                AND target_max_user_id >= target_min_user_id
            )
        )
);
~~~

단체 알림 생성 시 이 테이블에 한 행만 저장한다.

### 5.2 user_broadcast_state

사용자가 단체 알림을 읽거나 삭제할 때만 상태 행을 생성한다.

~~~sql
CREATE TABLE user_broadcast_state (
    user_id BIGINT NOT NULL,
    broadcast_notification_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, broadcast_notification_id),
    KEY idx_user_broadcast_state_notification
        (broadcast_notification_id, user_id),
    CONSTRAINT fk_user_broadcast_state_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_broadcast_state_notification
        FOREIGN KEY (broadcast_notification_id)
        REFERENCES broadcast_notification (broadcast_notification_id)
        ON DELETE CASCADE
);
~~~

상태 의미:

~~~text
행 없음                         읽지 않음, 삭제하지 않음
is_read = true                  읽음
is_deleted = true               알림함에서 삭제
~~~

단체 알림 생성 시에는 user_broadcast_state를 생성하지 않는다.

## 6. 단체 알림 생성

### 6.1 요청 검증

ALL:

- minUserId, maxUserId를 받지 않는다.

USER_ID_RANGE:

- minUserId, maxUserId를 모두 요구한다.
- minUserId >= 1
- minUserId <= maxUserId

공통:

- 템플릿 알림은 기존 템플릿 조회와 검증을 재사용한다.
- 커스텀 알림은 기존 title, content, redirectUrl, contextData 검증을 재사용한다.
- context_data는 null을 허용하지 않는다.
- 관리자 API가 필요하면 기존 인증·권한 설정을 재사용한다.

### 6.2 처리

~~~text
1. 입력 검증
2. 기존 템플릿 또는 커스텀 메시지 데이터 구성
3. broadcast_notification 1건 INSERT
4. transaction commit
~~~

하지 않는 것:

- 대상 사용자 조회
- MAX(user_id) 조회
- 대상 사용자 수 계산
- user_notification INSERT
- 사용자별 Redis 메시지 발행

DB commit이 성공하면 단체 알림은 다음 알림함 조회부터 보인다.

## 7. 통합 알림함 조회

기존 알림함의 최신 20건 응답 계약을 유지한다.

~~~text
1. 인증 principal에서 userId와 userCreatedAt 확인
2. 기존 방식으로 개인 알림 최신 20건 조회
3. 대상이며 삭제하지 않은 단체 알림 최신 20건 조회
4. 개인·단체 목록을 createdAt 최신순으로 병합
5. 상위 20건 반환
~~~

각 목록에서 20건씩 조회하면 통합 상위 20건을 결정할 수 있다.

공통 정렬:

~~~text
createdAt DESC,
notificationTypeRank DESC,
notificationId DESC
~~~

notificationTypeRank는 같은 시각의 순서만 고정한다.

~~~text
PERSONAL = 1
BROADCAST = 0
~~~

단체 알림 조회 조건:

~~~sql
SELECT b.*
FROM broadcast_notification b
LEFT JOIN user_broadcast_state s
    ON s.broadcast_notification_id = b.broadcast_notification_id
   AND s.user_id = :userId
WHERE :userCreatedAt <= b.created_at
  AND (
      b.audience_type = 'ALL'
      OR :userId BETWEEN b.target_min_user_id AND b.target_max_user_id
  )
  AND (s.user_id IS NULL OR s.is_deleted = FALSE)
ORDER BY b.created_at DESC, b.broadcast_notification_id DESC
LIMIT 20;
~~~

템플릿과 카테고리는 기존 개인 알림 조회처럼 함께 조회해 N+1을 만들지 않는다.

### 7.1 응답 식별자

두 테이블의 숫자 ID가 겹칠 수 있으므로 알림 출처를 함께 반환한다.

~~~json
{
  "notificationType": "BROADCAST",
  "notificationId": 1004,
  "createdAt": "2026-08-27T12:00:00Z"
}
~~~

허용 타입:

~~~text
PERSONAL
BROADCAST
~~~

기존 개인 알림 응답에는 notificationType = PERSONAL을 추가한다.

## 8. 단체 알림 읽음·삭제

기존 개인 알림 상태 변경 API와 로직은 변경하지 않는다.

단체 알림에는 별도 API를 추가한다.

~~~http
PATCH /api/v1/notifications/broadcast/{broadcastNotificationId}/read
DELETE /api/v1/notifications/broadcast/{broadcastNotificationId}
~~~

사용자 ID는 요청값이 아니라 인증 principal에서 가져온다.

상태 변경 전에 현재 사용자가 해당 단체 알림의 대상인지 확인한다.

읽음:

~~~sql
INSERT INTO user_broadcast_state (
    user_id,
    broadcast_notification_id,
    is_read,
    is_deleted
) VALUES (?, ?, TRUE, FALSE)
ON DUPLICATE KEY UPDATE
    is_read = TRUE;
~~~

삭제:

~~~sql
INSERT INTO user_broadcast_state (
    user_id,
    broadcast_notification_id,
    is_read,
    is_deleted
) VALUES (?, ?, TRUE, TRUE)
ON DUPLICATE KEY UPDATE
    is_read = TRUE,
    is_deleted = TRUE;
~~~

반복 요청은 같은 결과를 반환한다.

## 9. 성능 기준

단체 알림 생성 비용:

~~~text
DB write 1건
사용자 조회 0건
user_notification write 0건
user_broadcast_state write 0건
~~~

알림함 조회에는 기존 개인 알림 쿼리에 단체 알림 쿼리 1건이 추가된다.

초기 구현에서는 Cache를 추가하지 않는다. 운영 환경에서 알림함 조회 지연이 실제로 증가할 때 기존 Cache 인프라를 재사용한다.

user_broadcast_state는 읽음·삭제 사용자 수만큼만 증가한다. 행 증가량을 관측하되, 초기 구현에 별도 정리 작업이나 watermark를 추가하지 않는다.

## 10. 장애 범위

| 상황 | 동작 |
|---|---|
| 단체 알림 INSERT 실패 | 단체 알림이 생성되지 않음 |
| 단체 알림 INSERT 성공 | 다음 알림함 조회부터 노출 |
| 읽음·삭제 Upsert 실패 | 상태가 바뀌지 않으며 요청 실패 |
| 애플리케이션 재시작 | MySQL에서 그대로 조회 |

MySQL이 단체 알림과 사용자 상태의 원본이다.

재시도, 중복 생성 방지, 실시간 신호 전송 보장은 현재 범위가 아니다.

## 11. 테스트

### 11.1 필수 통합 테스트

1. ALL 단체 알림 생성 시 broadcast_notification 한 행만 생성된다.
2. 단체 알림 생성 시 user_notification과 user_broadcast_state가 생성되지 않는다.
3. 단체 알림 생성 이후 가입자는 해당 알림을 조회하지 못한다.
4. USER_ID_RANGE의 최소·최대 경계 사용자는 대상에 포함된다.
5. USER_ID_RANGE 밖 사용자는 대상에 포함되지 않는다.
6. 개인·단체 알림이 createdAt 기준으로 병합되어 최신 20건만 반환된다.
7. 한 사용자의 읽음·삭제가 다른 사용자에게 영향을 주지 않는다.
8. 단체 알림 읽음·삭제 반복 요청의 결과가 같다.
9. 기존 단일 사용자 알림 경로가 그대로 동작한다.
10. 기존 소규모 임의 사용자 목록 알림이 목록에 포함된 사용자에게만 생성된다.

### 11.2 최소 성능 확인

- 사용자 수와 관계없이 단체 알림 생성 SQL이 한 번만 실행되는지 확인
- 운영 데이터 규모에서 단체 알림 조회 쿼리의 EXPLAIN 확인
- 통합 알림함 p95가 기존 대비 허용 범위인지 측정

## 12. 구현 순서

1. users.created_at 조건 확인과 DB migration
2. broadcast_notification, user_broadcast_state migration
3. 명시적인 단체 알림 생성 경로
4. 기존 알림함에 단체 알림 조회·병합 추가
5. 단체 알림 읽음·삭제 API
6. 기존 다중 사용자 경로 건수 상한 확인
7. 필수 통합 테스트와 실행 계획 확인

## 13. Definition of Done

- 단체 알림 한 번에 broadcast_notification 한 건만 생성한다.
- 단체 알림 생성 시 사용자별 user_notification을 생성하지 않는다.
- 단체 알림 생성 시 사용자별 상태 row를 생성하지 않는다.
- 기존 개인 알림 저장·Redis 발행 경로를 변경하지 않는다.
- 생성 이후 가입한 사용자는 과거 단체 알림을 보지 않는다.
- USER_ID_RANGE 밖 사용자는 단체 알림을 보지 않는다.
- 개인·단체 알림이 기존 알림함에 함께 최신순으로 표시된다.
- 사용자별 읽음·삭제 상태가 다른 사용자와 격리된다.
- 기존 임의 사용자 목록을 연속 ID 범위로 잘못 변환하지 않는다.
- 단체 알림 생성 비용이 사용자 수에 비례하지 않는다.

## 14. 후속 기능 추가 기준

| 관측된 요구 | 추가할 기능 |
|---|---|
| 관리자 초안·승인 필요 | DRAFT 또는 READY 상태 |
| 예약 발행 필요 | 발행 시각과 예약 처리 |
| 중복 생성이 실제 문제 | Idempotency-Key |
| DB commit 후 이벤트 전달 보장 필요 | Transactional Outbox |
| 즉시 화면 갱신 필요 | 단일 Redis/SSE 갱신 신호 |
| 알림함 조회 지연 증가 | 기존 Cache 인프라 재사용 |
| 페이지 조회 필요 | 기존 API 요구에 맞는 Cursor |
| 불연속 대상 필요 | 별도 Audience 모델 |
| 사용자 상태 증가가 용량 문제 | 보관 정책 또는 읽음 watermark |

현재 단계에서는 위 기능을 미리 구현하지 않는다.
