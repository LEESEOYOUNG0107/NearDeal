// 하트 버튼: 페이지 이동 없이 토글. 실패하면 일반 폼 제출로 대체(JS 없이도 동작)
document.addEventListener('submit', async (e) => {
    const form = e.target.closest('.heart-form');
    if (!form) return;
    e.preventDefault();
    const btn = form.querySelector('.heart');
    if (btn.disabled) return;
    btn.disabled = true;
    try {
        const res = await fetch(form.action, {method: 'POST', headers: {'X-Requested-With': 'fetch'}});
        if (!res.ok) throw new Error();
        const on = btn.classList.toggle('on');
        btn.setAttribute('aria-pressed', on);
        btn.setAttribute('aria-label', on ? '찜 해제' : '찜하기');
        // 같은 카드의 찜 수 / 상세 페이지의 찜 수 갱신
        const scope = form.closest('.card-wrap, .action-bar, body');
        const counter = scope.querySelector('.fav-count') || document.querySelector('.fav-count');
        if (counter) counter.textContent = Math.max(0, Number(counter.textContent) + (on ? 1 : -1));
    } catch (err) {
        form.submit();
    } finally {
        btn.disabled = false;
    }
});
