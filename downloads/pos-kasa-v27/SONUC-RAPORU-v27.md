# POS Kasa Yönetim — Build v27 (1.0.5)

## Değişiklikler
- Toplam POS ve yatan debit girişleri ortak, katı `POSGuard.money` tutar doğrulamasını kullanır. `3053,50`, `3053.50`, `3.053,50` ve sayı olarak `3053.5` aynı 3053,50 TL değerini verir. Alan odağı kaybedildiğinde ve kayıt açıldığında gösterim `3.053,50` olur. JSON'da hesaplamaya uygun sayısal değer tutulur; yedek biçimi değiştirilmedi.
- Önceki kaynakta belirtilen üç yazım biçimi de kabul edildiğinden, cihazdaki hatanın tam nedeni bu kaynakla yeniden üretilemedi. Önceki tek düğme bütün toplam/debit taslağını doğruluyordu; eksik toplam da genel ondalık hatasına düşebiliyordu. Artık boş toplam açıkça belirtilir, debit kaydından önce toplamın kaydedilmesi gerekir. Doğrulamayı gevşetmeden ortak parser ve ayrılmış kayıt akışları kullanıldı.
- **Toplamı kaydet**: Kasa, gün, toplam ve kredi vadesini kaydeder; önceden kaydedilmiş debitleri korur, kaydedilmemiş debit taslağını yazmaz.
- **Debitleri kaydet**: Kaydedilmiş toplam/gün/vadeyi esas alır; debit ekleme/değiştirme/kaldırmayı kaydeder ve kalan krediyi kuruş üzerinden hesaplar. Kaydedilmemiş toplam taslağı otomatik kaydedilmez.
- Yatan debitler toplamı aşarsa kayıt reddedilir; önceki kayıt korunur. Yeniden kaydetme kopya satır üretmez.
- Kalan tutar formdaki taslağın önizlemesidir. Takip ekranı yalnız kaydedilen kayıtları kullanır.

## Örnek
Önce toplam 3053,50 TL → Toplamı kaydet. Sonra yatan debit 1000,25 TL → Debitleri kaydet. Kalan kredi **2.053,25 TL**. Debit kaldırılıp kaydedilirse kalan tekrar **3.053,50 TL**.

## Doğrulama
- Önceki 35 güvenlik/dosya testi: başarılı.
- Önceki 17 hesaplama kontrolü ve JSX derleme: başarılı.
- Yeni 29 tutar/ayrı kayıt kontrolü: başarılı.
- Chromium gerçek React ekranı: üç giriş biçimi, Türkçe gösterim, ayrı kaydetme, taslakların birbirini etkilememesi, yeniden açma, toplamı aşan debit reddi ve debit kaldırma başarılı. Sayfa JavaScript hatası yok. Sentetik kayıtlar ve geçici tarayıcı profili kullanıldı.
- Electron Windows x64 paket derlemesi: başarılı. İmza sertifikası yapılandırılmadığından çıktı imzasızdır. Gerçek Windows bilgisayarda çalışma bu Linux ortamında doğrulanmadı.

## Dosyalar
`pos-app/app-source.jsx`, üretilen `renderer.js`, `package.json`, `package-lock.json`, `BUILD.bat`; testler `tests/calculation.cjs`, `tests/browser-v27.mjs`; bu rapor ve kullanım belgesi. Uygulama kimliği `com.poskasayonetim.app`, ürün adı, logo ve `pos:*` kayıt anahtarları aynı kaldı. Yeni bağımlılık yok; mevcut kullanıcı verileri veya yedekleri değiştirilmedi. `safety.js` ve yerel yedekleme uygulaması değişmedi.

## Teslim ve geri dönüş
ZIP değişen/değişmeyen bütün kaynak ve vendor dosyalarını içerir; `node_modules`, derleme önbelleği, kullanıcı profilleri ve `dist` içermez. Hazır EXE paketi değildir. Windows'ta `pos-app/BUILD.bat` ile derlenir; program `dist/win-unpacked` klasöründedir. Tek başına EXE yerine klasörün tamamı gerekir.

Güncellemeden önce uygulama içinden JSON yedek alın; mevcut çalışan program klasörünü ayrıca saklayın. Kullanıcı veri klasörünü temizlemeyin. Kaynak geri dönüş noktası: önceki Build v26, commit `6d8403d990935d64eb935efa228f0d5e4a0a32e2`. Bu sürüm veri şeması dönüşümü yapmaz.
