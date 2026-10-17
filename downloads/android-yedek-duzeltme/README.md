# Android yedek kaydetme düzeltmesi

Dosyaya tıklayıp **Download raw file / Raw** düğmesiyle indir.

- **[Finans Test D güncelleme APK'sı](./Finans-Test-D-yedek-duzeltme.apk)** — önceki ayrı test APK'sıyla aynı kimlik/imza, versionCode 3.
- [Aynı ana kimlikli debug APK](./Finans-yedek-duzeltme-ayni-kimlik.apk) — yalnız önceki aynı imzalı `com.butcetakip.app` teslimiyle eşleşmesi kanıtlandı; başka kurulumlarda varsayma.
- [Güncel App.jsx — zip değil](./App.jsx).
- **[Türkçe kök neden, test ve güvenli güncelleme raporu](./YEDEK_DISA_AKTARMA_TEST_RAPORU.md)**.

Android sistem kaydetmesi için yalnız App.jsx yeterli değildir; eklenti içeren yeni APK gerekir. Kendi projenizde derleyeceksen rapordaki native dosyaları `android/` klasöründeki aynı dizinlere ekle. Yeni npm bağımlılığı yoktur.

**Mevcut uygulamayı kaldırma veya verilerini temizleme.** Telefonundaki sürüm farklı anahtarla imzalıysa güncelleme uygunluğu ayrıca doğrulanmalıdır. Gerçek cihazda yeni sistem seçicisi henüz denenmedi. Önceki 207 + yeni 22 JS + 10 native test başarılıdır.
