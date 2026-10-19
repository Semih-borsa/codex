# Test sonuçları — HavaPusulası 1.2.0

## 1.2.0 — ayrıntılı widget ve günlük tahmin tablosu

- Gradle/JDK/SDK önceki sürümle aynı. 30 birim testi, 0 hata. Lint 0 sorun; debug APK ve test APK başarılı.
- Android 11 (API 30) ve Android 15 (API 35) üzerinde güncellemeyle kurulum, **38’er instrumentation kontrolü** geçti.
- Widget gerçek RemoteViews olarak oluşturuldu: hissedilen 21°C, rüzgâr 16 km/sa, nem %55; durum ve yüksek/düşük 24° / 14° alanları işaretli test verileriyle doğrulandı. 280 × 430 dp ölçümünde üç günlük satırlar için okunabilir alan kontrol edildi.
- İkinci sayfa dördüncü gelecek günden başlıyor; şehir saat diliminde bugünü/geçmişi dışlama ve en fazla altı gün sınırı birim testinde doğrulandı. Eksik veride ölçüm/tahmin uydurulmadığı, süresi dolmuş sıcaklığın temizlendiği test edildi.
- Önizleme `ekranlar/widget-expanded-test.png`, testte çizilen görünümün görüntüsüdür; açıkça TEST VERİSİ etiketlidir. TextClock tarih alanı gerçek başlatıcıya bağlanınca sistem tarafından güncellenir. Bu görüntü canlı hava kanıtı değildir.
- Önceki gerçek widget ekleme, gövdeden uygulama açma ve alarm/reboot kontrolleri aşağıda tarihsel kayıt olarak korunur. Bu turda başlatıcıda elle boyutlandırma ve fiziksel Xiaomi testi tekrarlanmadı. Android başlatıcısının eski widget boyutunu koruması nedeniyle kullanıcı uzun basıp büyütmelidir.
- versionCode 5 / versionName 1.2.0, aynı debug imzası. Canlı API erişimi bulut ağ engeli nedeniyle doğrulanamadı; fiziksel cihaz ve büyük yazı ölçeği kontrolü hâlâ yapılmalıdır.

## 1.1.0 kayıtları

## 1.1.0 — arayüz ve widget’tan uygulama açılışı

28 birim testi, debug APK/test APK derlemesi başarılı. Son derlemede lint 0 sorun. Android 11 ve Android 15 emülatörlerinde 33’er instrumentation kontrolü geçti; sonrasında hava çiziminin en-boy oranı ve günlük sıcaklık metninin kaynak üzerinden biçimlenmesi düzeltildi, APK yeniden derlendi. Bu son APK Android 11’e güncellemeyle kuruldu ve 33 kontrol tekrar geçti (`design110-final-android30.log`). Test verisiyle ana tahmin/ölçüm/saatlik kartlar ve gerçek şehirlerle veri bulunmayan görünüm incelendi. Bunlar canlı hava testi değildir.

Android 11 başlatıcısında uygulamada Ankara seçiliyken İstanbul widget’ının tarih/bilgi alanına dokunuldu; uygulamanın İstanbul’a geçtiği doğrulandı. Ayrı Şehir düğmesi hâlâ şehir yapılandırmasını açtı. Açık/koyu tema ve şehir/ayar ekranlarının görsel kontrol kayıtları bu klasörde saklanır.

APK önceki sürümlerle aynı debug anahtarıyla imzalı, versionCode 4 / versionName 1.1.0. Gerçek Xiaomi cihazı, API 26, büyük sistem yazı ölçeği ve fiziksel tablet testi bu turda yapılmadı. Canlı hava erişimi ve Xiaomi pil kısıtlarıyla ilgili önceki sınırlar geçerlidir. Gerçek reboot/çevrimdışı alarm testleri bu tasarım güncellemesinde tekrarlanmadı.

## 1.0.2 kayıtları

## 1.0.2 — yenileme saati ve otomatik widget

- Temurin JDK 21 / Gradle 8.9 / SDK 35 derlemesi, debug APK ve test APK başarılı; lint 0 sorun.
- 28 birim testi, 0 hata. Aynı dakika içinde gerçek HH:mm, zaman ilerlese de indirme saatinin sabit kalması, önceki günün tarihinin eklenmesi ve telefon saat dilimi kapsandı.
- Android 11 ve Android 15 üzerinde güncellemeyle kurulum ve **33’er instrumentation kontrolü** geçti. API 35 bu turda çevrimdışı mod anahtarıyla çalıştırılmadı; önceki çevrimdışı sonuçlar aşağıda korunur.
- RemoteViews gerçek View olarak açılıp sıcaklık ve son başarılı indirme saati doğrulandı. Widget varlığına göre otomatik yenileme varsayılanı ve açıkça kapatma tercihi test edildi.
- Android 11 gerçek kurulumda MainActivity açıldıktan sonra JobScheduler kaydında kalıcı iş 7104, 30 dakika aralık / 5 dakika esneklik ve ölçülmeyen ağ koşulu gözlendi. Emülatörün ağı doğrulanmış bağlantı sağlamadığından iş ağ koşulunu bekliyordu; bu kayıt canlı veri indirme başarısı değildir.
- APK imzası önceki sürümle aynı, versionCode 3. Başarısız güncellemede son başarılı önbellek korunur; widget çizimi indirme tarihini değiştirmez.

Kayıtlar: `test-kayitlari/refresh-clock30.log`, `refresh-clock35.log`. Fiziksel Xiaomi, başarılı canlı API indirmesi ve Android'in tam periyodik teslimi doğrulanmış değildir. Gerçek yeniden başlatma/uzun süreli Doze testi bu turda tekrarlanmadı.

## 1.0.1 kayıtları

1.0.1 doğrulaması: 25 birim testi, lint (0 sorun), debug APK ve test APK derlemesi başarılı. Android 11 üzerinde güncellemeyle kurulum ve 32 instrumentation kontrolü yeniden geçti. APK imzası 1.0.0 ile aynı; versionCode 2. Gerçek Android 11 arayüzünde açık/koyu seçimleri, süreç yeniden başlatılınca koyu modun korunması ve ana ekran widget’ında gün/tarih gösterimi doğrulandı. Tarih telefonun yerel dil ve saat ayarını izler. Aşağıdaki API 35, gerçek reboot ve alarm sonuçları 1.0.0 temel sürümünün kayıtlarıdır; bu küçük arayüz güncellemesinde o aşamalar yeniden çalıştırılmadı.

Doğrulama: 18–19 Ekim 2026. Hazır APK debug imzalıdır; Play Store üretim yayını değildir.

| Kontrol | Sonuç |
|---|---|
| Temurin tam JDK 21.0.12+8, Gradle 8.9, AGP 8.7.3, SDK 35 | Derleme başarılı |
| `testDebugUnitTest` | 25 test, 0 hata |
| `lintDebug` | 0 sorun |
| `assembleDebug`, `assembleDebugAndroidTest` | Başarılı |
| Android 11 / API 30 instrumentation | 32 kontrol geçti |
| Android 15 / API 35 instrumentation, ağ kapalı | 36 kontrol geçti |
| Gerçek AlarmManager tetiklenmesi, Activity kapalı | 5 kontrol geçti |
| API 35 gerçek işletim sistemi yeniden başlatması | 5 kontrol geçti |
| `adb install -r` güncelleme sonrası kalıcılık | 5 kontrol geçti |

Birim testleri saat dilimi/DST, günlük alarm, eksik ve süresi dolmuş veri, öneri eşikleri, iş aralıkları ve şehir önceliğini kapsar. Android kontrolleri ayrı SQLite depolarında önbellek korunması, bozuk yanıt reddi, şehir limiti, izinler, alarm yeniden planlama, ağ koşullarına göre iş değiştirme, widget RemoteViews, geçersiz widget kimliği reddi ve gerçek Activity çizimini kapsar. API 35 çevrimdışı testinde aktif ağ olmadığı ayrıca doğrulanmıştır. Test verisi açıkça TEST VERİSİ olarak işaretlidir ve kullanıcı APK'sında bulunmaz.

Gerçek reboot testi çekirdek boot kimliğinin değiştiğini kontrol etti. Uygulama ekranı açılmadan AlarmManager üzerinde gelecek alarm gözlendi; şehirler, kayıtlı tahmin ve tercihler korundu. Android'in boot yayını yazılım emülatöründe gecikerek teslim edildi. Gerçek zamanlayıcı testinde bildirim, aynı gün ikinci bildirimin engellenmesi ve sonraki alarmın kurulması doğrulandı; önceki tercihler geri yüklendi. Bu ek aşamalar ana 32/36 kontrolden ayrı çalıştırıldı.

Android 11 gerçek arayüzünde şehir ekleme, çevrimdışı hata, silme onayını iptal etme, tema, bildirim tercihleri ve başlatıcının gerçek widget ekleme onayı denendi. Widget ana ekranda kayıt olmayan şehir için sıcaklık uydurmadan `—` ve kayıt bulunmadığı bilgisini gösterdi. Widget üzerindeki Şehir ile İstanbul seçimi, Aç ile ilgili şehir ekranına geçiş ve Yenile ile bağlantı bekleyen tek seferlik iş isteği de doğrulandı.

## Doğrulanamayanlar ve teslim sınırı

- Bu bulutun ağ geçidi Open-Meteo uç noktalarında 403/DNS erişim hatası verdi. Canlı şehir araması ve canlı tahmin indirme başarısı **doğrulanamadı**. Ayrıştırma ve arayüz testleri sabit, işaretli yanıtlarla yapıldı. Telefonda ilk gerçek indirme ayrıca kontrol edilmelidir.
- API 35 yazılım emülatöründe System UI ANR görüldü; HavaPusulası işlevsel testleri geçti fakat o sürümde gerçek başlatıcı/widget GUI testi tamamlanamadı. Ana ekran denemesi API 30'da yapıldı.
- Fiziksel Xiaomi 12T Pro bağlı değildi. OEM pil kısıtları, gerçek cihaz hızı, gece boyunca güncelleme ve tam saatinde bildirim teslimi doğrulanmadı. Desteklenen en düşük API 26'da ayrıca cihaz testi yapılmadı.
- Android arka plan işlerini erteleyebilir. 30 dakika bir tercih aralığıdır; kesin periyodik teslim garantisi değildir. İlk sürümde sesli konuşarak hatırlatma veya tahmin doğruluğu karşılaştırma ekranı yoktur.

## Tekrar çalıştırma

README'deki Gradle ve instrumentation komutlarını kullan. Ağsız test için yalnızca kendine ait test emülatöründe ağı kapatıp `adb -s SERIAL shell am instrument -w -e offline true com.semih.havapusulasi.test/com.semih.havapusulasi.WeatherInstrumentation` çalıştır; sonunda önceki ağ durumunu geri getir. Gerçek alarm testi `-e phase alarm-smoke` ile çalışır ve önceki bildirim tercihlerini geri yükler. Reboot hazırlık aşaması yalnızca boş test kurulumunu kabul eder; kişisel veri olan cihazda çalıştırılmaz.
