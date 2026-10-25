# POS Kasa Yönetim Build v32 — Sürüm 1.1.0

## Yapılan değişiklikler
1. Takip ekranında seçili gün ve 7 gün öncesi, toplam/kredi/debit olarak ayrı karşılaştırılır. Aynı göstergeler banka toplamı ve kasa kartlarına eklendi. Mevcut tutar ve Takip yerleşimi korunur; küçük kasa kredi/debit hücrelerinde yalnız ok/yüzde gösterilerek taşma önlendi.
2. Artış yeşil, azalış kırmızı; eşit tutar gri %0. Önceki türde kayıt yoksa veya önceki tutar sıfırsa gri “–”. Seçili günde kayıt yokken de varsayımsal -%100 gösterilmez. Karşılaştırma tarihini, eski tutarı ve eksiklik nedenini göstergenin üzerine gelerek görmek mümkündür. Rakamlar değişim için kuruş toplamlarıyla hesaplanır; hiçbir kayıt yazılmaz.
3. Toplam POS düzenleme penceresinde POS günü değişince kredi vadesi mevcut kredi vade ayarına göre anında güncellenir. Kullanıcının açık tercihiyle gerçekleşmiş debit yatış tarihleri korunur. Sadece Kaydet sonrası kalıcı olur.
4. Ayrı manuel kredi/debit düzenleme akışındaki tarih otomatiği ortak, doğrulanan vade-tarihi yardımcısıyla korunup test edildi. Kredi kendi vadesini, debit kendi vadesini kullanır. Sıfır gün vade ve ay/yıl değişimleri desteklenir.
5. POS gününün gerçek debit yatışından sonraya taşınması gibi tutarsız güncelleme, eski veriler korunarak reddedilir.

Yeni finans motoru veya saklama şeması oluşturulmadı. Haftalık yardımcılar yalnız mevcut Takip satırlarının karşılaştırma toplamlarını üretir; bakiye/işlem motorunu değiştirmez. Yeni bağımlılık yok.

## Doğrulama
- 35 güvenlik/dosya testi başarılı.
- Önceki 90 hesap kontrolü ve yeni 24 kontrol: **114 hesap kontrolü başarılı**. JSX derlemesi başarılı.
- Yeni kontroller: ayrı kredi/debit/toplam yüzdeleri; artış/azalış/eşitlik; eksik ve sıfır taban; banka/kasa toplamları; bilinmeyen kayıt bağlantılarının mevcut Takip davranışıyla aynı dışlanması; sıfır gün, ay sonu, artık yıl ve yıl değişimi; grup tarihi güncellendiğinde kredi vadesinin değişip gerçek debit tarihinin korunması.
- v31 kritik Chromium akışı tekrar başarılı: birbirinin üzerine yazmayan toplamlar, temizlenen giriş, aynı güne kayıt onayı/iptali, geçmişten debit/POS silme, kalan kredinin geri gelmesi, açık düzenleme, yazma hatası ve yeniden açılış.
- Önceki debit/tarih/temizleme/Takip regresyonu tekrar başarılı.
- Yeni v32 Chromium akışı başarılı: toplam %16,7 artış, kredi %50 artış, debit %50 azalış örneği; gerçek CSS renkleri; eksik/sıfır/eşit durumları; banka ve kasa etiketleri; karşılaştırma sırasında depolamanın değişmemesi; ayrı kredi/debit güncellemesi; toplam POS kredi tarihi; gerçek debit tarihinin korunması; tutarsız tarih reddi ve yeniden açılış.
- v31 yerel Electron yeni-kayıt/silme regresyonu tekrar başarılı.
- Yeni v32 yerel Electron akışı başarılı: haftalık gösterge, eksik baz, grup POS günü/kredi vadesi, gerçek debit tarihini koruma ve yeniden açılış. Testler yalnız sentetik, ayrı profillerde çalıştırıldı.
- Windows x64 Electron paket derlemesi başarılı. Paket içindeki runtime dosyaları kaynakla karşılaştırıldı. Sertifika yapılandırılmadığından imzasız çıktı üretildi. Gerçek Windows cihazında çalıştırma bu Linux ortamında test edilmedi.

## Dosyalar
Üretim: `pos-app/app-source.jsx`, üretilen `renderer.js`, sürüm etiketleri `package.json`, `package-lock.json`, `BUILD.bat`.
Test/belge: `tests/calculation.cjs` ek kontrolleri, `tests/browser-v32.mjs`, `tests/electron-v32.mjs`, test günlükleri, bu rapor ve kullanım belgesi.

`main.js`, native yedekleme, `safety.js`, logo, ürün adı, `pos:*` alanları ve uygulama kimliği **com.poskasayonetim.app** korunmuştur. Gerçek kullanıcı kayıtları veya eski yedekler değiştirilmedi. Güncelleme geçmiş tarihleri topluca yeniden hesaplamaz; tarih değişikliği kullanıcı bir kaydı açıp kaydettiğinde uygulanır.

## Teslim ve geri dönüş
ZIP değişen/değişmeyen tüm kaynak/vendor dosyalarını içerir; node_modules, dist, önbellek veya kullanıcı profili içermez. Hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Önce JSON yedek alın ve çalışan uygulama klasörünü saklayın; kullanıcı verilerini temizlemeyin.

Önceki sürüm geri dönüş noktası: Build v31 commit `e56d896399ab0f490de0c8993bc9e9f3a1ac17bf`. Saklama biçimi aynı kaldığı için kaynak geri dönüşü veri dönüşümü gerektirmez. Kullanıcı tarafından kaydedilmiş bir tarih düzeltmesini geri almak için ilgili kaydı yeniden düzenlemek veya uygun önceki yedeği kullanmak gerekir.
