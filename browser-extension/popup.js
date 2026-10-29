const api = globalThis.browser || globalThis.chrome;
const status = document.getElementById('status');
api.storage.local.get('token').then(({ token }) => { document.getElementById('code').value = token || ''; });
async function refresh() { const state = await api.storage.session.get('status'); status.textContent = state.status || 'Disconnected'; }
refresh();
api.storage.onChanged.addListener((changes, area) => { if (area === 'session' && changes.status) refresh(); });
document.getElementById('connect').onclick = async () => {
  const result = await api.runtime.sendMessage({ type: 'connect', token: document.getElementById('code').value.trim() });
  status.textContent = result.error || 'Connecting…';
};
document.getElementById('stop').onclick = async () => { await api.runtime.sendMessage({ type: 'stop' }); refresh(); };
