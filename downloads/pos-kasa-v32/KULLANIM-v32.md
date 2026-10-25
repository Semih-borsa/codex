# Build v32 — Haftalık yatış karşılaştırması ve tarih güncelleme

## Takip
Seçilen tarih, tam **7 gün önceki aynı günle** karşılaştırılır. En üstte toplam yatış, kredi kartı ve debit ayrı yüzdeler gösterir. Banka toplamında ve kasa kartlarında da karşılaştırma vardır.

- Artış: yeşil yukarı ok.
- Azalış: kırmızı aşağı ok.
- Değişim yok: gri %0.
- Önceki haftada o tür kayıt yoksa: gri “–”. Önceki tutar 0 TL ise yüzde tanımsız olduğundan yine “–”; üzerine gelince açıklama görünür. Seçili günün kaydı yoksa da düşüş varsayılmaz.

Örnek: kredi 1000 → 1500 TL **%50 artış**, debit 500 → 250 TL **%50 azalış**; toplam 1500 → 1750 TL **%16,7 artış**.

## İşlem Geçmişi → Güncelle
- Ayrı girilen kredi/debit işleminin POS günü değişince, yatış günü Ayarlar'daki ilgili kredi/debit vadesine göre güncellenir. Değişiklik yalnız Güncelle ile kaydedilir.
- Toplam POS kaydının POS günü değişince kredi vadesi otomatik güncellenir. Seçiminiz doğrultusunda **bankaya gerçekten yatan debitin tarihi korunur**.
- POS gününü gerçekleşmiş debit tarihinden sonraya taşımak tutarsızlık yaratırsa kayıt reddedilir; gerçekleşmiş tarih otomatik değiştirilmez.

Yeni toplam girişinin temizlenmesi, yeni debitin otomatik eşleşmesi ve yalnız İşlem Geçmişi üzerinden silme önceki sürümdeki gibi korunmuştur.

Tam Windows kaynak paketi; hazır EXE değildir. `pos-app/BUILD.bat` ile derleyin. Sürüm 1.1.0, kimlik `com.poskasayonetim.app` aynı. Önce JSON yedek alın, kullanıcı veri klasörünü temizlemeyin.
