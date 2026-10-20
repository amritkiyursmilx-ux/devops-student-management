# Jenkins CI/CD server

This directory contains everything needed to run the Jenkins CI/CD server that
builds, tests, and deploys this project:

- `Dockerfile`   — Jenkins LTS on JDK 21 with Maven, Git, and the Docker CLI.
- `docker-compose.yml` — Mounts the Docker socket so Jenkins can build and run
  containers, plus a named volume (`jenkins_home`) for Jenkins' own data.
- `bootstrap.ps1`  — Starts Jenkins, waits for health, prints the admin password,
  and lists the job-creation steps.
- `start-jenkins.bat` / `stop-jenkins.bat` — Convenience wrappers.

## Quick start

```powershell
cd jenkins
powershell -ExecutionPolicy Bypass -File bootstrap.ps1
```

Jenkins will be available at `http://localhost:9090`.

## Pipeline configuration

Create a new Pipeline job named `student-management-pipeline` with:

| Setting | Value |
|---|---|
| Definition | Pipeline script from SCM |
| SCM | Git |
| Repository URL | `https://github.com/amritkiyursmilx-ux/devops-student-management.git` |
| Branches to build | `*/main` |
| Script Path | `Jenkinsfile` |

The job uses the 8-stage declarative pipeline defined in the root `Jenkinsfile`:
`Checkout → Verify Environment → Compile Application → Execute Tests → Package JAR → Build Docker Image → Deploy Container → Verify Application Health`.

The pipeline polls GitHub every 5 minutes (`pollSCM('H/5 * * * *')`) so new commits on `main` are picked up automatically.
