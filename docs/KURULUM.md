# Cep Bütçem v1.8.2 — indirme, kurulum ve ilk ayarlar

## Bilgisayara indirmenin kolay yolu

1. Tarayıcıda [GitHub deposunu](https://github.com/Semih-borsa/codex/tree/main) aç. Giriş istenirse `Semih-borsa` hesabınla giriş yap. Özel depolar oturum açılmadan 404 gösterebilir.
2. Dosya listesinin üstündeki yeşil **Code** düğmesine bas.
3. Açılan menüde **Download ZIP** seç. Tarayıcı `codex-main.zip` dosyasını indirir; genellikle bilgisayarının **İndirilenler / Downloads** klasörüne kaydeder.
4. Tarayıcıda **Ctrl + J** ile indirmeleri açabilirsin. Tamamlanan ZIP dosyasının bulunduğu klasöre git.
5. Windows'ta ZIP'e sağ tıkla → **Tümünü ayıkla / Extract All** → **Ayıkla**. ZIP'in içinden projeyi çalıştırmak yerine ayıklanan klasörü kullan.
6. `codex-main` klasörüne gir. **downloads → CepButcem.apk** telefona kurulacak dosyadır. `settings.gradle`, `gradlew` ve `app` bulunan **codex-main** klasörü Android Studio projesidir.

Yalnızca gereken dosyayı da indirebilirsin:

- [Hazır APK](https://github.com/Semih-borsa/codex/blob/main/downloads/CepButcem.apk)
- [APK + Android Studio projesi, tam paket](https://github.com/Semih-borsa/codex/blob/main/downloads/CepButcem-Tam-Paket.zip)

Bu bağlantılar GitHub dosya sayfasını açar. Sayfadaki aşağı ok simgesine, **Download raw file** veya **View raw** bağlantısına bas. ZIP/APK için önizleme olmaması normaldir. Tarayıcının **Farklı kaydet** menüsü HTML sayfasını kaydedebilir; GitHub'ın dosya indirme düğmesini kullan.

Tam paketi indirdiysen ayıklandığında kökte **CepButcem.apk**, yanında **CepButcem** proje klasörü bulunur. `ONCE-OKU.txt` ilk adımları içerir.

## Telefona APK kur

1. Daha önce kullanmaya başladıysan uygulamada **Ayarlar → JSON yedeğini kaydet** seç ve yedeğini güvenli bir yere kaydet.
2. Telefonu USB ile bilgisayara bağla. Telefonda USB kullanımını **Dosya aktarımı** seç.
3. İndirdiğin `CepButcem.apk` dosyasını telefonun **Download / İndirilenler** klasörüne kopyala.
4. Telefonda **Dosyalar / Dosyalarım** uygulamasını aç → İndirilenler → `CepButcem.apk` dosyasına dokun.
5. Android izin isterse dosyayı açan uygulama için **Bu kaynaktan yüklemeye izin ver** seçeneğini aç, geri dön ve **Yükle / Güncelle** seç.
6. **Aç** düğmesine bas. Android 8.0 veya üzeri gerekir. Bilgisayarda APK dosyasını açman gerekmez; Android telefon kurar.

v1.8 hazır APK, önceki yayımlanan v1.0/v1.1/v1.2/v1.3/v1.4/v1.5 APK ile aynı imzayı kullanır. Güncelleme kurulumu mevcut gelir, gider, hesap ve bütçeleri korur; veritabanı silinmeden yükseltilir. **Uygulamayı kaldırmadan güncelle**. Farklı bilgisayarda farklı imzayla oluşturulmuş bir APK üzerine kurulumda imza uyuşmazlığı olabilir; kayıtlarını önce JSON olarak yedekle.

## Android Studio'da aç ve kendi APK'nı üret

1. [Android Studio](https://developer.android.com/studio) kur ve aç.
2. **Open** seç. Code → Download ZIP kullandıysan **codex-main**; tam paket kullandıysan **CepButcem** klasörünü seç. Seçtiğin klasörde `settings.gradle`, `gradlew` ve `app` birlikte bulunmalı.
3. **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** alanında tam **JDK 17** seç.
4. **SDK Manager** ile **Android SDK Platform 35** ve **Android SDK Build-Tools 34.0.0** kur. Lisansları kabul et.
5. **Sync Project with Gradle Files** işlemini tamamla. İlk derleme bağımlılık indirmek için internet gerektirir; uygulama telefonda çevrimdışı çalışır.
6. **Build → Build Bundle(s) / APK(s) → Build APK(s)** seç. Yeni Studio sürümlerinde menü adı **Generate App Bundles or APKs → Generate APKs** olabilir.
7. APK, proje içinde `app/build/outputs/apk/debug/app-debug.apk` yolunda oluşur. Bu dosyayı önceki bölümde anlatıldığı gibi telefona kopyalayıp kur.

Kendi derlemelerini güncellerken aynı imza anahtarını koru. Keystore dosyası ve şifrelerini GitHub'a yükleme.

## Uygulamayı maaş dönemine göre ayarla

1. **Ayarlar → Maaş günü ve hedefi değiştir** seç. Örneğin 15 yazarsan dönem **15 Ekim–14 Kasım** olur. Önceki/sonraki düğmeleri bu maaş dönemleri arasında geçer.
2. Tasarruf hedefini yüzde olarak seç; örneğin %20. Gün 1–31, hedef %0–100 aralığındadır. Kısa aylarda seçilen gün yoksa ayın son günü kullanılır.
3. **Açık temaya geç / Koyu temaya geç** ile görünümü ayarla. Tercihler yeniden açılışta korunur.
4. **Hesaplar** bölümünde başlangıç bakiyelerini düzenle. Açılış bakiyesi gelir sayılmaz. Maaş yatışını ayrıca **Gelir ekle** ile kaydet.
5. Giderlerini kaydet ve **Bütçe** ekranında maaş dönemi kategori limitlerini seç.

## Kredi kartı ve fatura takibi

**Kart:** Özet → Kredi kartlarım → Kredi kartı ekle. Yeni kart adı, kesinleşmiş başlangıç borcu (pozitif), kesim ve son ödeme gününü gir. Uygulama ayrı kart hesabını oluşturur; istersen mevcut bir hesabı seçebilirsin. Ardından bankadaki kalan ekstre, dönem içi ve provizyon tutarlarını ayrı gir. **Tutarları güncelle** ile güncel tutarları yenile. Bunlar sadece banka özetidir; gider veya net varlık kaydı oluşturmaz. Alışverişlerini ayrıca kart hesabına gider kaydet; ödemeni **Kartı yönet → Kart borcu öde · transfer** ile işle. Ödeme sonrasında banka özetini yeniden güncelle.

**Kart silme:** Kartı yönet → Kart takibini sil yalnızca kart görünümünü ve banka özetini kaldırır; hesabı/işlemlerini korur. Kart hesabını tamamen silmek için Kart hesabını ve kayıtlarını sil kullan.

**Hesap silme:** Ayarlar → ilgili hesap → Hesabı sil; veya Özet'teki hesap kartına dokun → Hesabı sil. Bağlı kayıt sayıları ve transfer etkisini onay ekranından oku. Silme, işlemler/faturalar/düzenli kuralları da kaldırır; önce JSON yedek al. Son kalan hesap silinemez.
**Fatura:** Ödeme takvimi → Fatura ekle ile ad, tutar, ödeme hesabı, kategori ve son ödeme tarihini gir. Vadesi gelen/geciken faturalar görünür. **Ödemeyi kaydet** gider oluşturur. Aylık tekrar seçilirse sonraki fatura aynı tahmini tutarla açılır; yeni tutarı geldiğinde düzenle. Aynı ödeme için ayrıca düzenli gider tanımlama. Hatalı ödemeyi **Ödemeyi geri al** ile düzelt. Ödeme takvimi uygulama içinde gösterilir; v1.4’te Ayarlar → Ödeme bildirimleri ile yerel hatırlatmaları açabilirsin.

## Asistan yorumlarını kullan

Özet → Asistan yorumlarını incele ve Analiz → Asistan yorumlarını aç bölümünde gelirinin harcanan yüzdesini, önceki döneme göre değişimleri, artışa en çok katkı yapan kategoriyi, tasarruf hedefini ve dönem sonu senaryosunu oku. Sonuçlar kayıtlarından çevrimdışı hesaplanır. Gelir veya önceki dönem verisi eksikse bunun belirtilmesi normaldir; veri olmadan yüzde/değişim sonucu üretilmez. Bekleyen faturalar senaryoya katılır; gelecekteki maaş varsayılmaz. Günlük hedef payı, banka hesabındaki kullanılabilir bakiye değildir.

## Yeni görünümde kısa yollar

**Özet:** bakiye, gelir/gider, harcama göstergesi, hesap kartları, yaklaşan ödemeler ve hızlı tekrar. Hesap kartına dokununca ayrıntısı açılır.

**Ödeme takvimi:** Özet veya Ayarlar → Ödeme takvimini aç. Kartları ve faturaları burada yönet.

**İşlem tekrarı:** İşlemler ekranında bir kayıt satırına basılı tut. Yeni tarih ve tutarı gözden geçirip Kaydet seç; Vazgeç mevcut kaydı korur.

**Harcama haritası:** Analiz → Harcama haritası. Güne dokun; toplam gelir/gider ve gün dengesi takvimin altında görünür. **İşlem detaylarını göster** ile listeyi aç/kapat; gün seçimi ayrı pencere açmaz.

## Web referansı ve mimari rehber

Tam paketteki CepButcem/web-reference veya depodaki web-reference klasöründe Node 22.12+/24 ile npm ci, npm test, npm run build çalıştır. npm run dev geliştirme sunucusunu açar. Kayıtlar demo oturumundadır ve sayfa yenilenince temizlenir. Android kayıtlarına bağlantı yoktur. Sunucu mimarisi, SQL, JWT ve dört haftalık plan: docs/ARCHITECTURE.md.

## v1.4: krediler, maaş ve bildirim

Ayarlar → Finans araçların → Kredilerim ve taksitler → Kredi ekle. Anapara, bankanın aylık taksiti, toplam vade, ilk taksit tarihi, hesap ve kategori gir. Ödemeyi işaretlerken **Bakiyeden düşülsün mü?** sorusunda **Evet · gider ekle** bakiyeyi azaltır; **Hayır · yalnızca takip** kredi planında ödenmiş gösterir, gider yaratmaz. Ödeme takviminden geri alabilirsin. Takibi kaldırma mevcut gider kayıtlarını korur.

**Ayarlar → Maaş hesaplayıcı → Maaş Hesapla:** Mevcut maaşını, yılı ve Ocak–Haziran veya Temmuz–Aralık dönemini seç. Altı ayın aylık enflasyonlarını gir; boş aylar %0 sayılır. **Zam Oranları** sekmesinde başlangıç yılı ve sonraki yılın Ocak/Temmuz toplu sözleşme zamlarını gir. Tahmini maaş ve artış oranı giriş yaptıkça güncellenir. **Kaydet** iki sekmedeki taslağı birlikte saklar; kaydetmeden kalıcı veri değişmez. Hesaplama ayarları JSON yedeğinde korunur.

Hesap `App.jsx` içindeki `MaasTab` ile aynıdır: aylık enflasyonlar bileşik çarpılır; enflasyon farkı = `max(0, ((1 + kümülatif enflasyon / 100) / (1 + mevcut dönem zammı / 100) − 1) × 100)`; tahmini maaş = `mevcut maaş × (1 + fark / 100) × (1 + sonraki dönem zammı / 100)`. Mevcut maaş dönem zammını zaten içerir. Temmuz–Aralık seçilince sonraki yılın Ocak zammı kullanılır. Tahmin gelir kaydı oluşturmaz; fiilen yatan maaşı **Gelir ekle** ile kaydet.

**Ayarlar → Ödeme bildirimleri → Ödeme hatırlatmalarını aç.** Android izin sorarsa izin ver. Vade günü/1/3 gün önce ve kontrol saatini seç. **Deneme bildirimi gönder** ile kontrol et. İzin kapalıysa Android bildirim ayarlarına yönlendirilirsin. Enerji tasarrufu nedeniyle belirtilen saatten sonra gelebilir; zorla durdurma sonrası uygulamayı aç. Banka özetini ödeme sonrası güncelle.

Analiz → Harcama haritasında seçtiğin günün toplam geliri, gideri ve dengesi takvimin altında görünür. **İşlem detaylarını göster** düğmesiyle ayrıntıyı aç veya gizle; ayrı gün penceresi açılmaz.

Konuya özel yardımda hangi konuyu istediğini seç; alışveriş senaryosu gerçek gelir/gideri değiştirmez. JSON v4 yedek kredi planını da taşır; eski yedekler okunur. Yeni telefonda bildirimleri yeniden etkinleştir.

## v1.6: kalan ekstre ve özet ekranından ödeme

Kartın **Tutarları güncelle** formunda dört ayrı alan var: kalan ekstre, bu ekstre için ödenen tutar, dönem içi borç ve provizyon. Örnek: ekstre 10.000 TL, daha önce 3.000 TL ödenmişse kalan 7.000, ödenen 3.000 gir. Ödenen tutar kalan borca eklenmez. Daha önce bankada yaptığın bir ödemeyi burada girdiysen tekrar ödeme olarak kaydetme. Yeni hesap kesiminde yeni ekstreye ait kalan/ödenen tutarları gir; eski ekstre ödemelerini yeni ekstreye taşıma.

**Özet → Yaklaşan ödemeler → Ödendi olarak işaretle**:

- Fatura: tutar ve gider kaydı onayını gör; onaylayınca bugün ödenir. Geçmiş tarih için ödeme merkezini kullan.
- Kredi: **Bakiyeden düşülsün mü?** → Evet gider, Hayır yalnızca takip; ardından ödeme tarihini seç.
- Kart: kalan ekstre tutarı hazır gelir. Kısmi ödeme için değiştir. **Ödemeyi kalan ekstre borcundan düş** seçiliyken kalan azalır, ödenen artar. Transfer seçiliyse banka/nakit→kart aktarımı oluşturulur; gider olarak tekrar sayılmaz. Transfer kapalıysa yalnızca ekstre takibi değişir.

Önümüzdeki 7 gün listesinin dışında kalan ve gecikmiş ödemeler **Ödeme takvimini aç** ile yönetilir. Kartın güncel banka özeti yoksa borç tahmin edilmez. Yeni kesim döneminde ödeme yapmak için önce banka özetini güncelle. Transferi sonradan düzenlemek/silmek bankadan takip edilen ekstre tutarlarını değiştirmez; gerekiyorsa **Tutarları güncelle** ile düzelt. JSON v5 yedeği eski v1–v4 yedekleriyle geri yükleme uyumunu korur; v5 yedeğini uygulamanın v1.6 veya üzeri sürümünde aç.

## v1.7 · Sıcak palet, bütçe halkası ve ayrı kartlar

- Açık tema: vurgu **#A74E32**, zemin **#FCF5EF**. Ana kart harcama riskine göre sıcak ton değiştirir; gelir/gider renkleri anlamlarını korur.
- Dönem bütçesi, sıfırdan güncel yüzdeye gelen yuvarlak göstergeyle sunulur. Gelir yoksa “—”; %100 aşılırsa gerçek yüzde yazılır.
- Toplam kalan ekstre ve dönem içi + provizyon, büyük finans kartının içinde iki ayrı kutudur; ödenen ekstre küçük yazıyla görünür.
- Her kutu aynı kartların önceki maaş dönemindeki son banka kaydına göre artış/azalış gösterir. Geçmiş yoksa veya bir kartın verisi eksikse oran üretilmez. Sıfır borçtan artışta yüzde yerine açıklama kullanılır.
- Kart başına son 24 maaş döneminin banka özetleri saklanır. Maaş günü değişince eski aralıklarla oran karşılaştırılmaz. İlk kullanımda önceki dönem verisi son kayıttan alınabilir; uygulama bilinmeyen geçmişi tahmin etmez.
- Ayarlar → Krediler/Maaş/Ödeme takvimi → geri, Ayarlar’a döner. Bir alt ekran başka bölümden açılmışsa geldiğin yere dönersin.
- Banka/nakit hesapları ve kartlar ayrı listelenir. İşlem formunda kaynak listesi banka/nakit içerir; giderde **Ödeme yöntemi → Kredi kartı** ayrı kart seçimini açar. Transfer iki tür arasında yapılabilir.
- Kart takibi silinince geçmiş kayıtlar korunur ve hesap banka listesine karışmaz. Ayarlar’daki Kartların bölümünden takibi yeniden açabilir veya onayla kart hesabını ve bağlı kayıtları kalıcı silebilirsin.

JSON yedek biçimi v6; v1–v5 yedekleri okunur. v6 yedeğini v1.7 ve üzeriyle aç. Kart geçmişi işlem tutarı değildir; banka özeti güncellemek tek başına gelir/gider yaratmaz.

## v1.8 · App.jsx göstergeleri ve kişisel planlama

- **App.jsx bütçe göstergesi:** güncel App.jsx'teki 128×128 üç renkli halka; %0–50 yeşil, %50–80 sarı, %80–100 kırmızı dilimler, aynı çizgi kalınlığı ve 3,5 saniyelik ease-out-cubic animasyon. **Ayarlar → Maaş dönemi ve görünüm → Yarım daire ibreye geç** ile ilk referanstaki 320×235 ibreyi seçebilirsin: 10 renk dilimi, 21 çizgi ve üçgen ibre. İki görünümde de gerçek gider/gelir oranı kullanılır; transferler hariçtir. %100'ün üstünde sayı gerçek oranı gösterir, çizim sınırda kalır. Gelir yoksa “—”; animasyonlar kapalıysa doğrudan son değer.
- **Hızlı işlem şablonları:** Ayarlar → Finans araçların → İşlem şablonları. Ad, tür, tutar, hesap, kategori ve açıklama saklanır. Özet'te ilk dört şablon tek dokunuşla bugüne eklenir; 10 saniye içinde Geri al kullanılabilir.
- **Gider tahmini:** yinelenen giderler ve gider olarak ödenmiş faturalar sabit giderdir. Değişken giderlerin günlük ortalaması yalnızca kalan günlere yayılır; gelecekte kayıtlı işlemler ve bekleyen faturalar ayrıca eklenir. En az 7 gün ve 3 harcama günü yoksa istatistiksel tahmin gösterilmez. Bilinen gelecek gelir ayrı sunulur; gerçekleşene kadar harcanabilir kabul edilmez.
- **Kategori uyarısı:** en az üç kayıtlı geçmiş maaş dönemindeki karşılaştırılabilir gün ortalamasının iki katını aşan gideri belirtir. Eksik dönemler sıfır varsayılmaz. Analiz → Kategori hareketleri çubuğuna dokununca tutar ve tarihler grafiğin altında görünür.
- **Birikim hedefleri:** her hedef ayrı banka/nakit hesabına bağlıdır. Para ayırmak ve geri almak gerçek transfer oluşturur; gelir/gider değildir. İlerleme hesabın bugünkü bakiyesidir. Son üç tamamlanmış dönemde pozitif net katkı varsa yaklaşık tamamlanma süresi gösterilir.
- **Kart alışveriş taksitleri:** bankadaki kredi takibinden ayrı; kart, aylık tutar, ilk tarih, toplam/ödenen taksit ve kalan yük. İşaretleme yalnızca takiptir; ekstra gider veya ekstre borcu üretmez. Kart ekranında aylık ve kalan takip yükü görünür.
- **İşlem silmede geri alma:** normal işlem silindikten sonra 10 saniye boyunca Geri al, aynı kimlik ve bakiye etkisiyle kaydı geri getirir. Bağlı fatura ödemeleri ödeme merkezinden yönetilir.
- **Ekstreye özel tarihler:** Kartı yönet → Ekstreye özel tarihler. Kesim ve son ödeme, o ekstre için takvim ve bildirimlerde kullanılır; son 24 kayıt saklanır. Önce banka tutarlarının güncel olması gerekir.

Yeni yönetim listeleri 20 kayıtla sayfalanır. Analizler ham işlem listesini yüklemek yerine tarih/kategori bazında SQL toplamlarını kullanır. JSON yedek **v7**; eski v1–v6 dosyalar desteklenir. Yeni yedeği v1.8 veya üzeriyle aç. Mevcut APK'nın üzerine güncelle; önce JSON yedek al ve uygulamayı kaldırma.
## v1.8.2 · Ödeme bildirimi ve sabit analiz konumu

- Son ödeme günü bildirimi zil simgesi ve **🔔 Son ödeme günü** başlığı taşır. Metin: “Bugün Elektrik son ödeme günü lütfen kontrol ediniz!”, iki ödemede “Elektrik ve Su”, üç ödemede “Elektrik, Su ve İnternet”. Fatura, kredi ve kart adları kullanılır. Yalnızca bugün vadesi gelenler bu cümlede birleşir; diğer tarihler ayrıntılarda kalır.
- Bildirim genişletilince tam cümle ve ödeme ayrıntıları okunur. Değişmeyen bildirim tekrar gönderilmez; ödeme listesi değişirse aynı bildirim güncellenir. Hatırlatma için **Ayarlar → Ödeme bildirimleri → Ödeme hatırlatmalarını aç** ve Android bildirim izni gerekir.
- Analiz → Gelir/Gider kategorilerini göster geçişi sayfayı en üste götürmez; kaydırma konumu ilk çizimden önce korunur.

