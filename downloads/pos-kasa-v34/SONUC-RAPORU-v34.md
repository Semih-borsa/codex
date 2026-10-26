# POS Kasa Yönetim Build v34 — 1.3.0

## Uygulanan yerleşim
- Yeni Özet sekmesi varsayılan açılış sayfası oldu. Üç günlük kart, önümüzdeki 7 günün beklenen POS grafiği, kasa performansı, mevcut POS Gelir Analizi ve Aylık Ödeme Takibi burada yer alır.
- İlk kartın başlığı tam olarak **BUGÜN TOPLAM YATACAK TUTAR**. Günlük kartlar ilgili tarihte Takip'i açar; gelecek gün çubukları da o tarihe kısayoldur. Grafik çubuklarına klavyeyle erişim desteklenir.
- Kullanıcının netleştirdiği tercihle günlük ortalama grafiği de Özet'e taşındı. Beklenen 7 günlük yatış grafiğinden ayrıdır. Günlük ortalama çubukları 20 birimden 32 birime genişletildi; mevcut ortalama hesabı ve filtreler değiştirilmedi.
- Ödeme takibi ve POS analizi Takip'ten kaldırıldı; gizli ikinci kopyaları oluşturulmadı. Özet'te birer kez render edilir. Ödeme hatırlatmasındaki bağlantı yeni konuma yönlendirildi.
- Takip'teki banka/kasa ayrıntı şablonu kaynak metin düzeyinde önceki sürümle karşılaştırıldı ve aynı kaldığı doğrulandı. Kasa tutarları adın altında durur. Tarih Aralığı Toplamı Takip'te kaldı.
- Özet'teki kasa performansında da tutar kasa adının altında gösterilir. Bu hafta/Bu ay/Bu yıl seçimi mevcut tarih/özet yardımcılarını kullanır. Kısa ödeme görünümü ayrı liste oluşturmaz; ödemelere kısayol verir. Haftalık ve aylık POS yorumları, taşınan analiz modülünde tek yerde tutulur.

## Hesap ve veri sınırları
Günlük kartlar eski Takip toplamlarıyla aynı satırları kullanır. Önümüzdeki 7 gün, yarın ile bugünden 7 gün sonrası arasındaki mevcut yatış kayıtlarından oluşur; tahsil edilmiş işaretli satırlar beklenen grafiğe alınmaz. Kasa performansı mevcut yatış tarihi bazlı aralık özetlerini kullanır.

Yeni Özet hesapları salt okunurdur. Kredi borcu, kredi tahsil durumu, POS/debit mutabakatı ve ödeme kaydetme mantığına müdahale edilmedi. Gerçek kredi yatışını doğrulayan veya “eksiksiz tahsil edildi” olarak işaretleyen yeni işlem yok. Kayıtlar tahminle birleştirilmez veya değiştirilmez. Yeni veritabanı/bağımlılık/ikinci finans motoru yok.

Veriler ortak `pos:*` kayıtlarından okunur. Ayrıntı sayfasına geçmek, filtre seçmek veya grafiği görüntülemek kayıt yazmaz. Kayıt ve yedek şemaları, gerçek debit tarihini koruma, yeni toplam girişini temizleme ve geçmişten güvenli silme korunmuştur.

## Test sonuçları
- **35 güvenlik/dosya testi** başarılı.
- Önceki 160 hesap kontrolüne yeni 22 kontrol eklendi: **182 hesap kontrolü** başarılı; JSX derlemesi de başarılı.
- Yeni sayısal kontroller: günlük kredi/debit/toplam, önceki hafta, 7 günlük tarih sınırları, tahsil edilmiş satırın beklenen grafikten dışlanması, tarih dışı ödemelerin dışlanması, yaklaşan ödeme toplamı, kasa sıralaması/payları, dönem değişimleri, artık yıl kıyası ve kaynak nesnelerin/ödeme durumlarının değişmemesi.
- Yeni Chromium Özet testi: varsayılan sekme, başlık, üç kart, 32 birim ortalama çubukları, ayrı 7 günlük grafik, doğru beklenen tutar, kasa adının altında tutar, günlük/gelecek tarih kısayolları, Takip'te eski yerleşimin korunması, analiz/ödemelerin tek yerde bulunması, kasa dönem filtresi, hatırlatma yönlendirmesi ve aynı depolama içeriğinin korunması.
- Taşınan analiz ve ödeme testleri tekrar başarılı: altı filtre/özel aralık, örnek ortalamalar ve kısa yorumlar; ayların açılış durumu; ödeme ekleme/düzenleme/silme, ödendi/geri alma, tarih değişince aya taşınma, hata halinde girişin korunması. Test beklentilerinde yalnız kullanıcının istediği sekme değişimi ve çubuk genişliği güncellendi; eski betikler ayrıca korundu.
- Önceki art arda toplam kaydı, üzerine yazmama, debit eşleştirme, geçmişten silme ve gerçek debit tarihini koruma regresyonları başarılı.
- Yerel Electron'da Özet, grafik ayrımı, kalın çubuklar, tek ödeme/analiz yerleşimi, Takip kısayolu, Türkçe dil ve kayıtların korunması doğrulandı. Önceki kayıt/silme Electron testi de geçti.
- Windows x64 paket derlemesi başarılı; paket içindeki runtime kaynakla karşılaştırıldı. Sertifika yapılandırılmadığından çıktı imzasızdır. Gerçek Windows cihaz testi bu Linux ortamında yapılamadı.

Testler sentetik kayıtlarla ve ayrı profillerde yapıldı; gerçek kullanıcı verileri ve eski yedekler değiştirilmedi.

## Dosyalar
Üretim: `pos-app/app-source.jsx`, üretilen `renderer.js`; sürüm metaverileri `package.json`, `package-lock.json`, `BUILD.bat`. Testler: `calculation.cjs` ek kontrolleri, `browser-v34.mjs`, `browser-v34-analytics.mjs`, `browser-v34-dates.mjs`, `electron-v34.mjs`, günlükler ve teslim belgeleri.

`main.js`, güvenli dışa aktarma, `safety.js`, Türkçe tarih gösterimi, logo, ürün adı ve kimlik **com.poskasayonetim.app** korunmuştur. Takip kasa kartlarının içeriği değiştirilmedi.

## Teslim ve geri dönüş
ZIP tüm kaynak/vendor dosyalarını içerir; `node_modules`, `dist`, önbellek veya kullanıcı profili içermez. Hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Önce JSON yedek alın; çalışan program klasörünü saklayın; kullanıcı verilerini temizlemeyin.

Önceki sürüm geri dönüş noktası: Build v33 commit `fd420dee293b1192bfcd0677e743b74bf6f4b560`. Saklama şeması aynı kaldı; bu yerleşim güncellemesi verileri dönüştürmez.
