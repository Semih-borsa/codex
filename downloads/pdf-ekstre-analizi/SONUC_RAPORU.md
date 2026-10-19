# PDF Ekstre Analizi — 1.4.0

## Kullanım ve özellikler

**Menü → Ekstre Analizi → Ekstre PDF dosyası** yolunu kullan. PDF okununca kart etiketi ve ekstre ayını gir; tarih, TL tutarı, işlem türü, kategori ve dahil edilecek satırları kontrol et. Ardından **Seçili satırları analiz et** düğmesine bas.

- Kişisel gider kategorilerinden öneriler; bilinmeyenlerde Kategorisiz. Kategoriler mevcut kategori listesine yazılmaz.
- Harcama, iade ve ekstre ödemesi ayrılır. Ekstre ödemesi yeni alışveriş/gelir sayılmaz; brüt harcama ve iade sonrası net ayrı gösterilir.
- Kategori dağılımı, en yüksek harcamalar, tekrarlayan işyerleri ve sayısal yorumlar bulunur.
- Aynı kart etiketinin önceki ekstre ayı da bu oturumda yüklüyse toplam/kategori karşılaştırması yapılır. Aynı açıklama/tutarın tekrarı yalnız **olası** düzenli ödeme olarak belirtilir; kesin abonelik denmez.
- Aynı PDF SHA-256 ile tekrar eklenmez. Aynı kart/ay için ikinci analiz engellenir; otomatik birleştirme yapılmaz.
- Çoklu tutar sütunu, döviz, eksik yıl veya belirsiz işaret otomatik kesin gider olarak kabul edilmez. Satırları düzeltebilir, eksik satırı elle ekleyebilirsin. Eksik yıl yalnız sen girersen tamamlanır.
- Karşılaştırılabilir harcama toplamı farklıysa analiz engellenir; istersen farkı açıkça kabul ederek yalnız seçili satırların **kısmi** analizini yapabilirsin. Ekstre borcu/toplam bakiye harcama toplamı yerine kullanılmaz.

## Gizlilik ve kapsam

PDF okuyucu ve worker APK içindedir; belgeyi bir sunucuya gönderen kod veya yapay zekâ servisi yoktur. Gerçek PDF worker tarayıcı testinde yalnız yerel uygulama varlıklarına GET istekleri gözlendi; PDF yükleme/POST/dış servis isteği yoktu. Android’in .mjs worker MIME desteği kaynakta doğrulandı. Fiziksel cihazda internet kapalı testi ayrıca yapılmalıdır.

**PDF, düzeltmeler ve analizler yalnız ekran belleğinde tutulur. Bölümden çıkınca silinir; JSON yedeğine/gerçek işlemlere/kart borcuna/bakiyeye yazılmaz.** Çoklu ay karşılaştırması için aynı oturumda ekstreleri yükle. En fazla 12 analiz tutulur.

Yalnız **metin içeren, şifresiz PDF**, en fazla **10 MB / 30 sayfa** desteklenir. Taranmış dosyalar için OCR, şifre açma ve kur çevirisi yoktur. İlk sürüm sentetik PDF’lerle doğrulandı; gerçek bankaların tüm düzenlerinin okunacağı garanti edilmez. Tarih/sütun/işaret/kategori önerilerini PDF ile kontrol et. Karma metin/görsel sayfa uyarılır; eksik bilgi tam ekstre gibi sunulmaz.

## Dosyalar ve korunanlar

- **[Ana uygulama APK’sı](./Finans-PDF-Ekstre-1.4.0.apk)** — bilgisayar komutu gerekmez.
- [App.jsx — zipsiz](./App.jsx) → src/App.jsx. Yeni React kaynakları tek dosyada.
- [package.json](./package.json), [package-lock.json](./package-lock.json): kaynak koddan derleyecekler için güncel bağımlılıklar.
- [android/app/build.gradle](./android/app/build.gradle): versionCode 7→8, versionName 1.3.1→1.4.0.

PDF okuma için **pdfjs-dist 6.4.299** eklendi. Güvenlik kontrolünde eski Capacitor Android sürümündeki GHSA-rvm3-566m-v7fv duyurusu görüldü. Android/core/CLI birlikte, aynı ana sürümde **8.5.3**’e sabitlendi; ilgili derleme aracı alt bağımlılıklarına uyumlu yamalar uygulandı. Native dosya kaydetme köprüsü değiştirilmedi, derlenip test edildi.

Kaynak karşılaştırmasında eski işlevler, A/B/C/D, geçmiş kart borcu, kalıcı planlama, yedekleme ve global tema/animasyon blokları birebir aynı. App köküne yalnız yeni menü ve ekran bağlantısı eklendi. Gerçek kullanıcı dosyası/kaydı açılmadı veya değiştirilmedi; yeni veri tabanı/migrasyon yok.

## Test ve derleme

**414/414 JS/React testi geçti; 0 başarısız, 0 atlanan.** Önceki 366 testin kaynak/beklentileri aynı. Yeni 48 kontrol: 31 işlev + 17 üretim Vite çıktısı ve gerçek PDF worker ile Chromium ekran testi. 9 sentetik PDF; normal, önceki ay, belirsiz/döviz, toplam farkı, taranmış, şifreli, bozuk, PDF olmayan ve 31 sayfa durumlarını kapsar. Elle düzeltme, kısmi onay, tekrar yükleme, iptal, veri koruma ve üç temada 320px uyumluluk doğrulandı. Ham sonuç: kanitlar/final-tests.tap.

**11 native test geçti** (10 yedek dosya I/O + 1 mevcut örnek test); bu derlemede görev yeniden çalıştı. npm run build, npx cap sync android ve assembleDebug başarılı. Worker ve güncel web dosyalarının APK içindeki hash eşitliği doğrulandı.

ESLint eski/yeni **49 hata / 12 uyarı**; yeni lint hatası eklenmedi. Vite büyük paket uyarısı sürüyor. npm audit önce 2 yüksek/2 kritik bulgu gösteriyordu; son kontrolde **yüksek/kritik 0**, CLI’nin iOS derleme araçları zincirinde (uuid/xcode/@capacitor/cli) **3 orta düzey** bulgu kaldı. Bunlar PDF okuyucuya ait değildir; audit çıktıları rapora eklendi, tüm bağımlılıklar sorunsuzmuş gibi sunulmadı.

## APK ve güvenli güncelleme

Kimlik **com.butcetakip.app**; sürüm **1.4.0 / versionCode8**, debug. Boyut **7.999.843 bayt**.

APK SHA-256: `337abcf442b2725ea5fb89f582ec2f4916b77b2c535b1492146af5128e06f981`.

Sertifika SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.

Önceki teslim edilen ana 1.3.1 APK ile kimlik/imza eşitliği, sürüm kodu 7→8 ve paketlenmiş Capacitor yapılandırması eşitliği doğrulandı. Kurulu telefon paketinin sertifikasına doğrudan erişilmedi; farklı anahtar veya daha yüksek sürümde uyumluluk varsayılmaz. **Uygulamayı kaldırma veya verilerini temizleme.** Kurulum reddedilirse durup paket kimliği/imzasını kontrol etmek gerekir.

Önce JSON yedeğini alıp bilgisayara da kopyala. Normal güncellemeden sonra bakiye/kayıtları, JSON kaydetme/geri yükleme ve bildirimleri kontrol et. Sonra PDF’yi seçip önizlemeyi doğrula. **Yeni APK ve gerçek banka PDF düzenleri fiziksel Android telefonda henüz doğrulanmadı.** Gerçek ekstreni bana veya herkese açık repoya göndermen gerekmez.

Geri dönüş kaynakları geri-donus/ içindedir. 8 kurulduktan sonra eski App gerekirse aynı imza ve 8’den yüksek sürüm koduyla, tercihen güvenlik yamalı Capacitor korunarak yeniden derlenir. Eski bağımlılık kopyaları yalnız başlangıç kanıtıdır; bilinen açıklar nedeniyle geri kurulması önerilmez. Veri silme/migrasyon geri alma gerekmez.
