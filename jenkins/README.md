# Jenkins CI/CD server

This directory contains everything needed to run the Jenkins CI/CD server that
builds, tests, and deploys this project:

- `Dockerfile`   — Jenkins LTS on JDK 21 with Maven, Git, and the Docker CLI.
- `docker-compose.yml` — Mounts the Docker socket so Jenkins can build and run
  containers, plus a named volume (`jenkins_home`) for Jenkins' own data.
- `entrypoint.sh` — Repairs the Docker socket ownership at start-up and drops
  back to the unprivileged `jenkins` user (see below).
- `bootstrap.ps1`  — Starts Jenkins, waits for health, prints the admin password,
  and lists the job-creation steps.
- `start-jenkins.bat` / `stop-jenkins.bat` — Convenience wrappers.

## Why there is an entrypoint script

Two Docker Desktop quirks break the pipeline in ways that are hard to see from
the build log, so the entrypoint repairs both on every start:

1. **Docker socket ownership.** Docker Desktop recreates `/var/run/docker.sock`
   as `root:root` whenever the WSL2 engine restarts. The `docker` group created
   by the image then has no access to it and every run fails with
   `permission denied ... /var/run/docker.sock`. The entrypoint restores the
   group ownership.

2. **`HOME` must point at the jenkins user.** The base image inherits
   `HOME=/root`. Because the server is started as the unprivileged `jenkins`
   user, `HOME` is reset to `/var/jenkins_home` - otherwise the Docker CLI
   looks for its configuration and the buildx plugin under `/root/.docker`,
   which the jenkins user cannot read, and `docker build` fails with
   `unknown shorthand flag: 't' in -t`.

The entrypoint runs as root only long enough to apply those two fixes, then
drops to the `jenkins` user with `setpriv`, so the server itself never runs as
root.

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
