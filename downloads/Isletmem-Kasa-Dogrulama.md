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
