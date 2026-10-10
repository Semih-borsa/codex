# Cep Bütçem v1.1 — indirme, kurulum ve ilk ayarlar

## Bilgisayara indirmenin kolay yolu

1. Tarayıcıda [GitHub deposunu](https://github.com/Semih-borsa/codex/tree/main) aç. Giriş istenirse `Semih-borsa` hesabınla giriş yap. Özel depolar oturum açılmadan 404 gösterebilir.
2. Dosya listesinin üstündeki yeşil **Code** düğmesine bas.
3. Açılan menüde **Download ZIP** seç. Tarayıcı `codex-main.zip` dosyasını indirir; genellikle bilgisayarının **İndirilenler / Downloads** klasörüne kaydeder.
4. Tarayıcıda **Ctrl + J** ile indirmeleri açabilirsin. Tamamlanan ZIP dosyasının bulunduğu klasöre git.
5. Windows'ta ZIP'e sağ tıkla → **Tümünü ayıkla / Extract All** → **Ayıkla**. ZIP'in içinden projeyi çalıştırmak yerine ayıklanan klasörü kullan.
6. `codex-main` klasörüne gir. **downloads → CepButcem.apk** telefona kurulacak dosyadır. `settings.gradle`, `gradlew` ve `app` bulunan **codex-main** klasörü Android Studio projesidir.

Yalnızca gereken dosyayı da indirebilirsin:

- [Hazır APK](https://github.com/Semih-borsa/codex/blob/main/downloads/CepButcem.apk)
- [APK + Android Studio projesi, tam paket](https://github.com/Semih-borsa/codex/blob/main/downloads/CepButcem-Tam-Paket.zip)

Bu bağlantılar GitHub dosya sayfasını açar. Sayfadaki aşağı ok simgesine, **Download raw file** veya **View raw** bağlantısına bas. ZIP/APK için önizleme olmaması normaldir. Tarayıcının **Farklı kaydet** menüsü HTML sayfasını kaydedebilir; GitHub'ın dosya indirme düğmesini kullan.

Tam paketi indirdiysen ayıklandığında kökte **CepButcem.apk**, yanında **CepButcem** proje klasörü bulunur. `ONCE-OKU.txt` ilk adımları içerir.

## Telefona APK kur

1. Daha önce kullanmaya başladıysan uygulamada **Ayarlar → JSON yedeğini kaydet** seç ve yedeğini güvenli bir yere kaydet.
2. Telefonu USB ile bilgisayara bağla. Telefonda USB kullanımını **Dosya aktarımı** seç.
3. İndirdiğin `CepButcem.apk` dosyasını telefonun **Download / İndirilenler** klasörüne kopyala.
4. Telefonda **Dosyalar / Dosyalarım** uygulamasını aç → İndirilenler → `CepButcem.apk` dosyasına dokun.
5. Android izin isterse dosyayı açan uygulama için **Bu kaynaktan yüklemeye izin ver** seçeneğini aç, geri dön ve **Yükle / Güncelle** seç.
6. **Aç** düğmesine bas. Android 8.0 veya üzeri gerekir. Bilgisayarda APK dosyasını açman gerekmez; Android telefon kurar.

v1.1 hazır APK, önceki yayımlanan v1.0 APK ile aynı imzayı kullanır. Güncelleme kurulumu mevcut gelir, gider, hesap ve bütçeleri korur; veritabanı silinmeden yükseltilir. **Uygulamayı kaldırmadan güncelle**. Farklı bilgisayarda farklı imzayla oluşturulmuş bir APK üzerine kurulumda imza uyuşmazlığı olabilir; kayıtlarını önce JSON olarak yedekle.

## Android Studio'da aç ve kendi APK'nı üret

1. [Android Studio](https://developer.android.com/studio) kur ve aç.
2. **Open** seç. Code → Download ZIP kullandıysan **codex-main**; tam paket kullandıysan **CepButcem** klasörünü seç. Seçtiğin klasörde `settings.gradle`, `gradlew` ve `app` birlikte bulunmalı.
3. **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** alanında tam **JDK 17** seç.
4. **SDK Manager** ile **Android SDK Platform 35** ve **Android SDK Build-Tools 34.0.0** kur. Lisansları kabul et.
5. **Sync Project with Gradle Files** işlemini tamamla. İlk derleme bağımlılık indirmek için internet gerektirir; uygulama telefonda çevrimdışı çalışır.
6. **Build → Build Bundle(s) / APK(s) → Build APK(s)** seç. Yeni Studio sürümlerinde menü adı **Generate App Bundles or APKs → Generate APKs** olabilir.
7. APK, proje içinde `app/build/outputs/apk/debug/app-debug.apk` yolunda oluşur. Bu dosyayı önceki bölümde anlatıldığı gibi telefona kopyalayıp kur.

Kendi derlemelerini güncellerken aynı imza anahtarını koru. Keystore dosyası ve şifrelerini GitHub'a yükleme.

## Uygulamayı maaş dönemine göre ayarla

1. **Ayarlar → Maaş günü ve hedefi değiştir** seç. Örneğin 15 yazarsan dönem **15 Ekim–14 Kasım** olur. Önceki/sonraki düğmeleri bu maaş dönemleri arasında geçer.
2. Tasarruf hedefini yüzde olarak seç; örneğin %20. Gün 1–31, hedef %0–100 aralığındadır. Kısa aylarda seçilen gün yoksa ayın son günü kullanılır.
3. **Açık temaya geç / Koyu temaya geç** ile görünümü ayarla. Tercihler yeniden açılışta korunur.
4. **Hesaplar** bölümünde başlangıç bakiyelerini düzenle. Açılış bakiyesi gelir sayılmaz. Maaş yatışını ayrıca **Gelir ekle** ile kaydet.
5. Giderlerini kaydet ve **Bütçe** ekranında maaş dönemi kategori limitlerini seç.

## Kredi kartı ve fatura takibi

**Kart:** Ayarlar → Hesap ekle ile kart için ayrı hesap oluştur. Mevcut borcun 4.000 TL ise açılış bakiyesine `-4000` yaz. Kredi kartı tanımla bölümünde bu hesabı, kesim ve son ödeme günlerini seç. Alışverişi kart hesabına **gider** kaydet. Bankadan kart borcunu ödediğinde **Kart borcu öde · transfer** kullan; ödeme gideri ikinci kez artırmaz. Gösterilen ekstre borcu tahminini bankanın gerçek ekstresiyle karşılaştır. Faiz, iade, bankanın kesim saati ve tatil değişiklikleri otomatik alınmaz.

**Fatura:** Ayarlar → Fatura ekle ile ad, tutar, ödeme hesabı, kategori ve son ödeme tarihini gir. Vadesi gelen/geciken faturalar görünür. **Ödemeyi kaydet** gider oluşturur. Aylık tekrar seçilirse sonraki fatura aynı tahmini tutarla açılır; yeni tutarı geldiğinde düzenle. Aynı ödeme için ayrıca düzenli gider tanımlama. Hatalı ödemeyi **Ödemeyi geri al** ile düzelt. Ödeme takvimi uygulama içinde gösterilir; arka plan bildirimi gönderilmez.

## Asistan yorumlarını kullan

Özet ve Analiz ekranında gelirinin harcanan yüzdesini, önceki döneme göre değişimleri, artışa en çok katkı yapan kategoriyi, tasarruf hedefini ve dönem sonu senaryosunu oku. Sonuçlar kayıtlarından çevrimdışı hesaplanır. Gelir veya önceki dönem verisi eksikse bunun belirtilmesi normaldir; veri olmadan yüzde/değişim sonucu üretilmez. Bekleyen faturalar senaryoya katılır; gelecekteki maaş varsayılmaz. Günlük hedef payı, banka hesabındaki kullanılabilir bakiye değildir.
