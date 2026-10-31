# Gün Plan 1.2.0 — ayın son günü ve önceki hafta içi

- 209 Java tekrar kontrolü geçti. 2024–2030 arasındaki her ayda gerçek son gün ve hafta sonuysa önceki cuma; 28/29/30/31, artık yıl, cumartesi/pazar, yıl geçişi, başlangıç sınırı ve tam hatırlatma saatinden sonra tekrar ilerlemesi dahil. Eski belirli gün/sonraki pazartesi/günlük/haftalık ve DST testleri korundu.
- 30 TaskStore/Backup/ReminderReceiver durum kontrolü geçti. Ay sonu ve önceki hafta içi alanları kayda/yedeğe gider; birbirine ters hafta sonu ayarları normalize edilir. Eski görevlerde yeni mod kapalıdır. Gerçek Java kodu, Android saklama/teslimat taklitleriyle test edildi; fiziksel alarm teslim testi değildir.
- 12 Playwright arayüz grubu geçti. Ay sonu seçimi, gün alanının kapanması, önceki hafta içi seçimini kaydetme/yeniden açma/kapatma, mod değiştirme ve saat koruma; takvimde şubat/ay/yıl/hafta sonu doğrulandı. Eski arayüz akışları geçti; JS hatası yok. Native köprü taklididir.
- `assembleStandalone` / `lintStandalone` başarılı. APK adı Gün Plan; paket `tr.gunplan.app.v2`, versionCode3/versionName1.2.0. 1.1.0 ile aynı sertifika kullanılır. İmza ve zipalign doğrulaması yapıldı. Fiziksel Android'de yeni sürümün bildirim teslimi bu çalışmada test edilmedi.

---

# Gün Plan 1.1.0 — aylık hafta sonu seçeneği

- 32 saf Java tekrar kontrolü geçti: eski 23 kontrol + cumartesi/pazarın pazartesiye taşınması, ay/yıl geçişi, kısa ay, artık yıl, ayın başında önceki aydan taşınan günü kaçırmama, tam saatte sonraki tekrara geçme ve başlangıç sınırı.
- Gerçek TaskStore / Backup / ReminderReceiver kodunda Android saklama/teslimat taklitleriyle 24 durum kontrolü geçti: yeni alanı saklama, eski kayıtta varsayılan kapalı, aylık dışı kurallarda yok sayma, yedekten koruma ve farklı politikaları tekilleştirmeme dahil. İşletim sistemi alarm teslim testi değildir.
- 10 Playwright arayüz grubu geçti: Gün Plan adı, aylık seçenek kaydı, takvimde ay geçişi ve seçenek kapalıyken eski günler, haftalık gün seçimi, düzenleme/arama, onaylı tamamlama, tema/yedek, mobil boyutlar, gece yarısı başlangıcı; JS hatası yok. Native köprü taklittir.
- `assembleStandalone` / `lintStandalone` başarılı. APK etiketi Gün Plan, paket v2/code2/name1.1.0, APK imza ve zipalign kontrolleri doğrulandı. Lint: 0 errors, 1 warnings.
- Yeni yayın orijinalden ayrı `tr.gunplan.app.v2` paketidir. Orijinal imza anahtarı bu ortamda yoktur. Eski uygulama silinmez; görevler JSON yedeğiyle aktarılır. Bu sürümde fiziksel cihaz bildirimi test edilmedi; eski 1.0.0 emülatör sonuçları aşağıda tarihî kapsamıyla korunur.

---

# GünPlan 1.0.0 — doğrulama

20 Ekim 2026. Bağımsız uygulama: tr.gunplan.app, sürüm kodu 1, Android 8+ / güncel WebView80+.

## Tamamlanan kontroller

- 23 saf Java tekrar testi: tek sefer, günlük, seçili pazartesi / çoklu gün / hafta sonu, ayın 2'si, 29–31 ve kısa ay, artık yıl, yıl geçişi, gelecekte başlayan görev, kaçırılmış tekrar, geçersiz kurallar, yaz/kış saati geçişleri.
- 18 kayıt/alıcı/yedek durum testi: atomik saklama, sonraki tekrarı kurma, erken/eski alarmı reddetme, erteleme, eski bildirimden tamamlamanın gelecek tekrarı atlamaması, çift eylem engelleme, duraklatma/devam, bildirim izni yokken görevi tüketmeme, yedek tekilleştirme/birleştirme, bozuk yedekte ve yazma hatasında veri koruma, silinen görevin bildirim vermemesi. Bu grupta Android saklama/teslimat taklitleri kullanılır; işletim sistemi testiyle karıştırılmamalıdır.
- 9 Playwright arayüz kontrol grubu: temiz ilk açılış, aylık gün2, haftalık gün seçimi doğrulama, arama/düzenleme/not kaçışlama/duraklatma, tamamlama/geçmiş, tema/izin-yedek ayarları, takvim ay geçişi, 320px form ve yatay taşma, gece yarısını aşan varsayılan tarihin geçmişe düşmemesi. JavaScript sayfa hatası yok. Arayüz testlerinde native köprü taklidi kullanıldı; örnek görevler APK verisi değildir.
- Release derlemesi ve Android lint başarılı: 0 hata, paket içi arayüz için JavaScript kullanımına ilişkin 1 uyarı. Uyarı gizlenmedi veya test devre dışı bırakılmadı.
- APK'nın HTML/JS/CSS dosyaları son kaynak dosyalarıyla bayt bazında karşılaştırıldı. Varsayılan kayıt listesi boş; demo görevler yüklenmez.
- Yayın imzası doğrulanır. Sertifika SHA256: 257be4272f40c01a0754ef86f32ea37fc6174239aa5d5043944378dd58b529f4. Yeni paket / yeni anahtar; diğer uygulamalarla aynı veri alanını kullanmaz.

## Uygulama davranışı

Telefonun yerel tarihi/saati kullanılır. Aylık31 tekrarında şubat sonu uygulanır; martta31 korunur. Bildirimden tamamlama önceki gerçekleşmeyi kapatır, gelecek planı atlamaz. Erteleme ayrıca10 dakikalık alarmdır. Yeniden başlatma, saat/saat dilimi değişikliği ve uygulama güncellemesinde alarmlar yeniden kurulur. Bildirim ve kanal kapalıysa kayıt tüketilmez; izin açılıp uygulamaya dönüldüğünde yeniden değerlendirilir.

Telefon kapalıyken, uygulama Android ayarlarından zorla durdurulmuşken veya üretici kısıtlamaları varken teslimat garanti edilemez. Kesin alarm izni yoksa Android'in gecikmeli alarm yöntemi kullanılır ve durum Ayarlar'da gösterilir. Android13+ bildirim ve Android12+ kesin alarm izin akışı kaynakta/API denetiminde kontrol edildi; her üreticinin telefonunda fiziksel test yapılmadı.

Görevler ve geçmiş cihazda kalır; INTERNET izni yoktur. Kullanıcının seçtiği dosyaya açık JSON yedeği aktarılabilir. Yedekten ekleme mevcut kayıtları silmez. Kaynaklara keystore, parola, SDK konumu ve derleme önbellekleri dahil edilmez. Kalıcı anahtar güncellemeler için güvenli tutulmalıdır.

## Gerçek Android emülatörü

Android11 / API30, AOSP WebView83.0.4103.120 üzerinde debug uygulama ve ayrı instrumentation APK'sı kuruldu. Native SharedPreferences okuma/yazma ve yedek tekilleştirmesi geçti. Gerçek AlarmManager alarmı90 saniye sonrasına kuruldu; uygulama Home ile arka plana alındı, arka plan işlemi am kill ile kapatıldı (zorla durdurulmadı). Zamanı geldiğinde NotificationManager kaydında gerçek “GünPlan arka plan testi” bildirimi doğrulandı. AndroidRuntime/GünPlan hata günlüğünde çökme bulunmadı. Bu, üreticilerin tüm pil politikalarının veya Android13+ izin ekranlarının fiziksel cihazda doğrulandığı anlamına gelmez.

Görsel emülatör kontrolünde ilk ekran yakalaması, donanım hızlandırması olmayan ortamın SystemUI yanıt vermiyor uyarısına takıldı. Sistem diyaloğu kapatılıp uygulama yeniden öne getirildi; gerçek GünPlan ekranı görüntülendi. Etkin Activity tr.gunplan.app/.MainActivity olarak doğrulandı, ilgili JavaScript console / uygulama ANR / uygulama çökme kaydı görülmedi. İlk sistem uyarısı bir uygulama testi başarısı olarak sayılmadı.
