# Build v31 — Yeni kayıt ile düzenleme birbirinden ayrıldı

## Yeni POS toplamı
Kasayı, POS gününü ve toplamı girip **Toplamı kaydet** kullanın. Başarılı kayıttan sonra tutar temizlenir; tarih düne döner. Bu form hiçbir eski kaydı güncellemez. Aynı kasa ve gün için tekrar kayıt varsa önceki kayıt korunur; ayrı bir kayıt eklemek için onay istenir.

## Yeni yatan debit
Kasa, bankaya yatış günü (varsayılan bugün) ve tutarı girip **Yatan debiti kaydet** kullanın. Program Ayarlar'daki debit vadesinden ilgili satışı bulur; belirsiz eşleşmede açık seçim ister. Başarılı kayıtta yeni debit tutarı temizlenir. Kalan kredi kendi kredi vadesinde Takip ekranındaki mevcut kartlarda görünür.

## Düzenleme ve silme
Ayrı kayıt silme bölümü kaldırıldı. **İşlem Geçmişi** satırındaki kalem simgesi düzenlemeyi, çarpı simgesi silme onayını açar.

- Debit satırını silmek yalnız o debiti kaldırır; tutarı bağlı POS kaydının kalan kredisine geri eklenir.
- POS/kredi satırını silmek, bu POS toplamı ve ona bağlı bütün debitleri kaldırır. Onayda bu etki açıkça yazılır.
- Bağımsız eski manuel işlem silinirse yalnız o işlem kaldırılır.
- Kalem simgesiyle açılan düzenleme penceresi yeni toplam formundan ayrıdır. Toplam ve debit değişiklikleri kendi Kaydet düğmeleriyle kaydedilir. Yeni debit eklemek için ana sayfadaki yeni giriş kullanılmalıdır.

Hatalı tutar, yazma hatası veya iptalde kayıtlar değiştirilmez; yeni giriş temizlenmez.

Tam kaynak paketidir; hazır EXE değildir. Windows'ta `pos-app/BUILD.bat` ile derleyin. Sürüm 1.0.9; uygulama kimliği `com.poskasayonetim.app` aynı. Önce JSON yedek alın; kullanıcı veri klasörünü temizlemeyin.
