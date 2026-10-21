import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

export const repoRoot = fileURLToPath(new URL('../../', import.meta.url));
export const sourcePath = process.env.FINANS_SOURCE || path.join(repoRoot, 'downloads/App.jsx');
export const source = fs.readFileSync(sourcePath, 'utf8');
export const sourceHash = crypto.createHash('sha256').update(source).digest('hex');

// Reuse the existing parser; never install a dependency or import/mount the app.
let parser;
for (const root of [process.env.FINANS_DEPENDENCY_ROOT, process.cwd(), '/workspace/finans-upload/finans-takip'].filter(Boolean)) {
  try { parser = createRequire(path.join(root, 'package.json'))('@babel/parser'); break; } catch {}
}
if (!parser) throw new Error('Mevcut @babel/parser bulunamadı. FINANS_DEPENDENCY_ROOT ile kurulu React projesini belirt; bu test paket yüklemez.');
const ast = parser.parse(source, { sourceType: 'module', plugins: ['jsx'] });
const declarations = new Map();
for (const node of ast.program.body) {
  if (node.type === 'FunctionDeclaration') declarations.set(node.id.name, node);
  if (node.type === 'VariableDeclaration') {
    for (const d of node.declarations) if (d.id.type === 'Identifier') declarations.set(d.id.name, { ...d, declarationKind: node.kind });
  }
}

export function location(name, owner) {
  return (owner ? callbackNode(owner, name) : declarations.get(name))?.loc.start.line;
}
function callbackNode(owner, name) {
  let found;
  function visit(node) {
    if (!node || typeof node !== 'object' || found) return;
    if (node.type === 'VariableDeclarator' && node.id?.name === name) { found = node.init; return; }
    for (const value of Object.values(node)) {
      if (Array.isArray(value)) value.forEach(visit);
      else if (value && typeof value === 'object') visit(value);
    }
  }
  const fn = declarations.get(owner);
  if (!fn) throw new Error(`Kaynak bileşeni bulunamadı: ${owner}`);
  visit(fn.body);
  if (!found) throw new Error(`Kaynak işleyicisi bulunamadı: ${owner}.${name}`);
  return found;
}

// DOM Storage-compatible enumerable keys. All values and failures are in memory.
export class MemoryStorage {
  #data = new Map();
  #failure;
  constructor(values = {}) { for (const [key, value] of Object.entries(values)) this.setItem(key, value); }
  get length() { return this.#data.size; }
  key(index) { return [...this.#data.keys()][index] ?? null; }
  getItem(key) { return this.#data.get(String(key)) ?? null; }
  failOnce(key) { this.#failure = String(key); }
  setItem(key, value) {
    key = String(key);
    if (this.#failure === key) { this.#failure = undefined; throw new Error('Simüle edilen depolama kotası hatası'); }
    value = String(value);
    this.#data.set(key, value);
    Object.defineProperty(this, key, { value, configurable: true, enumerable: true, writable: true });
  }
  removeItem(key) { this.#data.delete(String(key)); delete this[key]; }
  snapshot() { return Object.fromEntries(this.#data); }
}

const functions = ['yerelFinansYorumlari',
  'ekstreMetinAnahtari', 'ekstreTutarOku', 'ekstreTarihOku', 'ekstreKategoriOner', 'ekstreSatirlariAyikla', 'ekstreAnalizHesapla', 'ekstreYorumlari', 'ekstreParaOku', 'ekstreUzunTarih', 'ekstreKategoriBul', 'ekstreBankaliAyikla', 'ekstreOzetV2', 'ekstreArsivDogrula', 'aiKategoriGrubu', 'aiFinansOzeti', 'aiYanitiDogrula', 'aiHataMesaji', 'aiHttpHatasi', 'aiIstekGovdesi',
  'planlamaVarsayilan', 'planlamaVerisiniDogrula', 'planlamaTarihleri', 'planlamaHedefleri', 'planlamaHareketleri', 'bekleyenNakitHareketleri', 'nakitAkisiHesapla', 'yedekDosyasiniAktar', 'yerelTarihAnahtari', 'yerelTarihOku', 'takvimGunSirasi', 'takvimGunFarki', 'takvimGunuEkle',
  'maasDonemiBul', 'donemdekiSabitOdemeTarihi', 'sayisalOku', 'paraKurus', 'paraOku', 'paraTopla',
  'tutarGecerli', 'tamSayiGecerli', 'aylikTarih', 'formHatasi', 'devirIslemiMi', 'donemDevriHesapla',
  'kartTaksitPlaniOlustur', 'kartTaksitSatirOzeti', 'kartTaksitPlanDurumu', 'kartTaksitYukumlulukleri', 'kartTaksitBagla', 'kartTaksitOdemePayi', 'kartTaksitIadesi', 'kartTaksitIadeGeriAl', 'kartTaksitIptal', 'kartTaksitPlaniniDogrula', 'kartTaksitTakipOzeti', 'ayinGunu',
  'kartEkstreTarihteGecerli', 'kartKimligi', 'kartKayitlari', 'kartSonDurum', 'kartEkstreKayitlari', 'kartEkstreOdemesiMi', 'kartEkstreBakiyeleri', 'kartAktifEkstreKalani', 'kartEkstreleriKoru', 'kartEkstreGuncelle', 'kartBorcDurumu', 'kartBorcGirdisi',
  'kartEkstreDoneminiBul', 'kartToplamBorcHesapla', 'kartBorcDegisimi', 'kartGuncelToplami',
  'kartKaydiniGuncelle', 'kartAnalizHareketleri', 'analizIslemleri', 'maasDonemKey',
  'krediOdemeKaydiMi', 'gunlukFinansHareketleri', 'donemBakiyeHesapla', 'donemSonuGiderTahmini',
  'yedekKapsami', 'yedekEtkiHesapla', 'yedekGuvenlikKopyasiOlustur', 'yedekBaglariniDogrula',
  'yedekVerisiniDogrula', 'yedegiGuvenleYaz', 'yerelYedekPaketi', 'kayitDegisiklikleri', 'useLocalStorage',
];
const constants = ['EKSTRE_TURLERI', 'EKSTRE_KATEGORILERI', 'PARA_UST_SINIR', 'AYLAR', 'AYLAR_TAM', 'KATEGORI_SIMGE_SECENEKLERI'];
const extracted = [...constants, ...functions].map(name => {
  const node = declarations.get(name);
  if (!node && ['kartEkstreTarihteGecerli','yedekDosyasiniAktar','kartTaksitPlaniOlustur','kartTaksitSatirOzeti','kartTaksitPlanDurumu','kartTaksitYukumlulukleri','kartTaksitBagla','kartTaksitOdemePayi','kartTaksitIadesi','kartTaksitIadeGeriAl','kartTaksitIptal','kartTaksitPlaniniDogrula','kartTaksitTakipOzeti','ayinGunu','kartKimligi','kartKayitlari','kartSonDurum','kartEkstreKayitlari','kartEkstreOdemesiMi','kartEkstreBakiyeleri','kartAktifEkstreKalani','kartEkstreleriKoru','kartEkstreGuncelle','kartBorcDurumu','kartBorcGirdisi','krediOdemeKaydiMi','yedekKapsami','yedekEtkiHesapla','yedekGuvenlikKopyasiOlustur','yedekBaglariniDogrula'].includes(name)) return ''; // Older baseline has no explicit loan-payment marker helper.
  if (!node) throw new Error(`Kaynak işlevi bulunamadı: ${name}`);
  const text = source.slice(node.start, node.end);
  return node.type === 'VariableDeclarator' ? `${node.declarationKind} ${text};` : text;
}).join('\n');

export function createHarness(seed = {}) {
  const storage = new MemoryStorage(seed.storage);
  const notices = [], calls = [];
  class TestDate extends Date {
    constructor(...args) { super(...(args.length ? args : ['2026-10-16T12:00:00+03:00'])); }
    static now() { return new Date('2026-10-16T12:00:00+03:00').getTime(); }
  }
  const context = vm.createContext({
    Date: TestDate, Intl, Event, CustomEvent, Blob, URL, Promise,
    localStorage: storage,
    window: { localStorage: storage, confirm: () => true, dispatchEvent: () => {}, location: { reload: () => {} } },
    setTimeout: () => 0,
    document: { createElement: () => ({ click() {} }) },
    toast: (...args) => notices.push(args),
    kayitHatasiBildir: message => notices.push([message, 'hata']),
    odemeOnayi: () => true,
    _okunamayanAnahtarlar: new Set(), _kayitHatasi: null, _buIslemKayitHatali: false, _kayitGrubu: null,
    useState: initial => [typeof initial === 'function' ? initial() : initial, () => {}],
    useRef: value => ({ current: value }), useCallback: fn => fn,
    islemler: [], odemeler: [], kartlar: [], kartOdemeleri: [], krediler: [], taksitler: [],
    donemGun: 15, duzenleId: null, bos: {}, form: {}, ...seed.state,
  });
  for (const [setter, field] of Object.entries({ setKartlar:'kartlar',setKartOdemeleri:'kartOdemeleri',setKartOdemeModal:'kartOdemeModal',setKategoriler:'kategoriler',setKartIsim:'kartIsim',setKartKesimTarihi:'kartKesimTarihi',setKartSonOdemeTarihi:'kartSonOdemeTarihi',setKartTarihTaslaklari:'kartTarihTaslaklari',setIslemler: 'islemler', setOdemeler: 'odemeler', setKrediler: 'krediler', setTaksitler: 'taksitler', setForm: 'form', setDuzenleId: 'duzenleId', setModal: 'modal', setBekleyenYedek: 'bekleyenYedek' })) {
    context[setter] = value => { calls.push(setter); context[field] = typeof value === 'function' ? value(context[field]) : value; return true; };
  }
  vm.runInContext(extracted, context, { filename: `${sourcePath}#unchanged-source-functions` });
  return {
    context, storage, notices, calls,
    call: (name, ...args) => context[name](...args),
    callback(owner, name, ...args) {
      const node = callbackNode(owner, name);
      const fn = vm.runInContext(`(${source.slice(node.start, node.end)})`, context, { filename: `${sourcePath}:${node.loc.start.line}#${owner}.${name}` });
      return fn(...args);
    },
    balance(overrides = {}) {
      return context.donemBakiyeHesapla({
        islemler: context.islemler, odemeler: context.odemeler, kartlar: context.kartlar,
        kartOdemeleri: context.kartOdemeleri, krediler: context.krediler, taksitler: context.taksitler,
        ilk: '2026-10-15', son: '2026-11-14', bugun: '2026-10-16', ...overrides,
      });
    },
  };
}
