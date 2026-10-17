# Geçmiş dönem kart borcu düzeltmesi

GitHub'da dosyaya tıklayıp **Download raw file / Raw** ile indir.

- **[Finans Test D güncelleme APK'sı](./Finans-Test-D-gecmis-borc-duzeltme.apk)** — `com.butcetakip.app.devtest`, versionCode4, önceki test APK'sıyla aynı imza.
- **[App.jsx — zip değil](./App.jsx)** — `src/App.jsx` yerine kaydet.
- [Türkçe sonuç, test, imza ve geri dönüş raporu](./SONUC_RAPORU.md).
- [Ana kimlikli debug APK](./Finans-gecmis-borc-ayni-kimlik.apk) — yalnız aynı imzalı önceki debug kurulumu için; başka sürümde varsayma.
- [Android sürüm dosyası](./android/app/build.gradle) — yalnız versionCode4/versionName1.2.2 güncellemesi.

**268/268 JS ve React ekran testi geçti.** Eski 207 ve tarihsel 23 kontrol korundu; önceki 8 başarısız kontrol düzeldi. Yeni APK gerçek Android telefonda henüz denenmedi.

Mevcut uygulamayı kaldırma veya verilerini temizleme. Finans hesaplarındaki bu değişiklik tek App.jsx içindedir; önceki native sistem yedek kaydetme köprüsünü koru. Gerçek cihazdaki ayrı JSON geri yükleme hatası bu sürümde düzeltilmiş değildir.

Test kaynakları `tests/` altında. Kurulu bağımlılıkları kullanarak, bu klasörde:

```sh
TZ=Europe/Istanbul FINANS_SOURCE="$PWD/App.jsx" FINANS_DEPENDENCY_ROOT=/workspace/finans-upload/finans-takip node --test --test-reporter=tap tests/finance-baseline/*.test.mjs tests/finance-history-review/*.test.mjs
```

Başka ortamda mevcut proje/Chromium/Playwright yollarını FINANS_DEPENDENCY_ROOT, FINANS_CHROMIUM, FINANS_PLAYWRIGHT ile belirt; testler otomatik paket kurmaz. Bazı tarihsel kapsam kontrolü yardımcıları buluttaki eski geri dönüş kopyasına bağlıdır, `.test.mjs` çalıştırması bunları kapsamaz. Kesin son kanıt `kanitlar/history-fix-final-268.tap` dosyasıdır. Geri dönüş kopyaları yalnız kaynak/test bilgisi içerir; kişisel finans yedeği değildir.
