# İzin360 1.0 — doğrulama ve sınırlar

## Geçen kontroller

- **18 Node testinin tamamı geçti**, 0 başarısız/atlanan. Sıfır başlangıç hakkı, kesin kesirli gün ve para hesabı, gün/hafta sonu hesabı, izin düzenleme/silmede yeniden hesaplama, manuel düzeltmeler, yetim/yinelenen kayıt reddi, yedek doğrulama, CSV formül güvenliği, kimlik doğrulama, iki cihaz sürüm çakışması, toplu işlem geri alma, silinmiş kaydın eski sürümle diriltilmesini önleme ve boş/bozuk sunucu anahtarında güvenli duruş kapsandı. Windows paketindekiyle aynı **Node 24.22.0** sürümünün Linux çalışma ortamıyla da çalıştırıldı.
- **16 tarayıcı işlev akışı geçti**: personelde 0 hak, kullanılan izni elle değiştirme, tarih/gün hesabı, yarım gün düzenleme, kalan bakiyeyi doğrudan belirleme, puantaj, yinelenen puantaj reddi, kuruş hassasiyetinde maaş, Excel uyumlu CSV, JSON yedek, iki bağımsız cihaz deposunda eşitleme, çakışma seçimi, geri yükleme, arşiv/geri alma, mobil yerleşim ve çevrimdışı yeniden açılış. Desktop ve Android boyutlu Chromium'da JS çalışma zamanı hatası görülmedi. Telefon boyutlu testte ağ köprüsü benzetildi; aşağıdaki gerçek APK testi bunun yerine ayrıca çalıştırıldı.
- **8 Android JVM testi geçti**, 0 başarısız/atlanan. Yerel IPv4/HTTPS kök adresi doğrulama, HTTP internet adresi, kullanıcı/şifre gömülü URL, sorgu anahtarı ve özel ağ sınırları kapsandı.
- `assembleDebug` ve `lintDebug` başarılı; **0 lint hatası/uyarısı**. Tam JDK 17, SDK 35, Build-Tools 34.0.0, Gradle 8.9 ve AGP 8.7.3 kullanıldı.
- **Gerçek APK / Android 11 API 30 emülatörü:** paket kuruldu, gömülü arayüz Android WebView'da açıldı, Windows sunucusuna ait 3 test personeli yerel HTTP üzerinden alındı. Bağlantı anahtarı Android Keystore AES-GCM yoluyla kaydedilip ağ isteğinde çözüldü. Personel notu gerçek WebView formundan değiştirilip yerel SQLite sunucusuna gönderildi; sunucudaki değer ayrıca okundu. Gerçek WebView sürümü Chrome 83.0.4103.120 idi. Kontroller debug WebView CDP bağlantısıyla yapıldı; fiziksel dokunmatik/klavye testi değildir.
- Windows `.exe` başlatıcıları Go ile **Windows/amd64 PE** hedefi için derlendi. Paket içindeki Windows Node.js ZIP'i resmî SHA‑256 listesiyle doğrulandı. Sunucu Windows paketi yapısında dış Node modülü olmadan çalışır; web üretim derlemesi pakete dahil edilir. Dağıtım dosyaları personel veritabanı, bağlantı anahtarı, tarayıcı profili veya imza anahtarı içermez.

## Çalıştırılmayan / ayrı cihaz gerektiren kontroller

- **Fiziksel Windows 10/11 üzerinde EXE/Edge penceresi, SmartScreen ve Windows güvenlik duvarı akışı çalıştırılmadı.** Windows paketi çapraz derlendi; Linux'taki sunucu/tarayıcı kontrolleri yerel Windows çalıştırma testi olarak sunulmuyor.
- Fiziksel telefonda Wi‑Fi yönlendiricisi, üretici WebView sürümü, Android dosya seçici/dosya kaydetme ve sistem yazdırma penceresi ayrıca denenmelidir. Android 8 minSDK desteği derleme/API kontrolleriyle; çalışma zamanı ise Android 11 emülatörüyle doğrulandı. Güncel WebView önerilir.
- Eşitleme internet üzerinden barındırılmış bir hizmet olarak dağıtılmadı; istenen kapsam **aynı özel Wi‑Fi ağı**. Android sürekli arka plan eşitleme servisi çalıştırmaz. Uygulama açıkken kontrol eder; yeniden açıldığında bekleyen değişiklikleri gönderir.
- Resmî izin hakkı, Türkiye resmî tatili, SGK/vergi bordrosu hesabı ve kurumsal çok kullanıcılı yetkilendirme bu sürümün kapsamında değildir. Takip ve manuel hesaplama kayıtlarıdır.

Ekran görüntüleri yalnızca sentetik test personellerini içerir. İlk kurulumda personel listesi boştur. `test/browser-e2e.py` ve `test/native-smoke.mjs` yalnızca ayrı test veritabanı ve test emülatöründe çalıştırılmalıdır.
