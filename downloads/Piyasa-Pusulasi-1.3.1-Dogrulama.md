# Piyasa Pusulası1.3.1 — doğrulama

- 34 otomatik veri/hesaplama testi geçti. Fiyat, mum, hacim, geçmiş genişletme, gram dönüşümü, teknik hesaplar, arşiv ve kota kontrolleri değişmedi.
- 24 tarayıcı kontrol grubu geçti:15 genel akış,4 veri-kapsamı senaryosu,5 yenileme/AI regresyonu. Mobil390px, masaüstü1100px kontrolleri ve JavaScript hata yakalama dahil.
- Yeni5 regresyon: AI beklerken65s timer ve online olayı açıklamayı kesmez/başa atmaz; başarılı arka plan güncellemesi AI metni, grafik dönemi, açık ayrıntı ve kaydırmayı korur; başarısız yenileme veriyi değiştirmeden uyarı verir ve okuma konumunu korur; alarm formu otomatik analizi duraklatır; AI başlamadan önce gönderilmiş gecikmiş arka plan yanıtı açıklamayı kesemez.
- Testlerde gerçek zaman beklemek yerine Playwright sanal saat kullanıldı. AI isteği32s uygulama zaman aşımını aşmayan20s süreyle bekletildi; öncesindeki55s ile65s yenileme eşiği aşıldı. Kontrol edilmiş fiyat/AI yanıtları ve sahte native köprü kullanıldı; gerçek fiyat, Gemini erişimi veya AndroidKeystore testi değildir.
- Web üretim derlemesi ve Android assembleRelease/lintRelease başarılı. İmza doğrulandı, önceki sürümlerle aynı sertifika. Paket tr.piyasapusulasi.mobile; versionName1.3.1/versionCode5. Lint0 hata, önceki2 uyarı: yerel React için JavaScript ve arka plan alarmının commit yazımı.

## Değişen davranış
Otomatik yenileme ekranı kaldırıp yeniden oluşturmaz. Ayrı istek kimliğiyle çalışır; AI beklenirken veya alarm formu açıkken durur. Eski arka plan yanıtları bu işlemleri kesemez. Fiyat güncellense de önceki AI metni kendi fiyat zamanıyla tutulur. Kullanıcının açıkça yaptığı yenileme/yeni sembol seçimi analiz ve AI isteğini değiştirebilir; zamanlayıcı bunu yapamaz.

Başarısız arka plan yenilemesinde ekrandaki veri ve orijinal zamanı korunur; “yenilenmedi” uyarısı gösterilir. Bu, arşivden eski verinin otomatik yüklenmesi değildir. Manuel analiz hatasında arşiv hâlâ yalnız açık kullanıcı isteğiyle, eski veri etiketiyle açılır. Hesaplama ve kalıcı kayıt biçimi değişmedi.

## Araştırma ve canlı test sınırları
15 Ekim2026 tarihli kaynak raporu VERI-KAYNAKLARI.md içindedir. Investing, Fintables, KAP, Borsa İstanbul veri yayını, EODHD, Twelve Data, FMP, CollectAPI ve SEC sayfalarına9 erişim denemesinin tamamı ağ geçidinde CONNECT403 ile reddedildi. Bu oturumda kapsam/fiyat/ücret/API sözleşmesi doğrulanamadı; yeni finansal sağlayıcı bağlanmış değildir. CollectAPI mevcut sürümde yalnız isteğe bağlı hisse-listesi bağlantı denetimi olarak kalır; analiz kaynağı Yahoo'dur. Bilançodan eksik puanlar uydurulmaz.

Bu APK fiziksel cihaz veya emülatörde çalıştırılmadı. Canlı fiyatlar, Gemini ve bildirim teslimi doğrulanamadı. Anahtarlar ve imza dosyaları kaynak paketine dahil değildir. Mevcut uygulamayı kaldırmadan üzerine kurun; favori, tema, API anahtarı, alarm ve arşiv biçimleri korunur.
