# Doğrulama — Nöbet Çizelgesi 1.1 / 19 Ekim 2026

- JDK 17.0.17, Android SDK 35, build-tools 35.0.0, Gradle 8.9, AGP 8.7.3.
- `testDebugUnitTest`: **32 test, 0 başarısız, 0 atlanan**.
- `lintDebug`: **sorun bulunmadı**.
- `assembleDebug` / `assembleDebugAndroidTest`: başarılı.
- Android 11 / API 30 x86_64 emülatöründe 1.0 üzerine `adb install -r` ile 1.1 kurulumu başarılı; önceki ve yeni APK imza sertifikalarının SHA-256 değeri aynı.
- Android test çalıştırıcısı: **14 kontrol geçti, 0 başarısız**.

## Yeni sürümde doğrulananlar

Sabit yer kimliğiyle ad değiştirme, geçmiş sayımların korunması, ek şubenin otomatik dağıtımı, çalışma günleri ve genel mazeret, yalnız manuel görev yeri, eski haftayı düzenlerken sonraki haftaların korunması, iki personel arasında atama değiştirince azalan/artan sayımlar, tekrar kaydın mükerrer sayılmaması, eski hafta değişikliğinden doğan bekleyen hafta sonu hakkının korunması, haftalık sınırın yalnız açık istisnayla aşılması, istisnanın mazeret/dinlenme kontrollerini kaldırmaması, Türkçe alfabetik sıralama, istatistik sütunlarının ana yer hafta içi/sonu ve dinamik şubelere ayrılması.

Önceki sürümün 7 gün/12 slot, hafta sonu aynı kişi, perşembe/cuma dinlenmesi, mazeret tarih sınırları, sıra dönüşü, aynı gün iki yerde olmama ve 100 haftalık yük dengesi kontrolleri de geçti.

Android üzerinde gerçek dosya kayıt/yeniden okuma, eski sürüm JSON geçişi, yer ayarlarının saklanması, değişen çizelgeyle kalıcı istatistiklerin güncellenmesi, tekrar atama istisnasının saklanması, dash çıktısı, tüm yerleri içeren çok sayfalı PDF, PDF render ve paylaşım URI erişimi, uygunsuz dosya/yazma erişiminin reddi, açılış/koyu tema, Ayarlar ve A/B/C istatistik başlıklarının açılması, mavi düğme/beyaz yazı doğrulandı.

Üretilmiş örnek PDF `pdftotext` ile geri okundu: yalnız başlık, tarih aralığı ve çizelge var; “Onaylı haftalık çizelge”, alt açıklama, hazırlayan ve onaylayan alanları bulunmuyor. Atanmayan hafta sonu şube hücreleri — gösteriliyor. Örnekler sentetiktir; gerçek personel verisi kullanılmadı.

## Sınırlar

Fiziksel telefonda veya tüm Android sürümlerinde test edilmedi. WhatsApp'ta gerçek alıcıya gönderim yapılmadı; PDF üretimi ve Android paylaşım URI erişimi test edildi. APK debug imzalıdır. Canlı çoklu telefon eşitlemesi ve yönetici hesapları uygulanmadı; APK cihazda çalışır. Yetki/sunucu planı ORTAK-KULLANIM.md içindedir.
