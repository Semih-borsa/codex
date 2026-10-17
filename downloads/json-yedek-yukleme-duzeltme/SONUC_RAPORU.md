# JSON yedek yükleme düzeltmesi

## Sorun ve sınır

AyarlarTab.yedekYukle içindeki tek catch, dosya JSON ayrıştırması ile cihazın yerel kayıtlarını okurken oluşan SyntaxError hatasını aynı “Dosya geçerli JSON değil” mesajına dönüştürüyordu. Sentetik bozuk yerel kayıtta bu yanlış teşhis yeniden üretildi. File.text olmayan WebView de dosyayı okuyamıyordu. Telefonda oluşan hatanın kesin kaynağı cihaz logu olmadan doğrulanamaz; bu iki kod hatası düzeltildi. Gerçek kişisel yedek veya kayıt üzerinde deney yapılmadı.

Yedek alındıktan sonra işlemlerin değişmemesi yüklemeyi engellemez. Değişmeyen kayıtların geçerli tam yedeği önizlemeye gelir; aynı kayıtlar için ekleme/silme/değişme 0 olur.

## Değişiklikler

- App.jsx: yalnız AyarlarTab.yedekYukle değişti; kapsam kanıtı kanitlar/kapsam.json.
- Dosya okuma, JSON ayrıştırma, yedek doğrulama ve mevcut kayıt okuma ayrı hata aşamalarıdır.
- Eski WebView için FileReader UTF-8 alternatifi eklendi. İlk BOM karakteri varsa ayrıştırma öncesi kaldırılır; JSON içeriği değiştirilmez. Chromium Blob.text BOM'u zaten temizlediği için BOM testi eski sürümde de geçti; BOM telefon hatasının kanıtlanmış nedeni değildir.
- Dosya okunamıyorsa dosyayı telefona kaydedip yeniden seçme yönlendirmesi yapılır.
- Yerel kayıtlar bozuksa yükleme güvenli biçimde engellenir; dosya geçersizmiş gibi gösterilmez. Bozuk yerel kayıt silinmez, güvenlik kopyası zorunluluğu atlanmaz. Bu durumun gerçekten cihazında bulunması halinde ayrıca teşhis gerekir.
- 10 MB sınırı, mevcut finora-v2/v1 ve butce-takip-v1 doğrulayıcıları, önizleme/onay, geri yükleme öncesi güvenlik kopyası ve hata geri alma korunmuştur.
- Finans hesapları, geçmiş borç düzeltmesi, A/B/C/D, temalar ve yerleşim birebir aynı kaldı. Native dosya kaydetme kodu ve bağımlılıklar değişmedi.
- Android app/build.gradle: versionCode 4 → 5, versionName 1.2.2 → 1.2.3; ayrı test paketinde -d-test eki korundu.

## Test ve derleme

Mevcut 268 test ve 5 yeni React/Chromium testi: **273/273 geçti; başarısız/atlanan yok**. Eski testler değiştirilmedi. Yeni testler BOM, yerel bozuk kaydın mesajı, File.text bulunmaması, dosya okuma hatası ve değişmeyen yedek önizlemesini kapsar. Yeni 5 kontrol eski kaynakta 2 başarılı / 3 başarısız, yeni kaynakta 5 başarılı. Önce/sonra ham çıktılar kanitlar/ içindedir. Önceki güvenli geri yükleme, iptal, bozuk JSON, eksik paket, yazma başarısızlığı ve ödeme bağlantısı testleri de bu çalıştırmaya dahildir.

npm run build ve npx cap sync android başarılı. İki Android debug APK derlemesi başarılı. Değişmeyen native test görevi önbellekteki geçerli sonucu kullandı; yeni cihaz testi olarak sayılmadı. Vite büyük paket uyarısı devam ediyor; genel lint temizliği yapılmadı.

## Dosyalar ve kurulum

- [App.jsx](./App.jsx): src/App.jsx yerine kaydet; zip değildir.
- [Finans Test D APK](./Finans-Test-D-json-yukleme-duzeltme.apk): com.butcetakip.app.devtest, versionCode5.
- [Ana kimlikli APK](./Finans-json-yukleme-ayni-kimlik.apk): com.butcetakip.app, versionCode5.
- [Android sürüm dosyası](./android/app/build.gradle): kendi APK'nı hazırlıyorsan sürüm yükseltmesi.

APK boyutları, SHA-256, kimlik ve imza çıktıları kanitlar/apk-bilgileri.json içindedir. Her iki imza SHA-256 değeri:

bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1

Önceki teslimin aynı kimlikli APK'larıyla imzalar eşleşiyor, sürüm kodu yükseltildi. Telefonundaki kurulum doğrudan incelenmedi; farklı anahtar veya daha yüksek sürümde güncelleme varsayılmaz. Kurulum reddedilirse uygulamayı kaldırma veya verileri temizleme.

## Gerçek Android doğrulaması

Bu APK gerçek telefonda henüz denenmedi. Önce dışarıdaki yedeğini ayrıca bilgisayarda sakla. Önceki Finans Test D uygulamasını kaldırmadan aynı kimlikli APK ile güncelle. Ayarlardan yedek al; dosyayı yeniden seç. Önizlemede kayıt sayılarını ve etkilerini incele. İlk kontrolde Vazgeç'e bas; bakiye ve kayıtların korunduğunu doğrula. Gerçek geri yüklemeyi önce sentetik verili bağımsız test uygulamasında dene. Tekrar hata olursa yeni mesajı eksiksiz paylaş; geçerli dosya ile yerel bozuk kayıt hatası artık ayrıdır. Dosyayı veya kişisel verileri herkese açık repoya yükleme.

## Geri dönüş

Başlangıç kaynağı geri-donus/App.jsx içinde korundu; kullanıcı yedeği değildir. Telefonda versionCode5 kurulduktan sonra eski kaynak gerektiğinde aynı imzayla 5'ten yüksek yeni sürüm koduyla yeniden derlenmelidir; kaldırma/downgrade/veri temizleme gerekmez. Otomatik kayıt dönüşümü veya gerçek kullanıcı verisi değişikliği yapılmadı.
