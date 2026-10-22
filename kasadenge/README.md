# KasaDenge 1.0.0

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

İmzalı yayın için `KASADENGE_SIGNING_PROPERTIES` özel dosya yoludur; `storeFile/storePassword/keyAlias/keyPassword` içerir. Özel anahtarlar kaynakta bulunmaz. Aynı uygulamayı veri silmeden güncellemek aynı imza ve daha yüksek versionCode gerektirir. Bu 1.0.0 ilk yayındır.

Arayüz: açık kurumsal/koyu modern temalar, yeşil/mint vurgu ve tutarlı açık/fazla renkleri. POS ana ekranında bugün/7 günlük tahsilat özeti; uzun rapor formu açılır bölümde tutulur. Tarihler ekran ve CSV/PDF raporlarında **GG.AA.YYYY** görünür; yedek/veritabanındaki ISO tarih biçimi hesaplamalar için korunur. Tarih seçicinin sistem penceresi telefonun dilini kullanabilir.
