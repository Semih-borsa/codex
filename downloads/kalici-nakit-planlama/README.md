# Finans — Kalıcı Nakit Akışı Planlama 1.3.1

- **[App.jsx — zipsiz](./App.jsx)** → src/App.jsx.
- **[Ana uygulama APK’sı](./Finans-Nakit-Akisi-1.3.1.apk)** → com.butcetakip.app; versionCode7.
- [Android sürüm dosyası](./android/app/build.gradle).
- [Kısa Türkçe sonuç ve kurulum raporu](./SONUC_RAPORU.md).

GitHub dosya sayfasında **Download raw file** ile indir. **366/366 test başarılı**. Önce JSON yedeğini bilgisayara da kopyala. Önceki ana 1.3.0 APK ile kimlik/imza eşleşiyor; kurulu telefon paketine doğrudan erişilmedi. Uygulamayı kaldırma veya verilerini temizleme. Yeni APK gerçek cihazda henüz denenmedi.

Planlama Ayarları'nda düzenlemeyi başlat, planı taslağa ekle, Kaydet ile kalıcılaştır. Vazgeç taslağı bırakır. Eski yedekte bulunmayan planlama alanı korunur; kısmi paketler tam geri yükleme yapamaz.

## Testleri tekrar çalıştırmak

Kurulu bağımlılıkları olan projeyle bu klasörde:

```sh
TZ=Europe/Istanbul FINANS_SOURCE="$PWD/App.jsx" FINANS_DEPENDENCY_ROOT=/workspace/finans-upload/finans-takip node --test --test-reporter=tap tests/finance-baseline/*.test.mjs tests/finance-history-review/*.test.mjs
```

Başka ortamda kurulu proje yolunu ve gerekiyorsa FINANS_CHROMIUM / FINANS_PLAYWRIGHT yollarını değiştir. Otomatik paket kurulmaz. Ham test, kaynak kapsamı, imza ve hash kanıtları kanitlar/ içindedir.
