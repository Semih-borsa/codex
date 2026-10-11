# v1.6 · Birleşik finans kartı ve ödeme özeti

Net varlık ve dönem bütçesi ortak bir yüzeyde birleştirildi. Risk rengi ve durum etiketi tüm kartı kapsar; yüzde, ilerleme çubuğu ve dönem dengesi beyaz kontrastla okunur. Hızlı işlemler bu kartın hemen altında yer alır.

Kart bölümünün başında iki toplam kutusu bulunur: kalan ekstre/ödenen tutar ve dönem içi/provizyon dahil. Güncel banka özeti olmayan kartlar sıfır varsayılmaz; toplam dışında tutuldukları belirtilir. Bireysel kartlarda Hesap Kesim Tarihi etiketi kullanılır.

Yaklaşan ödemeler bugünden 7 gün sonraya kadar bekleyen fatura, kredi ve kart ekstrelerini sıralar. Her satır gün sayısını, tutarı ve Ödendi olarak işaretle eylemini içerir. Liste 20 kayıt halinde sayfalanır; ödenmiş, gecikmiş ve daha uzak ödemeler ödeme merkezinde kalır.

![Birleşik ana ekran](screenshots/combined-dashboard-v16.png)

![Birleşik ana ekran · açık tema](screenshots/combined-dashboard-light-v16.png)

![Kısmi ödeme sonrası ekstre toplamı](screenshots/card-totals-partial-v16.png)

![Tam ödeme sonrası ekstre toplamı](screenshots/card-totals-paid-v16.png)

![Haftalık ödemeler](screenshots/upcoming-week-v16.png)

## Önceki sürüm tasarımları

# v1.5 · Modern mobil tasarım

Özet ekranında önce bugünkü net varlık, sonra seçili maaş döneminin gelir/gideri ve harcama oranı görünür. Net varlığın altındaki yaylar dekoratif marka öğesidir; finansal grafik olarak sunulmaz. Kaydırılan içerikten ayrı, sabit bir alt gezinme çubuğu ana bölümleri erişilebilir tutar. Finans araçlarında ilgili üst bölüm de seçili görünür.

| Tasarım değeri | Açık tema | Koyu tema |
|---|---|---|
| Sayfa | `#F4F5F8` | `#0C111B` |
| Kart | `#FFFFFF` | `#171E2B` |
| Ana metin | `#172233` | `#F3F6FA` |
| İkincil metin | `#657286` | `#9AA8BA` |
| Kenarlık | `#E6E8EE` | `#293243` |
| Olumlu/gelir | `#00795D` | `#35D6AE` |
| Gider | `#BF2448` | `#FF7B8B` |
| Transfer | `#5261C9` | `#99ABFF` |

Kartlar 20 dp köşe, ana kart 24 dp köşe, tutarlı 12 dp aralık kullanır. Başlıklar 22 sp, ana bakiye 34 sp ve alan uygunsa otomatik küçültme, hızlı işlem etiketleri 11 sp'dir. Dönem okları en az 48 dp, hızlı işlem alanları en az 64 dp, alt sekmeler 54 dp dokunma alanı taşır. Formlar ve işlem satırları ortak yüzey ve yazı stilini kullanır.

Dönem bütçesi gösterge çubuğu ve net varlık kartı aynı risk eşiklerine bağlanır: %50 altı yeşil, %50–80 sarı, %80 ve üstü kırmızı. Metinsel durum etiketi renk bilgisini tamamlar. Gelir yoksa oran hesaplanmaz. Sayı gerçek yüzdeyi gösterir; ilerleme çubuğu %100'de dolar. Yeni kompakt gösterge Android yazı ölçeğini takip eder, genişliğe sığmayan oranı küçültür ve animasyonlar kapalıysa son değeri doğrudan gösterir. Açılış animasyonu bir saniyedir; ekran ayrılırken durdurulur.

Arka plan çiziminin gradyanı ve Path geometrisi ölçü değiştiğinde hazırlanır; çizim sırasında yeniden dizi/Path oluşturulmaz. Analizde uzun hesap açıklamaları aynı kartın içinde açılıp kapanır. Karşılaştırma verisi yoksa nötr metin kullanılır. Finans sorguları, sayfalama ve veri modeli aynı kalır.

## Emülatörden gerçek ekranlar

![Koyu tema ana ekran](screenshots/dashboard-dark-v15.png)

![Açık tema ana ekran](screenshots/dashboard-light-v15.png)

![İşlem listesi](screenshots/transactions-v15.png)

![Analiz ekranı](screenshots/analysis-v15.png)

## Önceki sürümlerin tasarım kararları

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
