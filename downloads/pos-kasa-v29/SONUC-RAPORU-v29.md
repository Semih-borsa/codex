# Build v29 (1.0.7) — Tarih seçimi düzeltmesi

## Kök neden
Önceki sürümün alt bölümünde tarih olarak görünen alan bir tarih girişi değil, kayıtlı POS toplamlarının listesiydi. `disabled={!debitGroups.length}` nedeniyle seçilen kasada kayıtlı toplam yokken liste pasifti. Önceki testler bu pasif durumu bekliyordu; kullanıcı için düzenlenebilir tarih gereksinimini kapsamıyordu. Bu eksiklik yeni ekran senaryolarıyla kapsandı; eski testler silinmedi veya gevşetilmedi.

## Değişiklik
- Alt bölüme her durumda düzenlenebilir **Debitin ait olduğu POS günü** tarih girişi eklendi.
- Kayıtlı POS toplamları listesi de artık pasif bırakılmıyor; boş olduğunda açıklayıcı seçenek gösteriliyor.
- Seçilen kasa ve tarihte tek doğrulanmış toplam kaydı varsa yükleniyor. Aynı gün birden fazla kayıt varsa otomatik birleştirilmeden seçim isteniyor. Kayıt yoksa veri üretilmeden uyarı gösteriliyor; yeni toplamın üst bölümden açıkça kaydedilmesi gerekiyor.
- POS satış günü ve debitin bankaya yatış tarihi açıkça ayrıldı. **+ Yatan debit ekle** sonrasındaki satırın Debit tarihi düzenlenebilir; POS gününden sonra olabilir, bugünden sonra olamaz.
- Kaydedilmemiş değişiklikleri koruyan onay, ayrı kaydetme düğmeleri ve ortak para doğrulaması korundu.

## Test sonuçları
- Önceki 35 güvenlik/dosya testi, 17 hesap kontrolü, 29 ek kontrol ve JSX derlemesi başarılı.
- v27 ve v28 React tarayıcı regresyon testleri değişmeden başarılı.
- v29 React tarayıcı testi başarılı: kayıt olmayan kasa için aktif POS tarihi, tarih üzerinden tam kayıt eşleştirme, çoklu gün kayıtları, eşleşmeyen günün gerçek veriyi değiştirmemesi, eksik toplamı açıkça oluşturma, bağımsız yatış tarihi, kalan tutar doğruluğu. Önceki kasa/debit/taslak koruma kontrolleri de tekrar çalıştı.
- Yerel Electron uygulamasında (`file:` kaynak, ayrı sentetik profil) tarih girişi, yeni toplam, farklı bankaya yatış tarihi, kalan kredi ve yeniden açılışta kayıtların korunması başarılı.
- Örnek: POS günü 1 Ocak 2026, toplam 3053,50 TL; debit yatış tarihi 2 Ocak 2026, tutar 1000,25 TL; kalan kredi 2053,25 TL. Yeniden açılışta iki ayrı tarih ve tutarlar korundu.
- Windows x64 Electron paket derlemesi başarılı. İmza sertifikası yapılandırılmadığından çıktı imzasızdır. Gerçek Windows cihazında çalışma bu Linux ortamında doğrulanmadı.

## Sınırlar ve dosyalar
Üretim değişiklikleri: `pos-app/app-source.jsx`, üretilen `renderer.js`, sürüm metaverileri `package.json`, `package-lock.json`, `BUILD.bat`. Yeni testler `tests/browser-v29.mjs`, `tests/electron-v29.mjs`; günlükler ve teslim belgeleri. Finans hesaplama yardımcıları, `safety.js`, native yedekleme sistemi, logo, uygulama kimliği ve `pos:*` saklama anahtarları değiştirilmedi. Yeni bağımlılık eklenmedi. Gerçek kullanıcı verileri ve yedekler değiştirilmedi.

## Teslim ve geri dönüş
ZIP bütün kaynak/vendor dosyalarını içerir; `node_modules`, `dist`, test profilleri ve önbellekler içermez, hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derlenir. Çalışan sürümü ve uygulama içi JSON yedeğini saklayın; kullanıcı veri klasörünü temizlemeyin. Kaynak geri dönüş noktası Build v28 commit `6f6fc7b76c41a2102061b65b8acc48b4ccd33af4`. Veri şeması değişmedi.
