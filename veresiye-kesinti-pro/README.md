# Veresiye Kesinti Pro

C# / .NET 8 / WPF masaüstü uygulaması. Excel kurulu olmadan ClosedXML (MIT) ile yerel `.xlsx` / `.xlsm` okur, yeni `.xlsx` sonuç üretir. Makro çalıştırmaz. Ağ istemcisi, telemetri veya kişisel veri günlüğü içermez.

**Durum:** Windows hedefi çapraz derlenir. WPF ekranları ve kurulum Windows'ta henüz çalıştırılarak doğrulanmadı. Bu sürüm, eksik kaynak makro nedeniyle üretim bordrosu için onaylanmış değildir.

## Kaynak dosyaya ilişkin önemli eksik

İstenen dosya adı V3.0(2) iken yüklenen dosya V3.0'dır. Altı sayfa vardır, ancak tüm personel/kesinti/irtibat veri satırları boştur. Dolayısıyla gerçek kişiler üzerinde eşleştirme doğrulaması yapılamaz. `KesintiRaporuFinal_KarakterDuyarsiz` makrosu bu VBA projesinde **yoktur**. Bu makronun iş kurallarının taşındığı iddia edilmez. Tam rapor kuralının doğrulanması için ilgili sürüm gereklidir. Mevcut makroların gerçek kodları [VBA-extracted.txt](docs/VBA-extracted.txt), sayfa ve kural analizi [ANALYSIS.md](docs/ANALYSIS.md) içindedir.

Sayfa1→Kesinti karşılaştırması bu sürümde güvenli, tekil normalize ad/soyad eşitliğidir. Tutar farklıysa kesin eşleşme yapılmaz, kontrol istenir. Bu, eksik VBA raporunun birebir uyarlaması değildir. Kısmi kesinti kuralı bilinmediği için “Kesinti Yapılamayan Tutar” alanında Sayfa1 tutarı gösterilir; fark tutarı olduğu varsayılmaz. Şüpheli kayıtlar kesinti yapılmayanlar ve irtibat listesinde görünür; durum sütunu kararın kesin olmadığını belirtir.

## Kullanım

1. Altı sayfayı içeren kaynak dosyayı yükleyin. Personel, kesinti ve Sayfa1 listelerini inceleyin.
2. Eşleştirmeyi başlatın; kontrol gerekenler ve sonuç önizlemelerini inceleyin.
3. Excel'e aktar ile yeni ve kullanılmayan bir dosya adı seçin. Kaynak ve mevcut dosyalar üzerine yazılmaz.

Kaynak kitabın sayfaları korunur. Banka ve Mutemet verileri yeni sonuç kitabında yeniden oluşturulur; Mutemet başlığı, birleşimler, toplam ve imzalar korunur. 298 kayıt aşılırsa imza/total bloğu aşağı taşınır. En sona `Kesinti Yapılmayanlar İrtibat` eklenir. Telefonlar metin yazılır; kaynakta sayı olarak girilip zaten kaybolmuş sıfırlar uydurulmaz. Excel makroları sonuç `.xlsx` dosyasına aktarılmaz. Banka çıktısındaki çözülemeyen kimlikler VBA'daki gibi ticari unvanla ve kalın yazıyla görünür; ödeme gönderimi öncesi mutlaka kontrol listesinden çözülmelidir.

## Windows derleme ve kurulum

Gereken: .NET 8 SDK (8.0.419 veya daha yeni 8.0 yaması), Windows 10/11 x64. Kurulum paketi için Inno Setup 6 ve PATH üzerinde `ISCC.exe`.

```powershell
cd veresiye-kesinti-pro
dotnet build src/Desktop/Desktop.csproj -c Release
dotnet run --project tests/Tests.csproj -- 'C:\veri\VERESİYE KESİNTİ FORMATI V3.0.xlsm'
powershell -ExecutionPolicy Bypass -File installer/build-windows.ps1
```

`artifacts/win-x64/VeresiyeKesintiPro.exe` ve bağımlılıkları self-contained dağıtımdır; Excel/.NET runtime kurulumu istemez. Setup çıktısı: `artifacts/installer/VeresiyeKesintiPro-Setup.exe`. Windows'ta Inno Setup çalıştırılmadan Setup.exe üretilmiş sayılmaz. Uygulama dizininin tamamını birlikte dağıtın.

Linux'ta motor testleri ve Windows çapraz derlemesi çalışır; WPF çalışmaz. Testler verilen gerçek boş şablonun geçici kopyalarına sentetik veriler yazar, kaynak SHA-256 değerinin değişmediğini doğrular ve geçici çıktıları siler. Telefon/TC için tamamen sentetik değerler kullanılır. Yeni ortamda şablon dosyasının yolunu test komutuna açıkça verin.
