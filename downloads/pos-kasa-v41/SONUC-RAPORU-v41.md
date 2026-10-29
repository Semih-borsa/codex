# Build v41 sonuç raporu

Başlangıç v40/1.5.0 kaynağı /workspace/pos-v40 değiştirilmeden korundu. Yeni çalışma /workspace/pos-v41, sürüm1.5.1. Gerçek kullanıcı depolamasına müdahale edilmedi.

## Değişiklikler

- native-ocr.js: Windows/PowerShell köprüsü yerine gömülü Tesseract alt süreci, sınırlı PNG/JPEG taşıma, zaman aşımı/sonuç doğrulama/çocuk süreç temizliği. Kaynak JPEG’ler Electron’un yüksek kaliteli örneklemesiyle hazırlanır; bozuk veya aşırı büyük görseller reddedilir. Artık geçici OCR fotoğrafı yazılmaz.
- ocr-runtime/: sabit sürümlü Tesseract.js6.0.1, WASM çekirdeği ve bağımlılıkları, resmî tessdata_best Türkçe/İngilizce modelleri, lisanslar ve kaynak kilidi. Paket içindeki235 dosyanın SHA-256 bütünlüğü doğrulanır. worker-local.cjs/offline.cjs ağ isteklerini engeller; model eksikse internetten indirilmez. read.cjs tek blok fiş düzeni ve gölgeye uygun yerel eşikleme kullanır. Gerekli tüm SIMD/standart motor dosyaları pakettedir.
- native-ocr.ps1 kaldırıldı. main.js gömülü runtime konumunu seçer; uygulama kapanışında OCR alt süreçlerini durdurur. Mevcut güvenilir ana pencere IPC denetimi ve sandbox/izinler korunur.
- bundle-ocr.cjs paketleme sonunda tüm motor/modül/dil dosyalarını resources/ocr-runtime içine kopyalayıp doğrular. Eksik motor içeren teslimin derlemeyi başarılı sayması engellenir. node_modules’un otomatik paket dışı bırakılmasına güvenilmez.
- app-source.jsx: yalnız fotoğraf hazırlama kalitesi ve JPEG kaynağının yerel köprüye iletimi. Manuel giriş formu v40 ile aynı SHA-256 değerindedir: ca96be9a85522dcf389530907364abddee6cfb3166d1805b472fc8bb633ad23a. Ekran alanları, vade eşlemesi ve kayıt/onay işleyişi korunur.
- receipt.js: OCR’nin binlik ayırıcı çevresine koyduğu boşluklar desteklendi; bozuk para tokeninin yalnız bir parçasının tutar sayılması engellendi. Rakamlar tahmin edilmez ve eksik basamak tamamlanmaz. Toplam/vade hesapları değiştirilmedi.
- renderer.js, package.json/package-lock.json, BUILD.bat ve yeni testler/belgeler. Ana proje npm bağımlılıkları değişmedi; gömülü OCR modülleri kendi kilidi ve lisanslarıyla kaynak paketindedir.

## Doğrulama

- Güncel npm test:156/156 test başarılı, sıfır atlama; mevcut182 sayısal kontrol ve JSX derlemesi geçti.
- Önceki108 finans/güvenlik testi ve fiş hesaplama/doğrulama senaryoları korunur. Windows’a özel eski OCR köprü testleri receipt-v40.test.cjs arşivinde durur; artık bilerek kullanılmayan WinRT yanıtı/platform şartı için geçiyor iddiası yoktur. Güncel receipt-v41.test.cjs aynı PNG/çıktı/hata/zaman aşımı güvenlik koşullarını yeni süreç sözleşmesiyle test eder; ayrıca ağ engeli, bozuk/eksik modeller, paketleme kopyası, JPEG boyut/kalite ve para tokeni durumlarını kapsar.
- Gerçek Electron ve gerçek Tesseract: sentetik yedi kalemli görüntüyü (OCR yanıtı sahte olmadan) okuyup toplam/kredi/debit ayrımını ve iki kayıtlı kaydetme akışını doğruladı. Windows OCR/PowerShell kullanılmadı.
- Paketlenmiş Linux uygulaması kaynak klasörüne ihtiyaç duymadan kendi resources içinden gerçek OCR yaptı; aynı inceleme/onay/kayıt akışı geçti.
- Kullanıcının iki örnek fotoğrafı yerelde, uygulamanın gerçek JPEG yükleme/işleme yolunda test edildi. Bu denemelerde her kalem elle doğrulanan beklenen tutarla eşleşti; genel toplam ve kredi/debit ayrımı doğruydu. Özellikle debit yurtdışı kredi vadesine ayrıldı. Fotoğraflar için finansal kayıt oluşturulmadı. Başlangıç OCR ayarlarında bazı alanlar hatalıydı ve eşleşme kapısı bunları engelledi; son ayarla iki örnek geçti. Bu sonuç farklı bütün fişlerde kusursuz OCR garantisi değildir.
- Özel fotoğraflar, ham OCR metinleri veya fotoğraflı ekran görüntüleri ZIP’e/repoya eklenmedi. Paylaşılan otomatik test görseli sentetiktir.
- Mevcut browser-v40 fiş pencere/onay/uyuşmazlık/iptal, browser-v38 çoklu nakit ve browser-v39 ödeme kabul testleri geçti. Finans kayıtları değişmedi; manuel alanlar korunur.
- Windows x64 üretim paketi oluşturuldu; program ASAR çalışma dosyaları kaynakla byte eşit, resources/ocr-runtime235 dosyası manifestle doğrulandı. Kod imza sertifikası yok; derleme imzasızdır. Gerçek Windows cihaz çalıştırması henüz yapılmadı.

## Teslim ve geri dönüş

Tam kaynak ZIP’i, gömülü motor/modeller dahil gelir. Root pos-app/node_modules ve dist geliştirme önbellekleri teslimde yoktur; ocr-runtime/node_modules özellikle dahildir ve silinmemelidir. Programı derlerken normal BUILD.bat yeterlidir. Çalıştırırken win-unpacked klasörünün tamamı kullanılmalıdır; yalnız EXE taşınmamalıdır.

Uygulama kimliği com.poskasayonetim.app ve mevcut yedek şeması korunur. Güncellemeden önce tam JSON ve eski program klasörü saklanmalıdır; veri temizleme/kaldırma gerekmez. Geri dönüş kaynağı v40’tır ve onun Windows OCR dil gereksinimi yeniden devreye girer. Gerçek Windows bilgisayarda iki fiş, eşleşmeme/düzeltme, onay ve yeniden açılış son kez kontrol edilmelidir.
