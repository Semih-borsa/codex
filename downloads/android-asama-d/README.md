# Aşama D — Android test teslimi

Dosyaya tıklayıp **Download raw file / Raw** üzerinden indirebilirsin.

- [App.jsx](./App.jsx): `finans-takip/src/App.jsx` yerine kaydet; zip değildir.
- **[Finans Test D — bağımsız APK](./Finans-D-bagimsiz-test.apk)**: önerilen test kurulumu; kimlik `com.butcetakip.app.devtest`. Mevcut uygulamadan ayrı veri tutar.
- [Güvenli kurulum, yedekleme, dosya boyutları ve imzalar](./ANDROID_GUVENLI_KURULUM_VE_TESLIM.md).
- [Android test listesi ve bakiye tabloları](./ANDROID_TEST_KONTROL_LISTESI.md).
- [ESLint öncelik raporu](./ESLINT_ONCELIK_RAPORU.md).
- [Önceki aynı kimlikli debug APK](./Finans-D-debug-ayni-kimlik.apk): mevcut uygulama üzerine güvenli güncelleme yapılabileceği doğrulanmadı. Test için yukarıdaki bağımsız APK'yı kullan.

Gerçek cihaz testi henüz yapılmadı. Mevcut uygulamayı kaldırma veya verilerini temizleme. Gerçek yedeğini önce bilgisayarına kopyala. `android-dogrulama` içindeki örnek JSON yalnız sentetik test verisidir, gerçek uygulamaya yükleme.
