Windows 1.0.1: [Düzeltilmiş Windows ZIP](../downloads/Izin360-Windows-1.0.1-x64.zip). Windows yol ayırıcı hatası nedeniyle JavaScript/CSS engellenip beyaz ekran oluşması düzeltildi.

Güncelleme: Önce eski klasördeki **Izin360-Durdur.exe** ile arka plandaki sunucuyu kapatın. Açık İzin360 pencerelerini kapatın. Yeni ZIP'in tamamını yeni bir klasöre çıkarıp **Izin360.exe** dosyasını çalıştırın. Eski sunucu açık kalırsa yeni program eski sunucuya bağlanır. `%APPDATA%\Izin360` kayıt klasörünü ve tarayıcı profilini silmeyin; yeni paket aynı veri dizinini kullanır. ZIP içindeki `app/dist/index.html` dosyası çift tıklanarak açılmaz.

# İzin360 · Windows ve Android personel takibi

Tek kişinin kendi personel kayıtlarını yönetmesi için geliştirildi. **Personel hesabı, talep, onay/ret veya akaryakıt modülü yoktur.** Yeni personelin izin hakkı **0 gün** başlar; işe giriş veya yıl değişiminde kendiliğinden hak eklenmez.

## İndir

- [Windows 10/11 x64 paketi](../downloads/Izin360-Windows-x64.zip)
- [Android 8+ APK](../downloads/Izin360.apk)
- [Kaynak proje paketi](../downloads/Izin360-Kaynak.zip)
- [Dosya sağlama toplamları](../downloads/Izin360-SHA256SUMS.txt)

GitHub dosya sayfasında **Download raw file** düğmesini kullan. Windows ZIP'ini tümüyle ayıkla; ZIP içinden doğrudan çalıştırma.

## Windows'ta başlatma

1. Paketin tamamını bir klasöre ayıkla; `runtime` ve `app` klasörlerini taşıma veya silme.
2. **Izin360.exe** dosyasını aç. Microsoft Edge varsa uygulama penceresi açılır; yoksa varsayılan tarayıcı kullanılır. Node.js veya başka bir program ayrıca kurman gerekmez.
3. İlk personelini ekle. Hak, devir ve varsa önceki kullanılan izni kendin gir.
4. Pencereyi kapatmak eşitleme sunucusunu durdurmaz; telefonun bağlanabilmesi için arka planda açık kalır. Tamamen durdurmak için **Izin360-Durdur.exe** kullan.

Windows paketi imzasız bir ilk sürümdür; SmartScreen uyarısı görülebilir. Windows güvenlik duvarı sorarsa yalnızca güvendiğin **özel ağ** erişimine izin ver. Kullanıcı hesabının kayıt dizini `%APPDATA%\Izin360`; Edge uygulama profili `%LOCALAPPDATA%\Izin360\Edge` altındadır. Paketi yeni klasöre güncellemek bu dizinleri silmez. Port 4310 başka program tarafından kullanılıyorsa uygulama uyarır; başka işlemler sonlandırılmaz.

## Android'e kurulum ve aynı Wi‑Fi'da eşitleme

1. `Izin360.apk` dosyasını telefona indir ve aç. Gerekirse dosyayı açtığın uygulamaya bilinmeyen kaynak kurulum izni ver. Güncel **Android System WebView / Chrome** gerekir.
2. Windows ve telefonu aynı güvenilir Wi‑Fi ağına bağla. Windows programı açık olsun.
3. Windows uygulamasında **Ayarlar → Windows Bağlantı Bilgileri → Bu Bilgisayarın Bağlantı Bilgilerini Göster** düğmesine bas.
4. Telefonda **Diğer → Ayarlar → Windows – Android Bağlantısı** bölümüne bilgisayarın gösterdiği adresi ve bağlantı anahtarını gir. Adres örneği: `http://192.168.1.20:4310`. Telefon için `localhost` kullanma; o adres telefonun kendisidir.
5. **Kaydet ve Eşitle** düğmesine bas. Başarılı olduğunda üstte **Güncel** görünür. Bir kayıt düzenlendiğinde önce bekleyen değişiklik sayısı görünür; ardından eşitlenir. Uygulama açıkken yaklaşık 25 saniyede bir kontrol edilir; Şimdi Eşitle düğmesiyle beklemeden yapılabilir.

Bilgisayar kapalıyken veya ağ yokken kayıtlar cihazda kalır. Eşitleme uygulama tekrar açılıp bilgisayara ulaştığında devam eder. Android arka planda kesintisiz servis çalıştırmaz. Aynı personel/kayıt iki cihazda farklı değiştirilirse **Sürümleri karşılaştır** ekranında hangisinin korunacağını sen seçersin. Çakışan değişiklikler sessizce birbirinin üzerine yazılmaz. İşlem grubu sunucuda birlikte kabul edilir veya reddedilir.

Bilgisayarın yerel IP adresi değişirse telefondaki adresi güncelle; anahtar aynı kaldıysa tekrar gir. Misafir Wi‑Fi ağları cihazlar arası erişimi kapatabilir. Sunucuya ulaşılamazsa Windows özel ağ güvenlik duvarı iznini ve 4310 portunu kontrol et. **Yönlendiricide port açma, sunucuyu internete doğrudan yayınlama.** Yerel HTTP trafiği aktarımda şifrelenmez; yalnızca güvenilir özel ağ içindir. Bu sürüm için ücretli sunucu veya alan adı gerekmez.

## Kayıtlar ve manuel müdahale

- **Personeller:** ad, bölüm, görev, telefon, işe giriş, izin hakkı, devir, kullanılan izin ve notlar düzenlenebilir. Personeli arşivlemek geçmişini korur; gerekirse arşivdekileri gösterip yeniden etkinleştirebilirsin.
- **İzin hakkı:** yeni personelde sıfırdır. Yasal hak veya kıdem hesabı yapılmaz. **Kalan = Hak + Devir − Kullanılan + Düzeltme.** Kullanılan alanını elle değiştirmek önceki kullanım düzeltmesini hesaplar; mevcut izin satırlarını silmez. Yeni yıl sıfırlaması yoktur; dönem geçişini hak/devir/kullanılan alanlarıyla sen yönetirsin.
- **İzin kayıtları:** tür, başlangıç/bitiş, gün sayısı, not ve bakiyeden düşülüp düşülmeyeceği düzenlenir. Başlangıç ve bitiş dahil gün önerilir; cumartesi/pazar çıkarma yardımcısı vardır. Resmî tatiller otomatik çıkarılmaz. `0,5` gibi kesirli günler girilebilir. Elle yazılan gün sayısı sonraki tarih değişiminde kendiliğinden değiştirilmez. Tarih çakışması uyarısı bilinçli olarak geçilebilir. Gelecek tarihli ve geçmiş tüm düşülecek kayıtlar bakiyeye dahildir.
- **Manuel bakiye:** artı/eksi gün ekleyebilir veya doğrudan olması gereken kalan günü belirleyebilirsin. Açıklama zorunludur. **Değişiklik Geçmişi → Bakiye Düzeltmeleri** bölümünden düzeltmenin tarihini, miktarını ve açıklamasını düzenleyebilir veya silebilirsin.
- **Puantaj:** gün, durum, çalışma ve fazla mesai dakikaları elle girilir. Aynı personel/gün için iki kayıt engellenir. Puantajdan otomatik izin veya ücret düşülmez.
- **Maaş / bordro:** ay, temel ücret, ek ödeme, kesinti ve ödenen tutar elle girilir. Net ve kalan otomatik toplanır; resmî vergi, SGK veya bordro hesabı yapılmaz. Aynı personel/ay için ikinci kayıt yerine mevcut kayıt düzenlenir.
- **Raporlar:** personel bakiyeleri ile ilgili ekranın aylık kayıtları Excel uyumlu UTF‑8 BOM/`;` ayracılı CSV dosyasına aktarılır. İzin kaydındaki yazdır simgesi hazır izin formu üretir; Windows'ta yazdır/PDF, Android'de sistem yazdırma menüsü kullanılır.
- **Değişiklik geçmişi:** ekleme, düzenleme ve silmeler tutulur. Sonradan tekrar değiştirilmemiş kayıtlarda Geri Al kullanılabilir. Bir kaydı geri almak ilişkili kayıtları bozacaksa işlem reddedilir; örneğin izinleri varken personel oluşturma kaydı geri alınmaz. Ekranda son 100 olay, tam yedekte tüm olaylar bulunur.

## Yedekler ve veri güvenliği

**Ayarlar → Tam Yedek İndir** tüm kayıtları ve düzenleme geçmişini JSON olarak dışa aktarır. Geri yüklemeden önce dosya doğrulanır; bozuk, desteklenmeyen veya 10 MB üzerindeki dosya mevcut kayıtların yerini almaz. Geri yükleme onay gerektirir ve eşitleme açıkken diğer cihaza da aktarılır.

Bağlantı anahtarı JSON yedeğine veya dışa aktarımlara eklenmez. Android'de Android Keystore AES-GCM ile şifrelenir; ağ istekleri yalnızca yapılandırılmış sunucuya gider, TLS doğrulaması atlanmaz, yönlendirme takip edilmez. Windows'ta sunucu veritabanı ve bağlantı anahtarı kullanıcıya özel kayıt dizinindedir. Tarayıcı/Android yerel kayıtları ve dışa aktarılan dosyalar uygulama düzeyinde şifrelenmez; cihaz kilidi ve Windows kullanıcı hesabını koru. Buluta otomatik yedek/telemetri gönderilmez.

Tarayıcı verilerini veya Android uygulamasını silmeden önce yedek al. Windows Edge profilini silmek kaydedilmemiş yerel değişiklikleri kaybettirebilir; önce Güncel durumunu görüp JSON yedek al. APK güncellemelerinde aynı imza kullanılmalıdır; teslim APK'sı debug imzalı deneme sürümüdür.

## Ekranlar

[Windows genel bakış](docs/screenshots/windows-overview.png) · [Android boyutunda genel bakış](docs/screenshots/android-overview.png) · [Yeni personel formu](docs/screenshots/android-new-person.png). Ekranlardaki personeller yalnızca test verisidir; dağıtım paketlerinde personel kaydı yoktur.

## Geliştirici kurulumu

Node 24 LTS, JDK 17, Android SDK 35/Build-Tools 34.0.0 gerekir. Windows başlatıcısı için Go 1.23+ kullanılır.

```sh
cd izin360
npm ci
npm test
npm run build
npm start
```

Geliştirme arayüzü: `npm run dev`; Vite `/api` isteklerini 4310'daki sunucuya yönlendirir. Sunucu kayıt dizinini `IZIN360_DATA_DIR` ile, portu `PORT` ile değiştirebilirsin. Üretim varsayılanı 4310'dur. Sunucu `node:sqlite` kullanır; ek sunucu paketi veya veritabanı kurulumu gerekmez.

```sh
npm run android:assets
cd android
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Windows'ta Android Studio ile `izin360/android` klasörünü aç; Windows terminalinde `gradlew.bat` kullan. Önce web derlemesi ve `android:assets` yapılmalıdır. APK konumu: `android/app/build/outputs/apk/debug/app-debug.apk`.

Windows paketi: web derlemesinden sonra `npm run windows:package`. `scripts/runtime.json` içindeki resmî Node Windows paketi SHA‑256 ile doğrulanır. Linux'ta `unzip`/Python, Windows'ta PowerShell kullanılır. `GO_EXE`, `IZIN360_NODE_ZIP`, `IZIN360_BUILD_CACHE` ve `IZIN360_WINDOWS_OUT` ile yerel derleyici/önbellek yolları verilebilir. Paketleme uygulama verilerini içermez.

Tarayıcı işlev testleri ayrı ve **boş test sunucusunda** `IZIN360_E2E_URL=http://127.0.0.1:4321 python3 test/browser-e2e.py` ile çalıştırılır; Playwright ve Chromium gerekir. Test kişisel veri dizininde çalıştırılmamalıdır. [Doğrulama ve sınırlar](docs/VALIDATION.md).
