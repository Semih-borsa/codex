import { useState, useMemo, useEffect, useRef, useId, useCallback, createContext, useContext } from "react";
import { createPortal } from "react-dom";
import { Capacitor } from "@capacitor/core";
import { LocalNotifications } from "@capacitor/local-notifications";
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, PieChart, Pie, Cell, LabelList } from "recharts";
// ── dateUtils.js (inline) ──────────────────────────────────────────────────
function yerelTarihAnahtari(tarih = new Date()) {
  const yil = tarih.getFullYear();
  const ay = String(tarih.getMonth() + 1).padStart(2, "0");
  const gun = String(tarih.getDate()).padStart(2, "0");
  return `${yil}-${ay}-${gun}`;
}

function yerelTarihOku(deger) {
  if (deger instanceof Date) {
    return new Date(deger.getFullYear(), deger.getMonth(), deger.getDate());
  }
  const eslesme = String(deger || "").match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!eslesme) return null;
  const tarih = new Date(Number(eslesme[1]), Number(eslesme[2]) - 1, Number(eslesme[3]));
  if (
    tarih.getFullYear() !== Number(eslesme[1]) ||
    tarih.getMonth() !== Number(eslesme[2]) - 1 ||
    tarih.getDate() !== Number(eslesme[3])
  ) return null;
  return tarih;
}

function takvimGunSirasi(deger) {
  const tarih = yerelTarihOku(deger);
  if (!tarih) return null;
  return Math.floor(Date.UTC(tarih.getFullYear(), tarih.getMonth(), tarih.getDate()) / 86400000);
}

function takvimGunFarki(son, ilk) {
  const sonGun = takvimGunSirasi(son);
  const ilkGun = takvimGunSirasi(ilk);
  return sonGun == null || ilkGun == null ? 0 : sonGun - ilkGun;
}

function takvimGunuEkle(deger, miktar) {
  const tarih = yerelTarihOku(deger);
  if (!tarih) return null;
  tarih.setDate(tarih.getDate() + miktar);
  return tarih;
}

function maasDonemiBul(deger, baslangicGunu = 15) {
  const tarih = yerelTarihOku(deger);
  if (!tarih) return null;
  const gun = Math.min(28, Math.max(1, Number.parseInt(baslangicGunu, 10) || 15));
  const baslangic = tarih.getDate() >= gun
    ? new Date(tarih.getFullYear(), tarih.getMonth(), gun)
    : new Date(tarih.getFullYear(), tarih.getMonth() - 1, gun);
  const bitis = new Date(baslangic.getFullYear(), baslangic.getMonth() + 1, gun - 1);
  return {
    yil: baslangic.getFullYear(),
    ay: baslangic.getMonth(),
    gun,
    key: `${baslangic.getFullYear()}-${baslangic.getMonth()}-${gun}`,
    baslangic,
    bitis,
  };
}

function donemdekiSabitOdemeTarihi(donemBaslangici, odemeGunu) {
  const baslangic = yerelTarihOku(donemBaslangici);
  if (!baslangic) return null;
  const istenenGun = Math.min(31, Math.max(1, Number.parseInt(odemeGunu, 10) || 1));
  const ayKaymasi = istenenGun >= baslangic.getDate() ? 0 : 1;
  const hedefYil = new Date(baslangic.getFullYear(), baslangic.getMonth() + ayKaymasi, 1).getFullYear();
  const hedefAy = new Date(baslangic.getFullYear(), baslangic.getMonth() + ayKaymasi, 1).getMonth();
  const ayinSonGunu = new Date(hedefYil, hedefAy + 1, 0).getDate();
  return new Date(hedefYil, hedefAy, Math.min(istenenGun, ayinSonGunu));
}


// Para değerleri kalıcı kayıtlarda TL olarak kalır; aritmetik tam sayı kuruşla yapılır.
const PARA_UST_SINIR = 1_000_000_000_000;
function sayisalOku(deger) {
  if (typeof deger === "number") return Number.isFinite(deger) ? deger : NaN;
  let metin = String(deger ?? "").trim();
  if (!metin) return NaN;
  if (metin.includes(",")) {
    if (!/^[+-]?(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d+)?$/.test(metin)) return NaN;
    metin = metin.replace(/\./g, "").replace(",", ".");
  }
  if (!/^[+-]?\d+(?:\.\d+)?$/.test(metin)) return NaN;
  const n = Number(metin);
  return Number.isFinite(n) ? n : NaN;
}
function paraKurus(deger) {
  const n = sayisalOku(deger);
  if (!Number.isFinite(n) || Math.abs(n) > PARA_UST_SINIR) return NaN;
  return Math.sign(n) * Math.round((Math.abs(n) + Number.EPSILON) * 100);
}
function paraOku(deger) { return paraKurus(deger) / 100; }
function paraTopla(...degerler) { return degerler.reduce((n, v) => n + (paraKurus(v) || 0), 0) / 100; }
function tutarGecerli(deger, sifir = false, negatif = false) {
  const n = sayisalOku(deger);
  return Number.isFinite(n) && Math.abs(n) <= PARA_UST_SINIR
    && (negatif || (sifir ? n >= 0 : n > 0))
    && Math.abs(n * 100 - Math.round(n * 100)) < 0.001;
}
function tamSayiGecerli(deger, min, max) {
  const n = sayisalOku(deger);
  return Number.isInteger(n) && n >= min && n <= max;
}
function odemeOnayi(ad,tutar) {
  return window.confirm(`${ad || 'Ödeme'}${Number.isFinite(Number(tutar)) ? ` · ₺${fmt(tutar)}` : ''} ödendi olarak işaretlensin mi?`);
}
function formHatasi(mesaj = "Geçerli, pozitif bir tutar ve tarih gir. Tutar en fazla iki ondalık basamak içerebilir.") {
  toast(mesaj, "hata"); return false;
}
function islemFormuGecerli(form, kartMi = false) {
  if (!yerelTarihOku(form.tarih)) return formHatasi("Geçerli bir işlem tarihi seç.");
  if (kartMi) {
    if (!tutarGecerli(form.donemIci || 0, true) || !tutarGecerli(form.provizyon || 0, true)
        || paraTopla(form.donemIci || 0, form.provizyon || 0) <= 0) return formHatasi();
  } else if (!tutarGecerli(form.tutar)) return formHatasi();
  return true;
}
function aylikTarih(baslangic, ayKaymasi) {
  const tarih = yerelTarihOku(baslangic);
  if (!tarih) return null;
  const hedef = new Date(tarih.getFullYear(), tarih.getMonth() + ayKaymasi, 1);
  const sonGun = new Date(hedef.getFullYear(), hedef.getMonth() + 1, 0).getDate();
  hedef.setDate(Math.min(tarih.getDate(), sonGun));
  return hedef;
}

// ── financeUtils.js (inline) ────────────────────────────────────────────────
const sayi = (deger) => Number.isFinite(Number(deger)) ? Number(deger) : 0;

const tarihMilisaniye = (deger, gunSonu = false) => {
  if (deger instanceof Date) return deger.getTime();
  const metin = String(deger || "");
  const eslesme = metin.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (eslesme) {
    const tarih = new Date(Number(eslesme[1]), Number(eslesme[2]) - 1, Number(eslesme[3]), gunSonu ? 23 : 0, gunSonu ? 59 : 0, gunSonu ? 59 : 0, gunSonu ? 999 : 0);
    return tarih.getTime();
  }
  const zaman = new Date(metin).getTime();
  return Number.isFinite(zaman) ? zaman : null;
};

const tarihAralikta = (deger, baslangic, bitis) => {
  const zaman = tarihMilisaniye(deger);
  return zaman != null && zaman >= tarihMilisaniye(baslangic) && zaman <= tarihMilisaniye(bitis, true);
};

function donemGiderOzeti(islemler, kartlar = [], odenmisKartIdleri = [], hesapModu = "bekleyen", kartOdemeleri = [], aktifDonemKey = "") {
  const kartMap = new Map((kartlar || []).map(kart => [String(kart.isim || ""), kart]));
  const odenmisler = new Set((odenmisKartIdleri || []).map(String));
  let normalGider = 0;
  let kartGideri = 0;
  let dahilKartGideri = 0;
  (islemler || []).filter(i => i.tip === "gider").forEach(islem => {
    const kart = kartMap.get(String(islem.kategori || ""));
    const tutar = sayi(islem.tutar);
    if (!kart) {
      normalGider = paraTopla(normalGider, tutar);
      return;
    }
    kartGideri = paraTopla(kartGideri, tutar);
    // Kartın kendi "Güncel bakiyeden düşülsün" ayarı öncelikli; yoksa genel hesap moduna bakılır.
    const dahilMi = kart.bakiyedenDussun != null ? kart.bakiyedenDussun !== false : (hesapModu === "kartlar_dahil" || odenmisler.has(String(kart.id)));
    if (dahilMi) dahilKartGideri = paraTopla(dahilKartGideri, tutar);
  });
  const kartOdemeGideri = (kartOdemeleri || [])
    .filter(o => o.odemeDonemKey === aktifDonemKey || (!o.odemeDonemKey && o.tarih && maasDonemKey(o.tarih, 15).key === aktifDonemKey))
    .reduce((toplam, o) => paraTopla(toplam, sayi(o.tutar)), 0);
  return {
    normalGider,
    kartGideri,
    dahilKartGideri,
    kartOdemeGideri,
    toplamGider: paraTopla(normalGider, dahilKartGideri, kartOdemeGideri),
  };
}

function donemHarcamaOrani(gelir, gider, birikim = 0) {
  const toplamGelir = sayi(gelir);
  const kullanilan = paraTopla(Math.max(0, sayi(gider)), Math.max(0, sayi(birikim)));
  if (toplamGelir <= 0) return kullanilan > 0 ? 100 : 0;
  return Math.max(0, (kullanilan / toplamGelir) * 100);
}

const turkceAdaGoreSirala = (liste = [], alan = "isim") => [...liste].sort((a, b) =>
  String(a?.[alan] || "").localeCompare(String(b?.[alan] || ""), "tr", { sensitivity: "base", numeric: true })
);

const tarihAnahtari = (tarih) => `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, "0")}-${String(tarih.getDate()).padStart(2, "0")}`;
const ayAnahtari = (tarih) => `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, "0")}`;

const tarihOku = (deger) => {
  const eslesme = String(deger || "").match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!eslesme) return null;
  const tarih = new Date(Number(eslesme[1]), Number(eslesme[2]) - 1, Number(eslesme[3]));
  return tarih.getFullYear() === Number(eslesme[1]) && tarih.getMonth() === Number(eslesme[2]) - 1 && tarih.getDate() === Number(eslesme[3]) ? tarih : null;
};

function ayinGunu(yil, ay, gun) {
  const sonGun = new Date(yil, ay + 1, 0).getDate();
  return new Date(yil, ay, Math.min(sonGun, Math.max(1, parseInt(gun) || 1)));
}

function kartSiradakiSonOdeme(kart, kartOdemeleri = [], simdi = new Date()) {
  if (kart?.sonOdemeTarihi) {
    const tarih = tarihOku(kart.sonOdemeTarihi);
    if (!tarih) return null;
    const kesinTarih = tarihAnahtari(tarih);
    const ilgiliOdemeler = (kartOdemeleri || []).filter(o =>
      String(o.kartId) === String(kart.id) && o.sonOdemeTarihi === kesinTarih
      && (!kart.ekstreDonemKey || (o.ekstreDonemKey || o.donemKey) === kart.ekstreDonemKey)
    );
    const odenen = ilgiliOdemeler.reduce((s, o) => paraTopla(s, o.tutar || 0), 0);
    const odendi = Number(kart.ekstre) > 0 ? odenen >= Number(kart.ekstre) : ilgiliOdemeler.length > 0;
    return { tarih, tarihAnahtari: kesinTarih, donemKey: ayAnahtari(tarih), odendi, kesinTarih: true };
  }
  if (!kart?.sonOdemeGunu) return null;
  const bugun = new Date(simdi.getFullYear(), simdi.getMonth(), simdi.getDate());
  const buAy = ayAnahtari(bugun);
  const buAyOdendi = (kartOdemeleri || []).some(o =>
    String(o.kartId) === String(kart.id) && o.sonOdemeDonemKey === buAy
  );
  const tarih = buAyOdendi
    ? ayinGunu(bugun.getFullYear(), bugun.getMonth() + 1, kart.sonOdemeGunu)
    : ayinGunu(bugun.getFullYear(), bugun.getMonth(), kart.sonOdemeGunu);
  return { tarih, tarihAnahtari: tarihAnahtari(tarih), donemKey: ayAnahtari(tarih) };
}

function kartHatirlatmalariniOlustur(kartlar, kartOdemeleri = [], simdi = new Date()) {
  return (kartlar || []).flatMap(kart => {
    const sonOdeme = kartSiradakiSonOdeme(kart, kartOdemeleri, simdi);
    if (!sonOdeme || sonOdeme.odendi) return [];
    return [{
      id: `kart-${kart.id}-${sonOdeme.donemKey}`,
      kartId: kart.id,
      kaynak: "kredi_karti",
      kategori: kart.isim,
      aciklama: `${kart.isim} kredi kartı`,
      tarih: sonOdeme.tarihAnahtari,
      tutar: 0,
      odendi: false,
    }];
  });
}

function bildirimSaatleriniAl(ayarlar) {
  const saatler = Array.isArray(ayarlar?.saatler) ? ayarlar.saatler : [ayarlar?.saat || "18:00"];
  return [...new Set(saatler.filter(s => typeof s === "string" && /^(?:[01]\d|2[0-3]):[0-5]\d$/.test(s)))].sort();
}

function bildirimPlanlariniOlustur(odemeler, ayarlar, simdi = new Date()) {
  const gunler = [...new Set((ayarlar?.gunler || [0]).map(Number).filter(g => Number.isInteger(g) && g >= 0 && g <= 365))];
  const saatler = bildirimSaatleriniAl(ayarlar);
  const gruplar = {};
  const bugun = tarihAnahtari(simdi);
  (odemeler || []).filter(o => !o.odendi && o.tarih).forEach(odeme => {
    const tarih = tarihOku(odeme.tarih);
    if (!tarih) return;
    gunler.forEach(onceGun => saatler.forEach(slot => {
      const [saat, dakika] = slot.split(":").map(Number);
      const at = new Date(tarih.getFullYear(), tarih.getMonth(), tarih.getDate(), saat, dakika);
      at.setDate(at.getDate() - onceGun);
      let telafi = false;
      if (at <= simdi) {
        if (onceGun !== 0 || tarihAnahtari(tarih) !== bugun) return;
        at.setTime(simdi.getTime() + 5000);
        telafi = true;
      }
      const anahtar = telafi ? `${bugun}-telafi` : `${tarihAnahtari(at)}-${slot}`;
      if (!gruplar[anahtar]) gruplar[anahtar] = { anahtar, at, onceGun, telafi, odemeler: [] };
      // Aynı ödemeyi birden fazla geçmiş saat için tekrar sayma.
      if (!gruplar[anahtar].odemeler.some(o => String(o.id) === String(odeme.id))) {
        gruplar[anahtar].odemeler.push({ ...odeme, onceGun });
      }
    }));
  });
  return Object.values(gruplar).sort((a, b) => a.at - b.at);
}

function krediBildirimleri(krediler = []) {
  return krediler.flatMap(k => (k.taksitler || []).filter(t => !t.odendi && t.tarih).map(t => ({
    id: `kredi-${k.id}-${t.no}`, kaynak: "kredi",
    aciklama: `${k.ad || k.isim || "Kredi"} · ${t.no}. taksit`,
    tarih: t.tarih, tutar: t.tutar, odendi: false,
  })));
}

function bildirimKimligi(anahtar) {
  let hash = 17;
  for (let i = 0; i < anahtar.length; i += 1) hash = ((hash * 31) + anahtar.charCodeAt(i)) >>> 0;
  return (hash % 2000000000) + 1;
}

// ── CONSTANTS ────────────────────────────────────────────────────────────────
const VARSAYILAN_KATEGORILER = {
  gelir: ["Maaş", "Freelance", "Kira Geliri", "Birikim", "Diğer"],
  gider: ["Market", "Fatura", "Ulaşım", "Sağlık", "Eğlence", "Giyim", "Restoran", "Diğer"],
};
const KATEGORI_RENKLER = {
  Maaş: "#f59e0b", Freelance: "#fbbf24", "Kira Geliri": "#d97706",
  Market: "#ef4444", Fatura: "#f97316", Ulaşım: "#8b5cf6", Sağlık: "#06b6d4",
  Eğlence: "#ec4899", Giyim: "#A74E32", Restoran: "#14b8a6", Diğer: "#78716c",
};
const KATEGORI_IKON = {
  Maaş: "briefcase", Freelance: "screen", "Kira Geliri": "home", Birikim: "coins",
  Market: "cart", Fatura: "receipt", Ulaşım: "car", Sağlık: "medical",
  Eğlence: "play", Giyim: "shirt", Restoran: "utensils", Kira: "home", Diğer: "tag",
};
const katIkon = (kat, tip) => <KategoriSimge kategori={kat} tip={tip}/>;
const KATEGORI_SIMGE_SECENEKLERI = [
  ['tag','Etiket'],['building','Apartman'],['bolt','Elektrik'],['drop','Su'],['flame','Doğalgaz'],['wifi','İnternet'],['phone','Telefon'],['utensils','Yemek'],['basketball','Basketbol'],['school','Okul / Kreş'],['piggy','Birikim'],['banknote','Nakit'],['transfer','Transfer'],['calendar','Ödeme takvimi'],['card','Kredi kartı'],['carry','Devir'],['exam','Sınav'],['walletPlus','Maaş farkı'],['cart','Market'],['car','Ulaşım'],['medical','Sağlık'],['play','Eğlence'],['shirt','Giyim'],['home','Ev'],['briefcase','İş'],['wallet','Cüzdan'],['receipt','Fatura'],['coins','Para'],['screen','Bilgisayar'],
];
const KATEGORI_RENK_SECENEKLERI = ['#10b981','#3b82f6','#f59e0b','#8b5cf6','#ec4899','#06b6d4','#ef4444','#a74e32','#64748b'];
const KategoriGorunumContext = createContext({ gorunumler: {}, setGorunumler: () => false });
function kategoriGorunumu(kategori, tip = 'gider', gorunumler = {}) {
  const secim = gorunumler[`${tip}:${kategori}`];
  if (secim) return secim;
  const ad = String(kategori || '').toLocaleLowerCase('tr-TR').replace(/ı/g,'i').normalize('NFD').replace(/[\u0300-\u036f]/g,'');
  const kurallar = [
    ['aidat','building','#8b5cf6'],['elektrik','bolt','#f59e0b'],['dogalgaz','flame','#f97316'],['internet','wifi','#3b82f6'],['telefon','phone','#06b6d4'],['basketbol','basketball','#f59e0b'],['kres','school','#ec4899'],['yemek','utensils','#f59e0b'],['birikim','piggy','#10b981'],['nakit','banknote','#10b981'],['transfer','transfer','#3b82f6'],['kredi kart','card','#8b5cf6'],['kredi','calendar','#8b5cf6'],['world','card','#8b5cf6'],['bonus','card','#10b981'],['maximum','card','#ec4899'],['devir','carry','#06b6d4'],['sinav','exam','#8b5cf6'],['maas fark','walletPlus','#10b981'],
  ];
  let kural = kurallar.find(([kelime]) => ad.includes(kelime));
  if (!kural && /(^|\s)su($|\s)/.test(ad)) kural=['su','drop','#3b82f6'];
  let ikon = kural?.[1] || KATEGORI_IKON[kategori] || 'tag';
  let renk = kural?.[2] || KATEGORI_RENKLER[kategori] || '#64748b';
  if (ad.includes('telefon') || /(?:ediz|oyku).*birikim/.test(ad)) {
    renk = ad.includes('semih') ? '#3b82f6' : ad.includes('bilge') ? '#8b5cf6' : ad.includes('ediz') ? '#06b6d4' : '#ec4899';
  }
  return { ikon, renk };
}
function KartMarkaRozeti({ isim }) {
  const ad = String(isim || '').toLocaleLowerCase('tr-TR').replace(/ı/g,'i').normalize('NFD').replace(/[\u0300-\u036f]/g,'');
  const marka = /yapi[\s_-]*kredi|\bykb\b/.test(ad) ? { kart:'World', banka:'Yapı Kredi', renk:'#6d28d9', bankaRenk:'#005baa' }
    : /world|vakif/.test(ad) ? { kart:'World', banka:'VakıfBank', renk:'#6d28d9', bankaRenk:'#946700' }
    : /maximum|is bank/.test(ad) ? { kart:'maximum', banka:'İş Bankası', renk:'#d41478', bankaRenk:'#164783' }
    : /bonus|garanti/.test(ad) ? { kart:'bonus', banka:'Garanti BBVA', renk:'#00864a', bankaRenk:'#006c3b' } : null;
  if (!marka) return <Icon name="card" size={21}/>;
  return <span className="card-brand-badge" aria-label={`${marka.kart} · ${marka.banka} marka rozeti`}>
    <strong style={{ color:marka.renk }}>{marka.kart}</strong>
    <small style={{ color:marka.bankaRenk }}>{marka.banka}</small>
  </span>;
}

function KategoriSimge({ kategori, tip = 'gider', size = 30 }) {
  const { gorunumler } = useContext(KategoriGorunumContext);
  const { ikon, renk } = kategoriGorunumu(kategori,tip,gorunumler);
  return <span className="category-symbol" aria-hidden="true" style={{ '--category-color':renk, width:size, height:size }}><Icon name={ikon} size={Math.round(size*.6)}/></span>;
}
function KategoriGorunumDuzenle({ kategori, tip }) {
  const { gorunumler, setGorunumler } = useContext(KategoriGorunumContext);
  const key = `${tip}:${kategori}`;
  const gorunum = kategoriGorunumu(kategori,tip,gorunumler);
  const kaydet = (alan,deger) => setGorunumler(p => ({...p,[key]:{...kategoriGorunumu(kategori,tip,p),[alan]:deger}}));
  return <details className="category-customizer"><summary><KategoriSimge kategori={kategori} tip={tip}/><span>{kategori}</span><small>Simge / renk</small></summary><div className="category-customizer-body">
    <Select label={`${kategori} ${tip} simgesi`} value={gorunum.ikon} onChange={e=>kaydet('ikon',e.target.value)}>{KATEGORI_SIMGE_SECENEKLERI.map(([id,ad])=><option key={id} value={id}>{ad}</option>)}</Select>
    <div className="category-colors" role="group" aria-label={`${kategori} ${tip} rengi`}>{KATEGORI_RENK_SECENEKLERI.map(renk=><button type="button" key={renk} aria-label={`${kategori} ${tip} renk ${renk}`} aria-pressed={gorunum.renk===renk} onClick={()=>kaydet('renk',renk)} style={{background:renk}}>{gorunum.renk===renk?'✓':''}</button>)}</div>
    <button type="button" className="tools-button" onClick={()=>setGorunumler(p=>{const yeni={...p};delete yeni[key];return yeni;})}>Otomatik simge ve renge dön</button>
  </div></details>;
}
function KategoriGorunumAyarlari({ kategoriler }) {
  return <section className="tools-card"><h3><Icon name="tag" size={17}/>Kategori simgeleri ve renkleri</h3><p className="tools-note">Mevcut ve yeni kategorilerin isimlerine göre simgeler otomatik önerilir. Bir kategoriye dokunarak simgeyi ve rengi değiştirebilirsin. Gelir ve gider seçimleri ayrı saklanır ve yedeğe dahildir.</p>{['gelir','gider'].map(tip=><details key={tip}><summary>{tip==='gelir'?'Gelir kategorileri':'Gider kategorileri'}</summary>{(kategoriler[tip]||[]).map(k=><KategoriGorunumDuzenle key={k} kategori={k} tip={tip}/>)}</details>)}</section>;
}


// Kart bakiyeleri birikmiş toplamdır; günlük harcama toplamların farkıdır.
function devirIslemiMi(i) {
  const ad = String(i.kategori || '').toLocaleLowerCase('tr-TR').replace(/ı/g,'i').normalize('NFD').replace(/[\u0300-\u036f]/g,'').trim();
  return i.kaynak === 'donem_devri' || ad === 'onceki aydan devir';
}
function donemDevriHesapla(islemler,kartlar,kartOdemeleri,donemGun,hesapModu,bugun=yerelTarihAnahtari(),odemeler=[]) {
  const aktif=maasDonemiBul(bugun,donemGun);
  const onceki=maasDonemiBul(yerelTarihAnahtari(takvimGunuEkle(aktif.baslangic,-1)),donemGun);
  const hesap=donemBakiyeHesapla({islemler,odemeler,kartlar,kartOdemeleri,bakiyeHesapModu:hesapModu,
    ilk:yerelTarihAnahtari(onceki.baslangic),son:yerelTarihAnahtari(onceki.bitis),bugun:yerelTarihAnahtari(onceki.bitis)});
  return {donemKey:aktif.key,kaynakDonemKey:onceki.key,tarih:yerelTarihAnahtari(aktif.baslangic),tutar:Math.max(0,hesap.gercekBakiye),kapanisBakiyesi:hesap.gercekBakiye};
}

function kartEkstreDoneminiBul(kart, donemGun) {
  const tarih = kart.ekstreSonOdemeTarihi || kart.sonOdemeTarihi;
  if (yerelTarihOku(tarih)) return maasDonemKey(tarih,donemGun).key;
  const anahtar = String(kart.ekstreDonemKey || '');
  if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(anahtar)) return anahtar;
  const tarihAnahtari = anahtar.startsWith('ekstre:') ? anahtar.slice(7) : '';
  return yerelTarihOku(tarihAnahtari) ? maasDonemKey(tarihAnahtari,donemGun).key : null;
}

function kartToplamBorcHesapla(kart,islemler,kartOdemeleri,donemKey,donemGun) {
  const toplam=islemler.filter(i=>i.tip==='gider' && i.kategori===kart.isim && maasDonemKey(i.tarih,donemGun).key===donemKey)
    .reduce((n,i)=>paraTopla(n,i.donemIci||0,i.provizyon||0),0);
  const ekstreKey=kart.ekstreDonemKey || kartEkstreDoneminiBul(kart,donemGun) || donemKey;
  const odenen=kartOdemeleri.filter(o=>String(o.kartId)===String(kart.id) && (o.ekstreDonemKey||o.donemKey)===ekstreKey).reduce((n,o)=>paraTopla(n,o.tutar),0);
  return paraTopla(toplam,Math.max(0,paraTopla(kart.ekstre||0,-odenen)));
}
function kartBorcDegisimi(toplam,onceki) {
  if(!onceki || !tutarGecerli(onceki.toplam,true))return null;
  const fark=paraTopla(toplam,-Number(onceki.toplam));
  return {fark,oran:Number(onceki.toplam)>0?fark/Number(onceki.toplam)*100:fark===0?0:null,yon:fark<0?'azalis':fark>0?'artis':'ayni'};
}
function KartBorcDegisimi({toplam,onceki}) {
  const fark=kartBorcDegisimi(toplam,onceki);
  if(!fark)return <span className="credit-debt-change" style={{color:'var(--text3)'}}>(Önceki dönem verisi yok)</span>;
  const isaret=fark.fark<0?'−':fark.fark>0?'+':'';
  return <span className="credit-debt-change" title={`Önceki maaş döneminin son kaydı: ${onceki.tarih} · ${fmtTL(onceki.toplam)}`} style={{color:fark.yon==='azalis'?'var(--income-text)':fark.yon==='artis'?'var(--expense-text)':'var(--text3)'}}>
    ({isaret}{fmtTL(Math.abs(fark.fark))} · {fark.oran==null?'Önceki dönem ₺0':`${isaret}%${Math.abs(fark.oran).toLocaleString('tr-TR',{maximumFractionDigits:1})}`})
  </span>;
}

function kartGuncelToplami(kayit) {
  return kayit.donemIci != null || kayit.provizyon != null
    ? paraTopla(kayit.donemIci || 0, kayit.provizyon || 0)
    : paraOku(kayit.tutar) || 0;
}

function kartKaydiniGuncelle(kayitlar, alanlar, tarih, ekstreAktarimi = 0) {
  const ilk = kayitlar[0];
  const eskiToplam = kayitlar.reduce((s, k) => paraTopla(s, kartGuncelToplami(k)), 0);
  const yeniToplam = paraTopla(alanlar.donemIci || 0, alanlar.provizyon || 0);
  const hareketler = {};
  kayitlar.forEach(k => {
    if (k.kartGunlukHareketler && typeof k.kartGunlukHareketler === "object") {
      Object.entries(k.kartGunlukHareketler).forEach(([gun, tutar]) => {
        hareketler[gun] = paraTopla(hareketler[gun] || 0, tutar);
      });
    } else if (k.kartGunBaslangicToplam != null && k.kartGunlukDelta && k.tarih) {
      hareketler[k.tarih] = paraTopla(hareketler[k.tarih] || 0, k.kartGunlukDelta);
    }
  });
  // Dönem içi/provizyon aktarımı toplam değişmedikçe harcama üretmez.
  const delta = ilk ? paraTopla(hareketler[tarih] || 0, yeniToplam, -eskiToplam, ekstreAktarimi) : 0;
  if (ilk) hareketler[tarih] = delta;
  const aktarimlar = {};
  kayitlar.forEach(k => Object.entries(k.kartEkstreAktarimlari || {}).forEach(([gun,tutar]) => {
    aktarimlar[gun] = paraTopla(aktarimlar[gun] || 0,tutar);
  }));
  if (ekstreAktarimi > 0) aktarimlar[tarih] = paraTopla(aktarimlar[tarih] || 0,ekstreAktarimi);
  return {
    ...(ilk || {}), ...alanlar, tip: "gider", kartHarcama: true,
    tutar: yeniToplam, tarih, kartGunlukDelta: delta,
    kartGunBaslangicToplam: ilk ? (ilk.kartGunBaslangicToplam ?? eskiToplam) : yeniToplam,
    kartGunBaslangicTarih: ilk?.kartGunBaslangicTarih || ilk?.tarih || tarih,
    kartGunlukHareketler: hareketler, kartEkstreAktarimlari: aktarimlar,
  };
}

function kartAnalizHareketleri(kayit) {
  if (!kayit.kartHarcama) return [];
  if (kayit.kartGunlukHareketler && typeof kayit.kartGunlukHareketler === "object") {
    return Object.entries(kayit.kartGunlukHareketler)
      .filter(([tarih, tutar]) => yerelTarihOku(tarih) && Number(tutar) > 0)
      .map(([tarih, tutar]) => ({ tarih, tutar: Number(tutar) }));
  }
  return kayit.kartGunBaslangicToplam != null && Number(kayit.kartGunlukDelta) > 0 && kayit.tarih
    ? [{ tarih: kayit.tarih, tutar: Number(kayit.kartGunlukDelta) }] : [];
}

function analizIslemleri(islemler) {
  return islemler.filter(i => !devirIslemiMi(i)).flatMap(i => i.kartHarcama
    ? kartAnalizHareketleri(i).map(h => ({ ...i, ...h, kartHarcama: false, kartGunlukKayit: true }))
    : [i]);
}

// Alışkanlık analizi (en pahalı gün / haftalık dağılım / genel alışkanlık) için bir işlemin sayılacak tutarı.
// Sabit giderler tamamen hariç. Kredi kartında İLK kez girilen toplam (geçmiş birikmiş tutar) hariç,
// ama SONRAKİ güncellemelerdeki fark (o gün gerçekten eklenen harcama) dahil edilir. Ödemeler zaten normal işlemdir, dahil.
const habitHareketleri = (i) => {
  if (!i || i.sabitId) return []; // sabit gider — tarihi kullanıcı alışkanlığını yansıtmaz
  if (!i.kartHarcama) return i.tutar > 0 && i.tarih ? [{ tarih: i.tarih, tutar: i.tutar }] : [];

  return kartAnalizHareketleri(i);
};

// Taksit adına göre akıllı ikon seçimi (anahtar kelime eşleştirme)
const TAKSIT_IKON_KURAL = [
  { k: ["mtv", "vergi", "trafik ceza", "harç"], i: "receipt" },
  { k: ["sigorta", "kasko", "dask", "ray sompo", "sompo", "poliçe"], i: "shield" },
  { k: ["hepsiburada", "trendyol", "n11", "amazon", "gittigidiyor", "e-ticaret", "eticaret", "online alışveriş"], i: "package" },
  { k: ["nakit avans", "avans", "kredi kartı", "kart borcu", "ekstre"], i: "card" },
  { k: ["gözlük", "atasun", "optik", "lens", "numaralı"], i: "medical" },
  { k: ["lcw", "lc waikiki", "waikiki", "penti", "koton", "mavi", "defacto", "giyim", "kıyafet", "elbise", "mont", "ceket", "pantolon", "gömlek", "iç giyim"], i: "shirt" },
  { k: ["superstep", "flo", "ayakkabı", "spor ayakkabı", "sneaker", "bot"], i: "tag" },
  { k: ["telefon", "iphone", "samsung", "xiaomi", "cep"], i: "screen" },
  { k: ["laptop", "bilgisayar", "pc", "macbook", "notebook"], i: "screen" },
  { k: ["tablet", "ipad"], i: "screen" },
  { k: ["tv", "televizyon", "ekran", "monitör"], i: "screen" },
  { k: ["saat", "watch", "kol saat"], i: "⌚" },
  { k: ["kulaklık", "airpod", "headphone"], i: "headphones" },
  { k: ["kamera", "fotoğraf", "objektif"], i: "screen" },
  { k: ["oyun", "playstation", "ps5", "xbox", "konsol"], i: "play" },
  { k: ["koltuk", "kanepe", "mobilya", "masa", "dolap", "yatak", "mutfak"], i: "home" },
  { k: ["buzdolabı", "çamaşır", "bulaşık", "fırın", "beyaz eşya", "kombi", "klima", "elektrikli süpürge", "süpürge"], i: "screen" },
  { k: ["araba", "araç", "oto", "lastik", "araç kredisi"], i: "car" },
  { k: ["motor", "motosiklet", "scooter"], i: "car" },
  { k: ["bisiklet"], i: "car" },
  { k: ["ev", "daire", "konut", "tapu"], i: "home" },
  { k: ["tatil", "seyahat", "uçak", "otel", "gezi"], i: "plane" },
  { k: ["mücevher", "altın", "yüzük", "kolye", "takı"], i: "coins" },
  { k: ["market", "gıda", "alışveriş"], i: "cart" },
  { k: ["kitap", "eğitim", "kurs", "ders"], i: "book" },
  { k: ["sağlık", "diş", "hastane", "gözlük", "tedavi"], i: "medical" },
  { k: ["spor", "fitness", "gym", "bisiklet aleti"], i: "medical" },
  { k: ["mobilya", "halı", "perde", "ev eşya"], i: "home" },
];
const taksitIkon = (ad) => {
  const s = (ad || '').toLocaleLowerCase('tr-TR');
  const rule = TAKSIT_IKON_KURAL.find(r=>r.k.some(k=>s.includes(k)));
  return <Icon name={rule?.i || 'receipt'} size={18}/>;
};

// ── TOAST ─────────────────────────────────────────────────────────────────────
let _toastYayin = null;
let _kayitHatasi = null;
const _okunamayanAnahtarlar = new Set();
let _kayitGrubu = null;
let _buIslemKayitHatali = false;
function kayitHatasiBildir(mesaj) {
  _kayitHatasi = mesaj;
  _buIslemKayitHatali = true;
  Promise.resolve().then(() => { _buIslemKayitHatali = false; });
  if (typeof window !== "undefined") window.dispatchEvent(new CustomEvent("finora-kayit-hatasi", { detail: mesaj }));
  if (_toastYayin) _toastYayin(mesaj, "hata");
}
function toast(mesaj, tip = "ok", geriAl = null) {
  if (_buIslemKayitHatali && tip !== "hata") return;
  if (_toastYayin) _toastYayin(mesaj, tip, geriAl);
}
function KayitDurumu() {
  const [hata, setHata] = useState(_kayitHatasi);
  useEffect(() => {
    const dinle = e => setHata(e.detail);
    window.addEventListener("finora-kayit-hatasi", dinle);
    return () => window.removeEventListener("finora-kayit-hatasi", dinle);
  }, []);
  return hata ? <div role="alert" className="kayit-hatasi"><strong>Veri kaydedilemedi</strong><p>{hata}</p><p>Son değişikliğin kalıcı olduğunu varsayma. Mevcut verilerini Ayarlar → Yedek Al ile dışa aktar.</p></div> : null;
}

function ToastKutusu() {
  const [veri, setVeri] = useState(null);
  useEffect(() => {
    _toastYayin = (mesaj, tip, geriAl) => {
      const id = Date.now();
      setVeri({ mesaj, tip, geriAl, id });
      setTimeout(() => setVeri(mevcut => mevcut?.id === id ? null : mevcut), geriAl ? 5200 : 2800);
    };
    return () => { _toastYayin = null; };
  }, []);
  if (!veri) return null;
  const renk = veri.tip === "hata" ? "#ef4444" : veri.tip === "uyari" ? "#f59e0b" : "#10b981";
  const ikon = veri.tip === "hata" ? "alert" : veri.tip === "uyari" ? "warning" : "check";
  return (
    <div key={veri.id} style={{ position: "fixed", top: "calc(14px + env(safe-area-inset-top))", left: "50%", transform: "translateX(-50%)", zIndex: 300, background: renk, color: "#fff", borderRadius: 16, padding: "10px 12px", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, display: "flex", alignItems: "center", gap: 9, boxShadow: "0 12px 32px rgba(0,0,0,0.3)", animation: "toastIn 0.28s ease", width: "min(92%, 430px)" }}>
      <span style={{ width: 20, height: 20, borderRadius: 99, background: "rgba(255,255,255,0.25)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, flexShrink: 0 }}><Icon name={ikon} size={17}/></span>
      <span style={{ flex: 1 }}>{veri.mesaj}</span>
      {veri.geriAl && <button onClick={() => { veri.geriAl(); setVeri(null); }} style={{ border: "1px solid rgba(255,255,255,.45)", background: "rgba(255,255,255,.18)", color: "#fff", borderRadius: 9, padding: "5px 9px", fontSize: 11, fontWeight: 700, cursor: "pointer" }}>Geri Al</button>}
    </div>
  );
}

const AYLAR = ["Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"];
const AYLAR_TAM = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];


// Kredi kartı marka renkleri
const krediKartiMarkaRengi = (isim = "") => {
  const ad = String(isim).toLocaleLowerCase("tr-TR").trim();
  // Koyu modda da marka rengi ve yazı net kalsın diye koyu zemin + parlak vurgu kullanılır.
  if (ad.includes("bonus")) return { ana: "#4ade80", soft: "rgba(22,163,74,.22)", border: "rgba(74,222,128,.65)" };
  if (ad.includes("maximum")) return { ana: "#93c5fd", soft: "rgba(30,58,138,.48)", border: "rgba(96,165,250,.72)" };
  if (ad.includes("world")) return { ana: "#facc15", soft: "rgba(161,98,7,.34)", border: "rgba(250,204,21,.72)" };
  return { ana: "#a5b4fc", soft: "rgba(167,78,50,.20)", border: "rgba(165,180,252,.58)" };
};

function islemKaynagi(i) {
  if(i.kartEkstreOdeme || i.kaynak==='ekstre_odemesi' || i.kartId!=null) return 'Ekstre ödemesi · nakit çıkışı';
  if(i.kartHarcama || i.kartGunlukKayit) return 'Kart borcu / güncellemesi';
  if(devirIslemiMi(i)) return 'Dönem devri · açılış bakiyesi';
  if(i.sabitId!=null) return 'Sabit gider';
  if(i.kaynak==='odeme' || i.odemeId!=null || i.odendi!=null) return 'Ödemeler';
  if(i.kaynak==='hizli_sablon' || i.sablonId!=null) return 'Hızlı işlem';
  return 'Elle eklenen / eski kayıt';
}
function yerelYedekPaketi(storage=window.localStorage) {
  const data={};
  for(let i=0;i<storage.length;i++) {
    const k=storage.key(i);
    if(k?.startsWith('ft_') && !k.startsWith('ft_auto_backup') && !k.startsWith('ft_manual_backup')) data[k]=JSON.parse(storage.getItem(k));
  }
  return {format:'finora-v2',surum:'1.7.0',olusturulma:new Date().toISOString(),data};
}
function otomatikYedekle(storage=window.localStorage) {
  const paket=yerelYedekPaketi(storage),gun=yerelTarihAnahtari();
  const eski=JSON.parse(storage.getItem('ft_auto_backup_daily_v2')||'[]');
  const liste=[...eski.filter(p=>p.gun!==gun),{...paket,gun}].slice(-3);
  storage.setItem('ft_auto_backup_daily_v2',JSON.stringify(liste));
  return liste;
}
function kayitDegisiklikleri(key,onceki,yeni,zaman=new Date().toISOString()) {
  if(!['ft_islemler','ft_odemeler','ft_kartlar','ft_kart_odemeleri'].includes(key) || !Array.isArray(onceki) || !Array.isArray(yeni)) return [];
  const a=new Map(onceki.map(i=>[String(i.id),i])),b=new Map(yeni.map(i=>[String(i.id),i]));
  return [...new Set([...a.keys(),...b.keys()])].flatMap(id=>{
    const eski=a.get(id),son=b.get(id);
    if(JSON.stringify(eski)===JSON.stringify(son)) return [];
    const ozet=i=>i?Object.fromEntries(['id','tip','kategori','aciklama','isim','kartAdi','kartId','tutar','tarih','donemIci','provizyon','ekstre','odendi','odendiTarih','sonOdemeTarihi','hesapKesimTarihi','bakiyedenDussun','kaynak','sabitId','odemeId','kartHarcama'].filter(k=>i[k]!=null).map(k=>[k,i[k]])):null;
    return [{key,kayitId:id,zaman,islem:!eski?'Eklendi':!son?'Silindi':'Değiştirildi',onceki:ozet(eski),yeni:ozet(son)}];
  });
}
function KayitGecmisi({kayitId=null,alan=null}) {
  const [liste,setListe]=useState([]);
  useEffect(()=>{const oku=()=>{try{setListe(JSON.parse(localStorage.getItem('ft_kayit_gecmisi')||'[]'));}catch{setListe([]);}};oku();window.addEventListener('finora-gecmis',oku);return()=>window.removeEventListener('finora-gecmis',oku);},[]);
  const secili=liste.filter(h=>(kayitId==null||String(h.kayitId)===String(kayitId))&&(!alan||h.key===alan)).slice().reverse();
  return <details className="calculation-help"><summary>Değişiklik geçmişi ({secili.length})</summary><p className="tools-note">Bu güncellemeden sonraki son 300 kayıt değişikliği cihazda tutulur. Eski değişiklikler geriye dönük oluşturulmaz.</p>{secili.map((h,n)=><details key={n}><summary>{h.islem} · {(h.yeni||h.onceki)?.aciklama||(h.yeni||h.onceki)?.kategori||(h.yeni||h.onceki)?.isim||'Kart'} · {new Date(h.zaman).toLocaleString('tr-TR')}</summary>{[['Önceki',h.onceki],['Sonraki',h.yeni]].map(([ad,i])=><p className="tools-note" key={ad}>{ad}: {i?`${i.aciklama||i.kategori||i.isim||'Kayıt'} · ${i.tarih||''} · ${fmtTL(i.tutar??i.ekstre??0)}${i.kartHarcama?` · Dönem içi ${fmtTL(i.donemIci||0)} · Provizyon ${fmtTL(i.provizyon||0)}`:''} · ${islemKaynagi(i)}`:'Kayıt yok'}</p>)}</details>)}</details>;
}
function YedekOnizleme({paket,onVazgec,onYukle}) {
  const data=paket.data;
  const sayim=d=>[['İşlem',d.ft_islemler?.length||0],['Kart',d.ft_kartlar?.length||0],['Ödeme',d.ft_odemeler?.length||0],['Ekstre ödemesi',d.ft_kart_odemeleri?.length||0]];
  let mevcut={};try{mevcut=yerelYedekPaketi().data;}catch{}
  const simdi=sayim(mevcut),sonra=sayim(data);
  return <Modal title="Yedek yükleme önizlemesi" onClose={onVazgec}><p className="tools-note">{paket.olusturulma?`Yedek tarihi: ${new Date(paket.olusturulma).toLocaleString('tr-TR')}`:'Yedek tarihi bilinmiyor.'} Mevcut kayıtlar bu yedekle değiştirilecek.</p>{sonra.map(([ad,n],i)=><div className="tools-row" key={ad}><span>{ad}</span><strong>{simdi[i][1]} → {n}</strong></div>)}<p className="tools-note">Yüklemeden önce mevcut verilerinin kurtarma kopyası cihazda saklanır. Bu işlem kayıtları birleştirmez.</p><Btn onClick={onYukle}>Yedeği yükle ve değiştir</Btn><Btn onClick={onVazgec}>Vazgeç</Btn></Modal>;
}

// ── LOCALSTORAGE HOOK ─────────────────────────────────────────────────────────
function useLocalStorage(key, defaultValue) {
  const [value, setValue] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      if (item == null) return defaultValue;
      const parsed = JSON.parse(item);
      if ((Array.isArray(defaultValue) && !Array.isArray(parsed))
        || (defaultValue && typeof defaultValue === "object" && !Array.isArray(defaultValue)
          && (!parsed || typeof parsed !== "object" || Array.isArray(parsed)))) {
        _okunamayanAnahtarlar.add(key);
        _kayitHatasi = "Kayıt biçimi okunamadı. Eski kayıt korunuyor; yedeğini alıp geçerli bir yedek yükle.";
        return defaultValue;
      }
      return parsed;
    } catch {
      _okunamayanAnahtarlar.add(key);
      _kayitHatasi = "Yerel kayıt okunamadı. Depolama iznini kontrol et; mevcut kayıtların üzerine yazma.";
      return defaultValue;
    }
  });
  const current = useRef(value);
  const setItem = useCallback(newValue => {
    if (_buIslemKayitHatali) return false;
    if (_okunamayanAnahtarlar.has(key)) {
      kayitHatasiBildir("Bu kayıt okunamadığı için üzerine yazılmadı. Geçerli bir yedeği yükleyerek kurtar.");
      return false;
    }
    // Aynı kullanıcı işleminin ardışık yazmaları birlikte geri alınır.
    // Örn. ödeme kaydı yazılamazsa ona bağlı gider kaydı da bırakılmaz.
    if (!_kayitGrubu) {
      _kayitGrubu = { oncekiler: new Map() };
      const grup = _kayitGrubu;
      Promise.resolve().then(() => { if (_kayitGrubu === grup) _kayitGrubu = null; });
    }
    const grup = _kayitGrubu;
    const val = typeof newValue === "function" ? newValue(current.current) : newValue;
    try {
      const oncekiHam = window.localStorage.getItem(key);
      const oncekiDeger = current.current;
      const degisiklikler=kayitDegisiklikleri(key,oncekiDeger,val);
      if(degisiklikler.length) {
        const alan='ft_kayit_gecmisi',ham=window.localStorage.getItem(alan);
        const onceki=JSON.parse(ham||'[]');
        window.localStorage.setItem(alan,JSON.stringify([...onceki,...degisiklikler].slice(-300)));
        if(!grup.oncekiler.has(alan)) grup.oncekiler.set(alan,{ham,geriAl:()=>window.dispatchEvent(new Event('finora-gecmis'))});
      }
      window.localStorage.setItem(key, JSON.stringify(val));
      if (!grup.oncekiler.has(key)) grup.oncekiler.set(key, {
        ham: oncekiHam,
        geriAl: () => { current.current = oncekiDeger; setValue(oncekiDeger); },
      });
    } catch {
      let geriAlindi = true;
      for (const [alan, onceki] of [...grup.oncekiler.entries()].reverse()) {
        try {
          if (onceki.ham === null) window.localStorage.removeItem(alan);
          else window.localStorage.setItem(alan, onceki.ham);
          onceki.geriAl();
        } catch { geriAlindi = false; }
      }
      kayitHatasiBildir(geriAlindi
        ? "Depolama alanı dolu veya yazma izni kapalı. Bu işlem kaydedilmedi; önceki kayıtlar korundu."
        : "Depolama kullanılamıyor; bu işlemin bazı kayıtları geri alınamadı. Uygulamayı yenilemeden mevcut verilerin dosya yedeğini al.");
      return false;
    }
    current.current = val;
    setValue(val);
    window.dispatchEvent(new Event('finora-gecmis'));
    return true;
  }, [key]);
  return [value, setItem];
}


// Yedek tamamen doğrulanır; kayıtlar ancak bundan sonra değiştirilir.
function yedekVerisiniDogrula(paket) {
  const nesneMi = v => v !== null && typeof v === "object" && !Array.isArray(v);
  if (!nesneMi(paket) || !["finora-v2", "finora-v1", "butce-takip-v1"].includes(paket.format) || !nesneMi(paket.data)) throw new Error("Yedek biçimi geçersiz.");
  const diziAnahtarlari = ["ft_islemler", "ft_odemeler", "ft_kartlar", "ft_kart_odemeleri", "ft_hizli_sablonlar", "ft_tekrarlayan", "ft_krediler", "ft_taksitler"];
  const nesneAnahtarlari = ["ft_butceler", "ft_kategoriler", "ft_sabit_log", "ft_zamlar", "ft_enflasyon", "ft_maas_ayarlar", "ft_bildirim_ayarlari"];
  const temiz = {};
  function guvenliJson(v, derinlik = 0) {
    if (derinlik > 30) throw new Error("Yedek çok fazla iç içe kayıt içeriyor.");
    if (typeof v === "number" && !Number.isFinite(v)) throw new Error("Geçersiz sayı.");
    if (v && typeof v === "object") Object.entries(v).forEach(([k, val]) => {
      if (["__proto__", "prototype", "constructor"].includes(k)) throw new Error("Geçersiz kayıt alanı.");
      guvenliJson(val, derinlik + 1);
    });
  }
  guvenliJson(paket.data);
  for (const [key, value] of Object.entries(paket.data)) {
    if (!key.startsWith("ft_") || key.startsWith("ft_manual_backup") || key.startsWith("ft_auto_backup")) continue;
    if (diziAnahtarlari.includes(key)) {
      if (!Array.isArray(value)) throw new Error(`${key}: liste bekleniyor.`);
      const ids = new Set();
      value.forEach(kayit => {
        if (!nesneMi(kayit) || kayit.id == null || !["string", "number"].includes(typeof kayit.id) || String(kayit.id) === "") throw new Error(`${key}: kayıt kimliği eksik.`);
        if (ids.has(String(kayit.id))) throw new Error(`${key}: yinelenen kayıt kimliği.`);
        ids.add(String(kayit.id));
        if (["ft_islemler", "ft_odemeler", "ft_kart_odemeleri", "ft_hizli_sablonlar", "ft_tekrarlayan"].includes(key) && !tutarGecerli(kayit.tutar, true)) throw new Error(`${key}: geçersiz tutar.`);
        if (["ft_islemler", "ft_odemeler", "ft_kart_odemeleri"].includes(key) && !yerelTarihOku(kayit.tarih)) throw new Error(`${key}: geçersiz tarih.`);
        if (key === "ft_islemler" && !["gelir", "gider"].includes(kayit.tip)) throw new Error("İşlem türü geçersiz.");
        if (key === "ft_odemeler" && kayit.odendi != null && typeof kayit.odendi !== "boolean") throw new Error("Ödeme durumu geçersiz.");
        if (key === "ft_kartlar" && typeof kayit.isim !== "string") throw new Error("Kart adı geçersiz.");
        if (key === "ft_hizli_sablonlar" && kayit.tip!=null && !["gelir","gider"].includes(kayit.tip)) throw new Error("Hızlı işlem türü geçersiz.");
        if (key === "ft_hizli_sablonlar" && typeof kayit.kategori !== "string") throw new Error("Şablon kategorisi geçersiz.");
        if (key === "ft_tekrarlayan" && (!tamSayiGecerli(kayit.gun, 1, 31) || typeof kayit.aktif !== "boolean")) throw new Error("Sabit gider günü/durumu geçersiz.");
        if (key === "ft_taksitler" && (!tutarGecerli(kayit.aylikTutar) || !tamSayiGecerli(kayit.vade, 1, 1200) || !tamSayiGecerli(kayit.odenen ?? 0, 0, Number(kayit.vade)))) throw new Error("Taksit kaydı geçersiz.");
        if (key === "ft_krediler") {
          if (!Array.isArray(kayit.taksitler) || !tamSayiGecerli(kayit.vade, 1, 1200) || kayit.taksitler.length !== Number(kayit.vade)) throw new Error("Kredi planı geçersiz.");
          kayit.taksitler.forEach(t => { if (!nesneMi(t) || !tutarGecerli(t.tutar) || !yerelTarihOku(t.tarih) || typeof t.odendi !== "boolean" || !tamSayiGecerli(t.no, 1, Number(kayit.vade))) throw new Error("Kredi taksiti geçersiz."); });
        }
        for (const alan of ["isim", "ad", "aciklama", "kategori", "kart", "kartAdi", "baslangic", "bitis", "sonOdemeTarihi", "hesapKesimTarihi"]) {
          if (kayit[alan] != null && typeof kayit[alan] !== "string") throw new Error(`${key}: ${alan} metin olmalı.`);
        }
        if (["ft_islemler", "ft_odemeler", "ft_tekrarlayan"].includes(key) && typeof kayit.kategori !== "string") throw new Error("Kategori eksik.");
        for (const alan of ["baslangic", "bitis", "sonOdemeTarihi", "hesapKesimTarihi"]) {
          if (kayit[alan] && !yerelTarihOku(kayit[alan])) throw new Error(`${key}: ${alan} tarihi geçersiz.`);
        }
        if (kayit.kartEkstreAktarimlari != null && (!nesneMi(kayit.kartEkstreAktarimlari) || Object.entries(kayit.kartEkstreAktarimlari).some(([gun, tutar]) => !yerelTarihOku(gun) || !tutarGecerli(tutar, true)))) throw new Error("Kart ekstre aktarımı geçersiz.");
        if (kayit.kartGunlukHareketler != null && (!nesneMi(kayit.kartGunlukHareketler) || Object.entries(kayit.kartGunlukHareketler).some(([gun, tutar]) => !yerelTarihOku(gun) || !tutarGecerli(tutar, true, true)))) throw new Error("Günlük kart hareketleri geçersiz.");
        for (const alan of ["donemIci", "provizyon", "ekstre", "aylikTutar", "toplamTutar"]) {
          if (kayit[alan] != null && !tutarGecerli(kayit[alan], true)) throw new Error(`${key}: ${alan} geçersiz.`);
        }
      });
    }
    if (nesneAnahtarlari.includes(key) && !nesneMi(value)) throw new Error(`${key}: nesne bekleniyor.`);
    if (key === "ft_kategoriler" && (!["gelir", "gider"].every(t => Array.isArray(value[t]) && value[t].every(k => typeof k === "string")))) throw new Error("Kategori listeleri geçersiz.");
    if (key === "ft_donem_gun" && !tamSayiGecerli(value, 1, 28)) throw new Error("Maaş dönemi günü geçersiz.");
    if(key==='ft_kayit_gecmisi' && (!Array.isArray(value)||value.length>300||value.some(h=>!nesneMi(h)||typeof h.key!=='string'||typeof h.kayitId!=='string'||typeof h.zaman!=='string'||!Number.isFinite(Date.parse(h.zaman))||!['Eklendi','Silindi','Değiştirildi'].includes(h.islem)||![h.onceki,h.yeni].every(i=>i===null||nesneMi(i)))))throw new Error('Kayıt geçmişi geçersiz.');
    if (key === "ft_schema_version" && typeof value !== "string") throw new Error("Sürüm kaydı geçersiz.");
    if (key === "ft_bakiye_hesap_modu" && typeof value !== "string") throw new Error("Bakiye hesap modu geçersiz.");
    if (key === "ft_tasarruf_hedefi" && !tamSayiGecerli(value,0,100)) throw new Error("Tasarruf hedefi geçersiz.");
    if (key === "ft_grafik_tipi" && !["halka","ibre"].includes(value)) throw new Error("Bütçe grafiği seçimi geçersiz.");
    if (key === "ft_kategori_gorunumleri" && (!nesneMi(value) || Object.values(value).some(v => !nesneMi(v) || !KATEGORI_SIMGE_SECENEKLERI.some(([id]) => id === v.ikon) || !/^#[0-9a-f]{6}$/i.test(v.renk)))) throw new Error("Kategori simge veya renk ayarları geçersiz.");
    if (key === "ft_tema" && !["light", "dark", "gray"].includes(value)) throw new Error("Tema geçersiz.");
    if (key === "ft_butceler" && Object.values(value).some(v => v !== "" && !tutarGecerli(v, true))) throw new Error("Bütçe limiti geçersiz.");
    if (["ft_zamlar", "ft_enflasyon"].includes(key) && Object.values(value).some(v => v !== "" && (!Number.isFinite(sayisalOku(v)) || sayisalOku(v) <= -100 || sayisalOku(v) > 10000))) throw new Error("Oran kaydı geçersiz.");
    if (key === "ft_sabit_log" && Object.values(value).some(v => typeof v !== "boolean")) throw new Error("Sabit gider günlüğü geçersiz.");
    if(key==='ft_kart_borc_ozetleri' && (!nesneMi(value)||Object.values(value).some(d=>!nesneMi(d)||Object.values(d).some(k=>!nesneMi(k)||!tutarGecerli(k.toplam,true)||!yerelTarihOku(k.tarih)))))throw new Error('Kart borcu dönem geçmişi geçersiz.');
    if (key === "ft_donem_devir_log" && (!nesneMi(value) || Object.values(value).some(v => !nesneMi(v) || !tutarGecerli(v.tutar,true) || !yerelTarihOku(v.tarih)))) throw new Error("Dönem devri kaydı geçersiz.");
    if (key === "ft_son_bildirim_planlama" && value !== null && !nesneMi(value)) throw new Error("Bildirim kaydı geçersiz.");
    if (key === "ft_bildirim_ayarlari" && value.saatler != null && (!Array.isArray(value.saatler) || value.saatler.length < 1 || value.saatler.length > 3 || value.saatler.some(s => typeof s !== "string" || !/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(s)))) throw new Error("Bildirim saatleri geçersiz.");
    if (key === "ft_bildirim_ayarlari" && (!Array.isArray(value.gunler) || value.gunler.some(g => !tamSayiGecerli(g, 0, 365)) || typeof value.saat !== "string" || !/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(value.saat))) throw new Error("Bildirim ayarları geçersiz.");
    if (key === "ft_maas_ayarlar") {
      if (value.mevcutMaas && !tutarGecerli(value.mevcutMaas)) throw new Error("Maaş tutarı geçersiz.");
      if (value.hesapDonem && !["ocak", "temmuz"].includes(value.hesapDonem)) throw new Error("Maaş hesap dönemi geçersiz.");
    }
    // Eski yedeklerde sayısal metinleri destekle; uygulamaya standart TL sayıları ver.
    temiz[key] = Array.isArray(value) && diziAnahtarlari.includes(key) ? value.map(kayit => {
      const kopya = { ...kayit };
      for (const alan of ["tutar", "donemIci", "provizyon", "ekstre", "aylikTutar", "toplamTutar", "kartGunlukDelta", "kartGunBaslangicToplam"]) {
        if (kopya[alan] != null) {
          if (!tutarGecerli(kopya[alan], true, true)) throw new Error(`${key}: ${alan} para değeri geçersiz.`);
          kopya[alan] = paraOku(kopya[alan]);
        }
      }
      if (Array.isArray(kopya.taksitler)) kopya.taksitler = kopya.taksitler.map(t => ({ ...t, tutar: paraOku(t.tutar) }));
      return kopya;
    }) : value;
  }
  if (!diziAnahtarlari.some(key => Object.prototype.hasOwnProperty.call(temiz, key))) throw new Error("Yedekte uygulama kayıtları bulunamadı.");
  return temiz;
}
function yedegiGuvenleYaz(data, storage = window.localStorage) {
  const onceki = Object.fromEntries(Object.keys(storage).filter(k => k.startsWith("ft_")).map(k => [k, storage.getItem(k)]));
  const yeni = Object.fromEntries(Object.entries(data).map(([k, v]) => [k, JSON.stringify(v)]));
  try {
    // Eski kayıtları peşinen silme. Yeni kayıtlar başarıyla yazıldıktan sonra artık kullanılmayanları kaldır.
    Object.entries(yeni).forEach(([k, v]) => storage.setItem(k, v));
    Object.keys(onceki).filter(k => !k.startsWith("ft_auto_backup") && !k.startsWith("ft_manual_backup") && !Object.prototype.hasOwnProperty.call(yeni, k)).forEach(k => storage.removeItem(k));
  } catch (hata) {
    try {
      Object.keys(yeni).filter(k => !Object.prototype.hasOwnProperty.call(onceki, k)).forEach(k => storage.removeItem(k));
      Object.entries(onceki).forEach(([k, v]) => storage.setItem(k, v));
    } catch {
      const kurtarma = { format: "finora-v2", olusturulma: new Date().toISOString(), data: {} };
      Object.entries(onceki).forEach(([k, v]) => { try { kurtarma.data[k] = JSON.parse(v); } catch { kurtarma.data[k] = v; } });
      const url = URL.createObjectURL(new Blob([JSON.stringify(kurtarma)], { type: "application/json" }));
      const a = document.createElement("a"); a.href = url; a.download = "finora-yukleme-oncesi-kurtarma.json"; a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      kayitHatasiBildir("Yedek yüklenemedi ve eski kayıtlar depolamaya tamamen geri yazılamadı. Yükleme öncesi kurtarma dosyası indirilmeye çalışıldı; dosyanın kaydedildiğini kontrol et.");
      throw new Error("Depolama kullanılamıyor. Kurtarma dosyasını sakla; uygulamayı yenileme.");
    }
    throw new Error("Yedek yazılamadı; önceki kayıtlar geri yüklendi. Depolama alanını/iznini kontrol et.");
  }
}

// ── INITIAL DATA ─────────────────────────────────────────────────────────────
const INITIAL_ISLEMLER = [];
const INITIAL_TEKRARLAYAN = [];
// ── HELPERS ──────────────────────────────────────────────────────────────────
const fmt = (n) => new Intl.NumberFormat("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
const kisaTarih = (tarih) => tarih ? `${String(tarih.getDate()).padStart(2, "0")}.${String(tarih.getMonth() + 1).padStart(2, "0")}` : "";

// ── YAHOO FINANCE ─────────────────────────────────────────────────────────────
// Bigpara altın sembol → API kodu mapping


// ── UI ATOMS ─────────────────────────────────────────────────────────────────
function Modal({ title, onClose, children }) {
  const panel = useRef(null);
  const kapat = useRef(onClose);
  kapat.current = onClose;
  useEffect(() => {
    const oncekiOdak = document.activeElement;
    const oncekiOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const odaklanabilirler = () => [...(panel.current?.querySelectorAll('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex="0"]') || [])].filter(el => el.getClientRects().length);
    (panel.current?.querySelector("input:not([disabled]), select:not([disabled]), textarea:not([disabled])") || odaklanabilirler()[0] || panel.current)?.focus();
    const tus = e => {
      const paneller = document.querySelectorAll("[data-app-modal]");
      if (paneller[paneller.length - 1] !== panel.current) return;
      if (e.key === "Escape") { e.preventDefault(); e.stopPropagation(); kapat.current(); }
      if (e.key === "Tab") {
        const liste = odaklanabilirler();
        if (!liste.length) { e.preventDefault(); panel.current?.focus(); return; }
        const ilk = liste[0], son = liste[liste.length - 1];
        if (e.shiftKey && (document.activeElement === ilk || !panel.current?.contains(document.activeElement))) { e.preventDefault(); son.focus(); }
        else if (!e.shiftKey && (document.activeElement === son || !panel.current?.contains(document.activeElement))) { e.preventDefault(); ilk.focus(); }
      }
    };
    const odak = e => {
      const paneller = document.querySelectorAll("[data-app-modal]");
      if (paneller[paneller.length - 1] === panel.current && !panel.current?.contains(e.target)) (odaklanabilirler()[0] || panel.current)?.focus();
    };
    document.addEventListener("keydown", tus, true);
    document.addEventListener("focusin", odak);
    return () => {
      document.removeEventListener("keydown", tus, true);
      document.removeEventListener("focusin", odak);
      document.body.style.overflow = oncekiOverflow;
      if (oncekiOdak?.isConnected) oncekiOdak.focus();
    };
  }, []);
  const icerik = (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.72)", zIndex: 1000, display: "flex", alignItems: "flex-end", justifyContent: "center", paddingTop: "calc(10px + env(safe-area-inset-top))", animation: "fadeIn 0.22s ease" }} onClick={onClose}>
      <div ref={panel} data-app-modal role="dialog" aria-modal="true" aria-label={title} tabIndex={-1} className="app-modal-panel" style={{ background: "var(--card)", borderTop: "1px solid var(--border-light)", borderLeft: "1px solid var(--border)", borderRight: "1px solid var(--border)", borderRadius: "26px 26px 0 0", padding: "14px 22px calc(26px + env(safe-area-inset-bottom))", width: "100%", maxWidth: 480, maxHeight: "calc(100dvh - 10px - env(safe-area-inset-top))", overflowY: "auto", overscrollBehavior: "contain", WebkitOverflowScrolling: "touch", boxSizing: "border-box", animation: "sheetUp 0.3s cubic-bezier(0.16,1,0.3,1)", boxShadow: "0 -12px 40px rgba(0,0,0,0.4)" }} onClick={e => e.stopPropagation()}>
        <div style={{ width: 40, height: 4, background: "var(--border2)", borderRadius: 99, margin: "0 auto 16px" }} />
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
          <span style={{ color: "var(--text)", fontSize: 18, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{title}</span>
          <button aria-label="Pencereyi kapat" onClick={onClose} style={{ background: "var(--card2)", border: "1px solid var(--border)", color: "var(--text3)", borderRadius: 99, width: 32, height: 32, cursor: "pointer", fontSize: 16, display: "flex", alignItems: "center", justifyContent: "center" }}>×</button>
        </div>
        {children}
      </div>
    </div>
  );
  return typeof document !== "undefined" ? createPortal(icerik, document.body) : icerik;
}
function Input({ label, ...props }) {
  const generatedId = useId();
  const id = props.id || generatedId;
  return (
    <div style={{ marginBottom: 14 }}>
      {label && <label htmlFor={id} style={{ color: "var(--text4)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", display: "block", marginBottom: 6, letterSpacing: "0.06em", textTransform: "none" }}>{label}</label>}
      <input {...(props.type === "number" ? { step: "any", inputMode: "decimal" } : {})} {...props} id={id} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 14px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 14, outline: "none", boxSizing: "border-box", ...props.style }} />
    </div>
  );
}
function Select({ label, children, ...props }) {
  const generatedId = useId();
  const id = props.id || generatedId;
  return (
    <div style={{ marginBottom: 14 }}>
      {label && <label htmlFor={id} style={{ color: "var(--text4)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", display: "block", marginBottom: 6, letterSpacing: "0.06em", textTransform: "none" }}>{label}</label>}
      <select {...props} id={id} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 14px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 14, outline: "none", boxSizing: "border-box" }}>{children}</select>
    </div>
  );
}
function Btn({ children, onClick, variant = "primary", style = {}, disabled, "aria-label": ariaLabel }) {
  const styles = { primary: { background: disabled ? "var(--text5)" : "var(--accent-strong)", color: "var(--on-accent)" }, secondary: { background: "var(--border)", color: "var(--text)" }, danger: { background: "#ef444422", color: "var(--expense-text)", border: "1px solid #ef444444" } };
  return <button aria-label={ariaLabel} onClick={onClick} disabled={disabled} style={{ border: "none", borderRadius: 10, padding: "10px 18px", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, fontSize: 13, cursor: disabled ? "not-allowed" : "pointer", letterSpacing: "0.04em", opacity: disabled ? 0.6 : 1, ...styles[variant], ...style }}>{children}</button>;
}
function Badge({ text, color }) {
  return <span style={{ background: color + "22", color, border: `1px solid ${color}44`, borderRadius: 6, padding: "2px 8px", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{text}</span>;
}

function Icon({ name, size = 22, strokeWidth = 1.9 }) {
  const paths = {
    building:<><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 7h1m4 0h1M9 11h1m4 0h1M9 15h1m4 0h1M10 21v-3h4v3"/></>,
    bolt:<path d="m13 2-9 12h7l-1 8 10-13h-7l0-7Z"/>,
    drop:<path d="M12 2C9 7 5 10 5 14a7 7 0 0 0 14 0c0-4-4-7-7-12Z"/>,
    flame:<path d="M12 2c2 5 6 6 6 12a6 6 0 0 1-12 0c0-3 2-5 4-7 0 4 2 4 2 1V2Z"/>,
    wifi:<><path d="M3 7a15 15 0 0 1 18 0M6 11a10 10 0 0 1 12 0M9 15a5 5 0 0 1 6 0"/><circle cx="12" cy="19" r="1"/></>,
    phone:<><rect x="6" y="2" width="12" height="20" rx="3"/><path d="M10 5h4M11 18h2"/></>,
    basketball:<><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3v18M5.5 5.5c7 1 12 6 13 13M18.5 5.5c-7 1-12 6-13 13"/></>,
    school:<><path d="m3 9 9-6 9 6v12H3V9ZM10 21v-6h4v6M7 11h1m8 0h1M12 3V1"/></>,
    piggy:<><path d="M5 9a8 6 0 0 1 13-1l3-1v7l-3 1-1 5h-3v-3H9v3H6l-1-5H3V9h2ZM10 6h4"/><circle cx="16" cy="10" r=".6"/></>,
    banknote:<><rect x="2" y="5" width="20" height="14" rx="2"/><circle cx="12" cy="12" r="3"/><path d="M5 9h1m12 6h1"/></>,
    transfer:<><path d="M3 7h17l-4-4M21 17H4l4 4"/></>,
    carry:<><path d="M4 12h15l-4-4m4 4-4 4M4 4v16"/></>,
    exam:<><path d="M6 3h12v18H6V3ZM9 8h6M9 12h6m-6 4h3"/></>,
    walletPlus:<><rect x="3" y="5" width="18" height="15" rx="2"/><path d="M3 9h18M12 12v5m-3-2.5h6"/></>,
    briefcase: <><rect x="3" y="7" width="18" height="14" rx="2"/><path d="M8 7V3h8v4M3 12h18M10 12v3h4v-3"/></>,
    screen: <><rect x="3" y="4" width="18" height="13" rx="2"/><path d="M8 21h8M12 17v4"/></>,
    cart: <><path d="M2 3h3l3 13h11l3-9H6M9 20h.01M18 20h.01"/><circle cx="9" cy="20" r="1"/><circle cx="18" cy="20" r="1"/></>,
    car: <><path d="m5 10 2-6h10l2 6M3 10h18v8H3v-8M5 18v3M19 18v3M6 14h2M16 14h2"/></>,
    medical: <><path d="M9 3h6v6h6v6h-6v6H9v-6H3V9h6V3Z"/></>,
    play: <><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4V8Z"/></>,
    shirt: <><path d="m8 3-6 4 3 5 3-2v11h8V10l3 2 3-5-6-4a4 4 0 0 1-8 0Z"/></>,
    utensils: <><path d="M4 3v6a3 3 0 0 0 6 0V3M7 3v18M18 3c-3 4-3 8 0 9h2V3h-2ZM20 12v9"/></>,
    package: <><path d="m3 7 9-5 9 5v10l-9 5-9-5V7Zm0 0 9 5 9-5M12 12v10M7 4l9 5"/></>,
    headphones: <><path d="M3 14v-2a9 9 0 0 1 18 0v2"/><rect x="3" y="12" width="4" height="8" rx="2"/><rect x="17" y="12" width="4" height="8" rx="2"/></>,
    plane: <><path d="m2 16 8-4V4a2 2 0 0 1 4 0v8l8 4v3l-8-2v4l-2 1-2-1v-4l-8 2v-3Z"/></>,
    book: <><path d="M12 5v16M3 3l9 2 9-2v16l-9 2-9-2V3Z"/></>,
    arrowDown: <><path d="M7 7l10 10M17 7v10H7"/></>,
    arrowUp: <><path d="M7 17 17 7M7 7h10v10"/></>,
    filter: <><path d="M4 6h16M7 12h10M10 18h4"/></>,
    download: <><path d="M12 3v12m-4-4 4 4 4-4M4 16v5h16v-5"/></>,
    edit: <><path d="m15 4 5 5M4 20l4-1L20 7a2 2 0 0 0-3-3L5 16l-1 4Z"/></>,
    trash: <><path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/></>,
    search: <><circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/></>,
    target: <><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="m12 12 8-8"/></>,
    bell: <><path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5l-2 3ZM10 21h4"/></>,
    tag: <><path d="M3 3h8l10 10-8 8L3 11V3Z"/><circle cx="7" cy="7" r="1"/></>,
    check: <><path d="m5 12 4 4L19 6"/></>,
    menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
    home: <><path d="m3 11 9-8 9 8" /><path d="M5 10v10h14V10M9 20v-6h6v6" /></>,
    card: <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="M3 10h18M7 15h3" /></>,
    chart: <><path d="M4 19V9M10 19V5M16 19v-7M22 19H2" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.82 2.82-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.04 1.56V21h-4v-.08A1.7 1.7 0 0 0 8.96 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.82-2.82.06-.06A1.7 1.7 0 0 0 4.6 15 1.7 1.7 0 0 0 3.08 14H3v-4h.08A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.82-2.82.06.06A1.7 1.7 0 0 0 8.96 4.6 1.7 1.7 0 0 0 10 3.08V3h4v.08A1.7 1.7 0 0 0 15.04 4.6a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.82 2.82-.06.06A1.7 1.7 0 0 0 19.4 9 1.7 1.7 0 0 0 20.92 10H21v4h-.08A1.7 1.7 0 0 0 19.4 15Z" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M7 3v4M17 3v4M3 10h18" /></>,
    shield: <><path d="M12 3 4 6v6c0 4 4 7 8 9 4-2 8-5 8-9V6l-8-3Z" /><path d="m8.5 12 2.5 2.5 4.5-5" /></>,
    warning: <><path d="m12 3 10 18H2L12 3Z" /><path d="M12 9v5M12 17h.01" /></>,
    alert: <><circle cx="12" cy="12" r="9" /><path d="M12 7v6M12 17h.01" /></>,
    moon: <><path d="M20 15.3A8.5 8.5 0 0 1 8.7 4a8.5 8.5 0 1 0 11.3 11.3Z" /></>,
    sun: <><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.65 17.65l1.42 1.42M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.65 6.35l1.42-1.42" /></>,
    wallet: <><path d="M4 7.5V6a2 2 0 0 1 2-2h12v4" /><rect x="3" y="7" width="18" height="13" rx="3" /><path d="M16 12h5v4h-5a2 2 0 0 1 0-4Z" /></>,
    receipt: <><path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3Z" /><path d="M9 8h6M9 12h6" /></>,
    repeat: <><path d="m17 2 4 4-4 4" /><path d="M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4" /><path d="M21 13v2a3 3 0 0 1-3 3H3" /></>,
    coins: <><ellipse cx="9" cy="7" rx="5" ry="3" /><path d="M4 7v4c0 1.7 2.2 3 5 3 1 0 2-.2 2.8-.5M14 9.2V7" /><ellipse cx="15" cy="15" rx="5" ry="3" /><path d="M10 15v3c0 1.7 2.2 3 5 3s5-1.3 5-3v-3" /></>,
    chevron: <><path d="m9 18 6-6-6-6" /></>,
    close: <><path d="M6 6l12 12M18 6 6 18" /></>,
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ display: "block" }}>
      {paths[name] || paths.wallet}
    </svg>
  );
}

// Mini sparkline: segmentler yöne göre renkli (düşüş yeşil, yükseliş kırmızı)

// Kategori aylık değişim grafiği — gradyanlı alan grafiği, her noktada değer etiketi, son nokta vurgulu balon
const fmtK = n => Math.round(n).toLocaleString("tr-TR");
function KategoriAlanGrafik({ seri, renk, gradId }) {
  if (!seri || seri.length < 2) return null;
  const CustomLabel = (props) => {
    const { x, y, value } = props;
    if (value == null || x == null || y == null) return null;
    const metin = "₺" + fmtK(value);
    if (value === 0) return null;
    return <text x={x} y={y - 9} textAnchor="middle" fontSize={8.5} fill="var(--text5)" fontFamily="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif">{metin}</text>;
  };
  return (
    <ResponsiveContainer width="100%" height={110}>
      <AreaChart data={seri} margin={{ top: 30, right: 10, left: 10, bottom: 0 }}>
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={renk} stopOpacity={0.5} />
            <stop offset="100%" stopColor={renk} stopOpacity={0} />
          </linearGradient>
        </defs>
        <XAxis dataKey="ay" hide />
        <YAxis hide domain={[0, "dataMax"]} />
        <Area type="monotone" dataKey="deger" stroke={renk} strokeWidth={2.5} fill={`url(#${gradId})`} dot={{ r: 3.5, fill: renk, strokeWidth: 0 }} activeDot={{ r: 5 }}>
          <LabelList dataKey="deger" content={CustomLabel} />
        </Area>
      </AreaChart>
    </ResponsiveContainer>
  );
}

// ── PROFİL PANELİ ─────────────────────────────────────────────────────────────

// ── MAAŞ AYI: 15'ten 14'e dönem ─────────────────────────────────────────────
function maasDonemKey(tarihStr, basGun) {
  const gun = basGun || ((parseInt((()=>{try{return localStorage.getItem("ft_donem_gun");}catch(e){return null;}})() || "15") || 15) || 15);
  const donem = maasDonemiBul(tarihStr, gun);
  if (!donem) return { yil: 0, ay: 0, key: "gecersiz", label: "Geçersiz tarih", baslangic: "" };
  const { baslangic: bas, bitis: bit, key } = donem;
  const label = `${bas.getDate()} ${AYLAR_TAM[bas.getMonth()]} – ${bit.getDate()} ${AYLAR_TAM[bit.getMonth()]}`;
  return { yil: bas.getFullYear(), ay: bas.getMonth(), key, label, baslangic: yerelTarihAnahtari(bas), bitis: yerelTarihAnahtari(bit) };
}
// Android alarmları uygulamanın JavaScript sürecinden bağımsız çalışır.
function bildirimEklentisi() {
  return Capacitor.isNativePlatform() && Capacitor.isPluginAvailable("LocalNotifications") ? LocalNotifications : null;
}
let bildirimPlanlamaSirasi = Promise.resolve();
const FINORA_BILDIRIM_KANALI = "finora_odemeler_v2";
async function bildirimKanaliHazirla(LN) {
  if (!LN || typeof LN.createChannel !== "function") return null;
  try {
    await LN.createChannel({
      id: FINORA_BILDIRIM_KANALI,
      name: "Ödeme Hatırlatmaları",
      description: "Fatura, kredi kartı ve taksit hatırlatmaları",
      importance: 5,
      visibility: 1,
      vibration: true,
      lights: true,
    });
    return FINORA_BILDIRIM_KANALI;
  } catch (e) {
    throw new Error("Bildirim kanalı oluşturulamadı: " + (e.message || "Android ayarlarını kontrol et"));
  }
}
async function bildirimIzniAl(LN) {
  try {
    const izin = await LN.checkPermissions();
    if (izin.display === "granted") return true;
    const sonuc = await LN.requestPermissions();
    return sonuc.display === "granted";
  } catch (e) { return false; }
}

function getDonemGun() {
  try { return parseInt(localStorage.getItem("ft_donem_gun") || "15") || 15; } catch (e) { return 15; }
}
function buAnkiDonem() {
  const gun = getDonemGun();
  const b = new Date();
  if (b.getDate() >= gun) return `${b.getFullYear()}-${b.getMonth()}-${gun}`;
  const bas = new Date(b.getFullYear(), b.getMonth() - 1, gun);
  return `${bas.getFullYear()}-${bas.getMonth()}-${gun}`;
}
function donemLabel(yil, ay, basGun) {
  const gun = Math.min(28,Math.max(1,Number(basGun)||15));
  const ilk = new Date(yil,ay,gun);
  const son = new Date(yil,ay+1,gun-1);
  const opts = {day:'numeric',month:'short'};
  return `${ilk.toLocaleDateString('tr-TR',opts)} – ${son.toLocaleDateString('tr-TR',opts)}`;
}

// Seçilebilir halka/ibre; eşikler ana kartta, 3500 ms animasyon aynen korunur.
function HarcamaGauge({ oran, grafikTipi = "halka" }) {
  const [animOran, setAnimOran] = useState(0);
  useEffect(() => {
    setAnimOran(0);
    const hedef = Math.max(0, Number.isFinite(oran) ? oran : 0);
    let baslangic = null;
    const sure = 3500;
    let raf;
    const adim = zaman => {
      if (baslangic === null) baslangic = zaman;
      const t = Math.min(1, (zaman - baslangic) / sure);
      setAnimOran(hedef * (1 - Math.pow(1 - t, 3)));
      if (t < 1) raf = requestAnimationFrame(adim);
    };
    raf = requestAnimationFrame(adim);
    return () => cancelAnimationFrame(raf);
  }, [oran, grafikTipi]);
  const gosterOran = Math.max(0, animOran);
  const doluluk = Math.min(100, gosterOran);
  const veriVar = Number.isFinite(oran);
  if(grafikTipi === "ibre") {
    // Cep Bütçem Design.BudgetGauge: aynı ölçüler, dilimler, ölçek ve merkez göbeği.
    const cx=160,cy=145,r=117;
    const point=(angle,radius)=>[cx+radius*Math.cos(angle*Math.PI/180),cy-radius*Math.sin(angle*Math.PI/180)];
    const arc=(from,to)=>{const a=point(from,r),b=point(to,r);return `M ${a[0]} ${a[1]} A ${r} ${r} 0 0 1 ${b[0]} ${b[1]}`;};
    const angle=180-doluluk*1.8;
    const tip=point(angle,89),left=point(angle+90,5),right=point(angle-90,5);
    const percentageColor='#fff';
    return <svg className="budget-needle" viewBox="0 0 320 235" role="img" aria-label={veriVar?`Kullanılan bütçe oranı yüzde ${Math.round(Math.max(0,oran))}`:'Bütçe oranı için henüz gelir yok'}>
      <path d={arc(180,0)} fill="none" stroke="rgba(0,0,0,.25)" strokeWidth="18"/>
      {Array.from({length:10},(_,i)=><path data-gauge-segment={i} key={i} d={arc(180-i*18-.7,180-(i+1)*18+.7)} fill="none" stroke={i<2?'#059669':i<5?'#10b981':i<8?'#f59e0b':'#ef4444'} strokeWidth="14" strokeLinecap="butt"/>)}
      {Array.from({length:21},(_,i)=>{const major=i%5===0,a=point(180-i*9,major?94:99),b=point(180-i*9,105);return <line data-gauge-tick={i} key={i} x1={a[0]} y1={a[1]} x2={b[0]} y2={b[1]} stroke="#fff" strokeWidth={major?1.8:1} opacity={major ? .8 : .35}/>;})}
      <text x="33" y="168" textAnchor="middle" fill="rgba(255,255,255,.8)" fontSize="12">0</text><text x="287" y="168" textAnchor="middle" fill="rgba(255,255,255,.8)" fontSize="12">100</text>
      {veriVar&&<g data-gauge-angle={angle}><path d={`M ${left[0]} ${left[1]} L ${tip[0]} ${tip[1]} L ${right[0]} ${right[1]} Z`} fill="currentColor"/><circle cx={cx} cy={cy} r="8" fill="currentColor"/><circle cx={cx} cy={cy} r="3" fill="rgba(0,0,0,.22)"/></g>}
      <text x="160" y="195" textAnchor="middle" fill={percentageColor} fontSize={Math.min(35,180/(String(Math.round(gosterOran)).length+1)/.65)} fontWeight="750">{veriVar?`%${Math.round(gosterOran)}`:'—'}</text>
      <text x="160" y="218" textAnchor="middle" fill="rgba(255,255,255,.8)" fontSize="12">{veriVar?'Kullanılan bütçe oranı':'Oran için gelir gerekli'}</text>
    </svg>;
  }
  return (
    <svg className="budget-ring" viewBox="0 0 128 128" role="img" aria-label={veriVar ? `Kullanılan bütçe oranı yüzde ${Math.round(Math.max(0,oran))}` : "Bütçe oranı için henüz gelir yok"}>
      <circle cx="64" cy="64" r="51" fill="none" stroke="var(--ring-track)" strokeWidth="10" />
      {veriVar&&doluluk>0&&<circle cx="64" cy="64" r="51" pathLength="100" fill="none" stroke="#fff" strokeWidth="10" strokeDasharray={`${doluluk} ${100-doluluk}`} transform="rotate(-90 64 64)" data-ring-progress={doluluk}/>}
      <text x="64" y="66" textAnchor="middle" dominantBaseline="middle" fill="currentColor" fontSize={Math.min(26,88/(String(Math.round(gosterOran)).length+1)/.65)} fontWeight="750">{veriVar ? `%${Math.round(gosterOran)}` : '—'}</text>
      <text x="64" y="86" textAnchor="middle" fill="currentColor" opacity=".75" fontSize="9">bütçe kullanımı</text>
    </svg>
  );
}
function Money({value, prefix="₺", size=15}) {
  const ref = useRef(null);
  const text = prefix + fmt(value);
  useEffect(()=>{
    const el=ref.current;
    if(!el) return;
    const fit=()=>{
      el.style.fontSize=`${size}px`;
      const width=el.clientWidth;
      if(width>0 && el.scrollWidth>width) el.style.fontSize=`${Math.max(9,size*width/el.scrollWidth)}px`;
    };
    fit();
    const observer = typeof ResizeObserver!=='undefined' ? new ResizeObserver(fit) : null;
    observer?.observe(el);
    return ()=>observer?.disconnect();
  },[text,size]);
  return <span className="money-fit" ref={ref} title={text} style={{fontSize:size}}>{text}</span>;
}

// ── ÖZET ──────────────────────────────────────────────────────────────────────
function Ozet({ islemler, setIslemler, kartlar, setKartlar, donemGun, kategoriler, odemeler, setOdemeler, hizliModal, setHizliModal, hizliSablonlar, setHizliSablonlar, taksitler, setTaksitler, kartOdemeleri, setKartOdemeleri, kartBorcOzetleri = {}, bakiyeHesapModu, tasarrufHedefi = 0, grafikTipi = "halka" }) {
  const [donemOffset, setDonemOffset] = useState(0);
  const [kartDuzenle, setKartDuzenle] = useState(null);
  const [kartDuzForm, setKartDuzForm] = useState({ donemIci: "", provizyon: "", ekstre: "", bakiyedenDussun: true });
  const [kartOdemeModal, setKartOdemeModal] = useState(null);
  const [kartOdemeTarih, setKartOdemeTarih] = useState(yerelTarihAnahtari());
  const [hizliForm, setHizliForm] = useState({ tip: "gider", kategori: "Market", tutar: "", aciklama: "", tarih: yerelTarihAnahtari(), donemeDahil: true, donemIci: "", provizyon: "", taksitAd: "", taksitAylik: "", taksitVade: "", taksitOdenen: "", taksitKart: "" });
  const hizliKartMi = (kartlar || []).some(k => k.isim === hizliForm.kategori) && hizliForm.tip === "gider";
  const [hizliListeAcik,setHizliListeAcik]=useState(false);
  const [sablonModal,setSablonModal]=useState(false);
  const [sablonForm,setSablonForm]=useState({id:null,ad:'',tip:'gider',kategori:'Market',tutar:''});
  const [seciliSablon,setSeciliSablon]=useState(null);
  const sablonKategorileri=tip=>(kategoriler?.[tip]||VARSAYILAN_KATEGORILER[tip]).filter(k=>tip!=='gider'||!kartlar.some(c=>c.isim===k));
  const sablonKimligi=()=>globalThis.crypto?.randomUUID?.()||`${Date.now()}-${Math.random().toString(36).slice(2)}`;
  const sablonFormunuAc=s=>{
    const tip=s?.tip==='gelir'?'gelir':'gider',cats=sablonKategorileri(tip);
    setSablonForm({id:s?.id??null,ad:s?.ad||s?.aciklama||'',tip,kategori:cats.includes(s?.kategori)?s.kategori:(cats.includes('Market')?'Market':cats[0]||''),tutar:s?String(s.tutar):''});
    setSablonModal(true);
  };
  const sablonKaydet=()=>{
    if(!sablonForm.ad.trim()||!tutarGecerli(sablonForm.tutar)||!sablonKategorileri(sablonForm.tip).includes(sablonForm.kategori)){formHatasi('İşlem adı, kategori ve geçerli pozitif tutar gir.');return;}
    const yeni={...sablonForm,id:sablonForm.id??sablonKimligi(),ad:sablonForm.ad.trim(),aciklama:sablonForm.ad.trim(),tutar:paraOku(sablonForm.tutar)};
    if(setHizliSablonlar(p=>sablonForm.id!=null?p.map(s=>s.id===sablonForm.id?{...s,...yeni}:s):[...p,yeni])===false)return;
    setSablonModal(false);setHizliListeAcik(true);toast('Hızlı işlem kaydedildi');
  };
  const sablonIsleminiKaydet=(sablon,deger,guncelle=false)=>{
    const tip=sablon.tip==='gelir'?'gelir':'gider';
    if(!tutarGecerli(deger)){formHatasi();return;}
    if(!sablonKategorileri(tip).includes(sablon.kategori)){toast('Bu kategori artık kullanılamıyor. Hızlı işlemi düzenleyip kategori seç.', 'uyari');return;}
    const tutar=paraOku(deger);
    if(setIslemler(p=>[...p,{id:sablonKimligi(),tip,kategori:sablon.kategori,tutar,aciklama:sablon.aciklama||sablon.ad||sablon.kategori,tarih:yerelTarihAnahtari(),donemeDahil:true,kaynak:'hizli_sablon',sablonId:sablon.id}])===false)return;
    if(guncelle&&setHizliSablonlar(p=>p.map(s=>s.id===sablon.id?{...s,tutar}:s))===false)return;
    setSeciliSablon(null);toast(`${sablon.ad||sablon.kategori} ${tip==='gelir'?'geliri':'gideri'} eklendi`);
  };
  const sablondanGiderEkle=sablon=>{
    setHizliModal(false);
    setSeciliSablon({sablon,adim:'soru',tutar:String(sablon.tutar)});
  };
  const hizliSablonSil = (id) => {
    if(setHizliSablonlar(p => p.filter(s => s.id !== id))===false)return;
    toast("Şablon silindi", "uyari");
  };
  const hizliEkle = () => {
    if (hizliForm.tip === "taksit") {
      if (!hizliForm.taksitAd.trim() || !tutarGecerli(hizliForm.taksitAylik)
        || !tamSayiGecerli(hizliForm.taksitVade, 1, 1200)
        || !tamSayiGecerli(hizliForm.taksitOdenen || 0, 0, Number(hizliForm.taksitVade))) return formHatasi("Taksit adı, pozitif tutar ve geçerli vade/ödenen taksit sayısı gir.");
    } else if (!islemFormuGecerli(hizliForm, hizliKartMi)) return;
    if (hizliForm.tip === "taksit") {
      if (!hizliForm.taksitAd.trim() || !hizliForm.taksitAylik || !hizliForm.taksitVade) return;
      setTaksitler(p => [...p, {
        id: Date.now(),
        ad: hizliForm.taksitAd.trim(),
        aylikTutar: paraOku(hizliForm.taksitAylik),
        vade: parseInt(hizliForm.taksitVade),
        odenen: parseInt(hizliForm.taksitOdenen) || 0,
        kart: hizliForm.taksitKart.trim(),
      }]);
      setHizliModal(false);
      setHizliForm({ tip: "gider", kategori: "Market", tutar: "", aciklama: "", tarih: yerelTarihAnahtari(), donemeDahil: true, donemIci: "", provizyon: "", taksitAd: "", taksitAylik: "", taksitVade: "", taksitOdenen: "", taksitKart: "" });
      toast("Taksitli harcama eklendi");
      return;
    }
    if (hizliKartMi) {
      const di = paraOku(hizliForm.donemIci) || 0;
      const pr = paraOku(hizliForm.provizyon) || 0;
      if (di === 0 && pr === 0) return;
      setIslemler(p => [...p, { id: Date.now(), tip: "gider", kategori: hizliForm.kategori, tutar: paraTopla(di, pr), donemIci: di, provizyon: pr, kartHarcama: true, aciklama: hizliForm.aciklama, tarih: hizliForm.tarih, kartGunBaslangicToplam: paraTopla(di, pr), kartGunBaslangicTarih: hizliForm.tarih, kartGunlukDelta: 0, kartGunlukHareketler: {} }]);
    } else {
      if (!hizliForm.tutar) return;
      setIslemler(p => [...p, { ...hizliForm, id: Date.now(), tutar: paraOku(hizliForm.tutar), donemIci: undefined, provizyon: undefined }]);
    }
    setHizliModal(false);
    setHizliForm({ tip: "gider", kategori: "Market", tutar: "", aciklama: "", tarih: yerelTarihAnahtari(), donemeDahil: true, donemIci: "", provizyon: "" });
    toast("İşlem eklendi");
  }; // 0=bu dönem, -1=önceki, vb.
  // Offset'e göre dönem hesapla
  const gununTarihi = yerelTarihAnahtari();
  const secilenDonem = useMemo(() => {
    const gun = donemGun || 15;
    const b = new Date();
    let yil, ay;
    if (b.getDate() >= gun) { yil = b.getFullYear(); ay = b.getMonth(); }
    else { const bas = new Date(b.getFullYear(), b.getMonth() - 1, gun); yil = bas.getFullYear(); ay = bas.getMonth(); }
    ay += donemOffset;
    while (ay < 0) { ay += 12; yil--; }
    while (ay > 11) { ay -= 12; yil++; }
    const baslangic = new Date(yil, ay, gun);
    const bitis = new Date(yil, ay + 1, gun - 1, 23, 59, 59, 999);
    return { yil, ay, key: `${yil}-${ay}-${gun}`, etiket: donemLabel(yil, ay, gun), baslangic, bitis };
  }, [donemOffset, donemGun, gununTarihi]);
  const aktifDonem = secilenDonem.key;
  const donemEtiketi = secilenDonem.etiket;
  const kartOdemeKaydiBul = (kart) => {
    const kayitlar = (kartOdemeleri || []).filter(o => String(o.kartId) === String(kart.id));
    const kayit = kayitlar.find(o => o.odemeDonemKey === aktifDonem) || kayitlar.find(o => o.donemKey === aktifDonem);
    if (!kayit || !kayit.tarih) return kayit;
    if (donemOffset === 0) return kayit;
    return new Date(kayit.tarih) <= secilenDonem.bitis ? kayit : null;
  };
  const kartEkstreAnahtari = (kart) => kart?.ekstreDonemKey || kartEkstreDoneminiBul(kart,donemGun) || aktifDonem;
  const kartEkstreOdemeToplami = (kart) => (kartOdemeleri || [])
    .filter(o => String(o.kartId) === String(kart.id) && (o.ekstreDonemKey || o.donemKey) === kartEkstreAnahtari(kart))
    .reduce((s, o) => paraTopla(s, (Number(o.tutar) || 0)), 0);
  const kartOdemeEkle = () => {
    if (!kartOdemeModal || !yerelTarihOku(kartOdemeTarih)) return;
    const kart = kartOdemeModal;
    const odemeDonemKey = maasDonemKey(kartOdemeTarih, donemGun).key;
    if (kart.odemeDuzenle && kart.sonEkstreOdeme) {
      if (setKartOdemeleri(p => p.map(o => String(o.id) === String(kart.sonEkstreOdeme.id)
        ? { ...o, tarih: kartOdemeTarih, odemeDonemKey } : o)) === false) return;
      setKartOdemeModal(null);
      toast("Ödeme tarihi güncellendi");
      return;
    }
    const guncelKart = kartlar.find(k => String(k.id) === String(kart.id));
    if (!guncelKart) return;
    const tutar = Math.max(0, paraTopla(guncelKart.ekstre || 0, -kartEkstreOdemeToplami(guncelKart)));
    if (tutar <= 0) { toast("Bu ekstrenin ödenecek tutarı kalmadı", "uyari"); return; }
    if (!odemeOnayi(`${kart.isim} ekstresi`, tutar)) return;
    if (setKartOdemeleri(p => [...p, {
      id: sablonKimligi(), kartId: kart.id, kartAdi: kart.isim, tutar,
      tarih: kartOdemeTarih, odemeDonemKey, ekstreDonemKey: kartEkstreAnahtari(guncelKart),
      sonOdemeTarihi: guncelKart.sonOdemeTarihi || "",
    }]) === false) return;
    setKartOdemeModal(null);
    toast(`${kart.isim} ekstresi ödendi`);
  };
  const kartDonemTarihleriniBul = (kart) => {
    const arsiv = kart?.donemTarihleri?.[aktifDonem];
    const odemeKaydi = kartOdemeKaydiBul(kart);
    const tumOdemeKaydi = (kartOdemeleri || []).find(o =>
      String(o.kartId) === String(kart.id) && o.donemKey === aktifDonem
    );
    const seciliAralikta = (deger) => {
      const tarih = yerelTarihOku(deger);
      return tarih && tarih >= secilenDonem.baslangic && tarih <= secilenDonem.bitis;
    };
    const sonOdemeTarihi = (donemOffset === 0 ? kart?.sonOdemeTarihi : "")
      || arsiv?.sonOdemeTarihi
      || (seciliAralikta(kart?.sonOdemeTarihi) ? kart.sonOdemeTarihi : "")
      || tumOdemeKaydi?.sonOdemeTarihi
      || "";
    const hesapKesimTarihi = (donemOffset === 0 ? kart?.hesapKesimTarihi : "")
      || arsiv?.hesapKesimTarihi
      || (seciliAralikta(kart?.hesapKesimTarihi) ? kart.hesapKesimTarihi : "");
    if (!sonOdemeTarihi) return { hesapKesimTarihi, sonOdemeTarihi: "", sonOdeme: null };
    const tarih = yerelTarihOku(sonOdemeTarihi);
    return {
      hesapKesimTarihi,
      sonOdemeTarihi,
      sonOdeme: tarih ? {
        tarih,
        tarihAnahtari: sonOdemeTarihi,
        donemKey: `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, "0")}`,
        odendi: !!odemeKaydi,
        kesinTarih: true,
      } : null,
    };
  };
  const aylikVeriler = useMemo(() => {
    const buSira = secilenDonem.yil * 100 + secilenDonem.ay;
    const map = {};
    gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,'1900-01-01','9999-12-31').forEach(i => {
      const { yil, ay, key } = maasDonemKey(i.tarih, donemGun);
      if (!map[key]) map[key] = { ay: donemLabel(yil, ay, donemGun), gelir: 0, gider: 0, sira: yil * 100 + ay };
      if (i.tip === "gelir") map[key].gelir += i.tutar; else map[key].gider += i.tutar;
    });
    return Object.values(map).filter(d => d.sira <= buSira).map(d => ({ ...d, net: d.gelir - d.gider })).sort((a, b) => a.sira - b.sira).slice(-6);
  }, [islemler, odemeler, kartOdemeleri, donemGun, secilenDonem]);
  const buDonemIslemler = islemler.filter(i => maasDonemKey(i.tarih, donemGun).key === aktifDonem);
  const bakiyeOzeti = donemBakiyeHesapla({islemler,odemeler,kartlar,kartOdemeleri,bakiyeHesapModu,
    ilk:yerelTarihAnahtari(secilenDonem.baslangic),son:yerelTarihAnahtari(secilenDonem.bitis)});
  const buAyBirikim=bakiyeOzeti.birikim, buAyDevir=bakiyeOzeti.devir, buAyGelir=bakiyeOzeti.gelir;
  const buAyGider=bakiyeOzeti.harcama, toplamGider=buAyGider, sabitGider=0;
  const kalanBakiye=bakiyeOzeti.gercekBakiye;
  const harcamaOrani=donemHarcamaOrani(buAyGelir,toplamGider,buAyBirikim);
  const guncelDonemMi=donemOffset===0;
  const gosterilenBakiye=kalanBakiye, gosterilenRiskOrani=harcamaOrani;
  const donemOdemeleri = useMemo(() => (odemeler || [])
    .filter(o => maasDonemKey(o.tarih, donemGun).key === aktifDonem)
    .sort((a, b) => yerelTarihOku(a.tarih) - yerelTarihOku(b.tarih)), [odemeler, donemGun, aktifDonem]);
  const odemeDurumu = useMemo(() => {
    const bugun = yerelTarihOku(yerelTarihAnahtari());
    const yediGunSonra = takvimGunuEkle(bugun, 7);
    const bekleyen = donemOdemeleri.filter(o => !o.odendi && o.tarih);
    const gecikenler = bekleyen.filter(o => yerelTarihOku(o.tarih) < bugun);
    const yaklasanlar = bekleyen.filter(o => {
      const tarih = yerelTarihOku(o.tarih);
      return tarih >= bugun && tarih <= yediGunSonra;
    });
    return {
      gecikenAdet: gecikenler.length,
      gecikenToplam: gecikenler.reduce((s, o) => paraTopla(s, (Number(o.tutar) || 0)), 0),
      yaklasanAdet: yaklasanlar.length,
      yaklasanToplam: yaklasanlar.reduce((s, o) => paraTopla(s, (Number(o.tutar) || 0)), 0),
    };
  }, [donemOdemeleri]);

  // Ay sonu tahmini gider (sadece içinde bulunulan dönem için)
  const tahminiAySonu = useMemo(() => {
    if (donemOffset !== 0) return null;
    const gun = donemGun || 15;
    const bugun = new Date();
    // Dönem başlangıç tarihi
    let basYil = secilenDonem.yil, basAy = secilenDonem.ay;
    const donemBas = new Date(basYil, basAy, gun);
    // Dönem bitiş: bir sonraki ayın (gun-1)'i
    const donemBit = new Date(basYil, basAy + 1, gun - 1);
    const sonuc = donemSonuGiderTahmini({ islemler, odemeler, kartlar, kartOdemeleri,
      baslangic:donemBas, bitis:donemBit, bakiyeHesapModu, bugun });
    return sonuc.sabitTaban > 0 || sonuc.degiskenGider > 0 ? sonuc : null;
  }, [donemOffset, donemGun, secilenDonem, islemler, odemeler, kartlar, kartOdemeleri, bakiyeHesapModu]);

  // Günlük pay: gerçek nakit çıkışı ve bekleyen borç için ayrılan pay birlikte dikkate alınır.
  const gunlukHarcamaLimiti = useMemo(() => {
    if (donemOffset !== 0) return null;
    const gun = donemGun || 15;
    const bugun = new Date(); bugun.setHours(0, 0, 0, 0);
    let basYil = secilenDonem.yil, basAy = secilenDonem.ay;
    const donemBas = new Date(basYil, basAy, gun);
    const donemBit = new Date(basYil, basAy + 1, gun - 1);
    const toplamGun = takvimGunFarki(donemBit, donemBas) + 1;
    const gecenGun = Math.min(toplamGun, Math.max(1, takvimGunFarki(bugun, donemBas) + 1));
    const kalanGun = Math.max(1, toplamGun - gecenGun + 1);
    const yaklasanToplam = (odemeler || []).filter(o => !o.odendi && o.tarih).reduce((s, o) => {
      const t = yerelTarihOku(o.tarih);
      return (t >= bugun && t <= donemBit) ? paraTopla(s, o.tutar) : s;
    }, 0);
    const hedef = tasarrufPayiHesapla({gelir:paraTopla(buAyGelir,buAyDevir),gider:paraTopla(bakiyeOzeti.nakitCikis,-buAyBirikim),birikim:buAyBirikim,hedef:tasarrufHedefi,kalanGun,ekBekleyen:bakiyeOzeti.ayrilan});
    return { ...hedef, kalanGun, yaklasanToplam, kullanilabilir:Math.max(0,hedef.pay) };
  }, [donemOffset, donemGun, secilenDonem, odemeler, kalanBakiye, buDonemIslemler, buAyGelir, toplamGider, buAyBirikim, tasarrufHedefi, kartlar, kartOdemeleri, bakiyeHesapModu]);

  // Önceki döneme göre kıyas (Gelir / Harcama / Birikim)
  const oncekiDonemKarsilastirma = useMemo(() => {
    const gun = donemGun || 15;
    let oYil = secilenDonem.yil, oAy = secilenDonem.ay - 1;
    while (oAy < 0) { oAy += 12; oYil--; }
    const oKey = `${oYil}-${oAy}-${gun}`;
    const oIslem = gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,'1900-01-01','9999-12-31').filter(i => maasDonemKey(i.tarih, gun).key === oKey);
    const oGelir = oIslem.filter(i => i.tip === "gelir" && !devirIslemiMi(i) && i.kategori !== "Birikim" && !(i.kategori === "Kira Geliri" && i.donemeDahil === false)).reduce((s, i) => paraTopla(s, i.tutar), 0);
    const oGider = oIslem.filter(i => i.tip === "gider").reduce((s, i) => paraTopla(s, i.tutar), 0);
    const oBirikim = islemler.filter(i => maasDonemKey(i.tarih,gun).key===oKey && i.tip === "gelir" && i.kategori === "Birikim").reduce((s, i) => paraTopla(s, i.tutar), 0);
    const yuzde = (yeni, eski) => eski > 0 ? ((yeni - eski) / eski) * 100 : (yeni > 0 ? 100 : 0);
    return {
      gelir: { deger: buAyGelir, yuzde: yuzde(buAyGelir, oGelir), varMi: oGelir > 0 || oGider > 0 || oBirikim > 0 },
      gider: { deger: toplamGider, yuzde: yuzde(toplamGider, oGider), varMi: oGelir > 0 || oGider > 0 || oBirikim > 0 },
      birikim: { deger: buAyBirikim, yuzde: yuzde(buAyBirikim, oBirikim), varMi: oGelir > 0 || oGider > 0 || oBirikim > 0 },
    };
  }, [islemler, odemeler, kartOdemeleri, secilenDonem, donemGun, buAyGelir, toplamGider, buAyBirikim]);

  // Bugün harcanan: normal işlemler tam tutarıyla, kredi kartı güncellemeleri sadece bugün eklenen fark (delta) ile sayılır
  const ozettenOdendiYap = (odeme) => {
    if(odeme.odendi || !odemeOnayi(odeme.aciklama||odeme.kategori,odeme.tutar))return;
    const mevcut = (islemler || []).find(i =>
      (odeme.islemId != null && String(i.id) === String(odeme.islemId)) ||
      (i.odemeId != null && String(i.odemeId) === String(odeme.id))
    );
    const islemId = mevcut?.id ?? odeme.islemId ?? (Date.now() + 1);
    setOdemeler(p => p.map(x => x.id === odeme.id ? { ...x, odendi: true, odendiTarih: new Date().toISOString(), islemId } : x));
    setIslemler(p => p.some(i => String(i.id) === String(islemId))
      ? p.map(i => String(i.id) === String(islemId) ? { ...i, kaynak: "odeme", odemeId: odeme.id } : i)
      : [...p, { id: islemId, tip: "gider", kategori: odeme.kategori || "Fatura", tutar: Number(odeme.tutar) || 0, aciklama: odeme.aciklama || odeme.kategori || "Fatura", tarih: odeme.tarih, kaynak: "odeme", odemeId: odeme.id }]
    );
    toast("Ödendi olarak işaretlendi");
  };
  const katDagilim = useMemo(() => {
    const map = {};
    islemler
      .filter(i => i.tip === "gider" && maasDonemKey(i.tarih, donemGun).key === aktifDonem)
      .forEach(i => { map[i.kategori] = paraTopla(map[i.kategori] || 0, i.tutar); });
    return Object.entries(map).sort((a, b) => b[1] - a[1]).slice(0, 6);
  }, [islemler, donemGun, aktifDonem]);

  // En çok harcanan 6 kategori için aylık (dönem) trend
  const kategoriTrendVeri = useMemo(() => {
    const buSira = secilenDonem.yil * 100 + secilenDonem.ay;
    const top6 = katDagilim.map(([kat]) => kat);
    const donemMap = {};
    islemler.filter(i => i.tip === "gider" && top6.includes(i.kategori)).forEach(i => {
      const { yil, ay, key } = maasDonemKey(i.tarih, donemGun);
      if (!donemMap[key]) donemMap[key] = { ay: donemLabel(yil, ay, donemGun), sira: yil * 100 + ay };
      donemMap[key][i.kategori] = paraTopla(donemMap[key][i.kategori] || 0, i.tutar);
    });
    const sirali = Object.values(donemMap).filter(d => d.sira <= buSira).sort((a, b) => a.sira - b.sira).slice(-6);
    // eksik kategorileri 0 yap
    sirali.forEach(d => top6.forEach(k => { if (d[k] === undefined) d[k] = 0; }));
    return { veri: sirali, kategoriler: top6 };
  }, [islemler, katDagilim, donemGun, secilenDonem]);
  const tt = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 14px" }}><div style={{ color: "var(--text3)", fontSize: 12, marginBottom: 6, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{label}</div>{payload.map(p => <div key={p.name} style={{ color: p.color, fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{p.name}: ₺{fmt(p.value)}</div>)}</div>;
  };
  return (
    <div className="home-overview">
      {/* Takvim simgeli kompakt dönem seçici; önceki/sonraki gezinme korunur. */}
      <div className="home-period-wrap">
        <div className="home-period home-period-pill">
          <button aria-label="Önceki dönem" onClick={() => setDonemOffset(o => o - 1)}>‹</button>
          <label className="home-period-label">
            <Icon name="calendar" size={17} />
            <span className="home-period-text">{donemEtiketi}</span>
            <span className="home-period-chevron" aria-hidden="true">⌄</span>
            <select className="home-period-select" aria-label="Maaş dönemini seç" value={donemOffset} onChange={e => setDonemOffset(Number(e.target.value))}>
              {Array.from({ length: Math.max(13, Math.abs(donemOffset) + 1) }, (_, n) => {
                const tarih = new Date(secilenDonem.yil, secilenDonem.ay - donemOffset - n, 1);
                return <option key={n} value={-n}>{donemLabel(tarih.getFullYear(), tarih.getMonth(), donemGun)} · {tarih.getFullYear()}{n === 0 ? " · Güncel" : ""}</option>;
              })}
            </select>
          </label>
          <button aria-label="Sonraki dönem" disabled={donemOffset === 0} onClick={() => setDonemOffset(o => Math.min(o + 1, 0))}>›</button>
        </div>
        {donemOffset !== 0 && <div className="home-period-history"><span><span className="inline-icon"><Icon name="shield" size={14}/></span>Tamamlanan dönem · dönem sonu görünümü</span><button onClick={() => setDonemOffset(0)}>Bugüne dön</button></div>}
      </div>

      {/* Bakiye ve bütçe kullanımı aynı ana kartta. */}
      {(() => {
        const veriYok = islemler.length === 0 && odemeler.length === 0 && kartlar.length === 0;
        const durum = veriYok || buAyGelir <= 0 ? "bos" : gosterilenBakiye < 0 ? "kotu" : gosterilenRiskOrani < 50 ? "iyi" : gosterilenRiskOrani < 80 ? "orta" : "kotu";
        return <section className="home-balance finance-hero" data-status={durum}>
          <div className="home-balance-heading"><span>BAKİYE</span><span className="home-balance-badge"><Icon name={durum==='bos'?'wallet':durum==='iyi'?'shield':durum==='orta'?'warning':'alert'} size={14}/>{veriYok?'Henüz veri yok':buAyGelir<=0?'Gelir kaydı yok':durum==='iyi'?'Güvende':durum==='orta'?'Dikkat':'Riskli'}</span></div>
          <div className="home-balance-value"><Money value={Math.abs(gosterilenBakiye)} prefix={gosterilenBakiye<0?'−₺':'₺'} size={34}/></div>
          {!veriYok&&<div className="day-totals" style={{marginTop:12}}><div><span>Ödemeler sonrası kalan</span><strong>{fmtTL(bakiyeOzeti.odemelerSonrasi)}</strong></div><div><span>Bekleyen ödemeler toplamı</span><strong>{fmtTL(bakiyeOzeti.ayrilan)}</strong></div></div>}
          {!veriYok && <div className={`hero-budget hero-budget--${grafikTipi}`}>
            {grafikTipi==='ibre'&&<h3 className="needle-panel-title">Dönem bütçem</h3>}
            <HarcamaGauge oran={buAyGelir>0?gosterilenRiskOrani:null} grafikTipi={grafikTipi}/>
            {grafikTipi!=='ibre'&&<div className="hero-budget-copy"><h3>Dönem bütçem</h3><div className="hero-budget-limit">{buAyGelir>0?`Kalan oran %${Math.round(Math.max(0,100-gosterilenRiskOrani))}`:'Oran için gelir kaydı gerekli'}</div></div>}
          </div>}

          {veriYok&&<div className="ilk-kullanim"><button onClick={()=>{setHizliForm(f=>({...f,tip:'gelir',kategori:'Maaş'}));setHizliModal(true);}}><Icon name="plus" size={16}/>İlk gelirini ekle</button></div>}
        </section>;
      })()}

      {/* GELİR / HARCAMA / BİRİKİM — önceki döneme göre kıyas */}
      <div className="home-summary" style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 12 }}>
        {[
          { etiket: "Gelir", ikon: "wallet", renk: "var(--income-text)", veri: oncekiDonemKarsilastirma.gelir, tersRenk: false },
          { etiket: "Harcama", ikon: "card", renk: "var(--expense-text)", veri: oncekiDonemKarsilastirma.gider, tersRenk: true },
          { etiket: "Birikim", ikon: "coins", renk: "var(--saving-text)", veri: oncekiDonemKarsilastirma.birikim, tersRenk: false },
        ].map(k => {
          const artti = k.veri.yuzde >= 0;
          // Harcama'da artış kötü (kırmızı ok), diğerlerinde artış iyi (yeşil ok)
          const okIyi = k.tersRenk ? !artti : artti;
          const okRenk = okIyi ? "#10b981" : "#ef4444";
          return (
            <div className="home-summary-card" key={k.etiket} style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 16, boxShadow: "var(--card-shadow)", padding: "12px 10px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                <span style={{ color: k.renk, fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{k.etiket}</span>
                <span style={{ width: 22, height: 22, borderRadius: 7, background: "var(--card2)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, flexShrink: 0 }}><Icon name={k.ikon} size={15} /></span>
              </div>
              <div className="home-summary-value" style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 5, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}><Money value={k.veri.deger} size={15}/></div>
              {k.veri.varMi ? (
                <div style={{ display: "flex", alignItems: "center", gap: 3 }}>
                  <span style={{ color: okRenk, fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{artti ? "↗" : "↘"} %{Math.abs(k.veri.yuzde).toFixed(1)}</span>
                </div>
              ) : (
                null
              )}
              <div className="home-summary-caption" style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 2 }}>{k.veri.varMi ? "Önceki dönem" : "Geçmiş yok"}</div>
            </div>
          );
        })}
      </div>
      <div className="home-quick-actions"><button onClick={()=>{setHizliForm(f=>({...f,tip:'gider',kategori:'Market'}));setHizliModal(true);}}><Icon name="arrowDown" size={18}/>Gider ekle</button><button onClick={()=>{setHizliForm(f=>({...f,tip:'gelir',kategori:'Maaş'}));setHizliModal(true);}}><Icon name="arrowUp" size={18}/>Gelir ekle</button></div>
      <section className="tools-card quick-template-section">
        <button className="quick-template-toggle" aria-expanded={hizliListeAcik} aria-controls="hizli-islem-listesi" onClick={()=>setHizliListeAcik(p=>!p)}><span><Icon name="repeat" size={18}/>Hızlı işlemler <small>{hizliSablonlar.length}</small></span><span aria-hidden="true">{hizliListeAcik?'⌃':'⌄'}</span></button>
        {hizliListeAcik&&<div id="hizli-islem-listesi"><p className="tools-note">Kaydettiğin işlemleri bugünün tarihiyle ekleyebilirsin.</p>{hizliSablonlar.length===0&&<p className="tools-note quick-template-empty">Henüz hızlı işlem yok. Örneğin “Nakit çekim” için kategori ve tutar kaydet.</p>}
          {hizliSablonlar.map(s=><article className="quick-template-row" key={s.id}><div className="quick-template-name"><strong>{s.ad||s.aciklama||s.kategori}</strong><span>{s.tip==='gelir'?'Gelir':'Gider'} · {s.kategori}</span><b style={{color:s.tip==='gelir'?'var(--income-text)':'var(--expense-text)'}}>₺{fmt(s.tutar)}</b></div><div className="quick-template-actions"><button className="tools-button quick-template-add" onClick={()=>sablondanGiderEkle(s)}>Ekle</button><button className="tools-button" aria-label={`${s.ad||s.kategori} hızlı işlemini düzenle`} onClick={()=>sablonFormunuAc(s)}><Icon name="edit" size={16}/></button><button className="tools-button" aria-label={`${s.ad||s.kategori} hızlı işlemini sil`} onClick={()=>hizliSablonSil(s.id)}><Icon name="trash" size={16}/></button></div></article>)}
          <Btn onClick={()=>sablonFormunuAc(null)} style={{width:'100%',marginTop:12}}>+ Hızlı işlem oluştur</Btn>
        </div>}
      </section>
      {sablonModal&&<Modal title={sablonForm.id!=null?'Hızlı işlemi düzenle':'Hızlı işlem oluştur'} onClose={()=>setSablonModal(false)}>
        <Input label="İşlem adı" value={sablonForm.ad} onChange={e=>setSablonForm(f=>({...f,ad:e.target.value}))} placeholder="Örn: Nakit çekim"/>
        <Select label="İşlem türü" value={sablonForm.tip} onChange={e=>{const tip=e.target.value;setSablonForm(f=>({...f,tip,kategori:sablonKategorileri(tip)[0]||''}));}}><option value="gider">Gider</option><option value="gelir">Gelir</option></Select>
        <Select label="Hızlı işlem kategorisi" value={sablonForm.kategori} onChange={e=>setSablonForm(f=>({...f,kategori:e.target.value}))}>{sablonKategorileri(sablonForm.tip).map(k=><option key={k} value={k}>{k}</option>)}</Select>
        <Input label="Kayıtlı tutar (₺)" inputMode="decimal" value={sablonForm.tutar} onChange={e=>setSablonForm(f=>({...f,tutar:e.target.value}))} placeholder="1000,00"/>
        <p className="tools-note">Bu adım yalnızca hızlı işlemi kaydeder; gelir veya gider oluşturmaz.</p><Btn onClick={sablonKaydet} style={{width:'100%',marginTop:12}}>Hızlı işlemi kaydet</Btn>
      </Modal>}
      {seciliSablon&&<Modal title={seciliSablon.adim==='soru'?'Hızlı işlem ekle':'Tutarı güncelle'} onClose={()=>setSeciliSablon(null)}>
        <div className="quick-template-preview"><strong>{seciliSablon.sablon.ad||seciliSablon.sablon.kategori}</strong><span>{seciliSablon.sablon.kategori} · ₺{fmt(seciliSablon.sablon.tutar)}</span></div>
        {seciliSablon.adim==='soru'?<><p className="quick-template-question">Tutarı güncellemek istiyor musun?</p><div className="tools-grid"><Btn onClick={()=>setSeciliSablon(p=>({...p,adim:'tutar'}))}>Evet</Btn><Btn variant="secondary" onClick={()=>sablonIsleminiKaydet(seciliSablon.sablon,seciliSablon.sablon.tutar)}>Hayır</Btn></div><button className="tools-button quick-template-cancel" onClick={()=>setSeciliSablon(null)}>İptal</button></>:<>
          <Input label="Yeni işlem tutarı (₺)" inputMode="decimal" value={seciliSablon.tutar} onChange={e=>setSeciliSablon(p=>({...p,tutar:e.target.value}))}/>
          <p className="tools-note">Yeni tutar bu işlemde kullanılır ve hızlı işlemin kayıtlı tutarı güncellenir.</p><Btn onClick={()=>sablonIsleminiKaydet(seciliSablon.sablon,seciliSablon.tutar,true)} style={{width:'100%'}}>İşlemi ekle</Btn>
        </>}
      </Modal>}

      {/* Sabit gider uyarısı */}
      {sabitGider > 0 && (
        <div style={{ background:"#ef444411", border:"1px solid #ef444433", borderRadius:10, padding:"8px 14px", marginBottom:10, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
          <span style={{ color: "var(--expense-text)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}><span className="inline-icon"><Icon name="repeat" size={16}/></span>Sabit Giderler (Aktif)</span>
          <span style={{ color: "var(--expense-text)", fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600, fontSize:13 }}>₺{fmt(sabitGider)}</span>
        </div>
      )}

      {donemOffset === 0 && (odemeDurumu.gecikenAdet > 0 || odemeDurumu.yaklasanAdet > 0) && (
        <div style={{ background: "linear-gradient(135deg,var(--card),var(--card2))", border: "1px solid var(--border)", borderRadius: 18, padding: "13px 14px", marginBottom: 12, display: "flex", alignItems: "center", gap: 12, boxShadow: "var(--card-shadow)" }}>
          <div style={{ width: 40, height: 40, borderRadius: 13, display: "grid", placeItems: "center", background: odemeDurumu.gecikenAdet > 0 ? "#ef444422" : "var(--accent-soft)", color: odemeDurumu.gecikenAdet > 0 ? "#ef4444" : "var(--accent)", flexShrink: 0 }}><Icon name={odemeDurumu.gecikenAdet > 0 ? "receipt" : "chart"} size={19} /></div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ color: "var(--text)", fontSize: 12, fontWeight: 700, marginBottom: 3 }}>Bütçe Özeti</div>
            <div style={{ color: "var(--text4)", fontSize: 11, lineHeight: 1.45 }}>
              {odemeDurumu.gecikenAdet > 0
                ? `${odemeDurumu.gecikenAdet} gecikmiş ödeme bulunuyor: ₺${fmt(odemeDurumu.gecikenToplam)}.`
                : `Önümüzdeki 7 günde ${odemeDurumu.yaklasanAdet} ödeme var: ₺${fmt(odemeDurumu.yaklasanToplam)}.`}
            </div>
          </div>
        </div>
      )}

      {/* AY SONU TAHMİNİ — ayrı kart */}
      {tahminiAySonu && (() => {
        const tahminOran = buAyGelir > 0 ? (tahminiAySonu.tahmin / buAyGelir) * 100 : 100;
        const rozetDurum = tahminOran < 50 ? "iyi" : tahminOran < 80 ? "orta" : "kotu";
        const rozetMetin = rozetDurum === "iyi" ? "Güvende" : rozetDurum === "orta" ? "Dikkat" : "Riskli";
        const rozetRenk = rozetDurum === "iyi" ? "#10b981" : rozetDurum === "orta" ? "#f59e0b" : "#ef4444";
        return (
          <div className="home-panel home-forecast" style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "14px 16px", marginBottom: 12, display: "flex", alignItems: "center", gap: 13 }}>
            <div style={{ width: 42, height: 42, borderRadius: 12, background: rozetRenk + "22", border: `1px solid ${rozetRenk}44`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 19, flexShrink: 0 }}><Icon name="chart" size={20} /></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 3 }}>Ay Sonu Tahmini Gider</div>
              <div style={{ display: "flex", alignItems: "baseline", gap: 6 }}>
                <span style={{ color: "var(--text)", fontSize: 17, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(tahminiAySonu.tahmin)}</span>
              </div>
              <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 3 }}>{tahminiAySonu.gecenGun}/{tahminiAySonu.toplamGun} Gün · Değişken Günlük Ort. ₺{fmt(tahminiAySonu.gunlukOrt)}</div>
              <div className="tools-note" style={{marginTop:5}}>Tek seferlik giderler ve kart açılışı: ₺{fmt(tahminiAySonu.sabitTaban)} · Bu tutar bir kez sayılır.</div>
            </div>
            <span style={{ background: rozetRenk + "22", border: `1px solid ${rozetRenk}55`, color: rozetRenk, borderRadius: 99, padding: "5px 11px", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, flexShrink: 0 }}>{rozetMetin}</span>
          </div>
        );
      })()}

      {/* GÜNLÜK HARCAMA LİMİTİ */}
      {gunlukHarcamaLimiti && buAyGelir > 0 && (
        <div className="home-panel home-daily-limit" style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "14px 16px", marginBottom: 12, display: "flex", alignItems: "center", gap: 13 }}>
          <div style={{ width: 42, height: 42, borderRadius: 12, background: "#f59e0b22", border: "1px solid #f59e0b44", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 19, flexShrink: 0 }}><Icon name="wallet" size={20} /></div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 3 }}>Tasarruf Sonrası Günlük Pay</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 5 }}>
              <span style={{ color: "var(--text)", fontSize: 17, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(gunlukHarcamaLimiti.limit)}</span>
              <span style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>/ gün</span>
            </div>
            <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 3 }}>
              %{tasarrufHedefi} hedef · ₺{fmt(gunlukHarcamaLimiti.hedefTutar)} ayır · {gunlukHarcamaLimiti.kalanGun} gün kaldı{gunlukHarcamaLimiti.pay < 0 ? ` · Hedef için ₺${fmt(-gunlukHarcamaLimiti.pay)} eksik` : ""}{gunlukHarcamaLimiti.yaklasanToplam > 0 ? ` · ₺${fmt(gunlukHarcamaLimiti.yaklasanToplam)} yaklaşan ödeme için pay ayrıldı` : ""}
            </div>
          </div>
        </div>
      )}

      {/* YAKLAŞAN ÖDEMELER */}
      {(() => {
        const bugun = new Date(); bugun.setHours(0, 0, 0, 0);
        const gosterilecek = (odemeler || []).filter(o => {
          const tarih = yerelTarihOku(o.tarih);
          return !o.odendi && tarih && tarih >= bugun && takvimGunFarki(tarih, bugun) <= 3;
        }).sort((a,b) => yerelTarihOku(a.tarih) - yerelTarihOku(b.tarih));
        if (gosterilecek.length === 0) return null;
        const gunFark = (t) => takvimGunFarki(t, bugun);
        return (
          <div className="home-panel home-payments" style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "14px 16px", marginBottom: 12 }}>
            <div className="upcoming-heading" style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 12 }}><Icon name="calendar" size={18}/>Yaklaşan Ödemeler</div>
            <p className="tools-note upcoming-note">Bugün ve önümüzdeki 3 gün · maaş döneminden bağımsız</p>
            {gosterilecek.map(o => {
              const gf = gunFark(o.tarih);
              return <article className="upcoming-item" key={o.id}>
                <div className="upcoming-description"><div><strong>{o.aciklama||o.kategori}</strong><span className={gf===0?'due-today':''}>{gf===0?'Bugün ödenecek':`${gf} gün kaldı · ${o.tarih.split('-').reverse().join('.')}`}</span></div><strong>{fmtTL(o.tutar)}</strong></div>
                <Btn onClick={()=>ozettenOdendiYap(o)} style={{width:'100%'}}>Ödendi olarak işaretle</Btn>
              </article>;
            })}
          </div>
        );
      })()}

      {/* Birikim mini kartı */}
      {buAyBirikim > 0 && (
        <div style={{ background: "var(--card)", border: "1px solid #10b98144", borderRadius: 14, padding: "12px 18px", marginBottom: 12, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <span style={{ fontSize: 18 }}><Icon name="coins" size={18}/></span>
            <span style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Dönem Birikimi (net'e dahil değil)</span>
          </div>
          <span style={{ color: "var(--income-text)", fontSize: 15, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(buAyBirikim)}</span>
        </div>
      )}
      {/* Kredi Kartı Özeti */}
      {kartlar && kartlar.length > 0 && (() => {
        const kartOzetleri = turkceAdaGoreSirala(kartlar).map(k => {
          const kartIslemleri = buDonemIslemler.filter(i => i.tip === "gider" && i.kategori === k.isim);
          const prov = kartIslemleri.reduce((s, i) => paraTopla(s, (Number(i.provizyon) || 0)), 0);
          const donem = kartIslemleri.reduce((s, i) => paraTopla(s, (Number(i.donemIci) || 0)), 0);
          const ekstreBuDonemde = kartEkstreDoneminiBul(k,donemGun) === aktifDonem;
          const kalanEkstre = Math.max(0,paraTopla(k.ekstre || 0,-kartEkstreOdemeToplami(k)));
          const ekstre = ekstreBuDonemde ? kalanEkstre : 0;
          const digerEkstre = ekstreBuDonemde ? 0 : kalanEkstre;
          const guncelBorc = donem + prov;
          const sonEkstreOdeme = (kartOdemeleri || []).filter(o => String(o.kartId) === String(k.id) && (o.ekstreDonemKey || o.donemKey) === kartEkstreAnahtari(k)).sort((a,b) => String(b.tarih).localeCompare(String(a.tarih)))[0] || null;
          return { ...k, ...kartDonemTarihleriniBul(k), ekstreToplam: Number(k.ekstre) || 0, ekstreOdenen: kartEkstreOdemeToplami(k), ekstre, digerEkstre, ekstreBuDonemde, prov, donem, guncelBorc, sonEkstreOdeme, islemler: kartIslemleri };
        });
        const toplamEkstre = kartOzetleri.reduce((s, k) => paraTopla(s, k.ekstre), 0);
        const toplamGuncelBorc = kartOzetleri.reduce((s, k) => paraTopla(s, k.guncelBorc), 0);

        return (
          <div className="kredi-karti-alani home-credit-panel" style={{ background: "var(--card)", border: "1px solid #A74E3233", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "14px 16px", marginBottom: 12 }}>
            <div className="credit-section-heading"><span className="inline-icon"><Icon name="card" size={16}/></span>Kredi Kartları</div>

            {/* Tüm kartların toplu özeti */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 8, marginBottom: 12 }}>
              <div style={{ padding: "11px 12px", borderRadius: 12, background: "var(--card2)", border: "1px solid var(--border)" }}>
                <div style={{ color: "var(--text5)", fontSize: 11, fontWeight: 700, textTransform: "none", letterSpacing: "0.05em", marginBottom: 4 }}>Ekstre toplamı</div>
                <div style={{ color: "var(--text)", fontSize: 16, fontWeight: 700 }}>₺{fmt(toplamEkstre)}</div>
                <div style={{ color: "var(--text5)", fontSize: 11, marginTop: 2 }}>Tüm kartların kesilmiş ekstreleri</div>
              </div>
              <div style={{ padding: "11px 12px", borderRadius: 12, background: "var(--card2)", border: "1px solid var(--border)" }}>
                <div style={{ color: "var(--text5)", fontSize: 11, fontWeight: 700, textTransform: "none", letterSpacing: "0.05em", marginBottom: 4 }}>Dönem içi ve provizyon</div>
                <div style={{ color: "var(--text)", fontSize: 16, fontWeight: 700 }}>₺{fmt(toplamGuncelBorc)}</div>
                <div style={{ color: "var(--text5)", fontSize: 11, marginTop: 2 }}>Dönem içi + provizyon</div>
              </div>
            </div>

            <div style={{ color: "var(--text5)", fontSize: 11, lineHeight: 1.45, marginBottom: 10 }}>
              Ekstre kesilmiş dönem borcudur; toplam borç kalan ekstre + dönem içi + provizyondur. Parantezdeki fark önceki maaş döneminin son kayıtlı toplamına göredir. Kart seçimi yalnız ayrılan bütçeyi ve tahmini gideri etkiler.
            </div>

            {kartOzetleri.map(k => {
              const odemeKaydi = kartOdemeKaydiBul(k);
              const sonOdeme = k.sonOdeme;
              const oncekiDonem=maasDonemiBul(yerelTarihAnahtari(takvimGunuEkle(secilenDonem.baslangic,-1)),donemGun);
              const oncekiBorc=kartBorcOzetleri[oncekiDonem.key]?.[String(k.id)];
              const toplamBorc=paraTopla(k.ekstre,k.digerEkstre,k.donem,k.prov);
              return (
                <article className="home-credit-card credit-tile" key={k.id}>
                  <div className="credit-tile-heading"><div><KartMarkaRozeti isim={k.isim}/><h3>{k.isim}</h3></div><span className="credit-type">KREDİ KARTI</span></div>
                  <div className="credit-total"><span>Toplam kart borcu · provizyon dahil</span><div className="credit-total-row"><Money value={toplamBorc} size={27}/><KartBorcDegisimi toplam={toplamBorc} onceki={oncekiBorc}/></div></div>
                  <div className="credit-buckets">
                    {[["Ekstre borcu",k.ekstre],["Dönem içi",k.donem],["Provizyon",k.prov]].map(([label,value])=><div key={label}><span>{label}</span><Money value={value} size={14}/></div>)}
                  </div>
                  {k.digerEkstre>0&&<p className="tools-note">Diğer döneme ait ödenmemiş ekstre: <strong>{fmtTL(k.digerEkstre)}</strong></p>}
                  {!k.ekstreBuDonemde&&<p className="tools-note">Seçili dönem için kayıtlı ekstre yok.</p>}
                  {k.sonEkstreOdeme&&<p className="tools-note">{k.ekstreBuDonemde?"Ekstre ödendi:":"Diğer dönemin ekstre ödemesi:"} {k.sonEkstreOdeme.tarih.split('-').reverse().join('.')}</p>}
                  <div className="credit-dates"><div><span>Hesap Kesim Tarihi</span><strong>{k.hesapKesimTarihi?k.hesapKesimTarihi.split('-').reverse().join('.'):'Henüz girilmedi'}</strong></div><div><span>Son Ödeme Tarihi</span><strong>{sonOdeme?sonOdeme.tarihAnahtari.split('-').reverse().join('.'):'Henüz girilmedi'}</strong></div></div>
                  <p className="tools-note">{k.bakiyedenDussun===false?'Dönem içi ve provizyon için bütçeden pay ayrılmıyor.':'Dönem içi ve provizyon için bütçeden pay ayrılıyor.'}</p>
                  <div className="credit-actions">{donemOffset===0&&<Btn onClick={()=>{
                    setKartDuzenle(k);
                    setKartDuzForm({donemIci:k.donem>0?String(k.donem):'',provizyon:k.prov>0?String(k.prov):'',ekstre:paraTopla(k.ekstre,k.digerEkstre)>0?String(paraTopla(k.ekstre,k.digerEkstre)):'',bakiyedenDussun:k.bakiyedenDussun!==false});
                  }}>Tutarları güncelle</Btn>}<button className="tools-button" disabled={paraTopla(k.ekstre,k.digerEkstre)<=0} onClick={()=>{setKartOdemeModal({...k,ekstre:paraTopla(k.ekstre,k.digerEkstre),odemeDuzenle:false});setKartOdemeTarih(yerelTarihAnahtari());}}>Ödendi olarak işaretle</button>{k.sonEkstreOdeme&&<button className="tools-button" onClick={()=>{setKartOdemeModal({...k,odemeDuzenle:true});setKartOdemeTarih(k.sonEkstreOdeme.tarih);}}>Ödeme tarihini düzenle</button>}</div>
                </article>
              );
            })}
          </div>
        );
      })()}
      {kartOdemeModal && (
        <Modal title={`${kartOdemeModal.isim} Ekstre Ödemesi`} onClose={() => setKartOdemeModal(null)}>
          <div style={{ color: "var(--text4)", fontSize: 11, lineHeight: 1.5, marginBottom: 12 }}>Ekstreyi ödendi olarak işaretle. Seçtiğin ödeme tarihi, ödemenin düşeceği maaş dönemini belirler.</div>
          <div style={{ padding: "10px 12px", background: "var(--card2)", borderRadius: 10, marginBottom: 12 }}>
            <div style={{ color: "var(--text5)", fontSize: 11, fontWeight: 700 }}>{kartOdemeModal.odemeDuzenle ? "Kaydedilmiş ödeme" : "Ödenecek ekstre"}</div>
            <div style={{ color: "var(--expense-text)", fontSize: 17, fontWeight: 700, marginTop: 3 }}>₺{fmt(kartOdemeModal.odemeDuzenle ? kartOdemeModal.sonEkstreOdeme.tutar : kartOdemeModal.ekstre)}</div>
          </div>
          <Input label="Ödeme Tarihi" type="date" value={kartOdemeTarih} onChange={e => setKartOdemeTarih(e.target.value)} />
          <div style={{ color: "var(--text5)", fontSize: 11, marginTop: -5, marginBottom: 8 }}>Bu tarih hangi maaş dönemine aitse ekstre ödemesi o dönemin giderine eklenir.</div>
          <Btn onClick={kartOdemeEkle} style={{ width: "100%", padding: 12 }}>{kartOdemeModal.odemeDuzenle ? "Ödeme Tarihini Kaydet" : "Ödendi Olarak İşaretle"}</Btn>
        </Modal>
      )}

      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 16 }}>
        <div style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 14 }}><span className="inline-icon"><Icon name="chart" size={16}/></span>Harcama Trendin</div>
        {aylikVeriler.length === 0 ? (
          <div style={{ textAlign: "center", color: "var(--text5)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", padding: "20px 0" }}>Henüz veri yok</div>
        ) : (
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={aylikVeriler} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
              <XAxis dataKey="ay" tick={{ fill: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }} axisLine={false} tickLine={false} tickFormatter={v => `${(v / 1000).toFixed(0)}K`} width={36} />
              <Tooltip content={tt} />
              <Line type="monotone" dataKey="gider" name="Gider" stroke="#ef4444" strokeWidth={2.5} dot={{ r: 3, fill: "#ef4444" }} activeDot={{ r: 5 }} />
              <Line type="monotone" dataKey="gelir" name="Gelir" stroke="#10b981" strokeWidth={2.5} dot={{ r: 3, fill: "#10b981" }} activeDot={{ r: 5 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
        <div style={{ display: "flex", justifyContent: "center", gap: 20, marginTop: 8 }}>
          <span style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}><span style={{ width: 10, height: 3, background: "#10b981", borderRadius: 2 }} />Gelir</span>
          <span style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}><span style={{ width: 10, height: 3, background: "#ef4444", borderRadius: 2 }} />Gider</span>
        </div>
      </div>
      {/* KART TUTAR DÜZENLE MODAL */}
      {kartDuzenle && (
        <Modal title={`${kartDuzenle.isim} - Tutarları Düzenle`} onClose={() => setKartDuzenle(null)}>
          <p className="tools-note">Ekstre borcu, dönem içi ve provizyondan ayrı takip edilir. Ekstre ödenmeden de yeni alışveriş tutarlarını güncelleyebilirsin. Dönem içi tutarın ekstreye taşınması yeni harcama sayılmaz.</p>
          <Input label="Kalan Ekstre Borcu (₺)" type="number" value={kartDuzForm.ekstre} onChange={e => setKartDuzForm(f => ({ ...f, ekstre: e.target.value }))} placeholder="0.00" />
          <Input label="Dönem İçi (₺)" type="number" value={kartDuzForm.donemIci} onChange={e => setKartDuzForm(f => ({ ...f, donemIci: e.target.value }))} placeholder="0.00" />
          <Input label="Provizyon (₺)" type="number" value={kartDuzForm.provizyon} onChange={e => setKartDuzForm(f => ({ ...f, provizyon: e.target.value }))} placeholder="0.00" />
          <button type="button" onClick={() => setKartDuzForm(f => ({ ...f, bakiyedenDussun: !f.bakiyedenDussun }))} style={{ width: "100%", display: "flex", alignItems: "center", gap: 11, padding: "11px 12px", marginBottom: 10, borderRadius: 12, border: kartDuzForm.bakiyedenDussun ? "1px solid #A74E3266" : "1px solid var(--border)", background: kartDuzForm.bakiyedenDussun ? "var(--accent-soft)" : "var(--card2)", color: "var(--text)", cursor: "pointer", textAlign: "left" }}>
            <span style={{ width: 22, height: 22, borderRadius: 7, border: kartDuzForm.bakiyedenDussun ? "none" : "2px solid var(--border2)", background: kartDuzForm.bakiyedenDussun ? "var(--accent)" : "var(--card)", color: "#fff", display: "grid", placeItems: "center", fontSize: 13, flexShrink: 0 }}>{kartDuzForm.bakiyedenDussun ? "✓" : ""}</span>
            <span>
              <span style={{ display: "block", fontSize: 12, fontWeight: 700 }}>Dönem içi ve provizyon için bütçeden pay ayır</span>
              <span style={{ display: "block", color: "var(--text5)", fontSize: 11, lineHeight: 1.45, marginTop: 2 }}>{kartDuzForm.bakiyedenDussun ? "Ödemeler sonrası kalanı ve tahmini gideri etkiler; gerçek bakiyeyi değiştirmez" : "Yalnız kart borcunda ve harcama raporunda görünür; gerçek bakiyeyi değiştirmez"}</span>
            </span>
          </button>
          <p className="tools-note">Bu ekran borç bilgisini günceller; ödeme kaydı oluşturmaz. Para çıkışını kaydetmek için karttaki “Ödendi olarak işaretle” düğmesini kullan.</p>
          <Btn onClick={() => {
            if (![kartDuzForm.donemIci, kartDuzForm.provizyon, kartDuzForm.ekstre].every(v => tutarGecerli(v || 0, true))) return formHatasi();
            const di = paraOku(kartDuzForm.donemIci) || 0;
            const pr = paraOku(kartDuzForm.provizyon) || 0;
            const ekstreTutar = paraOku(kartDuzForm.ekstre) || 0;
            const hedefIds = (kartDuzenle.islemler || []).map(i => i.id);
            const bugun = yerelTarihAnahtari();
            if (setIslemler(p => {
              const kayitlar = p.filter(i => hedefIds.includes(i.id));
              const eskiToplam = kayitlar.reduce((s, k) => paraTopla(s, kartGuncelToplami(k)), 0);
              const ekstreAktarimi = Math.min(
                Math.max(0, paraTopla(eskiToplam, -di, -pr)),
                Math.max(0, paraTopla(ekstreTutar, -paraTopla(kartDuzenle.ekstre || 0,kartDuzenle.digerEkstre || 0))),
              );
              const yeni = kartKaydiniGuncelle(kayitlar, {
                id: kayitlar[0]?.id ?? Date.now(), kategori: kartDuzenle.isim,
                donemIci: di, provizyon: pr,
                aciklama: kayitlar[0]?.aciklama || kartDuzenle.isim,
              }, bugun, ekstreAktarimi);
              // Sıfırlanan borçta günlük geçmişi koru.
              return [...p.filter(i => !hedefIds.includes(i.id)), yeni];
            }) === false) return;
            if (setKartlar(p => p.map(x => {
              if (String(x.id) !== String(kartDuzenle.id)) return x;
              const eskiTarih = x.ekstreSonOdemeTarihi || kartDuzenle.sonEkstreOdeme?.sonOdemeTarihi;
              const yeniEkstre = !!eskiTarih && eskiTarih !== (x.sonOdemeTarihi || "");
              const ekstreDonemKey = yeniEkstre ? `ekstre:${x.sonOdemeTarihi || aktifDonem}` : kartEkstreAnahtari(x);
              const odenen = yeniEkstre ? 0 : kartEkstreOdemeToplami(x);
              // Düzenleme alanı kalan borçtur; önceki ödeme yeniden düşülmez.
              return { ...x, ekstre: paraTopla(ekstreTutar,odenen), ekstreDonemKey,
                ekstreSonOdemeTarihi: x.sonOdemeTarihi || "", bakiyedenDussun: kartDuzForm.bakiyedenDussun };
            })) === false) return;
            setKartDuzenle(null);
            toast("Kart borcu güncellendi; ödeme kaydı oluşturulmadı");
          }} style={{ width: "100%", padding: 12, marginTop: 8 }}>Kaydet</Btn>
        </Modal>
      )}

      {/* HIZLI İŞLEM EKLE MODAL */}
      {hizliModal && (
        <Modal title="Yeni İşlem" onClose={() => setHizliModal(false)}>
          <div style={{ display: "flex", gap: 6, marginBottom: 12 }}>
            {[
              { id: "gider", label: "Gider", renk: "#ef4444" },
              { id: "gelir", label: "Gelir", renk: "#10b981" },
              { id: "taksit", label: "Taksit", renk: "#8b5cf6" },
            ].map(t => (
              <button key={t.id} onClick={() => setHizliForm(f => ({ ...f, tip: t.id, kategori: t.id === "taksit" ? f.kategori : (kategoriler?.[t.id] || VARSAYILAN_KATEGORILER[t.id])[0] }))} style={{ flex: 1, padding: "10px 4px", borderRadius: 10, border: "none", cursor: "pointer", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 12, fontWeight: 600, background: hizliForm.tip === t.id ? "var(--accent-strong)" : "var(--card2)", color: hizliForm.tip === t.id ? "#fff" : "var(--text3)" }}>
                {t.label}
              </button>
            ))}
          </div>
          {(hizliSablonlar || []).length > 0 && (
            <div style={{ marginBottom: 14 }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 7 }}>
                <span style={{ color: "var(--text4)", fontSize: 11, fontWeight: 700, letterSpacing: ".08em", textTransform: "uppercase" }}><span className="inline-icon"><Icon name="plus" size={16}/></span>Hızlı işlemler</span>
                <span style={{ color: "var(--text5)", fontSize: 11 }}>Tutar seçerek ekle</span>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(2,minmax(0,1fr))", gap: 7 }}>
                {(hizliSablonlar || []).map(s => {
                  return (
                    <div key={s.id} onClick={() => sablondanGiderEkle(s)} role="button" tabIndex={0} onKeyDown={e => { if (e.key === "Enter" || e.key === " ") sablondanGiderEkle(s); }} style={{ position: "relative", minWidth: 0, padding: "10px 30px 10px 10px", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 12, cursor: "pointer" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 7, minWidth: 0 }}>
                        <span style={{ width: 27, height: 27, flexShrink: 0, display: "grid", placeItems: "center", borderRadius: 9, background: s.tip==="gelir" ? "#10b98117" : "#ef444417", fontSize: 13 }}>{katIkon(s.kategori, s.tip||"gider")}</span>
                        <div style={{ minWidth: 0 }}>
                          <div style={{ color: "var(--text2)", fontSize: 11, fontWeight: 700, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{s.ad || s.kategori}</div>
                          <div style={{ color: s.tip==="gelir" ? "var(--income-text)" : "var(--expense-text)", fontSize: 11, fontWeight: 700, marginTop: 2 }}>₺{fmt(Number(s.tutar) || 0)}</div>
                        </div>
                      </div>
                      <button aria-label="Şablonu sil" onClick={e => { e.stopPropagation(); hizliSablonSil(s.id); }} style={{ position: "absolute", top: 6, right: 6, width: 22, height: 22, border: "none", borderRadius: 7, background: "transparent", color: "var(--text5)", cursor: "pointer", fontSize: 14 }}>×</button>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
          {hizliForm.tip === "taksit" ? (
            <>
              <div style={{ background: "var(--accent-soft)", border: "1px solid var(--border)", borderRadius: 10, padding: "9px 12px", marginBottom: 12, color: "var(--accent)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", lineHeight: 1.5 }}>
                Taksitli harcamalar giderinden düşmez, sadece takip amaçlıdır. Özet ekranında ilerlemesini görürsün.
              </div>
              <Input label="Ürün / Harcama Adı" value={hizliForm.taksitAd} onChange={e => setHizliForm(f => ({ ...f, taksitAd: e.target.value }))} placeholder="Örn: Telefon" />
              <Input label="Aylık Taksit Tutarı (₺)" type="number" value={hizliForm.taksitAylik} onChange={e => setHizliForm(f => ({ ...f, taksitAylik: e.target.value }))} placeholder="0.00" />
              <Input label="Toplam Taksit Sayısı (vade)" type="number" value={hizliForm.taksitVade} onChange={e => setHizliForm(f => ({ ...f, taksitVade: e.target.value }))} placeholder="12" />
              <Input label="Şimdiye Kadar Ödenen Taksit" type="number" value={hizliForm.taksitOdenen} onChange={e => setHizliForm(f => ({ ...f, taksitOdenen: e.target.value }))} placeholder="0" />
              <Input label="Kart / Kaynak (isteğe bağlı)" value={hizliForm.taksitKart} onChange={e => setHizliForm(f => ({ ...f, taksitKart: e.target.value }))} placeholder="Örn: Bonus Kart" />
            </>
          ) : (
          <>
          <Select label="Kategori" value={hizliForm.kategori} onChange={e => setHizliForm(f => ({ ...f, kategori: e.target.value }))}>{(kategoriler?.[hizliForm.tip] || VARSAYILAN_KATEGORILER[hizliForm.tip]).map(k => <option key={k}>{k}</option>)}</Select>
          {hizliKartMi ? (
            <>
              <Input label="Dönem İçi (₺)" type="number" value={hizliForm.donemIci} onChange={e => setHizliForm(f => ({ ...f, donemIci: e.target.value }))} placeholder="0.00" />
              <Input label="Provizyon (₺)" type="number" value={hizliForm.provizyon} onChange={e => setHizliForm(f => ({ ...f, provizyon: e.target.value }))} placeholder="0.00" />
            </>
          ) : (
            <Input label="Tutar (₺)" type="number" value={hizliForm.tutar} onChange={e => setHizliForm(f => ({ ...f, tutar: e.target.value }))} placeholder="0.00" />
          )}
          <Input label="Açıklama (isteğe bağlı)" value={hizliForm.aciklama} onChange={e => setHizliForm(f => ({ ...f, aciklama: e.target.value }))} placeholder="Açıklama" />
          <Input label="Tarih" type="date" value={hizliForm.tarih} onChange={e => setHizliForm(f => ({ ...f, tarih: e.target.value }))} />
          </>
          )}
          {hizliForm.tip === "gelir" && hizliForm.kategori === "Kira Geliri" && (
            <label style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, marginBottom: 4, cursor: "pointer" }}>
              <button type="button" onClick={() => setHizliForm(f => ({ ...f, donemeDahil: !f.donemeDahil }))} style={{ width: 22, height: 22, borderRadius: 6, border: `2px solid ${hizliForm.donemeDahil ? "#10b981" : "var(--border2)"}`, background: hizliForm.donemeDahil ? "#10b981" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, color: "var(--bg)" }}>{hizliForm.donemeDahil ? "✓" : ""}</button>
              <span style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Dönem gelirine dahil et</span>
            </label>
          )}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1.15fr", gap: 8, marginTop: 8 }}>
            <Btn variant="secondary" onClick={() => setHizliModal(false)} style={{ width: "100%", padding: 12 }}>Vazgeç</Btn>
            <Btn onClick={hizliEkle} style={{ width: "100%", padding: 12 }}>
              {hizliForm.tip === "gider" && !hizliKartMi ? "Gideri Kaydet" : "Kaydet"}
            </Btn>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ── İŞLEMLER ──────────────────────────────────────────────────────────────────
const fmtTL = n => "₺" + fmt(n);
// Yeni araçlar kayıtları değiştirmeden aynı işlem listesi üzerinde çalışır.
function csvMetniOlustur(islemler, odemeler = []) {
  const hucre = value => {
    let s = String(value ?? "");
    if (/^[\s]*[=+@-]/.test(s)) s = "'" + s;
    return '"' + s.replace(/"/g, '""') + '"';
  };
  const satirlar = [["Tarih", "Tür", "Kategori", "Açıklama", "Tutar (TL)", "Durum", "Kayıt türü"]];
  [...islemler].sort((a,b) => a.tarih.localeCompare(b.tarih)).forEach(i => {
    const o = odemeler.find(o => String(o.id) === String(i.odemeId) || String(o.islemId) === String(i.id));
    satirlar.push([i.tarih, devirIslemiMi(i) ? "Devir" : i.tip === "gelir" ? "Gelir" : "Gider", i.kategori, i.aciklama || "", (paraKurus(i.tutar) / 100).toFixed(2).replace('.', ','), o ? (o.odendi ? "Ödendi" : "Bekleyen ödeme") : i.tarih > yerelTarihAnahtari() ? "Planlanan" : "Kayıtlı", i.kartHarcama ? "Kart toplamı" : i.sabitId ? "Sabit gider" : "İşlem"]);
  });
  return '\uFEFF' + satirlar.map(s => s.map(hucre).join(';')).join('\r\n');
}
async function csvAktar(islemler, odemeler = []) {
  if (!islemler.length) { toast("Aktarılacak işlem yok", "uyari"); return; }
  try {
    const isim = `butce-islemler-${yerelTarihAnahtari()}.csv`;
    const blob = new Blob([csvMetniOlustur(islemler, odemeler)], {type:"text/csv;charset=utf-8"});
    if (navigator.share && typeof File !== "undefined") {
      const file = new File([blob], isim, {type:"text/csv"});
      if (navigator.canShare?.({files:[file]})) {
        try { await navigator.share({files:[file],title:"İşlemler"}); toast("CSV paylaşım ekranı açıldı"); return; }
        catch(e) { if(e.name === "AbortError") return; }
      }
    }
    const url = URL.createObjectURL(blob), a = document.createElement('a');
    a.href = url; a.download = isim; document.body.appendChild(a); a.click();
    setTimeout(() => {a.remove();URL.revokeObjectURL(url);},1000);
    toast("CSV indirme isteği gönderildi");
  } catch { toast("CSV oluşturulamadı", "hata"); }
}
function esnekRaporAraligi(mod, donemGun, offset, yil, ilk, son, bugun = yerelTarihAnahtari()) {
  const aktif = maasDonemiBul(bugun, donemGun);
  const bas = new Date(aktif.yil, aktif.ay + offset, aktif.gun);
  if(mod === 'ozel') return yerelTarihOku(ilk) && yerelTarihOku(son) && ilk <= son ? {ilk,son} : null;
  if(mod === 'yil') return {ilk:yerelTarihAnahtari(new Date(yil,0,aktif.gun)),son:yerelTarihAnahtari(new Date(Number(yil)+1,0,aktif.gun-1))};
  return {ilk:yerelTarihAnahtari(new Date(bas.getFullYear(),bas.getMonth()-(mod==='uc'?2:0),aktif.gun)),son:yerelTarihAnahtari(new Date(bas.getFullYear(),bas.getMonth()+1,aktif.gun-1))};
}
function aralikOzeti(islemler, ilk, son, bugun = yerelTarihAnahtari()) {
  const kayitlar = analizIslemleri(islemler).filter(i => i.tarih >= ilk && i.tarih <= son && i.tarih <= bugun);
  const gelir = kayitlar.filter(i => i.tip === 'gelir' && i.kategori !== 'Birikim' && !(i.kategori === 'Kira Geliri' && i.donemeDahil === false)).reduce((s,i)=>paraTopla(s,i.tutar),0);
  const gider = kayitlar.filter(i => i.tip === 'gider').reduce((s,i)=>paraTopla(s,i.tutar),0);
  return {kayitlar,gelir,gider,net:paraTopla(gelir,-gider),oran:gelir>0?(gelir-gider)/gelir*100:null};
}
function donemSonuGiderTahmini({ islemler, odemeler = [], kartlar = [], kartOdemeleri = [], baslangic, bitis, bakiyeHesapModu = "kartlar_dahil", bugun = new Date() }) {
  const ilk = yerelTarihAnahtari(baslangic), son = yerelTarihAnahtari(bitis), simdi = yerelTarihAnahtari(bugun);
  const toplamGun = takvimGunFarki(son, ilk) + 1;
  const gecenGun = Math.min(toplamGun, Math.max(1, takvimGunFarki(simdi, ilk) + 1));
  const aralikta = tarih => tarih >= ilk && tarih <= son;
  const kartIsimleri = new Set(kartlar.map(k => k.isim));
  const dahilKartKayitlari = islemler.filter(i => {
    if (i.tip !== 'gider' || !i.kartHarcama) return false;
    const kart = kartlar.find(k => k.isim === i.kategori);
    if (!kart) return false;
    return kart.bakiyedenDussun != null ? kart.bakiyedenDussun !== false
      : bakiyeHesapModu === 'kartlar_dahil' || kartOdemeleri.some(o => String(o.kartId) === String(kart.id) && aralikta(o.tarih));
  });
  const normal = islemler.filter(i => i.tip === 'gider' && !i.kartHarcama && !kartIsimleri.has(i.kategori) && aralikta(i.tarih));
  const sabitMi = i => i.sabitId != null || i.kaynak === 'odeme' || i.odemeId != null || i.tarih > simdi
    || odemeler.some(o => o.islemId != null && String(o.islemId) === String(i.id));
  let sabitGider = normal.filter(sabitMi).reduce((s,i) => paraTopla(s,i.tutar),0);
  // Ödeme ekranındaki kayıt, bağlı işlemde zaten sayıldıysa ikinci kez ekleme.
  odemeler.filter(o => aralikta(o.tarih)).forEach(o => {
    if (!normal.some(i => (o.islemId != null && String(i.id) === String(o.islemId)) || (i.odemeId != null && String(i.odemeId) === String(o.id)))) {
      sabitGider = paraTopla(sabitGider,o.tutar);
    }
  });
  // Kalan ekstre borcu ve bu dönemde yapılan ekstre ödemeleri bir kez sayılır.
  const kalanEkstre = kartlar.reduce((s,k) => {
    const odenen = kartOdemeleri.filter(o => String(o.kartId) === String(k.id)
      && (k.ekstreDonemKey ? (o.ekstreDonemKey || o.donemKey) === k.ekstreDonemKey : !k.sonOdemeTarihi || o.sonOdemeTarihi === k.sonOdemeTarihi))
      .reduce((n,o) => paraTopla(n,o.tutar),0);
    return paraTopla(s,Math.max(0,paraTopla(k.ekstre || 0,-odenen)));
  },0);
  const odenmisEkstre = kartOdemeleri.filter(o => aralikta(o.tarih) && o.tarih <= simdi).reduce((s,o) => paraTopla(s,o.tutar),0);
  const ekstreGideri = paraTopla(kalanEkstre,odenmisEkstre);
  // Bu dönem yapılan alışveriş sonradan ekstreye taşınırsa hem tabanda hem
  // günlük harcamada sayılmasın; günlük ortalaması korunur.
  const ekstreyeTasinanDegisken = dahilKartKayitlari.reduce((s,i) => {
    const hareketler = kartAnalizHareketleri(i).filter(h => aralikta(h.tarih) && h.tarih <= simdi);
    let kapsanan = 0;
    Object.entries(i.kartEkstreAktarimlari || {}).filter(([t]) => aralikta(t) && t <= simdi).sort(([a],[b]) => a.localeCompare(b)).forEach(([t,tutar]) => {
      const oncekiHarcama = hareketler.filter(h => h.tarih <= t).reduce((n,h) => paraTopla(n,h.tutar),0);
      kapsanan = paraTopla(kapsanan,Math.min(Number(tutar) || 0,Math.max(0,paraTopla(oncekiHarcama,-kapsanan))));
    });
    return paraTopla(s,kapsanan);
  },0);
  const degiskenNormal = normal.filter(i => !sabitMi(i) && i.tarih <= simdi).reduce((s,i) => paraTopla(s,i.tutar),0);
  const degiskenKart = dahilKartKayitlari.flatMap(kartAnalizHareketleri)
    .filter(h => aralikta(h.tarih) && h.tarih <= simdi).reduce((s,h) => paraTopla(s,h.tutar),0);
  const degiskenGider = paraTopla(degiskenNormal,degiskenKart);
  // İlk kart toplamı tek seferlik tabandır; yalnız sonraki artışlar günlük tahmine girer.
  // Ekstreye aktarılmış açılış tutarını tekrar sayma.
  const kartAcilisGideri = dahilKartKayitlari.reduce((s,i) => {
    const tarih = i.kartGunBaslangicTarih || i.tarih;
    if (!aralikta(tarih) || tarih > simdi) return s;
    const acilis = Math.max(0,Number(i.kartGunBaslangicToplam ?? kartGuncelToplami(i)) || 0);
    const aktarilan = Object.entries(i.kartEkstreAktarimlari || {}).filter(([t]) => aralikta(t) && t <= simdi)
      .reduce((n,[,tutar]) => paraTopla(n,tutar),0);
    const farklar = kartAnalizHareketleri(i).filter(h => aralikta(h.tarih) && h.tarih <= simdi);
    let kapsanan = 0;
    Object.entries(i.kartEkstreAktarimlari || {}).filter(([t]) => aralikta(t) && t <= simdi).sort(([a],[b])=>a.localeCompare(b)).forEach(([t,tutar])=>{
      const onceki = farklar.filter(h=>h.tarih<=t).reduce((n,h)=>paraTopla(n,h.tutar),0);
      kapsanan = paraTopla(kapsanan,Math.min(Number(tutar)||0,Math.max(0,paraTopla(onceki,-kapsanan))));
    });
    return paraTopla(s,Math.max(0,paraTopla(acilis,-aktarilan,kapsanan)));
  },0);
  const sabitTaban = paraTopla(sabitGider,kartAcilisGideri,Math.max(0,paraTopla(ekstreGideri,-ekstreyeTasinanDegisken)));
  const gunlukOrt = degiskenGider / gecenGun;
  const tahmin = paraOku(sabitTaban + gunlukOrt * toplamGun);
  return { tahmin, gunlukOrt, sabitTaban, sabitGider, ekstreGideri, kartAcilisGideri, degiskenGider, gecenGun, toplamGun };
}

function tasarrufPayiHesapla({gelir,gider,birikim=0,hedef,kalanGun,ekBekleyen=0}) {
  const hedefTutar = Math.round(paraKurus(gelir)*hedef/100)/100;
  const ayrilacak = Math.max(0,paraTopla(hedefTutar,-birikim));
  const pay = paraTopla(gelir,-gider,-birikim,-ayrilacak,-ekBekleyen);
  return {hedefTutar,ayrilacak,pay,limit:Math.max(0,pay)/Math.max(1,kalanGun)};
}
function TasarrufHedefi({hedef,setHedef}) {
  const [taslak,setTaslak] = useState(String(hedef));
  useEffect(()=>setTaslak(String(hedef)),[hedef]);
  return <section className="tools-card"><h3>Tasarruf hedefi</h3><p className="tools-note">Gelirinin ayırmak istediğin yüzdesi. Günlük harcama payı bu hedef ve kayıtlı ödemeler dikkate alınarak hesaplanır.</p><div className="tools-grid"><Input label="Hedef (%)" type="number" min="0" max="100" step="1" value={taslak} onChange={e=>setTaslak(e.target.value)} /><Btn onClick={()=>{if(!tamSayiGecerli(taslak,0,100)){toast("0–100 arasında tam sayı gir", "uyari");return;}setHedef(Number(taslak));toast("Tasarruf hedefi kaydedildi");}}>Hedefi kaydet</Btn></div></section>;
}
function gunlukFinansHareketleri(islemler,odemeler, kartOdemeleri,ilk,son,bugun=yerelTarihAnahtari(),gorunum='harcama') {
  const sonuc = [], sayilanOdemeler = new Set();
  const ekle = i => { if(yerelTarihOku(i.tarih) && i.tarih>=ilk && i.tarih<=son && i.tarih<=bugun) sonuc.push(i); };
  const odemeEkle = (o,i={}) => {
    if(!o.odendi || sayilanOdemeler.has(String(o.id))) return;
    sayilanOdemeler.add(String(o.id));
    const odendi = o.odendiTarih ? new Date(o.odendiTarih) : null;
    const tarih = odendi && Number.isFinite(odendi.getTime()) ? yerelTarihAnahtari(odendi) : o.tarih || i.tarih;
    ekle({...i,id:`odeme:${o.id}`,tip:'gider',kategori:o.kategori||i.kategori||'Fatura',aciklama:o.aciklama||i.aciklama||o.kategori||'Ödeme',tutar:paraOku(o.tutar),tarih,kaynak:'odeme',odemeId:o.id});
  };
  islemler.forEach(i=>{
    if(i.tip==='gelir') {
      if(gorunum==='nakit' && i.kategori==='Birikim') {ekle({...i,tip:'gider',kaynak:'birikim_aktarimi'});return;}
      if(!devirIslemiMi(i) && i.kategori!=='Birikim' && !(i.kategori==='Kira Geliri'&&i.donemeDahil===false))ekle(i);
      return;
    }
    if(i.tip!=='gider' || i.kartEkstreOdeme || i.kaynak==='ekstre_odemesi')return;
    if(i.kartHarcama) {
      if(gorunum==='nakit') return;
      kartAnalizHareketleri(i).forEach(h=>ekle({...i,...h,id:`kart-fark:${i.id}:${h.tarih}`,kartHarcama:false,kartGunlukKayit:true,aciklama:`${i.kategori} · Kart harcama artışı`,kaynak:'kart_gunluk_fark'}));return;
    }
    const o=odemeler.find(o=>(i.odemeId!=null&&String(o.id)===String(i.odemeId)) || (o.islemId!=null&&String(o.islemId)===String(i.id)));
    if(o){odemeEkle(o,i);return;}
    if(i.kaynak==='odeme'||i.odemeId!=null)return;
    ekle(i);
  });
  odemeler.forEach(o=>odemeEkle(o));
  if(gorunum==='nakit') kartOdemeleri.forEach(o=>ekle({id:`ekstre:${o.id}`,tip:'gider',kategori:o.kartAdi||'Kredi kartı',aciklama:o.aciklama||`${o.kartAdi||'Kredi kartı'} ekstre ödemesi`,tutar:paraOku(o.tutar),tarih:o.tarih,kaynak:'ekstre_odemesi'}));
  return sonuc;
}
function gunlukGiderHareketleri(islemler,odemeler,ilk,son,bugun=yerelTarihAnahtari(),kartOdemeleri=[],gorunum='harcama') {
  return gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,ilk,son,bugun,gorunum).filter(i=>i.tip==='gider');
}
function donemBakiyeHesapla({islemler,odemeler=[],kartlar=[],kartOdemeleri=[],ilk,son,bugun=yerelTarihAnahtari(),bakiyeHesapModu='kartlar_dahil'}) {
  const nakit = gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,ilk,son,bugun,'nakit');
  const harcamalar = gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,ilk,son,bugun,'harcama');
  const topla = liste => liste.reduce((s,i)=>paraTopla(s,i.tutar),0);
  const aralikta = t => t >= ilk && t <= son;
  const gelir = topla(nakit.filter(i=>i.tip==='gelir'));
  const devir = topla(islemler.filter(i=>i.tip==='gelir' && devirIslemiMi(i) && aralikta(i.tarih) && i.tarih<=bugun));
  const birikim = topla(nakit.filter(i=>i.kaynak==='birikim_aktarimi'));
  const nakitCikis = topla(nakit.filter(i=>i.tip==='gider'));
  const harcama = topla(harcamalar.filter(i=>i.tip==='gider'));
  const bagliOdeme = i => odemeler.find(o=>(i.odemeId!=null && String(o.id)===String(i.odemeId)) || (o.islemId!=null && String(o.islemId)===String(i.id)));
  let bekleyen = topla(odemeler.filter(o=>!o.odendi && o.tarih<=son));
  bekleyen = paraTopla(bekleyen,topla(islemler.filter(i=>i.tip==='gider' && !i.kartHarcama && i.kaynak!=='odeme' && i.odemeId==null && !bagliOdeme(i) && aralikta(i.tarih) && i.tarih>bugun)));
  bekleyen = paraTopla(bekleyen,topla(kartOdemeleri.filter(o=>aralikta(o.tarih) && o.tarih>bugun)));
  const ekstreBekleyen = kartlar.reduce((n,k)=>{
    if(k.sonOdemeTarihi && k.sonOdemeTarihi>son) return n;
    const odenen=kartOdemeleri.filter(o=>String(o.kartId)===String(k.id) && o.tarih<=son && o.tarih<=bugun
      && (k.ekstreDonemKey?(o.ekstreDonemKey||o.donemKey)===k.ekstreDonemKey:!k.sonOdemeTarihi||o.sonOdemeTarihi===k.sonOdemeTarihi)).reduce((a,o)=>paraTopla(a,o.tutar),0);
    // İleri tarihli ödeme zaten yukarıda planlandı; aynı borcu tekrar ayırma.
    const planli=kartOdemeleri.filter(o=>String(o.kartId)===String(k.id) && aralikta(o.tarih) && o.tarih>bugun
      && (k.ekstreDonemKey?(o.ekstreDonemKey||o.donemKey)===k.ekstreDonemKey:!k.sonOdemeTarihi||o.sonOdemeTarihi===k.sonOdemeTarihi)).reduce((a,o)=>paraTopla(a,o.tutar),0);
    return paraTopla(n,Math.max(0,paraTopla(k.ekstre||0,-odenen,-planli)));
  },0);
  const kartAyrilan = topla(islemler.filter(i=>i.tip==='gider' && i.kartHarcama && aralikta(i.tarih) && i.tarih<=bugun && (()=>{
    const k=kartlar.find(k=>k.isim===i.kategori);
    return k && (k.bakiyedenDussun!=null?k.bakiyedenDussun!==false:bakiyeHesapModu==='kartlar_dahil');
  })()).map(i=>({...i,tutar:kartGuncelToplami(i)})));
  const gercekBakiye=paraTopla(gelir,devir,-nakitCikis);
  const ayrilan=paraTopla(bekleyen,ekstreBekleyen,kartAyrilan);
  return {gelir,devir,birikim,nakitCikis,harcama,bekleyen,ekstreBekleyen,kartAyrilan,ayrilan,gercekBakiye,odemelerSonrasi:paraTopla(gercekBakiye,-ayrilan)};
}

function haftaninGunleriOzeti(hareketler,ilk,son,bugun=yerelTarihAnahtari()) {
  const adlar=['Pazartesi','Salı','Çarşamba','Perşembe','Cuma','Cumartesi','Pazar'];
  const gruplar=adlar.map(ad=>({ad,kacGun:0,toplamKurus:0,toplam:0}));
  const bitis=son<bugun?son:bugun;
  const bas=yerelTarihOku(ilk),sonTarih=yerelTarihOku(bitis);
  if(!bas||!sonTarih||bas>sonTarih)return gruplar;
  const gunSayisi=takvimGunFarki(bitis,ilk)+1, ilkGun=(bas.getDay()+6)%7;
  gruplar.forEach(g=>{g.kacGun=Math.floor(gunSayisi/7);});
  for(let i=0;i<gunSayisi%7;i++)gruplar[(ilkGun+i)%7].kacGun++;
  hareketler.filter(i=>i.tarih>=ilk&&i.tarih<=bitis).forEach(i=>{
    const t=yerelTarihOku(i.tarih);
    if(t)gruplar[(t.getDay()+6)%7].toplamKurus+=paraKurus(i.tutar)||0;
  });
  return gruplar.map(g=>({...g,toplam:g.toplamKurus/100}));
}
function HaftaninGunleri({islemler,odemeler,kartOdemeleri=[],ilk,son,gorunum='harcama'}) {
  const bugun=yerelTarihAnahtari();
  const veri=useMemo(()=>haftaninGunleriOzeti(gunlukGiderHareketleri(islemler,odemeler,ilk,son,bugun,kartOdemeleri,gorunum),ilk,son,bugun),[islemler,odemeler,kartOdemeleri,ilk,son,bugun,gorunum]);
  const max=Math.max(1,...veri.map(g=>g.toplam));
  return <section className="tools-card weekday-card"><h3><Icon name="calendar" size={17}/>Haftanın Günleri</h3><p className="tools-note">Seçili aralıkta haftanın her günü için toplam {gorunum==='nakit'?'nakit çıkışı':'harcama'}</p>{veri.map(g=><div className="weekday-row" key={g.ad}><div><span>{g.ad}</span><strong>{fmtTL(g.toplam)}</strong></div><div className="weekday-track"><span style={{width:`${g.toplam/max*100}%`}}/></div></div>)}<details className="calculation-help"><summary>Nasıl hesaplanır?</summary><p className="tools-note">Seçili aralıktaki tüm pazartesiler kendi içinde, diğer günler de kendi içinde toplanır; ortalama alınmaz. Harcama görünümünde kartın yalnız günlük artışları; nakit görünümünde yalnız ekstre ödemeleri sayılır. İkisi aynı toplamda birleşmez. Gelecek tarihli ve bekleyen ödemeler hariçtir.</p></details></section>;
}

function HarcamaHaritasi({islemler,odemeler,kartOdemeleri=[],ilk,son,gorunum='harcama'}) {
  const [ay,setAy] = useState(ilk.slice(0,7));
  const [secili,setSecili] = useState(null);
  useEffect(()=>{setAy(ilk.slice(0,7));setSecili(null);},[ilk,son]);
  const bugun = yerelTarihAnahtari();
  const tumHareketler = useMemo(()=>gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,ilk,son,bugun,gorunum),[islemler,odemeler,kartOdemeleri,ilk,son,bugun,gorunum]);
  const hareketler = tumHareketler.filter(i=>i.tip==='gider');
  const gelirler = tumHareketler.filter(i=>i.tip==='gelir');
  const gunGeliri = gelirler.filter(i=>i.tarih===secili).reduce((s,i)=>paraTopla(s,i.tutar),0);
  const map = {};
  hareketler.forEach(i=>{map[i.tarih]=paraTopla(map[i.tarih]||0,i.tutar);});
  const [y,m]=ay.split('-').map(Number), bas=new Date(y,m-1,1), gunler=new Date(y,m,0).getDate();
  const max=Math.max(1,...Object.entries(map).filter(([t])=>t.startsWith(ay)).map(([,v])=>v));
  const ayDegistir=delta=>{const t=yerelTarihAnahtari(new Date(y,m-1+delta,1)).slice(0,7);if(t>=ilk.slice(0,7)&&t<=son.slice(0,7)){setAy(t);setSecili(null);}};
  return <section className="tools-card"><h3>Harcama haritası</h3><div className="tools-row"><button className="tools-button" aria-label="Haritada önceki ay" disabled={ay<=ilk.slice(0,7)} onClick={()=>ayDegistir(-1)}>‹</button><strong>{bas.toLocaleDateString('tr-TR',{month:'long',year:'numeric'})}</strong><button className="tools-button" aria-label="Haritada sonraki ay" disabled={ay>=son.slice(0,7)} onClick={()=>ayDegistir(1)}>›</button></div><div className="heat-grid">{['Pt','Sa','Ça','Pe','Cu','Ct','Pa'].map(g=><span className="tools-note" key={g}>{g}</span>)}{Array.from({length:(bas.getDay()+6)%7},(_,i)=><span key={'blank'+i}/>)}{Array.from({length:gunler},(_,i)=>{
    const t=yerelTarihAnahtari(new Date(y,m-1,i+1)),tutar=map[t]||0,dis=t<ilk||t>son||t>bugun;
    return <button key={t} className="heat-day" disabled={dis} aria-pressed={secili===t} aria-label={`${t}: ${fmtTL(tutar)}`} onClick={()=>setSecili(t)} style={{background:tutar>0?`rgba(167,78,50,${.12+.65*tutar/max})`:'var(--card2)',color:tutar/max>.6?'#fff':'var(--text)',borderColor:secili===t?'var(--accent)':'var(--border)'}}><span>{i+1}</span><small>{tutar>0?'●':'·'}</small></button>;
  })}</div><p className="tools-note">Koyu renk daha fazla harcama demektir. Boş gün, kayıt olmadığı anlamına gelir. Bekleyen ödemeler ve ilk kart toplamı haritada yer almaz. Harcama görünümünde günlük kart farkları gösterilir. Harcama görünümünde ekstre ödemesi ikinci gider sayılmaz. Nakit hareketlerinde ekstre ödeme tarihinde görünür; kart alışverişi gösterilmez. Gelirde birikim ve önceki aydan devir hariçtir.</p>{secili&&<div aria-live="polite"><h4>{secili.split('-').reverse().join('.')} · {fmtTL(map[secili]||0)}</h4><div className="day-totals"><div><span>Gelir toplamı</span><strong style={{color:'var(--income-text)'}}>{fmtTL(gunGeliri)}</strong></div><div><span>{gorunum==='nakit'?'Nakit çıkışı toplamı':'Gider toplamı'}</span><strong style={{color:'var(--expense-text)'}}>{fmtTL(map[secili]||0)}</strong></div></div>{[['gelir','Gelir ayrıntıları',gelirler],['gider',gorunum==='nakit'?'Nakit çıkışı ayrıntıları':'Gider ayrıntıları',hareketler]].map(([tip,baslik,liste])=><div key={tip}><h4>{baslik}</h4>{liste.filter(i=>i.tarih===secili).map((i,k)=><div className="tools-row" key={`${i.id}-${k}`}><span>{i.aciklama||i.kategori}<small className="tools-note"> · {i.kategori}</small></span><strong style={{color:tip==='gelir'?'var(--income-text)':'var(--expense-text)'}}>{tip==='gelir'?'+':'−'}{fmtTL(i.tutar)}</strong></div>)}{!liste.some(i=>i.tarih===secili)&&<p className="tools-note">Bu gün için {tip} kaydı yok.</p>}</div>)}</div>}</section>;
}
function ekstreTrendIslemleri(islemler, kartlar, kartOdemeleri) {
  const kartIsimleri = new Set(kartlar.map(k => k.isim));
  const normal = islemler.filter(i => !i.kartHarcama && !(i.tip === 'gider' && kartIsimleri.has(i.kategori)));
  const odemeler = kartOdemeleri.map(o => ({
    id:`ekstre-trend:${o.id}`,tip:'gider',kategori:o.kartAdi || kartlar.find(k=>String(k.id)===String(o.kartId))?.isim || 'Kredi kartı',
    tutar:Number(o.tutar) || 0,tarih:o.tarih,kaynak:'ekstre_odemesi',
  }));
  return [...normal,...odemeler];
}

function yillikKategoriVerisi(islemler, yil, tip, simdi = new Date()) {
  const aySayisi = yil < simdi.getFullYear() ? 12 : yil === simdi.getFullYear() ? simdi.getMonth() + 1 : 0;
  return Array.from({ length: aySayisi }, (_, ay) => {
    const ilk = yerelTarihAnahtari(new Date(yil, ay, 1));
    const son = yerelTarihAnahtari(new Date(yil, ay + 1, 0));
    const kayitlar = aralikOzeti(islemler, ilk, son, yerelTarihAnahtari(simdi)).kayitlar;
    const kategoriler = {};
    kayitlar.filter(i => i.tip === tip && (tip !== "gelir" || (i.kategori !== "Birikim" && !(i.kategori === "Kira Geliri" && i.donemeDahil === false)))).forEach(i => {
      kategoriler[i.kategori] = paraTopla(kategoriler[i.kategori] || 0, i.tutar);
    });
    return { ay, label: AYLAR[ay].slice(0, 3), kategoriler };
  });
}

function YillikKategoriKiyas({ islemler, yil, tip }) {
  const [secimler, setSecimler] = useState({});
  const bugun = yerelTarihAnahtari();
  const aylar = useMemo(() => yillikKategoriVerisi(islemler, yil, tip), [islemler, yil, tip, bugun]);
  const kategoriler = [...new Set(aylar.flatMap(a => Object.keys(a.kategoriler)))].sort((a, b) => {
    const toplam = k => aylar.reduce((s, ay) => paraTopla(s, ay.kategoriler[k] || 0), 0);
    return toplam(b) - toplam(a) || a.localeCompare(b, "tr");
  });
  return <section className="tools-card annual-category-chart">
    <h3><Icon name={tip === "gelir" ? "arrowUp" : "arrowDown"} size={17}/>{tip === "gelir" ? "Gelir" : "Gider"} kategorileri · aylık değişim</h3>
    <p className="tools-note">{yil} · Yeşil: {tip === "gelir" ? "artış" : "azalış"} · Kırmızı: {tip === "gelir" ? "azalış" : "artış"} · Gri: ilk ay veya değişim yok. Bir aya dokunarak ayrıntısını gör.</p>
    {!kategoriler.length && <p className="tools-note">Bu yılda henüz {tip} kaydı yok.</p>}
    {kategoriler.map(kategori => {
      const degerler = aylar.map(a => a.kategoriler[kategori] || 0);
      const key = `${yil}:${tip}:${kategori}`;
      const idx = Math.min(secimler[key] ?? aylar.length - 1, aylar.length - 1);
      const tutar = degerler[idx];
      const onceki = idx > 0 ? degerler[idx - 1] : null;
      const fark = onceki === null ? null : paraTopla(tutar, -onceki);
      const olumlu = fark !== null && (tip === "gelir" ? fark > 0 : fark < 0);
      const renk = fark === null || fark === 0 ? "var(--text3)" : olumlu ? "var(--income-text)" : "var(--expense-text)";
      const oran = onceki > 0 ? (tutar - onceki) / onceki * 100 : null;
      const max = Math.max(1, ...degerler);
      const toplam = degerler.reduce((s, v) => paraTopla(s, v), 0);
      return <section className="category-trend-card" key={key} aria-label={`${kategori} ${tip} yıllık karşılaştırması`}>
        <div className="category-trend-top"><div className="category-trend-main">
          <div className="category-trend-name"><KategoriSimge kategori={kategori} tip={tip}/>{kategori}</div>
          <div className="category-trend-amount">{fmtTL(tutar)}</div>
          <span className="category-trend-caption">{AYLAR_TAM[idx]} {yil}{yil === new Date().getFullYear() && idx === new Date().getMonth() ? " · devam ediyor" : ""}</span>
        </div><div className="category-trend-change">
          <span className="category-trend-badge" style={{ color: renk }}>{onceki === null ? "İlk ay" : oran !== null ? `${oran > 0 ? "+" : oran < 0 ? "-" : ""}%${Math.abs(oran).toFixed(1).replace(".", ",")}` : tutar > 0 ? "Önceki ay 0 TL" : "Değişim yok"}</span>
          <span className="category-trend-caption">{onceki === null ? "Karşılaştırma yok" : "Önceki aya göre"}</span>
        </div></div>
        <div className="category-trend-scroll"><div className="category-trend-columns" style={{ gridTemplateColumns: `repeat(${aylar.length}, minmax(0, 1fr))` }}>
          {degerler.map((v, i) => {
            const delta = i ? v - degerler[i - 1] : 0;
            const iyi = tip === "gelir" ? delta > 0 : delta < 0;
            const c = !i || delta === 0 ? "var(--text3)" : iyi ? "var(--income-text)" : "var(--expense-text)";
            return <button type="button" className="category-trend-column" key={i} aria-pressed={idx === i} aria-label={`${kategori} ${AYLAR_TAM[i]} ${yil}: ${fmtTL(v)}`} onClick={() => setSecimler(s => ({ ...s, [key]: i }))}>
              <span className="category-trend-bar-area"><span className="category-trend-bar" style={{ background: c, color: c, height: v > 0 ? `${Math.max(v / max * 100, 4)}%` : "2px", opacity: idx === i ? 1 : .72 }}/></span>
              <span className="category-trend-month">{aylar[i].label}</span>
            </button>;
          })}
        </div></div>
        <div className="category-trend-selected" aria-live="polite">{onceki === null ? "Ocak için önceki ay kıyası yok." : <>Önceki ay: <strong>{fmtTL(onceki)}</strong> · Fark: <strong style={{ color: renk }}>{fark > 0 ? "+" : ""}{fmtTL(fark)}</strong></>}<span className="category-trend-caption">Yıl toplamı: {fmtTL(toplam)}</span></div>
      </section>;
    })}
  </section>;
}

function EsnekRapor({islemler,odemeler,kartlar=[],kartOdemeleri=[],donemGun,gelismisYorum,birikimRaporu}) {
  const [raporTuru,setRaporTuru]=useLocalStorage('ft_rapor_gorunum','harcama');
  const raporBugun=yerelTarihAnahtari();
  const raporKayitlari=useMemo(()=>gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,'1900-01-01','9999-12-31',raporBugun,raporTuru),[islemler,odemeler,kartOdemeleri,raporTuru,raporBugun]);
  const [mod,setMod]=useState('donem');
  const [offset,setOffset]=useState(0);
  const [yil,setYil]=useState(new Date().getFullYear());
  const [ilk,setIlk]=useState(yerelTarihAnahtari(maasDonemiBul(yerelTarihAnahtari(),donemGun).baslangic));
  const [son,setSon]=useState(yerelTarihAnahtari());
  const [sekme,setSekme]=useState('ozet');
  const [kategoriYili,setKategoriYili]=useState(new Date().getFullYear());
  const { gorunumler: kategoriGorunumleri } = useContext(KategoriGorunumContext);
  const aralik=esnekRaporAraligi(mod,donemGun,offset,yil,ilk,son);
  const bugun=yerelTarihAnahtari();
  const ozet=useMemo(()=>aralik?aralikOzeti(raporKayitlari,aralik.ilk,aralik.son):null,[raporKayitlari,aralik?.ilk,aralik?.son]);
  const bitis=aralik?(aralik.son<bugun?aralik.son:bugun):null;
  const onceki=aralik&&bitis>=aralik.ilk?(()=>{
    const gun=takvimGunFarki(bitis,aralik.ilk)+1;
    return aralikOzeti(raporKayitlari,yerelTarihAnahtari(takvimGunuEkle(aralik.ilk,-gun)),yerelTarihAnahtari(takvimGunuEkle(aralik.ilk,-1)));
  })():null;
  const kategoriToplamlari=tip=>{
    const map={};
    ozet?.kayitlar.filter(i=>i.tip===tip && (tip!=='gelir'||(i.kategori!=='Birikim'&&!(i.kategori==='Kira Geliri'&&i.donemeDahil===false)))).forEach(i=>{map[i.kategori]=paraTopla(map[i.kategori]||0,i.tutar);});
    return Object.entries(map).sort((a,b)=>b[1]-a[1]);
  };
  const giderKategorileri=kategoriToplamlari('gider');
  const gelirKategorileri=kategoriToplamlari('gelir');
  const trendIslemleri=useMemo(()=>raporKayitlari,[raporKayitlari]);
  const trendOzeti=useMemo(()=>aralik?aralikOzeti(trendIslemleri,aralik.ilk,aralik.son):null,[trendIslemleri,aralik?.ilk,aralik?.son]);
  const grafik=useMemo(()=>{
    if(!aralik||bitis<aralik.ilk)return [];
    const gunSayisi=takvimGunFarki(bitis,aralik.ilk)+1;
    const mod=gunSayisi<=45?'gun':gunSayisi<=730?'ay':'yil';
    const map={};
    const key=t=>mod==='gun'?t:mod==='ay'?t.slice(0,7):t.slice(0,4);
    for(let t=yerelTarihOku(aralik.ilk);yerelTarihAnahtari(t)<=bitis;){
      const k=key(yerelTarihAnahtari(t));
      if(!map[k])map[k]={key:k,label:mod==='gun'?t.toLocaleDateString('tr-TR',{day:'numeric',month:'short'}):mod==='ay'?t.toLocaleDateString('tr-TR',{month:'short',year:'2-digit'}):String(t.getFullYear()),gelir:0,gider:0};
      if(mod==='gun')t.setDate(t.getDate()+1);
      else if(mod==='ay')t=new Date(t.getFullYear(),t.getMonth()+1,1);
      else t=new Date(t.getFullYear()+1,0,1);
    }
    trendOzeti?.kayitlar.forEach(i=>{
      const bucket=map[key(i.tarih)];
      if(!bucket||i.kategori==='Birikim'||(i.tip==='gelir'&&i.kategori==='Kira Geliri'&&i.donemeDahil===false))return;
      bucket[i.tip]=paraTopla(bucket[i.tip],i.tutar);
    });
    return Object.values(map);
  },[trendOzeti,aralik?.ilk,bitis]);
  const fark=(simdi,eski)=>eski>0?`${simdi>=eski?'+':''}${((simdi-eski)/eski*100).toFixed(1).replace('.',',')}%`:'Önceki tutar yok';
  const dagilim=(liste,toplam,tip)=><section className="tools-card"><h3><Icon name={tip==='gelir'?'arrowUp':'arrowDown'} size={17}/>{tip==='gelir'?'Gelir kaynakları':'Gider kategorileri'}</h3>{liste.length?liste.map(([k,v])=><div className="category-distribution" key={k}><div className="tools-row"><span className="category-label"><KategoriSimge kategori={k} tip={tip}/>{k}</span><strong>{fmtTL(v)} <small>%{toplam>0?(v/toplam*100).toFixed(1):0}</small></strong></div><div className="distribution-track"><span style={{width:`${toplam>0?v/toplam*100:0}%`,background:kategoriGorunumu(k,tip,kategoriGorunumleri).renk}}/></div></div>):<p className="tools-note">Bu aralıkta {tip} kaydı yok.</p>}</section>;
  const gunSayisi=aralik&&bitis>=aralik.ilk?takvimGunFarki(bitis,aralik.ilk)+1:0;
  return <div className="reports-page">
    <section className="tools-card"><Select label="Hesaplama görünümü" value={raporTuru} onChange={e=>setRaporTuru(e.target.value)}><option value="harcama">Harcama · alışveriş ve günlük kart artışları</option><option value="nakit">Nakit hareketleri · ödenen ekstre ve para çıkışları</option></Select><p className="tools-note">{raporTuru==='harcama'?'Ekstre ödemesi yeniden gider sayılmaz. İlk girilen birikmiş kart borcu, tarihi bilinmediği için harcama sayılmaz.':'Kart alışverişleri hariçtir; ekstre ödemeleri ödendiği tarihte, birikime ayrılan para nakit çıkışı olarak görünür.'}</p></section>
    <section className="tools-card report-period"><h3><Icon name="calendar" size={17}/>Dönem raporu</h3>
      <Select label="Rapor aralığı" value={mod} onChange={e=>setMod(e.target.value)}>
        <option value="donem">Maaş dönemi</option><option value="uc">Son 3 maaş dönemi</option><option value="yil">12 maaş dönemi · seçili yıl</option><option value="ozel">Özel tarih aralığı</option>
      </Select>
      {(mod==='donem'||mod==='uc')&&<div className="tools-row report-period-nav"><button className="tools-button" aria-label="Raporda önceki dönem" onClick={()=>setOffset(o=>o-1)}>‹ Önceki</button><button className="tools-button" aria-label="Raporda sonraki dönem" disabled={offset>=0} onClick={()=>setOffset(o=>Math.min(0,o+1))}>Sonraki ›</button></div>}
      {mod==='yil'&&<Input label="Rapor yılı" type="number" min="2000" max="2100" value={yil} onChange={e=>{if(tamSayiGecerli(e.target.value,2000,2100))setYil(Number(e.target.value));}}/>}
      {mod==='ozel'&&<div className="tools-grid"><Input label="Rapor başlangıcı" type="date" value={ilk} onChange={e=>setIlk(e.target.value)}/><Input label="Rapor bitişi" type="date" value={son} onChange={e=>setSon(e.target.value)}/></div>}
      {!aralik?<p role="alert" className="form-alert">Geçerli bir tarih aralığı seç; başlangıç bitişten sonra olamaz.</p>:<p className="tools-note report-range">{aralik.ilk.split('-').reverse().join('.')} – {aralik.son.split('-').reverse().join('.')}</p>}
    </section>
    <div className="report-tabs" role="tablist" aria-label="Rapor bölümleri">
      {[['ozet','Özet','wallet'],['dagilim','Dağılım','tag'],['trend','Trend','chart'],['harita','Harita','calendar'],['yorum','Yorumlar','target']].map(([id,label,icon])=><button key={id} role="tab" id={`report-tab-${id}`} aria-controls="report-panel" aria-selected={sekme===id} tabIndex={sekme===id?0:-1} onKeyDown={e=>{const ids=['ozet','dagilim','trend','harita','yorum'];let idx=ids.indexOf(sekme);if(!['ArrowRight','ArrowLeft','Home','End'].includes(e.key))return;e.preventDefault();idx=e.key==='Home'?0:e.key==='End'?4:(idx+(e.key==='ArrowRight'?1:4))%5;setSekme(ids[idx]);document.getElementById(`report-tab-${ids[idx]}`)?.focus();}} onClick={()=>setSekme(id)}><Icon name={icon} size={16}/>{label}</button>)}
    </div>
    <div role="tabpanel" id="report-panel" aria-labelledby={`report-tab-${sekme}`}>
    {ozet&&<>
      {sekme==='ozet'&&<>
        <section className="tools-card report-overview"><div className="report-stat-grid">{[['Gelir',ozet.gelir,'var(--income-text)','arrowUp'],[raporTuru==='nakit'?'Nakit çıkışı':'Gider',ozet.gider,'var(--expense-text)','arrowDown'],['Net',ozet.net,'var(--text)','wallet']].map(([ad,v,c,icon])=><div className="report-stat" key={ad}><span><Icon name={icon} size={14}/>{ad}</span><strong style={{color:c}}><Money value={v} size={17}/></strong></div>)}</div>
          <div className="savings-result"><span>{raporTuru==='nakit'?'Nakit fazlası oranı':'Gelirden kalan oran'}</span><strong>{ozet.oran==null?'—':`%${ozet.oran.toFixed(1).replace('.',',')}`}</strong></div>
          {onceki&&<div className="report-comparisons"><span>Gelir <strong>{fark(ozet.gelir,onceki.gelir)}</strong></span><span>Gider <strong>{fark(ozet.gider,onceki.gider)}</strong></span><p className="tools-note">Önceki eşit uzunluktaki aralığa göre; bugüne kadar geçen günler karşılaştırılır.</p></div>}
          {!ozet.kayitlar.length&&<div className="empty-state"><Icon name="receipt" size={26}/><p>Bu aralıkta henüz veri yok.</p></div>}
          <details className="calculation-help"><summary>Nasıl hesaplanır?</summary><p className="tools-note">Gelir, birikim kayıtlarını ve döneme dahil edilmeyen kira gelirini içermez. Bekleyen ve ileri tarihli kayıtlar hariçtir. Harcama görünümü günlük kart artışlarını, nakit görünümü ekstre ödemelerini içerir. İlk kart toplamının geçmiş harcama tarihi bilinmez; bu yüzden yeni harcama olarak yazılmaz. Kalan oran gelir ile seçili görünümdeki çıkış arasındaki farkın gelire oranıdır; birikim hesabına aktarılan tutar değildir.</p></details>
        </section>
        {dagilim(giderKategorileri.slice(0,5),ozet.gider,'gider')}
      </>}
      {sekme==='dagilim'&&<>{dagilim(giderKategorileri,ozet.gider,'gider')}{dagilim(gelirKategorileri,ozet.gelir,'gelir')}</>}
      {sekme==='trend'&&<section className="tools-card"><h3><Icon name="chart" size={17}/>Gelir ve gider trendi</h3><p className="tools-note">Trendler seçtiğin hesaplama görünümünü kullanır. Ekstreyi ödediğin tarihte görmek için Nakit hareketleri seç. Harcama görünümünde yalnız yeni kart harcama artışları sayılır.</p>
        {grafik.length&&trendOzeti?.kayitlar.length?<><div className="report-chart"><ResponsiveContainer width="100%" height={220}><LineChart data={grafik} margin={{top:12,right:8,left:0,bottom:8}}><CartesianGrid stroke="var(--border)" vertical={false} strokeDasharray="3 4"/><XAxis dataKey="label" tick={{fontSize:10,fill:'var(--text3)'}} tickLine={false} axisLine={false} minTickGap={24}/><YAxis width={42} tick={{fontSize:10,fill:'var(--text3)'}} tickLine={false} axisLine={false} tickFormatter={v=>v>=1000?`${Math.round(v/1000)}B`:v}/><Tooltip content={({active,payload,label})=>active&&payload?.length?<div className="chart-tooltip"><strong>{label}</strong>{payload.map(p=><div key={p.dataKey}>{p.name}: {fmtTL(p.value)}</div>)}</div>:null}/><Line type="monotone" dataKey="gelir" name="Gelir" stroke="var(--income-text)" strokeWidth={2.3} dot={false}/><Line type="monotone" dataKey="gider" name="Gider" stroke="var(--expense-text)" strokeWidth={2.3} dot={false}/></LineChart></ResponsiveContainer></div><div className="chart-legend"><span><i style={{background:'var(--income-text)'}}/>Gelir</span><span><i style={{background:'var(--expense-text)'}}/>Gider</span></div><details className="calculation-help"><summary>Tutarları listele</summary><div className="trend-table">{grafik.map(g=><div className="tools-row" key={g.key}><span>{g.label}</span><span>{fmtTL(g.gelir)} / {fmtTL(g.gider)}</span></div>)}</div></details></>:<div className="empty-state"><Icon name="chart" size={26}/><p>Trend için henüz veri yok.</p></div>}
      </section>}
      {sekme==='trend'&&<>
        <section className="tools-card"><h3><Icon name="chart" size={17}/>Yıllık kategori karşılaştırması</h3>
          <Input label="Kategori karşılaştırma yılı" type="number" min="2000" max={new Date().getFullYear()} value={kategoriYili} onChange={e=>{if(tamSayiGecerli(e.target.value,2000,new Date().getFullYear()))setKategoriYili(Number(e.target.value));}}/>
          <p className="tools-note">Grafikler seçili rapor aralığından bağımsızdır; takvim aylarına göre yılbaşından bugünün ayına kadar gösterilir. Geçmiş yıllar 12 ay gösterilir. İçinde bulunduğumuz ay tamamlanmadığı için önceki tam ayla karşılaştırması geçicidir.</p>
        </section>
        <YillikKategoriKiyas islemler={trendIslemleri} yil={kategoriYili} tip="gider"/>
        <YillikKategoriKiyas islemler={trendIslemleri} yil={kategoriYili} tip="gelir"/>
      </>}
      {sekme==='trend'&&<HaftaninGunleri gorunum={raporTuru} islemler={islemler} odemeler={odemeler} kartOdemeleri={kartOdemeleri} ilk={aralik.ilk} son={aralik.son}/>}
      {sekme==='harita'&&<HarcamaHaritasi gorunum={raporTuru} islemler={islemler} odemeler={odemeler} kartOdemeleri={kartOdemeleri} ilk={aralik.ilk} son={aralik.son}/>}
      {sekme==='yorum'&&<><section className="tools-card"><h3><Icon name="target" size={17}/>Seçili aralığın yorumu</h3>
        {!ozet.kayitlar.length?<p className="tools-note">Yorum için henüz veri yok.</p>:<>
          <div className="insight-row"><Icon name={ozet.net>=0?'shield':'warning'} size={18}/><p>Bu aralıkta {fmtTL(Math.abs(ozet.net))} {ozet.net>=0?'gelir fazlası':'gider fazlası'} kaydedildi.{ozet.oran!==null?` Tasarruf oranı %${ozet.oran.toFixed(1).replace('.',',')}.`:' Gelir olmadığı için tasarruf oranı hesaplanmaz.'}</p></div>
          {giderKategorileri.length>0&&<div className="insight-row"><Icon name="tag" size={18}/><p>En yüksek gider {giderKategorileri[0][0]}: {fmtTL(giderKategorileri[0][1])}. Bu kategoride %10 azaltma {fmtTL(Math.round(paraKurus(giderKategorileri[0][1])*.1)/100)} pay bırakır.</p></div>}
          {gunSayisi>0&&<div className="insight-row"><Icon name="calendar" size={18}/><p>Günlük kayıtlı gider ortalaması {fmtTL(ozet.gider/gunSayisi)}. Hesap, aralıktaki tüm {gunSayisi} günü içerir.</p></div>}
        </>}
        <details className="calculation-help"><summary>Yorumların dayanağı</summary><p className="tools-note">Yorumlar kayıtlara dayanır; eksik kayıtlar sonucu değiştirir. Bekleyen ödemeler gerçek harcamaya ve nakit çıkışına dahil değildir. %10 azaltma bir senaryodur.</p></details>
      </section>{gelismisYorum&&<details className="legacy-insights"><summary>Gelişmiş dönem ve alışkanlık yorumları</summary><p className="tools-note">Bu yorumlar harcama esasını kullanır; dönem seçimi ayrıdır. Ekstre ödemeleri ikinci gider olarak sayılmaz.</p>{gelismisYorum()}</details>}{birikimRaporu&&<details className="legacy-insights"><summary>Yıllık birikim raporu</summary>{birikimRaporu()}</details>}</>}
      <div className="report-export"><button className="tools-button" disabled={!ozet.kayitlar.length} onClick={()=>csvAktar(ozet.kayitlar,odemeler)}><Icon name="download" size={16}/>Rapor işlemlerini CSV aktar</button></div>
    </>}
    </div>
  </div>;
}

function IslemlerTab({ islemler, setIslemler, setOdemeler, butceler, setButceler, kategoriler, setKategoriler, kartlar, donemGun, odemeler = [], kartOdemeleri = [], setKartOdemeleri }) {
  const { gorunumler: kategoriGorunumleri } = useContext(KategoriGorunumContext);
  const [modal, setModal] = useState(false);
  const [duzenleModal, setDuzenleModal] = useState(false);
  const butceBugun=yerelTarihAnahtari();
  const butceHareketleri=useMemo(()=>gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,'1900-01-01','9999-12-31',butceBugun),[islemler,odemeler,kartOdemeleri,butceBugun]);
  const [butceModal, setButceModal] = useState(false);
  const [katModal, setKatModal] = useState(false);
  const [yeniKat, setYeniKat] = useState({ tip: "gider", isim: "" });
  const [duzenleHedef, setDuzenleHedef] = useState(null);
  const [duzenleForm, setDuzenleForm] = useState({ tip: "gider", kategori: "Market", tutar: "", aciklama: "", tarih: "", donemeDahil: true, donemIci: "", provizyon: "" });
  const [filtre, setFiltre] = useState("hepsi");
  const [arama, setArama] = useState("");
  const [filtreAcik,setFiltreAcik] = useState(false);
  const [kategoriFiltre,setKategoriFiltre] = useState("");
  const [tarihIlk,setTarihIlk] = useState("");
  const [tarihSon,setTarihSon] = useState("");
  const tarihFiltreHatasi = !!(tarihIlk && tarihSon && tarihIlk > tarihSon);
  const filtreKategorileri = [...new Set([...kategoriler.gelir,...kategoriler.gider,...islemler.map(i=>i.kategori),...kartOdemeleri.map(o=>o.kartAdi || 'Kredi kartı')])].sort((a,b)=>a.localeCompare(b,'tr'));
  const [form, setForm] = useState({ tip: "gider", kategori: "Market", tutar: "", aciklama: "", tarih: yerelTarihAnahtari(), donemeDahil: true, donemIci: "", provizyon: "" });
  const [acikDonemler, setAcikDonemler] = useState({});
  const donemToggle = (key) => setAcikDonemler(p => ({ ...p, [key]: !p[key] }));

  const katEkle = () => {
    const isim = yeniKat.isim.trim();
    if (!isim) return;
    const tip = yeniKat.tip;
    if (kategoriler[tip].includes(isim)) {
      toast(`"${isim}" zaten ${tip === "gider" ? "gider" : "gelir"} kategorilerinde var`, "uyari");
      return;
    }
    setKategoriler(k => ({ ...k, [tip]: [...k[tip], isim] }));
    setYeniKat(y => ({ ...y, isim: "" }));
    toast(`"${isim}" kategorisi eklendi (${kategoriler[tip].length + 1}. kategori)`);
  };
  const katSil = (tip, isim) => {
    const varsayilan = VARSAYILAN_KATEGORILER[tip];
    if (varsayilan.includes(isim)) return; // varsayılanları silme
    setKategoriler(k => ({ ...k, [tip]: k[tip].filter(x => x !== isim) }));
  };
  const filtered = [...islemler,...kartOdemeleri.map(o => ({
    id:`kart-odeme:${o.id}`,tip:'gider',kategori:o.kartAdi || 'Kredi kartı',
    tutar:o.tutar,tarih:o.tarih,aciklama:o.aciklama || `${o.kartAdi || 'Kredi kartı'} ekstre ödemesi`,
    kartEkstreOdeme:true,kartOdemeId:o.id,kaynak:'ekstre_odemesi',
  }))]
    .filter(i =>
      !tarihFiltreHatasi &&
      (!kategoriFiltre || i.kategori === kategoriFiltre) &&
      (!tarihIlk || i.tarih >= tarihIlk) && (!tarihSon || i.tarih <= tarihSon) &&
      (filtre === "hepsi" || i.tip === filtre) &&
      (!arama || (i.aciklama || "").toLowerCase().includes(arama.toLowerCase()) || i.kategori.toLowerCase().includes(arama.toLowerCase()))
    )
    .sort((a, b) => yerelTarihOku(b.tarih) - yerelTarihOku(a.tarih));

  const donemGruplari = useMemo(() => {
    const grupMap = {};
    filtered.forEach(i => {
      const dk = maasDonemKey(i.tarih, donemGun);
      const key = dk.key;
      if (!grupMap[key]) {
        grupMap[key] = { key, label: dk.label, islemler: [], gelir: 0, gider: 0, baslangic: dk.baslangic };
      }
      grupMap[key].islemler.push(i);
      if (i.tip === "gelir") { if (!devirIslemiMi(i)) grupMap[key].gelir += i.tutar; }
      else if(!i.kartHarcama && !i.kartEkstreOdeme) grupMap[key].gider += i.tutar;
    });
    return Object.values(grupMap).sort((a, b) => yerelTarihOku(b.baslangic) - yerelTarihOku(a.baslangic));
  }, [filtered, donemGun]);
  const islemDuzenle = () => {
    if (!duzenleHedef) return;
    if (duzenleHedef.kartEkstreOdeme) {
      if (!tutarGecerli(duzenleForm.tutar) || !yerelTarihOku(duzenleForm.tarih)) return formHatasi();
      if (setKartOdemeleri(p => p.map(o => String(o.id) === String(duzenleHedef.kartOdemeId) ? {
        ...o,tutar:paraOku(duzenleForm.tutar),tarih:duzenleForm.tarih,
        aciklama:duzenleForm.aciklama.trim(),odemeDonemKey:maasDonemKey(duzenleForm.tarih,donemGun).key,
      } : o)) === false) return;
      setDuzenleModal(false);setDuzenleHedef(null);toast("Ekstre ödemesi güncellendi");return;
    }
    if (!islemFormuGecerli(duzenleForm, duzenleKartMi)) return;
    const kMi = (kartlar || []).some(k => k.isim === duzenleForm.kategori) && duzenleForm.tip === "gider";
    if (kMi) {
      const di = paraOku(duzenleForm.donemIci) || 0;
      const pr = paraOku(duzenleForm.provizyon) || 0;
      if (di === 0 && pr === 0) return;
      setIslemler(p => p.map(x => x.id === duzenleHedef.id ? kartKaydiniGuncelle(x.kartHarcama ? [x] : [], { ...x, kategori: duzenleForm.kategori, donemIci: di, provizyon: pr, aciklama: duzenleForm.aciklama }, duzenleForm.tarih) : x));
    } else {
      if (!duzenleForm.tutar) return;
      const bagli=odemeler.find(o=>String(o.islemId)===String(duzenleHedef.id)||String(o.id)===String(duzenleHedef.odemeId));
      if(bagli && duzenleForm.tip!=='gider'){formHatasi('Ödeme kaydı gider olarak kalmalı.');return;}
      if(bagli && setOdemeler(p=>p.map(o=>String(o.id)===String(bagli.id)?{...o,tutar:paraOku(duzenleForm.tutar),kategori:duzenleForm.kategori,aciklama:duzenleForm.aciklama,tarih:duzenleForm.tarih}:o))===false)return;
      if(setIslemler(p => p.map(x => x.id === duzenleHedef.id ? { ...x, ...duzenleForm, tutar: paraOku(duzenleForm.tutar), donemIci: undefined, provizyon: undefined, kartHarcama: undefined } : x))===false)return;
    }
    setDuzenleModal(false);
    setDuzenleHedef(null);
    toast("İşlem güncellendi");
  };
  const kartMi = (kartlar || []).some(k => k.isim === form.kategori) && form.tip === "gider";
  const duzenleKartMi = (kartlar || []).some(k => k.isim === duzenleForm.kategori) && duzenleForm.tip === "gider";
  const ekle = () => {
    if (!islemFormuGecerli(form, kartMi)) return;
    if (kartMi) {
      const di = paraOku(form.donemIci) || 0;
      const pr = paraOku(form.provizyon) || 0;
      if (di === 0 && pr === 0) return;
      setIslemler(p => [...p, { id: Date.now(), tip: "gider", kategori: form.kategori, tutar: paraTopla(di, pr), donemIci: di, provizyon: pr, kartHarcama: true, aciklama: form.aciklama, tarih: form.tarih, kartGunBaslangicToplam: paraTopla(di, pr), kartGunBaslangicTarih: form.tarih, kartGunlukDelta: 0, kartGunlukHareketler: {} }]);
    } else {
      if (!form.tutar) return;
      setIslemler(p => [...p, { ...form, id: Date.now(), tutar: paraOku(form.tutar), donemIci: undefined, provizyon: undefined }]);
    }
    setModal(false);
    setForm({ tip: "gider", kategori: "Market", tutar: "", aciklama: "", tarih: yerelTarihAnahtari(), donemeDahil: true, donemIci: "", provizyon: "" });
    toast("İşlem eklendi");
  };
  const islemSil = (hedef) => {
    if (hedef.kartEkstreOdeme) {
      const kayit = kartOdemeleri.find(o => String(o.id) === String(hedef.kartOdemeId));
      if (!kayit || !window.confirm(`${kayit.kartAdi || 'Kredi kartı'} ekstre ödemesi (${fmtTL(kayit.tutar)}) silinsin mi? Kalan ekstre borcu ve bakiye yeniden hesaplanacak.`)) return;
      if (setKartOdemeleri(p => p.filter(o => String(o.id) !== String(kayit.id))) === false) return;
      toast("Ekstre ödemesi silindi","uyari",()=>{
        if (setKartOdemeleri(p => p.some(o => String(o.id) === String(kayit.id)) ? p : [...p,kayit]) === false) return;
        toast("Ekstre ödemesi geri yüklendi");
      });return;
    }
    const bagli=odemeler.find(o=>String(o.islemId)===String(hedef.id)||String(o.id)===String(hedef.odemeId));
    if(bagli && !window.confirm('Bu işleme bağlı ödeme kaydı da silinecek. Devam edilsin mi?'))return;
    if(bagli && setOdemeler(p=>p.filter(o=>String(o.id)!==String(bagli.id)))===false)return;
    if(setIslemler(p => p.filter(x => x.id !== hedef.id))===false)return;
    toast("İşlem silindi", "uyari", () => {
      if(bagli && setOdemeler(p=>p.some(o=>String(o.id)===String(bagli.id))?p:[...p,bagli])===false)return;
      if(setIslemler(p => p.some(x => x.id === hedef.id) ? p : [...p, hedef])===false)return;
      toast("İşlem geri yüklendi");
    });
  };
  return (
    <div className="transactions-page">
      {/* Arama kutusu */}
      <div style={{ position: "relative", marginBottom: 10 }}>
        <span style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text5)", fontSize: 14 }}><Icon name="search" size={17}/></span>
        <input aria-label="İşlem ara" value={arama} onChange={e => setArama(e.target.value)} placeholder="İşlem ara..." style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "9px 12px 9px 36px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
        {arama && <button aria-label="Aramayı temizle" onClick={() => setArama("")} style={{ position: "absolute", right: 10, top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "var(--text4)", cursor: "pointer", fontSize: 14 }}>×</button>}
      </div>
      <div className="filter-toolbar"><button className="tools-button" aria-expanded={filtreAcik} aria-controls="islem-filtreleri" onClick={()=>setFiltreAcik(p=>!p)}><Icon name="filter" size={16}/>Filtreler{(kategoriFiltre||tarihIlk||tarihSon)&&<span className="filter-count">{[kategoriFiltre,tarihIlk,tarihSon].filter(Boolean).length}</span>}</button><span className="tools-note">{filtered.length} işlem</span><button className="tools-button" disabled={!filtered.length} onClick={()=>csvAktar(filtered,odemeler)}><Icon name="download" size={16}/>CSV</button></div>
      <div className="filter-chips">{kategoriFiltre&&<button onClick={()=>setKategoriFiltre('')}>{kategoriFiltre}<Icon name="close" size={12}/></button>}{tarihIlk&&<button onClick={()=>setTarihIlk('')}>Başlangıç: {tarihIlk.split('-').reverse().join('.')}<Icon name="close" size={12}/></button>}{tarihSon&&<button onClick={()=>setTarihSon('')}>Bitiş: {tarihSon.split('-').reverse().join('.')}<Icon name="close" size={12}/></button>}</div>
      {filtreAcik && (<section className="tools-card filter-panel" id="islem-filtreleri"><Select label="Kategori filtresi" value={kategoriFiltre} onChange={e=>setKategoriFiltre(e.target.value)}><option value="">Tüm kategoriler</option>{filtreKategorileri.map(k=><option key={k} value={k}>{k}</option>)}</Select><div className="tools-grid"><Input label="Başlangıç tarihi" type="date" value={tarihIlk} onChange={e=>setTarihIlk(e.target.value)}/><Input label="Bitiş tarihi" type="date" value={tarihSon} onChange={e=>setTarihSon(e.target.value)}/></div>{tarihFiltreHatasi&&<p role="alert">Başlangıç tarihi bitişten sonra olamaz.</p>}<div className="tools-row"><button className="tools-button" onClick={()=>{setKategoriFiltre("");setTarihIlk("");setTarihSon("");setArama("");setFiltre("hepsi");}}>Filtreleri temizle</button><button className="tools-button" disabled={!filtered.length} onClick={()=>csvAktar(filtered,odemeler)}>CSV aktar ({filtered.length})</button></div></section>)}
      <div style={{ display: "flex", gap: 8, marginBottom: 16, justifyContent: "space-between", alignItems: "center" }}>
        <div style={{ display: "flex", gap: 6 }}>
          {["hepsi", "gelir", "gider"].map(f => <button key={f} onClick={() => setFiltre(f)} style={{ padding: "6px 14px", borderRadius: 8, border: "none", cursor: "pointer", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 12, fontWeight: 600, background: filtre === f ? "var(--accent-strong)" : "var(--border)", color: filtre === f ? "var(--on-accent)" : "var(--text3)" }}>{f.charAt(0).toUpperCase() + f.slice(1)}</button>)}
        </div>
        <div style={{ display: "flex", gap: 6 }}>
          <button onClick={() => setKatModal(true)} style={{ background: "var(--border)", border: "none", borderRadius: 10, padding: "8px 10px", color: "var(--text3)", cursor: "pointer", fontSize: 13 }} title="Kategoriler" aria-label="Kategoriler"><Icon name="tag" size={17}/></button>
          <button onClick={() => setButceModal(true)} style={{ background: "var(--border)", border: "none", borderRadius: 10, padding: "8px 10px", color: "var(--text3)", cursor: "pointer", fontSize: 13 }} title="Bütçe Hedefleri" aria-label="Bütçe hedefleri"><Icon name="target" size={17}/></button>
          <Btn onClick={() => setModal(true)}>+ Ekle</Btn>
        </div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {donemGruplari.length === 0 && (
          <div style={{ textAlign: "center", color: "var(--text5)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, padding: "32px 0" }}>İşlem bulunamadı</div>
        )}
        {donemGruplari.map(grup => {
          const acik = acikDonemler[grup.key] === true;
          const net = grup.gelir - grup.gider;
          return (
            <div className="transaction-group" key={grup.key}>
              {/* Dönem başlığı */}
              <button aria-expanded={acik} onClick={() => donemToggle(grup.key)} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: acik ? "12px 12px 0 0" : 12, padding: "10px 14px", display: "flex", alignItems: "center", justifyContent: "space-between", cursor: "pointer" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <span style={{ color: acik ? "#f59e0b" : "var(--text4)", fontSize: 13, transition: "transform 0.2s", display: "inline-block", transform: acik ? "rotate(0deg)" : "rotate(-90deg)" }}>▼</span>
                  <div style={{ textAlign: "left" }}>
                    <div style={{ color: "var(--text)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{grup.label}</div>
                    <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 1 }}>{grup.islemler.length} kayıt · Bakiye için Özet ekranı</div>
                  </div>
                </div>
              </button>
              {/* Dönem işlemleri */}
              {acik && (
                <div style={{ border: "1px solid var(--border)", borderTop: "none", borderRadius: "0 0 12px 12px", overflow: "hidden" }}>
                  {grup.islemler.map((i, idx) => {
                    const renk = i.tip === "gelir" ? "var(--income-text)" : "var(--expense-text)";
                    const katRenk = kategoriGorunumu(i.kategori,i.tip,kategoriGorunumleri).renk;
                    return (
                      <div className="transaction-row" key={i.id} style={{ background: "var(--card)", borderTop: idx > 0 ? "1px solid var(--divider)" : "none", padding: "12px 14px", display: "flex", alignItems: "center", gap: 11 }}>
                        <div style={{ width: 42, height: 42, borderRadius: 13, background: katRenk + "1c", border: `1px solid ${katRenk}3a`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 19, flexShrink: 0 }}>{katIkon(i.kategori, i.tip)}</div>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{i.aciklama || i.kategori}</div>
                          <div style={{ display: "flex", gap: 6, marginTop: 3, alignItems: "center", flexWrap: "wrap" }}>
                            <span style={{ color: katRenk, fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{i.kategori}</span>
                            <span style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>· {i.tarih.split("-").reverse().join(".")}</span>
                            <span className="tools-note">{islemKaynagi(i)}</span>
                            {i.kartHarcama && (i.donemIci > 0 || i.provizyon > 0) && (
                              <span style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>
                                {i.donemIci > 0 ? `Dönem İçi: ₺${fmt(i.donemIci)}` : ""}{i.donemIci > 0 && i.provizyon > 0 ? " · " : ""}{i.provizyon > 0 ? `Provizyon: ₺${fmt(i.provizyon)}` : ""}
                              </span>
                            )}
                            {(() => {
                              if (i.tip !== "gider" || !butceler[i.kategori]) return null;
                              const ayHarcama = butceHareketleri.filter(x => x.tip === "gider" && x.kategori === i.kategori && maasDonemKey(x.tarih, donemGun).key === maasDonemKey(i.tarih, donemGun).key).reduce((s, x) => paraTopla(s, x.tutar), 0);
                              const limit = paraOku(butceler[i.kategori]);
                              if (ayHarcama > limit) return <span style={{ background: "#ef444422", color: "var(--expense-text)", border: "1px solid #ef444444", borderRadius: 6, padding: "1px 6px", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Bütçe Aşıldı!</span>;
                              if (ayHarcama / limit > 0.8) return <span style={{ background: "#f59e0b22", color: "#f59e0b", border: "1px solid #f59e0b44", borderRadius: 6, padding: "1px 6px", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>%{Math.round(ayHarcama / limit * 100)} kullanıldı</span>;
                              return null;
                            })()}
                          </div>
                        </div>
                        <div className="transaction-amount" style={{ textAlign: "right", flexShrink: 0 }}>
                          <div style={{ color: renk, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, fontSize: 14 }}>{i.tip === "gelir" ? "+" : "-"}₺{fmt(i.tutar)}</div>
                          <div style={{ display: "flex", gap: 4, marginTop: 2, justifyContent: "flex-end" }}>
                            <button onClick={() => { setDuzenleHedef(i); setDuzenleForm({ tip: i.tip, kategori: i.kategori, tutar: String(i.tutar), aciklama: i.aciklama || "", tarih: i.tarih || yerelTarihAnahtari(), donemeDahil: i.donemeDahil !== false, donemIci: i.donemIci != null ? String(i.donemIci) : "", provizyon: i.provizyon != null ? String(i.provizyon) : "" }); setDuzenleModal(true); }} style={{ background: "none", border: "none", color: "var(--text4)", cursor: "pointer", fontSize: 13 }} aria-label="Düzenle"><Icon name="edit" size={16}/></button>
                            <button onClick={() => islemSil(i)} style={{ background: "none", border: "none", color: "var(--text5)", cursor: "pointer", fontSize: 13 }} aria-label="Sil"><Icon name="trash" size={16}/></button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>
      {katModal && (
        <Modal title="Kategorileri Yönet" onClose={() => setKatModal(false)}>
          {["gelir", "gider"].map(tip => (
            <div key={tip} style={{ marginBottom: 20 }}>
              <div style={{ color: tip === "gelir" ? "#10b981" : "#ef4444", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 10 }}>
                {tip === "gelir" ? "Gelir Kategorileri" : "Gider Kategorileri"}
              </div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 10 }}>
                {(kategoriler[tip] || []).map(kat => {
                  const varsayilan = VARSAYILAN_KATEGORILER[tip].includes(kat);
                  return (
                    <div key={kat} style={{ display: "flex", alignItems: "center", gap: 4, background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 8, padding: "5px 10px" }}>
                      <KategoriGorunumDuzenle kategori={kat} tip={tip}/>
                      {!varsayilan && (
                        <button onClick={() => katSil(tip, kat)} style={{ background: "none", border: "none", color: "var(--expense-text)", cursor: "pointer", fontSize: 13, lineHeight: 1, padding: 0, marginLeft: 2 }}>×</button>
                      )}
                      {varsayilan && <span style={{ color: "var(--text5)", fontSize: 11, marginLeft: 2 }}>●</span>}
                    </div>
                  );
                })}
              </div>
              <div style={{ display: "flex", gap: 8 }}>
                <input
                  value={yeniKat.tip === tip ? yeniKat.isim : ""}
                  onChange={e => setYeniKat({ tip, isim: e.target.value })}
                  onKeyDown={e => e.key === "Enter" && katEkle()}
                  placeholder={`Yeni ${tip} kategorisi...`}
                  style={{ flex: 1, background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 8, padding: "8px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, outline: "none" }}
                />
                <button onClick={() => { setYeniKat({ tip, isim: yeniKat.tip === tip ? yeniKat.isim : "" }); katEkle(); }} style={{ background: "var(--accent-strong)", border: "none", borderRadius: 8, padding: "8px 14px", color: "var(--on-accent)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, fontSize: 13, cursor: "pointer" }}>+ Ekle</button>
              </div>
            </div>
          ))}
          <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textAlign: "center" }}>● işaretli kategoriler varsayılandır, silinemez</div>
        </Modal>
      )}
      {butceModal && (
        <Modal title="Bütçe Hedefleri" onClose={() => setButceModal(false)}>
          <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 16 }}>Kategoriye aylık limit koy. Aşınca kırmızı uyarı çıkar.</div>
          {(kategoriler?.gider || VARSAYILAN_KATEGORILER.gider).map(kat => {
            const aylikHarcama = butceHareketleri.filter(i => i.tip === "gider" && i.kategori === kat && maasDonemKey(i.tarih, donemGun).key === buAnkiDonem()).reduce((s, i) => paraTopla(s, i.tutar), 0);
            const limit = butceler[kat] || "";
            const asim = limit && aylikHarcama > paraOku(limit);
            return (
              <div key={kat} style={{ marginBottom: 12 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                  <span style={{ color: asim ? "#ef4444" : "var(--text2)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, fontWeight: 600 }}><span className="category-label"><KategoriSimge kategori={kat} tip="gider"/>{kat}</span></span>
                  <span style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Harcanan: ₺{fmt(aylikHarcama)}</span>
                </div>
                <input
                  type="number"
                  placeholder="Limit yok"
                  value={limit}
                  aria-label={`${kat} bütçe limiti`} min="0" step="0.01"
                  onChange={e => {
                    const deger = e.target.value;
                    if (deger === "" || tutarGecerli(deger, true)) setButceler(b => ({ ...b, [kat]: deger }));
                    else formHatasi("Bütçe limiti sıfır veya pozitif olmalı; en fazla iki ondalık basamak kullan.");
                  }}
                  style={{ width: "100%", background: asim ? "#ef444411" : "var(--card2)", border: `1px solid ${asim ? "#ef444466" : "var(--border)"}`, borderRadius: 8, padding: "8px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }}
                />
                {limit && (
                  <div style={{ height: 3, background: "var(--border)", borderRadius: 99, marginTop: 4 }}>
                    <div style={{ height: "100%", width: `${Math.min(aylikHarcama / paraOku(limit) * 100, 100)}%`, background: asim ? "#ef4444" : aylikHarcama / paraOku(limit) > 0.8 ? "#f59e0b" : "#10b981", borderRadius: 99, transition: "width 0.4s" }} />
                  </div>
                )}
              </div>
            );
          })}
        </Modal>
      )}
      {modal && (
        <Modal title="İşlem Ekle" onClose={() => setModal(false)}>
          <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
            {["gider", "gelir"].map(t => <button key={t} onClick={() => setForm(f => ({ ...f, tip: t, kategori: t === "gelir" ? "Maaş" : "Market" }))} style={{ flex: 1, padding: "8px", borderRadius: 8, border: "none", cursor: "pointer", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, fontWeight: 600, background: form.tip === t ? (t === "gelir" ? "#10b981" : "#ef4444") : "var(--border)", color: form.tip === t ? "#fff" : "var(--text3)" }}>{t === "gelir" ? "Gelir" : "Gider"}</button>)}
          </div>
          <Select label="Kategori" value={form.kategori} onChange={e => setForm(f => ({ ...f, kategori: e.target.value }))}>{(kategoriler[form.tip] || []).map(k => <option key={k}>{k}</option>)}</Select>
          {kartMi ? (
            <>
              <Input label="Dönem İçi (₺)" type="number" value={form.donemIci} onChange={e => setForm(f => ({ ...f, donemIci: e.target.value }))} placeholder="0.00" />
              <Input label="Provizyon (₺)" type="number" value={form.provizyon} onChange={e => setForm(f => ({ ...f, provizyon: e.target.value }))} placeholder="0.00" />
            </>
          ) : (
            <Input label="Tutar (₺)" type="number" value={form.tutar} onChange={e => setForm(f => ({ ...f, tutar: e.target.value }))} placeholder="0.00" />
          )}
          <Input label="Açıklama" value={form.aciklama} onChange={e => setForm(f => ({ ...f, aciklama: e.target.value }))} placeholder="Örn: Market alışverişi" />
          <Input label="Tarih" type="date" value={form.tarih} onChange={e => setForm(f => ({ ...f, tarih: e.target.value }))} />
          {form.tip === "gelir" && form.kategori === "Kira Geliri" && (
            <label style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, marginBottom: 4, cursor: "pointer" }}>
              <button type="button" onClick={() => setForm(f => ({ ...f, donemeDahil: !f.donemeDahil }))} style={{ width: 22, height: 22, borderRadius: 6, border: `2px solid ${form.donemeDahil ? "#10b981" : "var(--border2)"}`, background: form.donemeDahil ? "#10b981" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, color: "var(--bg)" }}>{form.donemeDahil ? "✓" : ""}</button>
              <span style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Dönem gelirine dahil et</span>
            </label>
          )}
          <Btn onClick={ekle} style={{ width: "100%", padding: 12, marginTop: 8 }}>Kaydet</Btn>
        </Modal>
      )}
      {duzenleModal && duzenleHedef?.kartEkstreOdeme && (
        <Modal title="Ekstre Ödemesini Düzenle" onClose={()=>{setDuzenleModal(false);setDuzenleHedef(null);}}><KayitGecmisi kayitId={duzenleHedef.kartOdemeId} alan="ft_kart_odemeleri"/>
          <p className="tools-note">{duzenleHedef.kategori} · Tutarı veya tarihi değiştirdiğinde kart borcu, bakiye ve ödeme raporları güncellenir.</p>
          <Input label="Ödenen Tutar (₺)" type="number" value={duzenleForm.tutar} onChange={e=>setDuzenleForm(f=>({...f,tutar:e.target.value}))}/>
          <Input label="Ödeme Tarihi" type="date" value={duzenleForm.tarih} onChange={e=>setDuzenleForm(f=>({...f,tarih:e.target.value}))}/>
          <Input label="Açıklama" value={duzenleForm.aciklama} onChange={e=>setDuzenleForm(f=>({...f,aciklama:e.target.value}))}/>
          <Btn onClick={islemDuzenle} style={{width:'100%'}}>Güncelle</Btn>
        </Modal>
      )}
      {duzenleModal && duzenleHedef && !duzenleHedef.kartEkstreOdeme && (
        <Modal title="İşlemi Düzenle" onClose={() => { setDuzenleModal(false); setDuzenleHedef(null); }}><KayitGecmisi kayitId={duzenleHedef.id} alan="ft_islemler"/>
          <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
            {["gider", "gelir"].map(t => <button key={t} onClick={() => setDuzenleForm(f => ({ ...f, tip: t, kategori: t === "gelir" ? "Maaş" : "Market" }))} style={{ flex: 1, padding: "8px", borderRadius: 8, border: "none", cursor: "pointer", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, fontWeight: 600, background: duzenleForm.tip === t ? (t === "gelir" ? "#10b981" : "#ef4444") : "var(--border)", color: duzenleForm.tip === t ? "#fff" : "var(--text3)" }}>{t === "gelir" ? "Gelir" : "Gider"}</button>)}
          </div>
          <Select label="Kategori" value={duzenleForm.kategori} onChange={e => setDuzenleForm(f => ({ ...f, kategori: e.target.value }))}>{(kategoriler[duzenleForm.tip] || []).map(k => <option key={k}>{k}</option>)}</Select>
          {duzenleKartMi ? (
            <>
              <Input label="Dönem İçi (₺)" type="number" value={duzenleForm.donemIci} onChange={e => setDuzenleForm(f => ({ ...f, donemIci: e.target.value }))} placeholder="0.00" />
              <Input label="Provizyon (₺)" type="number" value={duzenleForm.provizyon} onChange={e => setDuzenleForm(f => ({ ...f, provizyon: e.target.value }))} placeholder="0.00" />
            </>
          ) : (
            <Input label="Tutar (₺)" type="number" value={duzenleForm.tutar} onChange={e => setDuzenleForm(f => ({ ...f, tutar: e.target.value }))} />
          )}
          <Input label="Açıklama" value={duzenleForm.aciklama} onChange={e => setDuzenleForm(f => ({ ...f, aciklama: e.target.value }))} />
          <Input label="Tarih" type="date" value={duzenleForm.tarih} onChange={e => setDuzenleForm(f => ({ ...f, tarih: e.target.value }))} />
          {duzenleForm.tip === "gelir" && duzenleForm.kategori === "Kira Geliri" && (
            <label style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, marginBottom: 4, cursor: "pointer" }}>
              <button type="button" onClick={() => setDuzenleForm(f => ({ ...f, donemeDahil: !f.donemeDahil }))} style={{ width: 22, height: 22, borderRadius: 6, border: `2px solid ${duzenleForm.donemeDahil ? "#10b981" : "var(--border2)"}`, background: duzenleForm.donemeDahil ? "#10b981" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, color: "var(--bg)" }}>{duzenleForm.donemeDahil ? "✓" : ""}</button>
              <span style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Dönem gelirine dahil et</span>
            </label>
          )}
          <Btn onClick={islemDuzenle} disabled={!duzenleForm.tutar} style={{ width: "100%", padding: 12, marginTop: 8 }}>Güncelle</Btn>
        </Modal>
      )}
    </div>
  );
}

// ── ÖDEMELER ──────────────────────────────────────────────────────────────────
function OdemelerTab({ odemeler, setOdemeler, islemler, setIslemler, kategoriler, donemGun }) {
  const { gorunumler: kategoriGorunumleri } = useContext(KategoriGorunumContext);
  const [modal, setModal] = useState(false);
  const [acikDonemler, setAcikDonemler] = useState({});
  const [odemeFiltre, setOdemeFiltre] = useState("bekleyen");
  const bos = { aciklama: "", tutar: "", tarih: yerelTarihAnahtari(), kategori: "Fatura" };
  const [form, setForm] = useState(bos);
  const [duzenleId, setDuzenleId] = useState(null);
  const fmt = n => n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const donemToggle = key => setAcikDonemler(p => ({ ...p, [key]: !p[key] }));

  const kaydet = () => {
    if (!islemFormuGecerli(form)) return;
    if (duzenleId) {
      // Düzenleme: hem ödeme kaydını hem bağlı işlem kaydını güncelle
      const hedef = odemeler.find(x => x.id === duzenleId);
      setOdemeler(p => p.map(x => x.id === duzenleId ? { ...x, aciklama: form.aciklama, tutar: paraOku(form.tutar), tarih: form.tarih, kategori: form.kategori } : x));
      if (hedef) {
        setIslemler(p => p.map(x => x.id === hedef.islemId ? { ...x, kategori: form.kategori, tutar: paraOku(form.tutar), aciklama: form.aciklama || form.kategori, tarih: form.tarih } : x));
      }
      toast("Ödeme güncellendi");
    } else {
      const yeniId = Date.now();
      // İşlemlere de ekle (ödenmedi durumunda bile kaydet, gider olarak)
      const islemId = yeniId + 1;
      setIslemler(p => [...p, {
        id: islemId,
        tip: "gider",
        kategori: form.kategori,
        tutar: paraOku(form.tutar),
        aciklama: form.aciklama || form.kategori,
        tarih: form.tarih,
        kaynak: "odeme",
        odemeId: yeniId,
      }]);
      setOdemeler(p => [...p, {
        id: yeniId,
        aciklama: form.aciklama,
        tutar: paraOku(form.tutar),
        tarih: form.tarih,
        kategori: form.kategori,
        odendi: false,
        islemId,
      }]);
      toast("Ödeme eklendi");
    }
    setModal(false);
    setDuzenleId(null);
    setForm(bos);
  };

  const duzenleAc = (o) => {
    setDuzenleId(o.id);
    setForm({ aciklama: o.aciklama || "", tutar: String(o.tutar), tarih: o.tarih, kategori: o.kategori });
    setModal(true);
  };

  // Eski sürümden gelen ve bağlı işlem kimliği eksik olan ödemeleri de güvenli
  // biçimde bağlı işlem kaydına bağlar. Bekleyen ödeme yalnız ayrılan tutarda
  // kayıt olarak görünür; gerçek nakit çıkışı ödendiği tarihte oluşur.
  const odemeIsleminiBagla = (odeme) => {
    const mevcut = (islemler || []).find(i =>
      (odeme.islemId != null && String(i.id) === String(odeme.islemId)) ||
      (i.odemeId != null && String(i.odemeId) === String(odeme.id))
    );
    const islemId = mevcut?.id ?? odeme.islemId ?? (Date.now() + 1);
    setOdemeler(p => p.map(x => x.id === odeme.id ? { ...x, odendi: true, odendiTarih: new Date().toISOString(), islemId } : x));
    setIslemler(p => {
      const bulundu = p.some(i => String(i.id) === String(islemId));
      if (bulundu) {
        return p.map(i => String(i.id) === String(islemId) ? {
          ...i,
          tip: "gider",
          kategori: odeme.kategori || i.kategori || "Fatura",
          tutar: Number(odeme.tutar) || 0,
          aciklama: odeme.aciklama || odeme.kategori || i.aciklama || "Fatura",
          tarih: odeme.tarih || i.tarih,
          kaynak: "odeme",
          odemeId: odeme.id,
        } : i);
      }
      return [...p, {
        id: islemId,
        tip: "gider",
        kategori: odeme.kategori || "Fatura",
        tutar: Number(odeme.tutar) || 0,
        aciklama: odeme.aciklama || odeme.kategori || "Fatura",
        tarih: odeme.tarih,
        kaynak: "odeme",
        odemeId: odeme.id,
      }];
    });
  };

  const toggleOdendi = (odeme) => {
    if (!odeme.odendi) {
      if(!odemeOnayi(odeme.aciklama||odeme.kategori,odeme.tutar))return;
      odemeIsleminiBagla(odeme);
      toast("Ödendi olarak işaretlendi");
    } else {
      setOdemeler(p => p.map(x => x.id === odeme.id ? { ...x, odendi: false, odendiTarih: undefined } : x));
      toast("Ödendi işareti geri alındı");
    }
  };

  const sil = (odeme) => {
    const bagliIslem = (islemler || []).find(x =>
      (odeme.islemId != null && String(x.id) === String(odeme.islemId)) ||
      (x.odemeId != null && String(x.odemeId) === String(odeme.id))
    );
    setOdemeler(p => p.filter(x => x.id !== odeme.id));
    // İşlemlerdeki ilgili kaydı da sil
    setIslemler(p => p.filter(x =>
      !((odeme.islemId != null && String(x.id) === String(odeme.islemId)) ||
        (x.odemeId != null && String(x.odemeId) === String(odeme.id)))
    ));
    toast("Ödeme silindi", "uyari", () => {
      setOdemeler(p => p.some(x => x.id === odeme.id) ? p : [...p, odeme]);
      if (bagliIslem) setIslemler(p => p.some(x => x.id === bagliIslem.id) ? p : [...p, bagliIslem]);
      toast("Ödeme geri yüklendi");
    });
  };

  const donemGruplari = useMemo(() => {
    const gruplar = [
      {key:'bekleyen',label:'Bekleyen Ödemeler',odemeler:[]},
      {key:'odendi',label:'Ödenmiş',odemeler:[]},
    ];
    odemeler.forEach(o => {
      const durum = o.odendi ? 'odendi' : 'bekleyen';
      if (odemeFiltre !== 'hepsi' && durum !== odemeFiltre) return;
      gruplar.find(g => g.key === durum).odemeler.push(o);
    });
    return gruplar.filter(g => g.odemeler.length).map(g => ({...g,
      toplam:g.odemeler.reduce((n,o) => paraTopla(n,o.tutar),0),
      odendi:g.odemeler.filter(o => o.odendi).reduce((n,o) => paraTopla(n,o.tutar),0),
    }));
  },[odemeler,odemeFiltre]);

  return (
    <div className="payments-page">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
        <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em" }}>
          {odemeler.length} ödeme kaydı
        </div>
        <Btn onClick={() => { setForm(bos); setModal(true); }}>+ Ekle</Btn>
      </div>
      <div className="payment-filters">
        {[{ id: "bekleyen", label: "Bekleyen" }, { id: "odendi", label: "Ödenmiş" }, { id: "hepsi", label: "Tümü" }].map(f => (
          <button key={f.id} onClick={() => setOdemeFiltre(f.id)} style={{ border: odemeFiltre === f.id ? "1px solid rgba(167,78,50,.45)" : "1px solid var(--border)", background: odemeFiltre === f.id ? "var(--accent-soft)" : "var(--card2)", color: odemeFiltre === f.id ? "var(--accent)" : "var(--text4)", borderRadius: 11, padding: "8px", fontSize: 11, fontWeight: 700, cursor: "pointer" }}>{f.label}</button>
        ))}
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {donemGruplari.length === 0 && (
          <div style={{ textAlign: "center", color: "var(--text5)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, padding: "32px 0" }}>{odemeler.length ? "Bu grupta ödeme yok" : "Henüz ödeme kaydı yok"}</div>
        )}
        {donemGruplari.map(grup => {
          const acik = acikDonemler[grup.key] === true;
          const bekleyen = grup.toplam - grup.odendi;
          return (
            <section className="payment-group" data-group={grup.key} key={grup.key}>
              <button aria-expanded={acik} onClick={() => donemToggle(grup.key)} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: acik ? "12px 12px 0 0" : 12, padding: "10px 14px", display: "flex", alignItems: "center", justifyContent: "space-between", cursor: "pointer" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <span style={{ color: acik ? "#f59e0b" : "var(--text4)", fontSize: 13, display: "inline-block", transform: acik ? "rotate(0deg)" : "rotate(-90deg)", transition: "transform 0.2s" }}>▼</span>
                  <div style={{ textAlign: "left" }}>
                    <div style={{ color: "var(--text)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{grup.label}</div>
                    <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 1 }}>
                      {grup.odemeler.length} ödeme · {grup.odemeler.filter(o => o.odendi).length} ödendi
                    </div>
                  </div>
                </div>
                <div style={{ textAlign: "right" }}>
                  <div style={{ color: bekleyen > 0 ? "#ef4444" : "#10b981", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>
                    {bekleyen > 0 ? "-" : "✓"}₺{fmt(bekleyen > 0 ? bekleyen : grup.toplam)}
                  </div>
                  <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 1 }}>
                    {bekleyen > 0 ? `₺${fmt(grup.odendi)} ödendi` : "Tümü ödendi"}
                  </div>
                </div>
              </button>
              {acik && (
                <div style={{ border: "1px solid var(--border)", borderTop: "none", borderRadius: "0 0 12px 12px", overflow: "hidden" }}>
                  {grup.odemeler.sort((a, b) => yerelTarihOku(a.tarih) - yerelTarihOku(b.tarih)).map((o, idx) => {
                    const katRenk = kategoriGorunumu(o.kategori,"gider",kategoriGorunumleri).renk;
                    return (
                      <div className="payment-row" key={o.id} style={{ background: o.odendi ? "var(--header)" : "var(--card)", borderTop: idx > 0 ? "1px solid var(--divider)" : "none", padding: "12px 14px", display: "flex", alignItems: "center", gap: 10, opacity: o.odendi ? 0.5 : 1, transition: "opacity 0.2s" }}>
                        {/* Checkbox */}
                        <button className="payment-check" aria-label={`${o.odendi ? "Ödemeyi geri al" : "Ödendi olarak işaretle"}: ${o.aciklama || o.kategori}`} onClick={() => toggleOdendi(o)} style={{ width: 22, height: 22, borderRadius: 6, border: `2px solid ${o.odendi ? "#10b981" : "var(--border2)"}`, background: o.odendi ? "#10b981" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, color: "var(--bg)", transition: "all 0.2s" }}>
                          <Icon name={o.odendi ? "check" : "receipt"} size={17}/>
                        </button>

                        <div className="payment-description" style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", textDecoration: o.odendi ? "line-through" : "none" }}>
                            {o.aciklama || o.kategori}
                          </div>
                          <div style={{ display: "flex", gap: 6, marginTop: 3, alignItems: "center" }}>
                            <span style={{ color: katRenk, fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{o.kategori}</span>
                            <span style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>· {o.tarih.split("-").reverse().join(".")}</span>
                            <span className={`payment-status ${o.odendi?'paid':o.tarih<yerelTarihAnahtari()?'late':'pending'}`}>{o.odendi?'Ödenmiş':o.tarih<yerelTarihAnahtari()?'Gecikmiş':o.tarih===yerelTarihAnahtari()?'Bugün':`${takvimGunFarki(o.tarih,yerelTarihAnahtari())} gün kaldı`}</span>
                          </div>
                        </div>
                        <div className="payment-amount" style={{ textAlign: "right", flexShrink: 0 }}>
                          <div style={{ color: o.odendi ? "var(--text4)" : "#ef4444", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, fontSize: 14, textDecoration: o.odendi ? "line-through" : "none" }}>
                            ₺{fmt(o.tutar)}
                          </div>
                          <div style={{ display: "flex", gap: 8, marginTop: 3, justifyContent: "flex-end" }}>
                            <button onClick={() => duzenleAc(o)} style={{ background: "none", border: "none", color: "var(--text4)", cursor: "pointer", fontSize: 13 }} aria-label="Düzenle"><Icon name="edit" size={16}/></button>
                            <button onClick={() => sil(o)} style={{ background: "none", border: "none", color: "var(--text5)", cursor: "pointer", fontSize: 13 }} aria-label="Sil"><Icon name="trash" size={16}/></button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          );
        })}
      </div>

      {modal && (
        <Modal title={duzenleId ? "Ödeme Düzenle" : "Ödeme Ekle"} onClose={() => { setModal(false); setDuzenleId(null); }}>
          <Input label="Açıklama (isteğe bağlı)" value={form.aciklama} onChange={e => setForm(f => ({ ...f, aciklama: e.target.value }))} placeholder="Örn: Kira, elektrik..." />
          <Input label="Tutar (₺)" type="number" value={form.tutar} onChange={e => setForm(f => ({ ...f, tutar: e.target.value }))} placeholder="0.00" />
          <Input label="Ödeme Tarihi" type="date" value={form.tarih} onChange={e => setForm(f => ({ ...f, tarih: e.target.value }))} />
          <Select label="Kategori" value={form.kategori} onChange={e => setForm(f => ({ ...f, kategori: e.target.value }))}>
            {(kategoriler?.gider || VARSAYILAN_KATEGORILER.gider).map(k => <option key={k}>{k}</option>)}
          </Select>
          <Btn onClick={kaydet} style={{ width: "100%", padding: 12, marginTop: 8 }}>Kaydet</Btn>
        </Modal>
      )}
    </div>
  );
}


// ── TEKRARLAYAN ───────────────────────────────────────────────────────────────
// ── BİLDİRİM HELPERs ─────────────────────────────────────────────────────────

// ── AYARLAR ───────────────────────────────────────────────────────────────────
function AyarlarTab({ donemGun, setDonemGun, kartlar, setKartlar, kategoriler, setKategoriler, tema, setTema, sonBildirimPlanlama, islemler, setIslemler, kartOdemeleri = [], devirLog, setDevirLog, odemeler = [], tasarrufHedefi, setTasarrufHedefi, grafikTipi, setGrafikTipi, bildirimAyarlari, setBildirimAyarlari, bakiyeHesapModu, setBakiyeHesapModu }) {
  const [bekleyenYedek,setBekleyenYedek]=useState(null);
  const [otomatikler,setOtomatikler]=useState(()=>{try{return JSON.parse(localStorage.getItem('ft_auto_backup_daily_v2')||'[]');}catch{return [];}});
  const [kurtarma,setKurtarma]=useState(()=>{try{return JSON.parse(localStorage.getItem('ft_auto_backup_restore_before_v2')||'null');}catch{return null;}});
  const [bildirimDurum, setBildirimDurum] = useState("");
  const devirHesabi=donemDevriHesapla(islemler,kartlar,kartOdemeleri,Number(donemGun)||15,bakiyeHesapModu,yerelTarihAnahtari(),odemeler);
  const devirKayitlari=islemler.filter(i=>devirIslemiMi(i)&&maasDonemKey(i.tarih,Number(donemGun)||15).key===devirHesabi.donemKey);
  const mevcutDevir=devirKayitlari.reduce((n,i)=>paraTopla(n,i.tutar),0);
  const devirYenile=()=>{
    if(!window.confirm(`Bu dönemin devri ${fmtTL(mevcutDevir)} → ${fmtTL(devirHesabi.tutar)} olarak değiştirilsin mi? Geçmiş dönem kayıtları değişmeyecek.`))return;
    try{localStorage.setItem('ft_auto_backup_devir_v2',JSON.stringify(yerelYedekPaketi()));}catch{toast('Güvenlik yedeği yazılamadı; devir değiştirilmedi.','hata');return;}
    if(setIslemler(p=>[...p.filter(i=>!devirKayitlari.some(d=>String(d.id)===String(i.id))),...(devirHesabi.tutar>0?[{id:`donem-devri:${devirHesabi.donemKey}`,tip:'gelir',kategori:'Önceki aydan devir',kaynak:'donem_devri',aciklama:'Önceki aydan devir',tarih:devirHesabi.tarih,tutar:devirHesabi.tutar,kaynakDonemKey:devirHesabi.kaynakDonemKey}]:[])])===false)return;
    if(setDevirLog(p=>({...p,[devirHesabi.donemKey]:{...devirHesabi,yenidenHesaplandi:new Date().toISOString()}}))===false)return;
    toast('Devir gerçek kapanış bakiyesine göre güncellendi');
  };
  const yedekUygula=()=>{
    try{
      const data=yedekVerisiniDogrula(bekleyenYedek);
      localStorage.setItem('ft_auto_backup_restore_before_v2',JSON.stringify(yerelYedekPaketi()));
      yedegiGuvenleYaz(data);setBekleyenYedek(null);toast('Yedek geri yüklendi');setTimeout(()=>window.location.reload(),500);
    }catch(e){toast(e.message||'Yedek yüklenemedi; mevcut verilerin korundu.','hata');}
  };
  const bildirimTest = async () => {
    setBildirimDurum("Kontrol ediliyor...");
    const LN = bildirimEklentisi();
    if (!LN) {
      setBildirimDurum("❌ Eklenti bulunamadı. Bilgisayarda: npm install @capacitor/local-notifications → npx cap sync android → APK yeniden derle.");
      return;
    }
    const izinli = await bildirimIzniAl(LN);
    if (!izinli) {
      setBildirimDurum("❌ Bildirim izni verilmemiş. Telefon Ayarları → Uygulamalar → Bütçe Takip → Bildirimler'i aç.");
      return;
    }
    const kanalId = await bildirimKanaliHazirla(LN);
    try {
      const t = new Date(Date.now() + 5000);
      await LN.schedule({ notifications: [{
        id: 999999,
        title: "Ödeme Hatırlatması",
        body: "Test bildirimi — sistem çalışıyor ✓",
        schedule: { at: t, allowWhileIdle: true },
        ...(kanalId ? { channelId: kanalId } : {}),
        extra: { finoraTip: "test" },
      }] });
      // Şu an gerçekten planlı olan (bekleyen) bildirim sayısını göster — gerçek ödeme hatırlatmalarının kurulup kurulmadığının kanıtı
      let bekleyenMetin = "";
      try {
        const bekleyen = await LN.getPending();
        const sayi = bekleyen && bekleyen.notifications ? bekleyen.notifications.length : 0;
        bekleyenMetin = ` Şu an sistemde ${sayi} bildirim planlı duruyor (test dahil).`;
      } catch (e) {}
      setBildirimDurum(`✅ Eklenti ve bildirim izni tamam. Test bildirimi 5 saniye sonrasına kaydedildi; alarm izni kapalıysa gecikebilir.${bekleyenMetin}`);
    } catch (e) {
      setBildirimDurum("❌ Planlama hatası: " + (e && e.message ? e.message : "bilinmeyen"));
    }
  };
  const bildirimleriEtkinlestir = async () => {
    const LN = bildirimEklentisi();
    if (!LN) { setBildirimDurum("❌ Arka plan bildirimleri Android uygulamasında kullanılabilir."); return; }
    setBildirimDurum("İzinler kontrol ediliyor...");
    try {
      if (!await bildirimIzniAl(LN)) { setBildirimDurum("❌ Telefon ayarlarından uygulamanın bildirim iznini aç."); return; }
      let izin = await LN.checkExactNotificationSetting();
      if (izin.exact_alarm !== "granted") izin = await LN.changeExactNotificationSetting();
      await bildirimKanaliHazirla(LN);
      setBildirimAyarlari(a => ({ ...a }));
      setBildirimDurum(izin.exact_alarm === "granted"
        ? "✅ Bildirim ve alarm izinleri açık. Hatırlatmalar uygulama kapalıyken de Android tarafından gönderilir."
        : "Kesin alarm izni kapalı; Android bildirimleri geciktirebilir. İzin düğmesiyle tekrar açabilirsin.");
    } catch (e) { setBildirimDurum("❌ " + (e.message || "Bildirim izinleri kontrol edilemedi.")); }
  };
  const [kartIsim, setKartIsim] = useState("");
  const [kartKesimTarihi, setKartKesimTarihi] = useState("");
  const [kartSonOdemeTarihi, setKartSonOdemeTarihi] = useState("");
  const [kartTarihTaslaklari, setKartTarihTaslaklari] = useState({});

  const gunKaydet = (val) => {
    const parsed = parseInt(val);
    const g = isNaN(parsed) ? 15 : Math.min(28, Math.max(1, parsed));
    setDonemGun(String(g));
  };

  const kartEkle = () => {
    const isim = kartIsim.trim();
    if (!isim || !kartKesimTarihi || !kartSonOdemeTarihi) { toast("Kart adı, hesap kesim ve son ödeme tarihini seç", "uyari"); return; }
    if (kartSonOdemeTarihi < kartKesimTarihi) { toast("Son ödeme tarihi hesap kesim tarihinden önce olamaz", "uyari"); return; }
    const donemKey = maasDonemKey(kartSonOdemeTarihi, parseInt(donemGun) || 15).key;
    const yeniKart = {
      id: Date.now(), isim,
      hesapKesimTarihi: kartKesimTarihi,
      sonOdemeTarihi: kartSonOdemeTarihi,
      donemTarihleri: { [donemKey]: { hesapKesimTarihi: kartKesimTarihi, sonOdemeTarihi: kartSonOdemeTarihi } },
    };
    setKartlar(p => [...p, yeniKart]);
    // Kart adıyla tek kategori oluştur
    setKategoriler(k => ({ ...k, gider: [...(k.gider || []).filter(x => x !== isim), isim] }));
    setKartIsim("");
    setKartKesimTarihi("");
    setKartSonOdemeTarihi("");
  };

  const kartSil = (kart) => {
    setKartlar(p => p.filter(x => x.id !== kart.id));
    setKategoriler(k => ({ ...k, gider: (k.gider || []).filter(x => x !== kart.isim) }));
    setKartTarihTaslaklari(p => {
      const yeni = { ...p };
      delete yeni[kart.id];
      return yeni;
    });
  };

  const kartTarihTaslagiDegistir = (kart, alan, yeniTarih) => {
    setKartTarihTaslaklari(p => ({
      ...p,
      [kart.id]: {
        hesapKesimTarihi: p[kart.id]?.hesapKesimTarihi ?? kart.hesapKesimTarihi ?? "",
        sonOdemeTarihi: p[kart.id]?.sonOdemeTarihi ?? kart.sonOdemeTarihi ?? "",
        [alan]: yeniTarih,
      },
    }));
  };

  const kartTarihleriniKaydet = (kart) => {
    const taslak = kartTarihTaslaklari[kart.id] || {
      hesapKesimTarihi: kart.hesapKesimTarihi || "",
      sonOdemeTarihi: kart.sonOdemeTarihi || "",
    };
    if (!taslak.hesapKesimTarihi || !taslak.sonOdemeTarihi) { toast("Hesap kesim ve son ödeme tarihini seç", "uyari"); return; }
    if (taslak.sonOdemeTarihi < taslak.hesapKesimTarihi) { toast("Son ödeme tarihi hesap kesim tarihinden önce olamaz", "uyari"); return; }
    setKartlar(p => p.map(kayit => {
      if (String(kayit.id) !== String(kart.id)) return kayit;
      const arsiv = { ...(kayit.donemTarihleri || {}) };
      const arsivle = (hesapKesimTarihi, sonOdemeTarihi) => {
        if (!sonOdemeTarihi || !yerelTarihOku(sonOdemeTarihi)) return;
        const key = maasDonemKey(sonOdemeTarihi, parseInt(donemGun) || 15).key;
        arsiv[key] = { hesapKesimTarihi: hesapKesimTarihi || "", sonOdemeTarihi };
      };
      arsivle(kayit.hesapKesimTarihi, kayit.sonOdemeTarihi);
      arsivle(taslak.hesapKesimTarihi, taslak.sonOdemeTarihi);
      return { ...kayit, ...taslak, donemTarihleri: arsiv };
    }));
    setKartTarihTaslaklari(p => {
      const yeni = { ...p };
      delete yeni[kart.id];
      return yeni;
    });
    toast(`${kart.isim} tarihleri güncellendi`);
  };

  const bildirimGunuDegistir = (gun) => {
    const mevcut = bildirimAyarlari?.gunler || [0];
    const yeni = mevcut.includes(gun) ? mevcut.filter(g => g !== gun) : [...mevcut, gun].sort((a, b) => b - a);
    if (yeni.length === 0) { toast("En az bir hatırlatma zamanı seçilmeli", "uyari"); return; }
    setBildirimAyarlari(a => ({ ...(a || {}), gunler: yeni }));
  };

  const yedekIndir = async () => {
    try {
      const data = {};
      for (let i = 0; i < window.localStorage.length; i++) {
        const key = window.localStorage.key(i);
        if (key && key.startsWith("ft_") && !key.startsWith("ft_manual_backup") && !key.startsWith("ft_auto_backup")) {
          data[key] = JSON.parse(window.localStorage.getItem(key));
        }
      }
      const paket = { format: "finora-v2", surum: "1.7.0", olusturulma: new Date().toISOString(), data };
      const json = JSON.stringify(paket, null, 2);

      // Önce uygulama içinde güvenli bir kopya tut. Android WebView indirmeyi
      // desteklemese bile yedek kaybolmasın ve Yedeği Yükle bunu kullanabilsin.
      let yerelKopyaKaydedildi = true;
      try {
        window.localStorage.setItem("ft_manual_backup_latest", JSON.stringify(paket));
      } catch {
        yerelKopyaKaydedildi = false;
      }

      const fileName = `finora-yedek-${yerelTarihAnahtari()}.json`;
      const blob = new Blob([json], { type: "application/json" });

      // Android/webview destekliyorsa doğrudan paylaş/kaydet ekranını aç.
      if (navigator.share && typeof File !== "undefined") {
        try {
          const file = new File([blob], fileName, { type: "application/json" });
          if (!navigator.canShare || navigator.canShare({ files: [file] })) {
            await navigator.share({ title: "Bütçe Takip yedeği", text: "Bütçe Takip veri yedeği", files: [file] });
            toast(yerelKopyaKaydedildi ? "Yedek oluşturuldu ve paylaş/kaydet ekranı açıldı" : "Dosya paylaşım ekranı açıldı; uygulama içi kopya kaydedilemedi.", yerelKopyaKaydedildi ? "ok" : "uyari");
            return;
          }
        } catch (e) {
          if (e && e.name === "AbortError") { toast(yerelKopyaKaydedildi ? "Yedek uygulama içinde kaydedildi; paylaşım iptal edildi." : "Paylaşım iptal edildi ve yerel kopya kaydedilemedi. Yedek almayı tekrar dene.", yerelKopyaKaydedildi ? "ok" : "hata"); return; }
        }
      }

      // Normal tarayıcı fallback'i: Dosyalar/İndirilenler klasörüne indirme.
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName;
      a.style.display = "none";
      document.body.appendChild(a);
      a.click();
      setTimeout(() => { a.remove(); URL.revokeObjectURL(url); }, 1000);
      toast(yerelKopyaKaydedildi ? "Yerel kopya kaydedildi; dosya indirme isteği gönderildi." : "Yerel kopya kaydedilemedi; dosya indirme isteği gönderildi. Dosyanın kaydedildiğini kontrol et.", yerelKopyaKaydedildi ? "ok" : "uyari");
    } catch (e) {
      toast("Yedek oluşturulamadı", "hata");
    }
  };

  const yedekYukle = async (event) => {
    const file = event.target.files && event.target.files[0];
    event.target.value = "";
    if (!file) return;
    try {
      if (file.size > 10 * 1024 * 1024) throw new Error("Yedek dosyası 10 MB sınırını aşıyor.");
      const paket = JSON.parse(await file.text());
      const data = yedekVerisiniDogrula(paket);
      setBekleyenYedek({...paket,data});
    } catch (e) {
      toast(e instanceof SyntaxError ? "Dosya geçerli JSON değil; mevcut kayıtlar değiştirilmedi." : (e.message || "Yedek yüklenemedi; mevcut kayıtlar değiştirilmedi."), "hata");
    }
  };

  return (
    <div>
      <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 16 }}>Ayarlar</div>

      {/* Tema seçici */}
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 14 }}>
        <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 12 }}><span className="inline-icon"><Icon name="sun" size={16}/></span>Görünüm</div>
        <div style={{ display: "flex", gap: 8 }}>
          {[{id:"dark",label:"Koyu",bg:"#16161f",fg:"#f1f0ee"},{id:"light",label:"Açık",bg:"#ffffff",fg:"#16243f"},{id:"gray",label:"Gri",bg:"#cdd2d8",fg:"#14202e"}].map(t => (
            <button key={t.id} onClick={() => setTema(t.id)} style={{ flex: 1, padding: "14px 8px", borderRadius: 12, cursor: "pointer", border: tema === t.id ? "2px solid #A74E32" : "2px solid var(--border)", background: t.bg, color: t.fg, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, fontWeight: 600, transition: "all 0.2s" }}>
              <span className="inline-icon"><Icon name={t.id==='dark'?'moon':'sun'} size={16}/></span>{t.label}
            </button>
          ))}
        </div>
      </div>

      <section className="tools-card"><h3><Icon name="chart" size={17}/>Bütçe grafiği</h3><p className="tools-note">Genel Bakış ekranında halka veya ibre göstergesini kullan. Seçimin sonraki açılışlarda korunur.</p><div className="gauge-options">{[{id:'halka',label:'Halka',note:'Kompakt dairesel gösterge'},{id:'ibre',label:'İbre',note:'Renkli ölçek ve hareketli ibre'}].map(t=><button type="button" key={t.id} aria-pressed={grafikTipi===t.id} onClick={()=>setGrafikTipi(t.id)}><strong>{t.label}</strong><span>{t.note}</span></button>)}</div></section>
      <KategoriGorunumAyarlari kategoriler={kategoriler}/>
      <section className="tools-card"><h3>Önceki aydan devir</h3><p className="tools-note">Yalnız önceki dönemin artı gerçek kapanış bakiyesi devredilir. Bekleyen borçlar ve kart alışverişleri nakit çıkışı sayılmaz. Devir gelir raporuna eklenmez. Eski devirler kendiliğinden değiştirilmez.</p><div className="tools-row"><span>Bu dönemde kayıtlı devir</span><strong>{fmtTL(mevcutDevir)}</strong></div><div className="tools-row"><span>Önceki dönem gerçek kapanış</span><strong>{fmtTL(devirHesabi.kapanisBakiyesi)}</strong></div><p className="tools-note">Yeni devir önerisi: {fmtTL(devirHesabi.tutar)}. Geçmiş verileri düzelttiğinde buradan kontrol ederek güncelle.</p><Btn onClick={devirYenile}>Devri yeniden hesapla</Btn></section>
      <section className="tools-card"><h3>Kayıt geçmişi</h3><p className="tools-note">Eski, ödeme bağlantısı olmayan geçmiş gider kayıtları gerçekleşmiş kabul edilir. Yeni sabit giderler Ödemeler listesine bekleyen olarak gelir; ödendiğini işaretlemeden gerçek bakiye azalmaz.</p><KayitGecmisi/></section>
      <TasarrufHedefi hedef={tasarrufHedefi} setHedef={setTasarrufHedefi}/>
      <section className="tools-card"><h3>Excel / CSV aktarımı</h3><p className="tools-note">Tüm işlemlerini Türkçe sütunlar ve kuruş hassasiyetiyle aktar. İşlemler ekranında filtrelenmiş kayıtları da aktarabilirsin. CSV tam yedeğin yerine geçmez.</p><Btn onClick={()=>csvAktar(islemler,odemeler)} disabled={!islemler.length}>Tüm işlemleri CSV aktar</Btn></section>
      {/* Veri yedekleme */}
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 14 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 7 }}>
          <span style={{ width: 36, height: 36, borderRadius: 11, display: "grid", placeItems: "center", background: "var(--accent-soft)", color: "var(--accent)" }}><Icon name="wallet" size={18} /></span>
          <div>
            <div style={{ color: "var(--text)", fontSize: 13, fontWeight: 700 }}>Veri Yedekleme</div>
            <div style={{ color: "var(--text4)", fontSize: 11, marginTop: 2 }}>Telefon değiştirirken verilerinizi koruyun</div>
          </div>
        </div>
        <div style={{ color: "var(--text4)", fontSize: 11, lineHeight: 1.55, marginBottom: 13 }}>
          Tüm işlemler, kartlar, bütçeler ve ayarlar tek bir dosyada saklanır. Yedek önce uygulama içinde güvenli kopya olarak tutulur; cihaz destekliyorsa ayrıca dosya olarak kaydetme/paylaşma ekranı açılır.
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 7, background: "#10b98114", border: "1px solid #10b98135", color: "var(--income-text)", borderRadius: 10, padding: "8px 10px", fontSize: 11, fontWeight: 600, marginBottom: 11 }}><span>✓</span><span>Veri uyumluluğu ve otomatik güvenlik kopyası etkin</span></div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
          <button onClick={yedekIndir} style={{ border: "none", borderRadius: 12, padding: "11px 10px", background: "linear-gradient(135deg,var(--accent),var(--accent-strong))", color: "#fff", fontWeight: 700, fontSize: 12, cursor: "pointer" }}>Yedek Al</button>
          <label style={{ borderRadius: 12, padding: "10px", background: "var(--card2)", border: "1px solid var(--border)", color: "var(--text2)", fontWeight: 700, fontSize: 12, cursor: "pointer", textAlign: "center" }}>
            Yedeği Yükle
            <input type="file" accept="application/json,.json" onChange={yedekYukle} style={{ display: "none" }} />
          </label>
        </div>
      </div>

      <section className="tools-card"><h3>Otomatik güvenlik kopyaları</h3><p className="tools-note">Son 3 kayıt gününün kopyası yalnız bu cihazda tutulur. Uygulamayı silmek veya cihazı kaybetmek bunları da silebilir; dosya yedeğini ayrıca sakla.</p><Btn onClick={()=>{try{setOtomatikler(otomatikYedekle());toast('Güvenlik kopyası güncellendi');}catch{toast('Güvenlik kopyası yazılamadı; dosya yedeği al.','hata');}}}>Şimdi güvenlik kopyası al</Btn>{otomatikler.slice().reverse().map(p=><div className="tools-row" key={p.gun}><span>{p.gun} · {p.data.ft_islemler?.length||0} işlem</span><Btn onClick={()=>{try{setBekleyenYedek({...p,data:yedekVerisiniDogrula(p)});}catch(e){toast(e.message,'hata');}}}>Önizle</Btn></div>)}{kurtarma&&<Btn onClick={()=>{try{setBekleyenYedek({...kurtarma,data:yedekVerisiniDogrula(kurtarma)});}catch(e){toast(e.message,'hata');}}}>Son yüklemeden önceki verileri önizle</Btn>}</section>
      {bekleyenYedek&&<YedekOnizleme paket={bekleyenYedek} onVazgec={()=>setBekleyenYedek(null)} onYukle={yedekUygula}/>}
      {/* Bildirim testi */}
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 14 }}>
        <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 6 }}><span className="inline-icon"><Icon name="bell" size={16}/></span>Bildirimler</div>
        <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 12 }}>
          Aynı zamana denk gelen faturalar, kredi kartları ve taksitler tek bildirimde birleşir. Hatırlatma zamanlarını seçebilirsin.
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 7, marginBottom: 10 }}>
          {[{ gun: 3, label: "3 gün önce" }, { gun: 1, label: "1 gün önce" }, { gun: 0, label: "Aynı gün" }].map(secenek => {
            const aktif = (bildirimAyarlari?.gunler || [0]).includes(secenek.gun);
            return <button key={secenek.gun} onClick={() => bildirimGunuDegistir(secenek.gun)} style={{ border: aktif ? "1px solid rgba(167,78,50,.5)" : "1px solid var(--border)", background: aktif ? "var(--accent-soft)" : "var(--card2)", color: aktif ? "var(--accent)" : "var(--text4)", borderRadius: 11, padding: "9px 5px", fontSize: 11, fontWeight: 700, cursor: "pointer" }}>{aktif ? "✓ " : ""}{secenek.label}</button>;
          })}
        </div>
        {bildirimSaatleriniAl(bildirimAyarlari).map((saat, i, saatler) => <div key={i} style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <div style={{ flex: 1 }}><Input label={`Bildirim Saati ${i + 1}`} type="time" value={saat} onChange={e => {
            if (!e.target.value) return;
            const yeni = saatler.map((s, j) => i === j ? e.target.value : s);
            setBildirimAyarlari(a => ({ ...a, saat: yeni[0], saatler: yeni }));
          }} /></div>
          {saatler.length > 1 && <Btn onClick={() => { const yeni = saatler.filter((_, j) => j !== i); setBildirimAyarlari(a => ({ ...a, saat: yeni[0], saatler: yeni })); }} aria-label={`${saat} bildirim saatini kaldır`}>Kaldır</Btn>}
        </div>)}
        {bildirimSaatleriniAl(bildirimAyarlari).length < 3 && <Btn onClick={() => {
          const saatler = bildirimSaatleriniAl(bildirimAyarlari);
          const yeni = [...saatler, ["09:00", "12:00", "18:00", "21:00"].find(s => !saatler.includes(s))];
          setBildirimAyarlari(a => ({ ...a, saat: yeni[0], saatler: yeni }));
        }}>Saat Ekle</Btn>}
        <Btn onClick={bildirimleriEtkinlestir} style={{ width: "100%", padding: 12, marginTop: 8 }}>Arka Plan Bildirimlerini Etkinleştir</Btn>
        <Btn onClick={bildirimTest} style={{ width: "100%", padding: 12, marginTop: 8 }}><span className="inline-icon"><Icon name="bell" size={16}/></span>Bildirim Testi Yap</Btn>
        <div style={{ color: "var(--text4)", fontSize: 11, lineHeight: 1.5, marginTop: 7 }}>
          Seçilen saat için bildirim ve alarm izinlerini aç. Uygulamayı son uygulamalar ekranından kapatabilirsin; Android ayarlarından zorla durdurmak alarmları engeller. Bazı telefonlarda pil kısıtlamalarını da kaldırmak gerekir.
        </div>
        {bildirimDurum && (
          <div style={{ marginTop: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, color: bildirimDurum.startsWith("✅") ? "#10b981" : bildirimDurum.startsWith("❌") ? "#ef4444" : "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", lineHeight: 1.5 }}>
            <span className="inline-icon"><Icon name={bildirimDurum.startsWith("❌") ? "alert" : "check"} size={16}/></span>{bildirimDurum.replace(/^[❌✅]\s*/, "")}
          </div>
        )}
        {sonBildirimPlanlama && (
          <div style={{ marginTop: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, border: "1px solid var(--border)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", lineHeight: 1.6 }}>
            <div style={{ color: "var(--text4)" }}>Son otomatik planlama: {new Date(sonBildirimPlanlama.zaman).toLocaleString("tr-TR")}</div>
            {sonBildirimPlanlama.hata
              ? <div style={{ color: "var(--expense-text)" }}><span className="inline-icon"><Icon name="alert" size={16}/></span>Hata: {sonBildirimPlanlama.hata}</div>
              : <div style={{ color: sonBildirimPlanlama.sayi > 0 ? "#10b981" : "var(--text3)" }}>
                  {sonBildirimPlanlama.sayi > 0
                    ? `✓ Telefonda doğrulanan ${sonBildirimPlanlama.sayi} birleşik bildirim bekliyor`
                    : "Planlanacak gelecek tarihli veya bugün ödenmemiş bildirim yok"}
                  {sonBildirimPlanlama.odemeSayisi != null ? ` · ${sonBildirimPlanlama.odemeGunuSayisi || 0} ödeme günü · ${sonBildirimPlanlama.odemeSayisi} ödeme` : ""}
                  {sonBildirimPlanlama.kartSayisi ? ` · ${sonBildirimPlanlama.kartSayisi} kredi kartı` : ""}
                </div>}
            {sonBildirimPlanlama.telafiSayisi > 0 && <div style={{ color: "#f59e0b" }}>↻ Saati geçen {sonBildirimPlanlama.telafiSayisi} aynı-gün bildirimi telafi olarak kuruldu.</div>}
            {sonBildirimPlanlama.uyari && <div style={{ color: "#f59e0b" }}><span className="inline-icon"><Icon name="warning" size={16}/></span>{sonBildirimPlanlama.uyari}</div>}
          </div>
        )}
      </div>

      {/* Dönem başlangıç günü */}
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 14 }}>
        <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 6 }}><span className="inline-icon"><Icon name="calendar" size={16}/></span>Maaş Dönemi Başlangıcı</div>
        <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 14 }}>
          Dönem her ayın kaçında başlasın? Bitiş bir önceki gün olur.
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <input
            type="number"
            min="1"
            max="28"
            value={donemGun}
            onChange={e => gunKaydet(e.target.value)}
            style={{ width: 70, background: "var(--card2)", border: "1px solid #f59e0b44", borderRadius: 10, padding: "10px 12px", color: "#f59e0b", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 18, fontWeight: 600, outline: "none", textAlign: "center" }}
          />
          <div>
            <div style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>
              Her ayın <span style={{ color: "#f59e0b", fontWeight: 600 }}>{donemGun}. günü</span> başlar
            </div>
            <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 3 }}>
              Bitiş: ayın {(parseInt(donemGun) || 15) - 1}. günü
            </div>
          </div>
        </div>
      </div>

      {/* Kredi Kartları */}
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 14 }}>
        <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 6 }}><span className="inline-icon"><Icon name="card" size={16}/></span>Kredi Kartları</div>
        <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 14 }}>
          Hesap kesim ve son ödeme tarihini takvimden gün–ay–yıl olarak seç. Tarihler ait oldukları maaş döneminde saklanır ve bildirim son ödeme tarihine göre gönderilir.
        </div>
        {turkceAdaGoreSirala(kartlar || []).map(k => {
          const taslak = kartTarihTaslaklari[k.id] || {
            hesapKesimTarihi: k.hesapKesimTarihi || "",
            sonOdemeTarihi: k.sonOdemeTarihi || "",
          };
          const degisti = taslak.hesapKesimTarihi !== (k.hesapKesimTarihi || "") || taslak.sonOdemeTarihi !== (k.sonOdemeTarihi || "");
          return (
          <div key={k.id} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, background: "var(--card2)", borderRadius: 10, padding: "10px 12px", marginBottom: 8, border: "1px solid var(--border)" }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 4 }}><span className="inline-icon"><Icon name="card" size={16}/></span>{k.isim}</div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
                <label>
                  <span style={{ display: "block", color: "var(--text4)", fontSize: 11, marginBottom: 4 }}>Hesap Kesim Tarihi</span>
                  <input aria-label={`${k.isim} hesap kesim tarihi`} type="date" value={taslak.hesapKesimTarihi} onChange={e => kartTarihTaslagiDegistir(k, "hesapKesimTarihi", e.target.value)} style={{ width: "100%", minWidth: 0, boxSizing: "border-box", background: "var(--card)", border: "1px solid var(--border)", borderRadius: 7, padding: "6px", color: "var(--text)", fontSize: 11, fontWeight: 600, outline: "none" }} />
                </label>
                <label>
                  <span style={{ display: "block", color: "var(--text4)", fontSize: 11, marginBottom: 4 }}>Son Ödeme Tarihi</span>
                  <input aria-label={`${k.isim} son ödeme tarihi`} type="date" value={taslak.sonOdemeTarihi} onChange={e => kartTarihTaslagiDegistir(k, "sonOdemeTarihi", e.target.value)} style={{ width: "100%", minWidth: 0, boxSizing: "border-box", background: "var(--card)", border: "1px solid #10b98144", borderRadius: 7, padding: "6px", color: "var(--income-text)", fontSize: 11, fontWeight: 600, outline: "none" }} />
                </label>
              </div>
              <button onClick={() => kartTarihleriniKaydet(k)} disabled={!degisti} style={{ width: "100%", marginTop: 8, border: degisti ? "1px solid #A74E3255" : "1px solid var(--border)", borderRadius: 8, padding: "7px 10px", background: degisti ? "linear-gradient(135deg,#A74E32,#4f46e5)" : "var(--card)", color: degisti ? "#fff" : "var(--text5)", fontSize: 11, fontWeight: 700, cursor: degisti ? "pointer" : "default", opacity: degisti ? 1 : 0.7 }}>Kaydet</button>
            </div>
            <button onClick={() => kartSil(k)} style={{ background: "none", border: "none", color: "var(--expense-text)", cursor: "pointer", fontSize: 16, marginLeft: 8 }} aria-label="Sil"><Icon name="trash" size={16}/></button>
          </div>
          );
        })}
        {(kartlar || []).length === 0 && (
          <div style={{ color: "var(--text5)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textAlign: "center", padding: "8px 0 12px" }}>Henüz kart eklenmedi</div>
        )}
        <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: 8, marginTop: 4 }}>
          <input
            value={kartIsim}
            onChange={e => setKartIsim(e.target.value)}
            onKeyDown={e => e.key === "Enter" && kartEkle()}
            placeholder="Kart adı (World, Yapı Kredi World, Bonus...)"
            style={{ minWidth: 0, background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 8, padding: "9px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 12, outline: "none" }}
          />
          <input
            aria-label="Yeni kart hesap kesim tarihi"
            type="date"
            value={kartKesimTarihi}
            onChange={e => setKartKesimTarihi(e.target.value)}
            style={{ minWidth: 0, background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 8, padding: "9px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 11, outline: "none" }}
          />
          <input
            aria-label="Yeni kart son ödeme tarihi"
            type="date"
            value={kartSonOdemeTarihi}
            onChange={e => setKartSonOdemeTarihi(e.target.value)}
            style={{ minWidth: 0, background: "var(--card2)", border: "1px solid #10b98144", borderRadius: 8, padding: "9px", color: "var(--income-text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 11, outline: "none" }}
          />
        </div>
        <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 4, marginBottom: 10 }}>Kart adı · Hesap kesim tarihi · Son ödeme tarihi</div>
        <button onClick={kartEkle} style={{ width: "100%", background: "var(--accent-strong)", border: "none", borderRadius: 10, padding: "10px", color: "#fff", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, fontSize: 13, cursor: "pointer" }}>+ Kart Ekle</button>
      </div>

      <div style={{ textAlign: "center", color: "var(--border)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 24 }}>
        Bütçe Takip v1.6.9
      </div>
    </div>
  );
}


// ── ANALİZ ────────────────────────────────────────────────────────────────────
function AnalizTab({ islemler, kategoriler, donemGun, kartlar, odemeler = [], kartOdemeleri = [], bakiyeHesapModu = "kartlar_dahil" }) {
  const [secilenYil, setSecilenYil] = useState(new Date().getFullYear());
  const [gorunum, setGorunum] = useState("gider");
  const [analizSekme, setAnalizSekme] = useState("yorum");
  const [yorumDonemOffset, setYorumDonemOffset] = useState(0);
  const [trendSecimleri, setTrendSecimleri] = useState({});

  const yorumDonemi = useMemo(() => {
    const gun = donemGun || 15;
    const bugunkuDonem = maasDonemiBul(yerelTarihAnahtari(), gun);
    const baslangic = new Date(bugunkuDonem.yil, bugunkuDonem.ay + yorumDonemOffset, gun);
    const bitis = new Date(baslangic.getFullYear(), baslangic.getMonth() + 1, gun - 1);
    return {
      yil: baslangic.getFullYear(),
      ay: baslangic.getMonth(),
      key: `${baslangic.getFullYear()}-${baslangic.getMonth()}-${gun}`,
      baslangic,
      bitis,
      label: `${kisaTarih(baslangic)} – ${kisaTarih(bitis)}`,
    };
  }, [donemGun, yorumDonemOffset]);

  // Raporlar kendi hesaplarını kullanır; işlem ve ödeme kayıtları değiştirilmez.
  const bugunKey = yerelTarihAnahtari();
  const raporIslemleri=useMemo(()=>gunlukFinansHareketleri(islemler,odemeler,kartOdemeleri,'1900-01-01','9999-12-31',bugunKey,'harcama'),[islemler,odemeler,kartOdemeleri,bugunKey]);
  const gelirDahil = i => i.tip === "gelir" && i.kategori !== "Birikim"
    && !(i.kategori === "Kira Geliri" && i.donemeDahil === false);
  const habitRaporHareketleri = useMemo(() => raporIslemleri
    .filter(i => i.tip === "gider")
    .flatMap(i => {
      if (i.kaynak !== "odeme") return habitHareketleri(i);
      const odeme = odemeler.find(o => String(o.id) === String(i.odemeId)
        || (o.islemId != null && String(o.islemId) === String(i.id)));
      if (!odeme?.odendi) return [];
      const gercekTarih = odeme.odendiTarih ? new Date(odeme.odendiTarih) : yerelTarihOku(odeme.tarih);
      if (!gercekTarih || !Number.isFinite(gercekTarih.getTime())) return [];
      return habitHareketleri({ ...i, tarih: yerelTarihAnahtari(gercekTarih) });
    })
    .filter(i => yerelTarihOku(i.tarih) && i.tarih <= bugunKey && i.tutar > 0),
    [raporIslemleri, odemeler, bugunKey]);

  const yilIslemleri = useMemo(
    () => raporIslemleri.filter(i => yerelTarihOku(i.tarih)?.getFullYear() === secilenYil && i.tip === gorunum),
    [raporIslemleri, secilenYil, gorunum]
  );
  const toplamTutar = yilIslemleri.reduce((s, i) => paraTopla(s, i.tutar), 0);

  const kategoriVerisi = useMemo(() => {
    const map = {};
    yilIslemleri.forEach(i => { map[i.kategori] = paraTopla(map[i.kategori] || 0, i.tutar); });
    return Object.entries(map)
      .map(([name, value]) => ({ name, value, pct: toplamTutar > 0 ? (value / toplamTutar * 100).toFixed(1) : 0 }))
      .sort((a, b) => b.value - a.value);
  }, [yilIslemleri, toplamTutar]);

  const aylikVerisi = useMemo(() => {
    const gun = donemGun || 15;
    const donemler = [];
    for (let m = 0; m < 12; m++) donemler.push({ yil: secilenYil, ay: m, ay_kisa: AYLAR[m].slice(0, 3), gelir: 0, gider: 0 });
    const bugunDonem = maasDonemKey(yerelTarihAnahtari(), gun);
    const buSira = bugunDonem.yil * 100 + bugunDonem.ay;
    raporIslemleri.forEach(i => {
      if (i.tip === "gelir" && i.kategori === "Birikim") return;
      const dk = maasDonemKey(i.tarih, gun);
      if (dk.yil !== secilenYil) return;
      const bucket = donemler[dk.ay];
      if (bucket) bucket[i.tip] = paraTopla(bucket[i.tip], i.tutar);
    });
    // Gelecek dönemleri (bugünün dönemi sonrasını) gösterme
    return donemler.filter(d => (d.yil * 100 + d.ay) <= buSira).map(d => ({ ay: d.ay_kisa, gelir: d.gelir, gider: d.gider }));
  }, [raporIslemleri, secilenYil, donemGun]);

  const kategoriTrend = useMemo(() => {
    const gun = donemGun || 15;
    const bugunDonem = maasDonemKey(yerelTarihAnahtari(), gun);
    const buSira = bugunDonem.yil * 100 + bugunDonem.ay;
    const sonuc = [];
    for (let ay = 0; ay < 12; ay++) {
      if (secilenYil * 100 + ay > buSira) break; // gelecek dönemleri gösterme
      const label = AYLAR[ay].slice(0, 3);
      const ayIslem = raporIslemleri.filter(x => {
        if (x.tip !== "gider") return false;
        const dk = maasDonemKey(x.tarih, gun);
        return dk.yil === secilenYil && dk.ay === ay;
      });
      const toplam = ayIslem.reduce((s, x) => paraTopla(s, x.tutar), 0);
      const katMap = {};
      ayIslem.forEach(x => { katMap[x.kategori] = paraTopla(katMap[x.kategori] || 0, x.tutar); });
      sonuc.push({ label, toplam, katMap });
    }
    return sonuc;
  }, [raporIslemleri, secilenYil, donemGun]);

  const trendKategoriler = useMemo(() => {
    const set = new Set();
    kategoriTrend.forEach(d => Object.keys(d.katMap).forEach(k => set.add(k)));
    return [...set];
  }, [kategoriTrend]);

  // Son altı tamamlanmış maaş dönemi. Kayıtsız dönemler sıfır harcama sayılmaz.
  const altiAyOzet = useMemo(() => {
    const simdiDonemi = maasDonemiBul(bugunKey, donemGun || 15);
    const aylar = [];
    for (let n = 6; n >= 1; n--) {
      const bas = new Date(simdiDonemi.yil, simdiDonemi.ay - n, simdiDonemi.gun);
      const key = maasDonemKey(yerelTarihAnahtari(bas), simdiDonemi.gun).key;
      const kayitlar = raporIslemleri.filter(i => maasDonemKey(i.tarih, simdiDonemi.gun).key === key);
      const gelir = kayitlar.filter(gelirDahil).reduce((s, i) => paraTopla(s, i.tutar), 0);
      const gider = kayitlar.filter(i => i.tip === "gider").reduce((s, i) => paraTopla(s, i.tutar), 0);
      aylar.push({ label: donemLabel(bas.getFullYear(), bas.getMonth(), simdiDonemi.gun), gelir, gider, net: gelir - gider, veriVar: kayitlar.length > 0 });
    }
    const kayitli = aylar.filter(a => a.veriVar);
    const toplamGelir = kayitli.reduce((s, a) => paraTopla(s, a.gelir), 0);
    const toplamGider = kayitli.reduce((s, a) => paraTopla(s, a.gider), 0);
    const sayi = kayitli.length;
    return { toplamGelir, toplamGider, net: toplamGelir - toplamGider, aylar, sayi,
      ortGider: sayi ? toplamGider / sayi : 0, ortGelir: sayi ? toplamGelir / sayi : 0 };
  }, [raporIslemleri, donemGun, bugunKey]);

  // ── AKILLI YORUM ──
  const fmtTL = n => "₺" + Math.round(n).toLocaleString("tr-TR");

  const donemYorumlari = useMemo(() => {
    const gun = donemGun || 15;
    const { yil, ay, key: buKey } = yorumDonemi;
    let oYil = yil, oAy = ay - 1; if (oAy < 0) { oAy = 11; oYil--; }
    const onKey = `${oYil}-${oAy}-${gun}`;

    const donemKayitlari = raporIslemleri.filter(i => maasDonemKey(i.tarih, gun).key === buKey);
    const buIslem = donemKayitlari.filter(i => yorumDonemOffset !== 0 || i.tarih <= bugunKey);
    const onDonemKayitlari = raporIslemleri.filter(i => maasDonemKey(i.tarih, gun).key === onKey);
    const gecenGun = takvimGunFarki(bugunKey, yorumDonemi.baslangic) + 1;
    const oncekiBas = new Date(oYil, oAy, gun);
    const onIslem = onDonemKayitlari.filter(i => yorumDonemOffset !== 0
      || takvimGunFarki(i.tarih, oncekiBas) < gecenGun);
    const karsilastirma = yorumDonemOffset === 0 ? "geçen dönemin aynı süreli kısmına" : "geçen döneme";

    const buGelir = buIslem.filter(i => i.tip === "gelir" && i.kategori !== "Birikim" && !(i.kategori === "Kira Geliri" && i.donemeDahil === false)).reduce((s, i) => paraTopla(s, i.tutar), 0);
    const buGider = buIslem.filter(i => i.tip === "gider").reduce((s, i) => paraTopla(s, i.tutar), 0);
    const onGider = onIslem.filter(i => i.tip === "gider").reduce((s, i) => paraTopla(s, i.tutar), 0);

    const yorumlar = [];
    if (buIslem.length === 0 && !habitRaporHareketleri.some(h => maasDonemKey(h.tarih, gun).key === buKey)) {
      yorumlar.push({ tip: "bilgi", metin: "Bu dönem için bugüne kadar tarihli işlem yok. İleri tarihli kayıtlar gerçekleşen harcama yorumuna katılmaz." });
      return yorumlar;
    }
    yorumlar.push({ tip: "bilgi", metin: "Bu bölüm kayıt tarihlerine göre hesaplanır; ödeme kaydı, ödendiğini tek başına göstermez. Kart toplamları dönem borcunu temsil edebilir." });
    const planliGider = donemKayitlari.filter(i => i.tip === "gider" && i.tarih > bugunKey).reduce((s, i) => paraTopla(s, i.tutar), 0);
    if (yorumDonemOffset === 0 && planliGider > 0) yorumlar.push({ tip: "bilgi", metin: `Bu dönemde ileri tarihli ${fmtTL(planliGider)} gider kaydı var; bugüne kadarki gider toplamına dahil değil.` });
    const net = buGelir - buGider;
    if (buIslem.length === 0) yorumlar.push({ tip: "bilgi", metin: "Bu dönemde yalnızca kart günlük hareket geçmişi var; toplam borç kaydı başka döneme taşınmış olabilir. Gelir-gider özeti için yeterli dönem kaydı yok." });
    else if (net >= 0) yorumlar.push({ tip: "iyi", metin: `Bu dönem ${fmtTL(net)} artıda görünüyorsun. Gelir ${fmtTL(buGelir)}, gider ${fmtTL(buGider)}.` });
    else yorumlar.push({ tip: "uyari", metin: `Bu dönem ${fmtTL(Math.abs(net))} açık verdin. Gelir ${fmtTL(buGelir)}, gider ${fmtTL(buGider)}.` });

    if (buGelir > 0) {
      const oran = Math.round(net / buGelir * 100);
      if (oran >= 20) yorumlar.push({ tip: "iyi", metin: `Kayıtlara göre kalan gelir oranın %${oran} — gayet iyi, gelirinin önemli kısmını koruyorsun.` });
      else if (oran >= 0) yorumlar.push({ tip: "notr", metin: `Kayıtlara göre kalan gelir oranın %${oran}. %20 üstü hedeflemek bütçeni güçlendirir.` });
      else yorumlar.push({ tip: "uyari", metin: `Giderlerin gelirini aşıyor (kalan gelir %${oran}). Harcamaları gözden geçirmek faydalı olabilir.` });
    }
    if (onGider > 0) {
      const fark = Math.round((buGider - onGider) / onGider * 100);
      if (fark > 10) yorumlar.push({ tip: "uyari", metin: `Toplam giderin ${karsilastirma} göre %${fark} arttı (${fmtTL(onGider)} → ${fmtTL(buGider)}).` });
      else if (fark < -10) yorumlar.push({ tip: "iyi", metin: `Toplam giderin ${karsilastirma} göre %${Math.abs(fark)} azaldı (${fmtTL(onGider)} → ${fmtTL(buGider)}).` });
    }
    const katMap = {};
    buIslem.filter(i => i.tip === "gider").forEach(i => { katMap[i.kategori] = paraTopla(katMap[i.kategori] || 0, i.tutar); });
    const sirali = Object.entries(katMap).sort((a, b) => b[1] - a[1]);
    if (sirali.length > 0) {
      const [enKat, enTutar] = sirali[0];
      const pct = buGider > 0 ? Math.round(enTutar / buGider * 100) : 0;
      yorumlar.push({ tip: "bilgi", metin: `En çok ${enKat} kategorisinde harcadın: ${fmtTL(enTutar)} (giderinin %${pct}'i).` });
    }
    if (sirali.length >= 3) yorumlar.push({ tip: "bilgi", metin: `En büyük 3 gider kalemin: ${sirali.slice(0, 3).map(s => s[0]).join(", ")}.` });

    const onKatMap = {};
    onIslem.filter(i => i.tip === "gider").forEach(i => { onKatMap[i.kategori] = paraTopla(onKatMap[i.kategori] || 0, i.tutar); });
    let enCokArtan = null, enArtisYuzde = 0;
    Object.entries(katMap).forEach(([kat, tutar]) => {
      const onceki = onKatMap[kat] || 0;
      if (onceki > 0) {
        const artis = (tutar - onceki) / onceki * 100;
        if (artis > enArtisYuzde) { enArtisYuzde = artis; enCokArtan = { kat, artis: Math.round(artis), tutar, onceki }; }
      }
    });
    if (enCokArtan && enCokArtan.artis > 20) yorumlar.push({ tip: "uyari", metin: `${enCokArtan.kat} harcaman ${karsilastirma} göre %${enCokArtan.artis} arttı (${fmtTL(enCokArtan.onceki)} → ${fmtTL(enCokArtan.tutar)}).` });

    const kartIsimleri = (kartlar || []).map(k => k.isim);
    const kkToplam = buIslem.filter(i => i.tip === "gider" && (i.kartHarcama || kartIsimleri.includes(i.kategori))).reduce((s, i) => paraTopla(s, i.tutar), 0);
    if (kkToplam > 0 && buGider > 0) {
      const kkPct = Math.round(kkToplam / buGider * 100);
      yorumlar.push({ tip: "bilgi", metin: `Kredi kartı harcaman toplam giderinin %${kkPct}'i (${fmtTL(kkToplam)}).` });
    }

    // ── ANORMAL HARCAMA TESPİTİ (son 6 dönem ortalamasına göre) ──
    (() => {
      if (yorumDonemOffset === 0) return; // Tamamlanmamış dönem tam dönem ortalamasıyla kıyaslanmaz.
      // Geçmiş 6 dönemin kategori ortalamalarını çıkar
      const gecmisDonemler = [];
      for (let g = 1; g <= 6; g++) {
        let gy = yil, ga = ay - g;
        while (ga < 0) { ga += 12; gy--; }
        gecmisDonemler.push(`${gy}-${ga}-${gun}`);
      }
      const gecmisIslem = raporIslemleri.filter(i => i.tip === "gider" && gecmisDonemler.includes(maasDonemKey(i.tarih, gun).key));
      if (gecmisIslem.length === 0) return;
      const gecmisKat = {};
      gecmisIslem.forEach(i => {
        if (!gecmisKat[i.kategori]) gecmisKat[i.kategori] = { toplam: 0, donemler: new Set() };
        gecmisKat[i.kategori].toplam = paraTopla(gecmisKat[i.kategori].toplam, i.tutar);
        gecmisKat[i.kategori].donemler.add(maasDonemKey(i.tarih, gun).key);
      });
      const anormaller = [];
      Object.entries(katMap).forEach(([kat, tutar]) => {
        const g = gecmisKat[kat];
        if (!g || g.donemler.size < 3) return; // en az 2 dönem geçmiş olmalı
        const ort = g.toplam / g.donemler.size;
        if (ort <= 0) return;
        const kat_ = tutar / ort;
        if (kat_ >= 2) anormaller.push({ kat, kat_, tutar, ort });
      });
      anormaller.sort((a, b) => b.kat_ - a.kat_).slice(0, 2).forEach(a => {
        yorumlar.push({ tip: "uyari", metin: `Yüksek kategori gideri: ${a.kat} bu dönem kayıtlı geçmiş ortalamanın ${a.kat_.toFixed(1)} katı (${fmtTL(a.tutar)}, ${gecmisKat[a.kat].donemler.size} kayıtlı dönem ortalaması ~${fmtTL(a.ort)}).` });
      });
    })();

    // ── DÖNEM SONU TAHMİNİ ──
    (() => {
      if (yorumDonemOffset !== 0) return;
      const toplamGun = takvimGunFarki(yorumDonemi.bitis, yorumDonemi.baslangic) + 1;
      const gunSayisi = Math.min(toplamGun, Math.max(1, gecenGun));
      if (gunSayisi >= toplamGun) return;
      if (gunSayisi < 7) {
        yorumlar.push({ tip: "bilgi", metin: "Dönem sonu tahmini için en az 7 gün beklenir; ilk günlerdeki giderler yanıltıcı olabilir." });
        return;
      }
      const hesap = donemSonuGiderTahmini({islemler,odemeler,kartlar,kartOdemeleri,bakiyeHesapModu,
        baslangic:yorumDonemi.baslangic,bitis:yorumDonemi.bitis});
      const gunlukOrt = hesap.gunlukOrt;
      const tahminGider = hesap.tahmin;
      const tahminGelir = donemKayitlari.filter(gelirDahil).reduce((s, i) => paraTopla(s, i.tutar), 0);
      const tahminNet = tahminGelir - tahminGider;
      yorumlar.push({ tip: tahminNet < 0 ? "uyari" : "iyi", metin:
        `Mevcut kayıtlara göre dönem sonu tahmini: ${fmtTL(Math.abs(tahminNet))} ${tahminNet < 0 ? "açık" : "artı"}. Tahmini gider ${fmtTL(tahminGider)}. Tahminde sabit giderler ve ekstreler bir kez sayılır; günlük değişken gider ortalaması ${fmtTL(gunlukOrt)}. Kesin sonuç değildir.` });
    })();

    // ── BU DÖNEM EN PAHALI GÜN + HAFTALIK DAĞILIM (sabit giderler ve kredi kartı ilk-giriş toplamı hariç; kart güncelleme farkları ve ödemeler dahil) ──
    (() => {
      const habitIslem = habitRaporHareketleri
        .filter(i => maasDonemKey(i.tarih, gun).key === buKey);
      if (habitIslem.length === 0) return;
      const gunMap = {};
      habitIslem.forEach(i => { gunMap[i.tarih] = paraTopla(gunMap[i.tarih] || 0, i.tutar); });
      const siraliGunler = Object.entries(gunMap).sort((a, b) => b[1] - a[1]);
      if (siraliGunler.length > 0) {
        const [enGun, enTutar] = siraliGunler[0];
        yorumlar.push({ tip: "bilgi", metin: `Bu dönem en fazla harcamayı ${enGun.split("-").reverse().join(".")} tarihinde kayıtlı — ${fmtTL(enTutar)} (sabit giderler ve kredi kartı ilk-giriş toplamı hariç).` });
      }
      // Haftalık dağılım (dönem başlangıcından itibaren 7'şer günlük dilimler)
      const donemBas = yorumDonemi.baslangic;
      const donemBit = yorumDonemi.bitis;
      const bugun = yerelTarihOku(yerelTarihAnahtari());
      const gorunenBit = yorumDonemOffset === 0 && bugun < donemBit ? bugun : donemBit;
      const haftaSayisi = Math.max(1, Math.ceil((takvimGunFarki(gorunenBit, donemBas) + 1) / 7));
      const haftaToplam = Array.from({ length: haftaSayisi }, (_, index) => ({
        no: index + 1,
        baslangic: takvimGunuEkle(donemBas, index * 7),
        bitis: takvimGunuEkle(donemBas, Math.min(index * 7 + 6, takvimGunFarki(gorunenBit, donemBas))),
        tutar: 0,
      }));
      habitIslem.forEach(i => {
        const fark = takvimGunFarki(i.tarih, donemBas);
        const index = Math.floor(fark / 7);
        if (fark >= 0 && index < haftaToplam.length) haftaToplam[index].tutar += i.tutar;
      });
      const metin = haftaToplam.map((h, index) => {
        const devam = yorumDonemOffset === 0 && index === haftaToplam.length - 1 && bugun <= donemBit ? " • devam ediyor" : "";
        return `${h.no}. Hafta (${kisaTarih(h.baslangic)}–${kisaTarih(h.bitis)}) ${fmtTL(h.tutar)}${devam}`;
      }).join(" · ");
      yorumlar.push({ tip: "notr", metin: `Haftalık dağılım: ${metin}` });
    })();

    return yorumlar;
  }, [raporIslemleri, islemler, odemeler, kartOdemeleri, bakiyeHesapModu, donemGun, kartlar, yorumDonemi, yorumDonemOffset, bugunKey, habitRaporHareketleri]);

  const altiAyYorumlari = useMemo(() => {
    const o = altiAyOzet;
    if (!o.sayi) return [{ tip: "bilgi", metin: "Son 6 tamamlanmış maaş döneminde kayıt yok." }];
    const yorumlar = [
      { tip: o.net >= 0 ? "iyi" : "uyari", metin: `Son 6 tamamlanmış maaş döneminin ${o.sayi} tanesinde kayıt var. Kayıtlı dönemlerde gelir-gider farkın ${fmtTL(o.net)}; dönem başına ortalama ${fmtTL(o.net / o.sayi)}. Bu fark, gerçekleşmiş birikim anlamına gelmez.` },
      { tip: "bilgi", metin: `Kayıt bulunan dönemlerde ortalama gider ${fmtTL(o.ortGider)}, ortalama gelir ${fmtTL(o.ortGelir)}. Kayıtların eksiksiz olduğu varsayılır; kayıtsız dönemler sıfır sayılmaz.` }
    ];
    if (o.sayi === 6) {
      const ilk3 = o.aylar.slice(0, 3).reduce((s, a) => paraTopla(s, a.gider), 0);
      const son3 = o.aylar.slice(3).reduce((s, a) => paraTopla(s, a.gider), 0);
      if (ilk3 > 0) {
        const fark = Math.round((son3 - ilk3) / ilk3 * 100);
        yorumlar.push({ tip: fark > 10 ? "uyari" : fark < -10 ? "iyi" : "notr", metin: `Son 3 tamamlanmış dönemin toplam gideri önceki 3 döneme göre %${Math.abs(fark)} ${fark > 0 ? "arttı" : fark < 0 ? "azaldı" : "değişti"}.` });
      }
    } else yorumlar.push({ tip: "bilgi", metin: "Altı dönemin tamamında kayıt olmadığı için üçer dönemlik trend karşılaştırması yapılmadı." });
    const kayitli = o.aylar.filter(a => a.veriVar);
    if (kayitli.length >= 2) {
      const sirali = [...kayitli].sort((a, b) => b.net - a.net);
      if (sirali[0].net !== sirali[sirali.length - 1].net) yorumlar.push({ tip: "bilgi", metin: `Kayıtlı dönemler arasında en yüksek gelir-gider farkı ${sirali[0].label} (${fmtTL(sirali[0].net)}), en düşük fark ${sirali[sirali.length - 1].label} (${fmtTL(sirali[sirali.length - 1].net)}).` });
    }
    return yorumlar;
  }, [altiAyOzet]);

  // ── GENEL ALIŞKANLIK (tüm dönemler; sabit giderler ve kredi kartı ilk-giriş toplamı hariç, güncelleme farkları ve ödemeler dahil) ──
  // Zamanla birikeceği için tüm geçmiş veriden haftanın hangi günü daha çok harcadığını öğrenir.
  const genelAliskanlikYorumlari = useMemo(() => {
    const yorumlar = [];
    const habitIslem = habitRaporHareketleri;
    const gunMap = {};
    habitIslem.forEach(i => { gunMap[i.tarih] = paraTopla(gunMap[i.tarih] || 0, i.tutar); });
    const gunSayisi = Object.keys(gunMap).length;
    if (gunSayisi < 15) {
      yorumlar.push({ tip: "bilgi", metin: "Alışkanlık analizi için henüz yeterli veri yok. Veri biriktikçe (sabit giderler ve kredi kartı ilk-giriş toplamı hariç) bu bölüm otomatik dolacak." });
      return yorumlar;
    }
    const gunAdlari = ["Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi"];
    const haftaToplam = {};
    Object.entries(gunMap).forEach(([tarih, tutar]) => {
      const wd = yerelTarihOku(tarih).getDay();
      if (!haftaToplam[wd]) haftaToplam[wd] = { toplam: 0, adet: 0 };
      haftaToplam[wd].toplam = paraTopla(haftaToplam[wd].toplam, tutar);
      haftaToplam[wd].adet += 1;
    });
    const ortalamalar = Object.entries(haftaToplam).filter(([, v]) => v.adet >= 3).map(([wd, v]) => ({ wd: parseInt(wd), ort: v.toplam / v.adet, adet: v.adet }));
    if (ortalamalar.length >= 4) {
      ortalamalar.sort((a, b) => b.ort - a.ort);
      const en = ortalamalar[0];
      yorumlar.push({ tip: "bilgi", metin: `Geçmiş tüm dönemlere göre harcama kaydı olan günler içinde en yüksek günlük ortalama ${gunAdlari[en.wd]} — ${fmtTL(en.ort)} (${en.adet} gün verisine dayanarak).` });
      const az = ortalamalar[ortalamalar.length - 1];
      yorumlar.push({ tip: "notr", metin: `Harcama kaydı olan günler içinde en düşük günlük ortalama ${gunAdlari[az.wd]} — ${fmtTL(az.ort)}.` });
    }
    if (ortalamalar.length < 4) yorumlar.push({ tip: "bilgi", metin: "Gün karşılaştırması için haftanın en az 4 farklı gününde, her biri için 3 ayrı tarihli harcama kaydı gerekir." });
    yorumlar.push({ tip: "notr", metin: `Bu analiz harcama kaydı olan günleri karşılaştırır; kayıtsız günler sıfır sayılmaz. Ödeme kayıtları yalnızca ödendiğinde, gerçek ödeme tarihinde dahil edilir; eski kayıtta ödeme tarihi yoksa kayıt tarihi kullanılır. Bu analiz sabit giderleri (kira, abonelik vb.) ve kredi kartında ilk kez girilen toplamı hesaba katmaz; kart güncellemelerindeki günlük farkları ve ödemeleri dahil eder.` });
    return yorumlar;
  }, [habitRaporHareketleri]);

  const renderYorumKart = (yorumlar, baslik) => (
    <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 14px", marginBottom: 14 }}>
      <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 12 }}>{baslik}</div>
      {yorumlar.map((y, i) => {
        const renk = y.tip === "iyi" ? "#10b981" : y.tip === "uyari" ? "#ef4444" : y.tip === "notr" ? "#f59e0b" : "#A74E32";
        const ikon = y.tip === "iyi" ? "check" : y.tip === "uyari" ? "warning" : y.tip === "notr" ? "target" : "tag";
        return (
          <div key={i} style={{ display: "flex", gap: 10, padding: "10px 0", borderBottom: i < yorumlar.length - 1 ? "1px solid var(--divider)" : "none" }}>
            <span style={{ fontSize: 14, flexShrink: 0 }}><Icon name={ikon} size={17}/></span>
            <span style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", lineHeight: 1.5, borderLeft: `2px solid ${renk}`, paddingLeft: 10 }}>{y.metin.replace(/[\u{1F300}-\u{1FAFF}\u2600-\u27BF\uFE0F]/gu, "").trim()}</span>
          </div>
        );
      })}
    </div>
  );

  const renderAkilliYorum = () => (
    <div>
      <div style={{ background:"var(--card2)", border:"1px solid var(--border)", borderRadius:12, padding:"9px 10px", marginBottom:10, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
        <button onClick={() => setYorumDonemOffset(o => o - 1)} aria-label="Önceki dönem" style={{ background:"none", border:"none", color:"#f59e0b", fontSize:20, cursor:"pointer", padding:"0 8px" }}>‹</button>
        <div style={{ textAlign:"center" }}>
          <div style={{ color:"var(--text)", fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600 }}>{yorumDonemi.label}</div>
          <div style={{ color:"var(--text5)", fontSize: 12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop:2 }}>{yorumDonemOffset === 0 ? "Güncel maaş dönemi" : `${Math.abs(yorumDonemOffset)} dönem önce`}</div>
        </div>
        <button onClick={() => setYorumDonemOffset(o => Math.min(0, o + 1))} disabled={yorumDonemOffset === 0} aria-label="Sonraki dönem" style={{ background:"none", border:"none", color:yorumDonemOffset === 0 ? "var(--border2)" : "#f59e0b", fontSize:20, cursor:yorumDonemOffset === 0 ? "default" : "pointer", padding:"0 8px" }}>›</button>
      </div>
      {renderYorumKart(donemYorumlari, `${yorumDonemi.label}`)}
      {renderYorumKart(altiAyYorumlari, "Son 6 Tamamlanmış Maaş Dönemi")}
      {renderYorumKart(genelAliskanlikYorumlari, "Genel Alışkanlığın")}
      <div style={{ textAlign: "center", color: "var(--border)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 4 }}>
        Yorumlar eklediğin verilere göre otomatik güncellenir
      </div>
    </div>
  );

  const yillikGelir = raporIslemleri.filter(i => yerelTarihOku(i.tarih)?.getFullYear() === secilenYil && i.tip === "gelir" && i.kategori !== "Birikim").reduce((s, i) => paraTopla(s, i.tutar), 0);
  const yillikGider = raporIslemleri.filter(i => yerelTarihOku(i.tarih)?.getFullYear() === secilenYil && i.tip === "gider").reduce((s, i) => paraTopla(s, i.tutar), 0);

  const PASTA_RENKLER = ["#f59e0b","#A74E32","#10b981","#ef4444","#06b6d4","#ec4899","#8b5cf6","#f97316","#84cc16","#14b8a6"];
  const TREND_RENKLER = { "Market":"#f59e0b","Fatura":"#A74E32","Ulaşım":"#06b6d4","Sağlık":"#10b981","Eğlence":"#ec4899","Giyim":"#8b5cf6","Restoran":"#f97316" };
  const fmtTam = n => Math.round(n).toLocaleString("tr-TR");
  const fmtOndalik = n => n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const tt = ({ active, payload, label }) => {
    if (!active || !payload || !payload.length) return null;
    return (
      <div style={{ background:"var(--card)", border:"1px solid var(--border)", borderRadius:10, padding:"10px 14px" }}>
        <div style={{ color:"var(--text3)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom:6 }}>{label}</div>
        {payload.map(p => (
          <div key={p.name} style={{ color:p.color, fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>
            {p.name}: ₺{fmtTam(p.value)}
          </div>
        ))}
      </div>
    );
  };

  const renderDagilim = () => (
    <div>
      <div style={{ display:"flex", gap:8, marginBottom:14 }}>
        {["gider","gelir"].map(t => (
          <button key={t} onClick={() => setGorunum(t)} style={{ flex:1, padding:"9px", borderRadius:10, border:"none", cursor:"pointer", fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize:12, fontWeight:600, background: gorunum===t ? (t==="gider" ? "#ef4444" : "#10b981") : "var(--border)", color: gorunum===t ? "#fff" : "var(--text3)" }}>
            {t === "gider" ? "Gider" : "Gelir"}
          </button>
        ))}
      </div>
      {kategoriVerisi.length === 0 && (
        <div style={{ textAlign:"center", color:"var(--text5)", fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize:13, padding:"40px 0" }}>
          {secilenYil} için {gorunum} kaydı yok
        </div>
      )}
      {kategoriVerisi.length > 0 && (
        <div style={{ background:"var(--card)", border:"1px solid var(--border)", borderTop:"1px solid var(--border-light)", borderRadius:20, boxShadow:"var(--card-shadow)", padding:"16px 14px", marginBottom:14 }}>
          <div style={{ color:"var(--text3)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:14 }}>
            {gorunum === "gider" ? "Gider" : "Gelir"} Dağılımı — {secilenYil}
          </div>
          <div style={{ display:"flex", alignItems:"center", gap:12, marginBottom:14 }}>
            <PieChart width={120} height={120}>
              <Pie data={kategoriVerisi} cx={55} cy={55} innerRadius={32} outerRadius={56} dataKey="value" strokeWidth={0}>
                {kategoriVerisi.map((d, i) => (
                  <Cell key={i} fill={PASTA_RENKLER[i % PASTA_RENKLER.length]} />
                ))}
              </Pie>
            </PieChart>
            <div style={{ flex:1 }}>
              {kategoriVerisi.map((d, i) => (
                <div key={d.name} style={{ display:"flex", alignItems:"center", gap:6, marginBottom:5 }}>
                  <div style={{ width:8, height:8, borderRadius:"50%", background:PASTA_RENKLER[i % PASTA_RENKLER.length], flexShrink:0 }} />
                  <span style={{ color:"var(--text2)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", flex:1, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap" }}>{d.name}</span>
                  <span style={{ color:PASTA_RENKLER[i % PASTA_RENKLER.length], fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600 }}>%{d.pct}</span>
                </div>
              ))}
            </div>
          </div>
          <div style={{ borderTop:"1px solid var(--border)", paddingTop:12 }}>
            {kategoriVerisi.map((d, i) => (
              <div key={d.name} style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:7 }}>
                <div style={{ display:"flex", alignItems:"center", gap:8 }}>
                  <div style={{ width:3, height:16, borderRadius:99, background:PASTA_RENKLER[i % PASTA_RENKLER.length] }} />
                  <span style={{ color:"var(--text3)", fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{d.name}</span>
                </div>
                <span style={{ color:"var(--text)", fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600 }}>₺{fmtOndalik(d.value)}</span>
              </div>
            ))}
            <div style={{ borderTop:"1px solid var(--border)", paddingTop:8, marginTop:2, display:"flex", justifyContent:"space-between" }}>
              <span style={{ color:"var(--text4)", fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Toplam</span>
              <span style={{ color:"#f59e0b", fontSize:13, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600 }}>₺{fmtOndalik(toplamTutar)}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  const renderTrend = () => (
    <div>
      <div style={{ background:"var(--card)", border:"1px solid var(--border)", borderTop:"1px solid var(--border-light)", borderRadius:20, boxShadow:"var(--card-shadow)", padding:"16px 14px", marginBottom:14 }}>
        <div style={{ color:"var(--text3)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:14 }}>Aylık Toplam Gider — {secilenYil}</div>
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={kategoriTrend} margin={{ top:10, right:10, left:0, bottom:0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
            <XAxis dataKey="label" tick={{ fill:"var(--text4)", fontSize: 12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fill:"var(--text4)", fontSize: 12 }} axisLine={false} tickLine={false} tickFormatter={v => (v/1000).toFixed(0) + "K"} width={32} />
            <Tooltip content={tt} />
            <Line type="monotone" dataKey="toplam" name="Gider" stroke="#ef4444" strokeWidth={2.5} dot={{ r:3, fill:"#ef4444" }} activeDot={{ r:5 }} />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <div style={{ background:"var(--card)", border:"1px solid var(--border)", borderTop:"1px solid var(--border-light)", borderRadius:20, boxShadow:"var(--card-shadow)", padding:"16px 14px", marginBottom:14 }}>
        <div className="category-trend-heading">Kategori Bazlı Trend <span>{secilenYil}</span></div>
        {trendKategoriler.length === 0 && (
          <div style={{ color:"var(--text5)", fontSize:13, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textAlign:"center", padding:"20px 0" }}>Henüz gider kaydı yok</div>
        )}
        {trendKategoriler.map((kat, ki) => {
          const renk = TREND_RENKLER[kat] || PASTA_RENKLER[ki % PASTA_RENKLER.length];
          const degerler = kategoriTrend.map(d => d.katMap[kat] || 0);
          const maxVal = Math.max(...degerler, 1);
          const son = degerler[degerler.length - 1] || 0;
          const onceki = degerler[degerler.length - 2] || 0;
          const degisim = onceki > 0 ? Math.round((son - onceki) / onceki * 100) : null;
          const secimKey = `${secilenYil}-${kat}`;
          const secimIndex = trendSecimleri[secimKey];
          const aktifIndex = secimIndex != null && secimIndex < degerler.length ? secimIndex : degerler.length - 1;
          return (
            <section className="category-trend-card" key={kat} aria-label={`${kat} dönem trendi`}>
              <div className="category-trend-top">
                <div className="category-trend-main">
                  <div className="category-trend-name"><span className="category-trend-icon" style={{ color: renk, background: renk + "18" }}><Icon name={kat === "Fatura" ? "receipt" : "wallet"} size={18} /></span><span>{kat}</span></div>
                  <div className="category-trend-amount">₺{fmtTam(son)}</div>
                  <div className="category-trend-caption">Son dönem{kategoriTrend.length > 0 ? ` · ${kategoriTrend[kategoriTrend.length - 1].label}` : ""}</div>
                </div>
                {degisim !== null && onceki > 0 && (
                  <div className="category-trend-change">
                    <span className="category-trend-badge" style={{ color: degisim > 0 ? "#10b981" : degisim < 0 ? "#ef4444" : "var(--text)", background: degisim > 0 ? "#10b98118" : degisim < 0 ? "#ef444418" : "var(--card2)" }}>
                      {degisim > 0 ? "▲" : degisim < 0 ? "▼" : ""}{Math.abs(degisim)}%
                    </span>
                    <span className="category-trend-caption">Önceki döneme göre</span>
                  </div>
                )}
              </div>
              <div className="category-trend-scroll">
                <div className="category-trend-columns" style={{ gridTemplateColumns: `repeat(${Math.max(1, degerler.length)}, minmax(0, 1fr))`, minWidth: 0 }}>
                  {degerler.map((v, idx) => {
                    const mumRenk = idx === 0 ? "#9ca3af" : v < degerler[idx-1] ? "#10b981" : v > degerler[idx-1] ? "#ef4444" : "#9ca3af";
                    return (
                      <button type="button" className="category-trend-column" key={idx} aria-pressed={idx === aktifIndex} aria-label={`${kat} · ${kategoriTrend[idx].label}: ₺${fmtTam(v)}`} title={`${kategoriTrend[idx].label}: ₺${fmtTam(v)}`} onClick={() => setTrendSecimleri(p => ({ ...p, [secimKey]: idx }))}>
                        <span className="category-trend-bar-area"><span className="category-trend-bar" style={{ background: mumRenk, height: v > 0 ? Math.max(v / maxVal * 100, 4) + "%" : "0%", opacity: idx === aktifIndex ? 1 : 0.72 }} /></span>
                        <span className="category-trend-month">{kategoriTrend[idx].label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
              {aktifIndex >= 0 && <div className="category-trend-selected" aria-live="polite">{kategoriTrend[aktifIndex].label} dönemi: <strong>₺{fmtTam(degerler[aktifIndex])}</strong></div>}
            </section>
          );
        })}

      </div>
    </div>
  );

  const birikimKayitlari = useMemo(() => {
    const sonAy = secilenYil === new Date().getFullYear() ? new Date().getMonth() : 11;
    const ayMap = {};
    for (let m = 0; m <= sonAy; m++) ayMap[m] = { ay: AYLAR[m].slice(0, 3), tutar: 0, islemler: [] };
    islemler.filter(i => i.tarih<=bugunKey && yerelTarihOku(i.tarih)?.getFullYear() === secilenYil && i.tip === "gelir" && (i.kategori === "Birikim" || i.kategori === "Kira Geliri")).forEach(i => {
      const ay = yerelTarihOku(i.tarih).getMonth();
      if (ayMap[ay]) { ayMap[ay].tutar += i.tutar; ayMap[ay].islemler.push(i); }
    });
    return Object.values(ayMap);
  }, [islemler, secilenYil, bugunKey]);

  const yillikBirikim = birikimKayitlari.reduce((s, a) => paraTopla(s, a.tutar), 0);
  const birikimAySayisi = birikimKayitlari.filter(a => a.tutar > 0).length;
  // Aylık ortalama: Ocak'tan baz alıp geçen ay sayısına böl (içinde bulunulan yıl ise mevcut aya kadar)
  const gecenAySayisi = secilenYil === new Date().getFullYear() ? (new Date().getMonth() + 1) : 12;
  const ortBirikim = gecenAySayisi > 0 ? yillikBirikim / gecenAySayisi : 0;

  const renderBirikim = () => (
    <div>
      <div style={{ background:"#10b98111", border:"1px solid #10b98133", borderRadius:10, padding:"10px 14px", marginBottom:14, color: "var(--income-text)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>
        Gelir eklerken kategori olarak <strong>Birikim</strong> seç — burada takip edilir.
      </div>
      <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:10, marginBottom:14 }}>
        {[
          { label:"Yıllık", value:"₺" + fmtTam(yillikBirikim), color: "var(--income-text)", icon:"coins" },
          { label:"Aylık Ort.", value: ortBirikim > 0 ? "₺" + fmtTam(ortBirikim) : "—", color:"#f59e0b", icon:"chart" },
          { label:"Birikim Ayı", value: birikimAySayisi > 0 ? birikimAySayisi + " ay" : "—", color:"#A74E32", icon:"calendar" },
        ].map(c => (
          <div className="saving-stat-card" key={c.label} style={{ background:"linear-gradient(135deg,var(--card),var(--card2))", border:"1px solid var(--border)", borderRadius:14, padding:"12px 8px", textAlign:"center" }}>
            <div style={{ fontSize:16, marginBottom:4 }}><Icon name={c.icon} size={17}/></div>
            <div style={{ color:"var(--text4)", fontSize: 12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform:"uppercase", marginBottom:4 }}>{c.label}</div>
            <div style={{ color:c.color, fontSize:13, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600 }}>{c.value}</div>
          </div>
        ))}
      </div>
      {yillikBirikim === 0 ? (
        <div style={{ textAlign:"center", color:"var(--text5)", fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize:13, padding:"40px 0" }}>
          {secilenYil} için birikim kaydı yok
        </div>
      ) : (
        <div>
          <div style={{ background:"var(--card)", border:"1px solid var(--border)", borderTop:"1px solid var(--border-light)", borderRadius:20, boxShadow:"var(--card-shadow)", padding:"16px 14px", marginBottom:14 }}>
            <div style={{ color:"var(--text3)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:14 }}>Aylık Birikim</div>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={birikimKayitlari} margin={{ top:10, right:10, left:0, bottom:0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="ay" tick={{ fill:"var(--text4)", fontSize: 12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill:"var(--text4)", fontSize: 12 }} axisLine={false} tickLine={false} tickFormatter={v => (v / 1000).toFixed(0) + "K"} width={32} />
                <Tooltip contentStyle={{ background:"var(--card)", border:"1px solid var(--border)", borderRadius:10, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize:12 }} formatter={v => ["₺" + fmtTam(v), "Birikim"]} />
                <Line type="monotone" dataKey="tutar" name="Birikim" stroke="#10b981" strokeWidth={2.5} dot={{ r:3, fill:"#10b981" }} activeDot={{ r:5 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <div style={{ background:"var(--card)", border:"1px solid var(--border)", borderTop:"1px solid var(--border-light)", borderRadius:20, boxShadow:"var(--card-shadow)", padding:"16px 14px" }}>
            <div style={{ color:"var(--text3)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform:"uppercase", letterSpacing:"0.08em", marginBottom:12 }}>Ay ay detay</div>
            {birikimKayitlari.filter(d => d.tutar > 0).map((d, i) => {
              const maxT = Math.max(...birikimKayitlari.map(x => x.tutar), 1);
              return (
                <div key={i} style={{ marginBottom:10 }}>
                  <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:4 }}>
                    <span style={{ color:"var(--text3)", fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{d.ay}</span>
                    <span style={{ color: "var(--income-text)", fontSize:12, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight:600 }}>₺{fmtTam(d.tutar)}</span>
                  </div>
                  <div style={{ height:4, background:"var(--border)", borderRadius:99 }}>
                    <div style={{ height:"100%", width: (d.tutar / maxT * 100) + "%", background:"#10b981", borderRadius:99 }} />
                  </div>
                  {d.islemler.map((x, j) => (
                    <div key={j} style={{ display:"flex", justifyContent:"space-between", padding:"3px 0 0 8px" }}>
                      <span style={{ color:"var(--text5)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{x.aciklama || x.kategori}</span>
                      <span style={{ color:"var(--text4)", fontSize:11, fontFamily:"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>₺{fmtTam(x.tutar)}</span>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );

  return <EsnekRapor islemler={islemler} odemeler={odemeler} kartlar={kartlar} kartOdemeleri={kartOdemeleri} donemGun={donemGun} gelismisYorum={renderAkilliYorum} birikimRaporu={()=><><Input label="Birikim raporu yılı" type="number" min="2000" max="2100" value={secilenYil} onChange={e=>{if(tamSayiGecerli(e.target.value,2000,2100))setSecilenYil(Number(e.target.value));}}/>{renderBirikim()}</>}/>;
}

function TekrarlayanTab({ tekrarlayan, setTekrarlayan, kategoriler, setKategoriler }) {
  const { gorunumler: kategoriGorunumleri } = useContext(KategoriGorunumContext);
  const [modal, setModal] = useState(false);
  const [duzenleId, setDuzenleId] = useState(null);
  const bugun = yerelTarihAnahtari();
  const bos = { aciklama: "", tutar: "", gun: "1", kategori: "Fatura", baslangic: bugun, bitis: "" };
  const [form, setForm] = useState(bos);

  const duzenleAc = (t) => {
    setForm({ aciklama: t.aciklama || "", tutar: String(t.tutar), gun: String(t.gun), kategori: t.kategori, baslangic: t.baslangic || bugun, bitis: t.bitis || "" });
    setDuzenleId(t.id);
    setModal(true);
  };
  const kaydet = () => {
    if (!tutarGecerli(form.tutar) || !tamSayiGecerli(form.gun, 1, 31)
      || !yerelTarihOku(form.baslangic || bugun) || (form.bitis && (!yerelTarihOku(form.bitis) || form.bitis < (form.baslangic || bugun)))) return formHatasi("Pozitif tutar, 1–31 arasında gün ve sıralı başlangıç/bitiş tarihleri gir.");
    if (duzenleId) {
      setTekrarlayan(p => p.map(x => x.id === duzenleId
        ? { ...x, aciklama: form.aciklama, tutar: paraOku(form.tutar), gun: parseInt(form.gun), kategori: form.kategori, baslangic: form.baslangic || bugun, bitis: form.bitis || null }
        : x
      ));
    } else {
      setTekrarlayan(p => [...p, { ...form, id: Date.now(), tutar: paraOku(form.tutar), gun: parseInt(form.gun), aktif: true, baslangic: form.baslangic || bugun, bitis: form.bitis || null }]);
    }
    setModal(false);
    setDuzenleId(null);
    setForm(bos);
    toast(duzenleId ? "Ödeme güncellendi" : "Ödeme eklendi");
  };
  const toplam = tekrarlayan.filter(t => {
    if (!t.aktif) return false;
    const bugunDate = yerelTarihOku(yerelTarihAnahtari());
    if (t.baslangic && yerelTarihOku(t.baslangic) > bugunDate) return false;
    if (t.bitis && yerelTarihOku(t.bitis) < bugunDate) return false;
    return true;
  }).reduce((s, t) => paraTopla(s, t.tutar), 0);

  return (
    <div>
      <div style={{ background: "var(--card)", border: "1px solid #f59e0b33", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 20px", marginBottom: 16, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em" }}>Aylık Sabit Gider</div>
          <div style={{ color: "#f59e0b", fontSize: 24, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginTop: 2 }}>₺{fmt(toplam)}</div>
        </div>
        <Btn onClick={() => { setDuzenleId(null); setForm(bos); setModal(true); }}>+ Ekle</Btn>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {tekrarlayan.map(t => {
          const katRenk = kategoriGorunumu(t.kategori,"gider",kategoriGorunumleri).renk;
          return (
            <div key={t.id} style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 14, padding: "14px 16px", display: "flex", alignItems: "center", gap: 12, opacity: t.aktif ? 1 : 0.45 }}>
              <div style={{ flex: 1 }}>
                <div style={{ color: "var(--text)", fontSize: 14, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{t.aciklama}</div>
                <div style={{ display: "flex", gap: 8, marginTop: 4, alignItems: "center" }}><KategoriSimge kategori={t.kategori} tip="gider" size={24}/><Badge text={t.kategori} color={katRenk} /><span style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Her ayın {t.gun}. günü</span>
                {t.bitis && <span style={{ color: "#f59e0b", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Bitiş: {t.bitis}</span>}
                {t.baslangic && yerelTarihOku(t.baslangic) > yerelTarihOku(yerelTarihAnahtari()) && <span style={{ color: "#A74E32", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Başlangıç: {t.baslangic}</span>}
                </div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div style={{ color: "var(--expense-text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, fontSize: 15 }}>₺{fmt(t.tutar)}</div>
                <div style={{ display: "flex", gap: 6, marginTop: 4, justifyContent: "flex-end" }}>
                  <button onClick={() => setTekrarlayan(p => p.map(x => x.id === t.id ? { ...x, aktif: !x.aktif } : x))} style={{ background: t.aktif ? "#10b98122" : "var(--border)", border: "none", borderRadius: 6, color: t.aktif ? "#10b981" : "var(--text4)", padding: "3px 8px", fontSize: 11, cursor: "pointer", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{t.aktif ? "Aktif" : "Pasif"}</button>
                  <button onClick={() => duzenleAc(t)} style={{ background: "none", border: "none", color: "#f59e0b", cursor: "pointer", fontSize: 14 }} aria-label="Düzenle"><Icon name="edit" size={16}/></button>
                  <button onClick={() => setTekrarlayan(p => p.filter(x => x.id !== t.id))} style={{ background: "none", border: "none", color: "var(--text5)", cursor: "pointer", fontSize: 14 }} aria-label="Sil"><Icon name="trash" size={16}/></button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      {modal && (
        <Modal title={duzenleId ? "Tekrarlayan Düzenle" : "Tekrarlayan Ekle"} onClose={() => { setModal(false); setDuzenleId(null); setForm(bos); }}>
          <Input label="Açıklama" value={form.aciklama} onChange={e => setForm(f => ({ ...f, aciklama: e.target.value }))} placeholder="Örn: Netflix" />
          <Input label="Tutar (₺)" type="number" value={form.tutar} onChange={e => setForm(f => ({ ...f, tutar: e.target.value }))} placeholder="0.00" />
          <Input label="Her ayın kaçıncı günü?" type="number" min="1" max="31" value={form.gun} onChange={e => setForm(f => ({ ...f, gun: e.target.value }))} />
          <Select label="Kategori" value={form.kategori} onChange={e => setForm(f => ({ ...f, kategori: e.target.value }))}>{(kategoriler?.gider || VARSAYILAN_KATEGORILER.gider).map(k => <option key={k}>{k}</option>)}</Select>
          <Input label="Başlangıç Tarihi" type="date" value={form.baslangic} onChange={e => setForm(f => ({ ...f, baslangic: e.target.value }))} />
          <Input label="Bitiş Tarihi (boş = süresiz)" type="date" value={form.bitis} onChange={e => setForm(f => ({ ...f, bitis: e.target.value }))} />
          <Btn onClick={kaydet} style={{ width: "100%", padding: 12, marginTop: 8 }}>{duzenleId ? "Güncelle" : "Kaydet"}</Btn>
        </Modal>
      )}
    </div>
  );
}

// ── PORTFÖY ───────────────────────────────────────────────────────────────────

// ── MAIN ──────────────────────────────────────────────────────────────────────



// ── TAKSİTLİ HARCAMALAR ───────────────────────────────────────────────────────
function TaksitlerTab({ taksitler, setTaksitler }) {
  const [modal, setModal] = useState(false);
  const bos = { ad: "", aylikTutar: "", vade: "", odenen: "", kart: "" };
  const [form, setForm] = useState(bos);
  const [duzenleId, setDuzenleId] = useState(null);
  const fmt = n => n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const kaydet = () => {
    if (!form.ad.trim() || !tutarGecerli(form.aylikTutar) || !tamSayiGecerli(form.vade, 1, 1200)
      || !tamSayiGecerli(form.odenen || 0, 0, Number(form.vade))) return formHatasi("Ad, pozitif tutar ve geçerli vade/ödenen taksit sayısı gir.");
    const vade = parseInt(form.vade) || 1;
    const odenen = Math.max(0, Math.min(vade, parseInt(form.odenen) || 0));
    const veri = { ad: form.ad.trim(), aylikTutar: paraOku(form.aylikTutar), vade, odenen, kart: form.kart.trim() };
    if (duzenleId) {
      setTaksitler(p => p.map(x => x.id === duzenleId ? { ...x, ...veri } : x));
      toast("Taksit güncellendi");
    } else {
      setTaksitler(p => [...p, { id: Date.now(), ...veri }]);
      toast("Taksitli harcama eklendi");
    }
    setModal(false); setDuzenleId(null); setForm(bos);
  };
  const duzenleAc = (t) => { setDuzenleId(t.id); setForm({ ad: t.ad, aylikTutar: String(t.aylikTutar), vade: String(t.vade), odenen: String(t.odenen || 0), kart: t.kart || "" }); setModal(true); };
  const sil = (id) => { setTaksitler(p => p.filter(x => x.id !== id)); toast("Taksit silindi", "uyari"); };
  const taksitOde = (id) => { const t=taksitler.find(t=>t.id===id);if(!t||!odemeOnayi(`${t.ad} taksiti`,t.aylikTutar))return;setTaksitler(p => p.map(x => x.id === id ? { ...x, odenen: Math.min(x.vade, (x.odenen || 0) + 1) } : x)); toast("Taksit ödendi olarak işaretlendi"); };

  // Toplam özet
  const genelToplam = taksitler.reduce((s, t) => paraTopla(s, t.aylikTutar * t.vade), 0);
  const genelOdenen = taksitler.reduce((s, t) => paraTopla(s, t.aylikTutar * (t.odenen || 0)), 0);
  const genelKalan = genelToplam - genelOdenen;
  const genelYuzde = genelToplam > 0 ? (genelOdenen / genelToplam * 100) : 0;
  const aktifSayi = taksitler.filter(t => (t.odenen || 0) < t.vade).length;
  const aylikToplam = taksitler.filter(t => (t.odenen || 0) < t.vade).reduce((s, t) => paraTopla(s, t.aylikTutar), 0);

  // Sıralama: aktif olanlar önce, bitenler sona
  const sirali = [...taksitler].sort((a, b) => {
    const aBitti = (a.odenen || 0) >= a.vade, bBitti = (b.odenen || 0) >= b.vade;
    if (aBitti !== bBitti) return aBitti ? 1 : -1;
    return 0;
  });

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
        <span style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em" }}>{taksitler.length} TAKSİTLİ HARCAMA</span>
        <Btn onClick={() => { setDuzenleId(null); setForm(bos); setModal(true); }}>+ Ekle</Btn>
      </div>

      <div style={{ background: "var(--accent-soft)", border: "1px solid var(--border)", borderRadius: 12, padding: "9px 13px", marginBottom: 14, color: "#a78bfa", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", lineHeight: 1.5 }}>
        Buradaki taksitler giderinden düşmez, sadece takip amaçlıdır.
      </div>

      {taksitler.length === 0 && (
        <div style={{ textAlign: "center", color: "var(--text5)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", padding: "40px 0" }}>
          Henüz taksitli harcama eklemedin.<br />Taksitle aldığın ürünleri ekleyip takip edebilirsin.
        </div>
      )}

      {/* GENEL TOPLAM ÖZET */}
      {taksitler.length > 0 && (
        <div style={{ background: "var(--card)", border: "1px solid #8b5cf644", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 16px", marginBottom: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
            <span style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em" }}><span className="inline-icon"><Icon name="chart" size={16}/></span>Genel Durum</span>
            <span style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{aktifSayi} Aktif · Aylık ₺{fmt(aylikToplam)}</span>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 12 }}>
            <div style={{ background: "var(--card2)", borderRadius: 11, padding: "11px 6px", textAlign: "center" }}>
              <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 4 }}>TOPLAM BORÇ</div>
              <div style={{ color: "var(--text)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(genelToplam)}</div>
            </div>
            <div style={{ background: "var(--card2)", borderRadius: 11, padding: "11px 6px", textAlign: "center" }}>
              <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 4 }}>ÖDENEN</div>
              <div style={{ color: "var(--income-text)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(genelOdenen)}</div>
            </div>
            <div style={{ background: "var(--card2)", borderRadius: 11, padding: "11px 6px", textAlign: "center" }}>
              <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 4 }}>KALAN</div>
              <div style={{ color: "var(--expense-text)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(genelKalan)}</div>
            </div>
          </div>
          <div style={{ height: 10, background: "var(--card2)", borderRadius: 99, overflow: "hidden", border: "1px solid var(--border)" }}>
            <div style={{ height: "100%", width: `${genelYuzde}%`, background: "linear-gradient(90deg,#10b981,#059669)", borderRadius: 99, transition: "width 0.8s ease" }} />
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", marginTop: 6 }}>
            <span style={{ color: "var(--income-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>Ödenen %{genelYuzde.toFixed(1)}</span>
            <span style={{ color: "var(--expense-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>Kalan %{(100 - genelYuzde).toFixed(1)}</span>
          </div>
        </div>
      )}

      {/* TAKSİT KARTLARI */}
      {sirali.map((t, ti) => {
        const toplamBorc = t.aylikTutar * t.vade;
        const odenenTutar = t.aylikTutar * (t.odenen || 0);
        const kalan = toplamBorc - odenenTutar;
        const yuzde = toplamBorc > 0 ? (odenenTutar / toplamBorc * 100) : 0;
        const bitti = (t.odenen || 0) >= t.vade;
        const renk = "#8b5cf6";
        return (
          <div key={t.id} style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 18, boxShadow: "var(--card-shadow)", padding: "13px 14px", marginBottom: 11, opacity: bitti ? 0.6 : 1 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 11, marginBottom: 11 }}>
              <div style={{ width: 42, height: 42, borderRadius: 13, background: renk + "1c", border: `1px solid ${renk}3a`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 19, flexShrink: 0 }}>{taksitIkon(t.ad)}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ color: "var(--text)", fontSize: 14, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{t.ad}{bitti ? " ✓" : ""}</div>
                <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 2 }}>{t.kart ? t.kart + " · " : ""}Kalan: {t.vade - (t.odenen || 0)}/{t.vade} taksit</div>
              </div>
              <div style={{ textAlign: "right", flexShrink: 0 }}>
                <div style={{ color: "var(--text)", fontSize: 14, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(t.aylikTutar)}<span style={{ color: "var(--text5)", fontSize: 11 }}>/ay</span></div>
                <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 2 }}>Aylık Taksit</div>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 6, flexShrink: 0, marginLeft: 4 }}>
                <button onClick={() => duzenleAc(t)} style={{ background: "none", border: "none", color: "var(--text4)", cursor: "pointer", fontSize: 13, padding: 0 }} aria-label="Düzenle"><Icon name="edit" size={16}/></button>
                <button onClick={() => sil(t.id)} style={{ background: "none", border: "none", color: "var(--expense-text)", cursor: "pointer", fontSize: 13, padding: 0 }} aria-label="Sil"><Icon name="trash" size={16}/></button>
              </div>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 7, marginBottom: 10 }}>
              <div style={{ background: "var(--card2)", borderRadius: 9, padding: "8px 5px", textAlign: "center" }}>
                <div style={{ color: "var(--text5)", fontSize: 12.5, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 2 }}>TOPLAM</div>
                <div style={{ color: "var(--text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(toplamBorc)}</div>
              </div>
              <div style={{ background: "var(--card2)", borderRadius: 9, padding: "8px 5px", textAlign: "center" }}>
                <div style={{ color: "var(--text5)", fontSize: 12.5, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 2 }}>ÖDENEN</div>
                <div style={{ color: "var(--income-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(odenenTutar)}</div>
              </div>
              <div style={{ background: "var(--card2)", borderRadius: 9, padding: "8px 5px", textAlign: "center" }}>
                <div style={{ color: "var(--text5)", fontSize: 12.5, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 2 }}>KALAN</div>
                <div style={{ color: "var(--expense-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(kalan)}</div>
              </div>
            </div>
            <div style={{ height: 8, background: "var(--card2)", borderRadius: 99, overflow: "hidden", border: "1px solid var(--border)" }}>
              <div style={{ height: "100%", width: `${yuzde}%`, background: "linear-gradient(90deg,#10b981,#059669)", borderRadius: 99, transition: "width 0.8s ease" }} />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 6 }}>
              <span style={{ color: "var(--income-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>Ödenen %{yuzde.toFixed(1)}</span>
              {!bitti && <button onClick={() => taksitOde(t.id)} style={{ background: "#10b98122", border: "1px solid #10b98155", borderRadius: 8, padding: "3px 9px", color: "var(--income-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", cursor: "pointer", fontWeight: 600 }}>✓ Taksit Öde</button>}
              <span style={{ color: "var(--expense-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>Kalan %{(100 - yuzde).toFixed(1)}</span>
            </div>
          </div>
        );
      })}

      {modal && (
        <Modal title={duzenleId ? "Taksitli Harcamayı Düzenle" : "Taksitli Harcama Ekle"} onClose={() => { setModal(false); setDuzenleId(null); }}>
          <Input label="Ürün / Harcama Adı" value={form.ad} onChange={e => setForm(f => ({ ...f, ad: e.target.value }))} placeholder="Örn: Telefon" />
          <Input label="Aylık Taksit Tutarı (₺)" type="number" value={form.aylikTutar} onChange={e => setForm(f => ({ ...f, aylikTutar: e.target.value }))} placeholder="0.00" />
          <Input label="Toplam Taksit Sayısı (vade)" type="number" value={form.vade} onChange={e => setForm(f => ({ ...f, vade: e.target.value }))} placeholder="12" />
          <Input label="Şimdiye Kadar Ödenen Taksit" type="number" value={form.odenen} onChange={e => setForm(f => ({ ...f, odenen: e.target.value }))} placeholder="0" />
          <Input label="Kart / Kaynak (isteğe bağlı)" value={form.kart} onChange={e => setForm(f => ({ ...f, kart: e.target.value }))} placeholder="Örn: Bonus Kart" />
          <Btn onClick={kaydet} style={{ width: "100%", padding: 12, marginTop: 8 }}>Kaydet</Btn>
        </Modal>
      )}
    </div>
  );
}


// ── KREDİ / BORÇ TAKİBİ ───────────────────────────────────────────────────────
function KredilerTab({ krediler, setKrediler }) {
  const [modal, setModal] = useState(false);
  const [acikKredi, setAcikKredi] = useState({});
  const bos = { ad: "", toplamTutar: "", vade: "", baslangic: yerelTarihAnahtari(), aylikTaksit: "", ilkFarkli: false, ilkTaksit: "", sonFarkli: false, sonTaksit: "" };
  const [form, setForm] = useState(bos);
  const fmt = n => n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const taksitleriOlustur = (f) => {
    const vade = Number(f.vade);
    const toplam = paraKurus(f.toplamTutar);
    const ilk = f.ilkFarkli ? paraKurus(f.ilkTaksit) : null;
    const son = f.sonFarkli ? paraKurus(f.sonTaksit) : null;
    const ortaAdet = vade - (ilk !== null ? 1 : 0) - (son !== null ? 1 : 0);
    const kalan = toplam - (ilk || 0) - (son || 0);
    const otomatik = !f.aylikTaksit;
    const taban = ortaAdet > 0 ? Math.floor(kalan / ortaAdet) : 0;
    const ekstraKurus = ortaAdet > 0 ? kalan % ortaAdet : 0;
    let ortaSira = 0;
    return Array.from({ length: vade }, (_, i) => {
      let kurus;
      if (i === 0 && ilk !== null) kurus = ilk;
      else if (i === vade - 1 && son !== null) kurus = son;
      else { kurus = otomatik ? taban + (ortaSira < ekstraKurus ? 1 : 0) : paraKurus(f.aylikTaksit); ortaSira++; }
      return { no: i + 1, tarih: yerelTarihAnahtari(aylikTarih(f.baslangic, i)), tutar: kurus / 100, odendi: false };
    });
  };

  const ekle = () => {
    if (!form.ad.trim() || !tutarGecerli(form.toplamTutar) || !tamSayiGecerli(form.vade, 1, 1200)
      || !yerelTarihOku(form.baslangic) || (form.aylikTaksit && !tutarGecerli(form.aylikTaksit))
      || (form.ilkFarkli && !tutarGecerli(form.ilkTaksit)) || (form.sonFarkli && !tutarGecerli(form.sonTaksit))) return formHatasi("Kredi adı, pozitif tutarlar, geçerli tarih ve 1–1200 arasında vade gir.");
    if (Number(form.vade) === 1 && form.ilkFarkli && form.sonFarkli) return formHatasi("Tek taksitte ilk ve son tutar için yalnızca birini seç.");
    const taksitler = taksitleriOlustur(form);
    if (taksitler.some(t => !tutarGecerli(t.tutar))) return formHatasi("Bu tutarlar pozitif bir taksit planı oluşturmuyor.");
    if (paraKurus(paraTopla(...taksitler.map(t => t.tutar))) !== paraKurus(form.toplamTutar)) return formHatasi("Taksit toplamı faiz dahil toplam tutarla eşleşmeli.");
    setKrediler(p => [...p, {
      id: Date.now(),
      ad: form.ad.trim(),
      toplamTutar: paraOku(form.toplamTutar),
      vade: parseInt(form.vade),
      baslangic: form.baslangic,
      taksitler,
    }]);
    setModal(false);
    setForm(bos);
    toast("Kredi eklendi");
  };

  const taksitToggle = (krediId, no) => {
    const k=krediler.find(k=>k.id===krediId),t=k?.taksitler.find(t=>t.no===no);
    if(!t||(!t.odendi&&!odemeOnayi(`${k.ad||k.isim} · ${no}. taksit`,t.tutar)))return;
    setKrediler(p => p.map(k => k.id === krediId ? { ...k, taksitler: k.taksitler.map(t => t.no === no ? { ...t, odendi: !t.odendi } : t) } : k));
  };
  const krediSil = (id) => setKrediler(p => p.filter(k => k.id !== id));

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
        <span style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em" }}>{krediler.length} KREDİ / BORÇ</span>
        <Btn onClick={() => { setForm(bos); setModal(true); }}>+ Ekle</Btn>
      </div>

      {krediler.length === 0 && (
        <div style={{ textAlign: "center", color: "var(--text5)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", padding: "40px 0" }}>
          Henüz kredi eklemedin.<br />Çektiğin krediyi ekleyip taksitleri takip edebilirsin.
        </div>
      )}

      {krediler.map(k => {
        const toplamBorc = k.taksitler.reduce((s, t) => paraTopla(s, t.tutar), 0);
        const odenen = k.taksitler.filter(t => t.odendi).reduce((s, t) => paraTopla(s, t.tutar), 0);
        const kalan = toplamBorc - odenen;
        const yuzde = toplamBorc > 0 ? (odenen / toplamBorc * 100) : 0;
        const odenenAdet = k.taksitler.filter(t => t.odendi).length;
        // Sıralama: ödenmemişler önce (tarihe göre), ödenenler sona
        const sirali = [...k.taksitler].sort((a, b) => {
          if (a.odendi !== b.odendi) return a.odendi ? 1 : -1;
          return a.no - b.no;
        });
        const acik = acikKredi[k.id];
        return (
          <div key={k.id} style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 14px", marginBottom: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 12 }}>
              <div>
                <div style={{ color: "var(--text)", fontSize: 15, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{k.ad}</div>
                <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 2 }}>{odenenAdet}/{k.vade} taksit ödendi</div>
              </div>
              <button onClick={() => krediSil(k.id)} style={{ background: "none", border: "none", color: "var(--expense-text)", cursor: "pointer", fontSize: 15 }} aria-label="Sil"><Icon name="trash" size={16}/></button>
            </div>

            {/* Yatay ilerleme çubuğu */}
            <div style={{ height: 16, background: "var(--card2)", borderRadius: 99, overflow: "hidden", marginBottom: 6, position: "relative", border: "1px solid var(--border)" }}>
              <div style={{ height: "100%", width: `${yuzde}%`, background: "linear-gradient(90deg,#10b981,#059669)", borderRadius: 99, transition: "width 0.8s ease" }} />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 14 }}>
              <span style={{ color: "var(--income-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>Ödenen %{yuzde.toFixed(1)}</span>
              <span style={{ color: "var(--expense-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>Kalan %{(100 - yuzde).toFixed(1)}</span>
            </div>

            {/* Tutar özeti */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 12 }}>
              <div style={{ background: "var(--card2)", borderRadius: 10, padding: "9px 6px", textAlign: "center" }}>
                <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 3 }}>TOPLAM</div>
                <div style={{ color: "var(--text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(toplamBorc)}</div>
              </div>
              <div style={{ background: "var(--card2)", borderRadius: 10, padding: "9px 6px", textAlign: "center" }}>
                <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 3 }}>ÖDENEN</div>
                <div style={{ color: "var(--income-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(odenen)}</div>
              </div>
              <div style={{ background: "var(--card2)", borderRadius: 10, padding: "9px 6px", textAlign: "center" }}>
                <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 3 }}>KALAN</div>
                <div style={{ color: "var(--expense-text)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>₺{fmt(kalan)}</div>
              </div>
            </div>

            {/* Taksitler */}
            <button onClick={() => setAcikKredi(p => ({ ...p, [k.id]: !p[k.id] }))} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "9px", color: "var(--text3)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 12, cursor: "pointer" }}>
              {acik ? "▾ Taksitleri Gizle" : "▸ Taksitleri Göster"}
            </button>

            {acik && (
              <div style={{ marginTop: 10 }}>
                {sirali.map(t => (
                  <div key={t.no} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 10px", background: t.odendi ? "transparent" : "var(--card2)", borderRadius: 10, marginBottom: 6, opacity: t.odendi ? 0.5 : 1, border: t.odendi ? "1px solid var(--divider)" : "1px solid var(--border)" }}>
                    <button onClick={() => taksitToggle(k.id, t.no)} style={{ width: 20, height: 20, borderRadius: 6, border: `2px solid ${t.odendi ? "#10b981" : "var(--border2)"}`, background: t.odendi ? "#10b981" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, color: "#fff", padding: 0 }}>{t.odendi ? "✓" : ""}</button>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ color: "var(--text)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textDecoration: t.odendi ? "line-through" : "none" }}>{t.no}. Taksit</div>
                      <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{t.tarih.split("-").reverse().join(".")}</div>
                    </div>
                    <span style={{ color: t.odendi ? "var(--text5)" : "#ef4444", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, textDecoration: t.odendi ? "line-through" : "none" }}>₺{fmt(t.tutar)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}

      {modal && (
        <Modal title="Kredi / Borç Ekle" onClose={() => setModal(false)}>
          <Input label="Kredi Adı" value={form.ad} onChange={e => setForm(f => ({ ...f, ad: e.target.value }))} placeholder="Örn: İhtiyaç Kredisi" />
          <Input label="Toplam Tutar — faiz dahil (₺)" type="number" value={form.toplamTutar} onChange={e => setForm(f => ({ ...f, toplamTutar: e.target.value }))} placeholder="0.00" />
          <Input label="Vade (kaç ay)" type="number" value={form.vade} onChange={e => setForm(f => ({ ...f, vade: e.target.value }))} placeholder="12" />
          <Input label="İlk Taksit Tarihi" type="date" value={form.baslangic} onChange={e => setForm(f => ({ ...f, baslangic: e.target.value }))} />
          <Input label="Aylık Taksit (₺) — boşsa otomatik hesaplanır" type="number" value={form.aylikTaksit} onChange={e => setForm(f => ({ ...f, aylikTaksit: e.target.value }))} placeholder="0.00" />

          <label style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, marginBottom: 8, cursor: "pointer" }}>
            <button type="button" onClick={() => setForm(f => ({ ...f, ilkFarkli: !f.ilkFarkli }))} style={{ width: 22, height: 22, borderRadius: 6, border: `2px solid ${form.ilkFarkli ? "#f59e0b" : "var(--border2)"}`, background: form.ilkFarkli ? "#f59e0b" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, color: "#fff", padding: 0 }}>{form.ilkFarkli ? "✓" : ""}</button>
            <span style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>İlk taksit farklı</span>
          </label>
          {form.ilkFarkli && <Input label="İlk Taksit Tutarı (₺)" type="number" value={form.ilkTaksit} onChange={e => setForm(f => ({ ...f, ilkTaksit: e.target.value }))} placeholder="0.00" />}

          <label style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 12px", background: "var(--card2)", borderRadius: 10, marginBottom: 8, cursor: "pointer" }}>
            <button type="button" onClick={() => setForm(f => ({ ...f, sonFarkli: !f.sonFarkli }))} style={{ width: 22, height: 22, borderRadius: 6, border: `2px solid ${form.sonFarkli ? "#f59e0b" : "var(--border2)"}`, background: form.sonFarkli ? "#f59e0b" : "transparent", cursor: "pointer", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, color: "#fff", padding: 0 }}>{form.sonFarkli ? "✓" : ""}</button>
            <span style={{ color: "var(--text2)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Son taksit farklı</span>
          </label>
          {form.sonFarkli && <Input label="Son Taksit Tutarı (₺)" type="number" value={form.sonTaksit} onChange={e => setForm(f => ({ ...f, sonTaksit: e.target.value }))} placeholder="0.00" />}

          <Btn onClick={ekle} style={{ width: "100%", padding: 12, marginTop: 8 }}>Kaydet</Btn>
        </Modal>
      )}
    </div>
  );
}


// ── MAAŞ HESAPLAMA ────────────────────────────────────────────────────────────
const MAAS_AYLAR = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];

function MaasTab({ zamlar, setZamlar, maasAyarlar, setMaasAyarlar, enflasyon, setEnflasyon }) {
  const [altSekme, setAltSekme] = useState("hesapla");
  const buYil = new Date().getFullYear();

  // Taslak (draft) — kaydedilene kadar kalıcı veriye yazılmaz
  const [dZamlar, setDZamlar] = useState(zamlar);
  const [dEnflasyon, setDEnflasyon] = useState(enflasyon);
  const [dMaas, setDMaas] = useState(maasAyarlar);

  // Kaydedilmemiş değişiklik var mı?
  const degisti = JSON.stringify(dZamlar) !== JSON.stringify(zamlar) || JSON.stringify(dEnflasyon) !== JSON.stringify(enflasyon) || JSON.stringify(dMaas) !== JSON.stringify(maasAyarlar);

  const kaydet = () => {
    if (dMaas.mevcutMaas && !tutarGecerli(dMaas.mevcutMaas)) return formHatasi("Geçerli, pozitif bir maaş tutarı gir.");
    if ([dMaas.zamYil1, dMaas.hesapYil].some(v => v && !tamSayiGecerli(v, 2000, 2100))) return formHatasi("2000–2100 arasında bir yıl gir.");
    if ([...Object.values(dZamlar), ...Object.values(dEnflasyon)].some(v => v !== "" && (!Number.isFinite(sayisalOku(v)) || sayisalOku(v) <= -100 || sayisalOku(v) > 10000))) return formHatasi("Oranlar geçerli sayı olmalı ve −%100'den büyük olmalı.");
    setZamlar(dZamlar);
    setEnflasyon(dEnflasyon);
    setMaasAyarlar(dMaas);
    toast("Maaş bilgileri kaydedildi");
  };

  const yil1 = parseInt(dMaas.zamYil1) || buYil;
  const yil2 = yil1 + 1;

  const fmtTL = n => "₺" + n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const fmtPct = n => "%" + n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const zamAl = (yil, donem) => {
    const v = dZamlar[`${yil}-${donem}`];
    return v !== undefined && v !== "" ? sayisalOku(v) : 0;
  };
  const enfAl = (yil, ay) => {
    const v = dEnflasyon[`${yil}-${ay}`];
    return v !== undefined && v !== "" ? sayisalOku(v) : 0;
  };

  // ── HESAP MOTORU ──
  // Mevcut maaş zaten o dönemin zammını almıştır. Dönem zammı SADECE
  // enflasyon farkı hesabında kullanılır, maaşa tekrar uygulanmaz.
  const hesapla = useMemo(() => {
    const mevcutMaas = paraOku(dMaas.mevcutMaas) || 0;
    if (mevcutMaas === 0) return null;

    const secYil = parseInt(dMaas.hesapYil) || buYil;
    const secDonem = dMaas.hesapDonem || "ocak";
    const aylar = secDonem === "ocak" ? [0,1,2,3,4,5] : [6,7,8,9,10,11];
    const zamDonem = secDonem === "ocak" ? "ocak" : "temmuz";

    // Bu dönemin başında alınan zam (enflasyon farkı kıyas tabanı)
    const donemZammi = zamAl(secYil, zamDonem);

    // 6 ayın kümülatif enflasyonu
    let kumulatif = 1;
    const ayDetay = [];
    aylar.forEach(ayIdx => {
      const enf = enfAl(secYil, ayIdx);
      kumulatif *= (1 + enf / 100);
      ayDetay.push({ ay: MAAS_AYLAR[ayIdx], enf, kumulatif: (kumulatif - 1) * 100 });
    });
    const kumEnflasyon = (kumulatif - 1) * 100;

    // Enflasyon farkı (bileşik): (1+kümEnf)/(1+dönemZammı) - 1, negatifse 0
    const enfFarki = Math.max(0, ((1 + kumEnflasyon / 100) / (1 + donemZammi / 100) - 1) * 100);

    // Sonraki dönem zammı
    let sonrakiYil = secYil, sonrakiDonem;
    if (secDonem === "ocak") { sonrakiDonem = "temmuz"; }
    else { sonrakiDonem = "ocak"; sonrakiYil = secYil + 1; }
    const sonrakiZam = zamAl(sonrakiYil, sonrakiDonem);

    // Tahmini maaş = mevcut × (1 + enfFarkı) × (1 + sonrakiZam)
    // (Mevcut maaşa dönemin kendi zammı UYGULANMAZ, zaten içinde.)
    const tahminiMaas = mevcutMaas * (1 + enfFarki / 100) * (1 + sonrakiZam / 100);

    return {
      mevcutMaas, donemZammi, kumEnflasyon, enfFarki, zamDonem,
      ayDetay, secYil, secDonem,
      sonrakiYil, sonrakiDonem, sonrakiZam, tahminiMaas,
    };
  }, [dMaas, dZamlar, dEnflasyon]);

  return (
    <div>
      {/* Alt sekme */}
      <div style={{ display: "flex", gap: 6, marginBottom: 14, background: "var(--card2)", borderRadius: 12, padding: 4 }}>
        {[{id:"hesapla",label:"Maaş Hesapla"},{id:"zamlar",label:"Zam Oranları"}].map(s => (
          <button key={s.id} onClick={() => setAltSekme(s.id)} style={{ flex: 1, padding: "9px 4px", borderRadius: 9, border: "none", cursor: "pointer", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 12, fontWeight: 600, background: altSekme === s.id ? "#A74E32" : "transparent", color: altSekme === s.id ? "#fff" : "var(--text4)", transition: "all 0.2s" }}>
            {s.label}
          </button>
        ))}
      </div>

      {/* ZAM ORANLARI */}
      {altSekme === "zamlar" && (
        <div>
          <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 14px", marginBottom: 14 }}>
            <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Başlangıç Yılı</div>
            <input aria-label="Başlangıç yılı" type="number" value={dMaas.zamYil1} onChange={e => setDMaas(p => ({ ...p, zamYil1: e.target.value }))} placeholder={String(buYil)} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 12px", color: "#f59e0b", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 16, fontWeight: 600, outline: "none", boxSizing: "border-box", textAlign: "center" }} />
            <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 6, textAlign: "center" }}>{yil1}-{yil2} için 4 dönem zammı</div>
          </div>

          {[yil1, yil2].map(yil => (
            <div key={yil} style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 14px", marginBottom: 14 }}>
              <div style={{ color: "var(--text)", fontSize: 14, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600, marginBottom: 12 }}>{yil} Toplu Sözleşme Zamları</div>
              {[{id:"ocak",label:"Ocak Zammı"},{id:"temmuz",label:"Temmuz Zammı"}].map(d => (
                <div key={d.id} style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
                  <span style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", width: 110 }}>{d.label}</span>
                  <div style={{ flex: 1, position: "relative" }}>
                    <input aria-label={`${yil} ${d.label}`} type="number" step="0.01" value={dZamlar[`${yil}-${d.id}`] || ""} onChange={e => setDZamlar(p => ({ ...p, [`${yil}-${d.id}`]: e.target.value }))} placeholder="0.00" style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 8, padding: "8px 28px 8px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 14, outline: "none", boxSizing: "border-box" }} />
                    <span style={{ position: "absolute", right: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text4)", fontSize: 13 }}>%</span>
                  </div>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {/* MAAŞ HESAPLA */}
      {altSekme === "hesapla" && (
        <div>
          <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 14px", marginBottom: 14 }}>
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: "block", color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 5 }}>Mevcut (güncel) maaşın ₺</label>
              <input aria-label="Mevcut güncel maaş" type="number" step="0.01" value={dMaas.mevcutMaas} onChange={e => setDMaas(p => ({ ...p, mevcutMaas: e.target.value }))} placeholder="0.00" style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 14, outline: "none", boxSizing: "border-box" }} />
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 5 }}>Yıl</label>
                <input aria-label="Hesaplama yılı" type="number" value={dMaas.hesapYil} onChange={e => setDMaas(p => ({ ...p, hesapYil: e.target.value }))} placeholder={String(buYil)} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 14, outline: "none", boxSizing: "border-box" }} />
              </div>
              <div style={{ flex: 1.4 }}>
                <label style={{ display: "block", color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 5 }}>Dönem</label>
                <select aria-label="Maaş hesaplama dönemi" value={dMaas.hesapDonem} onChange={e => setDMaas(p => ({ ...p, hesapDonem: e.target.value }))} style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 10, padding: "10px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }}>
                  <option value="ocak">Ocak - Haziran</option>
                  <option value="temmuz">Temmuz - Aralık</option>
                </select>
              </div>
            </div>
          </div>

          {/* Aylık enflasyon */}
          <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: "1px solid var(--border-light)", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "16px 14px", marginBottom: 14 }}>
            <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>Aylık Enflasyon (%)</div>
            <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginBottom: 12 }}>Bilmediğin aylar boş kalabilir</div>
            {(dMaas.hesapDonem === "ocak" ? [0,1,2,3,4,5] : [6,7,8,9,10,11]).map(ayIdx => {
              const yil = parseInt(dMaas.hesapYil) || buYil;
              return (
                <div key={ayIdx} style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 8 }}>
                  <span style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", width: 70 }}>{MAAS_AYLAR[ayIdx]}</span>
                  <div style={{ flex: 1, position: "relative" }}>
                    <input aria-label={`${yil} ${MAAS_AYLAR[ayIdx]} enflasyon oranı`} type="number" step="0.01" value={dEnflasyon[`${yil}-${ayIdx}`] || ""} onChange={e => setDEnflasyon(p => ({ ...p, [`${yil}-${ayIdx}`]: e.target.value }))} placeholder="boş" style={{ width: "100%", background: "var(--card2)", border: "1px solid var(--border)", borderRadius: 8, padding: "8px 28px 8px 12px", color: "var(--text)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                    <span style={{ position: "absolute", right: 12, top: "50%", transform: "translateY(-50%)", color: "var(--text4)", fontSize: 12 }}>%</span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* SONUÇ */}
          {hesapla ? (
            <div>
              <div style={{ background: "linear-gradient(135deg,var(--card),var(--card2))", border: "1px solid #A74E3233", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", marginBottom: 14 }}>
                <div style={{ color: "var(--text3)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 14 }}>Hesaplama Detayı</div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid var(--divider)" }}>
                  <span style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Mevcut maaş</span>
                  <span style={{ color: "var(--text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{fmtTL(hesapla.mevcutMaas)}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid var(--divider)" }}>
                  <span style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>6 aylık kümülatif enflasyon</span>
                  <span style={{ color: "#f59e0b", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{fmtPct(hesapla.kumEnflasyon)}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid var(--divider)" }}>
                  <span style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>Enflasyon farkı</span>
                  <span style={{ color: hesapla.enfFarki > 0 ? "#10b981" : "var(--text4)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{hesapla.enfFarki > 0 ? fmtPct(hesapla.enfFarki) : "Yok"}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 0" }}>
                  <span style={{ color: "var(--text3)", fontSize: 12, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" }}>{hesapla.sonrakiDonem === "ocak" ? `${hesapla.sonrakiYil} Ocak` : "Temmuz"} zammı</span>
                  <span style={{ color: "var(--income-text)", fontSize: 13, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{fmtPct(hesapla.sonrakiZam)}</span>
                </div>
              </div>

              <div style={{ background: "var(--card)", border: "1px solid #10b98144", borderRadius: 20, boxShadow: "var(--card-shadow)", padding: "18px 16px", textAlign: "center" }}>
                <div style={{ color: "var(--text4)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>
                  {hesapla.sonrakiDonem === "ocak" ? `${hesapla.sonrakiYil} Ocak` : `${hesapla.secYil} Temmuz`} Tahmini Maaşın
                </div>
                <div style={{ color: "var(--income-text)", fontSize: 26, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 600 }}>{fmtTL(hesapla.tahminiMaas)}</div>
                <div style={{ color: "var(--text5)", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 8 }}>
                  Mevcuttan {fmtPct((hesapla.tahminiMaas / hesapla.mevcutMaas - 1) * 100)} artış
                </div>
              </div>
            </div>
          ) : (
            <div style={{ textAlign: "center", color: "var(--text5)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 13, padding: "30px 0" }}>
              Mevcut maaşını gir, hesaplama burada görünsün
            </div>
          )}
        </div>
      )}

      {/* KAYDET BUTONU */}
      <button onClick={kaydet} disabled={!degisti} style={{ width: "100%", marginTop: 8, padding: 14, borderRadius: 12, border: "none", cursor: degisti ? "pointer" : "default", background: degisti ? "linear-gradient(135deg,#10b981,#059669)" : "var(--card2)", color: degisti ? "#fff" : "var(--text5)", fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontSize: 14, fontWeight: 600, transition: "all 0.2s" }}>
        {degisti ? "Kaydet" : "✓ Kaydedildi"}
      </button>
      {degisti && (
        <div style={{ textAlign: "center", color: "#f59e0b", fontSize: 11, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", marginTop: 6 }}>
          Kaydedilmemiş değişiklik var
        </div>
      )}
    </div>
  );
}



export default function App() {
  const topbarRef = useRef(null);
  useEffect(()=>{
    const el=topbarRef.current;
    if(!el)return;
    const measure=()=>document.documentElement.style.setProperty('--topbar-height',`${el.getBoundingClientRect().height}px`);
    measure();
    const observer=typeof ResizeObserver!=='undefined'?new ResizeObserver(measure):null;
    observer?.observe(el);
    window.addEventListener('resize',measure);
    return ()=>{observer?.disconnect();window.removeEventListener('resize',measure);};
  },[]);
  const [aktifTab, setAktifTab] = useState("ozet");

  // 💾 TÜM VERİLER LOCALSTORAGE'DA KAYDEDILIYOR
  const [islemler, setIslemler] = useLocalStorage("ft_islemler", INITIAL_ISLEMLER);
  const [odemeler, setOdemeler] = useLocalStorage("ft_odemeler", []);
  const [kartlar, setKartlar] = useLocalStorage("ft_kartlar", []);
  const [kartOdemeleri, setKartOdemeleri] = useLocalStorage("ft_kart_odemeleri", []);
  const [kartBorcOzetleri,setKartBorcOzetleri]=useLocalStorage("ft_kart_borc_ozetleri",{});
  const [bakiyeHesapModu, setBakiyeHesapModu] = useLocalStorage("ft_bakiye_hesap_modu", "kartlar_dahil");
  const [hizliSablonlar, setHizliSablonlar] = useLocalStorage("ft_hizli_sablonlar", []);
  const [devirLog,setDevirLog] = useLocalStorage("ft_donem_devir_log", {});
  const [donemGun, setDonemGun] = useLocalStorage("ft_donem_gun", "15");
  const [tekrarlayan, setTekrarlayan] = useLocalStorage("ft_tekrarlayan", INITIAL_TEKRARLAYAN);
  const [butceler, setButceler] = useLocalStorage("ft_butceler", {});
  const [kategoriGorunumleri, setKategoriGorunumleri] = useLocalStorage("ft_kategori_gorunumleri", {});
  const [kategoriler, setKategoriler] = useLocalStorage("ft_kategoriler", VARSAYILAN_KATEGORILER);

  // 💾 Hangi ay/yıl sabit gider eklendi kaydı
  const [sabitEklemeLog, setSabitEklemeLog] = useLocalStorage("ft_sabit_log", {});

  // Maaş hesaplama verileri
  const [krediler, setKrediler] = useLocalStorage("ft_krediler", []);
  const [taksitler, setTaksitler] = useLocalStorage("ft_taksitler", []);
  const [zamlar, setZamlar] = useLocalStorage("ft_zamlar", {});
  const [enflasyon, setEnflasyon] = useLocalStorage("ft_enflasyon", {});
  const [maasAyarlar, setMaasAyarlar] = useLocalStorage("ft_maas_ayarlar", { mevcutMaas: "", zamYil1: "", hesapYil: "", hesapDonem: "ocak" });

  // Hamburger menü
  const [menuAcik, setMenuAcik] = useState(false);
  const [hizliModal, setHizliModal] = useState(false);
  const [grafikTipi,setGrafikTipi] = useLocalStorage("ft_grafik_tipi", "halka");
  const [tasarrufHedefi,setTasarrufHedefi] = useLocalStorage("ft_tasarruf_hedefi", 0);
  const [tema, setTema] = useLocalStorage("ft_tema", "light");
  const [bildirimAyarlari, setBildirimAyarlari] = useLocalStorage("ft_bildirim_ayarlari", { gunler: [0, 1], saat: "18:00" });
  const [bildirimYenileme, setBildirimYenileme] = useState(0);
  const [sonBildirimPlanlama, setSonBildirimPlanlama] = useLocalStorage("ft_son_bildirim_planlama", null);
  useEffect(()=>{
    try{
      if(!localStorage.getItem('ft_auto_backup_finance_v170'))localStorage.setItem('ft_auto_backup_finance_v170',JSON.stringify(yerelYedekPaketi()));
      otomatikYedekle();
    }catch{kayitHatasiBildir('Otomatik güvenlik kopyası oluşturulamadı. Depolama alanını kontrol edip dosya yedeği al.');}
  },[]);
  useEffect(()=>{
    const id=setTimeout(()=>{try{otomatikYedekle();}catch{kayitHatasiBildir('Otomatik güvenlik kopyası güncellenemedi; dosya yedeği al.');}},1500);
    return()=>clearTimeout(id);
  },[islemler,odemeler,kartlar,kartOdemeleri,donemGun,tekrarlayan,butceler,kategoriler,krediler,taksitler,devirLog,kartBorcOzetleri,bildirimYenileme]);
  useEffect(() => {
    try {
      const hedefSurum = "1.7.0";
      const mevcutSurum = JSON.parse(window.localStorage.getItem("ft_schema_version") || "null");
      if (mevcutSurum === hedefSurum) return;
      if (!window.localStorage.getItem("ft_auto_backup_v120")) {
        const data = {};
        for (let i = 0; i < window.localStorage.length; i += 1) {
          const key = window.localStorage.key(i);
          if (key?.startsWith("ft_") && !key.startsWith("ft_auto_backup")) {
            try { data[key] = JSON.parse(window.localStorage.getItem(key)); } catch (e) {}
          }
        }
        window.localStorage.setItem("ft_auto_backup_v120", JSON.stringify({ olusturulma: new Date().toISOString(), surum: mevcutSurum || "1.2.0", data }));
      }
      if (!window.localStorage.getItem("ft_auto_backup_v130")) {
        const data = {};
        for (let i = 0; i < window.localStorage.length; i += 1) {
          const key = window.localStorage.key(i);
          if (key?.startsWith("ft_") && !key.startsWith("ft_auto_backup")) {
            try { data[key] = JSON.parse(window.localStorage.getItem(key)); } catch (e) {}
          }
        }
        window.localStorage.setItem("ft_auto_backup_v130", JSON.stringify({ olusturulma: new Date().toISOString(), surum: mevcutSurum || "1.3.0", data }));
      }
      if (!window.localStorage.getItem("ft_auto_backup_v140")) {
        const data = {};
        for (let i = 0; i < window.localStorage.length; i += 1) {
          const key = window.localStorage.key(i);
          if (key?.startsWith("ft_") && !key.startsWith("ft_auto_backup")) {
            try { data[key] = JSON.parse(window.localStorage.getItem(key)); } catch (e) {}
          }
        }
        window.localStorage.setItem("ft_auto_backup_v140", JSON.stringify({ olusturulma: new Date().toISOString(), surum: mevcutSurum || "1.4.0", data }));
      }
      if (!window.localStorage.getItem("ft_auto_backup_v161")) {
        const data = {};
        for (let i = 0; i < window.localStorage.length; i += 1) {
          const key = window.localStorage.key(i);
          if (key?.startsWith("ft_") && !key.startsWith("ft_auto_backup")) {
            try { data[key] = JSON.parse(window.localStorage.getItem(key)); } catch (e) {}
          }
        }
        window.localStorage.setItem("ft_auto_backup_v161", JSON.stringify({ olusturulma: new Date().toISOString(), surum: mevcutSurum || "1.6.0", data }));
      }
      window.localStorage.setItem("ft_schema_version", JSON.stringify(hedefSurum));
    } catch { kayitHatasiBildir("Otomatik güvenlik kopyası veya sürüm kaydı yazılamadı. Depolama alanını kontrol edip dosya yedeği al."); }
  }, []);
  useEffect(() => {
    try {
      document.documentElement.setAttribute("data-theme", tema);
      document.body.style.background = tema === "light" ? "#FCF5EF" : tema === "gray" ? "#b7bec5" : "#0e131c";
    } catch (e) {}
  }, [tema]);

  useEffect(() => {
    const yenile = () => { if (document.visibilityState === "visible") setBildirimYenileme(v => v + 1); };
    window.addEventListener("focus", yenile);
    document.addEventListener("visibilitychange", yenile);
    return () => { window.removeEventListener("focus", yenile); document.removeEventListener("visibilitychange", yenile); };
  }, []);

  // Sıra, StrictMode ve hızlı düzenlemelerde eski planın yenisini silmesini önler.
  useEffect(() => {
    let iptal = false;
    const planla = async () => {
      if (iptal) return;
      const LN = bildirimEklentisi();
      if (!LN) return; // Tarayıcıda Android alarmı varmış gibi gösterme.
      try {
        const simdi = new Date();
        const kartHatirlatmalari = kartHatirlatmalariniOlustur(kartlar, kartOdemeleri, simdi);
        const tumu = [...odemeler, ...kartHatirlatmalari, ...krediBildirimleri(krediler)];
        const bekleyen = (await LN.getPending()).notifications || [];
        const sahipli = bekleyen.filter(n => n.extra?.finoraTip === "odeme_hatirlatma");
        const izin = await LN.checkPermissions();
        if (izin.display !== "granted") {
          // Ödenen/silinen kayıtların eski alarmlarını izin kapalıyken de kaldır.
          if (sahipli.length) await LN.cancel({ notifications: sahipli.map(n => ({ id: n.id })) });
          if (!iptal) setSonBildirimPlanlama({ zaman: simdi.toISOString(), sayi: 0, hata: "Ayarlardan Arka Plan Bildirimlerini Etkinleştir düğmesiyle bildirim iznini aç." });
          return;
        }
        const kesin = await LN.checkExactNotificationSetting();
        const kanalId = await bildirimKanaliHazirla(LN);
        let telafiLog = {};
        try { telafiLog = JSON.parse(localStorage.getItem("ft_bildirim_telafi_log") || "{}"); } catch { /* Bozuk geçmiş planı engellemesin. */ }
        if (!telafiLog || typeof telafiLog !== "object" || Array.isArray(telafiLog)) telafiLog = {};
        const planlar = bildirimPlanlariniOlustur(tumu, bildirimAyarlari, simdi).map(plan => {
          if (!plan.telafi) return plan;
          const mevcut = sahipli.find(n => n.extra?.planAnahtari === plan.anahtar);
          // Aynı gün tekrar açılışta gönderilmiş hatırlatmayı çoğaltma.
          const odemeler = plan.odemeler.filter(o => !telafiLog[`${o.tarih}:${o.id}`] || mevcut?.extra?.odemeAnahtarlari?.includes(`${o.tarih}:${o.id}`));
          const eskiZaman = mevcut?.schedule?.at ? new Date(mevcut.schedule.at) : null;
          return { ...plan, odemeler, at: eskiZaman && eskiZaman > simdi ? eskiZaman : plan.at };
        }).filter(p => p.odemeler.length);
        const bildirimler = planlar.map(plan => ({
          id: bildirimKimligi(plan.anahtar),
          title: "Ödeme Hatırlatması",
          body: plan.odemeler.map(o => `${o.aciklama || o.kategori || "Ödeme"}: ${o.onceGun === 0 ? "bugün" : o.onceGun === 1 ? "yarın" : `${o.onceGun} gün sonra`} son ödeme günü`).join(" · "),
          schedule: { at: plan.at, allowWhileIdle: true },
          ...(kanalId ? { channelId: kanalId } : {}),
          extra: { finoraTip: "odeme_hatirlatma", planAnahtari: plan.anahtar, odemeAnahtarlari: plan.odemeler.map(o => `${o.tarih}:${o.id}`) },
        }));
        // Önce yeni alarmları kaydet; başarısız olursa mevcut planı kaybetme.
        if (bildirimler.length) await LN.schedule({ notifications: bildirimler });
        const idler = new Set(bildirimler.map(n => n.id));
        const eski = sahipli.filter(n => !idler.has(Number(n.id)));
        if (eski.length) await LN.cancel({ notifications: eski.map(n => ({ id: n.id })) });
        const dogrulanan = (await LN.getPending()).notifications || [];
        const dogrulananIdler = new Set(dogrulanan.map(n => Number(n.id)));
        const sayi = bildirimler.filter(n => dogrulananIdler.has(n.id)).length;
        const bugun = yerelTarihAnahtari(simdi);
        Object.keys(telafiLog).forEach(k => { if (k.slice(0, 10) < bugun) delete telafiLog[k]; });
        // Kaydedilmiş aynı-gün alarmı saat geçince ikinci bir telafiye dönüşmesin.
        planlar.filter(p => dogrulananIdler.has(bildirimKimligi(p.anahtar))).forEach(p => p.odemeler.filter(o => o.onceGun === 0).forEach(o => { telafiLog[`${o.tarih}:${o.id}`] = true; }));
        localStorage.setItem("ft_bildirim_telafi_log", JSON.stringify(telafiLog));
        if (!iptal) setSonBildirimPlanlama({
          zaman: simdi.toISOString(), sayi, istenenSayi: bildirimler.length,
          odemeSayisi: tumu.filter(o => !o.odendi && o.tarih).length,
          odemeGunuSayisi: new Set(tumu.filter(o => !o.odendi && o.tarih).map(o => o.tarih)).size,
          kartSayisi: kartHatirlatmalari.length, telafiSayisi: planlar.filter(p => p.telafi).length,
          uyari: kesin.exact_alarm === "granted" ? null : "Kesin alarm izni kapalı; Android seçilen saati geciktirebilir. Ayarlardan Arka Plan Bildirimlerini Etkinleştir düğmesini kullan.",
          hata: sayi < bildirimler.length ? "Bazı hatırlatmalar Android alarm listesine kaydedilemedi." : null,
        });
      } catch (e) {
        if (!iptal) setSonBildirimPlanlama({ zaman: new Date().toISOString(), sayi: 0, hata: e.message || "Bildirimler planlanamadı." });
      }
    };
    bildirimPlanlamaSirasi = bildirimPlanlamaSirasi.then(planla, planla);
    return () => { iptal = true; };
  }, [odemeler, kartlar, kartOdemeleri, krediler, bildirimAyarlari, bildirimYenileme]);

  useEffect(()=>{
    const gun=Number(donemGun)||15,tarih=yerelTarihAnahtari(),donem=maasDonemKey(tarih,gun).key;
    const mevcut=kartBorcOzetleri[donem]||{};
    const yeni=Object.fromEntries(kartlar.map(k=>[String(k.id),{toplam:kartToplamBorcHesapla(k,islemler,kartOdemeleri,donem,gun),tarih,isim:k.isim}]));
    if(Object.entries(yeni).every(([id,k])=>mevcut[id]?.toplam===k.toplam && mevcut[id]?.isim===k.isim))return;
    setKartBorcOzetleri(p=>({...p,[donem]:{...(p[donem]||{}),...yeni}}));
  },[islemler,kartlar,kartOdemeleri,donemGun,kartBorcOzetleri,bildirimYenileme]);

  useEffect(() => {
    const hesap = donemDevriHesapla(islemler,kartlar,kartOdemeleri,Number(donemGun)||15,bakiyeHesapModu,yerelTarihAnahtari(),odemeler);
    if (Object.prototype.hasOwnProperty.call(devirLog,hesap.donemKey)) return;
    const mevcut = islemler.some(i => devirIslemiMi(i) && maasDonemKey(i.tarih,Number(donemGun)||15).key === hesap.donemKey);
    if (!mevcut && hesap.tutar > 0) {
      try {
        if (!localStorage.getItem('ft_auto_backup_devir_v1')) localStorage.setItem('ft_auto_backup_devir_v1',JSON.stringify({format:'finora-v2',data:{ft_islemler:islemler,ft_kartlar:kartlar,ft_kart_odemeleri:kartOdemeleri,ft_donem_gun:donemGun,ft_donem_devir_log:devirLog}}));
      } catch { kayitHatasiBildir('Dönem devrinden önce güvenlik yedeği yazılamadı; otomatik devir eklenmedi.'); return; }
      if (setIslemler(p => p.some(i => i.id === `donem-devri:${hesap.donemKey}`) ? p : [...p,{
        id:`donem-devri:${hesap.donemKey}`,tip:'gelir',kategori:'Önceki aydan devir',kaynak:'donem_devri',
        aciklama:'Önceki aydan devir',tutar:hesap.tutar,tarih:hesap.tarih,kaynakDonemKey:hesap.kaynakDonemKey,
      }]) === false) return;
    }
    setDevirLog(p => ({...p,[hesap.donemKey]:{...hesap,manuel:mevcut}}));
  },[islemler,odemeler,kartlar,kartOdemeleri,donemGun,bakiyeHesapModu,devirLog,bildirimYenileme]);

  // Eski sürümün ileri tarihli sabit giderlerini ödeme onayı bekleyen kayda bağla.
  // Geçmiş gerçekleşmiş kayıtları geriye dönük değiştirme.
  useEffect(()=>{
    const adaylar=islemler.filter(i=>i.sabitId!=null && i.tip==='gider' && i.tarih>yerelTarihAnahtari() && i.odemeId==null && i.kaynak!=='odeme'
      && !odemeler.some(o=>String(o.islemId)===String(i.id)));
    if(!adaylar.length)return;
    const yeni=adaylar.map(i=>({id:`sabit-bekleyen:${i.id}`,islemId:i.id,kategori:i.kategori,aciklama:i.aciklama||i.kategori,tutar:i.tutar,tarih:i.tarih,odendi:false,sabitId:i.sabitId}));
    if(setOdemeler(p=>[...p,...yeni.filter(o=>!p.some(k=>String(k.id)===String(o.id)))])===false)return;
    setIslemler(p=>p.map(i=>adaylar.some(a=>String(a.id)===String(i.id))?{...i,kaynak:'odeme',odemeId:`sabit-bekleyen:${i.id}`}:i));
  },[islemler,odemeler]);

  // 🔄 Her açılışta aktif sabit giderleri kontrol et, bu ay eklenmemişse ekle
  useEffect(() => {
    const bugunAnahtari = yerelTarihAnahtari();
    const gun = Math.min(28, Math.max(1, parseInt(donemGun) || 15));
    const donem = maasDonemiBul(bugunAnahtari, gun);
    const donemKey = `${donem.yil}-${donem.ay}`;

    const yeniLog = { ...sabitEklemeLog };
    const eklenecekler = [];
    const yeniSabitOdemeler = [];
    let logDegisti = false;

    tekrarlayan.forEach(t => {
      if (!t.aktif) return;
      const logKey = `${t.id}-${donemKey}`;
      const zatenKayitli = islemler.some(i => String(i.sabitId) === String(t.id) && maasDonemKey(i.tarih, gun).key === donem.key);
      if (yeniLog[logKey] || zatenKayitli) {
        if (zatenKayitli && !yeniLog[logKey]) { yeniLog[logKey] = true; logDegisti = true; }
        return;
      }

      // Ödeme günü dönem başlangıcından küçükse tarih bir sonraki takvim ayındadır.
      // Örn. 15 Ağustos–14 Eylül dönemindeki ayın 5'i, 5 Eylül olarak yazılır.
      const odemeDate = donemdekiSabitOdemeTarihi(donem.baslangic, t.gun);
      const baslangic = t.baslangic ? yerelTarihOku(t.baslangic) : null;
      const bitis = t.bitis ? yerelTarihOku(t.bitis) : null;
      if ((baslangic && odemeDate < baslangic) || (bitis && odemeDate > bitis)) return;
      const odemeTarih = yerelTarihAnahtari(odemeDate);

      const islemId=`sabit:${t.id}:${donem.key}`,odemeId=`sabit-odeme:${t.id}:${donem.key}`;
      yeniSabitOdemeler.push({id:odemeId,islemId,kategori:t.kategori,tutar:t.tutar,aciklama:t.aciklama,tarih:odemeTarih,odendi:false,sabitId:t.id});
      eklenecekler.push({
        id: islemId,
        kaynak:"odeme",odemeId,
        tip: "gider",
        kategori: t.kategori,
        tutar: t.tutar,
        aciklama: `${t.aciklama}`,
        tarih: odemeTarih,
        sabitId: t.id, // hangi sabitten geldiği
      });
      yeniLog[logKey] = true;
      logDegisti = true;
    });

    if (eklenecekler.length > 0) {
      if(setOdemeler(prev=>[...prev,...yeniSabitOdemeler.filter(o=>!prev.some(x=>String(x.id)===String(o.id)))])===false)return;
      if(setIslemler(prev => [...prev, ...eklenecekler.filter(i=>!prev.some(x=>String(x.id)===String(i.id)))])===false)return;
    }
    if (logDegisti) setSabitEklemeLog(yeniLog);
  }, [tekrarlayan, donemGun, islemler, sabitEklemeLog]);




  const menuGruplari = [
    { grup: "Ana bölümler", items: [
      { id: "ozet", label: "Genel Bakış", icon: "home" },
      { id: "islemler", label: "İşlemler", icon: "wallet" },
      { id: "odemeler", label: "Ödemeler", icon: "card" },

      { id: "analiz", label: "Raporlar", icon: "chart" },

    ]},
    { grup: "Bütçe araçları", items: [
      { id: "tekrarlayan", label: "Sabit Giderler", icon: "repeat" },
      { id: "krediler", label: "Krediler", icon: "receipt" },
      { id: "taksitler", label: "Taksitli Harcamalar", icon: "calendar" },
    ]},
    { grup: "Maaş", items: [
      { id: "maas", label: "Maaş Hesaplama", icon: "coins" },
    ]},
    { grup: "Diğer", items: [
      { id: "ayarlar", label: "Ayarlar", icon: "settings" },
    ]},
  ];
  const tumSekmeler = menuGruplari.flatMap(g => g.items);
  const aktifSekmeBilgi = tumSekmeler.find(t => t.id === aktifTab) || tumSekmeler[0];

  return (
    <KategoriGorunumContext.Provider value={{ gorunumler:kategoriGorunumleri, setGorunumler:setKategoriGorunumleri }}>
    <>
      <style>{`
        :root {
          --bg:#0b1020; --header:rgba(15,23,42,.92); --card:#121a2e; --card2:#182238;
          --border:#26334d; --divider:#202c43; --border2:#34435f;
          --text:#f8fafc; --text2:#dbe3ef; --text3:#9eacc1; --text4:#a4b2c7; --text5:#99a9c2;
          --border-light:rgba(255,255,255,.07);
          --card-shadow:0 14px 34px rgba(0,0,0,.22);
          --balance:linear-gradient(145deg,#4f46e5 0%,#A74E32 48%,#0ea5a4 130%);
          --balance-shadow:0 18px 38px rgba(79,70,229,.32);
          --accent:#A74E32; --accent-strong:#8B3D27; --accent-soft:rgba(167,78,50,.14);
          --success:#14b88a; --danger:#f05f70; --income-text:#4ade80; --expense-text:#fb7185; --saving-text:#93c5fd;
        }
        .kredi-karti-alani { color:var(--text); }
        [data-theme="light"] {
          --bg:#FCF5EF; --header:rgba(252,245,239,.96); --card:#fffdfa; --card2:#f5eae1;
          --border:#e6d8cd; --divider:#eee2d8; --border2:#d8c3b3;
          --income-text:#047857; --expense-text:#b91c1c; --saving-text:#1d4ed8;
          --text:#302821; --text2:#493e35; --text3:#645447; --text4:#726052; --text5:#786657;
          --border-light:rgba(255,255,255,.9);
          --card-shadow:0 12px 30px rgba(37,45,77,.07);
          --balance:linear-gradient(145deg,#5153e6 0%,#A74E32 52%,#24aaa8 135%);
          --balance-shadow:0 18px 38px rgba(81,83,230,.25);
        }
        * { margin:0; padding:0; box-sizing:border-box; -webkit-tap-highlight-color:transparent; }
        html, body, #root { background:var(--bg); min-height:100vh; margin:0; width:100%; overflow-x:hidden; }
        body { color:var(--text); }
        .app-modal-panel { background:var(--card) !important; outline:none; }
        .app-modal-panel button { min-height:44px; }
        .app-modal-panel button[aria-label="Pencereyi kapat"] { min-width:44px; }
        input:focus-visible, select:focus-visible, textarea:focus-visible, button:focus-visible { outline:2px solid var(--accent) !important; outline-offset:3px; }
        .kayit-hatasi { margin:12px 16px; padding:14px; border:1px solid #c24132; border-radius:14px; color:var(--text); background:var(--card); font-size:13px; line-height:1.6; }
        .ilk-kullanim { display:flex; flex-wrap:wrap; gap:8px; margin-top:14px; }
        .ilk-kullanim button { min-height:44px; padding:10px 14px; border:1px solid rgba(255,255,255,.6); border-radius:12px; background:rgba(0,0,0,.16); color:#fff; font-weight:600; }
        .nav-dock button, .app-content button { min-height:44px; }
        .app-content button[style*="width: 22px"], .app-content button[style*="width: 26px"], .app-content button[style*="width: 28px"], .app-content button[style*="width: 30px"], .app-content button[style*="width: 32px"] { min-width:44px; }


        button, input, select { font-family:system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif !important; }
        button { transition:transform .18s ease, background .18s ease, border-color .18s ease, opacity .18s ease; }
        button:active { transform:scale(.97); }
        ::-webkit-scrollbar { width:5px; }
        ::-webkit-scrollbar-track { background:transparent; }
        ::-webkit-scrollbar-thumb { background:var(--border2); border-radius:99px; }
        input[type=date]::-webkit-calendar-picker-indicator { filter:invert(0.4); }
        [data-theme="light"] input[type=date]::-webkit-calendar-picker-indicator { filter:invert(0); }
        select option { background:var(--card2); }
        @keyframes slideDown {
          from { transform:translateX(-50%) translateY(-100%); opacity:0; }
          to   { transform:translateX(-50%) translateY(0); opacity:1; }
        }
        @keyframes menuSlide {
          from { transform:translateX(-100%); }
          to   { transform:translateX(0); }
        }
        @keyframes sheetUp {
          from { transform:translateY(100%); }
          to   { transform:translateY(0); }
        }
        @keyframes fadeIn {
          from { opacity:0; }
          to   { opacity:1; }
        }
        @keyframes toastIn {
          from { opacity:0; transform:translateX(-50%) translateY(-16px); }
          to   { opacity:1; transform:translateX(-50%) translateY(0); }
        }
        @keyframes pageEnter {
          from { opacity:0; transform:translateY(8px); }
          to { opacity:1; transform:translateY(0); }
        }


        .category-trend-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; color:var(--text); font-size:16px; font-weight:600; margin-bottom:16px; }
        .tools-card { background:var(--card); color:var(--text); border:1px solid var(--border); border-radius:18px; padding:16px; margin-bottom:14px; min-width:0; }
        .tools-card h3 { font-size:14px; margin:0 0 12px; }
        .tools-card h4 { font-size:12px; margin:14px 0 8px; }
        .tools-note { font-size:11px; line-height:1.6; color:var(--text3); overflow-wrap:anywhere; }
        .tools-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; align-items:center; }
        .tools-grid > * { min-width:0; }
        .tools-row { display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; padding:7px 0; font-size:12px; }
        .tools-button { min-height:44px; padding:8px 12px; border-radius:10px; border:1px solid var(--border); background:var(--card2); color:var(--text); cursor:pointer; font-size:12px; }
        .tools-button:disabled,.heat-day:disabled { opacity:.4; cursor:default; }
        .report-stat { padding:12px; background:var(--card2); border-radius:12px; font-size:11px; }
        .report-stat strong { display:block; font-size:15px; margin-top:7px; overflow-wrap:anywhere; }
        .heat-grid { display:grid; grid-template-columns:repeat(7,minmax(0,1fr)); gap:4px; text-align:center; }
        .heat-day { display:flex; flex-direction:column; align-items:center; justify-content:center; padding:0; min-height:44px; border:2px solid var(--border); border-radius:8px; cursor:pointer; font-size:12px; }
        .heat-day small { font-size:10px; line-height:12px; }
        .annual-details > summary { padding:14px; margin-bottom:12px; color:var(--text); background:var(--card2); border:1px solid var(--border); border-radius:12px; cursor:pointer; font-size:12px; }
        .category-trend-heading > span { color:var(--text3); font-size:12px; font-weight:600; background:var(--card2); border:1px solid var(--border); border-radius:10px; padding:5px 10px; }
        .category-trend-card { background:var(--card2); border:1px solid var(--border); border-radius:18px; padding:16px; margin-bottom:12px; font-variant-numeric:tabular-nums; }
        .category-trend-card:last-child { margin-bottom:0; }
        .category-trend-top { display:flex; justify-content:space-between; align-items:flex-start; gap:12px; }
        .category-trend-main { min-width:0; }
        .category-trend-name { display:flex; align-items:center; gap:9px; color:var(--text2); font-size:14px; font-weight:600; overflow-wrap:anywhere; }
        .category-trend-icon { width:34px; height:34px; display:grid; place-items:center; border-radius:11px; flex-shrink:0; }
        .category-trend-amount { color:var(--text); font-size:22px; font-weight:700; letter-spacing:-.025em; margin-top:12px; overflow-wrap:anywhere; }
        .category-trend-caption { display:block; color:var(--text3); font-size:11px; line-height:1.5; margin-top:4px; }
        .category-trend-change { text-align:right; flex-shrink:0; }
        .category-trend-badge { display:inline-block; border-radius:8px; padding:5px 8px; font-size:12px; font-weight:600; }
        .category-trend-scroll { width:100%; min-width:0; margin-top:17px; padding-bottom:4px; }
        .category-trend-columns { display:grid; width:100%; min-width:0; gap:2px; align-items:end; }
        .category-trend-column { display:flex; flex-direction:column; gap:7px; align-items:center; justify-content:flex-end; border:0; border-radius:6px; background:transparent; padding:2px 0 4px; color:var(--text4); min-width:0; width:100%; min-height:104px; cursor:pointer; }
        .category-trend-column:focus-visible { outline:2px solid var(--accent); outline-offset:-2px; }
        .category-trend-bar-area { display:flex; align-items:flex-end; justify-content:center; height:72px; width:100%; }
        .category-trend-bar { position:relative; display:block; max-width:16px; width:70%; border-radius:5px 5px 2px 2px; transition:height .3s,opacity .15s; }
        .annual-category-chart .category-trend-bar::before { content:""; position:absolute; width:2px; background:currentColor; left:calc(50% - 1px); top:-4px; bottom:-3px; opacity:.6; }
        .annual-category-chart .category-trend-bar-area { padding:5px 0; box-sizing:border-box; }
        .category-trend-month { font-size:clamp(8px,2.4vw,11px); letter-spacing:-.2px; white-space:nowrap; line-height:1.5; }
        .category-trend-column[aria-pressed="true"] .category-trend-month { color:var(--text); font-weight:600; }
        .category-trend-selected { color:var(--text3); border-top:1px solid var(--border); padding-top:10px; margin-top:7px; font-size:12px; }
        .category-trend-selected strong { color:var(--text2); font-weight:600; }
        @media(max-width:360px) { .category-trend-card { padding:13px; } .category-trend-top { gap:8px; } .category-trend-amount { font-size:20px; } .category-trend-change { max-width:105px; } }

        /* Ana sayfa: yalnızca sunum; veri ve hesap kuralları korunur. */
        .home-overview { font-variant-numeric:tabular-nums; }
        .home-period-wrap { display:flex; flex-direction:column; align-items:flex-end; gap:7px; margin-bottom:14px; }
        .home-overview .home-period-pill { display:flex; align-items:center; max-width:100%; background:rgba(245,158,11,.09); border:1px solid rgba(245,158,11,.24); border-radius:99px; padding:0 3px; color:#f59e0b; }
        .home-period-pill > button { width:30px; min-height:40px; border:0; background:none; color:inherit; font-size:21px; cursor:pointer; border-radius:99px; }
        .home-period-pill > button:disabled { opacity:.3; cursor:default; }
        .home-period-label { position:relative; display:flex; align-items:center; gap:8px; min-height:40px; min-width:0; padding:0 4px; font-size:12px; font-weight:600; }
        .home-period-label > svg { flex-shrink:0; }
        .home-period-text { white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
        .home-period-chevron { font-size:18px; line-height:1; }
        .home-period-select { position:absolute; inset:0; width:100%; height:100%; min-height:40px; opacity:0; cursor:pointer; }
        .home-period-label:focus-within { outline:2px solid var(--accent); outline-offset:3px; border-radius:12px; }
        .home-period-history { display:flex; align-items:center; flex-wrap:wrap; justify-content:flex-end; gap:8px; color:var(--text3); font-size:11px; }
        .home-period-history button { background:none; border:0; color:#ef4444; font-size:11px; cursor:pointer; min-height:30px; }
        [data-theme="light"] .home-period-pill { color:#92400e; background:#fffbeb; border-color:#fcd34d; }
        .home-overview .home-balance { padding:23px 22px !important; margin-bottom:14px !important; }
        .home-balance-heading { position:relative; z-index:1; display:flex; align-items:center; justify-content:space-between; gap:10px; color:#fff; font-weight:600; font-size:16px; }
        .home-balance-badge { display:inline-flex; align-items:center; gap:5px; flex-shrink:0; background:rgba(0,0,0,.15); border:1px solid rgba(255,255,255,.16); border-radius:99px; padding:7px 12px; font-size:11px; font-weight:600; }
        .home-balance-value { position:relative; z-index:1; color:#fff; font-family:system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif; font-size:clamp(30px,9.5vw,46px); font-weight:700; line-height:1.1; letter-spacing:-.025em; margin:5px 0 3px; overflow-wrap:anywhere; }
        .home-balance-caption { position:relative; z-index:1; color:rgba(255,255,255,.84); font-size:11px; line-height:1.5; }
        .home-balance-wave { position:absolute; right:-70px; bottom:-115px; width:310px; height:205px; border-radius:48%; background:rgba(255,255,255,.08); transform:rotate(-19deg); pointer-events:none; }
        .home-overview .home-summary { gap:8px !important; margin-bottom:16px !important; }
        .home-overview .home-summary-card { padding:14px 10px !important; border-radius:18px !important; min-width:0; box-shadow:0 5px 16px rgba(0,0,0,.06) !important; }
        .home-overview .home-summary-card > div:first-child > span:last-child { width:27px !important; height:27px !important; border-radius:9px !important; color:inherit; }
        .home-overview .home-summary-value { font-size:clamp(11px,3.35vw,15px) !important; letter-spacing:-.025em; margin-bottom:8px !important; overflow:visible !important; white-space:normal !important; overflow-wrap:anywhere; }
        .home-overview .home-summary-caption { font-size:11px !important; line-height:1.45; margin-top:5px !important; }
        .home-panel, .home-overview .home-credit-panel { background:var(--card); border:1px solid var(--border); border-radius:22px !important; padding:18px !important; margin-bottom:14px !important; box-shadow:var(--card-shadow); }
        .home-panel-heading { display:flex; justify-content:space-between; align-items:center; gap:12px; color:var(--text); font-size:16px; font-weight:700; }
        .home-panel-heading > svg { color:var(--text4); }
        .home-gauge-panel { padding-bottom:16px !important; }
        .home-gauge-svg { margin-top:12px !important; }
        .home-gauge-split { display:grid; grid-template-columns:1fr 1fr; border-top:1px solid var(--border); padding-top:14px; margin-top:0; }
        .home-gauge-split > div { display:flex; flex-direction:column; gap:5px; padding:0 16px; }
        .home-gauge-split > div + div { border-left:1px solid var(--border); }
        .home-detail-label { color:var(--text3); font-size:12px; }
        .home-gauge-split strong { color:var(--text); font-size:21px; font-weight:600; }
        .home-percent-value { display:flex; align-items:center; gap:7px; }
        .home-percent-dot { display:inline-block; width:9px; height:9px; border-radius:50%; flex-shrink:0; }
        .home-gauge-note { color:var(--text4); font-size:11px; text-align:center; line-height:1.5; margin-top:14px; }
        .home-gauge-legend { display:flex; justify-content:center; flex-wrap:wrap; gap:18px; margin-top:10px; }
        .home-gauge-legend span { display:flex; align-items:center; gap:6px; color:var(--text3); font-size:11px; }
        .home-gauge-legend i { width:7px; height:7px; border-radius:50%; }
        .home-overview .home-forecast, .home-overview .home-daily-limit { gap:12px !important; }
        .home-overview .home-forecast > div:nth-child(2) > div:first-child, .home-overview .home-daily-limit > div:nth-child(2) > div:first-child { font-size:12px !important; }
        .home-overview .home-forecast > div:nth-child(2) > div:last-child, .home-overview .home-daily-limit > div:nth-child(2) > div:last-child { font-size:11px !important; line-height:1.6; }
        .home-overview .home-forecast > div:nth-child(2) span:first-child, .home-overview .home-daily-limit > div:nth-child(2) span:first-child { font-size:19px !important; }
        .home-overview .home-credit-panel > div:first-child { color:var(--text) !important; font-size:15px !important; font-weight:700; text-transform:none !important; letter-spacing:0 !important; }
        .home-overview .home-credit-card { border-radius:16px !important; padding:14px !important; margin-bottom:12px !important; }
        .home-overview .home-credit-card > div:nth-child(2) > div { padding:10px 8px !important; border-radius:10px !important; min-width:0; }
        .home-overview .home-credit-card > div:nth-child(2) > div > div:first-child { font-size:11px !important; }
        .home-overview .home-credit-card > div:nth-child(2) > div > div:last-child { font-size:12px !important; overflow-wrap:anywhere; }
        [data-theme="dark"] .home-overview .home-credit-panel { background:linear-gradient(155deg,#121a2e,#10192a) !important; }
        @media(max-width:360px) {
          .home-overview .home-panel, .home-overview .home-credit-panel { padding:14px !important; }
          .home-overview .home-summary-card { padding:12px 8px !important; }
          .home-overview .home-forecast { flex-wrap:wrap; }
          .home-overview .home-credit-card { padding:12px 10px !important; }
        }

        .app-shell { width:100%; max-width:540px; margin:0 auto; min-height:100vh; padding-bottom:104px; font-family:system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif; }
        .app-shell:before { content:""; position:fixed; inset:0; z-index:0; pointer-events:none; background:radial-gradient(circle at 10% -10%,rgba(167,78,50,.13),transparent 34%),radial-gradient(circle at 92% 18%,rgba(20,184,166,.08),transparent 30%); }
        .app-content { padding:14px 16px 0; position:relative; z-index:1; }
        .app-content > div { animation:pageEnter .28s cubic-bezier(.16,1,.3,1); }
        .topbar { position:sticky; top:0; z-index:50; padding:calc(14px + env(safe-area-inset-top)) 16px 12px; background:var(--header); border-bottom:1px solid var(--border); backdrop-filter:blur(22px); -webkit-backdrop-filter:blur(22px); box-shadow:0 8px 28px rgba(15,23,42,.06); }
        .nav-dock { position:fixed; left:50%; bottom:0; transform:translateX(-50%); z-index:90; width:100%; max-width:540px; padding:0 12px calc(10px + env(safe-area-inset-bottom)); }
        .nav-dock-inner { min-height:70px; padding:8px 8px; display:flex; align-items:center; justify-content:space-between; background:var(--header); border:1px solid var(--border); border-radius:24px; box-shadow:0 18px 42px rgba(15,23,42,.22); backdrop-filter:blur(22px); -webkit-backdrop-filter:blur(22px); }
        .desktop-brand { display:none; }
        input, select, textarea { min-height:44px; }
        @media (prefers-reduced-motion:reduce) { *, *:before, *:after { animation-duration:.01ms !important; transition-duration:.01ms !important; } }
        @media (min-width:760px) {
          .app-shell { max-width:760px; padding-bottom:112px; }
          .nav-dock { max-width:560px; bottom:14px; }
          .app-content { padding:20px 24px 0; }
          .topbar { border:1px solid var(--border); border-radius:0 0 24px 24px; padding-left:24px; padding-right:24px; }
        }
        :root {
          --bg:#0e131c; --header:#111824; --card:#171f2b; --card2:#202b3a;
          --border:#2c394b; --divider:#283343; --border2:#415066;
          --text:#f3f5f8; --text2:#d3dce7; --text3:#a8b5c6; --text4:#a8b5c6; --text5:#99a9be;
          --accent:#e9aa8f; --accent-strong:#A74E32; --accent-soft:rgba(233,170,143,.12); --on-accent:#fff;
          --card-shadow:0 3px 12px rgba(0,0,0,.12); --ring-track:rgba(255,255,255,.12);
        }
        [data-theme="light"] {
          --bg:#FCF5EF; --header:#FCF5EF; --card:#fffdfa; --card2:#f5eae1;
          --accent:#A74E32; --accent-strong:#A74E32; --accent-soft:#f0dfd4; --on-accent:#fff;
          --text:#302821; --text2:#493e35; --text3:#645447; --text4:#726052; --text5:#786657;
          --border:#e6d8cd; --divider:#eee2d8; --border2:#d8c3b3;
          --card-shadow:0 3px 12px rgba(76,48,28,.035); --ring-track:rgba(255,255,255,.16);
        }
        .app-shell { color:var(--text); }
        .home-summary-card svg { color:var(--text3); }
        .home-forecast > div:first-child, .home-daily-limit > div:first-child { color:var(--accent); }
        .app-shell:before { display:none; }
        .topbar { box-shadow:none; border-bottom:0; }
        .app-content { padding-top:10px; }
        .app-content h3 { font-size:14px; font-weight:600; letter-spacing:-.01em; }
        .tools-card h3 { display:flex; align-items:center; gap:8px; }
        .tools-card h3 svg { color:var(--accent); flex-shrink:0; }
        .tools-card, .home-panel { box-shadow:var(--card-shadow); border-radius:18px !important; }
        .tools-card { padding:16px; }
        .inline-icon { display:inline-flex; vertical-align:-3px; margin-right:6px; color:inherit; }
        .money-fit { display:block; width:100%; min-width:0; white-space:nowrap; font-variant-numeric:tabular-nums; line-height:1.25; letter-spacing:-.025em; }
        .home-period-wrap { align-items:stretch; margin-bottom:12px; }
        .home-overview .home-period-pill { display:grid; grid-template-columns:44px minmax(0,1fr) 44px; width:100%; border-radius:14px; padding:3px 4px; color:var(--text); background:var(--card); border:1px solid var(--border); }
        .home-period-pill > button { width:44px; min-height:44px; color:var(--text3); }
        .home-period-label { justify-content:center; min-height:44px; font-size:12px; }
        .home-period-label > svg { color:var(--accent); }
        .home-overview .finance-hero { position:relative; overflow:hidden; border-radius:24px; padding:18px !important; margin-bottom:12px !important; color:#fff; background:linear-gradient(120deg,#36483c,#536751); box-shadow:none; }
        .finance-hero[data-status="bos"] { background:linear-gradient(120deg,#53453b,#766151); }
        .finance-hero[data-status="orta"] { background:linear-gradient(120deg,#65472e,#99703f); }
        .finance-hero[data-status="kotu"] { background:linear-gradient(120deg,#713b35,#9f5145); }
        .finance-hero:after { content:""; position:absolute; width:240px; height:240px; border:1px solid rgba(255,255,255,.08); border-radius:50%; right:-145px; top:-100px; pointer-events:none; }
        .finance-hero .home-balance-heading { font-size:12px; font-weight:600; color:rgba(255,255,255,.9); }
        .finance-hero .home-balance-badge { padding:5px 8px; font-size:10px; background:rgba(255,255,255,.1); border-color:rgba(255,255,255,.12); }
        .finance-hero .home-balance-value { margin:10px 0 3px; }
        .hero-caption { font-size:11px; line-height:1.5; color:rgba(255,255,255,.78); }
        .hero-budget { display:grid; grid-template-columns:112px minmax(0,1fr); gap:12px; align-items:center; background:rgba(0,0,0,.12); border:1px solid rgba(255,255,255,.08); border-radius:16px; padding:9px; margin-top:14px; }
        .budget-ring { width:112px; height:112px; display:block; color:#fff; overflow:visible; flex-shrink:0; }
        .hero-budget-copy { min-width:0; }
        .hero-budget-copy h3 { font-size:13px; margin:0 0 7px; color:#fff; }
        .hero-budget-copy > p:not(.hero-caption) { font-size:12px; line-height:1.5; font-variant-numeric:tabular-nums; overflow-wrap:anywhere; }
        .hero-budget-copy > p span { color:rgba(255,255,255,.75); }
        .hero-budget-copy .hero-caption { font-size:10px; margin-top:4px; }
        .ring-legend { display:flex; flex-wrap:wrap; gap:8px; margin-top:8px; font-size:9px; color:rgba(255,255,255,.8); }
        .ring-legend span { display:flex; align-items:center; gap:3px; }
        .ring-legend i { width:5px; height:5px; border-radius:50%; }
        .ilk-kullanim button { display:inline-flex; align-items:center; gap:7px; font-size:12px; }
        .home-overview .home-summary { margin-bottom:12px !important; }
        .home-overview .home-summary-card { padding:12px 9px !important; border-radius:15px !important; box-shadow:none !important; }
        .home-overview .home-summary-card > div:first-child { margin-bottom:7px !important; }
        .home-overview .home-summary-card > div:first-child > span:last-child { width:22px !important; height:22px !important; background:none !important; }
        .home-overview .home-summary-value { margin-bottom:5px !important; white-space:nowrap !important; overflow-wrap:normal; }
        .home-overview .home-summary-caption { font-size:10px !important; line-height:1.4; margin-top:4px !important; }
        .home-quick-actions { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-bottom:12px; }
        .home-quick-actions button { display:flex; align-items:center; justify-content:center; gap:8px; min-height:48px; padding:10px; color:var(--text); font-size:12px; font-weight:650; background:var(--card); border:1px solid var(--border); border-radius:14px; }
        .home-quick-actions button svg { color:var(--accent); }
        .home-quick-actions button:first-child { background:#c92b3c; color:#fff; border-color:transparent; }
        .home-quick-actions button:first-child svg { color:inherit; }
        .home-quick-actions button:last-child { background:#007e66; color:#fff; border-color:transparent; }
        .home-quick-actions button:last-child svg { color:inherit; }
        .home-panel-heading { font-size:14px; }
        .nav-dock-inner { min-height:66px; border-radius:20px; box-shadow:0 5px 20px rgba(48,30,18,.12); padding:6px; }
        .nav-dock-inner > button { min-height:48px; }
        .nav-dock { padding-left:12px; padding-right:12px; }
        .filter-toolbar { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px; }
        .filter-toolbar .tools-button { display:flex; align-items:center; gap:6px; padding:8px 10px; font-size:11px; }
        .filter-count { background:var(--accent-soft); color:var(--accent); border-radius:7px; min-width:17px; padding:2px 4px; }
        .filter-chips { display:flex; flex-wrap:wrap; gap:5px; }
        .filter-chips:not(:empty) { margin-bottom:10px; }
        .filter-chips button { display:inline-flex; align-items:center; gap:5px; background:var(--accent-soft); color:var(--accent); padding:6px 9px; min-height:32px; font-size:10px; border:1px solid var(--border); border-radius:9px; }
        .filter-panel { margin-bottom:12px; }
        .transactions-page > div[style*="margin-bottom: 16px"] { flex-wrap:wrap; gap:10px !important; }
        .transaction-row { display:grid !important; grid-template-columns:34px minmax(0,1fr) auto; padding:12px !important; gap:9px !important; }
        .transaction-row > div:first-child { width:34px !important; height:34px !important; border:0 !important; border-radius:10px !important; color:var(--text3); }
        .transaction-row > div:nth-child(2) > div:first-child { white-space:normal !important; line-height:1.4; overflow-wrap:anywhere; }
        .transaction-amount { min-width:0; max-width:140px; }
        .transaction-amount > div:first-child { font-size:13px !important; font-variant-numeric:tabular-nums; overflow-wrap:anywhere; }
        .transaction-amount button { width:44px; min-height:44px; display:grid; place-items:center; }
        .transaction-row button { color:var(--text3); }
        .payment-filters { display:flex; flex-wrap:wrap; gap:6px; margin-bottom:14px; }
        .payment-filters button { flex:1; min-width:58px; }
        .payment-group { border-radius:16px; overflow:hidden; }
        .payment-group > button { background:var(--card2) !important; }
        .payment-group[data-group="gecikmis"] > button { border-left:3px solid var(--expense-text) !important; }
        .payment-row { display:grid !important; grid-template-columns:44px minmax(0,1fr) auto; gap:9px !important; padding:12px !important; opacity:1 !important; }
        .payment-check { width:44px !important; height:44px !important; min-width:44px !important; min-height:44px !important; border:1px solid var(--border) !important; background:var(--card2) !important; color:var(--text3) !important; border-radius:10px !important; }
        .payment-description > div:first-child { white-space:normal !important; overflow-wrap:anywhere; line-height:1.4; }
        .payment-description > div:nth-child(2) { flex-wrap:wrap; gap:4px !important; }
        .payment-status { display:inline-block; font-size:10px; border-radius:6px; padding:3px 5px; background:var(--card2); color:var(--text3); }
        .payment-status.late { color:var(--expense-text); background:rgba(239,68,68,.1); }
        .payment-status.paid { color:var(--income-text); background:rgba(16,185,129,.1); }
        .payment-amount { max-width:135px; min-width:0; }
        .payment-amount > div:first-child { font-size:13px !important; font-variant-numeric:tabular-nums; overflow-wrap:anywhere; }
        .payment-amount button { width:44px; min-height:44px; display:grid; place-items:center; }
        .report-period { margin-bottom:10px; }
        .report-period-nav { padding:0; }
        .report-period-nav .tools-button { min-height:36px; font-size:11px; padding:6px 10px; }
        .report-range { margin-top:8px; }
        .report-tabs { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:3px; padding:4px; background:var(--card2); border:1px solid var(--border); border-radius:13px; margin-bottom:12px; }
        .report-tabs button { display:flex; flex-direction:column; align-items:center; gap:5px; border:0; border-radius:9px; background:none; color:var(--text3); font-size:10px; padding:8px 1px; cursor:pointer; }
        .report-tabs button[aria-selected="true"] { background:var(--card); color:var(--accent); box-shadow:0 2px 5px rgba(48,30,18,.05); font-weight:600; }
        .report-stat-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; }
        .report-stat-grid .report-stat:last-child { grid-column:1/-1; display:flex; align-items:center; justify-content:space-between; }
        .report-stat-grid .report-stat:last-child strong { margin-top:0; max-width:70%; width:70%; text-align:right; }
        .report-stat > span { display:flex; align-items:center; gap:5px; }
        .report-stat { border:1px solid var(--border); border-radius:12px; }
        .savings-result { display:flex; justify-content:space-between; margin-top:14px; font-size:12px; }
        .savings-result strong { color:var(--accent); }
        .report-comparisons { display:flex; flex-wrap:wrap; gap:8px 18px; margin-top:14px; padding-top:12px; border-top:1px solid var(--border); font-size:11px; color:var(--text3); }
        .report-comparisons > span { display:flex; flex-direction:column; gap:4px; }
        .report-comparisons > p { flex-basis:100%; }
        .report-export { display:flex; justify-content:flex-end; margin-bottom:12px; }
        .report-export .tools-button { display:flex; align-items:center; gap:7px; font-size:11px; }
        .calculation-help { margin-top:14px; font-size:11px; color:var(--text3); border-top:1px solid var(--border); padding-top:12px; }
        .calculation-help > summary { cursor:pointer; min-height:32px; padding:5px 0; }
        .calculation-help p { margin-top:6px; }
        .category-distribution + .category-distribution { margin-top:9px; }
        .category-distribution .tools-row { font-size:12px; gap:6px; }
        .category-distribution strong { font-size:12px; font-variant-numeric:tabular-nums; }
        .category-distribution small { font-size:10px; color:var(--text3); margin-left:4px; font-weight:500; }
        .distribution-track { height:5px; border-radius:8px; background:var(--card2); overflow:hidden; }
        .distribution-track span { height:100%; display:block; border-radius:8px; }
        .report-chart { min-width:0; width:100%; margin-top:6px; }
        .chart-tooltip { background:var(--card); border:1px solid var(--border); border-radius:10px; padding:10px; color:var(--text); font-size:11px; line-height:1.7; }
        .chart-legend { display:flex; justify-content:center; gap:18px; font-size:11px; color:var(--text3); }
        .chart-legend span { display:flex; align-items:center; gap:5px; }
        .chart-legend i { width:12px; height:3px; border-radius:3px; }
        .trend-table { max-height:300px; overflow:auto; }
        .insight-row { display:flex; align-items:flex-start; gap:10px; border-bottom:1px solid var(--border); padding:12px 0; font-size:12px; line-height:1.6; }
        .insight-row > svg { color:var(--accent); flex-shrink:0; margin-top:2px; }
        .legacy-insights { color:var(--text); background:var(--card); border:1px solid var(--border); border-radius:14px; padding:12px; margin-bottom:12px; }
        .legacy-insights > summary { min-height:32px; cursor:pointer; font-size:12px; }
        .legacy-insights > p { margin:8px 0 12px; }
        .empty-state { display:flex; flex-direction:column; align-items:center; gap:10px; padding:20px 12px; color:var(--text3); font-size:12px; text-align:center; }
        .heat-day { font-variant-numeric:tabular-nums; }
        .form-alert { color:var(--expense-text); font-size:12px; line-height:1.5; }
        @media(max-width:360px) {
          .app-content { padding-left:12px; padding-right:12px; }
          .finance-hero .home-balance-heading { font-size:11px; gap:5px; }
          .finance-hero .home-balance-badge { font-size:9px; padding:5px 6px; }
          .hero-budget { grid-template-columns:96px minmax(0,1fr); padding:7px; gap:8px; }
          .budget-ring { width:96px; height:96px; }
          .hero-budget-copy > p:not(.hero-caption) { font-size:11px; }
          .ring-legend { gap:5px; font-size:8px; }
          .home-overview .home-summary-card { padding:10px 7px !important; }
          .home-overview .home-summary-caption { font-size:10px !important; }
          .transaction-row,.payment-row { gap:6px !important; padding:10px !important; }
          .transaction-amount,.payment-amount { max-width:112px; }
          .transaction-row > div:first-child { width:30px !important; height:30px !important; }
        }
        @media(min-width:760px) {
          .hero-budget { grid-template-columns:128px minmax(0,1fr); }
          .budget-ring { width:128px; height:128px; }
          .home-summary-card .money-fit { text-align:left; }
          .report-stat-grid { grid-template-columns:repeat(3,minmax(0,1fr)); }
          .report-stat-grid .report-stat:last-child { grid-column:auto; display:block; }
          .report-stat-grid .report-stat:last-child strong { text-align:left; margin-top:7px; width:100%; max-width:100%; }
          .report-tabs button { flex-direction:row; justify-content:center; gap:8px; font-size:12px; }
        }

        :root { --topbar-height:calc(68px + env(safe-area-inset-top)); --surface-depth:inset 0 1px 0 rgba(255,255,255,.07),0 3px 0 #0a1019,0 10px 22px rgba(0,0,0,.2); }
        [data-theme="light"] { --surface-depth:inset 0 1px 0 rgba(255,255,255,.95),0 3px 0 #e7d7c9,0 10px 22px rgba(84,55,34,.08); }
        html,body,#root { overflow-x:clip; }
        .topbar { position:sticky; top:0; z-index:60; background:var(--header); }
        .home-period-wrap,.report-period { position:sticky; top:var(--topbar-height); z-index:45; background:var(--bg); }
        .home-period-wrap { padding:6px 0 10px; margin-bottom:4px; }
        .report-period { margin-top:0; border-top-left-radius:0 !important; border-top-right-radius:0 !important; box-shadow:0 6px 12px rgba(0,0,0,.06); }
        .home-overview .finance-hero { background:linear-gradient(135deg,#047857 0%,#10b981 55%,#0d9488 100%); box-shadow:var(--surface-depth); }
        .finance-hero[data-status="bos"] { background:linear-gradient(135deg,#65594f,#877365); }
        .finance-hero[data-status="orta"] { background:linear-gradient(135deg,#b45309 0%,#f59e0b 55%,#d97706 100%); }
        .finance-hero[data-status="kotu"] { background:linear-gradient(135deg,#991b1b 0%,#ef4444 55%,#b91c1c 100%); }
        .hero-budget-limit { color:rgba(255,255,255,.82); font-size:10px; margin-top:8px; }
        .tools-card,.home-panel,.home-overview .home-summary-card,.report-stat,.legacy-insights,.saving-stat-card,.transaction-group,.payment-group,.app-content [style*="box-shadow: var(--card-shadow)"] { box-shadow:var(--surface-depth) !important; }
        .saving-stat-card { position:relative; overflow:hidden; }
        .saving-stat-card > div:first-child { display:flex; justify-content:center; color:var(--accent); }
        .saving-stat-card > div:nth-child(2) { text-transform:none !important; font-size:11px !important; }
        .home-overview .home-credit-panel { background:var(--card) !important; border-color:var(--border) !important; }
        .credit-section-heading { display:flex; align-items:center; gap:8px; color:var(--text); font-size:14px; font-weight:600; margin-bottom:12px; text-transform:none; }
        .home-overview .credit-tile { background:var(--card); border:1px solid var(--border); border-radius:20px !important; box-shadow:var(--surface-depth); padding:16px !important; margin-bottom:16px !important; color:var(--text); }
        .credit-tile-heading { display:flex; justify-content:space-between; align-items:center; gap:8px; }
        .credit-tile-heading > div { display:flex; gap:8px; align-items:center; min-width:0; }
        .credit-tile-heading h3 { margin:0; font-size:16px; overflow-wrap:anywhere; }
        .credit-tile-heading svg { color:var(--text3); flex-shrink:0; }
        .credit-type { background:var(--card2); color:var(--text3); padding:5px 7px; border-radius:8px; font-size:9px; white-space:nowrap; }
        .credit-total { margin:16px 0 12px; }
        .credit-total-row { display:flex; flex-wrap:wrap; align-items:baseline; gap:5px 9px; }
        .credit-debt-change { font-size:12px; font-weight:600; line-height:1.5; }
        .credit-total > span:first-child { display:block; color:var(--text3); font-size:11px; line-height:1.5; margin-bottom:5px; }
        .credit-total .money-fit { color:var(--text); font-weight:700; }
        .credit-buckets { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:6px; margin-bottom:12px; }
        .credit-buckets > div { border:1px solid var(--border); background:var(--card2); border-radius:12px; padding:10px 7px; min-width:0; }
        .credit-buckets > div > span:first-child { display:block; color:var(--text3); font-size:10px; line-height:1.4; margin-bottom:6px; text-transform:none; }
        .credit-buckets .money-fit { color:var(--text); font-weight:600; }
        .credit-dates { display:grid; gap:9px; margin:14px 0 10px; padding-top:12px; border-top:1px solid var(--border); }
        .credit-dates > div { display:flex; align-items:flex-start; justify-content:space-between; gap:10px; font-size:11px; line-height:1.5; }
        .credit-dates span { color:var(--text3); }
        .credit-dates strong { color:var(--text); font-weight:600; text-align:right; }
        .credit-actions { display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:12px; }
        .home-overview .credit-tile {
          --credit-blue:#99abff; --credit-green:#35d6ae; --credit-yellow:#f6bd60;
          --text:#f3f6fa; --text2:#f3f6fa; --text3:#9aa8ba;
          background:linear-gradient(135deg,#232e54,#171e2b);
          border-color:rgba(153,171,255,.255);
        }
        [data-theme="light"] .home-overview .credit-tile {
          --credit-blue:#5261c9; --credit-green:#00795d; --credit-yellow:#9d5700;
          --text:#30231e; --text2:#30231e; --text3:#79675e;
          background:linear-gradient(135deg,#f0f1ff,#ffffff);
          border-color:rgba(82,97,201,.255);
        }
        .credit-tile-heading svg { color:var(--credit-blue); }
        .credit-type { color:var(--credit-blue); background:rgba(153,171,255,.094); }
        [data-theme="light"] .credit-type { background:rgba(82,97,201,.094); }
        .credit-buckets > div { background:rgba(16,27,50,.667); border-color:transparent; }
        [data-theme="light"] .credit-buckets > div { background:rgba(255,255,255,.8); }
        .credit-buckets > div:nth-child(1) .money-fit { color:var(--credit-blue); }
        .credit-buckets > div:nth-child(2) .money-fit { color:var(--credit-green); }
        .credit-buckets > div:nth-child(3) .money-fit { color:var(--credit-yellow); }
        .credit-dates > div:first-child strong { color:var(--text3); }
        .credit-dates > div:last-child strong { color:var(--credit-blue); }
        .credit-tile .tools-note { color:var(--text3); }
        .credit-actions button:only-child, .credit-actions button:nth-child(3) { grid-column:1/-1; }
        .credit-actions button { min-width:0; font-size:10px !important; padding:9px 7px !important; line-height:1.5; letter-spacing:0 !important; }
        .home-payments .upcoming-heading { display:flex; align-items:center; gap:8px; text-transform:none !important; font-size:14px !important; font-weight:600; letter-spacing:0 !important; margin-bottom:7px !important; }
        .upcoming-heading svg { color:var(--accent); }
        .upcoming-note { margin-bottom:10px; }
        .upcoming-item { padding:12px 0; border-top:1px solid var(--border); }
        .upcoming-item:last-child { padding-bottom:0; }
        .upcoming-description { display:flex; justify-content:space-between; align-items:flex-start; gap:12px; margin-bottom:10px; }
        .upcoming-description > div { min-width:0; }
        .upcoming-description strong { font-size:13px; color:var(--text); line-height:1.5; }
        .upcoming-description > div > strong { display:block; overflow-wrap:anywhere; }
        .upcoming-description > strong { flex-shrink:0; font-variant-numeric:tabular-nums; }
        .upcoming-description span { display:block; color:var(--text3); font-size:11px; line-height:1.5; }
        .upcoming-description .due-today { color:var(--expense-text); }
        .upcoming-item button { font-size:12px !important; letter-spacing:0 !important; border-radius:12px !important; }
        .day-totals { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin:12px 0 14px; }
        .day-totals > div { background:var(--card2); border:1px solid var(--border); border-radius:12px; padding:12px 9px; box-shadow:var(--surface-depth); }
        .day-totals span { display:block; color:var(--text3); font-size:10px; margin-bottom:6px; }
        .day-totals strong { font-size:14px; font-variant-numeric:tabular-nums; overflow-wrap:anywhere; }
        .finance-hero .day-totals > div { background:transparent; border:0; border-radius:0; box-shadow:none; padding:6px 0; }
        .finance-hero .day-totals span { color:rgba(255,255,255,.8); }
        .finance-hero .day-totals strong { display:block; color:inherit; font-size:28px; font-weight:600; line-height:1.2; letter-spacing:-.6px; }
        .weekday-card > p { margin-bottom:12px; }
        .weekday-row { margin:0 0 14px; }
        .weekday-row > div:first-child { display:flex; justify-content:space-between; gap:10px; font-size:12px; margin-bottom:7px; line-height:1.5; }
        .weekday-row strong { color:var(--text); font-weight:600; font-variant-numeric:tabular-nums; }
        .weekday-track { height:6px; border-radius:8px; background:var(--card2); overflow:hidden; }
        .weekday-track span { display:block; height:100%; background:var(--accent-strong); border-radius:8px; }
        @media(max-width:360px) {
          .home-overview .credit-tile { padding:13px !important; }
          .credit-tile-heading h3 { font-size:14px; }
          .credit-buckets > div { padding:9px 5px; }
          .credit-buckets > div > span:first-child { font-size:9px; }
          .credit-dates > div { font-size:10px; }
        }

        .gauge-options { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:12px; }
        .gauge-options button { padding:14px 10px; border:1px solid var(--border); border-radius:12px; background:var(--card2); color:var(--text); cursor:pointer; text-align:left; }
        .gauge-options button[aria-pressed="true"] { border-color:var(--accent); background:var(--accent-soft); box-shadow:inset 0 0 0 1px var(--accent); }
        .gauge-options strong { display:block; font-size:13px; margin-bottom:5px; }
        .gauge-options span { display:block; font-size:11px; line-height:1.5; color:var(--text3); }
        .hero-budget.hero-budget--ibre { grid-template-columns:minmax(0,1fr); justify-items:center; gap:8px; background:transparent; border:0; border-top:1px solid rgba(255,255,255,.2); padding:12px 0 0; border-radius:0; }
        .budget-needle { width:100%; max-width:260px; height:auto; display:block; color:#fff; background:transparent; }
        .hero-budget--ibre .hero-budget-copy { width:100%; text-align:center; padding:0 3px; }
        :root { --gauge-warning:#f6bd60; }
        [data-theme="light"] { --gauge-warning:#9d5700; }
        .hero-budget.hero-budget--ibre { background:transparent; color:#fff; border:0; border-radius:0; padding:10px 0 0; gap:10px; justify-items:stretch; }
        .needle-panel-title { color:#fff; font-size:15px; margin:0 0 2px; text-align:left; }
        .budget-needle { max-width:350px; margin:0 auto; color:#fff; background:transparent; }
        .needle-facts { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
        .needle-facts > div { min-width:0; }
        .needle-facts span:not(.money-fit) { display:block; font-size:11px; color:rgba(255,255,255,.8); margin-bottom:5px; }
        .needle-facts strong { display:block; font-weight:600; color:#fff !important; }
        .needle-legend { display:flex; justify-content:space-between; flex-wrap:wrap; gap:6px; font-size:10px; color:rgba(255,255,255,.85); margin-top:2px; }
        .needle-legend span { display:flex; align-items:center; gap:4px; }
        .needle-legend i { width:6px; height:6px; border-radius:50%; background:#fff !important; }
        .needle-balance { margin-top:3px; color:#fff; }
        .needle-balance strong { font-size:12px; overflow-wrap:anywhere; }
        .needle-balance p { font-size:10px; line-height:1.5; color:rgba(255,255,255,.8); margin-top:4px; }
        @media(max-width:360px) { .hero-budget.hero-budget--ibre { padding:8px 0 0; } .needle-legend { font-size:9px; } }
        .quick-template-section { padding:0; overflow:hidden; }
        .quick-template-toggle { display:flex; align-items:center; justify-content:space-between; width:100%; gap:10px; background:transparent; color:var(--text); border:0; padding:15px 16px; cursor:pointer; font-size:13px; font-weight:600; }
        .quick-template-toggle > span:first-child { display:flex; align-items:center; gap:8px; }
        .quick-template-toggle svg { color:var(--accent); }
        .quick-template-toggle small { font-size:10px; background:var(--card2); color:var(--text3); padding:3px 6px; border-radius:6px; }
        #hizli-islem-listesi { border-top:1px solid var(--border); padding:12px 16px 16px; }
        .quick-template-empty { padding:12px 0; }
        .quick-template-row { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:12px 0; border-bottom:1px solid var(--border); }
        .quick-template-name { min-width:0; }
        .quick-template-name strong { display:block; font-size:12px; color:var(--text); overflow-wrap:anywhere; }
        .quick-template-name span { display:block; font-size:10px; color:var(--text3); margin:4px 0; }
        .quick-template-name b { display:block; font-size:13px; font-variant-numeric:tabular-nums; }
        .quick-template-actions { display:flex; align-items:center; gap:5px; flex-shrink:0; }
        .quick-template-actions .tools-button { display:flex; align-items:center; justify-content:center; min-width:36px; min-height:44px; padding:8px; }
        .quick-template-actions .quick-template-add { color:var(--on-accent); background:var(--accent-strong); font-weight:600; padding:8px 12px; }
        .quick-template-preview { display:flex; flex-direction:column; gap:6px; padding:12px; background:var(--card2); border:1px solid var(--border); border-radius:12px; margin-bottom:16px; }
        .quick-template-preview strong { color:var(--text); font-size:14px; }
        .quick-template-preview span { color:var(--text3); font-size:12px; }
        .quick-template-question { color:var(--text); font-size:14px; line-height:1.6; margin-bottom:16px; }
        .quick-template-cancel { width:100%; margin-top:10px; }
        @media(max-width:360px) { #hizli-islem-listesi { padding-left:12px; padding-right:12px; } .quick-template-actions { gap:3px; } .quick-template-actions .tools-button { min-width:32px; padding:6px; } }

        .category-symbol { display:inline-flex; align-items:center; justify-content:center; flex-shrink:0; color:var(--category-color); background:color-mix(in srgb,var(--category-color) 15%,transparent); border-radius:9px; }
        .category-label { display:flex; align-items:center; gap:9px; min-width:0; }
        .category-customizer { min-width:0; margin:6px 0; }
        .category-customizer summary { display:flex; align-items:center; gap:8px; cursor:pointer; color:var(--text); font-size:12px; padding:6px 0; }
        .category-customizer summary small { color:var(--text3); font-size:10px; margin-left:auto; }
        .category-customizer-body { padding:10px; background:var(--card2); border:1px solid var(--border); border-radius:12px; }
        .category-colors { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:12px; }
        .category-colors button { width:32px; height:32px; border:2px solid transparent; border-radius:50%; color:white; font-size:16px; cursor:pointer; }
        .category-colors button[aria-pressed="true"] { border-color:var(--text); }
        .card-brand-badge { display:flex; flex-direction:column; justify-content:center; align-items:center; gap:4px; width:70px; min-height:44px; padding:7px 5px; flex-shrink:0; border-radius:11px; background:#fff; border:1px solid rgba(15,23,42,.09); box-shadow:0 2px 5px rgba(0,0,0,.05); line-height:1.1; }
        .card-brand-badge strong { font-size:12px; font-weight:800; letter-spacing:-.35px; }
        .card-brand-badge small { font-size:8px; font-weight:650; white-space:nowrap; }
        [data-theme="gray"] {
          --bg:#b7bec5; --header:#c8cdd3; --card:#d3d7dc; --card2:#c7cdd3;
          --border:rgba(255,255,255,.3); --border-light:rgba(255,255,255,.44); --divider:#b8c0c8; --border2:#929eaa;
          --text:#101b29; --text2:#243242; --text3:#465564; --text4:#4b5968; --text5:#526170;
          --accent:#00815f; --accent-strong:#007e66; --accent-soft:rgba(0,153,111,.12); --on-accent:#fff;
          --income-text:#008359; --expense-text:#c92b3c; --saving-text:#244d8a; --card-shadow:0 4px 14px rgba(49,63,78,.07); --ring-track:rgba(255,255,255,.2);
          --surface-depth:inset 0 1px 0 rgba(255,255,255,.25),0 5px 15px rgba(49,63,78,.08);
        }
        [data-theme="gray"] .app-shell:before { background:linear-gradient(155deg,rgba(255,255,255,.09),transparent 65%); }
        [data-theme="gray"] .topbar { background:rgba(200,205,211,.96); box-shadow:0 4px 18px rgba(49,63,78,.04); }
        [data-theme="gray"] .nav-dock-inner { background:rgba(209,214,220,.97); box-shadow:0 8px 24px rgba(49,63,78,.12); }
        [data-theme="gray"] .home-overview .credit-tile {
          --credit-blue:#5261c9; --credit-green:#00795d; --credit-yellow:#9d5700;
          --text:#142031; --text2:#243242; --text3:#4b5968;
          background:linear-gradient(135deg,#dddfe9,#ccd2d8); border-color:rgba(82,97,201,.2);
        }
        [data-theme="gray"] .credit-type { background:rgba(82,97,201,.09); }
        [data-theme="gray"] .credit-buckets > div { background:rgba(255,255,255,.32); }
        [data-theme="gray"] .budget-needle, [data-theme="gray"] .finance-hero { color:#fff; }
        [data-theme="gray"] .category-customizer-body { background:#ccd2d8; }
        @media(max-width:360px) { .card-brand-badge { width:59px; padding:6px 3px; } .card-brand-badge strong { font-size:11px; } }
      `}</style>
      <div data-theme={tema} style={{ background: "var(--bg)", minHeight: "100vh", width: "100%" }}>
      <div className="app-shell">
        {/* HEADER */}
        <header ref={topbarRef} className="topbar">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, minWidth: 0 }}>
              <button aria-label="Menüyü aç" onClick={() => setMenuAcik(true)} style={{ width: 42, height: 42, borderRadius: 14, background: "var(--card2)", border: "1px solid var(--border)", color: "var(--text2)", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", padding: 0, margin: 0, boxSizing: "border-box", lineHeight: 0, flexShrink: 0 }}>
                <Icon name="menu" size={21} />
              </button>
              <div style={{ minWidth: 0 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--text4)", fontSize: 11, fontWeight: 600, letterSpacing: ".13em", textTransform: "uppercase", marginBottom: 2 }}>Bütçe Takip</div>
                <div style={{ color: "var(--text)", fontSize: 19, lineHeight: 1.15, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{aktifSekmeBilgi.label}</div>
              </div>
            </div>
            <button aria-label={tema === "dark" ? "Açık temaya geç" : tema === "light" ? "Gri temaya geç" : "Koyu temaya geç"} onClick={() => setTema(tema === "light" ? "gray" : tema === "gray" ? "dark" : "light")} style={{ width: 42, height: 42, borderRadius: 14, background: "var(--card)", border: "1px solid var(--border)", color: "var(--accent)", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", padding: 0, margin: 0, boxSizing: "border-box", lineHeight: 0, boxShadow: "var(--card-shadow)" }}>
              <Icon name={tema === "dark" ? "sun" : "moon"} size={20} />
            </button>
          </div>
        </header>

        {/* HAMBURGER MENÜ */}
        {menuAcik && (
          <>
            <div onClick={() => setMenuAcik(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", zIndex: 100 }} />
            <aside style={{ position: "fixed", top: 0, left: 0, bottom: 0, width: 300, maxWidth: "86%", background: "var(--header)", borderRight: "1px solid var(--border)", zIndex: 101, padding: "calc(18px + env(safe-area-inset-top)) 12px 24px", overflowY: "auto", animation: "menuSlide 0.24s cubic-bezier(.16,1,.3,1)", backdropFilter: "blur(24px)", boxShadow: "24px 0 60px rgba(0,0,0,.25)" }}>
              <div style={{ padding: "4px 8px 20px", borderBottom: "1px solid var(--divider)", marginBottom: 12, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <div style={{ width: 42, height: 42, borderRadius: 14, background: "var(--accent-strong)", color: "#fff", display: "grid", placeItems: "center", boxShadow: "none" }}><Icon name="wallet" size={21} /></div>
                  <div>
                    <div style={{ fontSize: 17, fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", fontWeight: 700, color: "var(--text)" }}>Bütçe Takip</div>
                    <div style={{ color: "var(--text4)", fontSize: 11, marginTop: 2 }}>Finans merkeziniz</div>
                  </div>
                </div>
                <button aria-label="Menüyü kapat" onClick={() => setMenuAcik(false)} style={{ width: 34, height: 34, borderRadius: 11, border: "1px solid var(--border)", background: "var(--card2)", color: "var(--text3)", display: "flex", alignItems: "center", justifyContent: "center", padding: 0, margin: 0, lineHeight: 0, boxSizing: "border-box", flexShrink: 0, cursor: "pointer" }}><Icon name="close" size={17} /></button>
              </div>
              {menuGruplari.map(grup => (
                <div key={grup.grup} style={{ marginBottom: 8 }}>
                  <div style={{ color: "var(--text5)", fontSize: 11, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.12em", padding: "10px 12px 6px" }}>{grup.grup}</div>
                  {grup.items.map(t => (
                    <button key={t.id} onClick={() => { setAktifTab(t.id); setMenuAcik(false); }} style={{ width: "100%", display: "flex", alignItems: "center", gap: 12, padding: "11px 12px", marginBottom: 3, borderRadius: 13, background: aktifTab === t.id ? "var(--accent-soft)" : "transparent", border: aktifTab === t.id ? "1px solid rgba(167,78,50,.2)" : "1px solid transparent", cursor: "pointer", textAlign: "left" }}>
                      <span style={{ width: 35, height: 35, borderRadius: 11, display: "grid", placeItems: "center", color: aktifTab === t.id ? "var(--accent)" : "var(--text4)", background: aktifTab === t.id ? "rgba(167,78,50,.1)" : "var(--card2)" }}><Icon name={t.icon} size={18} /></span>
                      <span style={{ color: aktifTab === t.id ? "var(--text)" : "var(--text3)", fontSize: 13, fontWeight: aktifTab === t.id ? 700 : 500, flex: 1 }}>{t.label}</span>
                      <span style={{ color: "var(--text5)" }}><Icon name="chevron" size={16} /></span>
                    </button>
                  ))}
                </div>
              ))}
            </aside>
          </>
        )}
        {/* CONTENT */}
        <KayitDurumu />
        <main className="app-content">
          {/* Hızlı işlem penceresi Ozet içinde portala taşındığı için bileşeni
              sekmeler arasında bağlı tutuyoruz. Görsel içerik gizlenir; modal
              document.body üzerinde açılarak mevcut sekmeyi değiştirmez. */}
          <div style={{ display: aktifTab === "ozet" ? "block" : "none" }}>
            <Ozet kartBorcOzetleri={kartBorcOzetleri} islemler={islemler} setIslemler={setIslemler} kartlar={kartlar} setKartlar={setKartlar} donemGun={parseInt(donemGun)||15} kategoriler={kategoriler} odemeler={odemeler} setOdemeler={setOdemeler} hizliModal={hizliModal} setHizliModal={setHizliModal} hizliSablonlar={hizliSablonlar} setHizliSablonlar={setHizliSablonlar} taksitler={taksitler} setTaksitler={setTaksitler} kartOdemeleri={kartOdemeleri} setKartOdemeleri={setKartOdemeleri} bakiyeHesapModu={bakiyeHesapModu} tasarrufHedefi={tasarrufHedefi} grafikTipi={grafikTipi} />
          </div>
          {aktifTab === "islemler" && <IslemlerTab setOdemeler={setOdemeler} islemler={islemler} setIslemler={setIslemler} odemeler={odemeler} butceler={butceler} setButceler={setButceler} kategoriler={kategoriler} setKategoriler={setKategoriler} kartlar={kartlar} kartOdemeleri={kartOdemeleri} setKartOdemeleri={setKartOdemeleri} donemGun={parseInt(donemGun)||15} />}
          {aktifTab === "odemeler" && <OdemelerTab odemeler={odemeler} setOdemeler={setOdemeler} islemler={islemler} setIslemler={setIslemler} kategoriler={kategoriler} donemGun={parseInt(donemGun)||15} />}
          {aktifTab === "tekrarlayan" && <TekrarlayanTab tekrarlayan={tekrarlayan} setTekrarlayan={setTekrarlayan} kategoriler={kategoriler} setKategoriler={setKategoriler} />}
          {aktifTab === "analiz" && <AnalizTab bakiyeHesapModu={bakiyeHesapModu} islemler={islemler} kategoriler={kategoriler} donemGun={parseInt(donemGun)||15} kartlar={kartlar} odemeler={odemeler} kartOdemeleri={kartOdemeleri} />}
          {aktifTab === "krediler" && <KredilerTab krediler={krediler} setKrediler={setKrediler} />}
          {aktifTab === "taksitler" && <TaksitlerTab taksitler={taksitler} setTaksitler={setTaksitler} />}
          {aktifTab === "maas" && <MaasTab zamlar={zamlar} setZamlar={setZamlar} maasAyarlar={maasAyarlar} setMaasAyarlar={setMaasAyarlar} enflasyon={enflasyon} setEnflasyon={setEnflasyon} />}
          {aktifTab === "ayarlar" && <AyarlarTab setIslemler={setIslemler} kartOdemeleri={kartOdemeleri} devirLog={devirLog} setDevirLog={setDevirLog} donemGun={donemGun} setDonemGun={setDonemGun} kartlar={kartlar} setKartlar={setKartlar} kategoriler={kategoriler} setKategoriler={setKategoriler} tema={tema} setTema={setTema} sonBildirimPlanlama={sonBildirimPlanlama} islemler={islemler} odemeler={odemeler} tasarrufHedefi={tasarrufHedefi} setTasarrufHedefi={setTasarrufHedefi} grafikTipi={grafikTipi} setGrafikTipi={setGrafikTipi} bildirimAyarlari={bildirimAyarlari} setBildirimAyarlari={setBildirimAyarlari} bakiyeHesapModu={bakiyeHesapModu} setBakiyeHesapModu={setBakiyeHesapModu} />}
        </main>

        {/* ALT NAVİGASYON */}
        <nav className="nav-dock" aria-label="Ana navigasyon">
          <div className="nav-dock-inner">
            {[
              { id: "ozet", label: "Özet", icon: "home" },
              { id: "islemler", label: "İşlemler", icon: "wallet" },
              { id: "_ekle", label: "Yeni", icon: "plus" },
              { id: "odemeler", label: "Ödemeler", icon: "card" },
              { id: "analiz", label: "Raporlar", icon: "chart" },
            ].map(t => {
              if (t.id === "_ekle") {
                return (
                  <button aria-label="Yeni işlem ekle" key="_ekle" onClick={() => setHizliModal(true)} style={{ width: 48, height: 48, borderRadius: 15, border: "none", cursor: "pointer", background: "var(--accent-strong)", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", padding: 0, margin: 0, marginTop: 0, boxShadow: "none", flexShrink: 0, boxSizing: "border-box", lineHeight: 0 }}><Icon name="plus" size={23} strokeWidth={2.3} /></button>
                );
              }
              const aktif = aktifTab === t.id;
              return (
                <button key={t.id} onClick={() => setAktifTab(t.id)} aria-current={aktif ? "page" : undefined} style={{ background: aktif ? "var(--accent-soft)" : "transparent", border: "1px solid transparent", borderRadius: 15, cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 4, padding: "8px 3px 6px", flex: 1, minWidth: 0, margin: "0 1px", color: aktif ? "var(--accent)" : "var(--text4)" }}>
                  <Icon name={t.icon} size={19} strokeWidth={aktif ? 2.25 : 1.8} />
                  <span style={{ fontSize: 11, fontWeight: aktif ? 800 : 600, color: "currentColor", whiteSpace: "nowrap" }}>{t.label}</span>
                </button>
              );
            })}
          </div>
        </nav>

      </div>
      </div>
      <ToastKutusu />
    </>
    </KategoriGorunumContext.Provider>
  );
}
