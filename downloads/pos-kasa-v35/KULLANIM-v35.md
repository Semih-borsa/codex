# Build v35 — Takip toplam kartı geri eklendi

Takip sekmesinde tarih seçiminin hemen altında, banka/kasa kartlarının üzerinde **BUGÜN TOPLAM YATACAK TUTAR** kartı yeniden bulunur. Toplam, kredi ve debit tutarları ayrı gösterilir. Yüzdeler seçili tarihin tam 7 gün öncesine göre hesaplanır: artış yeşil, azalış kırmızı; eksik veya sıfır karşılaştırma tabanı gri “–”.

Özet ekranı korunmuştur. Kasa tutarları adlarının altında kalır. Ödeme takibi ve POS analizi yalnız Özet'tedir. Finans kayıtları, kredi tahsilatı işleyişi ve tarih biçimi değiştirilmedi.

Tam kaynak paketidir; hazır EXE içermez. Windows'ta `pos-app/BUILD.bat` ile derleyin. Sürüm 1.3.1, kimlik `com.poskasayonetim.app` aynı. Önce JSON yedek alın; kullanıcı verilerini temizlemeyin.
