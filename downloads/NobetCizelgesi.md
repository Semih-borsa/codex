# Nöbet Çizelgesi 2.4

**2.4:** çizelgede bugünün satırına bal sarısı vurgu ve Bugün etiketi. Cumartesi/pazar ortak hafta sonu satırında görünür. Açık/koyu tema, başka hafta ve gün değişimi desteklenir. [Ayrıntılar](../nobet-cizelgesi/docs/GUN-VURGUSU-2.4.md).


**2.3:** dengeli haftalık dağıtım (uygun dört personel/beş gün: 1–1–1–2), kısıtlı günlerin öncelikli değerlendirilmesi, alfabetik personel ve mazeret türü başlıklarıyla açılır liste, ortalanmış ay/güneş tema simgesi ve çizelge/istatistik/PDF satırlarında siyah çizgiler. [Ayrıntılar](../nobet-cizelgesi/docs/DAGITIM-2.3.md). Önceden onaylı haftaları değiştirmek için yeni dağıtım önerip kaydedin.


**2.2:** ekrana sığan haftalık tablo, tek hafta sonu nöbetçisi satırı; Supabase hesap/giriş, e-posta kodu, davet, yönetici yetkisi ve sürüm kontrollü eşitleme istemcisi. [Tablo görünümü](../nobet-cizelgesi/docs/TABLO-2.2.md) · [Sunucu kurulumu](../nobet-cizelgesi/docs/ORTAK-KULLANIM.md). **Henüz canlı proje bağlı değil; iki gerçek telefonda bulut eşitlemesi doğrulanmadı.**

**Yeni arayüz:** Bütçe Takip’ten ilham alan kurumsal mavi özet kartı, çizgi simgeler, yüzen alt menü, personel kartları ve açık/koyu tema. [Ekran görüntüleri](../nobet-cizelgesi/docs/TASARIM-2.0.md)

- [APK 2.4 indir](NobetCizelgesi-2.4.apk)
- [Güncel APK](NobetCizelgesi.apk)
- [Android Studio kaynak ZIP](NobetCizelgesi-Android-Studio.zip)
- [Kullanım](../nobet-cizelgesi/README.md) · [Testler](../nobet-cizelgesi/docs/TESTLER.md)

**Resmî Tatiller:** otomatik takvim, manuel ekleme/düzenleme/devre dışı bırakma; ayrı görev yeri istatistikleri ve PDF. Yarım gün 0,5; tam gün 1. Normal sayımlar korunur. Dinî takvim 2020–2032 aralığındadır. [Ayrıntılar ve kapsam](../nobet-cizelgesi/docs/RESMI-TATILLER.md).

Android 8.0 ve üzeri. Mevcut uygulamayı **kaldırmadan** APK üzerinden güncelleyin; aynı imza korunur. APK debug imzalıdır.

**Manuel mazeret istisnası:** mazeret/kısıtlı personeli seçerken uyarı gösterilir; onaylarsanız yalnız seçili görev için uygulanır. Mazeret silinmez, otomatik dağıtım onu dikkate almaya devam eder.

**Aynı gün farklı şubelere atama:** kalemden seçimde mevcut görev yerleri gösterilir; teyit verirseniz ek görev taslağa eklenir. Vazgeçerseniz değişmez. İstisna kayıtla korunur; mazeret için görev kapsamlı teyit gerekir; dinlenme kuralları sürer.

**Kalemden seçim sonrası konum korunur:** personel seçince veya alanı boş bırakınca sayfa en üste dönmez.

**Birden fazla ve haftalık kısıt:** aynı personele ayrı kayıtlar eklenebilir. Mazeret bölümünde **Her Hafta Tekrarla** seçerek Her Pazartesi/Her Çarşamba gibi günler belirleyin. Haftalık modda tarih alanları yoktur. **+ Kısıt Ekle** ile aynı personele ek gün/görev yeri satırları eklenebilir. Tarihli izin/rapor ayrı modda kullanılır. Genel ve görev yerine özel kısıtlar birlikte denetlenir.

**↑ / ↓ ile personel taşıma:** isimleri silmeden sırayı değiştirip **Sırayı Kaydet** ile onaylayın. Kaydetmeden vazgeçmek için **Sıra Değişikliklerini İptal Et** kullanılabilir.

**Ayarlar → Hafta Sonu Nöbet Sırası:** personelleri 1. Sıra, 2. Sıra şeklinde düzenleyin. Yeni personel eklerken sıra numarası girilebilir. Yeni sıralama sonraki otomatik öneride 1. sıradan başlar; onaylı listeler değişmez, bekleyen mazeret hakları korunur.

**İstatistik → İstatistikleri PDF Olarak Paylaş:** yalnız tablolardan oluşan PDF dışa aktarılır. Uzun isimler bölünür; çok personel/şube ek sayfalarda korunur. Geçmiş ve gelecek ayrı tablolardır.

İstatistiklerde harfli başlıklar kaldırıldı; sıkı tablo düzeni yatay kaydırmadan ekrana sığar. Çok sayıdaki şube alt alta devam eder. PDF boşlukları azaltıldı; arayüz etiketlerinin baş harfleri büyütüldü.

Ayarlar'dan eklenen şubeyi silebilir/geri alabilir, ilk iki yer dahil çalışma günlerini değiştirebilirsiniz. Geçmiş kayıtlar ve sayımlar korunur. Gün ayarları yeni dağıtımlarda kullanılır. Görünüm seçimi liste oluştururken sorulur: gizlenen şubeler yalnız çizelge/PDF'den çıkar; kayıt ve istatistikleri kalır. Mavi daha açık kurumsal tona güncellendi.

Hesap/yönetici/eşitleme kodu vardır; canlı kullanmak için size ait Supabase projesini kurup bağlamanız ve kendi projenizde doğrulamanız gerekir. Uygulamayı kaldırmak/verilerini silmek kayıtları siler; PDF veri yedeği değildir.
