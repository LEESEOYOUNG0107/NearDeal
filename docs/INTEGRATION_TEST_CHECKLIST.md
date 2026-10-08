# NearDeal 통합 테스트 체크리스트

사용자 한 명이 서비스를 처음 쓰는 흐름을 처음부터 끝까지 따라가며 확인하는 표입니다.
**자동 점검**: 서버를 켠 뒤 `node docs/e2e-scenario.mjs` (다른 포트면 뒤에 `http://localhost:8081`).
아래 표의 순서와 같고, 마지막 실행 결과는 **60개 통과 / 0개 실패 / 1개 건너뜀**(AI는 Gemini 한도 초과로 건너뜀)이었습니다.

- 모든 요청의 `토큰`은 `POST /api/auth/login` 응답의 `accessToken` 입니다. `Authorization: Bearer <토큰>` 헤더로 보냅니다. Swagger UI(`/swagger-ui.html`)에서는 Authorize 에 토큰만 붙여 넣습니다.
- 실패 응답은 모두 `{status, error, message, path}` JSON 입니다(입력 검증 실패는 `fieldErrors` 추가).
- 🔓 = 로그인 필요 없음, 🔒 = 로그인 필요(없으면 401).

## 한눈에 보기

| # | 단계 | 핵심 API |
|---|---|---|
| 1 | 회원가입 | `POST /api/auth/signup` 🔓 |
| 2 | 로그인 | `POST /api/auth/login` 🔓, `GET /api/auth/me` 🔒 |
| 3 | 동네 설정 | `GET /api/regions`, `PUT /api/members/me/neighborhoods` 🔒 |
| 4 | AI 상품 설명 생성 | `POST /api/ai/product-description` 🔒 |
| 5 | 상품 등록 | `POST /api/products` 🔒 |
| 6 | 상품 검색 | `GET /api/products` 🔓, `GET /api/products/nearby` 🔒 |
| 7 | 상품 상세 | `GET /api/products/{id}` 🔓 |
| 8 | 찜 | `POST /api/products/{id}/favorite` 🔒 |
| 9 | 채팅 | `POST /api/chat-rooms`, `…/{roomId}/messages` 🔒, WebSocket `/ws-chat` |
| 10 | 예약 | `PATCH /api/products/{id}/status` 🔒 |
| 11 | 거래 완료 | 같은 API |
| 12 | 알림 확인 | `GET /api/notifications` 🔒 |

> 요청하신 시나리오의 "AI 상품 설명 생성 → 상품 등록 완료"는 두 단계입니다. AI는 글 초안만 돌려주고 상품을 만들지 않습니다.
> 사용자가 초안을 확인하고 고친 뒤 4번의 결과를 `description` 에 넣어 5번으로 등록합니다.

---

## 1. 회원가입

| 항목 | 내용 |
|---|---|
| 요청 API | `POST /api/auth/signup` 🔓 |
| 필요한 데이터 | `email`(이메일 형식, 100자 이하), `password`(8~64자, 한글은 72바이트 이하), `nickname`(2~20자), `profileImage`(선택, http(s) 주소) |
| 예상 결과 | **201** + `{id, email, nickname, …}`. 이메일은 소문자로 저장, **비밀번호는 응답에 없음**. DB에는 BCrypt 해시만 저장 |
| 실패할 수 있는 상황 | 이미 가입된 이메일 → **409** · 이메일 형식/닉네임 길이 위반 → **400**(`fieldErrors.email`, `fieldErrors.nickname`) · 비밀번호가 8자 미만이거나 64자 초과 → **400** · 같은 이메일로 동시에 가입 → 한쪽만 성공, 나머지 **409**(DB 유니크 제약) |

## 2. 로그인

| 항목 | 내용 |
|---|---|
| 요청 API | `POST /api/auth/login` 🔓 → 받은 토큰으로 `GET /api/auth/me` 🔒 |
| 필요한 데이터 | 가입한 `email`, `password` |
| 예상 결과 | **200** + `{accessToken, tokenType:"Bearer", expiresInSeconds:3600, member}`. `/me` 는 **200** 으로 내 정보와 내 동네 목록 |
| 실패할 수 있는 상황 | 이메일 없음/비밀번호 틀림 → **401**(어느 쪽이 틀렸는지 알려 주지 않음) · 토큰 없이 🔒 API → **401** "로그인이 필요합니다." · 만료된 토큰 → **401** "토큰이 만료되었습니다." · 위조/깨진 토큰 → **401** "유효하지 않은 토큰입니다." · 서버 재시작 후 토큰 → `JWT_SECRET` 이 `.env` 에 있으면 계속 유효, 없으면 **401** |

## 3. 동네 설정

| 항목 | 내용 |
|---|---|
| 요청 API | `GET /api/regions?keyword=성수` · `PUT /api/members/me/neighborhoods` · `GET /api/members/me/neighborhoods` 🔒 |
| 필요한 데이터 | PUT 본문 `{"neighborhoods":["성수동","역삼동"]}` (전체 목록을 통째로 교체, 첫 번째가 현재 동네) |
| 예상 결과 | 검색은 **200** + 이름 목록(최대 20개). 설정은 **200** + 저장된 목록(공백 제거됨). `[]` 를 보내면 설정 해제. `/api/auth/me` 에도 같은 값 |
| 실패할 수 있는 상황 | 3개 이상 → **400** · 같은 이름 두 번 → **400** · 쉼표/태그/21자 이상/공백만 → **400** · `neighborhoods` 필드 없음 → **400** · 실패한 요청은 기존 설정을 바꾸지 않음 · 동네 미설정 상태에서 `/api/products/nearby` → **400** "내 동네를 먼저 설정해 주세요." |

## 4. AI 상품 설명 생성

| 항목 | 내용 |
|---|---|
| 요청 API | `POST /api/ai/product-description` 🔒 |
| 필요한 데이터 | `productName`(필수, 100자), `condition`(필수, 100자), `features`(선택, 1000자), `extraInfo`(선택, 1000자). 서버에 `GEMINI_API_KEY` 환경변수 |
| 예상 결과 | **200** + `{description}`. 입력하지 않은 사양/구매시기는 지어내지 않고 가격도 언급하지 않음. **상품은 저장되지 않음** |
| 실패할 수 있는 상황 | 필수값 누락/길이 초과 → **400** · 키 없음/잘못됨, Gemini 과부하(503)·한도 초과(429) → **502** "AI 설명 생성에 실패했습니다…"(25초 안에 끝남, 원인은 서버 로그에만) · 로그인 안 함 → **401** · 호출 횟수 제한은 없음(알려진 한계) |

## 5. 상품 등록

| 항목 | 내용 |
|---|---|
| 요청 API | `POST /api/products` 🔒 (화면은 사진 포함 `POST /products` 폼) |
| 필요한 데이터 | `title`(100자), `price`(1~9,999,999,999), `category`(DIGITAL, FURNITURE, CLOTHES, BEAUTY, SPORTS, BOOKS, GAME, FOOD, ETC), `description`(3000자), `region`(50자). 판매자는 토큰의 회원으로 자동 지정(본문에 넣어도 무시) |
| 예상 결과 | **201** + 상품. 상태는 `SALE`, 조회수 0. 화면에서는 사진 최대 10장(한 장 5MB, 합계 55MB) |
| 실패할 수 있는 상황 | 제목 공백/101자 이상, 가격 0·음수·상한 초과, 없는 카테고리 값 → **400**(항목별 이유) · 깨진 JSON → **400** · `Content-Type` 이 JSON 이 아님 → **415** · 토큰 없음 → **401** · 화면 폼: 사진 용량 초과/형식 오류 → 입력값 유지한 채 안내 문구, 글자 수 초과 → 입력 화면으로 돌아가 안내 |

## 6. 상품 검색

| 항목 | 내용 |
|---|---|
| 요청 API | `GET /api/products?keyword=&category=&minPrice=&maxPrice=&status=&region=` 🔓 · 내 동네만: `GET /api/products/nearby` 🔒 |
| 필요한 데이터 | 조건은 모두 선택, 함께 쓰면 AND. `keyword` 는 제목 일부(대소문자 무시), `region` 은 거래 지역에 포함된 글자. `nearby` 는 내 동네가 설정되어 있어야 함 |
| 예상 결과 | **200** + 최신순 목록. 조건에 안 맞는 상품은 빠짐. `nearby` 는 현재 동네(첫 번째)가 지역에 포함된 상품만(`region` 파라미터는 무시) |
| 실패할 수 있는 상황 | `minPrice=abc`, `category=NOPE`, `status=NOPE` → **400** · `keyword` 의 `%`·`_` 는 글자 그대로 검색(와일드카드로 동작하지 않음) · 결과가 없으면 빈 목록 `[]`(**200**) · 동네 미설정 `nearby` → **400** |

## 7. 상품 상세

| 항목 | 내용 |
|---|---|
| 요청 API | `GET /api/products/{id}` 🔓 · (수정) `PATCH /api/products/{id}` 🔒 · (삭제) `DELETE /api/products/{id}` 🔒 |
| 필요한 데이터 | 상품 번호. 수정은 바꿀 필드만(제목, 가격, 카테고리, 설명, 지역) |
| 예상 결과 | **200** + 상품(판매자 번호·닉네임 포함). **조회할 때마다 조회수 +1**. 수정 **200**, 삭제 **204**(연결된 찜·채팅방·채팅 메시지도 함께 삭제, 알림은 남음) |
| 실패할 수 있는 상황 | 없는 상품 → **404** · 번호가 숫자가 아님 → **400** · 남의 상품 수정/삭제 → **403** · 수정 값이 공백뿐이거나 길이 초과 → **400** · 동시 조회가 많으면 조회수가 일부 누락될 수 있음(알려진 한계) |

## 8. 찜

| 항목 | 내용 |
|---|---|
| 요청 API | `POST /api/products/{id}/favorite` · `DELETE …/favorite` · `GET …/favorite` · `GET /api/favorites` 🔒 |
| 필요한 데이터 | 상품 번호, 로그인 토큰 |
| 예상 결과 | 찜 **201** + `{favoriteCount, favorited:true}`, 취소 **200**, 내 찜 목록 **200**. **판매자에게 찜 알림 1개**(같은 사람이 취소했다 다시 찜해도 중복 알림 없음, 자기 상품을 찜해도 알림 없음) |
| 실패할 수 있는 상황 | 이미 찜한 상품 → **409** · 없는 상품 → **404** · 두 번 빠르게 클릭 → 한 번만 저장(**409** 또는 성공) · 토큰 없음 → **401** |

## 9. 채팅

| 항목 | 내용 |
|---|---|
| 요청 API | 방 만들기 `POST /api/chat-rooms` · 내 방 목록 `GET /api/chat-rooms` · 메시지 `GET/POST /api/chat-rooms/{roomId}/messages` 🔒 · 실시간: WebSocket `/ws-chat` (CONNECT 헤더 `Authorization: Bearer 토큰`, 구독 `/topic/chat-rooms/{roomId}`, 전송 `/app/chat-rooms/{roomId}/messages`) |
| 필요한 데이터 | `{"productId":1}`, 메시지 `{"content":"…"}`(1~1000자). 구매자가 방을 만들고 판매자는 상품에서 자동 결정 |
| 예상 결과 | 방 **200**(같은 상품에 다시 요청하면 같은 방). 메시지 **201**. 방 참여자가 구독 중이면 **실시간 수신**, 보낸 메시지는 REST/WebSocket 모두 DB에 저장. 상대의 메시지를 읽으면 읽음 처리. **상대에게 알림**(같은 방의 알림은 하나로 합쳐 최신 내용으로 갱신) |
| 실패할 수 있는 상황 | 자기 상품에 방 만들기 → **400** · 없는 상품 → **404** · 참여자가 아닌 사람이 읽기/쓰기 → **403** · 참여자가 아닌 사람의 구독 → STOMP **ERROR** · 빈 메시지/1001자 → **400** · 토큰 없이 WebSocket 접속 → 연결 거부 · 연결 중 토큰이 만료돼도 끊기지 않음(접속 때만 검사, 알려진 한계) |

## 10. 예약

| 항목 | 내용 |
|---|---|
| 요청 API | `PATCH /api/products/{id}/status` 🔒 본문 `{"status":"RESERVED"}` (화면: 상품/채팅방의 상태 버튼 `POST /products/{id}/status`) |
| 필요한 데이터 | 판매자 토큰, `status` ∈ `SALE`(판매중) · `RESERVED`(예약중) · `SOLD_OUT`(거래완료) |
| 예상 결과 | **204**. 상세에 `RESERVED` 반영. **이 상품으로 채팅했거나 찜한 사람(판매자 본인 제외)에게 "예약중으로 변경됐어요" 알림** |
| 실패할 수 있는 상황 | 판매자가 아님(구매자 포함) → **403** · 잘못된 상태값/본문 없음 → **400** · 없는 상품 → **404** · 이미 같은 상태로 다시 변경 → **204** 이지만 알림은 만들지 않음 |

## 11. 거래 완료

| 항목 | 내용 |
|---|---|
| 요청 API | `PATCH /api/products/{id}/status` 🔒 본문 `{"status":"SOLD_OUT"}` |
| 필요한 데이터 | 10번과 같음 |
| 예상 결과 | **204**. `GET /api/products?status=SOLD_OUT` 에는 나오고 `status=SALE` 에는 안 나옴. 구매자/찜한 사람에게 "거래완료로 변경됐어요" 알림 |
| 실패할 수 있는 상황 | 10번과 같음 · 거래완료 후 다시 `SALE` 로 되돌리는 것은 허용(막지 않음) · 거래완료 상품에도 채팅방은 만들 수 있음(막지 않음, 필요하면 추가) |

## 12. 알림 확인

| 항목 | 내용 |
|---|---|
| 요청 API | `GET /api/notifications?unreadOnly=` · `GET /api/notifications/unread-count` · `PATCH /api/notifications/{id}/read` · `PATCH /api/notifications/read-all` 🔒 |
| 필요한 데이터 | 토큰. 앞 단계에서 만들어진 알림 |
| 예상 결과 | **판매자**: 찜 알림 1개 + 새 메시지 알림(방별 1개로 합쳐짐). **구매자**: 예약중·거래완료 알림 2개 + 판매자 메시지 알림. 최신순, 안 읽은 개수가 맞음, 하나 읽음 처리 후 `seen:true`, 모두 읽음 후 개수 **0** |
| 실패할 수 있는 상황 | 남의 알림 읽음 처리 → **403** · 없는 알림 → **404** · 본인이 한 행동의 알림은 본인에게 오지 않음 · 알림은 상품과 외래키가 없어서 상품을 지워도 남음(알림 목록의 링크가 404 가 될 수 있음) · 실시간 푸시는 없고 화면이 목록을 다시 불러와야 함 |

---

## 모든 단계에 공통으로 확인할 것

- [ ] `/api/**` 의 실패 응답에 스택트레이스·SQL·클래스 이름이 없다(`GlobalExceptionHandler`, `spring.web.error.include-*=never`).
- [ ] 비밀번호·API 키·토큰이 응답과 로그에 나오지 않는다(`LoginRequest`, `SignupRequest` 의 `toString` 은 비밀번호를 가린다).
- [ ] 다른 사용자의 데이터(상품 수정, 채팅방, 알림)는 403 으로 막힌다.
- [ ] `.env`(`GEMINI_API_KEY`, `DB_PASSWORD`, `JWT_SECRET`)는 Git 에 올라가지 않는다(`.gitignore`).

## 자동화되어 있지 않은 것 (직접 확인)

- 브라우저 화면(Thymeleaf)의 클릭 흐름: 로그인/가입 → 홈 → 상품 등록(사진 포함) → 상세 → 채팅 → 알림. 화면 폼 경로의 서버 쪽 검증은 점검 스크립트로 확인했지만, 눈으로 보는 것은 직접 해야 합니다.
- Gemini 가 실제로 응답하는 경우의 글 품질(한도 초과 시 502 로 끝나는 것까지만 자동 확인).
- 토큰 만료(60분) 시각 경과: 단위 테스트(`JwtProviderTest`)와 이전 단계의 짧은 만료 시나리오로 확인했습니다.
