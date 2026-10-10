# Doğrulama · v1.1.0 · 10 Ekim 2026

Kaynaklar tam JDK 17, Android SDK 35, Build-Tools 34.0.0 ve Gradle 8.9 ile derlendi. Debug APK Android 11 (API 30) emülatöründe çalıştırıldı.

## Otomatik kontroller

- `testDebugUnitTest`: **16 test geçti**, 0 başarısız, 0 hatalı, 0 atlanan.
- `connectedDebugAndroidTest`: **19 test geçti**, 0 başarısız, 0 hatalı, 0 atlanan. Gerçek Android SQLite ve ayrı `ledger-validation.db` kullanıldı.
- `lintDebug`: **0 hata, 0 uyarı**.
- `assembleDebug` / `assembleDebugAndroidTest`: başarılı.
- `apksigner verify`: imza geçerli. v1.0 ve v1.1 APK sertifikalarının SHA-256 özeti aynı; güncelleme imzası korunuyor.

Yeni kapsam: maaş döneminin yıl/ay sınırları, kısa ayda 31'inci gün, dönem uzunluğu ve tahmini, harcama yüzdesinde %100 aşımı/gelir yokluğu, kesimden sonraki kart ödeme günü, bilinen faturaları günlük ortalamada tekrar büyütmeyen tahmin, SQLite v1→v2 veri koruma, v1 yedeği desteği, yeni ayarların yedek dönüşümü, fatura ödeme/geri alma, aylık faturada 31→29→31, tekrar ödeme engeli ve kart ödemesinin gideri ikinci kez artırmaması test edildi.

## Ekran ve APK güncelleme kontrolü

Önce yayımlanmış v1.0 APK kuruldu; 1.250,50 TL gider ve 5.000,00 TL gelir girildi. v1.1 APK üzerine güncelleme olarak kuruldu. **3.749,50 TL bakiye ve kayıtlar korundu**.

- Maaş günü 15 seçildi; ayar kaydedildi ve yeniden açılışta korundu.
- Açık/koyu temalar değiştirildi; açık tema yeniden açılışta korundu.
- 250 TL Elektrik faturası eklendi ve ödendi; bakiye **3.499,50 TL** oldu. Ödeme geri alındığında **3.749,50 TL** geri geldi ve fatura tekrar bekleyen duruma döndü.
- Kart kesim/son ödeme tarihleri gösterildi. Kart ödeme ekranından 100 TL transfer kaydedildi; toplam net bakiye **3.749,50 TL** kaldı.
- Analizde harcanan gelir oranı **%25,0** doğrulandı.
- 1.000 TL Market bütçesinde **250,50 TL** aşım doğrulandı.
- Son APK'nın asistan ekranı ve fatura tahmini açıklaması okundu. 10 günlük 1.250,50 TL gider hızı ile 250 TL bekleyen fatura için **4.126,55 TL** dönem sonu senaryosu doğrulandı.

İlk ekran otomasyonunda hızlı kaydırma asistan başlığını atlayıp aramayı başarısız yaptı. Kalan kontroller küçük ve yavaş kaydırmalarla sürdürüldü ve geçti. Bu otomasyon hatası, geçmiş kontrollerin tekrar yapılmış veya tüm ilk betiğin geçmiş olduğu şeklinde değerlendirilmedi.

Ekran görüntülerindeki tutarlar emülatör test verisidir; APK bu verileri içermez. Kontrol sonrası emülatör uygulama verileri temizlendi. Fiziksel telefon üzerinde ayrıca doğrulama yapılmadı. Android 8.0 minimum sürümü statik kontrolle, çalıştırma davranışı Android 11'de doğrulandı. APK kişisel kullanım için debug imzalıdır.

Bulut `install_script` ve `start_skill` taslağı önceki kurulumda kaydedildi. Taslak kaydı ortam sürümünü yayımlamaz; yeni görevde snapshot geri yüklemesi ayrıca denenmedi. Bu sürüm yeni araç veya çalışma zamanı bağımlılığı eklemez.
