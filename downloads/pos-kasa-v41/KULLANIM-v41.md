# POS Kasa Yönetim — Build v41 / 1.5.1

Bu sürümde OCR motoru ve Türkçe/İngilizce dil modelleri programın içindedir. V40’taki Windows OCR dili veya PowerShell şartı kaldırıldı. Güncel yönerge bu dosyadır; eski sürüm belgelerindeki OCR kurulum şartları v41 için geçerli değildir.

## Kullanım

Önceki gibi Giriş > Fotoğraftan ekle düğmesine basın, fotoğrafı seçin veya sürükleyin. Kasa ve işlem tarihini seçin; varsayılan tarih dündür. Tamam · Fotoğrafı oku düğmesi gömülü motoru çalıştırır. İlk çalıştırmada da dil/motor indirmesi yapılmaz. Kullanım ücretsiz ve internetsizdir; fotoğraf API’ye veya buluta gönderilmez.

VakıfBank raporunda debit kart yurtdışı satışın kredi vadesine gitmesi dahil yedi kalemin kuralları korunur. Kalem toplamı fişteki genel toplamla kuruşu kuruşuna uyuşmadan, kasa/tarih/kalem kontrolünü onaylamadan kaydetme açılamaz. Yanlış okunan alanı düzeltince eşleşme yeniden kontrol edilir. Fotoğraf okuması her fişte kusursuz olmayabilir; kontrol/onay zorunluluğu devam eder. Aynı fişi ayrıca manuel girişten kaydetmeyin.

Mevcut manuel POS, çok satırlı nakit, ödeme özeti, vade ve grafik işlevleri korunur. Mobil/LAN geliştirmesi eklenmedi. Gerçek kullanıcı kayıtları dönüştürülmedi.

## Ek kurulum gerekiyor mu?

Derlenen programda ayrıca Tesseract, Windows OCR dili veya PowerShell kurulumu gerekmez. resources/ocr-runtime klasörü motoru, WebAssembly dosyalarını ve iki dili içerir. OCR bu klasör olmadan çalışmaz: programı taşırken sadece EXE’yi değil, win-unpacked klasörünün tamamını taşıyın. Dosyalar eksik/bozuksa internetten tamamlamak yerine hata gösterilir.

Bu ZIP tam kaynak paketidir, hazır EXE veya kurulum sihirbazı değildir. Kaynağı yeni klasöre çıkarıp pos-app/BUILD.bat ile önceki gibi derleyin. Kaynaktan derleme için önceki Node/npm/Electron araçları ve ilk bağımlılık kurulumu koşulları geçerlidir. OCR için ayrıca bir işlem yapmanız gerekmez; motor ve dil dosyaları ZIP’in içinde hazırdır. ocr-runtime klasörünün içinde npm install çalıştırmayın.

## Güvenlik ve yedek

OCR ayrı bir alt süreçte çalışır; işi bitince veya zaman aşımında kapanır. Ağ istekleri OCR sürecinde kapalıdır. Fotoğraf bellekte işlenir; v40’taki geçici fotoğraf dosyası yazma yolu kaldırıldı. Kalıcı kayıtlarda/yedekte yalnız onaylanan kalemler, genel toplam ve fiş bağlantıları bulunur.

Uygulama kimliği com.poskasayonetim.app ve veri şeması aynıdır. Güncellemeden önce tam JSON yedeğini ve eski program klasörünü saklayın; mevcut verileri temizlemeyin. Geri dönüş kaynağı v40’tır. V40’a dönerseniz onun eski Windows OCR dili gereksinimi yeniden geçerli olur. Fiş bağlantıları ve yedek biçimi v40 ile uyumludur.

Windows paketi başarıyla üretildi; gerçek Windows cihaz çalıştırması burada yapılmadı. Paketlenmiş uygulamada gerçek gömülü OCR, Linux test ortamında ayrıca doğrulandı. Bilgisayarınızda ilk denemede fiş tutarlarını gözle karşılaştırarak kontrol edin.
