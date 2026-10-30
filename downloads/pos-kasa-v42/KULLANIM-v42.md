# POS Kasa Yönetim — Build v42 / 1.5.2

## İndirme ve çalıştırma

Bu ZIP değişen ve değişmeyen kaynakları, testleri, simgeleri ve gömülü OCR motorunu içerir; hazır Windows EXE paketi değildir.

1. Güncellemeden önce mevcut uygulamadan JSON yedeği alın. Eski program klasörünü de saklayın.
2. ZIP'i yeni bir klasöre çıkarın. `pos-app/BUILD.bat` dosyasını Windows bilgisayarınızda çalıştırın. Mevcut Node.js/npm derleme gereksinimi devam eder.
3. Oluşan `pos-app/dist/win-unpacked` klasörünün tamamını kullanın. Yalnız EXE'yi taşımayın. `resources/ocr-runtime` klasörü OCR için gereklidir; ek OCR veya Windows dil bileşeni kurulmaz.
4. Programın kimliği `com.poskasayonetim.app`, ürün adı ve veri klasörü seçimi korunmuştur. Uygulama verilerini temizlemeyin, eski uygulamayı kaldırmanız gerekmez.

## Veresiye girişi

Giriş → Kasadan Gelen Nakit içinde mevcut kasa satırlarının altında **VERESİYE** bulunur. Kasa seçilmez. İşlem tarihi varsayılan dündür; tarih gösterimi gün–ay–yıldır. Tutarı örneğin `3053,50` yazın; alan `3.053,50` olarak biçimlenir.

**Veresiyeyi kaydet** yalnız veresiye satırını kaydeder. **Nakdi kaydet** mevcut çoklu kasa nakit girişlerini birlikte kaydetmeye devam eder; veresiye için istemeden kayıt oluşturmaz. Başarılı kayıtta tutar temizlenir, tarih yeniden düne döner. Yeni kayıt eskisini değiştirmez. Boş veya açık sıfır tutar, nakitte olduğu gibi 0 TL olarak kaydedilebilir.

İşlem Geçmişi'nde **NAKİT**, **POS**, **VERESİYE** listeleri ayrı ve başlangıçta kapalıdır. Veresiye listesini açarak kaydı düzenleyebilir veya onayla silebilirsiniz. Arama, tarih filtresi ve 50 kayıtlık sayfalama veresiye için de geçerlidir.

## Özet

- **POS-NAKİT-VERESİYE DAĞILIMI**: POS mavi, nakit yeşil, veresiye sarı. Halka ortasında üç türün toplamı bulunur. Tarih filtreleri korunmuştur. Satış dağılımında işlem/satış günü kullanılır; bankaya yatış günü kullanılmaz.
- **VERESİYE GELİR ANALİZİ**: POS/nakit analizleriyle aynı dönem seçenekleri, özel tarih aralığı, günlere göre ortalama, günlük değerler ve hafta/ay karşılaştırmaları bulunur. Hafta son 7 gündür; önceki hafta ondan önceki 7 gündür. Ay kıyası önceki ayın aynı gün sayısıyla yapılır.
- Veresiye satış dağılımını artırır; tahsil edilmiş nakit veya bankaya yatacak POS sayılmaz. Beklenen POS, kredi/debit vadeleri ve Para planlaması değişmez. Bu sürüm müşteri bazlı alacak/tahsilat sistemi eklemez.
- **Kasa Performansı** iç kaydırma çubuğu kullanmaz. Dörtten fazla kasa varsa iki sütun olur; kasa tutarı adının altında kalır. Önümüzdeki 7 Gün grafiği ve 32 birimlik çubuklar korunur. Ekranın tüm sayfa kaydırması normal şekilde devam eder.

## Yedekleme ve geri dönüş

Veresiye ayrı `pos:veresiye` alanında saklanır. Yeni JSON yedeği bu alanı içerir. Eski veya kısmi yedekte alan yoksa mevcut veresiye korunur; açık `veresiye: []` yalnız onaylı geri yüklemede o listeyi boşaltır. `null`, bozuk tarih/tutar, tekrarlı kimlik ve geçersiz liste reddedilir. Güvenlik kopyası ve geri yükleme günlüğü veresiye alanını da kapsar.

Geri dönmek için v41 kaynak/program klasörünü saklayın. Önce v42 yedeği alın; v41 veresiye ekranlarını ve yeni alanı tanımaz. Geri dönüş sırasında kullanıcı veri klasörünü silmeyin. Yeni yedeği eski sürümde düzenleyip tekrar dışa aktarmak yeni veresiye alanını kaybettirebilir; v42 yedeğinin orijinalini saklayın.

Gerçek Windows bilgisayarınızda ilk güncellemede bir sentetik kayıtla kaydet/açılış/geçmiş/yedek akışını kontrol edin. Bu ortamda Windows x64 derlemesi başarılıdır; gerçek Windows cihazında çalıştırma yapılmamıştır.
