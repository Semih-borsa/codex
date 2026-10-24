# Portföyüm 4.8.1 — düzeltme ve teslim raporu

## Kimlik ve güncelleme

- ApplicationId / namespace: `com.semih.portfoyum` — değişmedi.
- versionName: `4.8.1`; versionCode: `40801` (önceki kaynak: 4.8.0 / 40800).
- WebView adresi ve kayıt anahtarları (`portfoy_v4`, eski `portfoy_v3`, `portfoy_theme`) değişmedi.
- Yeni bağımlılık, veritabanı, finans motoru eklenmedi. SchemaVersion 4 korundu; yeni kazanç arşivi alanları isteğe bağlıdır.
- Bu ZIP kaynak projedir; bir APK kurulum dosyası değildir. Bilgisayara özgü SDK yolu, derleme çıktıları, anahtarlar ve kullanıcı verileri pakete eklenmedi.

## Yapılan düzeltmeler

1. Koyu moddaki portföy kartı dönem kutularının beyaz zemini kaldırıldı; koyu yüzey, sınır ve okunaklı açıklama renkleri uygulandı. Açık mod zeminleri korundu. İlgili yardımcı yazılar ve klavye odağı iyileştirildi.
2. Dönemsel K/Z artık toplam değer farkını kazanç olarak göstermiyor. Yeni günlük arşiv kayıtlarında portföyün toplam net kazancı ayrı saklanıyor; karşılaştırılabilir iki kazanç arasındaki fark kullanılıyor. Nakit yatırma, çekme ve portföyler arası TL aktarımı kazanç sayılmıyor. Açık ve gerçekleşmiş kazanç mevcut `totals` hesabından alınıyor; satış iki kez sayılmıyor.
3. Karşılaştırma için tam ilgili yerel takvim gününün kaydı aranıyor. 30 günlük eski kayıt günlük K/Z yerine kullanılmıyor. Eski arşivde kazanç bileşeni yoksa veya fiyat/maliyet eksikse tutar tahmin edilmiyor; `Getiri için veri eksik` / `Geçmiş oluşuyor` gösteriliyor. Eski arşiv silinmiyor veya geriye dönük doldurulmuyor.
4. Gösterilen dönemsel yüzde, kazanç farkının başlangıç net değerine oranıdır. TWR/XIRR değildir; ayarlarda açıklanır. Geçmişe maliyet düzeltme/silme/geri alma sonrası karşılaştırma işaretçisi değişir; uyumsuz eski kazanç kaydı kesin getiri gibi gösterilmez. Değer grafikleri ve toplam net değer korunur.
5. Kapalı kayıtlar, kimlikler, portföy bağlantıları, tutarlar, kapanış tarihleri, geri alma nesneleri ve değer/kazanç arşivi doğrulanıyor. Bozuk kayıtlar depolamaya geçmeden reddediliyor; portföy toplamı ve grafikler de önceden hesaplanarak kontrol ediliyor.
6. Ana kayıt okunamazsa boş bir normal yedek oluşturulması engelleniyor. Ayarlardaki ayrı `Ham kurtarma kopyasını kaydet` seçeneği orijinal metni değiştirmeden TXT olarak koruyor; bu dosya geçerli JSON yedeği gibi sunulmuyor.
7. Geri yükleme öncesi zorunlu güvenlik kopyası korunuyor. İptal/yazma hatasında eski kayıtlar korunuyor. Dosya seçimi sırasında değişen kayıt üzerine yükleme durduruluyor; bekleyen fiyat yanıtları eski işlem kuşağında bırakılıyor. Önizlemede eklenecek/değişecek/silinecek portföy, varlık ve kapanış sayıları gösteriliyor. Fiyatlar, işlem defterleri ve grafiklerin de yedekten değişeceği belirtiliyor.
8. Satışın maliyet dayanağı olan alışlar sonradan değiştirilemiyor/silinemiyor. Düzeltmek için ilgili satışların en yeniden başlayarak geri alınması isteniyor. Satış sonrası tarihli yeni alış ve not düzenleme korunuyor. Eski kayıtlar otomatik yeniden hesaplanmıyor.
9. Gelecek tarihli gerçek alış/kapanış yedekleri reddediliyor; bugünkü portföye sessizce katılmıyor. Hatalı yerel kayıt açılışta da boş veriyle ezilmiyor.

## Değişen dosyalar

- `app/src/main/assets/core.mjs`: kazanç arşivi, dönem filtresi, yedek doğrulama, maliyet koruması, kazanç karşılaştırma işaretçileri.
- `app/src/main/assets/app.mjs`: güvenli yedek/geri yükleme ve kurtarma akışı, doğru dönemsel K/Z gösterimi, sürüm yazısı.
- `app/src/main/assets/modern.css`: koyu mod kutuları, ilgili yardımcı yazılar ve odak görünümü.
- `app/build.gradle`: yalnız sürüm artırımı; paket kimliği aynı.
- `tests/ui.test.mjs`: mevcut testlere ek regresyon kontrolleri.
- Yeni `tests/safety-regressions.test.mjs`: hesap ve veri güvenliği regresyonları.
- Yeni `GUNCELLEME-4.8.1.md`: bu rapor.

Diğer bütün orijinal dosyalar ZIP içinde değiştirilmeden bulunur. Orijinal kaynak ayrı geri dönüş kopyasında korunmuştur.

## Testler

- Başlangıç: mevcut 68/68 JavaScript testi başarılı.
- Sonuç: 121/121 JavaScript testi başarılı (68 mevcut + 53 yeni); silinen, atlanan veya gevşetilen mevcut test yok.
- Native ağ/dosya politikası: 50/50 kontrol başarılı.
- Gerçek Chromium: 390×844 mobil ölçüde 6 ekran × 2 tema = 12 görünüm; JavaScript hatası ve yatay taşma yok. Koyu mod dönem zeminlerinin uygulandığı hesaplanmış CSS ile doğrulandı.
- Android: `:app:assembleDebug` JDK 17, Android SDK 35, AGP 8.9.2 ve mevcut Gradle 8.14.3 ile başarılı. Teslimdeki orijinal wrapper sürümü 8.11.1 değiştirilmedi; bu wrapper ile ayrı derleme doğrulanmış sayılmamalıdır.
- Testler sentetik verilerle yapıldı. Gerçek Android cihazda dokunma/klavye, sistem dosya seçicisi, iptal/yazma hatası, yeniden açma ve fiyat servisleri ayrıca doğrulanmalıdır. Canlı fiyat doğruluğu bu sürümde bağımsız doğrulanmadı.

## Güvenli kurulum

1. Mevcut telefon uygulamasından JSON yedeği alın. Dosyayı uygulama dışında ve tercihen ikinci bir konumda saklayın. Kayıt okunmuyorsa ham kaydı koruyun; boş yedeği gerçek yedek sanmayın.
2. ZIP'i ayrı bir klasöre çıkarın. Android Studio'da `Portfoyum-Android-Studio/Portfoyum-Android` klasörünü açın. Eski projenizin ve imza anahtarınızın kopyasını koruyun.
3. Önceki APK'yı imzalayan aynı anahtarı kullanın. Aynı bilgisayarda önceki debug anahtarı korunuyorsa orijinal `APK-OLUSTUR.bat` aynı anahtarla debug APK üretir; release kullanıyorsanız kendi mevcut release imza ayarınızla derleyin. Gizli anahtarı repoya yüklemeyin.
4. Paket adı aynıdır ve kaynak sürüm kodu artırılmıştır; ancak önceki kurulu APK'nın sertifikası arşivde yoktur. Bu nedenle yalnız paket adı üzerinden üzerine kurulum garantisi verilemez. Telefonda daha yüksek bir sürüm kodu varsa onu da kontrol edin.
5. İmza uyuşmazlığında mevcut uygulamayı kaldırmayın ve verisini temizlemeyin. Önce eski imza anahtarıyla yeni APK üretin.
6. Güncelleme sonrası portföy/varlık sayıları, toplam değer, satış geçmişi ve mevduat hareketlerini yedekle karşılaştırın.

## Android kontrol listesi

- Açık/koyu modda özet ve portföy içi dönem kutularını kontrol edin; koyu modda beyaz kutu olmamalı.
- Yeni fiyat geçmişi oluşana kadar eski günlük K/Z'nin boş/veri eksik görünmesinin bilinçli olduğunu kontrol edin; eski kazanç uydurulmaz.
- Nakit ekleyip çıkarın: toplam değer değişmeli, bu para kazanç olarak görünmemeli. Gerçek veri yerine ayrı sentetik portföy veya yedek kopyasında deneyin.
- Kısmi satış yapın, alış fiyatını değiştirmeyi deneyin: işlem engellenmeli. Satışı geri alınca düzenleme yapılabilmeli.
- Yedek alın, farklı dosya adına kaydedin, geri yükleme önizlemesini inceleyin. Ön yedeği kaydetmeyi iptal edince mevcut kayıtlar değişmemeli.
- Bozuk JSON ve bozuk kapanış kaydı yüklemeyi deneyin: anlaşılır hata çıkmalı, mevcut kayıtlar korunmalı.
- Uygulamayı tamamen kapatıp açın; portföyler ve tema korunmalı.

## Geri dönüş ve sınırlar

Orijinal 4.8.0 kaynak arşivi geri dönüş noktasıdır. Kaynak kodu geri almak verileri geri yüklemekle aynı işlem değildir. Eski kaynakla yeniden APK üretilecekse versionCode kurulu sürümden daha yüksek seçilmeli ve aynı anahtarla imzalanmalıdır; eski APK'yı kaldırarak düşürme önerilmez. Veri geri dönüşü için kurulumdan önce alınan doğrulanmış JSON yedeğini kullanın; geri yükleme öncesi yeni kayıtları da ayrıca yedekleyin.

Enflasyon/dolar/altın kıyası bu düzeltme paketine eklenmedi. Doğru bir sonraki adım, alış tarihleri ve nakit hareketleriyle uyumlu doğrulanmış tarihsel TÜFE/kur/altın verisini değerlendirmektir; bugünkü fiyat geçmişe uygulanmamalıdır. Büyük veri performansı ve canlı kaynakların güvenilirliği ölçüm gerektiren devam konularıdır; doğrulanmamış riskler düzeltilmiş gibi sunulmamıştır.
