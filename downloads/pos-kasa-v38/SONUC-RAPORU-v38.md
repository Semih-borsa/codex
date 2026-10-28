# Build v38 sonuç raporu

Başlangıç Build v37/1.4.1 kaynağı /workspace/pos-v37 altında değişmeden korundu. Yeni kaynak /workspace/pos-v38. Gerçek kullanıcı verilerine erişilmedi; testler sentetik kayıtlarla yapıldı.

## Dosyalar ve değişiklikler

- pos-app/app-source.jsx: iki varsayılan nakit satırı, aktif kasa seçimi, Kasa ekle ve fazla taslak satır kaldırma; tek doğrulama/tek depolama yazısıyla toplu kayıt, satır bazında hata; boş tutarı0 kaydetme; eski tek kayıt düzenleme akışı ve düzenleme sırasında toplu taslağı koruma; çift tıklama koruması. nakitVarsayilanKasalar ve nakitTopluKaydet mevcut nakitKaydet doğrulamasını kullanır. nakitKaydet’e allowZero seçeneği eklenmiştir; mevcut varsayılanı pozitiftir, yeni toplu/ekran düzenleme akışları sıfıra açıkça izin verir.
- pos-app/safety.js: yalnız cashList kontrolünde sıfır nakit kabulü; negatifler reddedilmeye devam eder. POS tutar doğrulamaları değişmedi. Yedek/güvenlik günlüğü şeması değişmedi.
- pos-app/renderer.js: kaynak JSX’ten üretildi.
- pos-app/package.json, package-lock.json, BUILD.bat:1.4.2/v38 ve yeni test komutu. Kimlik ve bağımlılıklar aynı.
- tests/batch-cash-v38.test.cjs, browser-v38.mjs, electron-v38.mjs; kullanım/rapor ve sonuç dosyaları.

main.js, preload.js, file-store.js, ikonlar, POS tahsilat hesapları ve dağılım halkası değiştirilmedi.

## Testler

- Önceki76 otomatik test +22 yeni test:98/98 geçti; atlama yok. Mevcut182 sayısal kontrol ve JSX derlemesi başarılı.
- Yeni testler kasa adlarına göre varsayılan1/2, pasif/yetersiz kasalar, iki/üç kasa kaydı, bir/iki boş tutarın0 olması, açık0/negatif ayrımı, ikinci satır hatasında değişmeme, eksik kasa/tarih/id, kimlik çakışması, sıfır yedek/geri yükleme, sıfır kaydı düzenleme ve kaynak POS verilerinin korunmasını kapsar.
- Chromium browser-v38: iki satır, varsayılan kasa1/2 ve dün, aktif seçenekler, tek kaydetle iki/üç kayıt, boş0 kayıtları, ekle/kaldır/listeden seçim/tarih, tutar temizleme, satır hatası ve depolama yazma hatasında tüm taslağı koruma, tek kayıt düzenleme sırasında taslak koruma, sıfıra düzenleme, JSON yedek, yeniden açılışta kalıcılık, çift tıklamada tek grup ve değişmeyen POS listesi geçti. JavaScript ekran hatası yok.
- Önceki browser-v37 dağılım/geçmiş akışı ve browser-v37-weekly POS haftalık/tarih akışı yeni kaynakta değişmeden geçti. Eski tek satır nakit ekleme düğümünü bekleyen v36 betikleri arşivde korunur; onların tamamının yeni çoklu girişte geçtiği iddia edilmez. Yeni kabul akışı browser-v38’dedir.
- Gerçek Electron çalışma zamanı Linux üzerinde ayrı sentetik profil: iki nakit satırı ve boş0 kayıt, yeniden açılış, mevcut Özet/Takip, kapalı geçmiş listeleri, dağılım paneli, Türkçe locale ve değişmeyen POS verisi geçti.
- Windows x64 electron-builder derlemesi başarılı. Paket içindeki main/renderer/safety/index/ikon kaynaklarla byte eşit. Kod imza sertifikası tanımlı değil; hazır Windows paketi imzasız. Teslim kaynak ZIP’idir.

Gerçek Windows cihazı testi henüz yapılmadı. Güncellemeden önce tam yedek alınmalı; cihazda iki/üç kasa, boş0 tutarlar, düzenleme/silme ve yeniden açılış kontrol edilmelidir. v37 sıfır nakit kaydını tanımadığından geri dönüş sınırı KULLANIM-v38.md dosyasında açıklanmıştır. Kaynak geri dönüş kopyası tek başına yeni sıfır verilerin eski sürüm tarafından okunacağını garanti etmez.
