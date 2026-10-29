(() => {
  const api = globalThis.browser || globalThis.chrome;
  function area(element) {
    const r = element.getBoundingClientRect();
    return Math.max(0, Math.min(r.right, innerWidth) - Math.max(r.left, 0)) *
      Math.max(0, Math.min(r.bottom, innerHeight) - Math.max(r.top, 0));
  }
  function video() {
    return [...document.querySelectorAll('video')].filter(v => area(v) > 0)
      .sort((a, b) => area(b) - area(a))[0];
  }
  function navigate(direction, current) {
    const label = direction > 0 ? 'Next' : 'Previous';
    // Prefer Instagram's actual navigation buttons; localized pages can use the scroll fallback.
    const buttons = [...document.querySelectorAll('button, [role="button"]')];
    const button = buttons.find(b => area(b) > 0 &&
      [b.getAttribute('aria-label'), ...[...b.querySelectorAll('[aria-label]')].map(n => n.getAttribute('aria-label'))]
        .some(text => text && new RegExp(`^${label}( reel| video)?$`, 'i').test(text.trim())));
    if (button) { button.click(); return true; }
    for (let parent = current.parentElement; parent; parent = parent.parentElement) {
      if (parent.scrollHeight > parent.clientHeight + 20 && /auto|scroll/.test(getComputedStyle(parent).overflowY)) {
        parent.scrollBy({ top: direction * parent.clientHeight, behavior: 'smooth' }); return true;
      }
    }
    return false;
  }
  api.runtime.onMessage.addListener((message, sender, reply) => {
    if (sender.id !== api.runtime.id) return false;
    (async () => {
      if (!/^\/reels?(\/|$)/.test(location.pathname)) throw new Error('Open Instagram Reels first.');
      const current = video();
      if (!current) throw new Error('No visible Reel. Play a Reel in the browser first.');
      if (message.command === 'TOGGLE_PLAY') {
        if (current.paused) await current.play(); else current.pause();
      } else if (message.command === 'NEXT' || message.command === 'PREVIOUS') {
        if (!navigate(message.command === 'NEXT' ? 1 : -1, current))
          throw new Error('Instagram navigation not found. Try the full Reels page.');
      } else throw new Error('Unknown command');
      reply({ ok: true });
    })().catch(error => reply({ ok: false, error: error.message }));
    return true;
  });
})();
