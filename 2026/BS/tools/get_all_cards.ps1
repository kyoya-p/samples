$colorMap = @{
    "R (Red)"    = "R"
    "P (Purple)" = "P"
    "G (Green)"  = "G"
    "W (White)"  = "W"
    "Y (Yellow)" = "Y"
    "B (Blue)"   = "B"
}
$colorKeys = "R (Red)", "P (Purple)", "G (Green)", "W (White)", "Y (Yellow)", "B (Blue)"

$costRanges = @(
    @{ Label = "Cost 0-2"; Range = "0-2" },
    @{ Label = "Cost 3"; Range = "3" },
    @{ Label = "Cost 4";   Range = "4" },
    @{ Label = "Cost 5-6"; Range = "5-6" },
    @{ Label = "Cost 7-30"; Range = "7-30" }
)

# Check for amper.bat
if (-not (Test-Path ".\amper.bat")) {
    Write-Error "amper.bat not found in current directory."
    exit 1
}

foreach ($range in $costRanges) {
    foreach ($key in $colorKeys) {
        $colorVal = $colorMap[$key]
        $costVal = $range.Range

        Write-Host -NoNewline "[$($range.Label)] [$key] : "

        $output = .\amper.bat run -m jvm-cli -- -c $costVal -a $colorVal -d .bscards 2>&1

        $countLine = $output | Where-Object { $_ -match "Found (\d+) cards" }
        $count = 0
        if ($null -ne $countLine -and $countLine -match "Found (\d+) cards") {
            $count = $Matches[1]
        }

        Write-Host "$count cards"
    }
}
