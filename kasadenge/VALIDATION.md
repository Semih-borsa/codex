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
