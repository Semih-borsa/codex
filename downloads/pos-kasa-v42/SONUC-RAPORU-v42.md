# Build v42 — Veresiye, üçlü dağılım ve yerleşim sonuçları

## Kaynak ve koruma

Ana kaynak v41/1.5.1'in tam kopyası üzerine geliştirildi. Geri dönüş kaynağı `/workspace/pos-v41` değiştirilmedi. Yeni çalışma `/workspace/pos-v42` altındadır. Gerçek kullanıcı kayıtları ve eski yedekler kullanılmadı/değiştirilmedi. Yeni bağımlılık eklenmedi. `com.poskasayonetim.app` kimliği, ürün adı ve gömülü OCR korunur. Sürüm 1.5.2'dir.

## Değişen üretim dosyaları

- `pos-app/app-source.jsx`: ayrı veresiye state/depolama, güvenli kayıt, giriş ve geçmişte düzenleme/silme, veresiye analizi, üçlü dağılım, geri yükleme önizlemesi ve kaydırmasız kasa performansı.
- `pos-app/safety.js`: veresiye liste doğrulama; yedek/snapshot/journal ve eski günlük uyumluluğu.
- `pos-app/renderer.js`: JSX kaynağından yeniden derlendi; elle düzenlenmedi.
- `pos-app/package.json`, `package-lock.json`: 1.5.2 sürümü; yeni test dosyası komutu. Mevcut bağımlılık sürümleri değişmedi.

Testler: yeni `tests/credit-sales-v42.test.cjs`, `tests/browser-v42.mjs`, `tests/electron-v42.mjs`; yürütme sonuçları ve ekran görüntüleri. Belgeler: KULLANIM-v42.md, bu rapor, README.md.

## Hesaplama ve veri davranışı

Veresiye kayıtları `{id,tarih,tutar}` biçiminde ve kasa/banka gerektirmeden ayrı alanda tutulur. Yeni kaydetme ekler; güncelleme yalnız kimliği eşleşen kaydı değiştirir. Silme onay ister. Nakit toplu kaydı ile veresiye ayrı kaydetme işlemleridir.

Veresiye analizinde mevcut `posAralikOzeti` ve `posAnalizKiyas` kullanılır; yalnız bu kayıtlar için kasa/banka filtresi uygulanmaz. POS ve nakit varsayılan akışı aynıdır. Tarih aralığı, hafta günü sayacı ve kuruş toplama mantığı ortaktır; ikinci bir hesap motoru eklenmedi.

600 TL POS +300 TL nakit +100 TL veresiye →1.000 TL toplam; %60/%30/%10. POS toplamına bağlı debit ikinci kez satış eklemez. Veresiye bankaya yatacak tutarlara dahil olmaz. Ekrandaki yüzdeler en büyük kalan yöntemiyle 0,1 puana yuvarlanır ve %100 eder; halka gerçek oranları kullanır. Sıfır toplamda yüzdeler çizgi gösterir.

Eksik veresiye alanı korunur; açık boş liste boşaltılabilir; null/bozuk liste reddedilir. Geri yükleme öncesi güvenlik kopyası, yazma doğrulaması ve hata halinde geri dönüş veresiye için de geçerlidir. Bozuk kayıt açılışta kullanıcıya bildirilir; otomatik sıfırlanmaz. Başarısız yeni girişte alanlar ve mevcut kayıtlar korunur.

## Test sonuçları

- Önceki **156 testin tamamı** ve **35 yeni test**: **191/191 başarılı**, başarısız/atlanan test yok. `tests/final-tests-v42.txt`.
- Mevcut **182 ek sayısal kontrol** ve JSX derleme başarılı.
- Yeni Chromium UI akışı: kaydetme, yeniden yükleme, düzenleme, silme/iptal, hatalı tutar, yazma başarısızlığı, eski POS/nakit verisinin korunması, üçlü dağılım, tarih filtreleri, bütün veresiye analiz dönemleri, özel aralık, günlük ortalama ve bozuk kayıtta güvenli açılış durdurma başarılı.
- 8 kasa ile 1100 ve 1500 piksel pencerelerde kasa performansı iki sütun; panel içinde yatay/dikey kaydırma yok, satırlar taşmıyor ve iki panelin dış yüksekliği eşit.
- Mevcut Chromium çoklu nakit v38, ödeme toplamları v39 ve fotoğraf v40 akışları başarılı. Fotoğraf UI testi deterministik OCR adaptörü kullanır.
- Gerçek Electron: veresiye kaydetme; uygulama süreci kapanıp yeniden açılınca kalıcılık; gerçek yerel IPC ile doğrulanmış JSON dosyası yazma; tam yedek ve eski kısmi yedekte korunma başarılı. Test dosya kaydetme penceresinin hedefi sentetik testte belirlenmiştir; Windows'un gerçek pencere davranışı test edilmemiştir.
- Gerçek gömülü Tesseract motoru ile v41 sentetik fiş/7 satır/1.230 TL toplam, 1.000 TL kredi ve230 TL debit tanıma, onay ve kayıt akışı başarılı; bu test OCR sonucu taklit etmez.
- Windows x64 `npm run dist` başarılı. Gömülü OCR için235 dosya paketlendi ve hashleri doğrulandı. Windows imzalama sertifikası tanımlı olmadığından paket imzalanmadı; appId değişmedi.

## Sınırlar ve gerçek cihaz kontrolü

Windows paketi bu ortamda derlendi; gerçek Windows bilgisayarında çalıştırılmadı. Güncellemeden önce JSON yedeği ve eski program klasörünü saklayın. Gerçek cihazda tarih biçimini, kaydetme/düzenleme/silme akışını, kapatıp açmayı, yedek dosyasını ve OCR'yi kontrol edin. ZIP tam kaynak paketidir; Windows çıktısını BUILD.bat ile üretin ve win-unpacked klasörünün tamamını taşıyın.

Yeni alan eski kaynakta görünmez; geri dönüşte v42 yedeğini saklayın ve veri klasörünü temizlemeyin. Hiçbir gerçek veri otomatik dönüştürülmedi. Finansal tahsilat kuralları, manuel POS alanları, debit eşleştirme, ödeme takibi ve OCR toplam onayı korunmuştur.
