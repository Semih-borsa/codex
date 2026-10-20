import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  LayoutDashboard,
  Users,
  CalendarDays,
  CalendarCheck,
  Clock3,
  Wallet,
  ChartNoAxesColumn,
  Settings,
  Plus,
  Search,
  Bell,
  ChevronLeft,
  ChevronRight,
  Menu,
  X,
  Check,
  Pencil,
  Download,
  Printer,
  RefreshCw,
  Wifi,
  WifiOff,
  History,
  Archive,
  Trash2,
  Undo2,
  FileText,
  ArrowUpRight,
  ShieldCheck,
  Copy,
  MoreHorizontal,
  AlertCircle,
} from "lucide-react";
import {
  TABLES,
  id,
  today,
  monthNow,
  niceDate,
  formatDays as days,
  formatMoney as money,
  inputNumber as num,
  parseNumber as number,
  balance,
  countDays,
  applyChanges,
  diff,
  receiveSync,
  resolveConflict,
  identical,
  csv,
  backup,
  readBackup,
  validateData,
} from "./model.mjs";
import {
  load,
  transact,
  subscribe,
  api,
  getConnection,
  setConnection,
  disconnect,
  native,
} from "./storage.mjs";
import "./styles.css";
const NAV = [
  ["overview", "Genel Bakış", LayoutDashboard],
  ["employees", "Personeller", Users],
  ["leaves", "İzin Kayıtları", FileText],
  ["calendar", "İzin Takvimi", CalendarDays],
  ["attendance", "Puantaj", Clock3],
  ["payroll", "Maaş / Bordro", Wallet],
  ["reports", "Raporlar", ChartNoAxesColumn],
  ["history", "Değişiklik Geçmişi", History],
  ["settings", "Ayarlar", Settings],
];
const KIND = ["Yıllık izin", "Mazeret", "Rapor", "Ücretsiz izin", "Diğer"];
const values = (d, t) => Object.values(d[t]);
const exportNumber = (n, scale = 1000) => n / scale;
const F = ({ label, help, children, ...p }) => (
  <label className="field">
    <span>{label}</span>
    {children || <input {...p} />}
    {help && <small>{help}</small>}
  </label>
);
function Btn({
  children,
  icon: Icon,
  primary = false,
  danger = false,
  className = "",
  ...p
}) {
  return (
    <button
      type="button"
      className={`btn ${primary ? "primary" : ""} ${danger ? "danger" : ""} ${className}`}
      {...p}
    >
      {Icon && <Icon size={17} />}
      {children}
    </button>
  );
}
function Empty({
  title = "Henüz kayıt yok",
  text = "İlk kaydını ekleyerek başlayabilirsin.",
  action,
}) {
  return (
    <div className="empty">
      <span className="empty-icon">
        <CalendarDays size={29} />
      </span>
      <h3>{title}</h3>
      <p>{text}</p>
      {action}
    </div>
  );
}
function Avatar({ name, index = 0 }) {
  return (
    <span className={"avatar tone" + (index % 4)}>
      {name
        .split(" ")
        .filter(Boolean)
        .map((s) => s[0])
        .slice(0, 2)
        .join("")
        .toLocaleUpperCase("tr")}
    </span>
  );
}
function App() {
  const [state, setState] = useState(null),
    ref = useRef(null),
    queue = useRef(Promise.resolve()),
    syncing = useRef(false);
  const [page, setPage] = useState("overview"),
    [month, setMonth] = useState(monthNow()),
    [search, setSearch] = useState(""),
    [modal, setModal] = useState(null),
    [toast, setToast] = useState(null),
    [drawer, setDrawer] = useState(false),
    [archived, setArchived] = useState(false),
    [network, setNetwork] = useState("idle"),
    [syncError, setSyncError] = useState(""),
    [print, setPrint] = useState(null),
    [deferred, setDeferred] = useState(null);
  const announce = (text, error = false) =>
    setToast({ text, error, at: Date.now() });
  useEffect(() => {
    load()
      .then((s) => {
        ref.current = s;
        setState(s);
      })
      .catch(() =>
        announce(
          "Yerel kayıtlar açılamadı. Depolama iznini ve boş alanı kontrol et.",
          true,
        ),
      );
    const before = (e) => {
      e.preventDefault();
      setDeferred(e);
    };
    window.addEventListener("beforeinstallprompt", before);
    return () => window.removeEventListener("beforeinstallprompt", before);
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 6500);
    return () => clearTimeout(timer);
  }, [toast]);
  const mutate = (fn) => {
    const job = queue.current
      .catch(() => {})
      .then(async () => {
        const next = await transact(fn, ref.current);
        ref.current = next;
        setState(next);
        return next;
      });
    queue.current = job;
    return job;
  };
  useEffect(
    () =>
      subscribe((s) => {
        ref.current = s;
        setState(s);
      }),
    [],
  );
  const sync = async (quiet = false) => {
    if (syncing.current || !ref.current) return;
    if (ref.current.conflicts.length) {
      if (!quiet)
        announce(
          "Önce çakışan kayıtlar için hangi sürümü koruyacağını seç.",
          true,
        );
      return;
    }
    if (!getConnection().url) return;
    if (!navigator.onLine && !native()) {
      setNetwork("offline");
      return;
    }
    syncing.current = true;
    setNetwork("syncing");
    try {
      await queue.current.catch(() => {});
      const response = await api("/api/sync", {
        deviceId: ref.current.deviceId,
        changes: diff(ref.current.data, ref.current.base),
      });
      await mutate((s) => receiveSync(s, response));
      setNetwork(response.ok ? "online" : "conflict");
      setSyncError("");
      if (!quiet)
        announce(
          response.ok
            ? "Kayıtlar eşitlendi."
            : "Çakışan kayıt var. Hangi sürümü koruyacağını seç.",
          !response.ok,
        );
    } catch (e) {
      setNetwork("offline");
      setSyncError(e.message);
      if (!quiet) announce(e.message, true);
    } finally {
      syncing.current = false;
    }
  };
  useEffect(() => {
    if (!state) return;
    const timer = setTimeout(() => sync(true), 1800),
      interval = setInterval(() => sync(true), 25000);
    const online = () => sync(true);
    window.addEventListener("online", online);
    return () => {
      clearTimeout(timer);
      clearInterval(interval);
      window.removeEventListener("online", online);
    };
  }, [Boolean(state)]);
  const commit = async (changes, title, expected) => {
    await mutate((s) => {
      if (
        expected &&
        !identical(s.data[expected.table][expected.id] || null, expected.value)
      )
        throw Error("Bu kayıt form açıkken değişti. Formu kapatıp yeniden aç.");
      const effective =
        typeof changes === "function" ? changes(s.data) : changes;
      return {
        ...s,
        data: applyChanges(s.data, effective, { title, device: s.deviceId }),
      };
    });
    announce("Kayıt kaydedildi.");
    setTimeout(() => sync(true), 500);
  };
  const open = (kind, record = null, extra = {}) =>
    setModal({ kind, record, ...extra });
  const changePage = (p) => {
    setPage(p);
    setSearch("");
    setDrawer(false);
  };
  useEffect(() => {
    window.__izinBack = () => {
      if (modal) {
        setModal(null);
        return true;
      }
      if (drawer) {
        setDrawer(false);
        return true;
      }
      if (page !== "overview") {
        setPage("overview");
        return true;
      }
      return false;
    };
    return () => delete window.__izinBack;
  }, [modal, drawer, page]);
  const download = (filename, body, type) => {
    if (native()) {
      window.Android.saveText(filename, body, type);
      return;
    }
    const url = URL.createObjectURL(new Blob([body], { type }));
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  const remove = (table, r, title) =>
    open("confirm", r, {
      title: "Kayıt silinsin mi?",
      text: "Kayıt bakiyelerden çıkarılır. Değişiklik geçmişinden geri alabilirsin.",
      action: () =>
        commit([{ table, id: r.id, value: null }], title, {
          table,
          id: r.id,
          value: r,
        }),
    });
  const printLeave = (record) => {
    setPrint(record);
    setTimeout(() => {
      if (native()) window.Android.printPage();
      else window.print();
    }, 180);
  };
  if (!state)
    return (
      <div className="loading">
        <div className="brand">
          İzin<span>360</span>
        </div>
        <p>Kayıtların hazırlanıyor…</p>
        {toast && <p role="alert">{toast.text}</p>}
      </div>
    );
  const data = state.data,
    employees = values(data, "employees"),
    active = employees.filter((e) => !e.archived),
    findEmployee = (key) => data.employees[key],
    filterEmployee = (e) =>
      (archived || !e.archived) &&
      `${e.name} ${e.department} ${e.position}`
        .toLocaleLowerCase("tr")
        .includes(search.toLocaleLowerCase("tr"));
  const filtered = employees
    .filter(filterEmployee)
    .sort((a, b) => a.name.localeCompare(b.name, "tr"));
  const leaves = values(data, "leaves")
    .filter(
      (l) =>
        l.start.slice(0, 7) === month &&
        (!search ||
          findEmployee(l.employeeId)
            ?.name.toLocaleLowerCase("tr")
            .includes(search.toLocaleLowerCase("tr"))),
    )
    .sort((a, b) => b.start.localeCompare(a.start));
  const currentLeaves = values(data, "leaves").filter(
    (l) =>
      l.start <= today() &&
      l.end >= today() &&
      !findEmployee(l.employeeId)?.archived,
  );
  const groupedOnLeave = new Set(currentLeaves.map((l) => l.employeeId));
  const pendingCount = diff(data, state.base).filter(
    (c) => c.table !== "audit",
  ).length;
  const status = state.conflicts.length
    ? "Çakışma var"
    : network === "syncing"
      ? "Eşitleniyor"
      : pendingCount && getConnection().url
        ? `${pendingCount} kayıt bekliyor`
        : network === "online"
          ? "Güncel"
          : getConnection().url
            ? "Yerel kayıt"
            : "Çevrimdışı hazır";
  const backupDownload = () =>
    download(`Izin360-Yedek-${today()}.json`, backup(data), "application/json");
  const exportRows = () => {
    let rows;
    if (page === "payroll")
      rows = [
        [
          "Personel",
          "Ay",
          "Temel ücret",
          "Ek ödeme",
          "Kesinti",
          "Net",
          "Ödenen",
          "Kalan",
        ],
        ...values(data, "payroll")
          .filter((r) => r.month === month)
          .map((r) => [
            findEmployee(r.employeeId)?.name,
            r.month,
            exportNumber(r.base, 100),
            exportNumber(r.extra, 100),
            exportNumber(r.deduction, 100),
            exportNumber(r.base + r.extra - r.deduction, 100),
            exportNumber(r.paid, 100),
            exportNumber(r.base + r.extra - r.deduction - r.paid, 100),
          ]),
      ];
    else if (page === "attendance")
      rows = [
        [
          "Personel",
          "Tarih",
          "Durum",
          "Çalışma (dk)",
          "Fazla mesai (dk)",
          "Not",
        ],
        ...values(data, "attendance")
          .filter((r) => r.date.slice(0, 7) === month)
          .map((r) => [
            findEmployee(r.employeeId)?.name,
            r.date,
            r.status,
            r.minutes,
            r.overtime,
            r.note,
          ]),
      ];
    else if (page === "leaves" || page === "calendar")
      rows = [
        [
          "Personel",
          "Tür",
          "Başlangıç",
          "Bitiş",
          "Gün",
          "Bakiyeden düş",
          "Not",
        ],
        ...leaves.map((r) => [
          findEmployee(r.employeeId)?.name,
          r.kind,
          r.start,
          r.end,
          exportNumber(r.days),
          r.deduct ? "Evet" : "Hayır",
          r.note,
        ]),
      ];
    else
      rows = [
        [
          "Personel",
          "Bölüm",
          "İşe giriş",
          "İzin hakkı",
          "Devir",
          "Kullanılan",
          "Düzeltme",
          "Kalan",
          "Arşiv",
        ],
        ...(page === "employees" ? employees.filter(filterEmployee) : active).map((e) => {
          const b = balance(data, e);
          return [
            e.name,
            e.department,
            e.startDate,
            exportNumber(e.entitlement),
            exportNumber(e.carry),
            exportNumber(b.used),
            exportNumber(b.adjustment),
            exportNumber(b.remaining),
            e.archived ? "Evet" : "Hayır",
          ];
        }),
      ];
    download(
      `Izin360-${page}-${month}.csv`,
      csv(rows),
      "text/csv;charset=utf-8",
    );
    announce("Excel ile açılabilen CSV dosyası indirildi.");
  };
  const editEmployee = (e) => open("employee", e);
  const employeeTable = (list, compact = false) => (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Personel</th>
            <th>İzin hakkı</th>
            <th>Devir</th>
            <th>Kullanılan</th>
            <th>Kalan</th>
            <th>İşlem</th>
          </tr>
        </thead>
        <tbody>
          {list.map((e, i) => {
            const b = balance(data, e);
            return (
              <tr key={e.id}>
                <td>
                  <div className="person-cell">
                    <Avatar name={e.name} index={i} />
                    <div>
                      <b>{e.name}</b>
                      <small>
                        {e.department || e.position || "Bölüm belirtilmedi"}
                        {e.archived ? " · Arşiv" : ""}
                      </small>
                    </div>
                  </div>
                </td>
                <td>{days(e.entitlement)}</td>
                <td>{days(e.carry)}</td>
                <td>{days(b.used)}</td>
                <td>
                  <b className={b.remaining < 0 ? "negative" : "positive"}>
                    {days(b.remaining)} gün
                  </b>
                </td>
                <td>
                  <div className="actions">
                    <Btn icon={Pencil} onClick={() => editEmployee(e)}>
                      Düzenle
                    </Btn>
                    {!compact && (
                      <button
                        className="icon-btn"
                        aria-label={`${e.name} bakiyesini düzelt`}
                        title="Bakiyeyi düzelt"
                        onClick={() =>
                          open("adjustment", null, { employeeId: e.id })
                        }
                      >
                        <Pencil size={16} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
  function calendar(compact = false) {
    const first = new Date(month + "-01T12:00:00"),
      offset = (first.getDay() + 6) % 7,
      total = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
    const cells = Array.from(
      { length: Math.ceil((offset + total) / 7) * 7 },
      (_, i) => {
        const d = new Date(first);
        d.setDate(i - offset + 1);
        const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
        return {
          key,
          day: d.getDate(),
          inMonth: key.startsWith(month),
          records: values(data, "leaves").filter(
            (l) =>
              l.start <= key &&
              l.end >= key &&
              !findEmployee(l.employeeId)?.archived,
          ),
        };
      },
    );
    const move = (n) => {
      const d = new Date(month + "-01T12:00:00");
      d.setMonth(d.getMonth() + n);
      setMonth(
        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
      );
    };
    return (
      <section className={"card calendar-card " + (compact ? "compact" : "")}>
        <div className="card-title">
          <h2>
            <CalendarDays />
            İzin Takvimi
          </h2>
          <div className="month-switch">
            <button aria-label="Önceki ay" onClick={() => move(-1)}>
              <ChevronLeft size={17} />
            </button>
            <b>
              {first.toLocaleDateString("tr-TR", {
                month: "long",
                year: "numeric",
              })}
            </b>
            <button aria-label="Sonraki ay" onClick={() => move(1)}>
              <ChevronRight size={17} />
            </button>
          </div>
        </div>
        <div className="calendar-grid">
          {["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"].map((d) => (
            <div className="weekday" key={d}>
              {d}
            </div>
          ))}
          {cells.map((c) => (
            <button
              key={c.key}
              className={`calendar-day ${!c.inMonth ? "muted" : ""} ${c.key === today() ? "today" : ""} ${c.records.length ? "has-leave" : ""}`}
              onClick={() =>
                open("day", null, { date: c.key, records: c.records })
              }
            >
              <span>{c.day}</span>
              {compact
                ? c.records.length > 0 && <i />
                : c.records
                    .slice(0, 3)
                    .map((l) => (
                      <small key={l.id}>
                        {findEmployee(l.employeeId)?.name}
                      </small>
                    ))}
              {!compact && c.records.length > 3 && (
                <small>+{c.records.length - 3} kişi</small>
              )}
            </button>
          ))}
        </div>
        <div className="legend">
          <i />
          İzin kaydı bulunan gün <span>·</span> Gün ayrıntısı için tarihe dokun
        </div>
      </section>
    );
  }
  function leaveTable() {
    return leaves.length ? (
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Personel</th>
              <th>İzin türü</th>
              <th>Tarih aralığı</th>
              <th>Gün</th>
              <th>Bakiye</th>
              <th>İşlem</th>
            </tr>
          </thead>
          <tbody>
            {leaves.map((r) => (
              <tr key={r.id}>
                <td>
                  <b>{findEmployee(r.employeeId)?.name}</b>
                </td>
                <td>
                  <span className={"pill " + (r.deduct ? "purple" : "green")}>
                    {r.kind}
                  </span>
                </td>
                <td>
                  {niceDate(r.start)} — {niceDate(r.end)}
                </td>
                <td>{days(r.days)}</td>
                <td>{r.deduct ? "Düşülür" : "Etkilemez"}</td>
                <td>
                  <div className="actions">
                    <button
                      className="icon-btn"
                      aria-label="İzin düzenle"
                      onClick={() => open("leave", r)}
                    >
                      <Pencil size={17} />
                    </button>
                    <button
                      className="icon-btn"
                      aria-label="İzin formu yazdır"
                      onClick={() => printLeave(r)}
                    >
                      <Printer size={17} />
                    </button>
                    <button
                      className="icon-btn danger"
                      aria-label="İzin sil"
                      onClick={() => remove("leaves", r, "İzin kaydı silindi")}
                    >
                      <Trash2 size={17} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ) : (
      <Empty
        title="Bu ay izin kaydı yok"
        text="İzinleri doğrudan kaydet; gün sayısını ve bakiyeden düşülmesini sen belirle."
        action={
          <Btn primary icon={Plus} onClick={() => open("leave")}>
            İzin kaydı ekle
          </Btn>
        }
      />
    );
  }
  return (
    <>
      <div className="app-shell">
        <aside className={"sidebar " + (drawer ? "open" : "")}>
          <div className="brand">
            İzin<span>360</span>
          </div>
          <small className="sidebar-caption">TEK KULLANICI · TAM KONTROL</small>
          <nav>
            {NAV.map(([key, label, Icon]) => (
              <button
                key={key}
                className={page === key ? "active" : ""}
                onClick={() => changePage(key)}
              >
                <Icon size={20} />
                {label}
              </button>
            ))}
          </nav>
          <div className="sidebar-footer">
            <ShieldCheck size={19} />
            <div>
              <b>Kontrol sende</b>
              <small>Otomatik izin artışı yok</small>
            </div>
          </div>
        </aside>
        {drawer && (
          <button
            className="scrim"
            aria-label="Menüyü kapat"
            onClick={() => setDrawer(false)}
          />
        )}
        <main>
          <header className="topbar">
            <button
              className="icon-btn mobile-menu"
              aria-label="Menüyü aç"
              onClick={() => setDrawer(true)}
            >
              <Menu />
            </button>
            <div className="breadcrumb">
              PERSONEL YÖNETİMİ <span>/</span>{" "}
              {NAV.find((n) => n[0] === page)[1]}
            </div>
            <div className="topbar-right">
              <button
                className={
                  "connection " + (network === "online" ? "online" : "")
                }
                onClick={() => sync()}
                title={syncError || "Şimdi eşitle"}
              >
                {network === "syncing" ? (
                  <RefreshCw size={14} className="spin" />
                ) : network === "online" ? (
                  <Wifi size={14} />
                ) : (
                  <WifiOff size={14} />
                )}{" "}
                {status}
              </button>
              <button
                className="icon-btn"
                aria-label="Bildirimler"
                onClick={() => open("notifications")}
              >
                <Bell size={19} />
                {(groupedOnLeave.size > 0 || state.conflicts.length > 0) && (
                  <i className="notification-dot" />
                )}
              </button>
              <span className="owner-avatar">TK</span>
            </div>
          </header>
          <div className="page">
            <div className="page-heading">
              <div>
                <div className="eyebrow">İZİN360 · SENİN ÇALIŞMA DÜZENİN</div>
                <h1>{NAV.find((n) => n[0] === page)[1]}</h1>
                <p>
                  {
                    {
                      overview:
                        "Personel kayıtların ve izin bakiyeleri, kontrol sende.",
                      employees:
                        "Her personelin bilgilerini ve izin hakkını sen belirle.",
                      leaves:
                        "İzinleri doğrudan kaydet, gerektiğinde her alanı düzenle.",
                      calendar: "Ekibinin izinlerini tek takvimde gör.",
                      attendance:
                        "Günlük çalışma ve fazla mesai kayıtlarını düzenle.",
                      payroll:
                        "Ücret, ek ödeme, kesinti ve yapılan ödemeleri takip et.",
                      reports:
                        "Kayıtlarından güncel özetler ve Excel raporları al.",
                      history: "Düzenlemeleri izle, uygun kayıtları geri al.",
                      settings:
                        "Windows bağlantısı, yedekler ve uygulama ayarları.",
                    }[page]
                  }
                </p>
              </div>
              <div className="heading-actions">
                {[
                  "overview",
                  "leaves",
                  "calendar",
                  "attendance",
                  "payroll",
                  "reports",
                ].includes(page) && (
                  <input
                    className="month-input"
                    aria-label="Takip ayı"
                    type="month"
                    value={month}
                    onChange={(e) => e.target.value && setMonth(e.target.value)}
                  />
                )}
                {page === "overview" || page === "employees" ? (
                  <Btn primary icon={Plus} onClick={() => editEmployee(null)}>
                    Personel Ekle
                  </Btn>
                ) : null}
                {["overview", "leaves", "calendar"].includes(page) && (
                  <Btn
                    primary={page !== "overview"}
                    icon={Plus}
                    onClick={() => open("leave")}
                  >
                    İzin Kaydı
                  </Btn>
                )}
                {page === "attendance" && (
                  <Btn primary icon={Plus} onClick={() => open("attendance")}>
                    Puantaj Ekle
                  </Btn>
                )}
                {page === "payroll" && (
                  <Btn primary icon={Plus} onClick={() => open("payroll")}>
                    Maaş Kaydı Ekle
                  </Btn>
                )}
              </div>
            </div>
            {state.conflicts.length > 0 && (
              <div className="banner warning">
                <AlertCircle size={19} />
                <span>
                  {state.conflicts.length} kayıt iki cihazda farklı düzenlenmiş.
                  Yerel değişikliklerin korunuyor.
                </span>
                <Btn onClick={() => open("conflicts")}>
                  Sürümleri karşılaştır
                </Btn>
              </div>
            )}
            {page === "overview" && (
              <>
                <div className="stats">
                  <Stat
                    title="Toplam Personel"
                    value={active.length}
                    note="Aktif personel"
                    icon={Users}
                    color="blue"
                  />
                  <Stat
                    title="Bugün İzinli"
                    value={groupedOnLeave.size}
                    note="İzin kayıtlarına göre"
                    icon={CalendarCheck}
                    color="mint"
                  />
                  <Stat
                    title="Bu Ay Kullanılan"
                    value={
                      days(
                        leaves
                          .filter((l) => l.deduct)
                          .reduce((a, l) => a + l.days, 0),
                      ) + " gün"
                    }
                    note="İzni bu ay başlayanlar"
                    icon={Clock3}
                    color="peach"
                  />
                  <Stat
                    title="Toplam Kalan İzin"
                    value={
                      days(
                        active.reduce(
                          (a, e) => a + balance(data, e).remaining,
                          0,
                        ),
                      ) + " gün"
                    }
                    note="Tüm kayıtlar dahil"
                    icon={ChartNoAxesColumn}
                    color="violet"
                  />
                </div>
                <section className="card">
                  <div className="card-title">
                    <h2>
                      <Users />
                      Personel İzin Bakiyeleri
                    </h2>
                    <button
                      className="text-btn"
                      onClick={() => changePage("employees")}
                    >
                      Tüm personeller <ArrowUpRight size={15} />
                    </button>
                  </div>
                  {active.length ? (
                    employeeTable(active.slice(0, 5), true)
                  ) : (
                    <Empty
                      title="İlk personelini ekle"
                      text="İzin hakkı 0 gün başlar. Hak, devir ve önceki kullanımı istediğin zaman düzenleyebilirsin."
                      action={
                        <Btn
                          primary
                          icon={Plus}
                          onClick={() => editEmployee(null)}
                        >
                          Personel Ekle
                        </Btn>
                      }
                    />
                  )}
                  <div className="card-foot">
                    <ShieldCheck size={14} /> Kalan = İzin hakkı + Devir −
                    Kullanılan + Düzeltme
                  </div>
                </section>
                <div className="two-col">
                  {calendar(true)}
                  <section className="card control-card">
                    <div className="card-title">
                      <h2>
                        <Pencil />
                        Manuel kontrol
                      </h2>
                    </div>
                    <div className="control-art">
                      <Pencil size={29} />
                      <div>
                        <h3>Her kayıt düzenlenebilir.</h3>
                        <p>
                          İzin hakkını sen gir. Eksik veya eski kayıtları artı /
                          eksi gün düzeltmesiyle dengele.
                        </p>
                      </div>
                    </div>
                    <Btn
                      primary
                      icon={Pencil}
                      onClick={() => open("adjustment")}
                    >
                      İzin Bakiyesini Düzelt
                    </Btn>
                    <Btn icon={History} onClick={() => changePage("history")}>
                      Değişiklik Geçmişi
                    </Btn>
                    <div className="mini-note">
                      <ShieldCheck size={16} />
                      Kayıtlar cihazda saklanır. Bağlantı kurduğunda Windows ve
                      Android eşitlenir.
                    </div>
                  </section>
                </div>
              </>
            )}
            {page === "employees" && (
              <>
                <div className="toolbar">
                  <div className="search">
                    <Search size={17} />
                    <input
                      placeholder="Personel, bölüm veya görev ara…"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                    />
                  </div>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={archived}
                      onChange={(e) => setArchived(e.target.checked)}
                    />{" "}
                    Arşivdekileri göster
                  </label>
                  <Btn icon={Download} onClick={exportRows}>
                    Excel’e Aktar
                  </Btn>
                </div>
                <section className="card">
                  {filtered.length ? (
                    employeeTable(filtered)
                  ) : (
                    <Empty
                      title={
                        search
                          ? "Aramana uygun personel yok"
                          : "Personel listen hazır"
                      }
                      action={
                        <Btn
                          primary
                          icon={Plus}
                          onClick={() => editEmployee(null)}
                        >
                          İlk Personeli Ekle
                        </Btn>
                      }
                    />
                  )}
                </section>
              </>
            )}
            {page === "leaves" && (
              <>
                <div className="toolbar">
                  <div className="search">
                    <Search size={17} />
                    <input
                      placeholder="Personel ara…"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                    />
                  </div>
                  <Btn icon={Download} onClick={exportRows}>
                    Excel’e Aktar
                  </Btn>
                </div>
                <section className="card">
                  <div className="card-title">
                    <h2>
                      <FileText />
                      İzin kayıtları
                    </h2>
                    <small>Başlangıç ayına göre listelenir</small>
                  </div>
                  {leaveTable()}
                </section>
                <div className="mini-note">
                  Gün sayısını elle değiştirebilirsin. Çakışan tarihler
                  uyarılır; geçmiş ve gelecek kayıtları bakiye hesabına dahil
                  edilir.
                </div>
              </>
            )}
            {page === "calendar" && calendar(false)}
            {page === "attendance" && (
              <>
                <div className="toolbar">
                  <span className="muted-text">
                    {month} · Çalışma süreleri dakika olarak kaydedilir.
                  </span>
                  <Btn icon={Download} onClick={exportRows}>
                    Excel’e Aktar
                  </Btn>
                </div>
                <section className="card">
                  {values(data, "attendance").some((r) =>
                    r.date.startsWith(month),
                  ) ? (
                    <div className="table-scroll">
                      <table>
                        <thead>
                          <tr>
                            <th>Personel</th>
                            <th>Tarih</th>
                            <th>Durum</th>
                            <th>Çalışma</th>
                            <th>Fazla mesai</th>
                            <th>İşlem</th>
                          </tr>
                        </thead>
                        <tbody>
                          {values(data, "attendance")
                            .filter((r) => r.date.startsWith(month))
                            .sort((a, b) => b.date.localeCompare(a.date))
                            .map((r) => (
                              <tr key={r.id}>
                                <td>
                                  <b>{findEmployee(r.employeeId)?.name}</b>
                                </td>
                                <td>{niceDate(r.date)}</td>
                                <td>
                                  <span className="pill green">{r.status}</span>
                                </td>
                                <td>{r.minutes} dk</td>
                                <td>{r.overtime} dk</td>
                                <td>
                                  <div className="actions">
                                    <button
                                      className="icon-btn"
                                      aria-label="Puantaj düzenle"
                                      onClick={() => open("attendance", r)}
                                    >
                                      <Pencil size={17} />
                                    </button>
                                    <button
                                      className="icon-btn danger"
                                      aria-label="Puantaj sil"
                                      onClick={() =>
                                        remove(
                                          "attendance",
                                          r,
                                          "Puantaj kaydı silindi",
                                        )
                                      }
                                    >
                                      <Trash2 size={17} />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <Empty
                      title="Bu ay puantaj kaydı yok"
                      text="Çalışma günlerini, izinleri ve fazla mesaileri elle kaydet."
                      action={
                        <Btn
                          primary
                          icon={Plus}
                          onClick={() => open("attendance")}
                        >
                          Puantaj Ekle
                        </Btn>
                      }
                    />
                  )}
                </section>
              </>
            )}
            {page === "payroll" && (
              <>
                <div className="banner">
                  <Wallet size={19} />
                  <span>
                    Bu bölüm manuel ödeme takibidir. Resmî SGK / vergi bordrosu
                    hesaplamaz.
                  </span>
                  <Btn icon={Download} onClick={exportRows}>
                    Excel’e Aktar
                  </Btn>
                </div>
                <section className="card">
                  {values(data, "payroll").some((r) => r.month === month) ? (
                    <div className="table-scroll">
                      <table>
                        <thead>
                          <tr>
                            <th>Personel</th>
                            <th>Temel ücret</th>
                            <th>Ek / Kesinti</th>
                            <th>Net</th>
                            <th>Ödenen</th>
                            <th>Kalan</th>
                            <th>İşlem</th>
                          </tr>
                        </thead>
                        <tbody>
                          {values(data, "payroll")
                            .filter((r) => r.month === month)
                            .map((r) => (
                              <tr key={r.id}>
                                <td>
                                  <b>{findEmployee(r.employeeId)?.name}</b>
                                </td>
                                <td>{money(r.base)}</td>
                                <td>
                                  {money(r.extra)} / {money(r.deduction)}
                                </td>
                                <td>
                                  <b>{money(r.base + r.extra - r.deduction)}</b>
                                </td>
                                <td>{money(r.paid)}</td>
                                <td className="positive">
                                  {money(
                                    r.base + r.extra - r.deduction - r.paid,
                                  )}
                                </td>
                                <td>
                                  <div className="actions">
                                    <button
                                      className="icon-btn"
                                      aria-label="Maaş düzenle"
                                      onClick={() => open("payroll", r)}
                                    >
                                      <Pencil size={17} />
                                    </button>
                                    <button
                                      className="icon-btn danger"
                                      aria-label="Maaş sil"
                                      onClick={() =>
                                        remove(
                                          "payroll",
                                          r,
                                          "Maaş kaydı silindi",
                                        )
                                      }
                                    >
                                      <Trash2 size={17} />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <Empty
                      title="Bu ay maaş kaydı yok"
                      text="Temel ücret, ek ödeme, kesinti ve ödenen tutarı sen gir."
                      action={
                        <Btn
                          primary
                          icon={Plus}
                          onClick={() => open("payroll")}
                        >
                          Maaş Kaydı Ekle
                        </Btn>
                      }
                    />
                  )}
                </section>
              </>
            )}
            {page === "reports" && (
              <>
                <div className="stats">
                  <Stat
                    title="Aktif Personel"
                    value={active.length}
                    icon={Users}
                    color="blue"
                  />
                  <Stat
                    title="İzin Kaydı"
                    value={leaves.length}
                    note="Başlangıcı seçili ayda"
                    icon={CalendarDays}
                    color="violet"
                  />
                  <Stat
                    title="Çalışma"
                    value={
                      values(data, "attendance")
                        .filter((r) => r.date.startsWith(month))
                        .reduce((a, r) => a + r.minutes, 0) + " dk"
                    }
                    icon={Clock3}
                    color="mint"
                  />
                  <Stat
                    title="Ödenen Ücret"
                    value={money(
                      values(data, "payroll")
                        .filter((r) => r.month === month)
                        .reduce((a, r) => a + r.paid, 0),
                    )}
                    icon={Wallet}
                    color="peach"
                  />
                </div>
                <section className="card">
                  <div className="card-title">
                    <h2>
                      <ChartNoAxesColumn />
                      Personel bakiye raporu
                    </h2>
                    <Btn icon={Download} onClick={exportRows}>
                      Excel’e Aktar
                    </Btn>
                  </div>
                  {active.length ? employeeTable(active, true) : <Empty />}
                </section>
                <div className="two-col">
                  <section className="card">
                    <h3>Diğer raporlar</h3>
                    <p>
                      İzin Kayıtları, Puantaj ve Maaş ekranlarında seçili ayın
                      raporunu ayrıca Excel’e aktarabilirsin.
                    </p>
                    <Btn icon={FileText} onClick={() => changePage("leaves")}>
                      İzin Raporuna Git
                    </Btn>
                  </section>
                  <section className="card">
                    <h3>Tüm kayıtların yedeği</h3>
                    <p>
                      JSON yedeği, düzenleme geçmişi dahil tüm personel
                      kayıtlarını içerir.
                    </p>
                    <Btn icon={Download} onClick={backupDownload}>
                      Tam Yedek İndir
                    </Btn>
                  </section>
                </div>
              </>
            )}
            {page === "history" && (
              <>
                <section className="card">
                  <div className="card-title">
                    <h2>
                      <Pencil />
                      Bakiye Düzeltmeleri
                    </h2>
                    <Btn primary icon={Plus} onClick={() => open("adjustment")}>
                      Düzeltme Ekle
                    </Btn>
                  </div>
                  {values(data, "adjustments").length ? (
                    <div className="table-scroll">
                      <table>
                        <thead>
                          <tr>
                            <th>Personel</th>
                            <th>Tarih</th>
                            <th>Gün</th>
                            <th>Açıklama</th>
                            <th>İşlem</th>
                          </tr>
                        </thead>
                        <tbody>
                          {values(data, "adjustments")
                            .sort((a, b) => b.date.localeCompare(a.date))
                            .map((a) => (
                              <tr key={a.id}>
                                <td>{findEmployee(a.employeeId)?.name}</td>
                                <td>{niceDate(a.date)}</td>
                                <td>
                                  {a.amount > 0 ? "+" : ""}
                                  {days(a.amount)}
                                </td>
                                <td>{a.reason}</td>
                                <td>
                                  <div className="actions">
                                    <button
                                      className="icon-btn"
                                      aria-label="Düzeltmeyi düzenle"
                                      onClick={() => open("adjustment", a)}
                                    >
                                      <Pencil size={16} />
                                    </button>
                                    <button
                                      className="icon-btn danger"
                                      aria-label="Düzeltmeyi sil"
                                      onClick={() =>
                                        remove(
                                          "adjustments",
                                          a,
                                          "Bakiye düzeltmesi silindi",
                                        )
                                      }
                                    >
                                      <Trash2 size={16} />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p>Henüz manuel bakiye düzeltmesi yok.</p>
                  )}
                </section>
                <section className="card">
                  <div className="card-title">
                    <h2>
                      <History />
                      Son değişiklikler
                    </h2>
                    <small>Son 100 kayıt · Tam geçmiş yedekte korunur</small>
                  </div>
                  {values(data, "audit").length ? (
                    values(data, "audit")
                      .sort((a, b) => b.at.localeCompare(a.at))
                      .slice(0, 100)
                      .map((a) => (
                        <div className="history-row" key={a.id}>
                          <span className="history-icon">
                            <Pencil size={16} />
                          </span>
                          <div>
                            <b>{a.title}</b>
                            <p>
                              {new Date(a.at).toLocaleString("tr-TR")} ·{" "}
                              {a.after?.name ||
                                a.before?.name ||
                                findEmployee(
                                  a.after?.employeeId || a.before?.employeeId,
                                )?.name ||
                                "Genel kayıt"}
                            </p>
                          </div>
                          <Btn
                            icon={Undo2}
                            disabled={
                              !identical(
                                data[a.table][a.entityId] || null,
                                a.after,
                              )
                            }
                            onClick={() =>
                              open("confirm", a, {
                                title: "Değişiklik geri alınsın mı?",
                                text: "Önceki değer geri gelir. Sonrasında değiştirilmiş kayıtlar için geri alma kapalıdır.",
                                action: () =>
                                  commit(
                                    [
                                      {
                                        table: a.table,
                                        id: a.entityId,
                                        value: a.before,
                                      },
                                    ],
                                    `${a.title} · Geri alındı`,
                                    {
                                      table: a.table,
                                      id: a.entityId,
                                      value: a.after,
                                    },
                                  ),
                              })
                            }
                          >
                            Geri Al
                          </Btn>
                        </div>
                      ))
                  ) : (
                    <Empty
                      title="Geçmişin burada görünecek"
                      text="Yeni kayıtlar ve düzenlemeler otomatik olarak geçmişe eklenir."
                    />
                  )}
                </section>
              </>
            )}
            {page === "settings" && (
              <SettingsPage
                data={data}
                state={state}
                commit={commit}
                sync={sync}
                network={network}
                syncError={syncError}
                announce={announce}
                open={open}
                downloadBackup={backupDownload}
                deferred={deferred}
                onImport={(text) => {
                  const incoming = readBackup(text);
                  open("confirm", null, {
                    title: "Yedek geri yüklensin mi?",
                    text: "Mevcut kayıtlar bu dosyanın içeriğiyle değiştirilir. Bağlantı kurulduğunda değişiklikler diğer cihaza da eşitlenir. Önce mevcut yedeğini indirmeni öneririz.",
                    action: async () => {
                      await mutate((s) => ({
                        ...s,
                        data: incoming,
                        conflicts: [],
                      }));
                      announce("Yedek geri yüklendi.");
                    },
                  });
                }}
              />
            )}
            <footer className="page-footer">
              <span>
                İzin360 <b>1.0</b> · Tek kullanıcı
              </span>
              <span>
                <ShieldCheck size={13} /> Yeni personelde izin hakkı 0 gün
              </span>
            </footer>
          </div>
        </main>
        <nav className="bottom-nav">
          {[
            ["overview", "Özet", LayoutDashboard],
            ["employees", "Personel", Users],
            ["leaves", "İzinler", CalendarDays],
          ].map(([key, label, Icon]) => (
            <button
              key={key}
              className={page === key ? "active" : ""}
              onClick={() => changePage(key)}
            >
              <Icon size={21} />
              {label}
            </button>
          ))}
          <button
            className={
              !["overview", "employees", "leaves"].includes(page)
                ? "active"
                : ""
            }
            onClick={() => setDrawer(true)}
          >
            <MoreHorizontal size={22} />
            Diğer
          </button>
        </nav>
      </div>
      {toast && (
        <div
          role="status"
          className={"toast " + (toast.error ? "error" : "")}
          key={toast.at}
        >
          {toast.error ? <AlertCircle size={19} /> : <Check size={19} />}
          <span>{toast.text}</span>
          <button aria-label="Mesajı kapat" onClick={() => setToast(null)}>
            <X size={16} />
          </button>
        </div>
      )}
      {modal && (
        <Modal
          key={`${modal.kind}:${modal.record?.id || "new"}:${modal.employeeId || ""}:${modal.date || ""}`}
          modal={modal}
          close={() => setModal(null)}
          data={data}
          state={state}
          commit={commit}
          month={month}
          announce={announce}
          printLeave={printLeave}
          mutate={mutate}
          open={open}
        />
      )}
      {print && (
        <div className="print-page">
          <h1>{data.settings.organization?.name || "PERSONEL İZİN FORMU"}</h1>
          <h2>Personel İzin Formu</h2>
          <dl>
            {[
              ["Ad Soyad", findEmployee(print.employeeId)?.name],
              ["Bölüm / Görev", findEmployee(print.employeeId)?.department],
              ["İzin Türü", print.kind],
              ["Başlangıç", niceDate(print.start)],
              ["Bitiş", niceDate(print.end)],
              ["Gün", days(print.days)],
              ["Bakiyeden düşülür", print.deduct ? "Evet" : "Hayır"],
              ["Açıklama", print.note || "—"],
            ].map(([label, value]) => (
              <React.Fragment key={label}>
                <dt>{label}</dt>
                <dd>{value}</dd>
              </React.Fragment>
            ))}
          </dl>
          <div className="signatures">
            <span>Personel İmzası</span>
            <span>
              {data.settings.organization?.manager || "Yetkili İmzası"}
            </span>
          </div>
          <small>İzin360 · Kayıt tarihi: {niceDate(today())}</small>
        </div>
      )}
    </>
  );
}
function Stat({ title, value, note, icon: Icon, color }) {
  return (
    <section className={"stat " + color}>
      <div className="stat-top">
        <span className="stat-icon">
          <Icon size={20} />
        </span>
        <span>{title}</span>
      </div>
      <strong>{value}</strong>
      {note && <small>{note}</small>}
    </section>
  );
}

function Modal({
  modal,
  close,
  data,
  state,
  commit,
  month,
  announce,
  printLeave,
  mutate,
  open,
}) {
  const { kind, record: r } = modal;
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [form, setForm] = useState(() => {
      if (kind === "employee")
        return {
          name: r?.name || "",
          department: r?.department || "",
          position: r?.position || "",
          phone: r?.phone || "",
          startDate: r?.startDate || today(),
          entitlement: num(r?.entitlement || 0),
          carry: num(r?.carry || 0),
          used: num(r ? balance(data, r).used : 0),
          note: r?.note || "",
          archived: r?.archived || false,
        };
      const employeeId =
        r?.employeeId ||
        modal.employeeId ||
        Object.values(data.employees).find((e) => !e.archived)?.id ||
        "";
      if (kind === "leave")
        return {
          employeeId,
          kind: r?.kind || "Yıllık izin",
          start: r?.start || modal.date || today(),
          end: r?.end || modal.date || today(),
          days: num(r?.days || 1000),
          deduct: r?.deduct ?? true,
          weekdaysOnly: r?.weekdaysOnly || false,
          daysManual: Boolean(r),
          note: r?.note || "",
          allowOverlap: false,
        };
      if (kind === "adjustment")
        return {
          employeeId,
          mode: "delta",
          amount: num(r?.amount || 0),
          date: r?.date || today(),
          reason: r?.reason || "",
        };
      if (kind === "attendance")
        return {
          employeeId,
          date: r?.date || today(),
          status: r?.status || "Çalıştı",
          minutes: String(r?.minutes ?? 480),
          overtime: String(r?.overtime || 0),
          note: r?.note || "",
        };
      if (kind === "payroll")
        return {
          employeeId,
          month: r?.month || month,
          base: num(r?.base || 0, 100),
          extra: num(r?.extra || 0, 100),
          deduction: num(r?.deduction || 0, 100),
          paid: num(r?.paid || 0, 100),
          note: r?.note || "",
        };
      return {};
    });
  const dialogRef = useRef(null),
    firstFocus = useRef(null),
    saveLock = useRef(false),
    pristine = useRef(JSON.stringify(form));
  useEffect(() => {
    firstFocus.current = document.activeElement;
    dialogRef.current.showModal();
    return () => firstFocus.current?.focus?.();
  }, []);
  const update = (key, val) =>
    setForm((f) => {
      const next = { ...f, [key]: val };
      if (kind === "leave") {
        if (key === "days") next.daysManual = true;
        if (["start", "end", "weekdaysOnly"].includes(key) && !f.daysManual) {
          try {
            next.days = num(countDays(next.start, next.end, next.weekdaysOnly));
          } catch {}
        }
      }
      return next;
    });
  const field = (key, label, props = {}) => (
    <F
      label={label}
      {...props}
      value={form[key] ?? ""}
      onChange={(e) => update(key, e.target.value)}
    />
  );
  const personSelect = (
    <F label="Personel *">
      <select
        required
        value={form.employeeId || ""}
        onChange={(e) => update("employeeId", e.target.value)}
      >
        <option value="">Personel seç</option>
        {values(data, "employees")
          .filter((e) => !e.archived || e.id === form.employeeId)
          .map((e) => (
            <option key={e.id} value={e.id}>
              {e.name}
              {e.archived ? " (arşiv)" : ""}
            </option>
          ))}
      </select>
    </F>
  );
  const execute = async (fn) => {
    if (saveLock.current) return;
    saveLock.current = true;
    setBusy(true);
    setError("");
    try {
      await fn();
      close();
    } catch (e) {
      setError(e.message || "İşlem tamamlanamadı.");
    } finally {
      saveLock.current = false;
      setBusy(false);
    }
  };
  const save = async (e) => {
    e.preventDefault();
    await execute(async () => {
      const key = r?.id || id();
      let table, value, title;
      const note = String(form.note || "").trim();
      if (kind === "employee") {
        table = "employees";
        const targetUsed = number(form.used, 1000, { signed: true });
        value = {
          id: key,
          name: form.name.trim(),
          department: form.department.trim(),
          position: form.position.trim(),
          phone: form.phone.trim(),
          startDate: form.startDate,
          entitlement: number(form.entitlement),
          carry: number(form.carry, 1000, { signed: true }),
          openingUsed: targetUsed - (r ? balance(data, r).recorded : 0),
          note,
          archived: form.archived,
        };
        title = r ? "Personel bilgileri güncellendi" : "Personel eklendi";
      } else if (kind === "leave") {
        table = "leaves";
        value = {
          id: key,
          employeeId: form.employeeId,
          kind: form.kind.trim(),
          start: form.start,
          end: form.end,
          days: number(form.days),
          deduct: form.deduct,
          weekdaysOnly: form.weekdaysOnly,
          note,
        };
        if (
          !form.allowOverlap &&
          values(data, "leaves").some(
            (l) =>
              l.id !== key &&
              l.employeeId === value.employeeId &&
              l.start <= value.end &&
              l.end >= value.start,
          )
        )
          throw Error(
            "Bu personelin aynı tarihlerde izin kaydı var. Kontrol edip çakışmaya izin ver kutusunu işaretleyebilirsin.",
          );
        title = r ? "İzin kaydı güncellendi" : "İzin kaydı eklendi";
      } else if (kind === "adjustment") {
        table = "adjustments";
        const emp = data.employees[form.employeeId];
        if (!emp) throw Error("Personel seç.");
        const amount = number(form.amount, 1000, { signed: true });
        value = {
          id: key,
          employeeId: form.employeeId,
          date: form.date,
          amount:
            form.mode === "target"
              ? amount - balance(data, emp).remaining + (r?.amount || 0)
              : amount,
          reason: form.reason.trim(),
        };
        if (!value.amount)
          throw Error(
            "Düzeltme miktarı sıfır. Değiştirmek istediğin bakiyeyi gir.",
          );
        title = "İzin bakiyesi elle düzeltildi";
      } else if (kind === "attendance") {
        table = "attendance";
        if (!/^\d+$/.test(form.minutes) || !/^\d+$/.test(form.overtime))
          throw Error("Süreleri dakika olarak tam sayı gir.");
        value = {
          id: key,
          employeeId: form.employeeId,
          date: form.date,
          status: form.status,
          minutes: Number(form.minutes),
          overtime: Number(form.overtime),
          note,
        };
        title = r ? "Puantaj güncellendi" : "Puantaj kaydı eklendi";
      } else if (kind === "payroll") {
        table = "payroll";
        value = {
          id: key,
          employeeId: form.employeeId,
          month: form.month,
          base: number(form.base, 100),
          extra: number(form.extra, 100),
          deduction: number(form.deduction, 100),
          paid: number(form.paid, 100),
          note,
        };
        title = r ? "Maaş kaydı güncellendi" : "Maaş kaydı eklendi";
      }
      let changes = [{ table, id: key, value }];
      if (kind === "employee")
        changes = (latest) => [
          {
            table,
            id: key,
            value: {
              ...value,
              openingUsed:
                number(form.used, 1000, { signed: true }) -
                (r ? balance(latest, latest.employees[r.id]).recorded : 0),
            },
          },
        ];
      if (kind === "adjustment" && form.mode === "target")
        changes = (latest) => [
          {
            table,
            id: key,
            value: {
              ...value,
              amount:
                number(form.amount, 1000, { signed: true }) -
                balance(latest, latest.employees[form.employeeId]).remaining +
                (r?.amount || 0),
            },
          },
        ];
      await commit(changes, title, { table, id: key, value: r || null });
    });
  };
  const titles = {
    employee: r ? "Personeli Düzenle" : "Yeni Personel",
    leave: r ? "İzin Kaydını Düzenle" : "İzin Kaydı Ekle",
    adjustment: "İzin Bakiyesini Düzelt",
    attendance: r ? "Puantajı Düzenle" : "Puantaj Ekle",
    payroll: r ? "Maaş Kaydını Düzenle" : "Maaş Kaydı Ekle",
    day: niceDate(modal.date) + " · İzin kayıtları",
    confirm: modal.title,
    notifications: "Güncel Durum",
    conflicts: "Kayıtları Karşılaştır",
  };
  const calculatedRemaining = () => {
    try {
      return days(
        number(form.entitlement) +
          number(form.carry, 1000, { signed: true }) -
          number(form.used, 1000, { signed: true }) +
          (r ? balance(data, r).adjustment : 0),
      );
    } catch {
      return "—";
    }
  };
  const adjustmentBalance =
    form.employeeId && data.employees[form.employeeId]
      ? balance(data, data.employees[form.employeeId]).remaining
      : 0;
  return (
    <dialog
      ref={dialogRef}
      className={
        "modal " + (["confirm", "notifications"].includes(kind) ? "small" : "")
      }
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) close();
      }}
      onClick={(e) => {
        if (e.target === dialogRef.current && !busy) close();
      }}
    >
      <div className="modal-head">
        <div>
          <div className="eyebrow">İZİN360 · MANUEL KONTROL</div>
          <h2>{titles[kind]}</h2>
          {kind === "employee" && !r && <p>İzin hakkı başlangıçta 0 gündür.</p>}
        </div>
        <button
          className="icon-btn"
          aria-label="Formu kapat"
          disabled={busy}
          onClick={close}
        >
          <X size={21} />
        </button>
      </div>
      <div className="modal-body">
        {error && (
          <div role="alert" className="form-error">
            <AlertCircle size={18} />
            {error}
          </div>
        )}
        {["employee", "leave", "adjustment", "attendance", "payroll"].includes(
          kind,
        ) && (
          <form onSubmit={save}>
            {kind === "employee" && (
              <>
                {field("name", "Ad Soyad *", {
                  required: true,
                  maxLength: 160,
                  autoFocus: true,
                  placeholder: "Personelin adı soyadı",
                })}
                <div className="form-grid">
                  {field("department", "Bölüm", {
                    maxLength: 160,
                    placeholder: "Bölüm gir",
                  })}
                  {field("position", "Görev", {
                    maxLength: 160,
                    placeholder: "Görev gir",
                  })}
                  {field("startDate", "İşe Giriş Tarihi *", {
                    type: "date",
                    required: true,
                  })}
                  {field("phone", "Telefon", { type: "tel", maxLength: 40 })}
                  {field("entitlement", "İzin Hakkı (gün) *", {
                    inputMode: "decimal",
                    required: true,
                  })}
                  {field("carry", "Devir (gün)", {
                    inputMode: "decimal",
                    required: true,
                  })}
                  {field("used", "Kullanılan İzin (gün)", {
                    inputMode: "decimal",
                    required: true,
                    help: "Elle değiştirmen eski kullanım düzeltmesini günceller; izin kayıtlarını silmez.",
                  })}
                  <div className="balance-preview">
                    <CalendarCheck size={22} />
                    <div>
                      <small>Kalan İzin</small>
                      <strong>{calculatedRemaining()} gün</strong>
                    </div>
                  </div>
                </div>
                {field("note", "Not", { maxLength: 3000 })}
                <div className="mini-note">
                  <ShieldCheck size={16} />
                  İzin hakkını sen belirlersin. İşe girişe veya yıl değişimine
                  göre otomatik hak eklenmez.
                </div>
                {r && (
                  <>
                    <Btn
                      icon={Pencil}
                      onClick={() => {
                        if (JSON.stringify(form) !== pristine.current) {
                          setError(
                            "Önce personel formundaki değişiklikleri kaydet.",
                          );
                          return;
                        }
                        open("adjustment", null, { employeeId: r.id });
                      }}
                    >
                      Kalan Bakiyeyi Elle Düzelt
                    </Btn>
                    <label className="check">
                      <input
                        type="checkbox"
                        checked={form.archived}
                        onChange={(e) => update("archived", e.target.checked)}
                      />{" "}
                      Personeli arşivle (kayıtları korunur)
                    </label>
                  </>
                )}
              </>
            )}
            {kind === "leave" && (
              <>
                {personSelect}
                <F label="İzin Türü *">
                  <input
                    required
                    list="leave-kinds"
                    value={form.kind}
                    maxLength={100}
                    onChange={(e) => {
                      const k = e.target.value;
                      setForm((f) => ({
                        ...f,
                        kind: k,
                        deduct: k === "Yıllık izin",
                      }));
                    }}
                  />
                  <datalist id="leave-kinds">
                    {KIND.map((k) => (
                      <option key={k}>{k}</option>
                    ))}
                  </datalist>
                </F>
                <div className="form-grid">
                  {field("start", "Başlangıç *", {
                    type: "date",
                    required: true,
                  })}
                  {field("end", "Bitiş *", { type: "date", required: true })}
                </div>
                <div className="calculation-row">
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={form.weekdaysOnly}
                      onChange={(e) => update("weekdaysOnly", e.target.checked)}
                    />{" "}
                    Hesaplarken cumartesi / pazarı çıkar
                  </label>
                  <Btn
                    icon={RefreshCw}
                    onClick={() => {
                      try {
                        update(
                          "days",
                          num(
                            countDays(form.start, form.end, form.weekdaysOnly),
                          ),
                        );
                      } catch (e) {
                        setError(e.message);
                      }
                    }}
                  >
                    Gün Hesapla
                  </Btn>
                </div>
                {field("days", "İzin Süresi (gün) *", {
                  inputMode: "decimal",
                  required: true,
                  help: "Başlangıç ve bitiş dahil hesaplanır. Resmî tatiller otomatik çıkarılmaz. 0,5 gibi değerleri elle girebilirsin.",
                })}
                <label className="check">
                  <input
                    type="checkbox"
                    checked={form.deduct}
                    onChange={(e) => update("deduct", e.target.checked)}
                  />{" "}
                  İzin bakiyesinden düş
                </label>
                <label className="check">
                  <input
                    type="checkbox"
                    checked={form.allowOverlap}
                    onChange={(e) => update("allowOverlap", e.target.checked)}
                  />{" "}
                  Tarih çakışmasını kontrol ettim, bu kayda izin ver
                </label>
                {field("note", "Açıklama / Not", { maxLength: 3000 })}
              </>
            )}
            {kind === "adjustment" && (
              <>
                {personSelect}
                <div className="balance-preview">
                  <Pencil />
                  <div>
                    <small>Mevcut Kalan İzin</small>
                    <strong>{days(adjustmentBalance)} gün</strong>
                  </div>
                </div>
                <F label="Düzeltme Yöntemi">
                  <select
                    value={form.mode}
                    onChange={(e) => {
                      update("mode", e.target.value);
                      update(
                        "amount",
                        e.target.value === "target"
                          ? num(adjustmentBalance)
                          : "0",
                      );
                    }}
                  >
                    <option value="delta">Artı / eksi gün ekle</option>
                    <option value="target">
                      Kalan bakiyeyi doğrudan belirle
                    </option>
                  </select>
                </F>
                {field(
                  "amount",
                  form.mode === "target"
                    ? "Olması Gereken Kalan (gün)"
                    : "Düzeltme (gün)",
                  {
                    inputMode: "decimal",
                    required: true,
                    help: "Artırmak için 2, azaltmak için -2 gir. Ondalık değerler kullanılabilir.",
                  },
                )}
                {field("date", "Düzeltme Tarihi", {
                  type: "date",
                  required: true,
                })}
                {field("reason", "Düzeltme Açıklaması *", {
                  required: true,
                  maxLength: 1000,
                  placeholder: "Örn. Önceki dönem kayıt düzeltmesi",
                })}
                <p className="mini-note">
                  İzin kayıtları değiştirilmez. Düzeltme ayrıca saklanır ve
                  geçmişten geri alınabilir.
                </p>
              </>
            )}
            {kind === "attendance" && (
              <>
                {personSelect}
                {field("date", "Tarih *", { type: "date", required: true })}
                <F label="Durum">
                  <select
                    value={form.status}
                    onChange={(e) => {
                      const status = e.target.value;
                      setForm((f) => ({
                        ...f,
                        status,
                        minutes: status === "Çalıştı" ? f.minutes : "0",
                      }));
                    }}
                  >
                    {[
                      "Çalıştı",
                      "İzinli",
                      "Raporlu",
                      "Devamsız",
                      "Hafta tatili",
                      "Diğer",
                    ].map((s) => (
                      <option key={s}>{s}</option>
                    ))}
                  </select>
                </F>
                <div className="form-grid">
                  {field("minutes", "Çalışma Süresi (dakika)", {
                    inputMode: "numeric",
                    required: true,
                    help: "8 saat için 480 gir.",
                  })}
                  {field("overtime", "Fazla Mesai (dakika)", {
                    inputMode: "numeric",
                    required: true,
                  })}
                </div>
                {field("note", "Not", { maxLength: 3000 })}
                <div className="mini-note">
                  Puantaj kaydı izin bakiyesini kendiliğinden değiştirmez. İzin
                  düşümü için İzin Kaydı kullan.
                </div>
              </>
            )}
            {kind === "payroll" && (
              <>
                {personSelect}
                {field("month", "Ay *", { type: "month", required: true })}
                <div className="form-grid">
                  {field("base", "Temel Ücret (₺)", {
                    inputMode: "decimal",
                    required: true,
                  })}
                  {field("extra", "Ek Ödeme (₺)", {
                    inputMode: "decimal",
                    required: true,
                  })}
                  {field("deduction", "Kesinti (₺)", {
                    inputMode: "decimal",
                    required: true,
                  })}
                  {field("paid", "Ödenen Tutar (₺)", {
                    inputMode: "decimal",
                    required: true,
                  })}
                </div>
                {field("note", "Ödeme / Kesinti Notu", { maxLength: 3000 })}
                <div className="mini-note">
                  Net = Temel ücret + Ek ödeme − Kesinti. Bu kayıt resmî bordro
                  veya vergi hesabı değildir.
                </div>
              </>
            )}
            <div className="modal-actions">
              <Btn disabled={busy} onClick={close}>
                Vazgeç
              </Btn>
              <Btn type="submit" primary disabled={busy} icon={Check}>
                {busy
                  ? "Kaydediliyor…"
                  : kind === "employee"
                    ? "Personeli Kaydet"
                    : "Kaydet"}
              </Btn>
            </div>
          </form>
        )}
        {kind === "confirm" && (
          <>
            <p>{modal.text}</p>
            <div className="modal-actions">
              <Btn onClick={close} disabled={busy}>
                Vazgeç
              </Btn>
              <Btn
                primary
                onClick={() => execute(modal.action)}
                disabled={busy}
              >
                Devam Et
              </Btn>
            </div>
          </>
        )}
        {kind === "day" && (
          <>
            {modal.records.length ? (
              modal.records.map((l) => (
                <div className="day-record" key={l.id}>
                  <div>
                    <b>{data.employees[l.employeeId]?.name}</b>
                    <p>
                      {l.kind} · {niceDate(l.start)} – {niceDate(l.end)} ·{" "}
                      {days(l.days)} gün
                    </p>
                  </div>
                  <Btn icon={Pencil} onClick={() => open("leave", l)}>
                    Düzenle
                  </Btn>
                  <Btn icon={Printer} onClick={() => printLeave(l)}>
                    Form
                  </Btn>
                </div>
              ))
            ) : (
              <Empty
                title="Bu gün için izin yok"
                text="Yeni bir izin kaydı ekleyebilirsin."
              />
            )}
            <Btn
              primary
              icon={Plus}
              onClick={() => open("leave", null, { date: modal.date })}
            >
              Bu Tarihe İzin Ekle
            </Btn>
          </>
        )}
        {kind === "notifications" && (
          <>
            <p>
              Bugün izinli{" "}
              {
                new Set(
                  values(data, "leaves")
                    .filter((l) => l.start <= today() && l.end >= today())
                    .map((l) => l.employeeId),
                ).size
              }{" "}
              personel var.
            </p>
            <p>
              {state.conflicts.length
                ? `${state.conflicts.length} eşitleme çakışması çözülmeyi bekliyor.`
                : "Çözülmeyi bekleyen eşitleme çakışması yok."}
            </p>
            <p className="mini-note">
              Uygulama personellere talep veya onay bildirimi göndermez.
              Kayıtları yalnızca sen yönetirsin.
            </p>
          </>
        )}
        {kind === "conflicts" && (
          <>
            {state.conflicts.length === 0 ? (
              <p>Çakışma kalmadı. Şimdi eşitleyebilirsin.</p>
            ) : (
              state.conflicts.map((c) => (
                <div className="conflict-card" key={c.table + c.id}>
                  <h3>
                    {c.local?.name ||
                      c.remote?.name ||
                      data.employees[
                        c.local?.employeeId || c.remote?.employeeId
                      ]?.name ||
                      "Kayıt"}
                  </h3>
                  <div className="conflict-values">
                    <div>
                      <b>Bu cihazdaki</b>
                      <p>{summary(c.local)}</p>
                    </div>
                    <div>
                      <b>Windows’taki / sunucudaki</b>
                      <p>{summary(c.remote)}</p>
                    </div>
                  </div>
                  <div className="actions">
                    <Btn
                      onClick={async () => {
                        try {
                          await mutate((s) => resolveConflict(s, c, true));
                          announce("Bu cihazdaki sürüm seçildi.");
                        } catch (e) {
                          setError(e.message);
                        }
                      }}
                    >
                      Bu Cihazdakini Koru
                    </Btn>
                    <Btn
                      primary
                      onClick={async () => {
                        try {
                          await mutate((s) => resolveConflict(s, c, false));
                          announce("Sunucudaki sürüm seçildi.");
                        } catch (e) {
                          setError(e.message);
                        }
                      }}
                    >
                      Sunucudakini Kullan
                    </Btn>
                  </div>
                </div>
              ))
            )}
          </>
        )}
      </div>
    </dialog>
  );
}
function summary(record) {
  if (!record) return "Kayıt silinmiş.";
  const labels = {
    name: "Ad",
    department: "Bölüm",
    entitlement: "Hak",
    carry: "Devir",
    openingUsed: "Önceki kullanım",
    kind: "Tür",
    start: "Başlangıç",
    end: "Bitiş",
    days: "Gün",
    amount: "Düzeltme",
    reason: "Açıklama",
    date: "Tarih",
    base: "Ücret",
    paid: "Ödenen",
    minutes: "Dakika",
    status: "Durum",
    note: "Not",
    archived: "Arşiv",
  };
  return Object.entries(record)
    .filter(([k]) => labels[k])
    .map(
      ([k, v]) =>
        `${labels[k]}: ${["entitlement", "carry", "openingUsed", "days", "amount"].includes(k) ? days(v) : ["base", "paid"].includes(k) ? money(v) : typeof v === "boolean" ? (v ? "Evet" : "Hayır") : v}`,
    )
    .join(" · ");
}
function SettingsPage({
  data,
  state,
  commit,
  sync,
  network,
  syncError,
  announce,
  open,
  downloadBackup,
  onImport,
  deferred,
}) {
  const initial = getConnection();
  const [url, setUrl] = useState(initial.url || ""),
    [token, setToken] = useState(""),
    [pairing, setPairing] = useState(null),
    [name, setName] = useState(data.settings.organization?.name || ""),
    [manager, setManager] = useState(data.settings.organization?.manager || "");
  const file = useRef(),
    organization = useRef(data.settings.organization || null);
  return (
    <div className="settings-grid">
      <section className="card">
        <div className="card-title">
          <h2>
            <Wifi />
            Windows – Android Bağlantısı
          </h2>
        </div>
        <p>
          Aynı Wi‑Fi ağında Windows programı açıkken telefondaki kayıtları
          eşitle. Farklı ağlardan kullanım için güvenilir bir HTTPS sunucusu
          gerekir.
        </p>
        <F
          label="Sunucu Adresi"
          placeholder="http://192.168.1.20:4310"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
        <F
          label="Bağlantı Anahtarı"
          type="password"
          autoComplete="new-password"
          placeholder={
            initial.hasToken || initial.token
              ? "Kaydedilmiş anahtar var; boş bırakırsan korunur"
              : "Windows’taki bağlantı anahtarı"
          }
          value={token}
          onChange={(e) => setToken(e.target.value)}
        />
        <div className="actions">
          <Btn
            primary
            icon={Check}
            onClick={() => {
              try {
                setConnection(url, token);
                setToken("");
                announce("Bağlantı ayarları kaydedildi.");
                sync();
              } catch (e) {
                announce(e.message, true);
              }
            }}
          >
            Kaydet ve Eşitle
          </Btn>
          <Btn
            icon={RefreshCw}
            onClick={() => sync()}
            disabled={network === "syncing"}
          >
            Şimdi Eşitle
          </Btn>
        </div>
        <small className="connection-detail">
          Son başarılı eşitleme:{" "}
          {state.lastSync
            ? new Date(state.lastSync).toLocaleString("tr-TR")
            : "Henüz yapılmadı"}
        </small>
        {syncError && <p className="form-error">{syncError}</p>}
        <Btn
          onClick={() =>
            open("confirm", null, {
              title: "Bağlantı kaldırılsın mı?",
              text: "Cihazındaki kayıtlar kalır. Diğer cihazla eşitleme durur.",
              action: async () => {
                disconnect();
                setUrl("");
                setToken("");
                announce("Bağlantı kaldırıldı.");
              },
            })
          }
        >
          Bağlantıyı Kaldır
        </Btn>
        <div className="mini-note">
          <ShieldCheck size={17} />
          Yerel HTTP bağlantısını yalnızca güvendiğin özel ağda kullan.
          Bilgisayar kapalıyken de kayıt tutabilirsin; eşitleme yeniden
          bağlandığında yapılır.
        </div>
      </section>
      <section className="card">
        <div className="card-title">
          <h2>
            <ShieldCheck />
            Windows Bağlantı Bilgileri
          </h2>
        </div>
        <p>
          Windows programını çalıştırdığın bilgisayarda bağlantı anahtarını açıp
          telefona gir. Anahtarı yalnızca kendi cihazlarınla paylaş.
        </p>
        <Btn
          icon={Wifi}
          onClick={async () => {
            try {
              setPairing(await api("/api/pairing"));
            } catch (e) {
              announce(e.message, true);
            }
          }}
        >
          Bu Bilgisayarın Bağlantı Bilgilerini Göster
        </Btn>
        {pairing && (
          <div className="pairing">
            <b>Telefon için adres</b>
            {pairing.addresses.length ? (
              pairing.addresses.map((a) => <code key={a}>{a}</code>)
            ) : (
              <p>
                Yerel ağ adresi bulunamadı. Windows Wi‑Fi bağlantısını kontrol
                et.
              </p>
            )}
            <b>Bağlantı anahtarı</b>
            <code className="secret">{pairing.token}</code>
            <Btn
              icon={Copy}
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(pairing.token);
                  announce("Anahtar kopyalandı.");
                } catch {
                  announce("Anahtarı seçip elle kopyalayabilirsin.");
                }
              }}
            >
              Anahtarı Kopyala
            </Btn>
            <Btn onClick={() => setPairing(null)}>Anahtarı Gizle</Btn>
          </div>
        )}
        <p className="mini-note">
          Windows güvenlik duvarı sorarsa yalnızca özel ağ erişimine izin ver.
          4310 portunu internete açma.
        </p>
      </section>
      <section className="card">
        <div className="card-title">
          <h2>
            <Download />
            Yedekleme ve Geri Yükleme
          </h2>
        </div>
        <p>
          JSON yedeği personel, izin, düzeltme, puantaj, maaş ve düzenleme
          geçmişini içerir. Bağlantı anahtarları yedeğe eklenmez.
        </p>
        <div className="actions">
          <Btn primary icon={Download} onClick={downloadBackup}>
            Tam Yedek İndir
          </Btn>
          <Btn icon={History} onClick={() => file.current.click()}>
            Yedekten Geri Yükle
          </Btn>
        </div>
        <input
          ref={file}
          hidden
          type="file"
          accept="application/json,.json"
          onChange={async (e) => {
            const f = e.target.files[0];
            if (!f) return;
            try {
              if (f.size > 10 * 1024 * 1024)
                throw Error("Dosya 10 MB sınırını aşıyor.");
              onImport(await f.text());
            } catch (error) {
              announce(error.message, true);
            }
            e.target.value = "";
          }}
        />
        <div className="mini-note">
          Yedek dosyası şifrelenmemiş personel bilgisi içerir. Güvenli bir yerde
          sakla. Tarayıcı verilerini silmeden veya APK’yı kaldırmadan önce yedek
          al.
        </div>
      </section>
      <section className="card">
        <div className="card-title">
          <h2>
            <Settings />
            İşyeri ve Form Bilgileri
          </h2>
        </div>
        <F
          label="İşyeri / Kurum Adı"
          value={name}
          maxLength={200}
          onChange={(e) => setName(e.target.value)}
        />
        <F
          label="Formdaki Yetkili Adı"
          value={manager}
          maxLength={160}
          onChange={(e) => setManager(e.target.value)}
        />
        <Btn
          primary
          icon={Check}
          onClick={async () => {
            try {
              const value = {
                id: "organization",
                name: name.trim(),
                manager: manager.trim(),
              };
              await commit(
                [{ table: "settings", id: "organization", value }],
                "İşyeri bilgileri güncellendi",
                {
                  table: "settings",
                  id: "organization",
                  value: organization.current,
                },
              );
              organization.current = value;
            } catch (e) {
              announce(e.message, true);
            }
          }}
        >
          Bilgileri Kaydet
        </Btn>
        {deferred && (
          <Btn icon={Download} onClick={() => deferred.prompt()}>
            Windows’a Uygulama Olarak Kur
          </Btn>
        )}
        <p className="mini-note">
          İzin360 1.0 · Tüm izin hakları manuel girilir. Takvim resmî tatil ve
          yasal hak hesabı yapmaz. Kayıtlar bir kişi tarafından takip edilir;
          personel hesabı ve onay süreci yoktur.
        </p>
      </section>
    </div>
  );
}
createRoot(document.getElementById("root")).render(<App />);
if ("serviceWorker" in navigator && !native() && import.meta.env.PROD)
  navigator.serviceWorker.register("./sw.js").catch(() => {});
