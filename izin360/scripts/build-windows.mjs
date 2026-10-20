import { readFile, writeFile, mkdir, rm, cp, copyFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { resolve, join } from "node:path";
import { execFileSync } from "node:child_process";
const root = process.cwd(),
  pin = JSON.parse(await readFile("scripts/runtime.json", "utf8"));
const cache = resolve(process.env.IZIN360_BUILD_CACHE || ".package-cache"),
  stage = join(cache, "Izin360-Windows-x64");
await mkdir(cache, { recursive: true });
const zip = process.env.IZIN360_NODE_ZIP || join(cache, pin.filename);
let bytes;
try {
  bytes = await readFile(zip);
} catch {
  const response = await fetch(pin.url);
  if (!response.ok) throw Error("Node.js runtime download failed");
  bytes = Buffer.from(await response.arrayBuffer());
  await writeFile(zip, bytes);
}
if (createHash("sha256").update(bytes).digest("hex") !== pin.sha256)
  throw Error("Node.js SHA-256 verification failed. Package was not used.");
await rm(stage, { recursive: true, force: true });
await mkdir(stage, { recursive: true });
const unpack = join(cache, "node-unpacked");
await rm(unpack, { recursive: true, force: true });
if (process.platform === "win32")
  execFileSync("powershell.exe", [
    "-NoProfile",
    "-NonInteractive",
    "-Command",
    `Expand-Archive -LiteralPath '${zip.replaceAll("'", "''")}' -DestinationPath '${unpack.replaceAll("'", "''")}'`,
  ]);
else execFileSync("unzip", ["-q", zip, "-d", unpack]);
await mkdir(join(stage, "runtime"), { recursive: true });
const nodeFolder = join(unpack, `node-${pin.version}-win-x64`);
await copyFile(
  join(nodeFolder, "node.exe"),
  join(stage, "runtime", "node.exe"),
);
await copyFile(
  join(nodeFolder, "LICENSE"),
  join(stage, "runtime", "NODE-LICENSE.txt"),
);
const go = process.env.GO_EXE || "go";
const version = execFileSync(go, ["version"], { encoding: "utf8" });
if (!version.startsWith("go version "))
  throw Error(
    "A Go compiler is required (the Go board game executable is not a compiler). Set GO_EXE to the Go compiler path.",
  );
execFileSync(
  go,
  [
    "build",
    "-trimpath",
    "-ldflags=-s -w -H=windowsgui",
    "-o",
    join(stage, "Izin360.exe"),
    ".",
  ],
  {
    cwd: join(root, "windows"),
    env: {
      ...process.env,
      GOOS: "windows",
      GOARCH: "amd64",
      GOTELEMETRY: "off",
    },
    stdio: "inherit",
  },
);
await copyFile(join(stage, "Izin360.exe"), join(stage, "Izin360-Durdur.exe"));
await mkdir(join(stage, "app", "src"), { recursive: true });
await cp(join(root, "server"), join(stage, "app", "server"), {
  recursive: true,
});
await cp(join(root, "dist"), join(stage, "app", "dist"), { recursive: true });
await copyFile(
  join(root, "src", "model.mjs"),
  join(stage, "app", "src", "model.mjs"),
);
await copyFile(join(root, "package.json"), join(stage, "app", "package.json"));
await copyFile(join(root, "README.md"), join(stage, "KULLANIM.md"));
await copyFile(join(root, "THIRD-PARTY-NOTICES.txt"), join(stage, "THIRD-PARTY-NOTICES.txt"));
await writeFile(
  join(stage, "BASLAT.cmd"),
  '@echo off\r\nstart "" "%~dp0Izin360.exe"\r\n',
);
await writeFile(
  join(stage, "DURDUR.cmd"),
  '@echo off\r\nstart "" "%~dp0Izin360-Durdur.exe"\r\n',
);
await writeFile(
  join(stage, "ILK-OKU.txt"),
  "\uFEFFİzin360 — Windows 10/11 x64\r\n\r\n1. ZIP dosyasının TAMAMINI ayıklayın.\r\n2. Izin360.exe dosyasına çift tıklayın. Edge uygulama penceresi veya varsayılan tarayıcı açılır.\r\n3. Telefonu aynı Wi-Fi ağına bağlayın. Windows uygulamasında Ayarlar → Windows Bağlantı Bilgileri bölümünü açın. APK ayarlarına adres ve anahtarı girin.\r\n4. Windows güvenlik duvarı sorarsa yalnızca ÖZEL ağ erişimine izin verin.\r\n\r\nNode.js ayrıca kurulmaz; runtime klasörünü silmeyin. Kayıtlar %APPDATA%\\Izin360 içinde tutulur. Uygulama penceresini kapatmak arka plandaki eşitlemeyi durdurmaz. Kapatmak için Izin360-Durdur.exe kullanın. Program imzasız bir ilk sürümdür; Windows güvenlik uyarısı gösterebilir.\r\n\r\nYedek almadan verileri veya tarayıcı profilini silmeyin. ZIP içinde örnek personel verisi yoktur.\r\n",
);
const output = resolve(
  process.env.IZIN360_WINDOWS_OUT || "../downloads/Izin360-Windows-x64.zip",
);
await mkdir(resolve(output, ".."), { recursive: true });
if (process.platform === "win32")
  execFileSync("powershell.exe", [
    "-NoProfile",
    "-NonInteractive",
    "-Command",
    `Compress-Archive -Path '${stage.replaceAll("'", "''")}' -DestinationPath '${output.replaceAll("'", "''")}' -Force`,
  ]);
else
  execFileSync("python3", [
    "-c",
    "import pathlib,zipfile,sys\nr=pathlib.Path(sys.argv[1])\nwith zipfile.ZipFile(sys.argv[2],'w',zipfile.ZIP_DEFLATED) as z:\n for p in r.rglob('*'):\n  if p.is_file():z.write(p,r.name+'/'+str(p.relative_to(r)))",
    stage,
    output,
  ]);
console.log(
  `Windows x64 package created (${pin.version}); native Windows execution still requires a Windows machine.`,
);
