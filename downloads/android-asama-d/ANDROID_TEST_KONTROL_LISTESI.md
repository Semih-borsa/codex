# Android cihaz test kontrol listesi

**Durum: hazırlanmıştır; gerçek cihazda henüz uygulanmadı.** Kutuların hiçbiri otomatik olarak geçti sayılmaz. Önceki 207 otomatik test başarılıdır; Android WebView/kurulum/dosya seçici testinin yerine geçmez. Bu aşamada yeni özellik geliştirilmez.

Test edilecek APK: **Finans-D-bagimsiz-test.apk**. Görünen ad **Finans Test D**, kimlik `com.butcetakip.app.devtest`.

## 0. Hazırlık ve veri ayrımı

- [ ] Gerçek uygulamadan dosya yedeği al; bilgisayara kopyala; başlangıç bakiyesi/bekleyen/kalan/dönem ekran görüntüsünü sakla.
- [ ] [Güvenli kurulum belgesindeki](./ANDROID_GUVENLI_KURULUM_VE_TESLIM.md) APK SHA-256 değerini indirdiğin dosyayla karşılaştır.
- [ ] Bağımsız APK'yı kur. Telefon mevcut uygulamayı değiştirmeyi teklif ederse dur; yanlış paket olabileceğini kontrol et.
- [ ] **Finans Test D**'nin ayrı uygulama olduğunu doğrula. Mevcut uygulamanın hâlâ ayrı açıldığını ve kayıtlarının yerinde olduğunu kontrol et.
- [ ] Cihaz modeli, Android sürümü, Android System WebView sürümü, uygulama sürümü `1.2.0-d-test`, saat dilimi ve test tarihini aşağıya yaz.
- [ ] Cihaz tarihini ileri/geri değiştirme; gerçek uygulamanın otomatik dönem/ödeme davranışını etkileyebilir. Gelecek tarihli dağıtımları plan ayrıntısından kontrol et.

| Bilgi | Doldurulacak |
|---|---|
| Cihaz / Android / WebView | |
| Test tarihi / saat dilimi | |
| APK SHA-256 | |
| Testi yapan | |
| Gerçek uygulamanın ilk bakiye/bekleyen/kalanı | |

### Ortak sentetik başlangıç

Her grubun tablo karşılaştırması bağımsızdır; diğer grupların borçlarını biriktirerek kıyaslama.

İstersen [20.000 TL sentetik başlangıç dosyasını](./android-dogrulama/SADECE_TEST_SENTETIK_20000_TL.json) **yalnız Finans Test D → Ayarlar → Yedeği Yükle** ile önizleyip kullan. Bu dosya gerçek veri içermez; test uygulamasının ilgili işlem/kart/kredi/ödeme/taksit listelerini değiştirir. **Gerçek finans uygulamana yükleme.** Bu test dosyasını mevcut gerçek yedeğinin yerine koyma. Her grup öncesi test sonuçlarını dışa aktar, sonra yalnız test uygulamasında bu başlangıca dön.

Dosyadaki tek gelir tarihi **16 Ekim 2026**, dönem günü 15'tir. Test günün bu dönem dışındaysa gelir kaydını test uygulamasında bugünün tarihine düzenle ve güncel dönemi seç. Otomatik devir gibi ek kayıt oluşmuşsa tabloyla karşılaştırmadan önce bu yalnız sentetik kaydı incele. Başlangıç kesin olarak **BAKİYE 20.000 / BEKLEYEN 0 / ÖDEMELER SONRASI 20.000 TL** olmalı. Sağlanmıyorsa sayıları zorla doğru kabul etme; farkı kaydet.

Bu dosyanın şema/bağ doğrulaması ve başlangıç motor hesabı bulutta kontrol edildi. Cihazdaki geri yükleme henüz denenmedi. Dosya yüklemek istemezsen aynı durumu yalnız yapay kayıtlarla elle oluşturabilirsin.

Tablolarda **gerçek bakiye**, BAKİYE tutarı; **bekleyen**, BEKLEYEN ÖDEMELER TOPLAMI; **kullanılabilir**, ÖDEMELER SONRASI KALAN BAKİYE anlamındadır. Birikim, başka hesap/kart/abonelik olmamalı. Test gelirleri/ödemeleri kullanılan güncel maaş döneminde olmalı.

## A. Kredi taksiti — ödeme, mükerrer düşmeme ve geri alma

1. [ ] Ortak başlangıcı sağla.
2. [ ] Menü → Krediler → Ekle: ad `ANDROID TEST KREDISI`, faiz dahil toplam **5.000 TL**, vade **1**, ilk taksit tarihi bugün. Kaydet.
3. [ ] Özet: BAKİYE 20.000, bekleyen 5.000, kullanılabilir 15.000 TL.
4. [ ] Krediler → Taksitleri aç → ödeme kutusuna dokun. Onayda önce vazgeç; hiçbir tutar/kayıt değişmemeli.
5. [ ] Tekrar dokunup onayla. İşlemler'de bugün tarihli tek bağlı kredi ödeme kaydı olmalı; vade tarihi işlem tarihi diye kullanılmamalı.
6. [ ] Özet: gerçek bakiye 15.000, bekleyen 0, kullanılabilir yine 15.000. 10.000'e düşmemeli.
7. [ ] Raporlar'da nakit görünümü 5.000 çıkışı göstermeli. Harcama görünümüne ikinci alışveriş gideri eklenmemeli.
8. [ ] Uygulamayı arka plana alıp aç; ardından son uygulamalardan kapatıp yeniden aç. Tek ödeme korunmalı.
9. [ ] Krediler'deki aynı kutuyla ödemeyi geri al, onayla. Bağlı ödeme kaldırılmalı; bekleyen taksit geri gelmeli.
10. [ ] Yeniden açınca 20.000 / 5.000 / 15.000 korunmalı. Geçmiş devir kaydı keyfi değişmemeli.

| A aşaması | Gerçek bakiye | Bekleyen | Kullanılabilir |
|---|---:|---:|---:|
| Başlangıç | 20.000 | 0 | 20.000 |
| Taksit bekliyor | 20.000 | 5.000 | 15.000 |
| Ödendi | 15.000 | 0 | 15.000 |
| Geri alındı | 20.000 | 5.000 | 15.000 |

Eski `odendi:true` kredi kaydı bulunan **sentetik eski yedek** ayrıca denenirse: açılışın geçmişe otomatik 5.000 TL gider oluşturmadığını kontrol et. Eski manuel ödeme aynı isim/tutarla otomatik eşleştirilmemeli. Gerçek eski yedeğini bu test için değiştirme.

## B. Kart ekstresi — kısmi/tam ödeme, bağımsız borç ve dönem geçişi

1. [ ] Ortak başlangıca dön; Ayarlar'da yeni bir test kartı ekle. Dönem içi/provizyon **0**, ekstre **5.000 TL**. Kesim tarihi doğrulanmış bir geçmiş/güncel tarih, vade bugünün içinde olduğu maaş döneminde olsun.
2. [ ] Kaydet, ekranı kapat/aç. Ekstre 5.000 kalmalı; dönem içi ve provizyonun sıfır olması ekstreyi silmemeli.
3. [ ] Borcu güncellemek nakit ödeme oluşturmamalı. Özet 20.000 / 5.000 / 15.000.
4. [ ] Kartta Ödendi olarak işaretle → ödeme tarihi bugün → önce onaydan vazgeç; sonra onayla. Nakit bakiye 15.000, kalan ekstre 0, kullanılabilir 15.000.
5. [ ] Kısmi ödeme testi için, **yalnız bu yapay kartta**, İşlemler'de ekstre ödemesini Düzenle → tutarı **2.000** yap. Mevcut ödeme penceresi tutar alanı sunmuyorsa varmış gibi kabul etme; burada kısmi tutarı İşlemler üzerinden düzenliyoruz. Henüz taksit payı dağıtma.
6. [ ] Özet gerçek bakiye 18.000, kalan ekstre 3.000, kullanılabilir 15.000.
7. [ ] Kartın kalan 3.000'ini bugün öde. Toplam nakit çıkışı 5.000; kullanılabilir yine 15.000.
8. [ ] İşlemler'de önce 3.000'lik, ardından 2.000'lik ödeme kayıtlarını ayrı ayrı silerek geri al; özgün ekstre değişmemeli.
9. [ ] Maaş dönemi oklarıyla başka döneme geçip geri gel. Ödenmemiş 5.000 TL borç kaybolmamalı; dönem değiştirmek ödeme üretmemeli. Diğer dönemin nakit bakiyesi gelir/devir farklı olabileceğinden otomatik 20.000 varsayma.
10. [ ] İlk 5.000'lik doğrulanmış ekstreyi değiştirmeden **yeni kesim/vade tarihleriyle** yeni 2.000 TL ekstre oluştur. Eski ödenmemiş 5.000 korunmalı; toplam borç 7.000 olmalı. Aynı doğrulanmış ekstre kimliğini farklı tutarla değiştirme denemesi engellenmeli.
11. [ ] Geçmiş dönem bilgisinin olmadığı yerde bugünkü tutar geçmişe kopyalanmamalı; karşılaştırılamayan dönemde sahte artış/azalış yüzdesi çıkmamalı.

| B aşaması (tek 5.000 TL ekstre) | Gerçek bakiye | Bekleyen | Kullanılabilir |
|---|---:|---:|---:|
| Ödenmemiş | 20.000 | 5.000 | 15.000 |
| 2.000 ödenmiş | 18.000 | 3.000 | 15.000 |
| Toplam 5.000 ödenmiş | 15.000 | 0 | 15.000 |
| Tüm ödemeler geri alınmış | 20.000 | 5.000 | 15.000 |

Raporlarda ekstre ödemesi **nakit çıkışı** olmalı; alışveriş harcamasını ikinci kez artırmamalı. Harcama/nakit görünümünü ayrı kontrol et.

## B2. Provizyon → dönem içi ve günlük fark

- [ ] Ayrı ortak başlangıçta test kartının ekstre borcu 0 olsun; bakiyeden düş seçeneğini açık tut.
- [ ] İlk banka durumu: dönem içi **10.000**, provizyon **5.000**. Bunu başlangıç gözlemi say; günlük harcama ekranının mevcut açılış değerini not et.
- [ ] Güncelle: **11.000 + 4.000 = 15.000**. Yeni alışveriş farkı **0 TL**, yeni nakit çıkışı yok. Önceki günlük harcamanın üstüne 15.000 eklenmemeli.
- [ ] Güncelle: **14.000 + 2.000 = 16.000**. Eklenen alışveriş farkı **1.000 TL**; 16.000 TL yeni harcama sayılmamalı.
- [ ] Harita gün ayrıntısı, kategori trendi ve haftanın günleri toplamlarını güncelleme öncesine göre karşılaştır. İlk banka gözlemiyle sonradan eklenen farkı karıştırma.
- [ ] Bakiyeden düş kapalı/açık tercihini ayrı dene. Dönem içi/provizyon payı açıkken aşağıdaki tablo geçerli; nakit bakiye bu gözlemlerle düşmemeli.

| B2 banka durumu | Gerçek bakiye | Ayrılan | Kullanılabilir |
|---|---:|---:|---:|
| 10.000 + 5.000 | 20.000 | 15.000 | 5.000 |
| 11.000 + 4.000 | 20.000 | 15.000 | 5.000 |
| 14.000 + 2.000 | 20.000 | 16.000 | 4.000 |

## C. Yedekleme ve geri yükleme

1. [ ] Yalnız test uygulamasında bir kişisel kategori, kart, kredi ve tarihli plan içeren sentetik durum oluştur; tüm sayaçları/bakiyeleri not et.
2. [ ] Ayarlar → Yedek Al. Android paylaş/kaydet ekranını tamamla; dosya yöneticisinde gerçek JSON dosyası olduğunu kontrol et. Uygulama içindeki kopya tek başına yeterli değil.
3. [ ] JSON'u bilgisayara kopyala; kopyanın SHA-256 değeri dosyayla aynı olmalı. Orijinali değiştirme.
4. [ ] Testte küçük bir ek kayıt oluştur. Önceki test yedeğini seç; önizleme eklenecek/silinecek/değişecek kayıtları doğru göstermeli. **Vazgeç** de: kayıtlar aynı kalmalı.
5. [ ] Aynı dosyayı tekrar seçip önizlemeden onayla. Önceki test durumu, kart/ekstre/ödeme/taksit bağları ve kişisel kategori korunmalı. Uygulamayı kapat/aç.
6. [ ] Test dosyasının ayrı bir kopyasını bozup geçersiz JSON yap; yükleme reddedilmeli ve mevcut kayıtlar değişmemeli. Gerçek yedeği bozma.
7. [ ] Ayrı kopyada `ft_islemler` alanını kaldır veya paketi kısmi işaretle. Alan listesi uyumsuzluğu/eksik çekirdek gerekçesiyle tam geri yükleme engellenmeli.
8. [ ] **Açık boş liste** testi yalnız sentetik dosyada yapılmalı: `ft_islemler:[]` ve onunla tutarlı alan listesi boş işlem bölümüdür; önizleme silinecekleri göstermeli. Yanlışlıkla onaylamak yerine önce vazgeçip değişmediğini doğrula.
9. [ ] Sentetik `finora-v1`, `finora-v2`, `butce-takip-v1` örneklerini tek tek dene. Biçim tanınmalı; eski kayıtlar kendiliğinden tarihli plana veya yeni giderlere dönüştürülmemeli. Gerçek yedekleri sırf test için elle başka biçime dönüştürme.
10. [ ] Uygulamadaki "Son yüklemeden önceki verileri önizle" seçeneği yükleme öncesi durumu gösterebilmeli. Geri almayı yalnız test verisinde dene.
11. [ ] Yazma başarısızlığı/süreç tam yazma ortasında kesilmesi: gerçek cihazda güvenli hata enjeksiyonu yapılmadıysa **denenmedi** yaz. Telefon depolamasını zorla doldurma; gerçek uygulamada hatayı tetikleme. Sentetik Chromium yazma hatası testleri başarılı olsa da cihaz atomikliği garanti edilmiş değildir.

## D. Tarihli kart taksit planı

### D1 — Takvim, kimlik ve kuruş

- [ ] Menü → Taksitli Harcamalar → Ekle; gerçek kart yerine test kartını seç. **Tarihli plan** yolunu kullan; özetin eski hızlı takip formuyla karıştırma.
- [ ] Toplam 100 TL, vade 3, alışveriş 9 Ekim 2026, doğrulanan ilk kesim 10 Ekim 2026, kesim günü 10, ödeme günü 20: 10 Ekim / 10 Kasım / 10 Aralık ve **33,33 / 33,33 / 33,34 TL** beklenir.
- [ ] Aynı planın yeni örneğinde ilk doğrulanan kesim 10 Kasım: Kasım / Aralık / Ocak 2027; Ekim'e taksit yazılmamalı.
- [ ] İlk tarihi boş bırak: kaydetme engellenmeli; tahmini tarih üretilmemeli.
- [ ] Kesim tercihi 10, banka ilk kesim 12 Ekim ve ilk ödeme 22 Ekim, ödeme tercihi 20: ilk 12/22; sonra 10 Kasım/20 Kasım ve 10 Aralık/20 Aralık.
- [ ] 31 Ocak 2027 ilk kesim, tercih 31: 31 Ocak / 28 Şubat / 31 Mart.
- [ ] 31 Ocak 2028 ilk kesim, tercih 31: 31 Ocak / 29 Şubat / 31 Mart. Ödeme günü 5 seçilirse sonraki ayın 5'i olmalı.
- [ ] Tercih 29 ve 30 için de kısa Şubat sonrası Mart'ta eski güne dönüldüğünü kontrol et.
- [ ] 1, 2, 3, 6, 12 taksitli örnekler oluştur; toplam kuruş tutarı alışveriş toplamıyla birebir eşleşmeli. 100 TL / 6: beş adet 16,66 ve son 16,70; /12: on bir adet 8,33 ve son 8,37.
- [ ] İkinci test kartında ikinci plan oluştur: taksitler yanlış karta/ekstreye bağlanmamalı. Aynı isimli kartlarda da kimlik bağı korunmalı.
- [ ] Kapat/aç ve yedekle/geri yükle sonrası plan/taksit/ekstre kimlikleri ve tutarlar aynı olmalı. Kesilmiş eski ekstre değişmemeli.

Bu sabit tarihli örnekler **tarih dağıtımını** sınar. Telefon tarihi/dönemi farklıysa bakiye tablolarıyla doğrudan kıyaslanmaz; cihaz saatini değiştirme.

### D2 — Banka borcu, kısmi ödeme, iade ve geri alma

Bu grup için ortak 20.000 TL başlangıcına dön. İlk taksitin vadesi seçili güncel döneme gelecek, kesimi alışverişten sonra ve bugünden geç olmayan tarihler seç. **Diğer iki taksit seçili dönem sonrasına düşmeli**; aksi halde tablodaki ilk rezerv 33,33 olmaz.

1. [ ] Banka borcu henüz olmayan test kartında 100 TL / 3 tarihli plan oluştur. Nakit ve alışveriş gideri oluşmamalı. Yalnız ilk taksit 33,33 TL bekleyen paya girmeli.
2. [ ] Aynı karta aynı ilk kesim tarihli **100 TL kesin banka ekstresi** gir; dönem içi/provizyon 0. Banka borcu toplamı 100 olduğunda ek 33,33 ikinci kez ayrılmamalı.
3. [ ] Plan ayrıntısından yalnız aynı kart/kesim tarihli kesin ekstreyi seçip açıkça bağla. Mevcut dönemin plan toplamı 33,33 ile banka tutarı 100 arasındaki **66,67 fark** gösterilmeli; diğer alışverişler olabileceğinden otomatik birleştirilmemeli.
4. [ ] Kartın normal ödeme yolundan 100 TL'yi bugün öde; sonra İşlemler'de bu yapay ödemeyi **20 TL** olarak düzenle (henüz pay dağıtma). Bu mevcut arayüzdeki kısmi ödeme denemesidir.
5. [ ] Taksit ayrıntısında Kısmi ödeme payı → bu aynı kart/ekstre ödemesini seç → **20 TL** pay ver. Yeni nakit ödeme oluşmamalı; ilk taksit kalanı **13,33 TL**.
6. [ ] Aynı 20 TL'nin bir kez daha dağıtılması veya daha büyük tutar engellenmeli. Pay dağıtıldıktan sonra İşlemler'de tutarı pay toplamının altına indirme denemesi reddedilmeli.
7. [ ] İlk takside **10 TL iade**, tarih bugün, **bankanın ekstresine yansıdı** işaretli gir. Özgün ekstre 100 kalmalı; net banka borcu 90, ödeme sonrası kalan 70, taksit kalanı 3,33 TL olmalı. Nakit gelir oluşmamalı.
8. [ ] İadeyi geri al: banka kalan 80, ilk taksit kalan 13,33. Başka kart değişmemeli.
9. [ ] İşlemler'den gerçek 20 TL ödemeyi silerek geri al. İlk taksit 33,33'e dönmeli; pay kaydı geçmiş bağıyla durabilir ama hesapta sayılmamalı.
10. [ ] Ayrı denemede 10 TL iadeyi **bankaya yansıdı işareti kapalı** gir: yalnız plan neti değişmeli; banka borcu ve gerçek nakit değişmemeli.
11. [ ] Uygulamayı son uygulamalardan kapatıp aç, ardından cihazı yeniden başlat. Aynı durum korunmalı; kendiliğinden ikinci işlem oluşmamalı.

| D2 aşaması | Gerçek bakiye | Bekleyen | Kullanılabilir |
|---|---:|---:|---:|
| Bankasız plan, yalnız ilk taksit vadede | 20.000,00 | 33,33 | 19.966,67 |
| Banka kesin borcu 100 TL | 20.000,00 | 100,00 | 19.900,00 |
| 20 TL ödeme ve pay dağıtımı | 19.980,00 | 80,00 | 19.900,00 |
| 10 TL bankaya yansıyan iade | 19.980,00 | 70,00 | 19.910,00 |
| İade geri alındı | 19.980,00 | 80,00 | 19.900,00 |
| 20 TL ödeme de geri alındı | 20.000,00 | 100,00 | 19.900,00 |

### D3 — İptal ve tamamlandı durumu

- [ ] Bankaya bağlanmamış ayrı planda ilk kesim geçmişte, sonraki iki kesim gelecekte olsun. Planı iptal et: yalnız gelecekteki bağlı olmayan satırlar iptal; geçmiş satır/özgün plan silinmemeli.
- [ ] Kesin ekstreye bağlı taksit/gerçek ödeme, plan iptaliyle silinmemeli. Banka borcu iptal edildi diye sıfırlanmamalı.
- [ ] Tek taksitli ve bankaya bağlı, tutarları uyumlu planı tamamen öde: tamamlandı; ödemeyi geri al: aktif/bekleyen. Bakiye de geri dönmeli.
- [ ] Plan toplamından küçük, tamamen ödenmiş banka ekstresi daha büyük planı varsayımla tamamen ödenmiş saymamalı; fark görünür olmalı.

## E. Korunan ekranlar ve genel cihaz akışı

- [ ] Açık/koyu/gri tema; halka/ibre tercihi ve animasyon süreleri aynı. Üst başlık/dönem sabitliği, alt menü ve klavye açıldığında form erişimi.
- [ ] İsimler, kişisel kategoriler, kart logoları ve rozetler korunuyor.
- [ ] Ödemeler bekleyen tek liste; özet yalnız bugün ve sonraki 3 gün. Bu görsel filtre bekleyen bakiye hesabının tamamını daraltmamalı.
- [ ] Harita gün ayrıntısında gelir/gider toplamları ve satırlar tutarlı; ekstre ödemesi nakit görünümünde, yeni alışveriş gibi iki kez görünmüyor.
- [ ] Haftanın günleri seçili maaş dönemindeki toplamlarla çalışıyor; ortalama gibi yorumlanmıyor.
- [ ] Sabit gider/ekstre günlere yayılarak dev tahmin oluşturmuyor. Örnek: sabit 10.000 + iki ekstre 10.000 ve 20.000, değişken harcama yok → **40.000 TL** taban/tahmin. Bu ayrı senaryodur, 20.000 TL bakiye tablosuyla karıştırma.
- [ ] Uçak modunda temel kayıtları açma/kaydetme, geri tuşuyla modal kapanması, yeniden açılış. Dosya paylaşma uygulaması internet gerektirebilir; yerel kayıt testiyle karıştırma.
- [ ] Test sonunda gerçek uygulamayı açıp hiçbir test kaydının oraya taşınmadığını, başlangıç tutarlarının değişmediğini doğrula. Gerçek hayatta arada işlem yaptıysan değişikliğin nedenini ayrıca not et.

## Sonuç kaydı ve durma koşulu

Her senaryoya **Geçti / Başarısız / Denenmedi** yaz; beklenen ve görülen üç tutarı kaydet. Hata varsa hangi ekranda, işlem sırası, tarihler, APK hash'i, ekran görüntüsü ve yalnız sentetik test yedeğini sakla. Aynı ödemeyi tekrar tekrar yaparak sonucu düzeltmeye çalışma.

| Senaryo | Beklenen bakiye / bekleyen / kalan | Görülen | Sonuç / kanıt |
|---|---|---|---|
| A ödeme | 15.000 / 0 / 15.000 | | |
| A geri alma | 20.000 / 5.000 / 15.000 | | |
| B kısmi ödeme | 18.000 / 3.000 / 15.000 | | |
| D2 kısmi + banka iadesi | 19.980 / 70 / 19.910 | | |
| C iptal/bozuk yedek | Kayıtlar değişmedi | | |
| Kapat/aç ve cihaz yeniden başlatma | Önceki durum aynen kaldı | | |
| Gerçek uygulamanın verileri | Testten etkilenmedi | | |

Bakiye mükerrer düşerse, veri eksilirse, geçmiş ekstre değişirse, dosya yedeği alınamıyorsa veya veri ayrımı doğrulanamıyorsa **cihaz doğrulaması tamamlandı sayılmaz**. Gerçek uygulamaya geçiş durdurulur; hata raporlanır. Bu belge yeni geliştirme onayı değildir.
