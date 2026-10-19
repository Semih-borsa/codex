# Finans — PDF Ekstre Analizi 1.4.0

**[Doğrudan kurulabilir APK’yı indir](./Finans-PDF-Ekstre-1.4.0.apk)**

GitHub dosya sayfasında **Download raw file** düğmesini kullan. Bilgisayarda npm/build komutu çalıştırman gerekmez.

- Menü → Ekstre Analizi → PDF seç → kart etiketi/ekstre ayı gir → satırları doğrula → analiz et.
- Yalnız metin içeren şifresiz PDF, 10 MB / 30 sayfa.
- Dosyalar cihazda işlenir; gerçek finans kayıtlarına yazılmaz. Bu bölümden çıkınca geçici analizler silinir.
- **414/414** JS/React + **11/11** native test başarılı. Yeni APK gerçek telefonda henüz denenmedi.
- Ana kimlik com.butcetakip.app; önceki ana APK ile aynı imza, versionCode8. Önce JSON yedeğini ayrıca sakla. Uygulamayı kaldırma veya verilerini temizleme.

[Sonuç/kurulum raporu](./SONUC_RAPORU.md) · [App.jsx — zipsiz](./App.jsx)

Kaynak tesliminde package.json ve lockfile da güncellendi; PDF okuyucu ayrı bağımlılıktır. APK bütün varlıkları içerir. Testler tests/ altında; önce Vite üretim çıktısını oluşturup mevcut bağımlılıkların bulunduğu ortamda FINANS_SOURCE ve FINANS_PDF_DIST yollarını bu teslimin App.jsx/dist konumuna ayarlayarak testleri çalıştırabilirsin. Kesin son çalıştırma kanitlar/final-tests.tap dosyasındadır.
