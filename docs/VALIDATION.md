# Doğrulama · v1.5.0 · 10 Ekim 2026

versionCode6/versionName1.5.0; JDK17, SDK35, Gradle8.9. Bu sürüm arayüz yenilemesidir; finans motoru ve SQLite/JSON şeması değişmedi.

- `testDebugUnitTest`, `lintDebug`, `assembleDebug`: başarılı. 29 JVM testi, 0 başarısız/hatalı/atlanan; Lint 0 hata/uyarı.
- Android11/API30 emülatörde v1.4 üzerine güncelleme: net varlık2.780,00 TL, gelir3.000,00 TL, gider220,00 TL korundu.
- Ana ekranda gider, gelir ve transfer kısayolları işlem formunu açtı; kayıt yapmadan iptal edildi. İşlem ve analiz sekmeleri, açık/koyu tema geçişi kontrol edildi.
- Bütçe göstergesi animasyonlar kapalıyken %7,3 son değerini çizdi. Yeni gösterge, daha önce Android üzerinde test edilmiş `Design.ratioAnimator` motorunu kullanır.
- Son APK iki temada yeniden açıldı; analizde Hesaplama bilgisi panelinin aynı kartta açılıp kapanması kontrol edildi. Gerçek ekran görüntüleri [tasarım dokümanında](DESIGN.md).
- APK imzası doğrulandı; SHA-256 sertifikası `a3c71704fc4e5a498772ad1825325fc493203b962d3b7c3f9f9db5c0dbf273c8`, v1.4 ile aynı. Paket bütünlüğü ve kaynak dosyalarının ZIP ile eşleşmesi ayrıca kontrol edildi.

v1.4'teki 35 Android/SQLite testi ve 100.000 kayıt ölçümü bu görsel değişiklik için tekrar koşturulmadı. Aşağıdaki rapor önceki sürümün sonuçlarını ve kapsamını korur; yeni sürümde yeniden ölçülmüş performans sonucu olarak sunulmaz.

---

# Doğrulama · v1.4.0 · 10 Ekim 2026

JDK17, SDK35/Build-Tools34.0.0, Gradle8.9; Android11/API30 yazılımsal emülatör. versionCode5/versionName1.4.0, minSDK26, targetSDK35.

## Otomatik testler

- JVM: **29 test**, 0 başarısız/hatalı/atlanan. Önceki 19 finans testi ve 10 bileşik enflasyonla maaş/annüite/ondalık doğrulama testi.
- Android SQLite: **35 test**, 0 başarısız/hatalı/atlanan. Son teslim APK’sında `am instrument -w` ile tam koşu: **84.033 saniye**. Önceki connectedDebugAndroidTest koşusunda 33 test geçti; yeni maaş yedek/bozuk yedek testleriyle toplam35 oldu. Testler ayrı `ledger-validation.db` kullanır.
- `lintDebug`: **No issues found**, 0 hata/uyarı; app ve test APK derlemeleri geçti.
- Kredi planı: 31 Ocak→29 Şubat→31 Mart; takip ödemesinde gider/bakiye değişmemesi, gerçek ödemede tek gider, tekrar ödemenin engellenmesi, her iki ödemeyi geri alma, kredi planını kaldırırken gerçek gideri koruma ve yedek dönüşü.
- Maaş ayarlarının yıl/dönem/negatif aylık enflasyonla JSON yedek dönüşü; geçersiz %-100 zam içeren restore’un mevcut kaydı koruyarak reddedilmesi; maaş alanı olmayan eski yedekte varsayılan ayarlar.
- Geçmiş JSON v1/v2/v3 uyumu, eksik taksitli bozuk kredi yedeğini reddetme ve eski kayıtları koruma. JSON v4 yazılır; SQLite v1/v2→v3 migration kayıtları korur.
- Günlük bakiye türetilmiş tablosu: tutar/tarih/transfer yönü düzenleme, ileri tarihli geliri gerçekleşmiş bakiyeye katmama, silme, restore ve FK temizliği. Tekrarlayan işlemi durdururken oluşan FK `SET NULL` güncellemesi de kapsanır.
- Günlük bildirim adayları: ödenen/yalnızca takip ödemelerini dışlama, 0/1 gün öncesi sınırı, eski kart özetini yeni kesim döngüsüne taşımama; yerel kontrol saati seçimi.

- Hazır APK imzası `apksigner verify --verbose --print-certs` ile doğrulandı; sertifika SHA-256 `a3c71704fc4e5a498772ad1825325fc493203b962d3b7c3f9f9db5c0dbf273c8`, önceki yayımlanmış sürümle aynı.

## App.jsx maaş hesabıyla karşılaştırma

Yüklenen dosyadaki `MaasTab`/`useMemo` hesap gövdesi doğrudan Node.js ile çalıştırıldı; aynı girişler Java `Calculators.salary` motoruna verildi. Sabit seed ile oluşturulmuş **300 senaryonun tamamında tahmini maaş kuruş düzeyinde eşleşti**. Ocak/Temmuz, sonraki yılın Ocak zammı, aylık negatif enflasyon ve mevcut dönem zammına göre farkın sıfırlanması kapsandı. JVM testleri ayrıca boş ayları sıfır kabul etme, zam çift uygulamama, bileşik çarpım ve geçersiz %-100 bölenini reddetmeyi doğrular. Referans dosyadaki kayan nokta işlemleri Android'de BigDecimal ile uygulanır; para sonuçları son adımda kuruşa yuvarlanır.

## 100.000 kayıt doğrulaması

Tek hesap/kategori, 100 güne dağıtılmış 100.000 gider; gerçek Android SQLite, yerel test verisi. Raporda **100 özet nesnesi**, SQL sayfasında **50 gerçek işlem**; count100.000, toplam10.000.000 kuruş ve bakiye−10.000.000 kuruş doğrulandı. Dönem sorgusunun index kullanması ayrıca `EXPLAIN QUERY PLAN` ile kontrol edildi.

| Ölçüm | İlk yaklaşım | Son yaklaşım |
|---|---:|---:|
| 100.000 fixture ekleme | 50.425 s | 66.114 s |
| Rapor + ilk sayfa/toplamlar + bakiye sorguları | 16.774 s | 0.542 s |
| Java rapor/sayfa nesnesi | 100 / 50 | 100 / 50 |

Son yaklaşım: covering rapor indeksi, arama yokken gereksiz join'leri kaldırma ve transaction içi trigger'larla günlük bakiye toplamları. Ek yazma işi vardır; bunun karşılığı geçmiş büyürken bakiyenin işlem sayısı yerine hesap/gün toplamlarından okunmasıdır. Fixture ekleme süresi uygulamada tek işlem ekleme süresi değildir. Sonuç TCG yazılımsal emülatör ölçümüdür; fiziksel telefonda süre garantisi değildir. Metin araması seçili dönemi tarayabilir; derin OFFSET ve milyonlarca kayıt ayrıca ölçülmelidir.

## APK güncelleme ve ekran kontrolü

v1.3 APK'da 3.000 TL gelir/100 TL gider oluşturuldu; v1.4 güncellemesinde iki kayıt ve 2.900 TL bakiye korundu. 300 TL anapara, 3×120 TL taksitli kredi eklendi. **Hayır · yalnızca takip** sonrası 1/3 ödendi, iki işlem ve 2.900 TL bakiye korundu. **Evet · gider ekle** sonrası 2/3 ödendi, üç işlem ve 2.780 TL bakiye doğrulandı.

- Maaş: mevcut30.000 TL; Ocak/Şubat enflasyonları %10/%10, mevcut Ocak zammı %10, Temmuz zammı %5 → **34.650 TL**. Kaydet/force-stop/yeniden açma sonrası aynı sonuç. Temmuz–Aralık boş enflasyon + sonraki yıl Ocak zammı %20 → **36.000 TL**. SQL snapshot'ta yıl/dönem/zam ayarları korunmuş, işlem sayısı hâlâ3.
- Harcama haritasının gerçek Canvas gün hücresine dokunuldu: **10.10.2026 · Gün özeti** aynı ekranda takvimin altında; gelir3.000/gider220/net2.780 TL. Detay listesi varsayılan kapalı, açıldıktan sonra gerçek Market işlemi görüldü.
- Konuya özel yardım kartı, günlük harcama payı ve finans araçlarının gezinmesi kontrol edildi.
- Bildirim açıldı; `dumpsys notification` içinde **Cep Bütçem · Bildirim testi**, PRIVATE görünürlük ve publicVersion; `dumpsys alarm` içinde günlük **CHECK_PAYMENTS** alarmı doğrulandı. Yeniden açılışta açık ayar korundu, ardından kapatma kontrol edildi. Bunlar API30 deneme bildirimi/alarm kontrolleridir; gerçek vade saatinde fiziksel telefona teslim denemesi değildir.
- Son SQLite snapshot: schema3, üç işlem, bir kredi, bir takip ödemesi, bir gerçek gider ödemesi; günlük bakiye toplamı278.000 kuruş.

Örnek ekranlar: [Krediler](screenshots/loans.png), [Takvim altında gün özeti](screenshots/day-summary.png), [Açılmış işlem listesi](screenshots/day-details.png), [Maaş hesabı](screenshots/salary.png). Son teslim APK’sı yeniden kuruldu; 2.780 TL bakiye ve kaydedilmiş Temmuz dönemi korundu, genel gelir/gider dönemi seçicisi maaş ekranından kaldırılmış halde 36.000 TL tahmin tekrar doğrulandı.

## Teslim sınırları

Android13+ izin akışı manifest/SDK35 lint ile doğrulandı; runtime ekran testleri API30 üzerindedir. Fiziksel telefon, üretici enerji tasarrufu, gerçek yeniden başlatma ve bildirim teslim saati henüz denenmedi. Alarm inexact olduğundan kesin dakika teslimi vaat edilmez. Bildirim ayarı cihaz bazlıdır. Maaş aracı App.jsx ile aynı enflasyon/zam tahmini, faiz aracı vergi/masraf hariç senaryodur.

Web kaynakları bu sürümde değişmedi; aşağıdaki v1.3 web doğrulamaları referans olarak korunur. Backend örnekleri dağıtılmış servis değildir.

---

# Önceki v1.3 doğrulama kaydı · 10 Ekim 2026

JDK 17, Android SDK35, Build-Tools34.0.0 ve Gradle8.9; Android11/API30 emülatörü. Hazır APK versionCode4, versionName1.3.0, minSDK26/Android8, targetSDK35.

## Android otomatik kontroller

- `testDebugUnitTest`: **19 test geçti**, 0 başarısız/hatalı/atlanan. Para, dönem, tahmin, harcama haritası, bağımsız tekrar taslağı ve ortak renk eşiklerinin sınırları.
- `connectedDebugAndroidTest`: **26 test geçti**, 0 başarısız/atlanan. Gerçek SQLite, ayrı `ledger-validation.db`; kişisel kayıt veritabanı kullanılmadı.
- `lintDebug`: **0 hata, 0 uyarı**. APK ve test APK derlemeleri başarılı.
- Yeni veritabanı kapsamı: kart hesabı/metadata atomic oluşturma, geçersiz tarihte/banka tutarında orphan hesap bırakmayan rollback, üç tutar/güncelleme tarihi yedek dönüşümü, v2 banka özeti olmayan kart uyumu, bozuk banka özeti restore'da mevcut verilerin korunması, kart takibini kaldırırken işlemlerin korunması, hesap silmede iki yönlü transfer/fatura/düzenli kural referanslarının temizlenmesi ve son hesabın silinmesinin engellenmesi.
- Android UI thread animasyon motoru: 0→ara değerler→tam40 geçişi; animasyon kapalıyken150 oranının animator oluşturmadan uygulanması geçti. Etkinlik testi emülatörde animator ölçeği1 ile yapıldı; sonradan0 tercihine geri döndürüldü.
- JSON v3 yazılır; v1/v2 okunur. SQLite şeması v2 devam eder. Önceki kart transferi/fatura ödeme/maaş dönemi testleri tekrar geçti.
- `apksigner verify`: geçerli; önceki yayımlanmış APK'larla aynı sertifika SHA-256 `a3c71704fc4e5a498772ad1825325fc493203b962d3b7c3f9f9db5c0dbf273c8`.

## APK güncelleme ve gerçek ekran akışı

Yayımlanmış v1.2 APK'da 1.000 TL gelir ve 500 TL gider oluşturuldu. Yeni APK güncelleme olarak kuruldu; **500 TL bakiye** ve iki işlem korundu; %50 için **DİKKAT/sarı** durum gösterildi.

- Özet'ten yeni Bonus kartı eklenerek ayrı hesap oluşturuldu. Ekstre1.200 TL, dönem içi350 TL, provizyon75 TL kaydedildi. Kartın üç kutusu, **1.625 TL** toplamı ve güncelleme tarihi doğrulandı.
- Kart yönetiminden provizyon90 TL yapıldı; toplam **1.640 TL** oldu. Banka özeti gider/gelir yaratmadı; kayıtlı net bakiye değişmedi.
- Kart takibi silme önce iptal edildi ve kart korundu; sonra onaylandı. SQLite kontrolünde kart metadata'sı kaldırılırken **Bonus finansal hesabı korundu**.
- Deneme hesabı eklendi; silme onayında 0 işlem gösterildi. İptal sonrası hesap vardı; onay sonrası kaldırıldı. Sonrasında ana ekran 500 TL ve sarı durum gösterdi.
- Son paket APK yeniden kuruldu ve açıldı; **1.200 TL** kayıtlı bakiye, dört işlem ve **%40** yeşil gösterge korundu.
- 300 TL ek gider sonrası gider800/gelir1000 = **%80, kırmızı**. 1.000 TL ek gelir sonrası gider800/gelir2000 = **%40, yeşil**. Genel kart ve dönem kartı renkleri görsel kontrol edildi.
- Tüm ekran otomasyonu bir betikte tamamlanmış gibi gösterilmedi: kart takibi kaldırıldıktan sonraki kontrolde yatay hesap listesinde ekran dışında kalan Bonus araması başarısız oldu. Hesabın korunduğu gerçek SQLite'dan doğrulandı; kalan hesap/renk kontrolleri ayrı devam betiğinde geçti. Bu, uygulama hatası veya yeniden çalıştırılmamış adımların geçmiş sayılması değildir.

## Web referansı / mimari doğrulaması

Node24; React19, Tailwind4, Vite7.3.7; `npm ci`, **6 hesaplama testi**, `npm run build` başarılı. `npm audit` anlık kontrolünde 0 bildirilen açık. Eski Vite geliştirme aracı uyarısı düzeltilerek güncel7.3.7 sürümü ve lockfile sabitlendi.

Chromium'da 390×844 mobil ve1280×900 masaüstü: form açma, gelir/gider ekleme, %80 durum, geçersiz ondalık tutar uyarısı, Escape ile dialog kapama, tema değiştirme, yatay taşma olmaması ve JS runtime hatası olmaması geçti. Ayrı tarayıcı kontrolünde yüzde0, ara değerler ve nihai80 doğrulandı; azaltılmış hareket tercihindeki akış da kullanıldı. Bunlar bir backend entegrasyonu değil yerel UI referans testidir.

PostgreSQL SQL şeması/raporları PGlite PostgreSQL motorunda yürütüldü: tablo kurulumu, para birimi FK, kullanıcılar arası hesap/kategori/kart snapshot FK reddi, yinelenen istek anahtarı, pozitif işlem/bütçe tutarı, gelir/gider toplamı ve kategori bütçe aşımı kontrol edildi. `controllers.mjs` Node syntax check geçti. Gerçek uzak PostgreSQL, JWT/refresh/e-posta uçtan uca akışı, TLS ve production dağıtım bu teslimde çalıştırılmadı. Bu noktalar mimari rehberin sprint/çıkış koşullarındadır.

## Teslim sınırları ve yeniden kullanım

Son APK temiz kurulumda **GELİR BEKLENİYOR / 0,00 TL** gösterdi. Test sonrası yalnızca kendi emülatörümüzün uygulama verileri temizlendi ve emülatör durduruldu.

Ekran görüntüleri yalnızca emülatör/tarayıcı test verileridir; APK boş kişisel verilerle başlar. Fiziksel telefon doğrulaması yapılmadı; minimumAndroid8 statik, çalışma Android11'de doğrulandı. APK kişisel kullanım için debug imzalıdır; production release keystore ayrı yönetilir.

Bulut kurulum taslağında mevcut Android yönergeleri korunup `install_script` ve `start_skill` alanlarına isteğe bağlı web referansı kurulum/test/build ve server başlatma/durdurma adımları eklendi. Taslak kaydı ortam snapshot'ını yayımlamaz; yeni göreve restore ayrıca denenmedi. Yeni çalışma zamanı sırrı veya banka bağlantısı eklenmedi.
