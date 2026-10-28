# Build v40 sonuç raporu

Temel v39/1.4.3 kaynağı /workspace/pos-v39 değiştirilmeden korundu. Yeni çalışma /workspace/pos-v40; sürüm1.5.0. Gerçek kullanıcı depolaması açılmadı/değiştirilmedi. Yeni bağımlılık yoktur.

## Değişen dosyalar

- app-source.jsx: manuel girişten ayrı Fotoğraftan ekle düğmesi/penceresi; yükleme/sürükleme, kasa/tarih, varsayılan dün, yerel fotoğraf hazırlama, yedi kalem, toplam/fark/vade gösterimi, düzenleme/onay/eşleşme kapısı; fiş bağlantılı kayıtları birlikte düzenleme/silme. Manuel giriş alanları ve manuel kaydetme işlevleri değiştirilmedi. Manuel formun v39 ile SHA-256 karşılaştırması aynıdır: ca96be9a85522dcf389530907364abddee6cfb3166d1805b472fc8bb633ad23a.
- receipt.js: ortak fiş ayrıştırıcısı, sabit vade eşlemesi, tam sayı kuruş kontrolü, mevcut txs biçimine atomik kayıt hazırlama, fiş bütünlük doğrulaması. Yeni bakiye motoru oluşturulmadı.
- native-ocr.js/native-ocr.ps1: Windows’un yerel WinRT OCR köprüsü; boyut/tür sınırı, güvenilir sabit komut, kullanıcıdan dosya yolu/komut almama, süre/çıktı sınırı ve geçici dosya temizliği. PowerShell kabuk enjeksiyonu yapılmaz; shell:false, sabit betik ve ayrı geçici dosya yolu kullanılır.
- main.js/preload.js: yalnız güvenilir ana pencerenin kullanabildiği dar OCR IPC kanalı; aynı anda bir okuma. Önceki sandbox/contextIsolation/webSecurity ve yedekleme kanalı korunur.
- safety.js: yalnız yeni fisId/fisDetay kayıtlarının tam bağlantı ve toplam doğrulaması; eski kayıtlar aynı kurallarla okunur. Yedek şeması değiştirilmedi, ek bilgi txs nesnelerinde korunur. Hatalı/yarım fiş geri yüklemesi mevcut veriyi değiştirmeden reddedilir.
- index.html: yerel receipt.js yükleme. CSP connect-src none ve script-src self korundu, dış bağlantı eklenmedi.
- ui-accessibility.js: pencerenin açıkça verilmiş erişilebilir adı korunur; isimsiz mevcut pencerelerin davranışı aynı.
- renderer.js, package.json/package-lock.json, BUILD.bat: derlenmiş kaynak,1.5.0/v40 ve paketlenecek yerel dosyalar/test komutu. Uygulama kimliği aynı.
- tests/receipt-v40.test.cjs, browser-v40.mjs, electron-v40.mjs ve raporlar.

## Testler ve sınırları

- Önceki108 otomatik test +36 yeni fiş/OCR köprü testi:144/144 geçti, atlama yok. Mevcut182 sayısal kontrol ve JSX derlemesi geçti.
- Yeni testler: yedi kalem, debit yurtdışının kredi vadesi, Türkçe/çok satırlı ve bilinen OCR etiket varyantları; tutarlarda tahmin yapmama; kuruş farkı; eksik/negatif/bozuk değer; onaysız kayıt engeli;0 gün/yıl geçişi; iade/belirsizlik; fişi bütün düzenleme; kısmi silme veya yedek tutarsızlığını reddetme; eski/yeni yedek okuma; kaynakları koruma. OCR işlemci testi PNG sınırı, platform/dil/hata yanıtı, zaman aşımı ve geçici dosya temizliğini sahte alt süreçle doğrular.
- Chromium browser-v40: dosya seçme/sürükleme, dün, ayrım/toplam, uyumsuzluk ve düzeltme sonrası yeniden onay, yazma hatası, fişi çift satır halinde kaydetme, vade tarihleri, düzenleme/silme/JSON/yeniden açılış, yanlış dosya/okuma hatası ve okuma sürerken iptal geçti. Manuel formun kaydedilmemiş değeri ve HTML’i değişmedi. Bu deterministik ekran testinde OCR yanıtı sentetiktir; gerçek Windows OCR doğruluğu testi değildir.
- Önceki browser-v38 çoklu nakit, browser-v39 ödeme ve browser-v37 dağılım/geçmiş kabul testleri yeni sürümde geçti.
- Gerçek Electron çalışma zamanı Linux: gerçek preload/IPC üzerinden desteklenmeyen platformun açık hatası, fiş penceresi/iptal, önceki nakit/ödeme/özet kontrolleri geçti. Windows WinRT/PowerShell çalışması bu ortamda test edilmedi.
- Kullanıcının iki örneğindeki elle doğrulanmış kalemlerle vade/toplam hesabı ayrıca kontrol edildi. Bu kontrol fotoğrafın Windows OCR tarafından doğru okunduğu anlamına gelmez. Yerel Linux OCR denemesinde yanlış/eksik okuma oluştu; toplam kontrolü kayıt yapılmasını engelledi. Bu Linux deneme aracı üretim uygulamasına eklenmedi. Özel fotoğraflar ve ham metinleri teslim ZIP’ine/repoya eklenmedi.
- Windows x64 electron-builder derlemesi başarılı. Paket içindeki OCR betiği dahil tüm değişen çalışma dosyaları kaynaklarla byte eşit. Kod imzalama sertifikası yok; derleme imzasız. Teslim kaynak ZIP’idir, hazır EXE içermez.

## Gerçek Windows’ta yapılacaklar

1. Tam yedek alıp test kopyasında açın; kurulu OCR dilini ve PowerShell’in kurum politikasıyla çalışabildiğini doğrulayın. Program sistem politikasını/dilini otomatik değiştirmez.
2. Her iki gerçek fişi seçip doğru kasa ve varsayılan dün tarihini kontrol edin; fiş tarihinin farklı olduğu uyarısını değerlendirin.
3. Yedi kalemi ve özellikle debit yurtdışı tutarının kredi vadesine ayrılmasını kontrol edin. OCR’nin tanıma doğruluğu için gerçek cihaz sonucu gereklidir.
4. Bir kalemi1 kuruş değiştirin; kayıt kapalı kalmalı. Düzeltip toplam eşleştiğinde ve onay verildiğinde kaydolmalı.
5. İşlem Geçmişi’nde iki vadeli kaydı, bankaya gerçekten yatmış olarak işaretlenmediğini, düzenleme/silme bütünlüğünü ve JSON yedeğini doğrulayın.
6. Aynı fişi manuel girişten tekrar eklemeyin. Fotoğraf yolu mevcut Toplam POS/Yatan Debit mutabakatını otomatik oluşturmaz; ayrımı bilinen kredi/debit satışlarını vadeli kaydeder.

Geri dönüş kaynağı v39 korunur. Yeni fişleri eski sürümle ayrı ayrı düzenlemek bağlantıyı bozabileceğinden yeni tam yedek saklanmadan ve veri kapsamı değerlendirilmeden eski sürümle işlem yapılmamalıdır. Gerçek kayıtlar veya eski yedekler otomatik dönüştürülmedi.
