# İşletmem Kasa 1.0.5 · Fiş Tarihi

[Yeni Android APK İndir](https://github.com/Semih-borsa/codex/blob/main/downloads/Isletmem-Kasa-1.0.5-Tarih.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.5-Kaynak.zip)

Fiş fotoğrafı kontrol ekranından işlem tarihini seçebilirsiniz. Seçtiğiniz tarih kayda ve 15/21 günlük tahsilat hesabına aktarılır. Android takvimi açılır; geçmiş tarihli fişler kaydetmeden önce kontrol edilir.

**Bu APK ayrı kurulur:** Telefonda “İşletmem Kasa · Tarih” adıyla görünür. Önceki uygulamanın imza anahtarı bu ortamda olmadığı için üstüne güncelleme değildir. Eski uygulamayı silmeyin.

1. Eski uygulama → Ayarlar → Yedeği Kaydet.
2. Yeni APK'yı kurun; “İşletmem Kasa · Tarih”i açın.
3. Ayarlar → Yedekten Ekle ile dosyanızı seçin.
4. Kayıtları ve kasa ayarlarını kontrol edin. Uygulamalar verileri otomatik paylaşmaz. Eski yedeği saklayın: geçmiş düzeltmeler/taslaklar dosyada korunur, yeni uygulamaya yalnız güncel kayıtlar eklenir. Aynı adlı mevcut kasanın adı/avansı/POS giriş türünü yeni uygulamada kontrol edin.

---

# İşletmem Kasa 1.0.4

[Android APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.4.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.4-Kaynak.zip)

**Aşağıdaki açıklama yalnız 1.0.4 APK içindir:** Eski uygulamayı silmeden güncelleyin. Aynı paket ve imza, daha yüksek sürüm kodu kullanılır. Güncellemeden önce uygulamadaki yedek seçeneği kullanılabilir.

- Onaylanan kasa farkı: banknot + adetli madeni + diğer bozuk − avans − beklenen. Gereken bozuk tutarı ilgili alanın altında gösterilir.
- Güncel kasa adı geçmişte görünür; kayıtların tutarları ve eski ad anlık görüntüleri silinmez.
- Bugün yatacak POS özeti tüm kasaların kredi/debit dağılımını listeler.
- Günlük/haftalık/aylık nakit ve POS; tek/tüm kasa, günlük toplam, satış/vade ayrımı.
- 1528.34 girişi alan değişince 1.528,34 olur; 5 TL (Madeni) aynı renkte gösterilir.
- Başlık, alan etiketi ve düğmelerin baş harfleri büyük.

[POS Görünümü · Örnek Veriler](Isletmem-Kasa-1.0.2-POS.png) · [Analiz Görünümü · Örnek Veriler](Isletmem-Kasa-1.0.2-Analiz.png) · [Doğrulama](Isletmem-Kasa-Dogrulama.md)


1.0.3 Fiş Okuma: OCR sütunları fiziksel satır konumuna göre birleştirilir, silik başlık ve TL son eki hataları daha esnek eşleştirilir. Gerçek örnek fişte 4/4 tutar ve 60.040,08 TL toplam doğrulandı. Okunamayan alanlar belirtilir; fotoğraf kaydında genel toplam zorunludur. Onaysız kayıt yapılmaz. Yüklenen özel fotoğraf yayına eklenmez. Önceki sürümden aynı paket ve imzayla güncellenir.


## 1.0.4

Sayım geçmişi gün bazlıdır. Günlük Mutabakat tüm kasaların imzalı farkını toplar; girilmemiş kasa görünmez, taslak etiketlidir. Tek sayım veya günün sayımları açık onayla silinir; ilişkili taslak/düzeltmeler temizlenir, POS ve diğer günler korunur.

Ayarlar → Kasa Düzenle → POS Giriş Türü → Tek Toplam. İşlem günü 15.000 TL toplam girilir. 15 gün sonra yatan debit 5.000 TL girilip bulunan işlem günü onaylanınca, aynı işlemin +21. gününe beklenen kredi 10.000 TL yazılır. Kredi yattığında ikinci giriş gerekmez. Debit gelene kadar dağılım beklenir. Yanlış tarihte kayıt bulunamazsa başka güne bağlanmaz. Güncel kayıt gerekçeli düzeltilebilir; eski sürümü arşivlenir.
