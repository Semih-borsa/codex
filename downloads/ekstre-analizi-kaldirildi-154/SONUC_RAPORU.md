# Ekstre Analizi kaldırıldı — 1.5.4

Ekstre Analizi menüsü, ekranı, PDF seçme/okuma akışı ve kategori analiz grafikleri uygulamadan kaldırıldı. `pdfjs-dist` bağımlılığı ve PDF worker referansı kaldırıldı; App.jsx artık PDF paketi kurulmasını gerektirmez.

Kartların normal ekstre borcu, ekstre ödeme işlemleri, tarihli taksit planları ve geçmiş borç güvenliği korunmuştur. Raporlardaki Son 6 maaş dönemi seçeneği ve diğer raporlar, yerel finans yorumları, temalar, grafikler ve bildirimler durmaktadır. 14 ana finans/yedek/bildirim işlevi işlem öncesi kaynakla aynıdır (`kanitlar/scope.json`).

Kullanıcının önceden yüklediği `ft_ekstre_analiz_v1` arşivi otomatik silinmedi. Eski JSON yedekleri veri kaybı olmadan okunabilsin diye arşiv doğrulaması ve saf uyumluluk yardımcıları korundu; bunlar ekran veya PDF okuyucu başlatmaz. Finans kayıtları ve eski yedekler değiştirilmedi. Veri tabanı veya yeni bağımlılık eklenmedi.

## Değişen dosyalar

- `App.jsx`: menü/ekran/PDF okuyucu kaldırılması.
- `package.json`, `package-lock.json`: kullanılmayan PDF paketinin çıkarılması.
- `android/app/build.gradle`: 1.5.4, versionCode 17.
- `tests/finance-baseline/payment-today.browser.test.mjs`: üç tema için Ekstre Analizi menüsünün bulunmaması beklentisi; aynı testlerin finans/kayıt kontrolleri korundu.

## Testler

Kaldırılan ekranın sekiz UI-ST ve üç RANGE-UI tema kontrolü kaynak olarak tutuldu; artık kaldırılmış ekranı bekledikleri için koşu dışı. Daha önce kaldırılmış API arayüzü ve eski PDF ekranı testleri de önceki teslimdeki gibi koşu dışı. Saf arşiv/yedek uyumluluk testleri koşuda kaldı. Finansal beklentiler gevşetilmedi.

Genel koşu **479/479 başarılı** (`kanitlar/all-tests.tap`); korunan ekranların ayrı koşusu **6/6 başarılı**. Korunan yerel yorum ekranları ve raporlardaki altı dönem seçimi ayrı koşuyla doğrulandı (`kanitlar/preserved-ui.tap`).

`npm run build`, `npx cap sync android` ve Android native test/debug APK derlemesi başarılı. Fiziksel telefonda bu sürüm henüz doğrulanmadı; menüden kaldırılmasını, 6 maaş dönemi seçimini, mevcut bakiye/kart işlemlerini ve JSON yedek al/geri yüklemeyi cihazda kontrol et.

## Teslim

Tam ve ZIP'siz `App.jsx`, `src/App.jsx` dosyasının yerine konur. Dosya import satırlarıyla başlar. Raporlar'daki altı dönem özelliğini korur. Elle derlemede verilen paket dosyaları proje köküne, Gradle dosyası `android/app/build.gradle` yoluna kaydedilir; `npm install`, `npm run build`, `npx cap sync android`, ardından Android derleme uygulanır. PDF paketi eski node_modules içinde kalsa bile yeni App.jsx onu kullanmaz.

Hazır APK: `Finans-1.5.4.apk`; applicationId `com.butcetakip.app`; versionCode 17. Önceki burada üretilen debug APK'larla aynı imza: sertifika SHA-256 `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`. Dosya boyut ve hash değerleri `SHA256.json` içindedir. Telefonda kurulu imza doğrulanamadığından üzerine güncelleme koşulsuz garanti değildir. Önce JSON yedeği al; çakışmada uygulamayı kaldırma/verilerini temizleme.

`geri-donus/` işlem öncesi kaynak/paket/sürüm kopyalarını içerir. Kaynak düzeyinde geri alınabilir. Telefonda zorla eski versionCode kurulmaz; gerekirse aynı imzayla daha yüksek versionCode taşıyan geri dönüş APK'sı hazırlanır.
