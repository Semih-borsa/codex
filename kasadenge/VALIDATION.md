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


## 1.0.5 Fiş İşlem Tarihi · 24 Ekim 2026

- 37 Node birim testi geçti. Kuruş aritmetiği ve +15/+21 tarih hesapları korunur.
- Fiş arayüz regresyonu: native takvim köprüsü tarayıcıda taklit edilerek iki gün önceki tarih seçildi; tarih onaydan kayda ve tüm satırların +15/+21 vadesine doğru geçti. Geçersiz eski takvim callback'i yok sayıldı. Önceden kayıt açılmışken yeni fotoğraf tarih kilidini/revizyonunu taşımıyor. Eksik tutar/genel toplam engelleri ve açık onay korundu. Bu tarayıcı testi gerçek Android takvim dokunma testi değildir.
- Mevcut sayım/POS ve tek toplam/debit tarayıcı paketleri geçti.
- `assembleDebug`, `lintDebug`, `assembleDateFix`, `lintDateFix` başarılı; 0 lint hata, mevcut 3 uyarı. Özel imzalı APK `apksigner verify` ve 16KB zipalign kontrolünden geçti. Paket `tr.kasadenge.app.tarih`, sürüm 1.0.5/code6; debuggable olmayan ayrı kurulum. Orijinal imza bu ortamda bulunmadığı için eski APK üstüne güncelleme olarak sunulmaz. Ana `release` paket kimliği değiştirilmedi.
- Bu sürümün gerçek cihazda Android takvim etkileşimi ve eski kullanıcı yedeğiyle aktarımı henüz doğrulanmadı. Eski APK silinmez; yedek ve orijinal uygulama korunarak aktarım yapılır. Yeni APK'da başlangıç verileri boştur; özel fiş/yedek/anahtar ZIP'e eklenmez.
