# Bildirim, ayarlar ve geri alma — 1.6.0

## Teslim ve değişen dosyalar

- `App.jsx`: bildirimden ödeme ayrıntısı/kayıt gezinmesi; ayar bölümleri; silme sonrası geri alma.
- `android/app/build.gradle`: sürüm 1.6.0, versionCode 18.
- Yeni testler: `payment-navigation.test.mjs`, `payment-navigation.browser.test.mjs`.
- Mevcut test köprüsü ve tarayıcı yardımcıları genişletildi. `phase-b.browser.test.mjs` kart ayarlarına ulaşırken yeni bölümü açar. `payment-today.browser.test.mjs` native taklidine dinleyici desteği eklendi. Finans beklentileri değiştirilmedi.
- `backup-export.browser.test.mjs`: doğrulanmış yedek tarihi ve iptal durumuna iki kontrol eklendi.

Yeni bağımlılık, veri tabanı veya native eklenti eklenmedi. Tek App.jsx ile uygulama kaynak teslimi devam ediyor; Android sürüm artışı Gradle dosyasındadır. Önceden kaldırılan PDF analizi/Gemini geri eklenmedi. Raporlardaki 6 maaş dönemi ve mevcut yerel yorumlar korundu.

## Bildirime dokunma

Yeni hatırlatmalar ödeme, kredi taksiti veya kart/ekstre kimliğini taşır; ad/tutar benzerliğinden eşleştirme yapılmaz. Tek hedefte ödeme ayrıntısı açılır. Aynı saate birden fazla ödeme varsa seçim listesi açılır. Eski birleşik bildirimler yalnız tam tarih/kimlik bağı kesin ve benzersizse çözümlenir.

Ayrıntı her açılışta mevcut kaydı okur: güncel tutar, vade, kısmi ekstre ödemesinden kalan, tamamlanmış veya silinmiş/belirsiz kayıt durumu gösterilir. Güncel borç başka bir eski ekstreye kopyalanmaz. Bilinmeyen bağlantıda ödeme yapılmaz. Bildirim açmak finans kaydı oluşturmaz/değiştirmez.

“Ödeme kaydını aç” normal ödemede doğru grubu açıp ilgili kaydı odaklar; kredide ilgili taksiti açar. Kartta önce belirli ekstrenin güncel ayrıntısı gösterilir; “Kredi kartını görüntüle” mevcut kartı açar. Bu düğme otomatik ödeme yapmaz. Ödeme kaydı silinmişse Ödemeler listesine geçilebilir. Tutar değiştirme/ödeme için mevcut akış ve onaylar korunur.

Ödeme tamamlanınca gelecekteki sahipli alarmlar yeniden planlanır/iptal edilir; sistem bildirim alanındaki ödenmiş/silinmiş hedef içeren eski bildirimler kaldırılmaya çalışılır. Geri alınan ödeme uygun gelecek seçili saatlerde tekrar planlanır. Geçmiş saatler için peş peşe bildirim üretilmez; mevcut aynı gün telafi/tekrar engeli korunmuştur.

Açık, arka plan ve soğuk açılış için Capacitor olay dinleyicisi kullanılır. Kurulu LocalNotifications Android kaynağında dokunma olayının dinleyici kurulana kadar tutulduğu doğrulandı. Bu kaynak incelemesi fiziksel cihaz testi yerine geçmez.

## Sade ayarlar

Görünüm açık gelir; açık/koyu/gri ve halka/ibre seçenekleri korunur. Maaş ve dönem, Hatırlatmalar, Kartlar ve kategoriler, Gelişmiş ayarlar ayrı açılır bölümlerdir. Maaş tutarını düzenle mevcut Maaş ekranına gider. Bildirim özetinde açık/kapalı, günler ve saatler görünür; yeni anahtar hatırlatmaları kapatıp açar. Eski ayarlarda bu alan yoksa mevcut açık davranış korunur.

Yedek Al/Yedeği Yükle doğrudan erişilebilir. Son doğrulanmış dosya yedeği tarihi yalnız native dışa aktarma gerçek kaydetme/doğrulama başarısı döndürdüğünde kaydedilir. İptal, hata veya tarayıcı indirmesinin başlaması doğrulanmış başarı sayılmaz. Cihaz içi otomatik kopyalar, devir kontrolü, kayıt geçmişi, CSV ve hedef gibi seyrek seçenekler Gelişmiş ayarlar altındadır. Geri yükleme önizlemesi ve güvenlik kontrolleri korunmuştur. Mevcut tercihler kendiliğinden değiştirilmez.

## Silinen işlemi geri alma

Mevcut kısa süreli geri alma altyapısı geliştirildi. Silme sonrası bildirimde Geri Al bulunur; ayrıca sayfalar arasında geçsen de “Son silinenler” alanından **1 dakika içinde** geri alınabilir. En son 10 geri alma kaydı bu oturumda tutulur. Uygulama yeniden başlatılırsa bu geçici liste korunmaz; kalıcı çöp kutusu değildir.

Normal işlem, ekstre ödeme kaydı ve normal ödemeyle bağlantılı işlem kayıtları eski kimlikleriyle geri getirilir. Bağlı kayıtların tamamı hazırlanır; sonradan eklenen farklı kayıtlar korunur. Aynı kimlikli farklı kayıt varsa üzerine yazılmaz. Bozuk kayıt okunursa işlem başlatılmaz. Yazma başarısızsa mevcut ortak saklama grubunun geri alma koruması kullanılır; kısmi geri yükleme başarılı sayılmaz. Aynı geri alma kaydı iki defa çalışmaz.

Kredi taksiti ödeme işlemleri hâlâ Krediler ekranındaki mevcut geri alma akışından yönetilir; bu bağlantıları koparacak serbest işlem silme açılmadı. Bu çalışma geçmiş arşivleri veya gerçek kullanıcı yedeklerini otomatik değiştirmez.

## Doğrulama

**Son genel koşu: 509/509 başarılı, 0 başarısız** (`kanitlar/final-tests.tap`). Native birim test görevi ve Android debug derlemesi başarılı; değişmeyen görevler Gradle önbelleğinden yararlanmıştır. Web üretim derlemesi ve Capacitor sync başarılı.

Yeni kontroller: üç tema, kapalı/açık ayar bölümleri, normal ödeme ve kredi hedefi, doğru kart/ekstre/kısmi bakiye, eski bildirimin güncel/silinmiş/ödenmiş kaydı, birleşik bildirim seçimi, taklit soğuk açılış, arka arkaya gelen olay, hatırlatma aç/kapat, ödeme/geri alma sonrası plan ve bildirim temizliği, bağlı silme/geri alma, süre aşımı, kimlik çakışması, bozuk veri ve yazma başarısızlığında bütünlük. Yedek başarı/iptal tarihi ayrıca sınandı.

İlk koşudaki hatalar, yeni yardımcıların eski metin dilimi tabanlı test köprüsüne JSX sokması, eski native taklidinde dinleyici bulunmaması ve kapanan ayar bölümünü eski testlerin açmadan kullanmasından kaynaklandı; test erişimi/akışı düzeltilip aynı finans beklentileriyle tekrarlandı. Yeni ödeme akışında tamamlanmış kaydın kapalı gruba taşınması da giderildi: bildirimden açılan kaydın bekleyen/ödenmiş grupları erişilebilir tutulur.

Önceki sürümlerde kaldırılan API/PDF ekranlarına ait testler kaynak olarak tutulup önceki teslimlerdeki gibi koşu dışındadır; bunlar geçmiş kodun testleri olarak saklanır. Yeni işlevlere uygun yeni kabul testleri koşuya dahildir. Mevcut 14 ana finans/yedek/bildirim işlevi önceki sürümle kaynak olarak aynı (`kanitlar/scope.json`). Genel lint temizliği yapılmadı.

## Android teslimi ve güvenli kurulum

APK: `Finans-1.6.0.apk`; package/applicationId `com.butcetakip.app`; versionCode 18. APK ve App.jsx dosyalarının boyut/SHA-256 değerleri `SHA256.json` içindedir. Sertifika SHA-256 önceki burada teslim edilen debug sürümleriyle aynı: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.

Telefonda kurulu imzaya erişim yok; üzerine güncelleme yalnız kimlik ve imza aynıysa mümkündür. Önce doğrulanmış JSON yedeği al. Çakışırsa uygulamayı kaldırma/verilerini temizleme; kurulu sürümün doğru imzasıyla derleme gerekir.

Tam App.jsx `src/App.jsx` yerine kaydedilir. Native sürüm dosyası `android/app/build.gradle` yolundadır. Hazır APK bilgisayarda derleme gerektirmez. Elle derlemede mevcut bağımlılıklarla `npm run build`, `npx cap sync android`, ardından Android derleme yapılır.

### Gerçek telefonda yapılması gerekenler

Bu APK fiziksel cihazda henüz doğrulanmadı. Native bildirim testlerinde köprü taklit edildi; gerçek Android alarm/dokunma/pil kısıtlaması davranışı kanıtlanmadı.

1. Önce yedek al, güncelle; bakiyeler ve mevcut kayıtları karşılaştır.
2. Yakın vadeli sentetik ödeme için seçili saatte bildirimi bekle. Uygulama açıkken, arka plandayken ve normal şekilde kapalıyken dokun; doğru ayrıntı açılmalı.
3. Aynı saatte iki ödeme oluştur; seçim listesinden doğru kayıt açılmalı. Kredi taksiti ve belirli kart ekstresini ayrıca dene.
4. Öde; gelecek hatırlatmanın ve eski sistem bildiriminin durumunu kontrol et. Eski bildirime dokunulursa yeniden ödeme yaptırılmamalı. Ödemeyi geri al; uygun gelecek hatırlatma dönmeli.
5. Kısmi ekstre ödemesinde kalan tutarı, silinmiş kayıtta anlaşılır mesajı doğrula.
6. Telefon yeniden başladıktan sonra planları, bildirim izni kapalıyken uyarıları kontrol et. Android ayarlarından zorla durdurma normal kapatma değildir; bu durumda alarm engellenebilir.
7. Bir test işlemini sil, başka sekmeye geç, 1 dakika dolmadan Son silinenler'den geri al; tutar ve bağlantı yalnız bir kez geri gelmeli. Süre dolduktan ve uygulama yeniden başlatıldıktan sonra geçici geri alma bulunmamalı.
8. Üç temayı, kaydetme/iptal sonrası yedek tarihini ve geri yükleme önizlemesini kontrol et.

`geri-donus/` işlem öncesi App.jsx ve build.gradle kaynaklarını içerir. Kaynak geri alınabilir. Cihazda zorla eski versionCode kurulmaz; gerekirse aynı imzalı daha yüksek versionCode taşıyan geri dönüş APK'sı hazırlanır.
