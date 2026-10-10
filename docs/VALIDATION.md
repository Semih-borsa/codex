# Doğrulama · v1.2.0 · 10 Ekim 2026

Kaynaklar tam JDK 17, Android SDK 35, Build-Tools 34.0.0 ve Gradle 8.9 ile derlendi. Debug APK Android 11 (API 30) emülatöründe çalıştırıldı.

## Otomatik kontroller

- `testDebugUnitTest`: **18 test geçti**, 0 başarısız, 0 hatalı, 0 atlanan.
- `connectedDebugAndroidTest`: **19 test geçti**, 0 başarısız, 0 hatalı, 0 atlanan. Gerçek Android SQLite ve ayrı `ledger-validation.db` kullanıldı.
- `lintDebug`: **0 hata, 0 uyarı**.
- `assembleDebug` / `assembleDebugAndroidTest`: başarılı.
- `apksigner verify`: imza geçerli. v1.0, v1.1 ve v1.2 APK sertifikalarının SHA-256 özeti aynı; güncelleme imzası korunuyor.

Hesaplama ve SQLite kapsamı: maaş döneminin yıl/ay sınırları, kısa ayda 31'inci gün, dönem uzunluğu ve tahmini, harcama yüzdesinde %100 aşımı/gelir yokluğu, kesimden sonraki kart ödeme günü, bilinen faturaları günlük ortalamada tekrar büyütmeyen tahmin, SQLite v1→v2 veri koruma, v1 yedeği desteği, yeni ayarların yedek dönüşümü, fatura ödeme/geri alma, aylık faturada 31→29→31, tekrar ödeme engeli ve kart ödemesinin gideri ikinci kez artırmaması test edildi.

Yeni v1.2 testleri: işlem tekrarının bağımsız, kaydedilmemiş taslak oluşturması ve günlük harcama toplamlarının gelir/transferleri dışarıda bırakarak tarih sınırlarına göre hesaplanması.

## Ekran ve APK güncelleme kontrolü

Önce yayımlanmış v1.1 APK kuruldu; 1.250,50 TL gider, 5.000,00 TL gelir ve bekleyen 250 TL Elektrik faturası girildi. v1.2 APK üzerine güncelleme olarak kuruldu. **3.749,50 TL bakiye, iki işlem ve fatura korundu**; yeni net varlık kartı gösterildi.

- İşlem satırına basılı tutularak tekrar formu açıldı. Vazgeç sonrası işlem sayısı iki kaldı. Kaydet sonrası üç oldu; eski kayıt değişmedi ve bakiye **2.499,00 TL** oldu. Bekleyen fatura varken yeni tekrar kaydı da doğru oluşturuldu.
- Koyu ve açık tema ekran görüntüleri alındı; açık tema kontrolünde **2.499,00 TL** korundu.
- Yeni ödeme merkezi Elektrik faturasını gösterdi. Ödemeyi kaydet işlemi faturayı Ödendi durumuna geçirdi; bakiye **2.249,00 TL** oldu.
- Harcama haritasından açılan günlük listede **10.10.2026 · 2.751,00 TL** seçildi; gün detayında toplam gider **2.751,00 TL** doğrulandı. Gelir ve transferler bu gider toplamına katılmadı.

- Canvas haritada 10 Ekim hücresine doğrudan dokunuldu; doğru gün ve **2.751,00 TL** gider toplamı açıldı. Harcama göstergesinde **%55,0**, gelirden kalanda **%45,0** ve hedef ilerlemesi gözle kontrol edildi.
- Özet kısa yolundan ayrı bütçe asistanı açıldı; yorumlar ve çevrimdışı hesaplama açıklaması gösterildi. Android geri tuşu genel bakışa döndürdü.

Önceki v1.1 ekran doğrulamaları: maaş günü 15 ve yeniden açılış, kalıcı tema, fatura ödeme/geri alma, kart ödeme transferinde toplam bakiyenin korunması, gelir harcama oranı, kategori bütçesi aşımı ve bekleyen faturaları dikkate alan asistan senaryosu. Yeni sürümde bu hesaplar değişmedi; ilgili otomatik testler tekrar geçti.

Ekran görüntülerindeki tutarlar emülatör test verisidir; APK bu verileri içermez. Kontrol sonrası yalnızca test emülatörünün uygulama verileri temizlendi. Fiziksel telefon üzerinde ayrıca doğrulama yapılmadı. Android 8.0 minimum sürümü statik kontrolle, çalıştırma davranışı Android 11'de doğrulandı. APK kişisel kullanım için debug imzalıdır.

Bulut `install_script` ve `start_skill` taslağı önceki kurulumda kaydedildi. Taslak kaydı ortam sürümünü yayımlamaz; yeni görevde snapshot geri yüklemesi ayrıca denenmedi. Bu sürüm yeni araç veya çalışma zamanı bağımlılığı eklemez.
