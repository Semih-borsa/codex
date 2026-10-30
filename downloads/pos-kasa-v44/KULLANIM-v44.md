# POS Kasa Yönetim — v44 / 1.5.4

Bu tam kaynak ZIP'idir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Güncellemeden önce JSON yedeği ve eski program klasörünü saklayın. Oluşan `dist/win-unpacked` klasörünün tamamını kullanın; yalnız EXE'yi taşımayın. Gömülü OCR ayrıca dil bileşeni gerektirmez.

## Gün sonu kontrolü

Özet'te bugünkü yatış kartlarının altında **GÜN SONU KONTROLÜ** bulunur. İşlem tarihi varsayılan dündür; istediğiniz geçmiş günü seçebilirsiniz. POS satış/işlem tarihi ve nakit işlem tarihi esas alınır. Bankaya yatış günüyle karıştırılmaz.

Her kasa için POS ve NAKİT durumu ayrı gösterilir:

- Kayıt varsa ilgili mavi/yeşil renkte tutar ve onay işareti.
- Açıkça 0 TL kaydı varsa **✓ 0 TL girildi**.
- Hiç kayıt yoksa gri, kesikli çerçevede **Kayıt yok**.

**Kasa tamam**, o kasanın iki kayıt türünde de giriş bulunduğunu ifade eder; banka tahsilatının eksiksiz gerçekleştiği anlamına gelmez. Üstte tamamlanan kasa ve girilmiş POS/nakit sayıları bulunur. Birden çok kayıt toplanır; kayıt sayısı esas alındığı için sıfır ile eksik ayrılır.

Aktif kasalar listelenir. Seçilen günde kaydı olan pasif kasa da geçmişi kaybetmemek için “Pasif” etiketiyle görünür. Pasif ve kayıtsız kasa eksik olarak listelenmez. Toplam POS'a bağlı debit yatışı ikinci satış veya ikinci giriş sayılmaz; kalan kredi 0 olsa da pozitif POS toplamı korunur.

Şerit yalnızca kayıtları okur. Tarih seçmek sıfır kayıt oluşturmaz, kayıtları düzeltmez ve hiçbir mali tutarı değiştirmez. Giriş/geçmiş üzerinden yaptığınız ekleme veya silme ekrana yansır.

## Satış dağılımı yorumları

POS-NAKİT-VERESİYE DAĞILIMI altında üç kısa yorum gösterilir. Örnek: **Nakit payı geçen haftaya göre 5 puan arttı.** Pay %20'den %25'e çıkmışsa fark5 yüzde puanıdır; toplam satışın veya tahsilatın %5 arttığı anlamına gelmez.

Karşılaştırılan tarih aralıkları yorumların üstünde görünür:

- Bu hafta: son7 gün ile önceki7 gün.
- Geçen hafta: önceki7 gün ile ondan önceki7 gün.
- Bu ay: ayın1'inden bugüne, önceki ayın aynı gününe kadar. Kısa ayda ay sonu kullanılır.
- Geçen ay: tam önceki ay ile ondan önceki tam ay.
- Bu yıl: yıl başından bugüne, önceki yılın aynı tarihine kadar. Artık yıl sınırları korunur.
- Özel aralık: seçilen aralıktan hemen önceki eşit sayıda takvim günü.

Tarih alanlarını değiştirirken mevcut sonuç korunur; **Uygula** ile hem grafik hem karşılaştırma güncellenir. Toplam satış iki dönemden birinde0 veya kayıtsızsa sahte oran üretilmez; karşılaştırılabilir veri bulunmadığı belirtilir. POS, nakit ve veresiye aynı satış dağılımı hesabını kullanır.

## Koruma ve geri dönüş

Kimlik **com.poskasayonetim.app**, veri biçimi, yedekleme, OCR, ödeme/debit vadeleri ve üçlü veresiye girişi korunmuştur. Yeni bağımlılık yoktur. Bu sürüm yeni kayıt türü veya veri dönüştürmesi eklemez.

Windows x64 derlemesi başarılıdır; gerçek Windows cihazında çalıştırılmadı. v43 kaynak/program klasörü geri dönüş noktasıdır. Şema değişmediği için v43 verileri okuyabilir. Geri dönüşte uygulama verilerini silmeyin; güncel JSON yedeğinizi saklayın.
