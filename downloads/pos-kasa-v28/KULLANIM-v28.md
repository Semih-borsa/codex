# Build v28 — Yatan debit için kasa ve POS kaydı seçimi

1. **Bankaya yatan debitler** bölümünden **Debit için kasa** seçin.
2. Kasada tek POS kaydı varsa otomatik açılır. Birden fazla kayıt varsa **POS işlem günü / kayıt** alanından debitin ait olduğu günü seçin. Listede toplam ve kalan tutar görünür.
3. **+ Yatan debit ekle** ile ödeme tarihini ve tutarını girin.
4. **Debitleri kaydet** düğmesine basın. Kalan kredi seçtiğiniz kasa ve gün için güncellenir.

Kayıtlı toplam olmayan kasada önce üst bölümden toplamı kaydedin. Kasa seçmek tek başına yeni toplam kaydı oluşturmaz. Debit girişi ve kaydetme, bir POS kaydı seçildikten sonra açılır. Üst ve alt kasa/kayıt seçimleri aynı kaydı düzenler; ayrı kasalara farkında olmadan kayıt yapılmaz.

Kaydedilmemiş değişiklik varken alt bölümden kasa veya kayıt değiştirilirse onay sorulur. Vazgeçildiğinde taslak korunur.

Tutarı `3053,50` yazın; gösterim `3.053,50` olur. Toplamı kaydet ve Debitleri kaydet ayrı çalışır.

Bu ZIP bütün kaynak dosyalarını içerir; hazır EXE içermez. Windows'ta `pos-app/BUILD.bat` ile derleyin. Güncellemeden önce JSON yedek alın; kullanıcı verilerini temizlemeyin.
