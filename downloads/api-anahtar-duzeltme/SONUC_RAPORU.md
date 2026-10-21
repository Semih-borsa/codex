# API anahtarı ve model seçimi düzeltmesi — 1.5.1

## Bulgular

Anahtar girme alanı ayar ekranının geçici durumuydu; sayfa değişiminde boşalması tek başına kayıt silindiğini göstermiyordu. Güvenlik nedeniyle kayıtlı anahtarın kendisi yeniden JavaScript'e verilmez. Önceki ekranda bunu açıklayan belirgin bir kayıt durumu yoktu. Test ve sil düğmeleri yalnız kayıtlı anahtara bakıyor, yeni yapıştırılan anahtarı dikkate almıyordu. Model seçenekleri ise bağlantı testi yapılana kadar yalnız seçili modeli içeriyordu; testten alınan liste sayfa değişince kayboluyordu.

Ayrıca başlangıçtaki gecikmiş anahtar durumu yanıtının yeni kaydedilmiş durumun üzerine yazmasını önleyen sürüm kontrolü eklendi. Telefonun hata günlüğü olmadığı için cihazda anahtar kaydının gerçekten neden başarısız olduğunu kesinleştirmek mümkün değildir. Eski APK'da native eklenti yoksa tek App.jsx bunu kuramaz; bu durum artık ekranda açıkça belirtilir.

## Düzeltmeler

- “Anahtar kayıtlı. Güvenlik için anahtarın kendisi gösterilmez.” durumu görünür. Boş alan yeni anahtar girmek içindir.
- Android'de kaydetme sonrası native durum yeniden okunup kayıt doğrulanır; doğrulanmadan başarı gösterilmez.
- Yapıştırılan anahtar varsa test ve sil etkinleşir. Test önce yeni anahtarı güvenli biçimde kaydeder, sonra bağlantıyı sınar. Sil, girilen taslağı da temizler.
- Model listesine 3.5 Flash Lite, 3 Flash Preview, 2.5 Flash, 2.5 Flash Lite ve 2.5 Pro seçenekleri eklendi. Hesabın erişebildiği diğer modeller bağlantı testinden alınır ve liste hatırlanır. Seçim kalıcıdır. Model listesinde bulunmak erişim, ücretsiz kullanım veya süreklilik garantisi değildir.
- Anahtar açık metinle localStorage'a veya JSON finans yedeğine yazılmaz. Android'de mevcut Keystore saklaması korundu. Tarayıcı anahtarı yalnız oturum belleğindedir; tarayıcıyı yeniden açınca tekrar girilir.
- PDF ekstreleri ve türetilmiş özetleri Gemini'ye gönderilmez; yerel analiz ayrımı korunmuştur. Finans hesapları, kayıtlar ve temalar değiştirilmedi.

## Kullanım

Ayarlar → API anahtarını yapıştır → Anahtarı kaydet. “Anahtar kayıtlı” yazısını kontrol et. Alanın boşalması normaldir. Model seçip Bağlantıyı test et; ardından istersen yorum kaynağını Gemini seç ve paylaşım onayını ver. Test doğrudan yapıştırılmış anahtarla da çalışır.

## Değişen dosyalar ve kurulum

- `App.jsx`: anahtar durumu, düğmeler ve model listesi.
- `android/app/build.gradle`: versionName 1.5.1, versionCode 14. Teslimdeki `build.gradle` bu dizine kaydedilir.
- `tests/finance-baseline/api-key-settings.browser.test.mjs`: dört yeni regresyon testi.

Bu düzeltmede yeni native eklenti/dependency eklenmedi. Önceki 1.5.0 sürümünün native Gemini eklentisi gerekir; hazır APK içerir. Yalnız App.jsx kopyalayıp eklentisiz eski APK derlemek güvenli anahtar saklamasını sağlamaz. 1.5.0 kaynak kurulum talimatları önceki teslim klasöründedir.

APK kimliği `com.butcetakip.app`, sürüm 1.5.1 (14). İmza önceki teslim APK'sıyla aynı Android Debug sertifikasıdır: SHA-256 `bfb3b3c3dc6fdb0d279a6cd4bf6002d9731e0aa87e1ea714c24df079542b04c1`. Telefonda kurulu sürümün imzası bilinmediği için üzerine güncelleme kesin garanti değildir. Önce JSON yedeği al; çakışma halinde mevcut uygulamayı kaldırma veya verilerini temizleme. Dosya boyutu ve SHA-256 `SHA256.json` içindedir.

## Doğrulama

Yeni dört kontrol: yapıştırınca test/sil etkinliği ve önce kaydetme; sayfa geçişi/yeniden açılışta kayıtlı durum; ek model seçenekleri ve seçim kalıcılığı; yalnız taslak anahtarın silinmesi. Native çağrılar tarayıcı testlerinde taklit edilmiştir; gerçek anahtar kullanılmamıştır.

Hedefli 18 tarayıcı testi başarılı. İlk genel koşuda eklenen durum etiketinin yedekleme testlerinin durum seçicisiyle çakıştığı dört kontrol tespit edildi. Etiket erişilebilir canlı metin olarak düzenlendi; test beklentileri değiştirilmeden tam koşu tekrarlandı. **Genel koşu 479/479 başarılı**, 0 başarısız, 0 atlanan (`kanitlar/all-tests.tap`); önceki kaldırılmış PDF arayüzünün 17 eski tarayıcı testi önceki teslimdeki gibi korunmuş fakat koşuya dahil edilmemiştir. Eski finans test beklentileri değiştirilmedi. `npm run build`, `npx cap sync android` ve Android debug APK derlemesi başarılı. Native kaynaklar değişmedi; mevcut native test görevi de başarılı.

Bu APK fiziksel telefonda ve gerçek Gemini hesabıyla doğrulanmadı. Telefonda kaydetme → başka sayfa → Ayarlar → uygulamayı kapat/aç → bağlantı testi adımlarını kontrol et. Anahtar görünmezken “Anahtar kayıtlı” ve test düğmesi görünür/etkin kalmalıdır.

## Geri dönüş

`geri-donus/` bu işlemden önceki App.jsx ve build.gradle kopyalarını içerir. Kaynak düzeyinde geri alınabilir. Android'de eski versionCode'u zorla kurmak/veri silmek yerine aynı imzalı daha yüksek versionCode taşıyan geri dönüş APK'sı hazırlanmalıdır.
