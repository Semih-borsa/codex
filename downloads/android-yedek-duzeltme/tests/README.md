# Tekrar çalıştırma

12 JavaScript test dosyası: mevcut 207 + yeni 22 = 229 test. Testler sentetik depolarda çalışır; gerçek telefon verisini kullanmaz. Node, mevcut projedeki @babel/parser/esbuild/React paketleri ve Chromium/Playwright gerekir; otomatik yeni paket yüklenmez.

Bulutta kullanılan bağımlılık kökü `/workspace/finans-upload/finans-takip`; Playwright `/opt/codex/cua_node/lib/node_modules/playwright-core/index.mjs`; Chromium `/usr/bin/chromium`. Başka ortamda kendi yollarını `FINANS_DEPENDENCY_ROOT`, `FINANS_PLAYWRIGHT`, `FINANS_CHROMIUM` ile ver. Teslim klasöründe:

```sh
TZ=Europe/Istanbul FINANS_SOURCE="$PWD/App.jsx" FINANS_DEPENDENCY_ROOT=/workspace/finans-upload/finans-takip node --test --test-reporter=tap tests/finance-baseline/*.test.mjs
```

Native 10 test, verilen BackupDocumentIOTest.java dosyasını aynı kaynak dizinine eklediğin tam Android/Capacitor projesinde mevcut Gradle/JDK/SDK ile `:app:testDebugUnitTest` göreviyle çalışır. Eski projede zaten JUnit 4.13.2 test bağımlılığı vardı. Android sistem belge seçicisinin gerçek ekranı bu JVM testleriyle sınanmaz; rapordaki fiziksel cihaz adımları ayrıca gereklidir.
