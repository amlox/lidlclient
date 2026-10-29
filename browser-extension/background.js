const api = globalThis.browser || globalThis.chrome;
let running = false;
let controller;
let generation = 0;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const instagram = url => /^https:\/\/www\.instagram\.com\/reels?(\/|$)/.test(url || '');

async function start() {
  if (running) return;
  running = true;
  const current = generation;
  try {
    while (current === generation) {
      const { target } = await api.storage.session.get('target');
      const { token } = await api.storage.local.get('token');
      if (!Number.isInteger(target) || !token) break;
      const tab = await api.tabs.get(target).catch(() => null);
      if (!tab || !instagram(tab.url)) {
        await api.storage.session.set({ status: 'Open Reels in the selected tab.' });
        await delay(1000); continue;
      }
      controller = new AbortController();
      const timeout = setTimeout(() => controller?.abort(), 18000);
      try {
        const response = await fetch('http://127.0.0.1:18764/poll', {
          headers: { Authorization: `Bearer ${token}` }, cache: 'no-store',
          credentials: 'omit', redirect: 'error', signal: controller.signal
        });
        if (current !== generation) break;
        if (response.status === 401) {
          await api.storage.session.set({ status: 'Wrong connection code. Copy it from LidlClient again.' });
          break;
        }
        if (!response.ok) throw new Error('Bridge unavailable');
        await api.storage.session.set({ status: 'Connected. Ready for Minecraft keybinds.' });
        if (response.status === 200) {
          const message = await response.json();
          if (['NEXT', 'PREVIOUS', 'TOGGLE_PLAY'].includes(message.command)) {
            const result = await api.tabs.sendMessage(target, message);
            if (!result?.ok) await api.storage.session.set({ status: result?.error || 'Reel control not found. Reload Instagram.' });
          }
        }
      } catch {
        if (current !== generation) break;
        await api.storage.session.set({ status: 'Waiting for Minecraft. Enable Reels Controls and check the code.' });
        await delay(1000);
      } finally { clearTimeout(timeout); }
    }
  } finally {
    running = false;
    if (current !== generation) start();
  }
}

api.runtime.onMessage.addListener((message, sender, reply) => {
  // Pairing is accepted only from this extension's popup, never from a web page/content script.
  if (sender.tab || sender.id !== api.runtime.id) return false;
  if (message.type === 'connect') {
    (async () => {
      if (!/^[a-f0-9-]{36}$/i.test(message.token || '')) throw new Error('Paste the connection code from Minecraft.');
      const [tab] = await api.tabs.query({ active: true, currentWindow: true });
      if (!tab || !instagram(tab.url)) throw new Error('First open instagram.com/reels/ in this tab.');
      generation++; controller?.abort();
      await api.storage.local.set({ token: message.token });
      await api.storage.session.set({ target: tab.id, status: 'Connecting…' });
      start(); reply({ ok: true });
    })().catch(error => reply({ error: error.message }));
    return true;
  }
  if (message.type === 'stop') {
    (async () => {
      generation++; controller?.abort();
      await api.storage.session.remove('target');
      await api.storage.session.set({ status: 'Disconnected' });
      reply({ ok: true });
    })();
    return true;
  }
  return false;
});
// Resume after service-worker suspension; session storage intentionally clears on browser restart.
start();
