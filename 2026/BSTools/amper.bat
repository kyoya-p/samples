@echo off

@rem
@rem Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@rem

setlocal
set amper_version=0.9.2
set amper_sha256=33304bb301d0c5276ad4aa718ce8a10486fda4be45179779497d2036666bb16f

if not defined AMPER_DOWNLOAD_ROOT set AMPER_DOWNLOAD_ROOT=https://packages.jetbrains.team/maven/p/amper/amper
if not defined AMPER_JRE_DOWNLOAD_ROOT set AMPER_JRE_DOWNLOAD_ROOT=https:/
if not defined AMPER_BOOTSTRAP_CACHE_DIR set AMPER_BOOTSTRAP_CACHE_DIR=%LOCALAPPDATA%\JetBrains\Amper
if [%AMPER_BOOTSTRAP_CACHE_DIR:~-1%] EQU [\] set AMPER_BOOTSTRAP_CACHE_DIR=%AMPER_BOOTSTRAP_CACHE_DIR:~0,-1%

goto :after_function_declarations

:download_and_extract
setlocal
set moniker=%~1
set url=%~2
set target_dir=%~3
set sha=%~4
set sha_size=%~5
set show_banner_on_cache_miss=%~6

set flag_file=%target_dir%\.flag
if exist "%flag_file%" (
    set /p current_flag=<"%flag_file%"
    if "%current_flag%" == "%sha%" exit /b
)

set download_and_extract_ps1= ^
Set-StrictMode -Version 3.0; ^
$ErrorActionPreference = 'Stop'; ^
 ^
$createdNew = $false; ^
$lock = New-Object System.Threading.Mutex($true, ('Global\amper-bootstrap.' + '%target_dir%'.GetHashCode().ToString()), [ref]$createdNew); ^
if (-not $createdNew) { ^
    Write-Host 'Another Amper instance is bootstrapping. Waiting for our turn...'; ^
    [void]$lock.WaitOne(); ^
} ^
 ^
try { ^
    if ((Get-Content '%flag_file%' -ErrorAction Ignore) -ne '%sha%') { ^
        if (('%show_banner_on_cache_miss%' -eq 'true') -and [string]::IsNullOrEmpty('%AMPER_NO_WELCOME_BANNER%')) { ^
            Write-Host '*** Welcome to Amper v.%amper_version%! ***'; ^
            Write-Host ''; ^
            Write-Host 'This is the first run of this version, so we need to download the actual Amper distribution.'; ^
            Write-Host 'Please give us a few seconds now, subsequent runs will be faster.'; ^
            Write-Host ''; ^
        } ^
        $temp_file = '%AMPER_BOOTSTRAP_CACHE_DIR%\' + [System.IO.Path]::GetRandomFileName(); ^
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
        Write-Host 'Downloading %moniker%...'; ^
        [void](New-Item '%AMPER_BOOTSTRAP_CACHE_DIR%' -ItemType Directory -Force); ^
        if (Get-Command curl.exe -errorAction SilentlyContinue) { ^
            curl.exe -L --silent --show-error --fail --output $temp_file '%url%'; ^
        } else { ^
            (New-Object Net.WebClient).DownloadFile('%url%', $temp_file); ^
        } ^
 ^
        $actualSha = (Get-FileHash -Algorithm SHA%sha_size% -Path $temp_file).Hash.ToString(); ^
        if ($actualSha -ne '%sha%') { ^
            $writeErr = if ($Host.Name -eq 'ConsoleHost') { [Console]::Error.WriteLine } else { $host.ui.WriteErrorLine } ^
            $writeErr.Invoke(\"ERROR: Checksum mismatch for $temp_file (downloaded from %url%): expected checksum %sha% but got $actualSha\"); ^
            exit 1; ^
        } ^
 ^
        if (Test-Path '%target_dir%') { ^
            Remove-Item '%target_dir%' -Recurse; ^
        } ^
        if ($temp_file -like '*.zip') { ^
            Add-Type -A 'System.IO.Compression.FileSystem'; ^
            [IO.Compression.ZipFile]::ExtractToDirectory($temp_file, '%target_dir%'); ^
        } else { ^
            [void](New-Item '%target_dir%' -ItemType Directory -Force); ^
            tar -xzf $temp_file -C '%target_dir%'; ^
        } ^
        Remove-Item $temp_file; ^
 ^
        Set-Content '%flag_file%' -Value '%sha%'; ^
        Write-Host 'Download complete.'; ^
        Write-Host ''; ^
    } ^
} ^
finally { ^
    $lock.ReleaseMutex(); ^
}

set PSModulePath=
set powershell=%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe
"%powershell%" -NonInteractive -NoProfile -NoLogo -Command %download_and_extract_ps1%
if errorlevel 1 exit /b 1
exit /b 0

:fail
echo ERROR: Amper bootstrap failed, see errors above
exit /b 1

:after_function_declarations

set amper_url=%AMPER_DOWNLOAD_ROOT%/org/jetbrains/amper/amper-cli/%amper_version%/amper-cli-%amper_version%-dist.tgz
set amper_target_dir=%AMPER_BOOTSTRAP_CACHE_DIR%\amper-cli-%amper_version%
call :download_and_extract "Amper distribution v%amper_version%" "%amper_url%" "%amper_target_dir%" "%amper_sha256%" "256" "true"
if errorlevel 1 goto fail

if defined AMPER_JAVA_HOME (
    if not exist "%AMPER_JAVA_HOME%\bin\java.exe" (
      echo Invalid AMPER_JAVA_HOME provided: cannot find %AMPER_JAVA_HOME%\bin\java.exe
      goto fail
    )
    if "%AMPER_JAVA_HOME%"=="%AMPER_JAVA_HOME:jbr-21=%" (
        set effective_amper_java_home=%AMPER_JAVA_HOME%
        goto jre_provisioned
    )
)

set zulu_version=25.28.85
set java_version=25.0.0
if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
    set pkg_type=jdk
    set jre_arch=aarch64
    set jre_sha256=f5f6d8a913695649e8e2607fe0dc79c81953b2583013ac1fb977c63cb4935bfb
) else if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
    set pkg_type=jre
    set jre_arch=x64
    set jre_sha256=d3c5db7864e6412ce3971c0b065def64942d7b0f3d02581f7f0472cac21fbba9
) else (
    echo Unknown Windows architecture %PROCESSOR_ARCHITECTURE% >&2
    goto fail
)

set jre_url=%AMPER_JRE_DOWNLOAD_ROOT%/cdn.azul.com/zulu/bin/zulu%zulu_version%-ca-%pkg_type%%java_version%-win_%jre_arch%.zip
set jre_target_dir=%AMPER_BOOTSTRAP_CACHE_DIR%\zulu%zulu_version%-ca-%pkg_type%%java_version%-win_%jre_arch%
call :download_and_extract "Amper runtime v%zulu_version%" "%jre_url%" "%jre_target_dir%" "%jre_sha256%" "256" "false"
if errorlevel 1 goto fail

set effective_amper_java_home=
for /d %%d in ("%jre_target_dir%\*") do if exist "%%d\bin\java.exe" set effective_amper_java_home=%%d
if not exist "%effective_amper_java_home%\bin\java.exe" (
  echo Unable to find java.exe under %jre_target_dir%
  goto fail
)
:jre_provisioned

"%effective_amper_java_home%\bin\java.exe" ^
  @"%amper_target_dir%\amper.args" ^
  "-Damper.wrapper.dist.sha256=%amper_sha256%" ^
  "-Damper.dist.path=%amper_target_dir%" ^
  "-Damper.wrapper.path=%~f0" ^
  %AMPER_JAVA_OPTIONS% ^
  -cp "%amper_target_dir%\lib\*" ^
  org.jetbrains.amper.cli.MainKt ^
  %*
exit /B %ERRORLEVEL%
