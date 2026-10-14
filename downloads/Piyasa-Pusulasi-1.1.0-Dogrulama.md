# Piyasa Pusulası 1.1.0 — doğrulama

- 15 Node hesaplama/veri doğrulama testi: eski/future veri reddi, Wilder RSI, SMA/EMA, gram kapanışlarının aynı tarihli FX ile dönüşümü ve SMA50 sayısal sonucu, eksik son kurun reddi, ortak tarihli yüzdesel karşılaştırma, sabit seride sıfır risk ve bilinen %25 düşüş, Türkçe hedef fiyat ayrıştırma.
- 9 mobil tarayıcı akışı: 390px ana ekran, analiz/grafik/alarm kaydı, açık tema ve kayıtların yeniden açılmada korunması, iki karşılaştırma alanının silinebilmesi/değiştirilebilmesi ve boş alan doğrulaması, 6 sembol tarama, gram TL gösterge birimleri ve risk paneli, eski verinin reddi, 503 durumunda önceki sonucun kaldırılması, gezinme sonrası bekleyen isteğin eski ekranı açmaması. Kontrol edilmiş sağlayıcı yanıtları kullanıldı; gerçek fiyat iddiası yoktur.
- Üretim web derlemesi, Android release APK ve Android lint çalıştırıldı. APK imza ve paket/sürüm bilgileri kontrol edildi.
- Bu sürüm fiziksel cihazda veya emülatörde çalıştırılmadı. Önceki 1.0.0 yerel Android/WebView kabuğu emülatörde açılmıştı; bu, yeni sürüm cihaz doğrulaması sayılmaz.
- Bulut ağı Yahoo canlı bağlantısını engelliyor. Telefon üzerinde canlı Yahoo veri erişimi, gerçek piyasa rakamları, bildirim teslimi ve kullanıcı anahtarı gerektiren Gemini çağrısı bu sürüm için doğrulanamadı.

## Güncelleme
Paket tr.piyasapusulasi.mobile; sürüm1.1.0; versionCode2. Önceki dağıtımla aynı imza. Kayıt anahtarları ve kayıt biçimi korunur; temizleme veya veri silme kodu eklenmedi. Mevcut uygulamayı kaldırmadan APK'yı üzerine kurun.

## Veri birimleri ve sınırlar
Gram göstergeleri TRY/gram; ons göstergeleri USD/ons. Gram serisi vadeli ons ve günlük FX kapanışıyla türetilir; banka/kuyumcu spot alış-satış fiyatı değildir. Günlük high/low aynı gün kapanış kuru ile ölçeklenmiş yaklaşık aralıktır. Ortak tarihler UTC tarih anahtarıyla eşleşir. Tarihler veya kur eksikse gelecek tarihli kurla doldurulmaz.

Getiriler fiyat getirisidir; temettü, enflasyon veya ayrıca uygulanan bölünme düzeltmesi yoktur. Son mum/hacim tamamlanmamış olabilir. Ekonomik açıklamalar veri kapsamını belirtir; finansal tablo olmadan değerleme yapmaz.
