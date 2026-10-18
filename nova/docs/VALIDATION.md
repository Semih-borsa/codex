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
