# Piyasa Pusulası1.4.1 — doğrulama

- 50 Node testi geçti: önceki43 ve7 yeni finansal değerlendirme testi. Kısmi7 olumlu/2 eksik; gerçek finansal borç satırıyla8 koşula çıkma; sıfır/zarar/eksik ayrımı; sağlıkta90 mevcut ağırlığın100'e ölçeklenmemesi; üçüncü bilanço eksikken bağımsız koşulların korunması; banka/ara dönem model sınırı; negatif borcun sağlıklı azalma sayılmaması.
- 6 mali tablo tarayıcı kontrol grubu geçti. Önceki okuyucu/oran/tablo/CSV, kapanış, dönem, JSON içe aktarımı, banka ve gecikmiş yanıt senaryoları; yeni sağlanan/sağlanmayan/eksik koşulların aynı ekranda görünmesi, kaynak kanıtlarının açılması, sağlıkta eksik ağırlığın gösterilmesi, gelen/eksik kalemler tablosu ve390px taşma kontrolü dahil. Kontrol edilmiş yanıtlar kullanıldı, JavaScript sayfa hatası yok. Önceki diğer tarayıcı senaryoları bu sürümde yeniden çalıştırılmış sayılmaz.
- Web üretim derlemesi ve Android assembleRelease/lintRelease başarılı; paket/sürüm ve imza kontrol edildi. Paket tr.piyasapusulasi.mobile; versionName1.4.1/versionCode7. Önceki APK ile aynı sertifika. Lint0 hata, önceki2 uyarı (yerel React JavaScript ve alarm commit yazımı).

## Gösterim
Mali tablo alındığında F-Score koşulları açık panelde listelenir: yeşil sağlanan, kırmızı sağlanmayan, sarı eksik/doğrulanamayan. Her koşul açılarak cari/karşılaştırma değeri, formül, dönem, kaynak satırı ve eksik kalem görülebilir. Pay ihracı ve dönem karşılaştırılabilirliği doğrulanmadığından toplam resmi F-Score değildir. Uzun yükümlülük toplamı gerçek finansal borç yerine kullanılmaz.

Finansal sağlık uygulamaya özgü şeffaf kontrol listesidir. Ağırlıklar kârlılık30, borç/sermaye30, büyüme20, likidite20. Net marj/CFO pozitifliği; özkaynak-aktif>=40%, toplam yükümlülük-aktif<=60%; nominal/reel hasılat büyümesi; cari oran>=1, nakit/pozitif net kâr>=1. Eşikler ekranda yazılıdır; model doğrulanmış kredi notu değildir. Reel büyüme için uygun enflasyon/yeniden düzenleme temeli bulunmadığından ilgili ağırlık bilinmez. Eksik ağırlık sıfır sayılmaz, kalan puan100'e ölçeklenmez ve tam /100 sağlık notu gösterilmez. Bankalarda sanayi modeli yoktur. Ara dönem verileri yıllık F-Score'a dönüştürülmez.

Kaynak ham ölçeği ve karşılaştırılabilirlik sınırları gösterilir. İçe aktarılan şirket/dönem kullanıcıdan gelir; bağımsız doğrulama iddiası yoktur. Sıfır, zarar ve eksik birbirinden ayrılır. Kaynak gelmezse şirket verisi üretilmez. Örnek finansal sayılar yalnız test dosyalarındadır; üretim verisi olarak eklenmedi.

## Canlı test sınırları
Yeni bir sağlayıcı bağlanmadı; mevcut İş Yatırım okuyucusu kullanılıyor. Canlı kaynak erişimi önceki oturumda CONNECT403 ile engellenmişti. Bu sürümde gerçek mali veriler, fiziksel Android/emülatör çalışması, clipboard, canlı fiyat/Gemini/bildirim teslimi doğrulanmadı. Finansal sağlık modeli ve teknik puanlar tarihsel başarı/olasılık kalibrasyonu görmedi.

Favori, tema, API anahtarı, alarm ve arşiv biçimi değiştirilmedi; kayıtları silen bir işlem eklenmedi. Mali tablo okuma/hesaplama oturum içindedir. Mevcut uygulamayı kaldırmadan üzerine kurun.
