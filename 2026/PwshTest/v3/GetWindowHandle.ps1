param (
    [Parameter(Mandatory=$true)]
    [int]$Id
)

function Get-ParentWindowHandle {
    param (
        [int]$StartPid
    )

    $currentId = $StartPid
    $depth = 0
    $maxDepth = 10 # Prevent infinite loops

    Write-Host "Searching for window handle starting from PID: $StartPid"

    while ($depth -lt $maxDepth) {
        $proc = Get-Process -Id $currentId -ErrorAction SilentlyContinue
        
        if ($proc) {
            # Check if current process has a window
            if ($proc.MainWindowHandle -ne 0) {
                Write-Host "Found Window Owner at depth $depth" -ForegroundColor Green
                return [PSCustomObject]@{
                    Handle       = $proc.MainWindowHandle
                    ProcessName  = $proc.ProcessName
                    Id           = $proc.Id
                    WindowTitle  = $proc.MainWindowTitle
                    OriginalPid  = $StartPid
                }
            }
        } else {
            Write-Warning "Process $currentId no longer exists."
            break
        }

        # Get parent PID using CIM/WMI
        try {
            $cimProc = Get-CimInstance Win32_Process -Filter "ProcessId = $currentId" -ErrorAction Stop
            $parentId = $cimProc.ParentProcessId
            
            if (-not $parentId) { 
                Write-Warning "No parent process found for PID $currentId."
                break 
            }
            
            Write-Host "  -> Parent: $($cimProc.Name) (PID: $parentId)"
            $currentId = $parentId
            $depth++
        } catch {
            Write-Warning "Failed to get parent process info: $_"
            break
        }
    }

    return $null
}

# --- Main ---

$result = Get-ParentWindowHandle -StartPid $Id

if ($result) {
    # Display summary to host (doesn't pollute pipeline)
    Write-Host "`n--- Result Summary ---" -ForegroundColor Cyan
    Write-Host "Target Handle: $($result.Handle)"
    Write-Host "Owner Process: $($result.ProcessName) (PID: $($result.Id))"
    Write-Host "Window Title:  $($result.WindowTitle)"
    
    # Return the raw object for pipeline usage
    return $result
} else {
    Write-Error "Could not find any window handle in the process tree of PID $Id."
    exit 1
}
