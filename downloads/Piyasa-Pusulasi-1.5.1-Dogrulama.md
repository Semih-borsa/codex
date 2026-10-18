# Piyasa Pusulası1.5.1 — analist404 düzeltmesi

- 56 Node veri/hesaplama testi geçti; tarih, öneri sınıfları, hedef birimi, kurum bazında en yeni kayıt ve gerçek medyan kontrolleri dahil.
- 24 tarayıcı kontrol grubu geçti:15 genel uygulama ve9 analist akışı. Yeni404 senaryoları: doğru şirket kimliği/rapor yapısı olan404 içerik uyarıyla okunur; gerçek dizin404 sonrasında belgelenmiş şirket araması çalışır; gerçek404 hata şablonu rapor sayılmaz; başka şirketin raporuna benzeyen404 içerik reddedilir. Önceki5 analist güvenlik/veri/istek iptali vakası ve genel JSON/arayüz akışları geçti. Kontrollü yanıtlar kullanıldı; JavaScript sayfa hatası yok.
- 30 native kontrol geçti:23 URL/metot sınırı ve7 sahte HTTPS bağlantı vakası. Native test404 getErrorStream gövdesinin ve durumunun korunmasını, boş404, izinli arama, ek query reddi, Yahoo404 reddi, analist403 reddi ve200 yanıtını kapsar. Gerçek ağ/anahtar kullanılmadı.
- Web üretim derlemesi, Android assembleRelease ve lintRelease başarılı. APK imza ve sürüm kontrolü yapıldı. Paket tr.piyasapusulasi.mobile, versionName1.5.1/versionCode9. Önceki APK ile aynı imza; lint0 hata ve önceki2 uyarı.

## Somut düzeltme
Önceki uygulama404 gövdesini okumadan kesiyordu. Analist kaynağı için HTTP durumu ve HTML gövdesi birlikte korunur.404'ün tamamı başarılı sayılmaz: dizinden eşleşen hisse adresi çıkarılmalı; hisse404'ünde title/h1/h2 içinde doğru hisse kimliği ve tanınan rapor tablosu olmalıdır. Bu kontroller içerik özgünlüğünün bağımsız kanıtı değildir; üçüncü taraf kaynağıdır. Kabul edilen404 içerik için durum uyarısı görünür.

Dizin adresi bulunamazsa aynı sitenin `arama?q=SEMBOL` yolu bir kez denenir. Standart1–12 harf/rakam dışında sorgu veya ekstra parametre kabul edilmez. Gerçek404 sayfası ve değişmiş yapı açık hata verir. Mesaj hangi aşamada sorun olduğunu belirtir.403/401/kota/5xx veya diğer sağlayıcıların404 yanıtları gevşetilmez. API anahtarları analist sitesine gönderilmez; TLS kontrolü ve boyut sınırı korunur.

## Canlı doğrulama sınırı
Bu oturumda senetler ve arama adresleri yeniden denendi; bulut ağında CONNECT403 alındı. Kullanıcının gördüğü gerçek404 gövdesi henüz görülmedi. Bu nedenle cihazdaki hatanın kesin sebebi veya canlı rapor erişiminin düzeldiği doğrulanmış değildir. Düzeltme testlerde yeniden üretilen bir404 taşıma sorununu giderir; kaynak gerçekten kaldırılmışsa rapor üretmek yerine açık hata kalır. Bu APK fiziksel cihaz/emülatörde çalıştırılmadı. Gerçek analist hedefleri, canlı fiyat, Gemini ve bildirim teslimi doğrulanmadı.

Kayıt biçimi, hesaplama modelleri ve mevcut favori/tema/anahtar/alarm/arşiv verileri değiştirilmedi. Aynı paket/imza ile mevcut uygulamayı kaldırmadan üzerine kurun. Üretime örnek analist raporu eklenmedi.
