param(
    [string]$InstallDir = (Join-Path $env:LOCALAPPDATA "neo4j-community-5.6.0"),
    [string]$Neo4jUri = "bolt://localhost:7687",
    [string]$Neo4jUser = "neo4j",
    [string]$Neo4jPass = "00000000",
    [switch]$UseLocalInstance,
    [string]$LocalPassword = ""
)

$neo4jVersion = "5.6.0"
$downloadUrl = "https://dist.neo4j.org/neo4j-community-$($neo4jVersion)-windows.zip"
$zipFile = "neo4j-community-$($neo4jVersion)-windows.zip"
$zipFilePath = Join-Path $env:TEMP $zipFile

# Configure proxy if available
$proxy = $null
if (-not [string]::IsNullOrEmpty($ProxyHost) -and $ProxyPort -ne 0) {
    $proxy = New-Object System.Net.WebProxy($ProxyHost + ":" + $ProxyPort)
    if (-not [string]::IsNullOrEmpty($ProxyUser)) {
        $proxy.Credentials = New-Object System.Net.NetworkCredential($ProxyUser, $ProxyPass)
    }
} elseif (-not [string]::IsNullOrEmpty($env:HTTP_PROXY)) {
    $proxyUri = New-Object System.Uri($env:HTTP_PROXY)
    $ProxyHost = $proxyUri.Host
    $ProxyPort = $proxyUri.Port
    $proxy = New-Object System.Net.WebProxy($ProxyHost + ":" + $ProxyPort)
    
    if (-not [string]::IsNullOrEmpty($env:HTTP_PROXY_USER) -and -not [string]::IsNullOrEmpty($env:HTTP_PROXY_PASS)) {
        $proxy.Credentials = New-Object System.Net.NetworkCredential($env:HTTP_PROXY_USER, $env:HTTP_PROXY_PASS)
    }
} elseif (-not [string]::IsNullOrEmpty($env:HTTPS_PROXY)) {
    $proxyUri = New-Object System.Uri($env:HTTPS_PROXY)
    $ProxyHost = $proxyUri.Host
    $ProxyPort = $proxyUri.Port
    $proxy = New-Object System.Net.WebProxy($ProxyHost + ":" + $ProxyPort)
    
    if (-not [string]::IsNullOrEmpty($env:HTTPS_PROXY_USER) -and -not [string]::IsNullOrEmpty($env:HTTPS_PROXY_PASS)) {
        $proxy.Credentials = New-Object System.Net.NetworkCredential($env:HTTPS_PROXY_USER, $env:HTTPS_PROXY_PASS)
    }
}

# Download Neo4j only if not already present
if (-not (Test-Path $zipFilePath)) {
    Write-Host "Downloading Neo4j Community Edition v$($neo4jVersion) to $($zipFilePath)..."
    if ($proxy) {
        Write-Host "Using proxy: $($proxy.Address)"
        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFilePath -Proxy $($proxy.Address.AbsoluteUri) -UseBasicParsing
    } else {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFilePath -UseBasicParsing
    }
} else {
    Write-Host "Using existing zip file at $($zipFilePath)."
}

# Extract the archive
Write-Host "Extracting to $($InstallDir)..."
Expand-Archive -Path $zipFilePath -DestinationPath $InstallDir -Force

# Note: Do not remove the zip file to allow reuse
# Remove-Item $zipFilePath

Write-Host "Neo4j Community Edition v$($neo4jVersion) installed successfully in $($InstallDir)"
Write-Host "You can find the executables in $($InstallDir)\bin"
