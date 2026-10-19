# Doğrulama — Nöbet Çizelgesi 1.4 / 19 Ekim 2026

- JDK 17.0.17, Android SDK 35, build-tools 35.0.0, Gradle 8.9, AGP 8.7.3.
- `testDebugUnitTest`: **41 test, 0 başarısız, 0 atlanan**.
- `lintDebug`: **sorun bulunmadı**.
- APK ve Android test APK derlemeleri başarılı.
- Android 11 / API 30 x86_64 emülatöründe önceki sürüm üzerine `adb install -r` ile 1.4 güncellemesi başarılı.
- Android test çalıştırıcısı: **22 kontrol geçti, 0 başarısız**.

## 1.4 kontrolleri

İstatistik PDF'si boş personel listesinde de geçerli, kompakt bir tablo üretir. 19 personel / 7 görev yeri / geçmiş ve gelecek atamalı sentetik örnekte 8 sayfanın tamamı PdfRenderer ile açılıp çizilir. Gizlenen şubeler istatistikte korunur. Uzun şube/personel adları satıra bölünür. İstatistik PDF paylaşım URI'sinin okunabildiği, yazma erişiminin reddedildiği ve ekrandaki dışa aktarma düğmesi doğrulanır.

## 1.3 kontrolleri

20 görev yerli sentetik veride her şubenin istatistik başlığı bulundu; harfli başlıkların ve yatay kaydırma bileşeninin olmadığı doğrulandı. 280, 320, 360 ve 600 dp ölçümlerde tablo hücreleri sınırlar içinde kaldı. Sayımların ve Türkçe alfabetik sıranın mevcut motor testleri korundu.

Kompakt 700 × 358 punto PDF Android PdfRenderer ile açılıp görüntülendi. Çok sayfalı dışa aktarım ve görünür/gizli yerlerin korunması da yeniden doğrulandı. Sabit başlıklarda Hafta İçi / Hafta Sonu yazımı kontrol edildi.

## Önceki kontroller

İlk ana yerin seçili hafta içi günleri, ilk şubeye hafta sonu günü seçilmesi, ana yerde yalnız cumartesi/yalnız pazar ataması, kapalı cumartesideki mazeretin açık pazarı engellememesi, yalnız pazarla bekleyen nöbet hakkının kapanması, tüm günler kapalıyken atama/döngü ilerlemesi olmaması test edildi.

Ek şubeyi silerken geçmiş/bugün ve sayımların korunması, yalnız gelecekteki atamaların kaldırılması, sonraki dağıtımda silinen yere atama yapılmaması, eski çizelgede geçmişinin görünmesi, geri alınca yeniden kullanılabilmesi test edildi. İlk iki temel yerin silinmemesi de doğrulandı.

Gizleme testleri: şube görünümden çıkıyor; görev kayıtları, personel sayımları ve otomatik dağıtım değişmiyor. Gizli yerin PDF sütunu çıkarılıyor; arşiv/gizlilik bayrakları ve seçili günler kayıttan yeniden açılınca korunuyor. Açık kurumsal mavi (#2A6FB1) ve beyaz düğme yazısı Android üzerinde doğrulandı.

Önceki sürümün 32 motor ve 14 Android kontrolü de geçti: mazeret/sıra, perşembe/cuma dinlenme, 100 haftalık yük dengesi, kayıtlı liste revizyonu/istatistik güncellemesi, onaylı tekrar şube ataması, Türkçe alfabetik tablo, yer adlarını koruyan geçiş, sade çok sayfalı PDF, paylaşım URI güvenliği, açık/koyu tema ve ekran açılışları.

## Sınırlar

Fiziksel telefon veya tüm Android sürümleri test edilmedi. WhatsApp'ta gerçek alıcıya gönderim yapılmadı; PDF üretimi ve Android paylaşım URI erişimi test edildi. APK debug imzalıdır. Canlı telefon eşitlemesi ve yönetici hesapları uygulanmadı; APK cihazda çalışır. Yetki/sunucu planı ORTAK-KULLANIM.md içindedir.
