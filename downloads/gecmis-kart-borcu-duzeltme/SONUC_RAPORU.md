# Geçmiş dönem kart borcu — düzeltme sonuç raporu

17 Ekim 2026. Yalnız onaylanan tarihsel gözlem, ekstre, gösterim ve taksit rezervi okuması düzeltildi. Yeni finans özelliğine geçilmedi. Gerçek telefon kayıtlarına erişilmedi; önceki kişisel yedek bu geliştirmede kullanılmadı. Eski kayıtlar, yedekler ve geçmiş arşivler otomatik değiştirilmedi.

## 1. Teslim ve değişen dosyalar

- **[Güncel App.jsx](./App.jsx)** — zip değildir; `finans-takip/src/App.jsx` yerine kaydedilir.
- **[Finans Test D güncelleme APK’sı](./Finans-Test-D-gecmis-borc-duzeltme.apk)** — önceki bağımsız test uygulaması içindir.
- [Ana kimlikli debug APK](./Finans-gecmis-borc-ayni-kimlik.apk) — yalnız eşleşen önceki debug kurulumu için değerlendirilmelidir.
- [Android app/build.gradle](./android/app/build.gradle) — yalnız APK sürüm numarası yükseldi: versionCode 3 → **4**, versionName 1.2.1 → **1.2.2**.

Finans mantığındaki değişikliğin tamamı tek **App.jsx** içindedir. Yeni React kaynak dosyası/veritabanı/paket eklenmedi. Native dosya kaydetme eklentisi, MainActivity ve package.json/lockfile değiştirilmedi. Kendi APK'nı derleyeceksen önceki yedek kaydetme düzeltmesinin native dosyalarını koru; yalnız eski, native köprüsüz APK'ya yeni App.jsx koymak Android kaydetmesini sağlamaz.

| İşlev | Değişiklik |
|---|---|
| `kartSonDurum` | Açık tarihçe ile bugünkü gövdeyi ayırır; tarihi/kimliği belirsiz gövdeyi kesin geçmiş gözlem yapmaz |
| `kartEkstreTarihteGecerli` | Yeni, salt okunur tarih/kimlik koşulu; ikinci finans motoru değildir |
| `kartBorcDurumu` | Kesim/kayıt/vadeyi ayrı denetler; doğrulanmış arşiv bileşenlerini tutarlı okur; eksikliği açıkça taşır |
| `Ozet` | Aynı tarihsel sonucu kart toplamı/alt alan/toplu özette kullanır; bilinmeyeni kesin sıfır göstermez |
| `donemBakiyeHesapla` | Tutarsız alt listeden yanlış rezerv düşmez; geçmiş D rezervini kapanış tarihinden okur |
| `kartTaksitSatirOzeti`, `kartTaksitYukumlulukleri` | Ödeme/iade/iptal etkilerini tarih kesitine göre okur; ileriki olayları kapanışa uygulamaz |

Ayrı test dosyaları eklendi; mevcut test yükleyicisine yalnız yeni tarih koşulunu kaynaktan alma desteği kondu. Mevcut testlerin beklentileri değiştirilmedi.

## 2. Gözlem ve ekstre düzeltmesi

Açık `kartDurumGecmisi` varsa geçmiş değer oradan okunur; üst gövdedeki bugünkü tutar eski güne yeniden bağlanmaz. Güncel, açık tarihli ve tarihçeden daha yeni uyumlu gövde hâlâ okunabilir. Tarihçesiz, çelişmeyen eski gözlemler korundu. Aynı gün belirsiz çoklu kayıtlar veya çelişkili kimliklerden kesin geçmiş toplam üretilmez.

Geçmiş kesilmiş ekstre için kimlik, geçerli kayıt/kesim tarihleri ve tarih sırası denetlenir. Gelecekte kesilecek ekstre geçmiş toplamına girmez. Kesimi geçmişte olan fakat vadesi sonraki maaş dönemindeki gerçek borç korunur. Vade, borcun oluşma tarihi yerine kullanılmaz.

Tarihi bilinmeyen/çelişkili eski ekstre geçmişte kesin borç sayılmaz; açıklama gösterilir. Bu kayıt silinmez ve geçerli güncel borcu sıfırlanmaz. Şekil olarak okunabilen eski yedekler reddedilerek veya otomatik dönüştürülerek düzeltilmedi; güvenlik tarihsel **okuma** katmanındadır.

## 3. Arşiv toplamı, ayrıntı ve kullanılabilir bakiye

Doğrulanmış geçmiş toplam/bileşenler korunur. Alt `ekstreler` listesi tarihsel olarak geçersizse veya toplamla çelişiyorsa kesin ayrıntı olarak kullanılmaz; doğrulanmış bileşenlerden hesaplanır ve tutarsızlık açıklanır. Kaydedilmiş arşiv diskte aynı kalır.

Özetin ekstre tutarı aynı `kartBorcDurumu` sonucundaki doğrulanmış tutardır. Bir yanda 0 TL toplam, diğer yanda eski listeden 15.000 TL kesin ekstre üretme yolu kapatıldı. Önceki ödeme işleminin toplamı değişmesin diye ödeme düğmesine verilen toplamın çift sayılması da önlendi; ödeme kayıt işleyicisi değiştirilmedi.

| Sentetik örnek | Düzeltme öncesi | Düzeltme sonrası |
|---|---|---|
| Geçmiş doğrulanmış toplam 0, eski alt liste 15.000 | Kart ana toplamı 0; alt borç 15.000 | Ana toplam ve ekstre 0; ayrıntı tutarsızlığı açıklanır |
| Aynı örnek, geçmiş nakit 20.000 TL | Yanlış 15.000 rezerv, kalan 5.000 | Rezerv 0, kalan **20.000 TL** |
| Geçerli devreden 5.000 TL ekstre | 5.000 borç | **5.000 borç korunur** |
| Kesim 10 Ekim, vade 20 Ekim; kapanış 14 Ekim | Borç ile vade ayrımı önemli | Kart borcu 5.000; o dönem vadesi gelmeyen ekstre rezervi 0 |

Eksik geçmişte bakiye yalnız bilinen kayıtlardan hesaplanabilir. Ekranda bunun tam tarihsel sonuç olmadığı açıklanır; bilinmeyen yükümlülük için tahmini tutar üretilmez.

## 4. Sıfır ile bilinmiyor ayrımı

- Doğru doğrulanmış geçmiş **0 TL**, 0 olarak gösterilir.
- Hiç bilgi yoksa **“Geçmiş dönem borç bilgisi bulunmuyor”** açıklaması kullanılır; üst toplam kutuları kesin ₺0 göstermez. Eski belirsizlik etiketi uyumluluk için kart ana alanında da korunur.
- Bazı kartlarda bilgi varsa yalnız bilinen bileşenlerin toplamı gösterilir; kaç kartın ekstre/gözlem bilgisinin eksik olduğu belirtilir.
- Arşivin kimlik/tarih/üst bileşenleri çelişiyorsa bugünkü borçla doldurulmaz; kesin geçmiş borç gösterilemediği açıklanır.

## 5. Aşama D tarihsel rezervi

Geçmiş dönem için değerlendirme tarihi **seçili kapanış** ile sınırlandırılır. Sonraki dönemin ödeme veya iadesi geçmişteki taksidi kapatmaz. Banka ekstre/gözlem kapsamı da aynı tarih kesitinden okunur; sonraki banka bilgisi geçmiş planı yanlışlıkla örtmez. Sonraki döneme ait alışveriş tarihli plan geçmişe alınmaz.

İptal etkisi kayıtlı `iptalTarihi` gelmeden uygulanmaz. Eski iptalin tarihi yoksa geçmiş durum bilinmiyor olarak taşınır; kesin aktif/ödenmiş gibi gösterilmez. Eski planlar, ödemeler ve iadeler yeniden yazılmaz.

Kontrol örneği: kapanışta 100 TL taksit var, sonraki dönemde 100 TL iade yapılmış → geçmiş rezerv **100 TL**; iadenin gerçekten etkili olduğu dönemde kalan düşer. Kapanıştan önceki 30 TL iade ise geçmiş rezervi **70 TL** yapar. Sonraki tam banka ödemesi geçmiş kapanıştaki 100 TL borcu silmez; plan ve banka borcu ikinci kez toplanmaz.

Kayıtlı tarihlerden okunabilen tarihçe korunur. Daha önce fiziksel olarak silinmiş bir ödeme/iade olayı veya hiç kaydedilmemiş tarih bu sürümde tahmin edilerek yeniden üretilemez. Bu veri yokluğu için geçmişe otomatik işlem yazılmadı.

## 6. Testlerin önce/sonra sonuçları

| Test grubu | Önce | Sonra |
|---|---|---|
| Mevcut 207 kabul testi | 207 geçti | **207 geçti** |
| 23 tarihsel kontrol | 15 geçti, 8 başarısız | **23 geçti; 8 eski hata düzeldi** |
| Önceki yedek dışa aktarma regresyonları | 22 geçti | **22 geçti** |
| Bu düzeltmeye ek kabul kontrolleri | Yeni | **16 geçti** |
| Son birleşik JS/React ekran çalıştırması | — | **268/268 geçti, 0 atlanan** |

İşlev ve gerçek React/Chromium testleri ayrı dosyalardadır. Tarihsel 23 kontrol: 15 işlev + 8 ekran. Ek 16 kontrol: 14 işlev + 2 ekran. Mevcut 207; A, B, C, D ve eski yedek biçimi/ödeme bağlantı testlerini içerir. Eski doğrulayıcı uyumsuz örneği kabul edebiliyor diye geçen H13/H14 tanı kontrolleri korunmuştur; çözüm bu kayıtları geçmişte yanlış kesin tutara dönüştürmemektir.

Son kesin çıktı: `kanitlar/history-fix-final-268.tap`. Ara dosyalar önceki çalıştırmalardır. Önceki analizdeki başarısız testler `geri-donus/history-unit.tap` ve `history-browser.tap` içinde korunur. Test yükleyicisi dışında eski test kaynakları hash karşılaştırmasında aynıdır.

Ek kontroller; gerçek 20.000 / rezerv 0 / kalan 20.000 hesabını, karma kartlarda eksik bilgi sayısını, arşivin diskte aynı kalmasını, vade/kesim ayrımını, eski uyumlu gözlemi, çelişkili kimlik/tarihi, tarihli/tarihsiz iptali ve kapanıştan sonraki iade/ödemeyi kapsar.

## 7. Korunanlar ve devam eden yedek sorunu

Kaynak/AST karşılaştırmasında günlük finans hareket motoru, kredi ödeme/geri alma, kart ödeme eşleştirmesi, ekstre değişmezliği, günlük alışveriş farkı, devir, yeni taksit planı dağıtımı, iade/iptal yazma işleyicileri ve App kök bileşeni korunmuştur. Stil blokları birebir aynı; açık/koyu/gri tema, halka/ibre, grafik yerleşimleri ve animasyon süreleri değiştirilmedi.

Yedek paketleme/doğrulama/geri yükleme/yazma yardımcıları, `AyarlarTab`, `yedekDosyasiniAktar`, native SAF köprüsü ve MainActivity aynı kaldı. **Daha önce bildirilen gerçek Android'deki JSON geri yükleme hatası ayrı ve henüz çözülmemiştir.** Geçerli dosya ile yerel kayıt okuma hatasının aynı mesajda birleştiği yol bu çalışmada değiştirilmedi. Bu hata giderilmiş gibi sunulmaz; bu APK geçmiş borç düzeltmesidir.

## 8. Derleme ve yapılandırma

- `npm run build`: başarılı; Vite büyük paket uyarısı sürüyor.
- `npx cap sync android`: başarılı.
- Ana kimlikli debug APK: **BUILD SUCCESSFUL**, offline Gradle/JDK21/SDK36.
- Bağımsız `.devtest` debug APK: **BUILD SUCCESSFUL**, aynı web varlıklarıyla.
- Değişmeyen native yedek I/O katmanının 10/10 geçerli sonucu korundu; Gradle `testDebugUnitTest` görevi **UP-TO-DATE** önbellek sonucunu kullandı. Bu 10 test yeni bir cihaz testi gibi sayılmadı.
- ESLint: **49 hata, 12 uyarı**, `no-undef` yok; genel lint temizliği yapılmadı ve lint başarılı denmedi.
- Yeni paket veya veritabanı yok. Gerçek finansal kayıt/yedek dönüşümü yok.

## 9. APK kimliği, imza ve güncelleme

### Finans-gecmis-borc-ayni-kimlik.apk

- Kimlik: `com.butcetakip.app`. Sürüm `1.2.2`, versionCode **4**.
- Boyut: **6989093 bayt**.
- Dosya SHA-256: `94038a95c3c9aa540bee7dd113b7974567c10a5eef050b675c2e425c96ec4f76`.
- Gerçek derleme yolu: `/workspace/finans-upload/finans-takip/android/app/build/outputs/apk/debug/app-debug.apk`.
- Önceki `Finans-yedek-duzeltme-ayni-kimlik.apk` ile sertifika eşitliği: **doğrulandı**.

### Finans-Test-D-gecmis-borc-duzeltme.apk

- Kimlik: `com.butcetakip.app.devtest`. Sürüm `1.2.2-d-test`, versionCode **4**.
- Boyut: **6987912 bayt**.
- Dosya SHA-256: `1a441bc3a45ae038835fa12a4e51e17408718be6ceb1107d546ae4dcd3bb2214`.
- Gerçek derleme yolu: `/workspace/android-device-validation/android/app/build/outputs/apk/debug/app-debug.apk`.
- Önceki `Finans-Test-D-yedek-duzeltme.apk` ile sertifika eşitliği: **doğrulandı**.


Her iki dosya `apksigner verify` ile doğrulandı. Android Debug sertifikası, RSA2048; sertifika SHA-256:

`bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`

Önceki yedek düzeltmesi teslim APK'larıyla karşılıklı uygulama kimlikleri ve sertifikalar aynı; **versionCode 3 → 4**. Önceki Finans Test D dosyasını kurduysan yeni `.devtest` APK için imza/kimlik/sürüm güncelleme koşulları dosyalar arasında doğrulandı. Daha eski aynı test ailesinin versionCode2 sertifikası da aynıydı.

Telefonundaki pakete doğrudan erişilmedi. Kendi bilgisayarında/Play'de/farklı anahtarla hazırlanmış sürüm veya daha yüksek versionCode varsa güvenli güncelleme varsayılmaz. Kurulum reddedilirse **uygulamayı kaldırma, verileri temizleme veya zorla downgrade yapma**. Kurulu paketin kimliği/sürümü/sertifikası salt okunur kontrol edilmelidir.

**Yeni APK gerçek telefon üzerinde doğrulanmadı.** Debug paketler release/mağaza teslimi değildir. İmza doğrulaması cihazda finans davranışının doğrulandığı anlamına gelmez.

## 10. Cihaz kontrolü ve geri dönüş

1. Mevcut kayıt/bakiye/kategori sayılarının ekran görüntüsünü al; dışarı kaydedilmiş yedeğin varsa bilgisayara da kopyala. Devam eden geri yükleme sorunu nedeniyle sırf test için gerçek veriyi geri yükleme/temizleme yapma.
2. Önceki **Finans Test D** kurulumuna aynı kimlikli yeni test APK'sını normal güncelle. Kaldırma yapma.
3. Sentetik test uygulamasında geçmiş 0, bilinmeyen geçmiş, gerçek 5.000 devreden borç ve çelişkili ayrıntı örneklerini kontrol et. Geçmişe gidip güncele dönünce gerçek bakiye/kayıtlar aynı kalmalı.
4. Geçmiş kapanıştan sonraki ödeme/iade ve tarihli iptalin eski kapanışı değiştirmediğini dene; kişisel arşivleri düzenleme.
5. Sistem dosya kaydetmesinin çalışmaya devam ettiğini kontrol et. Geri yükleme hatası sürerse bunu geçmiş borç düzeltmesinden ayrı kaydet.
6. Cihaz modeli, Android/WebView sürümü, seçili maaş dönemi etiketi ve görülen/ beklenen tutarları not et. Gerçek kişisel JSON'u herkese açık repoya yükleme.

Geri dönüş kaynakları `geri-donus/` klasöründe; başlangıç App.jsx hash'i `3d3fa6386aefeee2577c4ccb303c65ff23da8409ee467f336aa2991bb87d3c31`. Eski çalışan kaynak, native yedek köprüsüyle birlikte korunmuştur. Telefona versionCode4 kurulduktan sonra eski versionCode3 APK normal düşürme olarak kullanılmamalı. Gerekirse eski App.jsx aynı imzayla, **4'ten yüksek yeni versionCode** ile yeniden derlenir. Bu işlem ayrıca yapılmadı. Kaynak geri dönüşü için telefon verisini silmek gerekmez.

Yeni geçmiş sıfır kayıtları veya toplu arşiv dönüşümü yazılmadığından bunları geri alan bir veri migrasyonu yoktur. Her durumda mevcut dosya yedeklerini koru; gerçek geri yükleme otomatik uygulanmaz.

Onaylanan tarihsel okuma/gösterim düzeltmesi tamamlandı. Yeni finans özelliğine geçilmedi.

Teslim App.jsx SHA-256: `ddd4161b6a892a38c40a876900169d7885aff169fe779b281b8465d123f0aee2`.
