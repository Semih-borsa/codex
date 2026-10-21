# Yerel finans yorumları — 1.5.2

## Yapılanlar

Gemini/API anahtarı alanı, model seçimi, bağlantı testi, yorum önbelleği gösterimi ve otomatik API çağrılarını çalıştıran uygulama bağlantısı kaldırıldı. Eski Gemini tercihi kayıtlı olsa bile açılışta veya sayfa değişiminde dışarı yorum isteği gönderilmez. Android'de FinansGemini eklentisinin kaydı da kaldırıldı. Android'in JSON dosya kaydetme/geri yükleme eklentisi korunmuştur.

Yorumlar tamamen cihazda hazırlanır; internet, anahtar veya kota gerektirmez. Ekstre dosyaları ve ekstre analiz özetleri dışarı gönderilmez. Eski finans kayıtlarına, yedeklere veya depolanmış tercihlere toplu dönüşüm uygulanmadı. Önceden şifrelenmiş anahtar kaydı okunmaz/kullanılmaz; finans verileri silinmedi.

## Yeni yorum düzeni

Raporlar → Yorumlar bölümünde uygun veri varsa şu kartlar görünür:

- Dönemin görünümü: gelir, harcama veya nakit çıkışı, kalan/açık ve buna uygun ilk öneri.
- Ödeme gücün: güncel gerçek bakiye, yükümlülükler sonrası kalan ve ödeme açığı.
- Kontrol gereken kayıtlar: kart/taksit verisi eksikse kısa uyarı.
- Borç önceliğin: bekleyen ekstre ve kredi taksitleri; vade ve ödeme disiplini önerisi.
- Harcama odağın: en büyük kategori, diğer önemli kalemler ve esnek giderleri sınırlama önerisi.
- Gider eğilimin: karşılaştırılabilir önceki aralık varsa değişim ve uygulanabilir öneri.
- Önümüzdeki 7 gün / Nakit akışı uyarısı: kayıtlı yakın ödemeler ve varsa tarihli nakit açığı riski.
- Bu dönem için hedefin: ödemeler ve nakit tamponu önceliği.

Hesaplama yöntemini anlatan paragraflar, “Yorumların dayanağı” bölümü ve tekrar eden eski gelişmiş yorum akışı yorum ekranından kaldırıldı. Yıllık birikim raporu korunmuştur. Diğer rapor ekranlarındaki hesap açıklamaları, grafikler ve filtreler değiştirilmedi. Görünüm seçimi korunur: harcama ile nakit çıkışı birbirine karıştırılmaz; gelirden kalan tutara gerçekleşmiş birikim denmez.

Veri yokken olumlu finans hükmü üretilmez. Önceki dönem kaydı yoksa yüzde kıyas uydurulmaz. Geçmiş/özel tarih aralıklarında güncel bakiye, bugünkü kart borcu ve gelecekteki ödeme uyarıları gösterilmez. Güncel ödeme gücü ve nakit uyarıları yalnız mevcut maaş döneminde gösterilir. Eksik kayıt uyarıları yöntem açıklaması değil, karar açısından gerekli veri uyarılarıdır.

## Korunan yapı

İkinci finans motoru kurulmadı. Mevcut `donemBakiyeHesapla`, `gunlukFinansHareketleri` ve `nakitAkisiHesapla` sonuçları yorum metnine dönüştürülür. Kredi/kart/taksit, tarihsel borç, ödeme geri alma, ekstre arşivi, planlama ve yedekleme kuralları korunmuştur. 14 ana finans/yedek/bildirim işlevinin kaynakları işlem öncesiyle aynı bulundu (`kanitlar/scope.json`). Temalar, halka/ibre, animasyonlar ve diğer ekran yerleşimleri korunmuştur. Yeni paket eklenmedi.

Eski sürümün saf, ağ çağrısı yapmayan AI özet/doğrulama yardımcıları tarihsel testler için kaynakta durur; API isteği, anahtar erişimi ve arayüz bağlantıları kaldırılmıştır. Native eski kripto/eklenti kaynakları kayıt dışıdır; bunlar çağrılabilir uygulama özelliği olarak sunulmaz.

## Dosyalar

- `App.jsx`: tek React dosyası; güncel yerel yorumlar ve API arayüz/çağrılarının kaldırılması.
- `android/app/src/main/java/com/butcetakip/app/MainActivity.java`: Gemini eklentisi kaydı kaldırıldı; FinansBackup korundu.
- `android/app/build.gradle`: 1.5.2 / versionCode 15.
- `tests/finance-baseline/local-insights.test.mjs`, `local-insights.browser.test.mjs`: yeni kontroller.
- `source-harness.mjs`: yeni saf yorum işlevinin test kapsamına alınması.

Hazır APK bütün değişiklikleri içerir. Elle derlemede App.jsx `src/App.jsx` olarak ve native dosyalar belirtilen dizinlere kaydedilir; mevcut bağımlılıklarla `npm run build`, `npx cap sync android`, ardından Android derleme çalıştırılır. Önceki native yedekleme dosyaları korunmalıdır.

## Testler

Yeni 8 birim ve 5 gerçek React/Chromium ekran kontrolü başarılı: boş kayıt, sıfır bakiye, açık, borçlar, kıyas, yakın ödeme, geçmiş dönem ayrımı, üç tema, eski Gemini tercihine rağmen sıfır API çağrısı, kayıtların değişmemesi.

Genel koşu: **483/483 başarılı**, 0 başarısız (`kanitlar/all-tests.tap`). Kaldırılan API arayüzüne ait 10 test (KEY01–04, UI-AI01–06) kaynakları silinmeden bu yeni sürümün koşusundan çıkarılmıştır; artık mevcut olmayan butonların görünmesini istemeleri geçerli kabul koşulu değildir. Önceki kaldırılmış PDF arayüzünün 17 eski tarayıcı kontrolü de önceki teslimlerdeki gibi koşu dışındadır. Finansal testlerin beklentileri gevşetilmedi. Kalan eski finans/ekstre/yedek kontrolleri ve yeni yerel yorum testleri çalıştırılmıştır.

`npm run build`, `npx cap sync android`, native birim test görevi ve Android debug APK derlemesi başarılı. Değişmeyen Gradle test görevleri önbellekten yararlanmıştır. Genel lint temizliği bu çalışmanın kapsamında değildir.

## APK ve kurulum

Paket: `com.butcetakip.app`; sürüm: 1.5.2 (15). APK imzası önceki burada teslim edilen debug APK ile aynı: SHA-256 `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`. Dosya boyutu ve dosya SHA-256 değerleri `SHA256.json` içindedir.

Fiziksel telefonda bu APK henüz doğrulanmadı. Önce JSON yedeği al; uygulamayı kaldırma/verilerini temizleme. Telefonda kurulu sürümün imzası bilinmediğinden üzerine güncelleme garanti edilemez. İmza çakışırsa mevcut uygulamayı kaldırmak yerine doğru imzayla derleme gerekir.

Telefonda: güncelleme → kayıt/bakiyeleri kontrol → Ayarlar'da API alanı olmadığını kontrol → uçak modunda Raporlar/Yorumlar → mevcut/geçmiş dönem seç → üç tema → yedek al/geri yükle akışlarını doğrula.

`geri-donus/` işlem öncesindeki App.jsx, MainActivity ve build.gradle kopyalarını içerir. Kaynak geri alınabilir; cihazda zorla sürüm düşürme veya veri silme yapılmaz. Geri dönüş APK'sı gerekiyorsa aynı imzayla daha yüksek versionCode kullanılmalıdır.
