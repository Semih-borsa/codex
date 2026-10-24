# Portföyüm 4.8.2 — seçilebilir grafik dönemleri

Paket kimliği `com.semih.portfoyum` olarak korundu. Sürüm 4.8.2 / 40802. Önceki 4.8.1 düzeltmeleri korunur.

## Değişiklik

Portföy içindeki ve varlık ayrıntısındaki Günlük / Haftalık / Aylık etiketleri gerçek, erişilebilir düğmelere dönüştürüldü. Her grafiğin seçimi ayrıdır; gezinme, tema ve ekran yenilenmesinde uygulama oturumu boyunca korunur. Uygulama yeniden açıldığında mevcut otomatik başlangıç seçimi kullanılır. Seçim finans kayıtlarına veya yedeklere yazılmaz.

Seçim, son kayıt zamanından geriye doğru 24 saat / 7 gün / 30 gün içindeki noktaları kullanır. Haftalık ve aylık görünümde günün son noktası gösterilir. Başlıktaki Son kayıt alanı verinin tarihini açıkça gösterir; eski kayıtta güncel fiyat varmış gibi davranılmaz. Tutar ve yüzde seçili çizginin ilk-son noktaları arasındaki değer/fiyat değişimidir; yatırım kazancı diye yeniden adlandırılmaz. Aralık dışındaki eski nokta zorla eklenmez. İki nokta yoksa grafik için veri biriktiği, tam dönem geçmişi yoksa yalnız kayıtlı tarihlerin gösterildiği belirtilir. Veri uydurulmaz.

Klavye odağı seçimden sonra seçilen düğmede tutulur. `aria-pressed`, grup etiketi ve en az 44 piksel düğme yüksekliği eklendi. Açık/koyu tema korunur.

## Dosyalar

- `app/src/main/assets/core.mjs`: seçilebilir aralık parametresi ve kapsam bilgisi. Parametre verilmediğinde önceki otomatik davranış korunur.
- `app/src/main/assets/app.mjs`: düğmeler, tıklama, grafik başına oturum seçimi, yalnız ilgili grafiğin yenilenmesi.
- `app/src/main/assets/modern.css`: düğme stilleri ve odak görünümü.
- `app/build.gradle`: yalnız sürüm artırımı; applicationId değişmedi.
- `tests/ui.test.mjs`: iki ek akış testi.
- `tests/chart-ranges.test.mjs`: yedi yeni hesaplama testi.
- `GUNCELLEME-4.8.2.md`: bu rapor.

## Doğrulama

- Önceki 121 test korundu; toplam 130/130 başarılı, atlanan/silinen/gevşetilen test yok.
- Gerçek Chromium: iki tema × portföy/varlık grafiği × üç aralık = 12 başarılı tıklama. Çizgi değişimi, seçili düğme, klavye odağı, taşma/hata olmaması ve yerel finans kayıtlarının aynı kaldığı doğrulandı.
- Android `:app:assembleDebug` başarılı (JDK 17 / SDK 35 / AGP 8.9.2 / mevcut Gradle 8.14.3). Orijinal 8.11.1 wrapper değiştirilmedi.
- Gerçek telefonda henüz doğrulanmadı. Telefonunuzda üç seçeneği, varlık penceresini ve iki temayı ayrıca deneyin.

## Kurulum

Bu ZIP değişen ve değişmeyen bütün kaynakları içerir. İçteki `Portfoyum-Android-Studio/Portfoyum-Android` klasörünü Android Studio'da açın. Mevcut bilgisayarınızdaki önceki imza anahtarınızla derleyin; applicationId aynı olsa bile farklı anahtarla üretilen APK güncelleme olmaz. Güncellemeden önce JSON yedeğinizi alın. Mevcut uygulamayı kaldırmayın/verisini temizlemeyin. Kişisel kayıt veya gizli anahtar ZIP'e eklenmedi.

4.8.1 geri dönüş kaynağı repoda korunur. Kaynak geri alınarak tekrar APK üretilecekse aynı imzayla ve kurulu sürümden daha yüksek versionCode ile üretilmelidir.

## Enflasyon / dolar / altın fikri

Bu sürüme uygulanmadı; kullanıcıyla tasarım değerlendirmesi aşamasındadır. Plan: altın/gümüş dışındaki yatırımlarda alış tarihindeki TÜFE endeksi, USD/TRY ve gram altın fiyatıyla karşılaştırma; para giriş/çıkışı, satış, temettü ve faizleri ayrı değerlendirme; kaynak ve veri tarihi gösterimi. TÜFE aylık açıklanır. Resmî veya güvenilir tarihsel kaynağın otomatik erişimi henüz doğrulanmadı. Geçmiş veri yoksa bugünkü oran geçmişe uygulanmamalıdır. Mevcut bakiyeler değişmemelidir.
