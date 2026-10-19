# Nöbet Çizelgesi 1.1 — Android

Çevrimdışı personel, görev yeri, mazeret ve haftalık nöbet planlama uygulaması.

[APK 1.1 indir](../downloads/NobetCizelgesi-1.1.apk) · [Android Studio ZIP](../downloads/NobetCizelgesi-Android-Studio.zip) · [Test sonuçları](docs/TESTLER.md)

Android 8.0 ve üzeri. 1.0 kuruluysa **uygulamayı kaldırmadan** 1.1 APK'sını açarak güncelleyin. Aynı paket adı ve imza kullanılır; eski kayıtlar okunur. Kaldırmak veya uygulama verilerini silmek kayıtları siler.

## 1.1 yenilikleri

- **Ayarlar:** İşyeri/Şube adları değiştirilebilir. Yeni görev yeri/şube eklenebilir; çalışma günleri, kişi başı haftalık üst sınırı (0=sınırsız) ve otomatik/yalnız manuel dağıtım seçilebilir. En fazla 20 yer desteklenir. İlk iki yerin ana döngü ve hafta içi kuralları korunur, adları serbesttir.
- Yer kimliği sabittir: ad değişse de geçmiş nöbetler, mazeret bağlantıları ve sayımlar korunur. Çizelge/PDF/istatistikler yeni adı gösterir.
- Açık temanın düğmeleri **mavi zemin/beyaz yazı**; bilgi bildirimleri uygulama içinde aynı renklerde gösterilir. Açık/koyu/sistem seçimi hatırlanır.
- Kaydedilmiş haftada **Kaydedilmiş listeyi düzenle** veya gün/yer alanına dokunun. Düzenlemeyi **Değişiklikleri kaydet** ile onaylayın. Yalnız o hafta değiştirilir; diğer haftalar silinmez. Sayımlar güncel kayıtlardan hesaplanır; tekrar kaydetmek mükerrer sayım üretmez.
- Otomatik dağıtım şubede kişi başı haftalık sınırı korur. Elle aynı kişiye ikinci görev verildiğinde önceki atama tarihleri gösterilir ve **Tekrar atamak istiyor musun?** onayı istenir. Onay yalnız bu haftanın ilgili kişi/yer sınırı için istisnadır; mazereti, perşembe/cuma dinlenmesini veya aynı gün iki yer yasağını kaldırmaz. İstisna kaydedilir ve yeniden açınca korunur.
- PDF yalnız başlık, tarih aralığı ve çizelge içerir. “Onaylı haftalık çizelge”, alt açıklamalar ve hazırlayan/onaylayan alanları kaldırıldı. Atama olmayan hücreler **—** gösterilir. Yeni yerler dahil edilir; 3'ten fazla yerde PDF ek sayfalara bölünür.

## Kullanım

1. **Personel:** İsimleri ilk hafta sonu sırasına göre ekleyin. Personel çıkarma pasife alma şeklindedir; geçmişi korunur, bugün/gelecek atamaları kaldırılır. Yeniden etkinleştirebilirsiniz.
2. **Ayarlar:** Yer adlarını ve yeni şubeleri tanımlayın. Yeni yer eklemek eski haftalara kendiliğinden personel yazmaz; ilgili haftayı düzenleyin veya yeni öneri üretin.
3. **Mazeret:** Senelik izin/rapor/genel mazeret, başlangıç ve bitiş dahil **tüm yerlerde** atamayı engeller. İsterseniz yalnız seçtiğiniz yere kısıt girin. Mazeret eklenince yalnız çakışan atamalar kaldırılır; diğer haftaların çakışmayan kayıtları korunur. Boşalan günler ayrıca düzenlenebilir veya yeniden dağıtılabilir.
4. **Çizelge:** Hafta seçin → otomatik öneri veya boş manuel çizelge açın. Ana yerde her gün bir kişi; varsayılan şubede hafta içi kişi başı bir gün. Yeni yerlerde ayarladığınız gün/sınırlar kullanılır. Uygun kişi yoksa zorla atama yapılmaz; eksik günler bildirilir ve kayıttan önce teyit edilir.
5. Cumartesi+pazar ana yerde aynı kişidir. Bu kişi aynı haftanın perşembe/cuma günleri **tüm yerlerde** dinlenir. Manuel listede önce hafta sonunu seçmek kolaylık sağlar. Çakışan eski alanı önce boşaltın.
6. Hafta sonunu atlamak veya devretmek için mazeret bitiş tarihini girin. Atlanan kişi bitişten sonraki ilk uygun hafta sonuna önceliklidir. Birden çok bekleyen varsa en erken mazereti biten uygun kişi önceliklidir; diğerlerinin sırası korunur. Sonradan eski hafta düzenlense de bekleyen haklar, geçmiş kayıtlar üzerinden takip edilir.
7. Onaylı haftada **PDF oluştur ve paylaş** → WhatsApp veya başka uygulamayı seçin. Alıcıyı siz belirlersiniz; otomatik mesaj gönderilmez.

## İstatistik tablosu

Personeller Türkçe alfabetik sıradadır. A: Personel, B: ana yerin hafta içi nöbet günleri, C: ana yerin hafta sonu nöbet günleri, D: verdiğiniz adla ilk şube, E/F/...: eklediğiniz diğer görev yerleri. Şube sütunları o yerdeki toplam nöbet günleridir. Cumartesi+pazar 2 gündür, sıra açısından 1 bloktur. Tüm sütunlar için tablo yatay kaydırılabilir.

Bugün ve öncesindeki onaylı atamalar ile gelecek atamalar ayrı tablolardadır. Taslaklar sayılmaz. Fiilen gerçekleşmeyen görevleri kaydedilmiş çizelgede düzeltin; uygulama devam/saat ölçümü yapmaz.

## Gizlilik ve ortak kullanım

İnternet izni, reklam veya kişisel veri günlüğü yoktur. Veriler cihazın özel alanında atomik dosya güncellemesiyle tutulur; otomatik bulut yedeği kapalıdır. PDF yalnız kullanıcı paylaşımıyla seçilen uygulamaya okunabilir URI üzerinden verilir. PDF kayıtların yedeği değildir. Telefonlar kendi yerel verilerini tutar; **canlı eşitleme ve yönetici hesapları bu sürümde yoktur**. Kabul edilen yetki düzeni [ORTAK-KULLANIM.md](docs/ORTAK-KULLANIM.md) içindedir.

## Derleme

JDK 17, Android SDK 35, build-tools 35.0.0. Android Studio'da bu klasörü açın, `local.properties` dosyasında SDK yolunu tanımlayın. Gradle 8.9 wrapper SHA-256 doğrulamalıdır.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Windows: `gradlew.bat ...`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Bu teslim doğrudan kurulabilen debug imzalı APK'dır; Play Store yayını değildir. İmza anahtarı ZIP'e/repoya eklenmez; sonraki güncellemelerde aynı anahtar gerekir.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.semih.nobet.test/com.semih.nobet.SmokeRunner
```

Depolama/PDF testleri ayrı dizin kullanır; uygulamanın mevcut personel kayıtlarına yazmaz.
