# demo-search

Kafka · Redis · Elasticsearch · 토스페이먼츠를 학습하기 위한 Spring Boot 데모 프로젝트입니다.

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Kotlin |
| Framework | Spring Boot 3.2, Spring Batch, Spring WebFlux |
| Messaging | Apache Kafka |
| Search | Elasticsearch |
| Cache | Redis |
| Database | H2 (In-Memory) |
| Payment | 토스페이먼츠 |
| Build | Gradle (Kotlin DSL) |

## 주요 기능

### 상품 검색 (Kafka + Elasticsearch + Redis)
- 상품 등록 시 Kafka 이벤트 발행 → Consumer가 Elasticsearch 인덱싱
- Elasticsearch 풀텍스트 검색 (키워드, 카테고리)
- 검색 시 Redis ZSet으로 인기검색어 실시간 집계

### 결제 (토스페이먼츠)
- 주문 생성 → 결제창 호출 → 결제 승인 플로우
- 금액 검증으로 이중 결제 방지
- 멱등성 처리 (중복 요청 안전)
- 결제 취소

## 실행 방법

### 1. 사전 준비

- JDK 17
- Docker Desktop

### 2. 토스페이먼츠 테스트 키 설정

[토스페이먼츠 개발자센터](https://developers.tosspayments.com)에서 테스트 키 발급 후
`src/main/resources/application-secret.yml` 파일 생성:

```yaml
toss:
  payments:
    client-key: test_ck_...
    secret-key: test_sk_...
```

> `application-secret.yml`은 `.gitignore`에 등록되어 있어 GitHub에 올라가지 않습니다.

### 3. 인프라 실행

```bash
docker compose up -d
```

Kafka, Redis, Elasticsearch 컨테이너가 실행됩니다.

### 4. 앱 실행

IntelliJ Run Configuration에서 **Active profiles**: `secret` 설정 후 실행

## API

### 상품

| Method | URL | 설명 |
|---|---|---|
| POST | `/products` | 상품 등록 (Kafka 발행 → ES 인덱싱) |

### 검색

| Method | URL | 설명 |
|---|---|---|
| GET | `/search?keyword=` | 키워드 검색 + 인기검색어 카운트 |
| GET | `/search/category/{category}` | 카테고리 검색 |
| GET | `/search/rank` | 인기검색어 Top 10 |

### 주문 / 결제

| Method | URL | 설명 |
|---|---|---|
| POST | `/orders` | 주문 생성 |
| GET | `/orders/{orderId}` | 주문 조회 |
| POST | `/payments/confirm` | 결제 승인 |
| POST | `/payments/{paymentKey}/cancel` | 결제 취소 |

## 결제 테스트

브라우저에서 접속:
```
http://localhost:8080/payment.html
```

테스트 카드번호:
```
카드번호: 4242 4242 4242 4242
유효기간: 12/26
CVC: 123
```

## 데이터 흐름

```
[상품 등록]
POST /products → H2 저장 → Kafka 발행 → Consumer → ES 인덱싱

[검색]
GET /search → ES 풀텍스트 검색 → Redis ZSet score +1

[결제]
payment.html → 주문 생성 → 토스페이먼츠 결제창 → 승인 콜백 → DB 저장
```
