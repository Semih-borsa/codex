# Build v28 (1.0.6) sonuç raporu

## Sorun ve çözüm
Önceki sürümde yalnız kasa seçilince `gid` (kaydedilmiş toplam POS kimliği) oluşmuyordu. Debitleri kaydet doğru bir toplam kaydı olmadan çalışmayacak şekilde korumalıydı; ancak alt bölümde bu kaydı seçmek için yeterli alan bulunmuyordu.

Bankaya yatan debitler bölümüne kasa seçimi ve kasa bazında POS işlem günü/kayıt seçimi eklendi. Tek kayıt otomatik açılır; birden fazla kayıt için gün kullanıcı tarafından seçilir. Kayıtlı toplam yoksa önce üst bölümden toplam kaydedilmesi gerektiği belirtilir. Hiçbir geçmiş toplam tahmin edilmez, otomatik oluşturulmaz. Var olan kayıtlar otomatik değiştirilmez.

Üst ve alt bölüm aynı kasa/kaydı gösterir. Kayıt değiştirirken kaydedilmemiş tutar/debit taslağı varsa kullanıcıya onay sorulur; iptal taslağı korur. Debit ekleme ve kaydetme düğmeleri yalnız kayıt seçiliyken açılır. Toplam/debit ayrı kayıt düğmeleri ve Türkçe tutar gösterimi korunmuştur.

## Dosyalar
- `pos-app/app-source.jsx`, üretilen `renderer.js`.
- Sürüm/derleme etiketi: `package.json`, `package-lock.json`, `BUILD.bat`.
- Yeni sentetik tarayıcı testi `tests/browser-v28.mjs`, test günlükleri, bu rapor ve kullanım belgesi.
- Kimlik `com.poskasayonetim.app`, ürün adı, logo, mevcut hesaplama yardımcıları, yedekleme ve `pos:*` veri alanları değişmedi. Yeni bağımlılık yok.

## Sonuçlar
- 35 güvenlik/dosya testi: başarılı.
- Önceki 17 hesap kontrolü + v27'deki 29 ek kontrol: başarılı.
- JSX derlemesi: başarılı.
- v27 React tarayıcı regresyonu: başarılı.
- v28 React tarayıcı testi: alt kasa seçimi, tek kaydın otomatik açılması, çoklu gün için açık seçim, yalnız seçili gruba debit yazılması, taslak kaybı uyarısının iptal edilmesi, kayıtsız kasa uyarısı ve düğme durumları başarılı. JavaScript sayfa hatası yok.
- Windows x64 Electron paket derlemesi: başarılı. Gerçek Windows bilgisayarda çalıştırma bu Linux ortamında doğrulanmadı. İmza sertifikası yapılandırılmadığından çıktı imzasızdır.

Testler geçici tarayıcı profillerindeki sentetik verilerle yapıldı. Gerçek kullanıcı kayıtlarına dokunulmadı.

## Teslim ve geri dönüş
Tam kaynak ZIP'inde değişen/değişmeyen dosyalar ve vendor dosyaları vardır; `node_modules`, `dist`, kullanıcı profilleri ve önbellekler yoktur. Windows'ta `pos-app/BUILD.bat` çalıştırın. Kaynak ve uygulama klasörlerini saklayın, güncellemeden önce JSON yedek alın. Eski çalışan sürüme geri dönüş için Build v27 commit `55ddb11af3e06200e47dfa39f7638d4cca709698` kullanılabilir. Veri şeması değişmedi; kullanıcı veri klasörünü silmeyin.
