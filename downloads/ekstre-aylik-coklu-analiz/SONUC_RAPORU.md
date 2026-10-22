# Ekstre aralıkları ve 6 dönem raporu — 1.5.3

## Kullanım

Ekstre Analizi → Ekstre analiz aralığı:

- Aylık: seçilen tek ekstre ayı.
- 3 aylık: bitiş ayı dahil son üç takvim ayı.
- 6 aylık: bitiş ayı dahil son altı takvim ayı.
- Yıllık: seçilen yılın Ocak–Aralık ayları.

“Kart filtresi” tüm kartları veya tek kartı seçer. Aynı kartın seçili aylardaki satırları birleştirilerek ilk 5 kategori + Diğer pastası ve tüm kategorilerin tutar/yüzde çubukları gösterilir. En altta seçilen kartların genel toplamı ve kart payları vardır. Ödeme, iade, faiz/ücret, puan ve nakit avans ayrımı korunmuştur; brüt alışveriş ve iadeler sonrası net ayrı gösterilir.

Eksik aylar harcama sıfırmış gibi doldurulmaz; kart altında eksik ay listesi bulunur. Kısmi ekstre varsa uyarı korunur. Ekstreleri yönet bölümünden kaynak ayın belgesi düzenlenebilir/silinebilir. Birleşik toplamı düzenlemek yerine ilgili ayın kaydı açılır. Aylık görünümde bu işlem düğmeleri açık gelir.

Raporlar → Rapor aralığı → “Son 6 maaş dönemi” eklendi. Seçili maaş dönemi dahil altı dönemi kapsar; önceki/sonraki gezinmesi mevcut seçeneklerle aynı şekilde çalışır. Ekstre analizindeki takvim ayları ile rapordaki maaş dönemleri ayrı tutulur.

## Veri güvenliği

Ekstre arşivinin önceki toplam 12 belge sınırı, çok kartlı yıllık analiz için 120 belge ve 2.000.000 JSON karakteri sınırına genişletildi. Her belgedeki 2.000 satır ve 200 kategori kuralı sınırları korunmuştur. Cihazın kullanılabilir depolaması ayrıca sınır koyabilir; yazma hatası mevcut güvenli saklama akışında ele alınır. Sınırsız depolama garantisi verilmez.

Eski arşiv/yedek biçimi (surum:1) değişmedi. Mevcut kayıtlar dönüştürülmedi, çoğaltılmadı veya yeniden yazılmadı. Analiz aralığı ve kart filtresi yalnız görünümü değiştirir. PDF'ler ve özetler dışarı gönderilmez; Gemini/API yeniden eklenmedi.

14 ana finans/yedek/bildirim işlevinin işlem öncesiyle aynı olduğu kaynak karşılaştırmasıyla doğrulandı (`kanitlar/scope.json`). Kredi, kart borcu, ödeme, taksit, bakiyeler, yerel yorumlar, temalar, grafik renkleri ve animasyonlar korunmuştur. Yeni bağımlılık eklenmedi.

## Değişen dosyalar

- `App.jsx`: ekstre aralık/gruplama/filtre ekranı, sınırlı arşiv genişletmesi, altı dönem seçeneği.
- `android/app/build.gradle`: versionName 1.5.3, versionCode 16.
- `tests/finance-baseline/statement-ranges.test.mjs`: sekiz saf kabul kontrolü.
- `tests/finance-baseline/statement-ranges.browser.test.mjs`: üç tema + dönem raporu olmak üzere dört ekran kontrolü.
- `tests/finance-baseline/source-harness.mjs`: yeni saf işlevlerin test erişimi.

## Test ve derleme

Yeni 12 kontrol başarılı: ay/yıl sınırı, kart ve genel tutar, kategori ayrımı, eksik ay, iade/ödeme ayrımı, dört kartın 48 ekstreyle yıllık analizi, eski biçimin korunması, üç tema, mobil taşma ve altı maaş dönemi gezinmesi.

Genel koşu **495/495 başarılı**, 0 başarısız (`kanitlar/all-tests.tap`). Önceki teslimde kaldırılan API ekranına ait 10 test ve eski kaldırılmış PDF arayüzüne ait 17 test kaynak olarak korunmuş, aynı şekilde koşu dışındadır. Finansal beklentiler değiştirilmedi. Yeni sekiz birim kontrolünden biri ilk çalıştırmada test erişimine eklenmeyen rapor işlevi nedeniyle çalışamadı; test köprüsüne işlev eklendi, üretim hesaplaması veya beklenti değiştirilmeden yeniden koşuldu.

`npm run build`, `npx cap sync android`, Android native test görevi ve debug APK derlemesi başarılı. Değişmeyen Gradle test görevleri önbellekten yararlanmıştır. Fiziksel Android cihazda bu sürüm henüz doğrulanmadı.

Telefonda birden fazla ay yükleyip her aralığı/tek kart/tüm kartları seç; toplamları kaynak belgelerle karşılaştır. Bir ayı düzenle ve birleşik toplamın güncellendiğini kontrol et. Eksik ay uyarısını, arşivli JSON yedek al/geri yüklemeyi ve Raporlar'daki 6 maaş dönemini ayrıca doğrula. Gerçek telefon verilerine bu çalışma sırasında erişilmedi.

## Teslim ve geri dönüş

`App.jsx` ZIP olmadan tam kaynak dosyadır; `src/App.jsx` ile tamamen değiştirilir. Android sürüm dosyası `android/app/build.gradle` dizinine kaydedilir. Hazır `Finans-1.5.3.apk` bilgisayarda komut çalıştırmadan test kurulumu içindir.

APK applicationId `com.butcetakip.app`, versionCode 16. İmza önceki teslim edilen debug APK'larla aynıdır: sertifika SHA-256 `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`. APK ve App.jsx boyut/hash değerleri `SHA256.json` içindedir. Telefonda kurulu imza bilinmediğinden üzerine güncelleme koşulsuz garanti edilemez.

Kurulumdan önce JSON yedeği al; uygulamayı kaldırma veya verilerini temizleme. Çakışma varsa doğru kurulu imzayla derleme gerekir. `geri-donus/` bu işlemden önceki App.jsx ve build.gradle kopyalarını içerir. Kaynak geri alınabilir; telefona zorla eski sürüm kurmak yerine aynı imzalı daha yüksek versionCode taşıyan geri dönüş sürümü hazırlanmalıdır.
