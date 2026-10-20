# Renkli halka ve sıfır ekstre bildirimi — 1.4.4

## Yapılan değişiklikler

**Kartın tasarımı değiştirilmedi.** Yalnız halka grafiğin ilerleme çizgisi mevcut harcama oranına göre renklendirildi: %50 altı parlak yeşil `#34d399`, %50–80 altı altın sarısı `#fbbf24`, %80 ve üzeri canlı kırmızı `#ff626a`. İnce yarı saydam koyu alt çizgi halkayı kart renginden ayırır. İç alan şeffaf, yazı beyaz, kalan yay mevcut soluk beyaz renktedir. Kartın yerleşimi, zemin renkleri, ölçüleri, ibre seçeneği ve 3500 ms animasyon değişmedi. %100 üzerindeki oranların metinde gerçek değerle, çizimde en fazla %100 gösterilmesi korunur.

Kredi kartı hatırlatmaları artık eski kart gövdesindeki genel tutar/ödendi varsayımı yerine mevcut ortak `kartEkstreBakiyeleri` işlevinden kalan ekstre borcunu okur. **0 TL veya tam ödenmiş ekstre için kart bildirimi oluşturulmaz.** Kısmi ödemede kalan tutar, ödeme geri alındığında yeniden açık borç dikkate alınır. Dönem içi/provizyon tutarı tek başına ekstre hatırlatması oluşturmaz. Başka kartın/ekstrenin ödemesi bu borcu kapatmaz.

Her ekstre kendi kayıtlı vadesini kullanır. Eski açık borç yeni ekstre 0 TL diye kaybolmaz; kartın yeni vadesi eski ekstreye kopyalanmaz. Bilinen kesim tarihi gelecekte olan ekstre, henüz kesilmiş gibi planlanmaz. Eski yalnız gün bazlı kart kayıtları için mevcut tarih desteği korunur. Kimlikli ekstreye bilinmeyen vade tahmin edilmez. Kayıtlar veya yedekler otomatik dönüştürülmez.

Ayarlardaki hatırlatma günleri ve saatleri aynen kullanılır. Son ödeme günü için “Son ödeme günü” seçeneği açık olmalı. Saat geçmişse mevcut aynı-gün telafi davranışı sürer. Uygulama açıldığında/borç değiştiğinde mevcut bildirim planlama akışı artık gerekmeyen kart alarmlarını iptal eder; diğer ödeme alarmları korunur.

## Test sonuçları

**Sonuç: 406/406 test başarılı; başarısız, atlanan veya iptal edilen test yok.** Son tam test turu `kanitlar/tests.tap` dosyasındadır. Önceki uygulama kapsamının testleriyle yeni bildirim ve üç tema halka kontrolleri birlikte çalıştırılmıştır. Kaldırılmış PDF Ekstre Analizi özelliğinin 48 testi kapsam dışıdır; dosyaları silinmemiştir ve önceki teslimlerde korunur.

- Bildirim için 14 sentetik işlev testi: eski kaynakta 1 başarılı / 13 başarısız; düzeltmeden sonra 14/14 başarılı. Eski sonuçlar da saklanır.
- 9 yeni gerçek React/tarayıcı testi: açık/koyu/gri temada %35, %70, %95 halka rengi, şeffaf merkez, kontur ve kartın mevcut durum rengi kontrol edilir.
- 3 Android bildirim köprüsü taklidiyle çalışan React testi: sıfır ve tam ödenmiş ekstrede eski alarmın iptali; kısmi ödemede yalnız seçilen 15:00 ve 18:00 saatlerinde planlama; gerçek kayıtların değişmemesi.
- Web üretim derlemesi, `npx cap sync android`, Android `testDebugUnitTest` ve `assembleDebug`: başarılı. Native kod değişmedi; Gradle mevcut native test sonuçlarının önbelleğini kullanabilir.
- Mevcut finans motoru ve diğer kaynak korunmuştur: üretim App.jsx farkı yalnız `HarcamaGauge` ve `kartHatirlatmalariniOlustur` işlevlerindedir. `kanitlar/scope.json` bu kapsamı doğrular. Android yapılandırmasında yalnız sürüm yükseltildi; paket eklenmedi.

Gerçek Android telefonunda henüz test edilmedi. Bildirim için Android bildirim ve kesin alarm izinleri gerekir; pil kısıtlamaları veya zorla durdurma teslimi engelleyebilir/geciktirebilir. Köprü testleri gerçek işletim sisteminin alarm teslimini doğrulamaz.

## Dosyalar ve kurulum

- `App.jsx`: Güncel **zipsiz tek dosya**, `src/App.jsx` yerine kaydedilir.
- `Finans-1.4.4.apk`: Ana Android uygulaması debug güncellemesi.
- `android/app/build.gradle`: versionName **1.4.4**, versionCode **12**.
- ApplicationId: **com.butcetakip.app**.
- Sertifika SHA-256: `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`.
- APK boyutu: 7003472 bayt.
- APK SHA-256: `076831040f983088ee8c8cae17c54903a950477650c1a9b08a2b28827d9d9ae2`.

Önceki teslimimle aynı paket ve imza, daha yüksek versionCode kullanılır. Telefonda kurulu imza bilinmediğinden her kurulum için uyumluluk varsayılmaz. Kurulumdan önce uygulamadan JSON yedeği al. İmza hatası varsa mevcut uygulamayı kaldırma veya verilerini temizleme.

Cihazda güncellemeden sonra uygulamayı bir kez aç; sıfır ekstreli kart için eski planın kalktığını Ayarlar'daki planlama durumundan kontrol et. Pozitif kalan ekstre için bildirim günü/saatleri, tam/kısmi ödeme ve geri alma; üç temada halka; ibre seçeneğinin aynı kaldığı kontrol edilmelidir.

## Geri dönüş

İşlem öncesi kaynak ve sürüm dosyaları `geri-donus/` klasöründedir. Kaynak geri alınabilir; telefona düşük versionCode zorla kurulmaz. Gerekirse özgün imzayla daha yüksek sürüm kodlu geri dönüş derlemesi yapılır. Gerçek finans kayıtlarına ve yedeklerine müdahale edilmedi.
