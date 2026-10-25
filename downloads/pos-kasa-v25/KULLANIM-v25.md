# Build v25

Önce JSON yedeğinizi alın. Aynı uygulama adı ve kayıt anahtarları korunur.

Yeni kaynak: pos-app/app-source.jsx. Windows derlemek için Node.js 22+ LTS ve pos-app/BUILD.bat kullanın; yönetici olarak açmanız gerekmez. Paketler kilit dosyasından kurulur, testler çalışır ve Windows klasörü üretilir.

Ayarlar: kasa başına Düzenle/Kaydet/Vazgeç korunur. Toplam POS bölümü Yeni İşlem Girişi altındadır. Geçmiş arama, tarih filtresi ve sayfalama kullanılabilir. 0 gün vade aynı güne karşılık gelir.

Yedekleme sistem dosya seçicisini açar. Geri yüklemede değişecek alanlar onaylanır ve işlem öncesi güvenlik dosyası kaydetmeniz istenir. İptal halinde kayıtlar değişmez. Ham kurtarma dosyası normal JSON yedeğiyle aynı şey değildir.

Test ayrıntıları ve Windows kabul listesi SONUC-RAPORU-v25.md içindedir. Windows hedefi derlenmiştir; gerçek Windows cihaz testi henüz yapılmamıştır.
