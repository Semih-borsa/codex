# Isolated emulator only. Requires adb-shell and debug app + test APK builds.
import time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ARTIFACTS=ROOT/'.artifacts'
ARTIFACTS.mkdir(exist_ok=True)
from adb_shell.adb_device import AdbDeviceTcp

def connect():
 d=AdbDeviceTcp('127.0.0.1',5555,default_transport_timeout_s=120);d.connect();return d

d=connect()
def shell(command):return d.shell(command,read_timeout_s=120,timeout_s=150)
print(shell('getprop sys.boot_completed'),flush=True)
d.push(str(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'),'/data/local/tmp/gunplan-test.apk',read_timeout_s=120)
try:print(shell('pm install -r /data/local/tmp/gunplan-test.apk'),flush=True)
except Exception as e:
 print('Installer wait:',type(e).__name__,flush=True);d.close();d=connect()
assert 'package:' in shell('pm path tr.gunplan.app.test'), 'Test APK not installed'
print(shell('input keyevent 224; wm dismiss-keyguard; am start -n tr.gunplan.app/.MainActivity'),flush=True)
print(shell('input keyevent KEYCODE_HOME'),flush=True)
result=shell('am instrument -w tr.gunplan.app.test/tr.gunplan.app.PlannerInstrumentation');print(result,flush=True);assert 'PASS native storage' in result
print(shell('am kill tr.gunplan.app; pidof tr.gunplan.app'),flush=True)
for i in range(12):
 time.sleep(10)
 result=shell('dumpsys notification --noredact')
 if 'GünPlan arka plan testi' in result:
  open(ARTIFACTS/'emulator-notifications.txt','w').write(result);break
else:raise AssertionError('Real scheduled notification did not arrive')
print('PASS real scheduled Android notification with app background process killed',flush=True)
errors=shell('logcat -d -s AndroidRuntime:E GunPlan:E');print(errors,flush=True);assert 'FATAL EXCEPTION' not in errors
print(shell('am start -n tr.gunplan.app/.MainActivity'),flush=True)
time.sleep(5)
shell('screencap -p /data/local/tmp/gunplan-screen.png');d.pull('/data/local/tmp/gunplan-screen.png',str(ARTIFACTS/'emulator-screen.png'),read_timeout_s=120)
print('PASS actual app launch + emulator screenshot',flush=True)
