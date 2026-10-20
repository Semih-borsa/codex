# Finans motoru — değiştirilmemiş kaynak üzerindeki temel testler

Bu klasör yalnız test, sentetik veri ve sonuç dosyaları içerir. Üretim kodu veya kullanıcının mevcut kayıt/yedek dosyaları değiştirilmez. Yeni finans motoru oluşturulmaz.

## Esas alınan kaynak

- Depo içindeki esas: `downloads/App.jsx`.
- Tam React/Capacitor projesindeki karşılığı: `src/App.jsx`.
- `downloads/finans-takip-bildirim/src/App.jsx` dağıtım kopyasıdır.
- Üç kopyanın SHA-256 değeri başlangıç ve bitişte aynıdır: `118e06964a75c4fd3994b1bb80bcbf2fc4fd9371cc4494dadf7db774f34c0a74`.
- İncelenen commit ve işlev satırları: `kaynak-kaydi.json`.

## Yöntem

`source-harness.mjs`, projede zaten kurulu `@babel/parser` kullanarak kaynak AST'sini okur. Seçili üretim işlevlerinin ve kredi/ödeme işleyicilerinin metnini **gövdelerini değiştirmeden** Node VM içinde çalıştırır. Test beklentileri sabit iş kurallarıdır; finans hesabının test içinde ikinci bir uygulaması bulunmaz.

Depolama yalnız `MemoryStorage` içindedir. React hook testlerinde küçük hook taklitleri kullanılır; bunlar gerçek React yaşam döngüsünün yerine geçtiği için ayrıca tarayıcı kontrolleri yapılır.

`browser.test.mjs`, aynı App.jsx'yi mevcut esbuild ile yalnız bellekte derler (`write:false`). Her test boş, ayrı Chromium bağlamında başlar; uygulamanın normal kayıt/yedek yan etkileri yalnız o testin yapay deposunda oluşur. Kaynak, `dist`, Vite önbelleği veya kurulu paketler yazılmaz. Testin yerel sunucusu ve tarayıcısı işlem sonunda kapanır. Android bildirim eklentisi Chromium'da çalıştırılmaz.

Üretim APK'sına, gerçek kullanıcı WebView deposuna veya banka verisine bağlanılmaz. Tarih/dönem testleri `Europe/Istanbul` saat dilimini ve açık tarihli örnekleri kullanır.

## Çalıştırma

Node 24.19.0 ile çalıştırılmıştır. Kurulu React projesinin bağımlılıkları yeniden kullanılır; testler paket yüklemez.

Depo kökünde:

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/finance.test.mjs
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/browser.test.mjs
```

Birlikte:

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/finance.test.mjs tests/finance-baseline/browser.test.mjs
```

Ortam farklıysa mevcut kurulum yolları değişkenlerle belirtilebilir:

- `FINANS_SOURCE`: incelenecek App.jsx dosyası; varsayılan depodaki `downloads/App.jsx`.
- `FINANS_DEPENDENCY_ROOT`: mevcut `node_modules` içeren React proje dizini.
- `FINANS_PLAYWRIGHT`: mevcut Playwright modülünün yolu/ESM URL'si.
- `FINANS_CHROMIUM`: mevcut Chromium çalıştırılabilir dosyası.

Bulut ortamındaki bağımlılık kökü `/workspace/finans-upload/finans-takip`, Playwright yolu `/opt/codex/cua_node/lib/node_modules/playwright-core/index.mjs`, tarayıcı `/usr/bin/chromium` olarak kullanılmıştır. Bu yollar farklı bilgisayarlarda otomatik olarak var kabul edilmemelidir.

**Mevcut kaynakla çıkış kodu 1 beklenir:** kabul senaryoları gerçekten başarısızdır. Testler `todo` veya `skip` yapılarak yeşile çevrilmemiştir. Düzeltmeler onaylandıktan sonra aynı beklentilerin geçmesi hedeflenir.

## Sonuç dosyaları

| Dosya | İçerik |
| --- | --- |
| `sonuclar.tap` | 26 işlev/işleyici testi: 18 geçti, 8 başarısız |
| `tarayici-sonuclari.tap` | 3 gerçek React ekran akışı: 1 geçti, 2 başarısız |
| `tum-sonuclar.tap` | Birlikte çalıştırma: 29 test, 19 geçti, 10 başarısız |
| `eslint-sonuclari.json` | Üretim kaynağının mevcut kurallarla statik denetimi: 53 hata seviyesi, 12 uyarı seviyesi |
| `kaynak-kaydi.json` | Kaynak hash'i, commit, çalışma ortamı ve işlev satırları |

ESLint bulguları finansal test sayısına dahil değildir. Hata seviyesinde lint bulgusu, tek başına çalıştırma veya hesaplama hatası değildir.

## Sonuçları yorumlama sınırı

- S03b, açık `krediId` ve `taksitNo` bağlantısı verilen **gelecekteki entegrasyon sözleşmesini** sınar. Mevcut sürüm bu bağlantılı ödeme kaydını kendisi üretmez. Eski bağımsız manuel ödemeler eşleştirilmiş varsayılmamalıdır.
- S07a, tarihli kart taksit-ekstre planının bulunmadığını gösteren yetenek testidir. Henüz olmayan motorun banka kesim kurallarını doğruladığı anlamına gelmez.
- S07b banka kredisi takvimini kontrol eder; kart ekstre dağıtımının geçtiği anlamına gelmez.
- S08a eski nesnenin yerinde değişmediğini; S08b aynı geçmiş toplamın bugünkü ekstre durumundan etkilendiğini ayrı ayrı sınar.
- S09 eski format adlarıyla **yapay uyumluluk örneklerini** sınar; kullanıcının gerçek tarihi yedekleri açılmamıştır.
- S10b'de eksik yedek mevcut işlem anahtarını kaldırır, fakat gerçek işleyici yükleme öncesi kurtarma kopyasını oluşturur. Sonuç geri döndürülemez veri kaybı olarak sunulmamalıdır.
- S10c/e yazma istisnası sonrası geri almayı sınar. İşletim sistemi süreci yazmanın ortasında öldürürse ne olacağını doğrulamaz.
- UI kontrolleri masaüstü Chromium'un mobil boyutundadır; gerçek Android WebView/cihaz testi değildir.

Türkçe sonuçlar ve düzeltme planı: [`../../FINANS_TEST_SONUCLARI_VE_DUZELTME_PLANI.md`](../../FINANS_TEST_SONUCLARI_VE_DUZELTME_PLANI.md).

## Aşama A güncellemesi — 16 Ekim 2026

Yukarıdaki 29 test/19 geçti/10 başarısız ve eski hash bilgileri ilk analiz anının kaydıdır. İlk sonuçlar değiştirilmedi. Aşama A uygulaması sonrası aynı kabul testlerinde 23 geçti, 6 başarısız; ek 18 işlev ve 2 ekran testi geçti. Birlikte **49 test / 43 geçti / 6 başarısız / 0 atlanan**. Kalan hatalar B/C/D kapsamındadır; tüm test komutunun çıkışı 1 olmaya devam eder.

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/finance.test.mjs tests/finance-baseline/browser.test.mjs tests/finance-baseline/phase-a.test.mjs tests/finance-baseline/phase-a.browser.test.mjs
```

Yalnız ek A testleri için son iki dosya kullanılır. Gerçek kaynak yeni bağlantı yardımcı işleviyle VM'ye alınır; test saati 16 Ekim 2026 olarak sabittir. Eski kaynak bu yardımcı bulunmadan da çalıştırılabilir; `asama-a-oncesi-tekrar.tap` eski kaynakla aynı 19/10 sonucunu doğrular.

Güncel kanıtlar `asama-a-*` dosyaları; ayrıntılı rapor [`../../ASAMA_A_SONUC_RAPORU.md`](../../ASAMA_A_SONUC_RAPORU.md). Güncel hash ve stil karşılaştırması `asama-a-kapsam-kaydi.json` içindedir.

## Aşama C — Yedekleme güvenliği, 16 Ekim 2026

Aşama A'daki dört test dosyasının toplam **49 testi** değiştirilmeden tekrar çalıştırıldı. Başlangıç: **43 geçti / 6 başarısız** (`asama-c-oncesi-49.tap`). Finalde bu mevcut testler **44 geçti / 5 başarısız**: S10b düzeldi; Aşama A'nın 20 ek testi geçti.

Yeni `phase-c.test.mjs` **41** işlev/işleyici testi; `phase-c.browser.test.mjs` **9** gerçek React ekran testi içerir. Final birleşik kanıt:

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/finance.test.mjs tests/finance-baseline/browser.test.mjs tests/finance-baseline/phase-a.test.mjs tests/finance-baseline/phase-a.browser.test.mjs tests/finance-baseline/phase-c.test.mjs tests/finance-baseline/phase-c.browser.test.mjs
```

**99 test / 94 geçti / 5 başarısız / 0 atlanan**, çıkış 1. Kalanlar B/D kapsamındaki S04a, S04b, UI-S04, S08b, S07a. Kaynak hash'i/kapsam: `asama-c-kapsam-kaydi.json`; kesin final test çıktısı: `asama-c-tum-sonuclar.tap`.

Yalnız yeni C testleri:

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/phase-c.test.mjs tests/finance-baseline/phase-c.browser.test.mjs
```

Final C kanıtı `asama-c-odakli-sonuclar.tap`: **50/50 geçti**, çıkış 0. `asama-c-ek-testler.tap`, `asama-c-ek-tarayici.tap` ve `asama-c-mevcut-49.tap` geliştirme sırasındaki ara çalıştırma kayıtlarıdır; nihai kanıt yerine kullanılmaz.

`phase-c-scope-check.mjs`, bulut ortamındaki `/workspace/phase-c-backup/` Aşama A kopyasıyla AST/kaynak karşılaştırması yapar. Finans motorları, kredi ekranı, devir, localStorage hook'u ve stil blokları değişmemiştir. Yeni C yardımcıları sadece yedek/geri yükleme kapsamındadır.

Kalıcı depolama geri alma yazmalarını da reddederse yerinde atomiklik garanti edilmez; C26 testi bu durumda başarı mesajı verilmediğini, eski verinin güvenlik kopyası ve kurtarma dosyasında kaldığını doğrular. Android'de gerçek dosya kaydetme/geri açma ve süreç kaybı ayrıca denenmelidir.

Ayrıntılı rapor: [`../../ASAMA_C_SONUC_RAPORU.md`](../../ASAMA_C_SONUC_RAPORU.md).

## Aşama B — Kart borcu/dönem/ekstre güvenliği, 16 Ekim 2026

Önceki altı test dosyasının **99 testi** değişmeden yeniden çalıştırıldı. Başlangıç **94 geçti / 5 başarısız** (`asama-b-oncesi-99.tap`); finalde aynı testler **98 geçti / 1 başarısız**. A'nın 20 ve C'nin 50 ek testi geçti. S04a, S04b, UI-S04 ve S08b düzeldi; S07a (D: tarihli kart taksit dağıtımı) korunarak başarısız kaldı.

Yeni `phase-b.test.mjs` 34 işlev/işleyici, `phase-b.browser.test.mjs` 5 gerçek React ekran testi içerir. Birlikte **138 test / 137 geçti / 1 başarısız / 0 atlanan**, çıkış 1:

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/finance.test.mjs tests/finance-baseline/browser.test.mjs tests/finance-baseline/phase-a.test.mjs tests/finance-baseline/phase-a.browser.test.mjs tests/finance-baseline/phase-c.test.mjs tests/finance-baseline/phase-c.browser.test.mjs tests/finance-baseline/phase-b.test.mjs tests/finance-baseline/phase-b.browser.test.mjs
```

Yalnız yeni B testleri: son iki dosyayla aynı komut; **39/39 geçti**, çıkış 0. Kesin final kanıtı `asama-b-tum-sonuclar.tap` ve `asama-b-odakli-sonuclar.tap`; diğer B test çıktıları ara çalıştırmalardır.

`phase-b-scope-check.mjs`, `/workspace/phase-b-backup/` A+C kaynağıyla karşılaştırır. Günlük hareket motoru, kredi ekranı, devir, C önizleme/kapsam/güvenlik kopyası ve bütün stil blokları korunmuştur. C doğrulayıcı/geri yükleme kontrollerine yalnız yeni isteğe bağlı kart/ekstre alanları eklenmiştir.

Ayrıntılar ve geri dönüş: [`../../ASAMA_B_SONUC_RAPORU.md`](../../ASAMA_B_SONUC_RAPORU.md). Gerçek kullanıcı/Android verileri kullanılmadı; testler bağımsız sentetik depolardadır. D geliştirilmedi.

## Aşama D — Tarihli kredi kartı taksit planı, 16 Ekim 2026

Başlangıçtaki 138 test yeniden çalıştırıldı: 137 geçti, yalnız S07a başarısızdı (`/workspace/phase-d-backup/baseline-138.tap`). Mevcut sekiz test dosyasının beklentileri değiştirilmedi. D sonrasında bu 138 testin tamamı geçer; A'nın 20, C'nin 50 ve B'nin 39 ek testi korunur.

Yeni `phase-d.test.mjs` 60, `phase-d.browser.test.mjs` 9 test içerir. Son birleşik komut:

```sh
TZ=Europe/Istanbul node --test --test-reporter=tap tests/finance-baseline/finance.test.mjs tests/finance-baseline/browser.test.mjs tests/finance-baseline/phase-a.test.mjs tests/finance-baseline/phase-a.browser.test.mjs tests/finance-baseline/phase-c.test.mjs tests/finance-baseline/phase-c.browser.test.mjs tests/finance-baseline/phase-b.test.mjs tests/finance-baseline/phase-b.browser.test.mjs tests/finance-baseline/phase-d.test.mjs tests/finance-baseline/phase-d.browser.test.mjs
```

Kesin son kanıt `asama-d-tum-sonuclar.tap`; eski `asama-d-ek-*` ve `asama-d-mevcut-138.tap` geliştirme ara kayıtlarıdır. `phase-d-scope-check.mjs`, D öncesi A+B+C kaynağıyla finans/hareket/tema/ekstre koruma karşılaştırması yapar. Çıktısı `asama-d-kapsam-kaydi.json`, yalnız D kaynak farkı `asama-d-kaynak-degisiklikleri.patch` içindedir.

S07a yalnız taksit satır dizisinin varlığını sınar. Eksik tarihli takip kayıtları tarihsiz kalır; bu tek testten tarih motorunun doğruluğu çıkarılmamıştır. Yeni D testleri gerçek tarihli/kuruşlu dağıtımı, bankanın doğruladığı istisnai ilk tarihi, ay sonu/artık yılı, tekil ekstre bağını, ödeme payını, iadeyi/geri almayı, iptali, borcun çift sayılmamasını, yedek uyumluluğunu ve tarayıcıdaki kayıt/geri yükleme/yazma hatası akışlarını ayrıca sınar.

Tüm testler sentetik verilerle izole Chromium/VM ortamında çalışır. Gerçek telefon veya kişisel yedekler kullanılmaz. Uygulamayı kapatıp açma testi sayfa yeniden yüklemesidir; Android süreç kaybı ayrıca denenmelidir. Ayrıntılar: [`../../ASAMA_D_SONUC_RAPORU.md`](../../ASAMA_D_SONUC_RAPORU.md).

Son D sonucu: **207 test / 207 geçti / 0 başarısız / 0 atlanan**, çıkış 0. Önceki 138 + yeni 69 (60 işlev, 9 ekran). Web derlemesi, Capacitor Android senkronizasyonu ve Android debug derlemesi başarılı.
