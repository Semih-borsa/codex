# Yerel Ekstre Analizi ve İsteğe Bağlı Gemini — 1.5.0

## Teslim

- `App.jsx`: güncel tek React uygulama dosyası; ZIP değildir.
- `Finans-1.5.0.apk`: hazır Android debug APK; bilgisayarda derleme gerektirmez.
- Paket: `com.butcetakip.app`, versionName `1.5.0`, versionCode `13`.
- APK: 8.023.944 bayt. SHA-256: `3e1234bc2d1662cf96fd1bb125dc0bb3fb9c04bb00e7ea34930abfbf39971fd0`.
- İmza SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1` (Android Debug).
- App.jsx SHA-256: `62bd299fbe1cfcc366abf8c2e141486358152885e43aaa0a2468bfadcc248cad`.

## Gizlilik ve analiz ayrımı

PDF dosyaları, PDF metinleri, ekstre satırları ve bunlardan hesaplanan kategori/tutar özetleri **Gemini'ye gönderilmez**. Ekstre Analizi her zaman yereldir. Gemini seçilse bile bu bölüm yerel kalır. Belgeler internetsiz ayrıştırılabilir; ham PDF yerine onaylanan satırlar cihazdaki ayrı `ft_ekstre_analiz_v1` arşivinde tutulur. Bu arşiv kullanıcının JSON yedeğine dahil olur; yedeği özel bir yerde saklayın.

İsteğe bağlı Gemini yalnız mevcut maaş döneminin diğer finans kayıtlarının toplulaştırılmış özetini yorumlar. Ekstre arşivi bu özetin kaynağı değildir. Kişi/işyeri açıklamaları, kart numaraları ve ham işlemler gönderilmez. Ekstreden bağımsız olarak daha önce normal finans kayıtlarına girilmiş borç/ödeme tutarları genel finans özetinde bulunabilir.

Varsayılan yorum kaynağı Yerel'dir. Ayarlar'dan Gemini, veri paylaşımı onayı ve anahtar seçilmeden finans özeti gönderilmez. Android anahtarı AndroidKeyStore ile şifrelenir; manuel finans yedeğine ve Android bulut yedeğine dahil edilmez. Tarayıcıda anahtar yalnız oturum belleğindedir. Anahtar uygulamaya gömülmez.

Gemini yedi bölümlü yorum verir. Son başarılı yorumun tarihi ve eski veri uyarısı görünür; kota, bağlantı, anahtar ve model hataları belirtilir. Yerel yoruma dönülebilir. Otomatik yenileme veri değişince ve en az bir saat arayla; elle yenileme en az bir dakika arayla; uygulama sınırı günde 10 yorumdur. Bunlar sağlayıcının ücretsiz kota garantisi değildir. Canlı Gemini hesabı/anahtarıyla doğrulama yapılmadı; API senaryoları taklit yanıtlarla test edildi. Model listesi bağlantı testinden alınır; erişim hesaba/bölgeye/kotaya bağlıdır. Bağlantı testi küçük, finans verisi içermeyen bir istek kullanır.

## Ekstre ekranı

Menü → Ekstre Analizi → PDF seç → kart/dönem seç → satırları kontrol et → onaylayarak arşivle.

- Her kartta ilk beş kategori ve kalan kategorilerin doğru toplamı olan Diğer dilimi; altında bütün kategorilerin tutar/yüzde çubukları vardır.
- En altta tüm kartların toplam dağılımı ve kart payları gösterilir.
- Belirsiz/yanlış kategoriler, tutar, tarih ve işlem türü elle düzeltilebilir; kişisel kategoriler seçilebilir. İşyeri kuralı yalnız açık seçimle hatırlanır. Arşivlenmiş belge tekrar düzenlenebilir.
- Pasta paydası alışveriş ve o ekstredeki alışveriş taksitleridir. Önceki borç, ödeme, iade, puan, nakit avans ve faiz/ücret ayrı değerlendirilir; ödeme yeni alışveriş sayılmaz.
- Ekstre tutarıyla mutabakat farkı görünür; eksik belge açıkça kısmi olarak onaylanmadan saklanmaz. Aynı PDF/kart-dönem için tekrar uyarısı vardır. Aynı görünen gerçek işlem satırları otomatik silinmez.
- Üç sağlanan gerçek PDF yalnız yerel olarak kontrol edildi ve ekstre tutarları kuruşuna kadar mutabık bulundu. Bu kontrol bütün bankaların bütün sürümleri için garanti değildir. Yapı Kredi'nin güncel PDF örneği doğrulanmadı; eski görselden kesin ayrıştırıcı garantisi verilmez. Taranmış görüntü PDF'lerinde OCR yoktur; otomatik okuma başarısızsa elle doğrulama gerekir. Doğrulanamayan veriden kesin analiz üretilmez.
- Ekstre arşivlemek gerçek gelir/gider, kart borcu veya banka bakiyesi oluşturmaz/değiştirmez.

Halka grafiğin koyu çevre çizgisi yarı saydam beyaza çevrildi. Kart tasarımı, risk renkleri, ibre ve animasyon süreleri korundu.

## Dosyalar ve elle derleme

React değişiklikleri tek `App.jsx` içindedir. PDF okuma için `pdfjs-dist@6.4.299` geri eklendi; `package.json` ve `package-lock.json` teslimdedir. Capacitor 8.5.3 korundu.

Güvenli Android anahtar saklama ve Google isteği için ayrıca şu native kaynaklar gerekir:

- `android/app/src/main/java/com/butcetakip/app/MainActivity.java`
- `FinansGeminiPlugin.java`, `GeminiKeyCodec.java`, `GeminiWire.java` (aynı klasör)
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/xml/gemini_backup_rules.xml`
- `android/app/src/main/res/xml/gemini_data_extraction_rules.xml`
- `android/app/build.gradle` (sürüm artışı)

Android yedekleme eklentileri korunmuştur. Mevcut projede bu dosyaları ilgili dizinlere koyup teslim edilen paket dosyalarıyla `npm install`, `npm run build`, `npx cap sync android` çalıştırılır; sonra Android derlenir. Sadece App.jsx değiştirmek PDF bağımlılığını veya native anahtar saklama kodunu kurmaz. Hazır APK hepsini içerir.

## Testler ve sınırlar

- Son JavaScript/React/tarayıcı koşusu: **475/475 başarılı**, 0 başarısız, 0 atlanan (`kanitlar/release-tests.tap`). Önceki 406 kontrol, geri getirilen 31 saf PDF kontrolü, 24 yeni birim kontrolü ve 14 yeni ekran kontrolü.
- Eski kaldırılmış PDF ekranına bağlı 17 tarayıcı testi kaynak olarak korundu, bu koşuya dahil edilmedi; yeni önizleme/arşiv akışı 14 yeni ekran testiyle sınandı. Eski üç “Ekstre menüsü yok” beklentisi onaylanan özelliğin geri gelmesi nedeniyle “var” olarak güncellendi. Finansal beklentiler gevşetilmedi.
- Native birim testleri **21/21 başarılı**; değişmeyen görevler Gradle önbelleğinden yararlandı. Bunlar fiziksel cihazdaki Keystore veya ağ bağlantısını kanıtlamaz.
- `npm run build`, `npx cap sync android`, `:app:testDebugUnitTest` ve `:app:assembleDebug` başarılı.
- Ortak motor/yedek/bildirimlerde 14 ana işlevin önceki sürümle aynı olduğu kaynak karşılaştırmasıyla doğrulandı (`kanitlar/scope.json`). İkinci finans motoru/veritabanı eklenmedi.
- Genel ESLint temizliği yapılmadı; lint'in hatasız olduğu iddia edilmiyor.
- Gerçek telefonda bu APK henüz doğrulanmadı. PDF seçimi/iptali, arşivleme, yeniden açılma, JSON yedek/geri yükleme, üç tema, Gemini anahtar kaydı/silme, gerçek API bağlantısı ve eski yorumun hata sırasında görünmesi cihazda ayrıca test edilmelidir.

## Güvenli kurulum ve geri dönüş

Önce mevcut uygulamadan JSON yedeği alın, dosyanın boş olmadığını ve okunabildiğini kontrol edin; ikinci kopyasını saklayın. Telefonda uygulamayı kaldırmayın, verilerini temizlemeyin.

Bu APK'nın paket ve imzası önceki burada üretilen test APK'sıyla aynıdır; versionCode 12'den 13'e yükselmiştir. Telefonda kurulu uygulamanın imzasına erişim yoktur. Üzerine güncelleme yalnız kurulu uygulamanın kimliği ve imzası aynıysa mümkündür. Android uyumsuzluk bildirirse kaldırarak çözmeye çalışmayın; kurulu sürümün doğru imza anahtarı gerekir.

`geri-donus/` önceki çalışan App.jsx, paket dosyaları, Gradle ve özgün MainActivity/Manifest kopyalarını içerir. Kaynak geri dönüşünde bu dosyalar yerlerine alınır; yeni native dosyaları yalnız kaynak düzeyinde kaldırılabilir. Telefona eski versionCode'u zorla düşürmek yerine aynı imzayla daha yüksek versionCode taşıyan bir geri dönüş APK'sı hazırlanmalıdır. Kullanıcı finans kayıtları ve eski yedekler değiştirilmedi; herhangi bir otomatik toplu dönüşüm yapılmadı.
