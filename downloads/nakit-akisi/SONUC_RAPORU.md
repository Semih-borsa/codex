# 30/60/90 Günlük Nakit Akışı — Teslim

## Eklenenler

Ana menüde **Nakit Akışı**: 30/60/90 gün, dönem sonu tahmini bakiye, en düşük bakiye, ilk olası açık tarihi, günlük çizgi grafik ve dokunarak/tarih seçerek gün ayrıntısı. Kayıtlı hareketler düz, varsayımlar kesik çizgiyle ayrılır. Hareketlerde Kayıtlı/Tahmini etiketi bulunur. Gün sonu net bakiye hesaplanır; gün içi ödeme sırası tahmin edilmez.

Başlangıç mevcut `donemBakiyeHesapla` nakit bakiyesidir; bekleyen borçlar başlangıçta yeniden düşülmez, kendi tarihlerinde uygulanır. Bankaya bağlantı yoktur. Bankadaki tutar farklıysa yalnız bu tahmin için başlangıç bakiyesi girilebilir.

Tarihli gelecek gelirler, krediler, kısmi ödeme sonrası ekstre kalanları, ödeme bağlantıları, Aşama D tarihli planları ve sabit giderler mevcut ortak kurallarla okunur. Aynı açık ödeme/kredi bağlantısı, ekstre-plan veya ekstre-planlı ödeme iki kez düşülmez. Aynı kesim için banka ve plan farklıysa tutarlar/fark gösterilir; otomatik veri birleştirilmez. Tarihi olmayan eski taksit ve kart stoku geleceğe uydurma tarihle dağıtılmaz.

Maaş tutarı mevcut ayardan alınabilir; yatış tarihi ayrıca kullanıcı tarafından doğrulanır. Maaş dönemi başlangıcı otomatik yatış tarihi sayılmaz. Diğer gelirler tek sefer veya aylık varsayım olarak eklenebilir. Bunlar **yalnız ekran açıkken bellekte** tutulur; ekrandan çıkınca sıfırlanır, işlem/yedek olarak kaydedilmez. Zaten tarihli gelir kaydı olan tutarı tekrar varsayım olarak eklememek gerekir.

## Dosyalar

- [App.jsx](./App.jsx): **src/App.jsx** yerine; zip değildir.
- [Finans-Nakit-Akisi.apk](./Finans-Nakit-Akisi.apk): ana uygulama debug güncellemesi.
- [android/app/build.gradle](./android/app/build.gradle): kendi APK'nı derliyorsan ana Android projesindeki aynı dosya; yalnız sürüm 1.3.0 / versionCode6 yükseltmesi.
- Test kaynakları `tests/`; ham sonuçlar ve imza `kanitlar/`; başlangıç kaynakları `geri-donus/`.

App.jsx içindeki eklenen işlevler: `bekleyenNakitHareketleri`, `nakitAkisiHesapla`, `NakitAkisiTab`. `donemBakiyeHesapla` içindeki mevcut bekleyen nakit seçimi ortak yardımcıya çıkarıldı; aynı filtreler ve tutar kuralları korunarak tahminle paylaşıldı. İkinci kayıt/bakiye motoru veya veritabanı kurulmadı. App'te yalnız menü ve yeni ekran bağlantısı eklendi. Eski tema/CSS blokları ve diğer işlevler kaynak karşılaştırmasında aynıdır.

## Test ve derleme

**308/308 geçti; 0 başarısız, 0 atlanan.** Önceki 273 testin kaynak ve beklentileri değişmedi. Yeni 35 test: 26 işlev + 9 gerçek React/Chromium ekran testi. Kapsam: 30/60/90 gün, maaş günü, kredi/ekstre vadesi, kısmi ödeme, erken/geç planlı ödeme, tarihli taksit, çift sayım, dönem sınırı, negatif gün, eksik veri, ay sonu/artık yıl, iade/iptal, kuruş, ekran aç/kapa ve yeniden yüklemede kayıt koruma; üç tema ve 320px taşma kontrolü; grafiğe dokunma.

Dar ekran grafiği ilk çalıştırmada görünmüyordu; yeni ekranın CSS genişlik kısıtı düzeltildi. Nihai `kanitlar/final-tests.tap` çalıştırması tamamen başarılıdır; test silinmedi/atlanmadı veya beklenti gevşetilmedi.

`npm run build`, `npx cap sync android`, ana `:app:assembleDebug`: başarılı. Native yedek test görevi mevcut geçerli sonucu önbellekten kullandı (UP-TO-DATE); yeni cihaz testi sayılmadı. ESLint önce/sonra **49 hata / 12 uyarı**; genel temizlik yapılmadı, yeni lint hatası eklenmedi. Vite büyük paket uyarısı sürüyor.

Örnekler (sentetik): 20.000 nakit + 20 Ekim'de 5.000 kredi → 19 Ekim 20.000, 20 Ekim ve sonrası 15.000. 5.000 ekstreye önceden 2.000 ödeme varsa başlangıç 18.000, vadede kalan 3.000 düşer → 15.000. 20.000 başlangıç ve 25.000 ödeme → ilk açık ödeme günü, en düşük −5.000; sonraki 10.000 gelir → 5.000.

## Güvenlik ve sınırlar

Gerçek kullanıcı kaydı veya kişisel yedek açılmadı/değiştirilmedi. Yeni kalıcı alan/migrasyon/paket yok. Mevcut A/B/C/D, geçmiş borç okuması, JSON geri yükleme ve native Android sistem dosya kaydetmesi korundu. `.devtest`/laboratuvar projesi değiştirilmedi veya yeniden derlenmedi; sürüm dosyası ve paketlenmiş web varlıkları önceki teslimle aynıdır.

Tahmin, bilinen yükümlülükler ve kullanıcının belirttiği varsayımlarla sınırlıdır. Gelecekteki günlük alışveriş, faiz veya bilinmeyen kart vadesi otomatik üretilmez. Eksik veride sonuç tam mali tablo değildir; ekrandaki kapsam açıklamalarını incele. Gecikmiş bekleyen borçlar bugün ödenecek varsayımıyla işaretlenir. Vade sonrası planlı kart ödemesinde yükümlülük vadesi esas alınır; faiz hesaplanmaz. Kişisel geçmiş kayıtlara düzeltme yapılmaz.

## APK ve kurulum

- Kimlik: **com.butcetakip.app**; `.devtest` değildir.
- versionName **1.3.0**, versionCode **6** (önceki ana teslim 5).
- Boyut **6.994.772 bayt**.
- APK SHA-256: `40fc0e2bc0caabd15268b9162ed1f4b11b53b8f867a3299436cbd262b9f08896`.
- Gerçek derleme yolu: `/workspace/finans-upload/finans-takip/android/app/build/outputs/apk/debug/app-debug.apk`.
- Sertifika SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1` (Android Debug).

Önceki ana `Finans-json-yukleme-ayni-kimlik.apk` ile kimlik ve imza eşitliği doğrulandı; versionCode 5→6. **Telefonda kurulu gerçek sürümün imzasına erişilmedi.** Bu yüzden onun üzerine güvenli güncelleme kesinleştirilemez. Farklı anahtar, başka kimlik veya daha yüksek versionCode varsa güncelleme reddedilebilir. Uygulamayı kaldırma, verilerini temizleme veya zorla downgrade yapma.

Güncellemeden önce sistem dosya kaydetmesiyle JSON yedeği alıp telefondan bağımsız bir kopyasını bilgisayara sakla; kayıt sayıları ve bakiye ekranlarını not et. İmza eşleşen kurulumda normal güncelleme kullan. Sonrasında Nakit Akışı'nı menüden aç; başlangıç tutarını, ilk kredi/ekstre vadelerini, üç süreyi ve eksik veri açıklamalarını kontrol et. Önceki ekranlara dönünce bakiye/kayıtların aynı olduğunu doğrula. **Yeni APK gerçek Android telefonda henüz denenmedi.**

Geri dönüş: `geri-donus/App.jsx` ve eski sürüm dosyası korundu. VersionCode6 kurulmuşsa gerekirse eski kaynak aynı imzayla **6'dan yüksek** yeni sürüm koduyla derlenir; veri silme gerekmez. Otomatik veri dönüşümü olmadığı için geri alınacak finans migrasyonu yoktur.

Yalnız Nakit Akışı tamamlandı; diğer özelliklere geçilmedi.
