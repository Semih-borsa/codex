# Doğrulama — 17 Ekim 2026

Linux üzerinde .NET 8.0.426 SDK, ClosedXML 0.104.2.

- Motor test çalıştırıcısı: **23 kontrol geçti**, 0 başarısız. Gerçek yüklenen boş şablon açıldı; geçici kopyalara sentetik kayıtlar eklendi.
- Türkçe I/İ/ı, boşluklar, aynı ad, mükerrer kod, kod/ad çelişkisi, unvan ayrımı, tekrar eden kaynak/hedef test edildi.
- İlk personel satırı, boş satırların atlanması, gerçek V3 unvan birleştirmesi test edildi.
- Excel çıktı geri okuması: irtibat en son sayfa, telefon metin/sıfır koruması, irtibat yok durumu, toplam, TC metni, imza birleşimi, 301 kesinti kaydında toplam ve imzaların kaydırılması test edildi.
- Kaynak SHA-256 değişmedi; mevcut çıktı üzerine yazma reddedildi.
- WPF Release derlemesi: **0 uyarı, 0 hata**.
- `dotnet publish -c Release -r win-x64 --self-contained true`: **başarılı**. Windows EXE ve bağımlılıkları üretildi.
- Kaydedilen ortam kurulum komutları yeniden çalıştırıldı: restore/build başarılı.

## Çalıştırılmayanlar / eksikler

- Windows WPF çalıştırma, tema ve dosya diyaloglarının etkileşimli testi: **çalıştırılmadı**, Linux ortamı.
- Inno Setup derlemesi/kurulumu: **çalıştırılmadı**, Windows/Inno Setup yok. Setup.exe teslim edilmedi; .iss ve PowerShell üretim betiği var.
- Gerçek personel verileriyle test: **çalıştırılamadı**, yüklenen dosyada veri yok.
- `KesintiRaporuFinal_KarakterDuyarsiz` davranış uyumu: **doğrulanamadı**, makro kaynakta yok.
- Yeni cloud görevi/snapshot geri yükleme ve yayın: **doğrulanmadı**. Yapılandırma taslağındaki install_script ve start_skill kaydedildi; yayın kullanıcı tarafından yapılır.

Windows ZIP bir çapraz derleme çıktısıdır, Windows'ta çalışır olduğu doğrulanmış kurulum paketi değildir. Üretim bordrosuna geçmeden eksik makronun incelenmesi ve Windows kabul testleri gerekir.
