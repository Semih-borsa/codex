# HavaPusulası 1.0

Kişisel ve ticari olmayan kullanım için Android hava asistanı. Hesap, ücretli AI, sosyal özellik veya harita içermez. Telefonun GPS konumunu istemez. Android 8.0+ desteklenir. Ayrı uygulamadır; NOVA ve Cep Bütçem kayıtlarını kullanmaz/değiştirmez.

## APK'yı indir ve kur

1. [HavaPusulasi-1.0.0.apk](https://github.com/Semih-borsa/codex/blob/main/downloads/HavaPusulasi-1.0.0.apk) sayfasını aç. Depo özelse GitHub'a giriş yap.
2. **Download raw file** / aşağı ok simgesine bas. Tarayıcıdaki “Farklı kaydet” APK yerine sayfanın HTML'sini kaydedebilir.
3. İnen `.apk` dosyasını telefona aktar; Dosyalar uygulamasından aç. Android isterse bu dosyayı açtığın uygulamaya kurulum iznini kendin ver. HavaPusulası bu ayarı değiştiremez.
4. Şehirler → İstanbul/Ankara/İzmir kısayolu veya **Şehir ara** → doğru şehir/ilçe ve ülkeyi seç. En fazla 12 şehir kaydedilir.
5. **Tahmini yenile** ile ilk veriyi indir. İnternet olmadan sıfırdan hava tahmini üretilemez. Son başarılı indirmeden sonra uygulama çevrimdışı açılabilir.
6. Ayarlar → **Günlük bildirim** → saat seç. Android 13+ bildirim iznini ver. Android 12+ kesin zamanlama isteniyorsa ilgili düğmeyle izin ekranını açıp kendin seç. İzin yoksa yaklaşık alarm kullanılır.
7. Denemek için **Örnek bildirimi göster**. Bildirimde tüm kayıtlı şehirler ve verinin yaşı gösterilir. İnternetten yenileme yapmak yerine kayıtlı veriyi kullanır.

Güncellemelerde uygulamayı kaldırmadan APK'yı kur; kaldırma yerel şehir ve önbelleği siler. İlk paket kişisel kullanım için debug imzalıdır; Play Store üretim yayını değildir. Farklı bilgisayardaki debug anahtarıyla üretilen APK mevcut APK'nın üstüne kurulmayabilir; bu sürümde dışa aktarma/aktar içe alma yoktur.

## Neler çalışır?

- Çoklu şehir; şehirlerin kendi saat diliminde sonraki 24 saat ve 7 günlük bakış.
- Saatlik sıcaklık, hissedilen, yağış olasılığı, rüzgâr ve nem. Birimler °C, km/sa. Değer yoksa `—`; boş veriden sıfır üretilmez.
- Bu saatin **model tahmini** gösterilir; gerçek zamanlı ölçüm denmez. “Kaydedilmiş tahmin”, indirilme zamanı ve şehir saat dilimi görünür. 6 saatten eski kayıt işaretlenir; tahmin süresi dolduğunda eski sıcaklık güncelmiş gibi sunulmaz.
- Başarısız, bozuk veya hatalı birimdeki yanıt son başarılı önbelleği değiştirmez. Çevrimdışıyken yeniden açma ve saatlik/günlük liste çalışır. Şehir araması ve yeni tahmin indirme internet gerektirir.
- Telefonun yerel saatine göre günlük bildirim. Yeniden başlatma ve saat/saat dilimi değişikliğinde **gelecekteki** bildirim yeniden planlanır. Telefon kapalıyken kaçırılan günlük uyarı sonradan gönderilmez. Aynı gün yinelenen teslim engellenir.
- Ana ekrana eklenen, yeniden boyutlandırılabilen widget: her widget için ayrı şehir veya uygulamada seçili şehri izleme; sıcaklık, yağış ve kayıt yaşı. **Aç / Yenile / Şehir** düğmeleri.
- Sistem/açık/koyu tema; sıcaklık/yağış/rüzgâr eşiklerine dayalı basit günlük öneriler. AI veya resmî tehlike uyarısı değildir.
- Şehir silme ancak şehir ve önbelleğinin silineceği ayrıca onaylanırsa yapılır.

Bildirim teslimi Android Doze, pil tasarrufu, Xiaomi otomatik başlatma/pil kısıtları, uygulamanın zorla durdurulması ve izinlerden etkilenir. Kesin alarm izni bile mutlak teslim garantisi değildir. Sistem ayarları kendiliğinden değiştirilmez. Kilit ekranı içeriği özel işaretlenir. Bildirim sesi kanal/telefon ayarlarına bağlıdır; sessiz mod aşılmaz.

## Ağ, maliyet ve kullanım koşulları

API anahtarı ve sunucu yoktur. Yalnızca aşağıdaki HTTPS servislerine istek yapılır:

- `geocoding-api.open-meteo.com/v1/search`: yazılan şehir sorgusu; GeoNames şehir verisi.
- `api.open-meteo.com/v1/forecast`: seçilen şehrin koordinatları; 7 günlük saatlik/günlük model tahmini.

GPS, rehber, ses kaydı veya kullanıcı hesabı gönderilmez. Sağlayıcı normal HTTP isteğinde IP adresini görebilir. Şehirler/JSON önbelleği uygulamanın özel SQLite alanındadır; ayrıca şifrelenmez. Android bulut yedeklemesi ve cihaz aktarımı kapalıdır. API anahtarı APK'ya, kaynaklara veya örnek ayara gömülmemiştir; bu serviste gerekli değildir.

Open-Meteo ücretsiz uç noktaları **ticari olmayan** kullanım içindir. 18.10.2026 kontrolünde resmî [koşullar](https://open-meteo.com/en/terms) / [resmî koşul kaynağı](https://github.com/open-meteo/open-meteo-website/blob/main/src/routes/en/terms/+page.svelte), dakika başına 600, saat başına 5.000, gün başına 10.000 altında çağrı; karşılaştırma tablosunda ayda 300.000 sınırı belirtir. Kota, erişim ve koşullar değişebilir; 429 durumunda uygulama yeniden denemeyi zorlamaz. Kişisel, reklamsız bu paket için ücretli plana geçiş veya otomatik ücretlendirme yoktur. İnternet operatörü ücretleri sana aittir. Ticari/reklamlı/abonelikli ürüne dönüşmeden önce koşullar yeniden değerlendirilmelidir.

Hava verisi: **Open-Meteo**, şehir: **GeoNames**, [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Veriler yuvarlanıp Türkçe etiketlenir; sağlayıcı onayı ima edilmez. Kaynaklar uygulamada da görünür. Açık kaynak API sunucusunun AGPL lisanslı olması bu Android istemcisine sunucu kodu eklendiği anlamına gelmez; sunucu kaynakları pakete alınmamıştır.

Arka plan yenileme varsayılan **kapalıdır**. Açılırsa **30 dakika / 1 saat / 3 saat** arasından aralık seçilir; başlangıç tercihi 30 dakikadır. **Yalnızca Wi-Fi / ölçülmeyen ağ** varsayılan açıktır. Bu seçenek kapatılırsa mobil veri de kullanılabilir; operatör tarifesi geçerlidir. Android'in ağ sınıflandırması esas alınır.

Her döngüde en fazla 12 kayıtlı şehir yenilenir; widget'ta görünen ve uygulamada seçili şehirler önceliklidir. İki ardışık hatada döngü durur, sonraki plan beklenir. Android Doze/pil yönetimi/iş kotası nedeniyle **tam 30 dakikada bir, kesintisiz güncelleme veya bildirim anında taze veri garantisi yoktur**. Ön plan servisi veya sürekli uyanık tutma kullanılmaz. Elle uygulama/widget üzerinden yenileme mevcut internet bağlantısını kullanır.

## Ana ekran widget'ı

1. Uygulamada şehir ekleyip ilk tahmini indir.
2. Ayarlar → **Ana ekrana widget ekle**. Başlatıcı desteklemiyorsa telefonun ana ekranında boş yere basılı tut → **Widget'lar → HavaPusulası**.
3. Şehir seçimi açılırsa bir şehir veya **Uygulamada seçili şehri izle** seç. Widget üzerindeki **Şehir** düğmesiyle daha sonra değiştirebilirsin. Birden çok widget farklı şehirleri gösterebilir.
4. **Aç**, ilgili şehrin uygulama ekranını açar. **Yenile**, yalnızca o şehir için anahtarsız bir indirme işi ister; internet yoksa istek bağlantıyı bekleyebilir. Eski tahminin yaşı bu sırada görünür kalır.
5. Widget'ın sistemden gelen yaklaşık yarım saatlik ekran güncellemesi kayıtlı veriyi yeniden gösterir. Otomatik yeni veri indirmek için uygulama ayarındaki arka plan yenilemeyi ayrıca aç. Widget eklemek bu tercihi kendiliğinden açmaz.

Süresi dolmuş veride sıcaklık `—` olur; eski değer güncelmiş gibi kalmaz. Widget boyutu ana ekran başlatıcısından ayarlanabilir; küçük boyutta ayrıntılar sadeleşir, kayıt yaşı korunur. Widget kaldırıldığında o widget'ın tercihleri temizlenir; şehirler uygulamada saklanır.

## Android Studio ile aç

[HavaPusulasi-Android-Studio.zip](https://github.com/Semih-borsa/codex/blob/main/downloads/HavaPusulasi-Android-Studio.zip) dosyasını **Download raw file** ile indir ve ayıkla. Android Studio → **Open** → ZIP içindeki **HavaPusulasi** klasörü. `settings.gradle`, `build.gradle`, `gradlew` ve `app` aynı kökte olmalı.

Doğrulanan araç takımı:

| Bileşen | Sürüm |
|---|---|
| Gradle JVM | Eclipse Temurin JDK 21.0.12+8 (tam JDK; JRE yeterli değil) |
| Gradle Wrapper | 8.9; SHA-256 wrapper ayarında sabit |
| Android Gradle Plugin | 8.7.3 |
| Android platformu | compileSdk / targetSdk 35 |
| En düşük Android | API 26 / Android 8.0 |
| Java kaynak/hedef dili | 17 (JDK 21 üzerinde derlenir) |

NDK, CMake, Flutter, Node ve API anahtarı gerekmez. Android Studio'da Settings → Build Tools → Gradle → **Gradle JDK: JDK 21** seç. SDK Manager'dan Android 35 platformu ve önerilen SDK Build Tools'u kur. İlk senkronizasyon Gradle/Google Maven/Maven Central'a bağlanır. Tam JDK'da `java`, `javac`, `jlink` ve `jmods` bulunmalı.

### Debug APK üretme

Linux / macOS:

```bash
java -version
./gradlew --version
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Windows PowerShell:

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

Fiziksel Android veya emülatörde geliştirici seçenekleri/USB hata ayıklamayı kendin aç; ardından:

```bash
adb devices
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w com.semih.havapusulasi.test/com.semih.havapusulasi.WeatherInstrumentation
```

`SERIAL` yerine `adb devices` çıktısındaki cihazı yaz. Standart testler ayrı veritabanları kullanır; test dosyalarını otomatik silmez. Test kaynakları yalnızca test APK'sındadır; kullanıcı APK'sına örnek hava verisi eklenmez. [Temel test sonuçları ve sınırlar](docs/TEST-SONUCLARI.md).

## Teslim içeriği

- Eksiksiz Java kaynakları, `AndroidManifest.xml`, simge ve temalar.
- Gradle yapılandırması ve Wrapper JAR/betikleri.
- `config/settings.example.json`: anahtarsız örnek ayarlar; otomatik içe aktarma değildir.
- Bu Türkçe yönerge, mimari notlar, test sonuçları ve test kaynakları.
- ZIP kökünde ayrıca hazır debug APK ve SHA-256 dosyası. `local.properties`, `.gradle`, derleme önbellekleri, imza anahtarları ve kişisel veriler pakete alınmaz.

## Doğruluk ekranı: sonraki sürüm

İlk sürümde gerçek gözlemlerle karşılaştırılmış doğruluk yüzdesi gösterilmez. [Mimari ve ölçüm planı](docs/MIMARI.md), tahminin önceden sabitlenmesi, güvenilir gözlem kaynağı ve sızıntısız değerlendirme akışını tanımlar. Önce temel sürümün gerçek Xiaomi cihazında bildirim ve canlı servis doğrulaması tamamlanmalıdır.
