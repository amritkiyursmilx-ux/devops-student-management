# ---------------------------------------------------------------------------
# Load environment variables for the Student Management project.
#
# JDK 21 is located in this order:
#   1. An installed Adoptium/Temurin "jdk-21*" directory.
#   2. A Windows registry key under HKLM:\SOFTWARE\JavaSoft\JDK whose version
#      segment is 21 (for example ...\JavaSoft\JDK\21).
#
# Every candidate is a JDK 21 by construction, so a JDK 8/11/17 install can
# never be selected and break the build with "invalid target release: 21".
#
# env.bat applies exactly the same rules.
# ---------------------------------------------------------------------------

$javaHome = $null

# 1. Installed Adoptium/Temurin JDK 21.
$adoptium = 'C:\Program Files\Eclipse Adoptium'
if (Test-Path -LiteralPath $adoptium) {
    $candidate = Get-ChildItem -LiteralPath $adoptium -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
                 Sort-Object Name -Descending |
                 Select-Object -First 1
    if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate.FullName 'bin\java.exe'))) {
        $javaHome = $candidate.FullName
    }
}

# 2. Registered JDK 21 - the registry key name carries the version.
if (-not $javaHome) {
    try {
        Get-ChildItem -LiteralPath 'HKLM:\SOFTWARE\JavaSoft\JDK' -ErrorAction Stop | ForEach-Object {
            if ($javaHome) { return }
            if ($_.PSChildName -notmatch '^21(\.|$)') { return }
            $value = (Get-ItemProperty -LiteralPath $_.PSPath -Name JavaHome -ErrorAction SilentlyContinue).JavaHome
            if ($value -and (Test-Path -LiteralPath (Join-Path $value.TrimEnd('\') 'bin\java.exe'))) {
                $javaHome = $value.TrimEnd('\')
            }
        }
    } catch { }
}

if (-not $javaHome) {
    Write-Host 'ERROR: Could not locate a JDK 21. Set JAVA_HOME manually.' -ForegroundColor Red
    return
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\Git\cmd;C:\Program Files\Docker\Docker\resources\bin;" + $env:PATH

Write-Host 'Environment Loaded:' -ForegroundColor Green
Write-Host "  JAVA_HOME: $env:JAVA_HOME"
Write-Host "  Java:      $((java -version 2>&1)[0])"
Write-Host "  Git:       $(git --version)"
Write-Host "  Docker:    $(docker version --format '{{.Server.Version}}' 2>&1)"
