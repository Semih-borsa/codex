# Portföyüm 4.8.3 — mavi kartlar ve tema tutarlılığı

## Değişiklikler

- Mevduat giriş bilgi kartı açık/koyu temada mavi tonlara alındı.
- Mevduat hesabı ekleme düğmesi mobilde 44×44 piksel; artı SVG'si 20×20 piksel ve kare içinde ortalı. Gizlenen metin ayrı span içinde; erişilebilir düğme adı korunur.
- Kurlar sayfasındaki banka altın/gümüş fiyat paneli ve mobil banka satırları mavi yüzey kullanır.
- Geçmişte hesap hareketleri ve kapanmış kayıtlar panelleri, satırlarıyla birlikte açık/koyu temada mavi yüzey kullanır.
- Diğer mobil tablo satırlarının koyu modda sabit beyaz kalması giderildi. Varlık kâr/zarar renkleri korundu.
- Koyu modda faiz durum metinleri, kaynak rozetleri, uyarılar, faiz bilgi şeridi, masaüstü yerel-kayıt etiketi ve fiyat yenileme butonunun hover rengi düzeltildi.
- Mobil üst bölümde yalnız simge gösteren düğmelerin boş metin aralığından kaynaklanan hizası düzeltildi.

## Korunanlar

Paket adı `com.semih.portfoyum`, depolama anahtarları, finans hesapları, geçmiş kayıtlar, yedek sistemi ve 4.8.2 grafik seçicisi korundu. Yeni bağımlılık/veritabanı eklenmedi. Sürüm 4.8.3 / 40803. Gerçek kullanıcı kaydı kullanılmadı veya değiştirilmedi. 4.8.2 kaynak geri dönüş kopyası ayrıca saklandı; önceki teslim repoda bulunur.

## Değişen kaynaklar

- `app/src/main/assets/app.mjs`: ilgili kart sınıfları, erişilebilir artı düğmesi, sürüm yazısı.
- `app/src/main/assets/modern.css`: mavi yüzeyler, mobil/dark satırlar, okunabilirlik ve simge hizası.
- `app/build.gradle`: yalnız sürüm yükseltme; applicationId aynı.
- `tests/ui.test.mjs`: iki yeni arayüz kontrolü.

Finansal hesapların bulunduğu `core.mjs`, fiyat sağlayıcıları, native Java dosyaları ve eski yedekler değiştirilmedi.

## Doğrulama

- 132/132 JavaScript testi başarılı (önceki 130 test + 2 yeni test); silme/atlama/gevşetme yok.
- Chromium: 360, 390, 1024 piksel genişlik × 2 tema × 6 ekran = 36 kontrol. Yatay taşma/JavaScript hatası yok; koyu modda görünür opak beyaz yüzey taramasında hata yok.
- Ek 12 mevduat/ayrıntı penceresi kontrolü başarılı. Mobil artı simgesinin düğme merkezinden sapması en fazla 1 piksel; düğme 44×44. Arayüz gezinmesinde finans kayıtları değişmedi.
- Grafik seçicisi: iki tema × iki grafik türü × üç aralık = 12 gerçek Chromium tıklama kontrolü yeniden başarılı; finans kayıtları değişmedi.
- Android debug derlemesi başarılı (JDK 17 / SDK 35 / AGP 8.9.2 / mevcut Gradle 8.14.3). Orijinal 8.11.1 wrapper korunur. Gerçek Android cihazda doğrulanmadı.

## Kurulum / geri dönüş

ZIP bütün proje kaynaklarını içerir. Android Studio'da içteki proje klasörünü açıp önceki APK ile aynı imza anahtarını kullanarak derleyin. Paket adının aynı olması tek başına imza uyumluluğu garantisi değildir. Önce JSON yedeğinizi alın; uygulamayı kaldırmayın/verisini temizlemeyin. Eski kaynakla geri dönüş yapılacaksa aynı imza ve kurulu sürümden yüksek versionCode gerekir.

## Sonraki adım

Kullanıcı tercihi: karşılaştırma için API anahtarı gerektirmeyen yöntem araştırılacak. Bu sürüme doğrulanmamış otomatik veri entegrasyonu veya tahmini tarihsel veri eklenmedi. Ayrı `ANAHTARSIZ_KARSILASTIRMA_ON_INCELEME.md` dosyasında araştırma sınırları ve aday yöntemler bulunur.
