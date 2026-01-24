Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

$targetPid = 9680 # Windows Terminal PID

try {
    $root = [System.Windows.Automation.AutomationElement]::RootElement
    $condition = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ProcessIdProperty, $targetPid)
    $appElement = $root.FindFirst([System.Windows.Automation.TreeScope]::Children, $condition)

    if ($appElement) {
        Write-Host "Found Window: $($appElement.Current.Name)"
        
        # 子要素を探索 (タブやペインを探す)
        $children = $appElement.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
        
        foreach ($child in $children) {
            try {
                $rect = $child.Current.BoundingRectangle
                if ($rect.Width -gt 0 -and $rect.Height -gt 0) {
                    Write-Host "Element: '$($child.Current.Name)' Class: $($child.Current.ClassName) Rect: [$($rect.X), $($rect.Y), $($rect.Width), $($rect.Height)]"
                }
            } catch {}
        }
    } else {
        Write-Error "Window for PID $targetPid not found."
    }
} catch {
    Write-Error $_
}
