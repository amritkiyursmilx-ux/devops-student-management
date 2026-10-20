pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(
            name: 'DEPLOY',
            defaultValue: true,
            description: 'Deploy the updated application container to port 8081 after tests pass.'
        )
    }

    environment {
        IMAGE_NAME     = 'student-management'
        CONTAINER_NAME = 'student-management-app'
        HOST_PORT      = '8081'
        CONTAINER_PORT = '8080'
        // Used only when no existing container is mounted yet. The Deploy stage
        // normally reuses whatever volume the current container already has, so
        // this default never orphans existing data.
        DATA_VOLUME    = 'student-management_student-data'
        // Hostname used by the health-check stage.
        //   - Jenkins on the Windows host          -> localhost
        //   - Jenkins inside a container (this setup) -> host.docker.internal
        // A containerised Jenkins has its own "localhost", so the published port
        // on the Windows host is reachable only through host.docker.internal.
        HEALTH_HOST    = 'host.docker.internal'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '=== Stage 1: Checkout Source Code from GitHub ==='
                checkout scm
                script {
                    if (isUnix()) {
                        env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    } else {
                        env.GIT_COMMIT_SHORT = bat(script: '@git rev-parse --short HEAD', returnStdout: true).trim()
                    }
                    echo "Building commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        stage('Verify Environment') {
            steps {
                echo '=== Stage 2: Verify Java, Maven, and Docker Environment ==='
                script {
                    if (isUnix()) {
                        sh '''
                            echo "--- Java Version ---"
                            java -version
                            echo "--- Maven Wrapper Version ---"
                            ./mvnw --version
                            echo "--- Maven Version ---"
                            mvn -version
                            echo "--- Docker Version ---"
                            docker version
                        '''
                    } else {
                        bat '''
                            @echo --- Java Version ---
                            @java -version
                            @echo --- Maven Wrapper Version ---
                            @call mvnw.cmd -v
                            @echo --- Docker Version ---
                            @docker version
                        '''
                    }
                }
            }
        }

        stage('Compile Application') {
            steps {
                echo '=== Stage 3: Compile Application ==='
                script {
                    if (isUnix()) {
                        sh './mvnw --batch-mode --no-transfer-progress clean compile'
                    } else {
                        bat 'call mvnw.cmd --batch-mode clean compile'
                    }
                }
            }
        }

        stage('Execute Tests') {
            steps {
                echo '=== Stage 4: Execute Automated Tests ==='
                script {
                    if (isUnix()) {
                        sh './mvnw --batch-mode --no-transfer-progress test'
                    } else {
                        bat 'call mvnw.cmd --batch-mode test'
                    }
                }
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package JAR') {
            steps {
                echo '=== Stage 5: Package Spring Boot Application ==='
                script {
                    if (isUnix()) {
                        sh './mvnw --batch-mode --no-transfer-progress package -DskipTests'
                    } else {
                        bat 'call mvnw.cmd --batch-mode package -DskipTests'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/student-management-1.0.0.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '=== Stage 6: Build Docker Image ==='
                script {
                    if (isUnix()) {
                        sh 'docker build -t ${IMAGE_NAME}:b${BUILD_NUMBER} -t ${IMAGE_NAME}:${GIT_COMMIT_SHORT} -t ${IMAGE_NAME}:latest .'
                    } else {
                        bat 'docker build -t %IMAGE_NAME%:b%BUILD_NUMBER% -t %IMAGE_NAME%:%GIT_COMMIT_SHORT% -t %IMAGE_NAME%:latest .'
                    }
                }
            }
        }

        stage('Deploy Container') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                echo '=== Stage 7: Deploy Updated Application Container ==='
                script {
                    def volume = resolveDataVolume()
                    echo "Deploying with data volume: ${volume}"

                    if (isUnix()) {
                        sh """
                            set -eu
                            if docker inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
                                echo "Stopping and removing existing container ${CONTAINER_NAME}..."
                                docker stop "${CONTAINER_NAME}" || true
                                docker rm "${CONTAINER_NAME}" || true
                            fi

                            docker volume create "${volume}" >/dev/null

                            echo "Starting new container from ${IMAGE_NAME}:${GIT_COMMIT_SHORT}..."
                            docker run -d \\
                                --name "${CONTAINER_NAME}" \\
                                --restart unless-stopped \\
                                -p "${HOST_PORT}:${CONTAINER_PORT}" \\
                                -v "${volume}:/app/data" \\
                                -e SPRING_PROFILES_ACTIVE=docker \\
                                -e SERVER_PORT="${CONTAINER_PORT}" \\
                                "${IMAGE_NAME}:${GIT_COMMIT_SHORT}"
                        """
                    } else {
                        bat """
                            @docker stop ${CONTAINER_NAME} 2>nul || ver >nul
                            @docker rm ${CONTAINER_NAME} 2>nul || ver >nul
                            @docker volume create ${volume} >nul
                            @docker run -d --name ${CONTAINER_NAME} --restart unless-stopped -p ${HOST_PORT}:${CONTAINER_PORT} -v ${volume}:/app/data -e SPRING_PROFILES_ACTIVE=docker -e SERVER_PORT=${CONTAINER_PORT} ${IMAGE_NAME}:%GIT_COMMIT_SHORT%
                        """
                    }
                }
            }
        }

        stage('Verify Application Health') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                echo '=== Stage 8: Verify Application Health After Deployment ==='
                script {
                    if (isUnix()) {
                        sh '''
                            set -eu
                            echo "Waiting up to 90 seconds for the application to report UP on http://${HEALTH_HOST}:${HOST_PORT}/actuator/health..."
                            attempt=1
                            while [ "$attempt" -le 45 ]; do
                                if response=$(curl --silent --fail "http://${HEALTH_HOST}:${HOST_PORT}/actuator/health" 2>/dev/null); then
                                    if printf '%s' "$response" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
                                        echo "Health check PASSED: application is UP."
                                        printf '%s\\n' "$response"
                                        exit 0
                                    fi
                                fi
                                sleep 2
                                attempt=$((attempt + 1))
                            done
                            echo "Health check FAILED. Container logs:"
                            docker logs --tail 100 "${CONTAINER_NAME}" || true
                            exit 1
                        '''
                    } else {
                        bat '''
                            @powershell -Command "for ($i = 1; $i -le 45; $i++) { Start-Sleep -Seconds 2; try { $res = Invoke-RestMethod -Uri 'http://localhost:%HOST_PORT%/actuator/health' -TimeoutSec 3; if ($res.status -eq 'UP') { Write-Host 'Health check PASSED: application is UP'; exit 0 } } catch { } }; Write-Error 'Health check failed'; exit 1"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution completed.'
        }
        success {
            echo "PIPELINE SUCCESS: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}] built, tested, containerized and deployed ${env.GIT_COMMIT_SHORT}."
        }
        failure {
            echo "PIPELINE FAILED: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}]. Inspect the stage logs above."
        }
    }
}

/**
 * Reuse the named volume already mounted at /app/data on the running container,
 * so an existing database is never orphaned by a redeploy. Falls back to
 * DATA_VOLUME when there is no container yet.
 */
def resolveDataVolume() {
    if (!isUnix()) {
        return env.DATA_VOLUME
    }
    def existing = sh(
        script: """
            if docker inspect "${env.CONTAINER_NAME}" >/dev/null 2>&1; then
                docker inspect --format '{{range .Mounts}}{{if eq .Destination "/app/data"}}{{.Name}}{{end}}{{end}}' "${env.CONTAINER_NAME}"
            fi
        """,
        returnStdout: true
    ).trim()
    return existing ? existing : env.DATA_VOLUME
}
