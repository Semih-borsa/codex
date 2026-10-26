# Resmî Tatil Çalışma Sayımı — 2.6

Çizelgedeki resmî tatilin tarihine veya Ayarlar → Resmî Tatil Takvimi → Tam Gün Çalışanlar düğmesine dokunun. Görev yerine göre gerçek tam gün çalışanları işaretleyip kaydedin. Önceden yapılan atamalar otomatik onaylanmaz. Seçimler sonradan kaldırılabilir ve yedekte korunur.

Normal atama istatistikleri korunur. Ayrı resmî tatil tablosu ve PDF yalnız onaylanan çalışmaları kullanır. Önceki kullanıcı tercihi gereği tam tatil 1, yarım tatil 0,5 ağırlıktadır. Aynı gün/kişi/yer tekrarlanmaz. Hafta sonundaki iki tatil günü ayrı düzenlenir. Pasif personel ve eski görev yerleri için geçmiş kayıt düzenlenebilir.

JSON'da `holidayWork` alanı kullanılır; bu alan olmayan eski yedeklerde liste boştur. Paralel geliştirmedeki `fullDayHolidayWork` alanı da okunabilir. Eski sürümle kaydetmek yeni veriyi korumaz; güncel APK kullanılmalıdır.
