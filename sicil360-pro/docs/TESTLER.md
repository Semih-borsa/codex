# Doğrulama — Sicil360 Pro 1.1 / 23 Ekim 2026

## Sonuç

- JDK 17.0.17, Gradle 8.9, AGP 8.7.3, Android SDK/build-tools 35.
- JVM: **28 test, 0 başarısız, 0 atlanan** (`ModelTest`: 17, `ExcelTest`: 11).
- `lintDebug` ve release için `lintVitalRelease` başarılı.
- `assembleRelease` ve `assembleDebugAndroidTest` başarılı.
- Android 11 / API 30 x86_64 emülatöründe **son teslim release APK'sı üzerinde 7 kontrol grubu geçti, 0 başarısız**. Testler taklit alıcıya yönelir; gerçek kişiye mesaj göndermez.
- Release paketinde `DEBUGGABLE` bayrağı yok; INTERNET izni yok; `FLAG_SECURE` açık. İmza doğrulandı. Sertifika geliştirme sertifikasıdır; APK Play Store yayını değildir.
- Son APK SHA-256: `90263a5cbe1b0418c3106fe71edb6997ad3ab50fa7c0c92b1fe7aa9a236d4765`.
- Sertifika SHA-256: `a1acf948b51380a975bf87c15fd7eddc3afa4ab021df4cd65495dadebf44b94c`.

## Test Kapsamı

| Alan | Doğrulama |
|---|---|
| Excel | Geçerli XLSX, Türkçe, metin sıfırları, temel sayısal sıfır maskesi, bilimsel sayı, shared/inline string, seyrek sütun, ilk veri satırı ve boş satır |
| Hatalı dosya | Bozuk ZIP, DTD/harici entity, boyut/sütun sınırı, aynı XML satırının tekrarı, hassasiyet aşımı ve aşırı büyük sayı üssünün metne genişletilmemesi |
| Aktarım | Koru/güncelle/listeyi değiştir sayıları, eşlenmeyen isteğe bağlı alanların korunması, hatalı/eksik telefon, mükerrer sicilde seçim veya atlama zorunluluğu |
| Kimlik | `0001` ile `1` ayrımı; tam sicilde tek kesin sonuç; benzer/Türkçe isimlerde liste; arama değiştiğinde eski seçilmiş kişiye mesaj açılmasının engellenmesi |
| Şablon | Düz metnin aynen kalması; `{AD}` ifadesinin bile değişmemesi; boş/çok uzun mesaj reddi; Türkçe, satır sonu ve `&` gibi karakterlerin URL kodlanması |
| WhatsApp | Onaydan önce hiçbir dış açılış olmaması; iptal; normal/Business tek tek ve ikisi kuruluymuş gibi seçim; doğru E.164 numara ve metin; açık onayla tarayıcı; alıcı bulunamaması/hata durumunda gönderildi sayılmaması |
| Sıralı işlem | Tek sicilin iki kez seçilememesi, kişi değiştirmenin gönderim yapmaması, eski kimlik engeli, kullanıcı onayıyla sonraki kişi ve kuyruk iptali |
| Kalıcılık | Android Keystore AES-GCM, dosyada açık telefon bulunmaması, tekrar yazıda farklı şifreli veri, bozuk şifreli dosya reddi, yeniden okuma, eski sürümle yazma reddi |
| Yedek | Android ve JVM üzerinde parola korumalı taşınabilir yedek, yanlış parola/kurcalama reddi, çözme, atomik geri alma, geri almada sonraki geçmişi koruma, tümünü silmede geri alma dosyasını da kaldırma |
| UI | Açık/koyu temada beş sayfa, 360×640 dp emülatörde alt menü sınırları, okunabilir Türkçe metin, kilitlenince kişisel veri diyaloglarının kapanması ve kilit ekranı |

Son Android aktarım kontrolü Android `ContentResolver` üzerinden testin kendi dosya URI'sini okumaktan başlayıp geçici kopya → sayfa seçimi → sütun eşleştirme → çakışma çözümü → önizleme → onay → şifreli kalıcılık zincirini çalıştırır. Gerçek bir bulut dosya sağlayıcısının sistem seçici ekranı uçtan uca otomatikleştirilmedi.

## Bağımsız Dosya Uyumluluğu Ve Performans

Android'in ürettiği rehber XLSX'i bağımsız `openpyxl` ile açıldı: `000123` sicili ve `05320000000` telefonu `str` hücre olarak kaldı. Kaynakla verilen iki sayfalı Excel şablonu uygulamanın okuyucusuyla JVM'de açıldı; Türkçe başlıklar bulundu, boş veri satırları aktarılmadı. Microsoft Excel masaüstü uygulaması burada çalıştırılmadı.

Derleme makinesinde son ölçüm: **100.000 sentetik kayıtta 5000 tam sicil araması 31 ms**; **20.000 XLSX satırını yazma/okuma/eşleştirme 529 ms**. İlk ölçüm rehber yükleme süresini içermez ve uygulamanın 50.000 kayıt saklama sınırını artırmaz; yalnız arama algoritmasının bellek içi ölçümüdür. Bunlar telefon performansı veya tüm cihazlar için süre garantisi değildir.

## Başarısız Denemeler Ve Pratik Sınırlar

İlk genişletilmiş Android koşusunda kuyruk testi, AlertDialog onayının kuyruğa alınmış callback'ini beklemeden kontrol ettiği için **6 geçti / 1 başarısız** verdi. Teste koşul beklemesi eklendi; kontrol kaldırılmadı veya atlanmadı. Son release koşusunda aynı kontrol dahil 7/7 geçti. İlk Java derlemesindeki Android `Person` isim çakışması açık importlarla giderildi. Çalışma ortamının geçici kopması sırasında komutun yürütülmediği dosyalardan doğrulanıp devam edildi; o kesinti test başarısı sayılmadı.

Fiziksel telefon, gerçek WhatsApp/Business kurulumu, oturum açılmış hesap, gerçek sohbet ve gerçek gönderim test edilmedi. Yönlendirme testleri üretimdeki Intent sınırında sahte alıcı kullanır; WhatsApp'ın kendi arayüzünü veya sunucu gönderimini doğrulamaz. PIN/desen/parola doğrulama ekranında gerçek kullanıcıyla başarılı cihaz kilidi girişi denenmedi; kilit ve güvenli başarısızlık görünümü kontrol edildi. TalkBack, büyük font ölçeği, tablet ve tüm Android sürümleri tam olarak test edilmedi. Test görüntüleri sentetiktir; ilk debug koşusunda aynı UI kaynaklarıyla View çizimi üzerinden alındı.

Günlükler: geliştirme ortamında `sicil-release-build.log`, `sicil-release-delivery.txt`; tekrar çalıştırma komutları [DERLEME.md](DERLEME.md) içinde. [Dosya ve güvenlik sınırları](GUVENLIK-VE-SINIRLAR.md).

## 1.1 Görsel Güncelleme

Petrol & Mint açık tema, Grafit & Zümrüt koyu tema; WhatsApp simgeli, yeşil geçişli ve ortalanmış açma düğmesi. Düğme alt menünün üstüne sabitlendi. Küçük ekran kontrolü düğmenin en az 48 dp yüksekliğini, genişliğini, odaklanabilirliğini ve alt menü üzerinde görünmesini denetler. İlk denemede düğme kaydırılan içerikte kaldığından yeni görünürlük kontrolü assertion ile durdu; sabit yerleşim düzeltmesinden sonra son APK yeniden denetlendi. Önizleme ekranları sentetik test verileridir.
