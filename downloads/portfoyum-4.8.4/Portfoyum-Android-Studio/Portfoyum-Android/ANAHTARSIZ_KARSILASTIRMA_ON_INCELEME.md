# Enflasyon / dolar / altın — anahtarsız veri ön incelemesi

Tarih: 24 Ekim 2026. Bu dosya uygulanmış özellik raporu değildir; kaynak/doğruluk araştırmasıdır. Kullanıcı API anahtarı gerektirmeyen yöntem istedi.

## İncelenen seçenekler

1. **TÜFE:** TÜİK'in açık yayımladığı aylık endeks tablolarını kullanmak aday yöntemdir. Otomatik indirme adresi, dosya biçimi, seri kodu, son ay ve düzenli güncelleme henüz doğrulanmadı. Yıllık enflasyon oranı yerine aynı bazdaki aylık endeks seviyeleri gerekir. Baz yılı değişiminde zincir uyumu doğrulanmadan iki seri birleştirilemez.
2. **Dolar:** TCMB'nin açık tarihsel kur XML dosyaları adaydır. Standart aday adres biçimi `https://www.tcmb.gov.tr/kurlar/YYYYMM/DDMMYYYY.xml`. İşgünü olmayan tarihlerde yalnız önceki yayımlanmış kur kullanılabilir; gerçek kullanılan tarih gösterilmeli, gelecek güne bakılmamalı. İlgili dosyalar bu ortamdan doğrulanamadı. Alış/satış kurunun karşılaştırma anlamı açık seçilmeli; referans kur, bankanın gerçekleşmiş makası değildir.
3. **TÜFE için alternatif:** pandas-datareader'ın açık kaynak FRED okuyucusunda `https://fred.stlouisfed.org/graph/fredgraph.csv?id=...` üzerinden anahtarsız CSV okuma yolu görüldü. Bu, belirli bir Türkiye TÜFE serisinin güncel ve kesintisiz olduğu kanıtı değildir. Türkiye serisi, birimi, baz yılı, son ayı, lisansı ve OECD/TÜİK kaynak bağlantısı ayrıca doğrulanmalıdır. FRED'in genel API'siyle anahtarsız grafik CSV yolu aynı şey değildir.
4. **Gram altın:** Tarihsel TL/gram fiyatının kaynağı ve fiyat türü doğrulanmalı. Ons fiyatı veya vadeli kontrat doğrudan gram altın fiyatı sayılamaz. Ons dönüşümü kullanılacaksa spot altın, aynı tarihli kur ve saflık/birim dönüşümü açıkça belirtilmeli; fiziksel/banka gram fiyatıyla farkı gösterilmeli. Uygulamadaki mevcut sağlayıcılar bugünkü fiyatlar içindir; bunlar geriye dönük fiyat arşivi sayılmaz.
5. **EVDS:** İncelenen resmî olmayan açık kaynak istemci EVDS3 adresini ve API anahtarını header'da kullanıyor. Kullanıcı anahtarsız istediği için bunu çözüm olarak dayatmıyoruz. Bu istemci uygulamaya kurulmadı; anahtar istenmedi veya saklanmadı.

## Erişim ve kanıt sınırları

Bu çalışma ortamının ağ politikası TCMB ve TÜİK alan adlarına doğrudan erişimi engelliyor: EVDS2 ve TÜİK istekleri CONNECT 403 verdi. Bu, telefon uygulamasının erişemeyeceğini veya hizmetin çalışmadığını kanıtlamaz. Ağ sınırı aşılmadı, TLS kontrolü kapatılmadı ve aracıyla TCMB/TÜİK içeriği çekilmedi.

Okunabilen doküman/kodlar:

- pandas-datareader FRED okuyucusu: https://github.com/pydata/pandas-datareader/blob/main/pandas_datareader/fred.py
- Resmî olmayan EVDS istemcisi: https://github.com/fatihmete/evds ve https://pypi.org/project/evds/

Bu dokümanların okunması gerçek tarihsel veri alım testi değildir. Anahtarsız tam otomatik karşılaştırmanın çalıştığı henüz iddia edilemez.

## Hesaplama ve ürün kuralları

- Altın/gümüş varlıklarını hariç tutarken kalan yatırımların alış tarihleri ve tutarları esas alınmalı.
- Tek alış için TÜFE karşılaştırma değeri = yatırılan tutar × son uygun endeks / alış ayının uygun endeksi. Ay içi TÜFE için günlük kesinlik iddia edilmez; açıklanmamış ay tahmin edilmez.
- Döviz/altın karşılaştırması her katkıyı o tarihte alınabilecek referans birime çevirir. Birden fazla alış tek başlangıç parası gibi hesaplanmaz.
- Satış, çekim, temettü, faiz ve kapanıştan oluşan bağlı nakit kayıtları açıkça ayrıştırılmalı; alış/satış ve nakit aynı para için iki kez sayılmamalı. Kısmi satışın maliyet paylaşımı mevcut finans hesabıyla tutarlı olmalı.
- Eski başlangıç varlıklarının alış tarihi yoksa bugün alınmış gibi kabul edilmemeli. Kayıt tamlığı gösterilmeli; eksik varlıklar sessizce toplamdan çıkarılıp kesin sonuç sunulmamalı.
- Karşılaştırma verileri gerçek finans kayıtlarından ayrı, kaynak/tarih/birim/sürüm bilgisiyle saklanmalı; hatalı güncellemede son doğrulanmış arşiv korunmalı. Çevrimdışında son tarih açıkça gösterilmeli.
- Yalnız veri için açılan HTTPS host/path izinleri dar olmalı; tüm internete sınırsız native istek yetkisi verilmemeli.

## Uygulama öncesi tamamlanması gereken kanıt

Seçilen anahtarsız kaynaklardan en az üç gerçek tarihli örnek: geçmiş alış, hafta sonu/tatil ve son açıklanan ay. Resmî değerlerle çapraz kontrol; boş/bozuk/yeniden bazlanmış seri testleri; katkı/çekim/satış çift sayım testleri. Bunlar sağlanmadan uygulamada otomatik analiz etkinleştirilmemeli. Kullanıcının API anahtarı istememe tercihi korunmalı. Anahtarsız resmî dosyaların otomatik erişimi sağlanamazsa resmî dosyadan kullanıcı onaylı içe aktarma bir alternatif olarak ayrıca değerlendirilebilir; otomatik çalışıyor diye sunulamaz.
