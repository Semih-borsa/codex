# POS Kasa Yönetim — Build v36 / 1.4.0

- Yeni işlem girişinde Toplam POS takibi işaretli bir kasa seçildiğinde kredi/debit satırları yerine tek Toplam POS tutarı açılır. Aynı Kaydet düğmesi kullanılır. Başarılı kayıttan sonra tutar boşalır; tarih düne döner. Aynı kasa/gün için ikinci toplam önceki kaydı değiştirmez, onayla ayrı kayıt eklenir.
- Alt bölüm yalnız YATAN DEBİT TAKİBİ içerir. Bankaya gerçek yatış tarihi varsayılan bugündür. Mevcut vade/eşleştirme ve kalan kredi hesabı korunur.
- İkisinin arasında KASADAN GELEN NAKİT alanında kasa, işlem tarihi (varsayılan dün) ve tutar girilir. 3053,50 girişi 3.053,50 gösterilir. Başarılı kayıttan sonra tutar boşalır.
- Nakit de İşlem Geçmişi içinde NAKİT olarak görünür. Kalem/düzenle nakit alanına kaydı getirir; Nakdi güncelle yalnız o kaydı değiştirir. Sil düğmesi onayla yalnız seçilen nakit kaydını siler. Ayrı silme ekranı yoktur.
- Özet’te NAKİT GELİR ANALİZİ: Bu hafta, Geçen hafta, Bu ay, Geçen ay, Bu yıl, Özel aralık. Tarihler GG.AA.YYYY. Nakit analizi işlem tarihinden hesaplanır; POS analizi önceki gibi yatış tarihindendir. Ortalamalar seçili aralıktaki takvim günlerini içerir. Haftalık karşılaştırma son 7 gün ve önceki 7 gün; aylık karşılaştırma geçen ayın aynı gün sayısına kadardır.
- POS ortalama, nakit ortalama ve önümüzdeki 7 gün grafik çubukları aynı 32 SVG birimi kalınlığındadır. Başlıklar aynı yazı tipi, 11px boyut, renk ve harf aralığını kullanır.

## Veri ve güncelleme

Uygulama kimliği com.poskasayonetim.app ve ürün adı korunmuştur. Mevcut verileri temizlemeyin. Güncellemeden önce Ayarlar > Yedek Al ile doğrulanmış JSON kopyasını ayrı klasöre kaydedin ve mevcut program klasörünü saklayın.

Nakit ayrı pos:nakit listesinde saklanır; POS işlemleri, vadeler, beklenen kredi/debit ve mevcut nakit planının banka bakiyesi kendiliğinden değişmez. Nakit, sayım/para kasası bakiyesine otomatik eklenmez; bu alan kasalardan gelen nakit gelirlerinin ayrı takibidir.

Yeni JSON yedeği isteğe bağlı nakit alanını içerir. Eski yedekte nakit alanı yoksa mevcut nakit korunur. Açık nakit:[] yüklemesi nakit listesini boşaltır; bu etki önizlemede kayıt adediyle gösterilir. Güvenlik kopyası, günlük ve hata geri alma sistemi nakdi kapsar. Bozuk/kasa bağlantısı olmayan nakit reddedilir. Eski geri yükleme günlüğü yeni nakit alanını silmez. Yeni yedeği eski uygulamada geri yüklemek nakit alanını tanımaz; yeni nakit kayıtları için bu sürüm veya sonrası kullanılmalıdır.

## Derleme

Bu ZIP tam kaynak paketidir, hazır EXE değildir. pos-app/BUILD.bat önceki gibi Windows derlemesi üretir. Paket yeni bağımlılık eklemez. Node.js 22+ gerekir; otomatik test komutundaki --test-isolation=none için Node sürümünüzün bu bayrağı desteklemesi gerekir (doğrulama Node 24 ile yapıldı). Üretim derlemesi bu test bayrağını kullanmaz.

Geri dönüşte önce yeni tam yedeği saklayın, sonra eski v35 program klasörünü kullanın; verileri temizlemeyin. v35 yeni nakit listesini göstermez. Nakit içeren yedeği v36 ile açın. Eski yedeklere veya gerçek kayıtlara toplu dönüşüm yapılmadı.
