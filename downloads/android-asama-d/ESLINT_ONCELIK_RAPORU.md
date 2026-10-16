# ESLint öncelik değerlendirmesi

Aşama D sonundaki aynı App.jsx için kayıtlı sonuç: **50 hata, 12 uyarı; toplam 62 bulgu**. Bu aşamada kod temizliği yapılmadı ve lint geçiyor denmedi. Aşağıdaki öncelikler inceleme sırasıdır; ESLint hata seviyesiyle aynı kavram değildir. Finansal hesap hatası olduğu, yalnız lint mesajından çıkarılamaz. Tanımsız değişken (`no-undef`) bulgusu yoktur. 207 testin geçmesi, bu 62 bulgunun yok olduğu anlamına gelmez.

| Öncelik | ESLint hata | Uyarı | Toplam | Önerilen yaklaşım |
|---|---:|---:|---:|---|
| P1 | 5 | 1 | 6 | Yedek bütünlüğü / finansal özet güncelliğini hedefli test et |
| P2 | 9 | 4 | 13 | Render, memoizasyon ve rapor güncelliğini incele |
| P3 | 36 | 7 | 43 | Daha sonra bakım/uyarı temizliği; otomatik düzeltme yapma |

## Önce ele alınacaklar

- **P1 — 5 boş catch:** App.jsx:5051, 5061, 5071, 5081, 5093. Eski sürüm güvenlik kopyası oluşturulurken tek bir alanın JSON okuma hatası sessizce atlanabiliyor. Bu, mevcut gerçek kaydın silindiğini kanıtlamaz; fakat otomatik kopyanın bütünlüğünü anlamayı zorlaştırır. C'nin ayrı doğrulanan geri yükleme kopyasıyla aynı mekanizma değildir. Bozuk bir alanın yedeğe nasıl yansıdığı ayrıca sınanmalı.
- **P1 — 1 bağımlılık uyarısı:** App.jsx:1702. Tasarruf/günlük pay özetinde `bakiyeOzeti.ayrilan`, `bakiyeOzeti.nakitCikis`, `buAyDevir` doğrudan bağımlılık listesinde yok. Liste zaten başka ilişkili girdileri içeriyor; her durumda yanlış sonuç var denemez. İlgili değer tek başına değiştiğinde özetin güncellenmesi hedefli sınanmalı. Ana `donemBakiyeHesapla` sonucunun hatalı olduğu anlamına gelmez.

## Orta öncelikliler

Render sırasında ref ataması (1171), effect içinde durum güncelleme (1407, 2315, 2424), korunamayan memoizasyon (2528, 2562, 4807), rapor bağımlılıkları (2528, 2542, 2562, 4847) ve dönem varsayılanındaki gereksiz doğruluk ifadesi (1346) değerlendirilir. Bunlar yeniden çizim, performans veya rapor güncelliği açısından incelenmeli; animasyonları değiştirmek için gerekçe sayılmamalı.

3046'daki `Date.now` purity bulgusu ödeme işleyicisindeki kimlik üretimine işaret ediyor. Çağrı kullanıcı olayı içindeyse render sırasında çalıştığı varsayılmamalı; lint analizinin bağlamı incelenmeden mantık değiştirilmemeli.

## Düşük öncelikliler

34 kullanılmayan değişken/parametre, bildirim sayısını okurken tek boş catch, yorum metninden emoji temizleyen karakter sınıfı ve yedi hook uyarısı bu gruptadır. Beş hook uyarısı `useLocalStorage` içindeki `useCallback` ile oluşturulan setter fonksiyonlarının listede olmayışıdır; bu fonksiyonlar anahtar sabitken kararlıdır. İki uyarı gereksiz bağımlılık içindir. Bunları finans motoru hatası diye sunmak doğru olmaz.

## 62 bulgunun tam dökümü

Satırlar teslim edilen App.jsx'e aittir. Kaynak değişirse satırlar değişebilir. Aynı satırdaki iki kural iki ayrı bulgudur.

| # | Öncelik | Seviye | App.jsx satır:sütun | Kural | Mesaj |
|---|---|---|---|---|---|
| 1 | P3 | Hata | 147:7 | `no-unused-vars` | 'tarihAralikta' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 2 | P3 | Hata | 152:10 | `no-unused-vars` | 'donemGiderOzeti' is defined but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 3 | P3 | Hata | 783:7 | `no-unused-vars` | 'krediKartiMarkaRengi' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 4 | P3 | Hata | 1139:12 | `no-unused-vars` | 'hata' is defined but never used. |
| 5 | P2 | Hata | 1171:3 | `react-hooks/refs` | Error: Cannot access refs during render |
| 6 | P2 | Hata | 1346:27 | `no-constant-binary-expression` | Unexpected constant truthiness on the left-hand side of a `\|\|` expression. |
| 7 | P3 | Hata | 1346:97 | `no-unused-vars` | 'e' is defined but never used. |
| 8 | P3 | Hata | 1382:12 | `no-unused-vars` | 'e' is defined but never used. |
| 9 | P3 | Hata | 1386:87 | `no-unused-vars` | 'e' is defined but never used. |
| 10 | P2 | Hata | 1407:5 | `react-hooks/set-state-in-effect` | Error: Calling setState synchronously within an effect can trigger cascading renders |
| 11 | P3 | Uyarı | 1562:6 | `react-hooks/exhaustive-deps` | React Hook useMemo has an unnecessary dependency: 'gununTarihi'. Either exclude it or remove the dependency array. |
| 12 | P3 | Hata | 1648:9 | `no-unused-vars` | 'guncelDonemMi' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 13 | P1 | Uyarı | 1702:6 | `react-hooks/exhaustive-deps` | React Hook useMemo has missing dependencies: 'bakiyeOzeti.ayrilan', 'bakiyeOzeti.nakitCikis', and 'buAyDevir'. Either include them or remove the dependency array. |
| 14 | P3 | Hata | 1746:9 | `no-unused-vars` | 'kategoriTrendVeri' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 15 | P3 | Hata | 1990:21 | `no-unused-vars` | 'odemeKaydi' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 16 | P2 | Hata | 2315:17 | `react-hooks/set-state-in-effect` | Error: Calling setState synchronously within an effect can trigger cascading renders |
| 17 | P2 | Hata | 2424:18 | `react-hooks/set-state-in-effect` | Error: Calling setState synchronously within an effect can trigger cascading renders |
| 18 | P3 | Hata | 2440:10 | `no-unused-vars` | 'ekstreTrendIslemleri' is defined but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 19 | P3 | Uyarı | 2467:73 | `react-hooks/exhaustive-deps` | React Hook useMemo has an unnecessary dependency: 'bugun'. Either exclude it or remove the dependency array. |
| 20 | P3 | Hata | 2514:40 | `no-unused-vars` | 'kartlar' is assigned a value but never used. |
| 21 | P2 | Hata | 2528:22 | `react-hooks/preserve-manual-memoization` | Compilation Skipped: Existing memoization could not be preserved |
| 22 | P2 | Uyarı | 2528:88 | `react-hooks/exhaustive-deps` | React Hook useMemo has a missing dependency: 'aralik'. Either include it or remove the dependency array. |
| 23 | P2 | Uyarı | 2542:94 | `react-hooks/exhaustive-deps` | React Hook useMemo has a missing dependency: 'aralik'. Either include it or remove the dependency array. |
| 24 | P2 | Uyarı | 2562:5 | `react-hooks/exhaustive-deps` | React Hook useMemo has a missing dependency: 'aralik'. Either include it or remove the dependency array. |
| 25 | P2 | Hata | 2562:29 | `react-hooks/preserve-manual-memoization` | Compilation Skipped: Existing memoization could not be preserved |
| 26 | P3 | Hata | 2786:17 | `no-unused-vars` | 'net' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 27 | P3 | Hata | 2980:83 | `no-unused-vars` | 'donemGun' is defined but never used. |
| 28 | P2 | Hata | 3046:53 | `react-hooks/purity` | Error: Cannot call impure function during render |
| 29 | P3 | Hata | 3222:191 | `no-unused-vars` | 'devirLog' is defined but never used. |
| 30 | P3 | Hata | 3222:347 | `no-unused-vars` | 'setBakiyeHesapModu' is defined but never used. |
| 31 | P3 | Hata | 3225:19 | `no-unused-vars` | 'setKurtarma' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 32 | P3 | Hata | 3278:16 | `no-unused-vars` | 'e' is defined but never used. |
| 33 | P3 | Hata | 3278:19 | `no-empty` | Empty block statement. |
| 34 | P3 | Hata | 3430:14 | `no-unused-vars` | 'e' is defined but never used. |
| 35 | P3 | Hata | 3646:48 | `no-unused-vars` | 'kategoriler' is defined but never used. |
| 36 | P3 | Hata | 3649:10 | `no-unused-vars` | 'analizSekme' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 37 | P3 | Hata | 3649:23 | `no-unused-vars` | 'setAnalizSekme' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 38 | P3 | Hata | 3701:9 | `no-unused-vars` | 'aylikVerisi' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 39 | P3 | Hata | 3996:251 | `no-misleading-character-class` | Unexpected combined character in character class. |
| 40 | P3 | Hata | 4022:9 | `no-unused-vars` | 'yillikGelir' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 41 | P3 | Hata | 4023:9 | `no-unused-vars` | 'yillikGider' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 42 | P3 | Hata | 4044:9 | `no-unused-vars` | 'renderDagilim' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 43 | P3 | Hata | 4101:9 | `no-unused-vars` | 'renderTrend' is assigned a value but never used. Allowed unused vars must match /^[A-Z_]/u. |
| 44 | P3 | Hata | 4253:69 | `no-unused-vars` | 'setKategoriler' is defined but never used. |
| 45 | P3 | Hata | 4491:23 | `no-unused-vars` | 'ti' is defined but never used. |
| 46 | P2 | Hata | 4807:19 | `react-hooks/preserve-manual-memoization` | Compilation Skipped: Existing memoization could not be preserved |
| 47 | P2 | Uyarı | 4847:6 | `react-hooks/exhaustive-deps` | React Hook useMemo has missing dependencies: 'buYil', 'enfAl', and 'zamAl'. Either include them or remove the dependency array. |
| 48 | P3 | Hata | 5051:86 | `no-unused-vars` | 'e' is defined but never used. |
| 49 | P1 | Hata | 5051:89 | `no-empty` | Empty block statement. |
| 50 | P3 | Hata | 5061:86 | `no-unused-vars` | 'e' is defined but never used. |
| 51 | P1 | Hata | 5061:89 | `no-empty` | Empty block statement. |
| 52 | P3 | Hata | 5071:86 | `no-unused-vars` | 'e' is defined but never used. |
| 53 | P1 | Hata | 5071:89 | `no-empty` | Empty block statement. |
| 54 | P3 | Hata | 5081:86 | `no-unused-vars` | 'e' is defined but never used. |
| 55 | P1 | Hata | 5081:89 | `no-empty` | Empty block statement. |
| 56 | P3 | Hata | 5093:14 | `no-unused-vars` | 'e' is defined but never used. |
| 57 | P1 | Hata | 5093:17 | `no-empty` | Empty block statement. |
| 58 | P3 | Uyarı | 5171:6 | `react-hooks/exhaustive-deps` | React Hook useEffect has a missing dependency: 'setSonBildirimPlanlama'. Either include it or remove the dependency array. |
| 59 | P3 | Uyarı | 5189:5 | `react-hooks/exhaustive-deps` | React Hook useEffect has a missing dependency: 'setKartBorcOzetleri'. Either include it or remove the dependency array. |
| 60 | P3 | Uyarı | 5205:5 | `react-hooks/exhaustive-deps` | React Hook useEffect has missing dependencies: 'setDevirLog' and 'setIslemler'. Either include them or remove the dependency array. |
| 61 | P3 | Uyarı | 5216:5 | `react-hooks/exhaustive-deps` | React Hook useEffect has missing dependencies: 'setIslemler' and 'setOdemeler'. Either include them or remove the dependency array. |
| 62 | P3 | Uyarı | 5268:6 | `react-hooks/exhaustive-deps` | React Hook useEffect has missing dependencies: 'setIslemler', 'setOdemeler', and 'setSabitEklemeLog'. Either include them or remove the dependency array. |
