# Bootstrap Jenkins CI/CD for this project
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File jenkins/bootstrap.ps1
#
# This brings up the Jenkins container from the repo-local `jenkins/`
# directory, waits for it to become healthy, and prints the initial admin
# password plus the next steps to wire the job to GitHub.

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location -LiteralPath $scriptDir

try {
    Write-Host "Starting Jenkins CI/CD container..."
    docker compose up -d | Out-Null

    Write-Host "Waiting for Jenkins to become healthy (this can take a minute)..."
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:9090/login" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Host "`nJenkins is up at http://localhost:9090"
                break
            }
        } catch {
            Write-Host -NoNewline "."
            Start-Sleep -Seconds 2
        }
    }

    Write-Host "`nInitial admin password:"
    docker exec jenkins-server cat /var/jenkins_home/secrets/initialAdminPassword 2>$null

    Write-Host @"
Next steps:
  1. Open http://localhost:9090 and sign in with username "admin" and the password above.
  2. Create a new Pipeline job named "student-management-pipeline".
  3. Under "Pipeline script from SCM", set:
       SCM: Git
       Repository URL: https://github.com/amritkiyursmilx-ux/devops-student-management.git
       Branches to build: */main
       Script Path: Jenkinsfile
  4. Click Save, then Build Now.

Stop Jenkins later with: jenkins/stop-jenkins.bat
"@
} finally {
    Pop-Location
}
