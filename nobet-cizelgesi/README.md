# Nöbet Çizelgesi 2.5 — Android

**Yeni arayüz:** Bütçe Takip’ten ilham alan kurumsal mavi özet kartı, çizgi simgeler, yüzen alt menü, personel kartları ve açık/koyu tema. [Ekran görüntüleri](docs/TASARIM-2.0.md)

Yerel listeyle veya yetkili ortak ekiple personel, görev yeri, mazeret ve haftalık nöbet planlama uygulaması.

[APK 2.5 indir](../downloads/NobetCizelgesi-2.5.apk) · [Android Studio ZIP](../downloads/NobetCizelgesi-Android-Studio.zip) · [Test sonuçları](docs/TESTLER.md)

Android 8.0 ve üzeri. Önceki sürümlerden biri kuruluysa **uygulamayı kaldırmadan** 2.5 APK'sını açarak güncelleyin. Aynı paket adı ve imza kullanılır; eski kayıtlar okunur. Kaldırmak veya uygulama verilerini silmek kayıtları siler.

## 2.4.1 — Daha Açık Sarı

Bugünün satırı açık temada daha yumuşak krem sarısına (#FFF2CC), koyu temada biraz daha açık amber tonuna alındı. Yazı, kenarlık, Bugün etiketi ve gün belirleme davranışı korunur. [Güncel ekran](docs/ekranlar/today-2.4.1-light-weekday.png).

## 2.4 — Bugünün Satırını Vurgulama

Çizelge sayfasında telefonun yerel tarihine göre bugünün satırı bal sarısıyla vurgulanır ve **Bugün** etiketi görünür. Cumartesi ve pazar, aynı **Hafta Sonu Nöbetçisi** satırını vurgular. Başka hafta görüntüleniyorsa vurgu olmaz; çok şubeli tabloda ilgili satır her grupta görünür. Açık/koyu tema uyumludur. Uygulama ön plana geldiğinde ve açıkken yerel gece yarısında tarih yeniden kontrol edilir. Kayıtlar ve atamalar değişmez. [Görünüm](docs/GUN-VURGUSU-2.4.md).

## 2.3 — Dengeli Dağıtım Ve Açılır Mazeretler

Haftalık denge geçmiş toplamların önüne alınır: dört uygun personel/beş gün, haftalık sınır izin veriyorsa **1–1–1–2**. Kısıtlı personelin az sayıdaki uygun günü öncelikle değerlendirilir; gereksiz boşluk bırakmamak için geçici atamalar yeniden yerleştirilebilir. İzin ve haftalık üst sınırlar kendiliğinden aşılmaz. Mazeretler alfabetik personel → tür → kayıt şeklinde açılır başlıklarda gösterilir. Tema simgesi ortalandı; çizelge/istatistik/PDF satırları siyah çizgilerle ayrılır. [Kullanım ve sınırlar](docs/DAGITIM-2.3.md).

Eski onaylı listeler değişmez; yeniden dağıtmak için ilgili haftada yeni öneri üretip onaylayın. İptal edilen e-postasız giriş geliştirmesi bu sürümde yoktur.

## 2.2 — Kullanıcılar, Ortak Ekip Ve Tablo

Çizelge ekranında ekrana sığan beş hafta içi satırı ve cuma sonrası tek **Hafta Sonu Nöbetçisi** satırı var. Hücreye dokunarak düzenleme korunur. Gerçek iki günlük görev kayıtları ve sayımlar değişmez; çok şube alt alta tablo gruplarında gösterilir. [Yeni görünüm](docs/TABLO-2.2.md).

**Ayarlar → Kullanıcılar Ve Ortak Ekip** üzerinden Supabase bağlantısı, kayıt/giriş, e-posta kodu doğrulama, ekip daveti ve yönetici yetkisi yönetilir. Sunucu yalnız yöneticinin değişiklik yapmasına izin verir; her yönetici yeni yönetici atayabilir. İlk yönetici, proje sahibince kendi doğrulanmış UUID/e-posta kimliğiyle tanımlanır. Ortak liste yaklaşık 10 saniyelik ön plan kontrolleriyle yenilenir; çakışmalar sessizce üzerine yazılmaz.

**Canlı proje bağlanmadı; gerçek Supabase/iki fiziksel telefon eşitlemesi doğrulanmadı.** Proje kurulana kadar yerel liste kullanılabilir. [Adım adım Supabase kurulumu](docs/ORTAK-KULLANIM.md) · [Sunucu SQL](backend/001-supabase.sql) · [İlk yönetici SQL](backend/002-first-owner.sql). APK'ya özel sunucu anahtarı konulmaz. Eski kişisel dosya, ortak ekipten ayrı korunur; ilk aktarım açık teyitle yapılır.

## 2.1 — Resmî Tatiller

**Ayarlar → Resmî Tatil Takvimi:** Türkiye sabit resmî tatilleri ve 2020–2032 dinî bayramları çevrimdışı tanınır. Tatil ekleyebilir, tarihi/adı/süreyi düzeltebilir veya devre dışı bırakabilirsiniz. Yarım gün **0,5**, tam gün **1** sayılır. Aynı tarih çakışmalarında görev iki kez sayılmaz.

Normal istatistikler korunur. İstatistik ekranındaki ayrı **Resmî Tatiller** tabloları personel ve görev yerine göre sayar; kayıtlı nöbet/tatil değişiklikleri sayımlara yansır. Geçmiş ve gelecek ayrı tutulur; ayrı PDF düğmesi vardır. Tatil takvimi otomatik nöbet dağılımını değiştirmez. [Kapsam, kaynaklar ve kullanım](docs/RESMI-TATILLER.md).

## Hafta sonu dönüş önceliği

İzin veya raporun bitmesi tek başına öncelik oluşturmaz. Personelin hafta sonu sırası mazeret süresinde henüz gelmediyse normal sırası korunur. Sırası gelmiş, mazereti nedeniyle atlanmış ve sonraki kişi atanmışsa, mazereti bittikten sonraki ilk uygun hafta sonu önceliklidir. Sonrasında normal döngü kaldığı yerden devam eder. Örnek: sıra A/B/C/D/E; A raporlu olduğu için B atanır, A dönünce A atanır, ardından C devam eder. E aynı dönemde izinden dönse bile henüz sırası atlanmadıysa öne geçmez.

## Manuel mazeret istisnaları

Kalemden mazeretli/kısıtlı personeli elle seçince **Mazeret / Kısıt İstisnası** uyarısı açılır. Kişi, tarih, görev yeri ve çakışan bütün mazeret/kısıt kayıtları gösterilir. **Onayla** yalnız o görev için istisna verir; **Vazgeç** listeyi değiştirmez. Tarihli izin/rapor, genel kısıt ve haftalık gün kısıtları desteklenir. Aynı gün çoklu yer veya haftalık sınır da varsa aynı teyitte belirtilir. Ardından çizelge değişikliklerini kaydedin.

İstisna kişi+tarih+görev yeri+mazeret kaydıyla sınırlıdır ve yeniden açılınca korunur. Mazeret silinmez; diğer gün ve yerlerde geçerlidir. Yeni veya düzenlenmiş bir kısıt eski onaydan yararlanmaz. Otomatik dağıtım geçmiş istisnaları kullanmaz; mazeretli günlere kendiliğinden atama yapmaz. Pasif personel, kapalı yer, aynı slotta birden fazla kişi ve Perşembe/Cuma dinlenmesi kuralları sürer.

Hafta sonu elle seçiminde de mazeret için önce teyit alınır. Mazeret devam ederken onaylı istisnayla bekleyen hafta sonu tutulduysa o bekleyen hak tamamlanır; bitişten sonra aynı borç nedeniyle tekrar öne geçilmez. Eski dosyaların mazeret kimlikleri güvenli biçimde tamamlanır; eski kayıtlar kendiliğinden istisna kazanmaz.

## Aynı gün farklı şubelere atama

Kalemden aynı kişiyi aynı gün başka bir şubeye/görev yerine seçebilirsiniz. **Aynı Gün Atama Teyidi** mevcut görev yerlerini, tarihi, personeli ve ek atamayı gösterir. **Onayla** ile ek atama taslağa uygulanır; **Vazgeç** mevcut listeyi değiştirmez. Ardından çizelge değişikliklerini kaydedin.

İstisna yalnız o haftadaki kişi/tarih/görev yeri çifti için saklanır; başka bir tarih, kişi veya üçüncü yer için yeni teyit gerekir. Haftalık kişi sınırı da aşılacaksa aynı teyitte önceki atama tarihleriyle gösterilir. Mazeret/haftalık gün kısıtı varsa aynı teyitte görev için özel istisna istenir; pasiflik, kapalı yer ve Perşembe/Cuma dinlenmesi korunur. Otomatik dağıtım kendiliğinden aynı güne birden fazla yer atamaz.

Onaylı ek görevler kayıt yeniden açıldığında korunur. İstatistikte her şubenin nöbeti kendi sütununda sayılır; çizelge/PDF'de iki atama da görünür. Aynı yerde aynı gün birden fazla kişi kuralı değişmez.

## Sayfa konumunu koruma

Çizelgedeki kalemden personel seçtikten sonra sayfa en üste dönmez. Görünen günün ekran içindeki konumu korunur; alanı boş bırakma, haftalık sınır için teyit edilen seçim ve hafta sonu personel değişimi de aynı yolu kullanır. Kaydedilmiş listeyi düzenlemeye açarken konum korunur. Sekme/hafta değiştirmek normal şekilde yeni ekranı baştan açar.

## Tarihsiz haftalık kısıtlar

**Mazeret → Mazeret / Kısıt Ekle → Her Hafta Tekrarla** seçilince tarih ve mazeret türü alanları gizlenir. Yalnız görev yeri ve haftanın günleri gösterilir. Aynı satırda Pazartesi/Çarşamba gibi birden fazla gün işaretleyebilirsiniz. Alttaki **+ Kısıt Ekle** aynı personele başka görev yeri/gün birleşimi için bağımsız satır ekler; **− Kısıtı Kaldır** o satırı çıkarır. Gün seçilmeyen satır varsa hiçbir satır kaydedilmez.

Haftalık kayıtlar tarih seçmeden sürekli tekrar eder. Uygulama dağıtımdaki gerçek tarihin haftanın hangi günü olduğunu bulur ve seçili yerde o güne atama yapmaz. Yeni kurallar bugünden itibaren geçerlidir; eski nöbet geçmişi geriye dönük değiştirilmez. Onay ekranı kişi, günler ve görev yerlerini birlikte gösterir. Tarihli izin/rapor için Her Hafta Tekrarla seçeneğini kapalı bırakın; tarih alanları görünür.

Eski sürümde bitiş tarihli oluşturulmuş haftalık kayıtların mevcut kapsamı korunur ve **Eski Tarihli Kayıt** olarak işaretlenir. Düzenlenip haftalık modda kaydedilirse bugünden itibaren süresiz kurala dönüşür. Normal tarihli izin/raporlar değişmez.

## Birden fazla kısıt

**Mazeret → Mazeret / Kısıt Ekle** ile aynı personele birden fazla bağımsız kayıt ekleyebilirsiniz. Birini düzenlemek veya silmek diğerlerini kaldırmaz. **Her Hafta Tekrarla** seçeneğinde Pazartesi–Pazar arasından birden fazla gün işaretlenebilir. Örneğin aynı kişiye Her Pazartesi/Her Çarşamba Şube kısıtı, ayrıca Her Cuma tüm yerler için ayrı kısıt eklenebilir.

Yeni haftalık kısıtlarda tarih alanı yoktur; kurallar sürekli tekrar eder. Tekrar günleri boş bırakılamaz. Seçilmeyen günler başka mazeret yoksa serbesttir. Genel mazeret tüm görev yerlerini, yalnız belirli yere ait kısıt o yeri engeller. Otomatik/manuel atamalar tüm kısıtların birleşimini denetler.

Onaydan önce kişi, günler, tarih aralığı ve görev yeri gösterilir. Yalnız yeni kuralla çakışan kayıtlar kaldırılır; boşalan yerler ayrıca düzenlenebilir veya öneri üretilebilir. Düzenleme/silme sonrasında diğer kısıtlar ve bekleyen nöbet hakları korunur. Eski tarihli mazeretler güncellemede tarih aralığındaki her gün için geçerli kalır.

## Yukarı / aşağı taşıma

**Ayarlar → Hafta Sonu Nöbet Sırası** bölümündeki **↑ / ↓** düğmeleri personelleri birer sıra taşır. İsim silmek veya yeniden eklemek gerekmez. Birkaç taşıma yapıp **Sırayı Kaydet** ile önizlemeyi onaylayın. **Sıra Değişikliklerini İptal Et** kaydedilmemiş taşımaları geri alır. İlk kişinin yukarı, son kişinin aşağı düğmesi kapalıdır. Personele dokunarak doğrudan sıra numarası seçme de korunur.

Personel ekranındaki **Nöbet Sırasını Düzenle** düğmesi aynı ayarlara açılır. Kaydedilmemiş sıra, ekran/tema yeniden oluşturulurken korunur; personel listesi değişirse geçersiz taslak bırakılmaz. Taşıma sırasında personel kimliği, adı, eski çizelgeler ve istatistikler değişmez. Yeni sıra onaylanınca sonraki otomatik dağıtım 1. sıradan başlar; bekleyen mazeret hakları korunur.

## Numaralı hafta sonu sırası

**Ayarlar → Hafta Sonu Nöbet Sırası** bölümünde personeller **1. Sıra, 2. Sıra…** olarak listelenir. Personele dokunup yeni sıra numarasını seçin; diğer kişiler otomatik kaydırılır. Önizlemedeki yeni sıralamayı onaylayarak kaydedin. Aynı sırayı tekrar seçip onaylamak döngüyü 1. sıradan yeniden başlatır.

**Sıraya Yeni Personel Ekle** düğmesinde adı ve hafta sonu sıra numarasını girin. Personel bölümünden eklerken de sıra numarası seçilir. Her aktif personel sırada bir kez yer alır; pasifler atlanır. Yeni sıra kaydedildikten sonraki ilk otomatik öneri 1. sıradan başlar, sonraki onaylı haftalarda döngü devam eder. Mazereti biten bekleyen personelin önceliği korunur; bu nöbet normal sırayı tüketmez. Perşembe/Cuma dinlenmesi sürer.

Sıra değiştirmek geçmiş/onaylı çizelgeleri veya istatistikleri değiştirmez; açık öneri iptal edilir. Önceden hazırlanmış gelecek haftaları yeni sırayla kullanmak isterseniz yeniden öneri üretip onaylayın. Eski kayıtlar güncellenirken mevcut ekleme sırası ve döngü konumu korunur; Ayarlar'dan değiştirene kadar devam eder. Sıralama ve döngü konumu cihazda saklanır. İstatistikler alfabetik kalır.

## İstatistik PDF

İstatistik ekranında **İstatistikleri PDF Olarak Paylaş** düğmesi bulunur. PDF yalnız tabloları içerir; açıklama, özet veya imza alanı eklenmez. Bugün Ve Öncesi / Gelecek Atamalar ayrımı tablo başlığında korunur. Gelecek atama yoksa gelecek tablosu oluşturulmaz. Personeller Türkçe alfabetik sırada, tüm şubeler sizin verdiğiniz adla gösterilir. Çizelgede gizlenen şubeler istatistik PDF'sinde de sayılmaya devam eder.

Uzun adlar satıra bölünür; çok personel/şube varsa tablo ek sayfalara devam eder. Her sayfada sütun başlıkları ve personel adları tekrarlanır. Boşluklar tablo boyutuna göre sınırlıdır. PDF'yi Android paylaşım ekranından WhatsApp'a veya başka bir uygulamaya aktarabilirsiniz. İstatistik ekranındaki açıklama paragrafları da kaldırıldı.

## 1.3 düzeni

- İstatistik başlıklarındaki A/B/C harfleri kaldırıldı. **Personel, Hafta İçi, Hafta Sonu** ve verdiğiniz şube adları gösterilir.
- Tablo ekran genişliğine sığar; yatay kaydırma gerekmez. Çok sayıda şube varsa okunabilir sütun grupları alt alta devam eder. Hücre boşlukları azaltıldı, uzun adlar satıra bölünür.
- PDF kenarları ve satır araları daraltıldı; sayfa 700 × 358 punto olarak tabloya göre boyutlandı. Alttaki geniş boşluk kaldırıldı. Çok sayıda yer ek sayfalarda eksiksiz korunur.
- Sabit arayüz başlıkları/düğmeleri **Hafta İçi**, **Hafta Sonu** düzeninde büyük baş harflerle gösterilir; girilen personel/şube adları korunur.

## Korunan şube ayarları

- **Eklenen şubeyi silme:** Ayarlar → ilgili şube → Şubeyi sil. Silinen yer yeni dağıtımlardan çıkar; yarın ve sonraki atamaları kaldırılır. Geçmiş ve bugünkü kayıtları/istatistikleri korunur. Silinen görev yerleri düğmesinden geri alınabilir; eski gelecek atamalar otomatik geri gelmez. İlk iki temel yer silinmez, adı/günleri değiştirilebilir.
- **İlk iki yerin günleri de değiştirilebilir.** Hiç gün seçilmezse o yere yeni atama yapılmaz. Ana yerde yalnız cumartesi veya yalnız pazar da seçilebilir; yalnız seçili güne atama yapılır. İki hafta sonu günü seçiliyse aynı kişi atanır. Ana yerin hafta sonu nöbetçisi perşembe/cuma dinlenir. İlk şubenin hafta sonu günleri de seçilebilir. Gün değişikliği mevcut kayıtları silmez; yeni önerilerde uygulanır.
- **Çizelgede gösterilecek şubeler:** otomatik/manuel liste oluştururken sorulur; kaydedilmiş listede aynı düğmeyle değiştirilebilir. İşareti kaldırılan şube yalnız çizelge/PDF'de gizlenir. Atamaları ve istatistikleri korunur; otomatik dağıtım devam eder. Ana yer görünür kalır. Görünüm seçimi cihazda hatırlanır ve tüm haftalar için geçerlidir.
- Mavi, beyaz yazıyla okunabilen daha açık **kurumsal mavi** tona (#2A6FB1) güncellendi.

## Önceki sürümden korunan özellikler

- **Ayarlar:** İşyeri/Şube adları değiştirilebilir. Yeni görev yeri/şube eklenebilir; çalışma günleri, kişi başı haftalık üst sınırı (0=sınırsız) ve otomatik/yalnız manuel dağıtım seçilebilir. En fazla 20 yer desteklenir. İlk iki yerin adları ve günleri serbesttir; mevcut kişi başı sınırları korunur.
- Yer kimliği sabittir: ad değişse de geçmiş nöbetler, mazeret bağlantıları ve sayımlar korunur. Çizelge/PDF/istatistikler yeni adı gösterir.
- Açık temanın düğmeleri **mavi zemin/beyaz yazı**; bilgi bildirimleri uygulama içinde aynı renklerde gösterilir. Açık/koyu/sistem seçimi hatırlanır.
- Kaydedilmiş haftada **Kaydedilmiş listeyi düzenle** veya gün/yer alanına dokunun. Düzenlemeyi **Değişiklikleri kaydet** ile onaylayın. Yalnız o hafta değiştirilir; diğer haftalar silinmez. Sayımlar güncel kayıtlardan hesaplanır; tekrar kaydetmek mükerrer sayım üretmez.
- Otomatik dağıtım şubede kişi başı haftalık sınırı korur. Elle aynı kişiye ikinci görev verildiğinde önceki atama tarihleri gösterilir ve **Tekrar atamak istiyor musun?** onayı istenir. Onay yalnız bu haftanın ilgili kişi/yer sınırı için istisnadır. Aynı gün başka yer görevi varsa aynı teyitte gösterilir; mazeret için ayrıca görev kapsamlı istisna istenir, perşembe/cuma dinlenmesi korunur. İstisna kaydedilir ve yeniden açınca korunur.
- PDF yalnız başlık, tarih aralığı ve çizelge içerir. “Onaylı haftalık çizelge”, alt açıklamalar ve hazırlayan/onaylayan alanları kaldırıldı. Atama olmayan hücreler **—** gösterilir. Yeni yerler dahil edilir; 3'ten fazla yerde PDF ek sayfalara bölünür.

## Kullanım

1. **Personel:** İsimleri ve hafta sonu sıra numaralarını girin; sıralamayı Ayarlar’dan değiştirebilirsiniz. Personel çıkarma pasife alma şeklindedir; geçmişi korunur, bugün/gelecek atamaları kaldırılır. Yeniden etkinleştirebilirsiniz.
2. **Ayarlar:** Yer adlarını ve yeni şubeleri tanımlayın. Yeni yer eklemek/gün değiştirmek eski haftalara kendiliğinden personel yazmaz; ilgili haftayı düzenleyin veya yeni öneri üretin.
3. **Mazeret:** Senelik izin/rapor/genel mazeret, başlangıç ve bitiş dahil **tüm yerlerde** atamayı engeller. İsterseniz yalnız seçtiğiniz yere kısıt girin. Mazeret eklenince yalnız çakışan atamalar kaldırılır; diğer haftaların çakışmayan kayıtları korunur. Boşalan günler ayrıca düzenlenebilir veya yeniden dağıtılabilir.
4. **Çizelge:** Hafta seçin → otomatik öneri veya boş manuel çizelge açın. Varsayılan olarak ana yerde her gün bir kişi; ilk şubede hafta içi kişi başı bir gün. Bu yerlerin günleri de Ayarlar’dan değiştirilebilir. Yeni yerlerde ayarladığınız gün/sınırlar kullanılır. Uygun kişi yoksa zorla atama yapılmaz; eksik günler bildirilir ve kayıttan önce teyit edilir.
5. Ana yerde cumartesi+pazar seçiliyse aynı kişidir; yalnız biri seçiliyse o gün atanır. Bu kişi aynı haftanın perşembe/cuma günleri **tüm yerlerde** dinlenir. Manuel listede önce hafta sonunu seçmek kolaylık sağlar. Çakışan eski alanı önce boşaltın.
6. Hafta sonunu atlamak veya devretmek için mazeret bitiş tarihini girin. Atlanan kişi bitişten sonraki ilk uygun hafta sonuna önceliklidir. Birden çok bekleyen varsa en erken mazereti biten uygun kişi önceliklidir; diğerlerinin sırası korunur. Sonradan eski hafta düzenlense de bekleyen haklar, geçmiş kayıtlar üzerinden takip edilir.
7. Onaylı haftada **PDF oluştur ve paylaş** → WhatsApp veya başka uygulamayı seçin. Alıcıyı siz belirlersiniz; otomatik mesaj gönderilmez.

## İstatistik tablosu

Personeller Türkçe alfabetik sıradadır. Başlıklar Personel, Hafta İçi, Hafta Sonu ve sizin verdiğiniz şube adlarıdır; harfli sütun işaretleri yoktur. Ana yerin hafta içi/hafta sonu sayıları ayrı, her şubenin toplam nöbet günleri kendi sütunundadır. Cumartesi+pazar 2 gündür, sıra açısından 1 bloktur. Tablo ekrana sığar; çok sayıdaki şube sütunları personel adları tekrarlanarak aşağıda devam eder. Yatay kaydırma gerekmez.

Bugün ve öncesindeki onaylı atamalar ile gelecek atamalar ayrı tablolardadır. Taslaklar sayılmaz. Fiilen gerçekleşmeyen görevleri kaydedilmiş çizelgede düzeltin; uygulama devam/saat ölçümü yapmaz.

## Gizlilik ve ortak kullanım

Reklam veya kişisel veri günlüğü yoktur. Yerel liste atomik dosyada cihazda saklanır. 2.2 sürümünde isteğe bağlı ortak ekip için internet izni vardır: Supabase bağlantısı/giriş/ekip kurulmadan yerel liste kendiliğinden paylaşılmaz. Ortak ekibe geçince onaylanan değişiklikler bağlı HTTPS sunucusuyla paylaşılır; oturum ve ortak cache cihazda şifrelenir. Eski kişisel dosya ayrı kalır. Otomatik cihaz bulut yedeği kapalıdır. PDF yalnız kullanıcı paylaşımıyla seçilen uygulamaya okunabilir URI üzerinden verilir; veri yedeği değildir. [Ortak kullanım kurulumu, veri kapsamı ve doğrulama sınırı](docs/ORTAK-KULLANIM.md).

## Derleme

JDK 17, Android SDK 35, build-tools 35.0.0. Android Studio'da bu klasörü açın, `local.properties` dosyasında SDK yolunu tanımlayın. Gradle 8.9 wrapper SHA-256 doğrulamalıdır.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Windows: `gradlew.bat ...`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Bu teslim doğrudan kurulabilen debug imzalı APK'dır; Play Store yayını değildir. İmza anahtarı ZIP'e/repoya eklenmez; sonraki güncellemelerde aynı anahtar gerekir.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.semih.nobet.test/com.semih.nobet.SmokeRunner
```

Depolama/PDF testleri ayrı dizin kullanır; uygulamanın mevcut personel kayıtlarına yazmaz.

## 2.5 — Hafta Sonu Nöbet Çizelgesi

Çizelge sayfasında haftalık tablonun altındaki **Hafta Sonu Nöbet Çizelgesi ▾** düğmesine dokunun. Kayıtlı haftaların cumartesi/pazar tarihleri ve ana işyeri nöbetçi isimleri kronolojik açılır listede görünür: **31 Ağustos–1 Eylül 2024 · Semih**, **7–8 Eylül 2024 · Serdar**. Satıra dokununca ilgili haftaya geçilir. Kaydedilmemiş taslak varsa geçişten önce onay alınır.

Cumartesi ve pazar farklı kişilerse günleriyle ayrı gösterilir; boş atama — görünür. Ay/yıl geçişleri desteklenir. Liste yalnız kaydedilmiş haftaları okur; henüz kaydedilmemiş gelecekteki haftalar için isim tahmin etmez. Haftalık kayıt değiştirildiğinde liste yeniden açılışta güncellenir. Nöbet dağıtım motoru ve kayıt biçimi korunur.
