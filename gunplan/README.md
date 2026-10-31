[Gün Plan 1.2.0 APK indir](../downloads/GunPlan-1.2.0.apk)

**1.2.0 ay sonu:** Görev düzenleme → Tekrar → Her ay → Aylık gün seçimi → **Ayın son günü**. Ayın gerçek son günü 28/29/30/31 olarak bulunur. **“Ayın son günü hafta sonuysa önceki son hafta içi gününde hatırlat”** açıksa cumartesi/pazar sonu önceki cumaya alınır; saat korunur. Kapalıysa gerçek son gün kullanılır. Resmî tatiller hesaplanmaz. Belirli gün modundaki sonraki pazartesi seçeneği ayrı olarak korunur. Eski görevler kendiliğinden ay sonu moduna çevrilmez. Yeni ay sonu modu yedek ve takvimde saklanır.

**1.1.0 kullanıyorsanız:** 1.2.0 aynı paket/imza ve daha yüksek sürüm koduyla üzerine kurulur; uygulamayı silmeyin, görevler korunur. Güncelleme öncesi yedek alabilirsiniz.

**Orijinal 1.0.0'dan kurulum:** Bu APK `tr.gunplan.app.v2` paketiyle ayrı kurulur. Eski GünPlan'ın yayın anahtarı bu ortamda bulunmadığı için orijinal `tr.gunplan.app` üstüne güncelleme olarak sunulmaz. Eski uygulamayı silmeyin. Eski uygulamada Ayarlar → Yedeği dışa aktar; yeni Gün Plan'da Ayarlar → Yedekten ekle. Görevleri ve bildirim izinlerini kontrol ettikten sonra çift bildirim olmaması için eski uygulamadaki görevleri duraklatın. Veriler uygulamalar arasında otomatik paylaşılmaz.

# Gün Plan

İnternetsiz çalışan Android görev planlayıcısı. Paket: `tr.gunplan.app`, sürüm: 1.2.0. Android 8+ ve Android System WebView / Chrome 80+ gerektirir.

- Tek seferlik, günlük, haftanın seçili günleri veya aylık görevler.
- Aylık görevde isteğe bağlı **“Hafta sonuna denk gelirse sonraki ilk hafta içi gününe taşı”**. Cumartesi/pazar pazartesiye gider, saat korunur. Kısa ayda önce son gün bulunur, sonra hafta sonu kontrol edilir; sonraki aya/yıla geçebilir. Örneğin 31 Ocak cumartesi → 2 Şubat pazartesi; sonraki temel gün yine 31 olarak kalır. Resmî tatiller hesaba katılmaz. Eski görevlerde seçenek kapalıdır. Takvim, bildirim hesabı ve yedek aynı seçeneği kullanır.
- Ayın 29–31'i kısa aylarda son güne alınır; sonraki aylarda asıl gün korunur.
- Android AlarmManager ile uygulama arayüzü kapalıyken bildirim; bildirimden tamamlama / 10 dakika erteleme.
- Açık, koyu ve sistem teması; gün görünümü, aylık takvim, arama, kategoriler, önem işareti.
- Düzenleme, duraklatma, silme, son 300 tamamlanma kaydı; JSON yedek dışa aktarma ve mevcut kayıtları koruyarak içe ekleme.
- Veriler cihazda saklanır. INTERNET izni, sunucu, hesap ve reklam yoktur. Uygulama simgesi özel vektör çizimidir.

## Telefona kurulum

APK'yı indirip açın; gerekirse indirmeyi yaptığınız tarayıcı için APK yükleme izni verin. Gün Plan'da bildirim iznini ve Android 12+ için “Zamanında hatırlatma” iznini açın. Ayarlar'daki deneme bildirimiyle kontrol edin. Bazı telefonlarda pil / otomatik başlatma kısıtlamalarını ayrıca düzenlemek gerekir.

Android ayarlarından “Zorla durdur” uygulanırsa uygulama tekrar açılana kadar alarmlar çalışmaz. Telefon kapalıyken bildirim verilemez. Yeniden başlatınca aktif görevler kurulup atlanan tekrarlar tek bildirimle yakalanır. Kesin alarm izni kapalıysa Android gecikmeli teslim edebilir. Bildirim ve kanal kapalıysa görev tüketilmez; uygulama açıldığında izin durumu tekrar kontrol edilir.

Aylık takvim gelecekteki tekrarları da gösterir. Öngörülen sonraki tekrarlar ayrı ayrı önceden tamamlanmaz; görev düzenlenebilir. Önceki bildirimden tamamlamak, sonraki tekrarı atlamaz. Kaydedilen saatin anlamı cihazın yerel saatidir; saat dilimi ve saat değişikliklerinde plan yeniden değerlendirilir.

Uygulamayı silmeden önce yedek alın. Yedek dosyaları düz JSON'dur; paylaşmadan önce içeriğin kişisel verilerinizi içerdiğini unutmayın. İçe aktarma aynı tanımdaki görevleri çoğaltmaz; eski tek seferlik görevleri yeniden bildirmez. Bozuk yedek işleminde kayıtlar değiştirilmez.

## Derleme

JDK 17, Android SDK platform 35 / Build Tools 35.0.0 ve Gradle wrapper 8.9 kullanılır. `local.properties` içine yerel `sdk.dir` yolunu yazın (bu dosyayı paylaşmayın). Gerekirse `ANDROID_USER_HOME` ve `GRADLE_USER_HOME` yazılabilir dizinlere ayarlanır.

```
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintRelease
```

Release için dışarıda tutulan `GUNPLAN_SIGNING_PROPERTIES` dosyası `storeFile`, `storePassword`, `keyAlias`, `keyPassword` alanlarını içerir. Ardından `./gradlew :app:assembleRelease` çalıştırın. Yayın imza anahtarı ve parolası depoya/ZIP'e dahil edilmez. Güncellemeler mevcut kurulumla aynı imzayı kullanmalıdır.

## Testler

- `tests/RecurrenceTest.java`: saf Java tarih/tekrar kontrolleri.
- `tests/StoreTest.java` + `tests/support`: gerçek TaskStore / ReminderReceiver / Backup kodunun Android saklama ve teslimat taklitleriyle durum testleri. `org.json:json:20240303` test bağımlılığıdır; APK'ya eklenmez. Bunlar işletim sistemi alarm teslimi testi değildir.
- `tests/browser.cjs`: Playwright + Chromium; assets dizinini localhost:8767 üzerinden sunun. Köprü taklidiyle arayüz testleri ve yalnız test verileriyle ekran görüntüleri üretir. Örnek görevler APK'ya dahil edilmez.
- `app/src/androidTest`: ayrı test APK'sı, gerçek cihaz/emülatörde izole test kurulumu içindir; uygulama deposuna test görevi ekler. Kullanıcının günlük kayıtlarının olduğu cihazda çalıştırılmamalıdır.

Güvenlik: yalnız paket içindeki assets dosyaları WebView'da açılır; dış sayfalar engellenir. CSP, dosya/içerik erişimi kapatma, HTML kaçışlama, native doğrulama ve atomik kayıt uygulanır. Otomatik bulut yedeği kapalıdır; kullanıcı isterse dosya seçiciyle kendisi dışa aktarır.


1.1.0 yayımlanan ayrı paket için `GUNPLAN_SIGNING_PROPERTIES=/özel/release.properties ./gradlew :app:assembleStandalone :app:lintStandalone` kullanılır. Orijinal paket için `release` varyantı ve orijinal imza gerekir. Yeni ayrı paketin sonraki güncellemelerinde aynı yeni imza anahtarı korunmalıdır.
