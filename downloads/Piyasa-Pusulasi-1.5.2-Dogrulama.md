# Piyasa Pusulası 1.5.2 — canlı analist kaynağı düzeltmesi

16 Ekim 2026 tarihinde ağ erişimi açıldıktan sonra gerçek kaynak yanıtları incelendi.

## Sorun ve çözüm
- Ana sayfa HTTP200, eski /senetler ve /arama?q=THYAO yolları HTTP404 döndürüyor. Önceki sürümün keşif yolu çalışmıyordu.
- Sitenin herkese açık arama arayüzünün kullandığı POST /ajax/senet-ara.php yolu doğrulandı. Yalnız kelime=SEMBOL&auth=1 formu gönderilir. Buradaki auth=1 sitenin açık arama formunun sabit alanıdır; kullanıcı kimliği/anahtar/oturum açma değildir. Erişim engeli aşılmaz.
- Native istemci doğru form Content-Type ve kaynak Referer başlığını gönderir. URL, metot ve form alanları dar izin listesiyle doğrulanır; TLS, yanıt boyutu sınırı ve diğer sağlayıcı kontrolleri korunur.
- Güncel şirket sayfaları raporları HTML tablosu yerine .senetbox kartlarıyla sunuyor. Okuyucu, kartın şirket adresini eşleştirip kurum başlığı, görünen tavsiye metni, açık hedef/birim ve Türkçe yayın tarihini okur. Reklamlar ve başka şirket kartları alınmaz; renklerden tavsiye çıkarılmaz.
- Analist bölümü açılınca şirket kodu 700 ms sabit kaldıktan sonra otomatik sorgu yapılır. Yeniden dene düğmesi korunur. Hisse değişikliği eski isteği geçersiz kılar. HTML/adres yapıştırma arayüzü kaldırıldı.
- Tarihsiz/eski raporlar ayrı tutulur; eksik veri tahmin edilmez. Başarısız sorgu önceki şirketin/isteğin raporlarını temizler ve anlaşılır hata gösterir.

## Doğrulama
- THYAO, ASELS, EREGL için gerçek açık arama ve şirket sayfaları HTTP200 ile alındı. Tam gerçek HTML üzerinde okuyucu sırasıyla 35,31,25 rapor satırı okudu. Bu sayılar kaynağın döndürdüğü sayfalara aittir; tüm piyasanın kapsamı değildir.
- Kaynaktan alınan üçer kartlık küçük test örnekleriyle 5 tarayıcı kontrol grubu geçti: üç şirket otomatik okuma, somut kurum/tarih/hedef eşleştirmesi ve yanlış şirket reddi, gerçek404/önceki veriyi temizleme/eski yolları çağırmama. Örnek: THYAO Citi Bank, 14 Ekim 2026, Nötr,300TRY. Örnekler yalnız test klasöründedir, APK verisi değildir.
- Basit arayüz hata/yeniden deneme kontrolü geçti. AI beklerken yenileme/scroll için mevcut 5 tarayıcı regresyon grubu geçti.
- 56 Node veri/hesaplama testi geçti. 23 native URL/metot ve 7 HTTP404 kontrolü geçti; yeni native form/başlık/Referer ve hatalı form/metot ret testleri geçti.
- Web build, Android assembleRelease/lintRelease geçti. APK imzası doğrulandı; aynı SHA256 sertifikası cfe01f4f5b5a6c0b6fc67d2c237b21f21311781d9ded1fe521df1ea005ec8c43. Paket tr.piyasapusulasi.mobile; sürüm1.5.2/kod10.

## Sınırlar ve verilerin korunması
Canlı HTTPS erişimi ve gerçek HTML okuyucusu bu ortamda doğrulandı; APK fiziksel telefonda çalıştırılmadı. Native istek protokolü kontrollü bağlantılarla sınandı. Site gelecekte değişebilir veya erişimi sınırlayabilir. Raporların özgün kurum metniyle bağımsız doğrulaması yapılmadı; kaynak bir derleme sitesidir.
Hesaplama modelleri, depolama anahtarları, favoriler, arşivler, API anahtarları ve alarmlar değiştirilmedi. Önceki uygulamayı silmeden aynı imzalı APK'yı üzerine kurun.
