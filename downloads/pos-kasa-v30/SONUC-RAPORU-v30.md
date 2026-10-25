# Build v30 (1.0.8) sonuç raporu

## Uygulanan akış
İşletmem Kasa'nın repodaki `kasadenge/app/src/main/assets/settlements.mjs` içindeki yatış günü → satış günü → toplamdan debit çıkarma yaklaşımı incelendi. Oradaki sabit debit gecikmesi yerine, kullanıcının açık tercihiyle bu uygulamanın Ayarlar bölümündeki **Debit vade günü** kullanıldı. İşletmem Kasa kaynakları değiştirilmedi.

- Yeni POS satış tarihi varsayılanı dün; yeni debit yatış tarihi bugün.
- Yeni, sade yatan debit formunda kasa/yatış günü/tutar girilir. Tek, kesin vade eşleşmesi otomatik bulunur. Birden fazla veya sıfır eşleşmede tahminle kayıt oluşturulmaz; kullanıcı satış kaydını açıkça seçebilir.
- Debit gerçek yatış gününe eklenir. Aynı gruptaki kalan kredi tam sayı kuruşla azaltılır; kredi vadesi ve diğer kayıtlar korunur. Aynı toplam için daha sonra girilen yeni debitler önceki debitlerin üzerine yazılmaz.
- Yeni debit girişi başarılı kayıttan sonra temizlenir; yatış tarihi bugüne döner. Hata/iptal halinde giriş korunur. Boşalan alanla tekrar Kaydet'e basmak ikinci kayıt oluşturmaz.
- Eski kayıtları düzenleyen alan ayrıca korundu. Kayıtlı debit satırlarını gösterir; bu satırlar yeni giriş alanı gibi temizlenmez. Üstte kayıtlı debitler düzenlenirken yeni giriş kaydedilirse taslak kaybı için onay kontrolü vardır.
- Tüm ilgili Kaydet düğmeleri üstteki yeşil manuel düğmeyle aynı ortak stil, simge ve hover sınıfını kullanır.
- Takip ekranının tasarımı ve toplam hesabı değiştirilmedi: yeni debit gerçek yatış gününde, kalan kredi mevcut kredi vadesinde aynı banka/kasa kartlarında gösterilir. Takip tarih alanına test erişim etiketi eklendi.

## Test sonuçları
- Mevcut 35 güvenlik/dosya testi: başarılı.
- Önceki 17 hesap kontrolü ve 29 ek kontrol: başarılı.
- Yeni 25 otomatik debit eşleştirme/ekleme kontrolü: başarılı. Kuruş farkı, aynı grup için ikinci debit, diğer grupların korunması, negatif/sıfır/aşım tutarları, hatalı/gelecek tarihler, eksik ve çoklu eşleşme, açık düzeltme, sıfır gün ve ay sonu vadesi, tekrarlı kimlik ve yedek şema doğrulaması kontrol edildi.
- JSX derlemesi: başarılı.
- Önceki v27, v28, v29 React ekran testleri ve v29 Electron testi: başarılı.
- Yeni v30 Chromium testi: varsayılan tarihler, ayarlanan vadeyle eşleştirme, yeni tutarın temizlenmesi, boş tekrarlı kaydın engellenmesi, hata halinde girişin korunması, Kaydet düğmelerinin stillerinin eşitliği, kredi vadesindeki gerçek Takip kartında kalan tutar, çoklu/eksik eşleşme, kullanıcı düzeltmesi, önceki debitlerin korunması ve yeniden açılış başarılı. Sentetik localStorage yazma hatasında eski veriler ve girilen tutar korundu; sahte başarı gösterilmedi.
- Yeni v30 yerel Electron testi: sıfır günlük debit vadesiyle otomatik kayıt, dün/bugün varsayılanları, tutarı temizleme, kalan kredi ve yeniden açılış başarılı.
- Windows x64 paket derlemesi başarılı. Sertifika yapılandırılmadığından çıktı imzasızdır. Bu Linux ortamında gerçek Windows cihaz testi yapılmadı.

Örnek: toplam 3053,50 TL; debit 1000,25 TL; kalan kredi **2053,25 TL**. Takip ekranında kredi vade günü seçildiğinde bu tutar görüldü. Bir sonraki debit 53,25 TL eklendiğinde kalan 2000,00 TL ve iki ayrı debit kaydı korundu.

## Dosyalar ve veri güvenliği
Üretim: `pos-app/app-source.jsx`, üretilen `renderer.js`, `package.json`, `package-lock.json`, `BUILD.bat`. Test: `tests/calculation.cjs` ek kontrolleri, `tests/browser-v30.mjs`, `tests/electron-v30.mjs` ve sonuç günlükleri. Belgeler bu rapor ve kullanım dosyası.

Yeni veritabanı, bağımlılık veya ikinci finans motoru yok. Mevcut `pos:txs` grupları kullanılıyor. `safety.js`, native yedekleme, uygulama kimliği `com.poskasayonetim.app`, ürün adı ve logo korundu. Gerçek kullanıcı verileri ve eski yedekler değiştirilmedi. Yeni debit, kullanıcı kaydettiğinde oluşur; mevcut manuel kayıtlar varsayımla eşleştirilmez.

Vade hesabı mevcut ayara göre takvim günüdür. Banka tatil kayması veya birden fazla olası satış varsa bunu tarih/tutar benzerliğine dayanarak kesin kabul etmez. Yanlış otomatik eşleşmeye karşı formda bulunan satış günü ve kalan tutar kaydetmeden önce görünür; gerekirse açık seçim yapılabilir.

## Teslim ve geri dönüş
Tam kaynak ZIP'i değişen/değişmeyen dosyaları ve vendor dosyalarını içerir. `node_modules`, `dist`, önbellek veya kullanıcı profili içermez; hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Güncellemeden önce JSON yedek alın ve eski çalışan uygulama klasörünü saklayın. Kullanıcı veri klasörünü temizlemeyin.

Geri dönüş kaynağı: Build v29 commit `6e21fd556cd49ff3b423802413abe38f0e689d1e`. Veri şeması değişmedi; yeni debitler önceki sürümün de okuyabildiği bağlı işlem biçimindedir.
