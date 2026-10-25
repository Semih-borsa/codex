# POS Kasa Yönetim — Build v24 genel inceleme

## Kapsam ve durum

İstenen taşıma, küçültme ve satır bazında kilitli kasa düzenlemesi uygulandı. Aşağıdaki ilave bulgular yalnız inceleme sonucudur; kullanıcı onayı bekleniyor. Gerçek finans kayıtları değiştirilmedi. Chromium kontrolü gerçek Windows/Electron testi değildir.

## Öncelikli, yeniden üretilen sorunlar

1. **Yüksek — yedek şema doğrulaması yetersiz.** `jsonGeriYukle` yalnız txs/cfg alanının varlığını kontrol ediyor. Sentetik `{"txs":"bad"}` dosyası kabul edilip `3 işlem geri yüklendi` mesajı üretti. Liste olmayan içerik kayıtların açılmasını bozabilir. Öneri: yazmadan önce tüm çekirdek alanları, kasa/banka bağlantılarını, tarihleri, tutarları ve v23 bağlı toplam/debit ilişkilerini doğrulama.
2. **Yüksek — depolama yazma başarısızlığı geri yüklemede başarı sayılıyor.** Ortak `LS.set` hatayı sessiz yutuyor. Yazma hatası oluşturulan sentetik testte hiçbir kayıt yazılmadığı halde geri yükleme başarılı mesajı üretildi. Öneri: güvenlik kopyası, yazma kontrolü ve tekil/tutarlı geri yükleme; başarısızlıkta başarı mesajı göstermeme. V23'te cfg/txs normal kaydetme yolları ayrı kontrol içeriyor; risk ortak LS kullanan diğer yollar ve geri yüklemede sürüyor.

## Koddan görülen, ayrıca senaryo testi gereken riskler

3. **Yüksek — geri yükleme birden fazla anahtara sırayla yazıyor.** Ortasında kesinti/yazma sorunu olursa bazı alanlar yeni, bazıları eski kalabilir. Geri dönüş/ön doğrulama mekanizması gerekir.
4. **Orta — eski manuel tutar girişlerinin doğrulaması zayıf.** `parseTR` parseFloat kullanıyor; sayıyla başlayan bozuk metni kısmen okuyabilir. Manuel yeni işlem/EditModal kontrolleri negatif değerleri açıkça yasaklamıyor. Yeni Toplam POS yolu daha sıkı doğrulanıyor; eski yollar da aynı güvenli tutar/tarih denetimine bağlanmalı. Bu raporda eski manuel ekranların tüm hata senaryoları çalıştırılmadı.
5. **Orta — sıfır vade değeri bazı eski alanlarda varsayılan vadeye düşebilir.** `||21` / `||16` kullanımı 0 günü yok sayar; yeni toplam modundaki `??` ile davranış farklı olabilir. 0 günlük vade iş kuralı netleştirilip tüm hesaplarda tutarlı uygulanmalı.
6. **Düşük/orta — işlem geçmişi en fazla ilk 500 satırı gösteriyor, başlık tüm kayıt sayısını yazıyor.** Filtre/sayfalama veya görünür kayıt sınırı bilgisi gerekir. Eski kayıtlar depoda bulunurken ekranda yokmuş gibi anlaşılabilir.
7. **Orta — Electron webSecurity kapalı.** `main.js` içinde `webSecurity:false`; uygulama yerel dosya kullansa da bu geniş izin gereksinimi yeniden değerlendirilmelidir. Gerçek Electron güvenlik testi yapılmadı; gerekçe olmadan kapatıp çalışan dosya akışını bozmadım.

## Yazı, renk ve kullanım değerlendirmesi

- IBM Plex Mono gömülü; çevrimdışı çalışma açısından olumlu. Tutar ve kodlarda uygun, uzun açıklamalarda yoğun görünüm oluşturuyor. Bütün yazı tipini değiştirmek yerine gövde metni ile sayısal alanları ayırmayı öneririm; kullanıcı onayı gerekir.
- Çok sayıda yardımcı metin 10–11 piksel; bazı etiketlerde #475569 ve #64748b koyu zemin üzerinde soluk kalıyor. İstenen yeni kasa düzenleyicisindeki kilitli alanlar okunaklı tutuldu; program geneline toplu renk/font değişikliği yapılmadı.
- Emoji simgeler Windows sürümüne göre farklı görünür ve hizalanabilir. Tutarlı SVG simge takımı daha öngörülebilir olur.
- Body genelinde zoom:1.10 var. Windows ekran ölçeği ile birleşince bazı alanlar beklenenden büyük görünebilir. Native pencere minWidth 960; küçük ekran/tablet uyumluluğu garanti değil.
- Modal pencerelerde klavye odağı, Escape ile kapatma ve ekran okuyucu etiketleri program genelinde standartlaştırılabilir. Kapsamlı erişilebilirlik testi yapılmadı.

## Korunacaklar ve önerilen sıra

1. Önce yedek doğrulama/yazma güvenliği; kullanıcı verisine dokunmadan sentetik testler.
2. Manuel işlem tutarı/tarihleri ve 0 günlük vade tutarlılığı.
3. Yardımcı yazı kontrastı ve okunabilirlik; mevcut tasarımı koruyarak.
4. Geçmiş filtreleme/sayfalama, klavye ve simge tutarlılığı.
5. Electron güvenliği ve gerçek Windows testleri.

Bu ek maddelerin hiçbiri bu teslimde düzeltilmedi. Onaydan sonra her biri ayrı doğrulamayla uygulanmalıdır. Mevcut kullanıcı yedeklerini otomatik düzeltme veya geçmiş kayıtları yeniden hesaplama önerilmez.
