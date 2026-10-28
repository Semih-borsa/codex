# POS Kasa Yönetim — Build v39 / 1.4.3

Build v38 üzerine yalnız ödeme tutar özeti geliştirmesidir. Mobil uygulama/LAN senkronu eklenmedi.

Özet > Aylık Ödeme Takibi bölümünde GENEL TOPLAM, ÖDENMİŞ ve ÖDENECEK tüm kayıtlı aylar için görünür. Her ayın mevcut toplamı korunur; hemen altında o ayın ÖDENMİŞ ve ÖDENECEK tutarları ayrıca görünür. Ay kapalıyken de bu tutarlar okunabilir. Ödenmiş yeşil, ödenecek turuncudur. Bütün ödemeler tamamlandığında ÖDENECEK 0,00 TL olarak görünmeye devam eder.

Ödendi işaretleme genel/aylık toplamı değiştirmez; tutarı ödenecekten ödenmişe geçirir. Geri alma tersini yapar. Tutar düzenleme, silme ve son ödeme tarihini başka aya taşıma ilgili özetleri günceller. Hesaplar kayıtlı son ödeme ayına göre gruplanır. Yeni banka hareketi veya otomatik ödeme oluşturulmaz.

Kayıt şeması ve yedekler değişmedi. V38’in iki varsayılan nakit satırı, Kasa ekle, boş0 tutar, kapalı NAKİT/POS geçmişi ve dağılım halkası korunmuştur.

Bu ZIP tam kaynak paketidir, hazır EXE değildir. pos-app/BUILD.bat ile Windows paketi oluşturulur. Uygulama kimliği com.poskasayonetim.app aynı; yeni bağımlılık yoktur. Güncellemeden önce mevcut JSON yedeğini ve program klasörünü saklayın. Verileri temizlemeyin. Geri dönüş için v38 kaynak/program sürümü kullanılabilir; v38 ile veri şeması aynıdır. Daha eski sürümlerin sıfır nakit kaydını tanımama sınırı KULLANIM-v38.md’de anlatılır.
