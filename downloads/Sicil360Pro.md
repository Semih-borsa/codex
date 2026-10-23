# Sicil360 Pro 1.5 — Android

Excel rehberinden sicille kişi arama, kendi düz metin şablonunuzu seçme ve son onaydan sonra WhatsApp sohbetine hazırlama. Mesaja ad, soyad veya sicil otomatik eklenmez. Gönder düğmesine WhatsApp'ta kullanıcı basar.

- [Android APK indir](Sicil360Pro-1.5.apk)
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

## 1.4 Veresiye Borç Ve Ödeme Takibi

**Rehber → Borç Ve Ödemeler** veya **Takip → Ödemeler** bölümünü açın. Borç listesini yükleyin. İlk satır başlık, sütunlar A Sıra No / B Cari Kodu / C Ticari Unvanı / D Adı / E Soyadı / F Bakiye olmalıdır. Standart Excel yükleme de bu başlıkları tanıyıp borç önizlemesine geçer. Cari kod metin olarak saklanır; telefon gerekmez. Bu liste mesaj rehberinden ayrıdır; cari kod otomatik sicil veya telefon kabul edilmez.

- Önizlemede kişi sayısını ve toplamı kontrol edip listeye dönem adı vererek onaylayın. Orijinal Excel değiştirilmez.
- İlk Listenin Genel Toplamı ödemelerden bağımsız, yüklenen satırların sabit toplamıdır. Excel'deki Genel Toplam satırı kişi olarak içe alınmaz. Sabit toplam verilmişse satır toplamlarıyla karşılaştırılır; formüllü toplam yerine satırlardaki gerçek bakiyeler hesaplanır.
- Her kişi kartından **Ödeme Gir**: Tutar girin ya da **Tamamı Ödendi** seçin; son onaydan sonra kaydedilir. Tamamı Ödendi, varsa önceki kısmi ödemeden sonra kalan tutarı kapatır.
- Ödenen tutar geçerli ödeme girişlerinin toplamıdır. Kalan Borç, herkesin sıfırın altına düşmeyen kalan borçlarının toplamıdır. Fazla ödeme başka kişinin borcunu azaltmaz; ayrıca gösterilir. Örnek: 1.000 TL borca 1.100 TL ödeme → kalan 0 TL, fazla ödeme 100 TL.
- Borcu kapanan kişi **aynı listede ve aynı sırada** kalır. Bilgileri soluk ve Tamamı Ödendi görünür; detayı açılabilir. Ayrı pasif listeye taşınmaz. Uzun metinler satıra bölünür; tutarlar otomatik küçülür. Yatay kaydırma yoktur.
- Yanlış ödemeyi ödeme geçmişinden son onayla iptal edin. Kayıt silinmez, İptal Edildi kalır; tutarlar yeniden hesaplanır. Borç yeniden açılırsa kişi normal görünür.

[Boş borç Excel şablonu](../downloads/Sicil360Pro-Borc-Sablonu.xlsx). Her listede en fazla 50.000 kişi, toplam 100 liste; kişi başına 10.000 ödeme, detayda son 100 ödeme gösterilir. Saklama/yedek toplam sınırı 32 MB. Mükerrer cari kod, eksik ad, negatif bakiye, iki kuruş basamağını aşan değer veya hücre formülleri hatayla durdurulur; veri sessizce atılmaz. Cari kod/bakiye formüllerini Excel'de değer olarak kaydedin. Aynı ay/yılda aynı cari kodlar ve aynı ilk bakiyelerden oluşan yeniden yükleme mevcut listeyi açar; ödemeleri sıfırlamaz. Başka listeler Liste Seç ile açılır; eski listeler korunur.

Ödeme verileri yerelde şifreli ve `.s360` yedekte saklanır. Rehber geri alma işlemi ödemeleri geri sarmaz; ödeme hatası kendi geçmişinden iptal edilir. Tüm yedeği geri yükleme ve tüm verileri silme, ödeme listelerini de etkiler. Mevcut rehber Excel dışa aktarımı ödeme defterini içermez; eksiksiz taşımak için şifreli yedek kullanın. Bu kayıtlar kullanıcının girdiği ödeme beyanıdır; banka tahsilat doğrulaması veya otomatik para transferi yapılmaz.

### Aylık Geçmiş Ve Uyarılar

İçe aktarma önizlemesinde **Listenin Ait Olduğu Ay** ve **Yıl** seçilir. Ay/yıl dosyanın gerçek dönemi olmalıdır; yükleme tarihi tek başına dönem sayılmaz. Farklı aylarda aynı kişiler ve aynı bakiyeler bulunabilir. Aynı dönemde bir cari kod ikinci kez borçlandırılmaz; farklı dosyada çakışma varsa hata gösterilir.

Kişi kartları ve ödeme detayları cari koduyla geçmişi karşılaştırır:

- **Son 3 Ayda X Farklı Ayda Listede:** Seçili listenin ayı ve önceki iki takvim ayındaki görünme sayısıdır. Eksik aylara veri uydurulmaz.
- **En Az 3 Ardışık Dönemde Ödeme Kaydı Girilmemiş:** Kişi art arda aylarda pozitif borçla kayıtlı ve o dönemlerde hiçbir etkin ödeme yoksa gösterilir. Yeni liste yüklenince bu kişiler için toplu uyarı çıkar. Bu ifade gerçek dünyada ödeme yapılmadığının kanıtı değildir.
- **Yüklenen Dönemlerde Borcun %X Kadarı Ödenmiş:** Seçili ay dahil geçmiş dönemlerin bakiye toplamına göre ağırlıklı oran. Her dönem için ödenen miktar, o dönemin ilk borcuyla sınırlandırılır; fazla ödeme oranı şişirmez. Kısmi ödeme, hiç ödeme yapılmamasından ayrılır.

Bu hesaplar yalnız yüklenen aylara ve uygulamaya girilen ödemelere dayanır. Gelecek tarihli listeler geçmişe dahil edilmez. Aylık bakiye görüntüleri otomatik olarak yeni borç veya devir borcu diye birleştirilmez; ekrandaki toplamlar seçili listeye aittir. Eski borcun başka aya taşındığı dosyalarda geçmiş oranını dönem görüntülerine göre yorumlayın; toplam gerçek alacak olarak kullanmayın. Cari kod değişirse uygulama aynı kişi olduğunu isimden tahmin etmez.

## 1.5 Borç Kaydı Ve Liste Düzenleme / Silme

- **Takip → Ödemeler → Listeyi Düzenle / Sil:** Liste adı/dönemi değiştirilebilir. **Bu Listenin Tamamını Sil** seçilirse yalnız o liste, kişileri ve o listeye ait ödeme geçmişi son onayla silinir. Diğer dönemler ve ana rehber korunur. Deneme için yüklenen listenin tamamını buradan kaldırabilirsiniz.
- Her borç kartındaki **Düzenle / Sil:** Cari kod, ad, soyad, ticari unvan ve ilk borç düzeltilebilir. Ödeme geçmişi korunur, kişi aynı sırada kalır. İlk borcun elle düzeltilmesi liste genel toplamını değiştirir; bu normal ödeme girişinden farklıdır. Normal ödeme girişi ilk toplamı yine değiştirmez.
- **Bu Kaydı Sil:** Seçili kişi ve bu listedeki ödeme geçmişi son onayla silinir. Diğer aylardaki aynı cari kod ve rehber etkilenmez. Kalan kişiler yer değiştirmez.
- Silme/düzeltme sonrası toplamlar ve geçmiş analizleri mevcut kayıtlardan yeniden hesaplanır. Cari kod değiştirildiğinde geçmiş aylardaki eski kod otomatik değiştirilmez. Aynı dönemde mükerrer cari oluşturulamaz; dönem değişikliği de çakışma açısından denetlenir.

Silinen kişi/listenin ödemeleri de kaldırılır; silme işlemi uygulama içi geri alma ile geri getirilemez. Gerçek kayıtları değiştirmeden önce Ayarlar'dan şifreli yedek alın. Yanlış ödeme girişi için kişi silmek yerine ödeme geçmişindeki iptal işlevini kullanın. Özgün Excel dosyası değiştirilmez.
