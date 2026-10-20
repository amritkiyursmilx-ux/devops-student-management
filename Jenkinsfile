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
        DATA_VOLUME    = 'student-data'
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
                            echo "--- Maven Version ---"
                            mvn -version
                            echo "--- Docker Version ---"
                            docker version
                        '''
                    } else {
                        bat '''
                            @echo --- Java Version ---
                            @java -version
                            @echo --- Maven Version ---
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
                        sh 'mvn --batch-mode --no-transfer-progress clean compile'
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
                        sh 'mvn --batch-mode --no-transfer-progress test'
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
                        sh 'mvn --batch-mode --no-transfer-progress package -DskipTests'
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
                    if (isUnix()) {
                        sh '''
                            set -eu
                            selected_volume="$DATA_VOLUME"
                            if docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
                                existing_volume=$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/app/data"}}{{.Name}}{{end}}{{end}}' "$CONTAINER_NAME")
                                if [ -n "$existing_volume" ]; then
                                    selected_volume="$existing_volume"
                                fi
                            fi

                            docker volume create "$selected_volume" >/dev/null

                            if docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
                                echo "Stopping and removing existing container $CONTAINER_NAME..."
                                docker stop "$CONTAINER_NAME" || true
                                docker rm "$CONTAINER_NAME" || true
                            fi

                            echo "Deploying new container $CONTAINER_NAME with volume $selected_volume..."
                            docker run -d \\
                                --name "$CONTAINER_NAME" \\
                                --restart unless-stopped \\
                                -p "$HOST_PORT:$CONTAINER_PORT" \\
                                -v "$selected_volume:/app/data" \\
                                -e SPRING_PROFILES_ACTIVE=docker \\
                                -e SERVER_PORT="$CONTAINER_PORT" \\
                                "$IMAGE_NAME:$GIT_COMMIT_SHORT"
                        '''
                    } else {
                        bat '''
                            @docker stop %CONTAINER_NAME% 2>nul || ver >nul
                            @docker rm %CONTAINER_NAME% 2>nul || ver >nul
                            @docker run -d --name %CONTAINER_NAME% --restart unless-stopped -p %HOST_PORT%:%CONTAINER_PORT% -v %DATA_VOLUME%:/app/data -e SPRING_PROFILES_ACTIVE=docker -e SERVER_PORT=%CONTAINER_PORT% %IMAGE_NAME%:%GIT_COMMIT_SHORT%
                        '''
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
                            echo "Waiting up to 60 seconds for application to report UP on http://localhost:$HOST_PORT/actuator/health..."
                            attempt=1
                            while [ "$attempt" -le 30 ]; do
                                if response=$(curl --silent --show-error --fail "http://localhost:$HOST_PORT/actuator/health" 2>/dev/null); then
                                    if printf '%s' "$response" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
                                        echo "Health Check PASSED: Application is UP!"
                                        printf '%s\\n' "$response"
                                        exit 0
                                    fi
                                fi
                                sleep 2
                                attempt=$((attempt + 1))
                            done
                            echo "Health check failed! Showing container logs:"
                            docker logs --tail 80 "$CONTAINER_NAME" || true
                            exit 1
                        '''
                    } else {
                        bat '''
                            @powershell -Command "for ($i = 1; $i -le 30; $i++) { Start-Sleep -Seconds 2; try { $res = Invoke-RestMethod -Uri 'http://localhost:%HOST_PORT%/actuator/health' -TimeoutSec 3; if ($res.status -eq 'UP') { Write-Host 'Health Check PASSED: Application is UP'; exit 0 } } catch { } }; Write-Error 'Health check failed'; exit 1"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            deleteDir()
            echo 'Pipeline execution completed.'
        }
        success {
            echo "PIPELINE SUCCESS: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}] successfully built, tested, containerized, and deployed!"
        }
        failure {
            echo "PIPELINE FAILED: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}]. Inspect stage logs."
        }
    }
}
