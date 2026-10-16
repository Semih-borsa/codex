from pathlib import Path
import shutil,json,hashlib
source=Path('/workspace/finans-upload/finans-takip')
target=Path('/workspace/android-device-validation')
assert not target.exists(), 'Var olan test kopyası üzerine yazılmaz.'
target.mkdir()
# Yalnız Android kaynakları ve hazır web varlıkları. İmzalama anahtarları kopyalanmaz.
def ignore(directory,names):
 return [n for n in names if n in {'.git','.gradle','build','node_modules','google-services.json'} or n.endswith(('.keystore','.jks','.apk','.aab'))]
shutil.copytree(source/'android',target/'android',ignore=ignore)
(target/'node_modules').symlink_to(source/'node_modules',target_is_directory=True)
p=target/'android/app/build.gradle';s=p.read_text();assert 'signingConfig' not in s
assert '    buildTypes {\n' in s
s=s.replace('    buildTypes {\n','    buildTypes {\n        debug {\n            applicationIdSuffix ".devtest"\n            versionNameSuffix "-d-test"\n        }\n',1);p.write_text(s)
p=target/'android/app/src/debug/res/values/strings.xml';p.parent.mkdir(parents=True,exist_ok=True)
p.write_text('''<resources>
<string name="app_name">Finans Test D</string>
<string name="title_activity_main">Finans Test D</string>
<string name="package_name">com.butcetakip.app.devtest</string>
<string name="custom_url_scheme">com.butcetakip.app.devtest</string>
</resources>\n''')
p=target/'android/app/src/debug/AndroidManifest.xml'
p.write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools"><application android:allowBackup="false" tools:replace="android:allowBackup"/></manifest>\n''')
p=target/'android/app/src/main/assets/capacitor.config.json';data=json.loads(p.read_text());data['appId']='com.butcetakip.app.devtest';data['appName']='Finans Test D';p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
(target/'android/local.properties').write_text('sdk.dir=/workspace/android-sdk\n')
files=['android/app/build.gradle','android/app/src/main/AndroidManifest.xml','android/app/src/main/res/values/strings.xml','src/App.jsx']
evidence={str(source/p):hashlib.sha256((source/p).read_bytes()).hexdigest() for p in files}
Path('/workspace/output/android-dogrulama/uretim-korumasi.json').write_text(json.dumps(evidence,indent=2)+'\n')
print('Bağımsız test kopyası hazır:',target)
