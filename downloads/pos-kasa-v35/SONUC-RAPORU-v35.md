# POS Kasa Yönetim Build v35 — 1.3.1

Kullanıcının düzeltmesi doğrultusunda Takip sekmesindeki büyük toplam yatış kartı, tarih seçiminin altına ve banka/kasa kartlarının üzerine geri eklendi. Başlık **BUGÜN TOPLAM YATACAK TUTAR**. Toplam, kredi ve debit için ayrı tutar ve önceki haftanın aynı gününe göre değişim göstergeleri vardır. Kartın tutarları ve yüzdeleri seçili Takip tarihine göre güncellenir. Artış yeşil, azalış kırmızı, eksik veri veya sıfır taban gri “–”; değişmeyen tutar gri %0.

Mevcut `gTotal/gKredi/gDebit`, `takipGunOzeti` ve `HaftalikDegisim` kullanıldı. Yeni hesap motoru veya veri alanı yok. Özet sekmesi, kasa adı altındaki tutarlar, mevcut tarih seçici ve kasa kartları değiştirilmedi. Ödeme takibi ve POS analizi yalnız Özet'te kalır. Kredi tahsil durumu ve kullanıcı kayıtları değişmez.

## Doğrulama
- 35 güvenlik/dosya testi ve mevcut 182 hesap kontrolü başarılı; JSX derlemesi başarılı.
- Takip kartı gerçek React ekranında doğrulandı: kredi 1000→1500 TL için %50 artış, debit 500→250 TL için %50 azalış, toplam 1500→1750 TL için %16,7 artış. Renkler, eksik/sıfır taban ve tarih değişimi kontrolleri geçti.
- Özet, grafiklerin ayrılığı, kasa adının altında tutar, tek ödeme/analiz yerleşimi, tarih kısayolları ve değişmeyen depolama regresyonu geçti.
- Önceki art arda yeni kayıt/üzerine yazmama ve geçmişten güvenli silme akışı geçti.
- Yerel Electron'da geri eklenen Takip kartı, yüzde göstergeleri, mevcut Özet ve korunmuş işlem verileri doğrulandı. Testler sentetik verili ayrı profillerde yapıldı.
- Windows x64 paket derlemesi başarılı; paket runtime dosyaları kaynakla karşılaştırıldı. Sertifika olmadığından çıktı imzasızdır. Gerçek Windows cihazında çalıştırma testi bu Linux ortamında yapılmadı.

## Dosyalar ve geri dönüş
Üretim: `pos-app/app-source.jsx`, üretilen `renderer.js`; sürüm etiketleri `package.json`, `package-lock.json`, `BUILD.bat`. Test/belge: `tests/browser-v35.mjs`, `tests/electron-v35.mjs`, günlükler ve bu belgeler. Güvenli yedekleme, `main.js`, `safety.js`, uygulama kimliği, logo ve saklama şeması değişmedi.

ZIP bütün kaynak/vendor dosyalarını içerir; node_modules, dist, kullanıcı profilleri veya önbellek içermez. Hazır EXE değildir. `pos-app/BUILD.bat` ile derleyin. Önce JSON yedek alın ve çalışan program klasörünü saklayın. Kullanıcı veri klasörünü temizlemeyin.

Geri dönüş noktası Build v34 commit `9a47d6de1ccbd8af7960bce80820fcfd76ca761e`. Bu sürüm yalnız görünümü geri ekler; gerçek kayıtları veya eski yedekleri dönüştürmez.
