# Ana Finans uygulaması — Nakit Akışı

- **[App.jsx — zip değil](./App.jsx)** → src/App.jsx.
- **[Ana uygulama APK'sı](./Finans-Nakit-Akisi.apk)** → com.butcetakip.app, versionCode6.
- [Kısa sonuç, test ve güvenli kurulum raporu](./SONUC_RAPORU.md).
- [Android sürüm dosyası](./android/app/build.gradle).

GitHub dosya sayfasında Download raw file ile indir. 308/308 otomatik test geçti. Gerçek telefondaki imza bilinmediği için mevcut kurulumla uyumluluk varsayılmaz; kaldırma/veri temizleme yapma. Önce dosya yedeğini bilgisayara da kopyala. Bu APK gerçek cihazda henüz denenmedi.

## Testleri yeniden çalıştırma

Kurulu bağımlılıkları olan projeyle, bu klasörde:

```sh
TZ=Europe/Istanbul FINANS_SOURCE="$PWD/App.jsx" FINANS_DEPENDENCY_ROOT=/workspace/finans-upload/finans-takip node --test --test-reporter=tap tests/finance-baseline/*.test.mjs tests/finance-history-review/*.test.mjs
```

Başka ortamda kurulu proje yolunu ve gerekiyorsa FINANS_CHROMIUM / FINANS_PLAYWRIGHT yollarını değiştir. Testler otomatik paket kurmaz. Geri dönüş kaynakları kişisel finans yedeği değildir.
