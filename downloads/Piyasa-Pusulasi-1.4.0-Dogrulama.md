# Piyasa Pusulası1.4.0 — doğrulama

## Tamamlanan testler
- 43 Node veri/hesaplama testi geçti: önceki34 ve9 ücretsiz kaynak testi (dönemler/URL; ham satır ve oranlar; null/negatif payda/çift eşleşme; banka ve ara dönemde model sınırı; boş sonuçta grup alternatifi ve403'ü aşmaya çalışmama; günlük kapanış göstergeleri; eski/çelişkili/bozuk günlük seri reddi; CSV formül güvenliği).
- 29 tarayıcı kontrol grubu geçti:15 genel,4 veri kapsamı,5 AI/arka plan yenileme,5 yeni ücretsiz veri akışı. Yeni akışlar: Yahoo503 olsa da bağımsız mali tablo/ham tablo arama/oranlar/CSV kopyalama; alternatif kapanış çizgisi; dönem değişiminde önceki tabloyu temizleme ve ara dönem modelini engelleme; kaynak403'te önceki tabloyu kaldırıp açık JSON içe aktarımını etiketleme; sembol değişiminde gecikmiş yanıtı yok sayma ve banka modelini sınırlandırma. Mobil390px yatay taşma ve yakalanan JavaScript sayfa hatası yok. Ekran görüntüleri kontrollü veridir, gerçek şirket rakamı değildir.
- 16 native HTTP sınır vakası geçti: önceki10 ve yeni İş Yatırım host/HTTP/kimlik/port/yol/POST reddi. Gerçek ağ veya anahtar kullanılmadı. Clipboard çağrısı tarayıcıda sahte köprüyle sınandı; fiziksel Android clipboard testi değildir.
- Üretim web derlemesi ve Android assembleRelease/lintRelease başarılı. İmza doğrulandı ve önceki dağıtımla aynı. Paket tr.piyasapusulasi.mobile, versionName1.4.0/versionCode6. Lint0 hata; önceki2 uyarı (yerel React JavaScript ve alarm commit yazımı).

## Gerçek erişim kanıtı ve sınırlar
PyPI metadata ve kaynak arşivleri için HTTPS200 alındı: borsapy0.11.0 ve isyatirimhisse5.0.1. Kaynaklar çalıştırılmadan okundu; üçüncü taraf Python kodu uygulamaya paketlenmedi. MaliTablo istek/yanıt biçimi ve HisseTekil günlük veri protokolü bu kaynaklarda incelendi. Yeni JS okuyucu bu arayüzleri kullanır.

İş Yatırım ana sitesi ve MaliTablo adresi, Yahoo, TradingView scanner, Stooq ve alternatif kur servisi bu bulut ağında CONNECT403 ile engellendi. Yükseltilmiş read-only ağ denemesi de İş Yatırım'a erişemedi. Kısıtlama kaldırılmadı ve üretimde aracı proxy/erişim engeli aşma eklenmedi. Kaynak paketin belgeleri canlı yanıtın bugün aynı biçimde olacağını veya finansal kapsam/lisansı garanti etmez. Gerçek şirket mali verisi veya fiyat bu oturumda alınamadı. Uygulama bu adresleri telefonun normal internetinden doğrudan dener; API anahtarı ve abonelik istemez.

Bu APK fiziksel cihaz veya emülatörde çalıştırılmadı. İş Yatırım canlı verisi, mevcut Yahoo akışı, Gemini, Android clipboard ve bildirim teslimi cihazda doğrulanmadı. Testlerdeki fiyat/tablolar yalnız test dosyalarındadır; üretim paketine örnek finansal veri yerleştirilmedi.

## Ürün davranışı ve yöntem
Ana sayfada “Ücretsiz şirket verisi” bağımsız ekranı ve BIST analizinde aynı okuyucu bulunur. MaliTablo4 yıllık veya4 tamamlanmış çeyrek dönemi sorar. Tamamlanmış dönem yayınlanmış demek değildir: boş dönem atlanır, gerçek satır yoksa hata verilir. Mali tabloların kaynağı ve sorulan dönemler görünür; yanıt zamanı yayınlanma zamanı sayılmaz. Ara dönem gelir/nakit akışı YTD'dir. Getirme hatasında önceki mali tablo yeniymiş gibi gösterilmez.

Kesin etiketler/kod önekleriyle eşleştirme yapılır; çift eşleşen veya bilinmeyen satır tahmin edilmez. Gösterilebilen oranlar aynı dönem içindeki net/brüt marj, özkaynak/aktif, cari oran ve pozitif net kâra göre nakit kalitesidir. Bankalarda sanayi tipi marj/cari oran/model uygulanmaz. Kaynağın tutar ölçeği, enflasyon/konsolidasyon ve yeniden düzenleme temeli yanıttan doğrulanamadığı için F/K, PD/DD ve hedef fiyat hesaplanmaz.

Yıllık finansal koşullar için üç ardışık dönem ve ortalama aktif kullanılır; uzun yükümlülük toplamının finansal borç olmadığı açıklanır. Pay ihracı yok varsayılmaz. Resmi toplam F-Score veya keyfi finansal sağlık puanı yoktur; yalnız mevcut koşullar gözlemsel olarak gösterilir.

HisseTekil yolu yalnız şirket kodu/tarih/kapanış sütunlarını kabul eder. En az20 gözlem, pozitif fiyat, en fazla7 günlük son kapanış; çelişkili aynı gün veya bozuk tarih reddedilir. SMA20/50/200, RSI14, MACD12/26/9 kapanışlardan hesaplanır. Açılış/yüksek/düşük/hacim uydurulmaz, bu yolun fiyatı canlı/alarm kaynağı yapılmaz.

Kaynak yanıtı tarayıcıda açılabilir. Kullanıcı açıkça JSON içe aktarırsa şirket/dönemi kendisinin doğrulaması gerektiği ve verinin kullanıcıdan geldiği yazılır. İçe aktarılan veya doğrudan okunan mali tablolar bu sürümde kalıcı kaydedilmez. CSV metni panoya kopyalanır; satır adlarında formül başlangıcı etkisizleştirilir. API anahtarları hiçbir zaman bu kaynağa gönderilmez; HTTPS sertifika kontrolü kapatılmaz.

Önceki AI/scroll düzeltmesi, favori, tema, anahtar, alarm ve arşiv biçimleri korunur. Mevcut uygulamayı kaldırmadan üzerine kurun. Ücretsiz kaynakların sürekliliği ve güncel kullanım koşulları bağımsız olarak doğrulanamadı.
