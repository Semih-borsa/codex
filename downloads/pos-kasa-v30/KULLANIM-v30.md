# Build v30 — Yeni yatan debit girişi

1. Yeni POS toplamında varsayılan satış günü **dündür**. Gün/toplam/vade girip **Toplamı kaydet** kullanın.
2. **Bankaya yatan debitler** bölümündeki yeni girişte **kasa**, **bankaya yatış tarihi** ve **debit tutarı** girin. Varsayılan yatış tarihi **bugündür**.
3. Program Ayarlar'daki **Debit vade günü** ile eşleşen POS satışını bulur. Örneğin vade 16 günse 17 Ocak'ta yatan debit 1 Ocak POS kaydıyla eşleşir. Takvim günü kullanılır; banka tatil/hafta sonu kayması otomatik tahmin edilmez.
4. **Yatan debiti kaydet** düğmesine basın. Yalnız başarılı kayıttan sonra yeni tutar alanı temizlenir ve yatış tarihi bugüne döner. Hata olursa tutar korunur.
5. Kalan kredi, mevcut **kredi vadesinde** Takip ekranındaki aynı banka/kasa kartlarında görünür. Debit ise seçilen gerçek yatış gününde görünür.

Eşleşme yoksa veya birden fazla kayıt varsa otomatik kayıt yapılmaz. **Satış kaydı (gerekirse düzeltin)** alanından doğru kaydı seçebilirsiniz. Eksik POS toplamı tahmin edilmez. Eski debitler silinmez; yeni giriş eklenir.

**Kayıtlı POS / debit düzenleme** alanı önceki kayıtları açıp düzeltmek içindir. Buradaki dolu alanlar yeni giriş değil, kayıtlı debit satırlarıdır; kaydettikten sonra düzenleme amacıyla görünür kalırlar. Yeni debit için üstündeki sade kasa/yatış/tutar girişini kullanın.

Kaydet düğmeleri üstteki manuel kayıt düğmesiyle aynı yeşil tasarımı kullanır.

Tam Windows kaynak paketidir; hazır EXE içermez. `pos-app/BUILD.bat` ile derleyin. Sürüm 1.0.8, uygulama kimliği `com.poskasayonetim.app`. Önce JSON yedek alın; mevcut kullanıcı verilerini temizlemeyin.
