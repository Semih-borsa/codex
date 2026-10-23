const bridge = typeof window !== 'undefined' ? window.PortfoyumNative : null;
export const isAndroid = !!bridge?.postMessage;
const pending = new Map();

if (bridge) {
  bridge.onmessage = event => {
    let message;
    try { message = JSON.parse(event.data); } catch { return; }
    const request = pending.get(message.id);
    if (!request) return;
    clearTimeout(request.timer); pending.delete(message.id);
    if (message.ok) request.resolve(message.result);
    else request.reject(new Error(message.error || 'Telefon işlemi tamamlanamadı.'));
  };
}

export function callNative(action, args = {}, timeoutMs = 35000) {
  if (!isAndroid) return Promise.reject(new Error('Bu paket Android uygulamasında açılmalıdır.'));
  return new Promise((resolve, reject) => {
    const id = crypto.randomUUID();
    const timer = setTimeout(() => {
      pending.delete(id); reject(new Error('Telefon işlemi zaman aşımına uğradı.'));
    }, timeoutMs);
    pending.set(id, { resolve, reject, timer });
    try { bridge.postMessage(JSON.stringify({ id, action, args })); }
    catch (e) { clearTimeout(timer); pending.delete(id); reject(e); }
  });
}

export async function nativeFetch(url, options = {}) {
  const response = await callNative('http', {
    url: String(url), method: options.method || 'GET',
    headers: options.headers || {}, body: options.body || ''
  });
  return {
    ok: response.status >= 200 && response.status < 300,
    status: response.status,
    text: async () => response.body || ''
  };
}

export async function saveDocument(name, text, mimeType) {
  return callNative('saveFile', { name, text, mimeType: mimeType.split(';')[0] }, 600000);
}

export async function openDocument(accept) {
  return callNative('openFile', { accept }, 600000);
}
