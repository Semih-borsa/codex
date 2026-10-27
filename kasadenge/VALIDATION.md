# Doğrulama · 22 Ekim 2026

- 17 Node testi: sayım aritmetiği, kuruş hassasiyeti, dönem karşılaştırmaları, eksik günler, OCR etiketleri, CSV; POS 15/21 gün, ay/yıl ve artık gün geçişi, fiş toplamı, aralık filtreleri ve belirsiz OCR.
- Playwright: 320/412px, sayım onayı, gerekçeli düzeltme, tema/kasa ayarları, taslak, OCR onayı/tarih aktarımı, POS varsayılan dün, genel toplam uyuşmazlığı, onay öncesi kaydetmeme, takvim toplamı ve POS düzeltmesi.
- Android11/API30 emülatöründe gerçek SQLite ve APK içi OCR çalıştırıldı ve geçti. POS vadeleri, çift kayıt engeli, gerekçeli düzeltme arşivi, eski sürüm üzerinden düzeltme engeli ve yedek tekilleştirmesi doğrulandı.
- İmzalı release derlendi. Lint: 0 hata, 3 uyarı (WebView JavaScript kullanımı ve platform ExifInterface önerisi). zipalign 16KB kontrolü ve APK imza kontrolü başarılı.
- APK internet izni istemez. Türkçe TTS'nin kullanıcı telefonunda yüklü olması gerekir.
- Fiziksel kullanıcı telefonu ve gerçek banka tahsilatıyla doğrulama yapılmadı. OCR test metinleri basılı örnektir; el yazısı/gerçek fiş başarısı garanti edilmez, kullanıcı kontrolü gerekir.
- Örnek fiş: kredi 43.127,63 TL + yurt içi debit 16.912,45 TL = 60.040,08 TL; genel toplam ikinci satış satırı olarak sayılmaz.
- Son arayüz düzenlemesi: POS özet kartları, açılır rapor formu, açık/koyu temalar; tarih alanları cihazın İngilizce tarih sırasından bağımsız GG.AA.YYYY gösterilir. Tarayıcı tarih görünümü kontrolü geçti.

1.0.1 görsel güncelleme: ad İşletmem Kasa; Türkçe Manrope yazı tipi APK'ya gömülü (SIL OFL). Firma Ödeme paletinden mavi/lacivert açık/koyu tema, CepBütçem referanslı özet kartları, yeni mutabakat odak kartı. Paket ve imza korunur; 1.0.0 üzerine güncelleme mümkündür. İş kuralları bu görsel güncellemede değişmedi.


1.0.2: 22 birim testi geçti; noktalı ondalık giriş / Türkçe biçimlendirme, onaylanan avans örneği, Türkçe baş harfler, eski/yeni madeni etiketleri, kasa bazlı eşleştirme, tüm kasa günlük toplamları ve POS satış/vade ayrımı doğrulandı. Tarayıcıda 11 mevcut akış + 4 yeni regresyon grubu geçti: kasa adı değişikliğinin tarihî ad görüntüsünü silmeden geçmişte görünmesi; bugün tüm kasaların kredi/debit tutarları; tek/tüm kasa günlük/aylık raporları; 320px yatay taşma kontrolü. Geniş tablolar kendi içinde yatay kaydırılır.

1.0.3: Gerçek yüklenen POS fotoğrafı, Android11/API30 emülatöründe APK içindeki ML Kit motoruyla okundu. Eski blok sırasının etiket ve tutarları ayırdığı doğrulandı. Yeni `ReceiptOcr.orderedText` çıktısı gerçek Android'de alınıp ayrıştırıcıya verildi: 31.328,59 / 11.799,04 / 15.344,81 / 1.567,64 TL, 4/4 doğru kategori ve 60.040,08 TL genel toplam tam eşleşti. Fotoğraf yayımlanan kaynak/ZIP/APK'da bulunmaz.

28 birim testi geçti. Üç ek tarayıcı fiş akışı geçti: gerçek OCR yazım hatalarıyla alan doldurma ve onay öncesi kaydetmeme; bozuk rakamın tutarı boş bırakması ve elle düzeltme sonrası toplam uzlaşması; genel toplam okunmadığında fotoğraf kaydını engelleme. Önceki nakit/POS ekran testleri de geçti. Başka fotoğraflarda ışık, kesiklik ve netlik sonucu etkileyebilir; onay akışı korunur.

Günlük geçmiş güncellemesi: toplam 33 birim testi geçti. Dört yeni tarayıcı grubu; imzalı +1/−2=−1 toplam, kasa seçimi bağımsızlığı, boş kasanın hariç kalması, taslağın kayıtlı kasayı tek kez değiştirmesi, gün bazlı geçmiş, doğru kasayı açma, silmeden vazgeçme, açık onayla tek/gün silme ve eski onayla değişmiş kayıt silmeyi reddetme durumlarını doğruladı. Android emülatöründe gerçek SQLite silme onayı, atomik hata, taslak/düzeltme temizliği ve POS'un korunması da geçti.

Tek Toplam POS eklentisi: 37 birim testi; ayarlardan kasa bazlı tek alan, onay öncesi kaydetmeme, tarih/kasa eşleştirme, 15.000−5.000=10.000 TL, kredi için ikinci giriş gerektirmeme, dağılım bekleme ve toplamı çift saymayan analiz testleri geçti. Debit gerekçeli düzeltmesi tek kaydı koruyor ve beklenen krediyi yeniden hesaplıyor. Beş tarayıcı paketi (eski akış, gün bazlı geçmiş/silme, 1.0.2 regresyon, fiş OCR onayı, tek toplam POS) çalıştırıldı.

Android SQLite tek toplam testleri geçti: toplam/debit çift kayıt engeli, +15 gün eşleşmesi, kalan 10.000 TL kredi, gerekçeli düzeltme arşivi, şema3 yedek tekilleştirmesi ve ayrıntılı/tek toplam aynı gün çakışmasını engelleme. Yayın 1.0.4 aynı paket/imzayla versionCode5 kullanır.


## 1.0.7 · 24 Ekim 2026

50 JavaScript birim testi geçti: yeni defter şablonu, üst satır tarihi / çekim tarihi ayrımı, Kasa 1/2/3 eşleştirme, satır toplamından ve tek belirsiz satırda denge varsayımından adet çıkarma, belirsiz rakamların tahmin olarak işaretlenmesi, boş diğer bozuk alanının sıfır kabulü, kaynak okumaları çeliştiğinde uyarı ve tarih düzeltilmiş debit bağının korunması.

Tarayıcıda yeni defter akışı: tahmin onayı olmadan aktarmama, kasa/tarih değiştirme ve son kapanış onayı olmadan kaydetmeme geçti. POS tarih düzeltmesi için eski/yeni tarih önizlemesi, tek aktif kayda taşıma, vade yenileme, gerçek debit yatış gününü koruma ve sonradan debit tutar düzeltmesi akışları geçti. Önceki sayım/POS/OCR ekran regresyonları da geçti. Tarayıcıdaki native köprü taklittir.

Sınır: Android 11 emülatörü bu oturumda soğuk açılış ve çekirdek zaman aşımı sorunları verdi. Android 8 alternatifinde de ADB kurulum çağrıları zaman aşımına girdi. Bu sürümde gerçek yüklenen EL YAZISI fotoğrafının uçtan uca Android OCR sonucu ve yeni POS tarih düzeltmesinin native SQLite test sonucu doğrulanamadı. Test kodları eklendi; bunlar çalıştırılmış/passed olarak sunulmuyor. Önceki sürümlerin basılı fiş/native veritabanı sonuçları bu sayfada kendi sürümleri altında kalır.

Yeni OCR alan büyütme ve mavi yazı ayrıştırma kodu derlendi; el yazısında doğruluk garantisi yoktur. Tahminler ayrı işaretlenir, kullanıcı kontrolü ve son kayıt onayı korunur. Özel fotoğraf yayımlanan APK/kaynak/ZIP içinde bulunmaz.

Önceki uzak depo tarih seçici ve fiş tarihi değişiklikleri birleştirildi; güncel fiş tarayıcı testi tekrar çalıştırıldı (sonucu yayın kontrolünde kayıtlı). Orijinal imza bu ortamda mevcut; ayrı tarih varyantının anahtarı mevcut değil.

1.0.8: Kasa kaldırma tarayıcı kontrolü: vazgeçmede değişiklik yok; onayla active=false, seçili kasa etkin kasaya geçer; önceki POS/revizyon verileri korunur, son etkin kasa kaldırılamaz. Native veritabanı değişmedi.

1.0.9: Tarayıcıda son etkin kasayı kaldırma, kasasız Sayım/POS ekranları, yeniden kasa ekleyip sayıma devam etme ve mevcut POS/revizyon verilerini koruma geçti. Android ayar doğrulaması sıfır kasa/sıfır etkin kasa kabul eder; yeni kurulum varsayılan kasa oluşturmaz. Native cihaz testleri bu oturumda çalıştırılmadı.

1.0.10: Playwright daralan viewport ve taklit IME bildirimiyle beklenen tutar/son para adetlerinin görünürlüğü, Enter ile madeni 5 TL ve diğer bozuk alanına ilerleme, değerlerin ve sıfır olmayan kaydırmanın korunması, klavye kapanınca menünün geri gelmesi geçti. Gerçek Android klavyesiyle cihaz testi yapılmadı. Android IME köprüsü derleme kontrolünden geçer.

1.0.11: 51 Node testi geçti: +16 ay geçişi, tek toplam debit tam tarih eşleştirmesi, yanlış gün reddi, kredi +21, eski tahsilatın gerçek yatış günü/tutarının korunması ve yeni planlanan vade. Android schema3→4 geçişi ve eski tahsilat koruma native test senaryosu eklendi; gerçek Android/emülatörde çalıştırılmadı. Migration SQLiteOpenHelper yükseltme işlemi içinde çalışır; hata yükseltmeyi geri alır.

1.0.12 Analiz: 56 Node testi geçti; tam geçen hafta ortalaması, eksik gün/kasa kapsamı, az veri, sıfır baz, haftalık toplam denge ve çoklu POS terminali karşılaştırmaları doğrulandı. Playwright: örnekte 150 TL ortalamaya göre 100 TL için 50 TL düşük yorumu, kapalı ayrıntılar, açıldığında eski tablolar, tekil filtre kimlikleri, kasa filtresi ve 320px görünüm geçti. Android derlemesi ve APK imzası doğrulandı. Gerçek Android cihazda yeni analiz ekranı testi çalıştırılmadı.

1.0.13: 61 Node testi geçti; genel/kasa bazlı günlük nakit, imzalı fark, terminal birleştirme, kategori/vade günü karşılaştırması, eksik kasa ve belirsiz POS yüzdesi kontrolleri. Playwright: 📆 köprüsüyle tarih seçimi ve yatay pointer kaydırma kayıt tarihini/girilen tutarı değiştirmedi; POS genel/kasa kredi-debit toplamları ve yüzdeleri, ileri gün ve 320px taşma kontrolü geçti. Gerçek Android dokunma/datepicker test edilmedi; tarayıcı native köprüsü taklit edildi. Android derlemesi/imza kontrolü yayın öncesinde tamamlandı.

1.0.14: 64 Node testi geçti. Regresyonlar: başka kasadaki bilinmeyen dağılım, sağlam kasanın yüzdesini engellemez; aynı kasada debit vadesi belirsizken bilinen kredi yüzdesi hesaplanır; eski kayıt/pozitif baz yoksa açık neden. Playwright: başka kasada belirsiz POS varken genel neden ve sağlam kasada %10,0, 320px taşmasız görünüm geçti. Android assembleRelease ve imza kontrolü başarılı; gerçek cihazdaki kişisel kayıtlar görülmedi/test edilmedi.

1.0.15: 65 Node testi geçti. Farklı kasa kapsamıyla mevcut toplamların karşılaştırılması, bekleyen dağılıma rağmen genel/kasa/kategori yüzdeleri ve dağılım çözülünce −%40 → +%10 yeniden hesaplama kontrol edildi. Tarayıcıda genel ve tek kasa %10,0 ile bekleyen dağılım notu ve 320px görünüm geçti. Android build/imza kontrolü başarılı; gerçek cihaz testi yapılmadı.

1.0.16: 68 Node testi geçti; yıl/ay gruplama, aylık net −5 TL, tek toplam POS ay toplamı, çoklu kasa/terminal günlük birleşiminden haftanın günü ortalaması ve eksik/gelecek günlerin dışlanması. Tarayıcı: tüm kasalar için 300 TL vade aralığı, giriş kasası değişiminden bağımsız toplam, özel rapor filtresiyle 200 TL, tarih sırası değişiminde görünür aralığın tutarla uyumu, Nakit/POS yıl/ay açılır geçmiş ve aylık net, grafiklerin iki yorumdan önce gelmesi ve 320px taşma kontrolü geçti. Gerçek Android cihaz testi yapılmadı. Veritabanı/hesaplama vade kuralları değiştirilmedi.

1.0.17: Tarayıcıda tek nakit ve tek POS haftanın günleri grafiği, altlarında toplam14 tam gün/tutar satırı, örnek Perşembe ortalaması100 TL, grafik/yorum sırası, düz geçmiş öğelerinde sıfır radius/gölge, mevcut yıl/ay net ve vade aralığı kontrolleri, 320px taşma kontrolü geçti. Hesaplama modülleri değişmedi; önceki68 birim testi sonucu 1.0.16 altında geçerlidir. Android derlemesi/imza doğrulandı; gerçek cihaz testi yapılmadı.

1.0.18: Tarayıcıda İşletme Kasa başlık/markası, dün kayıt tarihi ve bugün görüntüleme/analiz, analiz tarihini değiştirince sayım tarihi/adet korunması, manuel tarih koruma, yıl geçişinde1 Ocak→31 Aralık varsayılanı ve Taslak başlık rozetinin yokluğu geçti. Hesaplama/veritabanı modülleri değişmedi. Android build, manifest adı/paket ve APK imzası yayın öncesi kontrol edilir; gerçek cihaz testi yapılmadı.

1.0.19: 72 Node testi geçti. Ek senaryolar: kasa1+%20/kasa2−%10 ve lider, henüz gelmeyen hafta gününün dışlanması, çoklu POS terminalinin toplanması, dağılımsız toplamın satışa dahil edilmesi, eşit lider, sıfır baz ve eksik/kısmi gün, tek kasa filtresi. Playwright aynı örneklerin iki yorum grubunda doğru gösterilmesini, kasa filtresini ve320px taşmasız görünümü doğruladı. Gerçek Android cihaz testi yapılmadı.
