# İlacım — Android ilaç dolabı

Android 8/API 26 ve üzeri için Türkçe, çevrimdışı ilaç ve randevu takip uygulaması. Cep Bütçem ile aynı depoda ayrı bir Android projesidir.

## Kullanım

- **Bugün:** günlük dozları saat sırasıyla gör; Aldım stoğu azaltır, Atladım değiştirmez. Kaydı geri almak stoğu geri ekler. Aynı doz iki kere kaydedilemez.
- **İlaçlarım:** kamera ile EAN/UPC/GTIN veya GS1 ilaç karekodu tara; elle barkod ve ilaç bilgisi de girilebilir. GTIN kontrol basamağı doğrulanır. Karekoddan ürün kodu alınır; kutunun son kullanma tarihi kullanıcı tarafından doğrulanıp girilir.
- **Kutu takibi:** her kutu ayrı son kullanma tarihi, miktar, birim, düşük stok eşiği, kullanıcı ve saklama yeri taşır. `12/2026`, 31 Aralık 2026 sonuna kadar geçerli kabul edilir. Süresi dolmuş kutuda Aldım kaydı engellenir; atlanan doz kaydı yapılabilir.
- **Planlar:** reçetedeki miktarı ve `08:00, 20:00` gibi saatleri, başlangıç/bitiş tarihini ve talimatı gir. Miktarlar üç ondalık basamağa kadar desteklenir; `0,5 tablet` veya `2,5 ml` mümkündür. Aynı kutunun çakışan saatleri engellenir. Bir kutuyu arşivlemek planlarını durdurur, geçmişini korur.
- **Randevular:** doktor, branş, klinik, tarih/saat ve hazırlık/sonuç notları. Yaklaşık bir saat önce yerel hatırlatma; tamamlandı işaretleme, düzenleme ve silme.
- **Ayarlar:** bildirim izni, deneme bildirimi, hatırlatmaları aç/kapat, API ayarları, JSON yedek ve geri yükleme. Yedek geri yüklenmeden önce doğrulanır; onay sonrası mevcut kayıtların yerini alır. API anahtarı yedeğe dahil edilmez.

## API bağlantısı

Henüz belirli bir ilaç bilgi sağlayıcısı seçilmedi. Manuel ekleme ve barkod tarama servis olmadan çalışır. Otomatik ürün/kategori doldurma için Türkiye'deki ilaç barkodlarını kapsayan gerçek bir API gerekir.

Uygulamadaki **Ayarlar → API bağlantısını yapılandır** bölümünde:

1. Sağlayıcının HTTPS GET URL'sine `{barcode}` yer tutucusu ekle.
2. Anahtar başlığını (örn. `Authorization` veya `X-API-Key`), önekini (örn. `Bearer` veya boş) ve anahtarı gir.
3. JSON yanıtının ilaç adı, kategori, güç ve form yollarını tanımla; `data.product.name` gibi noktalı nesne yolları desteklenir.
4. İlaç eklerken barkodu tara ve **Barkoddan API bilgilerini getir** düğmesine bas. Dönen bilgileri kutuyla karşılaştırıp kaydet.

Örnek beklenen JSON nesnesi:

```json
{"name":"Ürün adı","category":"Sağlayıcının kategorisi","strength":"500 mg","form":"Tablet"}
```

Kategori dönmezse kategori uydurulmaz; elle girilir. Dizi yanıtları, POST/OAuth akışları veya farklı GS1 sorgu biçimleri ayrı sağlayıcı adaptörü gerektirir. Anahtar Android Keystore AES-GCM ile şifrelenir. TLS kontrolü açık, düz HTTP kapalı; yönlendirmeler anahtarın başka adrese gitmesini önlemek için izlenmez. Gerçek API entegrasyonu, sağlayıcı seçilmeden doğrulanmış sayılmaz.

## Gizlilik ve hatırlatmalar

SQLite kayıtları cihazda tutulur. Otomatik sistem yedekleme/cihaz aktarımı kapalıdır; kullanıcı JSON yedeğiyle taşıyabilir. Dosya sağlık bilgisi içerir ve şifreli değildir. Uygulama kaldırılmadan önce yedek al. API anahtarı her cihazda yeniden girilmelidir. Bildirimlerde ilaç adı ve sağlık ayrıntıları yer almaz.

Hatırlatmalar `AlarmManager.setAndAllowWhileIdle` ile yaklaşık zamanda çalışır; kesin dakika garantisi yoktur. Android 13+ bildirim izni gerekir. Cihaz yeniden başlayınca ve saat dilimi değişince yeniden planlanır. Zorla durdurma/pil tasarrufu teslimi etkileyebilir. Uygulama doz, telafi dozu, tanı veya ilaç etkileşimi önermez; doktorun talimatını kayıt altına alır. Planlar her gün tekrar eder; gerektiğinde kullanım veya haftanın belirli günleri bu sürümde otomatik planlanmaz.

## Android Studio ile açma

ZIP paketini ayıkla; `settings.gradle`, `gradlew` ve `app` içeren **Ilacim** klasörünü Android Studio’da aç. Gradle JDK 17, SDK Platform 35 ve Build-Tools 34.0.0 seç. Gradle senkronizasyonundan sonra Run ile Android 8+ cihazda çalıştır. Windows terminalinde `gradlew.bat assembleDebug` kullanılabilir. APK oluşturmak için Build → Generate App Bundles or APKs → Generate APKs seç.

## Derleme

JDK 17, Android SDK 35, Build-Tools 34.0.0, Gradle 8.9 / AGP 8.7.3. Barkod çözümleme: ZXing core 3.5.3. Gradle wrapper SHA-256 doğrulaması aktiftir.

```sh
source /workspace/setup/activate.sh
cd /workspace/codex/ilacim
bash ./gradlew --no-daemon --max-workers=4 testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Debug imzalı deneme sürümüdür; üretim mağaza sürümü değildir. Güncellemede aynı imza anahtarı kullanılmalıdır.

Android testleri:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.semih.ilacim.test/com.semih.ilacim.ReadinessInstrumentation
```

Testler ayrı `readiness-test.db` ve ayrı test API tercihlerinde çalışır. JSON stok/doz miktarları kesin hesaplama için birimin 1/1000'i olarak tutulur; dosyayı elle değiştirmek önerilmez.
