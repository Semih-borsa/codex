# Güvenlik Ve Bilinen Sınırlar

## Veri Akışı

Uygulamanın INTERNET, rehber veya SMS izni yoktur; sunucu hesabı ve analitik yoktur. Sicil/isim/telefon verileri uygulamanın özel dosya alanında AES-GCM ile saklanır. Keystore anahtarı APK içinde veya dış yedekte değildir. Mesaj geçmişine tam telefon ve mesaj metni yazılmaz; yalnız sicil, şablon adı, durum ve zaman tutulur. Çökme/uygulama günlüklerine kişi ve mesaj yazan bir logger yoktur.

**Onayla Ve Aç** sonrasında telefon ve mesaj, kullanıcının seçtiği WhatsApp/Business ya da tarayıcıya standart `https://wa.me/…?text=…` bağlantısıyla aktarılır. Bu açık kullanıcı eylemidir. Seçilen uygulama/ağ hizmetinin veri işleme kuralları ayrıca geçerlidir. Kullanıcının dosya kaydında bulut depolama sağlayıcısını seçmesi de Android dosya sağlayıcısının kendi aktarımını başlatabilir; uygulamanın arka planda kendi sunucusuna veri gönderdiği anlamına gelmez.

Android dosya seçicisi yalnız seçilen dosyaya erişim verir. XLSX, çalışma sırasında uygulamanın özel önbelleğine geçici kopyalanır; bu geçici kopya şifrelenmiş değildir. Başarılı aktarımda veya Activity kapanışında kaldırılır. İşlem ortasında süreç öldüyse sonraki açılışta bir saatten eski geçici kopyalar temizlenir. Ana rehber ve geri alma dosyası şifrelidir; Android otomatik bulut yedeği/cihaz taşıması kapalıdır.

`FLAG_SECURE` ekran görüntüsü ve görev değiştirici önizlemesini kısıtlar. İsteğe bağlı uygulama kilidi telefonun PIN/desen/parolasını Android'in kendi doğrulama ekranıyla kullanır. Kilit açıkken uygulama arkaya alınınca kişisel veri diyalogları kapatılır ve dönüşte yeniden doğrulama gerekir. Cihaz ekran kilidi olmadan bu özellik etkinleştirilemez. Kök erişimli veya ele geçirilmiş bir cihazı koruma iddiası yoktur.

## Dosya Ve Kapasite Sınırları

- Yalnız XLSX; eski XLS, şifreli Office dosyaları ve CSV içe aktarma bu sürümde yok.
- En fazla 32 MB giriş dosyası, XML parçasında 64 MB, 50 sayfa, bir sayfada başlık dahil 50.001 satır ve 800.000 hücre, 128 sütun. Rehber en fazla 50.000 kişi.
- Kimlik/telefon alanındaki formüller ve 15 hanelik Excel sayı hassasiyetini aşan sayılar güvenli aktarım için reddedilir. Formüller hesaplanmaz. Hücrede yalnız sayısal `123` kaldıysa uygulama bunun önceden `000123` olduğunu bilemez. Sicil/telefonu Excel'de metin tutun.
- Temel yalnız-sıfır maskeleri (ör. `000000`) korunur; tüm Excel tarih/özel biçimlendirme kuralları desteklenmez. İçe aktarma önizlemesini inceleyin.
- En fazla 1000 şablon, şablon/mesajda 10.000 karakter, kişi başına 20 özel alan. Excel dışa aktarımında tüm özel alan adlarının toplamı en fazla 119 ayrı sütun olabilir.
- Kanonik veri/yedek içeriği en fazla 32 MB; yedek açılmadan önce boyut ve GCM doğrulaması yapılır. Yanlış şifre veya bozulma veri değişikliği yapmaz.
- Ana arama ilk 30, rehber araması ilk 50 sonucu gösterir; tüm kayıtlar bellekte aranır. Aramayı daraltarak diğer kayıtları bulun. Kesin sicil doğrudan anahtar aramasıdır; isim benzerliği bir kişiyi otomatik seçmez.
- Aynı isimli kişiler ayrı sicillerle ayrı kalır. Özel kimliklendirme yalnız tam sicildir. Telefonun biçimce geçerli olması, o numaranın doğru kişiye ait veya WhatsApp'ta kayıtlı olduğunu kanıtlamaz; kişi/numara onayı kullanıcıya aittir.

## Kullanım Sorumluluğu Ve Kurtarma

Excel dışa aktarımı şifresizdir; güvenli saklayın. Parolalı `.s360` yedeklerini ve parolayı ayrı güvenli yerlerde tutun. Uygulama içi tek seviyeli geri alma rehber, kişi takibi/notları ve şablonları geri getirir; geçmişi geri sarmaz. İptal/geri alma dış WhatsApp ekranındaki bir mesajı geri çekemez.

Cihaz anahtarı kaybolursa yerel dosya çözülemez. Anahtar geri alma/uzak parola sıfırlama hizmeti yoktur. Taşınabilir yedek kendi şifresiyle başka kurulumda açılabilir. Kişisel verilerin işlenme amacı, yetkiler, saklama süreleri, kişilere ulaşma izni ve KVKK yükümlülükleri kurum tarafından belirlenmelidir; bu uygulama hukuki uygunluk belgesi değildir.

Fiziksel telefon, WhatsApp/Business hesabı ve gerçek sohbet/gönderim doğrulanmadı. Emülatör testleri onay, Intent ve metin/numara yönlendirmesini taklit alıcıyla kontrol eder. WhatsApp sürümü/hesap durumu/bağlantı varsayılanlarına göre sohbet açılışı farklı olabilir. Gönderim doğrulanamıyorsa sistem gönderildi kaydı oluşturmaz; kullanıcı kendi beyanını ayrıca işaretleyebilir.

CRM takip alanları V2 veri biçiminde şifreli kayıtta/yedekte tutulur. V1 okunur, V2 eski uygulamada okunamaz. Takip durumu gönderim kanıtı değildir. Raporlar kalıcı sınırsız muhasebe kaydı değil; mevcut rehber ve son 1000 iletişim kaydından hesaplanan özetlerdir. Kişi silme ve REPLACE içe aktarım kişi takibini de kaldırır.
