pipeline {
    agent any

    environment {
        APP_NAME        = 'student-management'
        IMAGE_NAME      = 'student-management'
        CONTAINER_NAME  = 'student-management-app'
        PORT            = '8080'
    }

    // Java 21, Maven 3.9, and Docker CLI are pre-installed in the Jenkins environment
    // tools {
    //     jdk 'JDK21'
    //     maven 'Maven3'
    // }

    stages {
        // -------------------------------------------------------------
        // Stage 1: Checkout Source Code
        // -------------------------------------------------------------
        stage('Checkout') {
            steps {
                echo '=== Stage 1: Checking out source code from Git repository ==='
                checkout scm
            }
        }

        // -------------------------------------------------------------
        // Stage 2: Compile Application
        // -------------------------------------------------------------
        stage('Build & Compile') {
            steps {
                echo '=== Stage 2: Compiling Java source code ==='
                script {
                    if (isUnix()) {
                        sh 'mvn clean compile'
                    } else {
                        bat 'mvn clean compile'
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Stage 3: Automated Unit and Integration Tests
        // -------------------------------------------------------------
        stage('Execute Tests') {
            steps {
                echo '=== Stage 3: Running automated unit and integration tests ==='
                script {
                    if (isUnix()) {
                        sh 'mvn test'
                    } else {
                        bat 'mvn test'
                    }
                }
            }
            post {
                always {
                    // Record test results for Jenkins UI dashboard
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        // -------------------------------------------------------------
        // Stage 4: Package Runnable JAR
        // -------------------------------------------------------------
        stage('Package JAR') {
            steps {
                echo '=== Stage 4: Packaging application into executable Spring Boot JAR ==='
                script {
                    if (isUnix()) {
                        sh 'mvn package -DskipTests'
                    } else {
                        bat 'mvn package -DskipTests'
                    }
                }
            }
            post {
                success {
                    // Archive production JAR artifact
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // -------------------------------------------------------------
        // Stage 5: Build Docker Container Image
        // -------------------------------------------------------------
        stage('Docker Build') {
            steps {
                echo '=== Stage 5: Building Docker Image ==='
                script {
                    def imageTag = "${IMAGE_NAME}:${BUILD_NUMBER}"
                    def latestTag = "${IMAGE_NAME}:latest"
                    if (isUnix()) {
                        sh "docker build -t ${imageTag} -t ${latestTag} ."
                    } else {
                        bat "docker build -t ${imageTag} -t ${latestTag} ."
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Stage 6: Deploy Container Locally
        // -------------------------------------------------------------
        stage('Docker Deploy') {
            steps {
                echo '=== Stage 6: Deploying Docker container to local environment ==='
                script {
                    if (isUnix()) {
                        sh """
                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true
                            docker run -d --name ${CONTAINER_NAME} -p ${PORT}:8080 ${IMAGE_NAME}:latest
                        """
                    } else {
                        bat """
                            docker stop ${CONTAINER_NAME} 2>nul || ver >nul
                            docker rm ${CONTAINER_NAME} 2>nul || ver >nul
                            docker run -d --name ${CONTAINER_NAME} -p ${PORT}:8080 ${IMAGE_NAME}:latest
                        """
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Stage 7: Health Check Verification
        // -------------------------------------------------------------
        stage('Health Check') {
            steps {
                echo '=== Stage 7: Verifying application deployment health ==='
                script {
                    echo 'Waiting 20 seconds for container startup...'
                    sleep time: 20, unit: 'SECONDS'
                    if (isUnix()) {
                        sh """
                            curl -s -f http://localhost:${PORT}/actuator/health || exit 1
                        """
                    } else {
                        bat """
                            powershell -Command "try { \$res = Invoke-RestMethod -Uri 'http://localhost:${PORT}/actuator/health' -TimeoutSec 10; if (\$res.status -eq 'UP') { Write-Host 'Health Check PASSED: Application is UP'; exit 0 } else { Write-Error 'Application reported status not UP'; exit 1 } } catch { Write-Error \$_; exit 1 }"
                        """
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Post-Pipeline Actions & Notifications
    // -----------------------------------------------------------------
    post {
        always {
            echo 'Pipeline execution completed.'
            cleanWs deleteDirs: true, notFailBuild: true, patterns: [[pattern: 'target/**', type: 'EXCLUDE']]
        }
        success {
            echo "🎉 PIPELINE SUCCESS: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}] successfully built, tested, containerized, and deployed!"
        }
        failure {
            echo "❌ PIPELINE FAILED: ${env.JOB_NAME} [Build #${env.BUILD_NUMBER}] encountered an error. Please inspect logs."
        }
    }
}
