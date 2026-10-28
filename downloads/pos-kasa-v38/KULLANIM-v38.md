# POS Kasa Yönetim — Build v38 / 1.4.2

Nakit girişi varsayılan iki satırdır: Ayarlar’da aktif KASA 1 ve KASA 2 varsa onlar seçilir. İsimleri değişmişse mevcut aktif kasalardan farklı iki kasa seçilir; kayıtlı kasa icat edilmez. Aktif kasa sayısı yetersizse eksik satırda Kasa seçin görünür. İki tarih de varsayılan dündür; listeden kasa, alanlardan tarih ve tutar değiştirilebilir.

Kasa ekle üçüncü ve sonraki satırları açar; varsa henüz seçilmemiş aktif kasa varsayılan seçilir ve tarih yine dündür. Eklenen fazla satırlar X ile kaldırılabilir; bu işlem yalnız giriş taslağını kaldırır, kayıt geçmişini silmez.

Tek Nakdi kaydet düğmesi tüm satırları birlikte kaydeder. Boş/yalnız boşluk tutarı 0,00 TL olarak kayıt olur; atlanmaz. Geçerli kasa ve tarih yine zorunludur. 3053,50 tutarı 3.053,50 gösterilir. Bir satır bozuk/negatif/fazla ondalıklı/gelecek tarihli ise hiçbir satır kaydedilmez, satır numarasıyla hata gösterilir. Yazma hatasında tüm giriş taslağı ve eski kayıtlar korunur. Hızlı çift tıklama ikinci grup kayıt oluşturmaz.

Başarılı kayıttan sonra bütün tutarlar temizlenir ve tarihler düne döner. Seçili kasalar ve eklenen satır sayısı giriş ekranı açıkken korunur; yeni açılışta varsayılan iki kasa gelir. Boş tutarla yeniden Kaydet’e basmak yeni sıfır kayıtları ekler.

İşlem Geçmişi > NAKİT listesindeki kalem yalnız o kaydı düzenler; bu sırada çok satırlı kaydedilmemiş giriş taslağı korunur. Nakdi güncelle/Vazgeç sonrasında çok satırlı giriş geri gelir. Düzenlenen tutar da boş bırakılırsa sıfır kaydedilir. Silme onaylı ve kayıt bazındadır. POS kayıtları ve dağılım hesabı değişmez.

JSON yedeklerine sıfır nakit kayıtları da dahildir ve bu sürümde geri yüklenir. Sıfır, negatif/bozuk tutardan ayrılır. Eski geçerli yedekler desteklenmeye devam eder; nakit alanı olmayan eski yedek mevcut nakdi silmez.

## Güncelleme ve geri dönüş

Tam kaynak ZIP’idir, hazır EXE değildir. pos-app/BUILD.bat Windows x64 paketi oluşturur. Uygulama kimliği com.poskasayonetim.app ve ürün adı korunmuştur. Yeni bağımlılık eklenmedi. Güncellemeden önce mevcut programdan tam JSON yedeği alın, eski program klasörünü saklayın. Verileri temizlemeyin.

v37 ve daha eski sürümler sıfır nakit kaydını kabul etmez; yeni sıfır kayıtları oluşmuşsa doğrudan eski sürümü açmak güvenlik doğrulamasında durabilir. Geri dönüşte yeni tam yedeği ayrıca saklayın; yeni kayıtları silerek çözmeye çalışmayın. Eski sürüme dönüş gerekirse güncelleme öncesi yedeğin kapsamı kontrol edilerek ayrı, onaylı bir geri yükleme planı uygulanmalıdır. Kaynak geri dönüş kopyası v37 değiştirilmeden korunmuştur.
