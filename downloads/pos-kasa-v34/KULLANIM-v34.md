# Build v34 — Özet ekranı

Uygulama artık **Özet** sekmesiyle açılır.

- İlk kartın başlığı **BUGÜN TOPLAM YATACAK TUTAR**. Kredi ve debit ayrı kartlardadır. Kartlara tıklayınca Takip bugünün tarihiyle açılır.
- Önümüzdeki 7 günün beklenen POS yatışları ayrı grafiktedir. Bir çubuğa tıklayınca o günün Takip ayrıntıları açılır.
- Kasa performansında dönem seçilebilir. Tutarlar burada da kasa adının altındadır.
- Günlük ortalama grafiği POS Gelir Analizi ile birlikte Özet'e taşındı. Çubukları kalınlaştırıldı; mevcut filtreler ve ortalama hesabı korundu.
- Aylık Ödeme Takibi yalnız Özet'tedir. Hatırlatmadaki Ödemelere Git düğmesi bu bölümü açar.

**Takip** sekmesinde tarih seçimi, banka/kasa ayrıntıları ve Tarih Aralığı Toplamı bulunur. Kasa adlarının altındaki tutarlar eski yerleşimiyle korunmuştur. Ödeme listesi, günlük ortalama modülü ve büyük günlük özet kartları burada tekrarlanmaz.

Kredi tahsilatı işleyişi değiştirilmedi. Özet kayıtları okur; krediye otomatik tahsil edildi durumu yazmaz. Tarihler GG.AA.YYYY biçiminde kalır.

Tam Windows kaynak paketidir; hazır EXE değildir. `pos-app/BUILD.bat` ile derleyin. Sürüm 1.3.0, kimlik `com.poskasayonetim.app` aynı. Güncellemeden önce JSON yedek alın ve çalışan program klasörünü saklayın; kullanıcı verilerini temizlemeyin.
