# Doğrulama — Nöbet Çizelgesi 2.1 / 19 Ekim 2026

- JDK 17.0.17, Android SDK 35, build-tools 35.0.0, Gradle 8.9, AGP 8.7.3.
- `testDebugUnitTest`: **89 test, 0 başarısız, 0 atlanan**.
- `lintDebug`: **sorun bulunmadı**.
- APK ve Android test APK derlemeleri başarılı.
- Android 11 / API 30 x86_64 emülatöründe önceki sürüm üzerine `adb install -r` ile 2.1 güncellemesi başarılı.
- Android test çalıştırıcısı: **37 kontrol geçti, 0 başarısız**.

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

2.1 APK SHA-256: `20babe9232aa3eb0921be6de77f17d58cf9e734f42ac782e899a57f2a6a65318`. Paket `com.semih.nobet`, sürüm kodu 14. Önceki APK ile aynı imza doğrulandı.

## Sınırlar

Fiziksel telefon veya tüm Android sürümleri test edilmedi. WhatsApp'ta gerçek alıcıya gönderim yapılmadı; PDF üretimi ve Android paylaşım URI erişimi test edildi. APK debug imzalıdır. Dinî takvim 2020–2032 ile sınırlıdır; diğer yıllar manuel kontrol edilir. Sabit tatiller 2020 ve sonrasında üretilir. Takvim kaynağı/kapsamı RESMI-TATILLER.md içindedir. Canlı telefon eşitlemesi ve yönetici hesapları uygulanmadı; APK cihazda çalışır. Yetki/sunucu planı ORTAK-KULLANIM.md içindedir.
