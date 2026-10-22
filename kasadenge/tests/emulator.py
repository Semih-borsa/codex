from pathlib import Path
from adb_shell.adb_device import AdbDeviceTcp
import time
ROOT=Path(__file__).resolve().parents[1]
d=AdbDeviceTcp('127.0.0.1',5555,default_transport_timeout_s=180);d.connect()
for source,target in [(ROOT/'app/build/outputs/apk/debug/app-debug.apk','/data/local/tmp/kasadenge.apk'),(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk','/data/local/tmp/kasadenge-test.apk')]:
 d.push(str(source),target,read_timeout_s=180);result=d.shell('pm install -r '+target,read_timeout_s=180,timeout_s=240);print(result,flush=True);assert 'Success' in result
print(d.shell('am instrument -w tr.kasadenge.app.test/tr.kasadenge.app.CashInstrumentation',read_timeout_s=180,timeout_s=240),flush=True)
print(d.shell('am start -n tr.kasadenge.app/.MainActivity',read_timeout_s=90,timeout_s=120),flush=True)
time.sleep(8)
print(d.shell('logcat -d | grep -E "CONSOLE|Process: tr.kasadenge|ANR in tr.kasadenge" | tail -8',read_timeout_s=90,timeout_s=120),flush=True)
d.shell('screencap -p /data/local/tmp/kasadenge.png',read_timeout_s=90,timeout_s=120);d.pull('/data/local/tmp/kasadenge.png',str(ROOT/'tests/emulator.png'),read_timeout_s=90)
