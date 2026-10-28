# POS Kasa Yönetim — Build v37 / 1.4.1

## İşlem Geçmişi

NAKİT ve POS iki ayrı açılır listedir. Giriş sekmesi açıldığında ikisi de kapalıdır; istediğiniz başlığı açabilirsiniz. Her başlık filtreye uyan kayıt sayısını gösterir. Ortak arama/tarih filtreleri iki listeye uygulanır. Her listenin 50 kayıtlık sayfalaması bağımsızdır.

NAKİT listesindeki kalem düğmesi kaydı yukarıdaki Kasadan Gelen Nakit alanına getirir. Tutar, kasa veya tarihi değiştirip Nakdi güncelle düğmesine basın. Vazgeç değişiklikleri kaydetmez. Sil düğmesi onay ister, yalnız seçilen nakit kaydını siler. POS düzenleme ve bağlı debit silme işlemleri önceki gibi korunmuştur.

## POS / NAKİT DAĞILIMI

Özet ekranındaki yeni halka mavi POS, yeşil nakit satışlarını gösterir. Ortada ikisinin toplam tutarı, yanında her türün tutarı ve yüzdesi vardır; bu kartta kasa ayrıntısı tekrarlanmaz.

Bu hafta, Geçen hafta, Bu ay, Geçen ay, Bu yıl ve özel başlangıç/bitiş aralığı seçilebilir. Tarihler gün.ay.yıl olarak gösterilir. Bu hafta mevcut mantıkla bugün dahil son 7 gün, geçen hafta ondan önceki 7 gündür; Bu ay ay başından bugüne, Geçen ay önceki takvim ayıdır.

Dağılım satış/işlem tarihine göredir. Toplam POS takibi kayıtlarında toplam POS tutarı bir kez sayılır; ona bağlı kalan kredi ve yatan debit tekrar eklenmez. Manuel ayrı kredi/debit satış kayıtları kendi tutarıyla dahil olur. Nakit, girilen işlem tarihinden dahil olur. Örneğin toplam POS 800 TL, bunun yatan debiti 200 TL ve ayrı nakit 200 TL ise satış toplamı 1.000 TL; dağılım %80 POS / %20 nakittir. Sonradan debit yatması dağılımdaki toplam satışı artırmaz. Mevcut POS gelir analizi yatış tarihine göre çalışmaya devam eder.

Boş aralıkta toplam0, yüzdeler çizgi ve Kayıt yok görünür. Geçersiz tarih aralığı uygulanmaz, önceki sonuç korunur. Yüzdeler ekranda bir ondalığa yuvarlanır ve toplamı100 olacak şekilde gösterilir.

## Güncelleme ve güvenlik

Tam kaynak ZIP’idir, hazır EXE değildir. pos-app/BUILD.bat ile Windows paketi oluşturulur. Uygulama kimliği com.poskasayonetim.app ve ürün adı aynıdır; yeni bağımlılık yoktur. Önce mevcut programdan JSON yedeği alın ve eski program klasörünü saklayın. Kullanıcı verilerini temizlemeyin. Ayrı nakit saklama ve JSON yedek kuralları v36 ile aynıdır. Üretim verileri dönüştürülmedi.

Geri dönüş için v36 program klasörünü kullanabilirsiniz; nakit şeması değişmediği için nakit kayıtları v36’da da okunur. Güncellemeden/geri dönüşten önce her zaman son tam yedeği saklayın.
