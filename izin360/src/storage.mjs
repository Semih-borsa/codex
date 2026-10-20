import {emptyData,blankBase,id} from "./model.mjs";
let db;
const updates =
  typeof BroadcastChannel === "function"
    ? new BroadcastChannel("izin360-updates")
    : null;
const request = (req) =>
  new Promise((resolve, reject) => {
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
async function open() {
  if (db) return db;
  const req = indexedDB.open("izin360", 1);
  req.onupgradeneeded = () => req.result.createObjectStore("settings");
  db = await request(req);
  return db;
}
export async function load() {
  const d = await open(),
    value = await request(
      d.transaction("settings").objectStore("settings").get("state"),
    );
  return (
    value || {
      data: emptyData(),
      base: blankBase(),
      conflicts: [],
      deviceId: id(),
      lastSync: null,
    }
  );
}
export async function transact(fn, fallback) {
  const d = await open();
  return new Promise((resolve, reject) => {
    const tx = d.transaction("settings", "readwrite"),
      store = tx.objectStore("settings"),
      get = store.get("state");
    let next, problem;
    get.onsuccess = () => {
      try {
        next = fn(get.result || fallback);
        store.put(next, "state");
      } catch (e) {
        problem = e;
        tx.abort();
      }
    };
    tx.oncomplete = () => {
      updates?.postMessage("changed");
      resolve(next);
    };
    tx.onabort = () =>
      reject(problem || tx.error || Error("Yerel kayıt tamamlanamadı."));
    tx.onerror = () => reject(problem || tx.error);
  });
}
export function subscribe(fn) {
  if (!updates) return () => {};
  const listener = () =>
    load()
      .then(fn)
      .catch(() => {});
  updates.addEventListener("message", listener);
  return () => updates.removeEventListener("message", listener);
}
let callbackId = 0;
const callbacks = new Map();
window.__nativeResult = (key, response) => {
  const callback = callbacks.get(key);
  if (!callback) return;
  callbacks.delete(key);
  clearTimeout(callback.timeout);
  callback.resolve(response);
};
export const native = () => Boolean(window.Android?.request);
export function getConnection() {
  if (native()) return JSON.parse(window.Android.status());
  try {
    return (
      JSON.parse(localStorage.getItem("izin360-connection")) || {
        url: location.protocol.startsWith("http") ? location.origin : "",
        token: "",
      }
    );
  } catch {
    return { url: "", token: "" };
  }
}
export function setConnection(url, token) {
  url = url.trim().replace(/\/+$/, "");
  if (url) {
    const parsed = new URL(url);
    if (
      parsed.username ||
      parsed.password ||
      parsed.search ||
      parsed.hash ||
      !["https:", "http:"].includes(parsed.protocol) ||
      parsed.pathname !== "/"
    )
      throw Error(
        "Yalnızca sunucunun kök adresini gir (örn. http://192.168.1.20:4310).",
      );
    if (
      parsed.protocol === "http:" &&
      !/^(localhost|127\.0\.0\.1|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+)$/.test(
        parsed.hostname,
      )
    )
      throw Error(
        "İnternet bağlantısında HTTPS kullan. HTTP yalnızca yerel ağ içindir.",
      );
  }
  if (native()) {
    const result = JSON.parse(window.Android.saveConnection(url, token));
    if (!result.ok) throw Error(result.error);
    return;
  }
  const old = getConnection();
  localStorage.setItem(
    "izin360-connection",
    JSON.stringify({ url, token: token || (url === old.url ? old.token : "") }),
  );
}
export function disconnect() {
  if (native()) window.Android.clearConnection();
  else
    localStorage.setItem(
      "izin360-connection",
      JSON.stringify({ url: "", token: "" }),
    );
}
export async function api(path, body) {
  if (native()) {
    return new Promise((resolve, reject) => {
      const key = String(++callbackId);
      const timeout = setTimeout(() => {
        callbacks.delete(key);
        reject(
          Error("Windows bilgisayara ulaşılamadı. Bağlantıyı kontrol et."),
        );
      }, 25000);
      callbacks.set(key, { resolve, reject, timeout });
      window.Android.request(
        key,
        path,
        body === undefined ? "GET" : "POST",
        body === undefined ? "" : JSON.stringify(body),
      );
    }).then((r) => {
      if (![200, 409].includes(r.status))
        throw Error(r.body?.error || "Bağlantı kurulamadı.");
      return r.body;
    });
  }
  const connection = getConnection();
  if (!connection.url)
    throw Error("Eşitleme için önce Windows bağlantısını yapılandır.");
  let response;
  try {
    response = await fetch(connection.url + path, {
      method: body === undefined ? "GET" : "POST",
      headers: {
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...(connection.token
          ? { Authorization: "Bearer " + connection.token }
          : {}),
      },
      credentials: "include",
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(20000),
    });
  } catch {
    throw Error("Windows bilgisayara ulaşılamadı. Yerel kayıtların korunuyor.");
  }
  if (
    response.status === 401 &&
    !connection.token &&
    connection.url === location.origin
  ) {
    await fetch(connection.url + "/", {
      credentials: "include",
      signal: AbortSignal.timeout(8000),
    });
    response = await fetch(connection.url + path, {
      method: body === undefined ? "GET" : "POST",
      headers: body === undefined ? {} : { "Content-Type": "application/json" },
      credentials: "include",
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(20000),
    });
  }
  const result = await response.json();
  if (![200, 409].includes(response.status))
    throw Error(result.error || "İşlem tamamlanamadı.");
  return result;
}
