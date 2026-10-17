# Geçmiş maaş döneminde kart borcu — analiz ve sentetik test raporu

17 Ekim 2026. **Yalnız inceleme ve ayrı test dosyaları oluşturuldu. Üretim kodu değiştirilmedi; düzeltme veya yeni APK hazırlanmadı. Onay bekleniyor.** Mevcut temalar, grafikler, animasyonlar, finans hesapları, gerçek kayıtlar ve yedekler değiştirilmedi. Daha önce gönderilmiş gerçek yedek bu analizde kullanılmadı; bütün örnekler sentetiktir.

## 1. Sonuç ve kanıtın sınırı

Tutarlı bir doğrulanmış geçmiş dönem özeti 0 TL ise mevcut kod 0 TL gösteriyor. Bu temel senaryo hem hesaplama hem gerçek React ekran testinde geçti. Geçmişte hiçbir kayıt yokken güncel 15.000 TL'nin doğrudan geçmiş kartın ana borç alanına kopyalandığı genel bir hata da yeniden üretilemedi; ana alanda bilinmiyor gösterildi.

Bununla birlikte **eski/uyumsuz kayıt yollarında benzer belirti kesin olarak yeniden üretildi**:

1. Eski üst kayıt tarihiyle birlikte daha yeni bakiye gövdesi bulunması: yeni 15.000 TL, geçmişe ait gözlem gibi okunabiliyor.
2. Erken kayıt tarihli fakat sonraki dönemde kesilecek ekstre: yalnız kayıt tarihine bakan filtre, 15.000 TL'yi geçmişe dahil edebiliyor.
3. Doğrulanmış toplamı ve bileşenleri 0 olan arşivin alt ekstre listesi eski bir kalan içeriyorsa: üst toplam 0 iken ekstre alanı 15.000 TL gösterebiliyor. Aynı tutarsız liste geçmiş kullanılabilir bakiyeyi de etkileyebiliyor.

Bu testler telefonunuzdaki kaydın hangisi olduğunu kanıtlamaz. Standart yeni güncelleme yolu tarihçeyi doğru saklıyor; özellikle ilk iki uyumsuz veri örneği bu yeni yolun normal çıktısı değildir. Ancak geriye uyumluluk doğrulamasından geçebildikleri için güvenli okunmaları gerekir. Gerçek kullanıcı verisi kullanmadan yapılan analizde telefonunuzdaki tekil veri varyantı kesinleştirilmedi.

## 2. Dönem tanımı

Örneklerde maaş günü 15:

- Eylül maaş dönemi: **15 Eylül–14 Ekim 2026**.
- Ekim maaş dönemi: **15 Ekim–14 Kasım 2026**.
- Test saati: **16 Ekim 2026, 12.00, Europe/Istanbul**; yalnız sentetik saattir. Cihazın saati değiştirilmedi.

10 Ekim'de gerçekten kesilen bir ekstre 14 Ekim kapanışında vardır; vadesi 20 Ekim diye geçmişte yok sayılmaz. Bu, takvim ayı ile maaş dönemi arasındaki farktır. 16 Ekim'de kesilecek ekstre ise 14 Ekim kapanışına ait değildir. Gerçek önceki dönem borcunu korumakla gelecekte doğacak borcu geçmişe taşımamak ayrı kurallardır.

## 3. Fonksiyon ve ekran düzeyinde kök nedenler

Satırlar incelenen App.jsx SHA-256 `3d3fa6386aefeee2577c4ccb303c65ff23da8409ee467f336aa2991bb87d3c31` içindir.

### A. Eski kayıt gövdesine varsayılan dönüş

`kartSonDurum`, satır **400–409**, özellikle **403**:

Önce tarihçedeki uygun gözlemleri topluyor. Ardından `i.tarih <= dönem sonu` ise üst gövdedeki **bugünkü** `donemIci/provizyon` alanlarını da `i.tarih` tarihli gözlem olarak ekliyor. Bu gövdenin gerçekten o tarihte gözlendiği ayrıca doğrulanmıyor.

Sentetik eski kayıt: üst tarih 30 Eylül, gövdede 15.000 TL; açık tarihçede 20 Eylül 0 TL, 16 Ekim 15.000 TL. 14 Ekim kapanışı için 16 Ekim tarihçesi doğru elense de gövde 30 Eylül'e bağlanarak eski sıfır gözleminden daha yeni sanılıyor. Sonuç: **15.000 TL**. Güvenli sonuç, doğrulanabilen eski gözlem veya açık eksik/tutarsız bilgi olmalı; yeni gövde eski güne kesin borç diye bağlanmamalı.

Bu tutar `kartBorcDurumu` 481/484/490 → `kartToplamBorcHesapla` 597 → **Özet / kredi kartı / ana borç alanına** ulaşıyor. Aynı dönem içi/provizyon değerleri `donemBakiyeHesapla` 2391–2393 üzerinden ayrılan bütçeyi de etkileyebilir.

### B. Ekstre varlığının tarih denetimi eksik

`kartEkstreBakiyeleri`, satır **432–436**, verilen tarihi ödeme/iade düşümlerinde kullanıyor; ekstre kaydının o tarihte var olup olmadığını kendisi filtrelemiyor.

`kartBorcDurumu`, satır **482**, geçmişte yalnız kimlik + kayıt tarihi + kayıt tarihinin dönem sonundan önce olmasını kontrol ediyor. **Hesap kesim tarihi dönem sonundan sonra mı?** kontrolü yok.

Sentetik uyumsuz kayıt: kayıt tarihi 20 Eylül; kesim 16 Ekim; vade 20 Ekim; tutar 15.000 TL. 14 Ekim kapanışında ekstre henüz kesilmemiş olmasına rağmen toplama giriyor. Bu biçim mevcut yedek doğrulamasından geçebiliyor. Standart `kartEkstreGuncelle` gelecekteki kesimle yeni ekstre oluşturmayı zaten engelliyor; eksik olan, eski/ithal kaydı **okuma** savunması.

Sonuç Özet'te ana borçta; vadesi başka maaş dönemindeyse **“Diğer döneme ait ödenmemiş ekstre”** açıklamasında da görünebilir. Farklı dönemde vadeli gerçek eski borç doğru korunmalı; vade tek başına borcun oluşma tarihi değildir.

### C. Sıfır arşiv ile alt ekstre listesi çelişkisi

`kartBorcDurumu`, satır **480**, doğrulanmış sıfır özeti doğru önceliklendiriyor. Ancak arşivin alt `ekstreler` listesi ile `ekstre/toplam` alanlarının tutarlılığı bu okumada kontrol edilmiyor.

`Ozet`, satır **1957–1962**, ekstre kutusunu `durum.ekstreler[].kalan` üzerinden yeniden hesaplıyor. Ana kart toplamı ise aynı arşivin `toplam` alanından geliyor. Böylece sentetik örnekte:

- Ana toplam: **0 TL**.
- Alt “Ekstre borcu”: **15.000 TL**.

Mevcut yedek doğrulayıcı 1057–1058 üst bileşen toplamını kontrol ediyor, fakat alt ekstre listesinin bu toplamla tutarlı olup olmadığını kontrol etmiyor. `donemBakiyeHesapla` **2383–2388** de liste varsa onu esas aldığı için aynı örnekte 0 TL yerine **15.000 TL bekleyen ekstre** ayırabiliyor. Bu yalnız renk/metin sorunu değildir; geçmiş görünümün kullanılabilir bakiyesini de etkileyebilir. Güncel kaydın yazılarak değişmesi anlamına gelmez.

### D. Eksik bilgi toplu özette sıfıra dönüşüyor

Geçmiş bilgi yokken tek kartın ana alanı doğru biçimde **“Geçmiş veri yok / belirsiz kayıt”** diyor. Fakat Ozet **1964–1981** toplu üst kutuları eksik bileşenleri sayısal 0 olarak toplayıp kesin “Ekstre toplamı 0” ve “Dönem içi ve provizyon 0” gösterebiliyor. Bu alanlarda sıfır ile bilinmiyor ayrımı korunmuyor.

### E. Aşama D ile ilişki

`kartBorcDurumu` ve `kartToplamBorcHesapla` taksit planı parametresi almıyor. Sonraki döneme ait normal tarihli plan **geçmiş kart toplamına veya geçmiş taksit rezervine eklenmedi**; bu kontrol geçti. Dolayısıyla gösterilen 15.000 TL'nin doğrudan D plan toplamından geldiği doğrulanmadı.

Ayrı, somut bir tarihsel sorun bulundu: `donemBakiyeHesapla` **2394**, geçmiş dönem taksit rezervini hesaplarken `kartTaksitYukumlulukleri` işlevine dönem kapanışı yerine bugünü veriyor. 100 TL'lik Eylül taksidine 16 Ekim'de yapılan iade, Eylül kapanış rezervini **100 → 0** yapabiliyor. Bu, yeni borcu geçmişe eklemekten farklı bir hatadır; geçmiş görünümün sonraki olaydan etkilenmesidir. Test H11 ile üretildi. Plan/iade kaydı değiştirilmedi.

## 4. Test sonuçları

| Grup | Geçti | Başarısız | Atlanan |
|---|---:|---:|---:|
| Mevcut 207 test, değişmeden | **207** | **0** | **0** |
| Yeni işlev/hesaplama kontrolleri | 11 | 4 | 0 |
| Yeni gerçek React/Chromium ekran kontrolleri | 4 | 4 | 0 |
| Yeni kontroller toplamı | **15** | **8** | **0** |
| Tüm çalıştırmaların toplamı | **222** | **8** | **0** |

Yeni kontrollerden H13/H14, uyumsuz örneğin mevcut doğrulayıcıdan geçebildiğini kanıtlayan iki **tanı kontrolüdür**; geçmeleri veri güvenliğinin doğru olduğu anlamına gelmez. Diğer 21 kontrol beklenen davranışı sınar. Hiçbir eski test silinmedi, atlanmadı veya beklentisi gevşetilmedi. Yeni davranış testleri üretim kodu düzeltilmediği için bilerek başarısız kalıyor.

| Senaryo | Sonuç |
|---|---|
| Doğru doğrulanmış Eylül 0, Ekim 15.000 | H01 / UI-H01 geçti |
| Geçmiş kayıt yok; güncel borç geçmişe kopyalanmasın | H02 / UI-H02 geçti; toplu sıfır UI-H07 başarısız |
| Gerçek Eylül 5.000 ödenmemiş borç Ekimde de kalsın | H03 / UI-H08 geçti |
| Ekimdeki ödeme Eylül kapanışındaki borcu silmesin | H04 geçti |
| Kesim 10 Ekim, vade 20 Ekim; maaş sınırı 15 | H05 geçti |
| İleri kesim + erken kayıt tarihi | H06 / UI-H05 başarısız: geçmiş 15.000 |
| Yeni alışveriş/standart güncelleme eski sıfırı korusun | H07 / H08 geçti |
| Eski üst tarih + yeni gövde | H09 / UI-H03 başarısız: geçmiş 15.000 |
| Doğrulanmış 0 toplam + tutarsız alt ekstre | UI-H04 / H15 başarısız: alt borç/rezerv 15.000 |
| Sonraki dönemin normal D planı geçmişe eklenmesin | H10 geçti |
| Sonraki iade geçmiş D rezervini değiştirmesin | H11 başarısız: 100 yerine 0 |
| Saf hesap okumaları ve güncele dönüş kayıt/bakiyeyi değiştirmesin | H12 / UI-H01 / UI-H06 geçti |

Kanıtlar `tests/finance-history-review/history-unit.tap`, `history-browser.tap`, `mevcut-207.tap`; yeni kaynak testleri aynı klasördedir. Browser yalnız mobil boyutlu Chromium'dur; gerçek Android WebView testi değildir.

## 5. Önerilen en küçük düzeltme — henüz uygulanmadı

1. **Gözlem kaynağını kesinleştir:** `kartSonDurum` içinde açık tarihçe varsa üst gövdeyi ayrıca eski güne ait gözlem sayma. Tarihçesi olmayan eski kayıtlar için mevcut güvenilir, çelişmeyen tarih yolunu koru; daha yeni olay kanıtı varsa eski gövdeyi kesin geçmiş borç yapma. Veri yetersizse bilinmiyor göster; tarih/tutar uydurma.
2. **Ekstre zaman filtresini tamamla:** mevcut `kartBorcDurumu` geçmiş filtresine doğrulanmış kesim/varlık zamanı kontrolü ekle. Gerçekten kesilmiş eski borcu ve o tarihe kadar yapılan ödemeyi koru; sadece vadeye göre eleme yapma. Tarihleri bilinmeyen/çelişen kayıt kesin geçmiş borç olarak sunulmasın; güncel borç silinmesin.
3. **Tek tutarlı görünüm kullan:** arşiv üst toplamı, bileşenler ve alt listeler çelişirse ekranda ve ayrılan bakiyede farklı kaynaklardan kesin tutar üretme. Doğrulanmış 0 toplam ve 0 bileşenleri koru; çelişkili ayrıntıya uyarı/eksik bilgi uygula. Aynı mevcut motorun sonucu hem Ozet hem bakiye tarafından kullanılsın. Eski arşivi diskte otomatik düzeltme/yeniden hesaplama yapma.
4. **Bilinmiyor durumunu toplamlara taşı:** tamamı bilinmiyorsa “Geçmiş dönem borç bilgisi bulunmuyor”; kısmen biliniyorsa yalnız bilinen toplam + eksik bilgi açıklaması. Bilinmeyeni 0 ile doldurma. Doğrulanmış sıfır ise 0 göster.
5. **Taksit rezervinin tarih kesitini tutarlı yap:** geçmiş görünümde mevcut D yardımcılarına bugünden ileri olmayan seçili kapanış tarihini ver. Sonraki ödeme/iade geçmiş rezervi değiştirmesin; gelecekteki planı geriye taşıma. İptal gibi olaylarda da yalnız kayıtlı olay tarihine dayan; tarih yoksa geçmiş durumu tahmin etme. Bu yan bulguyu aynı onaylı tarih güvenliği kapsamında sınamak gerekir.

Beklenen üretim dosyası değişikliği yalnız **tek App.jsx**: `kartSonDurum`, `kartBorcDurumu`, `donemBakiyeHesapla` ve `Ozet`; D yardımcılarında gerekirse yalnız tarih-kesiti okuması. Yeni finans motoru/veritabanı/bağımlılık/native değişiklik gerekmez. Kalıcı şema dönüşümü, otomatik sıfır kayıt, toplu devir veya ekstre yeniden yazımı önerilmiyor. Yedek biçimlerinin korunması ve A/B/C/D testlerinin tamamının geçmesi kabul şartı olmalı.

## 6. Değişiklik yapılmadığının kontrolü ve sonraki adım

Üç App.jsx kopyası, Android sürüm dosyası ve mevcut test kaynaklarının başlangıç SHA-256 değerleri alınarak inceleme sonunda karşılaştırıldı: **değişen yok**. `baslangic-hashleri.json` ve `degisiklik-kontrolu.json` kanıttır. Yalnız yeni sentetik testler ve bu rapor yazıldı. Build/Capacitor sync/Android APK üretimi yapılmadı; bu analiz için gerekli değildi.

Öneri onaylanırsa önce geri dönüş kaynak kopyası alınmalı, mevcut gerçek dosya yedekleri korunmalı, bu başarısız testler değiştirilmeden düzeltilmeli; mevcut 207 ve sonraki dışa aktarma regresyonları tekrar çalıştırılmalı. Ardından aynı imza/kimlik ve daha yüksek versionCode ile cihaz testi hazırlanabilir. Onay gelmeden üretim koduna geçilmeyecek.
