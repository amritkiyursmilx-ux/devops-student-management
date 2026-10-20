@echo off
echo Stopping Jenkins CI/CD Container...
cd /d "%~dp0"
docker compose down
echo Jenkins container stopped.
pause
