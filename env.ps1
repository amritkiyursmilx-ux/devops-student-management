# Load Environment Variables for Student Management Project
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\Git\cmd;C:\Program Files\Docker\Docker\resources\bin;" + $env:PATH

Write-Host "✅ Environment Loaded:" -ForegroundColor Green
Write-Host "  JAVA_HOME: $env:JAVA_HOME"
Write-Host "  Java:      $((java -version 2>&1)[0])"
Write-Host "  Git:       $(git --version)"
Write-Host "  Docker:    $(docker --version)"
