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

## 1.2 ayrıntılı widget

280 × 430 dp önerilen alan; 250 × 300 dp minimum yeniden boyut. Hissedilen, rüzgâr ve nem üç sütunda; altında üç günlük tarih/durum/yüksek-düşük tablosu vardır. Altı gelecek güne iki sayfada erişilir. Sonraki düğmesi ayrı eylemdir; diğer bilgi alanları uygulamayı açar. 360 dp altı yükseklikte tablo yerine büyütme yönlendirmesi gösterilir. Günlük satırlar sabit 36 dp’dir; katmanlı ağırlıklarla tekrar ölçümden kaçınılır. Eksik ve eski veri kuralları korunur.

## 1.2.1 okunabilirlik

Şehir 18 sp, tarih ve gün bilgisi 13 sp, günlük yüksek/düşük 14 sp kalın, ölçüm değerleri etiketlerden büyük 16 sp kalın. Metinler #F1F6FF / #D2E1F8 ile koyu zeminden ayrılır. Gün satırları 48 dp. Güncel öneri 300 × 500 dp, minimum boyutlandırma 280 × 340 dp; günlük tablo eşiği 480 dp. Önceki sürümdeki daha küçük boyut önerisinin yerini alır. Ölçüm değerlerinin göreli boyut span’i Android yazı ölçeğini izler; sistem ayarları değiştirilmez.

## 1.2.2 kart yüksekliği

Aç/Yenile/Şehir alt satırı yoktur. Saydam FrameLayout başlatıcının ayırdığı alanı tutar; içteki renkli LinearLayout wrap_content yüksekliğindedir. Günlük satır grubunda layout_weight kaldırılmıştır. Kart bu yüzden son satır + 14 dp padding ile biter, yeniden boyutlandırmada esneyip altta renkli boşluk üretmez. Dış kapsayıcı bu farklı yükseklikleri korumak için gereklidir; UselessParent önerisi yalnız bu düğümde gerekçeli bastırılır. Güncel öneri 300 × 430 dp, tablo eşiği 420 dp. Uzun basarak yeniden yapılandırma özelliği API 28+ xml-v28 kaynağında tanımlıdır.
