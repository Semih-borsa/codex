# Telefonlar arası eşitleme — sonraki sürüm gereksinimi

Bu APK çevrimdışıdır. Bu dosya ortak kullanımın kabul edilen yetki kuralını kaydeder; canlı bulut bağlantısı veya yönetici giriş ekranı uygulanmış değildir.

- İlk yönetici uygulama sahibi kullanıcı olacak.
- Mevcut her yönetici başka bir ekip üyesini yönetici yapabilecek.
- Yalnız yöneticiler personel, mazeret ve çizelgeleri değiştirebilecek.
- Diğer ekip üyeleri güncel onaylı çizelgeyi ve izin verilen istatistikleri görebilecek.
- Davetle katılım ve kullanıcı girişi gerekecek; aynı uygulamayı yüklemek tek başına ekibe erişim sağlamayacak.

## Önerilen altyapı

Supabase Auth + Postgres + Row Level Security + Realtime; küçük ekip için başlangıç ücretsiz kota içinde olabilir. Alternatif Firebase Auth/Firestore'dur. Proje kullanıcıya ait hesapta kurulmalıdır; bu teslimde bir sunucu hesabı/projesi oluşturulmadı. Kurulum için projenin URL'si, mobil kullanım için public/anon anahtar ve etkinleştirilmiş giriş yöntemi gerekir. Service-role veya yönetici anahtarı APK'ya konulmaz ve sohbetten istenmez.

İlk yönetici, doğrulanmış hesap kimliğiyle güvenilir sunucu/proje yönetiminden bir kez atanır. Telefonun “ben ilk kullanıcıyım” talebiyle yönetici olunamaz. Yönetici atama işlemi sunucuda oturumun mevcut yönetici olduğunu doğrulayan işlem üzerinden yapılır; kullanıcı kendi rolünü doğrudan değiştiremez. Ekip kimliğiyle bütün sorgular sınırlandırılır.

Plan/personel/mazeret kayıtları ortak ekipte tutulur. Çizelge yayınlama atomik işlem olur; sürüm numarasıyla eşzamanlı düzenleme çakışmaları gösterilir. Cihaz çevrimdışıyken son onaylı çizelge okunabilir; çevrimdışı değişiklikler sunucu sürümü kontrol edilmeden başkalarının üzerine yazılmaz. Son güncelleme zamanı ve düzenleyen yönetici görünür. PDF paylaşımı canlı eşitlemenin yerine geçmez.

Bulut projesi bağlanıp gerçek iki cihaz/oturumla yetki ve eşitleme testleri geçmeden “telefonlar arası eşitleme çalışıyor” denmemelidir.
