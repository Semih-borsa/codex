# POS Kasa Yönetim Build v31 (1.0.9)

## Kök neden ve düzeltme
Önceki sürümde toplam kaydedildikten sonra ortak formun `gid` alanı kayıtlı grubun kimliğinde kalıyordu. Tutar tekrar yazılıp Kaydet'e basılınca yeni işlem yerine o grup güncelleniyordu. Yalnız tutarı boşaltmak, bu kimlik/düzenleme bağlantısını gidermiyordu.

Yeni kayıt ve düzenleme akışları ayrıldı:
- Ana sayfadaki `PosToplamGiris` yalnız yeni kimlikli toplam oluşturur. Eski grup kimliği bu formda tutulmaz. Başarılı yazmadan sonra tutar temizlenir, tarih düne ve kredi vadesi varsayılanına döner. Kasa seçimi kullanılabilir kalır.
- Aynı kasa/günde ikinci kayıt istenirse ayrı kayıt için onay alınır; ilk kayıt asla değiştirilmez. Onay iptalinde taslak ve kayıtlar korunur.
- Düzenleme yalnız İşlem Geçmişi'ndeki kalem simgesinden açılan pencerededir. Mevcut ortak hesaplama yardımcıları kullanılır. Kaydedilmemiş değişiklikle pencereyi kapatırken onay sorulur.
- Yeni debit formu önceki sürümdeki ayarlı vade eşleştirmesini, bugün varsayılanını, başarıda temizlemeyi ve vade tarihli Takip kartlarını korur. Yeni toplam formuyla kimlik/düzenleme durumu paylaşmaz.

## İşlem Geçmişi'nden silme
Eski ayrı POS/debit düzenleme-silme bölümü ve “Toplam kaydını sil” düğmesi kaldırıldı. Geçmiş satırındaki mevcut silme simgesi artık bağlı kayıtları da güvenli şekilde işler:
- Bağımsız işlem: yalnız satır silinir.
- Bağlı debit: yalnız debit silinir; tutarı tam sayı kuruşla bağlı kredinin kalan tutarına geri eklenir.
- POS/kredi toplam satırı: o grubun toplamı ve bütün bağlı debitleri silinir. Onayda etki açıkça belirtilir.
- Bağlantı doğrulanamazsa veya depolamaya yazılamazsa silme tamamlanmaz. Hata pencerede görünür, önceki kayıtlar korunur.

## Doğrulama
- Mevcut 35 güvenlik/dosya testi başarılı.
- Önceki 17 + 29 + 25 hesap kontrolü başarılı; yeni 19 kontrolle **90 hesap kontrolü** başarılı. JSX derlemesi de başarılı.
- Yeni Chromium ekran senaryosu başarılı: art arda farklı gün kayıtları, aynı gün yeni kaydı iptal/onaylama, boşalan tutar ve yeni kayıt kimliği, ilk kaydın korunması, geçmişten silme iptali, debit silince krediye iade, POS grubunu silme, ilişkisiz kayıtların korunması, açık düzenleme, ödenen debitten düşük toplamın reddi, kaydedilmemiş düzenlemeyi kapatmayı iptal etme, yazma hatasında toplam girişinin ve kayıtların korunması, silme yazma hatası, bağımsız manuel işlem silme, yeniden açılış.
- v30'un yeni debit eşleştirme/tarih/temizleme/hata koruma/Takip görünümü testleri v31'de başarılı. Kullanıcı isteğiyle kaldırılan eski ayrı düzenleme düğmesinin stil kontrolü yeni regresyon betiğine taşınmadı; diğer finansal ve ekran beklentileri korundu.
- Eski v27–v30 ekran betikleri silinmedi. Kaldırılan eski ortak forma bağımlı v27–v29 betiklerini güncel akış başarılıymış gibi raporlamıyoruz. Yerine gerçek yeni akışı sınayan `browser-v31.mjs` ve `browser-v31-regression.mjs` çalıştırıldı. Mevcut sayısal/güvenlik testlerinin beklentileri gevşetilmedi.
- Yerel Electron testi başarılı: art arda bağımsız toplamlar, yeni alanın temizlenmesi, sıfır gün debit vadesi, debit silince kredinin geri gelmesi, POS silme, diğer toplamın korunması ve yeniden açılış. Sentetik, ayrı profil kullanıldı.
- Windows x64 Electron paketi başarıyla derlendi. Kaynak ile paket içindeki runtime dosyaları karşılaştırıldı. Sertifika yapılandırılmadığından imzasız çıktı üretildi. Gerçek Windows cihazında çalıştırma testi bu Linux ortamında yapılmadı.

Örnek: 3053,50 TL ilk toplam + 4000,00 TL ikinci toplam ayrı korundu. İlkine 1000,25 TL debit eklenince kalan 2053,25 TL oldu; debit silinince 3053,50 TL'ye döndü. İlk toplam silinince ikinci toplam değişmeden kaldı. Bu sonuçlar hem hesap yardımcılarında hem ekranda doğrulandı.

## Değişen dosyalar
`pos-app/app-source.jsx`, üretilen `renderer.js`; sürüm etiketleri `package.json`, `package-lock.json`, `BUILD.bat`. Testler: `tests/calculation.cjs` ek kontrolleri, `browser-v31.mjs`, `browser-v31-regression.mjs`, `electron-v31.mjs`, test günlükleri ve rapor/kullanım belgeleri.

Yeni veritabanı/bağımlılık/ikinci finans motoru yok. Logo, kimlik `com.poskasayonetim.app`, ürün adı, mevcut yedekleme ve `pos:*` saklama alanları korundu. Gerçek kayıtlar ve eski yedekler değiştirilmedi; mevcut veriye toplu dönüşüm yapılmadı.

Bu düzeltme önceki sürümde üzerine yazılmış bir kaydı kendiliğinden geri getirmez. Böyle bir kayıt için üzerine yazılmadan önce alınmış yedek gerekir; tutar/tarih tahmin edilerek geçmiş veri üretilmez.

## Teslim ve geri dönüş
ZIP bütün kaynak ve vendor dosyalarını içerir; `node_modules`, `dist`, önbellek ve kullanıcı profili içermez. Hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Önce JSON yedek alın ve eski çalışan program klasörünü saklayın; kullanıcı verilerini temizlemeyin.

Önceki kaynak geri dönüş noktası: Build v30 commit `142710f609d0095d6aacf7405ad0292f59d8c4a9`. Veri şeması aynı kaldı; yeni kayıtlar ve silme sonrası kalan işlemler önceki sürümlerin de okuduğu biçimdedir.
