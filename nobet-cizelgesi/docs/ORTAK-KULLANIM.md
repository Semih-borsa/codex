# Ortak Kullanım — Nöbet Çizelgesi 2.2

Uygulamada hesap açma, e-posta kodunu doğrulama, giriş, kullanıcı daveti, yönetici atama ve ortak liste eşitleme istemcisi vardır. **Henüz kullanıcıya ait Supabase projesi bağlanmadı; gerçek Supabase ve iki fiziksel telefonla canlı eşitleme doğrulanmadı.** Sunucu kuruluncaya kadar kişisel yerel liste kullanılabilir. GitHub APK/kaynak indirme yeridir; ortak veri sunucusu değildir.

## 1. Supabase projesi

Kendi hesabınızdan [Supabase](https://supabase.com/dashboard) üzerinde yeni proje oluşturun. Veritabanı şifresini yalnız kendi güvenli alanınızda saklayın; uygulamaya veya sohbete girmeyin. Plan/kotaları kendi hesabınızdan kontrol edin; bu kod ücretli hizmet satın almaz.

Project Settings / API bölümünden **Project URL** (`https://PROJE.supabase.co`) ve **publishable/public** anahtarını alın. Eski **anon** anahtarı da desteklenir. **Secret / service_role** anahtarı uygulamaya girilmez; istemci bunları reddeder. URL olarak Dashboard adresini veya `/rest/v1` yolunu girmeyin. Bu sürüm standart `*.supabase.co` proje adreslerini destekler.

Yeni projenin SQL Editor bölümünde [001-supabase.sql](../backend/001-supabase.sql) dosyasını çalıştırın. Uygulama verileri ve RPC fonksiyonları oluşturulur. `public` şeması Data API için açık olmalıdır; **`nobet_private` şemasını API'ye açmayın**. Anonim/doğrulanmış kullanıcılara tablolara doğrudan yazma izni vermeyin. Mevcut uygulamaların bulunduğu projede şema değişikliklerini önce inceleyin; dosya yalnız `nobet_*` tablolarını/fonksiyonlarını yönetir.

## 2. E-posta doğrulama

Authentication / Email sağlayıcısında e-posta/şifre girişi ve **Confirm Email** açık olmalıdır. Gerçek kullanıcıların e-posta sahipliği doğrulanmadan ortak ekibe erişmesine izin vermeyin.

E-posta gönderimini kurun. Supabase'in varsayılan e-posta hizmeti yalnız izin verilen adreslere ve sınırlı kotayla gönderim yapabilir. Beş kişinin farklı adresleri için Authentication / SMTP ayarlarında size ait bir SMTP sağlayıcısı gerekebilir. SMTP parolası yalnız Supabase panelinde saklanır; APK'ya konulmaz. Sağlayıcının gönderici doğrulamasını ve kotalarını tamamlayın.

Authentication / Email Templates / **Confirm Signup** şablonuna kod ekleyin. Örneğin:

```html
<h2>Nöbet Çizelgesi</h2>
<p>E-posta doğrulama kodunuz: <strong>{{ .Token }}</strong></p>
<p>Bu kodu uygulamadaki E-posta Adresini Doğrula ekranına girin.</p>
```

Uygulama kodu `/auth/v1/verify` ile doğrular; internet sitesi veya özel bağlantı yakalama gerekmez. Doğrulama kodu eklenmeyen eski bağlantı şablonu yerine yukarıdaki kod şablonunu kullanın. Kod süresi/rate limitleri Supabase ayarlarına bağlıdır. Bu istemci e-posta/şifre + kod akışını kullanır; OAuth, CAPTCHA ve şifre kurtarma ekranı içermez.

## 3. İlk yönetici siz olun

1. APK'da **Ayarlar → Kullanıcılar Ve Ortak Ekip → Sunucu Bağlantısını Ayarla** yolundan URL/public anahtarını girin.
2. **Yeni Hesap Oluştur** ile kendi adınız, e-posta adresiniz ve en az 8 karakterli şifrenizle kayıt olun. E-postadaki kodu girin. Uygulamayı kapattıysanız **E-posta Kodunu Doğrula** kullanılabilir.
3. Aynı ekranda **Hesap Kimliğini Kopyala** ile hesabınızın UUID kimliğini alın. Supabase Authentication / Users bölümünde bunun kendi **doğrulanmış** hesabınız olduğunu kontrol edin.
4. [002-first-owner.sql](../backend/002-first-owner.sql) içindeki sıfırlı UUID ve örnek e-postayı kendi **hesap kimliğiniz ve e-postanızla** değiştirin; SQL Editor'da çalıştırın. Bu işlem proje sahibinin sunucu yetkisiyle yapılır. Uygulamaya ilk giren rastgele hesap kendini yönetici yapamaz. Değiştirilmemiş örnek UUID gerçek kullanıcıya bağlanamaz.
5. Uygulamada **Ekipleri Ve Davetleri Yenile → İlk Yönetici Olarak Ekip Oluştur** yolundan ekip adını girin. Sunucu hem doğrulanmış e-postayı hem önceden tanımlanan UUID'yi kontrol eder.
6. Ortak ekip başlangıçta boştur. Eski verileriniz bu telefondaysa **Telefondaki Yerel Listeyi İlk Kez Paylaş** düğmesiyle personel/hafta sayılarını kontrol edip paylaşın. Eski kişisel yerel dosya korunur. Bu düğme ortak ekibin sürümü 0 iken görünür; dolu ortak listenin üzerine otomatik aktarım yapılmaz.

## 4. Diğer dört telefon ve yöneticiler

Yönetici **Kullanıcıları Ve Yetkileri Yönet → Kullanıcı Davet Et** bölümünden e-posta ve rol seçer. **Yönetici Olsun** işaretliyse kişi listeyi ve kullanıcı yetkilerini yönetebilir; işaretli değilse yalnız görüntüler. Yönetici atama/davet işlemleri sunucuda da kontrol edilir. Her yönetici başka yönetici atayabilir. Son yönetici kaldırılamaz veya kullanıcıya dönüştürülemez.

Davet işlemi ekibe katılım iznini kaydeder; uygulama davet e-postası göndermez. Diğer kişi aynı APK'ya aynı Project URL/public anahtarını girer, davet edilen e-postayla kayıt olup kodla doğrular ve **Ekipleri Ve Davetleri Yenile** düğmesine basar. Davet yalnız doğrulanmış aynı e-posta hesabınca kabul edilir. Var olan üyenin rolü davetle değil kullanıcı listesinden değiştirilir. Bekleyen davetler iptal edilebilir; yönetici yetkisi kaldırılan kişinin henüz kabul edilmemiş davetleri iptal edilir.

Personel kayıtları ile giriş hesapları farklıdır: birinin nöbet personeli olması kendiliğinden hesap/ekip yetkisi vermez. Bir hesabın ekipten çıkarılması geçmiş nöbet personelini veya istatistiklerini silmez. Üye hesaplar ortak verilerin tamamını okuyabilir: personel, mazeret/kısıt, görev yeri, onaylı hafta ve tatil ayarları. Kullanıcı rolü bu verileri değiştiremez.

## 5. Güncellemeler ve çakışma

Yönetici kaydedince üstte **Sunucuya Gönderiliyor** görünür. Sunucu kabul edince ekip/sürüm bilgisi güncellenir. Diğer telefonlar uygulama açıkken yaklaşık **10 saniyede bir** sürüm ve yetki kontrolü yapar; değişmiş liste indirilir. Arka planda sürekli bağlantı kurulmaz; uygulama açıldığında tekrar kontrol edilir. **Ortak Listeyi Yenile** ile elle kontrol edilebilir. Bu sürüm WebSocket/Realtime yerine ön planda sürüm sorgulaması kullanır.

İki yönetici aynı sürümden değişiklik hazırlarsa ilk kayıt kabul edilir. Diğerinin eski sürümü sessizce üzerine yazılmaz. Bekleyen değişiklik şifreli cihaz kopyasında tutulur; ortak listeye uygulanmış gibi gösterilmez. Bağlantı hatasında **Bekleyen Değişikliği Yeniden Gönder** kullanılabilir. Sunucunun kabul yanıtı kaybolursa aynı işlem kimliğiyle tekrar gönderim ikinci kez işlem oluşturmaz.

Çakışmada **Yerel Kopyayı Arşivle Ve Sunucudakini Aç**, açık teyitten sonra bekleyen kopyayı şifreli cihaz arşivine saklayıp sunucudaki listeyi açar. Otomatik birleştirme veya zorla üzerine yazma yoktur. Arşivler uygulamanın özel alanındadır; bu sürümde arşiv geri yükleme ekranı yoktur. Yarım kalan taslak varken daha yeni sunucu sürümü gelirse taslak korunur; kaydetmek eski sürümü otomatik geçerli kılmaz.

Çevrimdışıyken daha önce indirilmiş ortak liste okunabilir. Ortak listeye yeni değişiklik için çevrimiçi yönetici yetkisi gerekir. Oturum yenilenemiyorsa aynı hesapla **Oturumu Yenile • Yeniden Giriş** yapılabilir; bekleyen değişiklik korunur. Çıkış bekleyen değişiklik çözülmeden yapılmaz. Çıkışta kişisel yerel liste açılır; bu liste ortak sunucuyu değiştirmez.

## Güvenlik ve doğrulama sınırı

- Sunucu işlemleri ekip üyeliğini ve rolünü `auth.uid()` ile kontrol eder. İstemci/hesap metadata'sındaki “admin” metni yetki sayılmaz.
- RLS açıktır; anonim ve oturum açmış kullanıcıların tablolara doğrudan yazması kapalıdır. Yazma/davet/rol işlemleri izin kontrollü RPC üzerinden yapılır.
- Sunucuya paylaşılmış liste HTTPS ile gider. Açık HTTP ve başka alan adları reddedilir; sistem TLS doğrulaması ve yönlendirme yasağı kullanılır.
- Şifre yalnız isteğin belleğinde kullanılır. Oturum/yenileme anahtarları ve ortak cache Android Keystore anahtarıyla AES-GCM şifrelenir. Kişisel veriler/anahtarlar günlüğe yazılmaz. Public proje anahtarı uygulama ayarıdır; veri erişimi ayrıca kullanıcı oturumu + üyelik gerektirir.
- Uygulamayı kaldırmak cihaz anahtarını ve yerel kayıtları kaldırabilir. Önceki sürümün üzerine **kaldırmadan güncelleyin**. Ortak veri ancak sunucuya kabul edilmişse oradadır; PDF veri yedeği değildir.
- RPC veri belgesi üst sınırı 2 MiB'dir. Davet/rol ve kayıt çakışmaları PostgreSQL'de test edildi. Android tarafında iki istemci **benzetimi** ile yetki, eşitleme, kayıp yanıt ve çakışma kontrol edildi. Gerçek Supabase Auth/SMTP/Data API ve iki fiziksel cihaz bağlantısı bu teslimde kurulup doğrulanmadı.

Kurulumdan sonra önce sentetik iki hesapla yönetici yazma/kullanıcı görüntüleme, diğer ekibe erişim reddi, iki yönetici çakışması, internet kesintisi ve yeniden giriş akışlarını kendi projenizde doğrulayın. Ardından gerçek listeyi paylaşın. Sorunlarda Project URL/public anahtar türünü, SQL kurulumunu, doğrulanmış hesap UUID'sini, e-posta SMTP/kod şablonunu ve üyelik rolünü kontrol edin; özel anahtar paylaşmayın.

## Yerel sunucu testini yeniden çalıştırma

Docker yüklü bir geliştirme makinesinde, `nobet-cizelgesi` klasöründe:

```sh
docker run --name nobet-db-test --network none -e POSTGRES_HOST_AUTH_METHOD=trust -d postgres:16-alpine
# PostgreSQL hazır olduktan sonra:
python3 backend/tests/run.py nobet-db-test
docker rm -f nobet-db-test
```

**Yalnız boş, geçici test konteynerinde çalıştırın.** Betik test veritabanındaki public/auth/nobet_private şemalarını sıfırlar. Auth UID/hesap tablosu taklidi gerçek Supabase JWT/e-posta hizmetinin doğrulandığı anlamına gelmez. Ağa port açılmaz; dış kullanıcı/mesaj yoktur.
