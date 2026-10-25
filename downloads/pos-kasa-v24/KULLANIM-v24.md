# Build v24 — güncel kullanım

Bu sürümde yalnız istenen arayüz ve kasa düzenleme akışı değiştirildi.

- GİRİŞ ekranı: Yeni İşlem Girişi → Toplam POS / Yatan Debit Takibi → İşlem Geçmişi sırası kullanılır. Toplam POS bölümü daha küçük yazı, alan yüksekliği ve boşluklarla düzenlendi.
- Ayarlar: her kasa ayrı kilitli satırdır. Düzenle'ye basınca yalnız o satırın adı, numarası, bankası, aktifliği ve Toplam POS seçimi açılır.
- Satırdaki Kaydet yalnız o kasayı hemen kalıcı kaydeder ve alanları kilitler. Vazgeç o satırdaki değişikliği iptal eder. Diğer satırlar etkilenmez.
- Vade, yeni kasa ekleme, banka ve diğer genel ayarlar için alttaki genel KAYDET devam eder. Açık bir satırdaki henüz kaydedilmemiş taslak, genel Kaydet ile uygulanmaz.
- Tarihsel işlem bağlantıları kasa kimliğiyle korunur. Pasif kasa ve kasa bazında Toplam POS seçimi v23 ile aynı mantıkta çalışır.

Paket/appId aynı; sürüm 1.0.2. Mevcut JSON yedeğinizi alın, uygulamanızın verilerini temizlemeyin. Windows ortamında mevcut BUILD.bat yöntemini kullanın. Bu ZIP kaynak projedir; Windows EXE derlemesi yapılmadı.

17 hesaplama kontrolü, JSX derlemesi ve Chromium akış kontrolü başarılı: toplam/debit, kaydı güncelleme, bölüm sırası, kilitli kasa, Düzenle/Kaydet/Vazgeç, pasif/normal kasa ayrımı ve yeniden açılışta kayıtların korunması. Sentetik veriler kullanıldı. Gerçek Windows/Electron üzerinde son kontrol gereklidir.

Genel analiz bulguları ayrı rapordadır ve bu sürümde uygulanmadı. KULLANIM-v23.md önceki sürümün tarihsel belgesidir; güncel düzenleme davranışı bu dosyada açıklanır.
