# Ödeme onayı ve Bugün Harcanan — 1.4.1

## İndirilecek dosyalar

- **App.jsx**: Tek dosyalı güncel uygulama kaynağı. Projede `src/App.jsx` yerine kaydedilir. ZIP değildir.
- **Finans-1.4.1.apk**: Ana uygulama için Android debug güncellemesi.
- `android/app/build.gradle`: Bilgisayardan yeniden derlenecekse sürüm yükseltmesi için güncellenen dosya.

GitHub dosya sayfasındaki **Download raw file** düğmesiyle indirilebilir. APK'yı kurmadan önce aşağıdaki imza koşullarını kontrol et.

## Yapılan değişiklikler

1. Ödeme, kredi taksiti, taksit takibi ve kart ekstresi için ödendi onayları standart tarayıcı penceresi yerine mevcut Modal bileşeninde açılır. Açık/koyu/gri renk değişkenleri, Escape, odak sınırı ve geri odaklama korunur. Vazgeç, pencereyi kapatma veya dışına dokunma kayıt oluşturmaz. Onay butonu tekrar çalıştırılamaz; tek onay tek işlem başlatır. Kartın tarih seçme ekranı korunur. İç içe pencerelerde onay penceresi kapandıktan sonra ödeme devam eder.
2. Özet ekranında Ay Sonu Tahmini Gider kartının ardından **Bugün Harcanan** kartı eklendi. Her zaman gerçek yerel bugünü gösterir; dönem seçimi bugünün tarihini değiştirmez. Harita ile aynı `gunlukFinansHareketleri(...,'harcama')` sonucunun gider toplamını kullanır. Yeni finans motoru yoktur.

Örnek: Kart toplamı 10.000 TL'den 10.500 TL'ye çıkmışsa kaydedilmiş günlük fark 500 TL'dir. Aynı gün 250 TL nakit alışveriş varsa Bugün Harcanan **750 TL** olur. İlk girilen birikmiş kart borcu, provizyondan kesinleşmeye geçerken toplam değişmemesi, gelirler, gelecek tarihler ve ekstre ödemeleri yeni alışveriş olarak sayılmaz. Ödenen faturanın bağlı işlemi ve ödeme kaydı bir kez sayılır. Kredi anapara ödemeleri mevcut harita mantığı gibi nakit görünümündedir; bu kartın alışveriş toplamına yeniden girmez.

Finans kayıtlarına toplu dönüştürme, geçmiş borç değişikliği, yedek formatı değişikliği ve yeni bağımlılık yoktur. Gerçek telefon verilerine erişilmedi. Açık/koyu/gri tema, grafik ve animasyon süreleri korunur.

## Test ve derleme

- Önceki **414 test** ve yeni **8 gerçek React/tarayıcı ekran testi**: **422/422 başarılı**, başarısız/atlanan/iptal edilen yok.
- Yeni testler: üç temada onay/iptal/Escape, onay öncesi kayıtların korunması, tek kredi ödeme kaydı; günlük kart farkı ve nakit toplamı; provizyon geçişi; ilk kart borcunun dışlanması; ekstre ve diğer günlerin dışlanması; yeniden açılış; ödenen faturanın tek sayılması.
- Eski testlerin yalnız onay penceresiyle etkileşim adımları güncellendi. **980 assert çağrısı aynen korundu**; beklenti gevşetilmedi. `kanitlar/scope.json` bunu ve ortak finans işlevlerinin değişmediğini belgeler.
- İlk test turunda iki eski ekran testi yeni pencereye onay vermeden kayıt kontrol ettiği için başarısızdı. Onay tıklaması eklendi; finansal beklentiler değiştirilmeden son turda ikisi de geçti.
- `npm run build`: başarılı. Mevcut büyük bundle uyarısı sürüyor.
- `npx cap sync android`: başarılı.
- `:app:testDebugUnitTest :app:assembleDebug --offline`: başarılı. Değişmeyen native kodun 11 testlik Gradle sonuçları başarılıdır (Gradle önbelleği kullanılır).
- ESLint: önceki **49 hata, 12 uyarı** devam ediyor; lint başarılı diye sunulmadı. Bu iş kapsamında genel kod temizliği yapılmadı.
- Kanıtlar `kanitlar/` klasöründedir. Testler yalnız sentetik veri kullanır.

## APK ve güvenli güncelleme

- ApplicationId / namespace: **com.butcetakip.app**. Bağımsız veya `.devtest` kimliği kullanılmadı.
- versionName: **1.4.1**, versionCode: **9**. Önceki teslim: 1.4.0 / 8.
- Dosya: **Finans-1.4.1.apk**, 8.002.163 bayt.
- SHA-256: `1d73a19637e79ecfec7ea2337059b3acc2d61a5f07e2626b626942126f75a245`.
- İmza sertifikası SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.
- İmzalayan: `C=US, O=Android, CN=Android Debug`.
- Bu ortamda daha önce teslim edilen 1.4.0 APK ile paket kimliği ve sertifika aynı; sürüm kodu yükseltildi. Telefonda başka bir anahtarla imzalanmış uygulama varsa aynı kimlikte bile Android güncellemeyi reddeder. Farklı paket kimliği ise mevcut uygulamanın güncellemesi değildir. **Telefondaki kurulu imza bilinmediğinden güvenli güncelleme kesin varsayılmamalıdır.**
- Uygulamayı kaldırma, verilerini temizleme, düşürmeye zorlama. Başka bir bilgisayarda yeniden derleme tek başına imza farkını çözmez; kurulu sürümün özgün imza anahtarı gerekir.
- Kurulumdan önce uygulamadan JSON yedek al, dosyanın oluştuğunu ve boş olmadığını kontrol et, ikinci bir yerde sakla. Bu sürüm kurulum sırasında gerçek kayıtları otomatik dönüştürmez.
- Gerçek Android telefonda bu sürüm henüz denenmedi. Cihazda üç temada ödeme onayı, Vazgeç/geri tuşu, onay sonrası tek kayıt, Bugün Harcanan–harita uyumu, ekstre ödemesinin harcama toplamını artırmaması, yedek alma/geri yükleme ve yeniden açılış kontrol edilmelidir.

## Kaynaklar ve geri dönüş

Üretim değişiklikleri yalnız `src/App.jsx` ve `android/app/build.gradle` dosyalarıdır. Ek testler ve kanıtlar ayrı tutulmuştur; Android yedekleme Java dosyaları değiştirilmemiştir. APK'nın içindeki web kaynağı ve teslim App.jsx aynı derlemeden gelir. Kaynak SHA-256 bilgisi `SHA256.json` içindedir.

İşlem öncesi kaynak `geri-donus/App.jsx`, eski sürüm yapılandırması `geri-donus/build.gradle` dosyalarında korunur. Kaynağa dönmek gerekirse bunlarla geri alınır; telefona eski düşük sürüm APK'sı zorla kurulmaz. İmzayı koruyarak daha yüksek sürüm koduyla geri dönüş derlemesi yapılır. Kullanıcı JSON yedeği kaynak geri dönüşünden ayrı tutulur; gerçek kayıtlar otomatik geri yüklenmez.
