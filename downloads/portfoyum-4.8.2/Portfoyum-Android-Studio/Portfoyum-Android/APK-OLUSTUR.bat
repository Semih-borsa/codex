@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

rem Sadece bu komut penceresinin Java ayari degisir; sistem ayarlari degismez.
if defined PORTFOY_JAVA_HOME (
    set "JAVA_HOME=%PORTFOY_JAVA_HOME%"
) else (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    ) else (
        if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
    )
)
set "PORTFOY_JAVA_EXE=java.exe"
if defined JAVA_HOME set "PORTFOY_JAVA_EXE=%JAVA_HOME%\bin\java.exe"
"%PORTFOY_JAVA_EXE%" tools\JavaCheck.java
if errorlevel 1 (
    echo.
    echo Java 17 veya 21 JDK secilmeli. Eski Java 8 JRE ile APK olusmaz.
    echo Android Studio icinde Gradle JDK olarak Embedded JDK veya JDK 17/21 secin.
    echo Alternatif: PORTFOY_JAVA_HOME ortam degiskenine JDK klasorunu yazin.
    pause
    exit /b 1
)

rem Android SDK yolunu ortam degiskenlerinden veya Windows varsayilanindan bul.
set "PORTFOY_SDK_DIR="
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platforms" set "PORTFOY_SDK_DIR=%ANDROID_HOME%"
if not defined PORTFOY_SDK_DIR if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platforms" set "PORTFOY_SDK_DIR=%ANDROID_SDK_ROOT%"
if not defined PORTFOY_SDK_DIR if exist "%LOCALAPPDATA%\Android\Sdk\platforms" set "PORTFOY_SDK_DIR=%LOCALAPPDATA%\Android\Sdk"
if not defined PORTFOY_SDK_DIR if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platforms" set "PORTFOY_SDK_DIR=%USERPROFILE%\AppData\Local\Android\Sdk"

rem Daha once Android Studio tarafindan yazilmis gecerli yolu da kabul et.
if not defined PORTFOY_SDK_DIR if exist "local.properties" (
    for /f "usebackq tokens=1,* delims==" %%A in ("local.properties") do (
        if /i "%%A"=="sdk.dir" set "PORTFOY_SDK_DIR=%%B"
    )
)

if not defined PORTFOY_SDK_DIR goto sdk_missing
if not exist "!PORTFOY_SDK_DIR!\platforms" goto sdk_missing

rem Java properties icin Windows ters egik cizgileri guvenli / isaretine cevir.
set "PORTFOY_SDK_PROPERTY=!PORTFOY_SDK_DIR:\=/!"
>"local.properties" echo sdk.dir=!PORTFOY_SDK_PROPERTY!
echo Android SDK bulundu: !PORTFOY_SDK_DIR!

if not exist "!PORTFOY_SDK_DIR!\platforms\android-35\android.jar" (
    echo.
    echo Android SDK bulundu fakat Android 15 ^(API 35^) platformu eksik.
    echo Android Studio ^> SDK Manager icinden Android 15 / API 35'i kurun.
    pause
    exit /b 1
)
if not exist "!PORTFOY_SDK_DIR!\build-tools\35.0.0\aapt2.exe" (
    echo.
    echo Android SDK Build Tools 35.0.0 eksik.
    echo Android Studio ^> SDK Manager ^> SDK Tools icinden 35.0.0 surumunu kurun.
    pause
    exit /b 1
)

echo.
echo APK olusturuluyor. Ilk derlemede Android/Gradle bilesenleri indirilebilir.
call gradlew.bat --no-daemon :app:assembleDebug
if errorlevel 1 (
    echo.
    echo APK olusmadi. Yukaridaki ilk hata mesaji nedeni gosterir.
    echo Gradle surumunu rastgele degistirmeyin; projedeki uyumlu eslesmeyi kullanin.
    pause
    exit /b 1
)
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "Portfoyum.apk" >nul
if errorlevel 1 (
    echo APK kopyalanamadi. app\build\outputs\apk\debug klasorunu kontrol edin.
    pause
    exit /b 1
)
echo.
echo HAZIR: %CD%\Portfoyum.apk
echo Bu dosyayi telefonunuza aktarabilirsiniz.
explorer.exe /select,"%CD%\Portfoyum.apk"
pause
endlocal
exit /b 0

:sdk_missing
echo.
echo Android SDK bulunamadi.
echo Android Studio ^> More Actions ^> SDK Manager ekranini acin.
echo Android 15 ^(API 35^) ve Android SDK Build-Tools 35.0.0'i kurun.
echo Standart konum genellikle: %LOCALAPPDATA%\Android\Sdk
echo Sonra APK-OLUSTUR.bat dosyasini yeniden calistirin.
pause
exit /b 1
