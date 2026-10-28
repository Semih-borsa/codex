# POS Kasa Yönetim — Build v40 / 1.5.0

## Fotoğraftan ekle

Giriş sekmesindeki ayrı Fotoğraftan ekle düğmesine basın. JPG/PNG/WebP fotoğrafını seçin veya pencerenin fotoğraf alanına sürükleyin. En fazla15MB kabul edilir. Aktif kasayı seçin; işlem tarihi varsayılan dündür. Fotoğrafın üzerindeki tarih otomatik olarak işlem tarihine yazılmaz. Tamam · Fotoğrafı oku düğmesi Windows’un yerel OCR özelliğini çağırır.

Açılan inceleme görünümünde fotoğraf solda, satış kalemleri sağdadır. Her kalemin hangi vadeye gittiği gösterilir:

| Kalem | Vade |
|---|---|
| Kredi kartı yurtiçi satış | Kredi kartı |
| Kredi kartı yurtdışı satış | Kredi kartı |
| Kredi kartı VakıfBank satış | Kredi kartı |
| Worldpuan satış | Kredi kartı |
| **Debit kart yurtdışı satış** | **Kredi kartı** |
| Debit kart yurtiçi satış | Debit kart |
| Debit kart VakıfBank satış | Debit kart |

Kalemler ile fişteki genel toplam ayrı ayrı düzenlenebilir. Her düzeltmede kuruş cinsinden eşleşme yeniden hesaplanır. Fark0 değilse, geçersiz tutar varsa veya kontrol onayı verilmediyse Kaydet kapalıdır. Kaydet işlevi aynı kuralları tekrar doğrular; yalnız düğmenin durumuna güvenmez. Genel toplam pozitif olmalıdır. Fişte bulunmayan kalemleri0 bırakın; okunmayan pozitif tutarı0 sanmayın, fotoğrafla karşılaştırın.

Okunan tarih/POS numarası yardımcı bilgidir. Seçilen tarih veya kasa koduyla farklıysa uyarı görünür; otomatik kasa/tarih değiştirilmez. Fotoğraf ve tutarlar kontrol edilmeden kaydetmeyin. İade/iptalli veya anlaşılmayan raporlar uyarıyla gösterilir; örnek satış fişlerinden farklı belgeler otomatik muhasebeleştirilmemelidir.

Fiş toplamıyla uyuşan doğrulanmış kalemler, mevcut kayıt listesine en fazla iki işlem olarak kaydolur: kredi vadesine tabi toplam ve debit vadesine tabi toplam. İkisi de mevcut Ayarlar’daki vade günlerini kullanır. Debit yurtdışı kredi toplamına eklenir. Debit kaydı bankaya gerçekten yatmış sayılmaz. Bu yol mevcut Toplam POS/Yatan Debit mutabakatı yerine, fotoğrafta ayrımı bilinen kredi/debit satışlarının vadeli kayıtlarını oluşturur; aynı fişi manuel toplam veya ayrı kart girişinden yeniden eklemeyin. Aynı kasa/günde mevcut kayıt varsa fotoğraf kaydında ayrıca tekrar uyarısı çıkar.

İşlem Geçmişi > POS içindeki fiş kaydının düzenle düğmesi fiş penceresini yeniden açar. Kalem/genel toplam kontrolü korunur. Sil düğmesi açık onayla o fişin kredi/debit kayıtlarını birlikte siler. Diğer kayıtlar etkilenmez.

## Windows koşulları ve gizlilik

Windows10/11 ve Windows’un kurulu OCR dili gerekir; Türkçe tercih edilir, yoksa kurulu İngilizce veya başka mevcut dil kullanılır. Dil bileşeni yoksa anlaşılır hata çıkar. Uygulama OCR dili yüklemez, sistem ayarlarını veya güvenlik duvarını değiştirmez. Gerekli bileşen kurulduktan sonra okuma internetsizdir; ücretli API veya bulut kullanılmaz. Kurumun PowerShell çalıştırma politikası engelliyorsa bu özellik hata verebilir; politika otomatik değiştirilmez.

Fotoğraf ana pencerede boyutlandırılıp yalnız yerel OCR işlemine gönderilir. Windows’un yerleşik PowerShell/WinRT OCR API’si kullanılır; dış OCR paketi indirilmez. Fotoğraf okuma sırasında kullanıcı geçici klasörüne yazılır, işlem sonrasında silinmeye çalışılır. Çökme/elektrik kesintisinde geçici dosya kalabilir. Kalıcı finans kayıtlarında ve JSON yedeğinde fotoğraf veya OCR ham metni tutulmaz; yalnız doğrulanan kalemler, genel toplam ve kayıt bağlantıları saklanır.

Bu sürümün Windows OCR fotoğraf okuma doğruluğu gerçek Windows cihazında henüz test edilmedi. Önce test kopyasıyla fiş, kategori ve tutarları kontrol ederek deneyin. Bulanıklık, gölge ve kat izi hatalı okuma oluşturabilir. Toplamın eşleşmesi kategori okumasının mutlaka doğru olduğunu kanıtlamaz; kullanıcı onayı bu yüzden zorunludur. OCR hata verirse geçerli fotoğrafın bulunduğu inceleme ekranında kalemler elle doldurulabilir; yine tam eşleşme gerekir. Yeniden okutmak için fotoğrafı tekrar seçin.

## Manuel giriş ve güncelleme

Mevcut manuel POS giriş bloğu v39 ile kaynak düzeyinde aynıdır; alanlar, kaydetme mantığı ve mevcut toplam modu korunur. V38/V39’un nakit, ödeme özeti ve grafik özellikleri dahildir. Mobil/LAN çalışması eklenmedi.

Tam kaynak ZIP’idir, hazır EXE değildir. pos-app/BUILD.bat Windows paketini oluşturur. Yeni bağımlılık yok; uygulama kimliği com.poskasayonetim.app aynıdır. Güncellemeden önce tam JSON yedeği ve eski program klasörünü saklayın; verileri temizlemeyin.

Kaynak geri dönüş noktası v39’dur. Yeni fiş kayıtları oluştuysa eski sürüm bunları normal kredi/debit satırları olarak görür, fiş bütünlüğü kontrolünü bilmez; eski sürümle bu kayıtları ayrı ayrı düzenlemeyin/silmeyin. Geri dönüşten önce yeni tam yedeği saklayın ve veri kapsamına uygun plan uygulayın. Eski yedekler yeni sürümde okunur.
