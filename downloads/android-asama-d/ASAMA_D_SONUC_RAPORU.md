# Aşama D — Tarihli kredi kartı taksitlerinin ekstre dağıtımı

Tarih: 16 Ekim 2026. Yalnız onaylanan Aşama D uygulandı. A, B ve C korunmuştur. Başka geliştirmeye geçilmedi; commit/push yapılmadı. Gerçek telefon kayıtlarına erişilmedi, kişisel veri veya eski yedek dosyası değiştirilmedi.

## 1. Yapılan değişiklikler ve dosyalar

Ana çalışma kaynağı `downloads/App.jsx` dosyasıdır. Aynı içeriğin teslim aynası `downloads/finans-takip-bildirim/src/App.jsx`, çalıştırılan tam proje kopyası `/workspace/finans-upload/finans-takip/src/App.jsx` konumundadır. Teslim edilen `App.jsx`, kullanıcının projesindeki **`src/App.jsx`** yerine kaydedilir. Üç kaynak kopyasının SHA-256 eşitliği kapsam kontrolünde doğrulandı. Uygulama için başka kaynak dosyası veya yeni bağımlılık gerekmez.

Üretim değişiklikleri:

| İşlev / bileşen | Değişiklik |
|---|---|
| `kartTaksitPlaniOlustur` | Kalıcı kart/plan kimliği, kuruş hesabı, tarihli taksit ve planlanan ekstre kimliği |
| `kartTaksitSatirOzeti`, `kartTaksitPlanDurumu` | Kalan, ödeme payı, iade ve aktif/tamamlandı/iptal durumu |
| `kartTaksitYukumlulukleri` | Ortak motorun kullanacağı plan yükümlülükleri ve banka borcuyla çakışma kontrolü |
| `kartTaksitBagla`, `kartTaksitOdemePayi` | Kullanıcı doğrulamasıyla kesin ekstre bağı ve mevcut ödeme payının dağıtılması |
| `kartTaksitIadesi`, `kartTaksitIadeGeriAl`, `kartTaksitIptal` | Kayıt silmeden iade/geri alma ve gelecekteki bağlı olmayan taksitlerin iptali |
| `kartEkstreBakiyeleri` | Bankaya yansıdığı açıkça doğrulanan iade düzeltmesinin kalan ekstre borcundan düşülmesi |
| `donemBakiyeHesapla`, `donemSonuGiderTahmini` | Bankada zaten temsil edilmeyen vadesi dönem sonuna kadar gelen taksidin tek seferlik yükümlülük olarak eklenmesi |
| `TaksitlerTab`, `TaksitEkstreDetay` | İlgili tarih/tutar alanları, ekstre/ödeme bağı, fark gösterimi, iade ve iptal akışı |
| `IslemlerTab` | Paylaştırılmış ekstre ödemesinin pay toplamının altına indirilmesini engelleme |
| Yedek doğrulayıcıları ve `AyarlarTab` | Yeni isteğe bağlı alanların ve çapraz bağların kontrolü; plan bağlı kartın yanlışlıkla silinmesini engelleme |
| `Ozet`, `AnalizTab`, `App` | Mevcut motorlara taksit verisinin iletilmesi; yalnız yeni planların durum bilgisinin güncellenmesi |

`gunlukFinansHareketleri`, günlük harcama/kategori analiz motorları, kredi ödeme akışı, pozitif devir işlevi, değişmez ekstre koruma işlevleri ve C'nin yedek önizleme/güvenlik kopyası yapısı kaynak karşılaştırmasında değişmedi. Bütün `<style>` blokları aynı kaldı. Açık/koyu/gri temalar, halka/ibre, grafik yerleşimleri ve animasyon süreleri değiştirilmedi. Formlara yalnız bu iş için gerekli alanlar eklendi.

Test ve belge dosyaları:

- `tests/finance-baseline/phase-d.test.mjs`: 60 işlev/hesaplama testi.
- `tests/finance-baseline/phase-d.browser.test.mjs`: 9 tarayıcı ekran testi.
- `tests/finance-baseline/source-harness.mjs`: Gerçek kaynak yardımcılarını izole test ortamına alma desteği.
- `tests/finance-baseline/phase-d-scope-check.mjs`: D öncesi kaynağa göre değişiklik sınırı kontrolü.
- `tests/finance-baseline/README.md`, bu rapor ve `asama-d-*` kanıt dosyaları.

Mevcut A/B/C ve ilk kabul testlerinin beklentileri değiştirilmedi. Önceki raporlar korundu.

## 2. Tarih ve tutar kuralları

Yeni tarihli plan için kart, toplam alışveriş tutarı, taksit sayısı, alışveriş tarihi, **bankanın doğruladığı ilk ekstre tarihi**, tercih edilen kesim günü ve ödeme günü veya doğrulanmış ilk ödeme tarihi gereklidir. Eksik ilk ekstre tarihi tahmin edilmez.

| Örnek | Sonuç |
|---|---|
| İlk ekstre 10 Ekim 2026, 3 taksit | 10 Ekim, 10 Kasım, 10 Aralık 2026 |
| Bankanın ilk ekstresi 10 Kasım 2026 | 10 Kasım, 10 Aralık 2026, 10 Ocak 2027; Ekim kaydı yok |
| Kesim günü 31 | Kısa ayda ay sonu; sonraki uygun ayda tekrar 31 |
| Toplam 100 TL / 3 | 33,33 + 33,33 + 33,34 TL |
| İlk doğrulanan kesim 12 Ekim, tercih 10 | İlk taksit 12 Ekim; sonraki taksitler 10 Kasım ve 10 Aralık |

Dağıtım tam sayı kuruşla hesaplanır; kalan kuruş son takside eklenir. Her taksidin ayrı kimliği, planlanan ekstre kimliği, kesim/son ödeme tarihi vardır. Planlanan ekstre gerçek banka ekstresi değildir. Kesin ekstre bağı yalnız aynı kart ve aynı kesim tarihi için kullanıcının doğrulamasıyla kurulur. Bir taksit başka bir kesin ekstreye otomatik taşınamaz.

Doğrulanan istisnai ilk kesim/ödeme tarihi tercih edilen günden önceliklidir. Sonraki aylar tercih edilen günlere göre bağımsız hesaplanır; Şubat kaynaklı tarih kayması sonraki aylara taşınmaz. Gelecekte bankanın tatil/iş günü nedeniyle değiştireceği gerçek tarihler tahmin edilmez; üretilen tarihler seçilen takvim kuralının planıdır.

## 3. Borcun iki kez sayılmasının önlenmesi

Plan oluşturmak alışveriş gideri, ekstre borcu veya nakit ödeme kaydı üretmez. Mevcut `donemBakiyeHesapla` motoruna plan yükümlülüğü verilir; ikinci bir bakiye motoru oluşturulmadı.

Bankanın borç durumu yetkilidir. Kesin ekstre bağı, aynı kesim tarihli banka ekstresi veya alışverişten sonraki banka stok bildirimiyle zaten temsil edilen plan, bankanın üzerine bir kez daha eklenmez. Tarihi bilinmeyen eski banka ekstresinde de tahmini ikinci borç eklemek yerine banka tutarı öncelikli tutulur. Bu, otomatik kayıt birleştirmesi veya ödeme eşleştirmesi değildir; plan ve banka kayıtları ayrı kalır. Fark, ekstre planı ayrıntısında gösterilir. Bilinmeyen kapsam otomatik çözümlenmez; kullanıcının banka verisini doğrulaması gerekir.

Kartın açık bakiyeden düşme tercihi varsa o tercih, yoksa mevcut genel bakiye modu kullanılır. Bankada temsil edilmediği bilinen, bu tercihe göre hesaba dahil edilen kartın dönem sonuna kadar vadesi gelen taksidi bekleyen yükümlülüğe bir kez eklenir. Geçmiş vadesi geçip ödenmemiş olan da kaybolmaz; sonraki farklı kesim tarihli taksit sırf önceki ekstre var diye kapatılmaz. Daha sonraki taksitler tarihli planda korunur. Ay sonu tahminine giren bu sabit tutar günlere yayılarak çoğaltılmaz.

### Ödeme, kısmi ödeme ve geri alma

- Nakit çıkışı mevcut gerçek kart ödeme kaydından gelir. Taksit ayrıntısında ödeme payı tanımlamak yeni nakit çıkışı oluşturmaz.
- Kısmi ödeme yalnız kullanıcı seçimiyle aynı kartın aynı ekstresindeki gerçekleşmiş ödemeden dağıtılır. Toplam pay ödeme tutarını veya kalan taksidi aşamaz.
- Aynı ekstre tamamen ödenmişse, bağlı planların net toplamı banka ekstresinin net tutarıyla tutarlı olduğu sürece bağlı taksitler kapanabilir. Küçük banka ekstresi daha büyük plan toplamını varsayımla kapatmaz.
- Gerçek ödeme İşlemler ekranından geri alınırsa artık bulunmayan ödeme payı hesapta sayılmaz. Kaybolan bağ silinmez; arayüzde geri alınmış olduğu belirtilir.
- Dağıtım payını kaldırmak asıl nakit ödemeyi silmez. Ödeme tutarı payların altına düşürülecekse önce paylar kaldırılmalıdır.
- Tamamlanmış yeni planın ödemesi geri alınınca durum tekrar aktif olabilir. Güncellenen yalnız yeni planın durum bilgisidir; eski taksit/ödeme kayıtlarına geçmiş gider yazılmaz.

### İade ve iptal

İade önce planın net kalanını azaltır. **Bankaya yansıdı** açıkça işaretlenmedikçe gerçek banka borcu değişmez. Bu işaretli iade için kesin ekstre bağı gerekir. Banka onaylı iade, özgün doğrulanmış ekstreyi değiştirmek yerine tarihli `ekstreDuzeltmeleri` kaydı oluşturur. Özgün ekstre ve geçmiş kapanmış dönem toplamları değiştirilmez.

İade nakit gelir oluşturmaz. Ekstreye borçtan fazla ödeme yapılması halinde oluşabilecek pozitif kart alacağı banka hesabına yatırılmış nakit sayılmaz; mevcut kalan borç alt sınırı sıfırdır. İadenin geri alınması hem plan düzeltmesini hem ona bağlı banka düzeltmesini geri alır; başka kartları değiştirmez.

Plan iptali, yalnız gelecekteki kesin ekstreye bağlanmamış taksitleri iptal eder. Geçmiş taksit, kesin ekstre ve gerçek ödeme silinmez. Bankadaki borç yalnız uygulamada plan iptal edildi diye kapatılmaz.

## 4. S07a ve tüm test sonuçları

| Test grubu | Başlangıç | Son durum |
|---|---:|---:|
| Mevcut 138 test | 137 geçti, S07a başarısız | **138 geçti** |
| Aşama A'nın 20 ek testi | Geçiyordu | **20 geçti** |
| Aşama C'nin 50 ek testi | Geçiyordu | **50 geçti** |
| Aşama B'nin 39 ek testi | Geçiyordu | **39 geçti** |
| Yeni D işlev testleri | Yeni | **60 geçti** |
| Yeni D ekran testleri | Yeni | **9 geçti** |
| Birleşik toplam | — | **207 geçti, 0 başarısız, 0 atlanan** |

A/C/B satırları mevcut 138'in alt kümeleridir; toplama ikinci kez eklenmemiştir. Test komutu çıkışı 0. Kesin son kanıt: `tests/finance-baseline/asama-d-tum-sonuclar.tap`. Başlangıç kanıtı: `/workspace/phase-d-backup/baseline-138.tap`.

Genel nakit modu ile kartın açık dahil/hariç tercihlerinin hem bakiye hem tahminde korunması ayrıca dört senaryoyla sınandı.

S07a, oluşturulan takip kaydında taksit satırlarının bulunmasını sınayan dar bir yetenek testidir. Tarih verilmeden oluşturulan takip satırlarında tarihler `null`, durum tarih bekliyor olarak tutulur. Bu testin geçmesi tek başına tarih motorunun doğru olduğunu kanıtlamaz. Bu nedenle yeni D testleri gerçek tarihli planları ayrıca doğrular. Eski kayıtlar otomatik tarihli plana çevrilmez.

Yeni test kapsamı: 1/2/3/6/12 taksit, tam kuruş toplamı, 29/30/31 ve Şubat/artık yıl, yıl geçişi, bankanın ilk ayı sonraya alması, istisnai doğrulanmış ilk tarih, kesim sınırı, birden çok kart/plan, tekil ekstre bağı, kısmi ve tam ödeme, pay sınırları, ödeme geri alma, plan/banka iadesi ve geri alma, iptal, çift borç olmaması, eski yedek biçimleri, bozuk bağlantılar, yeniden yüklemede kalıcılık ve yazma hatasında geri alma.

Tarayıcı testleri gerçek React ekranını mobil boyutlu Chromium'da çalıştırdı. Plan ekleme/ayrıntı, ekstre bağlama, kısmi pay, İşlemler'den ödeme silerek geri alma, banka iadesi/geri alma, iptal, eski kayıtla birlikte yedek alma/geri yükleme ve ikinci anahtar yazma hatasında iki kaydın birlikte geri alınması doğrulandı. Özetin eski hızlı takip formu için ayrıca regresyon testi eklendi. Kullanılan kayıtların tamamı sentetiktir.

## 5. Eski kayıt ve yedek uyumluluğu

Eski tarihli/tarihsiz kart kayıtları, A'nın kredi ödeme bağlantıları, B'nin kalıcı kart/ekstre kimlikleri ve C'nin güvenli geri yükleme sistemi korunur. `finora-v2`, `finora-v1`, `butce-takip-v1` biçimleri yapay uyumluluk örnekleriyle yeniden sınandı. Kullanıcının gerçek tarihli yedeklerinin tamamına erişilmedi; bunların hepsinin açıldığı iddia edilmiyor.

Yeni alanlar isteğe bağlı ve sürümlüdür (`planSurumu:1`). Eski kayıtların tarihleri/tutarları tahmin edilmez, manuel ödemeler isim/tutar benzerliğiyle eşleştirilmez. Önceki kayıtlar açılışta topluca dönüştürülmez. Kişisel kategoriler, rozetler, simgeler ve şablonlar korunur.

Yedek doğrulaması yeni planların kuruş/tarih dağıtımını, kart/ekstre kimliklerini, ödeme payı sınırlarını ve bankaya yansıyan iadenin çift yönlü bağını kontrol eder. Kısmi pakette korunacak yerel bölümler de bağlantı kontrolüne dahil edilir. Eksik işlem listesinin boş liste sayılmaması, yükleme önizlemesi, yükleme öncesi güvenlik kopyası ve yazma hatasında geri alma testleri geçmeye devam eder.

Plan işlemi öncesi `ft_auto_backup_taksit_islem_oncesi_v1` kopyası alınır. İade gibi plan ve kartı birlikte değiştiren işlemler A/C'deki ortak depolama geri alma akışını kullanır. Tarayıcı testi ikinci yazmanın başarısızlığında hem planın hem kartın önceki haline döndüğünü doğruladı. İşletim sisteminin tam yazma ortasında süreci öldürmesi veya geri alma yazmalarının da reddedilmesi için yerel depolamada mutlak atomiklik garantisi yoktur; bu mevcut sınırlama dosya yedeği ihtiyacını kaldırmaz.

## 6. Derleme ve yapılandırma

| Kontrol | Sonuç |
|---|---|
| `npm run build` | Başarılı; Vite 7.3.1 |
| `npx cap sync android` | Başarılı |
| Android `:app:assembleDebug --offline --no-daemon --max-workers=2` | **BUILD SUCCESSFUL** |
| Üç App kopyasının eşitliği ve kapsam kontrolü | Başarılı |
| ESLint | Başarısız: 50 hata, 12 uyarı; önceki B'de 51 hata, 12 uyarı vardı. Tanımsız değişken bulgusu yok. |

Lint başarılı gösterilmemiştir. Mevcut kullanılmayan değişken/React hook ve diğer kapsam dışı lint bulguları genel temizlik amacıyla değiştirilmedi. Vite büyük paket uyarısı devam ediyor; tek App teslim yapısı bu aşamada bölünmedi. Bunlar 207 hesaplama/ekran testinin sonucuna dahil değildir.

Bulutta Android için mevcut tam JDK 21 (`/workspace/jdk21`), SDK (`/workspace/android-sdk`) ve Gradle önbelleği (`/workspace/gradle-cache`) kullanıldı; varsayılan eksik Java derleyicisi yerine bu mevcut JDK seçildi. Yeni paket yüklenmedi, proje yapılandırması değiştirilmedi. Debug APK üretildi; release derlemesi, kişisel imzalama anahtarı ve mağaza yayını kullanılmadı.

Loglar: `asama-d-build.log`, `asama-d-cap-sync.log`, `asama-d-android-build.log`, `asama-d-eslint.json`. Kaynak kapsam ve hash kanıtı `asama-d-kapsam-kaydi.json`; D öncesine göre kaynak farkı `asama-d-kaynak-degisiklikleri.patch`.

## 7. Kullanım ve Android cihazında doğrulanacaklar

Yeni tarihli plan: Menü → **Taksitli Harcamalar → Ekle**. Kartı, toplam tutarı, alışveriş tarihini, bankanın doğruladığı ilk ekstreyi ve gün kurallarını gir. Oluşturulan planı **Ekstre planı / Düzenle** üzerinden açıp kesin banka ekstresine bağla. Kısmi ödeme dağıtılacaksa önce normal kart ödeme akışında gerçek ödemeyi kaydet, ardından ilgili taksite mevcut ödemenin payını ver.

Özetin hızlı taksit ekleme yolu eski takip davranışını korur ve tarihli plan için ilgili menüye yönlendirir. Tarihsiz takip kaydı kendiliğinden yeni banka borcu oluşturmaz. Yeni tarihli planın tarih/tutar sözleşmesi sonradan sessizce değiştirilmez; bu sürüm otomatik yeniden takvimleme veya banka entegrasyonu içermez.

Gerçek Android cihazında henüz çalıştırılmadı. Güncellemeden önce Ayarlar'dan dosya yedeği alınarak şu kontroller yapılmalı:

1. Mevcut verilerle güncelleme, tüm temalarda form/modal ve Türkçe tarih/tutar girişleri.
2. 100 TL / 3 taksit planı; 31 Ocak–Şubat–Mart tarihleri ve sonraki aya ilk ekstre tercihi.
3. Uygulamayı tamamen kapatıp açma ve mümkünse cihazı yeniden başlatma sonrası plan/bağların korunması.
4. Gerçek kart borcuyla planın ikinci borç oluşturmaması; tutar farkının ayrıntıda görünmesi.
5. Kısmi ödeme, ödeme geri alma, bankaya yansıyan iade ve iade geri alma; bakiye ve raporların tutarlılığı.
6. Android dosya seçicisinden yeni/eski yedek alma/geri yükleme, iptal ve düşük depolama davranışı.

Bunlar cihaz doğrulama adımlarıdır; masaüstü testleri yerine getirilmiş sayılmamıştır. Nakit akışı simülatörü, yapay zekâ ve başka yeni özellikler geliştirilmedi.

## 8. Geri dönüş yöntemi

D öncesi A+B+C çalışan kaynağı `/workspace/phase-d-backup/` altında üç ayrı kaynak kopyasıyla korundu. Başlangıç Git durumu, çalışma farkı ve kaynak hash'leri aynı dizindedir. D öncesi SHA-256:

`a5f41129ce3f7f5f47c8ca962f0dd2478d716b5eb9f39d82214ea84355ebf692`

Kaynak geri dönüşünde `App.before-phase-d.jsx` dosyasını projenin `src/App.jsx` yerine koyup web derlemesi, Capacitor senkronizasyonu ve Android derlemesini tekrarla. Git HEAD A/B/C'nin tümünü içermediği için `git reset --hard` kullanılmamalı; önceki çalışan kopya esas alınmalı.

Yeni plan/iade verisi kaydedildiyse yalnız eski kaynak dosyasına dönmenin finansal olarak yeterli olduğu varsayılmamalı. Önce güncel veriyi ayrıca dosyaya yedekle; tam veri geri dönüşü gerekiyorsa yalnız kullanıcı onayıyla güncelleme öncesi tam yedeği önizleyerek yükle. Bu, güncelleme sonrası işlemleri de geri alacağından güncel yedek silinmemeli. Gerçek veri geri yüklemesi bu çalışma sırasında yapılmadı. Ayrıntılar `/workspace/phase-d-backup/GERI_DONUS.md` içindedir.

## 9. Teslim

Teslim dosyası `/workspace/output/App.jsx`; **zip değildir**. Kaydedileceği yer `finans-takip/src/App.jsx`. Raporun teslim kopyası `/workspace/output/ASAMA_D_SONUC_RAPORU.md`.

Teslim App.jsx SHA-256: `980a0eadaa3837c93b6be781c388f32827c97fb52adaac0f4e54025c7990e155`.

İndirilebilir geri dönüş kaynağı: [Aşama D öncesi App.jsx](./App_ASAMA_D_ONCESI.jsx). Bu kopya güncel uygulamanın yerine yalnız geri dönüş gerektiğinde kullanılmalıdır.

Aşama D tamamlandı. Yeni bir aşama için işlem başlatılmadı.
