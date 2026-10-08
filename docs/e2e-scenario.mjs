// NearDeal 통합 시나리오 자동 점검 (docs/INTEGRATION_TEST_CHECKLIST.md 의 표와 같은 순서)
//
//   서버를 켠 뒤:  node docs/e2e-scenario.mjs            (기본 http://localhost:8080)
//                  node docs/e2e-scenario.mjs http://localhost:8081
//
// 임시 회원 2명(판매자/구매자)을 새로 만들어 쓰고 마지막에 만든 상품은 지운다.
// 회원은 지우는 API가 없어서 이메일이 e2e*@example.com 으로 남는다. Node 22+ 필요(내장 fetch/WebSocket).
// AI 단계는 GEMINI_API_KEY 와 Gemini 서비스 상태에 달려 있다. 502(외부 사정)면 SKIP 으로 표시하고 나머지를 계속한다.

const BASE = process.argv[2] || 'http://localhost:8080';
const WS = BASE.replace(/^http/, 'ws') + '/ws-chat';
let pass = 0, fail = 0, skip = 0;
const check = (name, ok, extra = '') => { ok ? pass++ : fail++; console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  → ' + extra : '')); };
const step = (t) => console.log('\n' + t);

const call = async (method, path, token, body) => {
  const r = await fetch(BASE + path, {
    method,
    headers: { ...(body !== undefined ? { 'Content-Type': 'application/json; charset=utf-8' } : {}), ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await r.text();
  let json = null; try { json = text ? JSON.parse(text) : null; } catch { /* 본문 없음 */ }
  return { status: r.status, json, text };
};

// ---- 순수 STOMP 프레임으로 실시간 채팅 확인 ----
function stomp(token) {
  const ws = new WebSocket(WS); const frames = []; const waiters = [];
  const c = {
    frames,
    send(cmd, headers = {}, body = '') { ws.send(cmd + '\n' + Object.entries(headers).map(([k, v]) => `${k}:${v}`).join('\n') + '\n\n' + body + '\0'); },
    waitFor(pred, ms = 4000) {
      const hit = frames.find(pred); if (hit) return Promise.resolve(hit);
      return new Promise((res) => { const t = setTimeout(() => res(null), ms); waiters.push({ pred, res: (f) => { clearTimeout(t); res(f); } }); });
    },
    close() { ws.close(); },
  };
  ws.onmessage = (ev) => {
    for (const raw of String(ev.data).split('\0')) {
      const text = raw.replace(/^\n+/, ''); if (!text) continue;
      const [head, ...rest] = text.split('\n\n'); const lines = head.split('\n');
      const f = { cmd: lines[0], headers: Object.fromEntries(lines.slice(1).map((l) => [l.slice(0, l.indexOf(':')), l.slice(l.indexOf(':') + 1)])), body: rest.join('\n\n') };
      frames.push(f);
      for (const w of waiters.slice()) if (w.pred(f)) { w.res(f); waiters.splice(waiters.indexOf(w), 1); }
    }
  };
  return new Promise((resolve) => { ws.onopen = () => { c.send('CONNECT', { 'accept-version': '1.2', 'heart-beat': '0,0', Authorization: 'Bearer ' + token }); resolve(c); }; ws.onerror = () => resolve(c); });
}

const stamp = Date.now();
const sellerMail = `e2e-seller-${stamp}@example.com`, buyerMail = `e2e-buyer-${stamp}@example.com`, PW = 'my-secret-pw';
let r;

step('1. 회원가입');
r = await call('POST', '/api/auth/signup', null, { email: sellerMail, password: PW, nickname: '판매자' });
check('판매자 가입 → 201, 비밀번호는 응답에 없음', r.status === 201 && !/password/i.test(r.text), String(r.status));
r = await call('POST', '/api/auth/signup', null, { email: buyerMail, password: PW, nickname: '구매자' });
check('구매자 가입 → 201', r.status === 201);
check('같은 이메일 재가입 → 409', (await call('POST', '/api/auth/signup', null, { email: sellerMail, password: PW, nickname: '중복' })).status === 409);
check('잘못된 이메일/짧은 비밀번호 → 400', (await call('POST', '/api/auth/signup', null, { email: 'bad', password: '1', nickname: 'x' })).status === 400);

step('2. 로그인');
r = await call('POST', '/api/auth/login', null, { email: sellerMail, password: PW });
const S = r.json?.accessToken; const sellerId = r.json?.member?.id;
check('판매자 로그인 → 200, accessToken(Bearer)', r.status === 200 && !!S && r.json.tokenType === 'Bearer');
r = await call('POST', '/api/auth/login', null, { email: buyerMail, password: PW });
const B = r.json?.accessToken; const buyerId = r.json?.member?.id;
check('구매자 로그인 → 200', r.status === 200 && !!B);
check('비밀번호 틀림 → 401', (await call('POST', '/api/auth/login', null, { email: sellerMail, password: 'wrong-password' })).status === 401);
check('토큰 없이 보호된 API → 401', (await call('GET', '/api/auth/me')).status === 401);
check('/me → 내 정보', (await call('GET', '/api/auth/me', S)).json?.id === sellerId);

step('3. 동네 설정');
check('동네 검색(성수) → 성수동 포함', (await call('GET', '/api/regions?keyword=' + encodeURIComponent('성수'), S)).json?.includes('성수동'));
check('동네 미설정으로 내 동네 상품 → 400', (await call('GET', '/api/products/nearby', B)).status === 400);
check('판매자 동네 설정 → 200', (await call('PUT', '/api/members/me/neighborhoods', S, { neighborhoods: ['성수동'] })).status === 200);
check('구매자 동네 설정(성수동, 역삼동) → 200', (await call('PUT', '/api/members/me/neighborhoods', B, { neighborhoods: ['성수동', '역삼동'] })).status === 200);
check('동네 3개 → 400', (await call('PUT', '/api/members/me/neighborhoods', B, { neighborhoods: ['a', 'b', 'c'] })).status === 400);

step('4~5. AI 상품 설명 생성');
r = await call('POST', '/api/ai/product-description', S, { productName: '아이폰 13', condition: '거의 새것', features: '128GB, 블루', extraInfo: '직거래 선호' });
if (r.status === 502) { skip++; console.log('SKIP AI 설명 → 502 (Gemini 쪽 과부하/한도 초과 등 외부 사정. 25초 안에 502로 끝나는 것까지가 이 서버의 책임)'); }
else check('AI 설명 → 200, 글만 돌려줌(상품은 자동 등록 안 됨)', r.status === 200 && !!r.json?.description, r.status === 200 ? r.json.description.slice(0, 30) + '…' : `${r.status} ${r.json?.message ?? ''}`);
check('상품명 없이 요청 → 400', (await call('POST', '/api/ai/product-description', S, { condition: 'x' })).status === 400);
const description = r.status === 200 ? r.json.description : '배터리 효율 90%, 생활기스 조금 있어요.';

step('6. 상품 등록 완료');
const title = `e2e-아이폰-${stamp}`;
r = await call('POST', '/api/products', S, { title, price: 500000, category: 'DIGITAL', description, region: '서울 성수동' });
const pid = r.json?.id;
check('상품 등록 → 201, 판매자=로그인한 회원, 상태 SALE', r.status === 201 && r.json.sellerId === sellerId && r.json.status === 'SALE', String(r.status));
check('토큰 없이 등록 → 401', (await call('POST', '/api/products', null, { title: 'x', price: 1, category: 'ETC', description: 'd', region: 'r' })).status === 401);
check('가격 0/제목 공백 → 400', (await call('POST', '/api/products', S, { title: ' ', price: 0, category: 'ETC', description: 'd', region: 'r' })).status === 400);
const other = (await call('POST', '/api/products', S, { title: `e2e-부산-${stamp}`, price: 1000, category: 'BOOKS', description: 'd', region: '부산 해운대구' })).json?.id;

step('7. 상품 검색');
const titles = async (q) => (await call('GET', '/api/products?' + q)).json?.map((p) => p.title) ?? [];
check('키워드 검색', (await titles('keyword=' + encodeURIComponent(title))).includes(title));
check('카테고리+가격 범위+상태 조합', (await titles(`category=DIGITAL&minPrice=400000&maxPrice=600000&status=SALE&keyword=${encodeURIComponent(title)}`)).includes(title));
check('조건에 안 맞으면 제외(최대가격 1000)', !(await titles(`maxPrice=1000&keyword=${encodeURIComponent(title)}`)).includes(title));
r = await call('GET', '/api/products/nearby', B);
check('내 동네(성수동) 상품 → 성수동 상품만', r.json?.some((p) => p.id === pid) && !r.json?.some((p) => p.id === other));
check('검색 파라미터 형식 오류 → 400', (await call('GET', '/api/products?minPrice=abc')).status === 400);

step('8. 상품 상세');
r = await call('GET', '/api/products/' + pid); const v1 = r.json?.viewCount;
check('상세(로그인 불필요) → 200, 판매자 닉네임', r.status === 200 && r.json.sellerNickname === '판매자');
check('조회할 때마다 조회수 +1', (await call('GET', '/api/products/' + pid)).json?.viewCount === v1 + 1);
check('없는 상품 → 404', (await call('GET', '/api/products/99999999')).status === 404);
check('남의 상품 수정 → 403', (await call('PATCH', '/api/products/' + pid, B, { title: 'hack' })).status === 403);
check('내 상품 가격 수정 → 200', (await call('PATCH', '/api/products/' + pid, S, { price: 450000 })).json?.price === 450000);

step('9. 찜');
r = await call('POST', `/api/products/${pid}/favorite`, B);
check('구매자가 찜 → 201, 찜 개수 1', r.status === 201 && r.json?.favoriteCount === 1 && r.json?.favorited === true);
check('같은 상품 다시 찜 → 409', (await call('POST', `/api/products/${pid}/favorite`, B)).status === 409);
check('내 찜 목록에 있음', (await call('GET', '/api/favorites', B)).json?.some((p) => p.id === pid));

step('10. 채팅 (REST + WebSocket)');
check('판매자가 자기 상품에 채팅방 → 400', (await call('POST', '/api/chat-rooms', S, { productId: pid })).status === 400);
r = await call('POST', '/api/chat-rooms', B, { productId: pid }); const room = r.json?.id;
check('구매자가 채팅방 생성 → 200, 구매자/판매자 지정', r.status === 200 && r.json.buyerId === buyerId && r.json.sellerId === sellerId);
check('같은 상품 다시 요청 → 같은 방', (await call('POST', '/api/chat-rooms', B, { productId: pid })).json?.id === room);
r = await call('POST', `/api/chat-rooms/${room}/messages`, B, { content: '안녕하세요, 아직 판매중인가요?' });
check('REST 메시지 전송 → 201', r.status === 201 && r.json.senderId === buyerId);
const outsider = (await (async () => { const m = `e2e-out-${stamp}@example.com`; await call('POST', '/api/auth/signup', null, { email: m, password: PW, nickname: '제3자' }); return (await call('POST', '/api/auth/login', null, { email: m, password: PW })).json.accessToken; })());
check('방 참여자가 아닌 사람이 읽기 → 403', (await call('GET', `/api/chat-rooms/${room}/messages`, outsider)).status === 403);
check('방 참여자가 아닌 사람이 쓰기 → 403', (await call('POST', `/api/chat-rooms/${room}/messages`, outsider, { content: '침입' })).status === 403);
check('1000자 초과 메시지 → 400', (await call('POST', `/api/chat-rooms/${room}/messages`, B, { content: 'a'.repeat(1001) })).status === 400);
// 실시간
const sc = await stomp(S), bc = await stomp(B);
const connected = await sc.waitFor((f) => f.cmd === 'CONNECTED') && await bc.waitFor((f) => f.cmd === 'CONNECTED');
check('WebSocket 접속(토큰) → CONNECTED', connected);
sc.send('SUBSCRIBE', { id: 's1', destination: `/topic/chat-rooms/${room}` });
bc.send('SUBSCRIBE', { id: 'b1', destination: `/topic/chat-rooms/${room}` });
await new Promise((res) => setTimeout(res, 500));
bc.send('SEND', { destination: `/app/chat-rooms/${room}/messages`, 'content-type': 'application/json' }, JSON.stringify({ content: '실시간 문의드려요' }));
const got = await sc.waitFor((f) => f.cmd === 'MESSAGE' && f.body.includes('실시간 문의드려요'));
check('판매자가 실시간으로 메시지 수신', !!got);
sc.send('SEND', { destination: `/app/chat-rooms/${room}/messages`, 'content-type': 'application/json' }, JSON.stringify({ content: '네, 판매중이에요' }));
check('구매자도 실시간 수신', !!(await bc.waitFor((f) => f.cmd === 'MESSAGE' && f.body.includes('판매중이에요'))));
const oc = await stomp(outsider); await oc.waitFor((f) => f.cmd === 'CONNECTED');
oc.send('SUBSCRIBE', { id: 'o1', destination: `/topic/chat-rooms/${room}` });
check('참여자가 아닌 사람의 구독 → 거부(ERROR)', !!(await oc.waitFor((f) => f.cmd === 'ERROR', 2500)));
const history = await call('GET', `/api/chat-rooms/${room}/messages`, S);
check('실시간으로 보낸 메시지도 DB에 저장됨', history.json?.some((m) => m.content === '실시간 문의드려요') && history.json?.some((m) => m.content === '네, 판매중이에요'));
sc.close(); bc.close(); oc.close();

step('11. 예약');
check('구매자가 상태 변경 시도 → 403', (await call('PATCH', `/api/products/${pid}/status`, B, { status: 'RESERVED' })).status === 403);
check('판매자가 예약중으로 변경 → 204', (await call('PATCH', `/api/products/${pid}/status`, S, { status: 'RESERVED' })).status === 204);
check('상세에 RESERVED 반영', (await call('GET', '/api/products/' + pid)).json?.status === 'RESERVED');
check('잘못된 상태값 → 400', (await call('PATCH', `/api/products/${pid}/status`, S, { status: 'NOPE' })).status === 400);

step('12. 거래 완료');
check('판매자가 거래완료로 변경 → 204', (await call('PATCH', `/api/products/${pid}/status`, S, { status: 'SOLD_OUT' })).status === 204);
check('거래완료 상품은 상태 필터로 구분', (await titles(`status=SOLD_OUT&keyword=${encodeURIComponent(title)}`)).includes(title) && !(await titles(`status=SALE&keyword=${encodeURIComponent(title)}`)).includes(title));
check('같은 상태로 다시 변경 → 204(알림은 중복 생성 안 됨)', (await call('PATCH', `/api/products/${pid}/status`, S, { status: 'SOLD_OUT' })).status === 204);

step('13. 알림 확인');
const sellerNotes = (await call('GET', '/api/notifications', S)).json ?? [];
const buyerNotes = (await call('GET', '/api/notifications', B)).json ?? [];
check('판매자: 찜 알림 1개', sellerNotes.filter((n) => n.type === 'FAVORITE' && n.productId === pid).length === 1);
check('판매자: 새 메시지 알림(방별로 하나로 합쳐짐)', sellerNotes.filter((n) => n.type === 'CHAT_MESSAGE' && n.roomId === room).length === 1);
check('구매자: 예약중/거래완료 상태 알림 2개', buyerNotes.filter((n) => n.type === 'STATUS_CHANGE' && n.productId === pid).length === 2);
check('판매자 본인은 자기 상태 변경 알림을 받지 않음', !sellerNotes.some((n) => n.type === 'STATUS_CHANGE' && n.productId === pid));
r = await call('GET', '/api/notifications/unread-count', B); const unread = r.json?.count;
check('안 읽은 개수 ≥ 2', unread >= 2, String(unread));
const first = buyerNotes[0];
check('알림 하나 읽음 처리 → 200', (await call('PATCH', `/api/notifications/${first.id}/read`, B)).json?.seen === true);
check('남의 알림 읽음 처리 → 403', (await call('PATCH', `/api/notifications/${first.id}/read`, S)).status === 403);
check('모두 읽음 → 안 읽은 개수 0', (await call('PATCH', '/api/notifications/read-all', B)).status === 200 && (await call('GET', '/api/notifications/unread-count', B)).json?.count === 0);

step('정리');
check('판매자가 상품 삭제 → 204', (await call('DELETE', '/api/products/' + pid, S)).status === 204 && (await call('DELETE', '/api/products/' + other, S)).status === 204);
check('삭제 후 상세 → 404', (await call('GET', '/api/products/' + pid)).status === 404);

console.log(`\n${pass} passed, ${fail} failed` + (skip ? `, ${skip} skipped` : ''));
process.exit(fail ? 1 : 0);
