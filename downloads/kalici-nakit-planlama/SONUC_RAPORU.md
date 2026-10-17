# Kalıcı Nakit Akışı Planlama — 1.3.1

## Eklenenler ve kullanım

Ana uygulamada **Nakit Akışı → Planlama Ayarları → Planları düzenle** bölümünü aç. Maaş yatış günü ve başlangıç tarihini belirle; mevcut maaş ayarını kullanabilir veya yalnız tahmin için başka tutar seçebilirsin. Mevcut maaş ayarı değiştiğinde onu kullanan tahmin güncellenir.

Gelir/gider planında açıklama, tutar, başlangıç ve tek sefer/haftalık/aylık/yıllık tekrar seç; **Planı taslağa ekle**, ardından **Kaydet**. Planlar düzenlenebilir, aktif/pasif yapılabilir ve silinebilir. **Vazgeç** taslağı bırakır; geçici senaryo kaydedilmez. Forma yazılıp taslağa eklenmemiş bilgi varsa Kaydet bunu sessizce yok saymaz, uyarır. Önceki geçici gelir alanı da açık Kaydet/Vazgeç seçenekleriyle kullanılabilir. Banka başlangıç bakiyesi geçici bir güncel gözlemdir; kalıcı plan olarak saklanmaz.

Planın tarihli tekrarını aynı olayı temsil eden işlem/ödeme/kredi/ekstre/kart taksiti/sabit gider kaydına açıkça bağlayabilirsin. Bağlı plan ikinci kez uygulanmaz. Sadece aynı tarih ve tutar otomatik eşleştirme yapmaz: hareket belirsiz işaretlenir, toplamda olası çakışma uyarısı görünür. Farklı tarih/tutarlı açık bağlantıda kayıt esas alınır, fark açıklanır. Eksik hedef otomatik silinmez; plan belirsiz korunur. Geçmiş tek seferlik plan otomatik bugüne taşınmaz, kontrol uyarısı gösterilir.

## Veri ve yedek güvenliği

- Yeni alan **ft_nakit_planlama_v1**, içerik sürümü **1**; gerçek işlem listelerinden ayrıdır. İlk açılışta kendiliğinden varsayılan yazılmaz. Yalnız kullanıcının Kaydet işlemi yazar.
- Plan kaydetmek gerçek gelir/gider, kredi/kart borcu, bütçe veya geçmiş dönem kaydı oluşturmaz/değiştirmez.
- Alan şeması, kimlikler, tarih/tutar, tekrar, durum ve bağlantılar doğrulanır. Bozuk JSON/şema veya bilinmeyen sürüm korunur; üzerine yazma engellenir ve kullanıcı bilgilendirilir.
- Yazma sonrası okuma doğrulanır; başarısızlıkta mevcut geri alma mekanizması eski planı korur. Düzenleme sırasında alan değişmişse eski taslak üzerine yazamaz.
- Tam JSON yedeği kaydedilmiş planları da içerir. Eski finora-v2, finora-v1 ve butce-takip-v1 yedeğinde planlama alanı yoksa **mevcut planlar korunur**. Geçerli tam yedekte açıkça verilen planlama bölümü yalnız önizleme/onayla değiştirilir.
- Önceki Aşama C kısmi geri yükleme engeli korunur: yalnız plan içeren veya kısmi işaretli paket önizlenebilir; **tam geri yükleme yapamaz**, finans verileri ve planlar değişmez. Açıkça boş geçerli planlama nesnesi tam yedekte yer alırsa önizleme/onay sonrası o alanı temizleyebilir; eksik alan boş kabul edilmez.
- Hatalı planlı yedek yazmadan reddedilir. Geri yükleme öncesi güvenlik kopyası, JSON okuma düzeltmesi ve Android sistem dosya kaydetmesi korundu.

## Değişen dosyalar ve testler

- **[App.jsx](./App.jsx)** → `src/App.jsx`. Tek dosya teslim korunuyor.
- **[android/app/build.gradle](./android/app/build.gradle)** → aynı yol: yalnız versionCode **6→7**, versionName **1.3.0→1.3.1**.
- Ayrı yeni testler `planning.test.mjs`, `planning.browser.test.mjs`; mevcut kaynak yükleyiciye yeni saf yardımcılar eklendi. Önceki testlerin kaynakları/beklentileri değişmedi.

**366/366 test geçti:** önceki **308** + yeni **58** (41 işlev, 17 gerçek React/Chromium ekran kontrolü); başarısız/atlanan yok. Kaydet/yeniden aç, düzenle/sil/pasif, Kaydet/Vazgeç, maaş değişimi, 29/30/31 kısa ay/artık yıl, tekrarlar, açık bağlantı/çakışma, eski/kısmi yedek, bozuk veri, kota/sessiz yazma/stale taslak, gerçek kayıt koruma ve üç temada 320px uyumluluk kapsandı. Kesin ham çıktı `kanitlar/final-tests.tap`.

`npm run build`, `npx cap sync android`, ana Android `assembleDebug`: **başarılı**. Değişmeyen native test görevi geçerli önbellek sonucunu kullandı (UP-TO-DATE); yeni cihaz testi sayılmadı. ESLint önce/sonra **49 hata, 12 uyarı**; yeni lint hatası eklenmedi, genel temizlik yapılmadı. Vite büyük paket uyarısı sürüyor.

Kaynak karşılaştırmasında ortak finans motoru, A/B/C/D, geçmiş borç işlevleri, App kökü ve global tema/animasyon blokları aynıdır. Değişen mevcut işlevler: NakitAkisiTab, nakitAkisiHesapla (planları aynı çizelgeye ekler), YedekOnizleme, yedekVerisiniDogrula, useLocalStorage (yalnız yeni planlama anahtarında ek yazma doğrulaması). Yeni bağımlılık, veritabanı veya bağımsız bakiye motoru yok. Laboratuvar, .devtest ve bağımsız Nakit Akışı kopyaları geliştirilmedi. Gerçek telefon kayıtlarına/kişisel yedeklere erişilmedi veya dönüşüm yapılmadı.

## APK, kurulum ve geri dönüş

**[Ana uygulama güncelleme APK’sı](./Finans-Nakit-Akisi-1.3.1.apk)**: `com.butcetakip.app`, **1.3.1 / versionCode7**, debug; **7.001.428 bayt**.

APK SHA-256: `4901556d57be1d6912333e82c11b9a14fa33ce72bbbc92fc74b75963d10224df`.

Sertifika SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.

Önceki teslim edilen ana **1.3.0** APK ile kimlik ve imza eşitliği doğrulandı; sürüm kodu 6→7. Telefonda kurulu paketin sertifikasına doğrudan erişilmedi. Kurulu sürüm o teslim APK’sıysa normal güncelleme koşulları eşleşiyor; başka anahtar veya daha yüksek sürüm kodunda uyumluluk varsayılmaz. Kurulum reddedilirse uygulamayı kaldırma/verileri temizleme veya zorla düşürme yapma.

Güncellemeden önce JSON dosya yedeğini alıp bilgisayara da kopyala; bakiye ve kayıt sayılarını not et. Yeni sürümde bir planı kaydet, uygulamayı kapat/aç ve hatırlandığını kontrol et. Bir taslakta Vazgeç'i dene; gerçek bakiye ve işlemlerin aynı kaldığını doğrula. Plan içeren yeni tam dosya yedeğini de sakla. **Bu 1.3.1 APK gerçek telefonda henüz denenmedi**; önceki 1.3.0 cihaz kontrollerinin başarılı olduğu kullanıcı tarafından bildirildi.

Geri dönüş kaynakları `geri-donus/` içinde (kişisel veri yedeği değildir). 7 kurulduktan sonra eski kaynak gerekiyorsa aynı imzayla **7’den yüksek** sürüm koduyla yeniden derlenir; kaldırma veya veri silme gerekmez. Planlama alanı eski sürümde yönetilemez fakat otomatik silinmez. Kullanıcı verisi migrasyonu uygulanmadı.
