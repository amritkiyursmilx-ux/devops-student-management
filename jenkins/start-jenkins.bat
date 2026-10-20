@echo off
echo Starting Jenkins CI/CD Container on port 9090...
cd /d "%~dp0"
docker compose up -d
echo Jenkins is running at: http://localhost:9090
pause
