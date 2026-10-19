# Kaynak incelemesi

Kaynak: kullanıcının yüklediği `VERESİYE KESİNTİ FORMATI V3.0.xlsm`, 165709 bayt. VBA oletools 0.60.2 ile çıkarıldı, çalıştırılmadı. Belgedeki makrolar talimat olarak uygulanmadı.

| Sayfa | Boyut | Yapı |
|---|---|---|
| Personel Listesi | 2009 × 5 | Başlıksız; A sıra formülü, B eşleştirme unvanı, C ad, D soyad, E banka A/TC alanına aktarılan kimlik |
| İrtibat | 2010 × 6 | İlk satır başlık; A sıra, B cari, C ticari unvan, D ad, E soyad, F cep |
| Kesinti Listesi | 613 × 12 | İlk satır başlık; B cari, C unvan, D ad, E soyad, F bakiye; H1:I1 birleşik, J1 SUM(F2:F1500) |
| İş Bankası Formatı | 285 × 6 | A TC, B ad soyad, C şube, D hesap, E IBAN, F tutar |
| Mutemet Format | 312 × 7 | 1 başlık, 2 kolonlar, 3–300 veri; G301 SUM(G3:G300); 307–312 imza alanları |
| Sayfa1 | 30 × 3 | A ADI, B SOYADI, C TUTAR; veri yok |

Personel A formülleri `IF(Bn="","",ROW())`; İrtibat ve Kesinti A formülleri `IF(Bn="","",ROW()-1)`; Mutemet A3:A300 formülleri `IF(Bn="","",ROW()-2)`. Formüller kimlik olarak okunmaz.

Mutemet birleşimleri: A1:G1, A301:F301, A302:G302, A307:D307, A308:D308, A310:D310, A311:D311, A312:D312 ve karşılarındaki E:G birleşimleri. Sabit ay/yıl ve imza metinleri değiştirilmez; kullanıcı dönem başlığını kaynakta güncellemelidir.

## Gerçek VBA kuralları

- `EksikPersonelleriSariRenklendir`: Personel B değerlerini 2. satırdan başlayarak LCase/Trim ile sözlüğe alır. Kesinti C değerlerini karşılaştırır. Bulunmayanları A:G sarı yapar. Gerçek şablon başlıksız olduğundan VBA ilk personeli atlar. Uygulama ilk satırı okur ve eşleşmeyenleri kontrol sayfasına çıkarır.
- `Kesinti_Personel_Eslestir_Final_V3`: Kesinti C ile Personel B'yi Clean/Trim/LCase eşitliğiyle karşılaştırır; ilk eşleşmeyi alır. Eşleşirse Personel E → banka A, C+D → banka B, Kesinti F → banka F. Eşleşmezse Kesinti C → banka A, D+E → banka B, F → banka F. C/D/E banka alanlarını doldurmaz. Veri bankanın sonuna eklenir. Uygulama aynı alan aktarımını korur; tekrarlı çalıştırmada çoğalmayı önlemek için yeni çıktıda veri alanını yeniden oluşturur. İlk/rastgele eşleşme yerine çoklu adayları kontrol gerektirir.
- `Kesinti_Personel_Eslestir_Final_V5` de var: İ/ı/I ve NBSP normalizasyonu ekler, Personel satır 1'den başlar, bulunmayan banka satırları kalındır. Uygulama bu normalizasyon güvenliğini kullanır. Türkçe ş/ç/ğ/ö/ü ayrımı silinmez; benzer yazımlar otomatik birleştirilmez. Unvanlar silinmez.
- `IsBankasi_To_Mutemet`: banka A:F → Mutemet B:G; A sıra numarası. Kaynak makronun temizleme aralığı imzaları silebilir. Uygulama yalnız veri bölgesini temizler, imzaları korur.
- `KesintiRaporuFinal_KarakterDuyarsiz`: çıkarılan modüllerde bulunamadı; Sayfa1'den rapor üretiminin özgün kuralı doğrulanamaz.
- Diğer modül sayfa koruma kaldırma yordamları içerir; uygulamada çalıştırılmaz veya kullanılmaz.

Personel E gerçek veri bulunmadığı için TC biçimiyle doğrulanamaz. V3 yorumunda hesap/IBAN, V5 yorumunda TC denmiştir. Banka gerçek başlığı TC olduğundan uygulama bu alana metin olarak aktarır, IBAN uydurmaz.

Sayfa1'in orijinal şemasında sicil/cari yok. Kullanıcı D1'e `Cari Kodu`, `Cari Kod` veya `Sicil` başlığı eklerse bu sütun birincil anahtar olarak okunur. Personel B'nin sicil olduğu varsayılmaz; VBA'nın unvan alanıdır. Cari eşleşmezse başka koda sahip adaya adla geçilmez. Çoklu aday, kod/ad uyuşmazlığı, tekrarlı kaynak/hedef, eksik ad/soyad kontrol gerektirir.

Tüm kaynak listelerdeki mükerrer cari kodlar, kaynak ve satır bilgisiyle kontrol listesine aktarılır. Sayfa1'e karşılığı olmayan Kesinti satırları da banka/personel eşleştirmesinde işlenir; sessizce atılmaz. Negatif veya sıfır tutar için iş kuralı kaynak VBA'da tanımlı değildir; tutar işareti değiştirilmez.
