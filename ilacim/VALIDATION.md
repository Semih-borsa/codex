# Doğrulama · 11 Ekim 2026

- Microsoft JDK 17.0.20.1, Android SDK 35, Build-Tools 34.0.0, Gradle 8.9 ve AGP 8.7.3 ile debug uygulama ve test APK'ları derlendi.
- JVM: **16 test geçti**, 0 başarısız/hatalı/atlanan. Ay sonu ve artık yıl, tarih/saat doğrulama, kesin kesirli miktar hesabı, barkod kontrol basamağı, GS1 ürün kodu çıkarımı, gerçek EAN ve DataMatrix görüntülerinin ZXing ile çözülmesi.
- Android 11/API 30 emülatörü: **8 test geçti**, 0 başarısız. Gerçek SQLite kalıcılığı, yarım doz stoğu, ilaç/plan/randevu/geçmiş yedek dönüşü, geçersiz ve yetim yedeklerin reddi, arşiv, Android Keystore şifreleme ve kategori uydurmayan JSON alan eşlemesi. Ayrı test veritabanı kullanıldı.
- İlk Android test koşusunda kullanıcı alanı kilitli olduğu için anahtar testi başarısız oldu. Kullanıcı `RUNNING_UNLOCKED` durumuna geçtikten sonraki tam koşuda 8 test geçti; ilk koşu başarı olarak sayılmadı.
- `lintDebug`: **No issues found**, 0 hata/uyarı.
- APK imzası `apksigner verify` ile geçerli. Debug sertifikası SHA-256: `25472d4648a5ce5fe56101a4abbff42b01dab6e33ae1d04dc61722ad163fdf31`.

Ana ekran Android 11 emülatöründe açıldı; başlık, boş stok/dönem ve gezinme düğmeleri gerçek UI hiyerarşisinde görüldü. Donanım hızlandırması olmayan emülatörde sistem arayüzü ve dokunma enjeksiyonu zaman zaman yanıt vermediği için tam etkileşimli form akışları doğrulanmış sayılmadı.

API sağlayıcısı henüz seçilmediği için canlı barkod sorgusu doğrulanmadı. Fiziksel kamera taraması, Android 13+ izin ekranı, gerçek telefonda arka plan bildirim teslimi ve üretici pil tasarrufu davranışları ayrıca denenmelidir. Emülatör KVM olmadan yazılımsal çalıştığından açılış ve sistem arayüzü yavaştır; bu performans fiziksel telefon ölçümü değildir.
