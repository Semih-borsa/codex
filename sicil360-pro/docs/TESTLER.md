# Doğrulama — Sicil360 Pro 1.8 / 24 Ekim 2026

- JVM: **61 test, 0 başarısız**; `lintDebug`, `assembleRelease`, `assembleDebugAndroidTest` başarılı.
- API 30 hedefli `ScrollPosition` kontrolü: 60 kişilik, iki sayfalı listede ikinci sayfada kısmi/tam ödeme kaydı ve sayfa/filtre korunması; üstteki kaydın silinmesinde sabit kartın ofsetinin 3 dp toleransta korunması; şablon, geçmiş ve ayarlar yenilemesi; asenkron rehber sonuçları; sekme geçişleri ve durum Bundle kaydı; ticari unvanla Türkçe arama. 1 geçti, 0 başarısız, diğer 12 atlandı. Son APK üzerinde ayrıca tam Android koşusu çalıştırıldı.
- Tam Android koşusu: **13 kontrol grubu geçti, 0 başarısız**. Önceki veri güvenliği, XLSX, mesaj onayı/yönlendirme, CRM, ödeme/düzeltme/silme, şablon indirme, yuvarlama ve açık/koyu ekran kontrolleri dahil.
- APK SHA-256: `d3dfd726e92753a7806f2f2cabb8d71b925225f6743ca0ae2e3a19f9750d00ac`. Paket ve sertifika korunur; veri/yedek biçimi değişmedi.
- Ekran 360×640 dp emülatörde denetlendi. Gerçek WhatsApp/Business, fiziksel telefon, TalkBack ve gerçek cihazda sistemin Activity'yi öldürüp geri getirmesi uçtan uca denenmedi. Konumun Bundle'a yazılıp yeniden okunması ve ilgili Activity kaydı test edildi; her cihaz/klavye sürümü için garanti iddia edilmez.

---

# Doğrulama — Sicil360 Pro 1.7 / 24 Ekim 2026

- JVM: **61 test, 0 başarısız**. Yeni kontroller: uzun ondalıklı/unsafe sayısal bakiye yuvarlama uyarısı, HALF_UP ve manuel para kuralının korunması; boş/NaN/negatif/çok büyük sayının sıfır borca dönüşmemesi. Genel toplam farkı artık uyarıyla onaylanabilir olduğundan ilgili test yeni davranışı doğrular.
- `testDebugUnitTest`, `lintDebug`, `assembleRelease`, `assembleDebugAndroidTest` başarılı.
- API 30 hedefli `DebtRounding`: önizlemede üç tutar uyarısı; onay öncesi veri eklenmemesi; onay sonrası 628,30 / 19.179,40 TL'nin şifreli kayıttan okunması; sonradan 630,00 TL'ye manuel düzeltme ve tekrar okuma. 1 kontrol geçti, 0 başarısız, diğer 11 grup atlandı. Tüm Android grupları bu düzeltme için tekrar çalıştırılmadı. Testte sentetik XLSX hücrelerine sorunlu ham sayısal değerler verilir; kullanıcının gerçek Excel dosyası paylaşılmadığından dosyanın kendisi doğrulanmadı.
- APK SHA-256: `8713bf995535f3a8d72b2fc7f5fed85ba6c95bc7c627ae4400a4a0afef3c6735`. Paket/veri biçimi ve sertifika korunur. Fiziksel telefon testi yapılmadı.

---

# Doğrulama — Sicil360 Pro 1.6 / 24 Ekim 2026

- JVM: 58 test, 0 başarısız. `lintDebug`, `assembleRelease`, `assembleDebugAndroidTest` başarılı.
- API 30 emülatöründe hedefli `DebtTemplate`: uygulamadaki şablon düğmesi, belgenin Android ContentResolver ile seçilmiş dosyaya yazılması, kaynakla birebir XLSX eşitliği, altı doğru başlık ve kayıtlara dokunulmaması kontrol edilir. Sistem dosya seçicisi arayüzü otomatik sürülmez; sonuç URI'si testte verilir. 1 kontrol geçti, 0 başarısız, diğer 10 grup atlandı; tam Android grubu bu küçük ekleme için yeniden çalıştırılmadı.
- APK SHA-256: `7eae7990d65319a1e7e1c8a0ea0341e223f85b5882df85bc593b923beeff11f6`. Aynı paket ve sertifika ile güncelleme; veri biçimi değişmedi. Fiziksel telefon testi yapılmadı.

---

# Doğrulama — Sicil360 Pro 1.5 / 23 Ekim 2026

## Sonuç

- JDK 17.0.17, Gradle 8.9, AGP 8.7.3, Android SDK/build-tools 35.
- JVM: **58 test, 0 başarısız, 0 atlanan** (`ModelTest`: 17, `ExcelTest`: 11, `CrmTest`: 5, `PaymentsTest`: 25).
- `lintDebug` ve release için `lintVitalRelease` başarılı.
- `assembleRelease` ve `assembleDebugAndroidTest` başarılı.
- Android 11 / API 30 x86_64 emülatöründe **son teslim release APK'sı üzerinde 10 kontrol grubu geçti, 0 başarısız**. Testler taklit alıcıya yönelir; gerçek kişiye mesaj göndermez.
- Release paketinde `DEBUGGABLE` bayrağı yok; INTERNET izni yok; `FLAG_SECURE` açık. İmza doğrulandı. Sertifika geliştirme sertifikasıdır; APK Play Store yayını değildir.
- Son APK SHA-256: `7c71364bf057b3ca3afcb7fdd02b2126c501a232c87c4a0b2f30f8c48ce8c294`.
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

## 1.2 Şablon Listesi

Dikey, tek seçimli şablon satırları gerçek Android RadioGroup/RadioButton bileşenleriyle doğrulanır. Şablona dokununca düz metnin gelmesi, seçili satıra tekrar dokununca elle düzenlenen metnin korunması, Özel Mesaj geçişinin metni silmemesi ve tekrar şablon seçiminin önizlemeyi doldurması kontrol edilir. Mevcut WhatsApp son onay ve yönlendirme kontrolleri aynı akış üzerinden çalışır.

## 1.3 CRM Kontrolleri

JVM: V1 kişi/yedek içeriğini okuma; V2 takip/not/tarih ve kopyalama turu; İstanbul gece yarısı sınırı; hazırlanma/gönderim beyanı ayrımı; tamamlandı durumunun güncel sayımı; Türkçe kişi/sicil filtresi; Excel UPDATE sırasında CRM alanlarının korunması. Android: tam sicille otomatik seçim, takip/not kaydı ve şifreli depodan yeniden okuma, tekrar iletişim filtresi, tamamlandı değişikliği, güncel rapor ve kişi geçmişine geçiş. Sentetik veri kullanılır; test gerçek WhatsApp mesajı göndermez.

## 1.4 Ödeme Kontrolleri

JVM 14 yeni senaryo: Türkçe/nümerik para ve kuruş kesinliği; negatif/bozuk/aşırı hassas tutar reddi; kısmi/tam/fazla ödeme; başka kişinin borcunun fazla ödemeden etkilenmemesi; iptal ve yeniden açılma; kayıt sırası ve ilk toplamın korunması; ilk veri satırı ve sıfırlı cari; mükerrer kod reddi; toplam satırının kişi sayılmaması ve toplam uyuşmazlığı; formül/güvensiz sayı reddi; şifreli yedek ödeme turu; V2 yedek uyumluluğu; tekrar yükleme parmak izi. Android ek kontrol: gerçek XLSX dosyasını okuma, önizleme/onay, 250 TL kısmi + kalanını kapatma, 1.200 TL fazla ödeme, iptal, şifreli depodan yeniden okuma, aynı sırada soluk kayıt, ekran genişliği, yeniden yüklemede ödeme koruma ve rehber geri alma sırasında ödeme koruma. Tüm kişiler sentetiktir; gerçek finansal işlem yapılmaz.

Aylık analiz için 5 ek JVM testi: üç ardışık ayda ödeme kaydı olmaması; kısmi ödeme ile serinin kırılması; eksik ayın ödeme yapmamış sayılmaması; ağırlıklı %80 hesabı ve fazla ödemenin üst sınırı; aynı isimli farklı cari/gelecek dönem ayrımı; ay/yılın yedekte korunması. Finansal test ilk geçişte başarılıydı; aynı çalışmada eski sıralı mesaj testi otomatik sicil aramasının kuyrukla çakıştığını yakaladı. Kuyruk açıkken otomatik kişi değişimi engellendi ve son APK üzerinde tüm kontrol grupları yeniden çalıştırıldı.

## 1.5 Düzenleme Ve Silme Kontrolleri

6 ek JVM testi: düzeltmede ödeme geçmişi/sıra/parmak izi/toplamların korunması; aynı dönemde mükerrer cari reddi ve kaydın değişmemesi; kişi silmenin diğer aya dokunmaması; liste dönem değişikliğinde çakışma kontrolü; liste silmenin diğer listeyi koruması; tüm kişileri silinmiş boş listenin veri turu. Android ek kontrol: cari/ad/borç formundan düzeltme ve şifreli kayıttan doğrulama, mevcut ödemenin korunması, silme onayını iptal etme ve tekrar onaylayarak silme, güncel toplam, liste adını değiştirme, tüm listeyi silme ve diğer dönemin kalması. Yalnız sentetik deneme verileri kullanılır.
