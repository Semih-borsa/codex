# Sicil360 Pro 1.2 — Android

Excel rehberinden sicille kişi arama, kendi düz metin şablonunuzu seçme ve son onaydan sonra WhatsApp sohbetine hazırlama. Mesaja ad, soyad veya sicil otomatik eklenmez. Gönder düğmesine WhatsApp'ta kullanıcı basar.

- [Android APK indir](Sicil360Pro-1.2.apk)
- [Güncel APK](Sicil360Pro.apk)
- [Android Studio kaynak paketi](Sicil360Pro-Android-Studio.zip)
- [Boş Excel şablonu](Sicil360Pro-Kisi-Sablonu.xlsx)
- [Kullanım kılavuzu](../sicil360-pro/README.md)
- [Test sonuçları ve sınırlar](../sicil360-pro/docs/TESTLER.md)

Android 8.0 ve üzeri, `com.semih.sicil360`. Açık/koyu tema, sütun eşleştirmeli XLSX aktarımı, mükerrer sicil çözümü, kişi ve şablon yönetimi, tek tek onaylı sıralı hazırlama, durum geçmişi, cihaz kilidi ve parola korumalı yedekleme.

Veriler cihazda tutulur; Supabase veya sunucu kurulumu gerekmez. Uygulamayı kaldırmadan önce Ayarlar'dan şifreli yedek alın. Excel çıktısı şifresizdir. APK debugger erişimi kapalı release varyantıdır; kişisel dağıtım için geliştirme sertifikasıyla imzalanır, Play Store paketi değildir.

Gerçek WhatsApp/Business istemcisinde ve fiziksel telefonda uçtan uca sohbet/gönderim doğrulanmadı. Testler mesaj göndermeyen alıcıyla onayı, doğru numara/metni ve normal/Business/tarayıcı yönlendirmesini kontrol eder. Telefonunuzdaki ilk kullanımı kendi kontrolünüzde tek kişiyle doğrulayın.

## 1.1 Tasarım Güncellemesi

Açık temada Petrol & Mint, koyu temada Grafit & Zümrüt renkleri kullanılır. WhatsApp simgeli yeşil geçişli açma düğmesi alt menünün üstünde sabit kalır. Şablon metinleri ve kişi doğrulama/onay akışı korunur. Güncelleme aynı paket ve sertifikayla mevcut kurulumun üzerine yüklenebilir; uygulamayı kaldırmayın.

## 1.2 Şablon Seçimi

Mesaj hazırlama ekranında şablonlar açılır liste yerine alt alta gösterilir. Sağdaki seçim düğmesi veya satıra dokunarak tek şablon seçilir; metni düzenlenebilir önizlemeye gelir. Seçili şablona tekrar dokunmak düzenlenen metni sıfırlamaz. Özel Mesaj seçeneği mevcut metni korur.
