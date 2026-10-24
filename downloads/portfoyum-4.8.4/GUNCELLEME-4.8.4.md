# Portföyüm 4.8.4 — kart rengi düzeltmesi ve karşılaştırma hazırlığı

## Uygulanan arayüz değişikliği

Kurlar ve Geçmiş sayfalarında yalnız banka/hareket/kapanış satır kartları, Özet sayfasındaki Tüm portföyler · Net değer kartıyla aynı `linear-gradient(135deg,#123a86 0%,#225ed0 56%,#3a80f5 100%)` rengini kullanır. Ortak CSS değişkeni iki görünümün aynı kalmasını sağlar.

Büyük kapsayıcı paneller mavi değildir; açık/koyu temanın `--bg` zeminini kullanır. Başlıklar normal tema yazısı, mavi iç kartlardaki tutar ve etiketler beyaz/açık renklerdir. Pozitif/negatif değerlerin ayrımı korunur. Mobilde ve masaüstünde uygulanır. Mevduat düzenlemesi, artı simgesi ve grafik seçicisi korunur.

Paket kimliği aynı: `com.semih.portfoyum`. Sürüm: **4.8.4 / 40804**.

## Dosyalar

- `app/src/main/assets/modern.css`: kapsayıcı/tekil kart ayrımı, ortak mavi geçiş ve okunaklı yazılar.
- `app/src/main/assets/app.mjs`: yalnız görünür sürüm yazısı.
- `app/build.gradle`: yalnız sürüm yükseltme.
- `GUNCELLEME-4.8.4.md`: bu rapor.

Finans motoru, kaynak sağlayıcıları, native dosyalar, depolama/yedek biçimi ve gerçek kayıtlar değiştirilmedi. 4.8.3 kaynak geri dönüş kopyası korundu. Yeni bağımlılık eklenmedi.

## Doğrulama

- Mevcut 132/132 otomatik test başarılı, atlama/silme/gevşetme yok.
- Gerçek Chromium: 360, 390, 1024 piksel × iki tema × altı ekran = 36 kontrol; ek 12 pencere kontrolü başarılı. Kurlar/Geçmiş satırlarının hesaplanmış arka planı Özet kartıyla birebir aynı; kapsayıcıların zemin rengi tema zeminiyle aynı ve kapsayıcılarda geçiş yok. Yatay taşma/JavaScript hatası yok. Finans kayıtları gezinme sırasında aynı kaldı.
- Android debug derlemesi başarılı (JDK 17 / SDK 35 / AGP 8.9.2 / mevcut Gradle 8.14.3); orijinal 8.11.1 wrapper korunur.
- Gerçek Android cihazda son doğrulama yapılmadı. Aynı bilgisayardaki eski imza anahtarıyla derleyin, önce JSON yedeğinizi alın; uygulamayı kaldırmayın/verisini temizlemeyin.

## Enflasyon/dolar/altın: hazırlanan ve tamamlanmayan işler

EVDS'ye veya API anahtarına bağımlılık eklenmedi. Üretim uygulamasına henüz karşılaştırma özelliği bağlanmadı. Ayrı `karsilastirma-hazirlik` klasöründeki hesaplama/CSV modülleri bir hazırlık prototipidir; APK içindeki uygulama bunları çağırmaz.

33/33 sentetik hazırlık testi başarılı: tarihlendirilmiş katkı/çekim, alternatif yatırım birimleri, nominal/karşılaştırmalı kazanç, aylık TÜFE endeksi, önceki işgünü, veri boşlukları, tekrarlı/bozuk dosyalar, aynı tarihli spot ons/kur dönüşümü, aşırı değerler. Bu testler kaynağın gerçek ve güncel olduğunu veya uygulamaya entegrasyonun tamamlandığını kanıtlamaz.

Canlı FRED CSV, TCMB tarihsel XML ve Stooq spot altın denemeleri bu çalışma ortamının proxy alan adı politikasında CONNECT 403 ile engellendi (uzak hizmetten HTTP yanıtı alınmadı). EVDS'nin o anki erişimiyle ilgisi yoktur. Proxy/TLS sınırı aşılmadı.

Desteklenen ortam yapılandırma aracıyla şu alan adlarının erişim taslağı kaydedildi: `www.tcmb.gov.tr`, `data.tuik.gov.tr`, `fred.stlouisfed.org`, `sdmx.oecd.org`, `stooq.com`. Araç `status=saved`, `requires_publish=true` döndürdü. Taslak etkin erişim değildir; kullanıcı ortam ayarlarında inceleyip kaydetmeli ve ortamı yayımlamalıdır. Kurulum betiği, repo üyelikleri, mevcut paket yöneticisi izinleri veya gizli bilgiler değiştirilmedi.

Erişim sağlanınca gerçek seri kimliği, birim/baz yılı, kapsanan alış tarihleri, güncel veri sonu ve kaynak kullanım koşulları doğrulanmalı. Sonrasında mevcut finans motorundan alınan doğrulanmış değerlerle arayüze ve sürümlü ayrı veri alanına entegrasyon yapılmalı; eski yedekler ve hesaplar korunmalı. Eksik alış tarihleri, satış/temettü/nakit bağlantıları belirsizse kesin sonuç üretilmemeli. Kullanıcının API anahtarı gerektirmeme tercihi korunur.

Bu sürüm karşılaştırmanın tamamlandığı sürüm değildir. Çalışan son uygulamayı korumak için doğrulanmamış bağlantı etkinleştirilmedi.
