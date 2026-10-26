# Build v33 — POS analizi ve aylık ödemeler

## POS gelir analizi
Takip sayfasında, Tarih Aralığı Toplamı'nın üzerinde bulunur. **Bu hafta, Geçen hafta, Bu ay, Geçen ay, Bu yıl, Özel aralık** seçeneklerini kullanın. İki tarih seçip Uygula'ya basabilirsiniz.

Grafiğin ince çubukları ve yanındaki liste, seçilen aralıkta haftanın günlerine göre günlük ortalamayı gösterir. Sağ üstte seçilen aralığın POS toplamı ve günlük ortalaması yer alır. Yazı tipi ve boyutları mevcut sayfayla uyumludur.

Haftalık kısa yorum, bugün dahil son 7 günü önceki 7 günle karşılaştırır. Örneğin 26.10.2026 için 20.10–26.10 ve 13.10–19.10. Aylık POS yorumu 01.10–26.10 ile 01.09–26.09 aralıklarını karşılaştırır. Geçen ayın daha kısa olduğu durumda son geçerli günü kullanılır. Geçen ay filtresi ise önceki ayın tamamını açar.

POS banka başlığının en sağındaki tekrar eden toplam kaldırılmıştır. En üstteki ana yatış toplamı ve kasa kartları korunmuştur.

## Aylık ödeme takibi
Ödemeler son ödeme tarihlerinin ayına göre açılır gruplardadır. Başlangıçta yalnız mevcut ay açıktır; diğer ayları ayrı ayrı açıp kapatabilirsiniz. Ödeme ekleme, düzenleme, tarih değiştirme, ödendi/geri alma ve silme işlemleri korunmuştur.

Altta aylar arası gider değişimi, bu yılın en yüksek/en düşük ayı ve aynı adla kaydedilmiş ödemelerde en fazla artan/azalan tutarların yüzdeleri kısa yorumlarla gösterilir. Veri yeterli değilse karşılaştırma uydurulmaz. Ekrana yöntem/açıklama notu eklenmedi.

Tarihler **GG.AA.YYYY** düzenindedir; yeni alanlar da aynı düzeni kullanır. Kayıtlar ve hesaplamaların tarih biçimi değiştirilmedi.

Tam Windows kaynak paketidir; hazır EXE içermez. `pos-app/BUILD.bat` ile derleyin. Sürüm 1.2.0, kimlik `com.poskasayonetim.app` aynı. Önce JSON yedek alın ve çalışan program klasörünü saklayın; kullanıcı verilerini temizlemeyin.
