# Ekstre Analizi kaldırıldı — 1.4.2

PDF Ekstre Analizi ekranı, menü bağlantısı, okuyucu/yorum yardımcıları ve `pdfjs-dist` bağımlılığı kaldırıldı. Bu değişiklik Vite'ın PDF paketi/worker çözümlenemiyor hatasının uygulamadaki nedenini kaldırır.

Kredi kartı ekstre borcu ve ekstre ödeme işlevleri korunur. Finans motoru, taksitler, geçmiş dönem düzeltmeleri, yedekleme, Nakit Akışı, kişisel kategoriler, üç tema, grafikler ve animasyonlar değişmedi. Tema uyumlu Ödeme Onayı ile Bugün Harcanan kartı aynen korunur. Kaynak karşılaştırması yalnız PDF analizi bloğu ve iki menü/ekran satırının kaldırıldığını doğrular. Gerçek finans verilerine erişilmedi; kayıt veya yedek dönüştürülmedi.

## İndir ve kullan

- `App.jsx`: Güncel tek kaynak dosyası; **zipsizdir**. Bilgisayardaki `src/App.jsx` yerine kaydet. PDF paketine artık ihtiyaç duymaz.
- `Finans-1.4.2.apk`: Ana Android uygulaması için debug güncelleme APK'sı.
- `package.json`, `package-lock.json`: PDF bağımlılığı kaldırılmış proje paket dosyaları. Diğer bağımlılık bildirimleri aynıdır.
- `android/app/build.gradle`: Sürüm kodu yükseltmesi.

Yalnız App.jsx dosyasını kullanırsan PDF paketi kurulu kalsa bile derleme onu istemez. Kendi proje paket dosyalarını koruyarak temizlemek istersen proje klasöründe `npm uninstall pdfjs-dist` çalıştırabilirsin. Ardından `npm run build`, başarılıysa `npx cap sync android` çalıştırılır. Genel bir `npm audit fix --force` uygulanmadı.

## Doğrulama

- Kalan uygulama kapsamı: **377/377 otomatik test başarılı**, başarısız/atlanan/iptal edilen yok. 374 mevcut kontrol + üç temada PDF menüsünün kaldırılması ve kayıtların korunması için 3 yeni ekran testi.
- Önceki 422 test içindeki PDF analizine özel 48 test, özellik kullanıcı isteğiyle kaldırıldığı için bu sürümün çalıştırma kapsamına alınmadı. Test dosyaları silinmedi ve beklentiler gevşetilmedi; önceki teslimde de korunuyor. 422 testin tamamı geçti denilmiyor. Test yardımcılarından yalnız kaldırılan PDF işlevlerinin zorunlu çıkarımı kaldırıldı.
- `npm run build`: başarılı; PDF uyarısı/hatası yok. Mevcut büyük web paketi uyarısı devam eder.
- `npx cap sync android`: başarılı.
- Android `:app:testDebugUnitTest :app:assembleDebug --offline`: başarılı. Mevcut native test kodu değişmedi; Gradle test önbelleği kullanılabilir.
- APK ve web çıktısı içinde PDF worker/analiz paketi bulunmadığı doğrulandı.
- Gerçek Android cihazda bu sürüm henüz denenmedi. Temalar, ödeme onayı/iptal, Bugün Harcanan, yedek alma ve uygulamayı yeniden açınca kayıtların korunması cihazda kontrol edilmeli.

## APK ve güncelleme uyumluluğu

- Uygulama kimliği: **com.butcetakip.app**. Bağımsız/.devtest paket kullanılmadı.
- versionName **1.4.2**, versionCode **10**; önceki teslim 1.4.1 / 9.
- Boyut: 7003444 bayt.
- APK SHA-256: `8151a032bef9b63e187481880a3cddcde240ae50d7ccc9afa383a71be4e56158`.
- Sertifika SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.
- Önceki teslim APK'sıyla aynı paket ve imza, daha yüksek sürüm kodu kullanılır. Telefonda kurulu sürümün imzası bilinmediğinden her kurulum için uyumluluk varsayılmaz. Farklı imza varsa uygulamayı kaldırma veya verilerini temizleme; özgün imza anahtarıyla derleme gerekir.
- Güncellemeden önce uygulamadan JSON yedeği alıp dosyayı ayrı yerde sakla. Bu geliştirme mevcut kayıtları silmez.

## Geri dönüş

İşlem öncesi kaynak ve paket/sürüm dosyaları `geri-donus/` klasöründe korunur. Kaynağa dönmek için bunlar kullanılabilir; telefona düşük sürüm zorla kurulmaz. Gerekirse özgün imzayla daha yüksek versionCode üzerinden geri dönüş derlemesi hazırlanır. Kullanıcı finans yedeği kaynak kopyasından ayrıdır; otomatik geri yüklenmez.
