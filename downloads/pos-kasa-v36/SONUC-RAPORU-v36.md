# Build v36 sonuç raporu

Temel: Build v35 (1.3.1), geri dönüş kopyası /workspace/pos-v35; kaynaklar yeni /workspace/pos-v36 altında geliştirildi. Gerçek kullanıcı depolaması açılmadı.

## Değişen dosyalar

- pos-app/app-source.jsx: koşullu tek toplam POS girişi, eski alt toplam girişinin kaldırılması, yeni nakit giriş/düzenleme, birleşik işlem geçmişi ve nakit silme, ayrı nakit state/saklama, yedek önizlemesinde nakit adedi, mevcut analiz bileşeninin nakit veri adaptörüyle tekrar kullanımı, 32 birim gelecek grafiği çubukları, ortak başlık stili.
- pos-app/renderer.js: JSX kaynağından yeniden üretildi.
- pos-app/safety.js: isteğe bağlı nakit listesi, kimlik/tutar/tarih/kasa doğrulaması; yedek/geri yükleme/güvenlik günlüğü kapsamı; eski günlüklerde eksik nakit alanının korunması.
- pos-app/package.json, package-lock.json, BUILD.bat: 1.4.0/v36 etiketi; test komutuna yeni nakit testlerinin eklenmesi. Bağımlılık sürümleri ve uygulama kimliği aynı.
- tests/cash-v36.test.cjs, browser-v36.mjs, electron-v36.mjs ve sonuç kayıtları; bu kullanım ve sonuç belgeleri.

main.js, preload.js, file-store.js, ui-accessibility.js, ikonlar ve vendor dosyaları değişmedi. Mevcut POS mutabakatı ve haftalık/yatış hesaplama işlevleri değişmedi. Yeni nakit analizi var olan posAralikOzeti/posAnalizKiyas kurallarını yalnız gösterim adaptörü üzerinden kullanır; POS listesine nakit yazılmaz.

## Doğrulama

- Mevcut 35 güvenlik testi: geçti.
- Yeni 28 nakit/güvenlik testi: geçti. Toplam 63 test, sıfır başarısız/atlanan.
- Mevcut 182 sayısal kontrol: geçti; JSX derlemesi başarılı.
- Chromium browser-v36: tek toplam alanı, ayrı kayıt ekleme, tutar temizleme, vade/debit eşleştirme, normal kasaya geçiş, dün/bugün varsayılanları, nakit ekleme/düzenleme/silme/yeniden açılış, yazma hatasında taslak ve kayıt koruma, kasa/POS ayrımı, ortak başlık stili, filtre/ortalama/boş aralık/geçersiz aralık, çubuk kalınlığı, yedeğe nakit dahil edilmesi ve eski yedekte nakdin korunması geçti. JavaScript ekran hatası yok.
- Mevcut browser-v35: haftalık oranlar ve renkler, sıfır/eksik baz, manuel ve bağlı kayıt tarihlerinin düzenlenmesi, gerçek debit tarihi koruması geçti.
- Mevcut browser-v34: Özet/Takip geçişi, özgün kasa kartları, toplamlar, filtreler, ödeme yönlendirme ve kayıtların değişmemesi geçti.
- Gerçek Electron çalışma zamanı (Linux, ayrı sentetik profil): nakit kaydı, yeniden açılış ve özet analizi, değişmeyen POS listesi, mevcut özet/takip göstergeleri, Türkçe locale geçti. Test profili silindi.
- Windows x64 electron-builder derlemesi başarılı. app.asar içindeki main/renderer/safety/index/ikonlar kaynakla byte karşılaştırmasında aynı. Kod imzalama sertifikası yok; paket imzasız. Hazır Windows paketi kaynak ZIP’ine eklenmedi.
- Gerçek Windows/banka ortamında kullanım henüz doğrulanmadı. Yeni toplam POS kaydı, ertesi gün debit, nakit kayıtları, tam JSON dışa/geri yükleme ve uygulama yeniden başlatma cihazda ayrıca kontrol edilmeli.

Önceki sürümlerin eski giriş alanlarına bağlı tarayıcı betikleri arşiv olarak korundu; kaldırılmış Toplamı kaydet düğmesini bekleyen eski betiklerin güncel sürümde geçtiği iddia edilmez. Kabul edilen yeni yerleşim için browser-v36 eklenmiştir. Eski sayısal/güvenlik beklentileri gevşetilmedi.
