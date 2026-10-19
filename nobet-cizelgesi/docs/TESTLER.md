# Doğrulama — 19 Ekim 2026

- .NET ile ilişkisi olmayan bağımsız Android projesi; JDK 17.0.17, Android SDK 35, build-tools 35.0.0, Gradle 8.9, AGP 8.7.3.
- `testDebugUnitTest`: **20 test, 0 başarısız, 0 atlanan**.
- `lintDebug`: **sorun bulunmadı**.
- `assembleDebug` ve `assembleDebugAndroidTest`: başarılı.
- APK imzası `apksigner verify --verbose` ile doğrulandı (v2, tek imzacı).
- APK gerçek Android 11 / API 30 x86_64 emülatörüne kuruldu.
- Android test çalıştırıcısı: **7 kontrol geçti, 0 başarısız**. Atomik kayıt/yeniden okuma, JSON mazeret döngüsü, PDF sayfasının oluşturulup render edilmesi, paylaşım URI'sinden PDF okuma ve dosya adı bilgisi, yazma/uygunsuz dosya erişiminin reddi, uygulama açılışı ve koyu tema/tema tercihinin saklanması doğrulandı.

Motor testleri: 7 gün/12 atama, her personele en fazla 1 şube günü, cumartesi+pazar aynı kişi, perşembe/cuma iki yerde dinlenme, senelik izin/raporun iki yerde engellemesi ve dahil tarih sınırları, mazeret sonrası ilk uygun hafta sonuna dönüş ve normal sıranın devamı, herkes mazeretliyken zorla atama yapılmaması, yer bazlı kısıt, aynı gün çift yer yasağı, pasif personel ve geçmişinin korunması, önerinin kayda dokunmaması, geçmiş düzenlemede sonraki haftaların geçersizleşmesi, 100 haftada hafta sonu eşitliği ve hafta içi yük dengesi, 6 personel/5 şube günü kapasitesi, kısıtlı günlere uygun arama, manuel hafta sonu devri ve mazeret çakışmasının reddi, bağımsız taslak kopyası.

## Sınırlar

Fiziksel telefonda veya tüm Android sürümlerinde test edilmedi. WhatsApp üzerinden gerçek bir alıcıya gönderim yapılmadı; Android PDF üretimi ve paylaşım URI erişimi doğrulandı. APK debug imzalıdır. Canlı çoklu telefon eşitlemesi ve yönetici hesapları uygulanmadı; bu sürüm cihazda çalışır. Gerekli sunucu/yetki planı ORTAK-KULLANIM.md içindedir.

Bulut ortamı için Android derleme/test adımları önceki Windows proje adımları korunarak `install_script` ve `start_skill` alanlarına kaydedildi. Bu taslak kaydı, ortamın yayımlandığı veya yeni görevde geri yüklemenin test edildiği anlamına gelmez. Etkinleştirmek için ortam ayarlarında inceleyip kaydetmek ve ortamı yayımlamak gerekir.
