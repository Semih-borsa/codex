# Karşılaştırma hazırlığı — uygulamaya bağlı değil

Sentetik hesaplama ve CSV doğrulama prototipi. `node --test *.test.mjs` ile 33 test çalışır. Gerçek bakiye hesaplamaz veya kaydetmez; mevcut finans motorunun sağlayacağı tarihlendirilmiş doğrulanmış değerleri karşılaştırmak içindir. Kaynakların canlı doğrulaması henüz tamamlanmadığından APK özelliği sayılmamalıdır. Katkı/çekim sınıflandırması, satış/temettü bağlantıları, arayüz ve güvenli önbellek entegrasyonu henüz yapılmadı.

CSV testindeki SYNTHETIC_INDEX gerçek seri değildir. FRED Türkiye seri kimliği, baz yılı ve güncelliği canlı kaynaklarla doğrulanmadan otomatik CPI kaynağı etkinleştirilmemelidir. Güncel TÜFE ayı yoksa eski ay sessizce yeni aya taşınmaz. Ons/kurdan türeyen gram referansı banka veya ziynet fiyatı değildir.
