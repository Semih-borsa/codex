# POS Kasa Yönetim — v43 / 1.5.3

Bu tam kaynak ZIP'idir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Güncellemeden önce JSON yedeği alın ve eski program klasörünü saklayın. Üretilen `dist/win-unpacked` klasörünün tamamını kullanın; yalnız EXE'yi taşımayın. Gömülü OCR için ayrıca dil bileşeni kurmanız gerekmez.

## Değişiklikler

- Halka ve günlük ortalama çubukları aynı renkleri kullanır: **POS mavi**, **nakit yeşil**, **veresiye sarı**. Çubukların dikey renk geçişleri ve üst tutar yazıları ilgili renk ailesindedir. Önümüzdeki7 gün POS grafiği de mavidir. Çubuk kalınlıkları ve hesaplamalar değişmedi.
- Veresiye bölümünde yan yana **üç geniş tutar alanı** vardır. Üstteki ortak işlem tarihi üçünü de kapsar; varsayılan dündür. Gün–ay–yıl görünümü korunur.
- Tek **Veresiyeyi kaydet** düğmesi üçünü birlikte kaydeder. Boş alanlar **0 TL** sayılır ve üç ayrı kimlikli kayıt oluşur. Kayıtlar önceki verilerin üzerine yazılmaz.
- Başarılı kayıtta üç tutar temizlenir, tarih düne döner. Alanlardan biri geçersizse hiçbir kayıt eklenmez; düzeltmeniz için girişler korunur. Yazma hatasında da form korunur.
- Geçmişten düzenle dediğinizde yalnız seçtiğiniz kayıt açılır. Güncelleme diğerlerini değiştirmez. Üçlü formdaki kaydedilmemiş taslak, tek kaydı düzenlerken veya vazgeçerken korunur.

Nakit toplu kaydı, POS girişleri, ödeme/debit vadeleri, geçmiş, yedekleme ve OCR önceki gibi çalışır. Veresiye satış dağılımına katılır; tahsil edilmiş nakit ya da bankaya yatacak POS sayılmaz.

## Derleme ve geri dönüş

Uygulama kimliği **com.poskasayonetim.app** ve ürün adı korunmuştur. Uygulamanızı kaldırmayın veya verilerini temizlemeyin. Windows x64 derlemesi başarılıdır; gerçek Windows bilgisayarında çalıştırma bu ortamda yapılmamıştır.

v42 kaynak/program klasörü geri dönüş noktasıdır. Veri biçimi değişmediği için v43'ün ayrı veresiye kayıtları v42 tarafından da okunabilir. Yeni yedeğinizi saklayın; geri dönüş için veri silmeyin. Daha eski ve veresiye alanını tanımayan sürümlerde yedeği yeniden dışa aktarmayın.
