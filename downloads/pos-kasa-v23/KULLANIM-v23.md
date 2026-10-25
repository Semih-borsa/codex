# POS Kasa Yönetim — Build v23

## Yeni kullanım
1. Önce mevcut programınızdan JSON yedeği alın. Eski klasörünüzü koruyun.
2. Ayarlar → Kasalar / POS Terminalleri: kasa adı/numarası doğrudan düzenlenebilir. Aktif kutusu kasayı pasife alır veya tekrar etkinleştirir. Geçmiş işlemler silinmez. Geçmişi olan kasanın silinmesi engellenir.
3. Yalnız yeni hesaplama yöntemini kullanacak kasalarda **Toplam POS takibi** kutusunu işaretleyip KAYDET'e basın. Varsayılan kapalıdır; diğer kasalar eski manuel kredi/debit yöntemiyle çalışır.
4. GİRİŞ → Toplam POS / Yatan Debit Takibi: kasa, işlem günü, toplam POS ve kredi vadesini seçin. Vade ayarlardaki kredi kartı gün sayısından hesaplanır; gerekirse doğrulanmış tarihi seçebilirsiniz.
5. Her yatan debit için ayrı tutar ve gerçek yatış tarihi ekleyin. Örnek: 10.000 TL toplam − 3.000 TL − 2.000 TL = 5.000 TL kredi kartı alacağı. Debit kaydı kredi vadesi yerine kendi gerçek yatış tarihine yazılır. Kalan kredi vade gününde mevcut TAKİP ekranına gelir.
6. Üstteki kayıt seçicisinden aynı toplamı yeniden açıp düzenleyin veya debit ekleyin. Mevcut bağlı satırlar değiştirilir; her kaydetmede yeni toplam eklenmez. Tek kasa/işlem gününde ikinci toplam engellenir. Bağlı kayıtlar işlem listesinden tek tek değiştirilemez; bu bölümden düzenlenir veya birlikte silinir.

## Muhasebe sınırları
- Bu bir banka bağlantısı değildir. Debitlerin gerçekten yatıp yatmadığını siz kaydedersiniz. Girilmemiş debit tutarı başlangıçta kalan krediye dahildir; bütün debitler girilene kadar kredi tahmini geçicidir.
- Toplam ve debitleri aynı brüt/net esasıyla girin. Banka komisyonu ayrıca ayrıştırılmaz.
- Önceki manuel işlemler varsayımla eşleştirilmez/dönüştürülmez. Aynı tutarı hem manuel hem toplam modunda girmeyin. Yeni mod, sonraki günlük kayıtlar için kasa bazında seçilebilir.
- Pasif kasalar yeni girişte görünmez; eski kayıtları TAKİP'te ve geçmişte korunur. Pasif kasanın mevcut toplam kaydı güncellenebilir.
- Gerçekten yatan yeni debit kayıtları nakit planında tekrar gelecek para sayılmaz. Eski manuel kayıtlara bu etiket otomatik atanmaz.
- Toplam/debit hesabı kuruş üzerinden yapılır. Debit toplamı POS toplamını aşamaz; hatalı/ileri tarih ve negatif tutarlar reddedilir.

## Teslim ve doğrulama
Bu proje Android değil, Electron tabanlı Windows uygulamasıdır. Önceki uygulama adı ve appId korunur: com.poskasayonetim.app. Sürüm 1.0.1. Mevcut localStorage anahtarları ve yedek biçimi korunur; yeni bağ bilgileri aynı işlem kayıtlarında tutulur. Gerçek kullanıcı kayıtlarında işlem yapılmadı.

Değişen orijinal dosyalar: pos-app/index.html ve pos-app/package.json. Diğer orijinal dosyalar aynen korunmuştur. Yeni kullanım belgesi ve hesaplama kontrol dosyası eklendi.

17 hesaplama kontrolü ve JSX derlemesi başarılı. Gerçek Chromium'da toplam/debit kaydetme, tekrar düzenleme, kasa adı/numarası değiştirme, pasif kasa/normal kasa ayrımı ve yeniden açılışta kayıtların korunması doğrulandı. Testlerde sentetik veriler kullanıldı.

Windows EXE derlemesi ve gerçek Windows uygulamasında son test yapılmadı. Kendi ortamınızda mevcut BUILD.bat yönteminizi kullanın. npm/electron-builder bağımlılıkları değiştirilmedi. Eski uygulamanızın verilerini temizlemeyin veya kaldırmayın. Yeni sürümü açınca mevcut verilerin göründüğünü kontrol edin; yedek geri yüklemeyi yalnız gerekirse kullanın.
