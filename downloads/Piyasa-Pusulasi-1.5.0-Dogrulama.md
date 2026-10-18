# Piyasa Pusulası1.5.0 — doğrulama

- 56 Node testi geçti: önceki50 ve6 analist testi. İstanbul gününe göre gerçek/geçersiz/gelecek rapor tarihi; yalnız aynı host ve doğru hisse adresi; açık tavsiye sınıfları; hedef birimi/sayı belirsizliği; kurum başına en yeni rapor ve gerçek medyan; çelişki/tekrar/yabancı para ayrımı.
- 26 tarayıcı kontrol grubu geçti:15 genel uygulama,6 mali tablo ve5 yeni analist akışı. Analist akışları: HTML taşıma/kurum/tarih/tavsiye/ortalama-medyan/eski rapor ayrımı/script çalışmaması/390px taşma; erişim hatasında eski raporu kaldırma ve etiketli içe aktarım; dış host veya yanlış şirket adresi reddi; HTML yapısı değişince açık hata; şirket düzenlenince gecikmiş yanıtı yok sayma. Kontrollü HTML ve fiyat yanıtları kullanıldı; gerçek kurum raporu testi değildir. Yakalanan JavaScript sayfa hatası yok.
- 23 native HTTP sınır kontrolü geçti: önceki16 ve7 yeni host, HTTP, kullanıcı bilgisi, port, query, yol ve POST reddi. Ağ çağrısı veya gerçek anahtar kullanılmadı.
- Web üretim ve Android assembleRelease/lintRelease başarılı; imza ve paket/sürüm kontrol edildi. Paket tr.piyasapusulasi.mobile; versionName1.5.0/versionCode8. Önceki sürümlerle aynı imza. Lint0 hata ve önceki2 uyarı (yerel React için JS, alarm commit yazımı).

## Yeni panelin kapsamı
Ana sayfa Ücretsiz şirket verisi veya BIST analizi içinde Analist görüşlerini getir düğmesi kullanılır. Hedef Fiyat derleme sitesinin şirket listesi ve eşleşen şirket sayfası okunur. Kurum, kaynak tarihi, tavsiyenin orijinal ifadesi, hedef sayı/birim ve kaynak bağlantısı görünür. Bu bilgi uygulamanın kendi AL/SAT/TUT kararı veya birincil kurum raporunun bağımsız doğrulaması değildir.

Son90/180 gün içinde kurum başına en yeni tarihli ve çelişkisiz kayıt özete alınır. Göreli endeks tavsiyeleri ve nötr ifadeler ayrı sayılır. Eski, tarihsiz, bozuk/gelecek tarihli ve çelişkili kayıtlar ayrı gruplardır. Alınma saati yayın tarihi olmaz. Hedef vadesi ve kurumsal aksiyon düzeltmeleri bilinmez. Ortalamada yalnız açık TRY birimli hedefler kullanılır; gerçek medyan ortadaki gözlem(ler)den hesaplanır. Bu bütün piyasanın uzlaşısı değildir ve fiyatın hedefe ulaşacağı anlamına gelmez.

Sayfa HTML'si uygulamaya eklenmez, script çalıştırılmaz. Strict tanınan tablo başlıkları yoksa hedef/tavsiye üretilmez. Kaynak hatasında önceki raporlar yeni veri diye tutulmaz. Manuel HTML ithali açıkça kullanıcıdan/bağımsız doğrulanmamış diye etiketlenir; host/şirket adresi uyuşması içerik özgünlüğünü kanıtlamaz. Şirket değişimi bekleyen raporu geçersiz kılar. API anahtarları derleme sitesine gönderilmez.

## Canlı erişim sınırı
16 Ekim2026'da Hedef Fiyat senetler sayfası ve İş Yatırım şirket kartı bu bulut ağında CONNECT403 ile engellendi. borsapy0.11.0 kaynak kodundaki açık adres keşfi protokolü incelendi; gerçek kaynak HTML tablosu doğrulanamadı. Bu nedenle telefondaki canlı rapor erişimi ve sitenin güncel tablo yapısı garanti edilemez. Parser yanlış anlamak yerine hata verir; kaynak normal tarayıcıdan da açılabilir. Erişim/ücret duvarı veya CAPTCHA aşma eklenmedi. Örnek raporlar yalnız test dosyasındadır, üretime piyasa verisi diye paketlenmedi.

Bu APK cihaz/emülatörde çalıştırılmadı. Canlı fiyat, broker hedeflerinin güncelliği/özgünlüğü, Gemini ve bildirim teslimi doğrulanmadı. Yeni analist paneli bir yatırım önerisi modeli değildir. Mevcut favori, tema, anahtar, alarm ve arşiv biçimleri değişmedi; üzerine kurulum için aynı paket/imza korunur.
