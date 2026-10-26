# Doğrulama — Nöbet Çizelgesi 2.6 / 26 Ekim 2026

- JVM: **116 test, 0 başarısız**. Yeni kurallar: atama tek başına resmî tatil sayılmaz; atanmamış ama seçilmiş kişi yalnız tatilde sayılır; seçim kaldırılır; gün/kişi/yer mükerreri sayılmaz; 0,5 ağırlık korunur; silinmiş görev yerindeki yalnız gerçek çalışma da raporda kalır. Önceki tatil testleri yeni açık çalışma seçimleriyle takvim ağırlıklarını doğrular.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` başarılı.
- API 30 hedefli Holiday kontrolleri: seçim ekranında otomatik işaret olmaması, Vazgeç, seçip kaydetme, JSON/yerel dosyadan yeniden okuma, yeniden açıp işareti kaldırma, normal atama sayılarının aynı kalması, eski yedekte otomatik çalışma onayı yaratılmaması, tatil takvimi ve yarım gün PDF çizimi. Son koşuda Holiday: 4 geçti/0 başarısız/44 atlandı; Table ve Today koşularının her biri: 1 geçti/0 başarısız/47 atlandı. Toplam 6 farklı hedefli kontrol başarılı. Tam Android grubunun tümü bu değişiklikte yeniden çalıştırılmadı.
- İlk emülatör açılışında Android System UI yanıt vermediği için ilk ekran kontrolü odak alamadı. Ortam toparlandı ve kontroller yeniden çalıştırıldı; uygulamanın çalışma seçimi son koşuda doğrulandı.
- APK SHA-256: `70780d764b31f59743314ed6461dc2a17b5363fcd1a67da18543ea7bd6502424`. Paket com.semih.nobet, sürüm kodu 20; aynı geliştirme sertifikası. Eski yerel/yedek kayıtlar açılır; yeni alan isteğe bağlıdır. Fiziksel telefon ve canlı bulut eşitlemesi denenmedi.

---

# Doğrulama — Nöbet Çizelgesi 2.5 / 24 Ekim 2026

- JVM: **112 test, 0 başarısız**. WeekendList için 4 yeni test: kronolojik sıra, ay sınırı, yıl sınırı, tek/ayrı nöbetçi, boş gün ve şubenin ana işyeri nöbetiyle karıştırılmaması.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` başarılı.
- API 30 emülatöründe hedefli `Weekend saved` ve `Table compact` koşuları. Her koşuda 1 geçti, 0 başarısız, 46 atlandı. Yeni listede tarih/isim metni ve satırdan haftaya geçiş; mevcut tablonun dar ekran yerleşimi kontrol edildi. Tam Android test grubunun tamamı bu değişiklik için yeniden çalıştırılmadı.
- Eski tablo kontrolü hafta sonu başlığını tam metinle aradığı için bugüne eklenen Bugün etiketiyle ilk denemede durdu. Kontrol ilgili satırı kimliğinden bulup etiketi de destekleyecek şekilde düzeltildi ve yeniden çalıştırıldı.
- Paket com.semih.nobet, sürüm kodu 19; önceki dağıtımlarla aynı geliştirme sertifikası. Fiziksel telefon testi yapılmadı.
- APK SHA-256: `ab1c7e202192360ab48c3339fe6b04cf61bf9cae109232003cc279e95c65c424`.

---

# Doğrulama — Nöbet Çizelgesi 2.4.1 / 21 Ekim 2026

Yalnız bugünün satırının zemin tonları değişti: açık temada #FFF2CC, koyu temada #66552F. `lintDebug`, uygulama ve test APK derlemeleri başarılı. Aynı imzayla yerinde güncelleme doğrulandı; sürüm kodu 18.

API 30 emülatöründe hedefli `Today` koşusu: **1 geçti, 0 başarısız, 45 atlandı**. Açık/koyu zemin ve yazı renkleri, hafta içi/cumartesi/pazar/başka hafta, etiketin kırpılmaması ve tarih yenileme kontrolleri geçti. Motor kodu değişmedi; JVM ve tam Android grubu bu kozmetik düzeltme için yeniden çalıştırılmadı. Fiziksel telefon testi yapılmadı.

Son APK SHA-256: `5a84ecabdd3904ac766deb02ef14fc5fb184d762dc4062f1c13a01d334b98fc4`.

---

# Doğrulama — Nöbet Çizelgesi 2.4 / 21 Ekim 2026

- JVM: **108 test geçti, 0 başarısız, 0 atlanan**. Yeni iki test yedi günün doğru satırını, iki hafta sonu gününü, başka haftaları ve yıl geçişini doğrular.
- `lintDebug`, APK ve Android test APK derlemeleri başarılı. Paket `com.semih.nobet`, sürüm kodu 17.
- Android 11 / API 30 emülatöründe önceki uygulama üzerine güncelleme başarılı; aynı imza korunur.
- Hedefli `Today` koşusu: **1 geçti / 0 başarısız / 45 atlandı**. Açık/koyu temada hafta içi, cumartesi, pazar, başka hafta; etiketin üç satırda kırpılmadan sığması; zemin/yazı renkleri ve tarih yenileme işlevi kontrol edildi. Sabit test tarihleri, cihaz saatini değiştirmeden çizim metoduna verildi.
- Hedefli `Table` koşusu: **1 geçti / 0 başarısız / 45 atlandı**. 360×640 dp ekranda tüm altı satır, hafta sonunun iki gerçek kaydının korunması ve çok şubeli 280/320/360/600 dp genişlik sınırları doğrulandı.
- Bu sürümde tüm 46 Android kontrolü birlikte yeniden çalıştırılmadı. Önceki 2.3 tam koşusu aşağıdadır. Fiziksel telefon testi yapılmadı.
- İlk Android denemesinde gereksiz framework kaynak kimliğiyle `setTag` kullanımı açılışı durdurdu; bu satır kaldırıldı, APK yeniden derlenip yukarıdaki kontroller aynı son APK üzerinde geçti. Başarısız ilk deneme başarı sayılmadı. Boşluklu adb filtre çağrısı da test başlatmadı; sonraki `Table` koşusu gerçek sonuçtur.
- Son APK SHA-256: `58232e0b38f1b1072b6930cb75c326126acd1b30c315e3d08161518e66296c32`.

Vurgu yalnız ekran sunumudur; dağıtım/veri/PDF kodu değişmez. Telefonun yerel tarihi kullanılır. Gerçek gece yarısında saatlerce bekleme veya saat dilimini canlı değiştirme deneyi yapılmadı; tarih yenileme işlevi emülatörde önceki gün işaretiyle çağrılarak kontrol edildi.

---

# Doğrulama — Nöbet Çizelgesi 2.3 / 20 Ekim 2026

- `testDebugUnitTest`: **106 test, 0 başarısız, 0 atlanan** (96 mevcut + 10 yeni dağıtım testi).
- `lintDebug`, APK ve Android test APK derlemeleri başarılı.
- Android 11 / API 30 emülatöründe tam koşu: **45 kontrol geçti, 0 başarısız, 0 atlanan**. Önceki sürüm üzerine güncelleme başarılı. Fiziksel telefon testi yapılmadı.
- Paket `com.semih.nobet`, sürüm kodu 16; önceki sürümle aynı imza. Güncelleme `adb install -r` ile yapılır.
- APK SHA-256: `8ba9bdab73c637ad6e835c5087db287d9e517d4e4f983c4b57896ac9051dece6`.

## 2.3 Dağıtım Regresyonları

Dört kişi/beş gün 1–1–1–2; çok farklı geçmiş toplamların haftalık dengeyi bozamaması; tek uygun günü olan kişi; çakışan uygunluklarda yeniden yerleştirme; haftalık üst sınırın aşılmaması ve boş gün uyarısı; izin/pasif personel; hafta sonunu hafta içi adalet sayımından ayırma ve dinlenmeyi koruma; başka görev yerinin dar uygun gününü tüketmeme; otomatik dağıtımı kapalı işyerinde hafta içi görev üretmeme.

Onuncu test, sabit tohumlu **180 farklı dört personel/beş gün uygunluk ve haftalık sınır örneğini**, boş bırakma dahil bütün olası atamaları deneyen bağımsız bir sayımla karşılaştırır. Doldurulan gün sayısı, görev alan kişi sayısı ve haftalık dağılım dengesi kontrol edilir. Bu karşılaştırma tek görev yerini mevcut atamalar çerçevesinde doğrular; bütün görev yerleri birlikte küresel optimum iddiası değildir.

Yeni testler eski 2.2 motoruyla da çalıştırıldı: **10 testin 4'ü başarısız**. Geçmişi az kişiye beş günün tamamının verilmesi (0–0–0–5), uygun dört gün varken üçünün doldurulması, başka yere ait tek uygun günün tüketilmesi ve hafta içi otomatik kapatma hatası yeniden oluşturuldu. Düzeltilmiş kodda aynı 10 test geçer.

İki yeni Android kontrolü: açık/koyu temada ortalı ve görünür ay/güneş simgesi; Türkçe alfabetik personel sırası; personel ve tür başlıklarının kapalı başlaması, bağımsız açılıp kapanması; çizelge/istatistik ekran satırlarının siyah olması. Haftalık, normal istatistik ve resmî tatil PDF'leri PDF renderer ile rasterleştirilip tam satır boyunca siyah ayraç pikselleri kontrol edilir. Sentetik ekran görüntüleri üretilir; gerçek personel kullanılmaz.

İptal edilen e-postasız hesap geliştirmesi bu sürüme alınmadı. CloudApi/CloudController/sunucu SQL değişmez; canlı Supabase kurulumu veya fiziksel telefonda doğrulama yapılmadı. Önceki onaylı haftalar otomatik değiştirilmez.

---

# Doğrulama — Nöbet Çizelgesi 2.2 / 20 Ekim 2026

- JDK 17.0.17, Android SDK 35, build-tools 35.0.0, Gradle 8.9, AGP 8.7.3.
- `testDebugUnitTest`: **96 test, 0 başarısız, 0 atlanan**.
- `lintDebug`: **sorun bulunmadı**.
- APK ve Android test APK derlemeleri başarılı.
- Android 11 / API 30 x86_64 emülatöründe önceki sürüm üzerine `adb install -r` ile 2.2 güncellemesi başarılı.
- Android test çalıştırıcısı: **43 kontrol geçti, 0 başarısız**.

## 2.2 Tablo Ve Ortak Ekip Kontrolleri

7 yeni JVM testi: beş hafta içi ve birleşik hafta sonu satırı tüm yedi tarihi kapsar; aynı hafta sonu görevlisi bir kez görünürken iki görev saklanır; farklı Cmt/Paz atamaları korunur; yalnız pazar açıkken o günün kişisi görünür; boş açık gün gizlenmez; boş alan ve yıl geçişi; geçersiz satır reddi. Önceki 89 test de geçti.

6 yeni Android kontrolü: HTTPS/proje alan adı ve public anahtar doğrulaması; HTTP/yabancı adres/service_role/secret anahtar reddi; oturum yenileme ve anahtar döndürme; metadata rolünün yetki sayılmaması; e-posta kodu/yeniden gönderim isteği ve SMTP kurulum hatası; Android Keystore AES-GCM dosyasının düz metin içermemesi, her yazıda farklı şifreli çıktı ve bozulmuş dosya reddi; iki **benzetim istemcisiyle** kullanıcı yazma reddi, yönetici yayınlama/diğer istemcide okuma, rol güncellemesi, sürüm çakışmasında yerel kopya ve sunucu verisinin korunması, kabul yanıtı kaybolunca aynı kimlikle yeniden gönderim; kayıt yanıtının hemen sonrasında açılan yeni taslağı silmemesi. Son kontrol haftalık tablonun altıncı satırının 360×640 dp telefona sığmasını, Cmt/Paz'ın ayrı satır olmamasını, iki gerçek görevin korunmasını ve beş görev yerinde 280/320/360/600 dp genişlik sınırlarını doğrular. Önceki 37 Android kontrolü de geçti.

`backend/tests/run.py`: **32 PostgreSQL kontrolü geçti, 0 başarısız**. Postgres 16 Alpine, ağsız geçici Docker konteyneri; iki ayrı DB bağlantısıyla aynı sürüme yazışmada tek kazanan. Anonim erişim, ilk yönetici UUID/e-posta/doğrulama kontrolü, metadata ile yönetici olamama, kullanıcı/ekip dışı okuma-yazma/rol/davet reddi, tablolara doğrudan yazma ve özel kurulum tablosunu okuma reddi, davet kabulü, yönetici devri, son yönetici koruması, kaldırılan üyenin erişimi, eski davetle geri dönememe, diğer ekip yöneticisinin erişememesi, boş/bozuk üst yapı reddi, eski sürüm yazışı, idempotent tekrar ve veri içermeyen sürüm sorgusu kontrol edildi.

**Bu testler canlı Supabase Auth/JWT/SMTP/Data API bağlantısının veya iki fiziksel telefonun doğrulandığı anlamına gelmez.** Auth tablosu/UID işlevi PostgreSQL testinde taklit edildi; Android ağ taşıması sentetik yanıtlarla test edildi. Kullanıcı henüz proje oluşturmadığından gerçek sunucu bağlantısı yapılmadı. APK'da bağlantı ayarları ve sunucu SQL dosyaları teslim edildi; proje kurulduğunda uçtan uca doğrulanmalıdır. Dışarı gerçek kişiye e-posta/davet gönderilmedi.

Önceki yazılım emülatörü System UI/system yanıt vermiyor pencereleri ilk odak kontrolünü engelledi; test başarı sayılmadı. Pencereler kapatılıp yeniden çalıştırıldı. Yeni tabloda hafta sonu satırının altta kaldığı ilk kontrol de başarısız sayıldı; satırların baseline hizalaması ve alanlar düzeltilip aynı kontrol yeniden geçti. Tüm mevcut kontrollerin son tam koşusu filtre kullanılmadan yapılır.

Son tam işlevsel koşudan sonra yalnız kişisel/ortak veri saklama açıklaması düzeltildi. Yeniden derleme, lint ve son APK ile `Activity launches` hedefli kontrolü geçti (1 geçti / 42 atlandı); bu hedefli koşu ayrı raporlanır.

## 2.1 Resmî Tatil Kontrolleri

14 yeni JVM testi: 2026 sabit tatilleri, 28 Ekim ve arifelerde yarım gün; Ramazan/Kurban bütün günleri; 2020–2032 sınırları ve artık yıl; destek dışı yıllarda dinî tarih tahmin edilmemesi; otomatik tatilin başka yıla taşınması ve sıfırlanması; devre dışı bırakma; çakışan tatillerde bir kez/en yüksek süreyle sayma; tatil ve nöbet düzenleme sonrası yeniden hesaplama; değişmeyen normal sayımlar; geçmiş/gelecek ayrımı; gizli/silinmiş şube, pasif personel ve güncel yer adları; tam sayı yarım gün birimleriyle yuvarlama hatası olmaması; geçersiz süre/boş adın reddi. Önceki 75 motor testi de geçti.

Ayrı PDF metni de çıkarıldı: Merkez ve Çarşı sütunlarında aynı sentetik personel için `0,5` doğrulandı; görseli RESMI-TATILLER.md içinde.

3 yeni Android kontrolü: özel/düzeltilmiş/devre dışı tatillerin JSON ve AtomicFile ile saklanıp yeniden okunması; eski kayıtta eksik tatil alanının otomatik takvimle güvenli açılması ve mükerrer kimliğin reddi; ayrı yarım gün PDF'sinin PdfRenderer ile açılıp çizilmesi; takvim ve editör ekranlarının açılması, boş ad doğrulaması, vazgeçme, yarım gün seçimi ve gerçek teyit düğmesiyle kayıt; diskteki veri ve canlı normal/tatil sayımlarının kontrolü. Sentetik veriler ayrı test deposuna yazılır.

İlk editör testi, ShowListener henüz işlenmeden Kaydet'e basınca doğrulamanın bağlanmamış olduğunu yakaladı. Dinleyici `show()` sonrasında aynı UI adımında bağlandı; kontrol kaldırılmadı. 20 şubeli ekran ölçüm testinde odak bekleme eklendi; Ayarlar ve İstatistik açılışları ayrı UI adımlarına bölündü. Aynı genişliklerde hem normal hem resmî tatil tablolarının sınırları kontrol edilir. Diğer ekranlardaki odak zaman aşımı sonrasında bütün test açılışları odak bekleyen ortak yardımcıdan geçirilir. Teyit düğmesi testi büyük/küçük harf dönüşümüne duyarsızdır; gerçek erişilebilirlik düğmesi bulunup tıklanır. Yalnız `-e match Holiday` ile yapılan odaklı koşu 3 geçti / 34 atlandı olarak ayrı raporlandı; tam koşuda filtre kullanılmaz. İlk başarısız/kesilmiş koşular başarı sayılmaz.

## 2.0 arayüz kontrolleri

Açık ve koyu temada beş sekme açıldı; seçili menü durumu, çizgi simgeler, menü genişliği ve tema düğmesi doğrulandı. Sentetik beş personelle ana ekran, gün kartları, personel, mazeret, istatistik ve ayarlar görüntüleri üretildi. Önceki 33 Android kontrolü ve 75 motor testi korunur. Tasarım Bütçe Takip uygulamasındaki kart/menü dilinden ilham alır; hesaplama ve veri şeması değişmez.

Yazılım emülasyonunda bir tekrar koşusu, 20 şubeyi dört genişlikte tek ana iş parçacığı adımında ölçerken odak zaman aşımıyla kesildi. Ölçümler aynı doğrulamaları koruyarak ayrı UI adımlarına bölündü ve ilk kare beklendi. Emülatör animasyonları kapatıldı. Son tam koşunun sonucu yukarıdadır; yarıda kalan koşu başarı sayılmadı. Önizleme alırken GPU sunumu için kısa bekleme uygulanır.

## 1.11 kontrolleri

Teyitsiz mazeret reddi ve görev kapsamlı teyitle izin; tarih/kişi/yer değişince eski iznin kullanılamaması; her çakışan mazeret için ayrı kayıt; yeni/değişen kısıtın eski izinle aşılamaması test edildi. İstisna pasiflik, dinlenme, aynı gün çoklu yer veya haftalık sınır için yetki vermez. Otomatik dağıtım eski istisnaları kullanmaz. Hafta sonu manuel adayın iki günü için uygun istisnalar gerekir. Mazeret sürerken istisnayla tutulmuş hafta sonu bekleyen hakkı kapatır.

Android: mazeret kimlikleri ve istisnalar diske yazılıp tekrar okundu. Eski dosyada eksik kimlik, her okumada aynı şekilde tamamlanır; istisna alanı yoksa otomatik izin verilmez. Uyarıda rapor + haftalık kısıt birlikte gösterilir; vazgeçmek kaydı/taslağı değiştirmez, onay taslağa iki görev kapsamlı izin ekler. Mazeret kayıtları korunur. Onay düğmesinin mesajı işlendiği sonrasında durum doğrulanır.

## 1.10 kontrolleri

Teyitsiz aynı gün çoklu görev reddi, teyitli iki şubeye izin, istisnanın kopyada korunması ve iki şubenin istatistikte ayrı sayılması test edildi. İstisna başka kişi/tarih/yere taşınamaz; üç yer için her ek çiftin teyidi gerekir. Aynı gün çoklu yer teyidi tek başına mazereti, haftalık gün kısıtını, Perşembe/Cuma dinlenmesini, aynı yerde çift slotu veya ayrı haftalık sınırı geçersiz kılmaz. Otomatik dağıtım çoklu görev üretmez.

Android üzerinde aynı gün ve haftalık sınır uyarıları tek teyitte gösterildi. Vazgeçince taslak/orijinal değişmedi; onaydan sonra iki istisna da taslağa geçti, kayıtlı eski hafta kullanıcı kaydedene kadar değişmedi. Diske yazıp okuma istisnayı korur; alanı olmayan eski JSON otomatik yetki kazanmaz.

## 1.9 kontrolleri

Android üzerinde onaylı listenin Perşembe kartına kaydırıldı. Personel seçim diyalogundan seçim yapılınca, onaylı görünüm taslağa geçerken başlık yüksekliği değişse de aynı günün ekran içindeki konumu 2 piksel toleransla korundu. İkinci seçimde alan boşaltılıp tekrar kontrol edildi; yeni boş hücre görünürken konum korunur. Kontroller çizim kareleri tamamlandıktan sonra yapılır. Personel sekmesine geçildiğinde kaydırma yeni ekranın başına döner.

## 1.8 kontrolleri

Android diyalog testi: haftalık modda başlangıç/bitiş alanları GONE, bitiş tarihi kutusu yok. Pazartesi ve Çarşamba birlikte seçilir. + ikinci bağımsız satır ekler; boş ikinci satır varken teyit hiçbir kaydı değiştirmez. İkinci satırda Cuma seçimi ilk satırı değiştirmez; − ile yalnız ikinci satır kaldırılır. Tarihli moda geçince tarihler tekrar görünür; haftalık moda dönünce seçili günler korunur.

Ek motor testleri: 420 gerçek tarih boyunca yıl değişiminde Pazartesi/Çarşamba tespiti; aynı kişiye ayrı şube günleri ve ana yer günü kurallarının 6 haftalık dağıtımlarda korunması. Önceki izin dönüş önceliği, çoklu kısıt, istatistik ve PDF kontrolleri korunur.

## Hafta sonu dönüş önceliği doğrulaması

Dört ek test geçti: (1) kendi sırası gelmeden izni biten kişi öne geçmez; (2) birden fazla hafta sonu raporlu olsa da sırası henüz gelmeyen kişi normal sırada kalır; (3) aynı anda izinden dönen iki kişiden yalnız sırası gerçekten atlanmış olan öncelik kazanır; (4) mazeret devam ederken sonraki personeller atanır, bitince bekleyen kişi gelir, ardından normal sıra kaldığı yerden sürer. Bu öncelik davranışı 1.7 sürümünden beri uygulanır; sonraki sürümlerde korunur.

## 1.7 kontrolleri

Haftalık Pazartesi/Çarşamba kısıtının 12 haftalık tekrarları, seçilmeyen günlerin serbestliği, başlangıç/bitişin dahil olması, tek yer kısıtının başka yeri engellememesi ve aynı personelin birden fazla kısıtının birleşimi test edildi. Bir kısıtı silmek diğerlerini kaldırmaz. 8 haftalık otomatik dağıtım ve elle atama doğrulaması tekrar günlerini denetler. Hafta sonu kısıtı bitince bekleyen nöbet önceliği sürer. Geçersiz tekrar maskeleri reddedilir.

Android: aynı kişiye ait süresiz ve bitiş tarihli haftalık kayıtlar ile tarihli rapor diske yazılıp yeniden okundu. Eski JSON'da tekrar günleri yoksa mazeret tüm tarih aralığında çalışmaya devam eder. Diyalogda haftalık moda geçiş ve Pazartesi/Çarşamba günlerini birlikte seçme doğrulandı.

## 1.6 kontrolleri

Android üzerinde ilk/son personelin sınır düğmeleri, aşağı ve yukarı taşıma sonrası görünen sıra numaraları, Kaydet düğmesinin görünmesi, ad/kimlik/eski kayıtların değişmemesi ve İptal ile kayıtlı sıraya dönüş doğrulandı. Kaydedilmemiş sıra ekran durumuna dahil edilir. Aktif personel listesi değişince geçersiz taslak temizlenir. Önceki 47 motor testi ve 24 Android kontrolü korundu.

## 1.5 kontrolleri

Seçilen özel sırayla 10 haftalık döngü, kayıtlı eski haftayı değiştirmeden yeni sıradan başlama, yeni personeli araya ekleme, pasif kişiyi atlama, mazeret sonrası bekleyen önceliği ve normal döngüye devam etme test edildi. Eksik/mükerrer/bilinmeyen kimlikli sıra reddedilir; geçerli ayar bozulmaz. Haftanın sıra sürümü kopyalamada korunur.

Android testleri: sıralama ve döngü konumu diske kaydedilip yeniden açıldıktan sonra doğru kişiden devam eder. Yeni alanları içermeyen eski JSON mevcut ekleme sırası ve döngü konumunu korur. Ayarlar'daki başlık, yeni personel düğmesi ve numaralı personel adları doğrulanır. Önceki istatistik/PDF/mazeret testleri yeniden geçti.

## 1.4 kontrolleri

İstatistik PDF'si boş personel listesinde de geçerli, kompakt bir tablo üretir. 19 personel / 7 görev yeri / geçmiş ve gelecek atamalı sentetik örnekte 8 sayfanın tamamı PdfRenderer ile açılıp çizilir. Gizlenen şubeler istatistikte korunur. Uzun şube/personel adları satıra bölünür. İstatistik PDF paylaşım URI'sinin okunabildiği, yazma erişiminin reddedildiği ve ekrandaki dışa aktarma düğmesi doğrulanır.

## 1.3 kontrolleri

20 görev yerli sentetik veride her şubenin istatistik başlığı bulundu; harfli başlıkların ve yatay kaydırma bileşeninin olmadığı doğrulandı. 280, 320, 360 ve 600 dp ölçümlerde tablo hücreleri sınırlar içinde kaldı. Sayımların ve Türkçe alfabetik sıranın mevcut motor testleri korundu.

Kompakt 700 × 358 punto PDF Android PdfRenderer ile açılıp görüntülendi. Çok sayfalı dışa aktarım ve görünür/gizli yerlerin korunması da yeniden doğrulandı. Sabit başlıklarda Hafta İçi / Hafta Sonu yazımı kontrol edildi.

## Önceki kontroller

İlk ana yerin seçili hafta içi günleri, ilk şubeye hafta sonu günü seçilmesi, ana yerde yalnız cumartesi/yalnız pazar ataması, kapalı cumartesideki mazeretin açık pazarı engellememesi, yalnız pazarla bekleyen nöbet hakkının kapanması, tüm günler kapalıyken atama/döngü ilerlemesi olmaması test edildi.

Ek şubeyi silerken geçmiş/bugün ve sayımların korunması, yalnız gelecekteki atamaların kaldırılması, sonraki dağıtımda silinen yere atama yapılmaması, eski çizelgede geçmişinin görünmesi, geri alınca yeniden kullanılabilmesi test edildi. İlk iki temel yerin silinmemesi de doğrulandı.

Gizleme testleri: şube görünümden çıkıyor; görev kayıtları, personel sayımları ve otomatik dağıtım değişmiyor. Gizli yerin PDF sütunu çıkarılıyor; arşiv/gizlilik bayrakları ve seçili günler kayıttan yeniden açılınca korunuyor. Açık kurumsal mavi (#2A6FB1) ve beyaz düğme yazısı Android üzerinde doğrulandı.

Önceki sürümün 32 motor ve 14 Android kontrolü de geçti: mazeret/sıra, perşembe/cuma dinlenme, 100 haftalık yük dengesi, kayıtlı liste revizyonu/istatistik güncellemesi, onaylı tekrar şube ataması, Türkçe alfabetik tablo, yer adlarını koruyan geçiş, sade çok sayfalı PDF, paylaşım URI güvenliği, açık/koyu tema ve ekran açılışları.

## APK Bütünlüğü

2.2 APK SHA-256: `c15c6a5c622c724658c2c7c42273b4a62f720b89798f5083e7fd1382a46665e6`. Paket `com.semih.nobet`, sürüm kodu 15. Önceki APK ile aynı imza doğrulandı.

## Sınırlar

Fiziksel telefon veya tüm Android sürümleri test edilmedi. WhatsApp'ta gerçek alıcıya gönderim yapılmadı; PDF üretimi ve Android paylaşım URI erişimi test edildi. APK debug imzalıdır. Dinî takvim 2020–2032 ile sınırlıdır; diğer yıllar manuel kontrol edilir. Sabit tatiller 2020 ve sonrasında üretilir. Takvim kaynağı/kapsamı RESMI-TATILLER.md içindedir. Hesap/yönetici/eşitleme kodu uygulanmıştır; canlı Supabase projesi bağlanmadı ve gerçek iki cihazla denenmedi. Kurulum ve doğrulama adımları ORTAK-KULLANIM.md içindedir.

## Paralel Dal Birleştirmesi

Depodaki 6a6c0cc değişikliğiyle aynı özellik farklı bir dalda geliştirildiği görüldü. Kayıt anahtarı `holidayWork` olarak birleştirildi; `fullDayHolidayWork` biçimi de okunur. Önceki yarım tatil 0,5 tercihi korundu. Takvimdeki doğrudan Tam Gün Çalışanlar kısayolu birleştirildi. Arşivdeki 2.6-TEST APK farklı sertifikalı geliştirme çıktısıdır; güncel normal 2.6 APK aynı a1acf… sertifikayla eski kararlı sürümün üzerine kurulur. Diğer projelerdeki değişiklikler korunmuştur.
