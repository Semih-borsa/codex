# HavaPusulası mimarisi

## İlk sürümün katmanları

`MainActivity` ekranları ve açık kullanıcı işlemlerini yönetir. Ağ istekleri tek worker üzerinde yapılır; ekran ana iş parçacığını bekletmez. Saatlik görünüm 24, şehir sayısı 12 ile sınırlıdır. Sekmeler arası gezinme veya şehir ekleme/silme banka/NOVA verilerine erişmez.

`WeatherApi` yalnızca sabit HTTPS uç noktalarıyla haberleşir. Yönlendirme takip edilmez. Bağlantı 10, okuma 15 saniye; yanıt en fazla 1 MiB. Şehir/koordinat, zaman dilimi, zaman sıralaması, dizi uzunluğu, değer aralığı ve ölçü birimleri doğrulanır. API 429 sınırını hata olarak iletir; otomatik hızlı tekrar döngüsü yoktur.

`Forecast` değişmez bir modeldir. Unix zamanları şehrin IANA zaman diliminde gösterilir; günlük alarm telefonun zaman dilimindedir. Yaz saati çakışmaları ve boşlukları test edilir. Eksik değer sıfır değildir. Bu saatin kaydı yoksa gelecekteki bir saat “şimdi” diye gösterilmez.

`WeatherStore`: SQLite, WAL, foreign key; şema 1:

```sql
CREATE TABLE cities (
 id INTEGER PRIMARY KEY, name TEXT NOT NULL, region TEXT NOT NULL,
 country TEXT NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL,
 zone TEXT NOT NULL, UNIQUE(lat,lon)
);
CREATE TABLE forecasts (
 city_id INTEGER PRIMARY KEY REFERENCES cities(id) ON DELETE CASCADE,
 raw TEXT NOT NULL, downloaded INTEGER NOT NULL
);
```

Her şehrin yalnızca son başarılı yanıtı tutulur; kayıtlar sınırsız büyümez. Yeni yanıt doğrulanmadan önbelleğe yazılmaz. Şehir silme UI'daki ayrı onaya bağlıdır. Yeni şemada otomatik `DROP TABLE` yoktur; sürüm yükseltmesi veri koruyan migration ile ayrıca tasarlanmalıdır.

`DailyReminder`: tek günlük AlarmManager alarmı. Kesin zaman izni varsa `setExactAndAllowWhileIdle`, yoksa `setAndAllowWhileIdle`. `BOOT_COMPLETED`, saat ve saat dilimi değişiklikleri gelecek alarmı kurar. Broadcast içinde ağ/TTS/ağır iş yapılmaz; yerel sınırlı önbellekten bildirim hazırlanır. Aynı günlük alarmın tekrarı kalıcı gün işaretiyle elenir. Varsayılan kapalı; geç kalan alarm en fazla 6 saat içinde değerlendirilir, kapalı telefon için sonradan uyarı sözü verilmez.

`RefreshJob`: kullanıcı açarsa veya widget kurulu olup açıkça kapatma tercihi yoksa JobScheduler ile 30/60/180 dakikalık dönem. Esneklik aralıkları Android'in minimum 5 dakika kuralına uyar. Varsayılan ölçülmeyen ağ; kullanıcı ANY/mobil veri seçebilir. Aralık veya ağ koşulu değişince mevcut iş değiştirilir. Widget şehirleri ve uygulamanın seçili şehri öncelikli, diğer kayıtlı şehirler ardından yenilenir; 12 şehir sınırı ve iki ardışık hatada kesme vardır. En fazla iki worker çalışır; her Android işi için ayrı iptal durumu tutulur. Durdurulan bir iş diğer widget'ın işini iptal etmez. Bildirim tazeliği bu işe bağımlı değildir; kayıt yaşı her durumda gösterilir.

`WeatherWidget`: Android `AppWidgetProvider` + `RemoteViews`; başlatıcının ana ekranında, uygulama açılmadan çalışır. Widget başına şehir kimliği veya 0=uygulamada seçili şehir tercihi saklanır. Son cache ve güncellik ayrı gösterilir. `updatePeriodMillis=1800000` yalnızca yerel ekran yenilemesidir; internet indirme politikası ayrı ve açık onaya bağlıdır. `Yenile` dokunuşu widget kimliği doğrulandıktan sonra bağımsız tek seferlik JobScheduler işi kurar; aynı widget'ın bekleyen isteği çoğaltılmaz. Bu iş mevcut ağı kullanabilir. Silinmiş/başkasına ait widget kimliğiyle işlem başlatılmaz.

`WidgetConfigureActivity` ana ekran başlatıcısından açılabilmek için dışarıya açıktır; verilen kimliğin bu uygulamanın gerçek widget'ına ait olduğunu doğrular. Özel `WIDGET_REFRESH` alıcısı dışarıya kapalı ve tıklama PendingIntent'leri immutable'dır. Widget kaldırma, yalnızca o widget'ın tercihini ve kendi indirme işini temizler; hava veritabanını silmez. Yeni cache yazılınca tüm ilgili görünüm güncellenir; başarısızlık eski cache'i değiştirmez. Widget'taki eski hata metni daha yeni başarılı indirme sonrasında görünmez.

## İzinler

| İzin | Amaç |
|---|---|
| INTERNET | Açık şehir/tahmin isteği ve isteğe bağlı arka plan işi |
| ACCESS_NETWORK_STATE | Android ağ kısıtlı JobScheduler işi |
| POST_NOTIFICATIONS | Android 13+ bildirim; kullanıcı verir |
| SCHEDULE_EXACT_ALARM | Android 12+ özel izin; kullanıcı seçer, yaklaşık alternatif vardır |
| RECEIVE_BOOT_COMPLETED | Yeniden başlatma sonrası gelecek alarm/iş planı |

JobService yalnızca Android'in `BIND_JOB_SERVICE` izniyle çağrılabilir. Alarm alıcısı dışarıya kapalıdır. API anahtarı, SMS, rehber, GPS, ödeme veya sürekli dinleme aracı yoktur. Kritik sistem ayarı uygulama tarafından değiştirilmez.

## Doğruluk ekranının sonraki sürüm altyapısı

1. `forecast_runs(id,city_id,downloaded_at,provider,model,raw_hash)` ve `predictions(run_id,valid_at,lead_hours,temp_c,rain_probability)` ile indirilmiş tahmini gelecekteki hedef saatten **önce** sabitle. `downloaded_at` modelin yayımlanma zamanı değildir; sağlayıcı yayın zamanı yoksa bu farkı açıkça göster.
2. Lisansı ve erişimi doğrulanmış gözlem kaynağı seç. İstasyon kimliği, koordinatı, şehir uzaklığı/yükseklik farkı, ölçüm zamanı ve kalite bayraklarını sakla. Yeniden analiz verisini yerinde termometre ölçümü gibi adlandırma.
3. Hedef saat geçtikten sonra gözlemi UTC anına eşleştir. Eksik/kalitesiz gözlemi yanlış tahmin sayma; kapsama oranını ayrıca göster. Sonradan güncellenmiş tahmin geçmişteki tahminin üzerine yazılmasın.
4. Sıcaklık için MAE ve yanlılık; yağış olasılığı için yeterli örnekte Brier skoru ve kalibrasyon. “% doğruluk” gibi tanımsız tek sayı kullanma. 6/24/48 saat öngörü aralıklarını ayrı raporla.
5. En az 30 gün ve yeterli örnek olmadan güvenilirlik sıralaması verme. Örnek sayısı, tarih aralığı, kapsama ve referans kaynak görünür olsun. İkinci sağlayıcı eklenmeden maliyet/koşullar yeniden kontrol edilsin.

Bu tablolar ve puanlar **v1.0'da çalışıyor diye sunulmaz**; sonraki sürüm planıdır. İlk sürümün kişisel/anahtarsız koşulları korunur.

1.0.2: `Forecast.lastUpdated` son başarılı indirme anını telefon saat diliminde HH:mm gösterir; farklı günde tarih eklenir. Widget güncellemesi bu anı değiştirmez. `RefreshJob.enabled` widget varlığını varsayılan olarak kullanır; açıkça kaydedilmiş kapatma tercihini korur. Android durdurmalarında geçerli iş yeniden denenebilir.

1.2: `Forecast.futureDays` şehir saat dilimindeki bugünü/geçmişi dışlar, en fazla altı gün döndürür. Widget üç satırlık iki sayfa kullanır; sayfa tercihi widget kimliğine bağlıdır. NEXT özel broadcast’i yalnızca bu uygulamaya ait geçerli widget kimliğini kabul eder. Sayfa değiştirmek ağ çağrısı yapmaz; widget silinince sayfa tercihi de temizlenir. Yeni servis/izin veya bağımlılık eklenmez.
