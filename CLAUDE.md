# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# 인프라 실행 (Kafka, Redis, Elasticsearch)
docker compose up -d

# 빌드
./gradlew build

# 앱 실행 (토스페이먼츠 키 적용 시 secret 프로파일 필수)
./gradlew bootRun --args='--spring.profiles.active=secret'

# 테스트
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.demo.search.payment.PaymentServiceTest"
```

## 환경 설정

토스페이먼츠 테스트 키는 `src/main/resources/application-secret.yml`에 입력 (gitignore 등록됨):

```yaml
toss:
  payments:
    client-key: test_ck_...
    secret-key: test_sk_...
```

`application.yml`의 `toss.payments` 키는 환경변수(`TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY`) 또는 secret 프로파일로만 주입. 직접 값을 넣으면 git에 노출되므로 금지.

## 아키텍처

### 데이터 흐름

**상품 등록 → 검색 인덱싱:**
```
POST /products
  → Product 저장 (H2)
  → ProductEventProducer: Kafka "product.created" 발행
  → ProductEventConsumer: 수신 후 ProductDocument를 ES 인덱싱
```

**검색 + 인기검색어:**
```
GET /search?keyword=
  → ElasticsearchRepository로 풀텍스트 검색
  → SearchRankService: Redis ZSet에 검색어 score +1
GET /search/rank → Redis ZSet reverseRange로 Top 10 반환
```

**결제 플로우:**
```
/payment.html (브라우저)
  → POST /orders: 주문 생성 (H2, status=PENDING)
  → 토스페이먼츠 결제창 (JS SDK)
  → GET /payment/success?paymentKey=&orderId=&amount= (리다이렉트)
  → PaymentService.confirm(): 금액검증 → 멱등성체크 → PG승인 → DB저장
  → Order.status PENDING → PAID
```

### 패키지 구조

- `product/` — 상품 등록, JPA Entity, Kafka Producer
- `event/` — Kafka Consumer (상품 이벤트 수신 → ES 인덱싱)
- `es/` — Elasticsearch Document, Repository, 검색 API
- `rank/` — Redis ZSet 인기검색어 집계
- `order/` — 주문 생성/조회, 상태 머신 (PENDING → PAID → CANCELLED)
- `payment/` — 토스페이먼츠 연동, 결제 승인/취소, 멱등성 처리

### 핵심 설계 포인트

**멱등성 (PaymentService.confirm):**
결제 승인 전 `paymentRepository.findByOrderId()`로 중복 체크. 이미 처리된 요청이면 기존 결과 반환.

**금액 검증:**
DB의 `order.amount`와 요청의 `amount`를 비교. 프론트에서 금액을 조작해도 서버에서 차단.

**JPA Entity:**
`allOpen` 플러그인으로 `@Entity` 클래스를 자동으로 open 처리 (Kotlin 기본이 final이라 프록시 생성에 필요).

**WebClient (동기 사용):**
토스페이먼츠 API 호출 시 `.block()`으로 동기 처리. WebFlux 의존성은 WebClient 사용을 위해서만 추가됨 (서버는 MVC).

## 인프라 포트

| 서비스 | 포트 |
|---|---|
| Spring Boot 앱 | 8080 |
| Kafka | 9092 |
| Redis | 6379 |
| Elasticsearch | 9200 |
