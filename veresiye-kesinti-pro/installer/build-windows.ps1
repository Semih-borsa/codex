$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
dotnet publish src/Desktop/Desktop.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=false -o artifacts/win-x64
if ($LASTEXITCODE -ne 0) { throw 'Windows publish başarısız' }
$iscc = Get-Command ISCC.exe -ErrorAction SilentlyContinue
if (!$iscc) { throw 'Inno Setup 6 kurun ve ISCC.exe dosyasını PATH içine ekleyin. Publish çıktısı artifacts/win-x64 içinde.' }
& $iscc.Source installer/Veresiye.iss
if ($LASTEXITCODE -ne 0) { throw 'Setup üretimi başarısız' }
