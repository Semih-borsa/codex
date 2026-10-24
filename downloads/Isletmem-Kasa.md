# İşletmem Kasa 1.0.12 · Analiz Ve Yorumlar

[APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.12.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.12-Kaynak.zip)

Analiz ekranı sadeleştirildi: dört temel tutar, günlük nakit/POS için geçen hafta ortalamasına göre TL ve yüzde farkı, haftalık açık/fazla/dengeli gün sayıları ve önceki dönemle karşılaştırmalar. Uzun tablolar Ayrıntılı Tablolar altında açılır. Eksik günler sıfır sayılmaz; aynı kasa kapsamı aranır, az veri ve bilinmeyen POS dağılımı belirtilir. Hesaplanan verilerden yorum üretilir; nakit/POS kâr sayılmaz. Kaydedilmiş veriler kullanılır, taslaklar dahil edilmez.

56 birim testi ve analiz arayüz kontrolü geçti. Kayıt ve hesaplama kuralları korunur; orijinal 1.0.11 üzerine silmeden kurun.

---

# İşletmem Kasa 1.0.11

[APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.11.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.11-Kaynak.zip)

Yurt içi debit kartlar +16 takvim günü olarak güncellendi. İlk açılışta kayıtlı ayrıntılı POS debit vadeleri otomatik düzeltilir; tek toplam kayıtlar da +16 gösterilir ve yeni yatışlar 16 gün önceki işleme eşleştirilir. Gerçek girilmiş banka yatış tarihleri/tutarları, toplamlar ve nakit kayıtları değiştirilmez. Eski yedekleri içe alırken aynı kural uygulanır. Kredi kartı/World/yurt dışı kartlarda +21 korunur.

Orijinal 1.0.10 üzerine uygulamayı silmeden kurun. 51 birim testi geçti; Android derlemesi/imza kontrol edildi. Gerçek cihazda veritabanı yükseltme testi henüz çalıştırılmadı.

---

# İşletmem Kasa 1.0.10

[APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.10.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.10-Kaynak.zip)

Klavye açıldığında yazılan alan görünür aralığa kaydırılır. Alt menü ve sabit sayım özeti klavyeyle gizlenip kapanınca geri gelir. Beklenen tutar, para adetleri ve diğer girişler için geçerlidir; hesaplama/kayıt şeması değişmedi. Tarayıcıda dar viewport ve IME köprüsü taklidiyle kontrol edildi; gerçek Android klavye cihaz testi yapılmadı. Orijinal 1.0.9 üzerine silmeden kurulur.

---

# İşletmem Kasa 1.0.9

[APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.9.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.9-Kaynak.zip)

Ana Kasa dahil son etkin kasa da onayla kaldırılabilir. Kasa arşivlenir; önceki sayım/POS ve beklenen ödemeler korunur. Etkin kasa yokken Kasa Ekle ekranı gösterilir. Yeni kurulumda varsayılan kasa oluşturulmaz; mevcut kullanıcı kasaları otomatik silinmez. Orijinal 1.0.8 üzerine silmeden güncelleyin.

---

# İşletmem Kasa 1.0.8

[APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.8.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.8-Kaynak.zip)

Ayarlar → Kasalar Ve Avanslar → Kasayı Sil. Kasa açık onaydan sonra arşivlenir; eski kayıtlar, analizler ve beklenen POS ödemeleri korunur. Seçili kasa kaldırılırsa etkin kasaya geçilir. Düzenle üzerinden yeniden etkinleştirilebilir. Son etkin kasa için önce başka bir kasa eklenmelidir.

Orijinal paket/imza korunur; 1.0.7 ve 1.0.4 orijinal paketinin üzerine kurulur. Ayrı tr.kasadenge.app.tarih paketli 1.0.5/1.0.6 için önceki yedek aktarım açıklaması geçerlidir; eski uygulamayı silmeyin.

---

# İşletmem Kasa 1.0.7

[APK İndir](https://raw.githubusercontent.com/Semih-borsa/codex/main/downloads/Isletmem-Kasa-1.0.7.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.7-Kaynak.zip)

- Basılı kasa defterinden tarih, kasa adı, beklenen tutar ve para adetleri için alan bazlı okuma eklendi. Okunamayan adet, satır toplamından tam bölünüyorsa tahmin olarak sunulur; tahminler ayrıca onay ister. Fotoğraf onayı formu doldurur, otomatik kapanış kaydetmez.
- Diğer Bozuk Para boş başlar; gereken bozuk tutarı hesaplanır.
- Kayıtlı ayrıntılı ve tek toplam POS işlem tarihleri gerekçeli düzeltilebilir. Vade yenilenir; önceden girilmiş gerçek debit banka tarihi/tutarı korunur. Tarih çakışmasında üzerine yazılmaz.
- Fiş tarihi için Android takvim güncellemesi korundu.

**Kurulum:** Bu dosya orijinal `tr.kasadenge.app` paketi ve orijinal sertifikasıyla imzalıdır; 1.0.4 ve eski orijinal sürümlerin üzerine kurulur. Repodaki 1.0.5/1.0.6 `tr.kasadenge.app.tarih` ayrı paketidir ve bu APK ile otomatik güncellenmez. O sürümü kullanıyorsanız önce Ayarlar → Yedeği Kaydet; orijinal uygulamada Ayarlar → Yedekten Ekle. Eski uygulamayı silmeyin. Güncel kayıtlar aktarılır; geçmiş düzeltmeler/taslaklar yedek dosyasında tutulur; kasa ayarlarını kontrol edin.

**Doğrulama:** 50 birim testi ve sayım/POS tarayıcı regresyonları geçti. Android derlemesi ve imza doğrulandı. Gerçek el yazılı fotoğrafın Android OCR sonucu ve yeni native SQLite tarih düzeltme testleri emülatör sorunu nedeniyle doğrulanamadı; el yazısının eksiksiz okunacağı garanti edilmez. Özel fotoğraf/anahtar yayımlanmaz.

---

# İşletmem Kasa 1.0.6

[APK doğrudan indir](https://github.com/Semih-borsa/codex/raw/refs/heads/main/downloads/Isletmem-Kasa-1.0.6.apk) · [Kaynak ZIP](Isletmem-Kasa-1.0.6-Kaynak.zip)

Telefondaki uygulama adı yalnız **İşletmem Kasa**. 1.0.5 “İşletmem Kasa · Tarih” sürümünün üzerine aynı paket ve imzayla kurulur; uygulamayı silmeden güncelleyin. Kayıtlar korunur. Fiş tarihi düzeltmesi devam eder. 1.0.4 ve daha eski orijinal paket için ayrı kurulum/yedek aktarımı gerekliliği sürer; orijinal uygulamayı silmeyin.

---

# İşletmem Kasa 1.0.5 · Fiş Tarihi

[APK doğrudan indir](https://github.com/Semih-borsa/codex/raw/refs/heads/main/downloads/Isletmem-Kasa.apk)

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
