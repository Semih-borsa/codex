# Geçmiş kart borcu incelemesi — üretim düzeltmesi yapılmadı

- **[Türkçe analiz ve test raporu](./RAPOR.md)**.
- [Hesaplama test sonuçları: 11 geçti / 4 başarısız](./history-unit.tap).
- [Ekran test sonuçları: 4 geçti / 4 başarısız](./history-browser.tap).
- [Mevcut 207 test: 207 geçti](./mevcut-207.tap).
- [Kaynakların ve mevcut testlerin değişmediği kontrolü](./degisiklik-kontrolu.json).

Bu klasörde gerçek kullanıcı verisi bulunmaz. Hiçbir üretim App.jsx, native kaynak, kişisel kayıt veya yedek değiştirilmedi. Yeni başarısız testler silinmedi/gevşetilmedi; düzeltme kullanıcı onayını bekliyor. Yeni APK yoktur.

## Tekrar çalıştırma

Mevcut Node/React/esbuild/@babel/parser, Chromium ve Playwright kurulumu gerekir. Yeni paket yükleyen adım yoktur. Kaynak önceki dışa aktarma tesliminin App.jsx dosyasıdır; kaynak hash'i raporda belirtilir. Aynı bulut ortamında, repo kökünden:

```sh
TZ=Europe/Istanbul FINANS_SOURCE="$PWD/downloads/android-yedek-duzeltme/App.jsx" FINANS_DEPENDENCY_ROOT=/workspace/finans-upload/finans-takip node --test --test-reporter=tap downloads/gecmis-kart-borcu-analizi/history.test.mjs downloads/gecmis-kart-borcu-analizi/history.browser.test.mjs
```

Beklenen mevcut analiz sonucu **23 kontrol: 15 geçti / 8 başarısız / 0 atlanan**, çıkış 1. Bunlardan H13/H14, doğrulayıcı eksikliğini kanıtlayan tanı kontrolleridir; geçmeleri doğrulayıcının doğru olduğu anlamına gelmez. Cihazın gerçek ekranı test edilmedi; mobil boyutlu Chromium'da sentetik veriler kullanıldı.

`FINANS_PLAYWRIGHT` ve `FINANS_CHROMIUM` ile başka ortamın kurulu araç yolları verilebilir. Mevcut 207 testin aynı kaynağa karşı yeniden çalıştırılmış çıktısı bu klasörde korunur. Tüm orijinal test kaynakları önceki teslim klasöründe bulunur; buradaki yeni testler onların yerine geçmez.
