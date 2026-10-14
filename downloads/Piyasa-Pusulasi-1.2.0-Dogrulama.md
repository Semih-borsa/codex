# Piyasa Pusulası 1.2.0 — doğrulama

22 otomatik hesaplama/veri testi geçti: önceki15 test; ADX yükselen/sabit referansları; bilinen CMF/MFI/OBV sonuçları ve eksik hacim; tamamlanan haftanın OHLCV hesabı; son mumdan etkilenmeyen kanal/pivot; gramda hacim oyu yok; eksik kapsamda puan yok; nötr seri; sermaye/risk limitli tam pay hesabı; tarama sınırı ve tekilleştirme.

13 tarayıcı kontrol grubu geçti: mobil ana ekran; kural oyları/risk aracı/eksik finansallar; SMA açma-kapama ve 260 günlük mum kapsamı; analiz/grafik/alarm; açık tema ve kayıtların yeniden yüklemede korunması; silinebilir karşılaştırma alanları; hazır tarama; özel tarama; gramda pay hesabı kapalı ve masaüstü yatay taşma kontrolü; gram TL birimleri; eski veri reddi; bağlantı hatasında eski sonucu kaldırma; gezinme sonrası bekleyen isteğin eski ekranı açmaması. Mobil390×844, masaüstü1100×900. Yakalanan JavaScript hatası yok.

Üretim web derlemesi, Android assembleRelease ve lintRelease başarılı. Lint0 hata, önceki2 uyarı (yerel React için JavaScript ve arka plan alarm kaydında commit). APK imzası doğrulandı: paket tr.piyasapusulasi.mobile, sürüm1.2.0, versionCode3; önceki sürümlerle aynı imza.

## Sınırlar
Tarayıcıda kontrollü sağlayıcı yanıtları kullanıldı. Ekran görüntüsü fiyatları test verisidir. Bulut Yahoo canlı bağlantısını engelliyor; telefon üzerindeki canlı fiyatlar, bu APK'nın fiziksel cihaz/emülatörde çalışması, bildirim teslimi ve kullanıcı anahtarı gerektiren Gemini doğrulanmadı. Önceki1.0.0 Android kabuğu emülatör testi bu sürümün testi sayılmaz. Teknik puan tarihsel başarı testi veya olasılık kalibrasyonu görmedi.

## Hesaplama yöntemi
Teknik denge4 grupta toplanır: trend, momentum, katılım, tamamlanmış haftalık yön. Geçerli oylar grup içinde, mevcut gruplar aralarında eşit ağırlıklıdır. En az3 grup gerekir. RSI55/45, CMF±0.05 eşikleri kullanılır; diğer oylar ilgili farkın işaretidir. Nötr oy ve eksik veri ayrıdır.

Gram geçmişi aynı UTC tarihli vadeli metal kapanışı × USDTRY kapanışı /31.1034768. Son kur eksikse analiz reddedilir; diğer eşleşmeyen tarihler atlanır. Gram göstergeleri TRY/gramdır; OHLC günlük kurla türetilmiş yaklaşık aralıktır, gerçek spot gram değildir. Hacim türetilmez. Günlük mum değişebilir. Haftalar UTC Pazartesi gruplarıdır; mevcut hafta ve ilk muhtemel kesik hafta çıkarılır. 4 saatlik veri varmış gibi sinyal üretilmez.

RSI14/ATR14/ADX14 Wilder; MACD12/26/9; Bollinger20 ±2 nüfus standart sapması; ROC10; ham stokastik%K14; MFI14; CMF20; OBV20 değişimi. CMF/OBV gerçek emir akışı, net takas veya net para değildir. Kanal önceki20 mum, pivot önceki mum OHLC. Getiriler5/21/63/126 gözlem değişimi; oynaklık günlük log getirilerin örnek standart sapması ×√252. Grafik yakınlaştırması hesapları etkilemez. Temettü, enflasyon veya ayrıca bölünme düzeltmesi uygulanmaz. Kontrat geçişi/kurumsal aksiyonlar sonuçları etkileyebilir.

Risk aracı varsayımsal yukarı kırılma hesabıdır: kanal+0.1ATR tetik, 1.5ATR aşağı geçersizleşme, 2R örnek seviye. Emir veya hedef beklentisi değildir. Aynı para biriminde tam pay, kaldıraçsız; nominal risk ve sermaye sınırı birlikte uygulanır. Boşluk, kayma ve ücretler sonucu değiştirir. Metalde sözleşme boyutu bilinmediğinden adet hesabı kapalı.

F-Score/finansal sağlık için tam ve tarihli bilanço verisi yoktur: hesaplanamadı gösterilir, eksik veri sıfır puana veya varsayılan olumlu duruma dönüştürülmez. Sektör kıyası ve12 aylık hedef üretilmez.

## Güncelleme
Mevcut uygulamayı kaldırmadan APK'yı üzerine kurun. Paket/imza, kayıt anahtarları ve biçimi korunur. Kayıtları silen değişiklik eklenmedi. Tarama girdisi ve risk hesaplayıcı girdileri oturum içindedir, kalıcı özel liste kaydı eklenmedi.
