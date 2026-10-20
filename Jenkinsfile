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
            defaultValue: false,
            description: 'Replace the running student-management-app container after the build succeeds. Requires approval.'
        )
    }

    environment {
        IMAGE_NAME     = 'student-management'
        CONTAINER_NAME = 'student-management-app'
        HOST_PORT      = '8081'
        CONTAINER_PORT = '8080'
        DATA_VOLUME    = 'student-management_data'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out the configured GitHub branch.'
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

        stage('Build & Compile') {
            steps {
                echo 'Building from the repository root with the agent-installed Maven.'
                script {
                    if (isUnix()) {
                        sh 'mvn --batch-mode --no-transfer-progress clean compile'
                    } else {
                        bat 'mvn --batch-mode --no-transfer-progress clean compile'
                    }
                }
            }
        }

        stage('Execute Tests') {
            steps {
                echo 'Running the automated unit and web-layer tests.'
                script {
                    if (isUnix()) {
                        sh 'mvn --batch-mode --no-transfer-progress test'
                    } else {
                        bat 'mvn --batch-mode --no-transfer-progress test'
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
                echo 'Packaging the executable Spring Boot JAR.'
                script {
                    if (isUnix()) {
                        sh 'mvn --batch-mode --no-transfer-progress package -DskipTests'
                    } else {
                        bat 'mvn --batch-mode --no-transfer-progress package -DskipTests'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/student-management-1.0.0.jar', fingerprint: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building commit-specific and build-number Docker tags.'
                script {
                    if (isUnix()) {
                        sh 'docker build -t ${IMAGE_NAME}:b${BUILD_NUMBER} -t ${IMAGE_NAME}:${GIT_COMMIT_SHORT} -t ${IMAGE_NAME}:latest .'
                    } else {
                        bat 'docker build -t %IMAGE_NAME%:b%BUILD_NUMBER% -t %IMAGE_NAME%:%GIT_COMMIT_SHORT% -t %IMAGE_NAME%:latest .'
                    }
                }
            }
        }

        stage('Docker Deploy') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                script {
                    if (!isUnix()) {
                        error('Deployment requires the configured Linux Jenkins container agent.')
                    }
                }
                timeout(time: 5, unit: 'MINUTES') {
                    input message: 'This will stop and replace the running student-management-app container on port 8081. Continue?', ok: 'Deploy'
                }
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
                        docker stop "$CONTAINER_NAME"
                        docker rm "$CONTAINER_NAME"
                    fi

                    docker run -d \\
                        --name "$CONTAINER_NAME" \\
                        --restart unless-stopped \\
                        -p "$HOST_PORT:$CONTAINER_PORT" \\
                        -v "$selected_volume:/app/data" \\
                        -e SPRING_PROFILES_ACTIVE=docker \\
                        -e SERVER_PORT="$CONTAINER_PORT" \\
                        "$IMAGE_NAME:$GIT_COMMIT_SHORT"
                '''
            }
        }

        stage('Health Check') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                echo 'Waiting up to 60 seconds for the deployed app to report UP.'
                sh '''
                    set -eu
                    attempt=1
                    while [ "$attempt" -le 30 ]; do
                        if response=$(curl --silent --show-error --fail "http://localhost:$HOST_PORT/actuator/health" 2>/dev/null); then
                            if printf '%s' "$response" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
                                printf '%s\\n' "$response"
                                exit 0
                            fi
                        fi
                        sleep 2
                        attempt=$((attempt + 1))
                    done
                    docker logs --tail 80 "$CONTAINER_NAME" || true
                    echo 'Health check failed.'
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            deleteDir()
            echo 'Pipeline execution completed.'
        }
        success {
            echo "PIPELINE SUCCESS: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}] completed."
        }
        failure {
            echo "PIPELINE FAILED: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}]. Inspect the stage logs."
        }
    }
}