# NOVA AI 1.0 — kişisel asistan kararı

## Onaylanan kapsam

Uygulamanın adı **NOVA AI**, kullanıcıya görünen yapay zekâ asistanının adı **Adaş** olarak belirlenmiştir. Sohbet başlığı ve asistanın kendini tanıtması bu adı kullanır. Adaş ürün içindeki asistan adıdır; temel modelin teknik adı, kaynağı ve lisansı model bilgilerinde doğru biçimde korunur.

Uygulama yalnızca sahibinin kişisel kullanımı içindir; çok kullanıcılı ürün kapsamı yoktur. Ücretli API, abonelik veya barındırma gerektirmez. İlk dağıtım Android APK, sonraki hedef Windows EXE'dir. NOVA, Cep Bütçem ve İlacım'dan ayrı uygulama kimliğine ve veri deposuna sahip olacaktır. İlk Android uygulamasının kaynakları `nova/` altındadır. Güncel kurulum ve doğrulama kapsamı için `nova/README.md` ve `nova/docs/VALIDATION.md` esas alınır.

İlk sürüm: Türkçe sohbet, bas-konuş, sesli cevap, yönetilebilir kişisel hafıza, görevler ve yerel hatırlatmalar, PDF/Excel/Word analizi, açık/koyu tema ve cihaz kaynak kullanımını gösteren yerel çalışma düzeni. API kullanım bütçesi yerine bellek, depolama ve cevap uzunluğu sınırları bulunur.

## Değiştirilemez işlem güvenliği

Kullanıcının kuralı: Asistan kendi kendine dosya silmez, ödeme yapmaz, mesaj göndermez veya kritik sistem ayarlarını değiştirmez. Her somut işlem açık onay gerektirir.

- Dil modeli yalnızca öneri/işlem taslağı üretir; doğrudan cihaz veya servis yetkisi alamaz.
- Onay ekranı eylemi, hedef dosyayı/alıcıyı, tutarı ve sonucu gösterir. Onay tek kullanımlık, kısa süreli ve yalnızca gösterilen parametrelere bağlıdır. Parametre değişirse yeni onay gerekir.
- Yetki denetimi modelden bağımsız kodda ve yerel işlem katmanında yapılır. Zaman aşımı, iptal veya uygulamanın kapanması onay sayılmaz.
- Sohbet, ses dökümü, belge içeriği ve model yanıtı onay mekanizmasını çalıştıramaz. Belgelerdeki talimatlar kullanıcı yetkisi sayılmaz.
- İlk sürüm ödeme, mesaj gönderme ve kritik ayar değiştirme servislerini bağlamaz. Kullanıcıya taslak veya yönlendirme sunar. Dosya/hafıza/sohbet silme yalnızca uygulamanın kendi açık onaylı yönetim ekranlarından yapılır.
- İşlem geçmişi, hassas içerik ve anahtarları kaydetmeden eylem türü/zamanı/sonucunu gösterir.

## Önerilen teknik yapı

İlk APK, mevcut ve doğrulanmış Android araçlarıyla yerel Java + JNI/C++ olarak uygulanmıştır; önceki Flutter önerisi bu ilk sürümde kullanılmamıştır. Android Studio üzerinden ayrı `nova` projesi açılır. Windows EXE için ayrı masaüstü arayüzü/adaptörler ve Windows derleme testi gerekir. Mevcut finans ve ilaç projeleri dönüştürülmez.

Katmanlar: ekranlar → kullanım senaryoları → veri/servis arayüzleri → SQLite, platform servisleri ve cihaz üstü model adaptörleri. Yerel çıkarım motoru arayüz arkasında tutulur.

Sunucu, hesap kaydı, API anahtarı ve ücretli servis yoktur. Sohbet ve belge analizi cihazda yapılır. Java arayüzü, llama.cpp tabanlı yerel çıkarım motoruna JNI üzerinden bağlanır. Android ARM64 CPU temel çalışma yoludur; Adreno/Vulkan hızlandırması ancak gerçek cihazda doğrulanırsa etkinleştirilir. Yonga üzerinde NPU bulunması kendiliğinden model desteği anlamına gelmez. İndirilen model dosyası sürüm, lisans ve SHA-256 özetiyle doğrulanır.

Veriler uygulamanın özel alanında tutulur. İlk sürümde biyometrik uygulama kilidi ve dışa aktarma ekranı yoktur; cihaz ekran kilidi kullanılır. Android otomatik yedeği kapalıdır; veri silme açık onaylıdır. Veriler başka cihaza veya buluta otomatik gönderilmez. İlk model/dil paketi indirmesi internet ve kullanıcının mevcut bağlantısını gerektirir; bunlar yüklendikten sonra çalışma çevrimdışıdır. Şebeke veri tarifesi, elektrik ve cihaz depolaması uygulama aboneliği dışında kalan kaynaklardır.

## Hedef cihaz ve ilk model profili

Hedef: **Xiaomi 12T Pro, 12 GB RAM, Snapdragon 8+ Gen 1**. Fiziksel cihaz erişimi olmadığı için bu belge hız veya pil ölçümü içermez; boş RAM ve işletim sistemi kısıtları gerçek kullanımda ölçülecektir.

- Başlangıç adayı: **Qwen2.5-1.5B-Instruct**, GGUF **Q4_K_M**. Kaynak model Apache-2.0 lisanslıdır; paketlemede model ve motor lisans bildirimleri korunur. Dağıtılacak GGUF dosyasının kaynağı ve bütünlüğü ayrıca doğrulanır.
- Model dosyası yaklaşık 1–1,2 GB; gerçek çalışma belleği bağlam, çalışma tamponları ve motora göre daha büyüktür. 12 GB RAM'in tamamı uygulamaya ait kabul edilmez.
- İlk profil: 2.048 token toplam bağlam ve 256 token azami yeni cevap; giriş için cevap payı ayrılır. Uzun konuşmalarda son mesajlar ve ilgili onaylı hafıza seçilir, tüm geçmiş her isteğe eklenmez.
- Uygulama, modeller, ses paketleri ve geçici belge işlemleri için başlangıçta yaklaşık 4–6 GB boş alan hedeflenir; indirme öncesi gerçek dosya ve geçici alan ihtiyacı denetlenir.
- İlk sürümde tek çıkarım işi çalışır. Bas-konuş metni üretildikten sonra konuşma tanıma motoru serbest bırakılır; cevap tamamlanınca seslendirme başlar. Bu sıra eşzamanlı bellek yükünü azaltır.
- Isınma, uzun cevaplar ve pil tüketimi gerçek cihazda değerlendirilir. Otomatik büyük model indirilmez; daha büyük model ancak ayrı kullanıcı seçimi ve ölçümle sunulur.
- Türkçe sohbet ve kaynaklı belge cevabı kabul testinde değerlendirilir. 1,5B modelin güçlü bulut modelleriyle eşit doğruluk veya muhakeme sunduğu vaat edilmez.

## Ekranlar ve veri akışı

| Ekran | Davranış |
| --- | --- |
| Sohbet | Akış halinde Türkçe cevap, durdurma, dosya ekleme, bas-konuş ve sesli cevap düğmesi |
| Hafıza | Kaydedilecek bilgiyi önizleme/onay, arama, düzenleme, tek tek veya toplu silme onayı |
| Görevler | Tarih/saat/saat dilimi, tamamlandı, ertele, bildirim izni ve durum göstergesi |
| Belgeler | Sistem dosya seçicisi, dosya/analiz durumu, sayfa-sayfa veya hücre kaynaklı cevap |
| Ayarlar | Açık/koyu/sistem teması, mikrofon/ses tercihleri, model, depolama, cevap uzunluğu, cihaz ve veriler |

Hafıza varsayılan olarak cihazda tutulur; model bir bilgiyi kendiliğinden kalıcılaştırmaz. Kullanıcı onayladığı bilgiyi düzenleyebilir ve silebilir. Sohbet ve hafıza silme ayrı eylemlerdir. Silinen hafıza sonraki isteklerde kullanılmaz. Ham sohbet geçmişinde aynı bilgi varsa sohbet silme işlemi ayrıca onaylanır.

Görevler ve hafıza yönetimi çevrimdışı çalışır. Model ve gerekli dil paketleri indirildikten sonra AI sohbeti ve belge analizi de çevrimdışı çalışır. Model eksikse indirme durumu gösterilir; sahte cevap üretilmez. Görev ekleme/düzenleme/silme, tamamlanma ve hatırlatma zamanını değiştirme modelin önerisinden sonra kullanıcı işlemiyle kesinleşir.

## Ses ve bildirimler

Basılı tutarak kayıt başlar; bırakınca durur. Görünür mikrofon durumu ve iptal seçeneği bulunur; arka planda sürekli dinleme yoktur. Türkçe konuşma tanıma için whisper.cpp tabanlı çok dilli tiny/base yerel model adayları gerçek cihazda değerlendirilir; model lisansı, indirme boyutu ve doğruluğu kontrol edilir. Sistem konuşma tanıması çevrimdışı çalışma doğrulanmadan kullanılmaz. Sesli cevap cihazın kurulu Türkçe TTS sesi çevrimdışı çalışıyorsa onunla okunur. Çevrimdışı Türkçe ses yoksa ücretsiz dil paketi kurulumu gösterilir; bulut servisine sessizce geçilmez. Sessiz metin akışı daima kullanılabilir.

Hatırlatmalar Android bildirim izniyle çalışır. Yeniden başlatma ve saat dilimi değişiminde bekleyen görevlerden yeniden planlanır. Uygulama zorla durdurulmuşsa veya işletim sistemi kısıtlıyorsa teslim garantisi verilmez; izin ve pil kısıtları ayarlarda görünür. Kesin zaman izni yoksa yaklaşık teslim durumu açıkça belirtilir.

## Belge işleme

İlk sürüm metin PDF, `.xlsx` ve `.docx` destekler. Eski `.xls`/`.doc` ve şifreli belgeler desteklenmiyorsa açık hata gösterilir. Taranmış PDF OCR'siz analiz edilmiş gibi sunulmaz; OCR ayrıca ücretsiz yerel motor ve Türkçe dil paketiyle değerlendirilir; hazır değilse bu durum açıkça gösterilir.

Dosya sistem seçicisinden kullanıcı tarafından seçilir; Analiz et ile yerel çıkarım başlar, sunucuya yüklenmez. Dosyalar için başlangıç üst sınırı 10 MB, çıkarılan metin ve sayfa/hücre sayısı için ayrı sınırlar uygulanır. ZIP tabanlı belgelerde açılmış boyut sınırı ve işleme zaman aşımı bulunur; makro çalıştırılmaz. Dosya adı yol olarak kullanılmaz; işleyici kullanıcı tarafından seçilmeyen dosyalara erişemez. Çıkarılan metin kaynak konumlarıyla parçalara ayrılır; küçük bağlama yalnızca soruyla ilgili parçalar eklenir, belgenin tümü analiz edilmiş gibi sunulmaz.

Analiz yanıtları PDF sayfasına, Excel sayfa/hücresine veya Word paragrafına kaynak verir. Desteklenmeyen içerikte tahmin yerine eksik kapsam belirtilir. Excel formülleri çalıştırılmaz; kaydedilmiş sonuç yoksa hesaplanmış değer uydurulmaz. Dosya içeriği komut veya işlem yetkisi değildir. Yerel geçici dosyaların saklama davranışı gösterilir. Asistan kendi kendine dosya temizliği/silme yapmaz; temizlik kullanıcıya boyut ve dosya kapsamıyla sunulur ve açık onayla yapılır.

## Ücretsiz çalışma ve kaynak sınırları

Ücretli API, barındırma ve abonelik kullanılmaz; ücretsiz deneme kotasına dayalı bir servis de temel bağımlılık değildir. Model/ses/OCR bileşenlerinin ücretsiz kullanım ve dağıtım lisansları paketleme öncesinde doğrulanır.

Ayarlar model boyutunu, boş alanı, cevap sınırını ve indirme durumunu gösterir. Dosya boyutu, açılmış belge boyutu, çıkarılan metin ve işlem süreleri ayrı sınırlandırılır. İptal düğmesi çıkarımı durdurur. Arka planda sürekli model veya mikrofon çalıştırılmaz. Hata durumunda ücretli servise geçiş yapılmaz.

## Arayüz yönü

Premium açık tema: zemin `#F7F8FC`, yüzey `#FFFFFF`, ana metin `#172033`, vurgu `#5B4FE9`. Koyu tema: zemin `#0B1020`, yüzey `#151D30`, ana metin `#F3F5FC`, vurgu `#AAA2FF`. Hata `#C93645`, başarı `#168060`. Gerçek bileşenlerde kontrast ve büyük yazı testi yapılır; durum yalnızca renkle anlatılmaz. Hafif animasyonlar, sistem animasyon azaltma tercihi ve en az 48 dp dokunma alanı kullanılır.

## Uygulama sırası ve kabul ölçütleri

1. Ayrı yerel Android proje iskeleti, tema, yerel SQLite, görev ve hafıza yönetimi. Kabul: uygulama yeniden açılınca kayıtlar korunur; izinsiz silme yoktur.
2. Yerel model indirme/doğrulama ve gerçek sohbet entegrasyonu. Kabul: uçak modunda Türkçe sohbet, ilk cevap ve üretim hızı ölçümü, bellek/ısınma kontrolü, iptal ve bozuk/eksik model hataları. API anahtarı, sunucu veya ödeme gerekmez.
3. Bas-konuş, Türkçe ses ve hatırlatmalar. Kabul: mikrofon reddi, kaydı iptal, bildirim reddi, yeniden başlatma ve saat dilimi senaryoları geçer.
4. Belge analizi ve Android APK. Kabul: üç formatta kaynaklı cevap, bozuk/aşırı büyük dosyada kontrollü hata, belge talimatlarıyla kritik işlem tetiklenememesi ve fiziksel Android cihaz kurulumu.
5. Windows adaptörleri ve EXE. Kabul: Windows üzerinde paketleme, mikrofon/ses/bildirim, güvenli belirteç depolama ve güncelleme testi. Android başarısı Windows doğrulaması sayılmaz.

## Henüz doğrulanmayan uygulama gereksinimleri

Yerel motor Android ARM64 ve x86_64 için Java/JNI ile bağlanmıştır. Gerçek Qwen/Whisper model dosyalarının indirilmesi ve fiziksel Xiaomi 12T Pro testleri henüz tamamlanmamıştır. Windows derleme/testi de ayrıca yapılacaktır. Bu belge hedef mimariyi belirler; hazır APK, test edilmiş hız veya doğruluk iddiası değildir.
