param(
    [string]$Neo4jUri = "bolt://10.36.104.102:7474",
    [string]$Neo4jUser = "neo4j",
    [string]$Neo4jPass = "Soft2cream",
    [switch]$UseLocalInstance,
    [string]$LocalPassword = ""
)

if ($UseLocalInstance) {
    $Neo4jUri = "bolt://localhost:7687"
    $Neo4jUser = "neo4j"
    $Neo4jPass = $LocalPassword
}

# Set environment variables for the current session
$env:NEO4J_URI = $Neo4jUri
$env:NEO4J_USERNAME = $Neo4jUser
$env:NEO4J_PASSWORD = $Neo4jPass

Write-Host "Neo4j connection environment variables set for the current session:"
Write-Host "  NEO4J_URI = $($env:NEO4J_URI)"
Write-Host "  NEO4J_USERNAME = $($env:NEO4J_USERNAME)"
Write-Host "  NEO4J_PASSWORD = $($env:NEO4J_PASSWORD -replace '.','*')" # Mask password for display

Write-Host "These settings are temporary and only apply to the current PowerShell session."
