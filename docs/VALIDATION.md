# Doğrulama · v1.3.0 · 10 Ekim 2026

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
