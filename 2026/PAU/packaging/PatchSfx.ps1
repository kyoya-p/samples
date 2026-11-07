# 7-Zip 9.20 Extra (公式) からインストーラ型 SFX (7zSD.sfx) を取得し、
# 昇格を要求させないマニフェスト (asInvoker) を埋め込んで出力する
param([Parameter(Mandatory)][string]$Out)
$ErrorActionPreference = 'Stop'
$work = Join-Path $env:TEMP ('PatchSfx-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $work | Out-Null
try {
    Invoke-WebRequest https://www.7-zip.org/a/7z920_extra.7z -OutFile "$work\extra.7z"
    $sha = (Get-FileHash "$work\extra.7z" -Algorithm SHA256).Hash
    if ($sha -ne 'D65A1D1A050E4FD7912C18468954F64DB824A1E1B621235A153D010BEAE14757') { throw "SHA256 mismatch: $sha" }
    & 7za x -y "-o$work" "$work\extra.7z" 7zSD.sfx | Out-Null
    Add-Type -TypeDefinition @"
using System; using System.Runtime.InteropServices;
public static class Res {
  [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)] public static extern IntPtr BeginUpdateResource(string f, bool d);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool UpdateResource(IntPtr h, IntPtr type, IntPtr name, ushort lang, byte[] data, uint cb);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool EndUpdateResource(IntPtr h, bool discard);
}
"@
    $manifest = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0"><trustInfo xmlns="urn:schemas-microsoft-com:asm.v3"><security><requestedPrivileges><requestedExecutionLevel level="asInvoker" uiAccess="false"/></requestedPrivileges></security></trustInfo></assembly>'
    $bytes = [Text.Encoding]::UTF8.GetBytes($manifest)
    $h = [Res]::BeginUpdateResource("$work\7zSD.sfx", $false)
    if (-not [Res]::UpdateResource($h, [IntPtr]24, [IntPtr]1, 0, $bytes, [uint32]$bytes.Length)) { throw 'UpdateResource failed' }
    if (-not [Res]::EndUpdateResource($h, $false)) { throw 'EndUpdateResource failed' }
    New-Item -ItemType Directory -Force (Split-Path -Parent $Out) | Out-Null
    Copy-Item "$work\7zSD.sfx" $Out -Force
} finally {
    Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
