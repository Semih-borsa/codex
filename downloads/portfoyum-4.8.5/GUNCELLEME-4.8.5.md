# Portföyüm 4.8.5 — Grafiğe bağlı değer değişimi kartları

## Değişiklik
Özetin genel kartı ve portföy kartlarındaki günlük/haftalık/aylık alanlar artık maliyete bağlı kâr farkını kullanmıyor. Portföy içindeki grafikle aynı `chartSeries` noktalarının ilk ve son değeri arasındaki tutar/yüzde farkını kullanıyor. Eksik alış maliyeti bu kartları tek başına boş bırakmaz.

Başlıklar **Günlük Değişim / Haftalık Değişim / Aylık Değişim** oldu. Para yatırma/çekme de değeri değiştirdiği için bu rakamlar K/Z diye adlandırılmıyor. Mevcut toplam net kâr/zarar, maliyet, stopaj ve satış hesapları korundu.

- Günlük 24 saat, haftalık 7 gün, aylık 30 günlük grafik penceresini kullanır; pencereler grafikteki son kayıt zamanına göre belirlenir.
- İlgili grafikte iki nokta yoksa çizgi ve “Değer geçmişi eksik” görünür. Gerçek veri olmadan sonuç üretilmez.
- Tam dönem geçmişi oluşmamışsa “Kısmi” etiketi gösterilir. Sıfır başlangıçta tutar farkı gösterilebilir, sonsuz yüzde üretilmez.
- Genel değişim, portföy yüzdelerinin toplamı değildir. Mevcut portföylerin ortak saat dilimlerindeki mutlak değerleri toplanıp toplamın farkı alınır. Son ortak veri eksikse kesin bir genel değişim üretilmez. Hiç kullanılmamış boş portföy, var olan grafiği engellemez.
- Daha önce izlenmiş portföy tamamen boşaldığında, bir sonraki normal grafik kaydı güncel sıfır değerini de saklar. Eski noktalar geriye dönük yazılmaz; hiç kullanılmamış boş portföye varsayımsal geçmiş oluşturulmaz.

## Korunanlar
Uygulama kimliği **com.semih.portfoyum**; sürüm **4.8.5 / 40805**. Açık/koyu temalar, kart renkleri, mevcut grafik seçicisi, fiyat sağlayıcıları, alış/satış/mevduat hesapları, yedek ve depolama şeması korundu. Yeni bağımlılık veya veritabanı eklenmedi. Enflasyon/dolar/altın karşılaştırması eklenmedi.

Gerçek telefon kayıtlarına erişilmedi. Eksik maliyetler doldurulmadı, eski fiyat/kazanç geçmişi dönüştürülmedi. Yeni kart ve grafik sorguları mevcut veriyi salt okunur kullanır.

## Dosyalar
- `app/src/main/assets/core.mjs`: ortak grafik noktalarının kullanımı, genel değer serisi, `periodValueChange`, izlenmiş boş portföyün güncel sıfır noktası.
- `app/src/main/assets/app.mjs`: dönem kartlarının değer farkına bağlanması, doğru başlıklar, kısmi/eksik veri görünümü ve sürüm yazısı.
- `app/build.gradle`: yalnız sürüm yükseltme.
- `tests/value-change.test.mjs`, `tests/value-change-browser.mjs`, `tests/ui.test.mjs`: ilgili sayısal ve ekran kontrolleri.

## Doğrulama
Önceki 4.8.4 kaynakları teslim ZIP'iyle karşılaştırıldı ve geri dönüş kopyası korundu. Başlangıçtaki 132 test geçti. Yeni sürümde **155/155 test başarılı**, atlama veya test silme yok. İki mevcut UI beklentisi, kullanıcının istediği davranışa uygun olarak yeni “Değişim” başlıklarına ve para girişinin değer farkına etkisine güncellendi; gerçek K/Z'nin para girişini kazanç saymama kontrolleri korundu.

Yeni kontroller: eksik maliyetle 1/7/30 günlük grafik-kart eşitliği; genel ortak değer ve ağırlıklı yüzde; boş/tek/kısmi geçmiş; zaman uyuşmazlığı; sıfır taban; eski snapshot uyumu; boşalan portföy; fiyat/maliyet/işlem/geçmiş kayıtlarının korunması.

Gerçek Chromium'da 360 ve 1024 piksel × açık/koyu tema başarılı. Maliyeti eksik portföy kartında +20 TL, genel kartta +30 TL; portföy içindeki günlük grafikte aynı +20 TL doğrulandı. Yatay taşma/JavaScript hatası yok. Finans depolaması gezinme ve grafik seçimi sırasında aynı kaldı.

Android debug derlemesi başarılı: JDK 17 / SDK 35 / AGP 8.9.2, mevcut Gradle 8.14.3 ile çevrimdışı doğrulandı. Projenin orijinal wrapper'ı korunur. APK içindeki JS varlıkları kaynakla karşılaştırıldı. Gerçek Android cihazda son test yapılmadı.

## Kurulum ve geri dönüş
Tam Android Studio kaynak ZIP'idir. Kendi bilgisayarındaki mevcut proje/aynı imza anahtarıyla derleyerek güncelle. Paket kimliğinin aynı olması tek başına imza uyumluluğunu kanıtlamaz; telefondaki eski imza burada doğrulanamadı. Uygulamayı kaldırma veya verilerini temizleme; önce JSON yedeği al.

Önceki kaynak geri dönüşü: `downloads/portfoyum-4.8.4/Portfoyum-Android-Studio-4.8.4.zip`. Veri şeması aynı kaldı; bu sürüm eski kayıtları topluca yeniden hesaplamaz.
