# ---------------------------------------------------------------------------
# Load environment variables for the Student Management project.
# JDK 21 is discovered from the Windows registry (then from the Adoptium install
# directory) instead of being hardcoded to one patched version.
# ---------------------------------------------------------------------------

$javaHome = $null

# 1. Windows registry (any registered JDK)
try {
    $reg = Get-ItemProperty -Path 'HKLM:\SOFTWARE\JavaSoft\JDK\*' -Name JavaHome -ErrorAction Stop
    $javaHome = ($reg | Where-Object { $_.JavaHome } | Select-Object -First 1).JavaHome
} catch { }

# 2. Highest-numbered Adoptium JDK 21 install
if (-not $javaHome) {
    $adoptium = 'C:\Program Files\Eclipse Adoptium'
    if (Test-Path -LiteralPath $adoptium) {
        $candidate = Get-ChildItem -LiteralPath $adoptium -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
                     Sort-Object Name -Descending |
                     Select-Object -First 1
        if ($candidate) { $javaHome = $candidate.FullName }
    }
}

if (-not $javaHome) {
    Write-Host 'ERROR: Could not locate a JDK. Set JAVA_HOME manually.' -ForegroundColor Red
    return
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\Git\cmd;C:\Program Files\Docker\Docker\resources\bin;" + $env:PATH

Write-Host 'Environment Loaded:' -ForegroundColor Green
Write-Host "  JAVA_HOME: $env:JAVA_HOME"
Write-Host "  Java:      $((java -version 2>&1)[0])"
Write-Host "  Git:       $(git --version)"
Write-Host "  Docker:    $(docker version --format '{{.Server.Version}}' 2>&1)"
