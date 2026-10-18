# Veri kaynağı alternatifleri — 15 Ekim 2026

Bu rapor aday karşılaştırmasıdır. Aşağıdaki dokuz adrese yapılan erişim denemelerinin tamamı bulut ağ geçidinde `CONNECT 403 Forbidden` ile engellendi. Sağlayıcıların kendi sunucularının uygulamamızı reddettiği, uç noktanın kaldırıldığı veya ücretli olduğu sonucu çıkarılamaz. Güncel ürün kapsamı, fiyat, BIST sembol desteği, API yanıtı ve kullanım lisansı bu oturumda doğrulanamadı. Abonelik satın alınmadı, anahtar kullanılmadı ve veri aktarımı açılmadı.

| Kaynak / aday | Değerlendirmedeki rol | Entegrasyondan önce doğrulanacaklar |
|---|---|---|
| [Investing](https://www.investing.com/webmaster-tools/) | Görsel gösterim/araştırma kaynağı adayı | Kamuya açık, bu kullanım için izinli veri API'si var mı? Widget veya web sayfası görünümü ham veri aktarım hakkı sayılmaz. Güncel API kapsamı doğrulanmadı. |
| [Fintables](https://fintables.com/) | BIST şirket finansalları için görüşülecek aday | Bireysel/kurumsal hesabın API veya dışa aktarma hakkı; tablo dönemleri, para birimi, konsolidasyon ve enflasyon düzeltmesi; otomatik kullanım izni. Açık API mevcut olduğu varsayılmadı. |
| [KAP](https://www.kap.org.tr/tr/) | Türkiye şirketlerinin resmi finansal bildirim kaynağı | Otomatik erişim yöntemi ve koşulları; standart tablo/XBRL kapsamı; şirket-dönem eşleştirme ve düzeltme bildirimleri. Her PDF'den güvenilir oran çıkarılabileceği varsayılmamalı. |
| [Borsa İstanbul veri yayını](https://www.borsaistanbul.com/tr/sayfa/224/veri-yayin) | Lisanslı BIST fiyat/tarihçe sağlayıcı seçimi | Doğrudan erişim veya yetkili dağıtıcı; gecikme, günlük OHLCV, bölünme/temettü düzeltmesi, kişisel kullanım ve yeniden dağıtım hakkı. Bu adresin güncel içeriği doğrulanmadı. |
| [EODHD](https://eodhd.com/financial-apis/stock-market-financial-data-api) | Tarihsel piyasa/finansal veri API adayı | İstenen BIST sembolleri ve mali tablo kapsamı gerçek hesapta test edilmeli; split-adjustment ve kota kontrolü gerekir. BIST finansalları var diye kabul edilmedi. |
| [Twelve Data](https://twelvedata.com/docs) | Çoklu piyasa OHLCV API adayı | BIST erişiminin pakette olup olmadığı, geçmiş derinliği, hacim ve mali tablo kapsamı ayrı doğrulanmalı. |
| [Financial Modeling Prep](https://site.financialmodelingprep.com/developer/docs) | Özellikle ABD verisi için API adayı | BIST şirketleri ve bilanço satırlarının kapsamı ayrıca test edilmeli; ABD desteğinden BIST desteği çıkarılamaz. |
| [CollectAPI hisse listesi](https://api.collectapi.com/economy/hisseSenedi) | Uygulamada isteğe bağlı bağlantı denetimi bulunan aday | Hesabın Economy paketi/kotası; sağlayıcı fiyat zamanı; tarihli günlük OHLCV ve iki/üç dönem mali tablolar için ayrı uç noktalar. Hisse listesi bunları sağlamış sayılmaz. |
| [SEC EDGAR API bilgisi](https://www.sec.gov/search-filings/edgar-application-programming-interfaces) | ABD şirketlerinin resmi mali bildirimleri için aday | CIK eşleştirmesi, dönem/birim ayrıştırma, fair-access şartları. BIST bilançosunun yerine geçmez. |

## Önerilen uygulama sırası

1. Önce seçilecek kaynağın şirket bazında üç örnek yanıtını doğrulamak: THYAO, EREGL ve bir banka. USD/TRY ve gram türetmesi de ayrı kontrol edilir. Bankalarda sanayi şirketi borç/FAVÖK modeli kullanılmaz.
2. Fiyat geçmişinde en az250 geçerli günlük OHLCV, tarih/zaman, para birimi ve kurumsal aksiyon bilgisi aranır. Yeni halka arzda bu kadar veri olmayabilir; puan uydurulmaz.
3. F-Score için karşılaştırılabilir yıllık dönemler, ROA/aktif devir hesabında ortalama aktifler için gerekirse üçüncü dönem; nakit akışı, uzun vadeli borç, cari oran, pay ihracı, brüt marj gerekir. Eksik veriye sıfır puan verilmez. Sonradan düzeltilmiş tablolar sürümlenir; AI bilgi boşluğu doldurmaz.
4. Belgelenmiş erişim ve örnek yanıt doğrulandıktan sonra kaynak adaptörü eklenir. Anahtar yalnız o sağlayıcıya gönderilir; kotaya göre istek sınırı ve önbellek kurulur. Kaynaklar farklı para birimi/dönem taşıyorsa karıştırılmaz.
5. Skorlar geçmiş veriyle ileriye bakmadan sınanır. Yönsel denge puanı başarı ihtimali veya kişiye özel alım emri sayılmaz.

Anahtarsız, doğrulanmış kapsam sağlayan bir kaynak bu ağ ortamında tespit edilemedi. BIST için öncelikli değerlendirme: resmi finansal bildirimler + belgelenmiş/lisansı uygun fiyat dağıtıcısı. CollectAPI hesabı uygun tarihçe ve mali tablo uç noktalarını gerçekten sağlıyorsa mevcut güvenli anahtar girişinden o adaptör genişletilebilir. Şimdiki bağlantı denetimi tam veri entegrasyonu değildir. Web sayfası kazıma veya oturum/erişim engeli aşma uygulanmadı.
