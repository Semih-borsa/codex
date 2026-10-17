# Android yedek dışa aktarma düzeltmesi — 17 Ekim 2026

## Sonuç

Yalnız yedek dışa aktarma düzeltildi. Android'de artık sistemin **belge oluştur/kaydet** ekranı açılır. Dosya yazılıp kapatıldıktan sonra aynı belge geri okunur; bayt sayısı ve SHA-256 içerik özeti eşleşirse başarı bildirilir. İptal nötr sonuçtur, yazma/doğrulama hatası görünür Türkçe hata verir. Genel kod temizliği veya yeni finans özelliği geliştirilmedi.

**Gerçek telefonunuzda yeni APK henüz denenmedi.** Sistem seçicisinin gerçek cihazdaki açılması, Kaydet/İptal düğmeleri ve dosya sağlayıcısının davranışı aşağıdaki cihaz adımlarıyla doğrulanmalıdır. Buluttaki sözleşme testleri bu fiziksel cihaz testinin yerine geçmez.

## 1. Kök neden ve eski testlerin sınırı

Eski `AyarlarTab.yedekIndir` işleyicisi:

1. `yerelYedekPaketi()` ile mevcut `ft_*` kayıtlarını JSON'a çeviriyordu.
2. Uygulama içine `ft_manual_backup_latest` kopyası yazıyordu; bu dışarıda dosya oluşturmak değildir.
3. Web Share API'yi deniyor, destek/hata durumunda `URL.createObjectURL(blob)` + `a.download` + `a.click()` kullanıyordu.
4. Android sistemine gerçekten dosya yazıldığına dair sonuç almadan olumlu mesaj verebiliyordu. `AbortError` ile paylaşım iptali de yerel kopyaya bakılarak olumlu renkte gösterilebiliyordu. Paylaşım hatası iç catch'te kullanıcıya açıklanmadan fallback'e düşüyordu.

Normal masaüstü Chromium'un dosya indirme olayı, Capacitor Android WebView'in Blob URL'yi telefonun dosya yöneticisine kaydettiğinin kanıtı değildir. Android WebView'de `anchor.click()` sistem kaydetme diyaloğu açmayı garanti etmez; projede bu Blob indirmeyi native dosyaya yazan bir yol yoktu. Önceki testler tarayıcıda dosya alıyordu; Android köprüsünü sınamıyordu.

Koddan bu güvenilirlik/sahte başarı sorunu doğrulandı. Telefonun Logcat çıktısına erişilmediği için cihazınızda tam olarak hangi paylaşım desteği/fallback/istisna dalının çalıştığı kesinleştirilmedi; olmayan bir cihaz izini elde edilmiş gibi sunmuyorum. `yerelYedekPaketi` veya finans motorunda bu belirtiyi açıklamak için veri dönüşümü yapılmadı.

## 2. Çözüm

Mevcut Capacitor core/android/CLI **8.2.0**, local-notifications **8.2.1**. Filesystem/Share paketi kurulu değildi. Yeni bağımlılık eklenmedi; package.json ve lockfile değiştirilmedi.

Küçük bir yerel Capacitor eklentisi `FinansBackup`, Android Storage Access Framework `ACTION_CREATE_DOCUMENT` kullanıyor. Kullanıcı sistem ekranından dosya adını ve konumu seçer. Geniş depolama izni, MANAGE_EXTERNAL_STORAGE veya ilave manifest izni gerekmez. Aynı işlemden gelen content URI üzerinden yazma ve geri okuma yapılır.

Akış:

- Düğmeye basınca kalıcı görünür durum metni çıkar; aynı anda ikinci çağrı engellenir, düğme bekleme süresince pasiftir.
- Mevcut `yerelYedekPaketi` ve **aynı** `yedekVerisiniDogrula` çalışır; bozuk kayıt native seçiciye gönderilmez.
- Mevcut JSON biçimi/sürümü/alanları korunur; UTF-8 Türkçe metin ve ₺ işareti korunur.
- Kullanıcının iptalinde finansal kayıt veya dış dosya yazılmaz; başarı mesajı verilmez. Önceden oluşturulan uygulama içi ek kopya, dış dosya kaydedildi diye sunulmaz.
- Yazma, flush ve close bitmeden; dosya geri okunup uzunluk/içerik eşleşmeden başarı yoktur.
- Okuma/yazma izni veya sağlayıcı hatasında ekranda hata kalır ve tekrar denemek mümkündür. Kısmen oluşmuş belge geçerli yedek sayılmaz. Kullanıcının dosyaları hatada otomatik silinmez.
- Yerel ek kopya kota nedeniyle yazılamazsa, sistem konumuna doğrulanmış dış dosya yazılması yine mümkün; bu iki sonuç ayrı açıklanır.
- İnternet kullanılmaz. İnternetsiz test için sistem seçicisinde **Telefon / İndirilenler** gibi yerel konum seçilmeli; Drive gibi uzak sağlayıcı seçiminin çevrimdışı çalışması garanti edilmez.
- Büyük JSON Android'in sınırlı Activity state Bundle'ına konmaz. Süreç tam seçici açıkken öldürülürse boş belgeyi başarılı saymak yerine yeniden deneme gerekir; süreç kesintisi cihazda ayrıca sınanmalı.
- Güncellenmemiş eski APK'da native köprü bulunmazsa açıkça **güncel Android APK'sı gerektiği** yazılır; Android'de sessiz Blob fallback'i yapılmaz.

Normal web tarayıcısında mevcut Blob indirme yolu korunur; yalnız "istek gönderildi, dosyayı kontrol et" şeklinde nötr sonuç verilir. Tarayıcı kaydetmesini uygulamanın doğrulayamadığı durumda başarı iddiası yoktur. Web Share API çözülmesinin dosyanın diske kaydedildiği anlamına geldiği varsayılmıyor.

## 3. Değişen dosyalar ve yerel projenizdeki yerleri

| Teslim dosyası | Yerel projenizde hedef | Neden |
|---|---|---|
| `App.jsx` | `src/App.jsx` | Görünür durum, iptal/hata ayrımı ve native köprü çağrısı |
| `android/app/src/main/java/com/butcetakip/app/FinansBackupPlugin.java` | Aynı dizin | Android sistem seçicisi ve sonucunu alma |
| `android/app/src/main/java/com/butcetakip/app/BackupDocumentIO.java` | Aynı dizin | Dosya yazma/kapatma/geri okuma ve içerik doğrulama |
| `android/app/src/main/java/com/butcetakip/app/MainActivity.java` | Aynı dizin | `registerPlugin(FinansBackupPlugin.class)` kaydı |
| `android/app/build.gradle` | Aynı dizin | versionCode **2 → 3**, versionName **1.2.0 → 1.2.1** |
| `android/app/src/test/java/com/butcetakip/app/BackupDocumentIOTest.java` | Aynı dizin, test dosyası | Native I/O kabul testleri; uygulamanın çalışması için zorunlu değil |

MainActivity dosyanızda sonradan başka özel native kod eklediyseniz onu silmeyin; mevcut onCreate içinde bu eklenti kaydını koruyarak birleştirin. Buradaki kaynak, yüklediğiniz projede boş BridgeActivity alt sınıfı olan dosya temel alınarak düzenlendi.

**Bu Android düzeltmesi yalnız App.jsx değiştirerek tamamlanamaz.** Tek dosyalı React uygulaması korunuyor; fakat sistem kaydetme ekranını ve gerçek yazma sonucunu sağlayan native kodun APK'ya derlenmesi gerekiyor. Native değişiklikler gizlenmedi; yukarıdaki dosyalar tek tek teslim ediliyor. APK'yı kurmak için kaynak kopyalama/komut çalıştırma gerekmez. Kendi projenizde derleyeceksen `npm run build`, `npx cap sync android` sonrası Android Studio/Gradle ile yeni APK derlenmeli.

Dağıtımın bağımsız test kopyasında `.devtest` kimliği, **Finans Test D** adı ve önceki test-only `allowBackup=false` ayarı korunmuştur. Üretim kimliğine `.devtest` eklenmedi.

## 4. Korunan kapsam

Düzeltme öncesi kaynak, MainActivity, Gradle sürüm dosyası ve paket manifesti `geri-donus/` klasöründe korundu. Git durumu kaydedildi.

AST/kaynak karşılaştırması: mevcut deklarasyonlardan **yalnız AyarlarTab** değişti; `AndroidYedekDosyasi` ve `yedekDosyasiniAktar` eklendi. İçe aktarmaya mevcut Capacitor paketinden `registerPlugin` eklendi. Bütün finans hesaplama işlevleri, kredi/kart/ekstre/taksit motorları, devir ve mevcut geri yükleme doğrulayıcıları aynı. Tema/stil blokları aynı; grafikler, animasyonlar ve ekran düzeni değiştirilmedi. Yalnız yedek düğmesinin işlem durumu ve ona ait sonuç satırı eklendi.

Gerçek kayıtlar okunmadı/silinmedi/dönüştürülmedi. Testler yalnız sentetik verilerde çalıştı. Native eklenti mevcut finans depolarını açmaz; yalnız JS tarafında üretilmiş anlık JSON'u kullanıcının seçtiği belgeye yazar.

## 5. Test sonuçları

| Kontrol | Sonuç |
|---|---|
| Mevcut 207 otomatik test | **207/207 geçti**; beklentiler değiştirilmedi |
| Yeni JS işlev testleri | **14/14 geçti** |
| Yeni Android-köprü ekran/sözleşme testleri | **8/8 geçti** |
| Birleşik JS/Chromium toplamı | **229/229 geçti, 0 atlanan** |
| Yeni Java native dosya I/O testleri | **10/10 geçti, 0 atlanan** |
| Toplam benzersiz otomatik test | **239 geçti, 0 başarısız** |
| `npm run build` | Başarılı |
| `npx cap sync android` | Başarılı |
| Ana kimlikli debug APK + Java testleri | BUILD SUCCESSFUL |
| Bağımsız `.devtest` debug APK + aynı Java testleri | BUILD SUCCESSFUL |

Java testlerinin iki derlemede tekrar çalışması toplamı yapay olarak 249'a çıkarmaz. Yeni JavaScript testleri gerçek Capacitor nativePromise sözleşmesini kontrollü stub ile sınar; gerçek Android belge seçicisi değildir. Java testleri native üretim I/O sınıfını gerçek geçici dosya/stream'lerle sınar; Android ContentResolver sağlayıcısının kendisini simüle etmez.

Sınananlar: onay beklerken başarı olmaması, tek işlem, iptal, eski APK'da eksik plugin, native hata, bayt/doğrulama tutarsızlığı, bozuk JSON'da seçicinin açılmaması, finansal kayıtların aynı kalması, internetsiz sözleşme, yerel ek kopya hatası ve masaüstü indirme. Java katmanında UTF-8 gerçek dosya, açma/izin/yazma/kapatma/geri okuma hataları, kısa/uzun ve aynı uzunlukta bozuk içerik başarı sayılmıyor.

İlk entegrasyon denemesinde yeni web kaydetme yaklaşımı üç eski Chromium indirme testinde zaman aşımı oluşturdu; bu yaklaşım kaldırılıp normal tarayıcı indirme davranışı korunarak sorun çözüldü. Test beklentileri gevşetilmedi. Eski/ara denemeler `kanitlar/backup-export-mevcut-207.tap` ve bazı odaklı çıktılarda görülebilir; **kesin son çıktı** `kanitlar/backup-export-tum-testler.tap` ve `kanitlar/native-10-test.xml` dosyalarıdır.

Native testin ilk offline çalışmasında projede zaten tanımlı JUnit 4.13.2 önbellekte yoktu. Mevcut test bağımlılığı normal Gradle çözümlemesiyle alındı; paket/bağımlılık bildirimi eklenmedi. Son APK/Java test derlemeleri offline olarak da geçti.

Vite büyük paket uyarısı mevcut tek App yapısıyla sürüyor. Güncel ESLint sonucu **49 hata, 12 uyarı**; tanımsız değişken (`no-undef`) bulgusu yok. Bu iş kapsamında genel ESLint temizliği yapılmadı; önceki lint bulgularının giderildiği iddia edilmiyor.

## 6. APK, imza ve güncelleme uyumluluğu

### Finans-yedek-duzeltme-ayni-kimlik.apk

- [APK’yı indir](./Finans-yedek-duzeltme-ayni-kimlik.apk)
- Gerçek derleme yolu: `/workspace/finans-upload/finans-takip/android/app/build/outputs/apk/debug/app-debug.apk`
- Teslim yolu: `/workspace/output/android-yedek-duzeltme/Finans-yedek-duzeltme-ayni-kimlik.apk`
- Boyut: **6986456 bayt** (6.663 MiB)
- Dosya SHA-256: `ada84f701cc6fc3cdbe256ba1e577f9e80f9f03271bf61094d1f2ce768c19a6c`
- Kimlik: `com.butcetakip.app`; sürüm `1.2.1`; versionCode **3**.
- Önceki `Finans-D-debug-ayni-kimlik.apk` ile imza aynı: **doğrulandı**.

### Finans-Test-D-yedek-duzeltme.apk

- [APK’yı indir](./Finans-Test-D-yedek-duzeltme.apk)
- Gerçek derleme yolu: `/workspace/android-device-validation/android/app/build/outputs/apk/debug/app-debug.apk`
- Teslim yolu: `/workspace/output/android-yedek-duzeltme/Finans-Test-D-yedek-duzeltme.apk`
- Boyut: **6986488 bayt** (6.663 MiB)
- Dosya SHA-256: `7942c1ec36b16c94e051b37d4f7595619d5e45080c1579d379c68fb34247f46c`
- Kimlik: `com.butcetakip.app.devtest`; sürüm `1.2.1-d-test`; versionCode **3**.
- Önceki `Finans-D-bagimsiz-test.apk` ile imza aynı: **doğrulandı**.


İki APK da Android Debug sertifikasıyla doğrulandı: `C=US, O=Android, CN=Android Debug`, RSA 2048, geçerli APK Signature Scheme v2. Sertifika SHA-256:

`bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`

Minimum Android 7 / API24, target SDK36. Debug paketler mağaza release teslimi değildir; özel anahtar paylaşılmadı. Dosya SHA-256 ile sertifika SHA-256 farklı amaç taşır.

**Önceki verdiğim `Finans-D-bagimsiz-test.apk` dosyasını kurduysan:** yeni `Finans-Test-D-yedek-duzeltme.apk` aynı `com.butcetakip.app.devtest` kimliğine ve aynı imzacı sertifikaya sahiptir; versionCode 2'den 3'e yükseldi. Bu iki teslim dosyası arasında Android'in imza/kimlik/sürüm güncelleme koşulları doğrulandı. Verilerin ayrı test uygulaması içinde kalması beklenir; kurmadan önce mümkünse mevcut yerel kopyayı koru ve hiçbir veri temizleme yapma.

**Önceki aynı kimlikli debug APK'yı kurduysan:** aynı doğrulama `com.butcetakip.app` için de yapıldı. Ancak telefonunda kendi bilgisayarında/Play'de/farklı anahtarla ürettiğin başka sürüm varsa bu sertifika eşitliği varsayılamaz. Telefondaki kurulu APK'ya doğrudan erişilmedi; hangi dosyanın kurulu olduğu bilinmeden güvenli güncelleme garanti edilmez. VersionCode'u 3'ten yüksek bir kurulum da ayrıca incelenmelidir.

Kurulum imza uyuşmazlığı veya downgrade nedeniyle reddedilirse **mevcut uygulamayı kaldırma, verileri temizleme veya zorla düşürme yapma**. Kurulu APK'nın kimlik/versionCode/sertifika bilgisi salt okunur karşılaştırılmalı. Önceki yedek düğmesi dışarı dosya vermediğinden dış yedeğin var olduğu da varsayılmamalı; uygulama içi kopya kaldırma işleminden korumaz.

## 7. Gerçek cihazda şimdi yapılacak kontrol

1. Telefonundaki uygulama **Finans Test D** ise yalnız `.devtest` güncellemesini seç; **Bütçe Takip** için paket seçmeden önce yukarıdaki imza koşulunu doğrula. Uygulamayı kaldırma.
2. Güncellemeden önce bakiye/bekleyen/kalan, işlem/kart/kredi/taksit sayıları ve kişisel kategoriler için ekran görüntüsü al; mevcut dış yedek dosyası varsa bilgisayara kopyala. Yeni kaydetme düzelmeden dış yedek varmış gibi hareket etme.
3. Uygulamayı yeni APK ile normal güncelle; açılışta aynı kayıtların kaldığını kontrol et. Yeni özellik/migrasyon beklenmez.
4. Uçak moduna geç; Ayarlar → Yedek Al. Düğme bekleme durumuna geçmeli, Android'in dosya oluşturma ekranı açılmalı. **Yerel Telefon/İndirilenler** konumunu seç ve kaydet.
5. Seçici açıkken başarı mesajı olmamalı. Kaydetme bitince "kaydedildi ve doğrulandı" görünmeli. Dosyalar uygulamasında gerçek `.json` dosyasını bul; 0 bayt olmamalı. Adını değiştirdiysen sistemde seçtiğin adı kullan. JSON'u salt okunur aç; Türkçe metin, format ve kayıt sayıları korunmalı.
6. Aynı dosyayı bilgisayara kopyala ve iki kopyanın SHA-256 özetini karşılaştır. Aynı test verisini **yalnız ayrı test uygulamasında** geri yükleyerek C önizleme/bağ koruma kontrolünü tamamla; gerçek üretim verisini sırf deneme için geri yükleme.
7. Tekrar Yedek Al → Android geri/İptal. Başarı mesajı olmamalı; iptal açıklaması görünmeli; finansal kayıtlar değişmemeli.
8. Güvenle kullanılabilir hatalı/erişilemeyen bir test sağlayıcısı varsa yazma hatasını dene. Yoksa **cihazda hata yolu denenmedi** yaz; gerçek telefon depolamasını zorla doldurma. Mesaj Türkçe olmalı, uygulama takılmamalı ve yeniden deneme açık olmalı.
9. Seçici açıkken uygulamayı/işlemi kesip yeniden açmayı yalnız sentetik testte dene. Kaydedilmediği halde başarı oluşmamalı; yeniden deneme mümkün olmalı. İşlemin tam ortasında OS süreç kaybı gerçek cihazda henüz doğrulanmadı.
10. Son bakiye/bekleyen/kalan ve kayıt sayıları başlangıçla aynı olmalı. Yedek alma finansal hareket oluşturmamalı.

Bu adımlardan biri başarısızsa cihaz doğrulaması bitmiş sayılmaz; ekran görüntüsü, Android/WebView sürümü, hangi uygulama adı/paketinin kurulu olduğu ve hata metni raporlanmalı. Gerçek kişisel JSON'u herkese açık repoya yükleme.

## 8. Geri dönüş

`geri-donus/App.jsx`, eski MainActivity ve eski app-build.gradle bu düzeltmeden önceki kopyalardır. Yeni native dosyalar önceden yoktu. Kaynak geri dönüşünde bu kopyalar kullanılabilir; fakat telefonda versionCode3 kurulduktan sonra eski versionCode2 APK'ya normal düşürme beklenmez. Gerekirse eski kaynak, aynı imza ve **daha yüksek yeni versionCode** ile yeniden derlenmeli; kaldırma/veri temizleme çözüm değildir. Bu işlem ayrıca değerlendirilmeden uygulanmadı. Eski dışa aktarma hatası geri gelebileceği için geri dönüş otomatik yapılmaz.

Yalnız dışa aktarma düzeltmesi tamamlandı. Gerçek cihaz doğrulamasından sonuç gelmeden yeni özellik geliştirmeye geçilmiyor.
