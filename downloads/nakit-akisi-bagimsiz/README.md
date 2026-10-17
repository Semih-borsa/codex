# Finans Nakit Akışı — bağımsız uygulama

**[APK’yı indir](./Finans-Nakit-Akisi-Bagimsiz.apk)** — GitHub dosya sayfasında Download raw file düğmesini kullan.

- Telefonda görünen ad: **Finans Nakit Akışı**.
- Paket kimliği: **com.butcetakip.nakitakisi**.
- Sürüm: **1.3.0-nakit**, versionCode **1**.
- Ana Finans uygulaması com.butcetakip.app ve önceki .devtest uygulamasından farklı kimliktedir. Bunları güncellemez; ayrı kurulur.
- Kayıtlar Android’in ayrı uygulama alanında tutulur; mevcut uygulamanın verilerini otomatik okumaz/taşımaz. Yeni kurulum boş finans kayıtlarıyla başlar. İstersen dosya yedeğini yalnız yeni uygulamada kendin içe aktarabilirsin; otomatik içe aktarma yapılmaz.
- Yeni uygulama, son Nakit Akışı sürümünün tamamını içerir. App.jsx ve hesaplamalar değiştirilmedi; yalnız ayrı derleme kopyasının paket kimliği, uygulama adı ve sürümü düzenlendi.
- Ana kaynak proje ile .devtest projesi değiştirilmedi. Yeni paket için Capacitor eşitlemesi, Android debug derlemesi ve native testler çalıştırıldı. Önceki 308 JS/React testi aynı kaynak içindir; kaynak değişmediği için tekrar çalıştırılmadı.
- APK gerçek telefonda henüz denenmedi. Mevcut Finans uygulamasını kaldırma veya verilerini temizleme.

Kimlik/imza, dosya boyutu ve SHA-256 kanıtları kanitlar/ klasöründedir. Android paket kimliği App.jsx içinden değişmez; yeni kimlik native derleme yapılandırmasındadır.
