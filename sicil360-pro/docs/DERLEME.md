# Derleme Ve Teknik Seçimler

Android yalnız ilk platformdur. Kotlin/Jetpack Compose değerlendirildi; bu sürümde depodaki hazır ve test edilmiş JDK17/AGP8.7.3 hattıyla **Java 17 + Android yerel bileşenleri** seçildi. Amaç çevrimdışı çalışma, az bağımlılık, küçük APK ve mevcut derleme zincirinde doğrulanabilir teslimdir. Kotlin/Compose zorunlu kabul edilmedi. Windows sürümü bu teslimatta yoktur.

İş kuralları Android'den bağımsız `Model`, `Codec`, `Backup` ve `Excel` sınıflarındadır. Android UI, Keystore ve dosya seçici ayrı tutulur. İkinci platform eklenirse bu test edilmiş kurallar referans alınır; bugün platformlar arasında çalışan bir ortak UI/uygulama olduğu iddia edilmez.

- Gradle 8.9 (wrapper), Android Gradle Plugin 8.7.3, Java 17.
- compileSdk/targetSdk 35, minSdk 26, build-tools 35.0.0.
- Çalışma zamanında üçüncü taraf paket yok; birim testlerinde JUnit 4.13.2.
- XLSX: `ZipFile` + Android/JVM SAX. Harici Excel programı gerekmez. Formül çalıştırılmaz, DTD/harici entity reddedilir. Ortak basit sıfır sayı maskeleri desteklenir; Excel'in tüm görüntüleme biçimlerinin kopyası değildir.
- Kalıcılık: Android Keystore AES-GCM + `AtomicFile`. Sürüm karşılaştırması eski veriyle yeni dosyanın üzerine yazmayı önler; kayıtlar toplu ve atomik değiştirilir. Veritabanı/arama bellekte yüklenir; dosya yükleme, arama ve ağır kodlama/şifreleme işleri tek arka plan yürütücüsündedir. Room yerine bu sürümde sınırlı, sürümlü şifreli dosya seçildi.
- UI: Petrol & Mint açık tema / Grafit & Zümrüt koyu tema kartları, dar ekrana göre ağırlıklı alt menü, açık/koyu/sistem teması. Özgün Canvas simgeleri; dış görsel veya CDN yok.

## Android Studio

`Sicil360Pro-Android-Studio.zip` dosyasını açın; `sicil360-pro` klasörünü Android Studio'da açın. JDK17 ve Android SDK35 kurulu olsun. Kendi SDK yolunuz için Android Studio'nun `local.properties` üretmesine izin verin; makineye özel dosya kaynak paketinde yoktur.

```sh
./gradlew testDebugUnitTest lintDebug assembleRelease assembleDebugAndroidTest
```

Teslim APK: `app/build/outputs/apk/release/app-release.apk`. Yerel hızlı geliştirme için `assembleDebug` da kullanılabilir.

Emülatör/telefon üzerindeki **sentetik test** APK'sı, kişisel veri kullanmadan ve gerçek mesaj göndermeden çalışır:

```sh
adb install -r app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.semih.sicil360.test/com.semih.sicil360.SmokeRunner
```

Testler `cache/validation/isolated` içinde kendi şifreli veri dosyasını kullanır. Tema/kilit tercihlerini geçici değiştirip geri yükler; kişisel rehber dosyasına kayıt yapmaz. Test önizlemeleri `FLAG_SECURE` nedeniyle sistem ekran görüntüsü yerine yalnız sentetik View çizimiyle üretilir. Bu sentetik önizlemeler test çalıştırıldığında `Android/data/com.semih.sicil360/files/synthetic-previews` klasörüne de yazılır.

Dağıtılan APK debugger erişimi kapalı release varyantıdır; bu kişisel dağıtımda geliştirme sertifikasıyla imzalanır (`release.signingConfig = signingConfigs.debug`). Play Store yayını yapılmadı. Üretim dağıtımında sahibi tarafından güvenle saklanan özel release anahtarı ve uygun imzalama süreci gerekir. Keystore dosyası/parolası bu depoda bulunmaz.
