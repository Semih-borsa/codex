# Sicil360 Pro 1.2 — Android

Excel rehberinden sicille kişi bulma ve **kendi düz metin şablonunuzu** onayla WhatsApp sohbetine hazırlama. Mesaja sicil, ad, soyad veya başka veri kendiliğinden eklenmez. `{AD}` gibi ifadeler de aynen yazı olarak kalır.

[APK indir](../downloads/Sicil360Pro-1.2.apk) · [Android Studio kaynak paketi](../downloads/Sicil360Pro-Android-Studio.zip) · [Excel şablonu](examples/Kisi-Sablonu.xlsx) · [Mesaj örnekleri](examples/Mesaj-Ornekleri.txt)

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
