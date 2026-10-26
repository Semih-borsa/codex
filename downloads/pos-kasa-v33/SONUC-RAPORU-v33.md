# POS Kasa Yönetim Build v33 — 1.2.0

## Yapılanlar
- Onaylanan görsele uygun POS Gelir Analizi modülü Tarih Aralığı Toplamı'nın üzerine eklendi. Altı filtre, iki tarih seçimi, ince SVG çubuklar (20 birim), haftanın günleri/tutar listesi, toplam ve günlük ortalama bulunur. Mevcut IBM Plex Mono yazı tipi, küçük yazı boyutları ve koyu renk düzeni kullanıldı. Yeni grafik bağımlılığı eklenmedi.
- Haftalık yorumlar bugün dahil son 7 gün / önceki 7 gün; aylık POS yorumları ayın başından bugüne / önceki ayın aynı gün aralığı üzerinden hesaplanır. Örnekte 20–26 Ekim / 13–19 Ekim ve 1–26 Ekim / 1–26 Eylül. Kısa aylarda önceki ayın son günü sınırdır. “Geçen ay” filtresi önceki ayın tamamını gösterir.
- POS banka başlığının sağındaki yinelenen toplam ve ona bağlı gösterge kaldırıldı. Üst ana toplam, kredi/debit ve kasa tutarları korunur.
- Aylık ödemeler son ödeme ayına göre açılır gruplara ayrıldı. Yalnız mevcut ay başlangıçta açıktır. Ödeme tarihini değiştirmek kaydı ilgili aya taşır; ödeme durumu, kimlik ve tutar korunur.
- Kısa gider yorumları, yıllık en yüksek/en düşük ay ve en fazla artan/azalan eşleşen ödeme adları eklendi. Ekrana hesaplama yöntemi/açıklama notu yazılmadı.
- Yeni alanlar ve mevcut alanlarda gün-ay-yıl gösterimi korunur. Electron'a Türkçe dil tercihi eklendi; yerel Electron'da uygulama dili `tr`, Türkçe Chromium ekranında `26.10.2026` ve `gg.aa.yyyy` görünümü doğrulandı. Kayıtlardaki ISO tarihler ve hesaplama mantığı dönüştürülmedi.

## Hesap sınırları
- POS analizi, mevcut Takip ve Tarih Aralığı Toplamı gibi **yatış tarihlerini** kullanır. Kredi/debit aynı mevcut işlem listesinden toplanır; yeni işlem veya bakiye motoru oluşturmaz.
- Haftanın bir gününün ortalaması: aralıktaki o güne ait tutar / aralıkta o haftalık günün takvimde kaç kez bulunduğu. Örneğin Ekim'in ilk 26 günündeki pazartesi toplamı dört pazartesiye bölünür. Aralıkta hiç bulunmayan gün “–”; bütünüyle verisiz aralık “Kayıt yok” gösterilir. Bu hesaplar kayıt yazmaz.
- Ödeme yorumları son ödeme ayına ait kayıtlı ödemelerin, ödenmiş ve bekleyenler birlikte, aylık toplamlarını kullanır. Yılın en yüksek/en düşük ayı hesaplanırken gelecekteki aylar dışlanır; verisiz aylar sıfır gider varsayılmaz.
- En fazla artan/azalan, iki ayda da bulunan tam ad eşleşmelerinin toplamlarındaki yüzdesel değişimdir. Adlarda yalnız büyük/küçük harf ve boşluk farkları normalleştirilir. Benzer isimler tahminle birleştirilmez. Eksik karşılaştırma ve sıfır tabanda yanıltıcı yüzde gösterilmez.
- Ödeme formunda tutar ve tarih doğrulaması ortak güvenlik yardımcısını kullanır. Hatalı/negatif tutar ve başarısız yazmada taslak korunur; mevcut ödeme kayıtları değişmez.

## Testler
- Mevcut **35 güvenlik/dosya testi** başarılı.
- Önceki **114 hesap kontrolü** ve yeni **46 kontrol** ile **160 hesap kontrolü** başarılı. JSX derlemesi başarılı.
- Yeni kontroller: verilen haftalık tarih sınırları; aynı günlü aylık kıyas; yıl/ay geçişi ve artık yıl; takvim günü sayıları ve haftalık ortalamalar; geçersiz/ters aralık reddi; ödeme ay grupları, bekleyen toplamı, yıllık sıralama, aynı adlı ödemelerde en fazla artış/azalış, eksik veri ve kaynak nesnelerin korunması.
- Yeni Chromium testi: bütün filtreler ve özel aralık; 270.000 TL toplam, 38.571,43 TL günlük ortalama; ince çubuklar ve gün listesi; haftalık %12,5 artış / aylık %10 azalış; yinelenen banka toplamının kaldırılması; geçersiz aralıkta grafiğin korunması; yalnız mevcut ay açık; bağımsız ay açma/kapama; ödeme ekleme/düzenleme/silme; ödendi/geri alma durumu; tarih değişince aya taşınma ve yeniden açılış. Negatif tutar ve sentetik ödeme yazma hatasında kayıtlar ve girişler korunur.
- Önceki yeni toplam/üzerine yazmama, debit eşleştirme, geçmişten silme ve tarih güvenliği regresyonları tekrar başarılı. Banka başlığındaki kaldırılan göstergeyi bekleyen eski ekran kontrolü, yeni regresyonda artık bulunmamasını doğrular; eski betik ayrıca korunmuştur.
- Yerel Electron v31/v32/v33 testleri başarılı. Yeni yerel test: ince grafik, özel aralık, mevcut ayın açık gelmesi, kısa yorum ve Türkçe uygulama dili.
- Windows x64 paket derlemesi başarılı; paket runtime dosyaları kaynakla karşılaştırıldı. İmza sertifikası yapılandırılmadığından çıktı imzasızdır. Gerçek Windows bilgisayarda çalıştırma bu Linux ortamında doğrulanmadı.

Testlerin tamamı sentetik kayıt ve ayrı profillerle yapıldı. Gerçek finans verilerine veya eski yedeklere müdahale edilmedi.

## Dosyalar ve uyumluluk
Üretim: `pos-app/app-source.jsx`, üretilen `renderer.js`, yalnız dil tercihi için `main.js`; sürüm etiketleri `package.json`, `package-lock.json`, `BUILD.bat`.
Test/belge: `tests/calculation.cjs` ek kontrolleri, `browser-v33.mjs`, `browser-v33-dates.mjs`, `electron-v33.mjs`, sonuç günlükleri ve bu belgeler.

Yeni veritabanı/bağımlılık yok. `safety.js`, native dosya kaydetme, uygulama kimliği `com.poskasayonetim.app`, logo, ürün adı ve `pos:*` alanları korundu. Güvenli yeni toplam/debit akışı, yalnız geçmişten silme ve gerçek debit tarihini koruma kuralları devam eder. Ay grupları, filtreler ve yorumlar görünüm hesaplarıdır; eski kayıtları topluca dönüştürmez.

## Teslim ve geri dönüş
ZIP bütün kaynak/vendor dosyalarını içerir; `node_modules`, `dist`, önbellek veya kullanıcı profili içermez. Hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Önce JSON yedek alın; çalışan program klasörünü saklayın; kullanıcı verilerini temizlemeyin.

Geri dönüş kaynağı Build v32 commit `dd4d52de21c9a9435fcd66439f5987563308f22f`. Saklama şeması değişmedi; kaynak geri dönüşü veri dönüşümü gerektirmez.
