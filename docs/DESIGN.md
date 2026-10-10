# v1.2 tasarım uyarlaması

Kullanıcının paylaştığı `App.jsx`, React/Recharts ile hazırlanmış bir finans arayüzü. Yapısı, özet/işlemler/ödemeler/analiz bölümleri, yarım daire harcama göstergesi, tema değişkenleri, hızlı giriş, taksit/kredi ve maaş ekranları incelendi. Android uygulamasına seçilmiş tasarım fikirleri ve kullanım kolaylıkları yerel Java/Canvas bileşenleriyle uyarlandı. Dosyanın kendisi projeye eklenmedi; React, Recharts, web fontu, localStorage veya Capacitor gereksinimi oluşmadı.

## Uygulananlar

- Lacivert/teal/indigo palet; açık temada beyaz yüzeyler, ince kenarlar ve tutarlı boşluklar.
- Gradyan net varlık kartı; boyuta uyarlanan tutarlar ve önceki dönemle karşılaştırmalı gelir/gider kartları.
- Yarım daire harcama göstergesi ve tasarruf hedefi ilerlemesi. Gösterge yayı %100'de dolar; sayı %100 üzerindeki gerçek oranı göstermeye devam eder. Gelir yoksa yüzde üretilmez.
- Yatay hesap kartları ve hesap detayları; bugünkü bakiye ile maaş dönemi gelir/gideri ayrı etiketlenir.
- Yaklaşan faturaların tarih ve durumları; kart ve faturaları yöneten ayrı ödeme merkezi.
- Ana ekranda kısa asistan özeti; ayrıntılı yorumların ayrı ekranı. Analizde yorumlar açılıp daraltılabilir.
- Maaş döneminin tüm günlerini gösteren harcama haritası. Güne dokununca kayıtlar açılır; ekran okuyucular için tutarlı bir günlük liste de bulunur. Gelir, transfer ve henüz gerçekleşmemiş giderler harcama rengine dahil edilmez.
- Son giderlerden hızlı tekrar ve işlem satırına basılı tutarak tekrar ekleme. Önceki kayıt korunur, yeni işlem formu açılır; Kaydet'e basılana kadar gider oluşmaz. Fatura ödemeleri ödeme merkezinden yönetilir.
- Unicode sembolleri yerine yerel çizgi ikonları; dokunma geri bildirimi, yuvarlatılmış alanlar ve okunabilir başlıklar.

## Korunan hesap kuralları

Örnek dosyadaki `maasDonemiBul` gün değerini 28 ile sınırlar; bizde kullanıcı 1–31 seçebilir, kısa ayda son gün kullanılır ve sonraki ay tekrar tercih edilen güne dönülür. `donemHarcamaOrani` yüzdeyi 100 ile sınırlar ve birikimi kullanım toplamına ekler; bizde harcanan gelir oranı yalnızca gider/gelirdir, %100'ü aşabilir ve tasarruf ayrı gösterilir. Kart alışverişi gider, kart ödemesi transfer olmaya devam eder. Net varlık, harcanabilir banka bakiyesi veya kredi notu olarak etiketlenmez.

Taksit/kredi ve maaş zammı ekranları bu sürüme taşınmadı. Bunlar yalnızca görsel bileşen eklemekten daha fazla veri modeli ve muhasebe kararı gerektirir. Mevcut aylık faturalar ve düzenli işlemler korunur. Yeni görünüm kalıcı veritabanı şemasını değiştirmez; v1.1 kayıt ve JSON v2 yedekleri uyumludur.

## Erişilebilirlik ve uygulama yapısı

İkonlar dekoratif çizimlerdir; tıklanabilir alanlar adlandırılır. Alt gezinme ve ana eylemler en az 48 dp dokunma alanı kullanır. Harcama göstergesinin sayı ve metin karşılığı, haritanın günlük listesi, grafiklerin tutar listesi vardır. Tutar kartları metni mevcut genişliğe uyarlar. Açık/koyu tema tercihi korunur. Çizimlerde yollar ve yay geometrisi tekrar kullanılır; çizim sırasında yeni dizi/Path oluşturulmaz.

`Design.java` yerel ikon, gösterge ve harita; `MainActivity.java` ekranlar; `Finance.java` günlük gider toplama ve bağımsız tekrar taslağı oluşturma işlemlerini içerir. İnternet izni veya yeni çalışma zamanı bağımlılığı eklenmedi.

## v1.3 devamı

Kart, dönem bütçesi ve gösterge ortak `Finance.spendingBand` eşiklerini kullanır: %50 altı yeşil, %50–80 sarı, %80+ kırmızı; gelir yoksa nötr. Gauge açılışta bir saniye animasyon yapar; Android animasyonları kapalıysa doğrudan final değeri gösterir ve ayrılırken animator iptal edilir. Üst özet daha kompakt hale getirildi.

Her kredi kartı özette ayrı yüzey, üç tutar kutusu ve son güncelleme tarihiyle gösterilir. Kullanıcı bankadaki tutarları elle girer. Birbirini içermeyen kalan ekstre/dönem içi/provizyon toplamı yalnızca banka referansıdır; SQLite gelir/gideri veya kayıtlı net bakiyeyi değiştirmez. Yeni kart hesabı ile metadata aynı transaction'da oluşur. Kart takibini silme ve finansal hesabı/bağlı kayıtlarını silme farklı akışlardır; kapsam onay ekranında görünür. Yedek JSON v3, eski v1/v2 okuma ve doğrulama korunur.

Web için çalışan referans ve uzun vadeli mimari: [Altı bölümlük rehber](ARCHITECTURE.md). React refactoring'i yerel Android kaynaklarıyla veya bankayla otomatik senkronizasyon olarak sunulmaz.

## v1.4 devamı

Mint cüzdan + altın coin marka simgesi Android adaptive foreground/background olarak ayrıldı; tema simgesi için tek renk vektör eklendi. Finans araçları Ayarlar'da tek kartta: krediler, maaş hesaplayıcı, konuya özel yardım. Kredi ödeme eylemi tek açık karar sorar: **Bakiyeden düşülsün mü?** Evet gider, Hayır yalnızca takip. Kredi kartında takip edilen ödeme sayısı ayrıca açıklanır.

Harcama takvimi drilldown'ı modal yerine aynı kart altında gösterilir: seçili tarih → gelir/gider/gün dengesi → varsayılan kapalı detay listesi. Açılır gün seçici Canvas haritasının erişilebilir alternatifidir. İşlem ve gün detayları 50, ödeme kartları 20 kayıtla sınırlıdır. Arka plan sorguları ve gecikmeli arama ekranda gezinmeyi kilitlemez.

Bildirim kartı izin durumunu açık gösterir; aç/kapat, gün öncesi ve saat seçimi ile deneme bildirimi içerir. Kilit ekranında ödeme tutarı yerine genel hatırlatma görünür. Maaş Hesapla/Zam Oranları sekmeleri App.jsx akışını izler; girişler tahmini anında günceller. Kaydet yalnızca maaş ayarlarını ve oranları saklar, finansal kayıt oluşturmaz.
