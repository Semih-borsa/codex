# v1.2.0 · Onaylı araçlar ve sesli hatırlatmalar

`testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` başarılı. **38 JVM testi**, hata/başarısız/atlanan yok. **45 Android kontrolü** API 30 x86_64 yazılım emülatöründe geçti. Son üretim APK'sında lint 0 hata/uyarı ve önceki sürümle aynı debug sertifikası doğrulandı (`a3c71704fc4e5a498772ad1825325fc493203b962d3b7c3f9f9db5c0dbf273c8`).

Yeni kontroller: Türkçe göreli/yazıyla süreler, yarın/noktalı saat, adlandırılmış ve sayısal tarih, geçmiş/geçersiz tarih ve yaz saati boşluğu; numara/metin doğrulaması, WhatsApp/SMS niyetlerinin URL kodlaması; şema 1→2 yükseltmesinde görev/sohbet korunması, eski görevlerde sesli seçimin kapalı kalması, sesli seçimin tekrar açılışta korunması; onaysız sesli komutun görev oluşturmaması ve iptalin veritabanını koruması. Gerçek mesaj gönderilmedi, mesaj uygulamaları testte başlatılmadı. SEND_SMS ve READ_CONTACTS izinlerinin yokluğu ve ses servisinin dışarıya kapalı olması doğrulandı.

Bildirim teslimi, özel kilit ekranı görünürlüğü, sesli oku eylemi, ses servisi başlatılınca ön plan bildirimi veya eksik çevrimdışı ses uyarısı ve durdurunca ön plan bildiriminin kalkması denetlendi. Emülatörde fiziksel ses dinlenmedi; bu sonuç Türkçe ses kalitesi veya kilitli fiziksel Xiaomi'de zamanında teslim garantisi değildir. Android 12+ kesin alarm izni, Android 14/15 ön plan servis kısıtları, Xiaomi pil yönetimi, kilit ekranı ve Doze fiziksel hedef cihazda ayrıca denenmelidir. Hiçbir sistem izni/ayar test tarafından kendiliğinden değiştirilmedi. Normal kullanıcı verileri yerine ayrı test veritabanları kullanıldı; test dosyaları otomatik silinmedi.

Araştırma HTML/JSON örnekleriyle HTTPS kaynak süzme, tekrarları eleme, sonuç sınırı, DDG yönlendirme çözümleme, Wikipedia giriş metni ve kaynak bağlantıları test edildi. Rastgele arama sonucu URL'leri otomatik ziyaret edilmez. İki kaynak da erişilemezse sonuç uydurulmaz. **DuckDuckGo ve Türkçe Wikipedia HTTPS bağlantıları bu bulut ağında 403 ile engellendi**; canlı web araştırması uçtan uca doğrulanmadı. Aşağıdaki v1.1 bölümünde belirtilen gerçek Qwen/Whisper modeli, mikrofon/TTS ve canlı Open-Meteo kısıtları da geçerlidir.

Native motor tanılaması eğitilmemiş test GGUF'u ile ilk gerçek token üretildiğinde durdurulur; 256 anlamsız tokenı yazılım emülatöründe üretmek kalite testi değildir. Gerçek model ağırlıkları ana APK'da yoktur. Şema artık 2'dir; eski görev/hafıza/sohbet korunur. APK, tam kaynak ZIP'i ve SHA-256 dosyaları aynı sürümden hazırlanır; yayımlanan uzak depo kopyası ayrıca karşılaştırılır.

---

# v1.1.0 · Konuya uygun cevaplar ve sesli sohbet

`testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` başarılı; 19 JVM testi, 0 hata/başarısız/atlanan. Lint 0 hata/uyarı. Android kontrol sayısı 24: önceki 18'e ek olarak yarının hava verisi, hava açıklaması/yağış olasılığı, tarih ve kaynak, eksik sıcaklığı reddetme, tanınan sesi Gönder düğmesine basmadan gönderme ve şehir eksikken şehir sorma.

Sesli yanıt sonrası dinleme, durdurulan/eski oturumun mikrofonu açamaması, 1,4 saniyelik duraklama, salt sessizlik ve kısa tıklamayı konuşma saymama JVM senaryolarıyla doğrulandı. Tanınan sesin uygulamada otomatik gönderilmesi gerçek Activity + tanıma sonucu callback'iyle, ayrı test veritabanında doğrulandı. Bu bir fiziksel mikrofon/TTS/Whisper konuşma kalitesi veya tam akustik döngü testi değildir.

Open-Meteo geocoding ve forecast HTTPS istekleri bu bulut ağından 403 ile engellendi. Hava verisi çözümleme ve gün seçimi, açıkça test için oluşturulmuş JSON yanıtlarıyla denetlendi; canlı API uçtan uca doğrulanmadı. Uygulama bağlantı hatasında hava uydurmaz. Gerçek Qwen ağırlıkları da indirilemedi; tekrar kontrolü ve örnekleme düzeltmesi gerçek modelde Türkçe anlama kalitesinin ölçüldüğü anlamına gelmez. Xiaomi üzerinde canlı hava ve karşılıklı sesli konuşma doğrulaması hâlâ gerekir.

Aynı uygulama kimliği ve imza ile güncelleme yapılır; veri şeması değişmedi, eski model dosyaları otomatik silinmez veya yeniden indirilmez. APK/source ZIP/sağlama toplamları yayından sonra uzak depo kopyasında karşılaştırılır.

---

# NOVA AI · Doğrulama

Bu belge tamamlanan kontrollerin sonuçlarıyla yayın öncesi güncellenir. APK'nın kurulması, büyük modelin gerçek cihazda Türkçe cevap kalitesinin doğrulanması anlamına gelmez.

Hedef fiziksel cihaz: Xiaomi 12T Pro / 12 GB RAM. Bu cihaz bulut ortamına bağlı değildir.

Hugging Face'e bu ortamdan erişim HTTP tünel 403 ile engellendi. Qwen ve Whisper gerçek ağırlıkları burada indirilemedi; APK'nın telefondan indirme ve elle dosya içe alma yolları bulunur. Gerçek 1.5B model sohbeti, Türkçe mikrofon çözümleme, sesli cevap kalitesi, pil/ısı ve Xiaomi bildirim teslimi fiziksel cihazda henüz doğrulanmadı.

Android test APK'sındaki küçük GGUF yalnızca rastgele ağırlıklı, eğitilmemiş bir tanılama dosyasıdır. Yerel model yükleme/token üretme yolunu test eder; gerçek Adaş modeli veya cevap kalitesi testi değildir. Bu dosya kullanıcıya verilen ana APK'nın içinde değildir. Test verileri ayrı readiness veritabanında tutulur; finans uygulamasının veya NOVA'nın normal verileri değiştirilmez.

## Tamamlanan kontroller

- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: başarılı. 5 JVM testi, 0 hata/başarısız/atlanan; Lint: 0 hata ve 0 uyarı.
- API 30 x86_64 emülatörde 18 Android kontrolü geçti: SQLite kayıtları ve yeniden açma, DOCX paragrafı, Excel hücresi ve paylaşılan metin, gerçek PDF metni/sayfa kaynağı, boyut sınırı, bozuk GGUF reddi, CPU token üretimi, bozuk Whisper modeli reddi ve model kopyalamada iptal.
- ARM64 ve x86_64 native motorlar APK'ya dahil. APK kişisel kullanım için debug imzalıdır.

Yerel `libnova.so` ve `libc++_shared.so` ELF PT_LOAD segmentleri iki ABI için de 16.384 bayt hizalıdır. Android 16 KB sayfa boyutu desteği NDK derleme ayarıyla etkinleştirilmiştir; gerçek 16 KB cihaz testi yapılmadı.

Bulut başlangıç talimatları `cloud-environment-onboarding:setup` ile ortam taslağındaki `start_skill` alanına kaydedildi. Bu kayıt çalışan ortamı yeniden başlatmaz veya yayımlamaz; kalıcı bulut ortamı güncellemesi için Ortam ayarlarında taslağı gözden geçirip kaydet/yayımla gerekir. Yeni bir bulut görevinde geri yükleme ayrıca test edilmedi.

Gerçek arayüz kontrolleri geçti: hafıza ekleme, silme onayını iptal ederek kaydı koruma, görev ekleme/tamamlama/yeniden açma, model indirme onayını iptal etme, açık/koyu tema, belge ekranı ve süreç yeniden başlatıldıktan sonra hafıza/görevlerin korunması. Görseller `docs/screenshots` altındadır. Emülatör açılırken yazılım emülasyonu nedeniyle bir System UI yanıt vermiyor penceresi görüldü; Bekle ile devam edilip uygulama akışları tamamlandı. Bu durum fiziksel telefon performans ölçümü değildir.

Emülatör arayüzünde v1.1 Sesli sohbet düğmesi, model olmadan yazılı hava sorusunda şehir isteme ve hava şehri ayar ekranını iptal etme geçti. Hava/soru ekranı `docs/screenshots/weather-city-v11.png` altında. Bu görsel canlı tahmin kanıtı değildir.


v1.2 arayüz kontrolü: Hatırlat/Mesaj/Araştır kısayolları, yazılı komuttan tarih/sesli seçimli önizleme, iptal edilince kaydetmeme, WhatsApp varsayılanı, gerçek numara/metin önizlemesi, son onay iptali, araştırma gizlilik uyarısı iptali, görevler ekranındaki izin durumu ve önceki görev kaydının korunması geçti. Yeni ekran örnekleri `screenshots/reminder-preview-v12.png`, `screenshots/message-preview-v12.png`, `screenshots/tasks-v12.png`. Komut sonrası yazı alanının temizlenmesi ayrıca Activity testiyle denetlenir.
