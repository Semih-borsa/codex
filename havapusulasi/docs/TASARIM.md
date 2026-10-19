# HavaPusulası 1.1 — tasarım sistemi

Amaç: ilk bakışta şehir, sıcaklık, tahmin türü ve güncelliğin anlaşılması. Sahte canlı veri veya doğruluk puanı eklenmez.

| Rol | Açık | Koyu |
|---|---|---|
| Zemin | #F4F7FC | #0A1220 |
| Kart | #FFFFFF | #142137 |
| Ana metin | #16243B | #F1F5FF |
| İkincil metin | #596B85 | #A9B8CF |
| Vurgu | #245DD8 | #91BDFF |
| Kart sınırı | #E2E8F2 | #27364D |

Ana tahmin kartı ve widget: #1B3C73 → #102037 lacivert geçiş, beyaz sıcaklık, #BACCE8 ikincil metin, #FFD18A güncellik uyarısı. Ana eylem açık mavi düğmedir. Uyarı metni yalnız renge bırakılmaz.

Tipografi: Android sans-serif, başlıklarda medium, büyük sıcaklıkta light. 27 sp ekran başlığı, 24 sp şehir, 76 sp sıcaklık; metin Android yazı ölçeğini izler. Büyük harfli küçük bölüm başlıklarında ölçülü harf aralığı. Kart radius 20–26 dp, kart içi 16–20 dp, ana eylemler en az 48 dp yükseklik.

Tahmin ekranı: şehir seçimi → ana tahmin ve son indirme saati → dört ölçüm kartı → kurala dayalı öneri → yatay saatlik kartlar → 7 gün → kaynak bilgisi. Alt gezinme sabittir; seçili sekme hem renk hem zemin ve erişilebilirlik açıklamasıyla belirtilir. Tablet içeriği 720 dp ile sınırlıdır.

Şehirler ve tercihler aynı kart/düğme/renk sistemini kullanır. Sistem/açık/koyu seçimi korunur. Şehir silme onayı tema ile uyumludur. Ripple dokunma geri bildirimi vardır; sürekli animasyon veya sürekli çalışan ağ servisi eklenmez.

Widget gövdesi, tarih/şehir/sıcaklık ve diğer bilgi alanları aynı şehir kimliğiyle uygulamayı açar. Yenile ve Şehir eylemlerine ayrı PendingIntent atanır. Widget kendi koyu zeminini kullanır; telefon duvar kağıdından bağımsız okunur.

Emülatör ekran görüntüleri gerçek uygulama çıktısıdır; test tahminleri varsa TEST VERİSİ etiketiyle ayrılır. Fiziksel Xiaomi'de ekran/yazı ölçeği ve erişilebilirlik ayrıca doğrulanmalıdır.

## Gerçek emülatör ekranları

[Açık ana ekran](ekranlar/home-light.png) · [Koyu ana ekran](ekranlar/home-dark.png) · [Ayarlar](ekranlar/settings-light.png) · [Şehirler](ekranlar/cities-light.png). Bu ekranlarda henüz tahmin indirilmemiş durum gösterilir; hava verisi uydurulmaz.
