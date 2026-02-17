param($Path = ".")

Invoke-WebRequest "https://github.com/jeremylong/DependencyCheck/releases/download/v12.1.0/dependency-check-12.1.0-release.zip" -OutFile "dchk.zip"
tar -xf "dchk.zip" -C "dchk"


& "dchk/dependency-check/bin/dependency-check.bat" --project "DependencyCheck" --scan "$Path" --format "HTML" --out "." --nvdApiKey $env:NVD_API_KEY --exclude "dchk/**" --disableOssIndex
