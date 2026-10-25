# Build v29 — POS günü ve debit yatış tarihi

1. Bankaya yatan debitler bölümünde kasayı seçin.
2. **Debitin ait olduğu POS günü** alanından satış gününü seçin. Tarih alanı kayıt olmasa da aktiftir.
3. O gün kayıtlı toplam varsa açılır. Alternatif olarak **Kayıtlı POS toplamı** listesinden bir kayıt seçebilirsiniz. Tek kasa kaydı varsa otomatik açılır.
4. Seçilen günün toplamı yoksa üst bölümde aynı kasa/gün için toplam tutarını girip **Toplamı kaydet** düğmesine basın. Program eksik toplamı tahmin etmez.
5. **+ Yatan debit ekle** düğmesine basın. Satırdaki **Debit tarihi**, paranın bankaya yattığı gündür; POS satış gününden farklı olabilir.
6. Tutarı `3053,50` biçiminde girip **Debitleri kaydet** düğmesine basın.

POS satış tarihi ile bankaya yatış tarihi farklı alanlardır. Tarih veya kayıt değiştirirken kaydedilmemiş değişiklik varsa onay sorulur. Toplam ve debit kayıt düğmeleri ayrı çalışır.

Windows kaynak paketi: `pos-app/BUILD.bat` ile derleyin. ZIP hazır EXE içermez. Önce JSON yedek alın; mevcut kullanıcı veri klasörünü temizlemeyin. Güncel sürüm 1.0.7, kimlik `com.poskasayonetim.app`.
