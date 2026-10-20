# Haftanın Günleri Özet ekranına taşındı — 1.4.3

Raporlar → Trend bölümündeki Haftanın Günleri bileşeni kaldırılıp Özet ekranındaki Harcama Trendin grafiğinin hemen altına taşındı. Tasarımı ve hesaplama işlevi değişmedi.

Aralık artık Özet'in seçili maaş döneminden alınır. O dönemdeki aynı haftanın günleri toplanır; ortalama alınmaz. Kartlarda yalnız kaydedilmiş günlük artışlar, diğer harcamalar mevcut ortak motorla sayılır. Ekstre ödemesi yeniden alışveriş sayılmaz. Gelecek tarihler ve bekleyen ödemeler mevcut kurala göre hariçtir. Önceki dönem seçildiğinde o dönemin aralığı bileşene aktarılır.

Finans motoru, borç/ödeme verileri, yedekleme, temalar, grafikler, animasyonlar ve önceki düzeltmeler korunur. Üretim kaynak farkı yalnız bileşenin yerinin ve dönem parametrelerinin değiştirilmesidir. Gerçek kullanıcı verilerine erişilmedi.

## Teslim

- `App.jsx`: Zipsiz tek dosya; projede `src/App.jsx` yerine kaydedilir.
- `Finans-1.4.3.apk`: Ana uygulama debug güncellemesi.
- `android/app/build.gradle`: versionName 1.4.3 / versionCode 11.

Uygulama kimliği `com.butcetakip.app`. Önceki teslimle aynı sertifika kullanılır: SHA-256 `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`. Telefonda kurulu sürümün imzası bilinmediği için kurulum uyumluluğu kesin varsayılmaz. Uygulamayı kaldırma/verilerini temizleme; önce JSON yedeğini al. Gerçek cihazda bu sürüm denenmedi.

## Kontroller

Web üretim derlemesi, Capacitor Android senkronizasyonu ve Android debug APK derlemesi başarılıdır. Bu küçük yer değişikliği için ilgili 14 React/tarayıcı ekran testi çalıştırıldı; tam 377 testlik paket bu teslimde tekrar çalıştırılmadı. Üç temada yerleşim, Raporlar Trend'den kaldırılması, 300 TL nakit + 500 TL kart farkının Pazar toplamında 800 TL olması, kayıtların korunması, mevcut ödeme onayı ve Bugün Harcanan davranışları kontrol edildi. Ayrıntılı sonuç `kanitlar/tests.tap` dosyasındadır.

Kaynak ve APK boyut/hash bilgileri `SHA256.json` içindedir. Kaynak geri dönüş kopyası `geri-donus/App.jsx`, eski sürüm yapılandırması `geri-donus/build.gradle` dosyasındadır. Telefona daha düşük sürüm zorla kurulmaz; gerekirse aynı imzayla daha yüksek sürüm kodlu geri dönüş derlenir.
