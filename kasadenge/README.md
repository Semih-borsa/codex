# İşletmem Kasa 1.0.4

Android 8+ için çevrimdışı kasa sayımı, mutabakat, POS raporu ve tahsilat takvimi. Paket: `tr.kasadenge.app`. Diğer uygulamalardan ayrı veritabanı kullanır.

## Sayım

Beklenen tutar avans hariç girilir. Fiziksel hedef = beklenen + o günün avansı. Sayılan = banknot adetleri × değerleri + adetle sayılan madeni + diğer bozuk. Fark = sayılan − hedef. Gereken diğer bozuk = hedef − banknot ve adetli madeni toplamı. Negatif ihtiyaçta hedefin zaten aşıldığı açıklanır. Kâğıt 5 TL'nin hemen altında madeni 5 TL bulunur; diğer bozuk tutarına bu madeni tekrar dahil edilmez.

Örnek: 800 TL beklenen + 1.500 TL avans = 2.300 TL hedef. 2.100 TL banknot + 100 TL adetli madeni için 100 TL diğer bozuk gerekir. Diğer bozuk 50 TL ise kasa 50 TL eksiktir.

Kasa adları, varsayılan avanslar ve para türleri düzenlenebilir. Eski kayıtların avans/para değerleri değişmez. Taslaklar yerelde saklanır. Aynı kasa/gün için tek güncel kapanış vardır; gerekçeli düzeltmeler eski hali arşivler ve raporlarda ikinci kez sayılmaz.

## POS ve tahsilat takvimi

İşlem tarihi varsayılan dün; işlem gününe 21 takvim günü kredi kartı (yurt içi, VakıfBank, yurt dışı), World puan ve yurt dışı debit için eklenir. Yurt içi debit (VakıfBank dahil) 15 takvim günüdür. Hafta sonu/resmî tatil ertelemesi veya komisyon uygulanmaz. Bunlar **beklenen brüt** tutarlardır; banka bağlantısı/gerçekleşme teyidi yoktur. 22 Ekim kredi işlemi 12 Kasım, 21 Ekim kredi işlemi 11 Kasım olur.

Fiş fotoğrafı cihaz üzerinde okunur, kart kategorileri kontrol formuna aktarılır. Tarih/kasa/POS adı kullanıcı tarafından kontrol edilir; açık onay olmadan kayıt oluşmaz. Genel toplam kontrol alanıdır, satışlara ikinci kez eklenmez. Kategori toplamıyla fiş genel toplamı uyuşmazsa kayıt engellenir. Okunamayan/tekrarlı kategoriler tahmin edilmez. Tek fişte aynı kategoriye ait birden fazla gerçek satış satırı varsa kullanıcı bunları kontrol edip kategori toplamını elle girer. İade/iptal fişleri otomatik desteklenmez; negatif tutar girilemez.

Aynı kasa+tarih+POS/terminal adına ikinci rapor eklenmez. Farklı gerçek terminaller için farklı ad kullanılır; aynı fişi farklı adlarla kaydetmekten kaçının. Yanlış tutar, kayıt açılıp gerekçeli düzeltilebilir; eski sürümü yerelde korunur. Kimlik alanları düzeltmede sabittir. Takvim bugün, gelecek 60 gün veya iki tarih aralığı (uçlar dahil) ile tek/tüm kasaları listeler.

## Analizler

Haftalık/aylık açık, fazla ve dengeli gün sayıları; açık/fazla toplamları ayrı ayrı; avans hariç sayım, önceki dönem eşleştirilmiş kayıtlı günleri, son sekiz aynı hafta günü ortalama/ortancası ve son 12 hafta gün dağılımı. Eksik günler sıfır kabul edilmez. POS işlem toplamı ve vadeye göre beklenen tahsilat nakitten ayrı gösterilir; ikisi toplanıp satış/kâr diye sunulmaz. POS karşılaştırmaları kayıtlı raporların toplamıdır; eksik raporlar sonucu etkiler.

## Gizlilik ve yedek

SQLite yerel veri; internet izni yok. ML Kit Latin OCR modeli APK içinde, internetsiz çalışır. Fotoğraf veritabanında/yedekte saklanmaz. El yazısı ve silik fişler yanlış okunabilir, kontrol zorunludur. Sesli teyit cihazda yüklü çevrimdışı Türkçe TTS sesi gerektirir.

JSON yedeği ayar, güncel sayım/POS, taslak ve düzeltme arşivlerini içerir. İçe aktarma güncel kapanışları, güncel POS raporlarını ve yeni kasa/para tanımlarını ekler; mevcut kimlikleri değiştirmez. Taslak/eski revizyon arşivleri dosyada korunur ancak içe aktarmada veritabanına geri yüklenmez. Sürüm 1 sayım yedekleri kabul edilir. Uygulamayı silmeden önce yedek alın. CSV sayım dönemi dışa aktarımı ve Android yazdır/PDF seçeneği vardır.

## Geliştirme

JDK17, Android SDK35/Build Tools35.0.0, Gradle wrapper8.9, AGP8.7.3. `local.properties` içine yerel SDK yolu yazılır. `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintRelease`.

`node --test tests/core.test.mjs tests/pos.test.mjs` bağımsız kuruş aritmetiği/tarih/OCR metni testleri. Tarayıcı işlev testi için Playwright+Chromium kurulu olmalı: `python3 -m http.server 8769 --directory app/src/main/assets` ardından `node tests/browser.cjs`. Tarayıcı köprüsü yalnız testte taklittir. Gerçek SQLite/OCR için emülatörde `CashInstrumentation`; `tests/emulator.py` localhost5555 ve Python adb-shell istemcisi kullanır, debug APK'yı test emülatörüne yükler.

İmzalı yayın için `KASADENGE_SIGNING_PROPERTIES` özel dosya yoludur; `storeFile/storePassword/keyAlias/keyPassword` içerir. Özel anahtarlar kaynakta bulunmaz. Aynı uygulamayı veri silmeden güncellemek aynı imza ve daha yüksek versionCode gerektirir. 1.0.1, önceki KasaDenge 1.0.0 üzerine aynı imzayla güncellenir; paket/veritabanı kimliği veri koruması için değişmez.

Arayüz: açık kurumsal/koyu modern temalar, Firma Ödeme referanslı mavi/lacivert vurgu, çevrimdışı Manrope yazı tipi ve tutarlı açık/fazla renkleri. POS ana ekranında bugün/7 günlük tahsilat özeti; uzun rapor formu açılır bölümde tutulur. Tarihler ekran ve CSV/PDF raporlarında **GG.AA.YYYY** görünür; yedek/veritabanındaki ISO tarih biçimi hesaplamalar için korunur. Tarih seçicinin sistem penceresi telefonun dilini kullanabilir.

Manrope variable font: Google Fonts resmi deposu, SIL Open Font License 1.1. Lisans uygulama varlıklarında `Manrope-OFL.txt`. Arayüz referansları: Firma Ödeme Sistemi kurumsal paleti ve CepBütçem özet kartı hiyerarşisi.


## 1.0.2 Güncellemesi

Onaylanan formül açıkça gösterilir: kasa farkı = banknot + adetli madeni + diğer bozuk − avans − beklenen (avans hariç). Gereken diğer bozuk = beklenen + avans − banknot − adetli madeni. Sonuç negatifse bozuk ihtiyaç sıfır gösterilir ve hedefin aşıldığı belirtilir. Bu, önceki sürümün aynı matematiksel sonucudur; geçmiş kapanışlar yeniden yazılmaz.

Kasa adları ekran/CSV'de güncel ayarlardan çözülür; tarihî kayıtların ad anlık görüntüsü ve tüm tutarlar yedekte korunur. Bugün yatacak kartı tüm kasaları ve her kasanın kredi/debit ayrımını gösterir. World kredi grubunda, yurt dışı debit debit grubundadır; vadesi +21 olarak kalır.

Analizde Günlük / Haftalık / Aylık ve Tüm Kasalar / Tek Kasa filtreleri, kasa bazlı ve günlük toplam tabloları vardır. POS satışları işlem tarihine, tahsilatlar vade tarihine göre ayrı analiz edilir. Nakit karşılaştırması aynı kasa ve eşleşmiş kayıtlı günlerle yapılır. Tüm kasalarda gün durumu günlük net farka göre; eksik/fazla tutarlar ise kasalar arasında mahsuplaştırılmadan gösterilir.

Tutar alanları `1528.34` veya `1.528,34` kabul eder; odak ayrıldığında `1.528,34` olur. Binlik nokta ve ondalık virgül içeren eski veriler desteklenir. `1.234` üçlü gruplama olarak 1234 TL'dir; kuruş için bir veya iki ondalık hane girilir. Para adetleri tam sayıdır. `5 TL (Madeni)` banknotlarla aynı renktedir. Başlık, düğme ve alan etiketleri Türkçe baş harfleri büyük biçimde gösterilir; kullanıcı notları otomatik değiştirilmez.

Ek kontroller: `node --test tests/core.test.mjs tests/pos.test.mjs tests/reports.test.mjs` ve `node tests/regression-102.cjs` (aynı localhost sunucusu gerekir).

## 1.0.3 · Fiş OCR Düzeltmesi

POS fişlerinde OCR bloklarının metin sırası soldaki etiketlerle sağdaki fiyatları ayırabiliyordu. `ReceiptOcr` gerçek satır konumlarına göre metni soldan sağa birleştirir; kasa defteri okuma akışı bu değişiklikten etkilenmez. Ayrıştırıcı harf aralıklı başlıkları, silik tutar etiketi yazılarını ve TL son ekindeki OCR harf hatalarını tolere eder; tutarın içindeki hatalı rakam/harfleri tahmin ederek değiştirmez. Sayı parçası yanlışlıkla daha küçük tutar olarak alınmaz.

Kontrol ekranı okunan/eksik kategori sayısını ve genel toplamla uyumu gösterir. Fotoğrafta görülen kart bölümlerinin tutarı boşsa veya genel toplam yoksa kayıt onayına geçilmez. Kullanıcı düzelttiği alanlarla toplamı yeniden kontrol eder; son kayıt onayı her zaman gerekir. Otomatik kredi/debit vadesi ve nakit hesaplamaları değişmedi.

Testler: `node --test tests/core.test.mjs tests/pos.test.mjs tests/reports.test.mjs tests/receipt-ocr.test.mjs`; `node tests/receipt-ui.cjs` aynı yerel test sunucusunu kullanır. Gerçek fotoğrafla Android OCR testi bu ortamda yapıldı: dört kategori ve 60.040,08 TL tam eşleşti. Özel fotoğraf kaynakta, yayın APK'sında veya ZIP'te bulunmaz. Tekrar test için kullanıcı kendi resmini geçici androidTest asset olarak ekleyebilir ve `am instrument -w -e receiptFixture <dosya-adı> tr.kasadenge.app.test/tr.kasadenge.app.CashInstrumentation` çalıştırabilir; bu opt-in yol çıktı olarak ham/sıralanmış metin ve koordinatları verir. Test görüntüsünü yayımlamayın.

## Gün Bazlı Sayım Geçmişi Ve Onaylı Silme

Günlük Mutabakat kartı kasa seçiminden bağımsızdır: seçili tarihteki her kasanın güncel farkı bir kez toplanır. Kaydedilmiş sayımlar ve geçerli, veri girilmiş taslaklar listelenir; taslaklar ayrıca etiketlenir, geçmiş/analizlere ancak kaydedildikten sonra girer. +1 TL ve −2 TL, genel toplamda −1 TL olur. Boş kasa formu eklenmez; geçersiz veya eski revizyona ait taslak toplama katılmaz, varsa son kayıtlı tutar korunur.

Geçmiş tüm kasaları günlere göre gösterir; tek sayım veya günün sayımları açık onayla silinebilir. Silme onayında kasa/tarih/tutar, kalıcılık ve taslak/eski düzeltme temizliği gösterilir. Native SQLite işlemi onay bayrağını, kayıt revizyonunu ve kayıt zamanını doğrular; toplu işlem atomiktir. POS kayıtları, kasa ayarları ve diğer günler silinmez. Eski dış yedekler değişmez; kullanıcı eski yedekten tekrar içe aktarırsa silinen kayıt geri eklenebilir.

Yeni testler: `node --test tests/daily.test.mjs`, `TEST_BASE_URL=http://127.0.0.1:8774 node tests/history-104.cjs`. Tarayıcı testleri isteğe bağlı TEST_BASE_URL alır; varsayılan 8769'dur.


## Tek Toplam POS Ve Yatan Debit · 1.0.4

Ayarlar → Kasa Düzenle → POS Giriş Türü: Ayrıntılı (önceki yedi alan, varsayılan) veya Tek Toplam. Her kasa kendi modunu seçer; eski kaydın türü ve tutarları değiştirilmez.

Tek Toplam modunda işlem tarihi (varsayılan dün) ve bir toplam tutar girilir. Debit gelene kadar “Dağılım Bekleniyor” yazılır; 15/21 günlere tutar uydurulmaz. Sonra aynı kasada gerçek debit yatış tarihi ve tutarı girilir. Uygulama bankaya yatıştan 15 takvim günü önceki toplam POS kaydını arar, bulduğu işlem gününü ve kalan kredi tutarını onay ekranında gösterir. Aynı kasa ve gün bulunamazsa başka bir kayıt otomatik seçilmez.

Örnek: 1 Ekim işlemi 15.000 TL; 16 Ekim yatan debit 5.000 TL → 22 Ekim beklenen kredi 10.000 TL. Kredi yattığında ikinci tutar girişi istenmez. Bu, banka bağlantısıyla teyit edilmiş ödeme değil; geçmişte saklanan beklenen kredi tutarıdır. İşlem gününden +15/+21 takvim günü, komisyonsuz kuralları korunur.

Toplam veya debit yanlış girilirse güncel kaydı gerekçeyle düzeltmek gerekir; eski hali arşivlenir. Debit toplam POS'u aşamaz. Aynı kasa/güne ayrıntılı ve tek toplam kayıtları birlikte girilip çift sayılmaz. Satış analizleri toplam 15.000 TL'yi bir kez sayar; dağılım bilinmeyen satış ayrı belirtilir, tahsilat takvimi yalnız bilinen dağılımı kullanır. Veritabanı 2'den 3'e yeni tablolar ekleyerek yükselir; sayımlar/ayrıntılı POS silinmez. Eski yedekler kabul edilir; yeni yedekler toplam ve debit kayıtlarını da içerir. İçe aktarma mevcut kayıtları değiştirmez.

Ek testler: `node --test tests/settlements.test.mjs`; `TEST_BASE_URL=... node tests/settlement-ui.cjs`.
