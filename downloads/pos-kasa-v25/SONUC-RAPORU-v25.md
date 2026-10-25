# POS Kasa Yönetim — Build v25 sonuç raporu

## Uygulananlar

1. **Yedek ve yazma güvenliği:** Şema, tarih/tutar, kimlikler, kasa/banka bağları, vade ve toplam POS/debit tutarlılığı yazmadan önce doğrulanır. Eksik alanlar korunur; açık boş işlem listesi gerçekten temizleme anlamındadır. Kısmi/tam paket ve değişecek alanlar onayda gösterilir. Geri yükleme öncesinde sistem dosya seçicisiyle fiziksel güvenlik kopyası istenir; iptal/yazma hatasında yükleme başlamaz. Ardından yerel güvenlik kopyası ve geri dönüş günlüğü oluşturulur. Yazma hatasında eski kayıtlar geri alınır; yarım kalan günlük açılışta kurtarılır. Kurtarma başarısızsa yazma engellenir. Bozuk ana kayıt boş veriyle otomatik ezilmez; ham kurtarma ve tam yedekten kurtarma ekranı açılır. Eksik alanlar sıfıra çevrilmez; eski ParaTakip-1.0 biçimi korunur.
2. **Manuel giriş ve vade:** Negatif/bozuk/iki ondalığı aşan tutarlar ve geçersiz tarihler reddedilir. Tutar alanı artık eksi işaretini sessizce silip negatif tutarı pozitife dönüştürmez. Günlük POS işlem tarihi gelecekte olamaz; yatış tarihi işlem tarihinden önce olamaz. Sıfır gün vade korunur; ayarlarda 0–365 tam gün doğrulanır. Düzenlenen kasanın banka ilişkisi kontrol edilir. Bağlı Toplam POS satırları tek tek değiştirilmez.
3. **Okunabilirlik:** Gömülü IBM Plex Mono ve mevcut koyu tasarım korundu. Yardımcı yazılar çoğunlukla en az 11 piksele çıkarıldı; soluk gri etiketlerin kontrastı artırıldı. Düzenle/Kaydet/Vazgeç kilitli kasa akışı ve kompakt Toplam POS konumu korundu.
4. **Geçmiş ve klavye:** Kasa/banka/tür/tutar araması, işlem tarihi başlangıç/bitiş filtresi ve 50 kayıtlık sayfalama eklendi; 500 kayıt sınırı kaldırıldı. Pencerelerde Tab/Shift+Tab odağı içeride tutulur, Escape kapatır, kapanışta odak önceki kontrole döner. Ana gezinme, düzenle/kapat/kaydet ve yedek düğmelerinde SVG simgeleri kullanıldı; erişilebilir adlar ve klavye odağı eklendi.
5. **Electron güvenliği:** webSecurity açıldı; sandbox/contextIsolation açık, nodeIntegration kapalı. Güvenli preload yalnız dosya kaydetme isteğini açar. Main IPC yalnız paketlenmiş ana sayfanın ana çerçevesinden gelen isteği kabul eder; dosya konumu kullanıcı tarafından sistem seçicisinde belirlenir. Dosya önce geçici dosyaya yazılıp fsync/byte kontrolü yapılır, taşınır ve yeniden okunarak doğrulanır. Harici pencereler, webview ve sayfa dışına gezinme engellenir; yalnız güvenilen yerel sayfanın bildirim izni korunur, kamera/mikrofon gibi izinler verilmez. Tek uygulama örneği kilidi eklenmiştir. Çalışma anındaki Babel/eval kaldırıldı; renderer önceden derlenir ve script CSP yalnız self olur.

## Sürüm ve uyumluluk

- Build v25 / uygulama sürümü 1.0.3.
- Uygulama adı, appId `com.poskasayonetim.app` ve pos:* kullanıcı kayıt anahtarları aynı.
- Electron 45.0.0 ve electron-builder 26.18.0: çalışma sırasında npm kayıt defterinde kararlı sürüm olarak gözlenen sürümlere sabitlendi. package-lock.json eklendi.
- Node.js 22 veya yeni LTS derleme için gerekli. BUILD.bat yönetici yetkisi istemez; npm ci → test → Windows derlemesi yapar ve doğru çıktı klasörünü açar.
- Ana kaynak artık pos-app/app-source.jsx; build-renderer.cjs renderer.js üretir. index.html derlenmiş renderer'ı yükler. vendor/babel yalnız kaynak derlemesinde kullanılır, Windows paketinde yoktur.
- Eski kayıtlar otomatik dönüştürülmedi/silinmedi. Gerçek kullanıcı depolaması üzerinde test yapılmadı. Yeni sürüm açılışta tutarsız kayıt bulursa tahminle düzeltmek yerine açılışı durdurur ve kurtarma seçeneklerini sunar.

## Doğrulama sonuçları

- 35 otomatik güvenlik/dosya testi: tümü başarılı, atlama yok. Eski yedek, kısmi paket, boş liste, bozuk JSON alanları, kimlik/bağ hataları, tutar/tarih, POS toplamı, güvenlik kopyası başarısızlığı, ara yazma hatası/geri dönüş, açılış kurtarma, eşzamanlı değişiklik ve gerçek dosya yazma/doğrulama kapsanır.
- Önceki 17 hesaplama kontrolü ve JSX derlemesi başarılı.
- Chromium: toplam/debit, düzenlemede çift kayıt olmaması, pasif/normal kasa ayrımı, bağımsız kasa kaydetme/vazgeçme, yeniden açılışta saklama; geçmiş arama/sayfalama, Escape, negatif tutar reddi ve 0 günlük yatış başarılı.
- Gerçek Electron 45 **Linux** çalışma testi başarılı: uygulama açılışı, sandbox/webSecurity/contextIsolation, preload, sınırlı pencere açma, gerçek IPC dosya yazma/iptal, FileReader ile bozuk yedek reddi ve başarılı/iptal geri yükleme. Sistem diyalogu seçimi testte taklit edildi; dosya yazma ve byte doğrulama gerçek dosya sistemi üzerinde yapıldı. Gömülü font yükleme ve XLSX üretimi de doğrulandı.
- **Windows x64 hedef paket derlemesi Linux üzerinden başarılı.** ASAR içindeki yedi kritik çalışma dosyası kaynakla byte olarak karşılaştırıldı. Windows executable PE çıktısı üretildi; imzalama sertifikası verilmediği için paket imzasızdır.
- npm audit: bildirilen 0 açık. Bu denetim gömülü vendor dosyalarının bağımsız güvenlik sertifikası değildir ve sıfır risk garantisi sayılmaz.
- **Gerçek Windows bilgisayarında çalıştırma testi yapılmadı.** Bu ortam Linux; Windows test sonucu varmış gibi gösterilmedi.

## Gerçek Windows kabul kontrolü

Önce mevcut uygulamadan JSON yedeği alın ve ayrı bir yerde saklayın. Eski sürümü/anahtar verisini silmeyin. İki sürümü aynı anda açmayın.

1. Aynı uygulama adıyla yeni sürümü açın; kasalar, işlem sayısı ve tutarlar eski sürümle aynı olmalı.
2. Kasa Düzenle → Vazgeç ve Kaydet akışlarını, aktif/pasif ve Toplam POS seçimini deneyin.
3. 0 günlük vade ile küçük sentetik kayıt girin; işlem ve yatış günü aynı olmalı. Negatif tutar reddedilmeli.
4. Toplam 10.000, yatan debit 3.000 + 2.000: kalan kredi 5.000; yeniden kaydetmek çift kayıt üretmemeli.
5. Geçmişte arama/tarih filtresi ve ikinci sayfayı açın; kaydı düzenleme penceresinde Tab, Shift+Tab, Escape deneyin.
6. JSON yedeği yerel diske kaydedin. Kaydetme penceresini iptal ettiğinizde başarı mesajı olmamalı.
7. Önce sentetik veriyle hazırlanmış test yedeğinde geri yüklemeyi deneyin. Güvenlik dosyası seçimini iptal edince kayıtlar değişmemeli. Bozuk dosya reddedilmeli.
8. Dosya kaydı için Windows erişim engeli/veri alanı dolu hatalarını ve OneDrive/ağ klasörlerini ayrıca test edin.
9. Excel dışa aktarma ve mevcut Windows ödeme bildirimlerini gerçek cihazda kontrol edin.

## Geri dönüş

Build v24 kaynak/ZIP teslimi repoda korunur. Güncellemeden önceki doğrulanmış JSON yedeği veri geri dönüş noktasıdır. Electron ana sürümü değiştiği için eski uygulamayı yeni profilin üzerinde zorla çalıştırmayın; eski sürüme dönmek gerekiyorsa profil yedeğini/JSON geri yüklemesini güvenli bir kopyada değerlendirin. Depolama alanını temizleyerek çözüm aranmaz.

## Teslim

Tam kaynak ZIP'i node_modules, test profilleri, geçici derleme önbellekleri ve gerçek kullanıcı verileri içermez. İlk kullanımdaki uygulama örnek/başlangıç verileri orijinal projeden korunmuştur. Windows derleme çıktısı ayrıca çalışma alanında üretildi. Kaynak projede BUILD.bat kullanılabilir. Tüm dosyaların değişen/değişmeyen halleri kaynak paketinde bulunur.
