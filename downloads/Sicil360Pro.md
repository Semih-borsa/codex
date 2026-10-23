# Sicil360 Pro 1.3 — Android

Excel rehberinden sicille kişi arama, kendi düz metin şablonunuzu seçme ve son onaydan sonra WhatsApp sohbetine hazırlama. Mesaja ad, soyad veya sicil otomatik eklenmez. Gönder düğmesine WhatsApp'ta kullanıcı basar.

- [Android APK indir](Sicil360Pro-1.3.apk)
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

## 1.3 Kişi Takibi Ve Raporlar

- Tam sicil girildiğinde kişi kartı otomatik açılır; isim veya kısmi telefon eşleşmesi otomatik kişi seçmez.
- Kişi kartındaki **Takip Ve Notlar** bölümünde İşlem Bekliyor / Tekrar İletişim / Tamamlandı durumunu ve kişi notunu kaydedin.
- Alt menü **Takip → Takip**: Duruma ve sicil/ad soyada göre filtrelenen, isim sıralı kişi listesi. Notları düzenleyin veya kişiye mesaj hazırlayın.
- **Takip → Geçmiş**: Sicil, mevcut kişi adı, hazırlanırken kaydedilmiş şablon adı, hazırlama zamanı ve kullanıcı tarafından işaretlenen durum. Kişi kartından yalnız o kişinin geçmişine geçilebilir. Mesajın tam içeriği geçmişe kaydedilmez.
- **Takip → Raporlar**: Bugün hazırlanan mesajlar, bunlardan gönderildi işaretlenenler, bugün tamamlandı durumuna alınmış ve halen tamamlanmış kişiler; toplam, bekleyen, tekrar iletişim ve tamamlanan kişi sayıları.

Takip durumu, Excel'deki Durum alanından ayrıdır. Yeni/eski kişiler ilk geçişte İşlem Bekliyor olarak başlar. Gönderimi işaretlemek kişi takibini otomatik tamamlamaz; kişi işlemini ayrıca tamamlayın. Kişi takibi ve notlar şifreli yerel kayıtta ve `.s360` yedekte saklanır. Excel güncelleme modu takip durumunu korur; kontrollü yeniden içe aktarma (REPLACE) rehberi değiştirdiği için takip durumları sıfırlanır. Excel dışa aktarımı mevcut rehber alanlarını ve notları içerir; CRM takibini bütünüyle taşımak için şifreli yedek kullanın.

Günlük mesaj sayıları son 1000 geçmiş kaydıyla sınırlıdır; geçmişi temizlemek raporu etkiler. Kişi sayıları güncel rehber ve güncel takip durumlarından hesaplanır. Önceki sürüm yedekleri açılabilir; yeni yedeği eski uygulama sürümleri açamaz. Güncellemeyi kaldırmadan üzerine kurun.
