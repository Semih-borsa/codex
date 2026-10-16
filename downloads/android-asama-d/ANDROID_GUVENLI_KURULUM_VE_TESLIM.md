# Android cihaz doğrulaması — güvenli kurulum ve teslim

16 Ekim 2026. Yeni finans özelliği veya genel kod temizliği yapılmadı. A/B/C/D sonrasındaki App.jsx değiştirilmedi. Gerçek telefon verisine erişilmedi, telefona uygulama kurulmadı ve mevcut uygulama kaldırılmadı. Cihaz doğrulaması henüz tamamlanmış değildir.

## İndirilecek dosyalar

- [Güncel App.jsx](./App.jsx) — yerel projenin `finans-takip/src/App.jsx` dosyasıdır; zip değildir.
- **[Önerilen bağımsız test APK'sı](./Finans-D-bagimsiz-test.apk)** — telefonda adı **Finans Test D**, kimliği `com.butcetakip.app.devtest`.
- [Önceki başarıyla derlenmiş, aynı kimlikli debug APK](./Finans-D-debug-ayni-kimlik.apk) — `com.butcetakip.app`. Mevcut sürümün üzerine kurulması önerilmiyor; imza/versionCode doğrulanmadı.
- [Adım adım cihaz test listesi ve bakiye tabloları](./ANDROID_TEST_KONTROL_LISTESI.md).
- [62 ESLint bulgusunun öncelik raporu](./ESLINT_ONCELIK_RAPORU.md).

APK'yı indirip kurmak için bilgisayarda npm/Capacitor komutu çalıştırman gerekmez. Kaynakla kendin derleyeceksen mevcut `npm run build` ve `npx cap sync android` akışın sürer; burada hazırlanan bağımsız kimlik üretim projesine uygulanmadı.

## Dosya bilgileri

### Önceki aynı kimlikli debug APK

- Gerçek derleme yolu: `/workspace/finans-upload/finans-takip/android/app/build/outputs/apk/debug/app-debug.apk`.
- İndirilebilir teslim yolu: `/workspace/output/Finans-D-debug-ayni-kimlik.apk`.
- Boyut: **7.259.568 bayt** (6,923 MiB).
- SHA-256: `5c608b0edee2a3c289845dff8c2c0da92dcc937740f4b1123dd75ae150391821`.
- Uygulama kimliği: `com.butcetakip.app`.
- Sürüm: `1.2.0`, versionCode **2**.
- Minimum Android: **7.0 / API 24**. Target/compile SDK: **36**.

### Bağımsız test APK’sı — önerilen

- Gerçek derleme yolu: `/workspace/android-device-validation/android/app/build/outputs/apk/debug/app-debug.apk`.
- İndirilebilir teslim yolu: `/workspace/output/Finans-D-bagimsiz-test.apk`.
- Boyut: **6.980.900 bayt** (6,658 MiB).
- SHA-256: `11ccfbd125303c2bb49432431ca1f07215cc0f8a9749f4820748e0f7cdec9ec5`.
- Uygulama kimliği: `com.butcetakip.app.devtest`.
- Sürüm: `1.2.0-d-test`, versionCode **2**.
- Minimum Android: **7.0 / API 24**. Target/compile SDK: **36**.


App.jsx: **485.539 bayt**; SHA-256:

`980a0eadaa3837c93b6be781c388f32827c97fb52adaac0f4e54025c7990e155`

### İmza

Her iki APK `apksigner verify --verbose --print-certs` ile doğrulandı:

- İmzacı sayısı: 1.
- Sertifika: `C=US, O=Android, CN=Android Debug`.
- Anahtar: RSA, 2048 bit.
- Sertifika SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.
- Sertifika SHA-1: `12c6f44ad94e21e90326bc64a6f274c64b888862`.
- APK Signature Scheme v2: geçerli. v1/v3/v3.1/v4 imzası ve SourceStamp bulunmuyor; v2 doğrulaması başarılı. Minimum SDK 24 olduğundan v1 yokluğu tek başına imza hatası değildir.
- İki APK da `debuggable`; mağaza/kalıcı üretim dağıtımı için release olarak sunulmuyor.

**APK SHA-256 dosyanın bütünlüğünü, sertifika SHA-256 ise imzalayan kimliği gösterir. Birbirinin yerine kullanılamaz.** Sertifikada Android Debug yazması telefonundaki debug sertifikasının aynı olduğu anlamına gelmez. Farklı bilgisayarlarda üretilen debug anahtarları farklı olabilir. Özel imzalama anahtarı teslim edilmedi veya okunmadı.

## Mevcut uygulamanın üzerine kurulabilir mi?

**Şu an güvenli güncelleme yapılabileceği doğrulanmış değildir.** Telefonundaki uygulamanın gerçek package ID'si, imzacı sertifikası ve versionCode'u elimizde yok.

Aynı uygulama üzerine güncellemede birlikte doğrulanması gerekenler:

1. Yüklü uygulamanın kimliği yeni APK ile aynı olmalı.
2. Android'in kabul edeceği imzacı aynı olmalı veya geçerli bir imza anahtarı dönüşüm zinciri bulunmalı; bu APK için böyle bir zincir varsayılmıyor.
3. Yeni versionCode, yüklü sürümün altına düşmemeli. Bu APK'nın versionCode'u **2**; telefondaki daha yüksekse normal güncelleme reddedilebilir.
4. Bu teknik koşullar sağlansa bile veri uyumluluğu, cihaz testi ve dışarı alınmış yedek ayrıca gerekir. Sadece kurulumun başarılı olması veri güvenliği kanıtı değildir.

İmza uyuşmazlığında Android kurulumu reddeder. **Çözüm olarak mevcut uygulamayı kaldırma, verilerini temizleme veya zorla sürüm düşürme yapma.** Mevcut release anahtarıyla uygun sürüm numaralı üretim paketi ayrı bir teslim işidir; bu aşamada üretilmedi. Google Play ile kurulmuş sürümde imzalayan anahtar, upload anahtarından farklı olabilir.

İstersen bilgisayarda salt okunur kontrol yapılabilir (Android SDK araçları gerekir): `adb shell pm path com.butcetakip.app` ile yüklü APK yolu bulunur; komutun döndürdüğü gerçek `base.apk` dosyası `adb pull` ile bilgisayara alınır. `apksigner verify --print-certs mevcut-base.apk` ve `aapt dump badging mevcut-base.apk` çıktıları karşılaştırılır. Yol tahmin edilmemeli; hiçbir `uninstall`, `pm clear` veya downgrade komutu kullanılmamalı. Bu kontroller telefonunda bu çalışma sırasında çalıştırılmadı.

## Önce gerçek veriyi yedekle

1. Telefonundaki **mevcut finans uygulamasını** aç; uygulamanın adını kontrol et. Özet bakiyesi, bekleyen ödemeler toplamı, kullanılabilir bakiye, dönem ve tarih için ekran görüntüsü al.
2. Ayarlar → **Yedek Al** ile JSON dosyasını dışarı kaydet. Dosyanın adı, tarihi ve boyutunu not et; dosya gerçekten İndirilenler/Belgeler içinde görünmeli. Sadece "kaydedildi" mesajına güvenme.
3. JSON'u değiştirmeden bilgisayarına da kopyala. İkinci bir güvenli yerde sakla. Yedek finansal bilgiler içerir; herkese açık depoya yükleme. Mevcut yedek sisteminin şifreleme sağladığı varsayılmamalı.
4. Dosyanın boş olmadığını ve JSON olarak açılabildiğini salt okunur kontrol et. `format`, `data.ft_islemler` ve yedekte beklediğin kart/kredi/ödeme/taksit/kategori bölümleri bulunmalı. Sıfır işlem kaydı gerçekten mümkün; boş listeyi sırf boş diye hatalı sayma. Eski uygulama bazı alanları farklı tutabilir; dosyayı elle yeni biçime dönüştürme.
5. Windows'ta `Get-FileHash -Algorithm SHA256 ".\yedek.json"` ile özetini kaydet; bilgisayara kopyalanan dosyada aynı olmalı.
6. Denemeleri önce boş, bağımsız **Finans Test D** uygulamasında yalnız yapay kayıtlarla yap. Gerçek yedeği test uygulamasında açmak zorunlu değildir. Sonradan uyumluluk denemesi istersen bu bir **kopya içe aktarma** işlemidir; özgün yedeği veya üretim verisini değiştirmez ama kişisel verinin ikinci bir cihaz içi kopyasını oluşturur. Açık kararın olmadan buna gerek yok.
7. Uygulama içi otomatik güvenlik kopyaları aynı depoda tutulabilir; kaldırma/veri temizleme/telefon kaybına karşı dışarıdaki dosyanın yerine geçmez. Android `allowBackup=true` ayarı da bulut yedeğinin alındığının kanıtı değildir.

## Bağımsız test kurulumu nasıl ayrıldı?

Üretim dosyalarına dokunmadan `/workspace/android-device-validation/android` altında Android kaynaklarının ayrı test kopyası oluşturuldu. İmzalama anahtarları kopyalanmadı, yeni paket yüklenmedi.

Yalnız bu test kopyasında:

- `app/build.gradle`: debug için `applicationIdSuffix ".devtest"`, `versionNameSuffix "-d-test"`.
- `app/src/debug/res/values/strings.xml`: görünen ad **Finans Test D**, paket/URL şeması ayrı.
- Test kopyasının `app/src/main/assets/capacitor.config.json` kimlik ve adı güncellendi.
- `app/src/debug/AndroidManifest.xml`: yalnız testte Android otomatik yedeklemesi kapalı (`allowBackup=false`); beklenmedik OS geri yüklemesiyle gerçek uygulama verisi karışmasın. Elle JSON yedekleme çalışmaya devam eder.
- Provider yetkisi mevcut `${applicationId}.fileprovider` kuralıyla test kimliğine ayrıldı. MainActivity Java namespace'i aynı kalabilir; paketlenmiş başlatıcı `com.butcetakip.app.MainActivity`, uygulama kimliği ise `.devtest` olarak doğrulandı.

Farklı applicationId Android'de ayrı UID/özel veri dizini demektir. Ana uygulamanın özel verisi tipik olarak `/data/user/0/com.butcetakip.app`, testinki `/data/user/0/com.butcetakip.app.devtest` altındadır (cihaz profilinde yol değişebilir). WebView origin'i aynı olsa bile depolar uygulama sandbox'ına ayrıdır. `ft_*` anahtarları aynı isimli kalır; içerikleri ortak değildir. Bildirim kanalları/izinleri de uygulama kapsamında ayrıdır. Paylaşılan İndirilenler'e dosya kaydetmek ayrı bir işlemdir; yedeklere ayırt edici isim ver.

**Kurulum:** yalnız `Finans-D-bagimsiz-test.apk` dosyasını indir → Android'in o dosya kaynağı için istediği kurulum iznini geçici ver → kurulumu tamamla → listeden **Finans Test D**'yi aç. Kimliği uygulama bilgisinden/ADB'den doğrula. Mevcut uygulamayı değiştirme sorusu veya aynı kimliğe ilişkin uyuşmazlık görürsen dur; yanlış APK seçilmiş olabilir. Test kimliği zaten kuruluysa temiz başlangıç varsayma. Üretim uygulamasının verilerini temizleme.

Bu APK bağımsız kurulum için **hazırlandı ve derlendi**; gerçek cihazda kurulduğu/çalıştığı henüz doğrulanmadı. İnternetsiz temel kayıt akışı ve dosya seçici davranışı cihaz listesinde sınanmalı. Bildirim özelliğine yeni geliştirme yapılmadı.

## Yapılan teknik kontroller ve sınırları

- Önceki **207/207** kabul testi sonucu ve kaynak SHA-256 korundu. Kaynak değişmediği için bu teslim hazırlığında finans testleri yeniden çalıştırılmadı.
- Ayrı test APK'sı mevcut JDK21/SDK36/Gradle önbelleğiyle `:app:assembleDebug --offline --no-daemon --max-workers=2` kullanılarak başarıyla derlendi.
- Her iki APK'nın imzası doğrulandı; kimlik, versionCode/name, minimum/target SDK ve debug bilgileri APK içinden okundu.
- İki APK'nın JavaScript varlıkları aynı; güncel `dist/index.html` ve görünür `dist/assets` dosyaları paket içeriğiyle hash üzerinden eşleştirildi.
- Üretim App.jsx, Android Gradle, manifest ve ana strings dosyaları başlangıç hash'leriyle aynı. Tek App.jsx teslim yapısı korundu.
- Yeni test paketi cloud ortamında doğrulandı; Android WebView, dosya kaydetme ve süreç kaybı gerçek cihazda hâlâ denenmeli. Cihaz doğrulaması bitmeden yeni özellik çalışmasına geçilmiyor.

Kanıtlar: [APK ve kaynak hash kaydı](./android-dogrulama/apk-kanitlari.json), [bağımsız APK imza çıktısı](./android-dogrulama/bagimsiz-imza.txt), [derleme günlüğü](./android-dogrulama/bagimsiz-apk-derleme.log). Test kopyasını hazırlayan yardımcı ve doğrulama betikleri aynı klasördedir; kullanıcının kurulum için bunları çalıştırması gerekmez.

## Geri dönüş

Bağımsız test APK'sı için üretim uygulamasında geri dönüş gerekmez; üretim kurulumu değiştirilmedi. Testte yapılan işlemleri gerçek veriye otomatik aktarma yoktur. Test sonunda üretim uygulamasını açıp başlangıç ekran görüntüsüyle bakiyeleri/kayıtları karşılaştır; orada yeni işlem yapmadıysan değişiklik beklenmez.

Aşama D kaynağını geri almak gerekirse [D öncesi kaynak kopyası](./App_ASAMA_D_ONCESI.jsx) ve [D sonuç raporundaki geri dönüş yöntemi](./ASAMA_D_SONUC_RAPORU.md) mevcut. Yeni D verilerini eski kaynakla yorumlamanın güvenli olduğu varsayılmamalı; gerçek geri yükleme ayrı onay ve dosya yedeği gerektirir.
