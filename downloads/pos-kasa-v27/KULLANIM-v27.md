# Build v27 — ayrı toplam/debit kaydı

1. Toplam POS takibi açık kasayı seçin. Gün, toplam tutar ve vade girin.
2. Tutarı `3053,50` yazabilirsiniz. Alan dışına çıkınca `3.053,50` görünür.
3. **Toplamı kaydet** düğmesine basın.
4. Bankaya yatan debitlerin tarih ve tutarlarını ekleyin; **Debitleri kaydet** düğmesine basın.
5. Sonraki debitler için üstteki kayıt listesinden ilgili kasa/günü açın. Debit güncellemelerini ayrıca kaydedin.
6. Toplamı değiştirirken Toplamı kaydet, debitleri değiştirirken Debitleri kaydet kullanın. Bir bölümün düğmesi diğer bölümün kaydedilmemiş değişikliklerini yazmaz.

Windows kaynak paketidir: `pos-app/BUILD.bat` dosyasını çalıştırın. Program kimliği ve eski verilerin konumu korunmuştur. Güncellemeden önce JSON yedek alın. Gerçek Windows cihazında ayrıca test edilmelidir.
