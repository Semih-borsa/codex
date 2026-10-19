# Nöbet Çizelgesi — Android

İşyeri ve şube nöbetlerini çevrimdışı planlar. Personel ekleme/pasife alma, otomatik dağıtım, sıfırdan manuel çizelge, onay, tarihli izin/rapor, hafta sonu sırası, istatistik ve PDF paylaşımı.

[APK indir](../downloads/NobetCizelgesi.apk) · [Android Studio kaynak ZIP](../downloads/NobetCizelgesi-Android-Studio.zip)

Android 8.0 ve üzeri. APK'yı indirip açın; tarayıcınız için “Bilinmeyen uygulama yükleme” izni gerekebilir. İlk açılışta **Personel** bölümüne isimleri girin; hazır isim veya kişisel veri içermez. Her telefonda veriler ayrıdır; çok kullanıcılı eşzamanlama yoktur.

Üstteki **Görünüm** düğmesiyle Sistem/Açık/Koyu temayı seçebilirsiniz. Kurumsal lacivert palet kullanılır; seçim cihazda hatırlanır.

## Kullanım

1. Personelleri hafta sonu başlangıç sırasına göre ekleyin. Ekleme sonrası çıkarma, geçmişi silmeden pasife alma şeklindedir. İstatistikler korunur; bugün/gelecek atamaları kaldırılır. Yeniden etkinleştirme mümkündür.
2. **Mazeret** → senelik izin/rapor/diğer genel mazeret girin. Başlangıç ve bitiş dahil her iki yerde atamayı engeller. Yalnız bir yer için kısıt gerekiyorsa “Yalnız şube/işyeri kısıtı” seçin. Düzenlenebilir/silinebilir.
3. **Çizelge** → hafta seçin → “Otomatik dağıtım öner”. İşyeri her gün 1 kişi; şube hafta içi 1 kişi, kişi başı haftada en fazla 1 şube günü. Cumartesi ve pazar aynı işyeri nöbetçisi. Bu kişi aynı haftanın perşembe/cuma günleri iki yerde de izinlidir.
4. İsterseniz **“Boş çizelge aç • manuel oluştur”** ile sıfırdan başlayın. Gün/yer alanına dokunarak kişi seçin veya boş bırakın. Otomatik öneri de düzenlenebilir. Manuel listede önce hafta sonunu seçmek perşembe/cuma izinlerinin yerleşmesini kolaylaştırır. Çakışma halinde atama reddedilir; çakışan alanı önce boşaltın.
5. Şubeye o hafta gitmeyecek kişiyi “Bu hafta şubeye gitmeyecek personel” ile teyit ederek hariç tutun. Uygulama bu kişiyi şubeye atamaz, işyeri uygunluğu sürer.
6. Hafta sonu nöbetçisini atlamak için **mazeret bitiş tarihini** seçip onaylayın. Ya da cumartesi/pazar alanına dokunup devralacak kişiyi seçin ve devredenin mazeret bitişini girin. Perşembe/cuma izinleri yeniden hesaplanır. Döngüde atlanan kişi, bitiş tarihinden sonraki ilk uygun hafta sonuna öncelikli gelir; iki günün de uygun olması gerekir. Birden çok bekleyen kişi varsa aynı hafta sonuna hepsi atanamaz: bitiş tarihi en erken olan uygun kişi önceliklidir, diğerlerinin hakkı korunur ve uyarı gösterilir. Elle devralma normal öneriyi de yerinden ediyorsa o kişinin sırası da bekleyenler listesinde tutulur.
7. **Dağıtımı onayla ve kaydet**. Öneri otomatik kaydedilmez; atama kuralları ve boş kalan alanlar teyit edilir. Uygun personel yoksa kural ihlal edilmez; alan BOŞ kalır ve uyarılır.
8. Onaylı haftada **PDF oluştur ve paylaş** → WhatsApp veya istediğiniz uygulamayı seçin. PDF A4 yataydır, 7 günü ve iki yeri gösterir; mazeret nedenlerini paylaşmaz. Otomatik mesaj göndermez; alıcıyı siz seçersiniz.

## Sıra ve istatistikler

Hafta sonu sırayla döner. Mazeret dönüşleri normal döngünün arasına öncelikli girer. Hafta içi işyeri yükü önceki onaylı haftalardaki nöbet sayılarıyla dengelenir. Şube atamaları kişilerin uygun günlerini dikkate alan aramayla oluşturulur; mümkünse 5 kişinin her birine bir gün verilir. 4 uygun kişiyle 5 şube günü ve kişi başı en fazla 1 gün aynı anda sağlanamaz; eksik gün ve gitmeyen personel açıkça belirtilir. 5'ten fazla personelde haftada yalnız 5 şube yeri vardır; geçmiş sayılar denge için kullanılır.

İstatistikler personel ve yer bazında hafta içi/hafta sonu nöbet **günlerini** sayar. Cumartesi+pazar 2 gündür, sıra açısından 1 bloktur. Bugün ve öncesindeki onaylı atamalar ile gelecek atamalar ayrıdır. Fiili gelmeyen/değişen görevleri çizelgede düzeltin; uygulama devam takibi veya saat ölçümü yapmaz.

Geçmiş bir haftanın yeniden onaylanması, o haftadan sonraki planları siler; döngü tutarlılığı için yeniden oluşturup onaylamanız gerekir. Mazeret eklenirken çakışan atamalar kaldırılır ve etkilenen ilk haftadan sonraki planlar açık onayla temizlenir. Bu davranış onay pencerelerinde belirtilir. Aynı hafta tekrar kaydedildiğinde istatistikler iki kez sayılmaz.

## Veri ve gizlilik

İnternet izni ve reklam yoktur. Kayıtlar uygulamanın özel alanında atomik dosya güncellemesiyle tutulur; otomatik bulut yedeği kapalıdır. PDF yalnız paylaşım için geçici önbelleğe yazılır; diğer uygulamalara yalnız seçilen PDF için okuma yetkisi verilir. Verileri silmek/uygulamayı kaldırmak geçmişi siler. PDF paylaşımı kayıtların yedeği değildir. APK güncellemesinde kaldırıp yeniden kurmak yerine aynı imzalı APK üzerinden güncelleyin.

## Derleme

JDK 17, Android SDK platform 35, build-tools 35.0.0 ve Gradle 8.9 (doğrulamalı wrapper) gerekir. Android Studio'da bu klasörü açın; `local.properties` dosyasında yerel SDK yolunu tanımlayın. Kaynak ZIP; kişisel kayıt, SDK veya imza anahtarı içermez.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Windows: `gradlew.bat ...`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Bu teslim doğrudan kurulabilen **debug imzalı** APK'dır; Play Store sürümü değildir. Sonraki güncellemelerde aynı imza gerekir; özel anahtar repoya eklenmez.

Android cihaz/emülatör üzerinde depolama, PDF ve açılış testleri:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.semih.nobet.test/com.semih.nobet.SmokeRunner
```

Testler kayıt/PDF kontrollerinde ayrı bir dizin kullanır; uygulamanın mevcut personel kayıtlarına yazmaz.
