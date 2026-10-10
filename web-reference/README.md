# Mobil öncelikli React / Tailwind referansı

Node 22.12+ veya Node 24, React 19, Vite 7 ve Tailwind 4.

```sh
cd web-reference
npm ci
npm test
npm run build
npm run dev
```

Cloud ortamında npm önbelleği için `npm ci --cache /workspace/npm-cache` kullanılabilir. Node_modules ve dist Git/ZIP paketine eklenmez. İlk kurulum internet gerektirir.

`TransactionForm.jsx` ve `PeriodSummary.jsx` çalışır; maaş günü 1–31, kısa ay uyarlaması, açık/koyu tema, animasyon, ileri tarihli kayıtların analizden dışlanması ve tam sayı kuruş hesapları bulunur. Mobilde native HTML dialog odak kilidi ve Escape ile kapanış sağlar; masaüstünde form sağ sütundadır.

Bu bir arayüz referansıdır: kayıtlar yalnızca React state içindedir, sayfa yenilenince temizlenir; Android SQLite ile bağlantısı, kullanıcı hesabı veya banka entegrasyonu yoktur. `onSave(payload, idempotencyKey)` arayüzünü [mimari rehberdeki](../docs/ARCHITECTURE.md) API'ye bağla; sunucudaki hesap/kategori UUID'lerini kullan. Bileşendeki kategori kimlikleri yalnızca yerel demo içindir.

Aylık Özet bileşeninin adı `PeriodSummary` seçildi; bu programda raporlar maaş dönemine aittir. Dönem farkı gerçek banka bakiyesi değildir. Renkler %50 altında yeşil, %50–80 sarı, %80+ kırmızı; gelir yoksa nötrdür. Animasyon azaltılmış hareket tercihine saygı duyar; ekran okuyucu yalnızca son oranı okur.

Production için rehberdeki kimlik doğrulama, tenant doğrulaması, sunucu veritabanı, senkronizasyon, erişilebilirlik tarayıcı testleri ve dağıtım adımları ayrıca tamamlanmalıdır. Derlenmesi bir sunucu entegrasyonunun veya canlıya almanın tamamlandığı anlamına gelmez.
