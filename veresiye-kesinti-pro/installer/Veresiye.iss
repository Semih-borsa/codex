[Setup]
AppId={{BC78FD51-1981-4EE8-9492-B4326FC6E483}
AppName=Veresiye Kesinti Pro
AppVersion=0.1.0
DefaultDirName={localappdata}\Programs\VeresiyeKesintiPro
DefaultGroupName=Veresiye Kesinti Pro
PrivilegesRequired=lowest
OutputDir=..\artifacts\installer
OutputBaseFilename=VeresiyeKesintiPro-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
[Files]
Source: "..\artifacts\win-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{group}\Veresiye Kesinti Pro"; Filename: "{app}\VeresiyeKesintiPro.exe"
[Run]
Filename: "{app}\VeresiyeKesintiPro.exe"; Description: "Uygulamayı aç"; Flags: nowait postinstall skipifsilent
