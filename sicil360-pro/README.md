# Sicil360 Pro 1.6 — Android

Excel rehberinden sicille kişi bulma ve **kendi düz metin şablonunuzu** onayla WhatsApp sohbetine hazırlama. Mesaja sicil, ad, soyad veya başka veri kendiliğinden eklenmez. `{AD}` gibi ifadeler de aynen yazı olarak kalır.

[APK indir](../downloads/Sicil360Pro-1.6.apk) · [Android Studio kaynak paketi](../downloads/Sicil360Pro-Android-Studio.zip) · [Excel şablonu](examples/Kisi-Sablonu.xlsx) · [Mesaj örnekleri](examples/Mesaj-Ornekleri.txt)

Android 8.0 ve üzeri, paket `com.semih.sicil360`, sürüm kodu 1. Telefonun kendi ekran kilidiyle isteğe bağlı uygulama kilidi; açık/koyu/sistem teması. APK kişisel yükleme için geliştirme sertifikasıyla imzalanmış release derlemedir; debugger erişimi kapalıdır. Mevcut diğer uygulamalarınızdan ayrı bir uygulamadır.

## İlk Kullanım

1. APK'yı telefona indirin, Android'in indirdiğiniz uygulamayı açma/yükleme ekranından kurun. Uygulama ilk açılışta boş rehberle gelir.
2. **Rehber → Excel İçe Aktar.** XLSX dosyasını ve ilgili sayfayı seçin.
3. Başlık satırını kontrol edin. Sicil, Ad/Ad Soyad ve Telefon sütunlarını eşleştirin; Soyad ve diğer alanlar isteğe bağlıdır. Başlık farklıysa öneriyi değiştirebilirsiniz. Aynı sütun iki alana bağlanamaz.
4. Varsayılan **Koru** mevcut sicili değiştirmez. **Güncelle** eşleşen sicili günceller; eşleştirilmeyen isteğe bağlı alanlar korunur. **Listeyi Değiştir** eski liste yerine yeni liste oluşturur ve kaldırılacak eski kayıt sayısını gösterir.
5. Önizlemede ekleme/güncelleme/atlama/hata/silme sayılarını inceleyin. Aynı sicil birden çok satırdaysa satırı kendiniz seçin veya tümünü atlayın. Hiçbiri sessizce seçilmez. Hatalı satırlar aktarılmaz; Excel'deki satır numarasıyla bildirilir. **Onayla Ve İçe Aktar** ve son onayı tamamlayın.
6. **Şablonlar → Şablon Oluştur:** isim, kategori ve istediğiniz mesajı yazıp kaydedin. Düzenleme, çoğaltma, silme ve son kullanım zamanı vardır.
7. **Ara:** sicili tam olarak yazın ve çıkan kişiyi seçin. İsim/telefon araması yalnız sonuç listesi sunar; kişi kendiliğinden seçilmez. Baştaki sıfırlar kimliğin parçasıdır: `000123` ile `123` ayrı sicillerdir; harfli sicillerde büyük/küçük harf de ayrıdır.
8. Kişi kartından adı ve telefonu kontrol edin. Biçim göstergesi yalnız numara biçimini doğrular; WhatsApp hesabının varlığını doğrulamaz.
9. Şablon seçin veya **Özel Mesaj** kullanın. Önizlemedeki metni her kişi için değiştirebilirsiniz. **WhatsApp'ta Aç** ile kişi, sicil, numara ve metni son kez birlikte onaylayın.
10. Normal WhatsApp ve Business birlikte kuruluysa seçim sunulur. Hiçbiri yoksa tarayıcı seçeneği sunulur. Son **Gönder** düğmesine WhatsApp'ta siz basarsınız. Uygulama kendiliğinden mesaj göndermez.
11. Dönüşte sonucu kendiniz işaretleyin. “Gönderim Tamamlandı” bile kullanıcı beyanıdır; sistemin WhatsApp gönderim teyidi değildir.

[Excel şablonu](examples/Kisi-Sablonu.xlsx) kişisel veri içermez. Sicil ve telefon hücreleri metin biçimindedir. Kendi verilerinizi **Kişiler** sayfasına yazın; diğer **Kullanım** sayfası açıklamadır. Uygulamadaki örnek Excel kaydetme düğmesi de kullanılabilir; örnek kişinin telefonu bilerek boştur ve doldurulmadan aktarılmaz.

## Kayıt, Sıralı İşlem Ve Geçmiş

Rehberde kişi ekleyebilir, kalem simgesinden düzenleyebilir ve silebilirsiniz. Sicil düzenleme sırasında değişmez; yanlış sicili düzeltmek için eskiyi silip doğru sicille yeni kayıt oluşturun. Telefon boş bırakılabilir, ancak mesaj açılamaz; geçersiz telefon elle kaydedilmez. Birim, unvan, e-posta, açıklama, durum ve en fazla 20 özel alan (`Anahtar=Değer`) desteklenir.

Rehberde kutularla kişi seçip **Seçilenlerle Sıralı İşleme Başla** kullanın. Aynı sicil kuyruğa iki kez eklenmez. En fazla 500 kişi; her kişi için ayrı önizleme/onay gerekir. **Sonraki Kişi** otomatik göndermez ve önceki kişinin gönderildiğini varsaymaz. Kuyruğu iptal edebilirsiniz. Kuyruk geçicidir; uygulama işlemi sonlandırılırsa yeniden seçilir.

İşlem geçmişi en fazla 1000 kayıt tutar, 100'er kayıtla sayfalanır. Sicil, şablon adı, zaman ve durum saklanır; tam telefon veya o mesajın metni saklanmaz. Şablon metinleri ise şablon yönetiminin parçası olarak saklanır.

## Yedekleme Ve Geri Alma

- **Ayarlar → Şifreli Yedek Oluştur:** en az 10 karakterlik şifre belirleyin ve `.s360` dosyasını güvenli bir yere kaydedin. AES-256-GCM ve PBKDF2-HMAC-SHA256 (210.000 tur) kullanılır. Şifreyi unutursanız yedek açılamaz.
- **Şifreli Yedekten Geri Yükle:** dosya ve şifre doğrulanır, kişi/şablon sayıları gösterilir. Son onaydan sonra mevcut rehber/şablon/geçmiş yedekle değiştirilir. Yanlış şifre/bozuk dosya mevcut kayıtları değiştirmez.
- **Son Veri Değişikliğini Geri Al:** son kişi, şablon veya aktarım öncesindeki rehber/şablonlara döner. Sonraki işlem geçmişi korunur; geri alma tek seviyedir. Taşınabilir şifreli yedeğin yerine geçmez.
- **Excel Dışa Aktar** sadece rehberi aktarır ve **şifresizdir**. Telefonlar/siciller metin hücreleridir. Şablon/geçmiş için şifreli yedek kullanın.
- **Tüm Kayıtları Sil** rehberi, şablonları, geçmişi ve yerel geri alma kopyasını kaldırır. Dışarı kaydettiğiniz dosyalar ayrıca sizin tarafınızdan silinmelidir.
- Uygulamayı kaldırmak cihaz verilerini ve yerel şifreleme anahtarını siler. Önceden parola korumalı yedek alın. Yerel dosya/anahtar bozulursa koruma amacıyla düzenleme kapatılır; verilerin üzerine yazılmaz. Böyle bir durumda uygulamayı silmeden önce hata durumunu değerlendirin. Anahtar kaybında yerel şifre çözülemez; taşınabilir yedek gerekir.

## Güvenlik, Sınırlar Ve Doğrulama

Veri saklama, geçici Excel dosyaları, uygulama kilidi, dış entegrasyon ve desteklenen dosya sınırları: [Güvenlik ve bilinen sınırlar](docs/GUVENLIK-VE-SINIRLAR.md).

[Derleme ve mimari](docs/DERLEME.md) · [Test sonuçları](docs/TESTLER.md) · [Açık ekran](docs/screenshots/light-0.png) · [Koyu ekran](docs/screenshots/dark-0.png).

**Gerçek WhatsApp/Business istemcisi, hesap kaydı ve fiziksel telefon uçtan uca gönderimi henüz doğrulanmadı.** Android'de onay sırası, numara/metin ve normal/Business/tarayıcı intent yönlendirmesi gönderim yapmayan test arayüzüyle doğrulanır. WhatsApp sürümü, numaranın kayıtlı olup olmaması ve cihazın bağlantı eşleştirmesi ekran davranışını etkileyebilir. İlk gerçek kullanımda kendi kontrolünüzde tek bir kişiyle doğrulayın.

## 1.1 Tasarım Güncellemesi

Açık temada Petrol & Mint, koyu temada Grafit & Zümrüt renkleri kullanılır. WhatsApp simgeli yeşil geçişli açma düğmesi alt menünün üstünde sabit kalır. Şablon metinleri ve kişi doğrulama/onay akışı korunur. Güncelleme aynı paket ve sertifikayla mevcut kurulumun üzerine yüklenebilir; uygulamayı kaldırmayın.

## 1.2 Şablon Seçimi

Mesaj hazırlama ekranında şablonlar açılır liste yerine alt alta gösterilir. Sağdaki seçim düğmesi veya satıra dokunarak tek şablon seçilir; metni düzenlenebilir önizlemeye gelir. Seçili şablona tekrar dokunmak düzenlenen metni sıfırlamaz. Özel Mesaj seçeneği mevcut metni korur.

## 1.3 Kişi Takibi Ve Raporlar

- Tam sicil girildiğinde kişi kartı otomatik açılır; isim veya kısmi telefon eşleşmesi otomatik kişi seçmez.
- Kişi kartındaki **Takip Ve Notlar** bölümünde İşlem Bekliyor / Tekrar İletişim / Tamamlandı durumunu ve kişi notunu kaydedin.
- Alt menü **Takip → Takip**: Duruma ve sicil/ad soyada göre filtrelenen, isim sıralı kişi listesi. Notları düzenleyin veya kişiye mesaj hazırlayın.
- **Takip → Geçmiş**: Sicil, mevcut kişi adı, hazırlanırken kaydedilmiş şablon adı, hazırlama zamanı ve kullanıcı tarafından işaretlenen durum. Kişi kartından yalnız o kişinin geçmişine geçilebilir. Mesajın tam içeriği geçmişe kaydedilmez.
- **Takip → Raporlar**: Bugün hazırlanan mesajlar, bunlardan gönderildi işaretlenenler, bugün tamamlandı durumuna alınmış ve halen tamamlanmış kişiler; toplam, bekleyen, tekrar iletişim ve tamamlanan kişi sayıları.

Takip durumu, Excel'deki Durum alanından ayrıdır. Yeni/eski kişiler ilk geçişte İşlem Bekliyor olarak başlar. Gönderimi işaretlemek kişi takibini otomatik tamamlamaz; kişi işlemini ayrıca tamamlayın. Kişi takibi ve notlar şifreli yerel kayıtta ve `.s360` yedekte saklanır. Excel güncelleme modu takip durumunu korur; kontrollü yeniden içe aktarma (REPLACE) rehberi değiştirdiği için takip durumları sıfırlanır. Excel dışa aktarımı mevcut rehber alanlarını ve notları içerir; CRM takibini bütünüyle taşımak için şifreli yedek kullanın.

Günlük mesaj sayıları son 1000 geçmiş kaydıyla sınırlıdır; geçmişi temizlemek raporu etkiler. Kişi sayıları güncel rehber ve güncel takip durumlarından hesaplanır. Önceki sürüm yedekleri açılabilir; yeni yedeği eski uygulama sürümleri açamaz. Güncellemeyi kaldırmadan üzerine kurun.

## 1.4 Veresiye Borç Ve Ödeme Takibi

**Rehber → Borç Ve Ödemeler** veya **Takip → Ödemeler** bölümünü açın. Borç listesini yükleyin. İlk satır başlık, sütunlar A Sıra No / B Cari Kodu / C Ticari Unvanı / D Adı / E Soyadı / F Bakiye olmalıdır. Standart Excel yükleme de bu başlıkları tanıyıp borç önizlemesine geçer. Cari kod metin olarak saklanır; telefon gerekmez. Bu liste mesaj rehberinden ayrıdır; cari kod otomatik sicil veya telefon kabul edilmez.

- Önizlemede kişi sayısını ve toplamı kontrol edip listeye dönem adı vererek onaylayın. Orijinal Excel değiştirilmez.
- İlk Listenin Genel Toplamı ödemelerden bağımsız, yüklenen satırların sabit toplamıdır. Excel'deki Genel Toplam satırı kişi olarak içe alınmaz. Sabit toplam verilmişse satır toplamlarıyla karşılaştırılır; formüllü toplam yerine satırlardaki gerçek bakiyeler hesaplanır.
- Her kişi kartından **Ödeme Gir**: Tutar girin ya da **Tamamı Ödendi** seçin; son onaydan sonra kaydedilir. Tamamı Ödendi, varsa önceki kısmi ödemeden sonra kalan tutarı kapatır.
- Ödenen tutar geçerli ödeme girişlerinin toplamıdır. Kalan Borç, herkesin sıfırın altına düşmeyen kalan borçlarının toplamıdır. Fazla ödeme başka kişinin borcunu azaltmaz; ayrıca gösterilir. Örnek: 1.000 TL borca 1.100 TL ödeme → kalan 0 TL, fazla ödeme 100 TL.
- Borcu kapanan kişi **aynı listede ve aynı sırada** kalır. Bilgileri soluk ve Tamamı Ödendi görünür; detayı açılabilir. Ayrı pasif listeye taşınmaz. Uzun metinler satıra bölünür; tutarlar otomatik küçülür. Yatay kaydırma yoktur.
- Yanlış ödemeyi ödeme geçmişinden son onayla iptal edin. Kayıt silinmez, İptal Edildi kalır; tutarlar yeniden hesaplanır. Borç yeniden açılırsa kişi normal görünür.

[Boş borç Excel şablonu](../downloads/Sicil360Pro-Borc-Sablonu.xlsx). Her listede en fazla 50.000 kişi, toplam 100 liste; kişi başına 10.000 ödeme, detayda son 100 ödeme gösterilir. Saklama/yedek toplam sınırı 32 MB. Mükerrer cari kod, eksik ad, negatif bakiye, iki kuruş basamağını aşan değer veya hücre formülleri hatayla durdurulur; veri sessizce atılmaz. Cari kod/bakiye formüllerini Excel'de değer olarak kaydedin. Aynı ay/yılda aynı cari kodlar ve aynı ilk bakiyelerden oluşan yeniden yükleme mevcut listeyi açar; ödemeleri sıfırlamaz. Başka listeler Liste Seç ile açılır; eski listeler korunur.

Ödeme verileri yerelde şifreli ve `.s360` yedekte saklanır. Rehber geri alma işlemi ödemeleri geri sarmaz; ödeme hatası kendi geçmişinden iptal edilir. Tüm yedeği geri yükleme ve tüm verileri silme, ödeme listelerini de etkiler. Mevcut rehber Excel dışa aktarımı ödeme defterini içermez; eksiksiz taşımak için şifreli yedek kullanın. Bu kayıtlar kullanıcının girdiği ödeme beyanıdır; banka tahsilat doğrulaması veya otomatik para transferi yapılmaz.

### Aylık Geçmiş Ve Uyarılar

İçe aktarma önizlemesinde **Listenin Ait Olduğu Ay** ve **Yıl** seçilir. Ay/yıl dosyanın gerçek dönemi olmalıdır; yükleme tarihi tek başına dönem sayılmaz. Farklı aylarda aynı kişiler ve aynı bakiyeler bulunabilir. Aynı dönemde bir cari kod ikinci kez borçlandırılmaz; farklı dosyada çakışma varsa hata gösterilir.

Kişi kartları ve ödeme detayları cari koduyla geçmişi karşılaştırır:

- **Son 3 Ayda X Farklı Ayda Listede:** Seçili listenin ayı ve önceki iki takvim ayındaki görünme sayısıdır. Eksik aylara veri uydurulmaz.
- **En Az 3 Ardışık Dönemde Ödeme Kaydı Girilmemiş:** Kişi art arda aylarda pozitif borçla kayıtlı ve o dönemlerde hiçbir etkin ödeme yoksa gösterilir. Yeni liste yüklenince bu kişiler için toplu uyarı çıkar. Bu ifade gerçek dünyada ödeme yapılmadığının kanıtı değildir.
- **Yüklenen Dönemlerde Borcun %X Kadarı Ödenmiş:** Seçili ay dahil geçmiş dönemlerin bakiye toplamına göre ağırlıklı oran. Her dönem için ödenen miktar, o dönemin ilk borcuyla sınırlandırılır; fazla ödeme oranı şişirmez. Kısmi ödeme, hiç ödeme yapılmamasından ayrılır.

Bu hesaplar yalnız yüklenen aylara ve uygulamaya girilen ödemelere dayanır. Gelecek tarihli listeler geçmişe dahil edilmez. Aylık bakiye görüntüleri otomatik olarak yeni borç veya devir borcu diye birleştirilmez; ekrandaki toplamlar seçili listeye aittir. Eski borcun başka aya taşındığı dosyalarda geçmiş oranını dönem görüntülerine göre yorumlayın; toplam gerçek alacak olarak kullanmayın. Cari kod değişirse uygulama aynı kişi olduğunu isimden tahmin etmez.

## 1.5 Borç Kaydı Ve Liste Düzenleme / Silme

- **Takip → Ödemeler → Listeyi Düzenle / Sil:** Liste adı/dönemi değiştirilebilir. **Bu Listenin Tamamını Sil** seçilirse yalnız o liste, kişileri ve o listeye ait ödeme geçmişi son onayla silinir. Diğer dönemler ve ana rehber korunur. Deneme için yüklenen listenin tamamını buradan kaldırabilirsiniz.
- Her borç kartındaki **Düzenle / Sil:** Cari kod, ad, soyad, ticari unvan ve ilk borç düzeltilebilir. Ödeme geçmişi korunur, kişi aynı sırada kalır. İlk borcun elle düzeltilmesi liste genel toplamını değiştirir; bu normal ödeme girişinden farklıdır. Normal ödeme girişi ilk toplamı yine değiştirmez.
- **Bu Kaydı Sil:** Seçili kişi ve bu listedeki ödeme geçmişi son onayla silinir. Diğer aylardaki aynı cari kod ve rehber etkilenmez. Kalan kişiler yer değiştirmez.
- Silme/düzeltme sonrası toplamlar ve geçmiş analizleri mevcut kayıtlardan yeniden hesaplanır. Cari kod değiştirildiğinde geçmiş aylardaki eski kod otomatik değiştirilmez. Aynı dönemde mükerrer cari oluşturulamaz; dönem değişikliği de çakışma açısından denetlenir.

Silinen kişi/listenin ödemeleri de kaldırılır; silme işlemi uygulama içi geri alma ile geri getirilemez. Gerçek kayıtları değiştirmeden önce Ayarlar'dan şifreli yedek alın. Yanlış ödeme girişi için kişi silmek yerine ödeme geçmişindeki iptal işlevini kullanın. Özgün Excel dosyası değiştirilmez.

## 1.6 Uygulama İçinden Borç Excel Şablonu

**Takip → Ödemeler → Borç Excel Şablonu İndir** düğmesine dokunun ve telefonda kaydedilecek konumu seçin. Boş şablon uygulamada gömülüdür; internet gerekmez. A Sıra No, B Cari Kodu, C Ticari Unvanı, D Adı, E Soyadı, F Bakiye başlıkları hazırdır. Cari kod sütunu metindir; baştaki sıfırlar korunabilir. Kişisel veya örnek kişi kaydı bulunmaz. Excel uygulamanızda doldurup **Borç Excel Listesi Yükle** ile içe aktarın. Şablon kaydetmek mevcut kişileri, listeleri veya ödemeleri değiştirmez.
