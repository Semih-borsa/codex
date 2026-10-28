# Build v37 sonuç raporu

Temel Build v36/1.4.0. Geri dönüş kaynağı /workspace/pos-v36 ayrı ve değiştirilmeden korundu; geliştirme /workspace/pos-v37 altında yapıldı.

## Değişiklikler

- pos-app/app-source.jsx: İşlem Geçmişi için varsayılan kapalı NAKİT/POS details listeleri, bağımsız sayfalama. Nakit güncelleme/silme akışları korundu. Özet’e satış tarihi esaslı POS/nakit dağılımı eklendi. satisDagilimi mevcut tarih aralığı toplayıcısını kullanır; toplam POS kaydı tek sayılır. Ortak nakit/POS gösterim bileşenleri ve veritabanı yerine yeni bir finans motoru oluşturulmadı. Kasa ayrıntısı dağılım kartında tekrarlanmıyor.
- pos-app/renderer.js: JSX kaynağından derlendi.
- pos-app/package.json, package-lock.json, BUILD.bat: sürüm1.4.1 / v37; yeni test betiği.
- tests/distribution-v37.test.cjs:13 yeni sentetik hesaplama testi.
- tests/browser-v37.mjs: yeni kabul akışı; browser-v37-regression.mjs ve browser-v37-weekly.mjs önceki akışların yeni kapalı listeleri açan sürümleri. Önceki betikler arşivde değişmeden kaldı. electron-v37.mjs gerçek Electron doğrulamasıdır.
- Yeni kullanım/rapor belgeleri.

safety.js, main.js, preload.js ve mevcut yedek şeması değişmedi. Finans kayıtlarını değiştiren eski hesaplama işlevlerine dokunulmadı. Kullanıcı kayıtlarına erişilmedi; yalnız sentetik veriler kullanıldı.

## Sonuçlar

- Mevcut63 güvenlik/nakit testi +13 dağılım testi:76/76 geçti, atlama yok.
- Mevcut182 sayısal kontrol ve JSX derlemesi geçti.
- Yeni testler:80/20 dağılım, bağlı debitin çift sayılmaması, satış-vade tarih ayrımı, manuel POS, aynı gün ayrı toplamlar, sonradan debit, yalnız nakit/POS, boş dönem, kuruş ve oran toplamı, dahil tarih sınırları, geçersiz tarih, pasif kasanın geçmişi, kaynakların değişmemesi.
- Chromium browser-v37: iki liste kapalı, bağımsız açma, 52 nakit için50+2 sayfalama, POS sayfasının korunması, nakit güncelleme, silmeyi iptal ve onay, arama/tarih filtresi, yeniden açınca kayıtların korunması ve listelerin kapalı olması geçti. Halka1000TL/80-20, iki renk, preset/özel/boş/ters aralık, kasa ayrıntısının olmaması ve POS kayıtlarının değişmemesi geçti. JavaScript ekran hatası yok.
- Chromium önceki akışlar: browser-v37-regression (eski v36 akışı, yeni details başlıklarını açıkça açar) ve browser-v37-weekly (eski v35 haftalık/tarih akışı) geçti. Önceki finansal beklentiler değiştirilmedi; kaldırılan açık tablo varsayımı yeni onaylı yerleşime uyarlandı.
- Gerçek Electron çalışma zamanı Linux üzerinde ayrı sentetik profille: kapalı listeler, yeni dağılım paneli, eski özet/takip göstergeleri, nakit kaydı/yeniden açılış, değişmeyen POS verileri ve Türkçe locale geçti.
- Windows x64 electron-builder üretim derlemesi başarılı. app.asar içindeki main/renderer/safety/index/ikon dosyaları kaynakla byte düzeyinde eşleşiyor. Sertifika tanımlı olmadığı için Windows paketi imzasızdır; kaynak ZIP hazır EXE içermez.

Gerçek Windows cihazında yeni liste açma/düzenleme/silme ve gerçek kasa satışlarıyla dağılım kontrolü henüz yapılmadı. Yedek ve program klasörü korunarak test edilmelidir. Yeni özellik başka bir hesabı/banka tahsilatını değiştirmez.
