# Doğrulama · 10 Ekim 2026

Bu bulut makinesinde kaynaklar derlendi ve debug APK Android 11 (API 30) emülatörüne kuruldu. Android SDK 35, Build-Tools 34.0.0, tam JDK 17 ve projenin Gradle 8.9 wrapper'ı kullanıldı.

## Sonuçlar

- `testDebugUnitTest`: **10 test geçti**, 0 başarısız, 0 hatalı, 0 atlanan.
- `connectedDebugAndroidTest`: **14 test geçti**, 0 başarısız, 0 hatalı, 0 atlanan. Gerçek Android SQLite üzerinde, ayrı test veritabanında çalıştı.
- `lintDebug`: **hata ve uyarı yok**.
- `assembleDebug` ve `assembleDebugAndroidTest`: başarılı.
- `apksigner verify`: APK imzası geçerli.
- Kaydedilen bulut kurulum betiği ayrıca tekrar çalıştırıldı: başarılı; mevcut Gradle çıktıları yeniden kullanıldı.

UI kontrolünde boş uygulamaya 1.250,50 TL gider ve 5.000,00 TL gelir girildi. Net bakiye 3.749,50 TL oldu. İşlem listesinde iki kayıt ve doğru kategoriler görüldü. Analiz özeti bu tutarları gösterdi. 1.000,00 TL Market bütçesinde 250,50 TL aşım oluştu. Nakit/Banka hesap ekranları açıldı ve uygulama durdurulup yeniden açıldığında bakiye korundu. UI kontrolü tamamlandı.

Ekran görüntülerindeki kayıtlar yalnızca emülatör test verisidir; dağıtılan APK bu kayıtlarla başlamaz. APK kişisel deneme için debug imzalıdır. Fiziksel telefon üzerinde henüz doğrulama yapılmadı; Android Studio'da kendi cihazınızla çalıştırabilirsiniz. Android 8.0 uyumluluğu minimum API ayarı ve statik taramayla kontrol edildi; çalıştırma testi Android 11'de yapıldı.

Bulut `install_script` ve `start_skill` alanları taslak olarak kaydedildi. Taslak kaydı yeni bir ortam sürümünü yayımlamaz; ortam ayarlarında inceleme/kayıt ve ardından yayımlama gerekir. Yeni bir görevde snapshot geri yüklemesi henüz ayrıca denenmedi.
