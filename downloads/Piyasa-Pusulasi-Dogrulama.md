# Doğrulama — 14 Ekim 2026

- 12 hesap/veri testi geçti: Wilder RSI sayısal örneği, yatay seride RSI50,
  SMA/EMA/MACD, yetersiz geçmiş, mum temizleme, zaman damgası, eski/future veri,
  seans durumu, gram/kur dönüşümü, Türkçe hedef tutarı ve alarm tazeliği.
- Chromium mobil 390×844: sembol arama, analiz, çizgi/mum ve dönem seçimi,
  tema ve hedef kaydının yeniden açılışta korunması, karşılaştırma ve altı sembol
  taraması geçti. Gram grafiğinin USD referansı olduğu doğrulandı.
- Kontrollü 503 ve eski tarihli sağlayıcı yanıtında önceki analiz kaldırıldı.
  Sekme değişimi sonrası eski isteğin ekrana dönmesi engellendi. JS çalışma hatası görülmedi.
- Release APK oluşturuldu ve Android Lint geçti: 0 hata, 2 açıklanmış uyarı.
  JavaScript bundled React için gereklidir; köprü yalnız HTTPS yerel uygulama
  sayfasında açılır. Ağ istekleri iki Yahoo alanı ve Gemini ile sınırlıdır;
  TLS doğrulaması kapatılmadı. SharedPreferences commit arka iş parçacığında
  kalıcı kayıt sonucu gerektiği için kullanılır.
- Android11/API30, WebView83, yazılımsal emülatör: APK kuruldu; aynı imza ile
  güncelleme kuruldu; MainActivity ve mobil ana ekranın çizimi doğrulandı.
  İlk sistem açılışında TCG emülasyonunun yavaşlığından System UI ANR yaşandı;
  daha düşük emülatör çözünürlüğünde uygulama ekranı doğrulandı.
- APK imzası apksigner ile geçerli, paket `tr.piyasapusulasi.mobile`, minSDK26,
  targetSDK35, versionCode1/versionName1.0.0; debug derleme değildir.

## Doğrulanmayan dış bağımlılıklar

Bu cloud ortamının ağı Yahoo alan adlarına 403/DNS engeli uyguladı.
Gerçek zamanlı sağlayıcı erişimi, gerçek piyasa fiyatının doğruluğu ve gerçek
fiyat hedefine ulaşıldığında fiziksel telefonda bildirim teslimi doğrulanmadı.
Fiyat/analiz testleri açıkça kontrollü yanıtlarla yapıldı. Gerçek hesapla Gemini
çağrısı yapılmadı; kişisel API anahtarı gerekmediği sürece uygulama teknik analizi kullanır.
Borsa tatili/kapalı seans fiyatını canlı veri olarak gösterme iddiası yoktur.
